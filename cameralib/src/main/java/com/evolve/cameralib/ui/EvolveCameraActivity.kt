package com.evolve.cameralib.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.window.layout.WindowMetrics
import androidx.window.layout.WindowMetricsCalculator
import com.evolve.cameralib.EvolveImagePicker
import com.evolve.cameralib.R
import com.evolve.cameralib.databinding.ActivityEvolveCameraBinding
import com.evolve.cameralib.databinding.CameraUiContainerBinding
import com.evolve.cameralib.databinding.FragmentCameraBinding
import com.evolve.cameralib.utils.*
import com.evolve.cameralib.utils.showToast
import com.permissionx.guolindev.PermissionX
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val IMMERSIVE_FLAG_TIMEOUT = 500L
private val TAG = EvolveCameraActivity::class.java.canonicalName

class EvolveCameraActivity : AppCompatActivity(),
    CameraXConfig.Provider {

    private var binding: ActivityEvolveCameraBinding? = null

    private var cameraUiContainerBinding: CameraUiContainerBinding? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var deviceOrientation = OrientationEventListener.ORIENTATION_UNKNOWN
    private var displayId: Int = -1
    private lateinit var cameraExecutor: ExecutorService

    private val forceImageCapture: Boolean by lazy {
        intent?.extras?.getBoolean(EvolveImagePicker.KEY_CAMERA_CAPTURE_FORCE) == true
    }

    private val frontCameraEnable: Boolean by lazy {
        intent?.extras?.getBoolean(EvolveImagePicker.KEY_FRONT_CAMERA) == true
    }

    private val imgFileName: String by lazy {
        intent?.extras?.getString(EvolveImagePicker.KEY_FILENAME, "") ?: ""
    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(
            this,
            SensorManager.SENSOR_DELAY_NORMAL
        ) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }
                deviceOrientation = orientation
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = rotation
                val isCaptureReady = when (orientation) {
                    in 0..10 -> true
                    in 85..95 -> true
                    in 175..185 -> true
                    in 265..275 -> true
                    else -> false
                }
                if (!forceImageCapture) return
                if (isCaptureReady) {
                    enableCaptureBtn()
                    showSuccessToast()
                    readyBg()
                } else {
                    disableCaptureBtn()
                    showWarningToast()
                    warningBg()
                }
            }
        }
    }

    private val displayManager by lazy {
        this.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = binding?.root?.let { view ->
            if (displayId == this@EvolveCameraActivity.displayId) {
                imageCapture?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    private lateinit var windowMetrics: WindowMetrics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEvolveCameraBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        try {
            this.supportActionBar?.hide()

            cameraExecutor = Executors.newSingleThreadExecutor()

            checkPermission()

        } catch (e: NullPointerException) {
            e.printStackTrace()
        }

    }

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build()
    }

    override fun onResume() {
        super.onResume()
        try {

            binding?.cameraContainer?.postDelayed({
                hideSystemUI()
            }, IMMERSIVE_FLAG_TIMEOUT)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            finishAfterTransition()
        } else {
            finish()
            super.onBackPressed()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding!!.cameraContainer).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        binding = null
        cameraUiContainerBinding = null

        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)

        super.onDestroy()
    }

    private fun checkPermission() {

        PermissionX.init(this)
            .permissions(
                Manifest.permission.CAMERA
            )
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Given permission is needed to take picture",
                    "OK",
                    "Cancel"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "You need to allow given permission in Settings youself",
                    "OK",
                    "Cancel"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    onPermissionGranted()
                } else {
                    this.showToast("Camera permission denied")
                    finish()
                }
            }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            lensFacing = when {
                hasBackCamera(cameraProvider) -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera(cameraProvider) -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }
            updateCameraSwitchButton()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val screenAspectRatio =
            aspectRatio(windowMetrics.bounds.width(), windowMetrics.bounds.height())
        val rotation = binding?.viewFinder?.display?.rotation
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(
                rotation ?: OrientationEventListener.ORIENTATION_UNKNOWN
            )
            .build()

        try {
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(
                    rotation ?: OrientationEventListener.ORIENTATION_UNKNOWN
                )
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build()
        } catch (e: Exception) {
            Log.d(TAG, "bindCameraUseCases: imagecaptureformat error: ${e.localizedMessage}")
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )

            preview?.setSurfaceProvider(binding?.viewFinder?.surfaceProvider)

            observeCameraState(camera?.cameraInfo)

        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun observeCameraState(cameraInfo: CameraInfo?) {
        cameraInfo?.cameraState?.observe(this) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        Log.d(TAG, "observeCameraState: CameraState: Pending Open")
                    }
                    CameraState.Type.OPENING -> {
                        Log.d(TAG, "observeCameraState: CameraState: Opening")
                    }
                    CameraState.Type.OPEN -> {
                        Log.d(TAG, "observeCameraState: CameraState: Open")
                    }
                    CameraState.Type.CLOSING -> {
                        Log.d(TAG, "observeCameraState: CameraState: Closing")
                    }
                    CameraState.Type.CLOSED -> {
                        Log.d(TAG, "observeCameraState: CameraState: Closed")
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    CameraState.ERROR_STREAM_CONFIG -> {
                        this.showToast("Stream config error")
                    }
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        this.showToast("Camera in use")
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        this.showToast("Max cameras in use")
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        this.showToast("Other recoverable error")
                    }
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        this.showToast("Camera disabled")
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        this.showToast("Fatal error")
                    }
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        this.showToast("Do not disturb mode enabled")
                    }
                }
            }
        }
    }

    private fun updateCameraUi() {
        cameraUiContainerBinding?.root?.let {
            binding?.root?.removeView(it)
        }
        cameraUiContainerBinding = CameraUiContainerBinding.inflate(
            LayoutInflater.from(this),
            binding?.root,
            true
        )
        cameraUiContainerBinding?.cameraCaptureButton?.setOnClickListener {
            imageCapture?.let { imageCapture ->

                val photoFile = createImageFile(this, imgFileName)

                val metadata = ImageCapture.Metadata().apply {
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

                imageCapture.takePicture(
                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                            Log.d(TAG, "Photo capture succeeded: $savedUri")
                            val intent = Intent()
                            intent.data = savedUri
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                    })

            }
        }

        cameraUiContainerBinding?.cameraSwitchButton?.let {
            it.isEnabled = false
            it.setOnClickListener {
                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                bindCameraUseCases()
            }
        }
    }

    private fun updateCameraSwitchButton() {
        try {
            cameraUiContainerBinding?.cameraSwitchButton?.isEnabled =
                hasBackCamera(cameraProvider) || hasFrontCamera(cameraProvider)

            if (frontCameraEnable) {
                cameraUiContainerBinding?.cameraSwitchButton?.visibility = View.VISIBLE
            } else {
                cameraUiContainerBinding?.cameraSwitchButton?.visibility = View.GONE
            }
        } catch (exception: CameraInfoUnavailableException) {
            cameraUiContainerBinding?.cameraSwitchButton?.isEnabled = false
        }
    }

    private fun showSuccessToast() {
        cameraUiContainerBinding?.warningView?.layoutWarning?.visibility = View.GONE
        cameraUiContainerBinding?.successView?.layoutSuccess?.visibility = View.VISIBLE
    }

    private fun showWarningToast() {
        cameraUiContainerBinding?.successView?.layoutSuccess?.visibility = View.GONE
        cameraUiContainerBinding?.warningView?.layoutWarning?.visibility = View.VISIBLE
    }

    private fun readyBg() {
        cameraUiContainerBinding?.cameraCaptureButton?.setBackgroundResource(R.drawable.ic_shutter_ready)
    }

    private fun warningBg() {
        cameraUiContainerBinding?.cameraCaptureButton?.setBackgroundResource(R.drawable.ic_shutter_warning)
    }

    private fun enableCaptureBtn() {
        cameraUiContainerBinding?.cameraCaptureButton?.isEnabled = true
    }

    private fun disableCaptureBtn() {
        cameraUiContainerBinding?.cameraCaptureButton?.isEnabled = false
    }

    private fun onPermissionGranted() {

        windowMetrics = WindowMetricsCalculator.getOrCreate()
            .computeCurrentWindowMetrics(this)

        displayManager.registerDisplayListener(displayListener, null)

        binding?.viewFinder?.post {
            updateCameraUi()
            setUpCamera()
        }
    }

}
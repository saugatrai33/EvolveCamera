package com.evolve.cameralib.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.FLASH_MODE_AUTO
import androidx.camera.core.impl.utils.Exif
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.window.WindowManager
import com.evolve.cameralib.R
import com.evolve.cameralib.databinding.CameraUiContainerBinding
import com.evolve.cameralib.databinding.FragmentCameraBinding
import com.evolve.cameralib.utils.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.evolve.cameralib.EvolveImagePicker.Companion.KEY_CAMERA_CAPTURE_FORCE
import com.evolve.cameralib.EvolveImagePicker.Companion.KEY_FILENAME
import com.evolve.cameralib.EvolveImagePicker.Companion.KEY_FRONT_CAMERA
import com.evolve.cameralib.EvolveImagePicker.Companion.KEY_IMAGE_CAPTURE_FORMAT

/**
 * Main fragment for this app. Implements all camera operations including:
 * - Viewfinder
 * - Photo taking
 */
class CameraFragment : Fragment() {

    private val TAG = CameraFragment::class.java.canonicalName

    private var binding: FragmentCameraBinding? = null
    private var cameraUiContainerBinding: CameraUiContainerBinding? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var windowManager: WindowManager
    private var deviceOrientation = OrientationEventListener.ORIENTATION_UNKNOWN

    private val forceImageCapture: Boolean by lazy {
        activity?.intent?.extras?.getBoolean(KEY_CAMERA_CAPTURE_FORCE) == true
    }
    private val frontCameraEnable: Boolean by lazy {
        activity?.intent?.extras?.getBoolean(KEY_FRONT_CAMERA) == true
    }
    private val imgFileName: String by lazy {
        activity?.intent?.extras?.getString(KEY_FILENAME, "")!!
    }
    private val imageCaptureFormat: Int by lazy {
        activity?.intent?.extras?.getInt(
            KEY_IMAGE_CAPTURE_FORMAT,
            ImageFormat.JPEG
        )!!
    }

    private lateinit var cameraExecutor: ExecutorService

    private val orientationEventListener by lazy {
        object : OrientationEventListener(
            requireContext()
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

    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().finish()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(), callback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(
            inflater,
            container,
            false
        )
        return binding!!.root
    }

    override fun onResume() {
        super.onResume()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                CameraFragmentDirections.actionCameraFragmentToPermissionsFragment()
            )
        }
    }

    override fun onDestroyView() {
        binding = null
        cameraUiContainerBinding = null
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    @SuppressLint("MissingPermission", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        windowManager = WindowManager(view.context)

        binding!!.viewFinder.post {
            updateCameraUi()
            setUpCamera()
        }

        binding!!.viewFinder.setOnTouchListener { _: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                MotionEvent.ACTION_UP -> {
                    val factory = binding!!.viewFinder.meteringPointFactory
                    val point = factory.createPoint(motionEvent.x, motionEvent.y)
                    val action = FocusMeteringAction.Builder(point).build()
                    camera!!.cameraControl.startFocusAndMetering(action)
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        bindCameraUseCases()
        updateCameraSwitchButton()
        updateCameraUi()
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            lensFacing = when {
                hasBackCamera(cameraProvider!!) -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera(cameraProvider!!) -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }
            updateCameraSwitchButton()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases() {
        val metrics = windowManager.getCurrentWindowMetrics().bounds
        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        val rotation = binding!!.viewFinder.display.rotation
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
        try {
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .setFlashMode(FLASH_MODE_AUTO)
                .setBufferFormat(imageCaptureFormat)
                .build()
        } catch (e: Exception) {
            Toast.makeText(
                requireActivity(),
                "Image format unsupported", Toast.LENGTH_LONG
            ).show()
            Log.d(TAG, "bindCameraUseCases: imagecaptureformat error: ${e.localizedMessage}")
        }
        cameraProvider.unbindAll()
        try {
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
            preview?.setSurfaceProvider(binding!!.viewFinder.surfaceProvider)
            observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
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
            binding!!.root.removeView(it)
        }
        cameraUiContainerBinding = CameraUiContainerBinding.inflate(
            LayoutInflater.from(requireContext()),
            binding!!.root,
            true
        )
        cameraUiContainerBinding?.cameraCaptureButton?.setOnClickListener {
            imageCapture?.let { imageCapture ->
                val photoFile = createImageFile(requireContext(), imgFileName)
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
                            requireActivity().setResult(Activity.RESULT_OK, intent)
                            requireActivity().finish()
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
                hasBackCamera(cameraProvider!!) || hasFrontCamera(cameraProvider!!)

            if (frontCameraEnable) {
                cameraUiContainerBinding?.cameraSwitchButton?.visibility = View.VISIBLE
            } else {
                cameraUiContainerBinding?.cameraSwitchButton?.visibility = View.GONE
            }
        } catch (exception: CameraInfoUnavailableException) {
            cameraUiContainerBinding?.cameraSwitchButton?.isEnabled = false
        }
    }
    // endregion

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
}
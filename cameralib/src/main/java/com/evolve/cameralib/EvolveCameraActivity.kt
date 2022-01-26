package com.evolve.cameralib

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.evolve.cameralib.databinding.ActivityEvolveCameraBinding

private const val IMMERSIVE_FLAG_TIMEOUT = 500L

class EvolveCameraActivity : AppCompatActivity(),
    CameraXConfig.Provider {

    private lateinit var binding: ActivityEvolveCameraBinding
    private val screenOrientation: Int by lazy {
        intent?.extras?.getInt(
            EvolveImagePicker.KEY_SCREEN_ORIENTATION,
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        )!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEvolveCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try {
            this.supportActionBar?.hide()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        requestedOrientation = screenOrientation
    }

    override fun getCameraXConfig(): CameraXConfig {
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig())
            .setMinimumLoggingLevel(Log.ERROR).build()
    }

    override fun onResume() {
        super.onResume()
        // Before setting full screen flags, we must wait a bit to let UI settle; otherwise, we may
        // be trying to set app to immersive mode before it's ready and the flags do not stick
        binding.fragmentContainer.postDelayed({
            hideSystemUI()
        }, IMMERSIVE_FLAG_TIMEOUT)
    }

    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Workaround for Android Q memory leak issue in IRequestFinishCallback$Stub.
            finishAfterTransition()
        } else {
            finish()
            super.onBackPressed()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.fragmentContainer).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

}
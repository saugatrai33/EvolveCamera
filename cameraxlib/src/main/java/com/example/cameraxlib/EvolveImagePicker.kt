package com.example.cameraxlib

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Create EvolveImagePicker object
 * */
class EvolveImagePicker {

    companion object {
        const val KEY_FRONT_CAMERA = "frontCamera"
        const val KEY_CAMERA_CAPTURE_FORCE = "forceCameraCapture"

        /**
         * Use this to use EvolveImagePicker in Activity Class
         *
         * @param activity AppCompatActivity Instance
         */
        @JvmStatic
        fun with(activity: AppCompatActivity): Builder {
            return Builder(activity)
        }
    }

    class Builder(private val activity: AppCompatActivity) {

        fun start(
            launcher: ActivityResultLauncher<Intent>,
            forceImageCapture: Boolean = true,
            enabledFrontCamera: Boolean = true
        ) {
            startActivity(launcher, forceImageCapture, enabledFrontCamera)
        }

        private fun startActivity(
            launcher: ActivityResultLauncher<Intent>,
            forceImageCapture: Boolean,
            enabledFrontCamera: Boolean
        ) {
            val intent = Intent(activity, EvolveCameraActivity::class.java)
            intent.putExtra(KEY_CAMERA_CAPTURE_FORCE, forceImageCapture)
            intent.putExtra(KEY_FRONT_CAMERA, enabledFrontCamera)
            launcher.launch(intent)
        }
    }
}
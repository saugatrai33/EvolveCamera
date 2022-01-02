package com.example.cameraxlib

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
        fun with(activity: Activity): Builder {
            return Builder(activity)
        }

        /**
         * Calling from fragment
         * */
        @JvmStatic
        fun with(fragment: Fragment): Builder {
            return Builder(fragment)
        }
    }

    class Builder(private val activity: Activity) {

        private var fragment: Fragment? = null

        constructor(fragment: Fragment) : this(fragment.requireActivity()) {
            this.fragment = fragment
        }

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
            val imagePickerIntent: Intent = if (fragment != null) {
                Intent(fragment?.requireActivity(), EvolveCameraActivity::class.java)
            } else {
                Intent(activity, EvolveCameraActivity::class.java)
            }
            imagePickerIntent.putExtra(KEY_CAMERA_CAPTURE_FORCE, forceImageCapture)
            imagePickerIntent.putExtra(KEY_FRONT_CAMERA, enabledFrontCamera)
            launcher.launch(imagePickerIntent)
        }
    }
}
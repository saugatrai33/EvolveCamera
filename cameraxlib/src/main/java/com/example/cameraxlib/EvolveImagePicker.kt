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

        fun start(launcher: ActivityResultLauncher<Intent>) {
            startActivity(launcher)
        }

        private fun startActivity(launcher: ActivityResultLauncher<Intent>) {
            val intent = Intent(activity, EvolveCameraActivity::class.java)
            launcher.launch(intent)
        }
    }
}
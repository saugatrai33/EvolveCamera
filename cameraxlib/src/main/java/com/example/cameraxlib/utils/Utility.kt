package com.example.cameraxlib.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import com.example.cameraxlib.R
import com.example.cameraxlib.utils.Constants.RATIO_16_9_VALUE
import com.example.cameraxlib.utils.Constants.RATIO_4_3_VALUE
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Use external media if it is available, our app's file directory otherwise */
fun getOutputDirectory(context: Context): File {
    val appContext = context.applicationContext
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists())
        mediaDir else appContext.filesDir
}

/**
 *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
 *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
 *
 *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
 *  of preview ratio to one of the provided values.
 *
 *  @param width - preview width
 *  @param height - preview height
 *  @return suitable aspect ratio
 */
fun aspectRatio(width: Int, height: Int): Int {
    val previewRatio = max(width, height).toDouble() / min(width, height)
    if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
        return AspectRatio.RATIO_4_3
    }
    return AspectRatio.RATIO_16_9
}

/** Returns true if the device has an available back camera. False otherwise */
fun hasBackCamera(cameraProvider: ProcessCameraProvider): Boolean {
    return cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
}

/** Returns true if the device has an available front camera. False otherwise */
fun hasFrontCamera(cameraProvider: ProcessCameraProvider): Boolean {
    return cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
}

/** Helper function used to create a timestamped file */
fun createFile(baseFolder: File, format: String, extension: String) =
    File(
        baseFolder, SimpleDateFormat(format, Locale.US)
            .format(System.currentTimeMillis()) + extension
    )

@Throws(IOException::class)
fun createImageFile(context: Context, fileName: String): File {
    // Create an image file name
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        createFileName(fileName), /* prefix */
        ".jpg", /* suffix */
        storageDir /* directory */
    )
}

/**
 * create file name for the given monitoring of the given project.
 *
 * @return String obtained after concatenation of timestamp with projectId and serverId */
private fun createFileName(fileName: String = ""): String {
    val timeStamp = Date().time
    return "$fileName$timeStamp"
}
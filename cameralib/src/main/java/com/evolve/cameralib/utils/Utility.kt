package com.evolve.cameralib.utils

import android.content.Context
import android.os.Environment
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import com.evolve.cameralib.R
import com.evolve.cameralib.utils.Constants.RATIO_16_9_VALUE
import com.evolve.cameralib.utils.Constants.RATIO_4_3_VALUE
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
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

@Throws(IOException::class)
fun createImageFile(context: Context, fileName: String): File {
    val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        createFileName(fileName),
        ".jpg",
        storageDir
    )
}

/**
 * create file name for the given monitoring of the given project.
 *
 * @return String obtained after concatenation of timestamp with projectId and serverId */
private fun createFileName(fileName: String): String {
    val timeStamp = Date().time
    return "$fileName$timeStamp"
}
package com.example.evolvecamerax

import android.content.Intent
import android.content.res.Configuration
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxlib.EvolveImagePicker
import com.squareup.picasso.Picasso
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.*

class MainActivity : AppCompatActivity() {

//    private lateinit var picture: SimpleDraweeView

    private lateinit var picture: ImageView
    private var imageUri: Uri? = null

    private var imgRotation = 0

    private val evolveActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data!!.data
            imageUri = data!!
            Log.d("MainActivity::", "result: $data")
            showImage()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        picture = findViewById(R.id.image)
        val btnCamera: Button = findViewById(R.id.btnCamera)
        btnCamera.setOnClickListener {
            EvolveImagePicker.with(this)
                .start(evolveActivityResultLauncher)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        showImage()
    }

    private fun showImage() {
        if (imageUri == null) return
        Picasso.get()
            .load(imageUri)
            .into(picture)
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun getBitmapFormUri(ac: AppCompatActivity, uri: Uri?): Bitmap? {
        var input: InputStream? = ac.contentResolver.openInputStream(uri!!)
        val onlyBoundsOptions = BitmapFactory.Options()
        onlyBoundsOptions.inJustDecodeBounds = true
        onlyBoundsOptions.inDither = true //optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
        input!!.close()
        val originalWidth = onlyBoundsOptions.outWidth
        val originalHeight = onlyBoundsOptions.outHeight
        if (originalWidth == -1 || originalHeight == -1) return null
        //Image resolution is based on 480x800
        val hh = 800f //The height is set as 800f here
        val ww = 480f //Set the width here to 480f
        //Zoom ratio. Because it is a fixed scale, only one data of height or width is used for calculation
        var be = 1 //be=1 means no scaling
        if (originalWidth > originalHeight && originalWidth > ww) { //If the width is large, scale according to the fixed size of the width
            be = (originalWidth / ww).toInt()
        } else if (originalWidth < originalHeight && originalHeight > hh) { //If the height is high, scale according to the fixed size of the width
            be = (originalHeight / hh).toInt()
        }
        if (be <= 0) be = 1
        //Proportional compression
        val bitmapOptions = BitmapFactory.Options()
        bitmapOptions.inSampleSize = be //Set scaling
        bitmapOptions.inDither = true //optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888 //optional
        input = ac.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        input!!.close()
        return bitmap
    }

}

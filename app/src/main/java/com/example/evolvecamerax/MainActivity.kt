package com.example.evolvecamerax

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
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
import android.os.PersistableBundle
import java.io.*

const val REQUEST_CODE_IMAGE_PICK = 101

class MainActivity : AppCompatActivity() {

    private lateinit var picture: ImageView
    private var imageUri: Uri? = null

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

            /*EvolveImagePicker.with(this)
                .start(REQUEST_CODE)*/
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_IMAGE_PICK) {
                val result = data!!.data
                imageUri = result
                Log.d("MainActivity::", "result: $data")
                showImage()
            }
        }
    }

    private fun showImage() {
        Picasso.get()
            .load(imageUri)
            .into(picture)
    }
}

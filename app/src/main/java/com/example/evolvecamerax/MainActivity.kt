package com.example.evolvecamerax

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxlib.EvolveImagePicker
import com.squareup.picasso.Picasso


class MainActivity : AppCompatActivity() {

    private lateinit var picture: ImageView
    private var imageUri: Uri? = null

    private val evolveActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val photoUri: Uri? = result.data?.data
            if (photoUri != null) {
                imageUri = photoUri
                showImage()
            }
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

    private fun showImage() {
        Picasso.get()
            .load(imageUri)
            .into(picture)
    }
}

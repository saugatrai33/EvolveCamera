package com.evolve.evolvecamera

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.evolve.cameralib.EvolveImagePicker
import com.squareup.picasso.Picasso
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var picture: ImageView

    private val evolveActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val photoUri: Uri? = result.data?.data
            photoUri?.let {
                showImage(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        picture = findViewById(R.id.image)
        val btnCamera: Button = findViewById(R.id.btnCamera)
        btnCamera.setOnClickListener {
            EvolveImagePicker
                .with(this)
                .start(
                    evolveActivityResultLauncher,
                    forceImageCapture = true
                )
        }
    }

    private fun showImage(uri: Uri) {
        Picasso.get()
            .load(uri)
            .into(picture)
    }
}

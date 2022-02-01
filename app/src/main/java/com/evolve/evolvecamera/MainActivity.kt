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
            EvolveImagePicker
                .with(this)
                .start(
                    evolveActivityResultLauncher,
                    forceImageCapture = true
                )
        }
        picture.setOnClickListener {
            val photoURI = FileProvider.getUriForFile(
                this,
                this.applicationContext.packageName.toString() + ".provider",
                File(imageUri!!.path)
            )
            val photoIntent = Intent(ACTION_VIEW, photoURI)
            photoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            photoIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(
                photoIntent
            )
        }
    }

    private fun showImage() {
        Picasso.get()
            .load(imageUri)
            .into(picture)
    }
}

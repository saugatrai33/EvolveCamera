package com.evolve.evolvecamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.evolve.cameralib.EvolveImagePicker

class MainActivity : AppCompatActivity() {

    private lateinit var picture: ImageView
    private var photoUri: Uri? = null

    private val evolveActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val intent = result?.data
            val imagePath = intent?.getStringExtra("imagePath")
            showImage(imagePath)
         /*   photoUri = result.data?.data
            photoUri?.let {
                showImage(it)
            }*/
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

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.clear()
    }

    private fun showImage(imagePath: String?) {
        Glide.with(this)
            .load(imagePath)
            .placeholder(R.drawable.ic_photo)
            .into(picture)
    }
}

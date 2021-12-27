package com.example.evolvecamerax

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.cameraxlib.EvolveImagePicker

class MainActivity : AppCompatActivity() {

    private lateinit var image: ImageView

    private val evolveActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data!!.data
            Log.d("MainActivity::", "result: ${data.toString()}")
            Glide.with(this)
                .load(data)
                .into(image)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        image = findViewById(R.id.image)
        EvolveImagePicker.with(this)
            .start(evolveActivityResultLauncher)
    }
}
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
import com.facebook.drawee.backends.pipeline.Fresco
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.squareup.picasso.Picasso
import java.security.MessageDigest
import com.squareup.picasso.Transformation;
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.app.Activity
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.squareup.picasso.Target
import java.io.*
import java.lang.Exception


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
            val rotationDegree = result.data!!.extras!!.getInt("rotation")
            Log.d("rotationDegree", "rotationDegree: $rotationDegree")
            imageUri = data!!
            imgRotation = rotationDegree
            Log.d("MainActivity::", "result: $data")
            showImage()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fresco.initialize(this)
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

        /*val bitmap: Bitmap? = getBitmapFormUri(this, imageUri)

        val matrix = Matrix()
        matrix.postRotate(imgRotation.toFloat())

        val rotatedBitmap: Bitmap = Bitmap.createBitmap(
            bitmap!!,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        picture.setImageBitmap(rotatedBitmap)*/

        /*val imageRequest = ImageRequestBuilder.newBuilderWithSource(imageUri)
            .setRotationOptions(RotationOptions.forceRotation(ROTATE_90))
            .build()

        picture.controller = Fresco.newDraweeControllerBuilder()
            .setImageRequest(imageRequest)
            .build()

        picture.setImageURI(imageUri)*/

        Picasso.get()
            .load(imageUri)
            .rotate(imgRotation.toFloat())
            .fit()
            .centerCrop()
            .into(picture)

//        picture.rotation = 0f

/*        Glide.with(this)
            .load(imageUri)
            .transform(RotateTransformation(imgRotation.toFloat()))
            .into(picture)*/
    }

    class CircleTransform : Transformation {
        override fun transform(source: Bitmap): Bitmap {
            val size = Math.min(source.width, source.height)
            val x = (source.width - size) / 2
            val y = (source.height - size) / 2
            val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
            if (squaredBitmap != source) {
                source.recycle()
            }
            val bitmap = Bitmap.createBitmap(size, size, source.config)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            val shader = BitmapShader(
                squaredBitmap,
                Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
            )
            paint.setShader(shader)
            paint.setAntiAlias(true)
            val r = size / 2f
            canvas.drawCircle(r, r, r, paint)
            squaredBitmap.recycle()
            return bitmap
        }

        override fun key(): String {
            return "circle"
        }
    }

    class RotateTransformation(rotateRotationAngle: Float) :
        BitmapTransformation() {
        private var rotateRotationAngle = 0f
        override fun transform(
            pool: BitmapPool,
            toTransform: Bitmap,
            outWidth: Int,
            outHeight: Int
        ): Bitmap {
            val matrix = Matrix()
            matrix.postRotate(rotateRotationAngle)
            return Bitmap.createBitmap(
                toTransform,
                0,
                0,
                toTransform.width,
                toTransform.height,
                matrix,
                true
            )
        }

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update("rotate$rotateRotationAngle".toByteArray())
        }

        init {
            this.rotateRotationAngle = rotateRotationAngle
        }
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

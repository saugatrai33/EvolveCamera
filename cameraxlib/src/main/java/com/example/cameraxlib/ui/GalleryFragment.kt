package com.example.cameraxlib.ui

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.navigation.fragment.navArgs
import com.example.cameraxlib.R
import com.example.cameraxlib.databinding.FragmentGalleryBinding
import com.example.cameraxlib.utils.padWithDisplayCutout
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder

/** Fragment used to present the user with a gallery of photo taken */
class GalleryFragment internal constructor() : Fragment() {

    private var _fragmentGalleryBinding: FragmentGalleryBinding? = null

    private val fragmentGalleryBinding get() = _fragmentGalleryBinding!!

    private val args: GalleryFragmentArgs by navArgs()

    private lateinit var imgUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        retainInstance = true

        imgUri = Uri.parse(args.rootDirectory)
        Log.d("GalleryFragment", "onCreate: imgUri:: $imgUri")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentGalleryBinding = FragmentGalleryBinding.inflate(
            inflater,
            container, false
        )
        return fragmentGalleryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            fragmentGalleryBinding.cutoutSafeArea.padWithDisplayCutout()
        }

        // Handle back button press
        fragmentGalleryBinding.imgBack.setOnClickListener {
            Navigation.findNavController(
                requireActivity(),
                R.id.fragment_container
            ).navigateUp()
        }

        // Handle check button press
        fragmentGalleryBinding.imgCheck.setOnClickListener {
            val intent = Intent()
            intent.data = imgUri
            requireActivity().setResult(Activity.RESULT_OK, intent)
            requireActivity().finish()
        }
        showImg()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Checks the orientation of the screen
        showImg()
    }

    private fun showImg() {
        val imageRequest = ImageRequestBuilder.newBuilderWithSource(imgUri)
            .setRotationOptions(RotationOptions.autoRotate())
            .build()

        fragmentGalleryBinding.picture.controller = Fresco.newDraweeControllerBuilder()
            .setImageRequest(imageRequest)
            .build()

        fragmentGalleryBinding.picture.setImageURI(imgUri)
    }

    override fun onDestroyView() {
        _fragmentGalleryBinding = null
        super.onDestroyView()
    }
}
package com.evolve.cameralib.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.evolve.cameralib.R

private val PERMISSIONS_REQUIRED = arrayOf(Manifest.permission.CAMERA)

class PermissionsFragment : Fragment() {

    var deniedCount = 0

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                navigateToCamera()
            } else {
                deniedCount++
                if (deniedCount > 2) {
                    Toast.makeText(
                        requireActivity(),
                        "Permanently permission denied.Open camera again.",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    activity?.finish()
                }
            }
        }

    private val launcher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (!hasPermissions(requireActivity())) {
                permanentPermissionRationale()
            } else {
                navigateToCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermissions(requireActivity())) {
            checkCameraPermission()
        } else {
            navigateToCamera()
        }
    }

    private fun navigateToCamera() {
        lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
                PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment()
            )
        }
    }

    private fun checkCameraPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                requireActivity(),
                PERMISSIONS_REQUIRED[0]
            ) -> {
                navigateToCamera()
            }
            else -> {
                requestPermission()
            }
        }
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(
            PERMISSIONS_REQUIRED[0]
        )
    }

    private fun permanentPermissionRationale() {
        AlertDialog.Builder(requireActivity())
            .setTitle("Permission Denied")
            .setMessage("Camera permission required to take picture.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts(
                    "package",
                    requireActivity().packageName,
                    PermissionsFragment::class.java.canonicalName
                )
                intent.data = uri
                requestPermission()
            }
            .show()
    }

    companion object {
        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

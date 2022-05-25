package com.evolve.cameralib.ui

import android.Manifest
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.evolve.cameralib.R
import com.evolve.cameralib.utils.showToast
import com.permissionx.guolindev.PermissionX


class PermissionsFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PermissionX.init(activity)
            .permissions(
                Manifest.permission.CAMERA
            )
            .explainReasonBeforeRequest()
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Given permission is needed to take picture",
                    "OK",
                    "Cancel"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "You need to allow given permission in Settings youself",
                    "OK",
                    "Cancel"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    navigateToCamera()
                } else {
                    this.showToast("Camera permission denied")
                    requireActivity().finish()
                }
            }
    }

    private fun navigateToCamera() {
//        lifecycleScope.launchWhenStarted {
//            Navigation.findNavController(requireActivity(), R.id.fragment_container).navigate(
//                PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment()
//            )
//        }
    }
}

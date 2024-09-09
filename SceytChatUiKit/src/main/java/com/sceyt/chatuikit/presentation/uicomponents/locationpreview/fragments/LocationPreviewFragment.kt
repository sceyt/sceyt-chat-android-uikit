package com.sceyt.chatuikit.presentation.uicomponents.locationpreview.fragments

import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytFragmentLocationPreviewBinding
import com.sceyt.chatuikit.extensions.checkAndAskPermissions
import com.sceyt.chatuikit.extensions.gerPermissionsForLocation
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.initPermissionLauncher
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.presentation.uicomponents.locationpreview.LocationResult
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

open class LocationPreviewFragment : Fragment() {
    protected var binding: SceytFragmentLocationPreviewBinding? = null
    private var requestLocationPermissionLauncher = initPermissionLauncher {
        setupCurrentLocation()
    }
    private var locationListener: LocationListener? = null
    private var googleMap: GoogleMap? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentLocationPreviewBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.applyStyle()
        initViews()
        requestLocationPermission()
    }

    fun setupLocationListener(locationListener: LocationListener) {
        this.locationListener = locationListener
    }

    private fun requestLocationPermission() {
        val permissions = gerPermissionsForLocation()
        if (requireContext().checkAndAskPermissions(requestLocationPermissionLauncher, *permissions)) {
            setupCurrentLocation()
        }
    }

    private fun setupCurrentLocation() {

    }

    private fun initViews() {
        binding?.toolbar?.setNavigationIconClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding?.btnShareLocation?.setOnClickListener {
            onShareClicked()
        }
        initMapView()
    }

    private fun initMapView() {
        binding?.mapView?.apply {
            onCreate(null);
            onStart();
            onResume();
            getMapAsync { googleMap ->
                this@LocationPreviewFragment.googleMap = googleMap
            }
        }
    }

    private fun onShareClicked() {
        val latLng = googleMap?.cameraPosition?.target ?: LatLng(0.0, 0.0)
        binding?.mapView?.let { mapView ->
            mapView.getMapAsync { googleMap ->
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13f))
                googleMap.addMarker(MarkerOptions().position(latLng))
                googleMap.isBuildingsEnabled = true
                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                googleMap.uiSettings.setAllGesturesEnabled(false)
                googleMap.setOnMapLoadedCallback {
                    googleMap.snapshot { bitmap ->
                        if (bitmap != null) {
                            val tmpFile = extractMapSnapshot(bitmap)
                            locationListener?.onShare(LocationResult(
                                snapshotPath = tmpFile.absolutePath,
                                latitude = latLng.latitude,
                                longitude = latLng.longitude
                            ))
                        }
                        mapView.visibility = View.GONE
                        mapView.onPause()
                        mapView.onStop()
                        mapView.onDestroy()
                    }
                }
            }
        }
    }

    private fun extractMapSnapshot(bitmap: Bitmap): File {
        val tempFile = File.createTempFile(UUID.randomUUID().toString(), ".png")
        val fos = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 1, fos)
        fos.flush()
        fos.close()
        MediaScannerConnection.scanFile(requireContext(), arrayOf(tempFile.absolutePath), null, null)
        return tempFile
    }

    private fun SceytFragmentLocationPreviewBinding.applyStyle() {
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.primaryColor))
        tvLocation.setTextColor(requireContext().getCompatColor(SceytChatUIKit.theme.textPrimaryColor))
        btnShareLocation.setBackgroundTint(requireContext().getCompatColor(SceytChatUIKit.theme.accentColor))
        btnShareLocation.setTextColor(requireContext().getCompatColor(SceytChatUIKit.theme.onPrimaryColor))
    }

    fun interface LocationListener {
        fun onShare(locationResult: LocationResult)
    }

}
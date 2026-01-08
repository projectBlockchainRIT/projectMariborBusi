package com.example.projektna.ui.settings

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.projektna.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Dialog za izbiro lokacije za simulacijo trka.
 * Omogoča izbiro na zemljevidu, vnos naslova (ulica, kraj), ali uporabo trenutne lokacije.
 */
class LocationPickerDialog : DialogFragment() {

    companion object {
        private const val TAG = "LocationPickerDialog"
        const val REQUEST_KEY = "location_picker_result"
        const val RESULT_LATITUDE = "latitude"
        const val RESULT_LONGITUDE = "longitude"
        const val RESULT_ADDRESS = "address"

        // Default center - Maribor
        private const val DEFAULT_LAT = 46.5547
        private const val DEFAULT_LON = 15.6459
        private const val DEFAULT_ZOOM = 14.0

        fun newInstance(): LocationPickerDialog {
            return LocationPickerDialog()
        }
    }

    // Selected location
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null
    private var selectedAddress: String? = null

    // Views
    private lateinit var mapView: MapView
    private lateinit var btnUseMapLocation: MaterialButton
    private lateinit var inputStreet: TextInputEditText
    private lateinit var inputCity: TextInputEditText
    private lateinit var btnSearchAddress: MaterialButton
    private lateinit var btnCurrentLocation: MaterialButton
    private lateinit var cardSelectedLocation: MaterialCardView
    private lateinit var textSelectedAddress: TextView
    private lateinit var textSelectedCoordinates: TextView
    private lateinit var textError: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnConfirm: MaterialButton

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(createView())
            .create()

        // Make dialog wider
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.95).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        return dialog
    }

    private fun createView(): View {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_location_picker, null)
        setupViews(view)
        setupMap()
        setupListeners()
        return view
    }

    private fun setupViews(view: View) {
        mapView = view.findViewById(R.id.map_view)
        btnUseMapLocation = view.findViewById(R.id.btn_use_map_location)
        inputStreet = view.findViewById(R.id.input_street)
        inputCity = view.findViewById(R.id.input_city)
        btnSearchAddress = view.findViewById(R.id.btn_search_address)
        btnCurrentLocation = view.findViewById(R.id.btn_current_location)
        cardSelectedLocation = view.findViewById(R.id.card_selected_location)
        textSelectedAddress = view.findViewById(R.id.text_selected_address)
        textSelectedCoordinates = view.findViewById(R.id.text_selected_coordinates)
        textError = view.findViewById(R.id.text_error)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnConfirm = view.findViewById(R.id.btn_confirm)

        // Set default city to Maribor
        inputCity.setText("Maribor")
    }

    private fun setupMap() {
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) { style ->
            Log.d(TAG, "Map style loaded")
        }

        // Set initial camera to Maribor
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(DEFAULT_LON, DEFAULT_LAT))
                .zoom(DEFAULT_ZOOM)
                .build()
        )

        // Prevent ScrollView from intercepting touch events on the map
        mapView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    // Disable parent scroll
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Re-enable parent scroll
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            // Return false to let MapView handle the touch
            false
        }
    }

    private fun setupListeners() {
        btnUseMapLocation.setOnClickListener {
            useMapCenterLocation()
        }

        btnSearchAddress.setOnClickListener {
            searchAddress()
        }

        btnCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }

        btnConfirm.setOnClickListener {
            confirmSelection()
        }
    }

    private fun useMapCenterLocation() {
        val center = mapView.mapboxMap.cameraState.center
        val latitude = center.latitude()
        val longitude = center.longitude()

        btnUseMapLocation.isEnabled = false
        btnUseMapLocation.text = getString(R.string.location_searching)
        hideError()

        lifecycleScope.launch {
            try {
                val address = reverseGeocode(latitude, longitude)
                val addressText = address?.let { formatAddress(it) }
                    ?: getString(R.string.location_unknown_address)
                updateSelectedLocation(latitude, longitude, addressText)
            } catch (e: Exception) {
                Log.e(TAG, "Reverse geocoding error", e)
                updateSelectedLocation(latitude, longitude, getString(R.string.location_unknown_address))
            } finally {
                btnUseMapLocation.isEnabled = true
                btnUseMapLocation.text = getString(R.string.location_use_map_center)
            }
        }
    }

    private fun searchAddress() {
        val street = inputStreet.text?.toString()?.trim() ?: ""
        val city = inputCity.text?.toString()?.trim() ?: ""

        if (street.isEmpty() && city.isEmpty()) {
            showError(getString(R.string.location_error_not_found))
            return
        }

        val addressQuery = buildString {
            if (street.isNotEmpty()) append(street)
            if (street.isNotEmpty() && city.isNotEmpty()) append(", ")
            if (city.isNotEmpty()) append(city)
            append(", Slovenija") // Add country for better accuracy
        }

        btnSearchAddress.isEnabled = false
        btnSearchAddress.text = getString(R.string.location_searching)
        hideError()

        lifecycleScope.launch {
            try {
                val result = geocodeAddress(addressQuery)
                if (result != null) {
                    // Move map to found location
                    mapView.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(result.longitude, result.latitude))
                            .zoom(16.0)
                            .build()
                    )
                    updateSelectedLocation(
                        result.latitude,
                        result.longitude,
                        formatAddress(result)
                    )
                } else {
                    showError(getString(R.string.location_error_not_found))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Geocoding error", e)
                showError(getString(R.string.location_error_geocoding))
            } finally {
                btnSearchAddress.isEnabled = true
                btnSearchAddress.text = getString(R.string.location_search_address)
            }
        }
    }

    private suspend fun geocodeAddress(addressQuery: String): Address? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(requireContext(), Locale("sl", "SI"))

        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(addressQuery, 1) { addresses ->
                    continuation.resume(addresses.firstOrNull())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocationName(addressQuery, 1)?.firstOrNull()
        }
    }

    private suspend fun reverseGeocode(latitude: Double, longitude: Double): Address? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(requireContext(), Locale("sl", "SI"))

        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    continuation.resume(addresses.firstOrNull())
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
        }
    }

    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()

        // Street with number
        address.thoroughfare?.let { street ->
            val streetWithNumber = if (address.subThoroughfare != null) {
                "$street ${address.subThoroughfare}"
            } else {
                street
            }
            parts.add(streetWithNumber)
        }

        // City/locality
        address.locality?.let { parts.add(it) }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            getString(R.string.location_unknown_address)
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showError(getString(R.string.location_permission_denied))
            return
        }

        btnCurrentLocation.isEnabled = false
        btnCurrentLocation.text = getString(R.string.location_searching)
        hideError()

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Move map to current location
                    mapView.mapboxMap.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(location.longitude, location.latitude))
                            .zoom(16.0)
                            .build()
                    )

                    lifecycleScope.launch {
                        try {
                            val address = reverseGeocode(location.latitude, location.longitude)
                            val addressText = address?.let { formatAddress(it) }
                                ?: getString(R.string.location_unknown_address)
                            updateSelectedLocation(location.latitude, location.longitude, addressText)
                        } catch (e: Exception) {
                            Log.e(TAG, "Reverse geocoding error", e)
                            updateSelectedLocation(
                                location.latitude,
                                location.longitude,
                                getString(R.string.location_unknown_address)
                            )
                        }
                    }
                } else {
                    showError(getString(R.string.location_error_no_location))
                }
                btnCurrentLocation.isEnabled = true
                btnCurrentLocation.text = getString(R.string.location_use_current)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get current location", e)
                showError(getString(R.string.location_error_no_location))
                btnCurrentLocation.isEnabled = true
                btnCurrentLocation.text = getString(R.string.location_use_current)
            }
    }

    private fun updateSelectedLocation(latitude: Double, longitude: Double, address: String) {
        selectedLatitude = latitude
        selectedLongitude = longitude
        selectedAddress = address

        cardSelectedLocation.visibility = View.VISIBLE
        textSelectedAddress.text = address
        textSelectedCoordinates.text = "%.5f, %.5f".format(latitude, longitude)

        btnConfirm.isEnabled = true
        hideError()
    }

    private fun showError(message: String) {
        textError.text = message
        textError.visibility = View.VISIBLE
    }

    private fun hideError() {
        textError.visibility = View.GONE
    }

    private fun confirmSelection() {
        val lat = selectedLatitude
        val lon = selectedLongitude
        val addr = selectedAddress

        if (lat != null && lon != null) {
            // Send result back to parent fragment
            parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle().apply {
                putDouble(RESULT_LATITUDE, lat)
                putDouble(RESULT_LONGITUDE, lon)
                putString(RESULT_ADDRESS, addr ?: "")
            })
        }
        dismiss()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDestroy()
    }
}

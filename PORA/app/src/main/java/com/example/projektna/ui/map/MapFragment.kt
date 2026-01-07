package com.example.projektna.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projektna.R
import com.example.projektna.data.api.model.BusRoute
import com.example.projektna.data.api.model.BusStop
import com.example.projektna.databinding.FragmentMapBinding
import com.example.projektna.databinding.BottomSheetStationBinding
import com.example.projektna.util.Constants
import com.example.projektna.util.Resource
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private val viewModel: MapViewModel by viewModels()

    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private val stationAnnotationMap = mutableMapOf<String, BusStop>()
    private var markerBitmap: Bitmap? = null
    private var routeStationMarkerBitmap: Bitmap? = null

    private var routes: List<BusRoute> = emptyList()
    private var selectedRoute: BusRoute? = null
    private var isLinesMode = false

    // Location tracking
    private var userLocation: Point? = null
    private var locationEnabled = false
    private var showingNearbyOnly = false
    private var pendingLocationPrompt = false

    // Permission launcher for location
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            pendingLocationPrompt = true
            enableLocationComponent()
        } else {
            Toast.makeText(context, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = binding.mapView
        initializeMap()
        setupModeToggle()
        setupLineSelector()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeMap() {
        mapView.mapboxMap.loadStyle(Style.STANDARD) { style ->
            // Create marker bitmaps
            markerBitmap = createMarkerBitmap()
            routeStationMarkerBitmap = createRouteStationMarkerBitmap()

            // Setup annotation managers
            setupAnnotationManagers()

            // Center on Maribor
            val cameraOptions = CameraOptions.Builder()
                .center(Point.fromLngLat(Constants.MARIBOR_LONGITUDE, Constants.MARIBOR_LATITUDE))
                .zoom(Constants.DEFAULT_ZOOM)
                .pitch(Constants.DEFAULT_PITCH)
                .bearing(0.0)
                .build()

            mapView.mapboxMap.setCamera(cameraOptions)

            // Load bus stops (default mode)
            viewModel.loadBusStops()
            // Also load routes for when user switches to lines mode
            viewModel.loadRoutes()
        }
    }

    private fun setupAnnotationManagers() {
        val annotationPlugin = mapView.annotations

        // Point annotations for station markers
        pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
        pointAnnotationManager?.addClickListener { annotation ->
            val stop = stationAnnotationMap[annotation.id]
            if (stop != null) {
                showStationBottomSheet(stop)
            }
            true
        }

        // Polyline annotations for route lines
        polylineAnnotationManager = annotationPlugin.createPolylineAnnotationManager()
    }

    private fun setupModeToggle() {
        binding.chipGroupMode.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipStations) -> {
                    isLinesMode = false
                    switchToStationsMode()
                }
                checkedIds.contains(R.id.chipLines) -> {
                    isLinesMode = true
                    switchToLinesMode()
                }
            }
        }
    }

    private fun switchToStationsMode() {
        // Hide line selector
        binding.textInputLayoutLine.visibility = View.GONE
        binding.autoCompleteLine.setText("", false)

        // Clear route polyline
        polylineAnnotationManager?.deleteAll()
        selectedRoute = null

        // Reset nearby stations state
        showingNearbyOnly = false
        binding.buttonShowAll.visibility = View.GONE

        // Show station markers
        viewModel.loadBusStops()
    }

    private fun switchToLinesMode() {
        // Show line selector
        binding.textInputLayoutLine.visibility = View.VISIBLE

        // Hide "Show all" button
        binding.buttonShowAll.visibility = View.GONE
        showingNearbyOnly = false

        // Hide station markers
        pointAnnotationManager?.deleteAll()
        stationAnnotationMap.clear()
    }

    private fun setupLineSelector() {
        binding.autoCompleteLine.setOnItemClickListener { _, _, position, _ ->
            val route = routes.getOrNull(position)
            if (route != null) {
                selectedRoute = route
                viewModel.loadRouteDetails(route.lineId)
            }
        }
    }

    private fun setupRoutesDropdown() {
        val routeNames = routes.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            routeNames
        )
        binding.autoCompleteLine.setAdapter(adapter)
    }

    private fun createMarkerBitmap(): Bitmap {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bus_stop)!!
        val size = (40 * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a circle background
        val paint = android.graphics.Paint().apply {
            color = ContextCompat.getColor(requireContext(), R.color.primary)
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw the icon centered
        val iconPadding = (size * 0.25).toInt()
        drawable.setBounds(iconPadding, iconPadding, size - iconPadding, size - iconPadding)
        drawable.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        drawable.draw(canvas)

        return bitmap
    }

    private fun createRouteStationMarkerBitmap(): Bitmap {
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_bus_stop)!!
        val size = (32 * resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw a circle background with secondary color
        val paint = android.graphics.Paint().apply {
            color = ContextCompat.getColor(requireContext(), R.color.secondary)
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Draw the icon centered
        val iconPadding = (size * 0.25).toInt()
        drawable.setBounds(iconPadding, iconPadding, size - iconPadding, size - iconPadding)
        drawable.setTint(ContextCompat.getColor(requireContext(), R.color.white))
        drawable.draw(canvas)

        return bitmap
    }

    private fun addStationMarkers(stops: List<BusStop>) {
        val manager = pointAnnotationManager ?: return
        val bitmap = markerBitmap ?: return

        // Clear existing markers
        manager.deleteAll()
        stationAnnotationMap.clear()

        // Add markers for each station
        val annotationOptions = stops.map { stop ->
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(stop.longitude, stop.latitude))
                .withIconImage(bitmap)
                .withIconSize(1.0)
        }

        val annotations = manager.create(annotationOptions)

        // Map annotations to stops
        annotations.forEachIndexed { index, annotation ->
            stationAnnotationMap[annotation.id] = stops[index]
        }
    }

    private fun addRouteStationMarkers(stops: List<BusStop>) {
        val manager = pointAnnotationManager ?: return
        val bitmap = routeStationMarkerBitmap ?: return

        // Clear existing station markers from previous line selection
        manager.deleteAll()
        stationAnnotationMap.clear()

        // Add markers for each station on the route
        val annotationOptions = stops.map { stop ->
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(stop.longitude, stop.latitude))
                .withIconImage(bitmap)
                .withIconSize(1.0)
        }

        val annotations = manager.create(annotationOptions)

        // Map annotations to stops for click handling
        annotations.forEachIndexed { index, annotation ->
            stationAnnotationMap[annotation.id] = stops[index]
        }
    }

    private fun drawRouteLine(route: BusRoute) {
        val manager = polylineAnnotationManager ?: return

        // Clear existing polylines
        manager.deleteAll()

        // Convert path coordinates to Points
        // The path is List<List<Double>> where each inner list is [latitude, longitude]
        // Mapbox expects [longitude, latitude], so we swap them
        val points = route.path.mapNotNull { coord ->
            if (coord.size >= 2) {
                Point.fromLngLat(coord[1], coord[0]) // swap: lng=coord[1], lat=coord[0]
            } else null
        }

        if (points.isEmpty()) {
            Toast.makeText(context, getString(R.string.no_route_data), Toast.LENGTH_SHORT).show()
            return
        }

        // Create polyline
        val polylineOptions = PolylineAnnotationOptions()
            .withPoints(points)
            .withLineColor(ContextCompat.getColor(requireContext(), R.color.primary))
            .withLineWidth(5.0)

        manager.create(polylineOptions)

        // Fit camera to show entire route
        fitCameraToRoute(points)
    }

    private fun fitCameraToRoute(points: List<Point>) {
        if (points.isEmpty()) return

        // Calculate bounds
        var minLat = Double.MAX_VALUE
        var maxLat = Double.MIN_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = Double.MIN_VALUE

        points.forEach { point ->
            minLat = minOf(minLat, point.latitude())
            maxLat = maxOf(maxLat, point.latitude())
            minLng = minOf(minLng, point.longitude())
            maxLng = maxOf(maxLng, point.longitude())
        }

        // Calculate center
        val centerLat = (minLat + maxLat) / 2
        val centerLng = (minLng + maxLng) / 2

        // Calculate appropriate zoom level based on bounds
        val latDiff = maxLat - minLat
        val lngDiff = maxLng - minLng
        val maxDiff = maxOf(latDiff, lngDiff)

        val zoom = when {
            maxDiff > 0.1 -> 11.0
            maxDiff > 0.05 -> 12.0
            maxDiff > 0.02 -> 13.0
            maxDiff > 0.01 -> 14.0
            else -> 15.0
        }

        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(centerLng, centerLat))
            .zoom(zoom)
            .pitch(0.0)
            .build()

        mapView.mapboxMap.flyTo(cameraOptions)
    }

    private fun showStationBottomSheet(stop: BusStop) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetStationBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        // Set station info
        sheetBinding.textViewStationName.text = stop.name
        sheetBinding.textViewStationNumber.text = stop.number?.let {
            getString(R.string.station_number_format, it)
        } ?: ""

        // Setup departures RecyclerView
        val departuresAdapter = DeparturesAdapter()
        sheetBinding.recyclerViewDepartures.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = departuresAdapter
        }

        // Load station details with departures
        sheetBinding.progressBarDepartures.visibility = View.VISIBLE
        viewModel.loadStopDetails(stop.id)

        // Store departures for delay dialog
        var currentDepartures: List<com.example.projektna.data.api.model.DepartureGroup> = emptyList()

        // Observe station metadata
        viewModel.stopMetadata.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    sheetBinding.progressBarDepartures.visibility = View.VISIBLE
                    sheetBinding.recyclerViewDepartures.visibility = View.GONE
                    sheetBinding.textViewNoDepartures.visibility = View.GONE
                }
                is Resource.Success -> {
                    sheetBinding.progressBarDepartures.visibility = View.GONE
                    val metadata = resource.data
                    if (metadata != null && metadata.departures.isNotEmpty()) {
                        currentDepartures = metadata.departures
                        departuresAdapter.submitList(metadata.departures)
                        sheetBinding.recyclerViewDepartures.visibility = View.VISIBLE
                        sheetBinding.textViewNoDepartures.visibility = View.GONE
                    } else {
                        sheetBinding.recyclerViewDepartures.visibility = View.GONE
                        sheetBinding.textViewNoDepartures.visibility = View.VISIBLE
                    }
                }
                is Resource.Error -> {
                    sheetBinding.progressBarDepartures.visibility = View.GONE
                    sheetBinding.recyclerViewDepartures.visibility = View.GONE
                    sheetBinding.textViewNoDepartures.visibility = View.VISIBLE
                }
            }
        }

        // Favorite button click
        sheetBinding.buttonFavorite.setOnClickListener {
            Toast.makeText(context, getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show()
        }

        // Report delay button click
        sheetBinding.buttonReportDelay.setOnClickListener {
            bottomSheetDialog.dismiss()
            showDelayInputDialog(stop, currentDepartures)
        }

        bottomSheetDialog.show()
    }

    private fun showDelayInputDialog(stop: BusStop, departures: List<com.example.projektna.data.api.model.DepartureGroup>) {
        val dialog = DelayInputDialog.newInstance(
            busStop = stop,
            availableLines = departures,
            userLatitude = userLocation?.latitude(),
            userLongitude = userLocation?.longitude()
        )
        dialog.show(childFragmentManager, DelayInputDialog.TAG)
    }

    private fun setupClickListeners() {
        binding.fabSearch.setOnClickListener {
            searchNearbyStations()
        }

        binding.fabMyLocation.setOnClickListener {
            handleLocationButtonClick()
        }

        binding.buttonShowAll.setOnClickListener {
            showAllStations()
        }
    }

    private fun handleLocationButtonClick() {
        if (hasLocationPermission()) {
            if (!locationEnabled) {
                pendingLocationPrompt = true
                enableLocationComponent()
            } else {
                showNearbyStationsPrompt()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun showNearbyStationsPrompt() {
        flyToUserLocation()

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.nearby_stations_title))
            .setMessage(getString(R.string.nearby_stations_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                showNearbyStationsOnly()
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun showNearbyStationsOnly() {
        val location = userLocation
        if (location != null) {
            // Switch to stations mode if in lines mode
            if (isLinesMode) {
                binding.chipStations.isChecked = true
            }
            showingNearbyOnly = true
            binding.buttonShowAll.visibility = View.VISIBLE
            // Use 300m radius for nearby stations
            viewModel.loadNearbyStops(location.latitude(), location.longitude(), 300)
        } else {
            Toast.makeText(context, getString(R.string.location_not_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAllStations() {
        showingNearbyOnly = false
        binding.buttonShowAll.visibility = View.GONE
        viewModel.loadBusStops()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableLocationComponent() {
        val locationComponentPlugin = mapView.location
        locationComponentPlugin.updateSettings {
            enabled = true
            pulsingEnabled = true
        }

        locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
            userLocation = point
            if (!locationEnabled) {
                locationEnabled = true
                if (pendingLocationPrompt) {
                    pendingLocationPrompt = false
                    showNearbyStationsPrompt()
                } else {
                    flyToUserLocation()
                }
            }
        }
    }

    private fun flyToUserLocation() {
        val location = userLocation
        if (location != null) {
            val cameraOptions = CameraOptions.Builder()
                .center(location)
                .zoom(15.0)
                .pitch(0.0)
                .build()
            mapView.mapboxMap.flyTo(cameraOptions)
        } else {
            Toast.makeText(context, getString(R.string.location_not_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun searchNearbyStations() {
        val location = userLocation
        if (location != null) {
            showNearbyStationsOnly()
            Toast.makeText(context, getString(R.string.searching_nearby_stations), Toast.LENGTH_SHORT).show()
        } else {
            if (!hasLocationPermission()) {
                requestLocationPermission()
            } else if (!locationEnabled) {
                enableLocationComponent()
                Toast.makeText(context, getString(R.string.enable_location_first), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.location_not_available), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.busStops.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Could show loading indicator
                }
                is Resource.Success -> {
                    if (!isLinesMode) {
                        resource.data?.let { stops ->
                            if (stops.isEmpty() && showingNearbyOnly) {
                                Toast.makeText(context, getString(R.string.no_nearby_stations), Toast.LENGTH_SHORT).show()
                                // Reset to show all stations
                                showAllStations()
                            } else {
                                addStationMarkers(stops)
                            }
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                    if (showingNearbyOnly) {
                        // Reset on error
                        showAllStations()
                    }
                }
            }
        }

        viewModel.routes.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    routes = resource.data ?: emptyList()
                    setupRoutesDropdown()
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.selectedRoute.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.cardLoading.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.cardLoading.visibility = View.GONE
                    resource.data?.let { route ->
                        if (route.path.isNotEmpty()) {
                            drawRouteLine(route)
                        }
                    }
                }
                is Resource.Error -> {
                    binding.cardLoading.visibility = View.GONE
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewModel.routeStations.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Loading indicator already shown by selectedRoute observer
                }
                is Resource.Success -> {
                    resource.data?.let { stops ->
                        if (stops.isNotEmpty() && isLinesMode) {
                            addRouteStationMarkers(stops)
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
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
        markerBitmap?.recycle()
        markerBitmap = null
        routeStationMarkerBitmap?.recycle()
        routeStationMarkerBitmap = null
        mapView.onDestroy()
        _binding = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}

package com.example.projektna.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projektna.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder

class MapFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var editSearch: EditText
    private lateinit var buttonClearSearch: ImageButton
    private lateinit var cardSearchResults: MaterialCardView
    private lateinit var recyclerSearchResults: RecyclerView
    private lateinit var cardSelectedLocation: MaterialCardView
    private lateinit var textLocationName: TextView
    private lateinit var textSelectedLocation: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val httpClient = OkHttpClient()
    private var searchJob: Job? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var selectedPoint: Point? = null
    private var selectedLocationName: String? = null

    private val searchAdapter = SearchResultAdapter { result ->
        onSearchResultSelected(result)
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            enableLocationComponent()
            goToMyLocation()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.map_location_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        initViews(view)
        setupMap()
        setupSearch()
        setupListeners()
    }

    private fun initViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        fabMyLocation = view.findViewById(R.id.fab_my_location)
        editSearch = view.findViewById(R.id.edit_search)
        buttonClearSearch = view.findViewById(R.id.button_clear_search)
        cardSearchResults = view.findViewById(R.id.card_search_results)
        recyclerSearchResults = view.findViewById(R.id.recycler_search_results)
        cardSelectedLocation = view.findViewById(R.id.card_selected_location)
        textLocationName = view.findViewById(R.id.text_location_name)
        textSelectedLocation = view.findViewById(R.id.text_selected_location)

        recyclerSearchResults.layoutManager = LinearLayoutManager(requireContext())
        recyclerSearchResults.adapter = searchAdapter
    }

    private fun setupMap() {
        mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
            setupAnnotationManager()
            setupMapClickListener()

            // Set initial camera to Slovenia (Ljubljana)
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(14.5058, 46.0569))
                    .zoom(10.0)
                    .build()
            )

            if (hasLocationPermission()) {
                enableLocationComponent()
            }
        }
    }

    private fun setupAnnotationManager() {
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
    }

    private fun setupMapClickListener() {
        mapView.mapboxMap.addOnMapClickListener { point ->
            selectLocation(point, null)
            hideSearchResults()
            hideKeyboard()
            true
        }
    }

    private fun setupSearch() {
        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                buttonClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query.length >= 3) {
                    performSearch(query)
                } else {
                    hideSearchResults()
                }
            }
        })

        editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = editSearch.text.toString()
                if (query.isNotEmpty()) {
                    performSearch(query)
                }
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun setupListeners() {
        fabMyLocation.setOnClickListener {
            if (hasLocationPermission()) {
                goToMyLocation()
            } else {
                requestLocationPermission()
            }
        }

        buttonClearSearch.setOnClickListener {
            editSearch.text.clear()
            hideSearchResults()
            hideKeyboard()
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()

        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val results = searchNominatim(query)
                if (results.isNotEmpty()) {
                    searchAdapter.submitList(results)
                    cardSearchResults.visibility = View.VISIBLE
                } else {
                    hideSearchResults()
                }
            } catch (e: Exception) {
                hideSearchResults()
            }
        }
    }

    private suspend fun searchNominatim(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5&addressdetails=1"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "M-Busi Android App")
            .build()

        val response = httpClient.newCall(request).execute()
        val results = mutableListOf<SearchResult>()

        if (response.isSuccessful) {
            val jsonArray = JSONArray(response.body?.string() ?: "[]")
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.optString("display_name", "")
                val lat = obj.optString("lat", "0").toDoubleOrNull() ?: 0.0
                val lon = obj.optString("lon", "0").toDoubleOrNull() ?: 0.0

                // Get shorter name from address details
                val address = obj.optJSONObject("address")
                val shortName = address?.let {
                    it.optString("city", "")
                        .ifEmpty { it.optString("town", "") }
                        .ifEmpty { it.optString("village", "") }
                        .ifEmpty { it.optString("municipality", "") }
                        .ifEmpty { it.optString("county", "") }
                        .ifEmpty { name.split(",").firstOrNull() ?: name }
                } ?: name.split(",").firstOrNull() ?: name

                results.add(SearchResult(shortName, name, lat, lon))
            }
        }

        results
    }

    private fun onSearchResultSelected(result: SearchResult) {
        hideSearchResults()
        hideKeyboard()
        editSearch.setText(result.name)
        editSearch.clearFocus()

        val point = Point.fromLngLat(result.lon, result.lat)
        selectLocation(point, result.name)
        flyToLocation(point)
    }

    private fun selectLocation(point: Point, name: String?) {
        selectedPoint = point
        selectedLocationName = name

        // Clear previous markers
        pointAnnotationManager?.deleteAll()

        // Add marker at selected location
        val markerBitmap = getBitmapFromDrawable(R.drawable.ic_location)
        markerBitmap?.let { bitmap ->
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(1.5)

            pointAnnotationManager?.create(pointAnnotationOptions)
        }

        // Update UI
        val lat = point.latitude()
        val lng = point.longitude()
        textSelectedLocation.text = String.format("%.5f, %.5f", lat, lng)
        textLocationName.text = name ?: getString(R.string.map_selected_location)
        cardSelectedLocation.visibility = View.VISIBLE
    }

    private fun flyToLocation(point: Point) {
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build()
        )
    }

    private fun hideSearchResults() {
        cardSearchResults.visibility = View.GONE
        searchAdapter.submitList(emptyList())
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(editSearch.windowToken, 0)
    }

    private fun getBitmapFromDrawable(drawableId: Int): Bitmap? {
        val drawable: Drawable? = ResourcesCompat.getDrawable(resources, drawableId, null)
        return drawable?.let { drawableToBitmap(it) }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
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
        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
    }

    @SuppressLint("MissingPermission")
    private fun goToMyLocation() {
        mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val point = Point.fromLngLat(it.longitude, it.latitude)
                flyToLocation(point)
                selectLocation(point, getString(R.string.map_my_location))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
    }

    fun getSelectedLocation(): Point? = selectedPoint
    fun getSelectedLocationName(): String? = selectedLocationName

    // Data class for search results
    data class SearchResult(
        val name: String,
        val fullAddress: String,
        val lat: Double,
        val lon: Double
    )

    // Search Result Adapter
    inner class SearchResultAdapter(
        private val onItemClick: (SearchResult) -> Unit
    ) : RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

        private var items: List<SearchResult> = emptyList()

        fun submitList(newItems: List<SearchResult>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textName: TextView = itemView.findViewById(R.id.text_name)
            private val textAddress: TextView = itemView.findViewById(R.id.text_address)

            fun bind(result: SearchResult) {
                textName.text = result.name
                textAddress.text = result.fullAddress

                itemView.setOnClickListener {
                    onItemClick(result)
                }
            }
        }
    }
}

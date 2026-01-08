package com.example.projektna.ui.settings

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.projektna.R
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.SimulationSensorType
import com.example.projektna.data.api.model.BusRoute
import com.example.projektna.data.repository.BusRouteRepository
import com.example.projektna.data.repository.SimulationRepository
import com.example.projektna.services.SimulationService
import com.example.projektna.util.Resource
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import org.json.JSONArray

class SettingsFragment : Fragment() {

    companion object {
        private const val TAG = "SettingsFragment"
    }

    private lateinit var preferencesManager: PreferencesManager
    private val busRouteRepository = BusRouteRepository()
    private val simulationRepository = SimulationRepository()
    private var availableRoutes: List<BusRoute> = emptyList()

    // Account views
    private lateinit var textUserUsername: TextView
    private lateinit var textUserEmail: TextView
    private lateinit var btnLogout: MaterialButton

    // Collapsible section views
    private lateinit var headerFrequency: LinearLayout
    private lateinit var contentFrequency: LinearLayout
    private lateinit var iconFrequencyExpand: ImageView

    // MQTT views
    private lateinit var switchMqtt: MaterialSwitch
    private lateinit var textMqttStatus: TextView
    private lateinit var layoutMqttSettings: LinearLayout
    private lateinit var inputBrokerUrl: TextInputEditText
    private lateinit var inputMqttUsername: TextInputEditText
    private lateinit var inputMqttPassword: TextInputEditText

    // Existing views
    private lateinit var sliderGpsInterval: Slider
    private lateinit var sliderAccelerometerInterval: Slider
    private lateinit var textGpsIntervalValue: TextView
    private lateinit var textAccelerometerIntervalValue: TextView

    // Simulation views
    private lateinit var switchSimulation: MaterialSwitch
    private lateinit var textSimulationStatus: TextView
    private lateinit var layoutSimulationSettings: LinearLayout
    private lateinit var dropdownSensorType: AutoCompleteTextView
    private lateinit var sliderSimulationInterval: Slider
    private lateinit var textSimulationIntervalValue: TextView
    private lateinit var layoutGpsRange: LinearLayout
    private lateinit var inputLatMin: TextInputEditText
    private lateinit var inputLatMax: TextInputEditText
    private lateinit var inputLonMin: TextInputEditText
    private lateinit var inputLonMax: TextInputEditText
    private lateinit var layoutAccelRange: LinearLayout
    private lateinit var inputAccelMin: TextInputEditText
    private lateinit var inputAccelMax: TextInputEditText
    private lateinit var checkboxManualLocation: MaterialCheckBox
    private lateinit var layoutManualLocation: LinearLayout
    private lateinit var inputManualLat: TextInputEditText
    private lateinit var inputManualLon: TextInputEditText
    private lateinit var btnPickOnMap: MaterialButton
    private lateinit var btnStartSimulation: MaterialButton
    private lateinit var btnSimulateCollision: MaterialButton
    private lateinit var layoutFollowRoute: LinearLayout

    // Route following views
    private lateinit var checkboxFollowRoute: MaterialCheckBox
    private lateinit var layoutRouteSelection: LinearLayout
    private lateinit var dropdownRoute: AutoCompleteTextView
    private lateinit var cardCurrentPosition: MaterialCardView
    private lateinit var textCurrentCoordinates: TextView
    private lateinit var textRouteProgress: TextView

    private val sensorTypes = listOf(
        SimulationSensorType.GPS,
        SimulationSensorType.ACCELEROMETER
    )

    private val simulationStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val success = it.getBooleanExtra(SimulationService.EXTRA_SUCCESS, false)
                val message = it.getStringExtra(SimulationService.EXTRA_MESSAGE)
                if (!success && message != null) {
                    view?.let { v ->
                        Snackbar.make(v, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private val simulationDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val latitude = it.getDoubleExtra(SimulationService.EXTRA_LATITUDE, 0.0)
                val longitude = it.getDoubleExtra(SimulationService.EXTRA_LONGITUDE, 0.0)
                val pathIndex = it.getIntExtra(SimulationService.EXTRA_PATH_INDEX, -1)
                val pathTotal = it.getIntExtra(SimulationService.EXTRA_PATH_TOTAL, -1)

                updateCurrentPositionDisplay(latitude, longitude, pathIndex, pathTotal)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        setupViews(view)
        loadSavedValues()
        setupListeners()
        updateSimulationUI()

        // Register for location picker result (on childFragmentManager since dialog is shown there)
        childFragmentManager.setFragmentResultListener(LocationPickerDialog.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            val latitude = bundle.getDouble(LocationPickerDialog.RESULT_LATITUDE)
            val longitude = bundle.getDouble(LocationPickerDialog.RESULT_LONGITUDE)
            val address = bundle.getString(LocationPickerDialog.RESULT_ADDRESS) ?: ""
            simulateCollisionAtLocation(latitude, longitude, address)
        }
    }

    override fun onResume() {
        super.onResume()
        val statusFilter = IntentFilter(SimulationService.ACTION_SIMULATION_STATUS)
        val dataFilter = IntentFilter(SimulationService.ACTION_SIMULATION_DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(simulationStatusReceiver, statusFilter, Context.RECEIVER_NOT_EXPORTED)
            requireContext().registerReceiver(simulationDataReceiver, dataFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(simulationStatusReceiver, statusFilter)
            requireContext().registerReceiver(simulationDataReceiver, dataFilter)
        }
        updateSimulationButtonState()
    }

    override fun onPause() {
        super.onPause()
        try {
            requireContext().unregisterReceiver(simulationStatusReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        try {
            requireContext().unregisterReceiver(simulationDataReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    private fun setupViews(view: View) {
        // Account views
        textUserUsername = view.findViewById(R.id.text_user_username)
        textUserEmail = view.findViewById(R.id.text_user_email)
        btnLogout = view.findViewById(R.id.btn_logout)

        // Collapsible section views
        headerFrequency = view.findViewById(R.id.header_frequency)
        contentFrequency = view.findViewById(R.id.content_frequency)
        iconFrequencyExpand = view.findViewById(R.id.icon_frequency_expand)

        // MQTT views
        switchMqtt = view.findViewById(R.id.switch_mqtt)
        textMqttStatus = view.findViewById(R.id.text_mqtt_status)
        layoutMqttSettings = view.findViewById(R.id.layout_mqtt_settings)
        inputBrokerUrl = view.findViewById(R.id.input_broker_url)
        inputMqttUsername = view.findViewById(R.id.input_mqtt_username)
        inputMqttPassword = view.findViewById(R.id.input_mqtt_password)

        // Existing views
        sliderGpsInterval = view.findViewById(R.id.slider_gps_interval)
        sliderAccelerometerInterval = view.findViewById(R.id.slider_accelerometer_interval)
        textGpsIntervalValue = view.findViewById(R.id.text_gps_interval_value)
        textAccelerometerIntervalValue = view.findViewById(R.id.text_accelerometer_interval_value)

        // Simulation views
        switchSimulation = view.findViewById(R.id.switch_simulation)
        textSimulationStatus = view.findViewById(R.id.text_simulation_status)
        layoutSimulationSettings = view.findViewById(R.id.layout_simulation_settings)
        dropdownSensorType = view.findViewById(R.id.autocomplete_sensor_type)
        sliderSimulationInterval = view.findViewById(R.id.slider_simulation_interval)
        textSimulationIntervalValue = view.findViewById(R.id.text_simulation_interval_value)
        layoutGpsRange = view.findViewById(R.id.layout_gps_range)
        inputLatMin = view.findViewById(R.id.input_lat_min)
        inputLatMax = view.findViewById(R.id.input_lat_max)
        inputLonMin = view.findViewById(R.id.input_lon_min)
        inputLonMax = view.findViewById(R.id.input_lon_max)
        layoutAccelRange = view.findViewById(R.id.layout_accel_range)
        inputAccelMin = view.findViewById(R.id.input_accel_min)
        inputAccelMax = view.findViewById(R.id.input_accel_max)
        checkboxManualLocation = view.findViewById(R.id.checkbox_manual_location)
        layoutManualLocation = view.findViewById(R.id.layout_manual_location)
        inputManualLat = view.findViewById(R.id.input_manual_lat)
        inputManualLon = view.findViewById(R.id.input_manual_lon)
        btnPickOnMap = view.findViewById(R.id.btn_pick_on_map)
        btnStartSimulation = view.findViewById(R.id.btn_start_simulation)
        btnSimulateCollision = view.findViewById(R.id.btn_simulate_collision)
        layoutFollowRoute = view.findViewById(R.id.layout_follow_route)

        // Route following views
        checkboxFollowRoute = view.findViewById(R.id.checkbox_follow_route)
        layoutRouteSelection = view.findViewById(R.id.layout_route_selection)
        dropdownRoute = view.findViewById(R.id.autocomplete_route)
        cardCurrentPosition = view.findViewById(R.id.card_current_position)
        textCurrentCoordinates = view.findViewById(R.id.text_current_coordinates)
        textRouteProgress = view.findViewById(R.id.text_route_progress)

        // Setup sensor type dropdown
        val sensorTypeLabels = sensorTypes.map { type ->
            when (type) {
                SimulationSensorType.GPS -> getString(R.string.simulation_type_gps)
                SimulationSensorType.ACCELEROMETER -> getString(R.string.simulation_type_accelerometer)
            }
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sensorTypeLabels)
        dropdownSensorType.setAdapter(adapter)
    }

    private fun loadSavedValues() {
        // Account values
        textUserUsername.text = preferencesManager.userUsername ?: getString(R.string.account_no_email)
        textUserEmail.text = preferencesManager.userEmail ?: getString(R.string.account_no_email)

        // MQTT values
        switchMqtt.isChecked = preferencesManager.isMqttEnabled
        inputBrokerUrl.setText(preferencesManager.mqttBrokerUrl)
        inputMqttUsername.setText(preferencesManager.mqttUsername)
        inputMqttPassword.setText(preferencesManager.mqttPassword)
        updateMqttUI()

        // Existing interval values
        val gpsInterval = preferencesManager.gpsIntervalSeconds
        val accelerometerInterval = preferencesManager.accelerometerIntervalSeconds

        sliderGpsInterval.value = gpsInterval.toFloat()
        sliderAccelerometerInterval.value = accelerometerInterval.toFloat()

        updateGpsIntervalText(gpsInterval)
        updateAccelerometerIntervalText(accelerometerInterval)

        // Simulation values
        switchSimulation.isChecked = preferencesManager.isSimulationEnabled

        val currentSensorType = preferencesManager.simulationSensorType
        val sensorTypeLabel = when (currentSensorType) {
            SimulationSensorType.GPS -> getString(R.string.simulation_type_gps)
            SimulationSensorType.ACCELEROMETER -> getString(R.string.simulation_type_accelerometer)
        }
        dropdownSensorType.setText(sensorTypeLabel, false)

        sliderSimulationInterval.value = preferencesManager.simulationIntervalSeconds.toFloat()
        updateSimulationIntervalText(preferencesManager.simulationIntervalSeconds)

        inputLatMin.setText(preferencesManager.simulationGpsLatMin.toString())
        inputLatMax.setText(preferencesManager.simulationGpsLatMax.toString())
        inputLonMin.setText(preferencesManager.simulationGpsLonMin.toString())
        inputLonMax.setText(preferencesManager.simulationGpsLonMax.toString())
        inputAccelMin.setText(preferencesManager.simulationAccelMin.toString())
        inputAccelMax.setText(preferencesManager.simulationAccelMax.toString())

        checkboxManualLocation.isChecked = preferencesManager.useManualLocation
        inputManualLat.setText(preferencesManager.simulationManualLat.toString())
        inputManualLon.setText(preferencesManager.simulationManualLon.toString())

        // Route following values
        checkboxFollowRoute.isChecked = preferencesManager.simulationFollowRoute
        preferencesManager.simulationRouteName?.let { routeName ->
            dropdownRoute.setText(routeName, false)
        }

        // Naloži shranjene koordinate za prikaz
        loadSavedRouteCoordinates()
    }

    private fun loadSavedRouteCoordinates() {
        val pathJson = preferencesManager.simulationRoutePath
        if (pathJson.isNullOrEmpty()) {
            textCurrentCoordinates.text = getString(R.string.simulation_no_route_selected)
            textRouteProgress.visibility = View.GONE
            return
        }

        try {
            val jsonArray = JSONArray(pathJson)
            if (jsonArray.length() > 0) {
                val pathIndex = preferencesManager.simulationPathIndex.coerceIn(0, jsonArray.length() - 1)
                val coordArray = jsonArray.getJSONArray(pathIndex)
                if (coordArray.length() >= 2) {
                    val lat = coordArray.getDouble(0)
                    val lon = coordArray.getDouble(1)
                    updateCurrentPositionDisplay(lat, lon, pathIndex, jsonArray.length())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load saved route coordinates", e)
            textCurrentCoordinates.text = getString(R.string.simulation_no_route_selected)
            textRouteProgress.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        // Logout listener
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Collapsible section listeners
        headerFrequency.setOnClickListener {
            toggleSection(contentFrequency, iconFrequencyExpand)
        }

        // MQTT listeners
        switchMqtt.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.isMqttEnabled = isChecked
            updateMqttUI()
        }

        inputBrokerUrl.doAfterTextChanged { text ->
            preferencesManager.mqttBrokerUrl = text?.toString() ?: PreferencesManager.DEFAULT_MQTT_BROKER_URL
        }

        inputMqttUsername.doAfterTextChanged { text ->
            preferencesManager.mqttUsername = text?.toString() ?: ""
        }

        inputMqttPassword.doAfterTextChanged { text ->
            preferencesManager.mqttPassword = text?.toString() ?: ""
        }

        // Existing listeners
        sliderGpsInterval.addOnChangeListener { _, value, fromUser ->
            val interval = value.toInt()
            updateGpsIntervalText(interval)
            if (fromUser) {
                preferencesManager.gpsIntervalSeconds = interval
            }
        }

        sliderAccelerometerInterval.addOnChangeListener { _, value, fromUser ->
            val interval = value.toInt()
            updateAccelerometerIntervalText(interval)
            if (fromUser) {
                preferencesManager.accelerometerIntervalSeconds = interval
            }
        }

        // Simulation listeners
        switchSimulation.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.isSimulationEnabled = isChecked
            updateSimulationUI()
            if (!isChecked && isSimulationServiceRunning()) {
                stopSimulationService()
            }
        }

        dropdownSensorType.setOnItemClickListener { _, _, position, _ ->
            preferencesManager.simulationSensorType = sensorTypes[position]
            updateSensorTypeUI()
        }

        sliderSimulationInterval.addOnChangeListener { _, value, fromUser ->
            val interval = value.toInt()
            updateSimulationIntervalText(interval)
            if (fromUser) {
                preferencesManager.simulationIntervalSeconds = interval
            }
        }

        // GPS range inputs
        inputLatMin.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationGpsLatMin = it
            }
        }
        inputLatMax.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationGpsLatMax = it
            }
        }
        inputLonMin.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationGpsLonMin = it
            }
        }
        inputLonMax.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationGpsLonMax = it
            }
        }

        // Accelerometer range inputs
        inputAccelMin.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationAccelMin = it
            }
        }
        inputAccelMax.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationAccelMax = it
            }
        }

        // Manual location
        checkboxManualLocation.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.useManualLocation = isChecked
            layoutManualLocation.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        inputManualLat.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationManualLat = it
            }
        }
        inputManualLon.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationManualLon = it
            }
        }

        btnPickOnMap.setOnClickListener {
            // TODO: Navigacija na zemljevid za izbiro lokacije
            view?.let { v ->
                Snackbar.make(v, "Izbira na zemljevidu še ni implementirana", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Route following listeners
        checkboxFollowRoute.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.simulationFollowRoute = isChecked
            updateRouteSelectionUI()
            if (isChecked && availableRoutes.isEmpty()) {
                loadRoutes()
            }
            // Reset route progress when toggling
            if (isChecked) {
                preferencesManager.resetRouteProgress()
            }
        }

        dropdownRoute.setOnItemClickListener { _, _, position, _ ->
            if (position in availableRoutes.indices) {
                val selectedRoute = availableRoutes[position]
                saveSelectedRoute(selectedRoute)
            }
        }

        btnStartSimulation.setOnClickListener {
            if (isSimulationServiceRunning()) {
                stopSimulationService()
            } else {
                startSimulationService()
            }
            updateSimulationButtonState()
        }

        // Collision simulation button - opens location picker dialog
        btnSimulateCollision.setOnClickListener {
            showLocationPickerDialog()
        }
    }

    private fun showLocationPickerDialog() {
        val dialog = LocationPickerDialog.newInstance()
        dialog.show(childFragmentManager, "location_picker")
    }

    private fun updateMqttUI() {
        val isEnabled = switchMqtt.isChecked
        layoutMqttSettings.visibility = if (isEnabled) View.VISIBLE else View.GONE
        textMqttStatus.text = if (isEnabled) {
            getString(R.string.mqtt_enabled)
        } else {
            getString(R.string.mqtt_disabled)
        }
    }

    private fun updateGpsIntervalText(interval: Int) {
        textGpsIntervalValue.text = getString(R.string.settings_interval_seconds, interval)
    }

    private fun updateAccelerometerIntervalText(interval: Int) {
        textAccelerometerIntervalValue.text = getString(R.string.settings_interval_seconds, interval)
    }

    private fun updateSimulationIntervalText(interval: Int) {
        textSimulationIntervalValue.text = getString(R.string.settings_interval_seconds, interval)
    }

    private fun updateSimulationUI() {
        val isEnabled = switchSimulation.isChecked
        layoutSimulationSettings.visibility = if (isEnabled) View.VISIBLE else View.GONE
        textSimulationStatus.text = if (isEnabled) {
            if (isSimulationServiceRunning()) getString(R.string.simulation_running)
            else getString(R.string.simulation_enabled)
        } else {
            getString(R.string.simulation_disabled)
        }

        if (isEnabled) {
            updateSensorTypeUI()
            layoutManualLocation.visibility = if (checkboxManualLocation.isChecked) View.VISIBLE else View.GONE
            updateSimulationButtonState()
        }
    }

    private fun updateSensorTypeUI() {
        val sensorType = preferencesManager.simulationSensorType
        val followRoute = preferencesManager.simulationFollowRoute

        when (sensorType) {
            SimulationSensorType.GPS -> {
                // GPS: pokaži GPS range (razen če sledi liniji), pokaži follow route opcijo
                val showGpsRange = !followRoute
                layoutGpsRange.visibility = if (showGpsRange) View.VISIBLE else View.GONE
                layoutFollowRoute.visibility = View.VISIBLE
                layoutAccelRange.visibility = View.GONE
            }
            SimulationSensorType.ACCELEROMETER -> {
                // ACCELEROMETER: pokaži accel range, skrij GPS range in follow route
                layoutGpsRange.visibility = View.GONE
                layoutFollowRoute.visibility = View.GONE
                layoutAccelRange.visibility = View.VISIBLE
            }
        }

        // Route selection je viden samo za GPS tip in ko je follow route omogočen
        updateRouteSelectionUI()
    }

    private fun updateRouteSelectionUI() {
        val followRoute = preferencesManager.simulationFollowRoute
        val sensorType = preferencesManager.simulationSensorType

        // Route selection je viden samo ko je GPS tip in follow route je omogočen
        val showRouteSelection = followRoute && sensorType == SimulationSensorType.GPS
        layoutRouteSelection.visibility = if (showRouteSelection) View.VISIBLE else View.GONE

        // GPS range je skrit, ko sledimo liniji
        if (sensorType == SimulationSensorType.GPS) {
            layoutGpsRange.visibility = if (followRoute) View.GONE else View.VISIBLE
        }

        // Če sledimo liniji in še nimamo naloženih linij, jih naložimo
        if (showRouteSelection && availableRoutes.isEmpty()) {
            loadRoutes()
        }
    }

    private fun updateSimulationButtonState() {
        val isRunning = isSimulationServiceRunning()
        btnStartSimulation.text = if (isRunning) {
            getString(R.string.simulation_stop)
        } else {
            getString(R.string.simulation_start)
        }
        btnStartSimulation.setIconResource(
            if (isRunning) R.drawable.ic_close else R.drawable.ic_simulation
        )

        textSimulationStatus.text = if (isRunning) {
            getString(R.string.simulation_running)
        } else if (switchSimulation.isChecked) {
            getString(R.string.simulation_enabled)
        } else {
            getString(R.string.simulation_disabled)
        }
    }

    private fun isSimulationServiceRunning(): Boolean {
        val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (SimulationService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun startSimulationService() {
        val intent = Intent(requireContext(), SimulationService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

    private fun stopSimulationService() {
        val intent = Intent(requireContext(), SimulationService::class.java)
        requireContext().stopService(intent)
    }

    private fun toggleSection(content: LinearLayout, icon: ImageView) {
        if (content.visibility == View.VISIBLE) {
            content.visibility = View.GONE
            icon.animate().rotation(0f).setDuration(200).start()
        } else {
            content.visibility = View.VISIBLE
            icon.animate().rotation(180f).setDuration(200).start()
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.logout_button) { _, _ ->
                performLogout()
            }
            .show()
    }

    private fun performLogout() {
        // Ustavi simulacijo, če teče
        if (isSimulationServiceRunning()) {
            stopSimulationService()
        }

        // Počisti auth podatke
        preferencesManager.clearAuthData()

        // Navigiraj na prijavo
        findNavController().navigate(R.id.action_settings_to_login)
    }

    private fun loadRoutes() {
        dropdownRoute.setText(getString(R.string.simulation_loading_routes), false)
        dropdownRoute.isEnabled = false

        lifecycleScope.launch {
            when (val result = busRouteRepository.getAllRoutes()) {
                is Resource.Success -> {
                    availableRoutes = result.data ?: emptyList()
                    val routeNames = availableRoutes.map { it.name }
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        routeNames
                    )
                    dropdownRoute.setAdapter(adapter)
                    dropdownRoute.isEnabled = true

                    // Nastavi prejšnjo izbrano linijo, če obstaja
                    preferencesManager.simulationRouteName?.let { savedName ->
                        if (routeNames.contains(savedName)) {
                            dropdownRoute.setText(savedName, false)
                        } else {
                            dropdownRoute.setText("", false)
                        }
                    } ?: dropdownRoute.setText("", false)

                    Log.d(TAG, "Loaded ${availableRoutes.size} routes")
                }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to load routes: ${result.message}")
                    view?.let { v ->
                        Snackbar.make(v, result.message ?: "Napaka pri nalaganju linij", Snackbar.LENGTH_SHORT).show()
                    }
                    dropdownRoute.setText(getString(R.string.simulation_no_routes), false)
                    dropdownRoute.isEnabled = false
                }
                is Resource.Loading -> {
                    // Already showing loading state
                }
            }
        }
    }

    private fun saveSelectedRoute(route: BusRoute) {
        preferencesManager.simulationRouteId = route.lineId
        preferencesManager.simulationRouteName = route.name

        // Serializiraj path v JSON
        val pathJson = JSONArray().apply {
            route.path.forEach { coord ->
                val coordArray = JSONArray()
                coord.forEach { coordArray.put(it) }
                put(coordArray)
            }
        }.toString()
        preferencesManager.simulationRoutePath = pathJson

        // Reset progress
        preferencesManager.resetRouteProgress()

        Log.d(TAG, "Saved route: ${route.name} with ${route.path.size} coordinates")

        // Posodobi prikaz
        if (route.path.isNotEmpty()) {
            val firstCoord = route.path[0]
            if (firstCoord.size >= 2) {
                updateCurrentPositionDisplay(firstCoord[0], firstCoord[1], 0, route.path.size)
            }
        }
    }

    private fun updateCurrentPositionDisplay(latitude: Double, longitude: Double, pathIndex: Int, pathTotal: Int) {
        textCurrentCoordinates.text = getString(R.string.simulation_position_format, latitude, longitude)

        if (pathIndex >= 0 && pathTotal > 0) {
            textRouteProgress.visibility = View.VISIBLE
            textRouteProgress.text = getString(R.string.simulation_route_progress, pathIndex + 1, pathTotal)
        } else {
            textRouteProgress.visibility = View.GONE
        }
    }

    /**
     * Simulira trčenje in pošlje podatke na strežnik.
     * Generira naključne vrednosti pospeška znotraj nastavljenega razpona
     * z lokacijo izbrano v dialogu.
     */
    private fun simulateCollisionAtLocation(latitude: Double, longitude: Double, address: String) {
        val minMag = preferencesManager.simulationAccelMin
        val maxMag = preferencesManager.simulationAccelMax

        // Generiraj naključno magnitudo
        val magnitude = minMag + (Math.random() * (maxMag - minMag)).toFloat()

        // Generiraj naključne x, y, z komponente, ki dajo to magnitudo
        val theta = Math.random() * Math.PI * 2
        val phi = Math.random() * Math.PI
        val x = (magnitude * Math.sin(phi) * Math.cos(theta)).toFloat()
        val y = (magnitude * Math.sin(phi) * Math.sin(theta)).toFloat()
        val z = (magnitude * Math.cos(phi)).toFloat()

        // Onemogoči gumb med pošiljanjem
        btnSimulateCollision.isEnabled = false
        btnSimulateCollision.text = getString(R.string.simulation_collision_sending)

        Log.d(TAG, "Simulating collision at: $address ($latitude, $longitude)")

        lifecycleScope.launch {
            when (val result = simulationRepository.submitSimulatedCollision(
                magnitude = magnitude,
                x = x,
                y = y,
                z = z,
                latitude = latitude,
                longitude = longitude
            )) {
                is Resource.Success -> {
                    Log.d(TAG, "Collision simulated successfully: magnitude=$magnitude at $address")
                    view?.let { v ->
                        Snackbar.make(v, R.string.simulation_collision_success, Snackbar.LENGTH_SHORT).show()
                    }
                }
                is Resource.Error -> {
                    Log.e(TAG, "Failed to simulate collision: ${result.message}")
                    view?.let { v ->
                        Snackbar.make(
                            v,
                            getString(R.string.simulation_collision_error, result.message),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
                is Resource.Loading -> {
                    // N/A - already showing loading state
                }
            }

            // Ponovno omogoči gumb
            btnSimulateCollision.isEnabled = true
            btnSimulateCollision.text = getString(R.string.simulation_collision_button)
        }
    }
}

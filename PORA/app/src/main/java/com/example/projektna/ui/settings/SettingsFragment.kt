package com.example.projektna.ui.settings

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
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
import androidx.navigation.fragment.findNavController
import com.example.projektna.R
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.SimulationSensorType
import com.example.projektna.services.SimulationService
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class SettingsFragment : Fragment() {

    private lateinit var preferencesManager: PreferencesManager

    // Account views
    private lateinit var textUserEmail: TextView
    private lateinit var btnLogout: MaterialButton

    // Collapsible section views
    private lateinit var headerMqtt: LinearLayout
    private lateinit var contentMqtt: LinearLayout
    private lateinit var iconMqttExpand: ImageView
    private lateinit var headerFrequency: LinearLayout
    private lateinit var contentFrequency: LinearLayout
    private lateinit var iconFrequencyExpand: ImageView

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
    private lateinit var layoutSpeedRange: LinearLayout
    private lateinit var inputLatMin: TextInputEditText
    private lateinit var inputLatMax: TextInputEditText
    private lateinit var inputLonMin: TextInputEditText
    private lateinit var inputLonMax: TextInputEditText
    private lateinit var inputSpeedMin: TextInputEditText
    private lateinit var inputSpeedMax: TextInputEditText
    private lateinit var checkboxManualLocation: MaterialCheckBox
    private lateinit var layoutManualLocation: LinearLayout
    private lateinit var inputManualLat: TextInputEditText
    private lateinit var inputManualLon: TextInputEditText
    private lateinit var btnPickOnMap: MaterialButton
    private lateinit var btnStartSimulation: MaterialButton

    private val sensorTypes = listOf(
        SimulationSensorType.GPS,
        SimulationSensorType.SPEED
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
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(SimulationService.ACTION_SIMULATION_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(simulationStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(simulationStatusReceiver, filter)
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
    }

    private fun setupViews(view: View) {
        // Account views
        textUserEmail = view.findViewById(R.id.text_user_email)
        btnLogout = view.findViewById(R.id.btn_logout)

        // Collapsible section views
        headerMqtt = view.findViewById(R.id.header_mqtt)
        contentMqtt = view.findViewById(R.id.content_mqtt)
        iconMqttExpand = view.findViewById(R.id.icon_mqtt_expand)
        headerFrequency = view.findViewById(R.id.header_frequency)
        contentFrequency = view.findViewById(R.id.content_frequency)
        iconFrequencyExpand = view.findViewById(R.id.icon_frequency_expand)

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
        layoutSpeedRange = view.findViewById(R.id.layout_speed_range)
        inputLatMin = view.findViewById(R.id.input_lat_min)
        inputLatMax = view.findViewById(R.id.input_lat_max)
        inputLonMin = view.findViewById(R.id.input_lon_min)
        inputLonMax = view.findViewById(R.id.input_lon_max)
        inputSpeedMin = view.findViewById(R.id.input_speed_min)
        inputSpeedMax = view.findViewById(R.id.input_speed_max)
        checkboxManualLocation = view.findViewById(R.id.checkbox_manual_location)
        layoutManualLocation = view.findViewById(R.id.layout_manual_location)
        inputManualLat = view.findViewById(R.id.input_manual_lat)
        inputManualLon = view.findViewById(R.id.input_manual_lon)
        btnPickOnMap = view.findViewById(R.id.btn_pick_on_map)
        btnStartSimulation = view.findViewById(R.id.btn_start_simulation)

        // Setup sensor type dropdown
        val sensorTypeLabels = sensorTypes.map { type ->
            when (type) {
                SimulationSensorType.GPS -> getString(R.string.simulation_type_gps)
                SimulationSensorType.SPEED -> getString(R.string.simulation_type_speed)
            }
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sensorTypeLabels)
        dropdownSensorType.setAdapter(adapter)
    }

    private fun loadSavedValues() {
        // Account values
        textUserEmail.text = preferencesManager.userEmail ?: getString(R.string.account_no_email)

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
            SimulationSensorType.SPEED -> getString(R.string.simulation_type_speed)
        }
        dropdownSensorType.setText(sensorTypeLabel, false)

        sliderSimulationInterval.value = preferencesManager.simulationIntervalSeconds.toFloat()
        updateSimulationIntervalText(preferencesManager.simulationIntervalSeconds)

        inputLatMin.setText(preferencesManager.simulationGpsLatMin.toString())
        inputLatMax.setText(preferencesManager.simulationGpsLatMax.toString())
        inputLonMin.setText(preferencesManager.simulationGpsLonMin.toString())
        inputLonMax.setText(preferencesManager.simulationGpsLonMax.toString())
        inputSpeedMin.setText(preferencesManager.simulationSpeedMin.toString())
        inputSpeedMax.setText(preferencesManager.simulationSpeedMax.toString())

        checkboxManualLocation.isChecked = preferencesManager.useManualLocation
        inputManualLat.setText(preferencesManager.simulationManualLat.toString())
        inputManualLon.setText(preferencesManager.simulationManualLon.toString())
    }

    private fun setupListeners() {
        // Logout listener
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        // Collapsible section listeners
        headerMqtt.setOnClickListener {
            toggleSection(contentMqtt, iconMqttExpand)
        }
        headerFrequency.setOnClickListener {
            toggleSection(contentFrequency, iconFrequencyExpand)
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

        // Speed range inputs
        inputSpeedMin.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationSpeedMin = it
            }
        }
        inputSpeedMax.doAfterTextChanged { text ->
            text?.toString()?.toFloatOrNull()?.let {
                preferencesManager.simulationSpeedMax = it
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

        btnStartSimulation.setOnClickListener {
            if (isSimulationServiceRunning()) {
                stopSimulationService()
            } else {
                startSimulationService()
            }
            updateSimulationButtonState()
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
        when (sensorType) {
            SimulationSensorType.GPS -> {
                layoutGpsRange.visibility = View.VISIBLE
                layoutSpeedRange.visibility = View.GONE
            }
            SimulationSensorType.SPEED -> {
                layoutGpsRange.visibility = View.VISIBLE // Potrebujemo tudi GPS za hitrost
                layoutSpeedRange.visibility = View.VISIBLE
            }
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
}

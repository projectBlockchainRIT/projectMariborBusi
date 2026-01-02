package com.example.projektna.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.projektna.R
import com.example.projektna.data.PreferencesManager
import com.google.android.material.slider.Slider

class SettingsFragment : Fragment() {

    private lateinit var preferencesManager: PreferencesManager

    private lateinit var sliderGpsInterval: Slider
    private lateinit var sliderAccelerometerInterval: Slider
    private lateinit var textGpsIntervalValue: TextView
    private lateinit var textAccelerometerIntervalValue: TextView

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
    }

    private fun setupViews(view: View) {
        sliderGpsInterval = view.findViewById(R.id.slider_gps_interval)
        sliderAccelerometerInterval = view.findViewById(R.id.slider_accelerometer_interval)
        textGpsIntervalValue = view.findViewById(R.id.text_gps_interval_value)
        textAccelerometerIntervalValue = view.findViewById(R.id.text_accelerometer_interval_value)
    }

    private fun loadSavedValues() {
        val gpsInterval = preferencesManager.gpsIntervalSeconds
        val accelerometerInterval = preferencesManager.accelerometerIntervalSeconds

        sliderGpsInterval.value = gpsInterval.toFloat()
        sliderAccelerometerInterval.value = accelerometerInterval.toFloat()

        updateGpsIntervalText(gpsInterval)
        updateAccelerometerIntervalText(accelerometerInterval)
    }

    private fun setupListeners() {
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
    }

    private fun updateGpsIntervalText(interval: Int) {
        textGpsIntervalValue.text = getString(R.string.settings_interval_seconds, interval)
    }

    private fun updateAccelerometerIntervalText(interval: Int) {
        textAccelerometerIntervalValue.text = getString(R.string.settings_interval_seconds, interval)
    }
}

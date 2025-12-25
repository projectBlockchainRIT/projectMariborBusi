package com.example.projektna.ui.sensors

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.projektna.R
import com.example.projektna.data.PreferencesManager
import com.example.projektna.services.AccelerometerService
import com.google.android.material.materialswitch.MaterialSwitch

class SensorsFragment : Fragment() {

    private lateinit var preferencesManager: PreferencesManager

    private lateinit var switchAccelerometer: MaterialSwitch
    private lateinit var accelerometerStatus: TextView
    private lateinit var accelerometerMagnitude: TextView

    private val accelerometerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val magnitude = it.getFloatExtra(AccelerometerService.EXTRA_MAGNITUDE, 0f)
                val isExtreme = it.getBooleanExtra(AccelerometerService.EXTRA_IS_EXTREME, false)
                updateMagnitudeDisplay(magnitude, isExtreme)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startAccelerometerService()
        } else {
            switchAccelerometer.isChecked = false
            Toast.makeText(
                requireContext(),
                "Potrebno je dovoljenje za obvestila",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sensors, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        switchAccelerometer = view.findViewById(R.id.switch_accelerometer)
        accelerometerStatus = view.findViewById(R.id.accelerometer_status)
        accelerometerMagnitude = view.findViewById(R.id.accelerometer_magnitude)

        setupAccelerometerSwitch()
        restoreSwitchStates()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(AccelerometerService.ACTION_ACCELEROMETER_DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                accelerometerReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            ContextCompat.registerReceiver(
                requireContext(),
                accelerometerReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(accelerometerReceiver)
    }

    private fun setupAccelerometerSwitch() {
        switchAccelerometer.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.isAccelerometerEnabled = isChecked

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        return@setOnCheckedChangeListener
                    }
                }
                startAccelerometerService()
            } else {
                stopAccelerometerService()
            }
        }
    }

    private fun restoreSwitchStates() {
        switchAccelerometer.isChecked = preferencesManager.isAccelerometerEnabled

        if (preferencesManager.isAccelerometerEnabled) {
            accelerometerStatus.text = getString(R.string.enabled)
            accelerometerMagnitude.visibility = View.VISIBLE
        }
    }

    private fun startAccelerometerService() {
        accelerometerStatus.text = getString(R.string.enabled)
        accelerometerMagnitude.visibility = View.VISIBLE

        val intent = Intent(requireContext(), AccelerometerService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopAccelerometerService() {
        accelerometerStatus.text = getString(R.string.disabled)
        accelerometerMagnitude.visibility = View.GONE

        val intent = Intent(requireContext(), AccelerometerService::class.java)
        requireContext().stopService(intent)
    }

    private fun updateMagnitudeDisplay(magnitude: Float, isExtreme: Boolean) {
        accelerometerMagnitude.text = getString(R.string.accelerometer_magnitude, magnitude)

        if (isExtreme) {
            accelerometerMagnitude.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.error)
            )
        } else {
            accelerometerMagnitude.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
        }
    }
}

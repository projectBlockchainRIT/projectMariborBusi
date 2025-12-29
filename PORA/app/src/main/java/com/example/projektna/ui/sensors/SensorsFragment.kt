package com.example.projektna.ui.sensors

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.projektna.R
import com.example.projektna.data.CameraData
import com.example.projektna.data.GpsData
import com.example.projektna.data.PreferencesManager
import com.example.projektna.services.AccelerometerService
import com.example.projektna.services.GpsService
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorsFragment : Fragment() {

    private lateinit var preferencesManager: PreferencesManager

    private lateinit var switchAccelerometer: MaterialSwitch
    private lateinit var accelerometerStatus: TextView
    private lateinit var accelerometerMagnitude: TextView

    private lateinit var switchGps: MaterialSwitch
    private lateinit var gpsStatus: TextView
    private lateinit var gpsCoordinates: TextView
    private lateinit var gpsSpeed: TextView

    private lateinit var buttonOpenCamera: MaterialButton
    private lateinit var cameraStatus: TextView
    private lateinit var cameraLastCapture: TextView
    private var currentPhotoUri: Uri? = null
    private var currentPhotoPath: String? = null

    private val accelerometerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val magnitude = it.getFloatExtra(AccelerometerService.EXTRA_MAGNITUDE, 0f)
                val isExtreme = it.getBooleanExtra(AccelerometerService.EXTRA_IS_EXTREME, false)
                updateMagnitudeDisplay(magnitude, isExtreme)
            }
        }
    }

    private val gpsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val latitude = it.getDoubleExtra(GpsService.EXTRA_LATITUDE, 0.0)
                val longitude = it.getDoubleExtra(GpsService.EXTRA_LONGITUDE, 0.0)
                val speed = it.getFloatExtra(GpsService.EXTRA_SPEED, 0f)
                val isExtreme = it.getBooleanExtra(GpsService.EXTRA_IS_EXTREME, false)
                updateGpsDisplay(latitude, longitude, speed, isExtreme)
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

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            checkNotificationPermissionForGps()
        } else {
            switchGps.isChecked = false
            Toast.makeText(
                requireContext(),
                "Potrebno je dovoljenje za lokacijo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val gpsNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startGpsService()
        } else {
            switchGps.isChecked = false
            Toast.makeText(
                requireContext(),
                "Potrebno je dovoljenje za obvestila",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.camera_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                onPhotoCaptured(path)
            }
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

        switchGps = view.findViewById(R.id.switch_gps)
        gpsStatus = view.findViewById(R.id.gps_status)
        gpsCoordinates = view.findViewById(R.id.gps_coordinates)
        gpsSpeed = view.findViewById(R.id.gps_speed)

        buttonOpenCamera = view.findViewById(R.id.button_open_camera)
        cameraStatus = view.findViewById(R.id.camera_status)
        cameraLastCapture = view.findViewById(R.id.camera_last_capture)

        setupAccelerometerSwitch()
        setupGpsSwitch()
        setupCameraButton()
        restoreSwitchStates()
    }

    override fun onResume() {
        super.onResume()
        val accelerometerFilter = IntentFilter(AccelerometerService.ACTION_ACCELEROMETER_DATA)
        val gpsFilter = IntentFilter(GpsService.ACTION_GPS_DATA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                accelerometerReceiver,
                accelerometerFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
            requireContext().registerReceiver(
                gpsReceiver,
                gpsFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            ContextCompat.registerReceiver(
                requireContext(),
                accelerometerReceiver,
                accelerometerFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            ContextCompat.registerReceiver(
                requireContext(),
                gpsReceiver,
                gpsFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(accelerometerReceiver)
        requireContext().unregisterReceiver(gpsReceiver)
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
        switchGps.isChecked = preferencesManager.isGpsEnabled

        if (preferencesManager.isAccelerometerEnabled) {
            accelerometerStatus.text = getString(R.string.enabled)
            accelerometerMagnitude.visibility = View.VISIBLE
        }

        if (preferencesManager.isGpsEnabled) {
            gpsStatus.text = getString(R.string.enabled)
            gpsCoordinates.visibility = View.VISIBLE
            gpsSpeed.visibility = View.VISIBLE
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

    private fun setupGpsSwitch() {
        switchGps.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.isGpsEnabled = isChecked

            if (isChecked) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                    return@setOnCheckedChangeListener
                }
                checkNotificationPermissionForGps()
            } else {
                stopGpsService()
            }
        }
    }

    private fun checkNotificationPermissionForGps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                gpsNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startGpsService()
    }

    private fun startGpsService() {
        gpsStatus.text = getString(R.string.enabled)
        gpsCoordinates.visibility = View.VISIBLE
        gpsSpeed.visibility = View.VISIBLE

        val intent = Intent(requireContext(), GpsService::class.java)
        ContextCompat.startForegroundService(requireContext(), intent)
    }

    private fun stopGpsService() {
        gpsStatus.text = getString(R.string.disabled)
        gpsCoordinates.visibility = View.GONE
        gpsSpeed.visibility = View.GONE

        val intent = Intent(requireContext(), GpsService::class.java)
        requireContext().stopService(intent)
    }

    private fun updateGpsDisplay(latitude: Double, longitude: Double, speed: Float, isExtreme: Boolean) {
        gpsCoordinates.text = getString(R.string.gps_coordinates, latitude, longitude)
        val speedKmh = speed * 3.6f
        gpsSpeed.text = getString(R.string.gps_speed, speedKmh)

        val textColor = if (isExtreme) {
            ContextCompat.getColor(requireContext(), R.color.error)
        } else {
            ContextCompat.getColor(requireContext(), R.color.primary)
        }
        gpsCoordinates.setTextColor(textColor)
        gpsSpeed.setTextColor(textColor)
    }

    private fun setupCameraButton() {
        buttonOpenCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                openCamera()
            }
        }
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        if (photoFile != null) {
            currentPhotoPath = photoFile.absolutePath
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(currentPhotoUri)
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.camera_error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "IMG_${timeStamp}"
            val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    private fun onPhotoCaptured(imagePath: String) {
        val timestamp = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeString = timeFormat.format(Date(timestamp))

        cameraStatus.text = getString(R.string.enabled)
        cameraLastCapture.visibility = View.VISIBLE
        cameraLastCapture.text = getString(R.string.last_capture, timeString)

        // TODO: Get current GPS location if GPS service is running
        val latitude: Double? = null
        val longitude: Double? = null

        val cameraData = CameraData(
            imagePath = imagePath,
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            isPendingUpload = true
        )

        // TODO: Save cameraData to Room database for MQTT upload queue
    }
}

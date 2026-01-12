package com.example.projektna.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.projektna.R
import com.example.projektna.data.PreferencesManager
import com.example.projektna.data.api.model.BusRoute
import com.example.projektna.data.api.model.BusStop
import com.example.projektna.databinding.FragmentEventsBinding
import com.example.projektna.util.Resource

class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EventsViewModel by viewModels()
    private lateinit var preferencesManager: PreferencesManager

    private var busStops: List<BusStop> = emptyList()
    private var routes: List<BusRoute> = emptyList()
    private var selectedStop: BusStop? = null
    private var selectedRoute: BusRoute? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferencesManager = PreferencesManager(requireContext())

        setupSlider()
        setupStationSelector()
        setupLineSelector()
        setupSubmitButton()
        observeViewModel()
    }

    private fun setupSlider() {
        binding.sliderDelay.addOnChangeListener { _, value, _ ->
            binding.textViewDelayValue.text = getString(R.string.delay_value_format, value.toInt())
        }
    }

    private fun setupStationSelector() {
        binding.autoCompleteStation.setOnItemClickListener { _, _, position, _ ->
            selectedStop = busStops.getOrNull(position)
        }
    }

    private fun setupLineSelector() {
        binding.autoCompleteLine.setOnItemClickListener { _, _, position, _ ->
            selectedRoute = routes.getOrNull(position)
        }
    }

    private fun setupSubmitButton() {
        binding.buttonSubmitDelay.setOnClickListener {
            submitDelay()
        }
    }

    private fun submitDelay() {
        val stop = selectedStop
        val route = selectedRoute

        if (stop == null) {
            Toast.makeText(context, R.string.select_station_first, Toast.LENGTH_SHORT).show()
            return
        }

        if (route == null) {
            Toast.makeText(context, R.string.select_line_first, Toast.LENGTH_SHORT).show()
            return
        }

        val userId = preferencesManager.userId
        if (userId == -1) {
            Toast.makeText(context, R.string.auth_error_not_logged_in, Toast.LENGTH_SHORT).show()
            return
        }

        val delayMinutes = binding.sliderDelay.value.toInt()

        viewModel.submitDelay(
            userId = userId,
            stopId = stop.id.toInt(),
            lineId = route.lineId.toInt(),
            delayMinutes = delayMinutes
        )
    }

    private fun observeViewModel() {
        viewModel.busStops.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    busStops = resource.data ?: emptyList()
                    setupStationsDropdown()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
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

        viewModel.delaySubmitResult.observe(viewLifecycleOwner) { resource ->
            resource?.let {
                when (it) {
                    is Resource.Loading -> {
                        binding.buttonSubmitDelay.isEnabled = false
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is Resource.Success -> {
                        binding.buttonSubmitDelay.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, R.string.delay_submitted, Toast.LENGTH_SHORT).show()
                        resetForm()
                        viewModel.clearDelayResult()
                    }
                    is Resource.Error -> {
                        binding.buttonSubmitDelay.isEnabled = true
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(
                            context,
                            getString(R.string.delay_submit_error, it.message),
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.clearDelayResult()
                    }
                }
            }
        }
    }

    private fun setupStationsDropdown() {
        val stationNames = busStops.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            stationNames
        )
        binding.autoCompleteStation.setAdapter(adapter)
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

    private fun resetForm() {
        binding.autoCompleteStation.setText("", false)
        binding.autoCompleteLine.setText("", false)
        binding.sliderDelay.value = 5f
        binding.textViewDelayValue.text = getString(R.string.delay_value_format, 5)
        selectedStop = null
        selectedRoute = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

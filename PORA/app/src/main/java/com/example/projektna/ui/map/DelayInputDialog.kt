package com.example.projektna.ui.map

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.projektna.R
import com.example.projektna.data.api.model.BusStop
import com.example.projektna.data.api.model.DepartureGroup
import com.example.projektna.data.repository.DelayRepository
import com.example.projektna.databinding.DialogDelayInputBinding
import com.example.projektna.util.Resource
import kotlinx.coroutines.launch

class DelayInputDialog : DialogFragment() {

    private var _binding: DialogDelayInputBinding? = null
    private val binding get() = _binding!!

    private val delayRepository = DelayRepository()

    private var busStop: BusStop? = null
    private var availableLines: List<DepartureGroup> = emptyList()
    private var selectedLine: String? = null
    private var userLatitude: Double? = null
    private var userLongitude: Double? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogDelayInputBinding.inflate(LayoutInflater.from(context))

        setupViews()
        setupSlider()
        setupLineSelector()

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.report_delay_title)
            .setView(binding.root)
            .setPositiveButton(R.string.submit_delay) { _, _ ->
                submitDelay()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
    }

    private fun setupViews() {
        busStop?.let { stop ->
            binding.textViewStationInfo.text = getString(R.string.station_delay_info, stop.name)
        }
        binding.textViewDelayValue.text = getString(R.string.delay_value_format, 5)
        binding.sliderDelay.value = 5f
    }

    private fun setupSlider() {
        binding.sliderDelay.addOnChangeListener { _, value, _ ->
            binding.textViewDelayValue.text = getString(R.string.delay_value_format, value.toInt())
        }
    }

    private fun setupLineSelector() {
        if (availableLines.isNotEmpty()) {
            val lineNames = availableLines.map { "${it.line} - ${it.direction}" }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                lineNames
            )
            binding.autoCompleteLine.setAdapter(adapter)

            binding.autoCompleteLine.setOnItemClickListener { _, _, position, _ ->
                selectedLine = availableLines.getOrNull(position)?.line
            }

            // Pre-select first line
            if (lineNames.isNotEmpty()) {
                binding.autoCompleteLine.setText(lineNames[0], false)
                selectedLine = availableLines[0].line
            }
        } else {
            binding.textInputLayoutLine.hint = getString(R.string.no_lines_available)
            binding.autoCompleteLine.isEnabled = false
        }
    }

    private fun submitDelay() {
        val stop = busStop ?: return
        val line = selectedLine

        if (line.isNullOrEmpty()) {
            Toast.makeText(context, R.string.select_line_first, Toast.LENGTH_SHORT).show()
            return
        }

        val delayMinutes = binding.sliderDelay.value.toInt()

        lifecycleScope.launch {
            val result = delayRepository.submitDelay(
                stationId = stop.id,
                stationName = stop.name,
                lineId = line,
                delayMinutes = delayMinutes,
                latitude = userLatitude,
                longitude = userLongitude
            )

            when (result) {
                is Resource.Success -> {
                    Toast.makeText(context, R.string.delay_submitted, Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    Toast.makeText(
                        context,
                        getString(R.string.delay_submit_error, result.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
                is Resource.Loading -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "DelayInputDialog"

        fun newInstance(
            busStop: BusStop,
            availableLines: List<DepartureGroup>,
            userLatitude: Double? = null,
            userLongitude: Double? = null
        ): DelayInputDialog {
            return DelayInputDialog().apply {
                this.busStop = busStop
                this.availableLines = availableLines
                this.userLatitude = userLatitude
                this.userLongitude = userLongitude
            }
        }
    }
}

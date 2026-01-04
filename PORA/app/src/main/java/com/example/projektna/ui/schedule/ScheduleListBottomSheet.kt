package com.example.projektna.ui.schedule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.projektna.R
import com.example.projektna.data.schedule.SensorSchedule
import com.example.projektna.data.schedule.SensorType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScheduleListBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: ScheduleViewModel by activityViewModels()
    private lateinit var sensorType: SensorType
    private lateinit var adapter: ScheduleAdapter

    companion object {
        private const val ARG_SENSOR_TYPE = "sensor_type"

        fun newInstance(sensorType: SensorType): ScheduleListBottomSheet {
            return ScheduleListBottomSheet().apply {
                arguments = bundleOf(ARG_SENSOR_TYPE to sensorType.name)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_schedule_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sensorTypeName = arguments?.getString(ARG_SENSOR_TYPE)
        if (sensorTypeName == null) {
            dismiss()
            return
        }
        sensorType = SensorType.valueOf(sensorTypeName)

        setupTitle(view)
        setupRecyclerView(view)
        setupAddButton(view)
        observeSchedules(view)
    }

    private fun setupTitle(view: View) {
        val title = view.findViewById<TextView>(R.id.title)
        title.text = getString(R.string.schedules_for_sensor, getSensorName())
    }

    private fun getSensorName(): String {
        return when (sensorType) {
            SensorType.ACCELEROMETER -> getString(R.string.accelerometer)
            SensorType.GPS -> getString(R.string.gps)
            SensorType.CAMERA -> getString(R.string.camera)
        }
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_schedules)

        adapter = ScheduleAdapter(
            onToggleEnabled = { schedule ->
                viewModel.toggleScheduleEnabled(schedule)
            },
            onItemClick = { schedule ->
                showEditDialog(schedule)
            }
        )

        recyclerView.adapter = adapter
    }

    private fun setupAddButton(view: View) {
        val buttonAdd = view.findViewById<MaterialButton>(R.id.button_add_schedule)
        buttonAdd.setOnClickListener {
            showEditDialog(null)
        }
    }

    private fun observeSchedules(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_schedules)
        val emptyText = view.findViewById<TextView>(R.id.empty_text)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getSchedulesForSensor(sensorType).collectLatest { schedules ->
                adapter.submitList(schedules)
                if (schedules.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyText.visibility = View.GONE
                }
            }
        }
    }

    private fun showEditDialog(schedule: SensorSchedule?) {
        val dialog = ScheduleEditorDialog.newInstance(sensorType, schedule)
        dialog.show(parentFragmentManager, "schedule_editor")
    }
}

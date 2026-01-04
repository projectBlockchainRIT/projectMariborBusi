package com.example.projektna.ui.schedule

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.projektna.R
import com.example.projektna.data.schedule.ActionType
import com.example.projektna.data.schedule.ScheduleType
import com.example.projektna.data.schedule.SensorSchedule
import com.example.projektna.data.schedule.SensorType
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScheduleEditorDialog : DialogFragment() {

    private val viewModel: ScheduleViewModel by activityViewModels()

    private lateinit var sensorType: SensorType
    private var existingSchedule: SensorSchedule? = null

    private var selectedHour: Int = 7
    private var selectedMinute: Int = 30
    private var selectedDateMillis: Long? = null

    companion object {
        private const val ARG_SENSOR_TYPE = "sensor_type"
        private const val ARG_SCHEDULE_ID = "schedule_id"
        private const val ARG_SCHEDULE_HOUR = "schedule_hour"
        private const val ARG_SCHEDULE_MINUTE = "schedule_minute"
        private const val ARG_SCHEDULE_TYPE = "schedule_type"
        private const val ARG_ACTION_TYPE = "action_type"
        private const val ARG_WEEK_DAYS = "week_days"
        private const val ARG_DATE_MILLIS = "date_millis"
        private const val ARG_DURATION = "duration"
        private const val ARG_IS_ENABLED = "is_enabled"

        fun newInstance(sensorType: SensorType, schedule: SensorSchedule?): ScheduleEditorDialog {
            return ScheduleEditorDialog().apply {
                arguments = bundleOf(
                    ARG_SENSOR_TYPE to sensorType.name,
                    ARG_SCHEDULE_ID to (schedule?.id ?: 0L),
                    ARG_SCHEDULE_HOUR to (schedule?.hour ?: 7),
                    ARG_SCHEDULE_MINUTE to (schedule?.minute ?: 30),
                    ARG_SCHEDULE_TYPE to (schedule?.scheduleType?.name ?: ScheduleType.DAILY.name),
                    ARG_ACTION_TYPE to (schedule?.actionType?.name ?: ActionType.AUTO_START.name),
                    ARG_WEEK_DAYS to (schedule?.weekDays ?: 0),
                    ARG_DATE_MILLIS to schedule?.scheduledDateMillis,
                    ARG_DURATION to (schedule?.durationMinutes ?: 30),
                    ARG_IS_ENABLED to (schedule?.isEnabled ?: true)
                )
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        sensorType = SensorType.valueOf(args.getString(ARG_SENSOR_TYPE)!!)

        val scheduleId = args.getLong(ARG_SCHEDULE_ID)
        if (scheduleId != 0L) {
            existingSchedule = SensorSchedule(
                id = scheduleId,
                sensorType = sensorType,
                scheduleType = ScheduleType.valueOf(args.getString(ARG_SCHEDULE_TYPE)!!),
                actionType = ActionType.valueOf(args.getString(ARG_ACTION_TYPE)!!),
                hour = args.getInt(ARG_SCHEDULE_HOUR),
                minute = args.getInt(ARG_SCHEDULE_MINUTE),
                weekDays = args.getInt(ARG_WEEK_DAYS),
                scheduledDateMillis = if (args.containsKey(ARG_DATE_MILLIS)) args.getLong(ARG_DATE_MILLIS) else null,
                durationMinutes = args.getInt(ARG_DURATION),
                isEnabled = args.getBoolean(ARG_IS_ENABLED)
            )
        }

        selectedHour = args.getInt(ARG_SCHEDULE_HOUR, 7)
        selectedMinute = args.getInt(ARG_SCHEDULE_MINUTE, 30)
        selectedDateMillis = if (args.containsKey(ARG_DATE_MILLIS)) args.getLong(ARG_DATE_MILLIS) else null

        val view = layoutInflater.inflate(R.layout.dialog_schedule_editor, null)
        setupViews(view, args)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (existingSchedule != null) R.string.edit_schedule else R.string.new_schedule)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ -> saveSchedule(view) }
            .setNegativeButton(R.string.cancel, null)

        if (existingSchedule != null) {
            builder.setNeutralButton(R.string.delete) { _, _ -> deleteSchedule() }
        }

        return builder.create()
    }

    private fun setupViews(view: View, args: Bundle) {
        setupTimeButton(view)
        setupScheduleTypeRadio(view, args)
        setupWeekDayChips(view, args)
        setupDateButton(view)
        setupActionTypeRadio(view, args)
        setupDurationSlider(view, args)
    }

    private fun setupTimeButton(view: View) {
        val buttonTime = view.findViewById<MaterialButton>(R.id.button_select_time)
        updateTimeButtonText(buttonTime)

        buttonTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText(R.string.select_time)
                .build()

            picker.addOnPositiveButtonClickListener {
                selectedHour = picker.hour
                selectedMinute = picker.minute
                updateTimeButtonText(buttonTime)
            }

            picker.show(parentFragmentManager, "time_picker")
        }
    }

    private fun updateTimeButtonText(button: MaterialButton) {
        button.text = String.format("%02d:%02d", selectedHour, selectedMinute)
    }

    private fun setupScheduleTypeRadio(view: View, args: Bundle) {
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_schedule_type)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_days)
        val dateButton = view.findViewById<MaterialButton>(R.id.button_select_date)

        val scheduleType = ScheduleType.valueOf(args.getString(ARG_SCHEDULE_TYPE)!!)
        when (scheduleType) {
            ScheduleType.DAILY -> radioGroup.check(R.id.radio_daily)
            ScheduleType.WEEKLY -> radioGroup.check(R.id.radio_weekly)
            ScheduleType.ONE_TIME -> radioGroup.check(R.id.radio_one_time)
        }

        updateScheduleTypeVisibility(radioGroup.checkedRadioButtonId, chipGroup, dateButton)

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            updateScheduleTypeVisibility(checkedId, chipGroup, dateButton)
        }
    }

    private fun updateScheduleTypeVisibility(checkedId: Int, chipGroup: ChipGroup, dateButton: MaterialButton) {
        when (checkedId) {
            R.id.radio_daily -> {
                chipGroup.visibility = View.GONE
                dateButton.visibility = View.GONE
            }
            R.id.radio_weekly -> {
                chipGroup.visibility = View.VISIBLE
                dateButton.visibility = View.GONE
            }
            R.id.radio_one_time -> {
                chipGroup.visibility = View.GONE
                dateButton.visibility = View.VISIBLE
            }
        }
    }

    private fun setupWeekDayChips(view: View, args: Bundle) {
        val weekDays = args.getInt(ARG_WEEK_DAYS)

        view.findViewById<Chip>(R.id.chip_monday).isChecked = (weekDays and SensorSchedule.MONDAY) != 0
        view.findViewById<Chip>(R.id.chip_tuesday).isChecked = (weekDays and SensorSchedule.TUESDAY) != 0
        view.findViewById<Chip>(R.id.chip_wednesday).isChecked = (weekDays and SensorSchedule.WEDNESDAY) != 0
        view.findViewById<Chip>(R.id.chip_thursday).isChecked = (weekDays and SensorSchedule.THURSDAY) != 0
        view.findViewById<Chip>(R.id.chip_friday).isChecked = (weekDays and SensorSchedule.FRIDAY) != 0
        view.findViewById<Chip>(R.id.chip_saturday).isChecked = (weekDays and SensorSchedule.SATURDAY) != 0
        view.findViewById<Chip>(R.id.chip_sunday).isChecked = (weekDays and SensorSchedule.SUNDAY) != 0
    }

    private fun setupDateButton(view: View) {
        val dateButton = view.findViewById<MaterialButton>(R.id.button_select_date)
        updateDateButtonText(dateButton)

        dateButton.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.select_date)
                .setSelection(selectedDateMillis ?: System.currentTimeMillis())
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                selectedDateMillis = selection
                updateDateButtonText(dateButton)
            }

            picker.show(parentFragmentManager, "date_picker")
        }
    }

    private fun updateDateButtonText(button: MaterialButton) {
        if (selectedDateMillis != null) {
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            button.text = dateFormat.format(Date(selectedDateMillis!!))
        } else {
            button.text = getString(R.string.select_date)
        }
    }

    private fun setupActionTypeRadio(view: View, args: Bundle) {
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_action_type)
        val durationLayout = view.findViewById<LinearLayout>(R.id.layout_duration)

        val actionType = ActionType.valueOf(args.getString(ARG_ACTION_TYPE)!!)
        when (actionType) {
            ActionType.NOTIFICATION_ONLY -> radioGroup.check(R.id.radio_notification)
            ActionType.AUTO_START -> radioGroup.check(R.id.radio_auto_start)
        }

        // Camera only supports notification
        if (sensorType == SensorType.CAMERA) {
            radioGroup.check(R.id.radio_notification)
            view.findViewById<View>(R.id.radio_auto_start).isEnabled = false
            durationLayout.visibility = View.GONE
        } else {
            updateActionTypeVisibility(radioGroup.checkedRadioButtonId, durationLayout)
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            updateActionTypeVisibility(checkedId, durationLayout)
        }
    }

    private fun updateActionTypeVisibility(checkedId: Int, durationLayout: LinearLayout) {
        durationLayout.visibility = if (checkedId == R.id.radio_auto_start) View.VISIBLE else View.GONE
    }

    private fun setupDurationSlider(view: View, args: Bundle) {
        val slider = view.findViewById<Slider>(R.id.slider_duration)
        val label = view.findViewById<TextView>(R.id.text_duration_label)

        val duration = args.getInt(ARG_DURATION, 30)
        slider.value = duration.toFloat()
        updateDurationLabel(label, duration)

        slider.addOnChangeListener { _, value, _ ->
            updateDurationLabel(label, value.toInt())
        }
    }

    private fun updateDurationLabel(label: TextView, minutes: Int) {
        label.text = getString(R.string.duration_minutes, minutes)
    }

    private fun saveSchedule(view: View) {
        val scheduleType = getSelectedScheduleType(view)
        val actionType = getSelectedActionType(view)
        val weekDays = getSelectedWeekDays(view)
        val duration = view.findViewById<Slider>(R.id.slider_duration).value.toInt()

        val schedule = SensorSchedule(
            id = existingSchedule?.id ?: 0,
            sensorType = sensorType,
            scheduleType = scheduleType,
            actionType = actionType,
            hour = selectedHour,
            minute = selectedMinute,
            weekDays = weekDays,
            scheduledDateMillis = if (scheduleType == ScheduleType.ONE_TIME) selectedDateMillis else null,
            durationMinutes = duration,
            isEnabled = existingSchedule?.isEnabled ?: true,
            createdAt = existingSchedule?.createdAt ?: System.currentTimeMillis(),
            lastTriggeredAt = existingSchedule?.lastTriggeredAt
        )

        viewModel.saveSchedule(schedule)
    }

    private fun getSelectedScheduleType(view: View): ScheduleType {
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_schedule_type)
        return when (radioGroup.checkedRadioButtonId) {
            R.id.radio_daily -> ScheduleType.DAILY
            R.id.radio_weekly -> ScheduleType.WEEKLY
            R.id.radio_one_time -> ScheduleType.ONE_TIME
            else -> ScheduleType.DAILY
        }
    }

    private fun getSelectedActionType(view: View): ActionType {
        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_action_type)
        return when (radioGroup.checkedRadioButtonId) {
            R.id.radio_notification -> ActionType.NOTIFICATION_ONLY
            R.id.radio_auto_start -> ActionType.AUTO_START
            else -> ActionType.AUTO_START
        }
    }

    private fun getSelectedWeekDays(view: View): Int {
        var days = 0
        if (view.findViewById<Chip>(R.id.chip_monday).isChecked) days = days or SensorSchedule.MONDAY
        if (view.findViewById<Chip>(R.id.chip_tuesday).isChecked) days = days or SensorSchedule.TUESDAY
        if (view.findViewById<Chip>(R.id.chip_wednesday).isChecked) days = days or SensorSchedule.WEDNESDAY
        if (view.findViewById<Chip>(R.id.chip_thursday).isChecked) days = days or SensorSchedule.THURSDAY
        if (view.findViewById<Chip>(R.id.chip_friday).isChecked) days = days or SensorSchedule.FRIDAY
        if (view.findViewById<Chip>(R.id.chip_saturday).isChecked) days = days or SensorSchedule.SATURDAY
        if (view.findViewById<Chip>(R.id.chip_sunday).isChecked) days = days or SensorSchedule.SUNDAY
        return days
    }

    private fun deleteSchedule() {
        existingSchedule?.let { viewModel.deleteSchedule(it) }
    }
}

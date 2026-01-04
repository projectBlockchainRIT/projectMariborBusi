package com.example.projektna.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projektna.R
import com.example.projektna.data.schedule.ActionType
import com.example.projektna.data.schedule.ScheduleType
import com.example.projektna.data.schedule.SensorSchedule
import com.google.android.material.materialswitch.MaterialSwitch

class ScheduleAdapter(
    private val onToggleEnabled: (SensorSchedule) -> Unit,
    private val onItemClick: (SensorSchedule) -> Unit
) : ListAdapter<SensorSchedule, ScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textTime: TextView = itemView.findViewById(R.id.text_time)
        private val textScheduleType: TextView = itemView.findViewById(R.id.text_schedule_type)
        private val textActionType: TextView = itemView.findViewById(R.id.text_action_type)
        private val textDays: TextView = itemView.findViewById(R.id.text_days)
        private val switchEnabled: MaterialSwitch = itemView.findViewById(R.id.switch_enabled)

        fun bind(schedule: SensorSchedule) {
            val context = itemView.context

            textTime.text = schedule.getFormattedTime()

            textScheduleType.text = when (schedule.scheduleType) {
                ScheduleType.DAILY -> context.getString(R.string.schedule_daily)
                ScheduleType.WEEKLY -> context.getString(R.string.schedule_weekly)
                ScheduleType.ONE_TIME -> context.getString(R.string.schedule_one_time)
            }

            textActionType.text = when (schedule.actionType) {
                ActionType.NOTIFICATION_ONLY -> context.getString(R.string.action_notification)
                ActionType.AUTO_START -> context.getString(
                    R.string.action_auto_start_with_duration,
                    schedule.durationMinutes
                )
            }

            if (schedule.scheduleType == ScheduleType.WEEKLY && schedule.weekDays != 0) {
                textDays.visibility = View.VISIBLE
                textDays.text = formatWeekDays(context, schedule.weekDays)
            } else {
                textDays.visibility = View.GONE
            }

            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = schedule.isEnabled
            switchEnabled.setOnCheckedChangeListener { _, _ ->
                onToggleEnabled(schedule)
            }

            itemView.setOnClickListener {
                onItemClick(schedule)
            }
        }

        private fun formatWeekDays(context: android.content.Context, weekDays: Int): String {
            val days = mutableListOf<String>()
            if (weekDays and SensorSchedule.MONDAY != 0) days.add(context.getString(R.string.day_mon))
            if (weekDays and SensorSchedule.TUESDAY != 0) days.add(context.getString(R.string.day_tue))
            if (weekDays and SensorSchedule.WEDNESDAY != 0) days.add(context.getString(R.string.day_wed))
            if (weekDays and SensorSchedule.THURSDAY != 0) days.add(context.getString(R.string.day_thu))
            if (weekDays and SensorSchedule.FRIDAY != 0) days.add(context.getString(R.string.day_fri))
            if (weekDays and SensorSchedule.SATURDAY != 0) days.add(context.getString(R.string.day_sat))
            if (weekDays and SensorSchedule.SUNDAY != 0) days.add(context.getString(R.string.day_sun))
            return days.joinToString(", ")
        }
    }

    class ScheduleDiffCallback : DiffUtil.ItemCallback<SensorSchedule>() {
        override fun areItemsTheSame(oldItem: SensorSchedule, newItem: SensorSchedule): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SensorSchedule, newItem: SensorSchedule): Boolean {
            return oldItem == newItem
        }
    }
}

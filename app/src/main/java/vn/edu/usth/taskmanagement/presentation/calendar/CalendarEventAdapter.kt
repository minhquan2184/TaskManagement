package vn.edu.usth.taskmanagement.presentation.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import vn.edu.usth.taskmanagement.databinding.ItemCalendarEventBinding
import vn.edu.usth.taskmanagement.service.CalendarEvent

class CalendarEventAdapter : ListAdapter<CalendarEvent, CalendarEventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemCalendarEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(private val binding: ItemCalendarEventBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: CalendarEvent) {
            binding.tvEventTitle.text = event.title

            if (event.isAllDay) {
                binding.tvStartTime.text = "All day"
                binding.tvEndTime.visibility = View.GONE
            } else {
                binding.tvStartTime.text = event.getStartTimeFormatted()
                binding.tvEndTime.text = event.getEndTimeFormatted()
                binding.tvEndTime.visibility = View.VISIBLE
            }

            if (event.description.isNullOrBlank()) {
                binding.tvEventDescription.visibility = View.GONE
            } else {
                binding.tvEventDescription.visibility = View.VISIBLE
                binding.tvEventDescription.text = event.description
            }
        }
    }
}

class EventDiffCallback : DiffUtil.ItemCallback<CalendarEvent>() {
    override fun areItemsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: CalendarEvent, newItem: CalendarEvent): Boolean {
        return oldItem == newItem
    }
}


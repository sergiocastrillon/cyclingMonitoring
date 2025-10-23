package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.permanentStorage.ActivityEntity
import java.text.SimpleDateFormat
import java.util.*

class ActivityAdapter(
    private val activities: List<ActivityEntity>,
    private val onItemClick: (ActivityEntity) -> Unit,
    private val onDeleteClick: (ActivityEntity) -> Unit ):
    RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStartDate: TextView = itemView.findViewById(R.id.tvStartDate)
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        val tvTotalTime: TextView = itemView.findViewById(R.id.tvTotalTime)

        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.history_activity, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = activities[position]
        val date = Date(activity.startTimeStamp)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.tvStartDate.text = sdf.format(date)
        holder.tvDistance.text = "Distancia: %.2f m".format(activity.totalDistance)
        holder.tvTotalTime.text = "Tiempo: " + formatTime(activity.totalTime)

        holder.itemView.setOnClickListener {
            onItemClick(activity)
        }
        holder.btnDelete.setOnClickListener {
            onDeleteClick(activity)
        }
    }

    override fun getItemCount() = activities.size

    private fun formatTime(seconds: Int): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return "%02d:%02d:%02d".format(hrs, mins, secs)
    }
}
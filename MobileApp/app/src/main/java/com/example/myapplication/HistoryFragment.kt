package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.permanentStorage.ActivityEntity

class HistoryFragment : Fragment() {

    private lateinit var viewModel: HistoryViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvActivities = view.findViewById<RecyclerView>(R.id.rvActivities)
        rvActivities.layoutManager = LinearLayoutManager(requireContext())

        viewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)
        viewModel.activities.observe(viewLifecycleOwner) { activities: List<ActivityEntity> ->
            if (activities.isEmpty()) {
                view.findViewById<View>(R.id.tvNoActivities).visibility = View.VISIBLE
                rvActivities.visibility = View.GONE
                return@observe
            } else {
                view.findViewById<View>(R.id.tvNoActivities).visibility = View.GONE
                rvActivities.visibility = View.VISIBLE
            }
            rvActivities.adapter = ActivityAdapter(
                activities,
                onItemClick = { activity ->
                    val fragment = ActivityDetailFragment().apply {
                        arguments = Bundle().apply {
                            putLong("activityId", activity.activityId)
                        }
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                },
                onDeleteClick = { activity ->
                    viewModel.deleteActivity(activity.activityId)
                }
            )
        }
        viewModel.loadActivities()
    }
}
package com.example.myapplication.ui.dashboard

import com.example.myapplication.IncidentActivity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root


        binding.buttonDomesticViolence.setOnClickListener {
            startIncidentActivity("Domestic Violence")
        }

        binding.buttonChildIncident.setOnClickListener {
            startIncidentActivity("Child Incident")
        }

        binding.buttonTrafficViolation.setOnClickListener {
            startIncidentActivity("Traffic Violation")
        }

        binding.buttonAdministrativePolice.setOnClickListener {
            startIncidentActivity("Administrative Police")
        }

        binding.buttonComplainPolice.setOnClickListener {
            startIncidentActivity("Complain Police")
        }

        binding.buttonTerrorismExtremism.setOnClickListener {
            startIncidentActivity("Terrorism Extremism")
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startIncidentActivity(incidentType: String) {
        val intent = Intent(activity, IncidentActivity::class.java).apply {
            putExtra("incidentType", incidentType)
        }
        startActivity(intent)
    }
}

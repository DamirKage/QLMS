package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityIncidentBinding

class IncidentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncidentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncidentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val incidentType = intent.getStringExtra("incidentType")
        binding.incidentTypeTextView.text = "Incident Type: $incidentType"
    }
}

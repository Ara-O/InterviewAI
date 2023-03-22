package com.example.interviewai

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.interviewai.databinding.ActivityInterviewPrepBinding

class InterviewPrep : AppCompatActivity() {
    private lateinit var binding: ActivityInterviewPrepBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityInterviewPrepBinding.inflate(layoutInflater)
        val view = binding.root
        super.onCreate(savedInstanceState)

        var interviewerMood = ""
        binding.criticalButton.setOnClickListener {
            interviewerMood = "Critical"
            binding.criticalButton.setBackgroundColor(Color.DKGRAY)
        }

        binding.niceButton.setOnClickListener {
            interviewerMood = "Nice"
            binding.niceButton.setBackgroundColor(Color.DKGRAY)

        }

         binding.specificButton.setOnClickListener {
            interviewerMood = "Specific"
             binding.specificButton.setBackgroundColor(Color.DKGRAY)

         }


        binding.startInterviewButton.setOnClickListener {
            intent.putExtra("experiences", binding.experiencesInput.text)
            intent.putExtra("job_outlook", binding.jobWishesInput.text)
            intent.putExtra("desired_salary_range", binding.desiredSalaryRangeInput.text)
            intent.putExtra("interviewerMood", binding.experiencesInput.text)
            intent.putExtra("resume_input", binding.resumeInput.text)

            val i = Intent(this, MainActivity::class.java)
            startActivity(i)
            finish()
        }
        setContentView(view)
    }
}
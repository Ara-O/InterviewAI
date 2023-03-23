package com.example.interviewai

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.interviewai.databinding.ActivityInterviewPrepBinding
import java.util.*

class InterviewPrep : AppCompatActivity(){
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


            val i = Intent(this, MainActivity::class.java)
            i.putExtra("experiences", binding.experiencesInput.text.toString())
            i.putExtra("job_outlook", binding.jobWishesInput.text.toString())
            i.putExtra("desired_salary_range", binding.desiredSalaryRangeInput.text.toString())
            i.putExtra("interviewerMood", binding.experiencesInput.text.toString())
            i.putExtra("resume_input", binding.resumeInput.text.toString())
            startActivity(i)
            finish()
        }
        setContentView(view)
    }

}
package com.example.interviewai

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
            binding.criticalButton.setTextColor( getResources().getColor(R.color.purple_theme_color, theme))
            binding.niceButton.setTextColor( getResources().getColor(R.color.white, theme))
            binding.specificButton.setTextColor( getResources().getColor(R.color.white, theme))

        }

        binding.niceButton.setOnClickListener {
            interviewerMood = "Nice"
//            binding.niceButton.ba (getResources().getColor(R.color.teal_200, theme))
            binding.criticalButton.setTextColor( getResources().getColor(R.color.white, theme))
            binding.niceButton.setTextColor( getResources().getColor(R.color.purple_theme_color, theme))
            binding.specificButton.setTextColor( getResources().getColor(R.color.white, theme))

        }

         binding.specificButton.setOnClickListener {
            interviewerMood = "Specific"
             binding.criticalButton.setTextColor( getResources().getColor(R.color.white, theme))
             binding.niceButton.setTextColor( getResources().getColor(R.color.white, theme))
             binding.specificButton.setTextColor( getResources().getColor(R.color.purple_theme_color, theme))


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
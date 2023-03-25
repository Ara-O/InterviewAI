package com.example.interviewai

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.interviewai.databinding.ActivitySignupBinding

class IntroActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        val view = binding.root

        binding.button.setOnClickListener {
            val i = Intent(this, InterviewPrep::class.java)
            startActivity(i)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
        setContentView(view)
    }
}
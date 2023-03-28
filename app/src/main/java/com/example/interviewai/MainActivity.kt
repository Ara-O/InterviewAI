package com.example.interviewai

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData.Item
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.Chat
import com.aallam.openai.client.OpenAI
import com.example.interviewai.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit


class MainActivity : AppCompatActivity(){
    private lateinit var viewBinding: ActivityMainBinding

    @OptIn(BetaOpenAI::class)
    private var chatHistory = mutableListOf<ChatMessage>()
    @OptIn(BetaOpenAI::class)
    private var recyclerViewChatHistory = mutableListOf<ChatMessage>()
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var itemAdapter: ItemAdapter

    @SuppressLint("NotifyDataSetChanged")
    @OptIn(BetaOpenAI::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)

        itemAdapter = ItemAdapter(applicationContext, recyclerViewChatHistory)

        val experiences = intent.getStringExtra("experiences")
        val jobOutlook = intent.getStringExtra("job_outlook")
        val desiredSalaryRange = intent.getStringExtra("desired_salary_range")
        val interviewerMood = intent.getStringExtra("interviewerMood")
        val resumeInput = intent.getStringExtra("resume_input")

        chatHistory.add(ChatMessage(
            role = ChatRole.System,
            content = "You are currently an interviewer for a company and you are about to interview a potential employee; These are the details the employee has given you to prep as an employer. Your only role here is to be the interviewer, and you will ask a question, then wait for my response as a user/interviewer, and the I will give a response and you will give me feedback based on the response I give you and then continue the interview"
        ))

        chatHistory.add(ChatMessage(
            role= ChatRole.User,
            content = "Hi! Nice to meet you, $experiences.\" +\n" +
                    "$jobOutlook. As for my desired salary range, I am looking for a range between $desiredSalaryRange per year, depending on the specifics of the role and the company." +
                    "In terms of my ideal interviewer, I appreciate individuals who are $interviewerMood in their questioning and provide detailed feedback on my responses." +
                    ". While I value kindness and respect, I also believe that constructive criticism is necessary for growth and development"+
                    "I am open to various formats for the interview, including personal and technical surveys, as long as they are conducted in a professional and respectful manner." +
                    "Please find below a summary of my experiences and skills: $resumeInput\" +\n" +
                    ".Conduct your interview with this interviewee. Do not write all the conversation at once. I want you to only do the interview with me. Ask me the questions and wait for my answers. Do not write explanations. Ask me the questions one by one like an interviewer does and wait for my answers. Start with an introduction"
            )
        )

        viewBinding.startCallInterview.setOnClickListener {
            val i = Intent(this, AudioInterviewActivity::class.java)
            startActivity(i)
        }

        val recyclerView = viewBinding.recyclerView
        recyclerView.adapter = itemAdapter

        recyclerViewChatHistory.add(ChatMessage(
            role = ChatRole.Assistant,
            content = "Welcome! Your interview will start shortly with the interviewer " +
                    " asking a question, once the question has been given, you can click the " +
                    "microphone icon to give your response, and once you are done, you can click the microphone icon again to stop talking" +
                    ". The interviewer will look at your response and after a short while, a response will be given. Good luck!"
        ))

        itemAdapter.notifyDataSetChanged()
        startInterview()
        recyclerView.setHasFixedSize(true)


        //Handle user text
        viewBinding.sendResponseButton.setOnClickListener {
            var usersResponse = ChatMessage(
                role = ChatRole.User,
                content =  viewBinding.userResponseText.text.toString()
            )
            viewBinding.userResponseText.text.clear()

            chatHistory.add(usersResponse)
            recyclerViewChatHistory.add(usersResponse)
            itemAdapter.notifyDataSetChanged()
            startInterview()
        }

        recyclerView.itemAnimator = FadeInItemAnimator()
        setContentView(viewBinding.root)
    }



    @OptIn(BetaOpenAI::class)
    private fun startInterview() {
        val openAI = OpenAI(BuildConfig.API_KEY)

        lifecycleScope.launch {
        var interviewersResponse = ""
            val chatCompletionRequest = ChatCompletionRequest(
                model = ModelId("gpt-3.5-turbo"),
                messages = chatHistory
            )

            val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
            interviewersResponse = completion.choices[0].message?.content.toString()

            chatHistory.add(ChatMessage(
                role = ChatRole.Assistant,
                content = interviewersResponse
            ))
            recyclerViewChatHistory.add(ChatMessage(
                role = ChatRole.Assistant,
                content = interviewersResponse
            ))
            itemAdapter.notifyDataSetChanged()
//            speakAIResponse(interviewersResponse)
        }


        }



}
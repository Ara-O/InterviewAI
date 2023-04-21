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
import com.example.interviewai.databinding.ActivityAudioInterviewBinding
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


    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private lateinit var viewBinding: ActivityMainBinding
    private var mediaFile: File? = null
    @OptIn(BetaOpenAI::class)
    private var recordingStopped: Boolean = false
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
                    " asking a question, once the question has been given, you can write your answer in the text space below." +
                    "Once you are done, you can click the send icon to get a response" +
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


        viewBinding.microphoneButton.setOnClickListener {
            lifecycleScope.launch{
            recordUserAudio()
            }
        }
        recyclerView.itemAnimator = FadeInItemAnimator()
        setContentView(viewBinding.root)
    }


    @OptIn(BetaOpenAI::class)
    private suspend fun recordUserAudio() {
        try {
            if(state){
                mediaRecorder?.stop()
                mediaRecorder?.release()
                state = false
                Toast.makeText(this, "Recording stopped!", Toast.LENGTH_SHORT).show()
                val openAI = OpenAI(BuildConfig.API_KEY)

                val request = TranscriptionRequest(
                    audio = FileSource(mediaFile?.absolutePath?.toPath()!!, fileSystem = FileSystem.SYSTEM),
                    model = ModelId("whisper-1"),
                )

                Toast.makeText(applicationContext, "Loading...Please wait", Toast.LENGTH_SHORT).show()
                val transcription = openAI.transcription(request)


                Log.d("AUDIO FILE HERe", transcription.text)
                viewBinding.userResponseText.append(transcription.text)

            }else{
                if (Build.VERSION.SDK_INT >= 31) {
                    Log.d("VERSION", "greater than 31")
                    mediaRecorder = MediaRecorder(this)
                } else {
                    Log.d("VERSION", "less than 31")
                    mediaRecorder = MediaRecorder()
                }
                Log.d("cellphone", "onCreate: Should BE STARTIG")
                mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
                mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                val internalStorageDir = filesDir
                val timeStamp: String = android.icu.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//            mediaFile =   File(mediaStorageDir.path + File.separator + "AUD_" + timeStamp + ".m4a")
                mediaFile =
                    File(internalStorageDir.path + File.separator + "AUD_" + timeStamp + ".m4a")
                val fileOutputStream = FileOutputStream(mediaFile)
                mediaRecorder!!.setOutputFile(fileOutputStream.fd)

                mediaRecorder!!.prepare()
                mediaRecorder!!.start()

                state = true
                Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
            }
        }catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                android.Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
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

        }

        }



}
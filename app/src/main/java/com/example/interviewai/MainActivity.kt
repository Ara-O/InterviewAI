package com.example.interviewai

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData.Item
import android.content.ContentValues
import android.content.Context
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

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var viewBinding: ActivityMainBinding
    private var tts: TextToSpeech? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private var mediaFile: File? = null
    private var recordingStopped: Boolean = false
    //    private var imageCapture: ImageCapture? = null
//    private var videoCapture: VideoCapture<Recorder>? = null
//    private var recording: Recording? = null
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

//        if (ContextCompat.checkSelfPermission(this,
//                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
//                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
//            ActivityCompat.requestPermissions(this, permissions,0)
//            Log.d("PERMS", "Permissions not granted")
//        }
//
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

//
//        tts = TextToSpeech(this, this)
//
//
//        viewBinding.audioCaptureButton.setOnClickListener {
//            try {
//                if(state){
//                    Log.d("cellphone", "onCreate: Should BE STOPPING")
//                    mediaRecorder?.stop()
//                    mediaRecorder?.release()
//                    state = false
//                    Toast.makeText(this, "Recording stopped!", Toast.LENGTH_SHORT).show()
//                    //Convert recording to text
//                    lifecycleScope.launch{
//                        convertSpeechToText()
//                    }
//                }else{
//
//                    if (Build.VERSION.SDK_INT >= 31) {
//                        Log.d("VERSION", "greater than 31")
//                        mediaRecorder = MediaRecorder(this)
//                    } else {
//                        Log.d("VERSION", "less than 31")
//                        mediaRecorder = MediaRecorder()
//                    }
//                    Log.d("cellphone", "onCreate: Should BE STARTIG")
//                mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
//                mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
//                mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
//
//                val internalStorageDir = filesDir
//                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
////            mediaFile =   File(mediaStorageDir.path + File.separator + "AUD_" + timeStamp + ".m4a")
//                mediaFile =
//                    File(internalStorageDir.path + File.separator + "AUD_" + timeStamp + ".m4a")
//                val fileOutputStream = FileOutputStream(mediaFile)
//                mediaRecorder!!.setOutputFile(fileOutputStream.fd)
//
//                mediaRecorder!!.prepare()
//                mediaRecorder!!.start()
//
//                state = true
//                Toast.makeText(this, "Recording started!", Toast.LENGTH_SHORT).show()
//                }
//            }catch (e: IllegalStateException) {
//                e.printStackTrace()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }


        val recyclerView = viewBinding.recyclerView
        recyclerView.adapter =itemAdapter

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
    private suspend fun convertSpeechToText() {
        val openAI = OpenAI(BuildConfig.API_KEY)

        val request = TranscriptionRequest(
            audio = FileSource(mediaFile?.absolutePath?.toPath()!!, fileSystem = FileSystem.SYSTEM),
            model = ModelId("whisper-1"),
        )

        val transcription = openAI.transcription(request)
        chatHistory.add(ChatMessage(
            role = ChatRole.User,
            content =  transcription.text
        ))


        Log.d("AUDIO FILE HERe", transcription.text)

        runOnUiThread{
//            viewBinding.interviewHistoryText.append("You:\n${transcription.text}\n\n")
        }

        startInterview()
    }



    override fun onDestroy() {
        super.onDestroy()
        if(tts != null){
            tts!!.shutdown()
        }
//        cameraExecutor.shutdown()
    }


    override fun onInit(status: Int) {
        //If TTS has been loaded successfully
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            tts!!.setSpeechRate(0.9f)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d("TTS","The Language not supported!")
            } else {
                Log.d("TTS","The Language is supported!")
                tts!!.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {
                        runOnUiThread {
                            viewBinding.audioCaptureButton.isEnabled = false
                        }
                        if(utteranceId == "welcomeText"){
                            Log.d("Started", "Welcome text has started")
                        }
                    }

                    override fun onDone(utteranceId: String) {
                        runOnUiThread{
//                            viewBinding.audioCaptureButton.isEnabled = true
//                            viewBinding.interviewerThinking.setText("Interviewer is listening")
                        }
                        if(utteranceId == "welcomeText"){
                            startInterview()
                        }
                    }

                    override fun onError(utteranceId: String) {
                        // Code to execute if there is an error
                        Log.e("error", "error in utteranceProgressListener")
                    }
                });

//                When TextToSpeech has processed, start functionality
                val welcomeText =  "\"Welcome! Your interview will start shortly with the interviewer " +
                        " asking a question, once the question has been given, you can click the " +
                        "microphone icon to give your response, and once you are done, you can click the microphone icon again to stop talking" +
                        ". The interviewer will look at your response and after a short while, a response will be given. Good luck!"
                tts!!.speak(welcomeText, TextToSpeech.QUEUE_FLUSH, null, "welcomeText")

            }
        }
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

    @OptIn(BetaOpenAI::class)
    private fun speakAIResponse(response: String){
        Log.d("speak", "Speak AI response")
        val recyclerView = viewBinding.recyclerView
        recyclerView.adapter = ItemAdapter(this, chatHistory)
        recyclerView.setHasFixedSize(true)

//        viewBinding.interviewerThinking.setText("Interviewer is Talking")
        tts!!.speak(response, TextToSpeech.QUEUE_FLUSH, null, "")
    }

}
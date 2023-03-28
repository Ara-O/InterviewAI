package com.example.interviewai

import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.interviewai.databinding.ActivityAudioInterviewBinding
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class AudioInterviewActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private lateinit var viewBinding: ActivityAudioInterviewBinding
    private var mediaFile: File? = null
    @OptIn(BetaOpenAI::class)
    private var chatHistory = mutableListOf<ChatMessage>()
    private var recordingStopped: Boolean = false
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityAudioInterviewBinding.inflate(layoutInflater)

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions,0)
            Log.d("PERMS", "Permissions not granted")
        }

        //Setting up text to speech
        tts = TextToSpeech(this, this)


        viewBinding.audioCaptureButton.setOnClickListener {
            recordUserAudio()
        }

        startInterview()
        setContentView(viewBinding.root)
    }

    private fun recordUserAudio() {
        try {
            if(state){
                Log.d("cellphone", "onCreate: Should BE STOPPING")
                mediaRecorder?.stop()
                mediaRecorder?.release()
                state = false
                Toast.makeText(this, "Recording stopped!", Toast.LENGTH_SHORT).show()
                //Convert recording to text
                lifecycleScope.launch{
                    convertSpeechToText()
                }
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
                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
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



    @OptIn(BetaOpenAI::class)
    private suspend fun convertSpeechToText() {
        val openAI = OpenAI(BuildConfig.API_KEY)

        val request = TranscriptionRequest(
            audio = FileSource(mediaFile?.absolutePath?.toPath()!!, fileSystem = FileSystem.SYSTEM),
            model = ModelId("whisper-1"),
        )

        val transcription = openAI.transcription(request)
        chatHistory.add(
            ChatMessage(
            role = ChatRole.User,
            content =  transcription.text
        )
        )

        Log.d("AUDIO FILE HERe", transcription.text)

        runOnUiThread{
//           viewBinding.interviewHistoryText.append("You:\n${transcription.text}\n\n")
        }

        startInterview()
    }

    private fun startInterview() {
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
    private fun speakAIResponse(response: String){
        Log.d("speak", "Speak AI response")
        viewBinding.interviewerThinking.setText("Interviewer is Talking")
        tts!!.speak(response, TextToSpeech.QUEUE_FLUSH, null, "")
    }

}
package com.example.interviewai

import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.audio.TranscriptionRequest
import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.example.interviewai.databinding.ActivityVoiceToTextTestBinding
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class VoiceToTextTest : AppCompatActivity() {
    private lateinit var binding: ActivityVoiceToTextTestBinding
    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var state: Boolean = false
    private var mediaFile: File? = null
    private var recordingStopped: Boolean = false
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(BetaOpenAI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceToTextTestBinding.inflate(layoutInflater)

        binding.startSpeak.setOnClickListener {
            startRecording()
        }

        binding.stopSpeak.setOnClickListener {
            stopRecording()
        }


        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions,0)
            Log.d("PERMS", "Permissions not granted")
        }

// TODO(Fix where to output file, getExternalStorageDirectory is deprecated in api 29 and accessing it doesnt work)
//        val output = File(filesDir, "test.mp3")

//        output = Environment.getExternalStorageDirectory().absolutePath + "/Android/data/thisisarecording.mp3"
//        getExternalFilesDir("")
//        output = File(filesDir, "user-file-recording")


        setContentView(binding.root)

    }
    private fun startRecording() {
        try {
            Log.d("UTPUT", output.toString())
            if (Build.VERSION.SDK_INT >= 31) {
                Log.d("VERSION", "greater than 31")
                mediaRecorder = MediaRecorder(this)
            }else{
                Log.d("VERSION", "less than 31")
                mediaRecorder = MediaRecorder()
            }

            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            val internalStorageDir = filesDir


//            val mediaStorageDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "MyApp")
//            if (!mediaStorageDir.exists()) {
//                if (!mediaStorageDir.mkdirs()) {
//                    Log.d("MyApp", "failed to create directory")
//                }
//            }

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
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun stopRecording(){
        if(state){
            mediaRecorder?.stop()
            mediaRecorder?.release()
            state = false

            //Convert recording to text
            lifecycleScope.launch{
                convertSpeechToText()
            }
        }else{
            Toast.makeText(this, "You are not recording right now!", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(BetaOpenAI::class)
    private suspend fun convertSpeechToText() {
        val openAI = OpenAI(BuildConfig.API_KEY)

//        val path =
//            Uri.parse(Environment.getExternalStorageDirectory().absolutePath + "/Android/data/thisisarecording.mp3")

        val request = TranscriptionRequest(
            audio = FileSource(mediaFile?.absolutePath?.toPath()!!, fileSystem = FileSystem.SYSTEM),
            model = ModelId("whisper-1"),
        )

        val transcription = openAI.transcription(request)
//
//
        Log.d("AUDIO FILE HERe", transcription.text)
//
//        lifecycleScope.launch{
//            val transcription = openAI.transcription(request)
//        }


        //        val openAI = OpenAI(BuildConfig.API_KEY)
//
//        val audioFile = File(filesDir, "recording.wav")
//
//
//        val request = TranscriptionRequest(
//            audio = FileSource(name = "audio-recording-001", source = audioSource),
//            model = ModelId("whisper-1"),
//        )
//
//        lifecycleScope.launch{
//            val transcription = openAI.transcription(request)
//        }
    }
}
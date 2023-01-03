package com.android.vadify.ui.chat

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.databinding.ActivityChatBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.google.android.material.textview.MaterialTextView
import kotlinx.android.synthetic.main.activity_chat.*


abstract class SpeechRecoganization : DataBindingActivity<ActivityChatBinding>(),
     RecognitionListener {

    var timer: CountDownTimer? = null
    var view: View? = null
    var progressBar: ProgressBar? = null
    var counterView: MaterialTextView? = null
    var speech: SpeechRecognizer? =null
    lateinit var recognizerIntent:Intent
    val viewModel: ChatViewModel by viewModels()
    //    abstract  val listeningLayout:(Boolean) -> Unit
    val viewModelLanguage: ChatViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Speech.init(this, packageName)
        //Speech.init(this, packageName)
        view = findViewById(R.id.toast_layout)
        progressBar = view?.findViewById<View>(R.id.progressBar) as ProgressBar
        counterView = view?.findViewById<View>(R.id.counter) as MaterialTextView

       languageChange(viewModelLanguage.checkLanguageCodeForSpeech())

       }


    override fun onResume() {
        super.onResume()

    }

    fun languageChange(languageCode: String)
    {
        Log.d( "languageChange: ",languageCode)
        recognizerIntent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        speech = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        speech?.setRecognitionListener(this)
    }
    fun speechToText(languageCode: String?) {
        try {
            when {
                // Speech.getInstance()?.isListening ?: false -> Speech.getInstance()?.stopListening()
                isAllGranted(Permission.RECORD_AUDIO) -> onRecordAudioPermissionGranted(languageCode)
                else -> {
                    runWithPermissions(Permission.RECORD_AUDIO) {
                        onRecordAudioPermissionGranted(languageCode)
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    fun onRecordAudioPermissionGranted(languageCode: String?) {
        try {
            mic.setImageResource(R.drawable.ic_mic_red)
            ChatActivity.isMicOn =true
            ChatActivity.isListenCall = false
            listening_input.hint = resources.getString(R.string.start_talking_i_m_listening)
            AnimationUtils.loadAnimation(applicationContext, R.anim.blink_image)
                .also { mic.startAnimation(it) }

            startCommand(languageCode, true,AudioManager.ADJUST_UNMUTE)
            //muteBeepSoundOfRecorder(false)


            /* Speech.getInstance()?.stopTextToSpeech()
            Speech.getInstance()?.startListening(this)
            Speech.getInstance()?.setListener(this)
        } catch (exc: SpeechRecognitionNotAvailable) {
            exc.printStackTrace()
//            showMessage(resources.getString(R.string.speechRecoganizationProcess))
        } catch (exc: GoogleVoiceTypingDisabledException) {
            exc.printStackTrace()
//            showMessage(resources.getString(R.string.speechRecoganizationProcess))
        }*/

        } catch (e: Exception) {
            e.printStackTrace()
//            showMessage(resources.getString(R.string.speechRecoganizationProcess))
        }
    }

    fun startCommand(languageCode: String?, firstTime: Boolean,Mutevalue:Int) {
        muteBeepSoundOfRecorder(!firstTime,Mutevalue)
        speech?.startListening(recognizerIntent)
    }







    /**
     * Function to remove the beep sound of voice recognizer.
     */

    fun muteBeepSoundOfRecorder(volume: Boolean, mutevalue :Int) {
        try {
            val mAlramMAnager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, mutevalue, 0)
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_ALARM, mutevalue, 0)
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_MUSIC, mutevalue, 0)
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_RING, mutevalue, 0)
            mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, mutevalue, 0)
        } catch (e: Exception) {
        }

    }

    // Todo Hide functionality as client requirement
    fun callTimerCountMethod() {
        if (timer == null) {
            var counter = 1
            val totalCounter = 10
            AnimationUtils.loadAnimation(applicationContext, R.anim.blink_image)
                .also { mic.startAnimation(it) }
            timer = object : CountDownTimer(ChatActivity.TIMER, ChatActivity.DELAY) {
                override fun onTick(millisUntilFinished: Long) {
                    if (millisUntilFinished < 10000) {
                        view?.visibility = View.VISIBLE
                        progressBar?.progress = counter
                        counterView?.text = totalCounter.minus(counter).toString()
                        counter++
                    }
                }

                override fun onFinish() {
                    stopTimerSpeechRecoganization()
                }
            }
            timer?.start()
        }
    }

    fun stopTimerSpeechRecoganization() {
        try {
            timer = null
            mic.clearAnimation()
            view?.visibility = View.GONE
            } catch (e: Exception) {
            Log.e("issue", "" + e.message.toString())
        }
    }

    fun stopSpeech() {
        if (speech!=null) {
            speech?.stopListening()
            speech?.cancel()
        }
        muteBeepSoundOfRecorder(true,AudioManager.ADJUST_UNMUTE)

    }

    fun stopSpeechRecoganization() {
        listening_input.hint = resources.getString(R.string.type_message)
        //Speech.getInstance()?.setListener(null)
        // Speech.getInstance()?.stopListening()



        stopSpeech()

        try {
            val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            manager.mode = AudioManager.MODE_NORMAL
            //manager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            //manager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0)
        } catch (e: SecurityException) {
        }

        mic.clearAnimation()
    }

    override fun onReadyForSpeech(params: Bundle?) {

    }

    override fun onRmsChanged(rmsdB: Float) {

    }

    override fun onBufferReceived(buffer: ByteArray?) {

    }

    override fun onPartialResults(partialResults: Bundle?) {

    }

    override fun onEvent(eventType: Int, params: Bundle?) {

    }

    override fun onBeginningOfSpeech() {


    }

    override fun onEndOfSpeech() {
        Log.d("callbackOfSpeech","onEndOfSpeech: ")
    }



    /*override fun onResults(results: Bundle?) {

        speech = SpeechRecognizer.createSpeechRecognizer(applicationContext)
        speech.setRecognitionListener(this)
        speech.startListening()
    }*/

}
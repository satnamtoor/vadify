package com.android.vadify.ui.login.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.Color
import android.media.AudioManager
import android.os.*
import android.os.ParcelFileDescriptor.open
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.data.api.models.LocalLanguageModel
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.databinding.ActivityChatBinding
import com.android.vadify.databinding.CommandRegistrationBinding
import com.android.vadify.ui.baseclass.BaseDaggerFragment
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.utils.SpeechRecognization
import com.android.vadify.viewmodels.CommandsViewModel
import com.google.android.material.textview.MaterialTextView
import com.google.common.collect.Range.open
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.microsoft.appcenter.utils.HandlerUtils.runOnUiThread
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.command_registration.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import java.io.File
import java.io.IOException
import java.nio.channels.AsynchronousSocketChannel.open
import java.util.*

abstract class CommandsSpeech : BaseDaggerFragment<CommandRegistrationBinding>(),
    RecognitionListener {

    val viewModel: ChatViewModel by viewModels()
    val viewCommand: CommandsViewModel by viewModels()
    var commandNames: ArrayList<String>? = null
    lateinit var defaultSharedPreferences: SharedPreferences
    var language: String? = null
    var languageCode: String? = null
    lateinit var handler: Handler
    var speech: SpeechRecognizer? = null
    lateinit var recognizerIntent: Intent
    val viewModels: ProfileFragmentViewModel by viewModels()
    private var isLast = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler(Looper.getMainLooper())
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (arguments?.getString("RETAKE")=="YES") {
            language = defaultSharedPreferences.getString("pkey_each_command_Language_retake", "")
            languageCode = defaultSharedPreferences.getString("pkey_each_command_Code_retake", "")



        }
        else{
            language = defaultSharedPreferences.getString("pkey_each_command_Language", "")
            languageCode = defaultSharedPreferences.getString("pkey_each_command_Code", "")
        }
        Log.d( "command_language ","language "+language +" languageCode " +languageCode)
        if (language.equals("") && languageCode.equals(""))
        {
            language="English"
            languageCode="en"
        }
        commandNames = ArrayList();
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        speech = SpeechRecognizer.createSpeechRecognizer(requireActivity())
        speech?.setRecognitionListener(this)
    }


    data class CommandData(
        var language: String,
        var commandName: String,
        var languageCode: String,
        var command1: String?,
        var command2: String?,
        var command3: String?
    )

    fun speechToText() {
        try {
            when {
                isAllGranted(Permission.RECORD_AUDIO) -> onRecordAudioPermissionGranted()
                else -> {
                    runWithPermissions(Permission.RECORD_AUDIO) {
                        onRecordAudioPermissionGranted()
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    fun onRecordAudioPermissionGranted() {
        try {
            startCommand()
        } catch (e: Exception) {
            showMessage(resources.getString(R.string.speechRecoganizationProcess))
        }
    }

    fun startCommand(mute: Boolean = true) {
        txtRecording.text = "Recording..."

            AnimationUtils.loadAnimation(activity, R.anim.blink_image)
                .also {
                    if(btnAudio.animation==null) {

                        btnAudio.startAnimation(it)

                    }
                }

            Log.d("language-code: ", "" + languageCode)
        txtLater.visibility = View.INVISIBLE
        if (mute)
            muteBeepSoundOfRecorder(AudioManager.ADJUST_UNMUTE, false)
        speech?.startListening(recognizerIntent)

    }

    fun stepscompleted(step: Int) {
        when (step) {
            1 -> {
                imageRecording.setImageResource(R.drawable.ic_check_audo_button)
            }
            2 -> {
                imageRecording2.setImageResource(R.drawable.ic_check_audo_button)
            }
            3 -> {
                imageRecording3.setImageResource(R.drawable.ic_check_audo_button)
                btnAudio.clearAnimation()
                txtRecording.text = "Retake"
                txtLater.visibility = View.VISIBLE
                txtLater.text = "Next"

                if (txtLater.text.equals("Next")) {
                    var mCommand = Commands(
                        language = language!!,
                        commandName = txtMayaCommand.text.toString(),
                        languageCode = languageCode!!,
                        command1 = commandNames?.get(0),
                        command2 = commandNames?.get(1),
                        command3 = commandNames?.get(2)
                    )
                    viewCommand.insert(mCommand)
                    if (txtMayaCommand.text.toString() != "MAYA CLOSE")
                        commandNames?.clear()
                    bindNetworkState(
                        viewModels.saveCommandData(mCommand),
                        progressDialog(R.string.updating)
                    )
                    {
                        txtLater.visibility = View.INVISIBLE
                        if (mCommand.commandName == "MAYA SEND") {
                            step2?.setBackgroundResource(R.drawable.ic_ellipse_blue);
                            step2.setTextColor(Color.parseColor("#FFFFFF"))
                            sendNextData("MAYA LISTEN")
                        } else if (mCommand.commandName == "MAYA LISTEN") {
                            step3?.setBackgroundResource(R.drawable.ic_ellipse_blue);
                            step3.setTextColor(Color.parseColor("#FFFFFF"))
                            sendNextData("MAYA OFF")
                        } else if (mCommand.commandName == "MAYA OFF") {
                            step4?.setBackgroundResource(R.drawable.ic_ellipse_blue);
                            step4.setTextColor(Color.parseColor("#FFFFFF"))
                            sendNextData("MAYA BYE")
                        } else if (mCommand.commandName == "MAYA BYE") {
                            step5?.setBackgroundResource(R.drawable.ic_ellipse_blue);
                            step5.setTextColor(Color.parseColor("#FFFFFF"))
                            sendNextData("MAYA CLOSE")
                        } else {

                        }
                    }
                }
            }
        }
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
        muteBeepSoundOfRecorder(AudioManager.ADJUST_MUTE, true)
    }

    override fun onEndOfSpeech() {
        if (commandNames?.size == 2) {
            muteBeepSoundOfRecorder(AudioManager.ADJUST_UNMUTE, false)
        }
    }

    override fun onError(error: Int) {
        try {
            GlobalScope.launch {
                delay(50)
                runOnUiThread {
                    if (!(txtMayaCommand.text.toString() == "MAYA CLOSE" && commandNames!!.size == 3))
                        startCommand(false)
                }
            }
        } catch (ex: Exception) {
        }

    }

    override fun onResults(results: Bundle?) {
        val matches = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches!![0]
        if (!text.isNullOrBlank()) {
            Log.d("speech-result: ", text)
            commandNames?.add(text)
            stepscompleted(commandNames!!.size)
            handler.postDelayed({
                commandNames?.let {
                        btnAudio.clearAnimation()
                        if (txtMayaCommand.text.toString() == "MAYA CLOSE" && commandNames!!.size == 3)
                        {
                            speech?.cancel()
                            speech?.destroy()
                            btnAudio.clearAnimation()
                            txtRecording.text = ""
                            txtLater.visibility = View.VISIBLE
                            txtLater.text = "Done"
                            isLast = true
                            command_congratulation.visibility = View.VISIBLE
                        }
                        else
                            startCommand()
                }
            }, 200)
        } else
            Log.d("onSpeechResult: ", "blank")
    }

    fun muteBeepSoundOfRecorder(mutevalue: Int, isMute: Boolean) {
        try {
            val mAlramMAnager =
                context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, mutevalue, 0)
                mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_ALARM, mutevalue, 0)
                mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_MUSIC, mutevalue, 0)
                mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_RING, mutevalue, 0)
                mAlramMAnager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, mutevalue, 0)
            } else {
                mAlramMAnager.setStreamMute(AudioManager.STREAM_MUSIC, isMute)
                mAlramMAnager.setStreamMute(AudioManager.STREAM_NOTIFICATION, isMute)
                mAlramMAnager.setStreamMute(AudioManager.STREAM_ALARM, isMute)
                mAlramMAnager.setStreamMute(AudioManager.STREAM_RING, isMute)
                mAlramMAnager.setStreamMute(AudioManager.STREAM_SYSTEM, isMute)

            }

        } catch (e: Exception) {
        }

    }
    fun sendNextData(text: String) {
        txtMayaCommand.text = text
        txtRecording.text = "Recording..."
        imageRecording.setImageResource(R.drawable.ic_audio_btn__complete)
        imageRecording2.setImageResource(R.drawable.ic_audio_btn__complete)
        imageRecording3.setImageResource(R.drawable.ic_audio_btn__complete)
        speechToText()
    }

    override fun onResume() {
        super.onResume()
        Log.d( "command_language ","language "+language +" languageCode " +languageCode)
    }
}
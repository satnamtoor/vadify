/*
package com.android.vadify.ui.chat

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.data.api.models.CommandsRequestModel
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.databinding.CommandInChatBinding
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.baseclass.BaseDaggerFragment
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.viewmodels.CommandsViewModel
import com.google.gson.Gson
import com.sac.speech.GoogleVoiceTypingDisabledException
import com.sac.speech.Speech
import com.sac.speech.SpeechDelegate
import com.sac.speech.SpeechRecognitionNotAvailable
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import kotlinx.android.synthetic.main.command_in_chat.*
import kotlinx.android.synthetic.main.command_in_chat.btnAudio
import kotlinx.android.synthetic.main.command_in_chat.imageRecording
import kotlinx.android.synthetic.main.command_in_chat.imageRecording2
import kotlinx.android.synthetic.main.command_in_chat.imageRecording3
import kotlinx.android.synthetic.main.command_in_chat.step2
import kotlinx.android.synthetic.main.command_in_chat.step3
import kotlinx.android.synthetic.main.command_in_chat.step4
import kotlinx.android.synthetic.main.command_in_chat.step5
import kotlinx.android.synthetic.main.command_in_chat.txtLater
import kotlinx.android.synthetic.main.command_in_chat.txtMayaCommand
import kotlinx.android.synthetic.main.command_in_chat.txtRecording
import kotlinx.android.synthetic.main.command_registration.*
import java.util.*
import kotlin.collections.ArrayList

class CommandFragmentInChat : BaseDaggerFragment<CommandInChatBinding>(), SpeechDelegate,
        Speech.stopDueToDelay, View.OnKeyListener {
    override val layoutRes: Int
        get() = R.layout.command_in_chat

    var delegate: SpeechDelegate? = null
    lateinit var audioManager: AudioManager
    var commanData: ArrayList<String?> = ArrayList()
    var isRecording = false
    val viewModel: ProfileFragmentViewModel by viewModels()
    val commandViewModel: CommandsViewModel by viewModels()

    var commandNames: ArrayList<String>? = null
    private var voiceTimer: CountDownTimer? = null

    //   val viewModel: CommandsViewModel by viewModels()
    lateinit var defaultSharedPreferences: SharedPreferences
    var language: String? = null

    lateinit var handler: Handler
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler = Handler()
        audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager;
        defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity)
        commandNames = ArrayList();

        language = defaultSharedPreferences.getString("pkey_motherLanguage", "")
        //  commandViewModel =
        //    ViewModelProvider(this).get(CommandsViewModel::class.java)
        commandViewModel.data.observe(requireActivity(), androidx.lifecycle.Observer {
            it.forEach {
                val mCommandResust = Commands(
                    it.language,
                    it.commandName,
                    it.languageCode,
                    it.command1?.toLowerCase(),
                    it.command2?.toLowerCase(),
                    it.command3?.toLowerCase()
                )
                bindNetworkState(
                        viewModel.saveCommandData(mCommandResust),
                        progressDialog(R.string.updating)
                )
                {
                    txtLater.visibility = View.INVISIBLE

                    if (mCommandResust.commandName == "MAYA SEND") {

                        step2?.setBackgroundResource(R.drawable.ic_ellipse_blue);


                        step2.setTextColor(Color.parseColor("#FFFFFF"))
                        sendNextData("MAYA LISTEN")
                    } else if (mCommandResust.commandName == "MAYA LISTEN") {
                        step3?.setBackgroundResource(R.drawable.ic_ellipse_blue);
                        step3.setTextColor(Color.parseColor("#FFFFFF"))
                        sendNextData("MAYA OFF")
                    } else if (mCommandResust.commandName == "MAYA OFF") {
                        */
/*step4.setBackgroundTintList(
                            activity?.getResources()?.getColorStateList(R.color.blue)
                        );*//*

                        step4?.setBackgroundResource(R.drawable.ic_ellipse_blue);
                        step4.setTextColor(Color.parseColor("#FFFFFF"))
                        sendNextData("MAYA BYE")
                    } else if (mCommandResust.commandName == "MAYA BYE") {

                        step5?.setBackgroundResource(R.drawable.ic_ellipse_blue);
                        step5.setTextColor(Color.parseColor("#FFFFFF"))
                        sendNextData("MAYA CLOSE")
                    } else {
                        command_congratulation1.visibility = View.VISIBLE

                    }
                }
            }

           // Log.d("onCreatefdx", Gson().toJson(it).toString())
        })


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view?.setOnKeyListener(this)

        subscribe(command_congratulation1.throttleClicks()) {
            command_congratulation1.visibility = View.GONE
            txtLater.visibility = View.VISIBLE
            txtLater.setText("Done")


        }

        btnAudio.setOnClickListener {
            txtLater.visibility = View.INVISIBLE
            speechToText()
            //  txtRecording.setText("Recording...")
        }


        txtRecording.setOnClickListener {
            if (txtRecording.text.equals("Retake")) {
                txtLater.visibility = View.INVISIBLE
                speechToText()
            }
        }

        txtLater.setOnClickListener {


            commanData.clear()
            if (txtLater.text.equals("Next")) {
                val defaultSharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(activity)
                val language = defaultSharedPreferences.getString("pkey_motherLanguage", "")
                if (language != null) {
                    commandViewModel.getCommand(txtMayaCommand.text.toString(), language)
                }
            } else {
                if (txtLater.text.equals("Done")) {
                    isRecording = false
                    requireActivity().supportFragmentManager.beginTransaction()
                            .remove(requireActivity().supportFragmentManager.findFragmentById(R.id.command_container)!!)
                            .commit()
                    stopSpeechRecoganization()
                } else {

                    // Speech.getInstance()?.stopTextToSpeech()
                    isRecording = false
                    ChatActivity.isHasValue = true
                    stopSpeechRecoganization()
                    requireActivity().supportFragmentManager.beginTransaction()
                        .remove(requireActivity().supportFragmentManager.findFragmentById(R.id.command_container)!!)
                        .commit()
                   // activity?.onBackPressed()
                  //  activity?.finish()
                   // BaseBackStack.GOTO_SPEECH_TO_TEXT=false
                }
            }


        }

    }


    override fun onDestroy() {
        super.onDestroy()


    }

    override fun onPause() {
        super.onPause()
        val defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
        val language = defaultSharedPreferences.getString("pkey_motherLanguage", "")
        if (language != null) {
            (activity as ChatActivity).commandViewModel.getCommandAll(language)
        }

        (activity as ChatActivity).initAlls()

        //stopVoiceRecord()

        */
/*  Speech.getInstance()?.stopTextToSpeech()
          stopSpeechRecoganization()
          Speech.getInstance()?.shutdown()*//*


        //releaseRecording()
        //disconnectMethod()

        Log.d("onPause", "called")
    }

    */
/*  private fun releaseRecording() {
          try {
              mRecorder?.stop()
              mRecorder?.release()
              mRecorder = null
          } catch (e: Exception) {
          }
      }
  *//*

    */
/*  private fun stopVoiceRecord() {
          voiceTimer?.cancel()
          voiceTimer = null
          viewModel.isSpeechLanguageEnable.value = ChatType.NORMAL
          releaseRecording()
      }*//*

    private fun showDialog(commanData: ArrayList<String?>) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Maya commands")

        val dataAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line, commanData
        )
        builder.setPositiveButton("Okay") { dialog, whichButton ->

            // if (txtMayaCommand.text.e)
            if (txtMayaCommand.text.toString() == "MAYA SEND") {

                sendNextData("MAYA LISTEN")
            } else if (txtMayaCommand.text.toString() == "MAYA LISTEN") {
                sendNextData("MAYA OFF")
            } else if (txtMayaCommand.text.toString() == "MAYA OFF") {
                sendNextData("MAYA BYE")
            } else if (txtMayaCommand.text.toString() == "MAYA BYE") {
                sendNextData("MAYA CLOSE")
            } else {
                //   activity?.finish()
                isRecording = false
                requireActivity().supportFragmentManager.beginTransaction()
                        .remove(requireActivity().supportFragmentManager.findFragmentById(R.id.command_container)!!)
                        .commit()
            }
        }
        builder.setAdapter(
                dataAdapter
        ) { dialog, which ->
        }

        val dialog: AlertDialog = builder.create()
        // dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0);
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(R.color.blue));
    }

    fun sendNextData(text: String) {
        txtMayaCommand.text = text
        imageRecording.setImageResource(R.drawable.ic_audio_btn__complete)
        imageRecording2.setImageResource(R.drawable.ic_audio_btn__complete)
        imageRecording3.setImageResource(R.drawable.ic_audio_btn__complete)
        speechToText()
    }

    fun speechToText() {
        try {
            when {
                Speech.getInstance()?.isListening ?: false -> Speech.getInstance()?.stopListening()
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
            isRecording = true
            txtRecording.setText("Recording...")
            //  listening_input.hint = resources.getString(R.string.start_talking_i_m_listening)
            AnimationUtils.loadAnimation(activity, R.anim.blink_image)
                    .also { btnAudio.startAnimation(it) }
            startCommand()
            */
/*   Speech.getInstance()?.stopTextToSpeech()
               Speech.getInstance()?.startListening(this)
               Speech.getInstance()?.setListener(this)*//*

        } catch (exc: SpeechRecognitionNotAvailable) {
            showMessage(resources.getString(R.string.speechRecoganizationProcess))
        } catch (exc: GoogleVoiceTypingDisabledException) {
            showMessage(resources.getString(R.string.speechRecoganizationProcess))
        } catch (e: Exception) {
            showMessage(resources.getString(R.string.speechRecoganizationProcess))
        }
    }


    override fun onStartOfSpeech() {

    }

    override fun onSpeechRmsChanged(value: Float) {
    }

    override fun onSpeechPartialResults(results: MutableList<String>?) {
    }

    override fun onSpeechResult(result: String?) {
        if (!result.isNullOrBlank()) {
            Log.d("onSpeechResult: ", result)
            commandNames?.add(result)

            if (commandNames?.size == 3) {
                var mCommand = Commands(
                    language = language!!,
                    commandName = txtMayaCommand.text.toString(),
                    languageCode = "",
                    command1 = commandNames?.get(0),
                    command2 = commandNames?.get(1),
                    command3 = commandNames?.get(2)
                )

                commandViewModel.insert(mCommand)

            }
        }
        stopSpeechRecoganization()

        handler.postDelayed(object : Runnable {
            override fun run() {
                commandNames?.let {
                    Log.d("fytytr", "run: " + it.size)
                    if (it.size != 0) {
                        speechToText()
                        try {
                            txtRecording.setText("Say again...")

                        } catch (e: Exception) {

                        }
                    }

                }
            }
        }, 200)


    }

    override fun onSpecifiedCommandPronounced(event: String?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                (Objects.requireNonNull(audioManager)).setStreamMute(AudioManager.STREAM_SYSTEM, true)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (Speech.getInstance().isListening) {
            muteBeepSoundOfRecorder()
            Speech.getInstance().stopListening()
        } else {
            // Always true pre-M
            try {
                Speech.getInstance().stopTextToSpeech()
                Speech.getInstance().startListening(null, this)
            } catch (exc: SpeechRecognitionNotAvailable) {
            } catch (exc: GoogleVoiceTypingDisabledException) {
            }
            muteBeepSoundOfRecorder()
        }
    }


    fun stopSpeechRecoganization() {
        Speech.getInstance()?.setListener(null)
        Speech.getInstance()?.stopListening()
        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND)
            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0)
        } catch (e: SecurityException) {
        }
        if (commandNames?.size == 1) {
            imageRecording.setImageResource(R.drawable.ic_check_audo_button)
            txtRecording.setText("Processing...")

        }
        if (commandNames?.size == 2) {
            imageRecording2.setImageResource(R.drawable.ic_check_audo_button)
            txtRecording.setText("Processing...")
        }
        if (commandNames?.size == 3) {
            imageRecording3.setImageResource(R.drawable.ic_check_audo_button)
            txtRecording.setText("Retake")
            txtLater.setText("Next")
            txtLater.visibility = View.VISIBLE
            commandNames?.clear()
        }
        btnAudio.clearAnimation()
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (!commanData.isEmpty()) {
                    showMessage("Recording in progress.")
                    return false
                } else {
                    return true
                }


            }
        }
        return false;

    }

    fun startCommand() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                (Objects.requireNonNull(audioManager)).setStreamMute(AudioManager.STREAM_SYSTEM, false)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        Speech.init(context)
        delegate = this
        Speech.getInstance().setListener(this)
        if (Speech.getInstance().isListening) {
            Speech.getInstance().stopListening()
            muteBeepSoundOfRecorder()
        } else {
            System.setProperty("rx.unsafe-disable", "True")

            try {
                Speech.getInstance().stopTextToSpeech()
                Speech.getInstance().startListening(null, this)
            } catch (exc: SpeechRecognitionNotAvailable) {
                //showSpeechNotSupportedDialog();
            } catch (exc: GoogleVoiceTypingDisabledException) {
                //showEnableGoogleVoiceTyping();
            }

            muteBeepSoundOfRecorder()
        }
    }

    private fun muteBeepSoundOfRecorder() {
        if (audioManager != null) {
            audioManager.setStreamSolo(AudioManager.STREAM_SYSTEM, true);
        }

    }
}*/

package com.android.vadify.ui.login.fragment

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.data.api.enums.ChatType
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.databinding.CommandRegistrationBinding
import com.android.vadify.ui.Home
import com.android.vadify.ui.baseclass.BaseDaggerFragment
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.activity.RetakeCommand
import com.android.vadify.ui.login.StartUpActivity
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.viewmodels.CommandsViewModel
import com.google.gson.Gson
import com.microsoft.appcenter.utils.HandlerUtils
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.command_registration.*
import kotlinx.android.synthetic.main.command_registration.btnAudio
import kotlinx.android.synthetic.main.command_registration.btnClose
import kotlinx.android.synthetic.main.command_registration.imageRecording
import kotlinx.android.synthetic.main.command_registration.imageRecording2
import kotlinx.android.synthetic.main.command_registration.imageRecording3
import kotlinx.android.synthetic.main.command_registration.step2
import kotlinx.android.synthetic.main.command_registration.step3
import kotlinx.android.synthetic.main.command_registration.step4
import kotlinx.android.synthetic.main.command_registration.step5
import kotlinx.android.synthetic.main.command_registration.txtLater
import kotlinx.android.synthetic.main.command_registration.txtMayaCommand
import kotlinx.android.synthetic.main.command_registration.txtRecording
import kotlinx.android.synthetic.main.item_contact.view.*
import kotlinx.android.synthetic.main.translation_screen.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CommandsFragment  : BaseDaggerFragment<CommandRegistrationBinding>(),
    RecognitionListener {

    override val layoutRes: Int
        get() = R.layout.command_registration
    val viewModelProfile: ProfileFragmentViewModel by viewModels()
    val commandViewModel: CommandsViewModel by viewModels()
    val viewModelss: ProfileFragmentViewModel by viewModels()
    var Recording = false
    var RETAKE: String? = "NO"
    private var firstCall = false
    var languageName: String? = null
    var languageEachCode: String? = null
    var nameRetake:String?=null

    val viewModel: ChatViewModel by viewModels()
    val viewCommand: CommandsViewModel by viewModels()
    var commandNames: ArrayList<String>? = null
    lateinit var defaultSharedPreferences: SharedPreferences
    var languageNew: String? = null
    var languageCodeNew: String? = null
    lateinit var handler: Handler
    var speech: SpeechRecognizer? = null
    lateinit var recognizerIntent: Intent
    val viewModels: ProfileFragmentViewModel by viewModels()
    private var isLast = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

                if(isAllGranted(Permission.RECORD_AUDIO)) { }
                else {
                    runWithPermissions(Permission.RECORD_AUDIO)
                    {  }
                }
        handler = Handler(Looper.getMainLooper())
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        nameRetake = arguments?.getString("RETAKE")
        Log.d( "onretake: ",""+nameRetake)

        subscribe(command_congratulation.throttleClicks()) {

            command_congratulation.visibility = View.GONE
            txtLater.visibility = View.VISIBLE
            txtLater.text = "Done"
        }

        subscribe(btnAudio.throttleClicks()) {
            if (!Recording) {
                commandNames = ArrayList();
                recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                recognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                Log.d( "onResume-command: ",""+ languageEachCode)
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageEachCode)
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                speech = SpeechRecognizer.createSpeechRecognizer(requireActivity())
                speech?.setRecognitionListener(this)
                Recording = true
                txtLater.visibility = View.INVISIBLE
                speechToText()
                txtRecording.text = "Recording..."
            }
        }


        subscribe(txtRecording.throttleClicks())
        {
            if (txtRecording.text.equals("Retake")) {
                commandNames?.clear()
                txtLater.visibility = View.INVISIBLE
                sendNextData(txtMayaCommand.text.toString())
            }
        }

        subscribe(txtLater.throttleClicks())
        {
            Recording = false
                if (activity is RetakeCommand) {
                    activity?.finish()
                } else if (activity is StartUpActivity) {

                    val intent = Intent(requireContext(), Dashboard::class.java)
                    intent.putExtra(Dashboard.IS_FIRST_TIME, true)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    val edit = defaultSharedPreferences.edit()
                    edit.putString("pkey_skip_language", "Yes")
                    edit.commit()
                    (activity as ChatActivity).viewModel.isSpeechLanguageEnable.value = ChatType.SPEECH_TO_TEXT
                    (activity as ChatActivity).speechToText(languageCodeNew)
                    ChatActivity.isHasValue = true
                    (activity as ChatActivity).commandViewModel.getCommandAll(languageNew!!)
                    requireActivity().supportFragmentManager.beginTransaction()
                        .remove(requireActivity().supportFragmentManager.findFragmentById(R.id.command_container)!!)
                        .commit()
                }
            }


        getAllLanguages()
        viewModelProfile.languageList.observe(requireActivity(), Observer {
            var allLanguage = ""
            it.forEach {it1 ->
                allLanguage += "${it1.countryCode}/${it1.name}/${it1.languageCode}#"
            }
            countryLanguageCommand.setCustomMasterCountries(allLanguage)
        })

        countryLanguageCommand.setOnCountryChangeListener {
            if (countryLanguageCommand.selectedLanguageCode!=null  && firstCall ) {
                countryLanguageCommand.also {
                    Log.d( "onCommandFragLanguage: ", it.selectedLanguageCode+" :--: "+ it.selectedCountryName+" :--: "+ it.selectedCountryEnglishName +" :--: "+ it.selectedCountryNameCode)
                    selected_lang_command.text = it.selectedLanguageCode.toUpperCase()

                    val parts = it.selectedCountryName.split("(")
                    Log.d( "language----", parts[0])
                    val edit = defaultSharedPreferences.edit()
                    if (nameRetake=="YES")
                    {
                        edit.putString("pkey_each_command_Language_retake", parts[0])
                        edit.putString("pkey_each_command_Code_retake", it.selectedLanguageCode)
                        edit.putString("pkey_each_command_country_code_retake", it.selectedCountryNameCode)
                        edit.commit()
                        txtRecordingLanguage.text =
                            "Recording command for " + defaultSharedPreferences.getString(
                                "pkey_each_command_Language_retake",
                                ""
                            )
                    }
                    else {
                        edit.putString("pkey_each_command_Language", parts[0])
                        edit.putString("pkey_each_command_Code", it.selectedLanguageCode)
                        edit.putString("pkey_each_command_country_code", it.selectedCountryNameCode)
                        edit.commit()
                        txtRecordingLanguage.text =
                            "Recording command for " + defaultSharedPreferences.getString(
                                "pkey_each_command_Language",
                                ""
                            )
                    }
                    languageNew = parts[0]
                    languageCodeNew= it.selectedLanguageCode
                    languageEachCode =it.selectedLanguageCode
                }

            }
            firstCall = true
        }
       /* viewModel.userlanguageList.observe(this, Observer {
            if (it!=null) {
                Log.d("idsdsdnitView: ", Gson().toJson(it.countryCode))
                countryLanguageCommand.setCountryForNameCode(it.countryCode)
                selected_lang_command.text = it.languageCode.toUpperCase()
                languageCode = it.languageCode + "-" + it.countryCode

            }else{
                countryLanguageCommand.setCountryForNameCode("en");
                selected_lang.text = "IN"
            }

        })*/

    }

    override fun onResume() {
        super.onResume()
        firstCall = false



        if (nameRetake == "NO") {
            languageName = defaultSharedPreferences.getString("pkey_each_command_Language", "")
            languageEachCode = defaultSharedPreferences.getString("pkey_each_command_Code", "")
            if (languageName.equals("") && languageEachCode.equals("")) {
                Log.d("calling", "onResume: ")
                countryLanguageCommand.setCountryForNameCode("US");
                txtRecordingLanguage.text = "Recording command for English (English)"

                val edit = defaultSharedPreferences.edit()
                edit.putString("pkey_each_command_Language", "English")
                edit.putString("pkey_each_command_Code", "en")
                edit.putString("pkey_each_command_country_code", "US")
                edit.commit()
                selected_lang_command.text = "en"

                languageNew = "English"
                languageCodeNew= "en"

            } else {

                txtRecordingLanguage.text =
                    "Recording command for " + defaultSharedPreferences.getString(
                        "pkey_each_command_Language",
                        ""
                    )
                countryLanguageCommand.setCountryForNameCode(
                    defaultSharedPreferences.getString(
                        "pkey_each_command_country_code",
                        ""
                    )
                );
                selected_lang_command.text = languageEachCode
                languageNew = languageName
                languageCodeNew= languageEachCode
            }
        }
        else{
            languageName = defaultSharedPreferences.getString("pkey_each_command_Language_retake", "")
            languageEachCode = defaultSharedPreferences.getString("pkey_each_command_Code_retake", "")
            if (languageName.equals("") && languageEachCode.equals("")) {
                Log.d("calling", "onResume: ")
                countryLanguageCommand.setCountryForNameCode("US");
                txtRecordingLanguage.text = "Recording command for English (English)"

                val edit = defaultSharedPreferences.edit()
                edit.putString("pkey_each_command_Language_retake", "English")
                edit.putString("pkey_each_command_Code_retake", "en")
                edit.putString("pkey_each_command_country_code_retake", "US")
                edit.commit()
                selected_lang_command.text = "en"

                languageNew = "English"
                languageCodeNew= "en"
            } else {
                txtRecordingLanguage.text =
                    "Recording command for " + defaultSharedPreferences.getString(
                        "pkey_each_command_Language_retake",
                        ""
                    )
                countryLanguageCommand.setCountryForNameCode(
                    defaultSharedPreferences.getString(
                        "pkey_each_command_country_code_retake",
                        ""
                    )
                );
                selected_lang_command.text = languageEachCode
                languageNew = languageName
                languageCodeNew= languageEachCode
            }


        }



    }
    override fun onDestroy() {
        super.onDestroy()
        speech = null
    }

    private fun getAllLanguages() {
        bindNetworkState(
            viewModelProfile.getAllLanguageResponseMo()
        )
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

      //  Log.d("language-code: ", "" + languageCode)
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
                        language = languageNew!!,
                        commandName = txtMayaCommand.text.toString(),
                        languageCode = languageCodeNew!!,
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
                HandlerUtils.runOnUiThread {
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

}
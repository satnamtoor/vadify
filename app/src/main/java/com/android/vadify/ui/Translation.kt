package com.android.vadify.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.BuildConfig
import com.android.vadify.R
import com.android.vadify.data.api.enums.ChatType
import com.android.vadify.data.api.models.TranslateModel
import com.android.vadify.databinding.HelpScreenBinding
import com.android.vadify.databinding.TranslationScreenBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_blocked.*
import kotlinx.android.synthetic.main.activity_blocked.toolbar
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.help_screen.*
import kotlinx.android.synthetic.main.home.*
import kotlinx.android.synthetic.main.translation_screen.*
import kotlinx.coroutines.*

class Translation : DataBindingActivity<TranslationScreenBinding>(), RecognitionListener {
    override val layoutRes: Int
        get() = R.layout.translation_screen
    val viewModelProfile: ProfileFragmentViewModel by viewModels()
    val viewModelChat: ChatViewModel by viewModels()
    var speech: SpeechRecognizer? =null
    lateinit var recognizerIntent:Intent
    lateinit var translateAdpapter: TranslationAdapter
    val listTranslate = ArrayList<TranslateModel>()
private lateinit var pref :SharedPreferences
    private var fromfirstCall = false
    private var tofirstCall = false
    companion object {
        var isMicOn: Boolean = false
        var isListenCall: Boolean = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        toolbar.setNavigationOnClickListener {
            finish()

        }

        getAllLanguages()
       viewModelProfile.languageList.observe(this, androidx.lifecycle.Observer {


          //  val allLanguageList = it.map { it1 -> it1.countryCode.toLowerCase() }

            var allLanguage = ""
           it.forEach {it1 ->
               allLanguage += "${it1.countryCode}/${it1.name}/${it1.languageCode}#"
           }

            countryLanguageSelectedFrom.setCustomMasterCountries(allLanguage)
            countryLanguageSelectedTo.setCustomMasterCountries(allLanguage)

             if (viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage_Code)
                     .isNullOrBlank()) {
                 countryLanguageSelectedFrom.setCountryForNameCode("en");
                 viewModelProfile.preferenceService.putString(R.string.pkey_fromLanguage,"en")
                 viewModelProfile.preferenceService.putString(R.string.pkey_fromLanguage_CountryCode,"en-US")

             }
            else{
                 var motherCOde = viewModelProfile.preferenceService.getString(
                     R.string.pkey_fromLanguage_Code
                 )
                 countryLanguageSelectedFrom.setCountryForNameCode(motherCOde)
            }

            if (viewModelProfile.preferenceService.getString(R.string.pkey_toLanguage_Code)
                    .isNullOrBlank()) {

                countryLanguageSelectedTo.setCountryForNameCode("en");
                viewModelProfile.preferenceService.putString(R.string.pkey_toLanguage,"en")
                viewModelProfile.preferenceService.putString(R.string.pkey_toLanguage_CountryCode,"en-US")


            }
            else{
                var motherCOde = viewModelProfile.preferenceService.getString(
                    R.string.pkey_toLanguage_Code
                )
                countryLanguageSelectedTo.setCountryForNameCode(motherCOde)
            }

        })

        countryLanguageSelectedFrom.setOnCountryChangeListener {

           /* val selectedLanguageCode =
                viewModelProfile.languageList.value?.filter { it.countryCode == countryLanguageSelectedFrom.selectedCountryNameCode }
*/
            if (countryLanguageSelectedFrom.selectedLanguageCode!=null && fromfirstCall) {
                countryLanguageSelectedFrom.also {

                    Log.d( "language--: ", countryLanguageSelectedFrom.selectedLanguageCode +" "+countryLanguageSelectedFrom.selectedCountryNameCode)
                    viewModelProfile.preferenceService.putString(R.string.pkey_fromLanguage_Code,countryLanguageSelectedFrom.selectedCountryNameCode)
                    viewModelProfile.preferenceService.putString(R.string.pkey_fromLanguage,countryLanguageSelectedFrom.selectedLanguageCode)
                    viewModelProfile.preferenceService.putString(R.string.pkey_fromLanguage_CountryCode,countryLanguageSelectedFrom.selectedLanguageCode + "_"+countryLanguageSelectedFrom.selectedCountryNameCode)

                }
                selected_langFrom.text = countryLanguageSelectedFrom.selectedLanguageCode.toString().toUpperCase()
                languageChange(viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage).toString())
               // }
            }
            fromfirstCall = true
        }


        countryLanguageSelectedTo.setOnCountryChangeListener {

            if (countryLanguageSelectedTo.selectedLanguageCode!=null && tofirstCall) {
                countryLanguageSelectedTo.also {

                  //  Log.d( "language--: ", Gson().toJson(it))
                    viewModelProfile.preferenceService.putString(R.string.pkey_toLanguage_Code,countryLanguageSelectedTo.selectedCountryNameCode)

                    viewModelProfile.preferenceService.putString(R.string.pkey_toLanguage,countryLanguageSelectedTo.selectedLanguageCode)

                    viewModelProfile.preferenceService.putString(R.string.pkey_toLanguage_CountryCode,countryLanguageSelectedTo.selectedLanguageCode + "_"+countryLanguageSelectedTo.selectedCountryNameCode)

                }
                selected_lang_to.text = countryLanguageSelectedTo.selectedLanguageCode.toString().toUpperCase()

            }

            tofirstCall = true
        }

        languageChange(viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage).toString())

        System.setProperty(ChatActivity.GOOGLE_KEY, BuildConfig.GOOGLE_API_KEY)


        subscribe(remove.throttleClicks())
        {

            deleteTranslationPopUp()

        }

        subscribe(listeningMicTrans.throttleClicks()) {

            if (isMicOn) {
                isMicOn = false
                micTrans.setImageResource(R.drawable.ic_mic_red)
                stopSpeechRecoganization()

            } else {
                isMicOn = true
                micTrans.setImageResource(R.drawable.ic_mic_red)
                //Log.d( "mic-from-language: ",""+viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage_CountryCode))
                speechToText(viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage).toString())

            }
        }

        subscribe(sendMessage.throttleClicks())
        {
if (!listening_inputTrans.text.toString().isNullOrBlank())
{

            translate(listening_inputTrans.text.toString())

            {
                it?.let {
                  //  Log.d("translation: ", Gson().toJson(it))
                    CoroutineScope(Dispatchers.Main).launch {
                        listTranslate.add(TranslateModel(listening_inputTrans.text.toString(), it))
                      translateAdpapter.notifyDataSetChanged()
                        chatListTrans.smoothScrollToPosition(listTranslate.size - 1)

                        addListToPref(listening_inputTrans.text.toString(), it)
                        listening_inputTrans.setText("")
                    }
                }
            }
}
        }

        subscribe(listenerCancelTrans.throttleClicks())
        {
            listening_inputTrans.setText("")
        }
        chatListTrans.layoutManager= LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false)

        translateAdpapter = TranslationAdapter(listTranslate, this@Translation)
        chatListTrans.adapter = translateAdpapter

        getListFromPref()

      //  translateUser("testing");
    }
    private fun getAllLanguages() {
        bindNetworkState(
            viewModelProfile.getAllLanguageResponseMo()
        )
    }

    override fun onStop() {
        super.onStop()
        isMicOn = false
        micTrans.setImageResource(R.drawable.ic_mic_red)
        stopSpeechRecoganization()
    }

    fun translate(
        message: String,
        callBack: (String?) -> Unit
    ) {

        Log.d("from:language ", ""+viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage))
        Log.d("to:language ", ""+viewModelProfile.preferenceService.getString(R.string.pkey_toLanguage))

        if (viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage)
                .equals(viewModelProfile.preferenceService.getString(R.string.pkey_toLanguage)))
        {
            //Log.d("from:language-first ", "error-                "+viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage))

           // callBack(message)
            showAlertDialog()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {

                try {
                    TranslateOptions.getDefaultInstance().service?.let {
                        it.translate(
                            message,
                            Translate.TranslateOption.sourceLanguage(
                                viewModelProfile.preferenceService.getString(R.string.pkey_fromLanguage)
                            ),
                            Translate.TranslateOption.targetLanguage(
                                viewModelProfile.preferenceService.getString(R.string.pkey_toLanguage)
                            ),
                            Translate.TranslateOption.model("nmt")
                        )?.let {
                            val translatedText = it.translatedText
                            print(translatedText)
                           // Log.d("translation: ", translatedText)
                            callBack(translatedText)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("message-exc", "" + e.message.toString())
                   callBack(message)
                }

        }

    }
    fun stopSpeechRecoganization() {
       // listening_inputTrans.hint = resources.getString(R.string.type_message)
        stopSpeech()
        try {
            val manager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            manager.mode = AudioManager.MODE_NORMAL

        } catch (e: SecurityException) {
        }

        micTrans.clearAnimation()
    }

    private fun showAlertDialog() {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this@Translation)
        alertDialog.setTitle("AlertDialog")
        alertDialog.setMessage("Please make sure from and To languages are different.")
        alertDialog.setPositiveButton(
            "Okay"
        ) { _, _ ->
            //Toast.makeText(this@Translation, "Alert dialog closed.", Toast.LENGTH_LONG).show()

        }
/*        alertDialog.setNegativeButton(
            "No"
        ) { _, _ -> }*/
        val alert: AlertDialog = alertDialog.create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }

    fun stopSpeech() {
        if (speech!=null) {
            speech?.stopListening()
            speech?.cancel()


        }
        muteBeepSoundOfRecorder(true,AudioManager.ADJUST_UNMUTE)

    }
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
            micTrans.setImageResource(R.drawable.ic_mic_red)
            isMicOn =true

            listening_inputTrans.hint = resources.getString(R.string.start_talking_i_m_listening)
            AnimationUtils.loadAnimation(applicationContext, R.anim.blink_image)
                .also { micTrans.startAnimation(it) }

            startCommand(languageCode, true,AudioManager.ADJUST_UNMUTE)


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun startCommand(languageCode: String?, firstTime: Boolean,Mutevalue:Int) {
        muteBeepSoundOfRecorder(!firstTime,AudioManager.ADJUST_MUTE)
        speech?.startListening(recognizerIntent)
    }

       fun languageChange(languageCode: String)
    {
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

    override fun onReadyForSpeech(p0: Bundle?) {

    }

    override fun onBeginningOfSpeech() {

    }

    override fun onRmsChanged(p0: Float) {

    }

    override fun onBufferReceived(p0: ByteArray?) {

    }

    override fun onEndOfSpeech() {

    }

    override fun onError(p0: Int) {
        if (isMicOn)
        {
        try {
            GlobalScope.launch {
                runOnUiThread {
                    speech?.cancel()
                }
                delay(50)
                runOnUiThread {
                    if (!ChatActivity.isAudioOn)
                        startCommand("", false, AudioManager.ADJUST_MUTE)
                }
            }
        } catch (ex: Exception) {
        }
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results!!.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches!![0]
        Log.d("speech-new-result: ", "" + results)

        listening_inputTrans.setText(listening_inputTrans.text.toString()+ text.toString()+" ")

       if (isMicOn)
       {
        try {
            GlobalScope.launch {
                runOnUiThread {
                    speech?.cancel()
                }
                delay(50)
                runOnUiThread {
                    if (!ChatActivity.isAudioOn)
                        startCommand("", false, AudioManager.ADJUST_MUTE)
                }
            }
        } catch (ex: Exception) {
        }

       }
    }

    override fun onPartialResults(p0: Bundle?) {

    }

    override fun onEvent(p0: Int, p1: Bundle?) {

    }



    private fun addListToPref(key : String,value : String) {
        val translateModel = TranslateModel(key, value)
        val prefsEditor = pref.edit()
        var newJsonString = Gson().toJson(translateModel)
        val storedJsonString = pref.getString("translated", "")
        if (!storedJsonString.isNullOrEmpty())
            newJsonString = "$storedJsonString||$newJsonString"
        prefsEditor.putString("translated", newJsonString)
        prefsEditor.commit()
    }

    private fun getListFromPref(){
        val jsonString = pref.getString("translated", "")
        if (!jsonString.isNullOrEmpty()) {
            val data = jsonString?.split("||")
            for (translateString in data) {
                val translatedModel = Gson().fromJson(translateString, TranslateModel::class.java)
                Log.d( "getListFromPref: ",Gson().toJson(translatedModel))
                listTranslate.add(TranslateModel(translatedModel.key,translatedModel.value))
                translateAdpapter.notifyDataSetChanged()
                chatListTrans.smoothScrollToPosition(listTranslate.size - 1)

            }
        }
    }
    private  fun deleteTranslation(){
        val prefsEditor = pref.edit()
        prefsEditor.remove("translated")
        prefsEditor.commit()
        listTranslate.clear()
        translateAdpapter.notifyDataSetChanged()
        viewModelProfile.preferenceService.putString(R.string.pkey_fromLanguage,"en")
        viewModelProfile.preferenceService.putString(R.string.pkey_fromLanguage_CountryCode,"en-US")

        viewModelProfile.preferenceService.putString(R.string.pkey_toLanguage,"en")
        viewModelProfile.preferenceService.putString(R.string.pkey_toLanguage_CountryCode,"en-US")

        countryLanguageSelectedTo.setCountryForNameCode("us");
        countryLanguageSelectedFrom.setCountryForNameCode("us");
    }

    fun deleteTranslationPopUp() {


        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(resources.getString(R.string.app_name))
        alertDialog.setMessage(resources.getString(R.string.delete_translation))
        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, resources.getString(R.string.yes)
        ) { dialog, which ->
            dialog.dismiss()
            deleteTranslation()
        }

        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, resources.getString(R.string.no)
        ) { dialog, which ->
            dialog.dismiss()
        }
        alertDialog.show()

        val btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = 10f
        btnPositive.layoutParams = layoutParams
        btnNegative.layoutParams = layoutParams
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.blue));
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.blue));
    }


    fun translateUser(
        message: String) {
        CoroutineScope(Dispatchers.IO).launch {

            try {
                TranslateOptions.getDefaultInstance().service?.let {
                    it.translate(
                        message,
                        Translate.TranslateOption.sourceLanguage(
                            "hi"
                        ),
                        Translate.TranslateOption.targetLanguage(
                            ""
                        ),
                        Translate.TranslateOption.model("nmt")
                    )?.let {
                        val translatedText = it.translatedText
                        //  print(translatedText)
                        Log.d("translation: ", translatedText)
                        //callBack(translatedText)
                    }
                }
            } catch (e: Exception) {
                Log.e("message-exc", "" + e.message.toString())
                // callBack(message)
            }

        }

    }

}
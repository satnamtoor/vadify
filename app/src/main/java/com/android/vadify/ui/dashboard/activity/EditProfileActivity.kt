package com.android.vadify.ui.dashboard.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import com.android.vadify.R
import com.android.vadify.databinding.ActivityEditProfileBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.dashboard.viewmodel.EditProfileViewModel
import com.android.vadify.ui.dashboard.viewmodel.SettingFragmentViewModel
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.viewmodels.CommandsViewModel
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import kotlinx.android.synthetic.main.activity_blocked.toolbar
import kotlinx.android.synthetic.main.activity_edit_profile.*

class EditProfileActivity : DataBindingActivity<ActivityEditProfileBinding>() {

    val viewModel: EditProfileViewModel by viewModels()
    val commandViewModel: CommandsViewModel by viewModels()
    var language = ""
    var languageCode = ""
    override val layoutRes: Int
        get() = R.layout.activity_edit_profile

    lateinit var languageCodeArray: List<String>
    // val viewModelProfile: ProfileFragmentViewModel by viewModels()

    //  val viewModelSettings: SettingFragmentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        Log.d("language-code ", viewModel.motherLanguage.value.toString())
        viewModel.languageList.observe(this, androidx.lifecycle.Observer {

            languageCodeArray = viewModel.languageList.value!!.map {
                it.code
            }

            val listName1 = viewModel.languageList.value!!.map {
                it.name.replace("(", "( ")
            }


            val listName = listName1!!.map {
                it.replace(")", " )")
            }


            val adapter: ArrayAdapter<String> =
                ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_item,
                    listName
                )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            motherLanguageText.adapter = adapter


            val array_spinner =
                motherLanguageText.getAdapter() as ArrayAdapter<String>?
            motherLanguageText.setSelection(array_spinner!!.getPosition(viewModel.motherLanguage.value))

        })

        motherLanguageText.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                adapter: AdapterView<*>,
                view: View,
                position: Int,
                l: Long
            ) {

                viewModel.motherLanguage.value = adapter.getItemAtPosition(position).toString()
                languageCode = languageCodeArray.get(position)
                language = viewModel.motherLanguage.value.toString()
                Log.i(
                    "language-selected-",
                    viewModel.motherLanguage.value.toString() + "code " + languageCode
                )
                val defaultSharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(this@EditProfileActivity)
                val edit = defaultSharedPreferences.edit()
                edit.putString("pkey_skip_language", "")
                edit.commit()


            }

            override fun onNothingSelected(adapterView: AdapterView<*>?) {

            }
        }





        language = viewModel.motherLanguage.value.toString()
        languageCode = viewModel.languageCodeNew.value.toString()

        // commandViewModel =
        //   ViewModelProvider(this).get(CommandsViewModel::class.java)
        commandViewModel.data.observe(this, androidx.lifecycle.Observer {
            if (it.isEmpty()) {
                viewModel.filterMethodN { isCondition, message ->
                    when {
                        isCondition -> {
                            bindNetworkState(
                                viewModel.updateProfileData(),
                                progressDialog(R.string.updating)
                            ) {

                                withButtonCentered(language)
                            }
                        }
                        else -> showSnackMessage(
                            resources.getString(
                                message ?: R.string.unknow_msg
                            )
                        )
                    }

                }

            } else {
                viewModel.filterMethod { isCondition, message ->
                    when {
                        isCondition -> {
                            bindNetworkState(
                                viewModel.updateProfileData(),
                                progressDialog(R.string.updating)
                            )
                        }
                        else -> showSnackMessage(
                            resources.getString(
                                message ?: R.string.unknow_msg
                            )
                        )
                    }

                }
            }
        })
        toolbar.setNavigationOnClickListener {
            finish()
        }

        /*subscribe(motherLanguage.throttleClicks()) {
            LocalLanguagePopUp {
                viewModel.motherLanguage.value = it.language
                language = it.language
                languageCode = it.code

            }.show(supportFragmentManager, null)


        }*/


//        viewModel.getLanguages()?.let {
//            val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, it)
//            motherLanguage.setAdapter(arrayAdapter)
//        }

        subscribe(NextBtn.throttleClicks())
        {

           // viewModel.puttlanguageCode(languageCode)
          //  commandViewModel.getCommandAll(language)
            viewModel.filterMethod { isCondition, message ->
                when {
                    isCondition -> {
                        bindNetworkState(
                            viewModel.updateProfileData(),
                            progressDialog(R.string.updating)
                        )
                    }
                    else -> showSnackMessage(
                        resources.getString(
                            message ?: R.string.unknow_msg
                        )
                    )
                }

            }

        }


    }

    override fun onResume() {
        super.onResume()
        Log.d("TAG", "onResume: ")
        getAllLanguages()
    }

    override fun onBindView(binding: ActivityEditProfileBinding) {
        binding.vm = viewModel
    }

    override fun onDestroy() {
        super.onDestroy()
        // Speech.getInstance()?.shutdown()
    }


    fun withButtonCentered(language: String) {

        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(resources.getString(R.string.app_name))
        alertDialog.setMessage(
            resources.getString(R.string.voice_command_setup_pop)
                    + "\n\n" + resources.getString(R.string.voice_command_setup_pop2)

        )

        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, resources.getString(R.string.record_now)
        ) { dialog, which ->
            val intent = Intent(this, RetakeCommand::class.java)
            intent.putExtra("LANGUAGE", language)
            intent.putExtra("LANG_CODE", languageCode)
            intent.putExtra("RETAKE", "YES")
            startActivity(intent)
            dialog.dismiss()
        }

        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, resources.getString(R.string.skip_now)
        ) { dialog, which -> dialog.dismiss() }
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

    private fun getAllLanguages() {
        bindNetworkState(
            viewModel.getAllLanguageResponseMo()
        )
    }
}
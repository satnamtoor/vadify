package com.android.vadify.ui.login.fragment

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.android.vadify.R
import com.android.vadify.databinding.FragmentLoginBinding
import com.android.vadify.ui.baseclass.BaseDaggerFragment
import com.android.vadify.ui.dashboard.activity.PolicyActivity
import com.android.vadify.ui.dashboard.activity.TermActivity
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.viewmodels.EncryptionViewModel
import com.android.vadify.widgets.getCountryCode
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.credentials.HintRequest
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.messaging.FirebaseMessaging
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import kotlinx.android.synthetic.main.fragment_login.*


class LoginFragment : BaseDaggerFragment<FragmentLoginBinding>() {

    override val layoutRes: Int
        get() = R.layout.fragment_login

    val viewModel: LoginFragmentViewModel by activityViewModels()
    val encryptionViewModel: EncryptionViewModel by viewModels()
    var countryCode: String = ""
    var dialCode: String = ""
    val viewModelProfile: ProfileFragmentViewModel by viewModels()

    companion object {
        const val SELECT_COUNTRY = "Select Country"
        const val COUNTRY_PICKER = "COUNTRY_PICKER"
        const val INDIA = 0
        const val UNITED_STATE = 1
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        countryInputBtn.setCustomMasterCountries(null)
        countryInputBtn.setExcludedCountries("abed")
        countryInputBtn.setLoadedLibraryMasterListLanguage(null)
        countryInputBtn.setLoadedLibraryMaterList(null)
        addHyphenPhoneNumber(phoneNumber)
        var countryName = countryInputBtn.selectedCountryName
        viewModel.countryCode.value = countryInputBtn.selectedCountryCodeWithPlus
        countryInputBtn.textView_selectedCountry.text = countryName.toString()
        countryCode = countryInputBtn.selectedCountryCodeWithPlus
        dialCode = countryInputBtn.selectedCountryNameCode
        countryInputBtn.setOnCountryChangeListener {
            viewModel.languageFlag.value = countryInputBtn.selectedCountryNameCode
            countryName = countryInputBtn.selectedCountryName
            countryInputBtn.textView_selectedCountry.text = countryName.toString()
            viewModel.countryCode.value = countryInputBtn.selectedCountryCodeWithPlus
            countryCode = countryInputBtn.selectedCountryCodeWithPlus
            dialCode = countryInputBtn.selectedCountryNameCode


        }


        /*   val token = FirebaseMessaging.getInstance().token
                    val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    val edit = defaultSharedPreferences.edit()
                    edit.putString("pkey_device_token",token.result)
                    edit.commit()*/

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("device token", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            val defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
            val edit = defaultSharedPreferences.edit()
            edit.putString("pkey_device_token", task.result!!)
            edit.commit()
            Log.w("device-token", task.result!!)
            //returnValue.invoke(task.result)
        })

        getEncryptionKeys()

        /*  subscribe(countryInputBtn.throttleClicks()) {
              selectCountryFromPicker()
          }*/

        subscribe(NextBtn.throttleClicks()) {
            viewModelProfile.filterMethod { isCondition, message ->
                when {
                    isCondition -> {
                        viewModel.phoneNumberFilterMethod { isCondition, message ->
                            //findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToVerificationFragment("","","",false))
                            when {
                                isCondition -> {
                                    bindNetworkState(
                                        viewModel.otpVerfication(countryCode,
                                            phoneNumber.text.toString()),
                                        progressDialog(R.string.sending)
                                    ) {

                                        var phone = phoneNumber.text.toString().replace("-", "")

                                        findNavController().navigate(
                                            LoginFragmentDirections.actionLoginFragmentToVerificationFragment(
                                                countryCode,
                                                phone,
                                                dialCode,
                                                false,
                                                binding.txtName.text.toString()
                                            )
                                        )

                                    }
                                }
                                else -> showSnackMessage(resources.getString(message
                                    ?: R.string.unknow_msg))
                            }


                        }


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
        Log.d("onResume: ", viewModel.languageFlag.value.toString())
        if (!viewModel.languageFlag.value.isNullOrBlank()) {
            countryInputBtn.setCountryForNameCode(viewModel.languageFlag.value)
        }
    }

    private fun getEncryptionKeys() {
        bindNetworkState(
            encryptionViewModel.staticContent()
        )
    }


    private fun initView() {
        txtName.requestFocus()
        addColorTermAndPolicy()
        // addHyphenPhoneNumber()
        //getLocalDeviceInformation(viewModel)

    }

    private fun addColorTermAndPolicy() {
        val spannable = SpannableStringBuilder(resources.getString(R.string.TermConditionPart1))

        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(textPaint: TextPaint) {
                // use this to change the link color
                textPaint.color = textPaint.linkColor
                // toggle below value to enable/disable
                // the underline shown below the clickable text
                textPaint.isUnderlineText = true

            }

            override fun onClick(view: View) {
                Log.d("click-click", "onClick: ")
                //Selection.setSelection((view as TextView).text as Spannable, 0)
                //view.invalidate()
                // link.second.onClick(view)
                startActivity(Intent(requireContext(), TermActivity::class.java))
            }
        }

        spannable.setSpan(
            clickableSpan, 28, 44, Spannable.SPAN_EXCLUSIVE_INCLUSIVE
        )

        val clickableSpan2 = object : ClickableSpan() {
            override fun updateDrawState(textPaint: TextPaint) {
                // use this to change the link color
                textPaint.color = textPaint.linkColor
                // toggle below value to enable/disable
                // the underline shown below the clickable text
                textPaint.isUnderlineText = true
            }

            override fun onClick(view: View) {
                //  Log.d("click-click", "onClick: ")
                startActivity(Intent(requireContext(), PolicyActivity::class.java))
            }
        }


        spannable.setSpan(
            clickableSpan2, 53, 67, Spannable.SPAN_EXCLUSIVE_INCLUSIVE
        )
//        textView6.text = spannable

//        textView6.movementMethod = LinkMovementMethod.getInstance()
    }


    /*private fun selectCountryFromPicker() {
       countryPicker(viewModel)

    }*/


    override fun onBindView(binding: FragmentLoginBinding) {
        binding.vm = viewModel
        binding.vmProfile = viewModelProfile
    }

    fun addHyphenPhoneNumber(phoneNumber: TextInputEditText) {
        var lastChar = ""
        phoneNumber.doBeforeTextChanged { _, _, _, _ ->
            val digits: Int = phoneNumber.text.toString().length
            if (digits > 1) lastChar = phoneNumber.text.toString().substring(digits - 1)
        }
        phoneNumber.doOnTextChanged { _, _, _, _ ->
            val digits: Int = phoneNumber.text.toString().length
            if (lastChar != "-") {
                if (digits == 3 || digits == 7) {
                    phoneNumber.append("-")
                }
            }


        }
    }

    fun getLocalDeviceInformation() {
        try {
            if ((viewModel.countryName.value ?: "").isEmpty()) {
                val countryShortName = requireContext().getCountryCode()
                val countryPinCode = PhoneNumberUtil.createInstance(requireContext())
                    .getCountryCodeForRegion(requireContext().getCountryCode()).toString()
                viewModel.updateCountryData(countryPinCode, countryShortName)

            }
        } catch (e: Exception) {
        }
    }


}

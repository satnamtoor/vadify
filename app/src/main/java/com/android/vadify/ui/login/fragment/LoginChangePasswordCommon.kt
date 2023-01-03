package com.android.vadify.ui.login.fragment

import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.databinding.ViewDataBinding
import com.android.vadify.R
import com.android.vadify.ui.baseclass.BaseDaggerFragment
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.android.vadify.widgets.getCountryCode
import com.android.vadify.widgets.messagePicker
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import kotlinx.android.synthetic.main.fragment_login.*

abstract class LoginChangePasswordCommon<FragmentLoginBinding : ViewDataBinding> :
    BaseDaggerFragment<FragmentLoginBinding>() {

    fun addHyphenPhoneNumber() {
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

    fun countryPicker(viewModel: LoginFragmentViewModel) {
      /*  val options =
            arrayOf<CharSequence>(getString(R.string.India), getString(R.string.United_state))
        requireContext().messagePicker(options) {
            when (it) {
                LoginFragment.INDIA -> viewModel.updateCountryInformation(
                    resources.getString(R.string.India),
                    "+91",
                    "IN"
                )
                LoginFragment.UNITED_STATE -> viewModel.updateCountryInformation(
                    resources.getString(
                        R.string.United_state
                    ), "+1", "US"
                )
            }
        }*/
    }

    fun getLocalDeviceInformation(viewModel: LoginFragmentViewModel) {
        try {
            if ((viewModel.countryName.value ?: "").isEmpty()) {
                val countryShortName = requireContext().getCountryCode()
                val countryPinCode = PhoneNumberUtil.createInstance(requireContext())
                    .getCountryCodeForRegion(requireContext().getCountryCode()).toString()
                viewModel.updateCountryData(countryPinCode, countryShortName)
                viewModel.flagSet(countryShortName)
            }
        } catch (e: Exception) {
        }
    }
}
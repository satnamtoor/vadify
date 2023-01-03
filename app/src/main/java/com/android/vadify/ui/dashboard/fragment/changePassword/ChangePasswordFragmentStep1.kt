package com.android.vadify.ui.dashboard.fragment.changePassword

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.android.vadify.R
import com.android.vadify.databinding.FragmentChangePasswordStep1Binding
import com.android.vadify.ui.login.fragment.LoginChangePasswordCommon
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import kotlinx.android.synthetic.main.fragment_change_password_step1.*

class ChangePasswordFragmentStep1 :
    LoginChangePasswordCommon<FragmentChangePasswordStep1Binding>() {

    private val viewModel: LoginFragmentViewModel by viewModels()

    override val layoutRes: Int
        get() = R.layout.fragment_change_password_step1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()


        toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        subscribe(NextBtn.throttleClicks()) {
            viewModel.phoneNumberFilterMethod { isCondition, message ->
                when {
                    isCondition -> {
                        bindNetworkState(
                            viewModel.changePasswordRequest(),
                            progressDialog(R.string.sending)
                        ) {
                            viewModel.countryCode.value?.let { countryCode ->
                                viewModel.phoneNumber.value?.let { phoneNumber ->
                                    viewModel.dialCode.value?.let { dialCode ->
                                        var phone = phoneNumber
                                        if (countryCode == "+91") {
                                            phone = phoneNumber.replace("-", "")
                                        }
                                        findNavController().navigate(
                                            ChangePasswordFragmentStep1Directions.actionChangePasswordFragmentStep1ToVerificationFragment2(
                                                countryCode,
                                                phone,
                                                dialCode,
                                                true
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> showSnackMessage(resources.getString(message ?: R.string.unknow_msg))
                }
            }
        }

        subscribe(countryInputField.throttleClicks()) {
            countryPicker(viewModel)
        }
        subscribe(countryInputBtn.throttleClicks()) {
            countryPicker(viewModel)
        }
    }


    private fun initView() {
        getLocalDeviceInformation(viewModel)
        addHyphenPhoneNumber()
    }


    override fun onBindView(binding: FragmentChangePasswordStep1Binding) {
        binding.vm = viewModel
    }
}
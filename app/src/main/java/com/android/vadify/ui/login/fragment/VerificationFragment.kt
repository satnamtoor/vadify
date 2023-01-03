package com.android.vadify.ui.login.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.vadify.R
import com.android.vadify.databinding.FragmentVerificationBinding
import com.android.vadify.ui.baseclass.BaseDaggerFragment
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.login.StartUpActivity
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.android.vadify.ui.login.viewmodel.ProfileFragmentViewModel
import com.android.vadify.widgets.numberFormat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher
import kotlinx.android.synthetic.main.fragment_verification.*


class VerificationFragment : BaseDaggerFragment<FragmentVerificationBinding>() {
    override val layoutRes: Int
        get() = R.layout.fragment_verification

    private val args: VerificationFragmentArgs by navArgs()

    val viewModel: LoginFragmentViewModel by viewModels()
    val viewModelProfile: ProfileFragmentViewModel by viewModels()
    lateinit var smsVerifyCatcher: SmsVerifyCatcher
    var startUpActivity: StartUpActivity? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        smsVerifyCatcher = SmsVerifyCatcher(requireActivity()) { message ->
            message?.let {
                viewModel.updateOtpCode(viewModel.parseCode(it))
            }
        }
        smsVerifyCatcher.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updatePhoneNumber()
        startUpActivity = this.activity as StartUpActivity?
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        subscribe(changeBtn.throttleClicks()) {
            findNavController().popBackStack()
        }

        subscribe(resendOtpBtn.throttleClicks()) {
            if (args.isChangePassword) {
                bindNetworkState(
                    viewModel.changePasswordRequest(),
                    progressDialog(R.string.sending)
                )
            } else {

                bindNetworkState(viewModel.otpVerfication(args.countryCode, args.phoneNumber),
                    progressDialog(R.string.sending))
            }
        }

        viewModel.otpCode.observe(viewLifecycleOwner) {
            if (!it.isNullOrBlank() && it.length >= 4) {
                viewModel.filterMethod { isCondition, message ->
                    when {
                        isCondition && !args.isChangePassword -> {
                            clickSubmit()
                            viewModel.updateOtpCode("")
                        }
                        isCondition && args.isChangePassword -> {
                            changePasswordWithOtpRequest()
                        }
                        else -> showSnackMessage(resources.getString(message
                            ?: R.string.unknow_msg))
                    }
                }

            }
        }


        subscribe(submitBtn.throttleClicks()) {
            viewModel.filterMethod { isCondition, message ->
                when {
                    isCondition && !args.isChangePassword -> {
                        clickSubmit()
                    }
                    isCondition && args.isChangePassword -> {
                        changePasswordWithOtpRequest()
                    }
                    else -> showSnackMessage(resources.getString(message ?: R.string.unknow_msg))
                }
            }
        }
    }

    private fun clickSubmit() {
        bindNetworkState(
            viewModel.loginApiHit(args.countryCode, args.phoneNumber, args.userName),
            progressDialog(R.string.verifying)
        )
        {
            bindNetworkState(
                viewModelProfile.updateProfileData(args.userName),
                progressDialog(R.string.updating))

            val intent = Intent(requireContext(), Dashboard::class.java)
            intent.putExtra(Dashboard.IS_FIRST_TIME, true)
            startActivity(intent)
            requireActivity().finish()

        }
    }

    private fun changePasswordWithOtpRequest() {
        bindNetworkState(
            viewModel.changePasswordWithOtpReqeust(
                args.countryCode,
                args.phoneNumber
            ), progressDialog(R.string.verifying)
        ) {
            requireActivity().finish()
        }
    }


    private fun updatePhoneNumber() {
        val formatedNumber =
            requireContext().numberFormat(args.dialCode, args.countryCode, args.phoneNumber)
        viewModel.updatePhoneNumber(args.countryCode, formatedNumber, args.phoneNumber)
    }


    override fun onStart() {
        super.onStart()
        /* if (isAllGranted(Permission.RECEIVE_SMS, Permission.READ_SMS)) {
             smsVerifyCatcher.onStart()
         } else {*/
        //  runWithPermissions(Permission.RECEIVE_SMS, Permission.READ_SMS) {
        //  otpVerification.isCursorVisible == true
        // }
        //}
        val imm =
            requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
    }

    override fun onStop() {
        super.onStop()
        smsVerifyCatcher.onStop()
    }


    override fun onBindView(binding: FragmentVerificationBinding) {
        binding.vm = viewModel
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        smsVerifyCatcher.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


}

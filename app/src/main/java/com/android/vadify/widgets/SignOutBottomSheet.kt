package com.android.vadify.widgets

import android.os.Bundle
import com.android.vadify.R
import com.android.vadify.databinding.SignOutBottomSheetBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerFragment
import kotlinx.android.synthetic.main.sign_out_bottom_sheet.*

class SignOutBottomSheet : BaseDialogDaggerFragment<SignOutBottomSheetBinding>() {
    override val layoutRes = R.layout.sign_out_bottom_sheet

    companion object {
        var response: ((Boolean) -> Unit)? = null
        fun create(response: ((Boolean) -> Unit)): SignOutBottomSheet {
            this.response = response
            return SignOutBottomSheet()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        yesBtn.setOnClickListener {
            response?.invoke(true)
            dismiss()
        }

        noBtn.setOnClickListener {
            dismiss()
        }
    }
}

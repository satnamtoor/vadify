package com.android.vadify.ui.chat.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.android.vadify.R
import com.android.vadify.databinding.LayoutCommandDrivenBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.layout_command_driven.*
import kotlinx.android.synthetic.main.message_popup.closeBtn


class CommandDrivenPopUp(var isCondition: (Boolean) -> Unit) :
    BaseDialogDaggerListFragment<LayoutCommandDrivenBinding>() {

    override val layoutRes: Int
        get() = R.layout.layout_command_driven


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCancelable(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        subscribe(closeBtn.throttleClicks()) {
            isCondition.invoke(false)
            dialog?.dismiss()
        }

        subscribe(speechRecoganization.throttleClicks()) {
            isCondition.invoke(true)
            dialog?.dismiss()
        }

    }

}

package com.android.vadify.ui.login.fragment.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import com.android.vadify.R
import com.android.vadify.databinding.ContactPopupBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.contact_popup.*


class NotificationPopUp(var isCondition: (Boolean) -> Unit) :
    BaseDialogDaggerListFragment<ContactPopupBinding>() {

    override val layoutRes: Int
        get() = R.layout.contact_popup


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title.text = requireActivity().resources.getString(R.string.NotificationTitle)
        message.text = requireActivity().resources.getString(R.string.NotificationMsg)

        subscribe(submitBtn.throttleClicks()) {
            dialog?.dismiss()
            isCondition.invoke(true)
        }

        subscribe(dontAllowBtn.throttleClicks()) {
            dialog?.dismiss()
            isCondition.invoke(true)
        }

    }
}

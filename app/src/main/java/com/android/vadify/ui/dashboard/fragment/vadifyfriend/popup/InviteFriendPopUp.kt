package com.android.vadify.ui.dashboard.fragment.vadifyfriend.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import com.android.vadify.R
import com.android.vadify.databinding.InviteFriendPopupBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.invite_friend_popup.*


class InviteFriendPopUp(var isCondition: (Boolean) -> Unit) :
    BaseDialogDaggerListFragment<InviteFriendPopupBinding>() {
    override val layoutRes: Int
        get() = R.layout.invite_friend_popup


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        subscribe(inviteBtn.throttleClicks()) {
            isCondition.invoke(true)
            dialog?.dismiss()
        }

        subscribe(laterBtn.throttleClicks()) {
            dialog?.dismiss()
        }

    }
}

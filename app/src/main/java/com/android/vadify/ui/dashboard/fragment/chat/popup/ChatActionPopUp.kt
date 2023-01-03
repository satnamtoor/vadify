package com.android.vadify.ui.dashboard.fragment.chat.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.android.vadify.R
import com.android.vadify.data.api.enums.ChatAction
import com.android.vadify.databinding.ChatActionPopupBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.chat_action_popup.*
import kotlinx.android.synthetic.main.message_popup.closeBtn


class ChatActionPopUp(var isUserMute: Boolean, var isCondition: (ChatAction) -> Unit) :
    BaseDialogDaggerListFragment<ChatActionPopupBinding>() {
    override val layoutRes: Int
        get() = R.layout.chat_action_popup


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        subscribe(closeBtn.throttleClicks()) { dialog?.dismiss() }
        if (isUserMute) materialButton.setText(resources.getString(R.string.unmute))
        else materialButton.setText(resources.getString(R.string.mute))


        subscribe(mailBtn.throttleClicks()) {
            isCondition.invoke(ChatAction.Contact_Info)
            dialog?.dismiss()
        }
        subscribe(blockContactBtn.throttleClicks()) {
            isCondition.invoke(ChatAction.Block_Contact)
            dialog?.dismiss()
        }

        subscribe(clearChatBtn.throttleClicks()) {
            isCondition.invoke(ChatAction.Clear_Chat)
            dialog?.dismiss()
        }

        subscribe(deleteChatBtn.throttleClicks()) {
            isCondition.invoke(ChatAction.Delete_Chat)
            dialog?.dismiss()
        }

        subscribe(materialButton.throttleClicks()) {
            isCondition.invoke(ChatAction.MUTE)
            dialog?.dismiss()
        }

    }


}

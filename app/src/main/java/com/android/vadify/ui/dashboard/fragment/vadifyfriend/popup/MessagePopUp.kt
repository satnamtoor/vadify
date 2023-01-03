package com.android.vadify.ui.dashboard.fragment.vadifyfriend.popup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import com.android.vadify.R
import com.android.vadify.databinding.MessagePopupBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.message_popup.*


class MessagePopUp(var isCondition: (Boolean) -> Unit) :
    BaseDialogDaggerListFragment<MessagePopupBinding>() {
    override val layoutRes: Int
        get() = R.layout.message_popup


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        subscribe(closeBtn.throttleClicks()) {
            dialog?.dismiss()
        }



        subscribe(mailBtn.throttleClicks()) {
            sendMessageThroughEmail()
            dialog?.dismiss()
        }


        subscribe(moreBtn.throttleClicks()) {
            sendMessageWithMultipleChooser()
            dialog?.dismiss()
        }


        subscribe(messageBtn.throttleClicks()) {
            isCondition.invoke(true)
            dialog?.dismiss()
        }

        subscribe(copyBtn.throttleClicks()) {
            copyMessageToClipboard()
            dialog?.dismiss()
        }

    }

    private fun copyMessageToClipboard() {

       // Log.d( "copyMessageToClipboard: ",text)
        // Log.d( "copyMessageToClipboard1: ",Gson().toJson(EncryptionViewModel.decryptString(chat.members[1].message)))


        val clipboardManager =
            activityContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("invite_message",  resources.getString(R.string.share_message)+"\n\n"
                + "  " + resources.getString(R.string.android_app)+"\n\n"
                + "  " + resources.getString(R.string.ios_app))
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(activityContext, "Invite message copied to clipboard", Toast.LENGTH_SHORT).show()
        dialog?.dismiss()
    }
    private fun sendMessageThroughEmail() {
        val intent = Intent(Intent.ACTION_SEND).also {
            it.data = Uri.parse("mailto:")
            it.type = "message/rfc822"
            it.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name))
            it.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.share_message)+"\n\n"
                    + "  " + resources.getString(R.string.android_app)+"\n\n"
                    + "  " + resources.getString(R.string.ios_app))
        }


        startActivity(intent)
    }

    private fun sendMessageWithMultipleChooser() {
      //  val intent = Intent(Intent.ACTION_SEND).also {
         //   it.type = "*/*"
         //   it.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.share_message)+ "  " + resources.getString(R.string.vadify_link));
       // }


        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type="text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.share_message)+"\n\n"
                + "  " + resources.getString(R.string.android_app)+"\n\n"
                + "  " + resources.getString(R.string.ios_app));
        startActivity(Intent.createChooser(shareIntent,getString(R.string.send_message_title)))
      //  startActivity(Intent.createChooser(intent, getString(R.string.send_message_title)))
    }
}

package com.android.vadify.ui.chat.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.viewModels
import com.android.vadify.R
import com.android.vadify.data.api.enums.MessageAction
import com.android.vadify.databinding.PopupMessageLayoutBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.android.vadify.ui.chat.adapter.ReplyItemAdapter
import com.android.vadify.ui.chat.viewmodel.ReplyMessageViewModel
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.popup_reply_layout.*


class ShareMessagePopUp(var isCondition: (String) -> Unit) :
    BaseDialogDaggerListFragment<PopupMessageLayoutBinding>() {


    override val layoutRes: Int
        get() = R.layout.popup_message_layout

    private val viewModel: ReplyMessageViewModel by viewModels { viewModelFactory }

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
        initAdapter(ReplyItemAdapter(), replyLayout, viewModel.shareList) {
            isCondition.invoke(it.message)
            dialog?.dismiss()
        }
        subscribe(closeBtn.throttleClicks()) {
            isCondition.invoke(MessageAction.NONE.value)
            dialog?.dismiss()
        }
    }


    override fun onBindView(binding: PopupMessageLayoutBinding) {
        binding.vm = viewModel
    }


}

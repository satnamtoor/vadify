package com.android.vadify.ui.chat.adapter

import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.databinding.ItemReplyLayoutBinding
import com.android.vadify.ui.chat.popup.ReplyMessagePopUp
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass

class ReplyItemAdapter() :
    DataBoundAdapterClass<ReplyMessagePopUp.MessageData, ItemReplyLayoutBinding>(diffCallback) {
    /**
     * The [LayoutRes] for the RecyclerView item
     * This is used to inflate the view.
     */
    override val defaultLayoutRes: Int
        get() = R.layout.item_reply_layout


    override fun bind(
        bind: ItemReplyLayoutBinding,
        itemType: ReplyMessagePopUp.MessageData?,
        position: Int
    ) {
        itemType?.let {
            bind.item = it
        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ReplyMessagePopUp.MessageData>() {
            override fun areItemsTheSame(
                oldItem: ReplyMessagePopUp.MessageData,
                newItem: ReplyMessagePopUp.MessageData
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: ReplyMessagePopUp.MessageData,
                newItem: ReplyMessagePopUp.MessageData
            ): Boolean = oldItem.message == newItem.message

        }
    }

    override fun map(binding: ItemReplyLayoutBinding): ReplyMessagePopUp.MessageData? {
        return binding.item
    }


}

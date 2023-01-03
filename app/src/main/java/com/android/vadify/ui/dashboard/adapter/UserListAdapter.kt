package com.android.vadify.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.data.api.models.ListOfUserResponse
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.databinding.ItemUserListBinding
import com.sdi.joyersmajorplatform.common.livedataext.throttle
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import com.android.vadify.data.api.enums.MessageType
import com.android.vadify.ui.dashboard.fragment.chat.ChatFragment
import com.android.vadify.ui.util.getAlphabetCharacter
import com.android.vadify.ui.util.imageUrl
import com.google.gson.Gson

class UserListAdapter(val userId: String) : DataBoundAdapterClass<ChatThread,
        ItemUserListBinding>(diffCallback) {

    override val defaultLayoutRes: Int
        get() = R.layout.item_user_list

    var selectedUsers: ArrayList<String> = ArrayList()

    private var itemClickEmitter: ObservableEmitter<ChatThread>? = null

    val singleChatClick: Observable<ChatThread> =
        Observable.create<ChatThread> { itemClickEmitter = it }.throttle()

    private var speechItemClickEmitter: ObservableEmitter<ChatThread>? = null
    val speechBtnClick: Observable<ChatThread> =
        Observable.create<ChatThread> {
            speechItemClickEmitter = it
        }.throttle()

    private var popUpBtnEmitter: ObservableEmitter<ChatThread>? = null
    val actionBtnClick: Observable<ChatThread> =
        Observable.create<ChatThread> {
            popUpBtnEmitter = it
        }.throttle()

    @SuppressLint("CheckResult")
    override fun bind(
        bind: ItemUserListBinding,
        itemType: ChatThread?,
        position: Int
    ) {
        itemType?.let { data ->
            bind.item = data
            bind.swipe.close(true)

            Log.d( "adapter-fragment ", Gson().toJson(data))

            if (data.lastMessageType.equals(MessageType.TEXT.value, ignoreCase = true)) {
                bind.textView24.text = data.lastMessage
            } else {
                bind.textView24.text = data.lastMessageType
            }

            if (data.type == "Single" && data.members.isNotEmpty()) {
                bind.name = data.members[0].name
                bind.url = data.members[0].profileImage
                if (data.members[0].profileImage.isNullOrEmpty()) {
                    bind.nameLabel.setText(getAlphabetCharacter(data.members[0].name))
                    bind.circularImageView.visibility = View.GONE
                } else {

                    bind.circularImageView.imageUrl(
                        data.members[0].profileImage,
                        R.drawable.user_placeholder
                    )
                    bind.circularImageView.visibility = View.VISIBLE

                }


                bind.speechToTextBtn.throttleClicks()
                    .subscribe { speechItemClickEmitter?.onNext(data) }
                bind.viewClick.throttleClicks().subscribe { itemClickEmitter?.onNext(data) }
                bind.blockUnblockLayout.throttleClicks().subscribe {
                    bind.swipe.close(true)
                    popUpBtnEmitter?.onNext(data)
                }
            }
            if (data.type == "Group") {
                bind.name = data.name
                bind.url = data.profileImage
                bind.speechToTextBtn.throttleClicks()
                    .subscribe { speechItemClickEmitter?.onNext(data) }
                bind.viewClick.throttleClicks().subscribe { itemClickEmitter?.onNext(data) }
                bind.blockUnblockLayout.throttleClicks().subscribe {
                    bind.swipe.close(true)
                    popUpBtnEmitter?.onNext(data)
                }
                if (data.profileImage.isNullOrEmpty()) {
                    bind.nameLabel.setText(getAlphabetCharacter(data.name))
                    bind.circularImageView.visibility = View.GONE
                } else {

                    bind.circularImageView.imageUrl(data.profileImage, R.drawable.user_placeholder)
                    bind.circularImageView.visibility = View.VISIBLE

                }
            }

        }
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<ChatThread>() {
            override fun areItemsTheSame(
                oldItem: ChatThread,
                newItem: ChatThread
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: ChatThread,
                newItem: ChatThread
            ): Boolean =
                !(oldItem.lastMessage == newItem.lastMessage && oldItem.id == newItem.id/* && oldItem.members[0].profileImage == oldItem.members[0].profileImage*/)
        }
    }

    override fun map(binding: ItemUserListBinding): ChatThread? {
        return binding.item
    }
}

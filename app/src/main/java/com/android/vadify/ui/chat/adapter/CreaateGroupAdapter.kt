package com.android.vadify.ui.chat.adapter

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.data.api.models.ContactSyncingResponse
import com.android.vadify.databinding.ItemCreateGroupBinding
import com.android.vadify.databinding.ItemVadifyFriendBinding
import com.android.vadify.ui.dashboard.fragment.call.activity.DirectCallActivity
import com.sdi.joyersmajorplatform.common.livedataext.throttle
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class CreaateGroupAdapter(val mContext: Context) :
    DataBoundAdapterClass<ContactSyncingResponse.UserGroup, ItemCreateGroupBinding>(diffCallback) {


    override val defaultLayoutRes: Int
        get() = R.layout.item_create_group

    var selectedUsers: ArrayList<String> = ArrayList()

    private var itemClickEmitter: ObservableEmitter<ContactSyncingResponse.Data.User>? = null

    val singleClick: Observable<ContactSyncingResponse.Data.User> =
        Observable.create<ContactSyncingResponse.Data.User> { itemClickEmitter = it }.throttle()

    override fun bind(
        bind: ItemCreateGroupBinding,
        itemType: ContactSyncingResponse.UserGroup?,
        position: Int
    ) {
        itemType?.let { data ->
            bind.item = data

            bind.userSelectionRadio.visibility = View.VISIBLE

            bind.userSelectionRadio.buttonDrawable = mContext.getDrawable(R.drawable.ic_close__1_)
            bind.rowSubtitle.text = data.number
            bind.url = data.profileImage
            //user_selection_radio

            bind.userSelectionRadio.setOnClickListener {
                DirectCallActivity.memberList.removeAt(position)
                notifyDataSetChanged()
            }

        }


    }

    companion object {
        private val diffCallback =
            object : DiffUtil.ItemCallback<ContactSyncingResponse.UserGroup>() {
                override fun areItemsTheSame(
                    oldItem: ContactSyncingResponse.UserGroup,
                    newItem: ContactSyncingResponse.UserGroup
                ): Boolean = oldItem == newItem

                override fun areContentsTheSame(
                    oldItem: ContactSyncingResponse.UserGroup,
                    newItem: ContactSyncingResponse.UserGroup
                ): Boolean =
                    !(oldItem.name == newItem.name && oldItem.number == newItem.number
                            && oldItem.profileImage == newItem.profileImage && oldItem._id == newItem._id)

            }
    }

    override fun map(binding: ItemCreateGroupBinding): ContactSyncingResponse.UserGroup? {
        return binding.item
    }
}
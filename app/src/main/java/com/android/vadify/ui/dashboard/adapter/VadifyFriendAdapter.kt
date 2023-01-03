package com.android.vadify.ui.dashboard.adapter

import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.data.api.models.ContactSyncingResponse
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.databinding.ItemVadifyFriendBinding
import com.android.vadify.ui.util.imageUrlWithoutPlaceHolder
import com.bumptech.glide.Glide
import com.sdi.joyersmajorplatform.common.livedataext.throttle
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class VadifyFriendAdapter(
    val multipleUserSelection: Boolean
) :
    DataBoundAdapterClass<ContactSyncingResponse.Data.User, ItemVadifyFriendBinding>(diffCallback) {


    override val defaultLayoutRes: Int
        get() = R.layout.item_vadify_friend

    var selectedUsers: ArrayList<String> = ArrayList()

    private var itemClickEmitter: ObservableEmitter<ContactSyncingResponse.Data.User>? = null

    val singleClick: Observable<ContactSyncingResponse.Data.User> =
        Observable.create<ContactSyncingResponse.Data.User> { itemClickEmitter = it }.throttle()

    override fun bind(
        bind: ItemVadifyFriendBinding,
        itemType: ContactSyncingResponse.Data.User?,
        position: Int
    ) {
        itemType?.let { data ->
            bind.item = data

            /*if (!data.profileImage.equals(""))
            {
                bind.circularImageView.imageUrlWithoutPlaceHolder(data.)
            }
            */
            Log.d( "vadity: ",""+multipleUserSelection)
            if (multipleUserSelection) {
                if (data.isBlocked) {
                    bind.userView.isEnabled = false

                    bind.cardView.alpha = 0.4F
                } else {

                    bind.userView.isEnabled = true
                    bind.cardView.alpha = 1.0F
                }
                    bind.userSelectionRadio.visibility = View.VISIBLE
                    bind.userSelectionRadio.isChecked = selectedUsers.contains(data._id)
                    bind.rowSubtitle.text = data.profileStatus

            } else {

                bind.userSelectionRadio.visibility = View.GONE
                bind.rowSubtitle.text = data.number
                bind.url = data.profileImage
            }

            bind.userView.setOnClickListener {

                if (multipleUserSelection) {

                    if (selectedUsers?.contains(data._id)!!) {
                        selectedUsers?.remove(data._id)
                    } else {
                        selectedUsers?.add(data._id)

                    }
                    bind.userSelectionRadio.isChecked = selectedUsers?.contains(data._id)

                } else {
                    itemClickEmitter?.onNext(data)
                }
            }
        }
    }

    companion object {
        private val diffCallback =
            object : DiffUtil.ItemCallback<ContactSyncingResponse.Data.User>() {
                override fun areItemsTheSame(
                    oldItem: ContactSyncingResponse.Data.User,
                    newItem: ContactSyncingResponse.Data.User
                ): Boolean = oldItem == newItem

                override fun areContentsTheSame(
                    oldItem: ContactSyncingResponse.Data.User,
                    newItem: ContactSyncingResponse.Data.User
                ): Boolean =
                    !(oldItem.name == newItem.name && oldItem.number == newItem.number
                            && oldItem.profileImage == newItem.profileImage && oldItem._id == newItem._id)

            }
    }

    override fun map(binding: ItemVadifyFriendBinding): ContactSyncingResponse.Data.User? {
        return binding.item
    }
}

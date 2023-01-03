package com.android.vadify.ui.dashboard.adapter

import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.data.api.models.BlockContentResponse
import com.android.vadify.data.api.models.GroupContactDetailResponse
import com.android.vadify.databinding.ItemBlockedContactsBinding
import com.android.vadify.databinding.ItemGroupListBinding
import com.sdi.joyersmajorplatform.common.livedataext.throttle
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class GroupMembersAdapter :
    DataBoundAdapterClass<GroupContactDetailResponse.Member, ItemGroupListBinding>(diffCallback) {


    private var itemClickEmitter: ObservableEmitter<GroupContactDetailResponse.Member>? = null
    val itemClicks: Observable<GroupContactDetailResponse.Member> =
        Observable.create<GroupContactDetailResponse.Member> {
            itemClickEmitter = it
        }.throttle()

    /**
     * The [LayoutRes] for the RecyclerView item
     * This is used to inflate the view.
     */
    override val defaultLayoutRes: Int
        get() = R.layout.item_group_list


    override fun bind(
        bind: ItemGroupListBinding,
        itemType: GroupContactDetailResponse.Member?,
        position: Int
    ) {
    /*    itemType?.let { data ->
            bind.item = data
            bind.blockUnblockLayout.throttleClicks().subscribe {
                itemClickEmitter?.onNext(data)
            }
        }*/


        itemType?.let {
            bind.item = it.userId
        }
    }


    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<GroupContactDetailResponse.Member>() {
            override fun areItemsTheSame(
                oldItem: GroupContactDetailResponse.Member,
                newItem: GroupContactDetailResponse.Member
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: GroupContactDetailResponse.Member,
                newItem: GroupContactDetailResponse.Member
            ): Boolean = !(oldItem.userId.name == newItem.userId.name
                    && oldItem.userId.number == newItem.userId.number
                    && oldItem.userId.profileImage == newItem.userId.profileImage
                    && oldItem.userId._id == newItem.userId._id
                    )


        }
    }

//    override fun map(binding: ItemSettingsBinding): String? {
//        return binding.item
//    }
}

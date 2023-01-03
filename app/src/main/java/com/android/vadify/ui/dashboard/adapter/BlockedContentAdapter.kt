package com.android.vadify.ui.dashboard.adapter

import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.data.api.models.BlockContentResponse
import com.android.vadify.databinding.ItemBlockedContactsBinding
import com.sdi.joyersmajorplatform.common.livedataext.throttle
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass
import io.reactivex.Observable
import io.reactivex.ObservableEmitter

class BlockedContentAdapter :
    DataBoundAdapterClass<BlockContentResponse.Data, ItemBlockedContactsBinding>(diffCallback) {


    private var itemClickEmitter: ObservableEmitter<BlockContentResponse.Data>? = null
    val itemClicks: Observable<BlockContentResponse.Data> =
        Observable.create<BlockContentResponse.Data> {
            itemClickEmitter = it
        }.throttle()

    /**
     * The [LayoutRes] for the RecyclerView item
     * This is used to inflate the view.
     */
    override val defaultLayoutRes: Int
        get() = R.layout.item_blocked_contacts


    override fun bind(
        bind: ItemBlockedContactsBinding,
        itemType: BlockContentResponse.Data?,
        position: Int
    ) {
        itemType?.let { data ->
            bind.item = data
            bind.blockUnblockLayout.throttleClicks().subscribe {
                itemClickEmitter?.onNext(data)
            }
        }


//        itemType?.let {
//            bind.item = it
//        }
    }


    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<BlockContentResponse.Data>() {
            override fun areItemsTheSame(
                oldItem: BlockContentResponse.Data,
                newItem: BlockContentResponse.Data
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: BlockContentResponse.Data,
                newItem: BlockContentResponse.Data
            ): Boolean = !(oldItem.name == newItem.name
                    && oldItem.number == newItem.number
                    && oldItem._id == newItem._id
                    )


        }
    }

//    override fun map(binding: ItemSettingsBinding): String? {
//        return binding.item
//    }
}

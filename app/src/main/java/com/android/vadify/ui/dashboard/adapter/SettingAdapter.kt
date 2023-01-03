package com.android.vadify.ui.dashboard.adapter

import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.databinding.ItemSettingsBinding
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass

class SettingAdapter : DataBoundAdapterClass<String, ItemSettingsBinding>(diffCallback) {
    /**
     * The [LayoutRes] for the RecyclerView item
     * This is used to inflate the view.
     */
    override val defaultLayoutRes: Int
        get() = R.layout.item_settings


    override fun bind(
        bind: ItemSettingsBinding,
        itemType: String?,
        position: Int
    ) {
        itemType?.let {
            bind.item = it

        }

//        itemType?.let {
//            bind.item = it
//        }
    }


    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean = oldItem == newItem

            override fun areContentsTheSame(
                oldItem: String,
                newItem: String
            ): Boolean = oldItem == newItem
        }
    }

    override fun map(binding: ItemSettingsBinding): String? {
        return binding.item
    }
}

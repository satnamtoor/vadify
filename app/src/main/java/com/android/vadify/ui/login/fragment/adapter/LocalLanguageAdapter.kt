package com.android.vadify.ui.login.fragment.adapter

import androidx.recyclerview.widget.DiffUtil
import com.android.vadify.R
import com.android.vadify.data.api.models.LocalLanguageModel
import com.android.vadify.databinding.ItemLocalLanguagesBinding
import com.sdi.joyersmajorplatform.uiview.recyclerview.DataBoundAdapterClass

class LocalLanguageAdapter :
    DataBoundAdapterClass<LocalLanguageModel.LocalLanguageModelItem, ItemLocalLanguagesBinding>(
        diffCallback
    ) {
    /**
     * The [LayoutRes] for the RecyclerView item
     * This is used to inflate the view.
     */
    override val defaultLayoutRes: Int
        get() = R.layout.item_local_languages


    override fun bind(
        bind: ItemLocalLanguagesBinding,
        itemType: LocalLanguageModel.LocalLanguageModelItem?,
        position: Int
    ) {
        itemType?.let {
            bind.item = it
        }
    }


    companion object {
        private val diffCallback =
            object : DiffUtil.ItemCallback<LocalLanguageModel.LocalLanguageModelItem>() {
                override fun areItemsTheSame(
                    oldItem: LocalLanguageModel.LocalLanguageModelItem,
                    newItem: LocalLanguageModel.LocalLanguageModelItem
                ): Boolean = oldItem == newItem

                override fun areContentsTheSame(
                    oldItem: LocalLanguageModel.LocalLanguageModelItem,
                    newItem: LocalLanguageModel.LocalLanguageModelItem
                ): Boolean = oldItem.language == newItem.language
            }
    }

    override fun map(binding: ItemLocalLanguagesBinding): LocalLanguageModel.LocalLanguageModelItem? {
        return binding.item
    }
}

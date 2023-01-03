package com.android.vadify.ui.dashboard.activity

import android.os.Bundle
import com.android.vadify.R
import com.android.vadify.databinding.ActivityChatAccountBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.dashboard.viewmodel.EditChatViewModel
import kotlinx.android.synthetic.main.activity_blocked.toolbar

class EditChatSetting : DataBindingActivity<ActivityChatAccountBinding>() {


    private val viewModel: EditChatViewModel by viewModels()

    override val layoutRes: Int
        get() = R.layout.activity_chat_account


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //  initAdapter(BlockedContentAdapter(),rvBlockedContact,viewModel.contactList)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onBindView(binding: ActivityChatAccountBinding) {
        binding.vm = viewModel
    }
}
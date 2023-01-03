package com.android.vadify.ui.dashboard.activity

import android.os.Bundle
import com.android.vadify.R
import com.android.vadify.databinding.ActivityNotificationBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.dashboard.viewmodel.NotificationViewModel
import kotlinx.android.synthetic.main.activity_blocked.*

class NotificationActivity : DataBindingActivity<ActivityNotificationBinding>() {

    private val viewModel: NotificationViewModel by viewModels()


    override val layoutRes: Int
        get() = R.layout.activity_notification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //  initAdapter(BlockedContentAdapter(),rvBlockedContact,viewModel.contactList)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onBindView(binding: ActivityNotificationBinding) {
        binding.vm = viewModel
    }
}
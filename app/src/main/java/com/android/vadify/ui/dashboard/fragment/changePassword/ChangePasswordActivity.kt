package com.android.vadify.ui.dashboard.fragment.changePassword

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import com.android.vadify.R
import com.android.vadify.ui.baseclass.BaseDaggerActivity
import kotlinx.android.synthetic.main.activity_start_up.*

class ChangePasswordActivity : BaseDaggerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.change_activity_start_up)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navhost_login.findNavController()
            .navigateUp() || super.onSupportNavigateUp()
    }
}

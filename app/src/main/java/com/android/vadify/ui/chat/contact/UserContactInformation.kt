package com.android.vadify.ui.chat.contact

import android.os.Bundle
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.android.vadify.R
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.baseclass.BaseDaggerActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.change_activity_start_up.*

class UserContactInformation : BaseDaggerActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_profile)
        findNavController(R.id.navUserProfile).setGraph(
            R.navigation.nav_user_contact,
            intent.extras
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return navUserContact.findNavController()
            .navigateUp() || super.onSupportNavigateUp()
    }


}

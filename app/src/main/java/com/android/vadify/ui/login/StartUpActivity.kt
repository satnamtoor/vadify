package com.android.vadify.ui.login

import android.os.Bundle
import androidx.activity.viewModels
import androidx.navigation.fragment.findNavController
import com.android.vadify.R
import com.android.vadify.ui.baseclass.BaseDaggerActivity
import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.android.vadify.utils.requestPermissionsCompat
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher
import kotlinx.android.synthetic.main.activity_start_up.*

class StartUpActivity : BaseDaggerActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_up)
        requestPermissionsCompat(arrayOf(android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS), 0)
    }




    override fun onSupportNavigateUp(): Boolean {
        return navhost_login.findNavController()
            .navigateUp() || super.onSupportNavigateUp()
    }


    override fun onBackPressed() {
        /*     val currentFragment= navhost_login.childFragmentManager.primaryNavigationFragment
             if (currentFragment is CommandsFragment && !currentFragment.Recording )
                 super.onBackPressed()
             else
                 showSnackMessage("Recording in process...")*/

        var currentFragment = navhost_login.childFragmentManager.primaryNavigationFragment
        if (currentFragment is CommandsFragment) {
            currentFragment = currentFragment as CommandsFragment
            if (!currentFragment.Recording)
                super.onBackPressed()
            else
                showSnackMessage("Recording in process...")
        } else
            super.onBackPressed()
    }
}

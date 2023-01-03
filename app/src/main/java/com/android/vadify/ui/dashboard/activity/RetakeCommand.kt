package com.android.vadify.ui.dashboard.activity

import android.os.Bundle
import android.util.Log
import com.android.vadify.R
import com.android.vadify.databinding.RetakeCommandBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.login.fragment.CommandsFragment
import kotlinx.android.synthetic.main.command_registration.*

class RetakeCommand : DataBindingActivity<RetakeCommandBinding>() {
    /*override val layoutRes: Int
        get() = R.layout.retake_command
*/
    override val layoutRes: Int
        get() = R.layout.retake_command

    lateinit var fragment : CommandsFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //  setContentView(R.layout.retake_command)
        val intent = intent.extras
        var language = intent?.get("LANGUAGE") as String
        var languageCode = intent?.get("LANG_CODE") as String

        Log.d("language: ", language)

        Log.d("language-code: ", languageCode)

        fragment = CommandsFragment()
        val bundle = Bundle().apply {
            putString("RETAKE", "YES")
        }
        fragment.arguments = bundle
        fragment.RETAKE = "YES"
        fragment.languageNew = language as String?
        fragment.languageCodeNew = languageCode as String?
        supportFragmentManager?.beginTransaction()
            ?.replace(
                R.id.command_containerr,
                fragment,
                "COMMAND_FRAG"
            )
            ?.commit()


    }

    override fun onBackPressed() {
        if (fragment.Recording)
            showSnackMessage("Recording in process...")
        else
            txtLater.callOnClick()
    }

}
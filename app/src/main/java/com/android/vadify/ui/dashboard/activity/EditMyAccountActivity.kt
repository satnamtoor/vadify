package com.android.vadify.ui.dashboard.activity

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.android.vadify.R
import com.android.vadify.databinding.ActivityEditAccountBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.dashboard.fragment.changePassword.ChangePasswordActivity
import com.android.vadify.ui.dashboard.viewmodel.EditMyAccountViewModel
import com.android.vadify.ui.login.StartUpActivity
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.activity_blocked.toolbar
import kotlinx.android.synthetic.main.activity_edit_account.*
import java.io.File

class EditMyAccountActivity : DataBindingActivity<ActivityEditAccountBinding>() {

    private val viewModel: EditMyAccountViewModel by viewModels()

    override val layoutRes: Int
        get() = R.layout.activity_edit_account

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        subscribe(changePassword.throttleClicks()) {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        subscribe(blockedContact.throttleClicks()) {
            startActivity(Intent(this, BlockedActivity::class.java))
        }



        subscribe(logoutBtn.throttleClicks()) {
            withButtonCentered()
            /*val signOutBottomSheet = SignOutBottomSheet.create {
                if (it) {
                    bindNetworkState(viewModel.logoutMethod(), onError = {
                        clearData()
                    }) {
                        clearData()
                    }
                }
            }
            signOutBottomSheet.show(supportFragmentManager, signOutBottomSheet.tag)*/
        }
    }

    private fun clearData() {
        deleteCache(this)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        viewModel.deleteUserandChat()

        val intent = Intent(this, StartUpActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent)
        finishAffinity()
    }

    fun deleteCache(context: Context) {
        try {
            val dir: File = context.cacheDir
            deleteDir(dir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            val children: Array<String> = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            dir.delete()
        } else if (dir != null && dir.isFile()) {
            dir.delete()
        } else {
            false
        }
    }
    override fun onBindView(binding: ActivityEditAccountBinding) {
        binding.vm = viewModel
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateNumber()
        viewModel.numberOfBlockUser()

    }

    fun withButtonCentered() {

        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle("Alert!")
        alertDialog.setMessage("This will delete all your history and account information from Vadify. Do you want to proceed?")

        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE, "Yes"
        ) { dialog, _ ->

            bindNetworkState(viewModel.deleteuser(), onError = {
                clearData()
            }) {
                clearData()
            }

            // viewModel.deleteuser()
            dialog.dismiss()
            /* val intent = Intent(this, StartUpActivity::class.java)
             intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
             intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
             intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
             startActivity(intent)
             finishAffinity()*/
        }

        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE, "No"
        ) { dialog, which -> dialog.dismiss() }
        alertDialog.show()

        val btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = 10f
        btnPositive.layoutParams = layoutParams
        btnNegative.layoutParams = layoutParams
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(resources.getColor(R.color.red_color));
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(resources.getColor(R.color.blue));
    }
}
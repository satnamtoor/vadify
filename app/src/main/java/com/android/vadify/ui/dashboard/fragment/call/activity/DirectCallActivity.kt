package com.android.vadify.ui.dashboard.fragment.call.activity

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.enums.CallStatus
import com.android.vadify.data.api.enums.CallType
import com.android.vadify.data.api.models.ContactSyncingResponse
import com.android.vadify.databinding.ActivityContactBinding
import com.android.vadify.service.Constants
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.CreateGroupFragment
import com.android.vadify.ui.chat.call.CallActivity
import com.android.vadify.ui.chat.call.videocall.VideoCallActivity
import com.android.vadify.ui.contact.viewmodel.ContactViewModel
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.fragment.call.adapter.DirectCallAdapter
import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.widgets.sticklist.SideBar
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.activity_contact.*
import kotlinx.android.synthetic.main.activity_contact.searchText
import kotlinx.android.synthetic.main.activity_contact.textView2
import kotlinx.android.synthetic.main.fragment_call.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


class DirectCallActivity : DataBindingActivity<ActivityContactBinding>(),
    SideBar.OnTouchingLetterChangedListener {


    override val layoutRes: Int
        get() = R.layout.activity_contact


    var activityName: String = ""
    val viewModel: ContactViewModel by viewModels()
    var alreadySelectedUser: ArrayList<String>? = java.util.ArrayList<String>()
    private var directCallAdapter: DirectCallAdapter? = null

    companion object {
        var memberList: ArrayList<ContactSyncingResponse.UserGroup> = ArrayList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var mIntent = intent.extras
        var roomId: String? = null
        var groupName: String? = null
        var groupImage: String? = null
        if (mIntent != null) {
            activityName = mIntent.get("Activity_Name").toString()
            roomId = mIntent.get(BaseBackStack.ROOM_ID).toString()
            groupName = intent.extras?.getString(BaseBackStack.GROUP_NAME)
            groupImage = intent.extras?.getString(BaseBackStack.GROUP_IMAGE)
            textView2.text = resources.getString(R.string.add_member)
            doneBtn.text = "Add"
        } else {
            doneBtn.visibility = View.GONE
            textView2.text = resources.getString(R.string.new_call)
        }

        sideBar.setOnTouchingLetterChangedListener(this)
        sideBar.setTextView(dialog)
        subscribe(cancelBtn.throttleClicks()) { finish() }

        subscribe(doneBtn.throttleClicks()) {
            //  Log.i("Array-list-value", Gson().toJson(memberList))
            /* supportFragmentManager.beginTransaction()
                 .replace(R.id.command_containergroup, CreateGroupFragment(), "COMMAND_FRAG")
                 .addToBackStack(null).commit()*/
            if (!memberList.isEmpty()) {
                val intent = Intent(this@DirectCallActivity, CreateGroupFragment::class.java)
                intent.putExtra(BaseBackStack.ROOM_ID, roomId)
                intent.putExtra(BaseBackStack.GROUP_NAME, groupName)
                intent.putExtra(BaseBackStack.GROUP_IMAGE, groupImage)
                startActivity(intent)
            } else {
                showMessage("Please select at least one contact.")
            }
        }


        val callListener = object : DirectCallAdapter.CallListener {
            override fun audioListener(user: ContactSyncingResponse.Data.User?) {
                callNavigation(CallActivity::class.java, user)
            }

            override fun videoListener(user: ContactSyncingResponse.Data.User?) {
                callNavigation(VideoCallActivity::class.java, user)
            }
        }

        if (mIntent?.getStringArrayList("filteredList") != null) {
            alreadySelectedUser = mIntent.getStringArrayList("filteredList")
           // Log.d("Allfriend", Gson().toJson(alreadySelectedUser))
        }

        viewModel.registerContactList.observe(this, Observer { list ->

         //   Log.d("contact-list", Gson().toJson(list))
            val unBlockedFriendList = list?.filter { !it.isBlocked }
            directCallAdapter = DirectCallAdapter(
                unBlockedFriendList,
                callListener,
                activityName,
                alreadySelectedUser
            )
            contactList.adapter = directCallAdapter
            contactList.smoothScrollToPosition(0)
        })

        viewModel.updateView(false)

        runWithPermissions(Permission.READ_CONTACTS) {
            getContacts()
        }


        searchText.doOnTextChanged { text, _, _, _ ->
            directCallAdapter?.contactList?.let {
                viewModel.filterDataWithCall(text.toString(), it as ArrayList).let {
                    directCallAdapter?.updateList(it)
                }
            }
        }

    }

    override fun onTouchingLetterChanged(content: String?) {
        content?.let {
            val position: Int? = directCallAdapter?.getPositionForSection(it[0].toInt())
            position?.let {
                if (it != -1) {
                    contactList.setSelection(position)
                }
            }
        }
    }


    private fun getContacts() {
        CoroutineScope(Dispatchers.IO).launch {
            val contactList: java.util.ArrayList<Dashboard.ContactInformation> = async {
                val contactList = ArrayList<Dashboard.ContactInformation>()
                val phones = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
                )
                phones?.let { it ->
                    while (it.moveToNext()) {
                        val name =
                            it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        val phoneNumber =
                            it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                .run {
                                    val number = this.replace(Regex("[^0-9+]"), "")
                                    when {
                                        number.length == 10 && !number.contains("+") -> viewModel.updatePhoneNumber(
                                            number
                                        )
                                        else -> number
                                    }
                                }
                        val photoUri =
                            it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI))
                        contactList.add(Dashboard.ContactInformation(name, phoneNumber, photoUri))
                    }
                    it.close()
                }
                contactList.distinctBy { Pair(it.name, it.name) } as ArrayList
            }.await()
            viewModel.updateContactListMethod(contactList)
        }
    }

    override fun onBindView(binding: ActivityContactBinding) {
        binding.vm = viewModel
    }


    private fun callNavigation(navigationClass: Class<*>, user: ContactSyncingResponse.Data.User?) {
        val intent = Intent(this, navigationClass).also {
            it.putExtra(CallActivity.ANOTHER_USERID, user?._id)
            it.putExtra(CallActivity.ANOTHER_USER_NAME, user?.name)
            it.putExtra(CallActivity.ANOTHER_USER_IMAGE, user?.profileImage)
            it.putExtra(Constants.SENDER_OR_RECEIVER, CallType.SENDER.value)
        }
        startActivity(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        memberList.clear()
    }
}
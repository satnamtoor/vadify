package com.android.vadify.ui

import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.databinding.FragmentVadifyFriendBinding
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.contact.ContactActivity
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.adapter.VadifyFriendAdapter
import com.android.vadify.ui.dashboard.fragment.call.activity.DirectCallActivity
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.VadifyFriendFragment
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.popup.InviteFriendPopUp
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.popup.MessagePopUp
import com.android.vadify.ui.dashboard.viewmodel.DashBoardViewModel
import com.android.vadify.ui.dashboard.viewmodel.VadifyFriendViewModel
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.fragment_vadify_friend.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class InviteGroup : DataBindingActivity<FragmentVadifyFriendBinding>() {

    override val layoutRes: Int
        get() = R.layout.fragment_vadify_friend
    val viewModel: VadifyFriendViewModel by viewModels()
    var adapter: VadifyFriendAdapter? = null
    var friendList = mutableListOf<ChatThread>()
    var forwardedChat: Chat? = null
    val viewModelDashboard: DashBoardViewModel by viewModels()
    companion object {
        const val TAG = "VadifyFriendFragment"
        const val SCREENT_TYPE_KEY = "SCREEN_TYPE"
        const val SCREEN_GROUP = "SCREEN_GROUP"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribe(inviteFriend.throttleClicks()) {
            val dialog = InviteFriendPopUp {


                MessagePopUp {
                    Intent(this, ContactActivity::class.java).also {
//                         it.putParcelableArrayListExtra(CONTACT_LIST,viewModel.contactList)
//                         it.putStringArrayListExtra(NOT_REGISTER_LIST,viewModel.notRegisterUser)
                        startActivity(it)
                    }
                }.show(supportFragmentManager, null)
            }
            dialog.show(supportFragmentManager, null)
        }
        viewModel.getChatThreads().observe(this, Observer {
            friendList = it as MutableList<ChatThread>

        })
        when {
            isAllGranted(Permission.READ_CONTACTS) ->
            {
                Log.d(VadifyFriendFragment.TAG, "onResume: call-1")
                getContacts()
            }
            else -> runWithPermissions(
                Permission.READ_CONTACTS
            ) {

                Log.d(VadifyFriendFragment.TAG, "onResume: call-2")
                getContacts()
            }
        }

        if (isAllGranted(
                Permission.CAMERA, Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE)) {
        } else {
            runWithPermissions(
                Permission.CAMERA, Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE) { }
        }
    }
    override fun onResume() {
        super.onResume()
            leftToolbarAction.visibility = View.GONE
            rightToolbarAction.visibility = View.INVISIBLE
           // title.text = "Vadify Friends"
            inviteFriend.visibility = View.VISIBLE







        bindNetworkState(viewModel.contactListNetworkState, loadingIndicator = friendListProgress)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        viewModel.contactSize.observe(this, Observer {
            if (forwardedChat != null) {
                appCompatTextView2.visibility = View.GONE
                relativeGroup.visibility = View.GONE
            } else {
                it?.let {
                    if (it > 0) {
                        appCompatTextView2.visibility = View.VISIBLE
                        relativeGroup.visibility = View.VISIBLE
                    }
                }
            }
        })
        adapter = initAdapter(
            VadifyFriendAdapter(forwardedChat != null),
            vadifyFriendList,
            viewModel.registerContactList
        )

        subscribe(adapter?.singleClick) { userdata ->

            /*  runWithPermissions(
                  Permission.READ_EXTERNAL_STORAGE,
                  Permission.WRITE_EXTERNAL_STORAGE,
              ) {*/
          //  Log.d( "sync-contact:ser ",""+ Gson().toJson(userdata))
           // Log.d( "friend-List ",""+ Gson().toJson(friendList))

            if (viewModel.isBlockedUserOrNot(userdata._id) == true) {
                showMessage(getString(R.string.unblock_message))
            } else {
                val friendData = friendList?.filter {
                    it.members[0].userId == userdata._id && it.members[0].number == userdata.number
                }
                val friendOtherData = friendList?.filter {
                    it.members[0].number != userdata.number
                }
               // Log.d( "onVadify: ", Gson().toJson(friendOtherData))
                NotificationManagerCompat.from(this).cancelAll()
                val intent = Intent(this, ChatActivity::class.java).also {

                    if (friendData?.size!! > 0 && friendData[0].type == "Single") {
                       // Log.d( "onVadify-single: ", Gson().toJson(friendData[0].members[0].userId))
                        it.putExtra(BaseBackStack.ROOM_ID, friendData[0].id)
                        it.putExtra(BaseBackStack.FIRST_TIME_START_CHAT, false)
                        it.putExtra(BaseBackStack.ANOTHER_USER_ID, friendData[0].members[0].userId)

                    } else {
                      //  Log.d( "onVadify-group: ", Gson().toJson(userdata._id))
                        it.putExtra(BaseBackStack.FIRST_TIME_START_CHAT, true)
                        it.putExtra(BaseBackStack.ANOTHER_USER_ID, userdata._id)
                        //  it.putExtra(ROOM_ID, viewModel.preferenceService.getString(R.string.pkey_user_Id))
                    }


                    it.putExtra(BaseBackStack.GROUP_TYPE, "Single")

                    // it.putExtra(ANOTHER_USER_ID, userdata._id)
                    it.putExtra(BaseBackStack.ANOTHER_USER_NAME, userdata.name)
                    it.putExtra(BaseBackStack.ANOTHER_USER_URL, userdata.profileImage)
                    // it.putExtra(FIRST_TIME_START_CHAT, false)
                    it.putExtra(BaseBackStack.LANGUAGE_SWITCH, true)
                    it.putExtra(BaseBackStack.MOTHER_LANGUAGE, userdata.language)
                    it.putExtra(BaseBackStack.PHONE_NUMBER, userdata.number)
                    it.putExtra(BaseBackStack.ANOTHER_USER_LANGUAGE_CODE, userdata.languageCode)
                    it.putExtra(
                        BaseBackStack.IMAGE_PATH,
                        viewModelDashboard.imagePath
                    )
                    it.putExtra(BaseBackStack.GOTO_SPEECH_TO_TEXT, false)

                }
                viewModelDashboard.clearImagePath()
                startActivity(intent)



            }
        }
        subscribe(relativeGroup.throttleClicks()) {
            var mintent: Intent = Intent(this, DirectCallActivity::class.java)
            mintent.putExtra("Activity_Name", "CHAT")
            startActivity(mintent)
        }


    }
    override fun onBindView(binding: FragmentVadifyFriendBinding) {
        binding.vm = viewModel
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
}
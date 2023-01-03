package com.android.vadify.ui.dashboard.fragment.chat

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.data.api.enums.ChatAction
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.databinding.FragmentChatBinding
import com.android.vadify.ui.InviteGroup
import com.android.vadify.ui.baseclass.BaseDaggerListFragment
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.contact.UserContactInformation
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.adapter.UserListAdapter
import com.android.vadify.ui.dashboard.fragment.call.activity.DirectCallActivity
import com.android.vadify.ui.dashboard.fragment.chat.popup.ChatActionPopUp
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.VadifyFriendFragment
import com.android.vadify.ui.dashboard.viewmodel.UserListViewModel
import com.android.vadify.utils.CountryCodeSelector
import com.android.vadify.utils.RxBus
import com.android.vadify.widgets.onTextChange
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import kotlinx.android.synthetic.main.fragment_chat.*

class ChatFragment : BaseDaggerListFragment<FragmentChatBinding>() {

    val viewModel: UserListViewModel by viewModels()
    // val viewModelRef: DashBoardViewModel by viewModels()
    val viewModelChat: ChatViewModel by viewModels()

    override val layoutRes: Int
        get() = R.layout.fragment_chat

    var adapter: UserListAdapter? = null


    companion object {
        const val TAG = "ChatFragment"
        const val SCREENT_TYPE_KEY = "SCREEN_TYPE"
        const val SCREEN_GROUP = "SCREEN_GROUP"
        const val RecyclerViewCache = 100
    }


    @SuppressLint("CheckResult")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // viewModel.deleteData()


        (requireActivity() as Dashboard).viewModel.localCountUpdate.observe(
            viewLifecycleOwner,
            Observer {

                viewModel.updateApi()
                //
            })

            // Log.d( "chat-frage: ", Gson().toJson(viewModel.getChatThreads()))

        val adapter = initAdapter(
            UserListAdapter(viewModel.getUserId()),
            userList,
            viewModel.getChatThreads()
        )
        this.adapter = adapter

        viewModel.filteredChatThreads.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })

        viewModel.forwardedMessageChatThread.observe(viewLifecycleOwner, Observer {
            it.let {
                if (it != null) {
                    callChatList(it)
                }
            }
        })

        subscribe(searchText.onTextChange()) { searchText ->
            viewModel.searchNameChanged(searchText.toString())
        }
        subscribe(addGroup.throttleClicks()) {


            var mintent = Intent(requireContext(), InviteGroup::class.java)
           // mintent.putExtra("Activity_Name", "CHAT")
            startActivity(mintent)



         /*   val transaction = activity?.supportFragmentManager?.beginTransaction()
            if (transaction != null) {
                transaction.replace(R.id.container, VadifyFriendFragment())
                transaction.disallowAddToBackStack()
                transaction.commit()
            }*/

        }



        viewModel.getChatThreadsFromAPI()

        subscribe(adapter.singleChatClick) { data ->
            if (data.isUserBlock) showMessage(getString(R.string.unblock_message))
            else callChatList(data)
        }

        subscribe(adapter.speechBtnClick) { data ->
            if (data.isUserBlock) showMessage(getString(R.string.unblock_message))
            else callChatList(data, true)
        }


        subscribe(adapter.actionBtnClick) { data ->

            viewModelChat.getMemberName(data.id)
            ChatActionPopUp(data.user.mute) { it ->
                val result =
                    CountryCodeSelector(requireActivity()).removeCountryCode(data.members[0].number)

                when (it) {
                    ChatAction.Block_Contact ->
                        if (!data.isUserBlock) bindNetworkState(
                            viewModel.getBlockUnBlockUser(result),
                            progressDialog(R.string.block_contact_progress)
                        ) {
                            data.isUserBlock = true
                            viewModel.updatePreferenceList(result)
                        }
                    ChatAction.Delete_Chat -> bindNetworkState(
                        viewModel.deleteChat(data.id),
                        progressDialog(R.string.delete_chat_progress)
                    ) {
                        viewModel.updateApi()
                    }
                    ChatAction.Clear_Chat -> bindNetworkState(
                        viewModel.clearChat(data.id),
                        progressDialog(R.string.clear_chat_progress)
                    ) {
                        viewModel.updateApi()
                    }
                    ChatAction.Contact_Info -> {

                        Intent(
                            requireContext(),
                            UserContactInformation::class.java
                        ).also { intent ->

                            Log.d( "room-ididid: ",data.id)
                            intent.putExtra(ROOM_ID, data.id)
                            intent.putExtra(FIRST_TIME_START_CHAT, false)
                            intent.putExtra(GROUP_TYPE, data.type)
                            if (data.type.equals("Single")) {
                                intent.putExtra(ANOTHER_USER_ID, data.members[0].userId)
                                intent.putExtra(ANOTHER_USER_NAME, data.members[0].name)
                                intent.putExtra(TYPE, false)
                                intent.putExtra(ANOTHER_USER_URL, data.members[0].profileImage)
                            } else {
                                intent.putExtra(ANOTHER_USER_ID, data.id)
                                intent.putExtra(ANOTHER_USER_NAME, data.name)
                                intent.putExtra(TYPE, true)
                                intent.putExtra(ANOTHER_USER_URL, data.profileImage)
                                intent.putStringArrayListExtra(ChatActivity.MEMBER_IDs, ArrayList(viewModelChat.memberIds))
                            }
                            startActivity(intent)

                        }
                    }
                    ChatAction.MUTE -> {
                        bindNetworkState(
                            viewModel.muteUnmuteMethod(data.id, !data.user.mute),
                            progressDialog(R.string.please_wait)
                        ) {
                            data.user.mute = !data.user.mute
                        }
                    }
                }
            }.show(childFragmentManager, null)
        }
        RxBus.listen(String::class.java).subscribe {
            if (it == "New Message") {
                updateAPI()
            } else {
                viewModel.getChatThread(it)
            }
        }

    }

    //8287952006
    private fun callChatList(data: ChatThread, goToSpeechToTextView: Boolean = false) {
        when {
            isAllGranted(
                Permission.READ_EXTERNAL_STORAGE,
                Permission.WRITE_EXTERNAL_STORAGE,
                Permission.RECORD_AUDIO,
                Permission.CAMERA
            ) -> {
                callMethod(data, goToSpeechToTextView)
            }
            else -> {
                runWithPermissions(
                    Permission.READ_EXTERNAL_STORAGE,
                    Permission.WRITE_EXTERNAL_STORAGE,
                    Permission.RECORD_AUDIO,
                    Permission.CAMERA
                ) {
                    callMethod(data, goToSpeechToTextView)
                }
            }
        }
    }

    private fun callMethod(data: ChatThread, goToSpeechToTextView: Boolean) {

        val intent = Intent(requireActivity(), ChatActivity::class.java).also { intent ->
             Log.d( "full_data ",Gson().toJson(data))


            intent.putExtra(ROOM_ID, data.id)
            if (!data.members.isNullOrEmpty()) {
                if (data.type.equals("Single")) {
                    intent.putExtra(ANOTHER_USER_ID, data.members[0].userId)

                    intent.putExtra(ANOTHER_USER_NAME, data.members[0].name)
                    intent.putExtra(TYPE, false)
                    intent.putExtra(ANOTHER_USER_URL, data.members[0].profileImage)
                } else {

                    intent.putExtra(ANOTHER_USER_ID, data.id)
                    intent.putExtra(ANOTHER_USER_NAME, data.name)
                    intent.putExtra(TYPE, true)
                    intent.putExtra(ANOTHER_USER_URL, data.profileImage)
                }

                intent.putExtra(MOTHER_LANGUAGE, data.members[0].language)
                intent.putExtra(LANGUAGE_SWITCH, data.members[0].motherSwitch)
                intent.putExtra(PHONE_NUMBER, data.members[0].number)
                intent.putExtra(GROUP_TYPE, data.type)

                intent.putExtra(ANOTHER_USER_LANGUAGE_CODE, data.members[0].languageCode)
                intent.putExtra(IMAGE_PATH, (requireActivity() as Dashboard).viewModel.imagePath)
                intent.putExtra(GOTO_SPEECH_TO_TEXT, goToSpeechToTextView)
            }
        }
        (requireActivity() as Dashboard).viewModel.clearImagePath()
        startActivity(intent)

//        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
//        startActivity(intent)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
//            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
//            startActivity(intent)
//        } else{
//
//        }
    }


    override fun onResume() {
        super.onResume()
        viewModel.updateApi()
    }

    fun updateAPI() {
        viewModel.getChatThreadsFromAPINew()
    }
}
package com.android.vadify.ui.chat.contact.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.navArgs
import com.afollestad.assent.Permission
import com.afollestad.assent.isAllGranted
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.api.enums.CallStatus
import com.android.vadify.data.api.enums.CallType
import com.android.vadify.data.api.models.BlockContentResponse
import com.android.vadify.data.api.models.GroupContactDetailResponse
import com.android.vadify.databinding.FragmentAnotherUserContactBinding
import com.android.vadify.service.Constants
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.baseclass.BaseDaggerListFragment
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.ChatActivity.Companion.MEMBER_IDs
import com.android.vadify.ui.chat.call.CallActivity
import com.android.vadify.ui.chat.call.videocall.VideoCallActivity
import com.android.vadify.ui.chat.contact.viewmodel.UserContactViewModel
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.activity.ProfileImage
import com.android.vadify.ui.dashboard.adapter.GroupMembersAdapter
import com.android.vadify.ui.dashboard.fragment.call.activity.DirectCallActivity
import com.android.vadify.ui.dashboard.viewmodel.BlockedContentViewModel
import com.android.vadify.ui.util.imageUrl
import com.android.vadify.utils.CountryCodeSelector
import com.google.gson.Gson
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import kotlinx.android.synthetic.main.fragment_another_user_contact.*
import java.util.*

class AnotherUserContact : BaseDaggerListFragment<FragmentAnotherUserContactBinding>() {

    private val viewModel: UserContactViewModel by viewModels()

    private val viewModelB: BlockedContentViewModel by viewModels()

    // private val args: AnotherUserContactArgs by navArgs()
    val viewModelChat: ChatViewModel by viewModels()
    var result = ""

    var adapter: GroupMembersAdapter? = null
    var bundle: Bundle? = null
    override val layoutRes: Int
        get() = R.layout.fragment_another_user_contact
    var profileImageUrl: String? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bundle = arguments
        initView()
        viewModel.number.observe(requireActivity(), Observer {
            Log.d("phone-number: ", "" + it)
            if (!it.isNullOrEmpty())
                if (it.equals("+16504302408")) {
                    binding.view1.visibility = View.GONE
                    binding.txtBlockName.visibility = View.GONE
                } else {
                    binding.view1.visibility = View.VISIBLE
                    binding.txtBlockName.visibility = View.VISIBLE
                }
            if (getArgsString(GROUP_TYPE) == "Single") {
                videoBtn.background =
                    requireActivity().getDrawable(R.drawable.another_user_videocall_btn)
                callBtn.background = requireActivity().getDrawable(R.drawable.another_user_call_btn)
                textView8.visibility = View.VISIBLE
                textView2.text = "Contact Info"
                result = CountryCodeSelector(requireActivity()).removeCountryCode(it!!)
                if (getArgsBool(IS_BLOCK)) {
                    txtBlockName.text = "Unblock Contact"
                    cardMedia.visibility = View.GONE
                    videoBtn.isEnabled = false
                    callBtn.isEnabled = false
                    messageBtn.isEnabled = false
                } else {
                    if (viewModelB.isBlockedUserOrNot(result) == true) {
                        txtBlockName.text = "Unblock Contact"
                        cardMedia.visibility = View.GONE
                        videoBtn.isEnabled = false

                        callBtn.isEnabled = false
                        messageBtn.isEnabled = false
                    } else {
                        txtBlockName.text = "Block Contact"
                        cardMedia.visibility = View.VISIBLE
                    }


                }

            } else {

                videoBtn.background =
                    requireActivity().getDrawable(R.drawable.ic_videocall_group_btn)

                callBtn.background = requireActivity().getDrawable(R.drawable.ic_call_btn)

                textView8.visibility = View.INVISIBLE
                textView2.text = "Group Info"
                textView11.visibility = View.GONE

                textView10.visibility = View.GONE
                txtBlockName.text = "Exit Group"
                groupCard.visibility = View.VISIBLE
            }
        })

        viewModelB.isBlockFinish.observe(requireActivity(), Observer {
            if (it) {
                requireActivity().finish()
            }

        })


        subscribe(profileImage.throttleClicks()) {
            // Log.d("click-click","click click boom " +viewModel.profileImage.value)

            val intent = Intent(requireActivity(), ProfileImage::class.java)
            intent.putExtra("SCREEN_TYPE", "AnotherUser")
            intent.putExtra("IMAGE", viewModel.profileImage.value)
            startActivity(intent)
            //startActivity(Intent(requireContext(), ProfileImage::class.java))
            //requireActivity().finish()
        }
        subscribe(btnAdd.throttleClicks()) {
            val filteredList = viewModel.groupMember.value?.map { it.userId._id }!!
            val roomId = getArgsString(ROOM_ID)
            var mintent = Intent(requireContext(), DirectCallActivity::class.java)
            mintent.putExtra("Activity_Name", "CHAT")
            mintent.putStringArrayListExtra("filteredList", filteredList as ArrayList<String>)
            mintent.putExtra(ROOM_ID, roomId)
            mintent.putExtra(GROUP_NAME, binding.textView3.text.toString())
            mintent.putExtra(GROUP_IMAGE, profileImageUrl)
            startActivity(mintent)
        }

        subscribe(backArrow.throttleClicks()) {
            requireActivity().finish()
        }

        subscribe(txtBlockName.throttleClicks()) {
            if (getArgsString(GROUP_TYPE) == "Single") {
                val result =
                    CountryCodeSelector(requireActivity()).removeCountryCode(viewModel.number.value!!)
                if (getArgsBool(IS_BLOCK)) {

                    bindNetworkState(
                        viewModelB.getUnblockResponse(
                            BlockContentResponse.Data(
                                getArgsString(ANOTHER_USER_ID)!!,
                                viewModel.userName.value.toString(),
                                result!!
                            )
                        ),
                        progressDialog(R.string.updating)

                    )
                    {
                    }
                } else {

                    //  Log.d("block-list", Gson().toJson(viewModelB.blockedContentResponse.data.value))
                    if (viewModelB.isBlockedUserOrNot(result) == false) {
                        bindNetworkState(
                            viewModel.getBlockUnBlockUser(result!!),
                            progressDialog(R.string.block_contact_progress)
                        ) {
                            viewModel.updatePreferenceList(result!!)
                            requireActivity().finish()
                        }
                    } else {
                        bindNetworkState(
                            viewModelB.getUnblockResponse(
                                BlockContentResponse.Data(
                                    getArgsString(ANOTHER_USER_ID)!!,
                                    viewModel.userName.value.toString(),
                                    result!!
                                )
                            ),
                            progressDialog(R.string.updating)

                        )
                        {
                        }
                    }
                }
            } else {
                bindNetworkState(
                    viewModel.exitRoomRequest(getArgsString(ROOM_ID)!!),
                    progressDialog(R.string.exit_from_group)
                ) {
                    requireActivity().finish()
                }
            }

        }


        subscribe(clearChat.throttleClicks()) {
            bindNetworkState(
                viewModel.clearChat(getArgsString(ROOM_ID)!!),
                progressDialog(R.string.clear_chat_progress)
            ) {
                requireActivity().finish()
            }
        }






        subscribe(callBtn.throttleClicks()) {

            if (isAllGranted(Permission.RECORD_AUDIO)) callNavigation(CallActivity::class.java)
            else runWithPermissions(Permission.RECORD_AUDIO) {
                callNavigation(CallActivity::class.java)
            }

        }



        subscribe(videoBtn.throttleClicks()) {
            if (isAllGranted(Permission.RECORD_AUDIO, Permission.CAMERA)) callNavigation(
                VideoCallActivity::class.java
            )
            else runWithPermissions(Permission.RECORD_AUDIO, Permission.CAMERA) {
                callNavigation(VideoCallActivity::class.java)
            }
        }


        subscribe(messageBtn.throttleClicks()) {
            when {
                getArgsBool(FIRST_TIME_START_CHAT) -> requireActivity().finish()
                isAllGranted(
                    Permission.READ_EXTERNAL_STORAGE,
                    Permission.WRITE_EXTERNAL_STORAGE,
                    Permission.RECORD_AUDIO,
                    Permission.CAMERA
                ) -> {
                    callChatMethod()
                }
                else -> {
                    runWithPermissions(
                        Permission.READ_EXTERNAL_STORAGE,
                        Permission.WRITE_EXTERNAL_STORAGE,
                        Permission.RECORD_AUDIO,
                        Permission.CAMERA
                    ) {
                        callChatMethod()
                    }
                }
            }
        }

        //   var list: MutableLiveData<List<GroupContactDetailResponse.UserId>?> = MutableLiveData()
        //  Log.d( "groupContactdetails: ",Gson().toJson(list.value))
        adapter = initAdapter(
            GroupMembersAdapter(),
            userList,
            viewModel.groupMember
        )

        binding.searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                val filteredList: List<GroupContactDetailResponse.Member> =
                    viewModel.groupMember.value?.filter {
                        it.userId.name.toLowerCase().startsWith(s.toString().toLowerCase())
                    }!!

                var mutableFilter: MutableLiveData<List<GroupContactDetailResponse.Member>>
                mutableFilter = if (!s.toString().isNullOrEmpty())
                    mutableLiveData(filteredList)
                else
                    viewModel.groupMember

                adapter = initAdapter(
                    GroupMembersAdapter(),
                    userList,
                    mutableFilter
                )
            }

        })
        if (getArgsString(GROUP_TYPE) == "Group")
            viewModelChat.getMemberName(getArgsString(ANOTHER_USER_ID)!!)
    }

    private fun getArgsString(key: String): String? {
        return bundle!!.getString(key)
    }

    private fun getArgsBool(key: String): Boolean {
        return bundle!!.getBoolean(key)!!
    }

    private fun callChatMethod() {
        NotificationManagerCompat.from(requireActivity()).cancelAll()
        val intent = Intent(requireActivity(), ChatActivity::class.java).also { intent ->
            intent.putExtra(ROOM_ID, getArgsString(ROOM_ID))
            intent.putExtra(ANOTHER_USER_ID, getArgsString(ANOTHER_USER_ID))


            intent.putExtra(LANGUAGE_SWITCH, true)
            intent.putExtra(MOTHER_LANGUAGE, viewModel.language.value)
            if (getArgsString(GROUP_TYPE) == "Single") {
                intent.putExtra(PHONE_NUMBER, viewModel.number.value)
                intent.putExtra(ANOTHER_USER_NAME, viewModel.userName.value)
                intent.putExtra(ANOTHER_USER_URL, viewModel.profileImage.value)
                intent.putExtra(TYPE, false)
            } else {
                intent.putExtra(TYPE, true)
                intent.putExtra(PHONE_NUMBER, "")
                intent.putExtra(ANOTHER_USER_NAME, viewModel.userNameGroup.value)
                intent.putExtra(ANOTHER_USER_URL, viewModel.profileImageGroup.value)
            }
            val activity = requireActivity()
            if (activity is Dashboard) {
                intent.putExtra(IMAGE_PATH, activity.viewModel.imagePath)
            }
            intent.putExtra(GOTO_SPEECH_TO_TEXT, false)

        }
        startActivity(intent)
        requireActivity().finish()
    }

    private fun initView() {

        viewModel.profileImage(getArgsString(GROUP_TYPE)!!)

        if (getArgsString(GROUP_TYPE) == "Single") {

            viewModel.updateData(getArgsString(ANOTHER_USER_ID)!!)
        } else {
            viewModel.updateData(getArgsString(ROOM_ID)!!)
        }
        viewModel.profileImageUrl.observe(requireActivity(), Observer { it ->
            it?.let {
                profileImageUrl = it
                binding.profileImage.imageUrl(it, R.drawable.user_placeholder)
            }
        })

    }


    override fun onBindView(binding: FragmentAnotherUserContactBinding) {
        binding.vm = viewModel
        /*Log.d(
            "nameeee",
            "" + Gson().toJson(viewModel.userResource.data) + "" + Gson().toJson(viewModel.groupResource.data)
        )*/

    }


    private fun callNavigation(navigationClass: Class<*>) {

        val intent = Intent(requireContext(), navigationClass).also {


            Log.d(
                "another: ",
                "activity " + getArgsString(ANOTHER_USER_ID) + " room " + getArgsString(ROOM_ID)
            )
           // Log.d("member-id: ", Gson().toJson(ArrayList(viewModelChat.memberIds)))

            it.putExtra(CallActivity.ANOTHER_USERID, getArgsString(ANOTHER_USER_ID))
            it.putExtra(Constants.SENDER_OR_RECEIVER, CallType.SENDER.value)
            it.putExtra(ChatActivity.ROOM_ID, getArgsString(ROOM_ID))
            it.putStringArrayListExtra(MEMBER_IDs, ArrayList(viewModelChat.memberIds))
            if (getArgsString(GROUP_TYPE) == "Single") {

                it.putExtra(CallActivity.ANOTHER_USER_NAME, viewModel.userName.value)
                it.putExtra(BaseBackStack.GROUP_TYPE, false)
                it.putExtra(CallActivity.ANOTHER_USER_IMAGE, viewModel.profileImage.value)
            } else {
                it.putExtra(BaseBackStack.GROUP_TYPE, true)
                it.putExtra(CallActivity.ANOTHER_USER_NAME, viewModel.userNameGroup.value)
                it.putExtra(CallActivity.ANOTHER_USER_IMAGE, viewModel.profileImageGroup.value)
            }
        }
        intent
        startActivity(intent)
    }
}
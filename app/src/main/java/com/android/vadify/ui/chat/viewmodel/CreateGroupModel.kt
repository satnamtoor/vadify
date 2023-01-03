package com.android.vadify.ui.chat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.vadify.data.api.models.AddNewGroupRequest
import com.android.vadify.data.api.models.CreateGroupRequest
import com.android.vadify.data.repository.FileUploadRepositry
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.chat.call.viewmodel.CallViewModel
import com.android.vadify.utils.BaseViewModel
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.uiview.NetworkState
import java.io.File
import javax.inject.Inject

class CreateGroupModel @Inject constructor(
    private val fileUploadRepositry: FileUploadRepositry,
    val preferenceService: PreferenceService,
    val userRepository: UserRepository
) : BaseViewModel() {
    val callRequestResponse = MutableLiveData<CallViewModel.CallRequestResponse>()
    val imagePath = mutableLiveData("")


    fun createGroupCallApi(
        groupName: String,
        groupPic: String?,
        membersIds: List<String>
    ): LiveData<NetworkState> {

        val networkRequest = userRepository.createGroupCallRequest(
            CreateGroupRequest(
                groupName,
                groupPic,
                membersIds,
                "Group"

            )
        )
        //Log.d("member_id", "" + membersIds)
        //  val networkRequest = userRepository.updateProfile(uploadData(getProfileRequestModel()))
        subscribe(networkRequest.request) {
           // Log.d("saveCommandData: ", "calling-2")
        }
        return networkRequest.networkState

    }
    fun updateGroupCallApi(
        roomId: String,
        membersIds: List<String>
    ): LiveData<NetworkState> {

        val networkRequest = userRepository.updateGroupCallRequest(
            AddNewGroupRequest(
                roomId,
                membersIds
            )
        )
        //Log.d("member_id", "" + membersIds)
        //  val networkRequest = userRepository.updateProfile(uploadData(getProfileRequestModel()))
        subscribe(networkRequest.request) {
            // Log.d("saveCommandData: ", "calling-2")
        }
        return networkRequest.networkState

    }

    fun compressedImageFile(file: File,groupName: String, membersIds: List<String>): LiveData<NetworkState> {
        return fileUploadRepositry.uploadProfilePic(file, "profile").also { it ->
            subscribe(it.request) {
                if (it.isSuccessful) {
                    imagePath.postValue(it.body()?.data?.url)
                    createGroupCallApi(groupName,it.body()?.data?.url,membersIds)
                }
            }
        }.networkState
    }

}
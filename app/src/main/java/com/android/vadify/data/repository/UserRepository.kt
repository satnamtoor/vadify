package com.android.vadify.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.android.vadify.R
import com.android.vadify.data.api.AuthApi
import com.android.vadify.data.api.models.*
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.data.db.chatThread.ChatThreadCache
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.data.db.contact.Contact
import com.android.vadify.data.db.contact.ContactListCache
import com.android.vadify.data.db.filter.ChatListFilter
import com.android.vadify.data.network.*
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.reactivex.Single
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject


class UserRepository @Inject constructor(
    private val appExecutors: AppExecutors,
    private val authApi: AuthApi,
    private val cache: ContactListCache,
    val chatThreadCache: ChatThreadCache,
    val chatListCache: ChatListCache,
    val preferenceService: PreferenceService
) {

    /*fun getChatThreads(): LiveData<List<ChatThread>?> {
        return chatThreadCache.getAll()
    }*/

    fun signInApi(loginRequestModel: OtpEncriptedRequestModel): IRequest<Response<LoginResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<LoginResponse> {
                override fun createNetworkRequest(): Single<Response<LoginResponse>> {
                    return authApi.apiUserLogin(loginRequestModel)
                }
            })
    }

    data class OtpEncriptedRequestModel(val data: String?)

    fun saveCommandsApi(commandsRequestModel: Commands): IRequest<Response<CommandsResponse>> {
        Log.d("saveCommandsApi:", "api-calling")
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<CommandsResponse> {
                override fun createNetworkRequest(): Single<Response<CommandsResponse>> {
                    Log.d("saveCommandsApi:", "api-calling-1")
                    return authApi.apiUserCommands(commandsRequestModel)
                }
            })
    }

    fun getBlockUnBlockUser(userId: String?): IRequest<Response<JsonObject>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<JsonObject> {
            override fun createNetworkRequest(): Single<Response<JsonObject>> {
                return authApi.getBlockUnBlockUser(userId)
            }
        })
    }

    fun clearChat(roomId: String?): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.clearChat(roomId)
            }
        })
    }

    fun deleteChat(roomId: String?): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.deleteChat(roomId)
            }
        })
    }

    fun deleteUser(): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.deleteUser()
            }
        })
    }


    fun muteUnmute(
        roomId: String?,
        muteUnMuteRequest: MuteUnMuteRequest
    ): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.muteUnmute(roomId, muteUnMuteRequest)
            }
        })
    }




    fun getBlockUser(): IResource<BlockContentResponse> {
        return NetworkResource(appExecutors, object :
            IRetrofitNetworkRequestCallback.IRetrofitNetworkResourceCallback<BlockContentResponse, BlockContentResponse> {
            override fun mapToLocal(response: BlockContentResponse): BlockContentResponse {
                return response
            }

            override fun createNetworkRequest(): Single<Response<BlockContentResponse>> {
                return authApi.getBlockedUser()
            }
        })
    }

    fun getBlockUserChat(): IRequest<Response<BlockContentResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<BlockContentResponse> {
                override fun createNetworkRequest(): Single<Response<BlockContentResponse>> {
                    return authApi.getBlockedUser()
                }
            })
    }



    fun getStaticContent(): IRequest<Response<StaticContentResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<StaticContentResponse> {
                override fun createNetworkRequest(): Single<Response<StaticContentResponse>> {
                    return authApi.getStaticKey()
                }
            })
    }

    fun updateContactList(contactRequestModel: List<Dashboard.ContactInformation>): IResource<ContactSyncingResponse> {
        return NetworkResource(
            appExecutors,
            object :
                IRetrofitNetworkRequestCallback.IRetrofitNetworkResourceCallback<ContactSyncingResponse, ContactSyncingResponse> {
                override fun mapToLocal(response: ContactSyncingResponse): ContactSyncingResponse {
                    //Log.d( "mapToLocal: ",Gson().toJson(response))
                    cache.insert(contactRequestModel.map { Contact.Mapper.from(it) })
                    return response
                }

                override fun createNetworkRequest(): Single<Response<ContactSyncingResponse>> {

                    return authApi.contactSyncing(ContactRequestModel(contactRequestModel.map { it.phone }))
                }
            })
    }

    fun d(TAG: String?, message: String) {
        val maxLogSize = 2000
        for (i in 0..message.length / maxLogSize) {
            val start = i * maxLogSize
            var end = (i + 1) * maxLogSize
            end = if (end > message.length) message.length else end
            Log.d(TAG, message.substring(start, end))
        }
    }

    fun otpVerification(loginRequestModel: LoginFragmentViewModel.OtpEncriptedRequestModel): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {

                //  Log.i("otp-data", "" + Gson().toJson(loginRequestModel))
                return authApi.otpVerification(loginRequestModel)
            }
        })
    }

    fun changePasswordRequest(changePasswordRequestModel: ChangeNumberRequestModel): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.changeNumberStep1(changePasswordRequestModel)
            }
        })
    }

    fun exitGroup(roomid: String): IRequest<Response<ExitGroupResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<ExitGroupResponse> {
                override fun createNetworkRequest(): Single<Response<ExitGroupResponse>> {
                    return authApi.exitRoom(roomid)
                }
            })
    }
    fun updatedUserLanguage(
        roomId: String?,
        updateLanguageRequest: UpdateLanguageRequest
    ): IRequest<Response<UpdateLanguageResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<UpdateLanguageResponse> {
                override fun createNetworkRequest(): Single<Response<UpdateLanguageResponse>> {
                    return authApi.updateLanguage(roomId,updateLanguageRequest)
                }
            })
    }



    fun changePasswordWithOtpRequest(changeNumberWithOtpModel: ChangeNumberWithOtpModel): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.changeNumberStep2(changeNumberWithOtpModel)
            }
        })
    }


    fun getListOfUser(userId: String, string: String?): IResource<ListOfUserResponse> {
        return NetworkResource(appExecutors, object :
            IRetrofitNetworkRequestCallback.IRetrofitNetworkResourceCallback<ListOfUserResponse, ListOfUserResponse> {
            override fun mapToLocal(response: ListOfUserResponse): ListOfUserResponse {
               // Log.d("message-response: ", Gson().toJson(response))
                var listOfUserResponse = ChatListFilter.Mapper.from(string, response)
                insertToLocalDatabase(response)
              //  Log.d("chatlist: ", Gson().toJson(listOfUserResponse))
                return listOfUserResponse
            }

            override fun createNetworkRequest(): Single<Response<ListOfUserResponse>> {
                Log.d("user-id: ", userId)

                return authApi.getUserList(userId)
            }
        })
    }

    fun insertToLocalDatabase(response: ListOfUserResponse) {

        var mutableResponse = response
         chatThreadCache.deleteAll()

        /*
        When the chat messages are deleted from server we get empty message fields.
        In that case we get the last message field values from Chat table
         */
       // Log.d("chatlistaaa-response-all","as  "+ Gson().toJson(mutableResponse))
        mutableResponse.data.forEach {
         //   Log.d("chatlistaaa-response","as  "+ Gson().toJson(it))
            var isDecrypting = true
            if (it.user.lastMessage.isNullOrEmpty()){
                val lastMessage = chatListCache.getLastMessage(it._id)
              //  Log.d("chatlistaaa-response-chatth","as  "+ Gson().toJson(lastMessage))
               if (lastMessage!=null) {
                    if (it.type != "" && it.lastMessageReceiver.isNullOrEmpty()) {

                        it.lastMessageReceiver = lastMessage.messageReceiver
                        it.lastMessageSender = lastMessage.messageSender
                        if(lastMessage.members.isNotEmpty()){
                       it.lastMessage = lastMessage.members.filter { it1 ->
                            it1.userId != lastMessage.from_id
                        }[0].message
                            }
                       // if (lastMessage.from_id)
                       // it.lastMessage = lastMessage.translatedText
                        it.lastMessageType = lastMessage.type
                        it.lastMessageDate = lastMessage.updatedAt
                        isDecrypting = false
                    }
                }
            }
/*
            if (it.type != "" && it.lastMessageReceiver.isNullOrEmpty() && it.lastMessageType.isNullOrEmpty()) {

                val lastMessage = chatListCache.getLastMessage(it._id)

                if (lastMessage != null) {
                    it.lastMessageReceiver = lastMessage.messageReceiver
                    it.lastMessageSender = lastMessage.messageSender
                    it.lastMessage = lastMessage.message
                    it.lastMessageType = lastMessage.type
                    it.lastMessageDate = lastMessage.updatedAt

                    isDecrypting = false
                }
            }*/

            // FIX: If members count is 0 then the user has been deleted, delete the chat thread too.
            if (it.members.isEmpty()) {
                Log.d("chatlistDelete","as  ")
                chatListCache.deleteSpecificChat(it._id)
                chatThreadCache.deleteChatThread(it._id)
                //badgeService.updateBadgeFromServer()

            } else {
                // Log.d("insertToLocalDatabase: ", Gson().toJson(it))
                chatThreadCache.insert(
                    ChatThread.Mapper.fromSingle(
                        it,
                        isDecrypting = isDecrypting
                    )
                )
            }
        }
    }

    fun getListOfUserNew(userId: String, string: String?) {
        Log.d("UserIdOfUser", userId)
        val call = authApi.getUserListNew(userId)
        call.enqueue(object : Callback<ListOfUserResponse> {
            override fun onFailure(call: Call<ListOfUserResponse>, t: Throwable) {}

            override fun onResponse(
                call: Call<ListOfUserResponse>,
                response: Response<ListOfUserResponse>
            ) {

                if (response.isSuccessful) {
                    response.body()?.let {
                   //     Log.d("new_user_list", Gson().toJson(it))
                        insertToLocalDatabase(it)
                    }
                }
            }
        })
    }

    fun updateProfile(profileUpdate: ProfileUpdateRequestModel): IRequest<Response<ProfileUpdateResponse>> {

        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<ProfileUpdateResponse> {
                override fun createNetworkRequest(): Single<Response<ProfileUpdateResponse>> {
                    return authApi.updateProfile(profileUpdate)
                }
            })
    }


    fun logoutMethod(): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.logoutMethod()
            }
        })
    }

    fun getContactInfoMethod(userId: String?): IResource<ContactDetailResponse> {
        return NetworkResource(appExecutors, object :
            IRetrofitNetworkRequestCallback.IRetrofitNetworkResourceCallback<ContactDetailResponse, ContactDetailResponse> {
            override fun mapToLocal(response: ContactDetailResponse): ContactDetailResponse {
                //Log.d("single-response:", "" + Gson().toJson(response))
                return response
            }

            override fun createNetworkRequest(): Single<Response<ContactDetailResponse>> {
                return authApi.getContactInformation(userId)
            }
        })
    }


    fun getGroupInfo(groupId: String?): IResource<GroupContactDetailResponse> {
        return NetworkResource(appExecutors, object : IRetrofitNetworkRequestCallback.
        IRetrofitNetworkResourceCallback<GroupContactDetailResponse, GroupContactDetailResponse> {
            override fun mapToLocal(response: GroupContactDetailResponse): GroupContactDetailResponse {
               // Log.d("group-response:", "" + Gson().toJson(response))
                return response
            }
            override fun createNetworkRequest(): Single<Response<GroupContactDetailResponse>> {
                return authApi.groupInfo(groupId)
            }
        })
    }


    fun singleCallRequest(singleCallRequest: SingleCallRequest): IRequest<Response<CallRequestResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<CallRequestResponse> {
                override fun createNetworkRequest(): Single<Response<CallRequestResponse>> {
                        return authApi.singleCall(singleCallRequest)
                }
            })
    }

    fun groupCallRequest(groupCallRequest: GroupCallRequest): IRequest<Response<CallRequestResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<CallRequestResponse> {
                override fun createNetworkRequest(): Single<Response<CallRequestResponse>> {
                    return authApi.groupCall(groupCallRequest)
                }
            })
    }


    fun callToken(): IRequest<Response<CallReceiverResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<CallReceiverResponse> {
                override fun createNetworkRequest(): Single<Response<CallReceiverResponse>> {
                    return authApi.callToken()
                }
            })
    }


    fun callTokenSingle(): Single<Response<CallReceiverResponse>> {
        return authApi.callToken()
    }

    fun callTokenUpdateSingle(
        id: String,
        callStatusRequest: CallStatusRequest
    ): Single<Response<CallReceiverResponse>> {
        return authApi.updateCallLogs(id, callStatusRequest)
    }

    fun updateLanguageSwitchState(
        motherSwitch: Boolean
    ): Single<Response<Unit>> {
        return authApi.updateLanguageSwitchState(
            motherSwitch,
            UpdateLanguageSwitchRequestModel(motherSwitch)
        )
    }

    fun deleteMessage(messageId: String): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.deleteMessage(messageId)
            }
        })
    }

    fun markChatAsRead(roomIds: List<String>): IRequest<Response<Unit>> {
        return NetworkRequest(appExecutors, object : IRetrofitNetworkRequestCallback<Unit> {
            override fun createNetworkRequest(): Single<Response<Unit>> {
                return authApi.markChatAsRead(MarkChatAsReadRequest(roomIds))
            }
        })
    }

    fun getAllLanguageResponse(): IRequest<Response<AllLanguageResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<AllLanguageResponse> {
                override fun createNetworkRequest(): Single<Response<AllLanguageResponse>> {
                    return authApi.getAllLanguages()
                }
            })
    }

    fun createGroupCallRequest(singleCallRequest: CreateGroupRequest): IRequest<Response<CreateRoomResponce>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<CreateRoomResponce> {
                override fun createNetworkRequest(): Single<Response<CreateRoomResponce>> {
                    return authApi.createRoom(singleCallRequest)
                }
            })
    }

    fun createUploadRequest(singleCallRequest: UploadAwsURL): IRequest<Response<UploadAWSResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<UploadAWSResponse> {
                override fun createNetworkRequest(): Single<Response<UploadAWSResponse>> {
                    return authApi.uploadImageAws(singleCallRequest)
                }
            })
    }

    fun updateGroupCallRequest(singleCallRequest: AddNewGroupRequest): IRequest<Response<Unit>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<Unit> {
                override fun createNetworkRequest(): Single<Response<Unit>> {
                    return authApi.updateRoom(singleCallRequest)
                }
            })
    }

   fun getUserLanguageResponse(roomId: String?): IRequest<Response<UserLanguageResponseModel>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<UserLanguageResponseModel> {
                override fun createNetworkRequest(): Single<Response<UserLanguageResponseModel>> {
                    return authApi.userLanguage(roomId)
                }
            })
    }


    fun getSupportResponse(): IRequest<Response<SupportResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<SupportResponse> {
                override fun createNetworkRequest(): Single<Response<SupportResponse>> {
                    return authApi.getSupportInfo()
                }
            })
    }



    fun getUserCommands(): IRequest<Response<UserCommandResponse>> {
        return NetworkRequest(
            appExecutors,
            object : IRetrofitNetworkRequestCallback<UserCommandResponse> {
                override fun createNetworkRequest(): Single<Response<UserCommandResponse>> {
                    return authApi.getUserCommand()
                }
            })
    }


}

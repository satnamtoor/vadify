package com.android.vadify.data.api

import com.android.vadify.data.api.models.*
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.data.repository.UserRepository
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.google.gson.JsonObject
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {

    companion object {
        const val MODULE_PATH = "api/v1/"
    }


    @GET("block-unblock/{userId}/")
    fun getBlockUnBlockUser(@Path("userId") userId: String?): Single<Response<JsonObject>>


    @DELETE("clear-chat/{roomId}/")
    fun clearChat(@Path("roomId") userId: String?): Single<Response<Unit>>


    @DELETE("delete-chat/{roomId}/")
    fun deleteChat(@Path("roomId") userId: String?): Single<Response<Unit>>

    @PUT("mute-unmute-room/{roomId}/")
    fun muteUnmute(
        @Path("roomId") userId: String?,
        @Body muteUnMuteRequest: MuteUnMuteRequest
    ): Single<Response<Unit>>


    @GET("blocked-users")
    fun getBlockedUser(): Single<Response<BlockContentResponse>>


    @GET("support-info")
    fun getSupportInfo(): Single<Response<SupportResponse>>


    @GET("common/static-content")
    fun getStaticKey(): Single<Response<StaticContentResponse>>




    @POST("login")
    fun apiUserLogin(@Body loginRequestModel: UserRepository.OtpEncriptedRequestModel): Single<Response<LoginResponse>>

    @POST("command")
    fun apiUserCommands(@Body commandsRequestModel: Commands): Single<Response<CommandsResponse>>


    @POST("get-otp")
    fun otpVerification(@Body loginRequestModel: LoginFragmentViewModel.OtpEncriptedRequestModel): Single<Response<Unit>>

    @PUT("profile")
    fun updateProfile(@Body profileUpdateReqestModel: ProfileUpdateRequestModel): Single<Response<ProfileUpdateResponse>>

    @DELETE("logout")
    fun logoutMethod(): Single<Response<Unit>>

    @DELETE("delete-account")
    fun deleteUser(): Single<Response<Unit>>

    @POST("contact-syncing")
    fun contactSyncing(@Body contactRequestModel: ContactRequestModel): Single<Response<ContactSyncingResponse>>

    @GET("room/{userId}")
    fun getUserList(@Path("userId") userId: String): Single<Response<ListOfUserResponse>>

    @GET("room/{userId}")
    fun getUserListNew(@Path("userId") userId: String): Call<ListOfUserResponse>

    @GET("room-chat-list/{roomId}")
    fun getChatList(
        @Path("roomId") roomId: String?,
        @Query("page") pageNo: Int,
        @Query("count") count: String?
    ): Single<Response<MessageResponseList>>



    @PUT("change-number")
    fun changeNumberStep2(@Body changeNumberWithOtpModel: ChangeNumberWithOtpModel): Single<Response<Unit>>




    @POST("single-call")
    fun singleCall(@Body singleCallRequest: SingleCallRequest): Single<Response<CallRequestResponse>>

    @POST("group-call")
    fun groupCall(@Body groupCallRequest: GroupCallRequest): Single<Response<CallRequestResponse>>


    @GET("call-token")
    fun callToken(): Single<Response<CallReceiverResponse>>

    @PUT("update-call-log/{callLogId}")
    fun updateCallLogs(
        @Path("callLogId") callLogId: String?,
        @Body callStatusRequest: CallStatusRequest
    ): Single<Response<CallReceiverResponse>>


    @GET("call-logs")
    fun getCallLogs(
        @Query("page") pageNo: Int,
        @Query("count") count: String?
    ): Single<Response<CallLogResponse>>

    @PUT("mother-switch")
    fun updateLanguageSwitchState(
        @Query("motherSwitch") motherSwitch: Boolean,
        @Body updateLanguageSwitchRequestModel: UpdateLanguageSwitchRequestModel
    ): Single<Response<Unit>>

    @DELETE("delete-message/{messageId}")
    fun deleteMessage(@Path("messageId") messageId: String): Single<Response<Unit>>

    @PUT("read-chat")
    fun markChatAsRead(@Body markChatAsReadRequest: MarkChatAsReadRequest): Single<Response<Unit>>

    @GET("common/languages")
    fun getAllLanguages(): Single<Response<AllLanguageResponse>>

    @POST("create-room")
    fun createRoom(@Body singleCallRequest: CreateGroupRequest): Single<Response<CreateRoomResponce>>

    @PUT("add-room-members")
    fun updateRoom(@Body singleCallRequest: AddNewGroupRequest): Single<Response<Unit>>


    @GET("contact-info/{userId}/")
    fun getContactInformation(@Path("userId") userId: String?): Single<Response<ContactDetailResponse>>


    @GET("room-info/{roomId}/")
    fun groupInfo(@Path("roomId") roomId: String?): Single<Response<GroupContactDetailResponse>>


    @PUT("leave-room/{roomId}/")
    fun exitRoom(@Path("roomId") roomId: String?): Single<Response<ExitGroupResponse>>

    @PUT("change-number-otp")
    fun changeNumberStep1(@Body changeNumberRequestModel: ChangeNumberRequestModel): Single<Response<Unit>>


    @PUT("update-language/{roomId}/")
    fun updateLanguage(  @Path("roomId") userId: String?
                         ,@Body updateRequest: UpdateLanguageRequest)
    : Single<Response<UpdateLanguageResponse>>

   @GET("user-language/{roomId}/")
   fun userLanguage(@Path("roomId") roomId: String?): Single<Response<UserLanguageResponseModel>>

    @GET("users-command")
    fun getUserCommand(): Single<Response<UserCommandResponse>>

    @POST("common/getpresignedurl")
    fun uploadImageAws(@Body singleRequest: UploadAwsURL): Single<Response<UploadAWSResponse>>

}
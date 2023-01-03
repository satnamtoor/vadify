package com.android.vadify

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDex
import androidx.work.Configuration
import androidx.work.WorkManager
import com.android.vadify.data.api.enums.CallStatus
import com.android.vadify.di.DaggerAppComponent
import com.android.vadify.service.MessageResponse
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.jakewharton.threetenabp.AndroidThreeTen
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.twilio.video.*
import dagger.android.support.DaggerApplication
import tvi.webrtc.Camera1Enumerator


class VadifyApplication : DaggerApplication() {

    var updateAudioCallStatus = MutableLiveData<VideoCallData>()
    var updateVideoCallStatus = MutableLiveData<VideoCallData>()
    var localVideoTrack: LocalVideoTrack? = null
    var camera1Enumerator: Camera1Enumerator? = null
    var cameraCapturer: CameraCapturer? = null
    var remoteParticipantVideo : ArrayList<RemoteVideoTrack> = ArrayList()
    var remoteParticipantAudio : ArrayList<String> = ArrayList()

    sealed class VideoCallType {
        object GroupCall : VideoCallType()
        object SingleCall : VideoCallType()
        object None : VideoCallType()
    }
    var videoCallType : VideoCallType?=null

    var firstTimeRoomId = mutableLiveData("")

    private val appComponent by lazy {
        DaggerAppComponent
            .builder()
            .application(this)
            .build()
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }


    override fun onCreate() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
        super.onCreate()
        initWorkManager()
        AndroidThreeTen.init(this)
        FirebaseApp.initializeApp(this)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        AppCenter.start(
            this, "bfaca87b-aea6-464a-839d-9652591273ce",
            Analytics::class.java, Crashes::class.java
        )
        AppCenter.setLogLevel(Log.VERBOSE)

    }


    private fun initWorkManager() {
        WorkManager.initialize(this, Configuration.Builder().run {
            setWorkerFactory(appComponent.daggerWorkerFactory())
            build()
        })
    }


    fun updateAudioCallStatus(
        roomListener: CallStatus,
        room: Room?,
        remoteParticipant: RemoteParticipant?
    ) {
        updateAudioCallStatus.postValue(VideoCallData(roomListener, room, remoteParticipant))
    }

    fun updateVideoCallStatus(
        roomListener: CallStatus,
        room: Room?,
        remoteParticipant: RemoteParticipant?
    ) {
        updateVideoCallStatus.value = VideoCallData(roomListener, room, remoteParticipant)
    }

    data class VideoCallData(
        var callStatus: CallStatus,
        val room: Room?,
        val remoteParticipant: RemoteParticipant?
    )


    override fun applicationInjector() = appComponent

    companion object {
        var messageList: HashMap<String, ArrayList<MessageResponse>?> =
            HashMap()
        var notificationList: ArrayList<MessageResponse>? = null
        fun addMessage(senderMobile: String, message: MessageResponse) {
            notificationList = ArrayList()
            if (messageList[senderMobile] != null) {
                notificationList = messageList[senderMobile]
            }
            notificationList!!.add(message)
            messageList[senderMobile] = notificationList
        }
        var progress : MutableLiveData<HashMap<String,Int>>  = mutableLiveData(HashMap())
       var progressDownload : MutableLiveData<HashMap<String,Int>>  = mutableLiveData(HashMap())
    }
}
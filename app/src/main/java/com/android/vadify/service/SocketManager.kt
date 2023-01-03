package com.android.vadify.service

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.android.vadify.BuildConfig
import com.android.vadify.R
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.utils.RxBus
import dagger.android.AndroidInjection
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URISyntaxException
import javax.inject.Inject

class SocketManager  {

    @Inject
    lateinit var preferenceService: PreferenceService

    companion object {
        val instance = SocketManager()

        const val MESSAGE = "MESSAGE"
        const val ACKNOWLEDGE_MESSAGE = "ACKNOWLEDGE_MESSAGE"
        const val RECEIVE_MESSAGE = "NEW_MESSAGE"
        const val CALL_LOG_STATUS = "CALL_LOG_STATUS"
        const val ONLINE_STATUS = "ONLINE_STATUS"
        const val JOIN_ONLINE_STATUS = "JOIN_ONLINE_STATUS"
        const val LEAVE_ONLINE_STATUS = "LEAVE_ONLINE_STATUS"
        const val TYPING = "TYPING"
        const val DISPLAY_TYPING = "DISPLAY_TYPING"
        const val MOTHER_LANGUAGE_SWITCH = "MOTHER_SWITCH"
        const val USER_LANGUAGE_CHANGE = "USER_LANGUAGE"
        const val TIMER = 60000L
        const val DELAY = 1000L
    }

    private var mSocket: Socket? = null

    private fun getSocketInstance(mContext: Activity?=null): Socket {

        if (mSocket == null) {
            try {
                val opts = IO.Options()
                opts.transports = arrayOf(WebSocket.NAME)
                opts.forceNew = true
                opts.reconnection = true
                val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
                val userID = defaultSharedPreferences.getString("pkey_user_Id", "")


                mSocket = IO.socket(
                    BuildConfig.API_URL + "?userId=" + userID,
                    opts
                )
            } catch (e: URISyntaxException) {
                Log.e("message are", "" + e.message.toString())
            }
        }
        return mSocket!!
    }

    fun isSocketConnected(): Boolean {
        return getSocketInstance().connected()
    }

    fun disConnect() {

        getSocketInstance().off(RECEIVE_MESSAGE, onNewMessage)
        getSocketInstance().off(ONLINE_STATUS, onlineStatus)
        getSocketInstance().off(DISPLAY_TYPING, typingResult)
        getSocketInstance().off(MOTHER_LANGUAGE_SWITCH, motherLanguageSwitchChanged)
        getSocketInstance().off(USER_LANGUAGE_CHANGE, userLanguageChanged)
        getSocketInstance().off(CALL_LOG_STATUS,callLogStatus)
        getSocketInstance().disconnect()
    }

    fun connect(mContext: Activity?=null) {

        getSocketInstance(mContext).on(RECEIVE_MESSAGE, onNewMessage)
        getSocketInstance(mContext).on(ONLINE_STATUS, onlineStatus)
        getSocketInstance(mContext).on(DISPLAY_TYPING, typingResult)
        getSocketInstance(mContext).on(MOTHER_LANGUAGE_SWITCH, motherLanguageSwitchChanged)
        getSocketInstance(mContext).on(USER_LANGUAGE_CHANGE, userLanguageChanged)
        getSocketInstance(mContext).on(CALL_LOG_STATUS,callLogStatus)

        getSocketInstance(mContext).connect()
            .on(Socket.EVENT_CONNECT) {
                println("socket connected--")
            }
            .on(Socket.EVENT_DISCONNECT) {
                println("socket disconnected--")
            }
            .on(Socket.EVENT_CONNECT_ERROR) {
                println("SOCKET CONNECTION ERROR-- $it")
            }
    }

    private val onNewMessage: Emitter.Listener = Emitter.Listener { args ->
        RxBus.publish(SocketMessage(SocketMessage.Type.NEW_MESSAGE, args))
    }

    private val onlineStatus: Emitter.Listener = Emitter.Listener { args ->
        //viewModel.checkOnListStatus(args)
        RxBus.publish(SocketMessage(SocketMessage.Type.ONLINE_STATUS, args))
    }

    private val typingResult: Emitter.Listener = Emitter.Listener { args ->

        RxBus.publish(SocketMessage(SocketMessage.Type.TYPING_RESULT, args))

    }

    private val userLanguageChanged: Emitter.Listener = Emitter.Listener { args ->
        RxBus.publish(SocketMessage(SocketMessage.Type.USER_LANGUAGE_CHANGED, args))
    }

    private val callLogStatus: Emitter.Listener = Emitter.Listener { args ->
        RxBus.publish(SocketMessage(SocketMessage.Type.CALL_LOG_STATUS, args))
    }

    private val motherLanguageSwitchChanged: Emitter.Listener = Emitter.Listener { args ->
        RxBus.publish(SocketMessage(SocketMessage.Type.MOTHER_LANGUAGE_SWITCH_CHANGED, args))
    }

    fun emit(event: String, vararg args: Any, callBack: (Array<out Any?>) -> Unit) {

        getSocketInstance().emit(event, args, object : Ack {
            override fun call(vararg args: Any?) {
                callBack(args)
            }
        })
    }
}

class SocketMessage(val messageType: Type, val message: Array<out Any?>) {
    enum class Type {
        NEW_MESSAGE, ONLINE_STATUS, TYPING_RESULT, USER_LANGUAGE_CHANGED, MOTHER_LANGUAGE_SWITCH_CHANGED,CALL_LOG_STATUS
    }
}




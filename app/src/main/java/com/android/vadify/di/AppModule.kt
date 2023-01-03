package com.android.vadify.di

import android.app.Application
import android.content.Context
import android.util.Log
import com.android.vadify.BuildConfig
import com.android.vadify.R
import com.android.vadify.VadifyApplication
import com.android.vadify.data.db.DbModule
import com.android.vadify.data.service.PreferenceService
//import com.github.nkzawa.engineio.client.transports.WebSocket
//import com.github.nkzawa.socketio.client.IO
//import com.github.nkzawa.socketio.client.Socket
import dagger.Module
import dagger.Provides
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import java.net.URISyntaxException
import javax.inject.Singleton

@Module(
    includes = [
        UiModule::class,
        DbModule::class,
        ServiceModule::class
    ]
)
class AppModule {

    @Provides
    @Singleton
    fun provideApplication(application: VadifyApplication): Application {
        return application
    }

    @Provides
    @Singleton
    fun provideContext(application: VadifyApplication): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun providePreferenceService(context: Context): PreferenceService {
        return PreferenceService(context)
    }

    @Provides
    @Singleton
    fun getSocketInstance(preferenceService: PreferenceService): Socket? {
        var mSocket: Socket? = null
        try {
            if (mSocket == null) {

                val opts = IO.Options()
                opts.transports = arrayOf(WebSocket.NAME)
                opts.forceNew = true
                opts.reconnection = true

                Log.d( "socket-users: ",""+preferenceService.getString(R.string.pkey_user_Id))
                mSocket = IO.socket(
                    BuildConfig.API_URL + "?userId=" + preferenceService.getString(R.string.pkey_user_Id),
                    opts
                )  //IO.socket("https://socketio-chat-h9jt.herokuapp.com") // IO.socket(BuildConfig.API_URL + "?userId="+ preferenceService.getString(R.string.pkey_user_Id))
            }
        } catch (e: URISyntaxException) {
            Log.e("message are", "" + e.message.toString())
        }
        return mSocket
    }
}


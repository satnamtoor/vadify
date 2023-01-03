package com.android.vadify.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.android.vadify.R
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.ui.login.viewmodel.LoginFragmentViewModel
import com.android.vadify.ui.walktroughdesign.WalkThroughScreen
import com.android.vadify.widgets.getDeviceToken
import com.stfalcon.smsverifycatcher.SmsVerifyCatcher
import dagger.android.AndroidInjection
import javax.inject.Inject

/**
 * Splash Activity for 3 seconds
 * @author Davinder syall
 */
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var prefference: PreferenceService

    //  lateinit var installStateUpdatedListener: InstallStateUpdatedListener
    /*  @Inject
      lateinit var appUpdateManager: AppUpdateManager

      @Inject
      lateinit var playServiceExecutor: Executor
  */

    @Inject
    lateinit var cache: ChatListCache

    //val appUpdateManager = AppUpdateManagerFactory.create(updateActionListener.getActivity())
    companion object {
        const val SPLASH_TIME_OUT = 3000L
        const val REQUEST_UPDATE_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        AndroidInjection.inject(this)
        getDeviceToken {
            if (!it.isNullOrBlank()) prefference.putString(
                R.string.pkey_device_token,
                it
            )
        }
        navigateScreenAfterDelay()

        Log.d( "authontication: ",""+ prefference.getString(R.string.pkey_secure_token))


    }



    private fun navigateScreenAfterDelay() {
        Handler(Looper.getMainLooper()).postDelayed({
            when {
                !prefference.getString(R.string.pkey_motherLanguage)
                    .isNullOrBlank() && !prefference.getString(R.string.pkey_user_Id)
                    .isNullOrBlank() -> {

                    val intent = Intent(this@SplashActivity, Home::class.java)
                    startActivity(intent)
                }
                else -> {
                    val intent = Intent(this@SplashActivity, WalkThroughScreen::class.java)
                    startActivity(intent)
                }
            }
            finish()
        }, SPLASH_TIME_OUT)
    }
}

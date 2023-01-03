package com.android.vadify.ui.dashboard

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.android.vadify.R
import com.android.vadify.data.service.PreferenceService
import com.android.vadify.databinding.ActivityDashboardBinding
import com.android.vadify.ui.Home
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.dashboard.viewmodel.DashBoardViewModel
import com.android.vadify.ui.dashboard.viewmodel.UserListViewModel
import com.android.vadify.ui.util.tryHandleCropImageResult
import com.android.vadify.viewmodels.EncryptionViewModel
import com.android.vadify.widgets.BadgeView
import com.android.vadify.widgets.addBadge
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.android.AndroidInjection
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.resolution
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject


class Dashboard : DataBindingActivity<ActivityDashboardBinding>() {

    lateinit var inboxBadge: BadgeView
    val viewModelUser: UserListViewModel by viewModels()
    val viewModel: DashBoardViewModel by viewModels()

    // val viewModelChat: ChatViewModel by viewModels()

    @Inject
    lateinit var preferenceService: PreferenceService



    private val encryptionViewModel: EncryptionViewModel by viewModels()

    private val viewModelChat: ChatViewModel by viewModels()

    companion object {
        const val IS_FIRST_TIME = "Is_first_time"
        const val IS_CHAT = "Is_chat"
        const val IS_AUDIO = "Is_audio"
        const val IS_GROUP = "Is_group"
        const val IS_VIDEO = "Is_video"
        const val IS_INVITE = "Is_invite"
        const val IS_CAMERA = "Is_camera"
    }

    private lateinit var navController: NavController

    override val layoutRes: Int
        get() = R.layout.activity_dashboard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        initDrawerBottomSheet()
       // userPreferences = UserPreferences(this)
        encryptionViewModel.staticContent()
        //  SocketManager.instance.connect(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        return nav_host_fragment.findNavController()
            .navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (intent.getBooleanExtra(IS_FIRST_TIME, false)) {
            val intent = Intent(this@Dashboard, Home::class.java)
            startActivity(intent)
            finish()
        } else {
            finish()

        }
    }


    private fun initDrawerBottomSheet() {


        setSupportActionBar(dashboardtoolbar)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_chat,
                R.id.navigation_camera,
                R.id.navigation_call,
                R.id.navigation_setting

            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.itemIconTintList = null

        if (intent.getBooleanExtra(IS_FIRST_TIME, false)) {
            navController.navigate(R.id.vadifyFriendFragment)
            navView.menu.findItem(R.id.navigation_chat).isCheckable = false
            navView.menu.findItem(R.id.navigation_chat).setOnMenuItemClickListener {
                if (!navView.menu.findItem(R.id.navigation_chat).isCheckable) {
                    navView.menu.findItem(R.id.navigation_chat).isCheckable = true
                }
                navController.navigate(R.id.navigation_chat)
                true
            }
        }

        if (intent.getBooleanExtra(IS_AUDIO, false)) {
            val bundal:Bundle = Bundle()
            bundal.putString("ACTIVITY_TYPE","audio")
            navController.navigate(R.id.navigation_call,bundal)
        }
        /*if (intent.getBooleanExtra(IS_GROUP, false)) {
            Log.d( "initDrawerBottomSheet: ","calling")
            navController.navigate(R.id.vadifyFriendFragment)
        }*/


        if (intent.getBooleanExtra(IS_VIDEO, false)) {
            val bundal:Bundle = Bundle()
            bundal.putString("ACTIVITY_TYPE","video")
            navController.navigate(R.id.navigation_call,bundal)
        }
        if (intent.getBooleanExtra(IS_INVITE, false)) {
            Log.d( "initDrawerBottomSheet: ","calling")
            navController.navigate(R.id.vadifyFriendFragment)
        }
        if (intent.getBooleanExtra(IS_CAMERA, false)) {
            cameraPicker()
        }




        inboxBadge = nav_view.addBadge(R.id.navigation_chat)
        viewModel.totalMembers.observe(
            this,
            Observer {
                // viewModelUser.getChatThreadsFromAPINew()
                // viewModelUser.updateApi()
                Log.d("batch-count", "" + it)
                if (it > 0)
                    inboxBadge.setNumber(it)
                else
                    inboxBadge.setNumber(null)
            })

        viewModel.localCountUpdate.observe(this, Observer {
            Log.d("local-batch-count", "" + it)
            if (it > 0) {
                inboxBadge.setNumber(it)
            } else
                inboxBadge.setNumber(null)

        })


        nav_view.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home-> {
                    if (intent.getBooleanExtra(IS_FIRST_TIME, false)) {
                        val intent = Intent(this@Dashboard, Home::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        finish()

                    }
                }
                R.id.navigation_camera -> cameraPicker()
                R.id.navigation_chat -> navController.navigate(R.id.navigation_chat)
                R.id.navigation_call -> navController.navigate(R.id.navigation_call)
                R.id.navigation_setting -> navController.navigate(R.id.navigation_setting)
            }
            true
        }
        navView.setOnLongClickListener {
            false
        }
    }

    fun updateNavigationView() = navController.navigate(R.id.vadifyFriendFragment)
    fun backStackNavigationView() = navController.popBackStack()



    override fun onResume() {
        super.onResume()
       // inboxBadge.setNumber(null)
        connectMethod()
        // viewModelUser.updateApi()
       // viewModel.serviceRefersh()
        viewModel.updateBadgeService()
       // viewModel.updateChatActivity(false)
        muteBeepSoundOfRecorder(false)

    }

    override fun onPause() {
        super.onPause()
        disconnectMethod()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectMethod()
    }

    @Parcelize
    data class ContactInformation(
        var name: String,
        var phone: String,
        var imageUri: String? = "",
        var sortLetter: String? = "",
        var selection: Boolean = false
    ) : Parcelable


    private fun cameraPicker() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setCropMenuCropButtonTitle(getString(R.string.Done))
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        tryHandleCropImageResult(requestCode, resultCode, data) {

            lifecycleScope.launch {

                val listener = object : HandlePathOzListener.SingleUri {
                    override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
                        lifecycleScope.launch {
                            val file = File(pathOz.path)
                            try {
                                val compressedImageFile1 =
                                    Compressor.compress(this@Dashboard, file) {
                                        resolution(
                                            800,
                                            800
                                        )
                                        format(Bitmap.CompressFormat.JPEG)
                                    }
                                viewModel.imagePath = compressedImageFile1.absolutePath
                                updateNavigationView()
                            } catch (e: Exception) {
                                print(e.stackTrace)
                            }
                        }
                    }

                }
                HandlePathOz(this@Dashboard, listener).getRealPath(it.uriContent!!)
            }
        }
    }

    /**
     * Function to remove the beep sound of voice recognizer.
     */
    private fun muteBeepSoundOfRecorder(volume: Boolean) {
        try {
            val amanager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            amanager.mode = AudioManager.MODE_NORMAL
            amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, volume)
            amanager.setStreamMute(AudioManager.STREAM_ALARM, volume)
            amanager.setStreamMute(AudioManager.STREAM_MUSIC, volume)
            amanager.setStreamMute(AudioManager.STREAM_RING, volume)
            amanager.setStreamMute(AudioManager.STREAM_SYSTEM, volume)
        } catch (e: Exception) {
        }

    }

    private fun connectMethod() {
        try {
            viewModelChat.getSocketInstance()?.let {
                it.on(ChatActivity.RECEIVE_MESSAGE, onNewMessage)
                it.connect()
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val onNewMessage: Emitter.Listener = Emitter.Listener { args ->

Log.d("message-recv", "yes")
        //  viewModel.receiveNewMessage(args)

        viewModel.serviceRefersh()
        viewModel.updateBadgeService()
    }


    private fun disconnectMethod() {
        viewModelChat.mSocket?.let {
            it.disconnect()
            it.off(ChatActivity.RECEIVE_MESSAGE, onNewMessage)

        }
    }


}

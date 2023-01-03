package com.android.vadify.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.android.vadify.R
import com.android.vadify.databinding.ActivityDashboardBinding
import com.android.vadify.databinding.HomeBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.viewmodel.ChatViewModel
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.fragment.changePassword.ChangePasswordActivity
import com.android.vadify.ui.dashboard.fragment.setting.SettingFragment
import com.android.vadify.ui.dashboard.viewmodel.DashBoardViewModel
import com.android.vadify.ui.dashboard.viewmodel.SettingFragmentViewModel
import com.android.vadify.ui.util.imageUrl
import com.android.vadify.ui.util.tryHandleCropImageResult
import com.android.vadify.widgets.addBadge
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import dagger.android.AndroidInjection
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.resolution
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_dashboard.*
import kotlinx.android.synthetic.main.activity_edit_account.*
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.home.*
import kotlinx.coroutines.launch
import java.io.File

class Home : DataBindingActivity<HomeBinding>(){

    val viewModel: SettingFragmentViewModel by viewModels()
    val viewModelDashBoard: DashBoardViewModel by viewModels()
    override val layoutRes: Int
        get() = R.layout.home
    private val viewModelChat: ChatViewModel by viewModels()


    companion object {
        private const val PROFILE_IMAGE_SIZE = 300
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)


        subscribe(help.throttleClicks()) {
            startActivity(Intent(this, HelpSupport::class.java))
        }
        subscribe(chat.throttleClicks()) {
            val intent = Intent(this, Dashboard::class.java)
            intent.putExtra(Dashboard.IS_CHAT, true)
            startActivity(intent)
        }

        subscribe(audioCall.throttleClicks()) {
            val intent = Intent(this, Dashboard::class.java)
            intent.putExtra(Dashboard.IS_AUDIO, true)
            startActivity(intent)
        }


        subscribe(videoCall.throttleClicks()) {
            val intent = Intent(this, Dashboard::class.java)
            intent.putExtra(Dashboard.IS_VIDEO, true)
            startActivity(intent)
        }

        subscribe(inviteFriend.throttleClicks()) {
            val intent = Intent(this, Dashboard::class.java)
            intent.putExtra(Dashboard.IS_INVITE, true)
            startActivity(intent)
        }

        subscribe(camera.throttleClicks()) {
            val intent = Intent(this, Dashboard::class.java)
            intent.putExtra(Dashboard.IS_CAMERA, true)
            startActivity(intent)
        }

        subscribe(btnProfilePic.throttleClicks()) {
            navigateCropImage()
        }

        subscribe(translation.throttleClicks()) {
            val intent = Intent(this, Translation::class.java)
            startActivity(intent)
        }

        viewModel.profileImage.observe(this, Observer {

            profilePic.imageUrl(it,R.drawable.user_placeholder)
        })


       // inboxBadge = nav_view.addBadge(R.id.navigation_chat)
        viewModelDashBoard.totalMembers.observe(
            this,
            Observer {
                // viewModelUser.getChatThreadsFromAPINew()
                // viewModelUser.updateApi()
                Log.d("batch-count", "" + it)
                if (it > 0)
                // inboxBadge.setNumber(it)
                {
                    chatBadge.visibility = View.VISIBLE
                    chatBadge.setText(it.toString())
                }
                    else {
                    chatBadge.visibility = View.GONE

                }
                })

        viewModelDashBoard.localCountUpdate.observe(this, Observer {
            Log.d("local-batch-count", "" + it)
            if (it > 0) {

                    chatBadge.visibility = View.VISIBLE
                    chatBadge.setText(it.toString())
                }
                else {
                    chatBadge.visibility = android.view.View.GONE

                }

        })


    }



    private fun navigateCropImage() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setCropMenuCropButtonTitle(getString(R.string.Done))
            .setCropShape(CropImageView.CropShape.OVAL)
            .setAspectRatio(1, 1)
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
                                val compressedImageFile =
                                    Compressor.compress(this@Home, file)
                                    {
                                        resolution(PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE)
                                        format(Bitmap.CompressFormat.JPEG)
                                    }
                                bindNetworkState(
                                    viewModel.compressedImageFile(compressedImageFile),
                                    loadingIndicator = progressBar
                                ) {
                                   // bindNetworkState(viewModel.updateNotificationData())
                                }
                            } catch (e: Exception) {
                                print(e.stackTrace)
                            }
                        }
                    }

                }
                HandlePathOz(this@Home, listener).getRealPath(it.uriContent!!)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // inboxBadge.setNumber(null)
        connectMethod()
        // viewModelUser.updateApi()
        // viewModel.serviceRefersh()
        viewModelDashBoard.updateBadgeService()
        // viewModel.updateChatActivity(false)


    }
    override fun onPause() {
        super.onPause()
        disconnectMethod()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectMethod()
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

        viewModelDashBoard.serviceRefersh()
        viewModelDashBoard.updateBadgeService()
    }


    private fun disconnectMethod() {
        viewModelChat.mSocket?.let {
            it.disconnect()
            it.off(ChatActivity.RECEIVE_MESSAGE, onNewMessage)

        }
    }
}


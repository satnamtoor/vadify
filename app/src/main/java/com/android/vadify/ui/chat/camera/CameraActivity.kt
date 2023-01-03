package com.android.vadify.ui.chat.camera

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import androidx.lifecycle.lifecycleScope
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.afollestad.assent.Permission
import com.afollestad.assent.runWithPermissions
import com.android.vadify.R
import com.android.vadify.databinding.ActivityCameraBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.videoplayer.VideoPlayerActivity
import com.android.vadify.ui.chat.viewmodel.CameraViewModel
import com.android.vadify.ui.util.tryHandleCropImageResult
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.resolution
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.coroutines.launch
import java.io.File


class CameraActivity : DataBindingActivity<ActivityCameraBinding>() {

    val viewModel: CameraViewModel by viewModels()

    companion object {
        const val URL = "URL"
        const val MESSAGE = "Message"
        const val IS_VIDEO_REORDING = "IS_VIDEO_RECORDING"
        const val CAMERA = 0
        const val GALLERY = 1
        const val NONE = 2
    }

    override val layoutRes: Int
        get() = R.layout.activity_camera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getBooleanExtra(IS_VIDEO_REORDING, false)
            .let { viewModel.isVideoRecording.value = it }

        /*runWithPermissions(
            Permission.WRITE_EXTERNAL_STORAGE,
            Permission.READ_EXTERNAL_STORAGE,
            Permission.CAMERA
        ) {*/
            when (viewModel.isVideoRecording.value) {
                true -> videoPicker()
                else -> navigateCropImage()
            }
        //}

        subscribe(player_icon.throttleClicks()) {
            startActivity(Intent(this, VideoPlayerActivity::class.java).also {
                it.putExtra("uri", viewModel.imageUrl.value)
            })
        }

        subscribe(videoBtn.throttleClicks()) {
            videoPicker()
        }

        subscribe(cameraBtn.throttleClicks()) {
            navigateCropImage()
        }

        subscribe(camerView.throttleClicks()) {
            navigateCropImage()
        }

        subscribe(closeBtn.throttleClicks()) {
            finish()
        }

        subscribe(send.throttleClicks()) {
            setResult(Activity.RESULT_OK, viewModel.getIntent())
            finish()
        }
    }

    private fun navigateCropImage() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setCropMenuCropButtonTitle(getString(R.string.Done))
            .start(this)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ChatActivity.VIDEO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                viewModel.imageUrl.value = getPath(uri)
                viewModel.isVideoAvailable.value = true
            }
        } else {
            if (resultCode == 0) {
                finish()
            } else {
                tryHandleCropImageResult(requestCode, resultCode, data) {

                    lifecycleScope.launch {

                        val listener = object : HandlePathOzListener.SingleUri {
                            override fun onRequestHandlePathOz(pathOz: PathOz, tr: Throwable?) {
                                lifecycleScope.launch {
                                    val file = File(pathOz.path)
                                    try {
                                        val compressedImageFile1 =
                                            Compressor.compress(this@CameraActivity, file) {
                                                resolution(
                                                    ChatActivity.PROFILE_IMAGE_SIZE,
                                                    ChatActivity.PROFILE_IMAGE_SIZE
                                                )
                                                format(Bitmap.CompressFormat.JPEG)
                                            }
                                        viewModel.imageUrl.value = compressedImageFile1.absolutePath
                                    } catch (e: Exception) {
                                        print(e.stackTrace)
                                    }
                                }
                            }

                        }
                        HandlePathOz(this@CameraActivity, listener).getRealPath(it.uriContent!!)
                    }
                }

            }

        }
    }

    override fun onBindView(binding: ActivityCameraBinding) {
        binding.vm = viewModel
    }


    private fun videoPicker() {
        val options = arrayOf<CharSequence>(
            getString(R.string.Camera),
            getString(R.string.Gallery),
            getString(R.string.Cancel)
        )
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.choice)
        builder.setItems(options) { dialog, item ->
            when (item) {
                CAMERA -> {
                    val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60)
                    startActivityForResult(intent, ChatActivity.VIDEO_REQUEST_CODE)
                }
                GALLERY -> {
                    val intent =
                        Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                       intent.type = "video/*"
                    startActivityForResult(intent, ChatActivity.VIDEO_REQUEST_CODE)
                }
                NONE -> {

                    dialog.dismiss()
                    finish()
                }
            }
        }
        builder.show()
    }
}
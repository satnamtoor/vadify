package com.android.vadify.ui.chat

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.android.vadify.R
import com.android.vadify.data.api.enums.MessageType
import com.android.vadify.data.api.models.ContactSyncingResponse
import com.android.vadify.data.repository.FileUploadRepositry
import com.android.vadify.databinding.CreateGroupBinding
import com.android.vadify.ui.baseclass.BaseBackStack
import com.android.vadify.ui.baseclass.BaseDaggerFragment
import com.android.vadify.ui.baseclass.BaseDaggerListFragment
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.adapter.CreaateGroupAdapter
import com.android.vadify.ui.chat.call.viewmodel.CallViewModel
import com.android.vadify.ui.chat.viewmodel.CreateGroupModel
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.adapter.VadifyFriendAdapter
import com.android.vadify.ui.dashboard.fragment.call.activity.DirectCallActivity
import com.android.vadify.ui.dashboard.fragment.camera.CameraFragment
import com.android.vadify.ui.util.imageUrl
import com.android.vadify.ui.util.tryHandleCropImageResult
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import com.sdi.joyersmajorplatform.common.progressDialog
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.resolution
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.create_group.*
import kotlinx.android.synthetic.main.create_group.vadifyFriendList
import kotlinx.android.synthetic.main.fragment_vadify_friend.*
import kotlinx.coroutines.launch
import java.io.File

class CreateGroupFragment : DataBindingActivity<CreateGroupBinding>() {

    override val layoutRes: Int
        get() = R.layout.create_group
    var adapter: CreaateGroupAdapter? = null

    val viewModel: CreateGroupModel by viewModels()
    var imagePath = ""
    var roomId : String? = null
    var updateGroupName : String? = null
    var groupImage : String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var list: MutableLiveData<List<ContactSyncingResponse.UserGroup>?> = MutableLiveData()
        list.value = DirectCallActivity.memberList
        if (intent.extras?.getString(BaseBackStack.ROOM_ID)!=null) {
            roomId = intent.extras?.getString(BaseBackStack.ROOM_ID)
            updateGroupName = intent.extras?.getString(BaseBackStack.GROUP_NAME)
            groupImage = intent.extras?.getString(BaseBackStack.GROUP_IMAGE)
            binding.groupName.setText(updateGroupName)
            binding.cameraCapture.imageUrl(groupImage,R.drawable.ic_group_icon)
        }
        adapter = initAdapter(
            CreaateGroupAdapter(this),
            vadifyFriendList,
            list
        )

        subscribe(cameraCapture.throttleClicks()) {
            cameraPicker()
        }


        subscribe(cancelBtn.throttleClicks())
        {
            finish()
        }

        subscribe(addGroup.throttleClicks())
        {
            when {
                groupName.text.isNullOrBlank() -> {
                    Toast.makeText(this, "Please enter group name", Toast.LENGTH_LONG)
                        .show()
                }
                DirectCallActivity.memberList.isEmpty() -> {
                    Toast.makeText(this, "Please select any member", Toast.LENGTH_LONG)
                        .show()
                }
                else -> {

                    val groupId = DirectCallActivity.memberList.map {
                        it._id
                    }
                    if(imagePath != "")

                    bindNetworkState(
                        viewModel.compressedImageFile(File(imagePath),groupName.text.toString(), groupId)
                    )
                    {
                        val intent = Intent(this, Dashboard::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                    }else {
                        if (roomId == "null"){
                            bindNetworkState(viewModel.createGroupCallApi(groupName.text.toString(), imagePath, groupId))
                            {
                                val intent = Intent(this, Dashboard::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                            }
                        }
                        else {
                            bindNetworkState(viewModel.updateGroupCallApi(roomId!!, groupId))
                            {
                                val intent = Intent(this, Dashboard::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(intent)
                            }
                        }
                    }
                }
            }


        }


    }

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
                                    Compressor.compress(this@CreateGroupFragment, file) {
                                        resolution(
                                            800,
                                            800
                                        )
                                        format(Bitmap.CompressFormat.JPEG)
                                    }
                                imagePath = compressedImageFile1.absolutePath
                                cameraCapture.setImageURI(Uri.parse(imagePath))

                            } catch (e: Exception) {
                                print(e.stackTrace)
                            }
                        }
                    }

                }
                HandlePathOz(this@CreateGroupFragment, listener).getRealPath(it.uriContent!!)
            }
        }
    }
}
package com.android.vadify.ui.dashboard.fragment.setting

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import br.com.onimur.handlepathoz.HandlePathOz
import br.com.onimur.handlepathoz.HandlePathOzListener
import br.com.onimur.handlepathoz.model.PathOz
import com.android.vadify.R
import com.android.vadify.databinding.FragmentSettingBinding
import com.android.vadify.ui.baseclass.BaseDaggerListFragment
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.activity.*
import com.android.vadify.ui.dashboard.adapter.SettingAdapter
import com.android.vadify.ui.dashboard.viewmodel.SettingFragmentViewModel
import com.android.vadify.ui.util.imageUrl
import com.android.vadify.ui.util.tryHandleCropImageResult
import com.canhub.cropper.CropImage
import com.canhub.cropper.CropImageView
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.resolution
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.home.*
import kotlinx.coroutines.launch
import java.io.File

class SettingFragment : BaseDaggerListFragment<FragmentSettingBinding>() {

    val viewModel: SettingFragmentViewModel by viewModels()

    lateinit var language: String
    var languageCode: String? = null

    companion object {
        const val BLOCKED_CONTACTS = "Blocked Contacts"
        const val NOTIFICATIONS = "Notifications"
        const val CHATS = "Chats"
        const val ABOUT_US = "About Us"
        const val TERM_POLICY = "Terms and Conditions"
        const val POLICY = "Privacy Policy"
        const val FAQ = "FAQ"
        const val INVITE_FRIENDS = "Invite Friends"
        const val RETAKE_COMMAND = "Record Voice Commands"
        private const val PROFILE_IMAGE_SIZE = 300
    }


    override val layoutRes: Int
        get() = R.layout.fragment_setting

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val settings = (resources.getStringArray(R.array.settings))

        language = viewModel.motherLanguage.value!!
        languageCode = viewModel.getlanguageCode()
        viewModel.updateSettingList(settings.toList())
        /*if (settings.get(0).contains("Retake", ignoreCase = true)) {
            //settings.set(0, settings.get(0) + " " + language)



            Log.d("language-code:", "" + languageCode)

        }*/

        subscribe(editImageIcon.throttleClicks()) {
            navigateCropImage()
        }

        viewModel.profileImage.observe(requireActivity(), Observer {

            profileImageSetting.imageUrl(it,R.drawable.user_placeholder)
        })
        subscribe(editProfile.throttleClicks()) {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }


        subscribe(profileImageSetting.throttleClicks()) {
            //.  Log.d("profile-image: ",viewModel.profileImage.value)
            val intent = Intent(requireActivity(), ProfileImage::class.java)
            intent.putExtra("SCREEN_TYPE", "Settings")
            startActivity(intent)
        }


        subscribe(appVersion.throttleClicks()) {
            val toast: Toast =
                Toast.makeText(activity, getString(R.string.version_testing), Toast.LENGTH_LONG)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        subscribe(editAccount.throttleClicks()) {
            startActivity(Intent(requireContext(), EditMyAccountActivity::class.java))
        }

        initAdapter(SettingAdapter(), rvSettings, viewModel.settingList) {

            when (it) {

                "$RETAKE_COMMAND" -> {
                    val intent = Intent(
                        requireContext(),
                        RetakeCommand::class.java
                    )
                    intent.putExtra("LANGUAGE", language)
                    intent.putExtra("LANG_CODE", languageCode)
                    intent.putExtra("RETAKE", "YES")
                    startActivity(intent)

                }
                BLOCKED_CONTACTS -> startActivity(
                    Intent(
                        requireContext(),
                        BlockedActivity::class.java
                    )
                )
                NOTIFICATIONS -> startActivity(
                    Intent(
                        requireContext(),
                        NotificationActivity::class.java
                    )
                )
                CHATS -> startActivity(Intent(requireContext(), EditChatSetting::class.java))
                ABOUT_US -> startActivity(Intent(requireContext(), AboutUsActivity::class.java))
                TERM_POLICY -> startActivity(Intent(requireContext(), TermActivity::class.java))
                FAQ -> startActivity(Intent(requireContext(), FaqActivity::class.java))
                INVITE_FRIENDS -> (requireActivity() as Dashboard).updateNavigationView()
                POLICY -> startActivity(Intent(requireContext(), PolicyActivity::class.java))
            }
        }
    }

    override fun onBindView(binding: FragmentSettingBinding) {
        binding.vm = viewModel
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateProfileInformation()
        if (viewModel.isNotificationUpdate()) {
            bindNetworkState(viewModel.updateNotificationData())
        }
    }


    private fun navigateCropImage() {
        CropImage.activity()
            .setGuidelines(CropImageView.Guidelines.ON)
            .setCropMenuCropButtonTitle(getString(R.string.Done))
            .setCropShape(CropImageView.CropShape.OVAL)
            .setAspectRatio(1, 1)
            .start(requireContext(), this)
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
                                    Compressor.compress(requireContext(), file)
                                    {
                                        resolution(PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE)
                                        format(Bitmap.CompressFormat.JPEG)
                                    }
                                bindNetworkState(
                                    viewModel.compressedImageFile(compressedImageFile),
                                    loadingIndicator = progressBar
                                ) {
                                    bindNetworkState(viewModel.updateNotificationData())
                                }
                            } catch (e: Exception) {
                                print(e.stackTrace)
                            }
                        }
                    }

                }
                HandlePathOz(requireContext(), listener).getRealPath(it.uriContent!!)
            }
        }
    }

}
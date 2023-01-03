package com.android.vadify.ui.dashboard.activity

import android.content.Intent
import android.os.Bundle
import com.android.vadify.R
import com.android.vadify.databinding.ActivityAboutUsBinding
import com.android.vadify.ui.baseclass.DataBindingActivity
import com.android.vadify.ui.chat.contact.viewmodel.UserContactViewModel
import com.android.vadify.ui.dashboard.viewmodel.SettingFragmentViewModel
import com.bumptech.glide.Glide
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.image_zoom.*

class ProfileImage : DataBindingActivity<ActivityAboutUsBinding>() {
    val viewModelSetting: SettingFragmentViewModel by viewModels()
    val viewModelAnother: UserContactViewModel by viewModels()
    override val layoutRes: Int
        get() = R.layout.image_zoom

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // if (bundle != null) {
            val intent : Intent = this.intent
            intent.getStringExtra("SCREEN_TYPE")?.let {
             //   mArticleImg = it
                if (it == "Settings") {
                    Glide.with(this).load(viewModelSetting.profileImage.value)
                        .placeholder(android.R.drawable.ic_menu_gallery).into(photo_view)

                } else {

                    Glide.with(this).load(intent.getStringExtra("IMAGE"))
                        .placeholder(android.R.drawable.ic_menu_gallery).into(photo_view)

                }
            }
       // }



        subscribe(txtClose.throttleClicks()) {
            finish()
        }


    }


}
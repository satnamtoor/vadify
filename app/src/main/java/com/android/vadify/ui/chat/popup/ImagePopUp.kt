package com.android.vadify.ui.chat.popup

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.android.vadify.R
import com.android.vadify.databinding.ImagePopBinding
import com.android.vadify.ui.baseclass.BaseDialogDaggerListFragment
import com.sdi.joyersmajorplatform.common.livedataext.mutableLiveData
import com.sdi.joyersmajorplatform.common.livedataext.throttleClicks
import kotlinx.android.synthetic.main.popup_reply_layout.*


class ImagePopUp(var url: String, var isCondition: () -> Unit) :
    BaseDialogDaggerListFragment<ImagePopBinding>() {

    override val layoutRes: Int
        get() = R.layout.image_pop

    var imageLoad = mutableLiveData("")


    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val layoutParams = dialog?.window?.attributes
        layoutParams?.x = 100; // right margin
        layoutParams?.y = 170; // top margin
        dialog?.window?.attributes = layoutParams;
        dialog?.setCancelable(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        imageLoad.value = url
        subscribe(closeBtn.throttleClicks()) {
            isCondition.invoke()
            dismiss()
        }
    }

    override fun onBindView(binding: ImagePopBinding) {
        binding.url = imageLoad
    }


}

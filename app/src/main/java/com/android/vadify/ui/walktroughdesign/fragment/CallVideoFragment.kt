package com.android.vadify.ui.walktroughdesign.fragment

import android.os.Bundle
import android.view.View
import com.android.vadify.R
import com.android.vadify.databinding.FragmentCallVideoBinding
import com.android.vadify.ui.baseclass.BaseDaggerFragment


class CallVideoFragment : BaseDaggerFragment<FragmentCallVideoBinding>() {
    override val layoutRes: Int
        get() = R.layout.fragment_call_video

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}

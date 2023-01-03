package com.android.vadify.ui.walktroughdesign.fragment

import android.os.Bundle
import android.view.View
import com.android.vadify.R
import com.android.vadify.databinding.FragmentVideoCallRoomBinding
import com.android.vadify.ui.baseclass.BaseDaggerFragment

class VideoRoomFragment : BaseDaggerFragment<FragmentVideoCallRoomBinding>() {
    override val layoutRes: Int
        get() = R.layout.fragment_video_call_room

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}

package com.android.vadify.ui.walktroughdesign.fragment

import android.os.Bundle
import android.view.View
import com.android.vadify.R
import com.android.vadify.databinding.FragmentTalkToChatBinding
import com.android.vadify.ui.baseclass.BaseDaggerFragment


class TalkToChatFragment : BaseDaggerFragment<FragmentTalkToChatBinding>() {
    override val layoutRes: Int
        get() = R.layout.fragment_talk_to_chat

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

}

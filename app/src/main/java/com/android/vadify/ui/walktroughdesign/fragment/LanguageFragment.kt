package com.android.vadify.ui.walktroughdesign.fragment

import android.os.Bundle
import android.view.View
import com.android.vadify.R
import com.android.vadify.databinding.FragmentLanguageBinding
import com.android.vadify.ui.baseclass.BaseDaggerFragment


class LanguageFragment : BaseDaggerFragment<FragmentLanguageBinding>() {
    override val layoutRes: Int
        get() = R.layout.fragment_language


//    1 1 1 1 1 0 0 0
//    0 0 1 1 1 0 1 1
//    0 1 1 1 1 1 1 1
//    0 1 0 1 0 1 1 1
//    0 1 1 1 1 1 0 0
//    0 1 0 1 0 1 0 1
//    1 1 1 1 0 0 0 0
//    0 0 0 0 1 1 1 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }


}

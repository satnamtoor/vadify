package com.android.vadify.ui.walktroughdesign.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.android.vadify.ui.walktroughdesign.fragment.CallVideoFragment
import com.android.vadify.ui.walktroughdesign.fragment.LanguageFragment
import com.android.vadify.ui.walktroughdesign.fragment.TalkToChatFragment
import com.android.vadify.ui.walktroughdesign.fragment.VideoRoomFragment

class ScreenOverViewAdapter(fm: FragmentManager, private var totalTabs: Int) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    // private var currentFragment: UsedReceiptFragment? = null

    override fun getItem(position: Int): Fragment {
        return when (position) {
            TALK_TO_CHAT -> TalkToChatFragment()
            LET_START -> CallVideoFragment()
            LANGUAGE -> LanguageFragment()
            VIDEO_ROOM -> VideoRoomFragment()
            else -> TODO("Invalid tab requested")
        }
    }

    // this counts total number of tabs
    override fun getCount(): Int {
        return totalTabs
    }

//    fun getCurrentFragment(): Fragment? {
//        return currentFragment
//    }

//    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
//        super.setPrimaryItem(container, position, `object`)
//        if (getCurrentFragment() !== `object` && `object` is UsedReceiptFragment) {
//            currentFragment = `object`
//        }
//    }

    companion object {
        const val TALK_TO_CHAT = 0
        const val LET_START = 1
        const val LANGUAGE = 2
        const val VIDEO_ROOM = 3
    }

}

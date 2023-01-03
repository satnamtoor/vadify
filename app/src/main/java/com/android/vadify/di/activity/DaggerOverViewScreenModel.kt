package com.android.vadify.di.activity

import com.android.vadify.ui.walktroughdesign.fragment.CallVideoFragment
import com.android.vadify.ui.walktroughdesign.fragment.LanguageFragment
import com.android.vadify.ui.walktroughdesign.fragment.TalkToChatFragment
import com.android.vadify.ui.walktroughdesign.fragment.VideoRoomFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DaggerOverViewScreenModel {

    @ContributesAndroidInjector
    abstract fun contributeCallVideoFragment(): CallVideoFragment

    @ContributesAndroidInjector
    abstract fun contributeLanguageFragment(): LanguageFragment

    @ContributesAndroidInjector
    abstract fun contributeTalkToChatFragment(): TalkToChatFragment

    @ContributesAndroidInjector
    abstract fun contributeVideoRoomFragment(): VideoRoomFragment

}



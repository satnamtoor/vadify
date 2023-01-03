package com.android.vadify.di.activity

import com.android.vadify.ui.chat.CreateGroupFragment
import com.android.vadify.ui.dashboard.fragment.call.CallFragment
import com.android.vadify.ui.dashboard.fragment.camera.CameraFragment
import com.android.vadify.ui.dashboard.fragment.chat.ChatFragment
import com.android.vadify.ui.dashboard.fragment.chat.popup.ChatActionPopUp
import com.android.vadify.ui.dashboard.fragment.setting.SettingFragment
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.VadifyFriendFragment
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.popup.InviteFriendPopUp
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.popup.MessagePopUp
import com.android.vadify.ui.login.fragment.CommandsFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DaggerDashboardModel {

    @ContributesAndroidInjector
    abstract fun contributeChatFragmentt(): ChatFragment

    @ContributesAndroidInjector
    abstract fun contributeCallFragment(): CallFragment

    @ContributesAndroidInjector
    abstract fun commandFragment(): CommandsFragment


    @ContributesAndroidInjector
    abstract fun contributeCameraFragment(): CameraFragment

    @ContributesAndroidInjector
    abstract fun contributeSettingFragment(): SettingFragment

    @ContributesAndroidInjector
    abstract fun contributeVadifyFriendFragmentt(): VadifyFriendFragment

    @ContributesAndroidInjector
    abstract fun contributeInviteFriendPopUp(): InviteFriendPopUp

    @ContributesAndroidInjector
    abstract fun contributeMessagePopUp(): MessagePopUp

    @ContributesAndroidInjector
    abstract fun contributeChatActionPopUp(): ChatActionPopUp


}



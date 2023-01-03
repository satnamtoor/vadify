package com.android.vadify.di.activity

import com.android.vadify.ui.chat.CreateGroupFragment
import com.android.vadify.ui.chat.popup.CommandDrivenPopUp
import com.android.vadify.ui.chat.popup.ImagePopUp
import com.android.vadify.ui.chat.popup.ReplyMessagePopUp
import com.android.vadify.ui.chat.popup.ShareMessagePopUp
import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.ui.dashboard.fragment.chat.ChatFragment
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.VadifyFriendFragment
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.popup.InviteFriendPopUp
import com.android.vadify.ui.dashboard.fragment.vadifyfriend.popup.MessagePopUp
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DaggerReplyPopUpModel {

    @ContributesAndroidInjector
    abstract fun contributeReplyMessagePopUp(): ReplyMessagePopUp

    @ContributesAndroidInjector
    abstract fun contributeCommandDrivenPopUp(): CommandDrivenPopUp

    @ContributesAndroidInjector
    abstract fun contributeImagePopUp(): ImagePopUp

    @ContributesAndroidInjector
    abstract fun contributeShareMessagePopUp(): ShareMessagePopUp

    @ContributesAndroidInjector
    abstract fun contributeInviteFriendPopUp(): InviteFriendPopUp

    @ContributesAndroidInjector
    abstract fun contributeMessagePopUp(): MessagePopUp

    @ContributesAndroidInjector
    abstract fun commandfragmentInChat(): CommandsFragment

    @ContributesAndroidInjector
    abstract fun contributeChatFragment(): ChatFragment

    @ContributesAndroidInjector
    abstract fun contributeVadifyFriendFragment(): VadifyFriendFragment
}



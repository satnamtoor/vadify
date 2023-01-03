package com.android.vadify.di

import androidx.lifecycle.ViewModelProvider
import com.android.vadify.di.activity.*
import com.android.vadify.service.CallNotificationService
import com.android.vadify.service.VideoCallNotificationService
import com.android.vadify.ui.*
import com.android.vadify.ui.chat.ChatActivity
import com.android.vadify.ui.chat.CreateGroupFragment
import com.android.vadify.ui.chat.call.CallActivity
import com.android.vadify.ui.chat.call.videocall.VideoCallActivity
import com.android.vadify.ui.chat.camera.CameraActivity
import com.android.vadify.ui.chat.contact.UserContactInformation
import com.android.vadify.ui.contact.ContactActivity
import com.android.vadify.ui.dashboard.Dashboard
import com.android.vadify.ui.dashboard.activity.*
import com.android.vadify.ui.dashboard.fragment.call.activity.DirectCallActivity
import com.android.vadify.ui.dashboard.fragment.changePassword.ChangePasswordActivity
import com.android.vadify.ui.login.StartUpActivity
import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.ui.walktroughdesign.WalkThroughScreen
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [ViewModelModule::class])
abstract class UiModule {

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @ContributesAndroidInjector(modules = [DaggerStartUpModel::class])
    abstract fun contributeSignUpActivity(): StartUpActivity

    @ContributesAndroidInjector(modules = [DaggerDashboardModel::class])
    abstract fun contributeDashboard(): Dashboard

    @ContributesAndroidInjector(modules = [DaggerOverViewScreenModel::class])
    abstract fun contributeWalkThroughScreen(): WalkThroughScreen

    @ContributesAndroidInjector
    abstract fun contributeBlockedActivity(): BlockedActivity

    @ContributesAndroidInjector(modules = [DaggerEditProfileModel::class])
    abstract fun retakecommand(): RetakeCommand


    @ContributesAndroidInjector
    abstract fun contributeNotificationActivity(): NotificationActivity

    @ContributesAndroidInjector(modules = [DaggerEditMyAccountModel::class])
    abstract fun contributeEditMyAccountActivity(): EditMyAccountActivity

    @ContributesAndroidInjector
    abstract fun contributeEditChatSetting(): EditChatSetting

    @ContributesAndroidInjector(modules = [DaggerEditProfileModel::class])
    abstract fun contributeEditProfileActivity(): EditProfileActivity

    @ContributesAndroidInjector
    abstract fun contributeProfileImageActivity(): ProfileImage


    @ContributesAndroidInjector
    abstract fun contributeAboutUsActivity(): AboutUsActivity

    @ContributesAndroidInjector
    abstract fun contributeTermActivity(): TermActivity

    @ContributesAndroidInjector
    abstract fun contributePolicyActivity(): PolicyActivity

    @ContributesAndroidInjector
    abstract fun contributeContactActivity(): ContactActivity

    @ContributesAndroidInjector
    abstract fun contributeCameraActivity(): CameraActivity

    @ContributesAndroidInjector(modules = [DaggerReplyPopUpModel::class])
    abstract fun contributeChatActivity(): ChatActivity

    @ContributesAndroidInjector
    abstract fun contributeSplashActivity(): SplashActivity

    @ContributesAndroidInjector
    abstract fun contributeHomeActivity(): Home

    @ContributesAndroidInjector
    abstract fun contributeHelpActivity(): HelpSupport

    @ContributesAndroidInjector
    abstract fun contributeTranslationActivity(): Translation

    @ContributesAndroidInjector(modules = [DaggerChangePasswordModel::class])
    abstract fun contributeChangePasswordActivity(): ChangePasswordActivity

    @ContributesAndroidInjector(modules = [DaggerUserContactModel::class])
    abstract fun contributeUserContactInformation(): UserContactInformation

    @ContributesAndroidInjector
    abstract fun contributeCallActivity(): CallActivity

    @ContributesAndroidInjector
    abstract fun contributeVideoCallActivity(): VideoCallActivity

    @ContributesAndroidInjector
    abstract fun contributeDirectCallActivity(): DirectCallActivity

    @ContributesAndroidInjector(modules = [DaggerReplyPopUpModel::class])
    abstract fun inviteGroupActivity(): InviteGroup

    @ContributesAndroidInjector
    abstract fun createGroup(): CreateGroupFragment

    @ContributesAndroidInjector
    abstract fun contributeCallNotificationService(): CallNotificationService

    @ContributesAndroidInjector
    abstract fun contributeVideoCallNotificationService(): VideoCallNotificationService



}

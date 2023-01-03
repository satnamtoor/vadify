package com.android.vadify.di.activity

import com.android.vadify.ui.chat.CreateGroupFragment
import com.android.vadify.ui.dashboard.fragment.camera.CameraFragment
import com.android.vadify.ui.dashboard.fragment.changePassword.ChangePasswordFragmentStep1
import com.android.vadify.ui.login.fragment.VerificationFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector


@Module
abstract class DaggerGroupModel {

    @ContributesAndroidInjector
    abstract fun mCreateGroup(): CreateGroupFragment


}
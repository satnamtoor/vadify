package com.android.vadify.di.activity

import com.android.vadify.ui.dashboard.fragment.changePassword.ChangePasswordFragmentStep1
import com.android.vadify.ui.login.fragment.VerificationFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DaggerChangePasswordModel {

    @ContributesAndroidInjector
    abstract fun contributeChangePasswordFragmentStep1(): ChangePasswordFragmentStep1

    @ContributesAndroidInjector
    abstract fun contributeVerificationFragment(): VerificationFragment


}



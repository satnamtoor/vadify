package com.android.vadify.di.activity

import com.android.vadify.widgets.SignOutBottomSheet
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DaggerEditMyAccountModel {

    @ContributesAndroidInjector
    abstract fun contributeSignOutBottomSheet(): SignOutBottomSheet


}



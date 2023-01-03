package com.android.vadify.di.activity

import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.ui.login.fragment.popup.LocalLanguagePopUp
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DaggerEditProfileModel {

    @ContributesAndroidInjector
    abstract fun contributeLocalLanguagePopUp(): LocalLanguagePopUp

    @ContributesAndroidInjector
    abstract fun commandsFragments(): CommandsFragment



}



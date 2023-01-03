package com.android.vadify.di.activity

import com.android.vadify.ui.login.fragment.CommandsFragment
import com.android.vadify.ui.login.fragment.LoginFragment
import com.android.vadify.ui.login.fragment.PersonalInformationFragment
import com.android.vadify.ui.login.fragment.VerificationFragment
import com.android.vadify.ui.login.fragment.popup.ContactPopUp
import com.android.vadify.ui.login.fragment.popup.LocalLanguagePopUp
import com.android.vadify.ui.login.fragment.popup.NotificationPopUp
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DaggerStartUpModel {

    @ContributesAndroidInjector
    abstract fun contributeVerificationFragment(): VerificationFragment

    @ContributesAndroidInjector
    abstract fun contributeLoginFragment(): LoginFragment

    @ContributesAndroidInjector
    abstract fun contributePersonalInformationFragment(): PersonalInformationFragment

    @ContributesAndroidInjector
    abstract fun contributeCommandsFragment(): CommandsFragment


    @ContributesAndroidInjector
    abstract fun contributeContactPopUp(): ContactPopUp

    @ContributesAndroidInjector
    abstract fun contributeNotificationPopUp(): NotificationPopUp

    @ContributesAndroidInjector
    abstract fun contributeLocalLanguagePopUp(): LocalLanguagePopUp


}



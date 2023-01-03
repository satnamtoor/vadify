package com.android.vadify.di.activity

import com.android.vadify.ui.chat.contact.fragment.AnotherUserContact
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class DaggerUserContactModel {

    @ContributesAndroidInjector
    abstract fun contributeAnotherUserContact(): AnotherUserContact


}



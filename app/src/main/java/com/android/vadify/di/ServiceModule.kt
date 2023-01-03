package com.android.vadify.di

import com.android.vadify.service.VadifyMessagingService
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Dagger2 configuration for UI modules
 * All Activities should be registered here
 * Fragments should be placed in separate modules and included as submodules for the activity
 */
@Module
abstract class ServiceModule {
    @ContributesAndroidInjector
    abstract fun contributeVadifyMessagingService(): VadifyMessagingService
}

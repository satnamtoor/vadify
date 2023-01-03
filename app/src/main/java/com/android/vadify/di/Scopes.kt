package com.android.vadify.di

import javax.inject.Qualifier

/**
 * Dagger2 scopes
 */

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class FragmentScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class GroupScope

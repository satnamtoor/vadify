package com.android.vadify.di

import com.android.vadify.VadifyApplication
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class,
        ApiModule::class
    ]
)

interface AppComponent : AndroidInjector<VadifyApplication> {

    fun daggerWorkerFactory(): DaggerWorkerFactory

    // Establish WorkerSubcomponent as subcomponent
    fun workerSubcomponentBuilder(): WorkerSubcomponent.Builder

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: VadifyApplication): Builder
        fun build(): AppComponent
    }

    override fun inject(app: VadifyApplication)
}



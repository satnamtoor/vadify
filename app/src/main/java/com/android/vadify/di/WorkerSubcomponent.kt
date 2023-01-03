package com.android.vadify.di

import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.vadify.ui.chat.viewmodel.DownloadFileWorker
import com.android.vadify.ui.chat.viewmodel.UploadFileWorker
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Provider

@Subcomponent(modules = [UploadFileWorker.Builder::class, DownloadFileWorker.Builder::class])
interface WorkerSubcomponent {
    fun workers(): Map<Class<out CoroutineWorker>, Provider<CoroutineWorker>>

    @Subcomponent.Builder
    interface Builder {
        @BindsInstance
        fun workerParameters(param: WorkerParameters): Builder
        fun build(): WorkerSubcomponent
    }
}
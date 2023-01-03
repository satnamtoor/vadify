package com.android.vadify.data.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Suppress("FunctionMaxLength", "TooManyFunctions")
@Module
class DbModule {
    @Provides
    @Singleton
    fun provideAppCache(context: Context): AppDb {
        return Room.databaseBuilder(context, AppDb::class.java, "Contact.db")
                .allowMainThreadQueries()
            .fallbackToDestructiveMigrationOnDowngrade()
            .fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideContactListCache(appDb: AppDb) = appDb.contactListCache()

    @Provides
    @Singleton
    fun provideChatListCache(appDb: AppDb) = appDb.chatListCache()

    @Provides
    @Singleton
    fun provideCallLogCache(appDb: AppDb) = appDb.callLogsCache()

    @Provides
    @Singleton
    fun provideCommandDao(appDb: AppDb) = appDb.commandsDao()

    @Provides
    @Singleton
    fun provideChatThreadDao(appDb: AppDb) = appDb.chatThreadCache()
}

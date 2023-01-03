package com.android.vadify.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.vadify.data.db.callLogs.CallListCache
import com.android.vadify.data.db.callLogs.CallLogs
import com.android.vadify.data.db.chat.Chat
import com.android.vadify.data.db.chat.ChatListCache
import com.android.vadify.data.db.chatThread.ChatThread
import com.android.vadify.data.db.chatThread.ChatThreadCache
import com.android.vadify.data.db.commands.CommandDao
import com.android.vadify.data.db.commands.Commands
import com.android.vadify.data.db.contact.Contact
import com.android.vadify.data.db.contact.ContactListCache


/**
 * API cache database
 */
const val SCHEMA_VERSION = 15

@TypeConverters(
    DataConverter::class,
    CallTypeConverters::class,
    ChatThreadMemberConverter::class,
    ChatThreadUserConverter::class
)

@Database(
    entities = [
        Contact::class,
        Chat::class,
        CallLogs::class,
        Commands::class,
        ChatThread::class
    ],
    version = SCHEMA_VERSION,
    exportSchema = false
)
@Suppress("TooManyFunctions", "UnnecessaryAbstractClass")
abstract class AppDb : RoomDatabase() {
    abstract fun contactListCache(): ContactListCache
    abstract fun chatListCache(): ChatListCache
    abstract fun commandsDao(): CommandDao
    abstract fun callLogsCache(): CallListCache
    abstract fun chatThreadCache(): ChatThreadCache

}

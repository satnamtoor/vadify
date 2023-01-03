package com.android.vadify.data.db
//
//import android.content.Context
//import androidx.room.Database
//import androidx.room.Room
//import androidx.room.RoomDatabase
//import com.android.vadify.data.db.commands.Commands
//import com.android.vadify.data.db.commands.CommandsDao
//
//
//@Database(entities = [Commands::class], version = 1, exportSchema = false)
//    abstract class CommadDatabase : RoomDatabase() {
//
//        abstract val commandDao: CommandsDao
//
//        companion object {
//
//            @Volatile
//            private var INSTANCE: CommadDatabase? = null
//
//            fun getInstance(context: Context): CommadDatabase {
//                synchronized(this) {
//                    var instance = INSTANCE
//
//                    if (instance == null) {
//                        instance = Room.databaseBuilder(
//                            context.applicationContext,
//                            CommadDatabase::class.java,
//                            "vadify_database"
//                        )
//                            .fallbackToDestructiveMigration()
//                            .build()
//                        INSTANCE = instance
//                    }
//                    return instance
//                }
//            }
//        }
//
//}
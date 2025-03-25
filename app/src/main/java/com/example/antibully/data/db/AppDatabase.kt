package com.example.antibully.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.antibully.data.db.dao.*
import com.example.antibully.data.models.*

@Database(
    entities = [Post::class, Alert::class, User::class, ChildLocalData::class],
    version = 6, // העליתי את הגרסה מ-5 ל-6 בגלל שינוי ב-entities
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun alertDao(): AlertDao
    abstract fun userDao(): UserDao
    abstract fun childDao(): ChildDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "anti_bully_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

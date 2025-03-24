package com.example.antibully.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.antibully.data.db.dao.PostDao
import com.example.antibully.data.db.dao.AlertDao
import com.example.antibully.data.db.dao.UserDao
import com.example.antibully.data.models.Post
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.User

@Database(entities = [Post::class, Alert::class, User::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun alertDao(): AlertDao
    abstract fun userDao(): UserDao  // <-- הוספה זהירה ונקייה

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

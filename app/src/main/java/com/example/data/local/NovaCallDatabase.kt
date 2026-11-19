package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SpeedDialEntity::class,
        BlockedNumberEntity::class,
        CallNoteEntity::class,
        AppCallRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NovaCallDatabase : RoomDatabase() {
    abstract fun novaCallDao(): NovaCallDao

    companion object {
        @Volatile
        private var INSTANCE: NovaCallDatabase? = null

        fun getInstance(context: Context): NovaCallDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NovaCallDatabase::class.java,
                    "novacall_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

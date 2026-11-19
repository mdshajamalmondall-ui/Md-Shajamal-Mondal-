package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "speed_dials")
data class SpeedDialEntity(
    @PrimaryKey val digit: Int,
    val name: String,
    val phoneNumber: String,
    val normalizedNumber: String,
    val preferredRoute: String
)

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey val normalizedNumber: String,
    val displayNumber: String,
    val label: String,
    val reason: String,
    val blockedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_notes")
data class CallNoteEntity(
    @PrimaryKey val normalizedNumber: String,
    val displayNumber: String,
    val contactName: String,
    val note: String,
    val hasReminder: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_call_records")
data class AppCallRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val normalizedNumber: String,
    val contactName: String?,
    val route: String,
    val timestamp: Long = System.currentTimeMillis()
)

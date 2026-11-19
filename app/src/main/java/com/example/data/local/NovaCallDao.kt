package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NovaCallDao {
    @Query("SELECT * FROM speed_dials ORDER BY digit ASC")
    fun observeSpeedDials(): Flow<List<SpeedDialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpeedDial(entity: SpeedDialEntity)

    @Query("DELETE FROM speed_dials WHERE digit = :digit")
    suspend fun deleteSpeedDial(digit: Int)

    @Query("SELECT * FROM blocked_numbers ORDER BY blockedAt DESC")
    fun observeBlockedNumbers(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers")
    suspend fun getAllBlockedNumbersOnce(): List<BlockedNumberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedNumber(entity: BlockedNumberEntity)

    @Query("DELETE FROM blocked_numbers WHERE normalizedNumber = :normalizedNumber")
    suspend fun deleteBlockedNumber(normalizedNumber: String)

    @Query("SELECT * FROM call_notes ORDER BY hasReminder DESC, updatedAt DESC")
    fun observeCallNotes(): Flow<List<CallNoteEntity>>

    @Query("SELECT * FROM call_notes")
    suspend fun getAllCallNotesOnce(): List<CallNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCallNote(entity: CallNoteEntity)

    @Query("DELETE FROM call_notes WHERE normalizedNumber = :normalizedNumber")
    suspend fun deleteCallNote(normalizedNumber: String)

    @Query("SELECT * FROM app_call_records ORDER BY timestamp DESC LIMIT 120")
    fun observeAppCallRecords(): Flow<List<AppCallRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppCallRecord(entity: AppCallRecordEntity)

    @Query("DELETE FROM app_call_records")
    suspend fun clearAppCallRecords()
}

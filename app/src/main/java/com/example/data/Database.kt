package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ZenNoteDao {
    @Query("SELECT * FROM zen_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<ZenNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ZenNote): Long

    @Delete
    suspend fun deleteNote(note: ZenNote)
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long
}

@Dao
interface SimulatedMessageDao {
    @Query("SELECT * FROM simulated_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<SimulatedMessage>>

    @Query("SELECT DISTINCT contactNameOrNumber FROM simulated_messages ORDER BY timestamp DESC")
    fun getAllContacts(): Flow<List<String>>

    @Query("SELECT * FROM simulated_messages WHERE contactNameOrNumber = :contact ORDER BY timestamp ASC")
    fun getMessagesWithContact(contact: String): Flow<List<SimulatedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SimulatedMessage): Long

    @Query("DELETE FROM simulated_messages")
    suspend fun deleteAllMessages()
}

@Dao
interface FocusScheduleDao {
    @Query("SELECT * FROM focus_schedules ORDER BY id ASC")
    fun getAllSchedules(): Flow<List<FocusSchedule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: FocusSchedule): Long

    @Query("DELETE FROM focus_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)
}

@Database(entities = [ZenNote::class, FocusSession::class, SimulatedMessage::class, FocusSchedule::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zenNoteDao(): ZenNoteDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun simulatedMessageDao(): SimulatedMessageDao
    abstract fun focusScheduleDao(): FocusScheduleDao
}

package com.example.data

import kotlinx.coroutines.flow.Flow

class FocusRepository(private val db: AppDatabase) {
    val notes: Flow<List<ZenNote>> = db.zenNoteDao().getAllNotes()
    val sessions: Flow<List<FocusSession>> = db.focusSessionDao().getAllSessions()
    val allMessages: Flow<List<SimulatedMessage>> = db.simulatedMessageDao().getAllMessages()
    val allContacts: Flow<List<String>> = db.simulatedMessageDao().getAllContacts()
    val schedules: Flow<List<FocusSchedule>> = db.focusScheduleDao().getAllSchedules()

    fun getMessagesWithContact(contact: String): Flow<List<SimulatedMessage>> =
        db.simulatedMessageDao().getMessagesWithContact(contact)

    suspend fun saveNote(note: ZenNote) = db.zenNoteDao().insertNote(note)
    suspend fun deleteNote(note: ZenNote) = db.zenNoteDao().deleteNote(note)

    suspend fun logSession(session: FocusSession) = db.focusSessionDao().insertSession(session)

    suspend fun insertMessage(message: SimulatedMessage) =
        db.simulatedMessageDao().insertMessage(message)

    suspend fun clearMessages() = db.simulatedMessageDao().deleteAllMessages()

    suspend fun saveSchedule(schedule: FocusSchedule) =
        db.focusScheduleDao().insertSchedule(schedule)

    suspend fun deleteSchedule(id: Long) =
        db.focusScheduleDao().deleteScheduleById(id)
}

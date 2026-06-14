package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zen_notes")
data class ZenNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val durationMinutes: Int,
    val startTime: Long,
    val endTime: Long,
    val status: String // "COMPLETED" or "INTERRUPTED"
)

@Entity(tableName = "simulated_messages")
data class SimulatedMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val contactNameOrNumber: String,
    val messageText: String,
    val timestamp: Long,
    val isSentByUser: Boolean
)

@Entity(tableName = "focus_schedules")
data class FocusSchedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,              // e.g., "Morning Physical Activity"
    val startTime: String,          // Short HH:mm format, e.g., "06:00"
    val endTime: String,            // Short HH:mm format, e.g., "08:30"
    val daysOfWeek: String,         // Comma separated short names, e.g., "Mon,Tue,Wed,Thu,Fri,Sat"
    val isEnabled: Boolean = true   // Master switch for this schedule
)

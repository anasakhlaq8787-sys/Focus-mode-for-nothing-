package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FeatureApp {
    NONE, DIALER, SMS, NOTES, STATS
}

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("focus_lock_prefs", Context.MODE_PRIVATE)
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "focus_lock_db"
    ).fallbackToDestructiveMigration().build()

    val repository = FocusRepository(db)

    // UI state
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _lockStartTime = MutableStateFlow(0L)
    val lockStartTime: StateFlow<Long> = _lockStartTime.asStateFlow()

    private val _lockEndTime = MutableStateFlow(0L)
    val lockEndTime: StateFlow<Long> = _lockEndTime.asStateFlow()

    private val _timeRemainingMs = MutableStateFlow(0L)
    val timeRemainingMs: StateFlow<Long> = _timeRemainingMs.asStateFlow()

    private val _currentApp = MutableStateFlow(FeatureApp.NONE)
    val currentApp: StateFlow<FeatureApp> = _currentApp.asStateFlow()

    // Notes
    val notes: StateFlow<List<ZenNote>> = repository.notes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Focus session logs
    val sessions: StateFlow<List<FocusSession>> = repository.sessions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Automated focus schedules
    val schedules: StateFlow<List<FocusSchedule>> = repository.schedules.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // SMS thread simulation
    val contacts: StateFlow<List<String>> = repository.allContacts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedContact = MutableStateFlow<String?>(null)
    val selectedContact: StateFlow<String?> = _selectedContact.asStateFlow()

    val activeMessages: StateFlow<List<SimulatedMessage>> = _selectedContact
        .flatMapLatest { contact ->
            if (contact != null) {
                repository.getMessagesWithContact(contact)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Emergency hold-to-unlock progress
    private val _emergencyProgress = MutableStateFlow(0f)
    val emergencyProgress: StateFlow<Float> = _emergencyProgress.asStateFlow()

    private var timerJob: Job? = null
    private var emergencyHoldJob: Job? = null

    init {
        // Restore focus lock state on app launch/restart
        val startTime = sharedPrefs.getLong("lock_start_time", 0L)
        val endTime = sharedPrefs.getLong("lock_end_time", 0L)
        val currentlyLocked = sharedPrefs.getBoolean("is_locked", false)

        val currentTime = System.currentTimeMillis()
        if (currentlyLocked && currentTime < endTime) {
            _isLocked.value = true
            _lockStartTime.value = startTime
            _lockEndTime.value = endTime
            _timeRemainingMs.value = endTime - currentTime
            startTimer()
        } else if (currentlyLocked) {
            // Expired while app was closed
            viewModelScope.launch {
                val durationMins = ((endTime - startTime) / 60000).toInt()
                repository.logSession(
                    FocusSession(
                        durationMinutes = if (durationMins > 0) durationMins else 1,
                        startTime = startTime,
                        endTime = endTime,
                        status = "COMPLETED"
                    )
                )
                clearLockState()
            }
        }

        // Pre-seed messaging data if database is empty
        viewModelScope.launch {
            repository.allMessages.first().let { currentMsgs ->
                if (currentMsgs.isEmpty()) {
                    seedDefaultMessages()
                }
            }
        }

        // Sync Scheduled Locks Periodically
        viewModelScope.launch {
            while (true) {
                try {
                    checkSchedulesAndLock()
                } catch (_: Exception) {
                    // Ignore check exceptions to keep background sync thread alive
                }
                delay(15000) // check every 15 seconds
            }
        }
    }

    // Start Focus Mode
    fun startFocusSession(durationMinutes: Int) {
        val startTime = System.currentTimeMillis()
        val durationMs = durationMinutes * 60 * 1000L
        val endTime = startTime + durationMs

        // Save in shared prefs
        sharedPrefs.edit()
            .putLong("lock_start_time", startTime)
            .putLong("lock_end_time", endTime)
            .putBoolean("is_locked", true)
            .apply()

        _isLocked.value = true
        _lockStartTime.value = startTime
        _lockEndTime.value = endTime
        _timeRemainingMs.value = durationMs
        _currentApp.value = FeatureApp.NONE

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemainingMs.value > 0) {
                delay(1000)
                val remaining = _lockEndTime.value - System.currentTimeMillis()
                if (remaining <= 0) {
                    _timeRemainingMs.value = 0
                    completeFocusSession()
                    break
                } else {
                    _timeRemainingMs.value = remaining
                }
            }
        }
    }

    private suspend fun completeFocusSession() {
        val startTime = _lockStartTime.value
        val endTime = _lockEndTime.value
        val durationMins = ((endTime - startTime) / 60000).toInt()

        repository.logSession(
            FocusSession(
                durationMinutes = if (durationMins > 0) durationMins else 1,
                startTime = startTime,
                endTime = endTime,
                status = "COMPLETED"
            )
        )

        // If it was scheduled, mark it bypassed/completed for today
        val activeScheduleId = sharedPrefs.getLong("active_schedule_id", -1L)
        if (activeScheduleId != -1L) {
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            sharedPrefs.edit()
                .putBoolean("bypassed_${activeScheduleId}_$todayDateStr", true)
                .remove("active_schedule_id")
                .apply()
        }

        clearLockState()
    }

    private fun clearLockState() {
        timerJob?.cancel()
        timerJob = null

        sharedPrefs.edit()
            .remove("lock_start_time")
            .remove("lock_end_time")
            .putBoolean("is_locked", false)
            .apply()

        _isLocked.value = false
        _lockStartTime.value = 0L
        _lockEndTime.value = 0L
        _timeRemainingMs.value = 0L
        _currentApp.value = FeatureApp.NONE
    }

    // Navigation between built-in Feature apps
    fun openApp(app: FeatureApp) {
        _currentApp.value = app
    }

    fun closeApp() {
        _currentApp.value = FeatureApp.NONE
    }

    // Dialer calls (initiates simple phone DIAL intent, standard capability on any dumbphone for emergencies)
    fun triggerDialIntent(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            getApplication<Application>().startActivity(intent)
        } catch (_: Exception) {
            // Fallback if no dialer app
        }
    }

    // SMS Simulated Actions
    fun selectContact(contactName: String?) {
        _selectedContact.value = contactName
    }

    fun sendSimulatedMessage(contact: String, text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val userMsg = SimulatedMessage(
                contactNameOrNumber = contact,
                messageText = text,
                timestamp = System.currentTimeMillis(),
                isSentByUser = true
            )
            repository.insertMessage(userMsg)

            // Auto-respond for specialized "Zen Bot" contact
            if (contact.equals("Zen Bot", ignoreCase = true)) {
                delay(1200)
                val reply = getZenBotResponse(text)
                val botMsg = SimulatedMessage(
                    contactNameOrNumber = "Zen Bot",
                    messageText = reply,
                    timestamp = System.currentTimeMillis(),
                    isSentByUser = false
                )
                repository.insertMessage(botMsg)
            } else {
                // Muted Auto-Reply for others during focus mode
                delay(2000)
                val autoReply = "Focus Lock Notice: I have locked my phone to limit distractions. I'll get back to you soon!"
                val sysMsg = SimulatedMessage(
                    contactNameOrNumber = contact,
                    messageText = autoReply,
                    timestamp = System.currentTimeMillis(),
                    isSentByUser = false
                )
                repository.insertMessage(sysMsg)
            }
        }
    }

    private fun getZenBotResponse(userInput: String): String {
        val text = userInput.lowercase()
        return when {
            text.contains("hello") || text.contains("hi") -> {
                "Peace be with you. I am Zen Bot, your quiet companion. Type 'quote' for minimalist motivation, or tell me how your focus is going."
            }
            text.contains("quote") || text.contains("inspire") || text.contains("motivation") -> {
                val quotes = listOf(
                    "Simplicity is the ultimate sophistication. — Leonardo da Vinci",
                    "Do not dwell in the past, do not dream of the future, concentrate the mind on the present moment. — Buddha",
                    "Real freedom is freedom from distraction.",
                    "Focus is more about saying 'no' than saying 'yes'. — Steve Jobs",
                    "The quiet mind accomplishes all things.",
                    "Your mind is for having ideas, not holding them. — David Allen",
                    "Disconnect to reconnect."
                )
                quotes.random()
            }
            text.contains("hard") || text.contains("distract") || text.contains("bored") -> {
                "Distractions are normal. Take three slow breaths, feel the air fill your chest, let the impulse to check social media float away, and return to your offline craft."
            }
            else -> {
                "Focus on the work ahead of you. No notifications. No feeds. Just you and your pure intent. What are you building right now?"
            }
        }
    }

    private suspend fun seedDefaultMessages() {
        val now = System.currentTimeMillis()
        val messages = listOf(
            SimulatedMessage(contactNameOrNumber = "Zen Bot", messageText = "Welcome to your Focus Lock phone. Send me a message to chat in offline-mode. Text 'quote' for mindful advice.", timestamp = now - 3600000, isSentByUser = false),
            SimulatedMessage(contactNameOrNumber = "Mom", messageText = "Are you focusing on your project, honey?", timestamp = now - 7200000, isSentByUser = false),
            SimulatedMessage(contactNameOrNumber = "Mom", messageText = "Yes, I started the lockout now!", timestamp = now - 7100000, isSentByUser = true),
            SimulatedMessage(contactNameOrNumber = "Mom", messageText = "Proud of you, talk later!", timestamp = now - 7000000, isSentByUser = false),
            SimulatedMessage(contactNameOrNumber = "Work Buddy", messageText = "The updates look great, let's connect after your deep work session.", timestamp = now - 14400000, isSentByUser = false)
        )
        for (m in messages) {
            repository.insertMessage(m)
        }
    }

    // Notes Operations
    fun saveZenNote(id: Long, title: String, content: String) {
        viewModelScope.launch {
            val note = ZenNote(
                id = id,
                title = title.ifBlank { "Untitled Memo" },
                content = content,
                timestamp = System.currentTimeMillis()
            )
            repository.saveNote(note)
        }
    }

    fun deleteZenNote(note: ZenNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Emergency Hold-to-Unlock API
    fun startEmergencyHold() {
        emergencyHoldJob?.cancel()
        _emergencyProgress.value = 0f
        emergencyHoldJob = viewModelScope.launch {
            val duration = 5000L // 5 seconds
            val steps = 50
            val delayMs = duration / steps
            for (i in 1..steps) {
                delay(delayMs)
                _emergencyProgress.value = i.toFloat() / steps
            }
            // Trigger emergency bypass
            interruptFocusSession()
        }
    }

    fun cancelEmergencyHold() {
        emergencyHoldJob?.cancel()
        emergencyHoldJob = null
        _emergencyProgress.value = 0f
    }

    private fun interruptFocusSession() {
        viewModelScope.launch {
            val startTime = _lockStartTime.value
            val endTime = _lockEndTime.value
            val durationMins = ((System.currentTimeMillis() - startTime) / 60000).toInt()

            repository.logSession(
                FocusSession(
                    durationMinutes = if (durationMins > 0) durationMins else 1,
                    startTime = startTime,
                    endTime = System.currentTimeMillis(),
                    status = "INTERRUPTED"
                )
            )

            // If it was scheduled, mark it bypassed/completed for (today) so we don't auto-lock immediately
            val activeScheduleId = sharedPrefs.getLong("active_schedule_id", -1L)
            if (activeScheduleId != -1L) {
                val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                sharedPrefs.edit()
                    .putBoolean("bypassed_${activeScheduleId}_$todayDateStr", true)
                    .remove("active_schedule_id")
                    .apply()
            }

            clearLockState()
        }
    }

    // Schedule Operations
    fun saveFocusSchedule(id: Long, label: String, startTime: String, endTime: String, daysOfWeek: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val schedule = FocusSchedule(
                id = id,
                label = label.ifBlank { "Focus Sync" },
                startTime = startTime,
                endTime = endTime,
                daysOfWeek = daysOfWeek,
                isEnabled = isEnabled
            )
            repository.saveSchedule(schedule)
        }
    }

    fun deleteFocusSchedule(id: Long) {
        viewModelScope.launch {
            repository.deleteSchedule(id)
        }
    }

    private suspend fun checkSchedulesAndLock() {
        if (_isLocked.value) return // Already locked

        val now = System.currentTimeMillis()
        val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        val currentDayOfWeek = SimpleDateFormat("E", Locale.US).format(Date(now)) // e.g. "Sun", "Mon", "Tue" etc.
        val currentTimeStr = SimpleDateFormat("HH:mm", Locale.US).format(Date(now))

        // Get enabled schedules from DB
        val enabledSchedules = repository.schedules.first().filter { it.isEnabled }

        for (schedule in enabledSchedules) {
            // Check day of week
            val daysList = schedule.daysOfWeek.split(",").map { it.trim() }
            if (daysList.contains(currentDayOfWeek) || schedule.daysOfWeek.lowercase().contains("everyday")) {
                // Check if bypassed for today
                val isBypassed = sharedPrefs.getBoolean("bypassed_${schedule.id}_$todayDateStr", false)
                if (!isBypassed) {
                    // Check if current time is within schedule window
                    if (isTimeInWindow(currentTimeStr, schedule.startTime, schedule.endTime)) {
                        val remainingMins = getRemainingMinutes(currentTimeStr, schedule.endTime)
                        if (remainingMins > 0) {
                            // Register active schedule ID to bypass/complete cleanly
                            sharedPrefs.edit()
                                .putLong("active_schedule_id", schedule.id)
                                .apply()
                            
                            startFocusSession(remainingMins)
                            break
                        }
                    }
                }
            }
        }
    }

    private fun isTimeInWindow(currentTime: String, start: String, end: String): Boolean {
        return try {
            val (currH, currM) = currentTime.split(":").map { it.toInt() }
            val (startH, startM) = start.split(":").map { it.toInt() }
            val (endH, endM) = end.split(":").map { it.toInt() }

            val currMins = currH * 60 + currM
            val startMins = startH * 60 + startM
            val endMins = endH * 60 + endM

            if (endMins >= startMins) {
                currMins in startMins until endMins
            } else {
                currMins >= startMins || currMins < endMins
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getRemainingMinutes(currentTime: String, end: String): Int {
        return try {
            val (currH, currM) = currentTime.split(":").map { it.toInt() }
            val (endH, endM) = end.split(":").map { it.toInt() }

            val currMins = currH * 60 + currM
            var endMins = endH * 60 + endM

            if (endMins < currMins) {
                endMins += 24 * 60
            }
            endMins - currMins
        } catch (_: Exception) {
            0
        }
    }
}

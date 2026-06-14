package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val focusViewModel: FocusViewModel = viewModel()
                    FocusLockApp(viewModel = focusViewModel)
                }
            }
        }
    }
}

@Composable
fun FocusLockApp(viewModel: FocusViewModel) {
    val isLocked by viewModel.isLocked.collectAsState()
    val currentApp by viewModel.currentApp.collectAsState()

    var systemTime by remember { mutableStateOf("12:00") }
    var systemDate by remember { mutableStateOf("Sun, Jun 14") }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            systemTime = SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
            systemDate = SimpleDateFormat("EEE, MMM dd", Locale.US).format(cal.time)
            delay(1000)
        }
    }

    // Modern minimalistic E-Ink LCD dot-matrix background pattern (subtle accent)
    val dotMatrixBg = Modifier.drawBehind {
        val dotRadius = 1.0f
        val spacing = 32f
        var x = 0f
        while (x < size.width) {
            var y = 0f
            while (y < size.height) {
                drawCircle(
                    color = Color(0xD0, 0xBC, 0xFF).copy(alpha = 0.04f), // SophisticatedPrimary color
                    radius = dotRadius,
                    center = Offset(x, y)
                )
                y += spacing
            }
            x += spacing
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    Scaffold(
        topBar = {
            CustomOsStatusBar(
                isLocked = isLocked,
                timeStr = systemTime,
                currentApp = currentApp,
                viewModel = viewModel
            )
        },
        bottomBar = {
            CustomOsBottomNavBar(
                isLocked = isLocked,
                currentApp = currentApp,
                viewModel = viewModel
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .then(dotMatrixBg)
        ) {
            Crossfade(targetState = isLocked, label = "session_state", animationSpec = tween(400)) { locked ->
                if (locked) {
                    when (currentApp) {
                        FeatureApp.NONE -> DumbphoneHome(viewModel = viewModel)
                        FeatureApp.DIALER -> DialerScreen(viewModel = viewModel)
                        FeatureApp.SMS -> SmsScreen(viewModel = viewModel)
                        FeatureApp.NOTES -> NotesScreen(viewModel = viewModel)
                        FeatureApp.STATS -> StatsScreen(viewModel = viewModel)
                    }
                } else {
                    FocusControlCenter(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CustomOsStatusBar(
    isLocked: Boolean,
    timeStr: String,
    currentApp: FeatureApp,
    viewModel: FocusViewModel
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(width = 0.5.dp, color = SophisticatedOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal strength custom graphics and operating system name
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tactical custom-drawn e-ink network strength bars
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    val activeColor = MaterialTheme.colorScheme.primary
                    val inactiveColor = SophisticatedOutline.copy(alpha = 0.5f)
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(2.dp, (3 + i * 2).dp)
                                .background(if (i <= 4) activeColor else inactiveColor)
                        )
                    }
                }
                
                Text(
                    text = "ZEN_NET 5G",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Central Capsule: Active system operating state
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(
                        color = if (isLocked) SophisticatedDarkRose else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = if (isLocked) SophisticatedRoseRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (isLocked) SophisticatedRoseRed else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isLocked) {
                        if (currentApp == FeatureApp.NONE) "ZEN_OS: LOCKED" else "OS: ${currentApp.name}"
                    } else "ZEN_OS: CONFIG",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) SophisticatedRoseRed else MaterialTheme.colorScheme.primary
                )
            }

            // Right Info: Custom Battery Graphics + Simple Monospace Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp, 10.dp)
                        .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.85f)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(1.dp))
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                _12HoursText(timeStr)
            }
        }
    }
}

@Composable
fun CustomOsBottomNavBar(
    isLocked: Boolean,
    currentApp: FeatureApp,
    viewModel: FocusViewModel
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(width = 0.5.dp, color = SophisticatedOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // OS BACK BUTTON: tactile click to exit features or back-traverse
            val isBackActive = isLocked && currentApp != FeatureApp.NONE
            IconButton(
                onClick = {
                    if (isBackActive) {
                        viewModel.closeApp()
                    }
                },
                enabled = isBackActive,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "OS Navigation Back",
                        tint = if (isBackActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "BACK",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isBackActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        }
                    )
                }
            }

            // OS HOME BUTTON: tactile centered OS key returns back to safe monochrome terminal screen
            IconButton(
                onClick = {
                    if (isLocked) {
                        viewModel.closeApp()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "OS Navigation Home",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "HOME",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // STATUS INDICATOR / SECURITY ENVELOPE CONFIG KEY
            IconButton(
                onClick = {
                    // Visual/Tactile interactive state switch indicator feedback
                },
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Settings,
                        contentDescription = "OS Security Level",
                        tint = if (isLocked) SophisticatedRoseRed else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (isLocked) "SECURE" else "UNLOCKED",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (isLocked) SophisticatedRoseRed else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==========================================
// NOTHING OS CORE WIDGET COMPONENTS
// ==========================================
@Composable
fun NothingWidget(
    modifier: Modifier = Modifier,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val baseModifier = modifier
        .clip(RoundedCornerShape(24.dp))
        .background(SophisticatedSurface)
        .border(borderWidth, SophisticatedOutline, RoundedCornerShape(24.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    
    Box(
        modifier = baseModifier.padding(16.dp),
        content = content
    )
}

// ==========================================
// UNLOCKED VIEW: Set Lock parameters
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusControlCenter(viewModel: FocusViewModel) {
    var selectedMinutes by remember { mutableStateOf(30) }
    val sessions by viewModel.sessions.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    // Screen scheduling states
    var showAddSchedule by remember { mutableStateOf(false) }
    var scheduleLabel by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf(6) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(8) }
    var endMinute by remember { mutableStateOf(30) }
    val selectedDays = remember { mutableStateListOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }

    val totalFocusMins = remember(sessions) {
        sessions.filter { it.status == "COMPLETED" }.sumOf { it.durationMinutes }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
    ) {
        // 1. Digital Console Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "• • • • • • • • • • • • • • • • • • • • • • • • • • • • • •",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SophisticatedOutline,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ZEN_OS : CONFIG_CENTER",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "NOTHING ZENLOCK",
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(SophisticatedRoseRed, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                NothingDigitalClockWidget(isFocusModeActive = false)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 2. Symmetric Widget Row (Live Stat Module & Fast lock launcher side-by-side)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Wing: Focus metrics telemetry
                NothingWidget(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.15f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MINUTES LOCKED",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Column {
                            Text(
                                text = "$totalFocusMins",
                                fontSize = 42.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(48.dp)
                            )
                            Text(
                                text = "TOTAL LOGGED TODAY",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // Right Wing: Direct 30 Min deep lock engage shortcut
                NothingWidget(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.15f),
                    onClick = { viewModel.startFocusSession(30) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "QUICK SECURE",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SophisticatedRoseRed, CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Quick engage",
                                tint = MaterialTheme.colorScheme.background,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "START 30 MINS",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 3. Time Selection Adjustment Widget Panel
        item {
            NothingWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column {
                    Text(
                        text = "LOCK RANGE GAUGE",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Rounded Preselector Toggles
                    val durations = listOf(5, 15, 30, 45, 60, 120)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        spacing = 8.dp
                    ) {
                        durations.forEach { mins ->
                            val isSelected = selectedMinutes == mins
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else SophisticatedOutline
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else SophisticatedOutline,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedMinutes = mins },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mins",
                                    color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = it.toInt() },
                        valueRange = 1f..360f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = SophisticatedOutline,
                            thumbColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CUSTOM PRESET ADJUSTER",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$selectedMinutes",
                                fontSize = 18.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MINS",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }

        // 4. Primary Hardware Secure Launcher Button
        item {
            Button(
                onClick = { viewModel.startFocusSession(selectedMinutes) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(60.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(width = 1.5.dp, color = MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SophisticatedRoseRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "LOCK SYSTEM HARDWARE",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        // 5. Automated Schedulers Listing Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUTOMATED SCHEDULERS",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.5.sp
                )
                
                Text(
                    text = if (showAddSchedule) "CLOSE PANEL" else "+ ADD ACTIVE SYNC",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        showAddSchedule = !showAddSchedule
                    }
                )
            }
        }

        // Create Automated rule card
        if (showAddSchedule) {
            item {
                NothingWidget(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    borderWidth = 1.5.dp
                ) {
                    Column {
                        Text(
                            text = "CREATE SYNC WINDOW",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = scheduleLabel,
                            onValueChange = { scheduleLabel = it },
                            placeholder = { Text("e.g. Study Session, No Digital Noise", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = SophisticatedOutline,
                                cursorColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Text(
                            text = "SELECT ACTIVE DAYS",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            daysList.forEach { day ->
                                val isDaySelected = selectedDays.contains(day)
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDaySelected) MaterialTheme.colorScheme.primary else SophisticatedOutline
                                        )
                                        .clickable {
                                            if (isDaySelected) {
                                                if (selectedDays.size > 1) {
                                                    selectedDays.remove(day)
                                                }
                                            } else {
                                                selectedDays.add(day)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.take(2),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDaySelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "START TIME",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                CustomTactileTimePicker(
                                    hour = startHour,
                                    minute = startMinute,
                                    onHourChange = { startHour = it },
                                    onMinuteChange = { startMinute = it }
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "END TIME",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                CustomTactileTimePicker(
                                    hour = endHour,
                                    minute = endMinute,
                                    onHourChange = { endHour = it },
                                    onMinuteChange = { endMinute = it }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                showAddSchedule = false
                                scheduleLabel = ""
                            }) {
                                Text("CANCEL", color = MaterialTheme.colorScheme.secondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (selectedDays.isNotEmpty()) {
                                        val startStr = String.format("%02d:%02d", startHour, startMinute)
                                        val endStr = String.format("%02d:%02d", endHour, endMinute)
                                        viewModel.saveFocusSchedule(
                                            id = 0L,
                                            label = scheduleLabel.ifBlank { "System Focus Sync" },
                                            startTime = startStr,
                                            endTime = endStr,
                                            daysOfWeek = selectedDays.joinToString(", "),
                                            isEnabled = true
                                        )
                                        showAddSchedule = false
                                        scheduleLabel = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                enabled = selectedDays.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("SAVE RULE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.background)
                            }
                        }
                    }
                }
            }
        }

        // List active rules
        if (schedules.isEmpty()) {
            item {
                NothingWidget(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No automatic schedulers active.\nClick + ADD ACTIVE SYNC above to secure daily windows.",
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        } else {
            items(schedules) { schedule ->
                NothingWidget(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = schedule.label.uppercase(Locale.US),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (schedule.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Active lock duration",
                                    tint = if (schedule.isEnabled) SophisticatedRoseRed else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatScheduleTime(schedule.startTime) + " - " + formatScheduleTime(schedule.endTime),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (schedule.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
                            Text(
                                text = schedule.daysOfWeek.uppercase(Locale.US),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = schedule.isEnabled,
                                onCheckedChange = { isChecked ->
                                    viewModel.saveFocusSchedule(
                                        id = schedule.id,
                                        label = schedule.label,
                                        startTime = schedule.startTime,
                                        endTime = schedule.endTime,
                                        daysOfWeek = schedule.daysOfWeek,
                                        isEnabled = isChecked
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.background,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.secondary,
                                    uncheckedTrackColor = SophisticatedOutline
                                )
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = { viewModel.deleteFocusSchedule(schedule.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove rule",
                                    tint = SophisticatedRoseRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Historic Session Logs
        item {
            Text(
                text = "HISTORICAL INTENTS",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (sessions.isEmpty()) {
            item {
                NothingWidget(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No focus sessions logged today.\nEngage the hardware locks to track mindful windows.",
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        } else {
            items(sessions.take(4)) { s ->
                val timeFormat = remember { SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()) }
                val startStr = timeFormat.format(Date(s.startTime))
                NothingWidget(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${s.durationMinutes} MINUTES LOCKED",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = startStr.uppercase(Locale.US),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        val success = s.status == "COMPLETED"
                        Box(
                            modifier = Modifier
                                .background(
                                    if (success) Color(0xFF101C14) else Color(0xFF231012),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (success) Color(0xFF2B5B3B) else Color(0xFF5B2B2B),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = s.status,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (success) Color(0xFF4CAF50) else SophisticatedRoseRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PORTABLE HELPING LAYOUT: Simple FlowRow
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    spacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = { content() }
    )
}

// ==========================================
// LOCKED STATE: Immersive Dumbphone Home
// ==========================================
@Composable
fun DumbphoneHome(viewModel: FocusViewModel) {
    val lockStartTime by viewModel.lockStartTime.collectAsState()
    val lockEndTime by viewModel.lockEndTime.collectAsState()
    val timeRemainingMs by viewModel.timeRemainingMs.collectAsState()
    val emergencyProgress by viewModel.emergencyProgress.collectAsState()

    var systemTime by remember { mutableStateOf("") }
    var systemDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val cal = Calendar.getInstance()
            systemTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
            systemDate = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(cal.time)
            delay(1000)
        }
    }

    // Format remaining duration
    val secondsLeft = (timeRemainingMs / 1000) % 60
    val minutesLeft = (timeRemainingMs / (1000 * 60)) % 60
    val hoursLeft = (timeRemainingMs / (1000 * 60 * 60)) % 24
    val countdownString = String.format("%02d:%02d:%02d", hoursLeft, minutesLeft, secondsLeft)

    val totalDuration = lockEndTime - lockStartTime
    val elapsed = totalDuration - timeRemainingMs
    val progressFraction = if (totalDuration > 0) (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f) else 1f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Nothing Phone Inspired Digital Clock Widget
        Spacer(modifier = Modifier.height(8.dp))
        NothingDigitalClockWidget(
            modifier = Modifier.fillMaxWidth(),
            isFocusModeActive = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 2. Hardware Glyph Radial Interaction Circle Base (Displays focus countdown)
        Box(
            modifier = Modifier
                .size(210.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val outlineColor = SophisticatedOutline
            
            // Render 24 Physical Glyph LED Dot Segments
            Canvas(modifier = Modifier.fillMaxSize()) {
                val numDots = 24
                val radius = (size.minDimension - 16.dp.toPx()) / 2f
                val centerPoint = Offset(size.width / 2f, size.height / 2f)
                
                for (i in 0 until numDots) {
                    val angleDegrees = (i * (360f / numDots)) - 90f
                    val angleRad = Math.toRadians(angleDegrees.toDouble())
                    val dotCenter = Offset(
                        (centerPoint.x + radius * Math.cos(angleRad)).toFloat(),
                        (centerPoint.y + radius * Math.sin(angleRad)).toFloat()
                    )
                    
                    val fraction = i.toPercentFraction(numDots)
                    // Light dots matching remaining sweep percent
                    val isLit = (1f - progressFraction) >= fraction
                    
                    drawCircle(
                        color = if (isLit) Color.White else outlineColor.copy(alpha = 0.25f),
                        radius = 4.dp.toPx(),
                        center = dotCenter
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = countdownString,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "LOCKED_SECURE",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 3. Essential Permitted Utilities Launcher Board
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "• PERMITTED UTILITIES •",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Layout 4 essential apps in symmetrical 2x2 grid format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicAppIcon(
                        title = "Phone",
                        tag = "dial_app",
                        icon = Icons.Default.Phone,
                        onClick = { viewModel.openApp(FeatureApp.DIALER) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    BasicAppIcon(
                        title = "Messages",
                        tag = "sms_app",
                        icon = Icons.Default.Send,
                        onClick = { viewModel.openApp(FeatureApp.SMS) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    BasicAppIcon(
                        title = "Notes",
                        tag = "notes_app",
                        icon = Icons.Default.Edit,
                        onClick = { viewModel.openApp(FeatureApp.NOTES) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    BasicAppIcon(
                        title = "Sessions",
                        tag = "stats_app",
                        icon = Icons.Default.List,
                        onClick = { viewModel.openApp(FeatureApp.STATS) }
                    )
                }
            }
        }

        // 4. Emergency Hold Control Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D0D0E))
                    .border(1.5.dp, SophisticatedOutline, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.startEmergencyHold()
                                try {
                                    awaitRelease()
                                } finally {
                                    viewModel.cancelEmergencyHold()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(emergencyProgress)
                        .background(SophisticatedRoseRed.copy(alpha = 0.25f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (emergencyProgress > 0.05f) "DISENGAGING OS TARGET (${(emergencyProgress * 100).toInt()}%)" else "••• HOLD FOR EMERGENCY BYPASS •••",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedRoseRed,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .width(112.dp)
                    .height(4.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f), CircleShape)
            )
        }
    }
}

// Inline helper to calculate step ratio securely 
private fun Int.toPercentFraction(totalSteps: Int): Float {
    return this.toFloat() / totalSteps.toFloat()
}

@Composable
fun _12HoursText(time24: String) {
    val text = remember(time24) {
        try {
            val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf24.parse(time24)
            if (date != null) {
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
            } else {
                time24
            }
        } catch (_: Exception) {
            time24
        }
    }
    Text(
        text = text,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

// Minimal Grid Icon Button (Nothing OS Glyph Tile)
@Composable
fun BasicAppIcon(
    title: String,
    tag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(SophisticatedSurface, CircleShape)
                .border(1.5.dp, SophisticatedOutline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title.uppercase(Locale.US),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 0.5.sp
        )
    }
}

// ==========================================
// FEATURE APP: Dialer
// ==========================================
@Composable
fun DialerScreen(viewModel: FocusViewModel) {
    var dialNumber by remember { mutableStateOf("") }
    val dialHistory = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { viewModel.closeApp() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "BASIC DIALER",
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }

        // Display area (Nothing OS Matte Box)
        NothingWidget(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dialNumber.ifEmpty { "ENTER NUMBER" },
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (dialNumber.isEmpty()) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(SophisticatedRoseRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ZEN COGNITIVE BYPASS ACTIVE",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tactile Keypad (Symmetrical Circles)
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    row.forEach { k ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SophisticatedSurface)
                                .border(1.5.dp, SophisticatedOutline, CircleShape)
                                .clickable { dialNumber += k },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = k,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dialer actions row (Call & Clear)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                // Backspace/Delete Key
                Box(
                    modifier = Modifier
                        .size(100.dp, 52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(SophisticatedSurface)
                        .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(26.dp))
                        .clickable {
                            if (dialNumber.isNotEmpty()) {
                                dialNumber = dialNumber.dropLast(1)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Place Call trigger Intent with Red Symmetrical Outline Pill
                Box(
                    modifier = Modifier
                        .size(110.dp, 52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .clickable {
                            if (dialNumber.isNotBlank()) {
                                dialHistory.add(dialNumber)
                                viewModel.triggerDialIntent(dialNumber)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Call Intent",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CALL",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Log of dialed numbers
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SophisticatedSurface, RoundedCornerShape(16.dp))
                .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "CALL HISTORY (LOCK_SESSION)",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (dialHistory.isEmpty()) {
                Text(
                    text = "No call intents placed in lock window.",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                )
            } else {
                LazyColumn {
                    items(dialHistory.toList().reversed()) { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SophisticatedRoseRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DIALED: $item (SYSTEM REDIRECT)",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// FEATURE APP: SMS Messages with Zen Bot Support
// ==========================================
@Composable
fun SmsScreen(viewModel: FocusViewModel) {
    val contacts by viewModel.contacts.collectAsState()
    val selectedContact by viewModel.selectedContact.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll chat bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (selectedContact != null) {
                        viewModel.selectContact(null)
                    } else {
                        viewModel.closeApp()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = (selectedContact ?: "SMS INBOX").uppercase(Locale.US),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selectedContact != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(SophisticatedRoseRed, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE_CONN",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedRoseRed,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedContact == null) {
            // Contacts / Threads list view
            Text(
                text = "• DECRYPTED INBOX TARGETS •",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(contacts) { contact ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(16.dp))
                                .clickable { viewModel.selectContact(contact) },
                            color = SophisticatedSurface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(SophisticatedBg, CircleShape)
                                            .border(1.5.dp, SophisticatedOutline, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.take(2).uppercase(Locale.US),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = contact.uppercase(Locale.US),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (contact == "Zen Bot") "FOCUSED AI SECURE COGNITION" else "AUTHORIZED EMERGENCY CHANNEL",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Open Chat",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Conversational active chat view
            Column(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Transparent)
                        .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        val alignLeft = !msg.isSentByUser
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (alignLeft) Arrangement.Start else Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (alignLeft) 4.dp else 16.dp,
                                            bottomEnd = if (alignLeft) 16.dp else 4.dp
                                        )
                                    )
                                    .background(
                                        if (alignLeft) SophisticatedSurface else Color.White
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = SophisticatedOutline,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = msg.messageText,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (alignLeft) Color.White else Color.Black
                                    )
                                    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                                    Text(
                                        text = timeFormat.format(Date(msg.timestamp)),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (alignLeft) MaterialTheme.colorScheme.secondary else Color.Black.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type actions row
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "ENTER TEXT SPACE...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = SophisticatedOutline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = SophisticatedSurface,
                            unfocusedContainerColor = SophisticatedSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                if (textInput.isNotBlank()) {
                                    viewModel.sendSimulatedMessage(selectedContact!!, textInput)
                                    textInput = ""
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// FEATURE APP: Zen Notes Notepad
// ==========================================
@Composable
fun NotesScreen(viewModel: FocusViewModel) {
    val notes by viewModel.notes.collectAsState()

    var expandedNoteId by remember { mutableStateOf<Long?>(null) }
    var inCreatingState by remember { mutableStateOf(false) }

    // Note inputs
    var titleInput by remember { mutableStateOf("") }
    var contentInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App header bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (inCreatingState) {
                        inCreatingState = false
                    } else {
                        viewModel.closeApp()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (inCreatingState) "NEW MEMO" else "ZEN NOTES",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
            }

            if (!inCreatingState) {
                IconButton(onClick = {
                    titleInput = ""
                    contentInput = ""
                    inCreatingState = true
                }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add memo",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (inCreatingState) {
            // Note creation form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SophisticatedSurface, RoundedCornerShape(16.dp))
                    .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("MEMO TITLE", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = SophisticatedOutline,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contentInput,
                    onValueChange = { contentInput = it },
                    label = { Text("JOT FOCUS THOUGHTS DOWN...", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = SophisticatedOutline,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 15
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.saveZenNote(0L, titleInput, contentInput)
                        inCreatingState = false
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Save Memo",
                        modifier = Modifier.padding(end = 6.dp).size(16.dp)
                    )
                    Text("SAVE TO INTENT MEMORY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            // Memos list
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your notepad is quiet.\nTap '+' above to record reflections.",
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes) { note ->
                        val isExpanded = expandedNoteId == note.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(16.dp))
                                .clickable { expandedNoteId = if (isExpanded) null else note.id },
                            color = SophisticatedSurface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = note.title.uppercase(Locale.US),
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        val timeFormat = remember { SimpleDateFormat("MMMM dd, HH:mm", Locale.getDefault()) }
                                        Text(
                                            text = timeFormat.format(Date(note.timestamp)).uppercase(Locale.getDefault()),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteZenNote(note) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete memo",
                                            tint = SophisticatedRoseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = note.content,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// FEATURE APP: Stats Display
// ==========================================
@Composable
fun StatsScreen(viewModel: FocusViewModel) {
    val sessions by viewModel.sessions.collectAsState()

    val totalCompletedCount = remember(sessions) {
        sessions.count { it.status == "COMPLETED" }
    }
    val totalInterruptedCount = remember(sessions) {
        sessions.count { it.status == "INTERRUPTED" }
    }
    val totalMins = remember(sessions) {
        sessions.filter { it.status == "COMPLETED" }.sumOf { it.durationMinutes }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = { viewModel.closeApp() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ZEN STATS",
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large focal total minutes counters (Nothing OS Widget)
        NothingWidget(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$totalMins MINS",
                    fontSize = 38.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "TOTAL DISCIPLINED TIME LOGGED",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Ratio summary board
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(16.dp)),
                color = SophisticatedSurface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "COMPLETED",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "$totalCompletedCount TIMES",
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(16.dp)),
                color = SophisticatedSurface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BYPASSED",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "$totalInterruptedCount TIMES",
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SophisticatedRoseRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Log list view
        Text(
            text = "• SESSIONS AUDIT MEMORY •",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded sessions.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions) { s ->
                    val sDate = remember(s.startTime) {
                        SimpleDateFormat("EEE, MMM dd • HH:mm", Locale.getDefault()).format(Date(s.startTime))
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(16.dp)),
                        color = SophisticatedSurface,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${s.durationMinutes} MINUTES",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = sDate.uppercase(Locale.getDefault()),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            val doneColor = if (s.status == "COMPLETED") Color.White else SophisticatedRoseRed
                            Text(
                                text = s.status.uppercase(Locale.US),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = doneColor,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// OUT-OF-LINE HELPER COMPOSABLES & FUNCTIONS
// ==========================================
@Composable
fun CustomTactileTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        // Hour
        IconButton(
            onClick = { onHourChange(if (hour == 0) 23 else hour - 1) },
            modifier = Modifier.size(30.dp)
        ) {
            Text("-", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = String.format("%02d", hour),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        IconButton(
            onClick = { onHourChange((hour + 1) % 24) },
            modifier = Modifier.size(30.dp)
        ) {
            Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Text(":", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(horizontal = 2.dp))

        // Minute
        IconButton(
            onClick = { onMinuteChange(if (minute == 0) 59 else minute - 1) },
            modifier = Modifier.size(30.dp)
        ) {
            Text("-", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = String.format("%02d", minute),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        IconButton(
            onClick = { onMinuteChange((minute + 1) % 60) },
            modifier = Modifier.size(30.dp)
        ) {
            Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

fun formatScheduleTime(timeStr: String): String {
    return try {
        val sdf24 = SimpleDateFormat("HH:mm", Locale.US)
        val date = sdf24.parse(timeStr)
        if (date != null) {
            SimpleDateFormat("h:mm a", Locale.US).format(date)
        } else {
            timeStr
        }
    } catch (_: Exception) {
        timeStr
    }
}


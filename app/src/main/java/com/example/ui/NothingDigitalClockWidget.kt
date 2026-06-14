package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

/**
 * NothingDigitalClockWidget Composable
 *
 * A high-fidelity, interactive digital clock widget designed with
 * the modern Nothing OS signature aesthetic (Monochromatic slate, red pixel accents).
 *
 * Replicates the React widget style:
 * 1. Cellular Status Bar (Zen Lock status, signal bar graphics, battery progress element)
 * 2. Format Selector capsule button (Live 12H/24H format toggling)
 * 3. Segment Display Console (Charcoal black screen, large clock font, blinking red colon)
 * 4. Uppercase date & DOT divider (SUN • JUN 14)
 * 5. Radial progression circle (Canvas-rendered minute-lap progress sweep tracking time in seconds)
 * 6. Hardcoded protocol active subtext
 * 7. Bottom physical-looking speaker slot hardware slot detail
 */
@Composable
fun NothingDigitalClockWidget(
    modifier: Modifier = Modifier,
    isFocusModeActive: Boolean = false
) {
    var currentTime by remember { mutableStateOf(Date()) }
    var is12h by remember { mutableStateOf(false) }
    var colonVisible by remember { mutableStateOf(true) }

    // Fast ticking ticker context: updates time state
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(250) // High-precision sampling
        }
    }

    // Secondary blink effect interval for the colon
    LaunchedEffect(Unit) {
        while (true) {
            colonVisible = !colonVisible
            delay(500)
        }
    }

    val cal = remember(currentTime) {
        Calendar.getInstance().apply { time = currentTime }
    }

    // Time calculations
    val hoursVal = cal.get(Calendar.HOUR_OF_DAY)
    val minutesVal = cal.get(Calendar.MINUTE)
    val secondsVal = cal.get(Calendar.SECOND)

    val hourStr = remember(hoursVal, is12h) {
        if (is12h) {
            val h = hoursVal % 12
            (if (h == 0) 12 else h).toString().padStart(2, '0')
        } else {
            hoursVal.toString().padStart(2, '0')
        }
    }

    val minuteStr = remember(minutesVal) {
        minutesVal.toString().padStart(2, '0')
    }

    val secondStr = remember(secondsVal) {
        secondsVal.toString().padStart(2, '0')
    }

    val amPmStr = remember(hoursVal) {
        if (hoursVal >= 12) "PM" else "AM"
    }

    // Date formatting (E.g., "SUN • JUN 14")
    val dayOfWeekName = remember(currentTime) {
        SimpleDateFormat("EEE", Locale.US).format(currentTime).uppercase(Locale.US)
    }
    val monthName = remember(currentTime) {
        SimpleDateFormat("MMM", Locale.US).format(currentTime).uppercase(Locale.US)
    }
    val dayOfMonthStr = remember(currentTime) {
        SimpleDateFormat("dd", Locale.US).format(currentTime)
    }

    // Progress fraction of the current minute
    val secondsProgress = remember(secondsVal) {
        secondsVal.toFloat() / 60.0f
    }

    Column(
        modifier = modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(SophisticatedSurface)
            .border(1.5.dp, SophisticatedOutline, RoundedCornerShape(32.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Cellular Status Bar (Retro Status Bar)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Symmetrical Cellular Signal Strength Dots
                Row(
                    modifier = Modifier.height(10.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp)
                ) {
                    Box(modifier = Modifier.size(2.dp, 3.dp).background(Color.White, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.size(2.dp, 5.dp).background(Color.White, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.size(2.dp, 7.dp).background(Color.White, RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.size(2.dp, 9.dp).background(Color.White, RoundedCornerShape(1.dp)))
                }
                Text(
                    text = if (isFocusModeActive) "ZEN_LOCK" else "NOTHING_OS",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedMuted,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "87%",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedMuted
                )
                // Battery Container outline
                Box(
                    modifier = Modifier
                        .size(16.dp, 8.dp)
                        .border(1.dp, SophisticatedMuted, RoundedCornerShape(1.5.dp))
                        .padding(1.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.87f)
                            .background(SophisticatedWhite, RoundedCornerShape(0.5.dp))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Format Switch Box Button (Interactive 12h/24h Toggle)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(SophisticatedBg)
                .border(1.dp, SophisticatedOutline, RoundedCornerShape(12.dp))
                .clickable { is12h = !is12h }
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (is12h) "FORMAT 24H" else "FORMAT 12H",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SophisticatedMuted,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Central Time Glass Console LCD Display Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SophisticatedBg)
                .border(1.dp, SophisticatedOutline, RoundedCornerShape(18.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour Characters
                    Text(
                        text = hourStr,
                        fontSize = 44.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        textAlign = TextAlign.Center
                    )

                    // Blinking dot colon divider (Iconic Nothing Red!)
                    val colonAlpha by animateFloatAsState(
                        targetValue = if (colonVisible) 1.0f else 0.2f,
                        animationSpec = tween(durationMillis = 200),
                        label = "colon_blink"
                    )
                    Text(
                        text = ":",
                        fontSize = 40.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SophisticatedRoseRed.copy(alpha = colonAlpha),
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    // Minute Characters
                    Text(
                        text = minuteStr,
                        fontSize = 44.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        textAlign = TextAlign.Center
                    )

                    // AM/PM representation context labels
                    if (is12h) {
                        Text(
                            text = amPmStr,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = SophisticatedRoseRed,
                            modifier = Modifier
                                .padding(start = 4.dp, bottom = 4.dp)
                                .align(Alignment.Top)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Seconds ticker with red dot sensor icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(SophisticatedRoseRed, CircleShape)
                    )
                    Text(
                        text = "SEC_$secondStr",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SophisticatedMuted,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Date and Day Segment Display Pills
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(SophisticatedBg)
                .border(1.dp, SophisticatedOutline, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = dayOfWeekName,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SophisticatedWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "•",
                    fontSize = 8.sp,
                    color = SophisticatedMuted,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$monthName $dayOfMonthStr",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = SophisticatedWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. Tactile Radial Progression Loop tracking seconds lap
        Box(
            modifier = Modifier.size(108.dp),
            contentAlignment = Alignment.Center
        ) {
            // Draw radial arcs & target indicator dot
            Canvas(modifier = Modifier.fillMaxSize()) {
                val circleRadius = size.width / 2f - 4.dp.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Background track
                drawCircle(
                    color = SophisticatedOutline,
                    radius = circleRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Sweep active seconds progress arc
                drawArc(
                    color = SophisticatedRoseRed,
                    startAngle = -90f,
                    sweepAngle = secondsProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 8.dp.toPx(),
                        size.height - 8.dp.toPx()
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Indicator head dot matching Nothing Os theme loop
                val angleRad = Math.toRadians((secondsProgress * 360f - 90f).toDouble())
                val headX = (centerOffset.x + circleRadius * Math.cos(angleRad)).toFloat()
                val headY = (centerOffset.y + circleRadius * Math.sin(angleRad)).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(headX, headY)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "MIN_LAP",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SophisticatedMuted,
                    letterSpacing = 1.sp
                )
                val percentProgress = (secondsProgress * 100).toInt()
                Text(
                    text = "$percentProgress%",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 6. Security protocol warning labels
        Text(
            text = "• COGNITIVE LOCK_PROTOCOL ACTIVE •",
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            color = SophisticatedRoseRed.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 7. Physical speaker slot hardware grill lines
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(SophisticatedOutline)
        )
    }
}

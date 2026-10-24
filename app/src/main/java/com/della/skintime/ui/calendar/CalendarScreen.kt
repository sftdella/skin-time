package com.della.skintime.ui.calendar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.della.skintime.R
import com.della.skintime.SkinTimeApp
import com.della.skintime.data.db.RoutineSession
import com.della.skintime.data.db.RoutineSessionDetail
import com.della.skintime.ui.theme.*
import com.della.skintime.utils.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(onNavigateToAnalytics: () -> Unit) {
    val context = LocalContext.current
    val db      = (context.applicationContext as SkinTimeApp).database
    val scope   = rememberCoroutineScope()
    val today   = StreakManager.today()

    var currentMonth    by remember { mutableStateOf(YearMonth.now()) }
    var completedDates  by remember { mutableStateOf(emptySet<String>()) }
    var allSessionDates by remember { mutableStateOf(emptyMap<String, Boolean>()) }

    var selectedDate       by remember { mutableStateOf(today) }
    var selectedSessions   by remember { mutableStateOf(emptyList<RoutineSession>()) }
    var sessionDetailsMap  by remember { mutableStateOf(emptyMap<Int, List<RoutineSessionDetail>>()) }
    var expandedSessionId  by remember { mutableStateOf<Int?>(null) }

    fun loadSessionsForDate(date: String) {
        scope.launch(Dispatchers.IO) {
            val sessions   = db.getSessionsByDate(date)
            val detailsMap = sessions.associate { s -> s.id to db.getSessionDetails(s.id) }
            withContext(Dispatchers.Main) {
                selectedDate      = date
                selectedSessions  = sessions
                sessionDetailsMap = detailsMap
                expandedSessionId = null
            }
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val done    = db.getCompletedSessionDates()
            val all     = db.getAllSessions()
            val dateMap = all.groupBy { it.date }.mapValues { (_, v) -> v.any { it.isCompleted } }
            withContext(Dispatchers.Main) {
                completedDates  = done
                allSessionDates = dateMap
            }
        }
        // Auto-load today's sessions
        loadSessionsForDate(today)
    }

    val dayFmt = DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale("id"))
    val displaySelectedDate = try {
        LocalDate.parse(selectedDate).format(dayFmt)
            .replaceFirstChar { it.uppercase() }
    } catch (e: Exception) { selectedDate }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PinkBackground, PinkSoft)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Calendar",
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PinkSoft)
                        .clickable { onNavigateToAnalytics() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("📊", fontSize = 18.sp)
                }
            }

            // Calendar card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                FullCalendar(
                    yearMonth      = currentMonth,
                    completedDates = completedDates,
                    allSessionDates = allSessionDates,
                    todayStr       = today,
                    selectedDate   = selectedDate,
                    onPrevMonth    = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth    = { currentMonth = currentMonth.plusMonths(1) },
                    onDateClick    = { loadSessionsForDate(it) }
                )
            }

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CalLegendItem(CalToday, "Hari ini")
                CalLegendItem(CalCompleted, "Selesai", showCheck = true)
                CalLegendItem(PinkLight, "Ada session")
                CalLegendItem(CalEmpty, "Kosong")
            }

            // ── Session list untuk tanggal yang dipilih ─────────────────────
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    displaySelectedDate,
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PinkDark,
                    modifier = Modifier.weight(1f)
                )
                if (selectedSessions.isNotEmpty()) {
                    Text(
                        "${selectedSessions.size} Session",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (selectedSessions.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌸", fontSize = 32.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Belum ada routine pada hari ini",
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                selectedSessions.forEach { session ->
                    val details    = sessionDetailsMap[session.id] ?: emptyList()
                    val isExpanded = expandedSessionId == session.id

                    SessionCard(
                        session    = session,
                        details    = details,
                        isExpanded = isExpanded,
                        onToggle   = {
                            expandedSessionId = if (isExpanded) null else session.id
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Lihat Catatan button
                Button(
                    onClick = { onNavigateToAnalytics() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text("📋", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Lihat Analytics",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = White
                    )
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable
private fun SessionCard(
    session: RoutineSession,
    details: List<RoutineSessionDetail>,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (session.isCompleted) PinkSoft else White
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(sessionTypeIcon(session.sessionType)), null,
                    modifier = Modifier.size(42.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        sessionTypeDisplayName(session.sessionType),
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (session.isCompleted) PinkDark else TextPrimary
                    )
                    Text(
                        "${session.time}  •  ${details.size} Step",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (session.isCompleted) PinkPrimary else CalEmpty.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (session.isCompleted) "✓ Selesai" else "○ Belum",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (session.isCompleted) White else TextSecondary
                    )
                }
            }

            // Expandable step list
            if (isExpanded && details.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = PinkLight, thickness = 1.dp)
                Spacer(Modifier.height(8.dp))
                details.forEach { detail ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (detail.isDone) "✓" else "○",
                            fontSize = 13.sp,
                            color = if (detail.isDone) PinkPrimary else CalEmpty,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            detail.stepName,
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (detail.isDone) TextPrimary else TextSecondary
                        )
                        if (detail.isOptional) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "(Opsional)",
                                fontFamily = PlusJakartaFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (details.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isExpanded) "▲ Tutup" else "▼ Lihat step",
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = PinkPrimary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun FullCalendar(
    yearMonth: YearMonth,
    completedDates: Set<String>,
    allSessionDates: Map<String, Boolean>,
    todayStr: String,
    selectedDate: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateClick: (String) -> Unit
) {
    val formatter      = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val daysInMonth    = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7
    val dayLabels      = listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab")

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevMonth) {
                Text("◀", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 16.sp)
            }
            Text(
                yearMonth.month.getDisplayName(TextStyle.FULL, Locale("id")) + " " + yearMonth.year,
                fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            IconButton(onClick = onNextMonth) {
                Text("▶", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, color = PinkPrimary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        val rows = ((firstDayOfWeek + daysInMonth) + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = row * 7 + col - firstDayOfWeek + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dateStr    = yearMonth.atDay(dayNum).format(formatter)
                        val isToday    = dateStr == todayStr
                        val isDone     = completedDates.contains(dateStr)
                        val hasSession = allSessionDates.containsKey(dateStr)
                        val isSelected = dateStr == selectedDate

                        val bgColor = when {
                            isDone     -> CalCompleted
                            isToday    -> CalToday
                            hasSession -> PinkLight
                            else       -> androidx.compose.ui.graphics.Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(
                                    if (isSelected && !isDone && !isToday)
                                        Modifier.background(PinkLight)
                                    else Modifier
                                )
                                .clickable { onDateClick(dateStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Text("✓", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = White)
                            } else {
                                Text(
                                    dayNum.toString(),
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = when {
                                        isToday    -> PinkDark
                                        isSelected -> PinkPrimary
                                        else       -> TextSecondary
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalLegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    showCheck: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(14.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            if (showCheck) Text("✓", fontSize = 8.sp, color = White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = TextSecondary)
    }
}

fun sessionTypeEmoji(type: String): String = when (type) {
    "Morning"            -> "☀️"
    "Night"              -> "🌙"
    "Aktivitas Tambahan" -> "✨"
    else                 -> "💄"
}

fun sessionTypeIcon(type: String): Int = when (type) {
    "Morning"            -> R.drawable.ic_morning
    "Night"              -> R.drawable.ic_night
    "Aktivitas Tambahan" -> R.drawable.ic_aktivitas_tambahan
    else                 -> R.drawable.ic_ceklis_routine
}

fun sessionTypeDisplayName(type: String): String = when (type) {
    "Morning" -> "Morning Routine"
    "Night"   -> "Night Routine"
    else      -> type
}

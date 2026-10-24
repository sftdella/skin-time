package com.della.skintime.ui.routine

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.della.skintime.R
import com.della.skintime.SkinTimeApp
import com.della.skintime.data.db.ActivitySchedule
import com.della.skintime.data.db.ChecklistStep
import com.della.skintime.data.db.RoutineSession
import com.della.skintime.data.db.RoutineSessionDetail
import com.della.skintime.ui.calendar.sessionTypeEmoji
import com.della.skintime.ui.getStepIcon
import com.della.skintime.ui.theme.*
import com.della.skintime.utils.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineScreen(
    onManageChecklist: () -> Unit,
    onManageActivities: () -> Unit
) {
    val context = LocalContext.current
    val db      = (context.applicationContext as SkinTimeApp).database
    val scope   = rememberCoroutineScope()
    val today   = StreakManager.today()
    val todayDow = LocalDate.now().dayOfWeek.value

    var allSteps        by remember { mutableStateOf(emptyList<ChecklistStep>()) }
    var todayActivities by remember { mutableStateOf(emptyList<ActivitySchedule>()) }
    var activeSession   by remember { mutableStateOf<RoutineSession?>(null) }
    var activeDetails   by remember { mutableStateOf(emptyList<RoutineSessionDetail>()) }
    var todaySessions   by remember { mutableStateOf(emptyList<RoutineSession>()) }
    var completedDates  by remember { mutableStateOf(emptySet<String>()) }
    var streak          by remember { mutableIntStateOf(0) }
    var completedDetailsMap   by remember { mutableStateOf(emptyMap<Int, List<RoutineSessionDetail>>()) }
    var editingCompletedId    by remember { mutableStateOf<Int?>(null) }
    var showAddSheet    by remember { mutableStateOf(false) }
    var showManageSheet by remember { mutableStateOf(false) }
    var showAchievement by remember { mutableStateOf(false) }
    var snackMessage    by remember { mutableStateOf<String?>(null) }
    var refreshKey      by remember { mutableIntStateOf(0) }
    var currentMonth    by remember { mutableStateOf(YearMonth.now()) }
    var historyDate     by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            val steps     = db.getAllSteps()
            val acts      = db.getActivitiesForDay(todayDow)
            val active    = db.getActiveSession(today)
            val details   = if (active != null) db.getSessionDetails(active.id) else emptyList()
            val todaySess = db.getSessionsByDate(today)
            val done      = db.getCompletedSessionDates()
            val s         = StreakManager.calculateStreak(done)
            // Load details untuk semua completed session (untuk fitur edit)
            val completedMap = todaySess
                .filter { it.isCompleted }
                .associate { sess -> sess.id to db.getSessionDetails(sess.id) }
            withContext(Dispatchers.Main) {
                allSteps            = steps
                todayActivities     = acts
                activeSession       = active
                activeDetails       = details
                todaySessions       = todaySess
                completedDates      = done
                streak              = s
                completedDetailsMap = completedMap
            }
        }
    }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    // Selesai hanya jika semua step wajib (non-opsional) sudah diceklis
    val mandatoryDetails = activeDetails.filter { !it.isOptional }
    val isReadyToComplete = activeSession != null &&
        (mandatoryDetails.isEmpty() || mandatoryDetails.all { it.isDone })

    fun toggleDetail(detail: RoutineSessionDetail) {
        val newDone = !detail.isDone
        scope.launch(Dispatchers.IO) {
            db.updateDetailDone(detail.id, newDone)
            val updated = db.getSessionDetails(activeSession!!.id)
            withContext(Dispatchers.Main) { activeDetails = updated }
        }
    }

    fun toggleCompletedDetail(sessionId: Int, detail: RoutineSessionDetail) {
        val newDone = !detail.isDone
        scope.launch(Dispatchers.IO) {
            db.updateDetailDone(detail.id, newDone)
            val updated = db.getSessionDetails(sessionId)
            withContext(Dispatchers.Main) {
                completedDetailsMap = completedDetailsMap + (sessionId to updated)
            }
        }
    }

    fun addSession(type: String) {
        if (activeSession != null) {
            snackMessage = "Selesaikan session yang aktif terlebih dahulu"
            return
        }
        // Tiap tipe hanya boleh 1x per hari
        if (todaySessions.any { it.sessionType == type }) {
            snackMessage = "$type sudah ditambahkan hari ini"
            return
        }
        scope.launch(Dispatchers.IO) {
            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            val sessionId = db.createSession(RoutineSession(date = today, time = time, sessionType = type))
            val steps: List<ChecklistStep>
            val activities: List<ActivitySchedule>
            when (type) {
                "Aktivitas Tambahan" -> {
                    steps = emptyList()
                    activities = todayActivities
                }
                else -> {
                    steps = db.getAllSteps()
                    activities = emptyList()
                }
            }
            steps.forEach { step ->
                db.insertSessionDetail(
                    RoutineSessionDetail(
                        sessionId  = sessionId.toInt(),
                        stepName   = step.name,
                        isOptional = step.isOptional
                    )
                )
            }
            activities.forEach { act ->
                db.insertSessionDetail(
                    RoutineSessionDetail(
                        sessionId = sessionId.toInt(),
                        stepName  = act.name
                    )
                )
            }
            val newActive  = db.getActiveSession(today)
            val newDetails = if (newActive != null) db.getSessionDetails(newActive.id) else emptyList()
            val todaySess  = db.getSessionsByDate(today)
            withContext(Dispatchers.Main) {
                activeSession = newActive
                activeDetails = newDetails
                todaySessions = todaySess
            }
        }
    }

    fun completeSession() {
        val session = activeSession ?: return
        scope.launch(Dispatchers.IO) {
            db.completeSession(session.id)
            val done = db.getCompletedSessionDates()
            val s    = StreakManager.calculateStreak(done)
            val todaySess = db.getSessionsByDate(today)
            withContext(Dispatchers.Main) {
                activeSession = null
                activeDetails = emptyList()
                todaySessions = todaySess
                completedDates = done
                streak = s
                if (s > 0 && s % 7 == 0) showAchievement = true
            }
        }
    }

    fun cancelSession() {
        val session = activeSession ?: return
        scope.launch(Dispatchers.IO) {
            db.deleteSession(session.id)
            val todaySess = db.getSessionsByDate(today)
            withContext(Dispatchers.Main) {
                activeSession = null
                activeDetails = emptyList()
                todaySessions = todaySess
            }
        }
    }

    val completedDatesMap = remember(todaySessions, completedDates) {
        completedDates.associateWith { true }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        "Routine",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    // Kelola (⚙)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PinkSoft)
                            .clickable { showManageSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    // Tambah Session (+)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PinkPrimary)
                            .clickable { showAddSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", fontSize = 22.sp, color = White, fontWeight = FontWeight.Bold)
                    }
                }

                // Streak Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painterResource(R.drawable.ic_streak_fire), null,
                            modifier = Modifier.size(52.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🔥 $streak Hari Streak",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = PinkPrimary
                            )
                            Text(
                                if (streak > 0) "Jangan berhenti, kamu hebat! 🔥"
                                else "Mulai rutinitas skincare hari ini! ✨",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Image(
                            painterResource(R.drawable.ic_moon_routine), null,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Mini Calendar
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    MiniCalendar(
                        yearMonth      = currentMonth,
                        completedDates = completedDatesMap,
                        todayStr       = today,
                        onPrevMonth    = { currentMonth = currentMonth.minusMonths(1) },
                        onNextMonth    = { currentMonth = currentMonth.plusMonths(1) },
                        onDateClick    = { dateStr -> historyDate = dateStr }
                    )
                }

                // Legend
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LegendItem(CalToday, "Hari ini")
                    LegendItem(CalCompleted, "Selesai", showCheck = true)
                    LegendItem(CalEmpty, "Belum")
                }

                Spacer(Modifier.height(8.dp))

                // Today's completed sessions
                val completedToday = todaySessions.filter { it.isCompleted }
                if (completedToday.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "✓ Selesai Hari Ini",
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PinkDark
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    completedToday.forEach { session ->
                        CompletedSessionCard(
                            session    = session,
                            details    = completedDetailsMap[session.id] ?: emptyList(),
                            isEditing  = editingCompletedId == session.id,
                            onEditToggle = {
                                editingCompletedId =
                                    if (editingCompletedId == session.id) null else session.id
                            },
                            onToggleDetail = { detail ->
                                toggleCompletedDetail(session.id, detail)
                            }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Active Session
                if (activeSession != null) {
                    ActiveSessionSection(
                        session       = activeSession!!,
                        details       = activeDetails,
                        isReady       = isReadyToComplete,
                        onToggle      = { toggleDetail(it) },
                        onComplete    = { completeSession() },
                        onCancel      = { cancelSession() }
                    )
                } else if (completedToday.isEmpty()) {
                    // Empty state
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
                            Text("🌸", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Belum ada routine hari ini",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Ketuk + untuk mulai session skincare kamu!",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    // Has completed sessions but no active → prompt to add more
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PinkSoft),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painterResource(R.drawable.ic_great_job), null,
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Great Job! 💜",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = PinkDark
                                )
                                Text(
                                    "Kamu sudah selesai ${completedToday.size} session hari ini!\nMau tambah lagi? Ketuk +",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Full-width Add button at bottom
                Button(
                    onClick = { showAddSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary)
                ) {
                    Text(
                        "✚  Tambah Sesi / Step / Aktivitas",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = White
                    )
                }

                Spacer(Modifier.height(90.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Add Session Bottom Sheet
    if (showAddSheet) {
        AddSessionSheet(
            hasActiveSession = activeSession != null,
            usedTypes        = todaySessions.map { it.sessionType }.toSet(),
            onSelect = { type ->
                showAddSheet = false
                addSession(type)
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // Manage Bottom Sheet
    if (showManageSheet) {
        ManageSheet(
            onManageChecklist = {
                showManageSheet = false
                onManageChecklist()
            },
            onManageActivities = {
                showManageSheet = false
                onManageActivities()
            },
            onDismiss = { showManageSheet = false }
        )
    }

    if (showAchievement) AchievementDialog(streak, onDismiss = { showAchievement = false })

    // History Bottom Sheet
    historyDate?.let { date ->
        val db2 = (LocalContext.current.applicationContext as SkinTimeApp).database
        HistorySessionSheet(
            date = date,
            db   = db2,
            onDismiss = { historyDate = null }
        )
    }
}

@Composable
private fun CompletedSessionCard(
    session: RoutineSession,
    details: List<RoutineSessionDetail> = emptyList(),
    isEditing: Boolean = false,
    onEditToggle: () -> Unit = {},
    onToggleDetail: (RoutineSessionDetail) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PinkSoft),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sessionTypeEmoji(session.sessionType), fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    session.sessionType,
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PinkDark,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    session.time,
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.width(8.dp))
                Text("✓", color = PinkPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = onEditToggle,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (isEditing) "Tutup" else "Edit ✏",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = PinkPrimary
                    )
                }
            }

            if (isEditing && details.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = PinkPrimary.copy(alpha = 0.15f)
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    details.forEach { detail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = detail.isDone,
                                onCheckedChange = { onToggleDetail(detail) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PinkPrimary,
                                    uncheckedColor = TextSecondary
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(getStepIcon(detail.stepName)),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (detail.isDone) PinkPrimary else TextSecondary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                detail.stepName,
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (detail.isDone) PinkDark else TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            if (detail.isOptional) {
                                Text(
                                    "opsional",
                                    fontFamily = PlusJakartaFamily,
                                    fontSize = 10.sp,
                                    color = TextSecondary.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ActiveSessionSection(
    session: RoutineSession,
    details: List<RoutineSessionDetail>,
    isReady: Boolean,
    onToggle: (RoutineSessionDetail) -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    // Session header
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(sessionTypeEmoji(session.sessionType), fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            "${session.sessionType} • ${session.time}",
            fontFamily = PlusJakartaFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(CalEmpty.copy(alpha = 0.5f))
                .clickable { onCancel() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                "Batalkan",
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }

    Spacer(Modifier.height(6.dp))

    if (details.isEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Text(
                "Tidak ada step untuk session ini.",
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }
    } else {
        details.forEach { detail ->
            SessionDetailRow(detail = detail, onToggle = { onToggle(detail) })
        }
    }

    Spacer(Modifier.height(12.dp))

    // Complete button
    if (isReady) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PinkSoft)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.ic_great_job), null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Great Job! 💜",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = PinkDark
                    )
                    Text(
                        "Kamu hebat sudah merawat dirimu ✨",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onComplete,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Selesai",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = White
                    )
                }
            }
        }
    } else {
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(disabledContainerColor = PinkLight)
        ) {
            Text(
                "Selesai Routine",
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun SessionDetailRow(detail: RoutineSessionDetail, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (detail.isDone) PinkSoft else White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(getStepIcon(detail.stepName)), null,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    detail.stepName,
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (detail.isDone) PinkDark else TextPrimary
                )
                if (detail.isOptional) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "(Opsional)",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
            Checkbox(
                checked = detail.isDone,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = PinkPrimary,
                    uncheckedColor = CalEmpty
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSessionSheet(
    hasActiveSession: Boolean,
    usedTypes: Set<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Tambah Session",
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = TextPrimary
            )

            if (hasActiveSession) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PinkLight)
                ) {
                    Text(
                        "⚠️ Selesaikan session yang aktif terlebih dahulu.",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = PinkDark,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            listOf(
                Triple("☀️", "Morning Routine",    "Morning"),
                Triple("🌙", "Night Routine",      "Night"),
                Triple("✨", "Aktivitas Tambahan", "Aktivitas Tambahan"),
                Triple("💄", "Custom Routine",     "Custom")
            ).forEach { (emoji, label, type) ->
                val alreadyUsed = type in usedTypes
                val isDisabled  = hasActiveSession || alreadyUsed

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when {
                                alreadyUsed      -> CalEmpty.copy(alpha = 0.25f)
                                hasActiveSession -> CalEmpty.copy(alpha = 0.25f)
                                else             -> PinkSoft
                            }
                        )
                        .clickable(enabled = !isDisabled) { onSelect(type) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        emoji,
                        fontSize = 22.sp,
                        modifier = Modifier.alpha(if (isDisabled) 0.4f else 1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDisabled) TextSecondary else TextPrimary
                        )
                        if (alreadyUsed) {
                            Text(
                                "✓ Sudah ditambahkan hari ini",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = PinkPrimary
                            )
                        }
                    }
                    if (alreadyUsed) {
                        Text("✓", color = PinkPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageSheet(
    onManageChecklist: () -> Unit,
    onManageActivities: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Kelola",
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PinkSoft)
                    .clickable { onManageChecklist() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(R.drawable.ic_ceklis_routine), null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Kelola Step",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        "Tambah, ubah, atau hapus step routine",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PinkSoft)
                    .clickable { onManageActivities() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painterResource(R.drawable.ic_aktivitas_tambahan), null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Kelola Aktivitas",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        "Atur aktivitas tambahan mingguan kamu",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySessionSheet(
    date: String,
    db: com.della.skintime.data.db.AppDatabase,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id"))
    val displayDate = try { LocalDate.parse(date).format(formatter) } catch (e: Exception) { date }

    var sessions by remember { mutableStateOf(emptyList<RoutineSession>()) }
    var expanded by remember { mutableStateOf<Int?>(null) }
    var detailsMap by remember { mutableStateOf(emptyMap<Int, List<RoutineSessionDetail>>()) }

    LaunchedEffect(date) {
        withContext(Dispatchers.IO) {
            val s = db.getSessionsByDate(date)
            withContext(Dispatchers.Main) { sessions = s }
        }
    }

    fun toggleExpand(sessionId: Int) {
        scope.launch(Dispatchers.IO) {
            val details = db.getSessionDetails(sessionId)
            withContext(Dispatchers.Main) {
                detailsMap = detailsMap + (sessionId to details)
                expanded = if (expanded == sessionId) null else sessionId
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = White) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                displayDate,
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            if (sessions.isEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Belum ada session untuk tanggal ini.",
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            } else {
                sessions.forEach { session ->
                    val isExpanded = expanded == session.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { toggleExpand(session.id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (session.isCompleted) PinkSoft else White
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(sessionTypeEmoji(session.sessionType), fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        session.sessionType,
                                        fontFamily = PlusJakartaFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (session.isCompleted) PinkDark else TextPrimary
                                    )
                                    Text(
                                        session.time,
                                        fontFamily = PlusJakartaFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    if (session.isCompleted) "✓" else "○",
                                    color = if (session.isCompleted) PinkPrimary else CalEmpty,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            if (isExpanded) {
                                val details = detailsMap[session.id] ?: emptyList()
                                if (details.isNotEmpty()) {
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tutup", color = White, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MiniCalendar(
    yearMonth: YearMonth,
    completedDates: Map<String, Boolean>,
    todayStr: String,
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
                Text("<", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 18.sp)
            }
            Text(
                yearMonth.month.getDisplayName(TextStyle.FULL, Locale("id")) + " " + yearMonth.year,
                fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = TextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.Center
            )
            IconButton(onClick = onNextMonth) {
                Text(">", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, color = PinkPrimary, fontSize = 18.sp)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            dayLabels.forEach { label ->
                Text(
                    label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
                    fontSize = 11.sp, color = PinkPrimary
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val rows = ((firstDayOfWeek + daysInMonth) + 6) / 7
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = row * 7 + col - firstDayOfWeek + 1
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dateStr = yearMonth.atDay(dayNum).format(formatter)
                        val isToday = dateStr == todayStr
                        val isDone  = completedDates[dateStr] == true
                        val bgColor = when {
                            isDone  -> CalCompleted
                            isToday -> CalToday
                            else    -> androidx.compose.ui.graphics.Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(bgColor)
                                .clickable { onDateClick(dateStr) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Text("✓", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = White)
                            } else {
                                Text(
                                    dayNum.toString(),
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isToday) PinkDark else TextSecondary
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
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String, showCheck: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            if (showCheck) Text("✓", fontSize = 8.sp, color = White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(4.dp))
        Text(label, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun AchievementDialog(streak: Int, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎉", fontSize = 48.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Achievement Unlocked!",
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp, color = PinkDark, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "$streak Days Consistent Routine",
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, color = PinkPrimary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Image(painterResource(R.drawable.ic_great_job), null, modifier = Modifier.size(80.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "Kamu berhasil menjaga rutinitas skincare\nselama $streak hari berturut-turut ✨",
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                    fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Terima Kasih 💕", color = White, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

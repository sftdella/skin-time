package com.della.skintime.ui.analytics

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.della.skintime.ui.getStepIcon
import com.della.skintime.ui.theme.*
import com.della.skintime.utils.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db      = (context.applicationContext as SkinTimeApp).database

    var streak         by remember { mutableIntStateOf(0) }
    var completedDates by remember { mutableStateOf(emptySet<String>()) }
    var sessionByDate  by remember { mutableStateOf(emptyMap<String, Int>()) }
    var topSteps       by remember { mutableStateOf(emptyList<Pair<String, Int>>()) }
    var totalSessions  by remember { mutableIntStateOf(0) }
    var totalMorning   by remember { mutableIntStateOf(0) }
    var totalNight     by remember { mutableIntStateOf(0) }
    var totalOther     by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val dates   = db.getCompletedSessionDates()
            val s       = StreakManager.calculateStreak(dates)
            val byDate  = db.getSessionCountsByDate()
            val steps   = db.getMostUsedSteps(5)
            val all     = db.getAllSessions()
            val morning = all.count { it.sessionType == "Morning" && it.isCompleted }
            val night   = all.count { it.sessionType == "Night" && it.isCompleted }
            val other   = all.count { it.sessionType !in listOf("Morning", "Night") && it.isCompleted }
            withContext(Dispatchers.Main) {
                completedDates = dates
                streak         = s
                sessionByDate  = byDate
                topSteps       = steps
                totalSessions  = all.count { it.isCompleted }
                totalMorning   = morning
                totalNight     = night
                totalOther     = other
            }
        }
    }

    val skinScore = when {
        streak >= 30 -> 95
        streak >= 21 -> 88
        streak >= 14 -> 78
        streak >= 7  -> 65
        streak >= 3  -> 50
        streak >= 1  -> 35
        else         -> 20
    }

    val last7Dates = (6 downTo 0).map { i ->
        val d = LocalDate.now().minusDays(i.toLong())
        val s = StreakManager.formatDate(d)
        s to (sessionByDate[s] ?: 0)
    }
    val maxCount = (last7Dates.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

    val thisMonth  = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    val daysActive = completedDates.count { it.startsWith(thisMonth) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analytics",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 22.sp, color = PinkDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PinkBackground)
            )
        },
        containerColor = PinkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Brush.verticalGradient(listOf(PinkBackground, PinkSoft)))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Overview cards ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OverviewCard(Modifier.weight(1f), "🔥", "$streak", "Streak")
                OverviewCard(Modifier.weight(1f), "📅", "$daysActive", "Hari Aktif")
                OverviewCard(Modifier.weight(1f), "✅", "$totalSessions", "Total Sesi")
            }

            Spacer(Modifier.height(16.dp))

            // ── Skin Score ───────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Skin Score",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PinkDark,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { skinScore / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = PinkPrimary,
                            trackColor = PinkLight,
                            strokeWidth = 12.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$skinScore",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 36.sp,
                                color = PinkDark
                            )
                            Text(
                                "/100",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            skinScore >= 90 -> "Excellent! Kulitmu terawat banget 🌟"
                            skinScore >= 75 -> "Bagus! Terus pertahankan rutinmu 💪"
                            skinScore >= 50 -> "Lumayan! Tingkatkan konsistensimu ✨"
                            skinScore >= 30 -> "Mulai bagus! Jangan lupa rutin ya 🌸"
                            else            -> "Yuk mulai rutinmu hari ini! 💕"
                        },
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Grafik Mingguan ──────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Grafik Mingguan",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PinkDark
                    )
                    Text(
                        "Sesi selesai per hari (7 hari terakhir)",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                    )
                    val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale("id"))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        last7Dates.forEach { (dateStr, count) ->
                            val date    = try { LocalDate.parse(dateStr) } catch (e: Exception) { return@forEach }
                            val isToday = dateStr == StreakManager.today()
                            val pct     = if (count == 0) 0f else count.toFloat() / maxCount.toFloat()
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (count > 0) {
                                    Text(
                                        "$count",
                                        fontFamily = PlusJakartaFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = PinkDark
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height((pct * 100).dp.coerceAtLeast(8.dp))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            when {
                                                count == 0 -> PinkLight
                                                isToday    -> PinkDark
                                                else       -> PinkPrimary
                                            }
                                        )
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    date.format(dayFmt).take(1),
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isToday) PinkDark else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Checklist Paling Sering ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Checklist Paling Sering",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PinkDark
                    )
                    Text(
                        "Step skincare yang paling sering kamu lakukan",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )
                    if (topSteps.isEmpty()) {
                        Text(
                            "Belum ada data. Yuk mulai rutinmu! 🌸",
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    } else {
                        val maxStepCount = topSteps.maxOf { it.second }.coerceAtLeast(1)
                        topSteps.forEachIndexed { idx, (name, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painterResource(getStepIcon(name)), null,
                                    modifier = Modifier.size(28.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            name,
                                            fontFamily = PlusJakartaFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "$count×",
                                            fontFamily = PlusJakartaFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PinkPrimary
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PinkLight)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(count.toFloat() / maxStepCount)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (idx == 0) PinkDark
                                                    else PinkPrimary.copy(alpha = 0.7f + (0.05f * (4 - idx)))
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Achievement ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Achievement",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PinkDark
                    )
                    Spacer(Modifier.height(12.dp))
                    val achievements = listOf(
                        AchievementDef(R.drawable.ic_achievement_7_days,        "7 Hari Berturut-turut", "Konsisten 1 minggu",    streak >= 7),
                        AchievementDef(R.drawable.ic_achievement_30_days,       "30 Hari Berturut-turut","Dedikasi luar biasa!",  streak >= 30),
                        AchievementDef(R.drawable.ic_achievement_100_checklist, "100 Checklist",         "100 sesi selesai",      totalSessions >= 100),
                        AchievementDef(R.drawable.ic_achievement_morning_hero,  "Morning Hero",          "Rajin pagi hari",       totalMorning >= 10),
                        AchievementDef(R.drawable.ic_achievement_night_owl,     "Night Owl",             "Rajin malam hari",      totalNight >= 10),
                        AchievementDef(R.drawable.ic_achievement_mask_queen,    "Mask Queen",            "Sering pakai masker",   totalOther >= 5),
                        AchievementDef(R.drawable.ic_achievement_hydration,     "Hydration Queen",       "Rutin 2 minggu penuh",  streak >= 14)
                    )
                    achievements.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { ach ->
                                AchievementItem(Modifier.weight(1f), ach)
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Statistik ────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Statistik",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PinkDark
                    )
                    Spacer(Modifier.height(12.dp))
                    StatRow("☀️", "Morning Routine",  "$totalMorning sesi")
                    HorizontalDivider(color = PinkLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                    StatRow("🌙", "Night Routine",    "$totalNight sesi")
                    HorizontalDivider(color = PinkLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                    StatRow("✨", "Aktivitas Lainnya","$totalOther sesi")
                    HorizontalDivider(color = PinkLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                    StatRow("📅", "Total Hari Aktif", "${completedDates.size} hari")
                    HorizontalDivider(color = PinkLight, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                    StatRow("🔥", "Streak Sekarang",  "$streak hari")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private data class AchievementDef(val icon: Int, val title: String, val desc: String, val unlocked: Boolean)

@Composable
private fun AchievementItem(modifier: Modifier, ach: AchievementDef) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (ach.unlocked) PinkSoft else PinkLight.copy(alpha = 0.5f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(ach.icon), null,
                modifier = Modifier.size(48.dp),
                alpha = if (ach.unlocked) 1f else 0.35f,
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(6.dp))
            Text(
                ach.title,
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (ach.unlocked) PinkDark else TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
            Text(
                ach.desc,
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                if (ach.unlocked) "✓ Tercapai!" else "🔒 Terkunci",
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (ach.unlocked) PinkPrimary else TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun OverviewCard(modifier: Modifier, emoji: String, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = PinkDark
            )
            Text(
                label,
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatRow(emoji: String, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 18.sp, modifier = Modifier.width(28.dp))
        Text(
            label,
            fontFamily = PlusJakartaFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontFamily = PlusJakartaFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = PinkPrimary
        )
    }
}

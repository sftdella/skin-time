package com.della.skintime.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.della.skintime.R
import com.della.skintime.SkinTimeApp
import com.della.skintime.data.db.RoutineSession
import com.della.skintime.ui.theme.*
import com.della.skintime.utils.ReminderManager
import com.della.skintime.utils.StreakManager
import com.della.skintime.utils.WeatherData
import com.della.skintime.utils.WeatherManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

private val skincare_quotes = listOf(
    "Self care is not a luxury,\nit's a necessity.",
    "Your skin is your largest organ.\nTake care of it. ✨",
    "Glowing skin is always in. 🌟",
    "Consistency is the key to great skin.",
    "Invest in your skin. It represents you\nfor a very long time.",
    "Take care of your skin today,\nso it takes care of you tomorrow.",
    "Skincare is self love. 💕",
    "Be gentle with your skin.\nOur skin is delicate. 🌸"
)

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val db      = (context.applicationContext as SkinTimeApp).database
    val scope   = rememberCoroutineScope()
    val today   = StreakManager.today()

    var reminderHour    by remember { mutableIntStateOf(ReminderManager.getReminderHour(context)) }
    var reminderMinute  by remember { mutableIntStateOf(ReminderManager.getReminderMinute(context)) }
    var reminderEnabled by remember { mutableStateOf(ReminderManager.isReminderEnabled(context)) }
    var showTimePicker  by remember { mutableStateOf(false) }

    var todaySessions  by remember { mutableStateOf(emptyList<RoutineSession>()) }
    var completedDates by remember { mutableStateOf(emptySet<String>()) }
    var streak         by remember { mutableIntStateOf(0) }
    var weather        by remember { mutableStateOf<WeatherData?>(null) }

    // "loading" | "no_permission" | "no_location" | "error" | "loaded"
    var weatherState   by remember { mutableStateOf("loading") }

    val dayOfYear  = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val todayQuote = skincare_quotes[dayOfYear % skincare_quotes.size]

    // DB data
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val sessions = db.getSessionsByDate(today)
            val done     = db.getCompletedSessionDates()
            val s        = StreakManager.calculateStreak(done)
            withContext(Dispatchers.Main) {
                todaySessions  = sessions
                completedDates = done
                streak         = s
            }
        }
    }

    // ── Location helpers ────────────────────────────────────────────────────

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun fetchWeatherWithGps() {
        scope.launch(Dispatchers.IO) {
            try {
                val lm  = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                if (loc == null) {
                    withContext(Dispatchers.Main) { weatherState = "no_location" }
                    return@launch
                }

                val w = WeatherManager.fetchWeather(loc.latitude, loc.longitude)
                withContext(Dispatchers.Main) {
                    if (w != null) {
                        weather      = w
                        weatherState = "loaded"
                    } else {
                        weatherState = "error"
                    }
                }
            } catch (_: SecurityException) {
                withContext(Dispatchers.Main) { weatherState = "no_permission" }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { weatherState = "error" }
            }
        }
    }

    // Location permission launcher
    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            weatherState = "loading"
            fetchWeatherWithGps()
        } else {
            weatherState = "no_permission"
        }
    }

    // Notification permission launcher
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            reminderEnabled = true
            ReminderManager.setReminderEnabled(context, true)
        }
    }

    // On start: cek permission → fetch atau request
    LaunchedEffect(Unit) {
        if (hasLocationPermission()) {
            fetchWeatherWithGps()
        } else {
            locationPermLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ── UI ──────────────────────────────────────────────────────────────────

    val hour = LocalTime.now().hour
    val (greeting, subGreeting) = when (hour) {
        in 5..10  -> "Good Morning! ☀️"  to "Selamat pagi, Della 🌸"
        in 11..14 -> "Good Afternoon! 🌤️" to "Selamat siang, Della ✨"
        in 15..17 -> "Good Evening! 🌇"  to "Selamat sore, Della 💆"
        else      -> "Good Night! 🌙"    to "Selamat malam, Della ⭐"
    }

    val morningDone = todaySessions.any { it.sessionType == "Morning" && it.isCompleted }
    val nightDone   = todaySessions.any { it.sessionType == "Night" && it.isCompleted }
    val progressPct = when {
        morningDone && nightDone -> 100
        morningDone || nightDone -> 50
        else -> 0
    }

    val last7 = (6 downTo 0).map { i ->
        val date    = LocalDate.now().minusDays(i.toLong())
        val dateStr = StreakManager.formatDate(date)
        dateStr to completedDates.contains(dateStr)
    }

    val latestAchievement = listOf(100, 60, 30, 21, 14, 7, 3).firstOrNull { streak >= it }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PinkBackground, PinkSoft, PinkLight)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // Logo
            Text(
                "Skin Time",
                fontFamily = MoonTimeFamily,
                fontSize = 48.sp,
                color = PinkDark
            )
            Text(
                "it's time to care for your skin 💗",
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Image(
                painter = painterResource(R.drawable.ic_skin_time_moon),
                contentDescription = null,
                modifier = Modifier.height(160.dp).fillMaxWidth(),
                contentScale = ContentScale.Fit
            )

            // Greeting
            Text(
                greeting,
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = TextPrimary
            )
            Text(
                subGreeting,
                fontFamily = PlusJakartaFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(16.dp))

            // ── Weather Card ─────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                when (weatherState) {
                    "loading" -> {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PinkPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Mendeteksi lokasi & cuaca...",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    "no_permission" -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Izin lokasi diperlukan",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Aktifkan izin lokasi agar cuaca di sekitarmu bisa ditampilkan.",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    locationPermLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Aktifkan Lokasi",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = White
                                )
                            }
                        }
                    }

                    "no_location" -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🛰️", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "GPS tidak terdeteksi",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Pastikan GPS/Lokasi aktif di pengaturan HP kamu, lalu coba lagi.",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 17.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    weatherState = "loading"
                                    fetchWeatherWithGps()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PinkPrimary)
                            ) {
                                Text(
                                    "Coba Lagi",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    "error" -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Gagal memuat data cuaca",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Periksa koneksi internet kamu dan coba lagi.",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    weatherState = "loading"
                                    fetchWeatherWithGps()
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PinkPrimary)
                            ) {
                                Text(
                                    "Coba Lagi",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    "loaded" -> {
                        val w = weather ?: return@Card
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📍", fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    w.cityName,
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PinkDark
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    "CUACA HARI INI",
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painterResource(R.drawable.ic_weather_sunny), null,
                                    modifier = Modifier.size(48.dp)
                                )
                                Column {
                                    Text(
                                        w.condition,
                                        fontFamily = PlusJakartaFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "${w.temperature}°C",
                                        fontFamily = PlusJakartaFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp,
                                        color = PinkDark
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painterResource(R.drawable.ic_weather_humidity), null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "Humidity ${w.humidity}%",
                                            fontFamily = PlusJakartaFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painterResource(R.drawable.ic_weather_uv), null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "UV ${w.uvLevel}",
                                            fontFamily = PlusJakartaFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (w.uvLevel in listOf("Tinggi", "Sangat Tinggi", "Ekstrem"))
                                                PinkDark else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Skin Recommendation (hanya muncul jika cuaca berhasil dimuat) ──
            if (weatherState == "loaded") weather?.let { w ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PinkSoft),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Skin Recommendation",
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PinkDark
                        )
                        Text(
                            WeatherManager.getWeatherContext(w),
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        WeatherManager.getSkinRecommendations(w).forEach { rec ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✔", color = PinkPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    rec,
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Progress Hari Ini ────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Progress Hari Ini",
                        fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PinkDark
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProgressItem(Modifier.weight(1f), "☀️", "Morning", morningDone)
                        ProgressItem(Modifier.weight(1f), "🌙", "Night", nightDone)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$progressPct%",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 24.sp,
                                color = if (progressPct == 100) PinkPrimary else TextPrimary
                            )
                            Text(
                                "Progress",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    if (progressPct > 0) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progressPct / 100f },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = PinkPrimary,
                            trackColor = PinkLight
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Progress Mingguan ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Progress Mingguan",
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PinkDark,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${last7.count { it.second }} / 7 Hari",
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PinkPrimary
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val dayFmt = DateTimeFormatter.ofPattern("EEE", Locale("id"))
                        last7.forEach { (dateStr, isDone) ->
                            val date    = try { LocalDate.parse(dateStr) } catch (e: Exception) { return@forEach }
                            val isToday = dateStr == today
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape)
                                        .background(when { isDone -> PinkPrimary; isToday -> CalToday; else -> PinkLight }),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDone) Text("✓", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    date.format(dayFmt).take(1),
                                    fontFamily = PlusJakartaFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isDone) PinkDark else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Achievement Terbaru ───────────────────────────────────────────
            if (latestAchievement != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PinkSoft),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painterResource(R.drawable.ic_trophy), null,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Achievement Terbaru",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PinkDark
                            )
                            Text(
                                "$latestAchievement Hari Berturut-turut",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = PinkPrimary
                            )
                            Text(
                                "Selamat! Kamu konsisten luar biasa ✨",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Reminder Card ─────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(R.drawable.ic_reminder_alarm), null,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "REMINDER",
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = PinkPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "%02d:%02d".format(reminderHour, reminderMinute),
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 40.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Setiap hari",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(PinkSoft)
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painterResource(R.drawable.ic_edit_time), null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "Ubah",
                                fontFamily = PlusJakartaFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = PinkPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (!reminderEnabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (!granted) {
                                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Button
                                    }
                                }
                            }
                            val newState = !reminderEnabled
                            reminderEnabled = newState
                            ReminderManager.setReminderEnabled(context, newState)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (reminderEnabled) PinkDark else PinkPrimary
                        )
                    ) {
                        Image(painterResource(R.drawable.ic_notification), null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (reminderEnabled) "NONAKTIFKAN REMINDER" else "AKTIFKAN REMINDER",
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = White
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Quote ─────────────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Image(painterResource(R.drawable.ic_petik), null, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(
                            todayQuote,
                            fontFamily = PlusJakartaFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painterResource(R.drawable.ic_skincare_girl), null,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour   = reminderHour,
            initialMinute = reminderMinute,
            onConfirm = { h, m ->
                reminderHour   = h
                reminderMinute = m
                ReminderManager.saveReminderTime(context, h, m)
                if (reminderEnabled) ReminderManager.scheduleReminder(context)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
private fun ProgressItem(modifier: Modifier, emoji: String, label: String, done: Boolean) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(46.dp).clip(CircleShape)
                .background(if (done) PinkPrimary else PinkLight),
            contentAlignment = Alignment.Center
        ) {
            if (done) Text("✓", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            else Text(emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontFamily = PlusJakartaFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = if (done) PinkDark else TextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Atur Waktu Reminder",
                    fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(Modifier.height(20.dp))
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = PinkSoft,
                        clockDialSelectedContentColor = White,
                        selectorColor = PinkPrimary,
                        timeSelectorSelectedContainerColor = PinkLight
                    )
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondary, fontFamily = PlusJakartaFamily)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(state.hour, state.minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Simpan", color = White, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

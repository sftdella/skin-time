package com.della.skintime.ui.routine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.della.skintime.R
import com.della.skintime.SkinTimeApp
import com.della.skintime.data.db.ActivitySchedule
import com.della.skintime.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DAY_NAMES = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
private val DAY_FULL  = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
// ISO: 1=Senin, 2=Selasa, ..., 7=Minggu
private val DAY_VALUES = listOf(1, 2, 3, 4, 5, 6, 7)

@Composable
fun ManageActivitiesScreen(onBack: () -> Unit) {
    val context    = LocalContext.current
    val db         = (context.applicationContext as SkinTimeApp).database
    val scope      = rememberCoroutineScope()

    var activities    by remember { mutableStateOf(emptyList<ActivitySchedule>()) }
    var refreshKey    by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAct    by remember { mutableStateOf<ActivitySchedule?>(null) }

    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            val list = db.getAllActivities()
            withContext(Dispatchers.Main) { activities = list }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PinkBackground, PinkSoft)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 22.sp, color = TextPrimary,
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Kelola Aktivitas",
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp, color = TextPrimary, modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        "+ Tambah", fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp, color = White
                    )
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PinkSoft)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Aktivitas Terjadwal",
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
                        fontSize = 13.sp, color = PinkDark
                    )
                    Text(
                        "Tambah aktivitas tambahan seperti Masker Wajah, Exfoliating, dll. Pilih hari agar muncul otomatis di Routine.",
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                        fontSize = 12.sp, color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daftar Aktivitas",
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, color = TextPrimary, modifier = Modifier.weight(1f)
                )
                Text(
                    "${activities.size} aktivitas",
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                    fontSize = 12.sp, color = PinkPrimary
                )
            }

            Spacer(Modifier.height(8.dp))

            if (activities.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painterResource(R.drawable.ic_aktivitas_tambahan), null,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Belum ada aktivitas tambahan.",
                            fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                            fontSize = 14.sp, color = TextSecondary
                        )
                        Text(
                            "Tap \"+ Tambah\" untuk menambahkan.",
                            fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                            fontSize = 12.sp, color = TextSecondary
                        )
                    }
                }
            } else {
                activities.forEach { act ->
                    ActivityManageItem(
                        activity = act,
                        onEdit   = { editingAct = act },
                        onDelete = {
                            scope.launch(Dispatchers.IO) {
                                db.deleteActivity(act.id)
                                withContext(Dispatchers.Main) { refreshKey++ }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Aktivitas hanya muncul di Routine pada hari yang kamu pilih.",
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                        fontSize = 12.sp, color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }

    if (showAddDialog) {
        ActivityDialog(
            initialName = "",
            initialDays = emptyList(),
            title       = "Tambah Aktivitas Baru",
            onConfirm   = { name, days ->
                scope.launch(Dispatchers.IO) {
                    db.insertActivity(ActivitySchedule(name = name, days = days))
                    withContext(Dispatchers.Main) { refreshKey++; showAddDialog = false }
                }
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingAct?.let { act ->
        ActivityDialog(
            initialName = act.name,
            initialDays = act.days,
            title       = "Edit Aktivitas",
            onConfirm   = { name, days ->
                scope.launch(Dispatchers.IO) {
                    db.updateActivity(act.copy(name = name, days = days))
                    withContext(Dispatchers.Main) { refreshKey++; editingAct = null }
                }
            },
            onDismiss = { editingAct = null }
        )
    }
}

@Composable
private fun ActivityManageItem(
    activity: ActivitySchedule,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painterResource(R.drawable.ic_aktivitas_tambahan), null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    activity.name,
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
                    fontSize = 14.sp, color = TextPrimary
                )
                val dayText = if (activity.days.isEmpty()) "Tidak ada jadwal"
                else activity.days.map { d ->
                    DAY_NAMES.getOrNull(DAY_VALUES.indexOf(d)) ?: ""
                }.filter { it.isNotBlank() }.joinToString(", ")
                Text(
                    dayText,
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                    fontSize = 12.sp, color = TextSecondary
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Image(painterResource(R.drawable.ic_edit_time), "Edit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Text("🗑", fontSize = 16.sp)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus Aktivitas?", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text  = { Text("Hapus \"${activity.name}\" dari daftar?", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Hapus", color = PinkDark, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal", color = TextSecondary, fontFamily = PlusJakartaFamily)
                }
            },
            containerColor = White, shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ActivityDialog(
    initialName: String,
    initialDays: List<Int>,
    title: String,
    onConfirm: (String, List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    var name         by remember { mutableStateOf(initialName) }
    var selectedDays by remember { mutableStateOf(initialDays.toSet()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    title,
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp, color = TextPrimary
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nama Aktivitas", fontFamily = PlusJakartaFamily) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PinkPrimary, focusedLabelColor = PinkPrimary
                    )
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Hari Aktif:",
                    fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                // Day chips - 2 baris
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DAY_VALUES.take(4).forEachIndexed { idx, dayVal ->
                            DayChip(
                                label    = DAY_NAMES[idx],
                                selected = dayVal in selectedDays,
                                onClick  = {
                                    selectedDays = if (dayVal in selectedDays)
                                        selectedDays - dayVal else selectedDays + dayVal
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DAY_VALUES.drop(4).forEachIndexed { idx, dayVal ->
                            DayChip(
                                label    = DAY_NAMES[idx + 4],
                                selected = dayVal in selectedDays,
                                onClick  = {
                                    selectedDays = if (dayVal in selectedDays)
                                        selectedDays - dayVal else selectedDays + dayVal
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondary, fontFamily = PlusJakartaFamily)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) onConfirm(name.trim(), selectedDays.sorted())
                        },
                        enabled = name.isNotBlank(),
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

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PinkPrimary else CalEmpty)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
            fontSize = 12.sp, color = if (selected) White else TextSecondary
        )
    }
}

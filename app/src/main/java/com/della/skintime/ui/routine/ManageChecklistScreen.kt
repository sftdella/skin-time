package com.della.skintime.ui.routine

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.della.skintime.R
import com.della.skintime.SkinTimeApp
import com.della.skintime.data.db.ChecklistStep
import com.della.skintime.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ManageChecklistScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db      = (context.applicationContext as SkinTimeApp).database
    val scope   = rememberCoroutineScope()

    var steps      by remember { mutableStateOf(emptyList<ChecklistStep>()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingStep   by remember { mutableStateOf<ChecklistStep?>(null) }

    LaunchedEffect(refreshKey) {
        withContext(Dispatchers.IO) {
            val s = db.getAllSteps()
            withContext(Dispatchers.Main) { steps = s }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PinkBackground, PinkSoft)))) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // Top Bar
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 22.sp, color = TextPrimary,
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
                }
                Text("Kelola Checklist", fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary,
                    modifier = Modifier.weight(1f))
                Button(onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    Text("+ Tambah Step", fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp, color = White)
                }
            }

            // Info Card
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PinkSoft)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Kelola urutan dan langkah skincare routine", fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PinkDark)
                    Text("Tambah, edit, hapus, dan atur urutan step skincare sesuai kebutuhanmu.",
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                        fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Daftar Step Skincare", fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary,
                    modifier = Modifier.weight(1f))
                Text("${steps.size} step", fontFamily = PlusJakartaFamily,
                    fontWeight = FontWeight.Medium, fontSize = 12.sp, color = PinkPrimary)
            }

            Spacer(Modifier.height(8.dp))

            steps.forEachIndexed { index, step ->
                StepManageItem(
                    step        = step,
                    canMoveUp   = index > 0,
                    canMoveDown = index < steps.size - 1,
                    onEdit      = { editingStep = step },
                    onDelete    = {
                        scope.launch(Dispatchers.IO) {
                            db.deleteStep(step.id)
                            val remaining = db.getAllSteps()
                            remaining.forEachIndexed { i, s ->
                                db.updateStep(s.copy(orderIndex = i))
                            }
                            withContext(Dispatchers.Main) { refreshKey++ }
                        }
                    },
                    onMoveUp = {
                        scope.launch(Dispatchers.IO) {
                            val prev = steps[index - 1]
                            db.updateStep(step.copy(orderIndex = index - 1))
                            db.updateStep(prev.copy(orderIndex = index))
                            withContext(Dispatchers.Main) { refreshKey++ }
                        }
                    },
                    onMoveDown = {
                        scope.launch(Dispatchers.IO) {
                            val next = steps[index + 1]
                            db.updateStep(step.copy(orderIndex = index + 1))
                            db.updateStep(next.copy(orderIndex = index))
                            withContext(Dispatchers.Main) { refreshKey++ }
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = White)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Atur Urutan: Gunakan tombol ↑ ↓ untuk mengubah urutan step skincare.",
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                        fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(90.dp))
        }
    }

    if (showAddDialog) {
        StepDialog(initialName = "", initialDescription = "", initialOptional = false, title = "Tambah Step Baru",
            onConfirm = { name, desc, optional ->
                scope.launch(Dispatchers.IO) {
                    db.insertStep(ChecklistStep(name = name, description = desc,
                        orderIndex = steps.size, isOptional = optional))
                    withContext(Dispatchers.Main) { refreshKey++; showAddDialog = false }
                }
            },
            onDismiss = { showAddDialog = false })
    }

    editingStep?.let { step ->
        StepDialog(initialName = step.name, initialDescription = step.description,
            initialOptional = step.isOptional, title = "Edit Step",
            onConfirm = { name, desc, optional ->
                scope.launch(Dispatchers.IO) {
                    db.updateStep(step.copy(name = name, description = desc, isOptional = optional))
                    withContext(Dispatchers.Main) { refreshKey++; editingStep = null }
                }
            },
            onDismiss = { editingStep = null })
    }
}

@Composable
private fun StepManageItem(
    step: ChecklistStep, canMoveUp: Boolean, canMoveDown: Boolean,
    onEdit: () -> Unit, onDelete: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(28.dp)) {
                    Text("↑", fontSize = 16.sp, color = if (canMoveUp) PinkPrimary else CalEmpty,
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(28.dp)) {
                    Text("↓", fontSize = 16.sp, color = if (canMoveDown) PinkPrimary else CalEmpty,
                        fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Image(painterResource(R.drawable.ic_love), null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${step.orderIndex + 1}. ${step.name}", fontFamily = PlusJakartaFamily,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    if (step.isOptional) {
                        Spacer(Modifier.width(4.dp))
                        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(PinkSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("Opsional", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold,
                                fontSize = 9.sp, color = PinkDark)
                        }
                    }
                }
                Text(step.description, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                    fontSize = 12.sp, color = TextSecondary)
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
            title = { Text("Hapus Step?", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Hapus \"${step.name}\" dari daftar?", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium, color = TextSecondary) },
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
private fun StepDialog(
    initialName: String, initialDescription: String, initialOptional: Boolean,
    title: String, onConfirm: (String, String, Boolean) -> Unit, onDismiss: () -> Unit
) {
    var name     by remember { mutableStateOf(initialName) }
    var desc     by remember { mutableStateOf(initialDescription) }
    var optional by remember { mutableStateOf(initialOptional) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Nama Step", fontFamily = PlusJakartaFamily) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinkPrimary, focusedLabelColor = PinkPrimary))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("Deskripsi", fontFamily = PlusJakartaFamily) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PinkPrimary, focusedLabelColor = PinkPrimary))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = optional, onCheckedChange = { optional = it },
                        colors = CheckboxDefaults.colors(checkedColor = PinkPrimary))
                    Text("Opsional", fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Medium,
                        fontSize = 14.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondary, fontFamily = PlusJakartaFamily)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), desc.trim(), optional) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                        shape = RoundedCornerShape(12.dp)) {
                        Text("Simpan", color = White, fontFamily = PlusJakartaFamily, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

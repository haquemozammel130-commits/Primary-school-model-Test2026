package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.bank.QuizQuestionBank
import com.example.data.model.CompetitionConfig
import com.example.data.model.PublishedEntry
import com.example.data.model.QuizSubmission
import com.example.data.model.UiState
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.util.ExportManager
import com.google.firebase.Timestamp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPanelScreen(
    configState: UiState<CompetitionConfig?>,
    allSubmissionsState: UiState<List<QuizSubmission>>,
    isBusy: Boolean,
    onSaveSettings: (Int, Boolean, Int, String, String, CompetitionConfig?) -> Unit,
    onPublishResults: (Int, Int, String, List<QuizSubmission>, CompetitionConfig) -> Unit,
    onUnpublishResults: () -> Unit,
    onDeleteSubmission: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentConfig = (configState as? UiState.Success)?.data
    val allSubmissions = (allSubmissionsState as? UiState.Success)?.data.orEmpty()

    var selectedModel by remember { mutableIntStateOf(1) }
    var competitionOpen by remember { mutableStateOf(true) }
    var roundNumber by remember { mutableIntStateOf(1) }
    var examTitle by remember { mutableStateOf("প্রাইমারি স্কুল টেস্ট পরীক্ষা") }
    var noticeText by remember { mutableStateOf("১০০ প্রশ্নের MCQ প্রতিযোগিতায় স্বাগতম।") }
    var initializedFromServer by remember { mutableStateOf(false) }

    LaunchedEffect(currentConfig) {
        if (currentConfig != null && !initializedFromServer) {
            selectedModel = currentConfig.activeModelNumber.coerceIn(1, 20)
            competitionOpen = currentConfig.competitionOpen
            roundNumber = currentConfig.roundNumber.coerceIn(1, 1000)
            examTitle = currentConfig.examTitle
            noticeText = currentConfig.notice
            initializedFromServer = true
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyCurrentModelRound by remember { mutableStateOf(true) }
    var pendingDeleteSubmission by remember { mutableStateOf<QuizSubmission?>(null) }

    val filteredSubmissions = remember(allSubmissions, selectedModel, roundNumber, searchQuery, filterOnlyCurrentModelRound) {
        allSubmissions
            .filter { sub ->
                val matchesModel = if (filterOnlyCurrentModelRound) {
                    sub.modelNumber == selectedModel && sub.roundNumber == roundNumber
                } else {
                    true
                }
                val q = searchQuery.trim()
                val matchesSearch = q.isEmpty() ||
                    sub.participantName.contains(q, ignoreCase = true) ||
                    sub.schoolName.contains(q, ignoreCase = true) ||
                    sub.rollNumber.contains(q, ignoreCase = true)
                matchesModel && matchesSearch
            }
            .sortedWith(
                compareByDescending<QuizSubmission> { it.score }
                    .thenBy { it.durationSeconds }
                    .thenBy { it.createdAt ?: Timestamp(0, 0) }
            )
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val csvFile = ExportManager.generateResultCsvFile(
                context = context,
                modelNumber = selectedModel,
                roundNumber = roundNumber,
                submissions = filteredSubmissions
            )
            val ok = ExportManager.copyFileToUri(context, csvFile, uri)
            Toast.makeText(
                context,
                if (ok) "CSV রেজাল্ট ফাইল সফলভাবে সেভ হয়েছে!" else "CSV সেভ করতে সমস্যা হয়েছে।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val entries = filteredSubmissions.mapIndexed { idx, sub ->
                PublishedEntry(
                    sheetId = "m${selectedModel}_r$roundNumber",
                    userId = sub.userId,
                    rank = idx + 1,
                    participantName = sub.participantName,
                    schoolName = sub.schoolName,
                    rollNumber = sub.rollNumber,
                    modelNumber = sub.modelNumber,
                    roundNumber = sub.roundNumber,
                    score = sub.score,
                    correctCount = sub.correctCount,
                    wrongCount = sub.wrongCount,
                    unansweredCount = sub.unansweredCount,
                    durationSeconds = sub.durationSeconds
                )
            }
            val pdfFile = ExportManager.generateResultSheetPdf(
                context = context,
                examTitle = examTitle.ifBlank { "প্রাইমারি স্কুল টেস্ট পরীক্ষা" },
                modelNumber = selectedModel,
                roundNumber = roundNumber,
                entries = entries
            )
            val ok = ExportManager.copyFileToUri(context, pdfFile, uri)
            Toast.makeText(
                context,
                if (ok) "PDF রেজাল্ট শিট সফলভাবে সেভ হয়েছে!" else "PDF সেভ করতে সমস্যা হয়েছে।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "এডমিন কন্ট্রোল প্যানেল",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "শুধুমাত্র অনুমোদিত এডমিনের জন্য সংরক্ষিত",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider()

                    // Competition ON / OFF Toggle
                    Surface(
                        color = if (competitionOpen) StatusSuccessContainer else StatusDangerContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (competitionOpen) "প্রতিযোগিতা চালু আছে (ON)" else "প্রতিযোগিতা বন্ধ আছে (OFF)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (competitionOpen) StatusSuccess else StatusDanger
                                )
                                Text(
                                    text = if (competitionOpen) "সকল ইউজার বর্তমানে পরীক্ষা দিতে পারবেন" else "ইউজারদের জন্য ফর্ম জমা বন্ধ রাখা হয়েছে",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = competitionOpen,
                                onCheckedChange = { competitionOpen = it },
                                modifier = Modifier.testTag("admin_competition_toggle")
                            )
                        }
                    }

                    // 20 Model Selector Grid
                    Text(
                        text = "মডেল টেস্ট নির্বাচন করুন (মডেল ১ – ২০):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (m in 1..20) {
                            val isSelected = selectedModel == m
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .size(width = 62.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedModel = m }
                                    .testTag("admin_model_chip_$m")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "ম-${QuizQuestionBank.toBanglaDigits(m)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Round selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "প্রতিযোগিতার রাউন্ড নম্বর:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (roundNumber > 1) roundNumber-- },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "রাউন্ড কমান")
                            }
                            Text(
                                text = QuizQuestionBank.toBanglaDigits(roundNumber),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { if (roundNumber < 1000) roundNumber++ },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "রাউন্ড বাড়ান")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = examTitle,
                        onValueChange = { examTitle = it.take(120) },
                        label = { Text("পরীক্ষার শিরোনাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noticeText,
                        onValueChange = { noticeText = it.take(500) },
                        label = { Text("ইউজারদের জন্য নোটিশ") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveSettings(
                                selectedModel,
                                competitionOpen,
                                roundNumber,
                                examTitle,
                                noticeText,
                                currentConfig
                            )
                        },
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_save_settings_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("মডেল ও অন/অফ সেটিংস সেভ করুন")
                        }
                    }
                }
            }
        }

        // Export & Publish Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "রেজাল্ট এক্সপোর্ট (CSV / PDF) ও পাবলিশ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "নির্বাচিত মডেল ${QuizQuestionBank.toBanglaDigits(selectedModel)} (রাউন্ড ${QuizQuestionBank.toBanglaDigits(roundNumber)}) — মোট সাবমিশন: ${QuizQuestionBank.toBanglaDigits(filteredSubmissions.size)} জন",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                saveCsvLauncher.launch("results_model_${selectedModel}_r$roundNumber.csv")
                            },
                            enabled = filteredSubmissions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_download_csv_button")
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV ডাউনলোড")
                        }

                        OutlinedButton(
                            onClick = {
                                savePdfLauncher.launch("result_sheet_model_${selectedModel}_r$roundNumber.pdf")
                            },
                            enabled = filteredSubmissions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("admin_download_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF ডাউনলোড")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val csv = ExportManager.generateResultCsvFile(
                                    context = context,
                                    modelNumber = selectedModel,
                                    roundNumber = roundNumber,
                                    submissions = filteredSubmissions
                                )
                                ExportManager.shareFile(context, csv, "text/csv", "CSV ফাইল শেয়ার করুন")
                            },
                            enabled = filteredSubmissions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CSV শেয়ার")
                        }

                        OutlinedButton(
                            onClick = {
                                val entries = filteredSubmissions.mapIndexed { idx, sub ->
                                    PublishedEntry(
                                        sheetId = "m${selectedModel}_r$roundNumber",
                                        userId = sub.userId,
                                        rank = idx + 1,
                                        participantName = sub.participantName,
                                        schoolName = sub.schoolName,
                                        rollNumber = sub.rollNumber,
                                        modelNumber = sub.modelNumber,
                                        roundNumber = sub.roundNumber,
                                        score = sub.score,
                                        correctCount = sub.correctCount,
                                        wrongCount = sub.wrongCount,
                                        unansweredCount = sub.unansweredCount,
                                        durationSeconds = sub.durationSeconds
                                    )
                                }
                                val pdf = ExportManager.generateResultSheetPdf(
                                    context = context,
                                    examTitle = examTitle.ifBlank { "প্রাইমারি স্কুল টেস্ট পরীক্ষা" },
                                    modelNumber = selectedModel,
                                    roundNumber = roundNumber,
                                    entries = entries
                                )
                                ExportManager.openPdfFile(context, pdf)
                            },
                            enabled = filteredSubmissions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF প্রিভিউ")
                        }
                    }

                    Button(
                        onClick = {
                            val cfg = currentConfig ?: CompetitionConfig(
                                activeModelNumber = selectedModel,
                                competitionOpen = competitionOpen,
                                roundNumber = roundNumber,
                                examTitle = examTitle,
                                notice = noticeText
                            )
                            onPublishResults(
                                selectedModel,
                                roundNumber,
                                examTitle.ifBlank { "প্রাইমারি স্কুল টেস্ট পরীক্ষা" },
                                allSubmissions,
                                cfg
                            )
                        },
                        enabled = !isBusy && filteredSubmissions.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_publish_results_button")
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ইউজার অ্যাপে রেজাল্ট শিট পাবলিশ করুন")
                    }

                    if (currentConfig?.resultsPublished == true) {
                        TextButton(
                            onClick = onUnpublishResults,
                            enabled = !isBusy,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.Unpublished, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("পাবলিশ ব্যানার বন্ধ করুন")
                        }
                    }
                }
            }
        }

        // Submissions List Header & Search
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "জমাকৃত উত্তরপত্রসমূহ (${QuizQuestionBank.toBanglaDigits(filteredSubmissions.size)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { filterOnlyCurrentModelRound = !filterOnlyCurrentModelRound }) {
                        Text(if (filterOnlyCurrentModelRound) "সব মডেল দেখান" else "শুধু নির্বাচিত মডেল")
                    }
                }
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("নাম, বিদ্যালয় বা রোল দিয়ে খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (allSubmissionsState is UiState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredSubmissions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "এই মডেল ও রাউন্ডে এখনো কোনো পরীক্ষার্থী ফর্ম সাবমিট করেনি।",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filteredSubmissions, key = { it.id }) { sub ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sub.participantName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${sub.schoolName} • রোল: ${sub.rollNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "মডেল ${QuizQuestionBank.toBanglaDigits(sub.modelNumber)} | রাউন্ড ${QuizQuestionBank.toBanglaDigits(sub.roundNumber)} • সঠিক: ${QuizQuestionBank.toBanglaDigits(sub.correctCount)} | ভুল: ${QuizQuestionBank.toBanglaDigits(sub.wrongCount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${QuizQuestionBank.toBanglaDigits(sub.score)}/১০০",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        IconButton(
                            onClick = { pendingDeleteSubmission = sub },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "সাবমিশন মুছুন",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingDeleteSubmission != null) {
        val target = pendingDeleteSubmission!!
        AlertDialog(
            onDismissRequest = { pendingDeleteSubmission = null },
            title = { Text("সাবমিশন মুছবেন?") },
            text = { Text("${target.participantName}-এর জমাকৃত উত্তরপত্রটি স্থায়ীভাবে মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteSubmission = null
                        onDeleteSubmission(target.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSubmission = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

package com.example.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.bank.QuizQuestionBank
import com.example.data.model.CompetitionConfig
import com.example.data.model.PublishedEntry
import com.example.data.model.PublishedSheet
import com.example.data.model.UiState
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.GoldSecondaryContainer
import com.example.util.ExportManager
import java.util.Locale

@Composable
fun PublishedResultsScreen(
    configState: UiState<CompetitionConfig?>,
    publishedSheetsState: UiState<List<PublishedSheet>>,
    observeEntries: (String) -> kotlinx.coroutines.flow.Flow<UiState<List<PublishedEntry>>>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = (configState as? UiState.Success)?.data
    val sheets = (publishedSheetsState as? UiState.Success)?.data.orEmpty()

    val preferredSheetId = remember(config, sheets) {
        if (config != null && config.resultsPublished && config.publishedModelNumber > 0) {
            "m${config.publishedModelNumber}_r${config.publishedRoundNumber}"
        } else {
            sheets.firstOrNull()?.id
        }
    }

    var selectedSheetId by remember(preferredSheetId) { mutableStateOf(preferredSheetId) }
    val activeSheet = sheets.firstOrNull { it.id == selectedSheetId } ?: sheets.firstOrNull()

    val entriesFlow = remember(activeSheet?.id) {
        activeSheet?.let { observeEntries(it.id) }
    }
    val entriesState: UiState<List<PublishedEntry>> = if (entriesFlow != null) {
        entriesFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading).value
    } else {
        UiState.Success(emptyList())
    }
    val entries = (entriesState as? UiState.Success)?.data.orEmpty()

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && activeSheet != null) {
            val file = ExportManager.generateResultSheetPdf(
                context = context,
                examTitle = activeSheet.examTitle,
                modelNumber = activeSheet.modelNumber,
                roundNumber = activeSheet.roundNumber,
                entries = entries
            )
            val ok = ExportManager.copyFileToUri(context, file, uri)
            Toast.makeText(
                context,
                if (ok) "রেজাল্ট শিট PDF সেভ হয়েছে!" else "PDF সেভ করা যায়নি।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    if (publishedSheetsState is UiState.Loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (publishedSheetsState is UiState.Error) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "রেজাল্ট লোড করতে সমস্যা হয়েছে: ${publishedSheetsState.message}",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    if (sheets.isEmpty() || activeSheet == null) {
        Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "এখনো কোনো রেজাল্ট শিট প্রকাশিত হয়নি",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "এডমিন প্যানেল থেকে রেজাল্ট শিট পাবলিশ করলে সকল পরীক্ষার্থীর মেধাতালিকা এখানে দেখা যাবে।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (sheets.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sheets.forEach { sheet ->
                        FilterChip(
                            selected = sheet.id == activeSheet.id,
                            onClick = { selectedSheetId = sheet.id },
                            label = {
                                Text("মডেল ${QuizQuestionBank.toBanglaDigits(sheet.modelNumber)} (রাউন্ড ${QuizQuestionBank.toBanglaDigits(sheet.roundNumber)})")
                            }
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = GoldSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${activeSheet.examTitle} — রেজাল্ট শিট",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "মডেল টেস্ট: ${QuizQuestionBank.toBanglaDigits(activeSheet.modelNumber)}  •  রাউন্ড: ${QuizQuestionBank.toBanglaDigits(activeSheet.roundNumber)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ResultStatBox(
                            label = "মোট পরীক্ষার্থী",
                            value = QuizQuestionBank.toBanglaDigits(activeSheet.totalParticipants),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ResultStatBox(
                            label = "সর্বোচ্চ নম্বর",
                            value = "${QuizQuestionBank.toBanglaDigits(activeSheet.highestScore)}/১০০",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        ResultStatBox(
                            label = "গড় নম্বর",
                            value = String.format(Locale.US, "%.1f", activeSheet.averageScore),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                savePdfLauncher.launch("result_sheet_model_${activeSheet.modelNumber}_r${activeSheet.roundNumber}.pdf")
                            },
                            enabled = entries.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("user_download_result_pdf_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF ডাউনলোড")
                        }
                        OutlinedButton(
                            onClick = {
                                val file = ExportManager.generateResultSheetPdf(
                                    context = context,
                                    examTitle = activeSheet.examTitle,
                                    modelNumber = activeSheet.modelNumber,
                                    roundNumber = activeSheet.roundNumber,
                                    entries = entries
                                )
                                ExportManager.openPdfFile(context, file)
                            },
                            enabled = entries.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ওপেন / শেয়ার")
                        }
                    }
                }
            }
        }

        if (entriesState is UiState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (entries.isEmpty()) {
            item {
                Text(
                    text = "এই রাউন্ডে কোনো অংশগ্রহণকারী পাওয়া যায়নি।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            item {
                Text(
                    text = "মেধাক্রম তালিকা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(entries, key = { it.userId }) { entry ->
                PublishedEntryCard(entry = entry, isMe = entry.userId == currentUserId)
            }
        }
    }
}

@Composable
private fun ResultStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PublishedEntryCard(entry: PublishedEntry, isMe: Boolean) {
    val badgeBg = when (entry.rank) {
        1 -> GoldSecondaryContainer
        2 -> Color(0xFFE5E7EB)
        3 -> Color(0xFFFED7AA)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeText = when (entry.rank) {
        1 -> GoldSecondary
        2 -> Color(0xFF374151)
        3 -> Color(0xFF9A3412)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMe) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(badgeBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = QuizQuestionBank.toBanglaDigits(entry.rank),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeText
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "${entry.participantName} (আপনি)" else entry.participantName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${entry.schoolName} • রোল: ${entry.rollNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${QuizQuestionBank.toBanglaDigits(entry.score)}/১০০",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

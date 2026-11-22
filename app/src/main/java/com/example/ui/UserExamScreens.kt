package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.bank.QuizQuestionBank
import com.example.data.model.CompetitionConfig
import com.example.data.model.McqQuestion
import com.example.data.model.QuizSubject
import com.example.data.model.QuizSubmission
import com.example.data.model.UiState
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.util.ExportManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OPTION_LABELS_BN = listOf("ক", "খ", "গ", "ঘ")

@Composable
fun ExamLobbyScreen(
    configState: UiState<CompetitionConfig?>,
    mySubmissionsState: UiState<List<QuizSubmission>>,
    participantName: String,
    schoolName: String,
    rollNumber: String,
    onFormChange: (String, String, String) -> Unit,
    onStartExam: (Int, Int) -> Unit,
    onOpenSubmission: (QuizSubmission) -> Unit,
    modifier: Modifier = Modifier
) {
    val mySubmissions = (mySubmissionsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_quiz_hero),
                                contentDescription = "কুইজ প্রতিযোগিতা ব্যানার",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "প্রাইমারি স্কুল টেস্ট পরীক্ষা",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "২০টি মডেল টেস্ট • ১০০টি MCQ • পূর্ণমান ১০০",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            when (configState) {
                is UiState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("প্রতিযোগিতার তথ্য লোড হচ্ছে...")
                        }
                    }
                }
                is UiState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "তথ্য লোড করতে সমস্যা হয়েছে: ${configState.message}",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val config = configState.data
                    if (config == null) {
                        CompetitionClosedCard(
                            title = "প্রতিযোগিতা এখনো শুরু হয়নি",
                            subtitle = "এডমিন প্যানেল থেকে মডেল টেস্ট নির্বাচন ও প্রতিযোগিতা চালু করা হলে এখানে পরীক্ষার ফর্ম সক্রিয় হবে।"
                        )
                    } else {
                        val activeModel = QuizQuestionBank.getModelInfo(config.activeModelNumber)
                        val alreadySubmitted = mySubmissions.firstOrNull {
                            it.modelNumber == config.activeModelNumber && it.roundNumber == config.roundNumber
                        }

                        ActiveCompetitionCard(
                            config = config,
                            modelTitle = activeModel.titleBn,
                            modelSubtitle = activeModel.subtitleBn,
                            alreadySubmitted = alreadySubmitted,
                            participantName = participantName,
                            schoolName = schoolName,
                            rollNumber = rollNumber,
                            onFormChange = onFormChange,
                            onStartExam = { onStartExam(config.activeModelNumber, config.roundNumber) },
                            onViewSubmitted = { sub -> onOpenSubmission(sub) }
                        )
                    }
                }
            }
        }

        if (mySubmissions.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "আমার জমাকৃত উত্তরপত্র (${QuizQuestionBank.toBanglaDigits(mySubmissions.size)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(mySubmissions, key = { it.id }) { sub ->
                PastSubmissionCard(
                    submission = sub,
                    onOpen = { onOpenSubmission(sub) }
                )
            }
        }
    }
}

@Composable
private fun CompetitionClosedCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StatusDangerContainer),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = StatusDanger,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StatusDanger
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ActiveCompetitionCard(
    config: CompetitionConfig,
    modelTitle: String,
    modelSubtitle: String,
    alreadySubmitted: QuizSubmission?,
    participantName: String,
    schoolName: String,
    rollNumber: String,
    onFormChange: (String, String, String) -> Unit,
    onStartExam: () -> Unit,
    onViewSubmitted: (QuizSubmission) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (config.competitionOpen) StatusSuccessContainer else StatusDangerContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (config.competitionOpen) "● প্রতিযোগিতা চালু (ON)" else "● প্রতিযোগিতা বন্ধ (OFF)",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (config.competitionOpen) StatusSuccess else StatusDanger,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    text = "রাউন্ড ${QuizQuestionBank.toBanglaDigits(config.roundNumber)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = config.examTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = modelTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = modelSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (config.notice.isNotBlank()) {
                Text(
                    text = "নোটিশ: ${config.notice}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            if (alreadySubmitted != null) {
                Surface(
                    color = StatusSuccessContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = StatusSuccess
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "আপনি এই মডেলের উত্তরপত্র জমা দিয়েছেন!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }
                        Text(
                            text = "প্রাপ্ত নম্বর: ${QuizQuestionBank.toBanglaDigits(alreadySubmitted.score)} / ১০০",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { onViewSubmitted(alreadySubmitted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("view_submitted_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("উত্তরপত্র ও PDF ডাউনলোড দেখুন")
                        }
                    }
                }
            } else if (!config.competitionOpen) {
                CompetitionClosedCard(
                    title = "প্রতিযোগিতা বর্তমানে বন্ধ আছে",
                    subtitle = "এডমিন প্রতিযোগিতা চালু (ON) করলে এখান থেকে পরীক্ষা শুরু করতে পারবেন।"
                )
            } else {
                Text(
                    text = "পরীক্ষার্থীর তথ্য পূরণ করুন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = participantName,
                    onValueChange = { onFormChange(it, schoolName, rollNumber) },
                    label = { Text("পরীক্ষার্থীর পূর্ণ নাম") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("participant_name_input")
                )
                OutlinedTextField(
                    value = schoolName,
                    onValueChange = { onFormChange(participantName, it, rollNumber) },
                    label = { Text("বিদ্যালয়ের নাম") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("school_name_input")
                )
                OutlinedTextField(
                    value = rollNumber,
                    onValueChange = { onFormChange(participantName, schoolName, it) },
                    label = { Text("রোল / আইডি নম্বর") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roll_number_input")
                )

                Button(
                    onClick = onStartExam,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("start_exam_button"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "১০০ প্রশ্নের পরীক্ষা শুরু করুন",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PastSubmissionCard(
    submission: QuizSubmission,
    onOpen: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val dateText = dateFormat.format(submission.createdAt?.toDate() ?: Date())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("past_submission_${submission.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "মডেল টেস্ট - ${QuizQuestionBank.toBanglaDigits(submission.modelNumber)} (রাউন্ড ${QuizQuestionBank.toBanglaDigits(submission.roundNumber)})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${submission.participantName} • রোল: ${submission.rollNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${QuizQuestionBank.toBanglaDigits(submission.score)}/১০০",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PDF দেখুন →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExamTakingScreen(
    session: ActiveExamSession,
    isSubmitting: Boolean,
    onSelectAnswer: (Int, Int) -> Unit,
    onSubmitExam: () -> Unit,
    onExitExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubject by remember { mutableStateOf<QuizSubject?>(null) }
    var showNavigatorSheet by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    BackHandler { showExitConfirmDialog = true }

    val answeredCount = remember(session.answers) { session.answers.count { it in 0..3 } }
    val filteredQuestions = remember(session.questions, selectedSubject) {
        if (selectedSubject == null) session.questions
        else session.questions.filter { it.subject == selectedSubject }
    }

    val mins = session.elapsedSeconds / 60
    val secs = session.elapsedSeconds % 60
    val timerText = "${QuizQuestionBank.toBanglaDigits(mins.toString().padStart(2, '0'))}:${QuizQuestionBank.toBanglaDigits(secs.toString().padStart(2, '0'))}"

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            title = {
                Column {
                    Text(
                        text = "মডেল টেস্ট - ${QuizQuestionBank.toBanglaDigits(session.modelNumber)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "উত্তর: ${QuizQuestionBank.toBanglaDigits(answeredCount)}/১০০  •  সময়: $timerText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = { showExitConfirmDialog = true },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                }
            },
            actions = {
                IconButton(
                    onClick = { showNavigatorSheet = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("question_map_button")
                ) {
                    Icon(Icons.Default.GridView, contentDescription = "প্রশ্ন তালিকা")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        LinearProgressIndicator(
            progress = { answeredCount / 100f },
            modifier = Modifier.fillMaxWidth().height(5.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSubject == null,
                onClick = { selectedSubject = null },
                label = { Text("সব (১০০)") }
            )
            QuizSubject.entries.forEach { subject ->
                FilterChip(
                    selected = selectedSubject == subject,
                    onClick = { selectedSubject = subject },
                    label = { Text("${subject.shortBn} (২৫)") }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(
                items = filteredQuestions,
                key = { _, q -> q.number }
            ) { _, question ->
                val qIndex = question.number - 1
                val selectedOption = session.answers.getOrElse(qIndex) { -1 }
                McqQuestionCard(
                    question = question,
                    selectedOption = selectedOption,
                    onSelectOption = { optIdx -> onSelectAnswer(qIndex, optIdx) }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        Surface(
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "উত্তর দেওয়া হয়েছে: ${QuizQuestionBank.toBanglaDigits(answeredCount)}/১০০",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "বাকি: ${QuizQuestionBank.toBanglaDigits(100 - answeredCount)}টি প্রশ্ন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { showSubmitConfirmDialog = true },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("submit_quiz_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ফর্ম সাবমিট করুন")
                    }
                }
            }
        }
    }

    if (showNavigatorSheet) {
        ModalBottomSheet(onDismissRequest = { showNavigatorSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "১০০টি প্রশ্নের অবস্থা (সবুজ = উত্তর দেওয়া)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    for (i in 0 until 100) {
                        val isAnswered = session.answers.getOrElse(i) { -1 } in 0..3
                        Surface(
                            color = if (isAnswered) StatusSuccessContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .size(34.dp)
                                .clickable {
                                    selectedSubject = session.questions[i].subject
                                    showNavigatorSheet = false
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = QuizQuestionBank.toBanglaDigits(i + 1),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isAnswered) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubmitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirmDialog = false },
            title = { Text("উত্তরপত্র জমা দিতে চান?") },
            text = {
                Text(
                    "আপনি ১০০টি প্রশ্নের মধ্যে ${QuizQuestionBank.toBanglaDigits(answeredCount)}টির উত্তর দিয়েছেন। জমা দেওয়ার পর সরাসরি PDF উত্তরপত্র ডাউনলোড করতে পারবেন।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmDialog = false
                        onSubmitExam()
                    },
                    modifier = Modifier.testTag("confirm_submit_dialog_button")
                ) {
                    Text("হ্যাঁ, জমা দিন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmDialog = false }) {
                    Text("আরো দেখুন")
                }
            }
        )
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("পরীক্ষা বাতিল করবেন?") },
            text = { Text("এখন বের হয়ে গেলে আপনার বর্তমান উত্তরগুলো সংরক্ষিত হবে না।") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onExitExam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("বের হোন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("থাকুন")
                }
            }
        )
    }
}

@Composable
private fun McqQuestionCard(
    question: McqQuestion,
    selectedOption: Int,
    onSelectOption: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "প্রশ্ন ${QuizQuestionBank.toBanglaDigits(question.number)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = question.subject.titleBn,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            question.options.forEachIndexed { idx, optText ->
                val isSelected = selectedOption == idx
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onSelectOption(idx) }
                        .testTag("q_${question.number}_opt_$idx")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${OPTION_LABELS_BN[idx]}) $optText",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmissionResultScreen(
    submission: QuizSubmission,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }
    val context = LocalContext.current
    val questions = remember(submission.modelNumber) {
        QuizQuestionBank.getQuestionsForModel(submission.modelNumber)
    }

    var cachedPdf by remember { mutableStateOf<File?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val pdfFile = cachedPdf ?: ExportManager.generateAnswerSheetPdf(context, submission, questions).also {
                cachedPdf = it
            }
            val saved = ExportManager.copyFileToUri(context, pdfFile, uri)
            Toast.makeText(
                context,
                if (saved) "উত্তরপত্র PDF সফলভাবে সেভ করা হয়েছে!" else "PDF সেভ করতে সমস্যা হয়েছে।",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            title = { Text("উত্তরপত্র ও ফলাফল বিবরণী") },
            navigationIcon = {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(StatusSuccessContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Text(
                            text = "প্রাপ্ত নম্বর: ${QuizQuestionBank.toBanglaDigits(submission.score)} / ১০০",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${submission.participantName} • ${submission.schoolName} (রোল: ${submission.rollNumber})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatChip("সঠিক", QuizQuestionBank.toBanglaDigits(submission.correctCount), StatusSuccessContainer, StatusSuccess)
                            StatChip("ভুল", QuizQuestionBank.toBanglaDigits(submission.wrongCount), StatusDangerContainer, StatusDanger)
                            StatChip("ফাঁকা", QuizQuestionBank.toBanglaDigits(submission.unansweredCount), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                val file = cachedPdf ?: ExportManager.generateAnswerSheetPdf(context, submission, questions).also {
                                    cachedPdf = it
                                }
                                savePdfLauncher.launch(file.name)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_answer_pdf_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("উত্তরপত্র PDF ফাইল ডাউনলোড করুন")
                        }

                        OutlinedButton(
                            onClick = {
                                val file = cachedPdf ?: ExportManager.generateAnswerSheetPdf(context, submission, questions).also {
                                    cachedPdf = it
                                }
                                ExportManager.openPdfFile(context, file)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("open_share_answer_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("সরাসরি PDF ওপেন / শেয়ার করুন")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "১০০টি প্রশ্নের উত্তর পর্যালোচনা",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            itemsIndexed(questions, key = { _, q -> q.number }) { idx, q ->
                val chosen = submission.answers.getOrElse(idx) { -1 }
                val isCorrect = chosen == q.correctIndex
                val statusColor = when {
                    chosen == -1 -> MaterialTheme.colorScheme.onSurfaceVariant
                    isCorrect -> StatusSuccess
                    else -> StatusDanger
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${QuizQuestionBank.toBanglaDigits(q.number)}. ${q.question}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        val chosenLabel = if (chosen in 0..3) "${OPTION_LABELS_BN[chosen]}) ${q.options[chosen]}" else "উত্তর দেওয়া হয়নি"
                        val correctLabel = "${OPTION_LABELS_BN[q.correctIndex]}) ${q.options[q.correctIndex]}"
                        Text(
                            text = "আপনার উত্তর: $chosenLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "সঠিক উত্তর: $correctLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    bgColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color
) {
    Surface(color = bgColor, shape = RoundedCornerShape(10.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = textColor)
        }
    }
}

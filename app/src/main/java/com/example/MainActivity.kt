package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.attemptAutoSignIn
import com.example.auth.signOut
import com.example.data.firebase.QuizRepository
import com.example.ui.AdminPanelScreen
import com.example.ui.AuthScreen
import com.example.ui.ExamLobbyScreen
import com.example.ui.ExamTakingScreen
import com.example.ui.MainViewModel
import com.example.ui.ProfileScreen
import com.example.ui.PublishedResultsScreen
import com.example.ui.SubmissionResultScreen
import com.example.ui.theme.PrimaryQuizTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class AppTab(
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    EXAM(R.string.nav_exam, Icons.Default.Quiz, "tab_exam"),
    RESULTS(R.string.nav_results, Icons.Default.EmojiEvents, "tab_results"),
    PROFILE(R.string.nav_profile, Icons.Default.Person, "tab_profile"),
    ADMIN(R.string.nav_admin, Icons.Default.AdminPanelSettings, "tab_admin")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrimaryQuizTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf<FirebaseUser?>(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedQuizApp(
                currentUser = user,
                onSignOut = {
                    signOut(credentialManager, scope) {
                        currentUser = null
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedQuizApp(
    currentUser: FirebaseUser,
    onSignOut: () -> Unit
) {
    val uid = currentUser.uid
    val displayName = currentUser.displayName ?: ""
    val email = currentUser.email ?: ""

    val viewModel: MainViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MainViewModel(
                    repository = QuizRepository(db),
                    currentUserId = uid,
                    defaultDisplayName = displayName,
                    userEmail = email
                )
            }
        }
    )

    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val configState by viewModel.configState.collectAsStateWithLifecycle()
    val mySubmissionsState by viewModel.mySubmissionsState.collectAsStateWithLifecycle()
    val publishedSheetsState by viewModel.publishedSheetsState.collectAsStateWithLifecycle()
    val allSubmissionsForAdminState by viewModel.allSubmissionsForAdminState.collectAsStateWithLifecycle()

    val activeExam by viewModel.activeExam.collectAsStateWithLifecycle()
    val completedSubmission by viewModel.completedSubmission.collectAsStateWithLifecycle()
    val participantName by viewModel.participantName.collectAsStateWithLifecycle()
    val schoolName by viewModel.schoolName.collectAsStateWithLifecycle()
    val rollNumber by viewModel.rollNumber.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        val msg = feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    // Strict UI isolation: Admin tab is ONLY exposed when isAdmin == true
    val availableTabs = remember(isAdmin) {
        if (isAdmin) listOf(AppTab.EXAM, AppTab.RESULTS, AppTab.ADMIN, AppTab.PROFILE)
        else listOf(AppTab.EXAM, AppTab.RESULTS, AppTab.PROFILE)
    }
    var currentTab by remember { mutableStateOf(AppTab.EXAM) }
    if (currentTab !in availableTabs) {
        currentTab = AppTab.EXAM
    }

    val activeSession = activeExam
    val completed = completedSubmission

    if (activeSession != null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            ExamTakingScreen(
                session = activeSession,
                isSubmitting = isBusy,
                onSelectAnswer = viewModel::selectAnswer,
                onSubmitExam = viewModel::submitActiveExam,
                onExitExam = viewModel::cancelActiveExam,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    if (completed != null) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            SubmissionResultScreen(
                submission = completed,
                onClose = viewModel::closeSubmissionSummary,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        availableTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        availableTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                when (currentTab) {
                    AppTab.EXAM -> ExamLobbyScreen(
                        configState = configState,
                        mySubmissionsState = mySubmissionsState,
                        participantName = participantName,
                        schoolName = schoolName,
                        rollNumber = rollNumber,
                        onFormChange = viewModel::updateParticipantForm,
                        onStartExam = viewModel::startExam,
                        onOpenSubmission = viewModel::openSubmissionSummary,
                        modifier = Modifier.fillMaxSize()
                    )

                    AppTab.RESULTS -> PublishedResultsScreen(
                        configState = configState,
                        publishedSheetsState = publishedSheetsState,
                        observeEntries = viewModel::observePublishedEntries,
                        currentUserId = uid,
                        modifier = Modifier.fillMaxSize()
                    )

                    AppTab.ADMIN -> {
                        if (isAdmin) {
                            AdminPanelScreen(
                                configState = configState,
                                allSubmissionsState = allSubmissionsForAdminState,
                                isBusy = isBusy,
                                onSaveSettings = viewModel::saveAdminCompetitionSettings,
                                onPublishResults = viewModel::publishResultSheet,
                                onUnpublishResults = viewModel::unpublishResults,
                                onDeleteSubmission = viewModel::deleteSubmission,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    AppTab.PROFILE -> ProfileScreen(
                        displayName = displayName,
                        email = email,
                        isAdmin = isAdmin,
                        mySubmissionsState = mySubmissionsState,
                        onSignOutClick = onSignOut,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

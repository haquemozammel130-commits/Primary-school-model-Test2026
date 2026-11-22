package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.bank.QuizQuestionBank
import com.example.data.firebase.QuizRepository
import com.example.data.model.CompetitionConfig
import com.example.data.model.McqQuestion
import com.example.data.model.PublishedEntry
import com.example.data.model.PublishedSheet
import com.example.data.model.QuizSubmission
import com.example.data.model.UiState
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ActiveExamSession(
    val modelNumber: Int,
    val roundNumber: Int,
    val participantName: String,
    val schoolName: String,
    val rollNumber: String,
    val questions: List<McqQuestion>,
    val answers: List<Int> = List(100) { -1 },
    val elapsedSeconds: Int = 0
)

class MainViewModel(
    private val repository: QuizRepository,
    val currentUserId: String,
    defaultDisplayName: String,
    val userEmail: String
) : ViewModel() {

    private val _isAdmin = MutableStateFlow(repository.isBootstrappedAdmin())
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _participantName = MutableStateFlow(defaultDisplayName.ifBlank { "" })
    val participantName: StateFlow<String> = _participantName.asStateFlow()

    private val _schoolName = MutableStateFlow("")
    val schoolName: StateFlow<String> = _schoolName.asStateFlow()

    private val _rollNumber = MutableStateFlow("")
    val rollNumber: StateFlow<String> = _rollNumber.asStateFlow()

    private val _activeExam = MutableStateFlow<ActiveExamSession?>(null)
    val activeExam: StateFlow<ActiveExamSession?> = _activeExam.asStateFlow()

    private val _completedSubmission = MutableStateFlow<QuizSubmission?>(null)
    val completedSubmission: StateFlow<QuizSubmission?> = _completedSubmission.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    private var timerJob: Job? = null

    val configState: StateFlow<UiState<CompetitionConfig?>> = repository.observeCompetitionConfig()
        .map<CompetitionConfig?, UiState<CompetitionConfig?>> { UiState.Success(it) }
        .catch { error -> emit(unwrapFlowError("config", error)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val mySubmissionsState: StateFlow<UiState<List<QuizSubmission>>> =
        repository.observeMySubmissions(currentUserId)
            .map<List<QuizSubmission>, UiState<List<QuizSubmission>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("mySubmissions", error)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val publishedSheetsState: StateFlow<UiState<List<PublishedSheet>>> =
        repository.observePublishedSheets()
            .map<List<PublishedSheet>, UiState<List<PublishedSheet>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("publishedSheets", error)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    @Suppress("EXPERIMENTAL_API_USAGE")
    val allSubmissionsForAdminState: StateFlow<UiState<List<QuizSubmission>>> =
        isAdmin.flatMapLatest { admin ->
            if (!admin) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAllSubmissionsForAdmin()
                    .map<List<QuizSubmission>, UiState<List<QuizSubmission>>> { UiState.Success(it) }
                    .catch { error -> emit(unwrapFlowError("adminSubmissions", error)) }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            val verified = repository.verifyAdminStatus(currentUserId)
            _isAdmin.value = verified
            if (verified) {
                repository.ensureDefaultConfigIfAdmin()
            }
        }
    }

    fun observePublishedEntries(sheetId: String): Flow<UiState<List<PublishedEntry>>> {
        return repository.observePublishedEntries(sheetId)
            .map<List<PublishedEntry>, UiState<List<PublishedEntry>>> { UiState.Success(it) }
            .catch { error -> emit(unwrapFlowError("publishedEntries", error)) }
    }

    fun updateParticipantForm(name: String, school: String, roll: String) {
        _participantName.value = name
        _schoolName.value = school
        _rollNumber.value = roll
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(message: String) {
        _feedbackMessage.value = message
    }

    fun startExam(modelNumber: Int, roundNumber: Int) {
        val cleanName = _participantName.value.trim()
        val cleanSchool = _schoolName.value.trim()
        val cleanRoll = _rollNumber.value.trim()
        if (cleanName.isEmpty() || cleanSchool.isEmpty() || cleanRoll.isEmpty()) {
            _feedbackMessage.value = "পরীক্ষা শুরু করতে অনুগ্রহ করে নাম, বিদ্যালয়ের নাম এবং রোল নম্বর লিখুন।"
            return
        }
        val questions = QuizQuestionBank.getQuestionsForModel(modelNumber)
        _completedSubmission.value = null
        _activeExam.value = ActiveExamSession(
            modelNumber = modelNumber,
            roundNumber = roundNumber,
            participantName = cleanName,
            schoolName = cleanSchool,
            rollNumber = cleanRoll,
            questions = questions,
            answers = List(100) { -1 },
            elapsedSeconds = 0
        )
        startTimer()
    }

    fun selectAnswer(questionIndex: Int, optionIndex: Int) {
        val current = _activeExam.value ?: return
        if (questionIndex !in current.answers.indices || optionIndex !in 0..3) return
        val updated = current.answers.toMutableList()
        updated[questionIndex] = optionIndex
        _activeExam.value = current.copy(answers = updated)
    }

    fun cancelActiveExam() {
        stopTimer()
        _activeExam.value = null
    }

    fun submitActiveExam() {
        val session = _activeExam.value ?: return
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.submitQuizForm(
                participantName = session.participantName,
                schoolName = session.schoolName,
                rollNumber = session.rollNumber,
                modelNumber = session.modelNumber,
                roundNumber = session.roundNumber,
                answers = session.answers,
                questions = session.questions,
                durationSeconds = session.elapsedSeconds
            )
            _isBusy.value = false
            result.onSuccess { submitted ->
                stopTimer()
                _activeExam.value = null
                _completedSubmission.value = submitted
                _feedbackMessage.value = "উত্তরপত্র সফলভাবে জমা হয়েছে! এখন PDF ডাউনলোড করতে পারেন।"
            }.onFailure { err ->
                _feedbackMessage.value = "জমা দিতে সমস্যা হয়েছে: ${err.localizedMessage ?: "অনুগ্রহ করে পুনরায় চেষ্টা করুন"}"
            }
        }
    }

    fun openSubmissionSummary(submission: QuizSubmission) {
        _completedSubmission.value = submission
    }

    fun closeSubmissionSummary() {
        _completedSubmission.value = null
    }

    // --- Admin Actions ---

    fun saveAdminCompetitionSettings(
        activeModelNumber: Int,
        competitionOpen: Boolean,
        roundNumber: Int,
        examTitle: String,
        notice: String,
        currentConfig: CompetitionConfig?
    ) {
        if (!_isAdmin.value || _isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateCompetitionConfig(
                activeModelNumber = activeModelNumber,
                competitionOpen = competitionOpen,
                roundNumber = roundNumber,
                examTitle = examTitle,
                notice = notice,
                resultsPublished = currentConfig?.resultsPublished ?: false,
                publishedModelNumber = currentConfig?.publishedModelNumber ?: 0,
                publishedRoundNumber = currentConfig?.publishedRoundNumber ?: 0
            )
            _isBusy.value = false
            result.onSuccess {
                _feedbackMessage.value = "প্রতিযোগিতার সেটিংস সংরক্ষণ করা হয়েছে।"
            }.onFailure { err ->
                _feedbackMessage.value = "সেটিংস সংরক্ষণ ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}"
            }
        }
    }

    fun publishResultSheet(
        modelNumber: Int,
        roundNumber: Int,
        examTitle: String,
        allSubmissions: List<QuizSubmission>,
        currentConfig: CompetitionConfig
    ) {
        if (!_isAdmin.value || _isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.publishResultSheet(
                modelNumber = modelNumber,
                roundNumber = roundNumber,
                examTitle = examTitle,
                submissions = allSubmissions,
                currentConfig = currentConfig
            )
            _isBusy.value = false
            result.onSuccess { sheet ->
                _feedbackMessage.value = "মডেল ${QuizQuestionBank.toBanglaDigits(sheet.modelNumber)}-এর রেজাল্ট শিট (${QuizQuestionBank.toBanglaDigits(sheet.totalParticipants)} জন) সকল ইউজার অ্যাপে পাবলিশ করা হয়েছে!"
            }.onFailure { err ->
                _feedbackMessage.value = "রেজাল্ট পাবলিশ ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}"
            }
        }
    }

    fun unpublishResults() {
        if (!_isAdmin.value || _isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.unpublishResults()
            _isBusy.value = false
            result.onSuccess {
                _feedbackMessage.value = "রেজাল্ট শিট পাবলিশ বন্ধ করা হয়েছে।"
            }.onFailure { err ->
                _feedbackMessage.value = "অপারেশন ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}"
            }
        }
    }

    fun deleteSubmission(submissionId: String) {
        if (!_isAdmin.value || _isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteSubmission(submissionId)
            _isBusy.value = false
            result.onSuccess {
                _feedbackMessage.value = "সাবমিশন মুছে ফেলা হয়েছে।"
            }.onFailure { err ->
                _feedbackMessage.value = "মুছতে ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}"
            }
        }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _activeExam.value ?: break
                _activeExam.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }

    private fun unwrapFlowError(tag: String, error: Throwable): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Stream error [$tag]", rootError)
        return UiState.Error(rootError.localizedMessage ?: "ডাটা লোড করতে সমস্যা হয়েছে")
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

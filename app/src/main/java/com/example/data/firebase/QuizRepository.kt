package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.data.model.CompetitionConfig
import com.example.data.model.McqQuestion
import com.example.data.model.PublishedEntry
import com.example.data.model.PublishedSheet
import com.example.data.model.QuizSubmission
import com.google.firebase.Timestamp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class QuizRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "haquemozammel130@gmail.com"
        private const val CONFIG_COLLECTION = "competition_config"
        private const val CONFIG_DOC_ID = "current"
        private const val SUBMISSIONS_COLLECTION = "submissions"
        private const val PUBLISHED_SHEETS_COLLECTION = "published_sheets"
        private const val ENTRIES_SUBCOLLECTION = "entries"
        private const val ADMINS_COLLECTION = "admins"
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google Sign-In required before accessing Firestore.")
    }

    fun isBootstrappedAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true)
    }

    suspend fun verifyAdminStatus(uid: String): Boolean {
        if (uid.isBlank()) return false
        if (isBootstrappedAdmin()) return true
        return try {
            val doc = db.collection(ADMINS_COLLECTION).document(uid).get().await()
            doc.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "$ADMINS_COLLECTION/$uid")
            false
        }
    }

    fun observeCompetitionConfig(): Flow<CompetitionConfig?> = flow {
        val uid = requireUserId()
        val path = "$CONFIG_COLLECTION/$CONFIG_DOC_ID"
        emitAll(
            db.collection(CONFIG_COLLECTION).document(CONFIG_DOC_ID)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CompetitionConfig::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun ensureDefaultConfigIfAdmin(): Result<Unit> {
        val path = "$CONFIG_COLLECTION/$CONFIG_DOC_ID"
        return try {
            val uid = requireUserId()
            if (!verifyAdminStatus(uid)) return Result.success(Unit)
            val docRef = db.collection(CONFIG_COLLECTION).document(CONFIG_DOC_ID)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "activeModelNumber" to 1,
                    "competitionOpen" to true,
                    "roundNumber" to 1,
                    "examTitle" to "প্রাইমারি স্কুল টেস্ট পরীক্ষা",
                    "notice" to "১০০ প্রশ্নের MCQ প্রতিযোগিতায় স্বাগতম। সঠিক উত্তর দিয়ে ফর্ম সাবমিট করুন।",
                    "resultsPublished" to false,
                    "publishedModelNumber" to 0,
                    "publishedRoundNumber" to 0,
                    "updatedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCompetitionConfig(
        activeModelNumber: Int,
        competitionOpen: Boolean,
        roundNumber: Int,
        examTitle: String,
        notice: String,
        resultsPublished: Boolean,
        publishedModelNumber: Int,
        publishedRoundNumber: Int
    ): Result<Unit> {
        val path = "$CONFIG_COLLECTION/$CONFIG_DOC_ID"
        return try {
            val uid = requireUserId()
            require(activeModelNumber in 1..20) { "Model number must be between 1 and 20." }
            require(roundNumber in 1..1000) { "Round number must be between 1 and 1000." }
            val trimmedTitle = examTitle.trim().ifEmpty { "প্রাইমারি স্কুল টেস্ট পরীক্ষা" }.take(120)
            val trimmedNotice = notice.trim().take(500)

            val docRef = db.collection(CONFIG_COLLECTION).document(CONFIG_DOC_ID)
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val createPayload = mapOf(
                    "activeModelNumber" to activeModelNumber,
                    "competitionOpen" to competitionOpen,
                    "roundNumber" to roundNumber,
                    "examTitle" to trimmedTitle,
                    "notice" to trimmedNotice,
                    "resultsPublished" to resultsPublished,
                    "publishedModelNumber" to publishedModelNumber.coerceIn(0, 20),
                    "publishedRoundNumber" to publishedRoundNumber.coerceIn(0, 1000),
                    "updatedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "activeModelNumber" to activeModelNumber,
                    "competitionOpen" to competitionOpen,
                    "roundNumber" to roundNumber,
                    "examTitle" to trimmedTitle,
                    "notice" to trimmedNotice,
                    "resultsPublished" to resultsPublished,
                    "publishedModelNumber" to publishedModelNumber.coerceIn(0, 20),
                    "publishedRoundNumber" to publishedRoundNumber.coerceIn(0, 1000),
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeMySubmissions(userId: String): Flow<List<QuizSubmission>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Authenticated user mismatch." }
        val path = SUBMISSIONS_COLLECTION
        emitAll(
            db.collection(SUBMISSIONS_COLLECTION)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuizSubmission::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAllSubmissionsForAdmin(): Flow<List<QuizSubmission>> = flow {
        val uid = requireUserId()
        val path = SUBMISSIONS_COLLECTION
        emitAll(
            db.collection(SUBMISSIONS_COLLECTION)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuizSubmission::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<QuizSubmission> { it.score }
                            .thenBy { it.durationSeconds }
                            .thenBy { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    suspend fun submitQuizForm(
        participantName: String,
        schoolName: String,
        rollNumber: String,
        modelNumber: Int,
        roundNumber: Int,
        answers: List<Int>,
        questions: List<McqQuestion>,
        durationSeconds: Int
    ): Result<QuizSubmission> {
        var docPath = SUBMISSIONS_COLLECTION
        return try {
            val uid = requireUserId()
            val cleanName = participantName.trim()
            val cleanSchool = schoolName.trim()
            val cleanRoll = rollNumber.trim()
            require(cleanName.length in 1..80) { "পরীক্ষার্থীর নাম ১ থেকে ৮০ অক্ষরের মধ্যে হতে হবে।" }
            require(cleanSchool.length in 1..120) { "বিদ্যালয়ের নাম ১ থেকে ১২০ অক্ষরের মধ্যে হতে হবে।" }
            require(cleanRoll.length in 1..40) { "রোল নম্বর ১ থেকে ৪০ অক্ষরের মধ্যে হতে হবে।" }
            require(modelNumber in 1..20) { "মডেল নম্বর সঠিক নয়।" }
            require(roundNumber in 1..1000) { "রাউন্ড নম্বর সঠিক নয়।" }
            require(answers.size == 100 && questions.size == 100) { "১০০টি প্রশ্নের উত্তর ফরম্যাট সঠিক নয়।" }

            var correctCount = 0
            var wrongCount = 0
            var unansweredCount = 0
            val normalizedAnswers = answers.mapIndexed { idx, chosen ->
                val sanitized = if (chosen in 0..3) chosen else -1
                when {
                    sanitized == -1 -> unansweredCount++
                    sanitized == questions[idx].correctIndex -> correctCount++
                    else -> wrongCount++
                }
                sanitized
            }
            val score = correctCount
            val safeDuration = durationSeconds.coerceIn(0, 14400)

            val docId = "${uid}_m${modelNumber}_r${roundNumber}"
            val docRef = db.collection(SUBMISSIONS_COLLECTION).document(docId)
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "participantName" to cleanName,
                "schoolName" to cleanSchool,
                "rollNumber" to cleanRoll,
                "modelNumber" to modelNumber,
                "roundNumber" to roundNumber,
                "answers" to normalizedAnswers,
                "score" to score,
                "correctCount" to correctCount,
                "wrongCount" to wrongCount,
                "unansweredCount" to unansweredCount,
                "durationSeconds" to safeDuration,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()

            val resultModel = QuizSubmission(
                id = docId,
                userId = uid,
                participantName = cleanName,
                schoolName = cleanSchool,
                rollNumber = cleanRoll,
                modelNumber = modelNumber,
                roundNumber = roundNumber,
                answers = normalizedAnswers,
                score = score,
                correctCount = correctCount,
                wrongCount = wrongCount,
                unansweredCount = unansweredCount,
                durationSeconds = safeDuration,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            Result.success(resultModel)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSubmission(submissionId: String): Result<Unit> {
        val docPath = "$SUBMISSIONS_COLLECTION/$submissionId"
        return try {
            requireUserId()
            require(submissionId.isNotBlank()) { "Submission ID must not be blank." }
            db.collection(SUBMISSIONS_COLLECTION).document(submissionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun publishResultSheet(
        modelNumber: Int,
        roundNumber: Int,
        examTitle: String,
        submissions: List<QuizSubmission>,
        currentConfig: CompetitionConfig
    ): Result<PublishedSheet> {
        val sheetId = "m${modelNumber}_r${roundNumber}"
        val sheetPath = "$PUBLISHED_SHEETS_COLLECTION/$sheetId"
        return try {
            val uid = requireUserId()
            require(modelNumber in 1..20) { "Model number must be 1..20." }
            require(roundNumber in 1..1000) { "Round number must be 1..1000." }

            val filtered = submissions
                .filter { it.modelNumber == modelNumber && it.roundNumber == roundNumber }
                .sortedWith(
                    compareByDescending<QuizSubmission> { it.score }
                        .thenBy { it.durationSeconds }
                        .thenBy { it.createdAt ?: Timestamp(0, 0) }
                )

            val totalParticipants = filtered.size
            val highestScore = filtered.maxOfOrNull { it.score } ?: 0
            val averageScore = if (totalParticipants > 0) {
                filtered.sumOf { it.score }.toDouble() / totalParticipants.toDouble()
            } else {
                0.0
            }

            val sheetRef = db.collection(PUBLISHED_SHEETS_COLLECTION).document(sheetId)
            val existingSheet = sheetRef.get().await()
            val cleanTitle = examTitle.trim().ifEmpty { "প্রাইমারি স্কুল টেস্ট পরীক্ষা" }.take(120)

            // Delete stale entries from a previous publish of the same sheet if any
            val existingEntries = sheetRef.collection(ENTRIES_SUBCOLLECTION).get().await()
            if (!existingEntries.isEmpty) {
                existingEntries.documents.chunked(400).forEach { chunk ->
                    val delBatch = db.batch()
                    chunk.forEach { delBatch.delete(it.reference) }
                    delBatch.commit().await()
                }
            }

            // Write sheet metadata + ranked entries atomically in batches (chunked safely <= 400)
            val firstBatch = db.batch()
            if (!existingSheet.exists()) {
                firstBatch.set(
                    sheetRef,
                    mapOf(
                        "modelNumber" to modelNumber,
                        "roundNumber" to roundNumber,
                        "examTitle" to cleanTitle,
                        "published" to true,
                        "totalParticipants" to totalParticipants,
                        "highestScore" to highestScore,
                        "averageScore" to averageScore,
                        "publishedBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                firstBatch.update(
                    sheetRef,
                    mapOf(
                        "modelNumber" to modelNumber,
                        "roundNumber" to roundNumber,
                        "examTitle" to cleanTitle,
                        "published" to true,
                        "totalParticipants" to totalParticipants,
                        "highestScore" to highestScore,
                        "averageScore" to averageScore,
                        "publishedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val firstChunk = filtered.take(400)
            firstChunk.forEachIndexed { index, sub ->
                val entryRef = sheetRef.collection(ENTRIES_SUBCOLLECTION).document(sub.userId)
                firstBatch.set(entryRef, buildPublishedEntryMap(sheetId, index + 1, sub))
            }

            // Also update global competition_config so all user apps know results are live
            val configRef = db.collection(CONFIG_COLLECTION).document(CONFIG_DOC_ID)
            val configSnap = configRef.get().await()
            if (!configSnap.exists()) {
                firstBatch.set(
                    configRef,
                    mapOf(
                        "activeModelNumber" to currentConfig.activeModelNumber.coerceIn(1, 20),
                        "competitionOpen" to currentConfig.competitionOpen,
                        "roundNumber" to currentConfig.roundNumber.coerceIn(1, 1000),
                        "examTitle" to cleanTitle,
                        "notice" to currentConfig.notice.take(500),
                        "resultsPublished" to true,
                        "publishedModelNumber" to modelNumber,
                        "publishedRoundNumber" to roundNumber,
                        "updatedBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                firstBatch.update(
                    configRef,
                    mapOf(
                        "resultsPublished" to true,
                        "publishedModelNumber" to modelNumber,
                        "publishedRoundNumber" to roundNumber,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            firstBatch.commit().await()

            if (filtered.size > 400) {
                filtered.drop(400).chunked(400).forEachIndexed { chunkIdx, chunk ->
                    val batch = db.batch()
                    chunk.forEachIndexed { offset, sub ->
                        val rank = 400 + chunkIdx * 400 + offset + 1
                        val entryRef = sheetRef.collection(ENTRIES_SUBCOLLECTION).document(sub.userId)
                        batch.set(entryRef, buildPublishedEntryMap(sheetId, rank, sub))
                    }
                    batch.commit().await()
                }
            }

            Result.success(
                PublishedSheet(
                    id = sheetId,
                    modelNumber = modelNumber,
                    roundNumber = roundNumber,
                    examTitle = cleanTitle,
                    published = true,
                    totalParticipants = totalParticipants,
                    highestScore = highestScore,
                    averageScore = averageScore,
                    publishedBy = uid,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sheetPath)
            Result.failure(e)
        }
    }

    private fun buildPublishedEntryMap(
        sheetId: String,
        rank: Int,
        sub: QuizSubmission
    ): Map<String, Any> {
        return mapOf(
            "sheetId" to sheetId,
            "userId" to sub.userId,
            "rank" to rank,
            "participantName" to sub.participantName.take(80),
            "schoolName" to sub.schoolName.take(120),
            "rollNumber" to sub.rollNumber.take(40),
            "modelNumber" to sub.modelNumber,
            "roundNumber" to sub.roundNumber,
            "score" to sub.score,
            "correctCount" to sub.correctCount,
            "wrongCount" to sub.wrongCount,
            "unansweredCount" to sub.unansweredCount,
            "durationSeconds" to sub.durationSeconds,
            "published" to true,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    suspend fun unpublishResults(): Result<Unit> {
        val path = "$CONFIG_COLLECTION/$CONFIG_DOC_ID"
        return try {
            val uid = requireUserId()
            db.collection(CONFIG_COLLECTION).document(CONFIG_DOC_ID)
                .update(
                    mapOf(
                        "resultsPublished" to false,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observePublishedSheets(): Flow<List<PublishedSheet>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection(PUBLISHED_SHEETS_COLLECTION)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PublishedSheet::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, PUBLISHED_SHEETS_COLLECTION, uid)
                    throw error
                }
        )
    }

    fun observePublishedEntries(sheetId: String): Flow<List<PublishedEntry>> = flow {
        val uid = requireUserId()
        val path = "$PUBLISHED_SHEETS_COLLECTION/$sheetId/$ENTRIES_SUBCOLLECTION"
        emitAll(
            db.collection(PUBLISHED_SHEETS_COLLECTION)
                .document(sheetId)
                .collection(ENTRIES_SUBCOLLECTION)
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PublishedEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.rank }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    private fun handleFlowError(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
    }
}

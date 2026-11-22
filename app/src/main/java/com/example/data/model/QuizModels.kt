package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class QuizSubject(val titleBn: String, val shortBn: String) {
    BANGLA("বাংলা", "বাংলা"),
    ENGLISH("ইংরেজি", "ইংরেজি"),
    MATH("গণিত", "গণিত"),
    GENERAL_KNOWLEDGE("সাধারণ জ্ঞান ও বিজ্ঞান", "সা. জ্ঞান")
}

data class McqQuestion(
    val number: Int,
    val subject: QuizSubject,
    val question: String,
    val options: List<String>,
    val correctIndex: Int
)

data class QuizModelInfo(
    val modelNumber: Int,
    val titleBn: String,
    val subtitleBn: String,
    val totalMarks: Int = 100
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class CompetitionConfig(
    @DocumentId val id: String = "",
    val activeModelNumber: Int = 1,
    val competitionOpen: Boolean = false,
    val roundNumber: Int = 1,
    val examTitle: String = "প্রাইমারি স্কুল টেস্ট পরীক্ষা",
    val notice: String = "প্রতিযোগিতায় অংশগ্রহণের জন্য সকলকে স্বাগতম।",
    val resultsPublished: Boolean = false,
    val publishedModelNumber: Int = 0,
    val publishedRoundNumber: Int = 0,
    val updatedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizSubmission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val participantName: String = "",
    val schoolName: String = "",
    val rollNumber: String = "",
    val modelNumber: Int = 1,
    val roundNumber: Int = 1,
    val answers: List<Int> = List(100) { -1 },
    val score: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val unansweredCount: Int = 100,
    val durationSeconds: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublishedSheet(
    @DocumentId val id: String = "",
    val modelNumber: Int = 1,
    val roundNumber: Int = 1,
    val examTitle: String = "",
    val published: Boolean = true,
    val totalParticipants: Int = 0,
    val highestScore: Int = 0,
    val averageScore: Double = 0.0,
    val publishedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublishedEntry(
    @DocumentId val id: String = "",
    val sheetId: String = "",
    val userId: String = "",
    val rank: Int = 1,
    val participantName: String = "",
    val schoolName: String = "",
    val rollNumber: String = "",
    val modelNumber: Int = 1,
    val roundNumber: Int = 1,
    val score: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val unansweredCount: Int = 0,
    val durationSeconds: Int = 0,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)

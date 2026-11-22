package com.example

import com.example.data.bank.QuizQuestionBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun all20ModelsHave100ValidUniqueQuestions() {
        assertEquals(20, QuizQuestionBank.allModels.size)
        for (modelNumber in 1..20) {
            val questions = QuizQuestionBank.getQuestionsForModel(modelNumber)
            assertEquals("Model $modelNumber should have 100 questions", 100, questions.size)
            val distinctPrompts = questions.map { it.question }.toSet()
            assertEquals("Model $modelNumber should have 100 unique prompts", 100, distinctPrompts.size)
            questions.forEachIndexed { idx, q ->
                assertEquals(idx + 1, q.number)
                assertEquals(4, q.options.size)
                assertTrue(q.correctIndex in 0..3)
                assertEquals(4, q.options.toSet().size)
            }
        }
    }
}

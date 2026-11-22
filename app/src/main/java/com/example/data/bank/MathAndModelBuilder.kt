package com.example.data.bank

import com.example.data.model.McqQuestion
import com.example.data.model.QuizModelInfo
import com.example.data.model.QuizSubject
import kotlin.random.Random

object QuizQuestionBank {

    val allModels: List<QuizModelInfo> = (1..20).map { modelNum ->
        val topicHint = when ((modelNum - 1) % 4) {
            0 -> "পূর্ণাঙ্গ মূল্যায়ন সেট"
            1 -> "বৃত্তি ও মেধা যাচাই সেট"
            2 -> "চূড়ান্ত অনুশীলন সেট"
            else -> "বিশেষ প্রতিযোগিতা সেট"
        }
        QuizModelInfo(
            modelNumber = modelNum,
            titleBn = "মডেল টেস্ট - ${toBanglaDigits(modelNum.toString().padStart(2, '0'))}",
            subtitleBn = "$topicHint • ১০০টি MCQ • পূর্ণমান ১০০",
            totalMarks = 100
        )
    }

    fun getModelInfo(modelNumber: Int): QuizModelInfo {
        val safeModel = modelNumber.coerceIn(1, 20)
        return allModels[safeModel - 1]
    }

    fun getQuestionsForModel(modelNumber: Int): List<McqQuestion> {
        val safeModel = modelNumber.coerceIn(1, 20)
        val rng = Random(safeModel * 7919L + 104729L)

        // Pick 25 unique Bangla, 25 unique English, 25 unique GK, and 25 unique Math = 100 MCQs
        val bangla = pickAndShuffle(BanglaEnglishBank.banglaQuestions, 25, rng)
        val english = pickAndShuffle(BanglaEnglishBank.englishQuestions, 25, rng)
        val gk = pickAndShuffle(GkScienceBank.gkScienceQuestions, 25, rng)
        val math = generateMathQuestionsForModel(safeModel, 25, rng)

        val combined = bangla + english + math + gk
        return combined.mapIndexed { index, raw ->
            val shuffled = shuffleOptions(raw.options, raw.correctIndex, rng)
            McqQuestion(
                number = index + 1,
                subject = raw.subject,
                question = raw.question,
                options = shuffled.first,
                correctIndex = shuffled.second
            )
        }
    }

    private fun pickAndShuffle(
        pool: List<RawQuestion>,
        count: Int,
        rng: Random
    ): List<RawQuestion> {
        return pool.shuffled(rng).take(count)
    }

    private fun shuffleOptions(
        options: List<String>,
        correctIndex: Int,
        rng: Random
    ): Pair<List<String>, Int> {
        val indexed = options.mapIndexed { idx, text -> idx to text }.shuffled(rng)
        val newCorrect = indexed.indexOfFirst { it.first == correctIndex }.coerceAtLeast(0)
        return indexed.map { it.second } to newCorrect
    }

    private fun generateMathQuestionsForModel(
        modelNumber: Int,
        count: Int,
        rng: Random
    ): List<RawQuestion> {
        val questions = mutableListOf<RawQuestion>()
        val usedPrompts = HashSet<String>()
        var seedOffset = 0

        while (questions.size < count) {
            seedOffset++
            val type = (questions.size + modelNumber + seedOffset) % 10
            val candidate = when (type) {
                0 -> {
                    // Addition & subtraction word/numeric problem
                    val a = 120 + ((modelNumber * 37 + seedOffset * 19) % 450)
                    val b = 45 + ((modelNumber * 23 + seedOffset * 13) % 280)
                    val c = 15 + ((modelNumber * 11 + seedOffset * 7) % 80)
                    val ans = a + b - c
                    buildNumericMath(
                        "${toBanglaDigits(a)} + ${toBanglaDigits(b)} − ${toBanglaDigits(c)} = কত?",
                        ans,
                        listOf(ans + 10, ans - 10, ans + 5),
                        useBangla = true
                    )
                }
                1 -> {
                    // Multiplication
                    val a = 14 + ((modelNumber * 7 + seedOffset * 3) % 35)
                    val b = 12 + ((modelNumber * 5 + seedOffset * 4) % 25)
                    val ans = a * b
                    buildNumericMath(
                        "${toBanglaDigits(a)} × ${toBanglaDigits(b)} = কত?",
                        ans,
                        listOf(ans + a, ans - b, ans + 12),
                        useBangla = true
                    )
                }
                2 -> {
                    // Division
                    val divisor = 6 + ((modelNumber + seedOffset) % 14)
                    val quotient = 12 + ((modelNumber * 3 + seedOffset * 5) % 40)
                    val dividend = divisor * quotient
                    buildNumericMath(
                        "${toBanglaDigits(dividend)} ÷ ${toBanglaDigits(divisor)} = কত?",
                        quotient,
                        listOf(quotient + 2, quotient - 2, quotient + 4),
                        useBangla = true
                    )
                }
                3 -> {
                    // Average of 3 numbers
                    val base = 20 + ((modelNumber * 6 + seedOffset * 9) % 50)
                    val x = base
                    val y = base + 6
                    val z = base + 12
                    val ans = base + 6
                    buildNumericMath(
                        "${toBanglaDigits(x)}, ${toBanglaDigits(y)} এবং ${toBanglaDigits(z)}-এর গড় কত?",
                        ans,
                        listOf(ans + 3, ans - 3, ans + 6),
                        useBangla = true
                    )
                }
                4 -> {
                    // Percentage
                    val pct = listOf(10, 20, 25, 40, 50, 75)[(modelNumber + seedOffset) % 6]
                    val whole = listOf(100, 200, 300, 400, 500, 600, 800)[(modelNumber * 2 + seedOffset) % 7]
                    val ans = (pct * whole) / 100
                    buildNumericMath(
                        "${toBanglaDigits(whole)}-এর ${toBanglaDigits(pct)}% কত?",
                        ans,
                        listOf(ans + 10, (ans / 2).coerceAtLeast(1), ans + 25),
                        useBangla = true
                    )
                }
                5 -> {
                    // Rectangle area / perimeter
                    val length = 10 + ((modelNumber * 3 + seedOffset * 2) % 20)
                    val width = 5 + ((modelNumber * 2 + seedOffset) % 12)
                    if (seedOffset % 2 == 0) {
                        val area = length * width
                        buildNumericMath(
                            "একটি আয়তক্ষেত্রের দৈর্ঘ্য ${toBanglaDigits(length)} মিটার ও প্রস্থ ${toBanglaDigits(width)} মিটার হলে ক্ষেত্রফল কত বর্গমিটার?",
                            area,
                            listOf(2 * (length + width), area + 10, area - 5),
                            useBangla = true
                        )
                    } else {
                        val perimeter = 2 * (length + width)
                        buildNumericMath(
                            "একটি আয়তক্ষেত্রের দৈর্ঘ্য ${toBanglaDigits(length)} সেমি ও প্রস্থ ${toBanglaDigits(width)} সেমি হলে পরিসীমা কত সেমি?",
                            perimeter,
                            listOf(length + width, length * width, perimeter + 4),
                            useBangla = true
                        )
                    }
                }
                6 -> {
                    // Unitary method
                    val items = 4 + ((modelNumber + seedOffset) % 6)
                    val unitCost = 12 + ((modelNumber * 5 + seedOffset * 3) % 25)
                    val totalCost = items * unitCost
                    val askItems = items + 3
                    val ans = askItems * unitCost
                    buildNumericMath(
                        "${toBanglaDigits(items)}টি কলমের দাম ${toBanglaDigits(totalCost)} টাকা হলে, ${toBanglaDigits(askItems)}টি কলমের দাম কত টাকা?",
                        ans,
                        listOf(ans + unitCost, ans - unitCost, totalCost + askItems),
                        useBangla = true
                    )
                }
                7 -> {
                    // Profit / Loss
                    val buy = 200 + ((modelNumber * 25 + seedOffset * 20) % 500)
                    val profit = 30 + ((modelNumber * 10 + seedOffset * 5) % 90)
                    val sell = buy + profit
                    buildNumericMath(
                        "একটি বই ${toBanglaDigits(buy)} টাকায় কিনে ${toBanglaDigits(sell)} টাকায় বিক্রি করলে কত টাকা লাভ হবে?",
                        profit,
                        listOf(profit + 15, (profit - 10).coerceAtLeast(1), sell - 100),
                        useBangla = true
                    )
                }
                8 -> {
                    // Time & Unit conversions
                    when ((modelNumber + seedOffset) % 4) {
                        0 -> {
                            val hrs = 2 + ((modelNumber + seedOffset) % 6)
                            val ans = hrs * 60
                            buildNumericMath(
                                "${toBanglaDigits(hrs)} ঘণ্টায় কত মিনিট?",
                                ans,
                                listOf(ans + 30, ans - 30, hrs * 100),
                                useBangla = true
                            )
                        }
                        1 -> {
                            val kg = 3 + ((modelNumber + seedOffset) % 7)
                            val ans = kg * 1000
                            buildNumericMath(
                                "${toBanglaDigits(kg)} কিলোগ্রামে কত গ্রাম?",
                                ans,
                                listOf(kg * 100, ans + 500, ans - 500),
                                useBangla = true
                            )
                        }
                        2 -> {
                            val km = 2 + ((modelNumber + seedOffset) % 8)
                            val ans = km * 1000
                            buildNumericMath(
                                "${toBanglaDigits(km)} কিলোমিটার সমান কত মিটার?",
                                ans,
                                listOf(km * 100, ans + 200, km * 500),
                                useBangla = true
                            )
                        }
                        else -> {
                            val dozen = 2 + ((modelNumber + seedOffset) % 5)
                            val ans = dozen * 12
                            buildNumericMath(
                                "${toBanglaDigits(dozen)} ডজন সমান কয়টি?",
                                ans,
                                listOf(dozen * 10, ans + 6, ans - 4),
                                useBangla = true
                            )
                        }
                    }
                }
                else -> {
                    // Triangle / Angle / Square
                    when ((modelNumber + seedOffset) % 3) {
                        0 -> {
                            val a1 = 45 + ((modelNumber * 3 + seedOffset * 2) % 40)
                            val a2 = 50 + ((modelNumber * 2 + seedOffset) % 35)
                            val ans = 180 - (a1 + a2)
                            buildNumericMath(
                                "একটি ত্রিভুজের দুটি কোণ ${toBanglaDigits(a1)}° ও ${toBanglaDigits(a2)}° হলে তৃতীয় কোণটি কত ডিগ্রি?",
                                ans,
                                listOf(ans + 10, (ans - 10).coerceAtLeast(5), 90),
                                useBangla = true
                            )
                        }
                        1 -> {
                            val side = 6 + ((modelNumber + seedOffset) % 14)
                            val ans = side * side
                            buildNumericMath(
                                "একটি বর্গক্ষেত্রের এক বাহুর দৈর্ঘ্য ${toBanglaDigits(side)} সেমি হলে ক্ষেত্রফল কত বর্গসেমি?",
                                ans,
                                listOf(side * 4, ans + side, ans - 4),
                                useBangla = true
                            )
                        }
                        else -> {
                            val n = 10 + ((modelNumber + seedOffset) % 15)
                            val ans = n * n
                            buildNumericMath(
                                "${toBanglaDigits(n)}-এর বর্গ কত?",
                                ans,
                                listOf(n * 2, ans + 10, ans - 10),
                                useBangla = true
                            )
                        }
                    }
                }
            }

            if (usedPrompts.add(candidate.question)) {
                questions.add(candidate)
            }
        }
        return questions
    }

    private fun buildNumericMath(
        prompt: String,
        correctValue: Int,
        rawDistractors: List<Int>,
        useBangla: Boolean
    ): RawQuestion {
        val distinctDistractors = mutableListOf<Int>()
        for (d in rawDistractors) {
            if (d != correctValue && d >= 0 && d !in distinctDistractors) {
                distinctDistractors.add(d)
            }
        }
        var offset = 1
        while (distinctDistractors.size < 3) {
            val candidate = correctValue + offset
            if (candidate != correctValue && candidate !in distinctDistractors) {
                distinctDistractors.add(candidate)
            }
            offset++
        }
        val values = listOf(correctValue) + distinctDistractors.take(3)
        val options = values.map { if (useBangla) toBanglaDigits(it) else it.toString() }
        return RawQuestion(
            subject = QuizSubject.MATH,
            question = prompt,
            options = options,
            correctIndex = 0
        )
    }

    fun toBanglaDigits(number: Int): String = toBanglaDigits(number.toString())

    fun toBanglaDigits(input: String): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }
}

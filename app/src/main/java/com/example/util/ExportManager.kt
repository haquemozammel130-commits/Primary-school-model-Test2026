package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.bank.QuizQuestionBank
import com.example.data.model.McqQuestion
import com.example.data.model.PublishedEntry
import com.example.data.model.QuizSubmission
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private val OPTION_LABELS = listOf("ক", "খ", "গ", "ঘ")

    private fun loadTypeface(context: Context): Typeface {
        return try {
            ResourcesCompat.getFont(context, R.font.hind_siliguri) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    private fun ensureExportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateAnswerSheetPdf(
        context: Context,
        submission: QuizSubmission,
        questions: List<McqQuestion>
    ): File {
        val typeface = loadTypeface(context)
        val pdf = PdfDocument()
        val exportDir = ensureExportDir(context)
        val outFile = File(
            exportDir,
            "uttarpotra_model_${submission.modelNumber}_r${submission.roundNumber}_${submission.rollNumber.replace(Regex("[^a-zA-Z0-9_-]"), "")}.pdf"
        )

        val headerBgPaint = Paint().apply {
            color = Color.rgb(9, 110, 78)
            style = Paint.Style.FILL
        }
        val cardBgPaint = Paint().apply {
            color = Color.rgb(245, 243, 238)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(209, 213, 219)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 252, 231)
            textSize = 11f
            this.typeface = typeface
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 28, 25)
            textSize = 10.5f
            this.typeface = typeface
        }
        val boldBodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 28, 25)
            textSize = 11f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val correctPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(21, 128, 61)
            textSize = 10f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val wrongPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(185, 28, 28)
            textSize = 10f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 114, 128)
            textSize = 9.5f
            this.typeface = typeface
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val submittedDateStr = dateFormat.format(submission.createdAt?.toDate() ?: Date())

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        // Top Banner
        canvas.drawRoundRect(
            RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 64f),
            10f,
            10f,
            headerBgPaint
        )
        canvas.drawText("প্রাইমারি স্কুল টেস্ট পরীক্ষা — উত্তরপত্র", MARGIN + 16f, y + 26f, titlePaint)
        canvas.drawText(
            "মডেল টেস্ট: ${QuizQuestionBank.toBanglaDigits(submission.modelNumber)} | রাউন্ড: ${QuizQuestionBank.toBanglaDigits(submission.roundNumber)} | পূর্ণমান: ১০০",
            MARGIN + 16f,
            y + 46f,
            subHeaderPaint
        )
        y += 78f

        // Examinee Info + Score Summary Card
        canvas.drawRoundRect(
            RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 92f),
            8f,
            8f,
            cardBgPaint
        )
        canvas.drawRoundRect(
            RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 92f),
            8f,
            8f,
            borderPaint
        )
        canvas.drawText("পরীক্ষার্থীর নাম: ${submission.participantName}", MARGIN + 14f, y + 22f, boldBodyPaint)
        canvas.drawText("বিদ্যালয়: ${submission.schoolName}", MARGIN + 14f, y + 40f, bodyPaint)
        canvas.drawText("রোল নম্বর: ${submission.rollNumber}   |   জমা: $submittedDateStr", MARGIN + 14f, y + 58f, bodyPaint)
        val durationMin = submission.durationSeconds / 60
        val durationSec = submission.durationSeconds % 60
        canvas.drawText(
            "প্রাপ্ত নম্বর: ${QuizQuestionBank.toBanglaDigits(submission.score)} / ১০০   |   সঠিক: ${QuizQuestionBank.toBanglaDigits(submission.correctCount)}   |   ভুল: ${QuizQuestionBank.toBanglaDigits(submission.wrongCount)}   |   অনুত্তরিত: ${QuizQuestionBank.toBanglaDigits(submission.unansweredCount)}   |   সময়: ${durationMin}মি ${durationSec}সে",
            MARGIN + 14f,
            y + 78f,
            correctPaint
        )
        y += 108f

        // Table Header
        canvas.drawText("প্রশ্নভিত্তিক উত্তর বিবরণী (১ – ১০০)", MARGIN, y, boldBodyPaint)
        y += 14f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
        y += 12f

        val contentWidth = (PAGE_WIDTH - 2 * MARGIN).toInt()
        for ((idx, q) in questions.withIndex()) {
            val chosen = submission.answers.getOrNull(idx) ?: -1
            val chosenText = if (chosen in 0..3) "${OPTION_LABELS[chosen]}) ${q.options[chosen]}" else "উত্তর দেওয়া হয়নি"
            val correctText = "${OPTION_LABELS[q.correctIndex]}) ${q.options[q.correctIndex]}"
            val statusText = when {
                chosen == -1 -> "অনুত্তরিত"
                chosen == q.correctIndex -> "সঠিক (+১)"
                else -> "ভুল (০)"
            }
            val statusPaint = when {
                chosen == -1 -> mutedPaint
                chosen == q.correctIndex -> correctPaint
                else -> wrongPaint
            }

            val qLayout = StaticLayout.Builder
                .obtain("${QuizQuestionBank.toBanglaDigits(q.number)}. ${q.question}", 0, ("${QuizQuestionBank.toBanglaDigits(q.number)}. ${q.question}").length, boldBodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            val ansLine = "আপনার উত্তর: $chosenText   |   সঠিক উত্তর: $correctText"
            val aLayout = StaticLayout.Builder
                .obtain(ansLine, 0, ansLine.length, bodyPaint, contentWidth - 85)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            val blockHeight = qLayout.height + aLayout.height + 16f
            if (y + blockHeight > PAGE_HEIGHT - MARGIN - 20f) {
                drawFooter(canvas, pageNum, mutedPaint)
                pdf.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
            }

            canvas.save()
            canvas.translate(MARGIN, y)
            qLayout.draw(canvas)
            canvas.restore()
            y += qLayout.height + 2f

            canvas.save()
            canvas.translate(MARGIN + 10f, y)
            aLayout.draw(canvas)
            canvas.restore()

            canvas.drawText(statusText, PAGE_WIDTH - MARGIN - 65f, y + 12f, statusPaint)
            y += aLayout.height + 8f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
            y += 8f
        }

        drawFooter(canvas, pageNum, mutedPaint)
        pdf.finishPage(page)

        FileOutputStream(outFile).use { fos ->
            pdf.writeTo(fos)
        }
        pdf.close()
        return outFile
    }

    fun generateResultSheetPdf(
        context: Context,
        examTitle: String,
        modelNumber: Int,
        roundNumber: Int,
        entries: List<PublishedEntry>
    ): File {
        val typeface = loadTypeface(context)
        val pdf = PdfDocument()
        val exportDir = ensureExportDir(context)
        val outFile = File(exportDir, "result_sheet_model_${modelNumber}_r${roundNumber}.pdf")

        val headerBgPaint = Paint().apply {
            color = Color.rgb(9, 110, 78)
            style = Paint.Style.FILL
        }
        val rowAltPaint = Paint().apply {
            color = Color.rgb(245, 243, 238)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(209, 213, 219)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 252, 231)
            textSize = 11f
            this.typeface = typeface
        }
        val boldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 28, 25)
            textSize = 10.5f
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(26, 28, 25)
            textSize = 10f
            this.typeface = typeface
        }
        val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(107, 114, 128)
            textSize = 9.5f
            this.typeface = typeface
        }

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas
        var y = MARGIN

        // Header
        canvas.drawRoundRect(
            RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 66f),
            10f,
            10f,
            headerBgPaint
        )
        canvas.drawText("$examTitle — চূড়ান্ত রেজাল্ট শিট", MARGIN + 16f, y + 26f, titlePaint)
        val highest = entries.maxOfOrNull { it.score } ?: 0
        canvas.drawText(
            "মডেল টেস্ট: ${QuizQuestionBank.toBanglaDigits(modelNumber)} | রাউন্ড: ${QuizQuestionBank.toBanglaDigits(roundNumber)} | মোট পরীক্ষার্থী: ${QuizQuestionBank.toBanglaDigits(entries.size)} | সর্বোচ্চ নম্বর: ${QuizQuestionBank.toBanglaDigits(highest)}/১০০",
            MARGIN + 16f,
            y + 46f,
            subHeaderPaint
        )
        y += 82f

        fun drawTableHeader() {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 24f, rowAltPaint)
            canvas.drawText("ক্রম", MARGIN + 6f, y + 16f, boldPaint)
            canvas.drawText("নাম", MARGIN + 42f, y + 16f, boldPaint)
            canvas.drawText("বিদ্যালয়", MARGIN + 185f, y + 16f, boldPaint)
            canvas.drawText("রোল", MARGIN + 345f, y + 16f, boldPaint)
            canvas.drawText("নম্বর", MARGIN + 405f, y + 16f, boldPaint)
            canvas.drawText("সঠিক/ভুল", MARGIN + 455f, y + 16f, boldPaint)
            y += 26f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
        }

        drawTableHeader()

        entries.forEachIndexed { idx, entry ->
            if (y + 24f > PAGE_HEIGHT - MARGIN - 20f) {
                drawFooter(canvas, pageNum, mutedPaint)
                pdf.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN
                drawTableHeader()
            }
            if (idx % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f, rowAltPaint)
            }
            canvas.drawText(QuizQuestionBank.toBanglaDigits(entry.rank), MARGIN + 8f, y + 15f, boldPaint)
            canvas.drawText(entry.participantName.take(22), MARGIN + 42f, y + 15f, bodyPaint)
            canvas.drawText(entry.schoolName.take(24), MARGIN + 185f, y + 15f, bodyPaint)
            canvas.drawText(entry.rollNumber.take(10), MARGIN + 345f, y + 15f, bodyPaint)
            canvas.drawText("${QuizQuestionBank.toBanglaDigits(entry.score)}/১০০", MARGIN + 405f, y + 15f, boldPaint)
            canvas.drawText(
                "${QuizQuestionBank.toBanglaDigits(entry.correctCount)}/${QuizQuestionBank.toBanglaDigits(entry.wrongCount)}",
                MARGIN + 462f,
                y + 15f,
                bodyPaint
            )
            y += 22f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, borderPaint)
        }

        drawFooter(canvas, pageNum, mutedPaint)
        pdf.finishPage(page)

        FileOutputStream(outFile).use { fos ->
            pdf.writeTo(fos)
        }
        pdf.close()
        return outFile
    }

    fun generateResultCsvFile(
        context: Context,
        modelNumber: Int,
        roundNumber: Int,
        submissions: List<QuizSubmission>
    ): File {
        val exportDir = ensureExportDir(context)
        val outFile = File(exportDir, "results_model_${modelNumber}_r${roundNumber}.csv")
        val sorted = submissions.sortedWith(
            compareByDescending<QuizSubmission> { it.score }
                .thenBy { it.durationSeconds }
        )
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

        val sb = StringBuilder()
        // UTF-8 BOM so Excel displays Bengali characters cleanly
        sb.append('\uFEFF')
        sb.append("Rank,Name,School,Roll,Model,Round,Score,Correct,Wrong,Unanswered,DurationSeconds,SubmittedAt\n")
        sorted.forEachIndexed { index, sub ->
            val rank = index + 1
            val submittedAt = dateFormat.format(sub.createdAt?.toDate() ?: Date())
            sb.append(
                listOf(
                    rank.toString(),
                    csvEscape(sub.participantName),
                    csvEscape(sub.schoolName),
                    csvEscape(sub.rollNumber),
                    sub.modelNumber.toString(),
                    sub.roundNumber.toString(),
                    sub.score.toString(),
                    sub.correctCount.toString(),
                    sub.wrongCount.toString(),
                    sub.unansweredCount.toString(),
                    sub.durationSeconds.toString(),
                    csvEscape(submittedAt)
                ).joinToString(",")
            ).append('\n')
        }

        outFile.writeText(sb.toString(), Charsets.UTF_8)
        return outFile
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\"", "\"\"")
        return "\"$clean\""
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, paint: Paint) {
        canvas.drawText(
            "প্রাইমারি স্কুল টেস্ট পরীক্ষা  •  পৃষ্ঠা ${QuizQuestionBank.toBanglaDigits(pageNum)}",
            MARGIN,
            PAGE_HEIGHT - 16f,
            paint
        )
    }

    fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    fun openPdfFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            shareFile(context, file, "application/pdf", "PDF শেয়ার বা সেভ করুন")
        }
    }

    fun copyFileToUri(context: Context, sourceFile: File, targetUri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}

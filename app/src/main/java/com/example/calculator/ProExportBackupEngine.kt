package com.example.calculator

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================================================================
// מנוע ייצוא PDF/CSV אמיתי וגיבוי/שחזור מלא ל-Google Drive דרך SAF (Tab 55)
// עובד ב-100% Native Android (API 26..34) ללא FileProvider וללא תלויות חיצוניות
// =========================================================================
object ProExportBackupEngine {

    private const val REQ_EXPORT_PDF = 5501
    private const val REQ_EXPORT_CSV = 5502
    private const val REQ_BACKUP_JSON = 5503
    private const val REQ_RESTORE_JSON = 5504

    private var lastStatusMessage: String = ""
    private var lastStatusIsError: Boolean = false
    private var statusViewRef: TextView? = null

    private fun nowStampFile(): String =
        SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

    private fun nowStampReadable(): String =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())

    private fun updateStatus(msg: String, isError: Boolean = false) {
        lastStatusMessage = msg
        lastStatusIsError = isError
        statusViewRef?.apply {
            visibility = if (msg.isBlank()) View.GONE else View.VISIBLE
            text = "\u200F$msg"
            setTextColor(if (isError) Color.parseColor("#D32F2F") else Color.parseColor("#2E7D32"))
        }
    }

    fun launchPdfExport(activity: MainActivity) {
        if (activity.historyList.isEmpty()) {
            val msg = "אין חישובים שמורים בהיסטוריה לייצוא PDF — בצע חישוב אחד לפחות"
            updateStatus(msg, true)
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
                putExtra(Intent.EXTRA_TITLE, "CalcPro_History_${nowStampFile()}.pdf")
            }
            activity.startActivityForResult(intent, REQ_EXPORT_PDF)
        } catch (_: Throwable) {
            val msg = "לא נמצא מנהל קבצים זמין לשמירת PDF במכשיר"
            updateStatus(msg, true)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun launchCsvExport(activity: MainActivity) {
        if (activity.historyList.isEmpty()) {
            val msg = "אין חישובים שמורים בהיסטוריה לייצוא CSV — בצע חישוב אחד לפחות"
            updateStatus(msg, true)
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, "CalcPro_History_${nowStampFile()}.csv")
            }
            activity.startActivityForResult(intent, REQ_EXPORT_CSV)
        } catch (_: Throwable) {
            val msg = "לא נמצא מנהל קבצים זמין לשמירת CSV במכשיר"
            updateStatus(msg, true)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun launchDriveBackup(activity: MainActivity) {
        try {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
                putExtra(Intent.EXTRA_TITLE, "CalcPro_Backup_${nowStampFile()}.json")
            }
            Toast.makeText(activity, "בחר ב-Google Drive או תיקייה במכשיר לשמירת הגיבוי", Toast.LENGTH_LONG).show()
            activity.startActivityForResult(intent, REQ_BACKUP_JSON)
        } catch (_: Throwable) {
            val msg = "לא ניתן לפתוח את בורר הגיבוי במכשיר"
            updateStatus(msg, true)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun launchDriveRestore(activity: MainActivity) {
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
            Toast.makeText(activity, "בחר קובץ גיבוי CalcPro_Backup (.json) מ-Google Drive או מהמכשיר", Toast.LENGTH_LONG).show()
            activity.startActivityForResult(intent, REQ_RESTORE_JSON)
        } catch (_: Throwable) {
            val msg = "לא ניתן לפתוח את בורר השחזור במכשיר"
            updateStatus(msg, true)
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun handleActivityResult(
        activity: MainActivity,
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ): Boolean {
        if (requestCode !in listOf(REQ_EXPORT_PDF, REQ_EXPORT_CSV, REQ_BACKUP_JSON, REQ_RESTORE_JSON)) {
            return false
        }
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            updateStatus("הפעולה בוטלה", false)
            return true
        }
        val uri = data.data!!
        when (requestCode) {
            REQ_EXPORT_PDF -> writePdfToUri(activity, uri)
            REQ_EXPORT_CSV -> writeCsvToUri(activity, uri)
            REQ_BACKUP_JSON -> writeBackupJsonToUri(activity, uri)
            REQ_RESTORE_JSON -> restoreBackupJsonFromUri(activity, uri)
        }
        return true
    }

    private fun writePdfToUri(activity: MainActivity, uri: Uri) {
        val doc = PdfDocument()
        try {
            val pageW = 595
            val pageH = 842
            val margin = 38f
            val items = activity.historyList.toList()
            val stamp = nowStampReadable()

            val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1E2433")
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#546E7A")
                textSize = 11f
                textAlign = Paint.Align.RIGHT
            }
            val headerCellP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            val bodyLeftP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1A1A1A")
                textSize = 11.5f
                textAlign = Paint.Align.LEFT
            }
            val bodyBoldRightP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#0D47A1")
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val numP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#607D8B")
                textSize = 10.5f
                textAlign = Paint.Align.RIGHT
            }
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#CFD8DC")
                strokeWidth = 0.8f
            }

            var pageNum = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
            var canvas = page.canvas
            var y = 52f

            fun drawPageHeader() {
                canvas.drawText("אולר שווייצרי Pro fx-991 — דוח היסטוריית חישובים", pageW - margin, y, titleP)
                y += 18f
                canvas.drawText("תאריך ייצוא: $stamp   |   סה״כ חישובים: ${items.size}   |   עמוד $pageNum", pageW - margin, y, subP)
                y += 16f

                // פס כותרת טבלה
                bgPaint.color = Color.parseColor("#263238")
                canvas.drawRoundRect(margin, y, pageW - margin, y + 24f, 6f, 6f, bgPaint)
                headerCellP.textAlign = Paint.Align.LEFT
                canvas.drawText("תרגיל (Expression)", margin + 10f, y + 16f, headerCellP)
                headerCellP.textAlign = Paint.Align.RIGHT
                canvas.drawText("תוצאה (Result)", pageW - margin - 46f, y + 16f, headerCellP)
                canvas.drawText("#", pageW - margin - 10f, y + 16f, headerCellP)
                y += 30f
            }

            drawPageHeader()

            for ((idx, entry) in items.withIndex()) {
                if (y > pageH - 56f) {
                    doc.finishPage(page)
                    pageNum++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                    canvas = page.canvas
                    y = 52f
                    drawPageHeader()
                }

                val exprPart = entry.substringBefore("=").trim().take(48)
                val resPart = entry.substringAfter("=", entry).trim().take(34)

                if (idx % 2 == 0) {
                    bgPaint.color = Color.parseColor("#F5F7FA")
                    canvas.drawRect(margin, y - 13f, pageW - margin, y + 9f, bgPaint)
                }

                canvas.drawText(exprPart, margin + 10f, y, bodyLeftP)
                canvas.drawText(resPart, pageW - margin - 46f, y, bodyBoldRightP)
                canvas.drawText("${idx + 1}", pageW - margin - 10f, y, numP)
                canvas.drawLine(margin, y + 9f, pageW - margin, y + 9f, linePaint)
                y += 23f
            }

            doc.finishPage(page)

            activity.contentResolver.openOutputStream(uri)?.use { out ->
                doc.writeTo(out)
                out.flush()
            } ?: throw IllegalStateException("לא ניתן לפתוח ערוץ כתיבה לקובץ")

            val okMsg = "✓ קובץ ה-PDF נשמר בהצלחה (${items.size} חישובים, $pageNum עמודים)"
            updateStatus(okMsg, false)
            Toast.makeText(activity, okMsg, Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            val errMsg = "שגיאה בייצוא PDF: ${t.message ?: "תקלה לא ידועה"}"
            updateStatus(errMsg, true)
            Toast.makeText(activity, errMsg, Toast.LENGTH_LONG).show()
        } finally {
            try { doc.close() } catch (_: Throwable) {}
        }
    }

    private fun csvEscape(raw: String): String =
        "\"${raw.replace("\"", "\"\"")}\""

    private fun writeCsvToUri(activity: MainActivity, uri: Uri) {
        try {
            val items = activity.historyList.toList()
            val stamp = nowStampReadable()
            val sb = StringBuilder()
            sb.append("מספר,תרגיל,תוצאה,תאריך ייצוא\r\n")
            for ((idx, entry) in items.withIndex()) {
                val exprPart = entry.substringBefore("=").trim()
                val resPart = entry.substringAfter("=", entry).trim()
                sb.append(idx + 1).append(',')
                    .append(csvEscape(exprPart)).append(',')
                    .append(csvEscape(resPart)).append(',')
                    .append(csvEscape(stamp)).append("\r\n")
            }

            activity.contentResolver.openOutputStream(uri)?.use { out ->
                // UTF-8 BOM כדי שמיקרוסופט אקסל יציג עברית בצורה מושלמת
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(sb.toString().toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: throw IllegalStateException("לא ניתן לפתוח ערוץ כתיבה לקובץ")

            val okMsg = "✓ קובץ CSV לאקסל נשמר בהצלחה (${items.size} שורות)"
            updateStatus(okMsg, false)
            Toast.makeText(activity, okMsg, Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            val errMsg = "שגיאה בייצוא CSV: ${t.message ?: "תקלה לא ידועה"}"
            updateStatus(errMsg, true)
            Toast.makeText(activity, errMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun writeBackupJsonToUri(activity: MainActivity, uri: Uri) {
        try {
            activity.savePreferences()
            val rootJson = JSONObject()
            rootJson.put("app", "SwissKnifeCalcPro")
            rootJson.put("backupVersion", 2)
            rootJson.put("exportedAt", nowStampReadable())
            rootJson.put("memoryValue", activity.memoryValue)

            val histArr = JSONArray()
            for (entry in activity.historyList) histArr.put(entry)
            rootJson.put("history", histArr)

            val prefsJson = JSONObject()
            val allMap = activity.prefs?.all ?: emptyMap()
            for ((k, v) in allMap) {
                if (v == null || k == "history") continue
                val entryObj = JSONObject()
                when (v) {
                    is Boolean -> { entryObj.put("t", "B"); entryObj.put("v", v) }
                    is Int -> { entryObj.put("t", "I"); entryObj.put("v", v) }
                    is Long -> { entryObj.put("t", "L"); entryObj.put("v", v) }
                    is Float -> { entryObj.put("t", "F"); entryObj.put("v", v.toDouble()) }
                    is String -> { entryObj.put("t", "S"); entryObj.put("v", v) }
                    else -> continue
                }
                prefsJson.put(k, entryObj)
            }
            rootJson.put("prefs", prefsJson)

            activity.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(rootJson.toString(2).toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: throw IllegalStateException("לא ניתן לפתוח ערוץ כתיבה לגיבוי")

            val okMsg = "✓ הגיבוי המלא נשמר בהצלחה (${activity.historyList.size} חישובים + כל ההגדרות והמועדפים)"
            updateStatus(okMsg, false)
            Toast.makeText(activity, okMsg, Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            val errMsg = "שגיאה בשמירת הגיבוי: ${t.message ?: "תקלה לא ידועה"}"
            updateStatus(errMsg, true)
            Toast.makeText(activity, errMsg, Toast.LENGTH_LONG).show()
        }
    }

    private fun restoreBackupJsonFromUri(activity: MainActivity, uri: Uri) {
        try {
            val rawText = activity.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
            } ?: throw IllegalStateException("לא ניתן לקרוא את קובץ הגיבוי")

            val json = JSONObject(rawText)
            val editor = activity.prefs?.edit()
            val prefsObj = json.optJSONObject("prefs")
            if (editor != null && prefsObj != null) {
                val keys = prefsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val item = prefsObj.optJSONObject(k) ?: continue
                    when (item.optString("t")) {
                        "B" -> editor.putBoolean(k, item.optBoolean("v"))
                        "I" -> editor.putInt(k, item.optInt("v"))
                        "L" -> editor.putLong(k, item.optLong("v"))
                        "F" -> editor.putFloat(k, item.optDouble("v").toFloat())
                        "S" -> editor.putString(k, item.optString("v"))
                    }
                }
            }

            val restoredHist = mutableListOf<String>()
            val histArr = json.optJSONArray("history")
            if (histArr != null) {
                for (i in 0 until histArr.length()) {
                    val s = histArr.optString(i, "")
                    if (s.isNotBlank()) restoredHist.add(s)
                }
            }
            editor?.putString("history", restoredHist.joinToString("||"))
            editor?.apply()

            activity.loadPreferences()
            if (json.has("memoryValue")) {
                activity.memoryValue = json.optDouble("memoryValue", activity.memoryValue)
            }
            activity.historyList.clear()
            activity.historyList.addAll(restoredHist)
            activity.savePreferences()

            val okMsg = "✓ השחזור הושלם בהצלחה! שוחזרו ${restoredHist.size} חישובים והגדרות"
            updateStatus(okMsg, false)
            Toast.makeText(activity, okMsg, Toast.LENGTH_LONG).show()
            activity.applyTheme()
        } catch (t: Throwable) {
            val errMsg = "קובץ הגיבוי אינו תקין: ${t.message ?: "פורמט שגוי"}"
            updateStatus(errMsg, true)
            Toast.makeText(activity, errMsg, Toast.LENGTH_LONG).show()
        }
    }

    fun buildScreen(activity: MainActivity): View {
        val isMin = activity.isMinimalistMode
        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5")
        val accentCol = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }

        val scroll = ScrollView(activity).apply { isFillViewport = true }
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(16f), activity.dp(8f), activity.dp(16f), activity.dp(26f))
        }
        scroll.addView(root)

        // 1. כרטיס כותרת + תג מועדף
        val headerCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(16f), activity.dp(14f), activity.dp(16f), activity.dp(14f))
            background = when {
                isMin -> MinimalistTheme.createMinimalistCardDrawable(activity, 20f, 1)
                activity.isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(activity, 20f)
                else -> GradientDrawable().apply {
                    cornerRadius = activity.dp(20f).toFloat()
                    setColor(Utils.lightenColor(activity.bgColor, 0.08f))
                    setStroke(activity.dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(activity, accentCol))
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(12f) }
        }

        val headerTopRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val isFav = ProFavoritesEngine.isFavoriteTab(activity.prefs, 55)
        val favChipBg = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else Color.argb(60, Color.red(accentCol), Color.green(accentCol), Color.blue(accentCol))
        val favChip = TextView(activity).apply {
            tag = "PRO_STAR_CHIP"
            text = if (isFav) "★ מועדף" else "☆ הוסף למועדפים"
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, favChipBg, 2.3))
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(activity.dp(12f), activity.dp(6f), activity.dp(12f), activity.dp(6f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(favChipBg)
            }
            setOnClickListener {
                activity.feedback(it)
                ProFavoritesEngine.toggleFavoriteTab(activity, 55)
                activity.switchTab(55)
            }
        }
        val headerTitleTv = TextView(activity).apply {
            text = "\u200Fייצוא PDF/CSV, גיבוי Drive והיסטוריה"
            setTextColor(textPrimary)
            textSize = 17.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = activity.dp(8f)
            }
        }
        headerTopRow.addView(favChip)
        headerTopRow.addView(headerTitleTv)
        headerCard.addView(headerTopRow)

        headerCard.addView(TextView(activity).apply {
            text = "\u200Fייצוא דוחות רשמיים, גיבוי ושחזור מלא מול Google Drive וחיפוש בהיסטוריה"
            setTextColor(textSecondary)
            textSize = 13f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(6f), 0, 0)
        })
        root.addView(headerCard)

        // 2. כפתורי פעולה: PDF, CSV, גיבוי Drive ושחזור גיבוי
        root.addView(activity.createActionBtn("ייצא היסטוריה ותוצאות לקובץ PDF", true) {
            launchPdfExport(activity)
        })
        root.addView(activity.createActionBtn("ייצא היסטוריה ותוצאות לקובץ CSV (אקסל)", true) {
            launchCsvExport(activity)
        })
        root.addView(activity.createActionBtn("גיבוי מלא ל-Google Drive / קובץ (.json)", true) {
            launchDriveBackup(activity)
        })
        root.addView(activity.createActionBtn("שחזור היסטוריה והגדרות מגיבוי Drive", false) {
            launchDriveRestore(activity)
        })

        // באנר סטטוס חי לפעולת הייצוא/גיבוי האחרונה
        val statusTv = TextView(activity).apply {
            visibility = if (lastStatusMessage.isBlank()) View.GONE else View.VISIBLE
            text = "\u200F$lastStatusMessage"
            setTextColor(if (lastStatusIsError) Color.parseColor("#D32F2F") else Color.parseColor("#2E7D32"))
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(12f), activity.dp(8f), activity.dp(12f), activity.dp(8f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(14f).toFloat()
                setColor(if (isMin) MinimalistTheme.CARD_WHITE else Color.argb(38, 255, 255, 255))
                setStroke(activity.dp(1f), accentCol)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }
        statusViewRef = statusTv
        root.addView(statusTv)

        // 3. שורת חיפוש חיה בהיסטוריה
        val searchBox = activity.createInputBox("חפש בהיסטוריה בלתי מוגבלת...").apply {
            tag = "HAS_CUSTOM_LIVE_WATCHER"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        root.addView(searchBox)

        val counterTv = TextView(activity).apply {
            setTextColor(textSecondary)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(4f), 0, activity.dp(4f), activity.dp(8f))
        }
        root.addView(counterTv)

        val listContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(listContainer)

        fun renderHistory(query: String) {
            listContainer.removeAllViews()
            val all = activity.historyList
            val filtered = if (query.isBlank()) all else all.filter { it.contains(query, ignoreCase = true) }
            counterTv.text = "\u200Fמציג ${filtered.size} מתוך ${all.size} חישובים שמורים (לחץ להעתקה):"

            if (filtered.isEmpty()) {
                listContainer.addView(TextView(activity).apply {
                    text = if (all.isEmpty()) "אין עדיין חישובים שמורים בהיסטוריה." else "לא נמצאו תוצאות תואמות לחיפוש."
                    setTextColor(textSecondary)
                    textSize = 14.5f
                    gravity = Gravity.CENTER
                    setPadding(activity.dp(12f), activity.dp(24f), activity.dp(12f), activity.dp(24f))
                })
                return
            }

            for (entry in filtered) {
                val exprPart = entry.substringBefore("=").trim()
                val resPart = entry.substringAfter("=", entry).trim()
                val cardBg = if (isMin) MinimalistTheme.CARD_WHITE else Color.argb(34, 255, 255, 255)

                val rowCard = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutDirection = View.LAYOUT_DIRECTION_LTR
                    setPadding(activity.dp(14f), activity.dp(10f), activity.dp(14f), activity.dp(10f))
                    background = GradientDrawable().apply {
                        cornerRadius = activity.dp(14f).toFloat()
                        setColor(cardBg)
                        setStroke(activity.dp(1f), if (isMin) Color.parseColor("#E6E1D9") else Color.argb(36, 255, 255, 255))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = activity.dp(7f) }
                }

                val delBtn = TextView(activity).apply {
                    text = "✕"
                    setTextColor(if (isMin) MinimalistTheme.ALERT_RED_TEXT else Color.parseColor("#FF5252"))
                    textSize = 14.5f
                    setPadding(activity.dp(6f), activity.dp(4f), activity.dp(10f), activity.dp(4f))
                    setOnClickListener {
                        activity.feedback(it)
                        activity.historyList.remove(entry)
                        activity.savePreferences()
                        renderHistory(searchBox.text.toString())
                    }
                }

                val textCol = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.RIGHT
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        activity.feedback(it)
                        activity.copyToClipboard(resPart)
                    }
                }
                textCol.addView(TextView(activity).apply {
                    text = "\u200E$exprPart ="
                    textDirection = View.TEXT_DIRECTION_LTR
                    setTextColor(textSecondary)
                    textSize = 13f
                    gravity = Gravity.RIGHT
                })
                textCol.addView(TextView(activity).apply {
                    text = "\u200E$resPart"
                    textDirection = View.TEXT_DIRECTION_LTR
                    setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, cardBg, 2.2))
                    textSize = 16.5f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.RIGHT
                })

                rowCard.addView(delBtn)
                rowCard.addView(textCol)
                listContainer.addView(rowCard)
            }
        }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderHistory(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        renderHistory("")
        return scroll
    }
}

fun MainActivity.createProVerifiedExportCenterScreen(): View =
    ProExportBackupEngine.buildScreen(this)

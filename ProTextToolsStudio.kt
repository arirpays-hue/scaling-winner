package com.example.calculator

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.min
import kotlin.math.roundToInt

// =========================================================================
// מנוע עיבוד טקסטים מהיר במיוחד (Ultra-Fast Text Tools Studio - Tab 81)
// מותאם לכמויות טקסט אדירות (100,000+ תווים) באמצעות עיבוד ברקע ללא חסימת UI
// =========================================================================
private data class FastTextStats(
    val totalChars: Int = 0,
    val noSpaceChars: Int = 0,
    val wordCount: Int = 0,
    val lineCount: Int = 0,
    val lettersCount: Int = 0,
    val digitsCount: Int = 0,
    val punctCount: Int = 0,
    val utf8Bytes: Int = 0
)

private object UltraFastTextEngine {

    // ספירה בלולאה אחת O(N) ללא Regex וללא יצירת מערכים מיותרים (פחות מ-1ms ל-120,000 תווים!)
    fun computeStats(text: CharSequence): FastTextStats {
        val len = text.length
        if (len == 0) return FastTextStats()

        var noSpace = 0
        var words = 0
        var lines = 1
        var letters = 0
        var digits = 0
        var inWord = false
        var bytes = 0

        for (i in 0 until len) {
            val c = text[i]
            val code = c.code
            bytes += when {
                code < 0x80 -> 1
                code < 0x800 -> 2
                else -> 3
            }

            if (c == '\n') lines++

            if (c.isWhitespace()) {
                inWord = false
            } else {
                noSpace++
                if (!inWord) {
                    words++
                    inWord = true
                }
                when {
                    c.isDigit() -> digits++
                    c.isLetter() || c in '\u05D0'..'\u05EA' -> letters++
                }
            }
        }
        val punct = (noSpace - letters - digits).coerceAtLeast(0)
        return FastTextStats(len, noSpace, words, lines, letters, digits, punct, bytes)
    }

    fun toUpperCaseFast(s: String): String = s.uppercase(Locale.getDefault())

    fun toLowerCaseFast(s: String): String = s.lowercase(Locale.getDefault())

    fun toTitleCaseFast(s: String): String {
        val len = s.length
        val sb = StringBuilder(len)
        var capNext = true
        for (i in 0 until len) {
            val c = s[i]
            if (c.isLetter()) {
                sb.append(if (capNext) c.uppercaseChar() else c.lowercaseChar())
                capNext = false
            } else {
                sb.append(c)
                capNext = c.isWhitespace() || c == '(' || c == '[' || c == '"' || c == '-' || c == '.' || c == '/'
            }
        }
        return sb.toString()
    }

    fun toSentenceCaseFast(s: String): String {
        val len = s.length
        val sb = StringBuilder(len)
        var capNext = true
        for (i in 0 until len) {
            val c = s[i]
            if (c.isLetter()) {
                sb.append(if (capNext) c.uppercaseChar() else c.lowercaseChar())
                capNext = false
            } else {
                sb.append(c)
                if (c == '.' || c == '!' || c == '?' || c == '\n') {
                    capNext = true
                }
            }
        }
        return sb.toString()
    }

    fun invertCaseFast(s: String): String {
        val len = s.length
        val sb = StringBuilder(len)
        for (i in 0 until len) {
            val c = s[i]
            sb.append(
                when {
                    c.isUpperCase() -> c.lowercaseChar()
                    c.isLowerCase() -> c.uppercaseChar()
                    else -> c
                }
            )
        }
        return sb.toString()
    }

    // קיצוץ רווחים מיותרים בתחילת/סוף כל שורה וצמצום רווחים כפולים בתוך השורה (תואם לסרטון)
    fun trimWhitespaceFast(s: String): String {
        val lines = s.split('\n')
        val sb = StringBuilder(s.length)
        for (idx in lines.indices) {
            val line = lines[idx].trim()
            var prevSpace = false
            for (j in line.indices) {
                val c = line[j]
                if (c == ' ' || c == '\t') {
                    if (!prevSpace) {
                        sb.append(' ')
                        prevSpace = true
                    }
                } else {
                    sb.append(c)
                    prevSpace = false
                }
            }
            if (idx < lines.lastIndex) sb.append('\n')
        }
        return sb.toString()
    }

    fun removeAllWhitespaceFast(s: String): String {
        val sb = StringBuilder(s.length)
        for (i in s.indices) {
            val c = s[i]
            if (!c.isWhitespace()) sb.append(c)
        }
        return sb.toString()
    }

    fun removeEmptyLinesFast(s: String): String {
        val lines = s.split('\n')
        val sb = StringBuilder(s.length)
        var first = true
        for (line in lines) {
            if (line.isNotBlank()) {
                if (!first) sb.append('\n')
                sb.append(line)
                first = false
            }
        }
        return sb.toString()
    }

    fun removeDuplicateLinesFast(s: String): String {
        val seen = LinkedHashSet<String>()
        val lines = s.split('\n')
        val sb = StringBuilder(s.length)
        var first = true
        for (line in lines) {
            if (seen.add(line)) {
                if (!first) sb.append('\n')
                sb.append(line)
                first = false
            }
        }
        return sb.toString()
    }

    fun sortLinesAscFast(s: String): String =
        s.split('\n').sortedWith(String.CASE_INSENSITIVE_ORDER).joinToString("\n")

    fun sortLinesDescFast(s: String): String =
        s.split('\n').sortedWith(String.CASE_INSENSITIVE_ORDER.reversed()).joinToString("\n")

    fun sortLinesByLengthFast(s: String): String =
        s.split('\n').sortedBy { it.length }.joinToString("\n")

    fun shuffleLinesFast(s: String): String =
        s.split('\n').shuffled().joinToString("\n")

    fun reverseTextFast(s: String): String = StringBuilder(s).reverse().toString()

    fun reverseLinesOrderFast(s: String): String = s.split('\n').asReversed().joinToString("\n")

    fun addLineNumbersFast(s: String): String {
        val lines = s.split('\n')
        val sb = StringBuilder(s.length + lines.size * 6)
        for (i in lines.indices) {
            if (i > 0) sb.append('\n')
            sb.append(i + 1).append(". ").append(lines[i])
        }
        return sb.toString()
    }

    fun stripLineNumbersFast(s: String): String {
        val regex = Regex("^\\s*\\d+[.)\\-:]\\s*")
        return s.split('\n').joinToString("\n") { line -> line.replaceFirst(regex, "") }
    }

    fun removeHebrewNikudFast(s: String): String {
        val sb = StringBuilder(s.length)
        for (i in s.indices) {
            val c = s[i]
            if (c !in '\u0591'..'\u05C7') sb.append(c)
        }
        return sb.toString()
    }

    fun extractMatchesFast(s: String, pattern: Regex): String {
        val matches = LinkedHashSet<String>()
        for (m in pattern.findAll(s)) {
            matches.add(m.value)
        }
        return if (matches.isEmpty()) s else matches.joinToString("\n")
    }

    fun linesToCsvFast(s: String): String =
        s.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

    fun csvToLinesFast(s: String): String =
        s.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
}

// =========================================================================
// מסך כלי הטקסט החדש (Pro Ultra-Fast Text Tools Studio - Tab 81)
// =========================================================================
fun MainActivity.createProUltraTextToolsStudioScreen(): View {
    val scroll = ScrollView(this)
    val root = LinearLayout(this).apply {
        tag = "SKIP_UNIVERSAL_LIVE"
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18f), dp(12f), dp(18f), dp(24f))
    }
    root.addView(
        createProCanvaHeader(
            "🔤 כלי טקסט מהיר Pro (Text Tools Studio)",
            "מעבד בקלות מאות אלפי תווים ברקע • 23 פעולות עיבוד וניקוי • חיפוש והחלפה • בטל/בצע שוב"
        )
    )

    val executor = Executors.newSingleThreadExecutor()
    val uiHandler = Handler(Looper.getMainLooper())
    var stats = FastTextStats()
    var isProcessing = false
    var isProgrammaticEdit = false

    val undoStack = ArrayDeque<String>()
    val redoStack = ArrayDeque<String>()

    fun pushUndoState(prevText: String) {
        if (undoStack.lastOrNull() == prevText) return
        val maxHistory = if (prevText.length > 50_000) 4 else 12
        while (undoStack.size >= maxHistory) undoStack.removeFirst()
        undoStack.addLast(prevText)
        redoStack.clear()
    }

    // קנבס ניתוח תווים חי (מותאם לכל 4 ערכות הנושא)
    val textAnalyticsCanvas = object : View(this) {
        private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val txtP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val d = resources.displayMetrics.density
            val w = width.toFloat()
            val h = height.toFloat()
            val pal = getProCanvasColors()

            val cx = w * 0.22f
            val cy = h * 0.50f
            val r = min(w * 0.15f, h * 0.34f)
            val ringRect = RectF(cx - r, cy - r, cx + r, cy + r)
            strokeP.strokeWidth = 9f * d
            strokeP.color = pal.grid
            strokeP.clearShadowLayer()
            canvas.drawCircle(cx, cy, r, strokeP)

            if (stats.totalChars > 0) {
                val spaces = (stats.totalChars - stats.noSpaceChars).coerceAtLeast(0)
                val segs = listOf(
                    Pair(stats.lettersCount, pal.accent1),
                    Pair(stats.digitsCount, pal.warning),
                    Pair(spaces, pal.accent2),
                    Pair(stats.punctCount, pal.accent3)
                )
                var startAng = -90f
                for ((cnt, col) in segs) {
                    if (cnt <= 0) continue
                    val sweep = (cnt.toFloat() / stats.totalChars.toFloat()) * 360f
                    strokeP.color = col
                    strokeP.applyAuroraGlow(pal.isAurora, col, d)
                    canvas.drawArc(ringRect, startAng, sweep.coerceAtLeast(2f), false, strokeP)
                    startAng += sweep
                }
            }

            txtP.clearShadowLayer()
            txtP.color = pal.ink
            txtP.textSize = 14.5f * d
            CanvasUiHelper.drawFittedText(canvas, BidiFmt.ltr(stats.wordCount), cx, cy + 2f * d, r * 1.6f, 14.5f * d, 9f * d, txtP)
            txtP.color = pal.muted
            txtP.textSize = 9.5f * d
            canvas.drawText("מילים", cx, cy + 14f * d, txtP)

            val barL = w * 0.43f
            val barR = w - 16f * d
            val barW = (barR - barL).coerceAtLeast(10f)
            val denom = stats.totalChars.coerceAtLeast(1).toFloat()
            val spacesCnt = (stats.totalChars - stats.noSpaceChars).coerceAtLeast(0)
            val rows = listOf(
                Triple("אותיות (${stats.lettersCount})", stats.lettersCount / denom, pal.accent1),
                Triple("ספרות (${stats.digitsCount})", stats.digitsCount / denom, pal.warning),
                Triple("רווחים ($spacesCnt)", spacesCnt / denom, pal.accent2)
            )
            txtP.textAlign = Paint.Align.RIGHT
            for ((idx, item) in rows.withIndex()) {
                val ry = h * (0.24f + idx * 0.24f)
                txtP.color = pal.ink
                txtP.textSize = 10.5f * d
                canvas.drawText("\u200F${item.first}", barR, ry - 4f * d, txtP)

                fillP.color = pal.grid
                canvas.drawRoundRect(RectF(barL, ry, barR, ry + 6.5f * d), 4f * d, 4f * d, fillP)
                fillP.color = item.third
                val fillW = barW * item.second.coerceIn(0f, 1f)
                if (fillW > 1f) {
                    canvas.drawRoundRect(RectF(barL, ry, barL + fillW, ry + 6.5f * d), 4f * d, 4f * d, fillP)
                }
            }

            val readSec = ((stats.wordCount / 200.0) * 60.0).roundToInt()
            val sizeLabel = if (stats.utf8Bytes >= 1024) {
                "${Utils.formatResult(stats.utf8Bytes / 1024.0, 1)} KB"
            } else {
                "${stats.utf8Bytes} B"
            }
            txtP.textAlign = Paint.Align.CENTER
            txtP.color = pal.muted
            txtP.textSize = 10.5f * d
            val footerStr = if (isProcessing) "⏳ מעבד טקסט במהירות..." else "\u200Fקריאה: ~${BidiFmt.ltr("$readSec ש׳")} • גודל: ${BidiFmt.ltr(sizeLabel)}"
            CanvasUiHelper.drawFittedText(canvas, footerStr, (barL + barR) / 2f, h - 7f * d, barW, 11f * d, 8.5f * d, txtP)
        }
    }.apply { styleProCanvasCard(this, 138f) }

    // תיבת טקסט מותאמת לכמויות טקסט ענקיות (גובה קבוע, גלילה פנימית חלקה, ללא בדיקת איות שמאטה)
    val textIn = createInputBox("הקלד או הדבק כאן טקסט (תומך גם ב-120,000+ תווים במהירות)...").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or
            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        textDirection = View.TEXT_DIRECTION_ANY_RTL
        gravity = Gravity.TOP or Gravity.START
        textSize = 14.5f
        isVerticalScrollBarEnabled = true
        overScrollMode = View.OVER_SCROLL_ALWAYS
        movementMethod = ScrollingMovementMethod.getInstance()
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(235f)
        ).apply { bottomMargin = dp(8f) }

        setOnTouchListener { v, event ->
            v.parent?.requestDisallowInterceptTouchEvent(true)
            if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP ||
                (event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_CANCEL
            ) {
                v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    // שורת מונה חיה תואמת לסרטון
    val liveCounterTv = TextView(this).apply {
        text = "\u200F0 תווים • 0 ללא רווחים • 0 מילים • 0 שורות"
        setTextColor(if (isMinimalistMode) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY)
        textSize = 13.0f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.RIGHT
        setPadding(dp(4f), 0, dp(4f), dp(8f))
    }

    // סרגל פעולות מהירות עליון: הדבק | בטל | בצע שוב | חיפוש והחלפה
    lateinit var undoBtn: TextView
    lateinit var redoBtn: TextView

    fun refreshUndoRedoButtons() {
        undoBtn.alpha = if (undoStack.isNotEmpty()) 1.0f else 0.45f
        redoBtn.alpha = if (redoStack.isNotEmpty()) 1.0f else 0.45f
    }

    var pendingStatsTask: Runnable? = null

    fun scheduleStatsUpdate(rawText: String) {
        pendingStatsTask?.let { uiHandler.removeCallbacks(it) }
        val r = Runnable {
            executor.execute {
                val computed = UltraFastTextEngine.computeStats(rawText)
                uiHandler.post {
                    stats = computed
                    liveCounterTv.text = "\u200F${BidiFmt.ltr(computed.totalChars)} תווים • ${BidiFmt.ltr(computed.noSpaceChars)} ללא רווחים • ${BidiFmt.ltr(computed.wordCount)} מילים • ${BidiFmt.ltr(computed.lineCount)} שורות"
                    textAnalyticsCanvas.invalidate()
                }
            }
        }
        pendingStatsTask = r
        uiHandler.postDelayed(r, if (rawText.length > 15_000) 140L else 40L)
    }

    textIn.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            if (isProgrammaticEdit) return
            scheduleStatsUpdate(s?.toString() ?: "")
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    })

    fun applyTransformAsync(label: String, transform: (String) -> String) {
        if (isProcessing) return
        val current = textIn.text?.toString() ?: ""
        if (current.isEmpty()) {
            Toast.makeText(this, "התיבה ריקה — הדבק או הקלד טקסט קודם", Toast.LENGTH_SHORT).show()
            return
        }
        isProcessing = true
        textAnalyticsCanvas.invalidate()
        val startNs = System.nanoTime()

        executor.execute {
            val result = try {
                transform(current)
            } catch (_: Throwable) {
                current
            }
            val computed = UltraFastTextEngine.computeStats(result)
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)

            uiHandler.post {
                isProcessing = false
                if (result != current) {
                    pushUndoState(current)
                    isProgrammaticEdit = true
                    textIn.setText(result)
                    textIn.setSelection(0)
                    textIn.scrollTo(0, 0)
                    isProgrammaticEdit = false
                }
                stats = computed
                liveCounterTv.text = "\u200F${BidiFmt.ltr(computed.totalChars)} תווים • ${BidiFmt.ltr(computed.noSpaceChars)} ללא רווחים • ${BidiFmt.ltr(computed.wordCount)} מילים • ${BidiFmt.ltr(computed.lineCount)} שורות"
                textAnalyticsCanvas.invalidate()
                refreshUndoRedoButtons()
                if (current.length >= 10_000) {
                    Toast.makeText(this, "✓ $label הושלם תוך ${elapsedMs}ms", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // שורת כפתורי עזר מהירה: הדבק / בטל / בצע שוב / חיפוש והחלפה
    val utilityRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8f) }
    }

    val findReplaceContainer = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        setPadding(dp(12f), dp(10f), dp(12f), dp(10f))
        background = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@createProUltraTextToolsStudioScreen, 18f, 0)
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@createProUltraTextToolsStudioScreen, 18f)
            else -> GradientDrawable().apply {
                setColor(Utils.lightenColor(bgColor, 0.07f))
                cornerRadius = dp(18f).toFloat()
            }
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(10f) }
    }

    fun makeSmallToolBtn(label: String, onClick: (View) -> Unit): TextView {
        val chipBg = if (isMinimalistMode) MinimalistTheme.NEUTRAL_SECONDARY_BG else Utils.lightenColor(bgColor, 0.12f)
        return TextView(this).apply {
            tag = "PRO_STAR_CHIP"
            text = label
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE, chipBg, 2.3))
            gravity = Gravity.CENTER
            setPadding(dp(8f), dp(9f), dp(8f), dp(9f))
            background = GradientDrawable().apply {
                cornerRadius = dp(14f).toFloat()
                setColor(chipBg)
                setStroke(dp(1f), if (isMinimalistMode) Color.parseColor("#DDD7CE") else primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(3f), 0, dp(3f), 0)
            }
            setOnClickListener { feedback(it); onClick(it) }
        }
    }

    val pasteBtn = makeSmallToolBtn("📥 הדבק") {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipText = cb?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!clipText.isNullOrEmpty()) {
            val prev = textIn.text?.toString() ?: ""
            if (prev.isNotEmpty()) pushUndoState(prev)
            isProgrammaticEdit = true
            textIn.setText(clipText)
            textIn.setSelection(0)
            textIn.scrollTo(0, 0)
            isProgrammaticEdit = false
            refreshUndoRedoButtons()
            scheduleStatsUpdate(clipText)
            Toast.makeText(this, "הודבקו ${clipText.length} תווים", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "לוח ההעתקה ריק", Toast.LENGTH_SHORT).show()
        }
    }

    undoBtn = makeSmallToolBtn("↩️ בטל") {
        if (undoStack.isNotEmpty()) {
            val cur = textIn.text?.toString() ?: ""
            redoStack.addLast(cur)
            val prev = undoStack.removeLast()
            isProgrammaticEdit = true
            textIn.setText(prev)
            textIn.setSelection(0)
            isProgrammaticEdit = false
            refreshUndoRedoButtons()
            scheduleStatsUpdate(prev)
        }
    }

    redoBtn = makeSmallToolBtn("↪️ חזור") {
        if (redoStack.isNotEmpty()) {
            val cur = textIn.text?.toString() ?: ""
            undoStack.addLast(cur)
            val next = redoStack.removeLast()
            isProgrammaticEdit = true
            textIn.setText(next)
            textIn.setSelection(0)
            isProgrammaticEdit = false
            refreshUndoRedoButtons()
            scheduleStatsUpdate(next)
        }
    }

    val toggleFindBtn = makeSmallToolBtn("🔍 החלף") {
        findReplaceContainer.visibility = if (findReplaceContainer.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    utilityRow.addView(pasteBtn)
    utilityRow.addView(undoBtn)
    utilityRow.addView(redoBtn)
    utilityRow.addView(toggleFindBtn)
    refreshUndoRedoButtons()

    // תוכן סרגל חיפוש והחלפה
    val findIn = createInputBox("חפש טקסט...").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_TEXT
    }
    val replaceIn = createInputBox("החלף ב-...").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_TEXT
    }
    val ignoreCaseCb = CheckBox(this).apply {
        text = "  התעלם מאותיות גדולות/קטנות"
        isChecked = true
        setTextColor(if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
    }
    val replaceAllBtn = createActionBtn("🔄 החלף את כל ההופעות", true) {
        val needle = findIn.text?.toString() ?: ""
        val repl = replaceIn.text?.toString() ?: ""
        if (needle.isEmpty()) {
            Toast.makeText(this, "הזן טקסט לחיפוש", Toast.LENGTH_SHORT).show()
            return@createActionBtn
        }
        val ignoreCase = ignoreCaseCb.isChecked
        applyTransformAsync("חיפוש והחלפה") { src ->
            src.replace(needle, repl, ignoreCase = ignoreCase)
        }
    }
    findReplaceContainer.addView(findIn)
    findReplaceContainer.addView(replaceIn)
    findReplaceContainer.addView(ignoreCaseCb)
    findReplaceContainer.addView(replaceAllBtn)

    // שורה 1 של צ'יפים: כל 11 הפעולות המדויקות מהסרטון
    val primaryTransforms: List<Pair<String, (String) -> String>> = listOf(
        "אותיות גדולות" to UltraFastTextEngine::toUpperCaseFast,
        "אותיות קטנות" to UltraFastTextEngine::toLowerCaseFast,
        "כל מילה באות גדולה" to UltraFastTextEngine::toTitleCaseFast,
        "אות גדולה בתחילת משפט" to UltraFastTextEngine::toSentenceCaseFast,
        "היפוך רישיות" to UltraFastTextEngine::invertCaseFast,
        "קיצוץ רווחים" to UltraFastTextEngine::trimWhitespaceFast,
        "הסרת כל הרווחים" to UltraFastTextEngine::removeAllWhitespaceFast,
        "הסרת שורות ריקות" to UltraFastTextEngine::removeEmptyLinesFast,
        "הסרת שורות כפולות" to UltraFastTextEngine::removeDuplicateLinesFast,
        "מיון שורות" to UltraFastTextEngine::sortLinesAscFast,
        "היפוך" to UltraFastTextEngine::reverseTextFast
    )

    // שורה 2 של צ'יפים: 12 תכונות פרו נוספות
    val bonusTransforms: List<Pair<String, (String) -> String>> = listOf(
        "מיון שורות (ת-א)" to UltraFastTextEngine::sortLinesDescFast,
        "מיון לפי אורך שורה" to UltraFastTextEngine::sortLinesByLengthFast,
        "ערבוב שורות 🎲" to UltraFastTextEngine::shuffleLinesFast,
        "מספור שורות (1. 2.)" to UltraFastTextEngine::addLineNumbersFast,
        "הסרת מספור שורות" to UltraFastTextEngine::stripLineNumbersFast,
        "היפוך סדר שורות" to UltraFastTextEngine::reverseLinesOrderFast,
        "הסרת ניקוד עברי" to UltraFastTextEngine::removeHebrewNikudFast,
        "חילוץ קישורים (URL)" to { s -> UltraFastTextEngine.extractMatchesFast(s, Regex("https?://\\S+")) },
        "חילוץ אימיילים (@)" to { s -> UltraFastTextEngine.extractMatchesFast(s, Regex("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}")) },
        "חילוץ מספרים (123)" to { s -> UltraFastTextEngine.extractMatchesFast(s, Regex("-?\\d+(?:\\.\\d+)?")) },
        "שורות ➔ פסיקים" to UltraFastTextEngine::linesToCsvFast,
        "פסיקים ➔ שורות" to UltraFastTextEngine::csvToLinesFast
    )

    fun createChipBar(items: List<Pair<String, (String) -> String>>): HorizontalScrollView {
        val hs = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8f) }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        for ((label, fn) in items) {
            val chip = TextView(this).apply {
                tag = "PRO_STAR_CHIP"
                text = label
                textSize = 13.0f
                typeface = Typeface.DEFAULT_BOLD
                val chipBg = if (isMinimalistMode) MinimalistTheme.CARD_WHITE else Utils.lightenColor(bgColor, 0.12f)
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE, chipBg, 2.3))
                gravity = Gravity.CENTER
                setPadding(dp(15f), dp(10f), dp(15f), dp(10f))
                background = GradientDrawable().apply {
                    cornerRadius = dp(14f).toFloat()
                    setColor(chipBg)
                    setStroke(dp(1.2f), if (isMinimalistMode) Color.parseColor("#E0DBD3") else primaryColor)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(dp(4f), dp(2f), dp(4f), dp(2f)) }
                setOnClickListener {
                    feedback(it)
                    applyTransformAsync(label, fn)
                }
            }
            row.addView(chip)
        }
        hs.addView(row)
        return hs
    }

    // שורת 3 כפתורים תחתונה תואמת לסרטון: העתק | שיתוף | נקה
    val bottomActionsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(4f) }
    }

    val copyBtn = createActionBtn("📋 העתק", true) {
        val txt = textIn.text?.toString() ?: ""
        if (txt.isNotEmpty()) copyToClipboard(txt) else Toast.makeText(this, "אין טקסט להעתקה", Toast.LENGTH_SHORT).show()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.15f).apply { rightMargin = dp(5f) } }

    val shareBtn = createActionBtn("📤 שיתוף", false) {
        val txt = textIn.text?.toString() ?: ""
        if (txt.isNotEmpty()) {
            val safeShare = if (txt.length > 100_000) txt.substring(0, 100_000) else txt
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, safeShare)
                    },
                    "שתף טקסט באמצעות"
                )
            )
        } else {
            Toast.makeText(this, "הזן טקסט לשיתוף", Toast.LENGTH_SHORT).show()
        }
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(4f); rightMargin = dp(4f) } }

    val clearBtn = createActionBtn("🧹 נקה", false) {
        val prev = textIn.text?.toString() ?: ""
        if (prev.isNotEmpty()) pushUndoState(prev)
        isProgrammaticEdit = true
        textIn.setText("")
        isProgrammaticEdit = false
        stats = FastTextStats()
        liveCounterTv.text = "\u200F0 תווים • 0 ללא רווחים • 0 מילים • 0 שורות"
        textAnalyticsCanvas.invalidate()
        refreshUndoRedoButtons()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f).apply { leftMargin = dp(5f) } }

    bottomActionsRow.addView(copyBtn)
    bottomActionsRow.addView(shareBtn)
    bottomActionsRow.addView(clearBtn)

    root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            pendingStatsTask?.let { uiHandler.removeCallbacks(it) }
            executor.shutdownNow()
        }
    })

    root.addView(textAnalyticsCanvas)
    root.addView(textIn)
    root.addView(liveCounterTv)
    root.addView(utilityRow)
    root.addView(findReplaceContainer)
    root.addView(createChipBar(primaryTransforms))
    root.addView(createChipBar(bonusTransforms))
    root.addView(bottomActionsRow)
    scroll.addView(root)
    return scroll
}

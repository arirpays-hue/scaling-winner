package com.example.calculator

import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.lang.ref.WeakReference
import kotlin.math.abs

// =========================================================================
// מנוע הגנה שקט מפני קריסות, מצב בטוח (Safe Mode) וטיפול בכל מצבי הקצה
// (ProCrashGuardAndRecovery.kt) — פועל אוטומטית ברקע ללא כפתור בהגדרות
// =========================================================================
object ProCrashGuardAndRecovery {

    private const val GUARD_PREFS_NAME = "CalcProCrashGuardPrefs"
    private const val KEY_BOOT_CRASH_COUNT = "boot_crash_count"
    private const val KEY_LAST_CRASH_TIME = "last_crash_time_ms"
    private const val KEY_SAFE_MODE_ACTIVE = "safe_mode_active"

    private const val BOOT_LOOP_WINDOW_MS = 25_000L
    private const val STABLE_RUNTIME_RESET_MS = 6_000L

    private const val MAX_INPUT_LENGTH = 220
    private const val MAX_HISTORY_ENTRY_LEN = 320
    private const val MAX_CLIPBOARD_CHARS = 95_000
    private const val MAX_SAFE_FACTORIAL = 170
    private const val MAX_PAREN_DEPTH = 24
    private const val MAX_POWER_EXPONENT = 10_000.0

    data class MathEvalOutcome(
        val success: Boolean,
        val numericValue: Double,
        val formattedResult: String,
        val userMessage: String = ""
    )

    private var installed = false
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null
    private var hostActivityRef: WeakReference<MainActivity>? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stabilityRunnable: Runnable? = null

    var isSafeModeActive: Boolean = false
        private set

    private fun guardPrefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(GUARD_PREFS_NAME, Context.MODE_PRIVATE)

    // =========================================================================
    // 1. אתחול שקט, לכידת קריסות גלובלית והגנה מלופ קריסות בהפעלה
    // =========================================================================
    fun install(activity: MainActivity) {
        appContext = activity.applicationContext
        hostActivityRef = WeakReference(activity)

        val gp = guardPrefs(activity)
        val now = System.currentTimeMillis()
        val lastCrash = gp.getLong(KEY_LAST_CRASH_TIME, 0L)
        val bootCount = gp.getInt(KEY_BOOT_CRASH_COUNT, 0)

        if (bootCount >= 2 && (now - lastCrash) <= BOOT_LOOP_WINDOW_MS) {
            isSafeModeActive = true
            gp.edit().putBoolean(KEY_SAFE_MODE_ACTIVE, true).apply()
            applySafeModeRecovery(activity)
        } else {
            isSafeModeActive = gp.getBoolean(KEY_SAFE_MODE_ACTIVE, false)
        }

        // ניקוי אוטומטי ושקט של כל מצבי הקצה במצב האפליקציה וההעדפות
        sanitizeAppState(activity)

        if (!installed) {
            installed = true
            defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                handleUncaughtThrowable(thread, throwable)
            }
        }

        // לאחר 6 שניות של ריצה יציבה — איפוס שקט של מונה קריסות ההפעלה
        stabilityRunnable?.let { mainHandler.removeCallbacks(it) }
        val r = Runnable {
            try {
                appContext?.let { ctx ->
                    guardPrefs(ctx).edit()
                        .putInt(KEY_BOOT_CRASH_COUNT, 0)
                        .putBoolean(KEY_SAFE_MODE_ACTIVE, false)
                        .apply()
                }
                isSafeModeActive = false
            } catch (_: Throwable) {}
        }
        stabilityRunnable = r
        mainHandler.postDelayed(r, STABLE_RUNTIME_RESET_MS)
    }

    fun onActivityResumed(activity: MainActivity) {
        hostActivityRef = WeakReference(activity)
        sanitizeAppState(activity)
        if (isSafeModeActive) {
            Toast.makeText(
                activity,
                "האפליקציה התאוששה מתקלה קודמת ופתחה את המחשבון במצב בטוח",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleUncaughtThrowable(thread: Thread, throwable: Throwable) {
        val isMainThread = (thread === Looper.getMainLooper().thread)
        val isFatalError = (throwable is Error) // OutOfMemoryError / StackOverflowError / LinkageError

        // 1. בלימה שקטה של קריסות בתהליכי רקע (רשת, מטבעות, חיישנים, Speed Test, SSL)
        if (!isMainThread && !isFatalError) {
            return
        }

        // 2. רישום מהיר לזיהוי לופ קריסות בהפעלה הבאה
        try {
            appContext?.let { ctx ->
                val gp = guardPrefs(ctx)
                val now = System.currentTimeMillis()
                val prevCrash = gp.getLong(KEY_LAST_CRASH_TIME, 0L)
                val prevCount = gp.getInt(KEY_BOOT_CRASH_COUNT, 0)
                val nextCount = if ((now - prevCrash) <= BOOT_LOOP_WINDOW_MS) prevCount + 1 else 1
                gp.edit()
                    .putLong(KEY_LAST_CRASH_TIME, now)
                    .putInt(KEY_BOOT_CRASH_COUNT, nextCount)
                    .commit()
            }
        } catch (_: Throwable) {}

        // 3. שחרור חלון צף ומטמונים כדי למנוע דליפת חלון / OOM חוזר
        try { ProFloatingMiniCalc.dismiss() } catch (_: Throwable) {}
        try {
            if (throwable is OutOfMemoryError) {
                hostActivityRef?.get()?.cachedRates?.clear()
            }
        } catch (_: Throwable) {}

        val prev = defaultHandler
        if (prev != null) {
            prev.uncaughtException(thread, throwable)
        } else {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun applySafeModeRecovery(activity: MainActivity) {
        try {
            ProFloatingMiniCalc.dismiss()
            ProCanvasAnimations.onActivityPause()
            activity.currentTabIndex = 0
            activity.currentInput = ""
            activity.justCalculated = false
            activity.useAnimations = false
            ProUiMotionEngine.setEnabled(activity, false)
            if (!activity.memoryValue.isFinite()) activity.memoryValue = 0.0
            activity.savePreferences()
        } catch (_: Throwable) {}
    }

    // =========================================================================
    // 2. בידוד קריסות לכל 121 המסכים (Tabs 0..120) כולל התמודדות עם OOM
    // =========================================================================
    fun buildTabSafely(
        activity: MainActivity,
        tabIndex: Int,
        builder: () -> View
    ): View {
        return try {
            builder()
        } catch (oom: OutOfMemoryError) {
            onLowMemory(activity)
            createRecoveryFallbackView(activity, tabIndex, isMemoryIssue = true)
        } catch (_: Throwable) {
            createRecoveryFallbackView(activity, tabIndex, isMemoryIssue = false)
        }
    }

    private fun createRecoveryFallbackView(
        activity: MainActivity,
        failedTabIndex: Int,
        isMemoryIssue: Boolean
    ): View {
        val isMin = activity.isMinimalistMode
        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5")
        val accentCol = if (isMin) MinimalistTheme.ACCENT_SAGE_GREEN else activity.primaryColor
        val toolTitle = IosGlassTheme.cleanCategoryTitle(
            activity.categories.getOrElse(failedTabIndex) { "כלי #$failedTabIndex" },
            true
        )

        val scroll = ScrollView(activity).apply { isFillViewport = true }
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(activity.dp(18f), activity.dp(24f), activity.dp(18f), activity.dp(24f))
        }
        scroll.addView(root)

        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(18f), activity.dp(18f), activity.dp(18f), activity.dp(18f))
            background = when {
                isMin -> MinimalistTheme.createMinimalistCardDrawable(activity, 22f, 0)
                activity.isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(activity, 22f)
                else -> GradientDrawable().apply {
                    cornerRadius = activity.dp(22f).toFloat()
                    setColor(Utils.lightenColor(activity.bgColor, 0.08f))
                    setStroke(activity.dp(1.2f), accentCol)
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(14f) }
        }

        card.addView(TextView(activity).apply {
            text = "\u200Fלא ניתן לטעון כרגע את המסך: $toolTitle"
            setTextColor(textPrimary)
            textSize = 17.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
        })

        card.addView(TextView(activity).apply {
            text = if (isMemoryIssue) {
                "\u200Fהמכשיר בעומס זיכרון זמני. פונו משאבים ברקע — נסה לטעון את המסך שוב."
            } else {
                "\u200Fאירעה שגיאה זמנית בטעינת הכלי (ייתכן שחיישן או משאב חסרים במכשיר). האפליקציה ממשיכה לפעול כרגיל."
            }
            setTextColor(textSecondary)
            textSize = 14f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(8f), 0, 0)
        })
        root.addView(card)

        root.addView(activity.createActionBtn("נסה לטעון שוב ↻", true) {
            sanitizeAppState(activity)
            activity.switchTab(failedTabIndex)
        })
        root.addView(activity.createActionBtn("חזור למחשבון רגיל", false) {
            activity.switchTab(0)
        })
        return scroll
    }

    // =========================================================================
    // 3. כל מצבי הקצה המתמטיים (קלט, חזקות, עצרת, סוגריים, זיכרון M, NaN/Inf)
    // =========================================================================
    fun safeAppendInput(current: String, token: String): String {
        val cleanCur = if (current == "Error" || current == "Overflow" || current == "NaN" || current == "Infinity" || current == "-Infinity") {
            ""
        } else {
            current
        }

        val binaryOps = setOf("÷", "×", "−", "+", "^", "%")
        if (cleanCur.isEmpty()) {
            if (token in setOf("÷", "×", "^", "%", "!")) return ""
            if (token == ".") return "0."
            return token
        }

        // מניעת שתי נקודות עשרוניות באותו מספר (למשל 12.3.4)
        if (token == ".") {
            val lastNumberSegment = cleanCur.takeLastWhile { it.isDigit() || it == '.' }
            if (lastNumberSegment.contains('.')) return cleanCur
            if (lastNumberSegment.isEmpty()) {
                val withZero = "${cleanCur}0."
                return if (withZero.length <= MAX_INPUT_LENGTH) withZero else cleanCur
            }
        }

        // החלפת אופרטור כפול רצוף (למשל ++ או ×÷) במקום שגיאת תחביר
        val lastChar = cleanCur.last().toString()
        if (token in binaryOps && lastChar in binaryOps) {
            if (token == "−" && lastChar in setOf("÷", "×", "^")) {
                // מאפשר חזקה או כפל במספר שלילי כמו 5×−2
            } else {
                return cleanCur.dropLast(1) + token
            }
        }

        // מניעת עצרת כפולה מקוננת (!!) שעלולה לגרום לקפיאה
        if (token == "!" && cleanCur.endsWith("!")) {
            return cleanCur
        }

        // מניעת סוגר סוגר מיותר אם אין סוגר פתוח
        if (token == ")") {
            val opens = cleanCur.count { it == '(' }
            val closes = cleanCur.count { it == ')' }
            if (closes >= opens) return cleanCur
        }

        // הגבלת עומק סוגריים מקוננים למניעת StackOverflowError
        if (token.contains("(")) {
            val openDepth = cleanCur.count { it == '(' } - cleanCur.count { it == ')' }
            if (openDepth >= MAX_PAREN_DEPTH) return cleanCur
        }

        val combined = cleanCur + token
        return if (combined.length > MAX_INPUT_LENGTH) cleanCur else combined
    }

    fun safeEvaluateMathExpression(rawExpression: String, precision: Int): MathEvalOutcome {
        val trimmed = rawExpression.trim()
        if (trimmed.isEmpty()) return MathEvalOutcome(false, 0.0, "0", "")
        if (trimmed.length > MAX_INPUT_LENGTH) {
            return MathEvalOutcome(false, 0.0, "Error", "התרגיל ארוך מדי")
        }

        // בדיקת עומק סוגריים למניעת קריסת רקורסיה (StackOverflowError)
        var depth = 0
        var maxDepth = 0
        for (ch in trimmed) {
            if (ch == '(') {
                depth++
                if (depth > maxDepth) maxDepth = depth
            } else if (ch == ')') {
                depth--
                if (depth < 0) return MathEvalOutcome(false, 0.0, "Error", "סוגריים לא מאוזנים")
            }
        }
        if (maxDepth > MAX_PAREN_DEPTH) {
            return MathEvalOutcome(false, 0.0, "Error", "יותר מדי סוגריים מקוננים")
        }

        // חסימת עצרת כפולה או מעל 170! (חריגת Double / קפיאת UI)
        if (trimmed.contains("!!")) {
            return MathEvalOutcome(false, 0.0, "Overflow", "עצרת כפולה חורגת מהטווח")
        }
        val factRegex = Regex("""(\d+(?:\.\d+)?)!""")
        for (match in factRegex.findAll(trimmed)) {
            val n = match.groupValues[1].toDoubleOrNull() ?: Double.MAX_VALUE
            if (n > MAX_SAFE_FACTORIAL || n < 0.0) {
                return MathEvalOutcome(false, 0.0, "Overflow", "עצרת נתמכת עד $MAX_SAFE_FACTORIAL!")
            }
        }

        // ניקוי והשלמת ביטוי
        var normalized = trimmed
            .replace(",", ".")
            .replace(" ", "")
        while (normalized.isNotEmpty() && normalized.last() in setOf('+', '−', '-', '×', '*', '÷', '/', '^')) {
            normalized = normalized.dropLast(1)
        }
        if (normalized.isEmpty()) return MathEvalOutcome(false, 0.0, "0", "")

        // סגירה אוטומטית של סוגריים פתוחים בסוף התרגיל
        val openCount = normalized.count { it == '(' }
        val closeCount = normalized.count { it == ')' }
        if (openCount > closeCount) {
            normalized += ")".repeat((openCount - closeCount).coerceAtMost(MAX_PAREN_DEPTH))
        }

        // חסימת מגדל חזקות קיצוני (למשל 9^9^9 או 10^100000)
        val powerRegex = Regex("""\^(\d+(?:\.\d+)?)""")
        for (m in powerRegex.findAll(normalized)) {
            val exp = m.groupValues[1].toDoubleOrNull() ?: 0.0
            if (abs(exp) > MAX_POWER_EXPONENT) {
                return MathEvalOutcome(false, 0.0, "Overflow", "חזקה גדולה מדי לחישוב")
            }
        }

        return try {
            val rawRes = Utils.evalMathExpression(normalized)
            when {
                rawRes.isNaN() -> MathEvalOutcome(false, 0.0, "Error", "חישוב לא מוגדר (חלוקה ב-0 או שורש שלילי)")
                rawRes.isInfinite() -> MathEvalOutcome(false, 0.0, "Overflow", "התוצאה גדולה מדי (Overflow)")
                else -> {
                    // נירמול -0.0 ל-0.0
                    val cleanRes = if (rawRes == 0.0) 0.0 else rawRes
                    val safePrec = precision.coerceIn(1, 12)
                    val formatted = Utils.formatResult(cleanRes, safePrec)
                    MathEvalOutcome(true, cleanRes, formatted)
                }
            }
        } catch (_: StackOverflowError) {
            MathEvalOutcome(false, 0.0, "Error", "הביטוי מורכב מדי")
        } catch (_: OutOfMemoryError) {
            MathEvalOutcome(false, 0.0, "Overflow", "אין מספיק זיכרון לחישוב")
        } catch (_: Throwable) {
            MathEvalOutcome(false, 0.0, "Error", "ביטוי לא תקין")
        }
    }

    fun safeEvaluateMemoryDelta(currentExpr: String, precision: Int): Double? {
        if (currentExpr.isBlank()) return 0.0
        val out = safeEvaluateMathExpression(currentExpr, precision)
        return if (out.success && out.numericValue.isFinite()) out.numericValue else null
    }

    fun safeParseDouble(raw: String?, fallback: Double = 0.0): Double {
        if (raw.isNullOrBlank()) return fallback
        val v = raw.trim().replace(",", ".").toDoubleOrNull() ?: return fallback
        return if (v.isFinite()) v else fallback
    }

    // =========================================================================
    // 4. ניקוי ותיקון אוטומטי של מצבי קצה בהעדפות, היסטוריה, מטבעות וערכות נושא
    // =========================================================================
    fun sanitizeAppState(activity: MainActivity) {
        try {
            var changed = false

            if (activity.calcPrecision !in 1..12) {
                activity.calcPrecision = activity.calcPrecision.coerceIn(1, 12)
                changed = true
            }
            if (!activity.buttonCornerStyle.isFinite() || activity.buttonCornerStyle < 0f || activity.buttonCornerStyle > 48f) {
                activity.buttonCornerStyle = 24f
                changed = true
            }
            if (activity.soundType !in 0..3) {
                activity.soundType = activity.soundType.coerceIn(0, 3)
                changed = true
            }
            if (!activity.memoryValue.isFinite()) {
                activity.memoryValue = 0.0
                changed = true
            }
            if (activity.currentTabIndex !in activity.categories.indices) {
                activity.currentTabIndex = 0
                changed = true
            }
            if (activity.currentInput.length > MAX_INPUT_LENGTH) {
                activity.currentInput = activity.currentInput.take(MAX_INPUT_LENGTH)
                changed = true
            }
            if (activity.currentInput in setOf("NaN", "Infinity", "-Infinity")) {
                activity.currentInput = ""
                changed = true
            }

            // תיקון קודי מטבע ריקים או פגומים
            if (activity.fromCurrency.length != 3) {
                activity.fromCurrency = "USD"
                changed = true
            }
            if (activity.toCurrency.length != 3) {
                activity.toCurrency = "ILS"
                changed = true
            }

            // ניקוי שערי מטבע שגויים (<= 0 או NaN/Infinity) למניעת חלוקה ב-0
            val badRates = activity.cachedRates.filterValues { !it.isFinite() || it <= 0.0 }.keys.toList()
            for (k in badRates) activity.cachedRates.remove(k)

            // ניקוי היסטוריה מרשומות פגומות, ריקות או ענקיות
            val maxAllowed = if (ProState.isProEnabled(activity.prefs)) 500 else 30
            val origSize = activity.historyList.size
            val cleaned = activity.historyList
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.contains("NaN") && !it.contains("Infinity") }
                .map { if (it.length > MAX_HISTORY_ENTRY_LEN) it.take(MAX_HISTORY_ENTRY_LEN) else it }
                .take(maxAllowed)
                .toList()
            if (cleaned.size != origSize) {
                activity.historyList.clear()
                activity.historyList.addAll(cleaned)
                changed = true
            }

            // מניעת מצב שבו יותר מערכת נושא אחת פעילה במקביל
            val activeThemes = listOf(activity.isMinimalistMode, activity.isAuroraMode, activity.isIosGlassMode).count { it }
            if (activeThemes > 1) {
                if (activity.isMinimalistMode) {
                    activity.isAuroraMode = false
                    activity.isIosGlassMode = false
                } else if (activity.isAuroraMode) {
                    activity.isIosGlassMode = false
                }
                changed = true
            }

            // הבטחה שבתפריט הצד תמיד יישאר לפחות המחשבון הרגיל גלוי
            if (activity.categories.indices.none { it != 19 && activity.isCalculatorVisibleInDrawer(it) }) {
                activity.setCalculatorVisibleInDrawer(0, true)
                changed = true
            }

            if (changed) activity.savePreferences()
        } catch (_: Throwable) {}
    }

    // =========================================================================
    // 5. הגנות מערכת: דיאלוגים, Clipboard, Intents, UI Post, קנבס ולחץ זיכרון
    // =========================================================================
    fun isActivityAlive(activity: Activity?): Boolean {
        if (activity == null) return false
        return !activity.isFinishing && !activity.isDestroyed
    }

    fun safeShowDialog(activity: Activity?, dialog: Dialog?): Boolean {
        if (!isActivityAlive(activity) || dialog == null) return false
        return try {
            dialog.show()
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun safeDismissDialog(dialog: Dialog?) {
        if (dialog == null) return
        try {
            if (dialog.isShowing) dialog.dismiss()
        } catch (_: Throwable) {}
    }

    fun safePostUi(activity: Activity?, delayMs: Long = 0L, action: () -> Unit) {
        val r = Runnable {
            if (!isActivityAlive(activity)) return@Runnable
            try {
                action()
            } catch (_: Throwable) {}
        }
        if (delayMs <= 0L) mainHandler.post(r) else mainHandler.postDelayed(r, delayMs)
    }

    fun safeClipboardCopy(context: Context, text: String, cleanToast: Boolean = false) {
        try {
            val safeText = if (text.length > MAX_CLIPBOARD_CHARS) {
                text.take(MAX_CLIPBOARD_CHARS) + "…"
            } else {
                text
            }
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            cb.setPrimaryClip(ClipData.newPlainText("CalcResult", safeText))
            Toast.makeText(
                context,
                if (cleanToast) "הועתק ללוח" else "הועתק ללוח 📋",
                Toast.LENGTH_SHORT
            ).show()
        } catch (_: Throwable) {
            Toast.makeText(context, "לא ניתן להעתיק ללוח כרגע", Toast.LENGTH_SHORT).show()
        }
    }

    fun safeStartActivity(activity: Activity?, intent: Intent, fallbackMsg: String = "לא נמצאה אפליקציה מתאימה לפעולה זו"): Boolean {
        if (!isActivityAlive(activity)) return false
        return try {
            activity!!.startActivity(intent)
            true
        } catch (_: Throwable) {
            Toast.makeText(activity, fallbackMsg, Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun safeReadIntentAction(intent: Intent?): String? {
        return try {
            intent?.action
        } catch (_: Throwable) {
            null
        }
    }

    inline fun <T> safeRun(fallback: T, block: () -> T): T {
        return try {
            block()
        } catch (_: Throwable) {
            fallback
        }
    }

    fun onTrimMemory(activity: MainActivity, level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            try {
                if (activity.cachedRates.size > 25) {
                    val usd = activity.cachedRates["USD"]
                    val ils = activity.cachedRates["ILS"]
                    val eur = activity.cachedRates["EUR"]
                    activity.cachedRates.clear()
                    usd?.let { activity.cachedRates["USD"] = it }
                    ils?.let { activity.cachedRates["ILS"] = it }
                    eur?.let { activity.cachedRates["EUR"] = it }
                }
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                    ProCanvasAnimations.onActivityPause()
                }
            } catch (_: Throwable) {}
        }
    }

    fun onLowMemory(activity: MainActivity) {
        onTrimMemory(activity, ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}

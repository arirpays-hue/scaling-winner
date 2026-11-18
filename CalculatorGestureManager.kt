package com.example.calculator

import android.app.PictureInPictureParams
import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Rational
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlin.math.*

data class CalcGestureType(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeSymbol: String,
    val defaultActionId: String
)

data class CalcGestureAction(
    val id: String,
    val category: String,
    val title: String
)

object CalculatorGestureManager {

    private const val PREF_PREFIX = "calc_gesture_map_"
    private const val PREF_SHOW_TOAST = "calc_gesture_show_toast"
    private const val PREF_SENSITIVITY = "calc_gesture_sensitivity_mode" // 0 = רגילה, 1 = גבוהה (החלקה קצרה), 2 = ארוכה

    var isGestureSectionExpanded = false

    // =========================================================================
    // 1. כל 13 סוגי ההחלקות והמחוות הנתמכות במסך המחשבון הרגיל והמדעי
    // =========================================================================
    val gestureTypes: List<CalcGestureType> = listOf(
        CalcGestureType("SWIPE_LEFT", "החלקה שמאלה", "גרירת אצבע מימין לשמאל על צג המחשבון", "⬅️", "ACT_AC"),
        CalcGestureType("SWIPE_RIGHT", "החלקה ימינה", "גרירת אצבע משמאל לימין על צג המחשבון", "➡️", "ACT_BACKSPACE"),
        CalcGestureType("SWIPE_UP", "החלקה למעלה", "גרירת אצבע מתחתית הצג כלפי מעלה", "⬆️", "ACT_VOICE_INPUT"),
        CalcGestureType("SWIPE_DOWN", "החלקה למטה", "גרירת אצבע מראש הצג כלפי מטה", "⬇️", "ACT_HISTORY"),
        CalcGestureType("SWIPE_UP_LEFT", "החלקה באלכסון למעלה-שמאלה", "גרירה אלכסונית לפינה השמאלית העליונה", "↖️", "ACT_CLEAR_ENTRY"),
        CalcGestureType("SWIPE_UP_RIGHT", "החלקה באלכסון למעלה-ימינה", "גרירה אלכסונית לפינה הימנית העליונה", "↗️", "ACT_TOGGLE_SCI"),
        CalcGestureType("SWIPE_DOWN_LEFT", "החלקה באלכסון למטה-שמאלה", "גרירה אלכסונית לפינה השמאלית התחתונה", "↙️", "ACT_NEGATE"),
        CalcGestureType("SWIPE_DOWN_RIGHT", "החלקה באלכסון למטה-ימינה", "גרירה אלכסונית לפינה הימנית התחתונה", "↘️", "ACT_TTS_SPEAK"),
        CalcGestureType("TWO_FINGER_LEFT", "החלקה ב-2 אצבעות שמאלה", "גרירת שתי אצבעות יחד שמאלה על הצג", "✌️⬅️", "ACT_MEM_CLEAR"),
        CalcGestureType("TWO_FINGER_RIGHT", "החלקה ב-2 אצבעות ימינה", "גרירת שתי אצבעות יחד ימינה על הצג", "✌️➡️", "ACT_MEM_RECALL"),
        CalcGestureType("SINGLE_TAP", "נקישה בודדת על הצג", "לחיצה קצרה אחת על מסך התוצאה", "👆", "ACT_COPY"),
        CalcGestureType("DOUBLE_TAP", "נקישה כפולה על הצג", "שתי לחיצות מהירות על מסך התוצאה", "👆👆", "ACT_PASTE"),
        CalcGestureType("LONG_PRESS", "לחיצה ארוכה על הצג", "החזקת האצבע על מסך התוצאה", "⏳", "ACT_PIN_FAV")
    )

    // =========================================================================
    // 2. קטלוג 36 הפעולות שניתן להצמיד לכל החלקה או מחווה
    // =========================================================================
    val availableActions: List<CalcGestureAction> = listOf(
        // מחיקה ועריכה
        CalcGestureAction("ACT_NONE", "ללא", "🚫 ללא פעולה (מבוטל)"),
        CalcGestureAction("ACT_AC", "מחיקה ועריכה", "🧹 מחיקת כל מסך המחשבון (כמו כפתור AC)"),
        CalcGestureAction("ACT_BACKSPACE", "מחיקה ועריכה", "⌫ מחיקת הספרה האחרונה (Backspace)"),
        CalcGestureAction("ACT_CLEAR_ENTRY", "מחיקה ועריכה", "🗑️ מחיקת המספר האחרון בלבד בתרגיל (CE)"),
        CalcGestureAction("ACT_EQUALS", "מחיקה ועריכה", "🟰 חישוב תוצאת התרגיל מיד (כמו =)"),
        CalcGestureAction("ACT_LAST_ANS", "מחיקה ועריכה", "🔄 שליפת התוצאה האחרונה מההיסטוריה (Ans)"),
        CalcGestureAction("ACT_WRAP_PARENS", "מחיקה ועריכה", "括号 עטיפת כל התרגיל בסוגריים (...)"),
        CalcGestureAction("ACT_APPEND_00", "מחיקה ועריכה", "0️⃣ הוספת פעמיים אפס (00)"),
        CalcGestureAction("ACT_APPEND_000", "מחיקה ועריכה", "0️⃣ הוספת שלוש פעמים אפס (000 - אלפים)"),

        // לוח גזירים וקול
        CalcGestureAction("ACT_COPY", "העתקה וקול", "📋 העתקת המספר / התוצאה ללוח"),
        CalcGestureAction("ACT_PASTE", "העתקה וקול", "📥 הדבקת מספר או תרגיל מהלוח לצג"),
        CalcGestureAction("ACT_COPY_FULL_EXPR", "העתקה וקול", "📝 העתקת התרגיל המלא עם התוצאה ללוח"),
        CalcGestureAction("ACT_TTS_SPEAK", "העתקה וקול", "🔊 הקראת התוצאה בקול בעברית (TTS)"),
        CalcGestureAction("ACT_VOICE_INPUT", "העתקה וקול", "🎙️ הפעלת הקלדה קולית במיקרופון"),

        // מתמטיקה מהירה על המספר בצג
        CalcGestureAction("ACT_NEGATE", "מתמטיקה מהירה", "± היפוך סימן המספר חיובי / שלילי (+/-)"),
        CalcGestureAction("ACT_SQRT", "מתמטיקה מהירה", "√ חישוב שורש ריבועי מיידי (√x)"),
        CalcGestureAction("ACT_SQUARE", "מתמטיקה מהירה", "x² העלאת המספר בריבוע (x²)"),
        CalcGestureAction("ACT_CUBE", "מתמטיקה מהירה", "x³ העלאת המספר בשלישית (x³)"),
        CalcGestureAction("ACT_RECIPROCAL", "מתמטיקה מהירה", "⅟ חישוב הופכי של המספר (1/x)"),
        CalcGestureAction("ACT_PERCENT_VAL", "מתמטיקה מהירה", "% חלוקת המספר ב-100 (המרה לאחוז)"),
        CalcGestureAction("ACT_FACTORIAL", "מתמטיקה מהירה", "❗ חישוב עצרת למספר (x!)"),
        CalcGestureAction("ACT_ADD_VAT", "מתמטיקה מהירה", "🧾 הוספת מע״מ 18% למספר בצג (+18%)"),
        CalcGestureAction("ACT_REMOVE_VAT", "מתמטיקה מהירה", "🧾 חילוץ מחיר ללא מע״מ 18% (÷1.18)"),
        CalcGestureAction("ACT_ROUND_INT", "מתמטיקה מהירה", "🎯 עיגול התוצאה למספר שלם קרוב"),
        CalcGestureAction("ACT_INSERT_PI", "מתמטיקה מהירה", "π הוספת הקבוע פאי (π) לתרגיל"),

        // זיכרון המחשבון
        CalcGestureAction("ACT_MEM_PLUS", "זיכרון (M)", "➕ הוספת המספר לזיכרון (M+)"),
        CalcGestureAction("ACT_MEM_MINUS", "זיכרון (M)", "➖ הפחתת המספר מהזיכרון (M-)"),
        CalcGestureAction("ACT_MEM_RECALL", "זיכרון (M)", "📤 קריאת המספר מהזיכרון לצג (MR)"),
        CalcGestureAction("ACT_MEM_CLEAR", "זיכרון (M)", "🧹 איפוס הזיכרון ל-0 (MC)"),

        // ניווט וכלים
        CalcGestureAction("ACT_TOGGLE_SCI", "ניווט וכלים", "🔬 מעבר מהיר בין מחשבון רגיל למחשבון מדעי"),
        CalcGestureAction("ACT_HISTORY", "ניווט וכלים", "🕒 פתיחת חלון היסטוריית החישובים"),
        CalcGestureAction("ACT_PIN_FAV", "ניווט וכלים", "⭐ נעיצת החישוב הנוכחי במועדפים"),
        CalcGestureAction("ACT_OPEN_FAV_CENTER", "ניווט וכלים", "🌟 מעבר למרכז המועדפים והחישובים הנעוצים"),
        CalcGestureAction("ACT_OPEN_SMART_NOTES", "ניווט וכלים", "📝 מעבר למחברת פתקים חכמים ופותר משוואות"),
        CalcGestureAction("ACT_ENTER_PIP", "ניווט וכלים", "🪟 מזעור המחשבון לחלון צף (PiP)"),
        CalcGestureAction("ACT_CYCLE_THEME", "ניווט וכלים", "🎨 החלפה מהירה בין 4 ערכות הנושא")
    )

    fun getAssignedActionId(prefs: android.content.SharedPreferences?, gestureId: String): String {
        val defaultId = gestureTypes.find { it.id == gestureId }?.defaultActionId ?: "ACT_NONE"
        return prefs?.getString(PREF_PREFIX + gestureId, defaultId) ?: defaultId
    }

    fun setAssignedActionId(prefs: android.content.SharedPreferences?, gestureId: String, actionId: String) {
        prefs?.edit()?.putString(PREF_PREFIX + gestureId, actionId)?.apply()
    }

    fun getActionTitle(actionId: String): String {
        return availableActions.find { it.id == actionId }?.title ?: "ללא פעולה"
    }

    fun isShowToastEnabled(prefs: android.content.SharedPreferences?): Boolean {
        return prefs?.getBoolean(PREF_SHOW_TOAST, true) ?: true
    }

    fun setShowToastEnabled(prefs: android.content.SharedPreferences?, enabled: Boolean) {
        prefs?.edit()?.putBoolean(PREF_SHOW_TOAST, enabled)?.apply()
    }

    fun getSensitivityMode(prefs: android.content.SharedPreferences?): Int {
        return (prefs?.getInt(PREF_SENSITIVITY, 0) ?: 0).coerceIn(0, 2)
    }

    fun setSensitivityMode(prefs: android.content.SharedPreferences?, mode: Int) {
        prefs?.edit()?.putInt(PREF_SENSITIVITY, mode.coerceIn(0, 2))?.apply()
    }

    fun resetToDefaultGestures(prefs: android.content.SharedPreferences?) {
        val ed = prefs?.edit() ?: return
        for (g in gestureTypes) {
            ed.putString(PREF_PREFIX + g.id, g.defaultActionId)
        }
        ed.putInt(PREF_SENSITIVITY, 0)
        ed.apply()
    }

    fun applyGesturePresetProfile(prefs: android.content.SharedPreferences?, profileIndex: Int) {
        val ed = prefs?.edit() ?: return
        when (profileIndex) {
            0 -> {
                // פרופיל 1: החלקה שמאלה = AC, ימינה = מחיקת ספרה
                resetToDefaultGestures(prefs)
            }
            1 -> {
                // פרופיל 2: מהיר ביד אחת (שמאלה = מחיקת ספרה, ימינה = AC, למעלה = =, למטה = היסטוריה)
                ed.putString(PREF_PREFIX + "SWIPE_LEFT", "ACT_BACKSPACE")
                ed.putString(PREF_PREFIX + "SWIPE_RIGHT", "ACT_AC")
                ed.putString(PREF_PREFIX + "SWIPE_UP", "ACT_EQUALS")
                ed.putString(PREF_PREFIX + "SWIPE_DOWN", "ACT_HISTORY")
                ed.putString(PREF_PREFIX + "DOUBLE_TAP", "ACT_TOGGLE_SCI")
            }
            2 -> {
                // פרופיל 3: מדעי ומתמטי (אלכסונים לשורש, ריבוע, מע"מ ומעבר למדעי)
                ed.putString(PREF_PREFIX + "SWIPE_LEFT", "ACT_AC")
                ed.putString(PREF_PREFIX + "SWIPE_RIGHT", "ACT_BACKSPACE")
                ed.putString(PREF_PREFIX + "SWIPE_UP", "ACT_TOGGLE_SCI")
                ed.putString(PREF_PREFIX + "SWIPE_DOWN", "ACT_HISTORY")
                ed.putString(PREF_PREFIX + "SWIPE_UP_LEFT", "ACT_SQRT")
                ed.putString(PREF_PREFIX + "SWIPE_UP_RIGHT", "ACT_SQUARE")
                ed.putString(PREF_PREFIX + "SWIPE_DOWN_LEFT", "ACT_ADD_VAT")
                ed.putString(PREF_PREFIX + "SWIPE_DOWN_RIGHT", "ACT_REMOVE_VAT")
            }
        }
        ed.apply()
    }

    // =========================================================================
    // 3. חיבור גלאי ההחלקות החכם לצג המחשבון הרגיל והמדעי (attachToCalculatorDisplay)
    // =========================================================================
    fun attachToCalculatorDisplay(activity: MainActivity, displayCard: View) {
        var downX = 0f
        var downY = 0f
        var downTime = 0L
        var maxPointers = 1
        var lastTapTime = 0L
        var longPressTriggered = false
        val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())

        val longPressRunnable = Runnable {
            if (maxPointers == 1) {
                longPressTriggered = true
                val actId = getAssignedActionId(activity.prefs, "LONG_PRESS")
                if (actId != "ACT_NONE") {
                    executeGestureAction(activity, displayCard, "LONG_PRESS", actId)
                }
            }
        }

        val pendingSingleTapRunnable = Runnable {
            val actId = getAssignedActionId(activity.prefs, "SINGLE_TAP")
            if (actId != "ACT_NONE") {
                executeGestureAction(activity, displayCard, "SINGLE_TAP", actId)
            }
        }

        displayCard.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    downY = event.y
                    downTime = System.currentTimeMillis()
                    maxPointers = 1
                    longPressTriggered = false
                    handler.postDelayed(longPressRunnable, longPressTimeout)
                    true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    maxPointers = max(maxPointers, event.pointerCount)
                    handler.removeCallbacks(longPressRunnable)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dist = hypot((event.x - downX).toDouble(), (event.y - downY).toDouble())
                    if (dist > activity.dp(16f)) {
                        handler.removeCallbacks(longPressRunnable)
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    false
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    if (longPressTriggered) return@setOnTouchListener true

                    val dx = event.x - downX
                    val dy = event.y - downY
                    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                    val sensMode = getSensitivityMode(activity.prefs)
                    val minSwipePx = when (sensMode) {
                        1 -> activity.dp(28f).toFloat() // רגישות גבוהה (החלקה קצרה)
                        2 -> activity.dp(64f).toFloat() // החלקה ארוכה
                        else -> activity.dp(42f).toFloat() // רגילה
                    }

                    if (dist >= minSwipePx) {
                        handler.removeCallbacks(pendingSingleTapRunnable)
                        val gestureId = classifySwipeDirection(activity, dx, dy, maxPointers)
                        val actionId = getAssignedActionId(activity.prefs, gestureId)
                        executeGestureAction(activity, v, gestureId, actionId)
                    } else if (dist < activity.dp(14f)) {
                        val now = System.currentTimeMillis()
                        val doubleTapAction = getAssignedActionId(activity.prefs, "DOUBLE_TAP")
                        if (now - lastTapTime < 280L && doubleTapAction != "ACT_NONE") {
                            handler.removeCallbacks(pendingSingleTapRunnable)
                            lastTapTime = 0L
                            executeGestureAction(activity, v, "DOUBLE_TAP", doubleTapAction)
                        } else {
                            lastTapTime = now
                            if (doubleTapAction == "ACT_NONE") {
                                pendingSingleTapRunnable.run()
                            } else {
                                handler.postDelayed(pendingSingleTapRunnable, 250L)
                            }
                        }
                    }
                    true
                }
                else -> false
            }
        }
    }

    // מסווג את זווית ההחלקה ל-4 כיוונים או 8 כיוונים (אם הוגדרה פעולה לאלכסון) או 2 אצבעות
    private fun classifySwipeDirection(activity: MainActivity, dx: Float, dy: Float, pointers: Int): String {
        if (pointers >= 2) {
            return if (dx < 0f) "TWO_FINGER_LEFT" else "TWO_FINGER_RIGHT"
        }

        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (deg < 0f) deg += 360f

        // בדיקה אם הזווית היא אלכסונית והמשתמש הגדיר פעולה פעילה לאותו אלכסון
        val diagonalCandidate: String? = when {
            deg in 202.5f..247.5f -> "SWIPE_UP_LEFT"
            deg in 292.5f..337.5f -> "SWIPE_UP_RIGHT"
            deg in 112.5f..157.5f -> "SWIPE_DOWN_LEFT"
            deg in 22.5f..67.5f -> "SWIPE_DOWN_RIGHT"
            else -> null
        }

        if (diagonalCandidate != null) {
            val diagAct = getAssignedActionId(activity.prefs, diagonalCandidate)
            if (diagAct != "ACT_NONE") return diagonalCandidate
        }

        // אם האלכסון מוגדר כ"ללא פעולה" או שההחלקה ישרה — נתייחס ל-4 הכיוונים הראשיים
        return if (abs(dx) >= abs(dy)) {
            if (dx < 0f) "SWIPE_LEFT" else "SWIPE_RIGHT"
        } else {
            if (dy < 0f) "SWIPE_UP" else "SWIPE_DOWN"
        }
    }

    // =========================================================================
    // 4. ביצוע הפעולה שנבחרה עבור ההחלקה (executeGestureAction)
    // =========================================================================
    fun executeGestureAction(activity: MainActivity, targetView: View, gestureId: String, actionId: String) {
        if (actionId == "ACT_NONE") return

        // משוב צליל ורטט מותאם (אם הפעולה היא AC נשמיע את צליל ה-AC!)
        activity.feedback(targetView, if (actionId == "ACT_AC") "AC" else "")

        // אנימציית תגובה קלה לצג המחשבון בכיוון ההחלקה
        if (activity.useAnimations) {
            val shiftX = when {
                gestureId.contains("LEFT") -> -activity.dp(10f).toFloat()
                gestureId.contains("RIGHT") -> activity.dp(10f).toFloat()
                else -> 0f
            }
            val shiftY = when {
                gestureId.contains("UP") -> -activity.dp(8f).toFloat()
                gestureId.contains("DOWN") -> activity.dp(8f).toFloat()
                else -> 0f
            }
            if (shiftX != 0f || shiftY != 0f) {
                targetView.translationX = shiftX
                targetView.translationY = shiftY
                targetView.animate().translationX(0f).translationY(0f).setDuration(180).setInterpolator(DecelerateInterpolator()).start()
            }
        }

        fun evaluateCurrentOrZero(): Double {
            return try {
                val raw = activity.currentInput.ifEmpty { "0" }
                val v = Utils.evalMathExpression(raw)
                if (v.isNaN() || v.isInfinite()) 0.0 else v
            } catch (_: Throwable) {
                0.0
            }
        }

        fun applyUnaryMathTransform(labelPrefix: String, labelSuffix: String = "", transform: (Double) -> Double) {
            try {
                val origStr = activity.currentInput.ifEmpty { "0" }
                val baseVal = evaluateCurrentOrZero()
                val resVal = transform(baseVal)
                if (resVal.isNaN() || resVal.isInfinite()) {
                    activity.currentInput = "Error"
                    activity.currentView?.text = "Error"
                    activity.justCalculated = true
                    return
                }
                val formatted = Utils.formatResult(resVal, activity.calcPrecision)
                val exprStr = "$labelPrefix$origStr$labelSuffix"
                activity.expressionView?.text = "$exprStr ="
                activity.historyList.add(0, "$exprStr = $formatted")
                activity.savePreferences()
                activity.currentInput = formatted
                activity.currentView?.text = formatted
                activity.justCalculated = true
            } catch (_: Throwable) {}
        }

        when (actionId) {
            "ACT_AC" -> activity.onEquationButtonClick("AC")
            "ACT_BACKSPACE" -> activity.onEquationButtonClick("⌫")
            "ACT_EQUALS" -> activity.onEquationButtonClick("=")
            "ACT_CLEAR_ENTRY" -> {
                val cur = activity.currentInput
                val lastOpIdx = cur.indexOfLast { it in listOf('+', '−', '-', '×', '÷', '*', '/', '^', '(') }
                activity.currentInput = if (lastOpIdx >= 0) cur.substring(0, lastOpIdx + 1) else ""
                activity.currentView?.text = activity.currentInput.ifEmpty { "0" }
            }
            "ACT_LAST_ANS" -> {
                val lastAns = activity.historyList.firstOrNull()?.substringAfter("=", "")?.trim() ?: ""
                if (lastAns.isNotEmpty()) {
                    if (activity.justCalculated) {
                        activity.currentInput = lastAns
                        activity.justCalculated = false
                    } else {
                        activity.currentInput += lastAns
                    }
                    activity.currentView?.text = activity.currentInput
                }
            }
            "ACT_WRAP_PARENS" -> {
                if (activity.currentInput.isNotEmpty()) {
                    activity.currentInput = "(${activity.currentInput})"
                    activity.currentView?.text = activity.currentInput
                    activity.justCalculated = false
                }
            }
            "ACT_APPEND_00" -> {
                activity.onEquationButtonClick("0")
                activity.onEquationButtonClick("0")
            }
            "ACT_APPEND_000" -> {
                activity.onEquationButtonClick("0")
                activity.onEquationButtonClick("0")
                activity.onEquationButtonClick("0")
            }
            "ACT_COPY" -> {
                activity.copyToClipboard(activity.currentInput.ifEmpty { "0" })
            }
            "ACT_PASTE" -> {
                val cb = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clipTxt = cb?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
                val cleaned = SmartNotesMathEngine.cleanMathString(clipTxt)
                    .filter { it.isDigit() || it in ".+-*/^()" }
                if (cleaned.isNotEmpty()) {
                    activity.currentInput = cleaned
                    activity.currentView?.text = cleaned
                    activity.justCalculated = false
                    Toast.makeText(activity, "הודבק מהלוח: $cleaned", Toast.LENGTH_SHORT).show()
                }
            }
            "ACT_COPY_FULL_EXPR" -> {
                val expr = activity.expressionView?.text?.toString()?.trim() ?: ""
                val cur = activity.currentInput.ifEmpty { "0" }
                val full = if (expr.isNotEmpty()) "$expr $cur" else cur
                activity.copyToClipboard(full)
            }
            "ACT_TTS_SPEAK" -> {
                val textToRead = if (activity.currentInput.isEmpty() || activity.currentInput == "0") "אפס" else activity.currentInput
                activity.tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null)
                Toast.makeText(activity, "🔊 מקריא: $textToRead", Toast.LENGTH_SHORT).show()
            }
            "ACT_VOICE_INPUT" -> {
                activity.startVoiceRecognition()
            }
            "ACT_NEGATE" -> {
                val cur = activity.currentInput
                if (cur.isEmpty() || cur == "0") {
                    activity.currentInput = "-"
                } else if (cur.startsWith("-")) {
                    activity.currentInput = cur.removePrefix("-")
                } else {
                    activity.currentInput = "-$cur"
                }
                activity.currentView?.text = activity.currentInput.ifEmpty { "0" }
                activity.justCalculated = false
            }
            "ACT_SQRT" -> applyUnaryMathTransform("√(", ")") { if (it < 0) Double.NaN else sqrt(it) }
            "ACT_SQUARE" -> applyUnaryMathTransform("(", ")²") { it * it }
            "ACT_CUBE" -> applyUnaryMathTransform("(", ")³") { it * it * it }
            "ACT_RECIPROCAL" -> applyUnaryMathTransform("1/(", ")") { if (abs(it) < 1e-15) Double.NaN else 1.0 / it }
            "ACT_PERCENT_VAL" -> applyUnaryMathTransform("(", ")%") { it / 100.0 }
            "ACT_FACTORIAL" -> applyUnaryMathTransform("(", ")!") { n ->
                if (n < 0 || n > 170 || abs(n - n.roundToInt()) > 1e-9) Double.NaN
                else {
                    var r = 1.0
                    for (i in 2..n.roundToInt()) r *= i
                    r
                }
            }
            "ACT_ADD_VAT" -> applyUnaryMathTransform("(", ") + 18% מע״מ") { it * 1.18 }
            "ACT_REMOVE_VAT" -> applyUnaryMathTransform("(", ") ללא מע״מ") { it / 1.18 }
            "ACT_ROUND_INT" -> applyUnaryMathTransform("round(", ")") { it.roundToLong().toDouble() }
            "ACT_INSERT_PI" -> activity.onEquationButtonClick("π")
            "ACT_MEM_PLUS" -> activity.onEquationButtonClick("M+")
            "ACT_MEM_MINUS" -> activity.onEquationButtonClick("M-")
            "ACT_MEM_RECALL" -> activity.onEquationButtonClick("MR")
            "ACT_MEM_CLEAR" -> activity.onEquationButtonClick("MC")
            "ACT_TOGGLE_SCI" -> {
                val nextTab = if (activity.currentTabIndex == 1) 0 else 1
                activity.switchTab(nextTab)
            }
            "ACT_HISTORY" -> activity.showHistoryDialog()
            "ACT_PIN_FAV" -> {
                val cur = activity.currentInput.ifEmpty { "0" }
                val expr = activity.expressionView?.text?.toString()?.trim() ?: ""
                val entryText = if (expr.isNotEmpty()) "$expr $cur" else cur
                val existing = activity.prefs?.getString("pro_pinned_calcs_raw", "") ?: ""
                val entry = "מחשבון ראשי:\n$entryText"
                val updated = if (existing.isBlank()) entry else "$entry||$existing"
                activity.prefs?.edit()?.putString("pro_pinned_calcs_raw", updated)?.apply()
                Toast.makeText(activity, "⭐ החישוב ($entryText) ננעץ במועדפים!", Toast.LENGTH_SHORT).show()
            }
            "ACT_OPEN_FAV_CENTER" -> activity.switchTab(ProFavoritesEngine.FAVORITES_CENTER_TAB_INDEX)
            "ACT_OPEN_SMART_NOTES" -> {
                ProState.setProEnabled(activity.prefs, true)
                activity.switchTab(78)
            }
            "ACT_ENTER_PIP" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activity.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(3, 4)).build())
                }
            }
            "ACT_CYCLE_THEME" -> {
                when {
                    !activity.isIosGlassMode && !activity.isAuroraMode && !activity.isMinimalistMode -> {
                        activity.isIosGlassMode = true
                    }
                    activity.isIosGlassMode -> {
                        activity.isIosGlassMode = false
                        activity.isAuroraMode = true
                    }
                    activity.isAuroraMode -> {
                        activity.isAuroraMode = false
                        activity.isMinimalistMode = true
                    }
                    else -> {
                        activity.isMinimalistMode = false
                    }
                }
                val defaultPreset = MinimalistTheme.getPalettesForCurrentTheme(activity).first()
                MinimalistTheme.applyThemePalettePreset(activity, defaultPreset)
            }
        }
    }

    // =========================================================================
    // 5. סמל וקטורי לכפתור "החלקות במסך המחשבון" בהגדרות (לכל 4 ערכות הנושא)
    // =========================================================================
    fun createGestureVectorIcon(
        context: Context,
        tintColor: Int,
        sizeDp: Float = 22f,
        isMinimalist: Boolean = false,
        isAurora: Boolean = false
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isAurora) {
                    setShadowLayer(5f * d, 0f, 0f, Color.argb(170, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val badgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isMinimalist) MinimalistTheme.ACCENT_PASTEL_BLUE else Color.argb(38, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.40f

                if (isMinimalist) {
                    canvas.drawCircle(cx, cy, r * 0.96f, badgeP)
                } else {
                    canvas.drawRoundRect(RectF(cx - r * 1.1f, cy - r * 1.1f, cx + r * 1.1f, cy + r * 1.1f), r * 0.44f, r * 0.44f, badgeP)
                }

                // חץ החלקה דו-כיווני למעלה + נקודת מגע ואצבע
                canvas.drawLine(cx - r * 0.68f, cy - r * 0.44f, cx + r * 0.68f, cy - r * 0.44f, strokeP)
                canvas.drawLine(cx - r * 0.68f, cy - r * 0.44f, cx - r * 0.42f, cy - r * 0.68f, strokeP)
                canvas.drawLine(cx - r * 0.68f, cy - r * 0.44f, cx - r * 0.42f, cy - r * 0.20f, strokeP)
                canvas.drawLine(cx + r * 0.68f, cy - r * 0.44f, cx + r * 0.42f, cy - r * 0.68f, strokeP)
                canvas.drawLine(cx + r * 0.68f, cy - r * 0.44f, cx + r * 0.42f, cy - r * 0.20f, strokeP)

                canvas.drawCircle(cx, cy + r * 0.05f, r * 0.18f, strokeP)
                canvas.drawArc(RectF(cx - r * 0.42f, cy + r * 0.18f, cx + r * 0.42f, cy + r * 0.88f), 180f, 180f, false, strokeP)
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    fun applyGestureButtonIcon(settingsRoot: View?, activity: MainActivity) {
        if (settingsRoot == null) return
        val btn = settingsRoot.findViewWithTag<TextView>("GESTURE_SETTINGS_OPEN_BTN") ?: return
        val iconTint = when {
            activity.isMinimalistMode -> MinimalistTheme.TEXT_CHARCOAL
            else -> MinimalistTheme.resolveDrawerIconColor(activity, isSelected = false, isExpandedHeader = false)
        }
        val icon = createGestureVectorIcon(
            context = activity,
            tintColor = iconTint,
            sizeDp = 22f,
            isMinimalist = activity.isMinimalistMode,
            isAurora = activity.isAuroraMode
        )
        btn.compoundDrawablePadding = activity.dp(12f)
        if (activity.isRightHanded) {
            btn.setCompoundDrawables(null, null, icon, null)
        } else {
            btn.setCompoundDrawables(icon, null, null, null)
        }
    }

    // =========================================================================
    // 6. תפריט "החלקות במסך המחשבון" המתקפל במסך ההגדרות (עם כפתור "סיום ✔️")
    // =========================================================================
    fun createCollapsibleGesturesSettingsSection(activity: MainActivity): View {
        val isMin = activity.isMinimalistMode
        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY
        val accentCol = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }

        val wrapper = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = activity.dp(4f); bottomMargin = activity.dp(8f) }
        }

        lateinit var openBtn: TextView
        lateinit var panelContainer: LinearLayout
        val actionLabelMap = mutableMapOf<String, TextView>()

        fun getActiveCount(): Int {
            return gestureTypes.count { getAssignedActionId(activity.prefs, it.id) != "ACT_NONE" }
        }

        fun getOpenBtnText(): String {
            val active = getActiveCount()
            val raw = "👆 החלקות במסך המחשבון ($active/${gestureTypes.size} פעילות)"
            return IosGlassTheme.cleanCategoryTitle(raw, activity.isCleanGlassActive)
        }

        fun refreshAllRows() {
            for (g in gestureTypes) {
                val actId = getAssignedActionId(activity.prefs, g.id)
                actionLabelMap[g.id]?.text = getActionTitle(actId)
            }
        }

        fun closePanel() {
            isGestureSectionExpanded = false
            panelContainer.visibility = View.GONE
            openBtn.visibility = View.VISIBLE
            openBtn.text = getOpenBtnText()
            applyGestureButtonIcon(wrapper, activity)
            activity.savePreferences()
        }

        openBtn = activity.createActionBtn(getOpenBtnText(), false) {
            isGestureSectionExpanded = true
            openBtn.visibility = View.GONE
            panelContainer.visibility = View.VISIBLE
            if (activity.useAnimations) {
                panelContainer.alpha = 0f
                panelContainer.animate().alpha(1f).setDuration(200).start()
            }
        }.apply {
            tag = "GESTURE_SETTINGS_OPEN_BTN"
            visibility = if (isGestureSectionExpanded) View.GONE else View.VISIBLE
        }

        panelContainer = LinearLayout(activity).apply {
            tag = "MINIMALIST_CUSTOMIZER_IGNORE"
            orientation = LinearLayout.VERTICAL
            visibility = if (isGestureSectionExpanded) View.VISIBLE else View.GONE
            setPadding(activity.dp(14f), activity.dp(14f), activity.dp(14f), activity.dp(14f))
            background = when {
                isMin -> MinimalistTheme.createMinimalistCardDrawable(activity, 22f, 0)
                activity.isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(activity, 22f)
                else -> GradientDrawable().apply {
                    setColor(Utils.lightenColor(activity.bgColor, 0.06f))
                    cornerRadius = activity.dp(22f).toFloat()
                    setStroke(activity.dp(1f), MinimalistTheme.resolveBorderStrokeColor(activity, Utils.lightenColor(activity.bgColor, 0.16f)))
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }

        val topDoneBtn = activity.createActionBtn("סיום ✔️", true) {
            closePanel()
        }
        panelContainer.addView(topDoneBtn)

        val headerTitle = TextView(activity).apply {
            text = "👆 הגדרת החלקות ומחוות במסך המחשבון הרגיל והמדעי"
            setTextColor(textPrimary)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(4f), activity.dp(4f), activity.dp(4f), activity.dp(2f))
        }
        val headerSub = TextView(activity).apply {
            text = "בחר עבור כל כיוון החלקה על צג התוצאה איזו פעולה תתבצע מיד (${availableActions.size} אפשרויות לבחירה):"
            setTextColor(textSecondary)
            textSize = 13f
            gravity = Gravity.RIGHT
            setPadding(activity.dp(4f), 0, activity.dp(4f), activity.dp(10f))
        }
        panelContainer.addView(headerTitle)
        panelContainer.addView(headerSub)

        // משטח בדיקת החלקות חי (Live Test Pad) בתוך ההגדרות
        var testStatusText = "👆 החלק כאן לכל כיוון כדי לבדוק את ההחלקה בזמן אמת"
        var lastArrowPreview = "↔️ ↕️"
        val liveTestPad = object : View(activity) {
            private val p = Paint(Paint.ANTI_ALIAS_FLAG)
            private var tDownX = 0f
            private var tDownY = 0f
            private var tPointers = 1

            override fun onTouchEvent(event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        parent?.requestDisallowInterceptTouchEvent(true)
                        tDownX = event.x
                        tDownY = event.y
                        tPointers = 1
                        return true
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        tPointers = max(tPointers, event.pointerCount)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        parent?.requestDisallowInterceptTouchEvent(false)
                        val dx = event.x - tDownX
                        val dy = event.y - tDownY
                        val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        val gId = if (dist >= activity.dp(26f)) {
                            classifySwipeDirection(activity, dx, dy, tPointers)
                        } else {
                            "SINGLE_TAP"
                        }
                        val gSpec = gestureTypes.find { it.id == gId }
                        val actId = getAssignedActionId(activity.prefs, gId)
                        lastArrowPreview = gSpec?.badgeSymbol ?: "👆"
                        testStatusText = "זוהתה: ${gSpec?.title ?: gId}  ➔  ${getActionTitle(actId)}"
                        activity.feedback(this)
                        invalidate()
                        return true
                    }
                }
                return true
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val d = resources.displayMetrics.density
                val w = width.toFloat(); val h = height.toFloat()

                val box = RectF(4f * d, 4f * d, w - 4f * d, h - 4f * d)
                p.style = Paint.Style.FILL
                p.color = if (isMin) MinimalistTheme.ACCENT_PASTEL_BLUE else Color.argb(45, Color.red(accentCol), Color.green(accentCol), Color.blue(accentCol))
                canvas.drawRoundRect(box, 16f * d, 16f * d, p)

                p.style = Paint.Style.STROKE
                p.strokeWidth = 1.4f * d
                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else accentCol
                canvas.drawRoundRect(box, 16f * d, 16f * d, p)

                p.style = Paint.Style.FILL
                p.textAlign = Paint.Align.CENTER
                p.typeface = Typeface.DEFAULT_BOLD
                p.color = textPrimary
                p.textSize = 20f * d
                canvas.drawText(lastArrowPreview, w / 2f, h * 0.42f, p)

                p.textSize = 12.5f * d
                CanvasUiHelper.drawFittedText(canvas, "\u200F$testStatusText", w / 2f, h * 0.78f, w - 24f * d, 12.5f * d, 9f * d, p)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(88f)
            ).apply { bottomMargin = activity.dp(10f) }
        }
        panelContainer.addView(liveTestPad)

        // פרופילים מהירים + רגישות החלקה
        val presetsScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }
        val presetsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val profileSpecs = listOf(
            0 to "⚡ ברירת מחדל (שמאל = AC)",
            1 to "🤚 הפוך (שמאל = ⌫, ימין = AC)",
            2 to "🔬 פרופיל מדעי ומע״מ"
        )
        for ((pIdx, pTitle) in profileSpecs) {
            val chip = TextView(activity).apply {
                tag = "PRO_STAR_CHIP"
                text = pTitle
                setTextColor(textPrimary)
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(activity.dp(12f), activity.dp(8f), activity.dp(12f), activity.dp(8f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(999f).toFloat()
                    setColor(if (isMin) MinimalistTheme.ACCENT_BUTTER_YELLOW else Color.argb(45, 255, 255, 255))
                    setStroke(activity.dp(1f), accentCol)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(activity.dp(3f), 0, activity.dp(3f), 0) }
                setOnClickListener {
                    activity.feedback(it)
                    applyGesturePresetProfile(activity.prefs, pIdx)
                    refreshAllRows()
                    Toast.makeText(activity, "✅ הופעל: $pTitle", Toast.LENGTH_SHORT).show()
                }
            }
            presetsRow.addView(chip)
        }
        presetsScroll.addView(presetsRow)
        panelContainer.addView(presetsScroll)

        // כפתור רגישות החלקה (אורך ההחלקה הנדרש)
        val sensNames = listOf("רגילה (מומלץ)", "גבוהה (החלקה קצרה ומהירה)", "ארוכה (מונע החלקות אקראיות)")
        val sensBtn = activity.createActionBtn(
            "📏 רגישות אורך החלקה: ${sensNames[getSensitivityMode(activity.prefs)]}",
            false
        ) { btnView ->
            val nextMode = (getSensitivityMode(activity.prefs) + 1) % 3
            setSensitivityMode(activity.prefs, nextMode)
            (btnView as TextView).text = "📏 רגישות אורך החלקה: ${sensNames[nextMode]}"
        }
        panelContainer.addView(sensBtn)

        // רשימת כל 13 סוגי ההחלקות והמחוות לבחירה אישית
        for (g in gestureTypes) {
            val cardRow = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(activity.dp(14f), activity.dp(11f), activity.dp(14f), activity.dp(11f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(16f).toFloat()
                    setColor(if (isMin) MinimalistTheme.CARD_WHITE else Color.argb(32, 255, 255, 255))
                    setStroke(
                        activity.dp(1f),
                        if (isMin) Color.parseColor("#E3DFD9") else Color.argb(45, 255, 255, 255)
                    )
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = activity.dp(7f) }
            }

            val topLine = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }

            val badgeTv = TextView(activity).apply {
                text = g.badgeSymbol
                setTextColor(textPrimary)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(activity.dp(10f), activity.dp(5f), activity.dp(10f), activity.dp(5f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(12f).toFloat()
                    setColor(if (isMin) MinimalistTheme.NEUTRAL_SECONDARY_BG else Color.argb(50, 255, 255, 255))
                }
            }

            val titleCol = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.RIGHT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = activity.dp(10f)
                }
            }

            val gTitleTv = TextView(activity).apply {
                text = g.title
                setTextColor(textPrimary)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT
            }
            val gSubTv = TextView(activity).apply {
                text = g.subtitle
                setTextColor(textSecondary)
                textSize = 12f
                gravity = Gravity.RIGHT
            }
            titleCol.addView(gTitleTv)
            titleCol.addView(gSubTv)

            topLine.addView(badgeTv)
            topLine.addView(titleCol)

            val currentActId = getAssignedActionId(activity.prefs, g.id)
            val chosenActionPill = TextView(activity).apply {
                tag = "PRO_STAR_CHIP"
                text = getActionTitle(currentActId)
                val pillBg = if (isMin) MinimalistTheme.ACCENT_MUTED_GREEN else Color.argb(55, Color.red(accentCol), Color.green(accentCol), Color.blue(accentCol))
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, pillBg, 2.3))
                textSize = 13.5f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                setPadding(activity.dp(14f), activity.dp(9f), activity.dp(14f), activity.dp(9f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(12f).toFloat()
                    setColor(pillBg)
                    setStroke(activity.dp(1f), accentCol)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = activity.dp(7f) }
            }
            actionLabelMap[g.id] = chosenActionPill

            val pickClickListener = View.OnClickListener {
                activity.feedback(it)
                val options = availableActions.map { act -> "[${act.category}]  ${act.title}" }
                activity.showItemPicker("בחר פעולה עבור: ${g.title}", options) { chosenStr ->
                    val pickedIndex = options.indexOf(chosenStr)
                    if (pickedIndex in availableActions.indices) {
                        val pickedAction = availableActions[pickedIndex]
                        setAssignedActionId(activity.prefs, g.id, pickedAction.id)
                        chosenActionPill.text = pickedAction.title
                        testStatusText = "עודכן: ${g.title}  ➔  ${pickedAction.title}"
                        liveTestPad.invalidate()
                    }
                }
            }

            cardRow.setOnClickListener(pickClickListener)
            chosenActionPill.setOnClickListener(pickClickListener)

            cardRow.addView(topLine)
            cardRow.addView(chosenActionPill)
            panelContainer.addView(cardRow)
        }

        val resetBtn = activity.createActionBtn("↺ אפס את כל ההחלקות לברירת המחדל", false) {
            resetToDefaultGestures(activity.prefs)
            refreshAllRows()
            sensBtn.text = "📏 רגישות אורך החלקה: ${sensNames[0]}"
            Toast.makeText(activity, "החלקות המחשבון אופסו לברירת המחדל", Toast.LENGTH_SHORT).show()
        }
        panelContainer.addView(resetBtn)

        val bottomDoneBtn = activity.createActionBtn("סיום ✔️", true) {
            closePanel()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = activity.dp(8f) }
        }
        panelContainer.addView(bottomDoneBtn)

        wrapper.addView(openBtn)
        wrapper.addView(panelContainer)
        return wrapper
    }
}

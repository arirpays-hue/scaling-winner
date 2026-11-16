package com.example.calculator

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import java.util.Locale
import kotlin.math.*

object MinimalistTheme {

    const val MINIMALIST_THEME_NAME = "עיצוב מינימליסטי"

    // =========================================================================
    // צבעי ברירת המחדל המקוריים של "עיצוב מינימליסטי" (Pastel Light / Material You)
    // =========================================================================
    private val DEFAULT_BG_CREAM = Color.parseColor("#FCF9F5")
    private val DEFAULT_DRAWER_HEADER_YELLOW = Color.parseColor("#FFE094")
    private val DEFAULT_TEXT_CHARCOAL = Color.parseColor("#2C2C2C")
    private val DEFAULT_TEXT_MUTED_GREY = Color.parseColor("#424242")
    private val DEFAULT_NEUTRAL_SECONDARY_BG = Color.parseColor("#F1EDE9")
    private val DEFAULT_CARD_WHITE = Color.parseColor("#FFFFFF")
    private val DEFAULT_ACCENT_SAGE_GREEN = Color.parseColor("#A3D5B5")
    private val DEFAULT_ACCENT_MUTED_GREEN = Color.parseColor("#E2F1E7")
    private val DEFAULT_ACCENT_SKY_BLUE = Color.parseColor("#B1D4F9")
    private val DEFAULT_ACCENT_PASTEL_BLUE = Color.parseColor("#D7E9FC")
    private val DEFAULT_ACCENT_MUTED_PEACH = Color.parseColor("#FF9E80")
    private val DEFAULT_ACCENT_TERRACOTTA = Color.parseColor("#FFA48E")
    private val DEFAULT_ACCENT_BUTTER_YELLOW = Color.parseColor("#FFE094")
    private val DEFAULT_ACCENT_WARM_MUSTARD = Color.parseColor("#FFD54F")

    // =========================================================================
    // מערכת 10 ערוצי צבע מותאמים אישית לכל אלמנט באפליקציה
    // =========================================================================
    const val SLOT_ACTION_1 = 0       // כפתורי פעולה 1 (÷ − + וכפתורים ראשיים)
    const val SLOT_ACTION_2 = 1       // כפתורי פעולה 2 (= × וכפתורים משניים / צהוב)
    const val SLOT_LOGO_BG = 2        // צבע לוגו 1 (רקע באנר לוגו בתפריט הצד)
    const val SLOT_LOGO_TEXT = 3      // צבע לוגו 2 (טקסט וסמל הלוגו בתפריט הצד)
    const val SLOT_MAIN_BG = 4        // צבע רקע ראשי (מסך ותפריט צד)
    const val SLOT_CARDS_INPUTS = 5   // צבע כרטיסים ותיבות קלט
    const val SLOT_MEM_CMD_KEYS = 6   // צבע כפתורי זיכרון ופקודות (AC, ⌫, %, MC, MR, M+, M-)
    const val SLOT_NUM_KEYS = 7       // צבע מקשי מספרים (0-9)
    const val SLOT_ACTIVE_PILL = 8    // צבע פריט פעיל בתפריט וכותרות Pro
    const val SLOT_TEXT_ICONS = 9     // צבע טקסט ואייקונים ראשי
    const val TOTAL_COLOR_SLOTS = 10

    val slotCustomized = BooleanArray(TOTAL_COLOR_SLOTS) { false }
    val slotBaseColors = IntArray(TOTAL_COLOR_SLOTS)
    val slotBrightness = FloatArray(TOTAL_COLOR_SLOTS) { 1.0f }
    val slotOpacity = FloatArray(TOTAL_COLOR_SLOTS) { 1.0f }
    var selectedElementSlot: Int = SLOT_ACTION_1

    init {
        resetSlotsArrayToDefaults()
    }

    private fun resetSlotsArrayToDefaults() {
        val defaults = getDefaultSlotColors()
        for (i in 0 until TOTAL_COLOR_SLOTS) {
            slotCustomized[i] = false
            slotBaseColors[i] = defaults[i]
            slotBrightness[i] = 1.0f
            slotOpacity[i] = 1.0f
        }
    }

    private fun getDefaultSlotColors(): IntArray = intArrayOf(
        DEFAULT_ACCENT_TERRACOTTA,     // 0: כפתורי פעולה 1
        DEFAULT_ACCENT_BUTTER_YELLOW,  // 1: כפתורי פעולה 2
        DEFAULT_DRAWER_HEADER_YELLOW,  // 2: צבע לוגו 1 (רקע)
        DEFAULT_TEXT_CHARCOAL,         // 3: צבע לוגו 2 (טקסט וסמל)
        DEFAULT_BG_CREAM,              // 4: רקע ראשי
        DEFAULT_CARD_WHITE,            // 5: כרטיסים ותיבות קלט
        DEFAULT_ACCENT_SKY_BLUE,       // 6: כפתורי זיכרון ופקודות
        DEFAULT_CARD_WHITE,            // 7: מקשי מספרים
        DEFAULT_ACCENT_SAGE_GREEN,     // 8: פריט פעיל וכותרות
        DEFAULT_TEXT_CHARCOAL          // 9: טקסט ואייקונים ראשי
    )

    fun getSlotEffectiveColor(slot: Int, fallback: Int = getDefaultSlotColors()[slot]): Int {
        if (slot !in 0 until TOTAL_COLOR_SLOTS || !slotCustomized[slot]) return fallback
        val allowAlpha = (slot != SLOT_MAIN_BG)
        return applyBrightnessAndOpacity(
            slotBaseColors[slot],
            slotBrightness[slot],
            if (allowAlpha) slotOpacity[slot] else 1.0f
        )
    }

    // מאפיינים דינמיים המתעדכנים אוטומטית בכל האפליקציה כאשר המשתמש משנה צבע של אלמנט
    val BG_CREAM: Int get() = getSlotEffectiveColor(SLOT_MAIN_BG, DEFAULT_BG_CREAM)
    val DRAWER_BG: Int get() = getSlotEffectiveColor(SLOT_MAIN_BG, DEFAULT_BG_CREAM)
    val DRAWER_HEADER_YELLOW: Int get() = getSlotEffectiveColor(SLOT_LOGO_BG, DEFAULT_DRAWER_HEADER_YELLOW)
    val LOGO_TEXT_COLOR: Int get() = getSlotEffectiveColor(SLOT_LOGO_TEXT, DEFAULT_TEXT_CHARCOAL)
    val TEXT_CHARCOAL: Int get() = getSlotEffectiveColor(SLOT_TEXT_ICONS, DEFAULT_TEXT_CHARCOAL)
    val TEXT_MUTED_GREY: Int get() = if (slotCustomized[SLOT_TEXT_ICONS]) {
        val c = getSlotEffectiveColor(SLOT_TEXT_ICONS, DEFAULT_TEXT_CHARCOAL)
        Color.argb(185, Color.red(c), Color.green(c), Color.blue(c))
    } else DEFAULT_TEXT_MUTED_GREY
    val NEUTRAL_SECONDARY_BG: Int get() = if (slotCustomized[SLOT_CARDS_INPUTS]) {
        Utils.lightenColor(getSlotEffectiveColor(SLOT_CARDS_INPUTS, DEFAULT_CARD_WHITE), -0.05f)
    } else DEFAULT_NEUTRAL_SECONDARY_BG
    val CARD_WHITE: Int get() = getSlotEffectiveColor(SLOT_CARDS_INPUTS, DEFAULT_CARD_WHITE)

    val ACCENT_SAGE_GREEN: Int get() = getSlotEffectiveColor(SLOT_ACTIVE_PILL, DEFAULT_ACCENT_SAGE_GREEN)
    val ACCENT_MUTED_GREEN: Int get() = if (slotCustomized[SLOT_ACTIVE_PILL]) {
        Utils.lightenColor(getSlotEffectiveColor(SLOT_ACTIVE_PILL, DEFAULT_ACCENT_SAGE_GREEN), 0.35f)
    } else DEFAULT_ACCENT_MUTED_GREEN
    val ACCENT_SKY_BLUE: Int get() = getSlotEffectiveColor(SLOT_MEM_CMD_KEYS, DEFAULT_ACCENT_SKY_BLUE)
    val ACCENT_PASTEL_BLUE: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) {
        Utils.lightenColor(getSlotEffectiveColor(SLOT_MEM_CMD_KEYS, DEFAULT_ACCENT_SKY_BLUE), 0.28f)
    } else DEFAULT_ACCENT_PASTEL_BLUE
    val ACCENT_MUTED_PEACH: Int get() = getSlotEffectiveColor(SLOT_ACTION_1, DEFAULT_ACCENT_MUTED_PEACH)
    val ACCENT_TERRACOTTA: Int get() = getSlotEffectiveColor(SLOT_ACTION_1, DEFAULT_ACCENT_TERRACOTTA)
    val ACCENT_BUTTER_YELLOW: Int get() = getSlotEffectiveColor(SLOT_ACTION_2, DEFAULT_ACCENT_BUTTER_YELLOW)
    val ACCENT_WARM_MUSTARD: Int get() = DEFAULT_ACCENT_WARM_MUSTARD

    // תאימות מלאה לכל הקבצים האחרים בפרויקט
    val TEXT_DARK: Int get() = TEXT_CHARCOAL
    val TEXT_MUTED: Int get() = TEXT_MUTED_GREY
    val HEADER_PISTACHIO_CARD: Int get() = ACCENT_MUTED_GREEN
    val HEADER_STAR_PILL: Int get() = ACCENT_SAGE_GREEN
    val COMPASS_MINT_CARD: Int get() = ACCENT_MUTED_GREEN
    val COMPASS_DIAL_RING_BG: Int get() = Color.parseColor("#F4F1EA")
    val ALERT_PINK_PILL: Int get() = Color.parseColor("#F9DCD8")
    val ALERT_RED_TEXT: Int get() = Color.parseColor("#B9382C")
    val ACTION_CORAL_PRIMARY: Int get() = ACCENT_TERRACOTTA
    val SWAP_YELLOW_PILL: Int get() = ACCENT_BUTTER_YELLOW
    val PILL_MC_BLUE: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_SKY_BLUE else Color.parseColor("#CBE4F0")
    val PILL_MR_MINT: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_PASTEL_BLUE else DEFAULT_ACCENT_SAGE_GREEN
    val PILL_MPLUS_LIME: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_SKY_BLUE else Color.parseColor("#D4E8B0")
    val PILL_MMINUS_YELLOW: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_PASTEL_BLUE else DEFAULT_ACCENT_BUTTER_YELLOW
    val PILL_AC_PERIWINKLE: Int get() = ACCENT_SKY_BLUE
    val PILL_CMD_LAVENDER: Int get() = ACCENT_PASTEL_BLUE
    val PILL_NUM_WHITE: Int get() = getSlotEffectiveColor(SLOT_NUM_KEYS, DEFAULT_CARD_WHITE)

    var pillCornerPt: Float = 24f
    var shadowSoftness: Int = 0
    var useCustomAccentTint: Boolean
        get() = slotCustomized[SLOT_ACTION_1]
        set(value) { slotCustomized[SLOT_ACTION_1] = value }
    var useCustomBgTint: Boolean
        get() = slotCustomized[SLOT_MAIN_BG]
        set(value) { slotCustomized[SLOT_MAIN_BG] = value }
    var warmPaperTintMode: Int = 0

    fun loadMinimalistPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        pillCornerPt = prefs.getFloat("min_pill_corner", 24f)
        shadowSoftness = prefs.getInt("min_shadow_softness", 0)
        warmPaperTintMode = prefs.getInt("min_paper_tint_mode", 0)
        selectedElementSlot = prefs.getInt("min_selected_slot", SLOT_ACTION_1).coerceIn(0, TOTAL_COLOR_SLOTS - 1)
        val defaults = getDefaultSlotColors()
        for (i in 0 until TOTAL_COLOR_SLOTS) {
            slotCustomized[i] = prefs.getBoolean("min_slot_cust_$i", false)
            slotBaseColors[i] = prefs.getInt("min_slot_col_$i", defaults[i])
            slotBrightness[i] = prefs.getFloat("min_slot_brt_$i", 1.0f).coerceIn(0.20f, 1.0f)
            slotOpacity[i] = prefs.getFloat("min_slot_opc_$i", 1.0f).coerceIn(0.25f, 1.0f)
        }
    }

    fun saveMinimalistPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        val ed = prefs.edit()
            .putFloat("min_pill_corner", pillCornerPt)
            .putInt("min_shadow_softness", shadowSoftness)
            .putInt("min_paper_tint_mode", warmPaperTintMode)
            .putInt("min_selected_slot", selectedElementSlot)
        for (i in 0 until TOTAL_COLOR_SLOTS) {
            ed.putBoolean("min_slot_cust_$i", slotCustomized[i])
            ed.putInt("min_slot_col_$i", slotBaseColors[i])
            ed.putFloat("min_slot_brt_$i", slotBrightness[i])
            ed.putFloat("min_slot_opc_$i", slotOpacity[i])
        }
        ed.apply()
    }

    private fun applyBrightnessAndOpacity(baseColor: Int, brightness: Float, opacity: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        hsv[2] = (hsv[2] * brightness).coerceIn(0.12f, 1.0f)
        val alpha = (opacity * 255f).toInt().coerceIn(65, 255)
        return Color.HSVToColor(alpha, hsv)
    }

    fun getBackgroundColor(activity: MainActivity? = null): Int {
        if (slotCustomized[SLOT_MAIN_BG]) {
            return getSlotEffectiveColor(SLOT_MAIN_BG, DEFAULT_BG_CREAM)
        }
        if (activity != null && !activity.isMinimalistMode) {
            return activity.bgColor
        }
        return when (warmPaperTintMode) {
            1 -> Color.parseColor("#FFFFFF")
            2 -> Color.parseColor("#F2F7F4")
            else -> DEFAULT_BG_CREAM
        }
    }

    fun getPrimaryActionColor(activity: MainActivity): Int {
        if (slotCustomized[SLOT_ACTION_1]) {
            return getSlotEffectiveColor(SLOT_ACTION_1, DEFAULT_ACCENT_TERRACOTTA)
        }
        return if (activity.isMinimalistMode) DEFAULT_ACCENT_TERRACOTTA else activity.primaryColor
    }

    fun getSecondaryActionColor(activity: MainActivity): Int {
        if (slotCustomized[SLOT_ACTION_2]) {
            return getSlotEffectiveColor(SLOT_ACTION_2, DEFAULT_ACCENT_BUTTER_YELLOW)
        }
        return if (activity.isMinimalistMode) Color.parseColor("#F48484") else activity.primaryColor
    }

    // =========================================================================
    // 1. דיאלוג "בחר מצב תצוגה" מאוחד עם כל 4 האפשרויות
    // =========================================================================
    fun showUnifiedDisplayModeDialog(activity: MainActivity) {
        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = activity.isMinimalistMode
        val cardBgCol = if (isMin) BG_CREAM else Color.parseColor("#1E222B")
        val titleCol = if (isMin) TEXT_CHARCOAL else Color.WHITE
        val dividerCol = if (isMin) Color.argb(28, 44, 44, 44) else Color.argb(35, 255, 255, 255)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20f), activity.dp(22f), activity.dp(20f), activity.dp(18f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(cardBgCol)
                setStroke(activity.dp(1.2f), if (isMin) Color.parseColor("#E0DBD3") else Color.argb(60, 255, 255, 255))
            }
        }

        val titleTv = TextView(activity).apply {
            text = "בחר מצב תצוגה"
            setTextColor(titleCol)
            textSize = 20f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(6f), 0, activity.dp(6f), activity.dp(16f))
        }
        root.addView(titleTv)

        val modes = listOf(
            Triple(0, "🎨 עיצוב רגיל (קלאסי)", Color.parseColor("#FF9F0A")),
            Triple(1, "💎 עיצוב הזכוכית (Liquid Glass Pro)", Color.parseColor("#64B5F6")),
            Triple(2, "🌌 הזוהר הצפוני (Aurora Borealis)", AuroraBorealisTheme.AURORA_PRIMARY),
            Triple(3, "🌿 עיצוב מינימליסטי (Pastel Light)", ACCENT_SAGE_GREEN)
        )

        val currentModeIdx = when {
            activity.isMinimalistMode -> 3
            activity.isAuroraMode -> 2
            activity.isIosGlassMode -> 1
            else -> 0
        }

        for ((idx, label, highlightCol) in modes) {
            val isSelected = (idx == currentModeIdx)
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(activity.dp(14f), activity.dp(14f), activity.dp(14f), activity.dp(14f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(16f).toFloat()
                    if (isSelected) {
                        val fillCol = if (isMin) ACCENT_SAGE_GREEN else Color.argb(65, Color.red(highlightCol), Color.green(highlightCol), Color.blue(highlightCol))
                        setColor(fillCol)
                    } else {
                        setColor(Color.TRANSPARENT)
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    activity.feedback(it)
                    activity.isMinimalistMode = (idx == 3)
                    activity.isAuroraMode = (idx == 2)
                    activity.isIosGlassMode = (idx == 1)
                    if (activity.isMinimalistMode) {
                        activity.buttonCornerStyle = pillCornerPt
                    }
                    activity.savePreferences()
                    dlg.dismiss()
                    activity.applyTheme()
                }
            }

            val radioView = createCustomRadioCircleView(activity, isSelected, isMin, highlightCol)
            val labelTv = TextView(activity).apply {
                text = label
                setTextColor(
                    if (isSelected && !isMin) highlightCol
                    else if (isMin) TEXT_CHARCOAL
                    else Color.WHITE
                )
                textSize = 16.5f
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif", Typeface.NORMAL)
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            row.addView(radioView)
            row.addView(labelTv)
            root.addView(row)

            if (idx < modes.lastIndex) {
                val div = View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(1f)).apply {
                        setMargins(activity.dp(8f), activity.dp(2f), activity.dp(8f), activity.dp(2f))
                    }
                    setBackgroundColor(dividerCol)
                }
                root.addView(div)
            }
        }

        dlg.setContentView(root)
        dlg.show()
        val widthPx = (activity.resources.displayMetrics.widthPixels * 0.90f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    // =========================================================================
    // 2. דיאלוג "בחר סוג צליל" מתוקן וקריא ב-100% בכל ערכות הנושא
    // =========================================================================
    fun showUnifiedSoundPickerDialog(activity: MainActivity) {
        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = activity.isMinimalistMode
        val cardBgCol = if (isMin) BG_CREAM else Color.parseColor("#1E222B")
        val titleCol = if (isMin) TEXT_CHARCOAL else Color.WHITE
        val dividerCol = if (isMin) Color.argb(28, 44, 44, 44) else Color.argb(35, 255, 255, 255)
        val activeAccent = when {
            isMin -> TEXT_CHARCOAL
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20f), activity.dp(22f), activity.dp(20f), activity.dp(18f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(cardBgCol)
                setStroke(activity.dp(1.2f), if (isMin) Color.parseColor("#E0DBD3") else Color.argb(60, 255, 255, 255))
            }
        }

        val titleTv = TextView(activity).apply {
            text = "בחר סוג צליל הקשה"
            setTextColor(titleCol)
            textSize = 20f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(6f), 0, activity.dp(6f), activity.dp(16f))
        }
        root.addView(titleTv)

        val soundOptions = listOf(
            0 to "🔊 מערכת (צליל אנדרואיד תקני)",
            1 to "🎵 צליל 1 (קליק מכני עדין)",
            2 to "🎵 צליל 2 (הקשת מעבדה)",
            3 to "🎵 צליל 3 (טון דיגיטלי)"
        )

        for ((sCode, sLabel) in soundOptions) {
            val isSelected = (activity.soundType == sCode)
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(activity.dp(14f), activity.dp(14f), activity.dp(14f), activity.dp(14f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(999f).toFloat()
                    if (isSelected) {
                        val fillCol = if (isMin) ACCENT_SAGE_GREEN else Color.argb(65, Color.red(activeAccent), Color.green(activeAccent), Color.blue(activeAccent))
                        setColor(fillCol)
                    } else {
                        setColor(if (isMin) CARD_WHITE else Color.TRANSPARENT)
                        if (isMin) setStroke(activity.dp(1f), Color.parseColor("#EAE5DE"))
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = activity.dp(6f) }
                setOnClickListener {
                    activity.soundType = sCode
                    activity.useSound = true
                    activity.savePreferences()
                    activity.feedback(it)
                    dlg.dismiss()
                    activity.switchTab(19)
                }
            }

            val radioView = createCustomRadioCircleView(activity, isSelected, isMin, activeAccent)
            val labelTv = TextView(activity).apply {
                text = sLabel
                setTextColor(
                    if (isMin) TEXT_CHARCOAL
                    else if (isSelected) activeAccent
                    else Color.WHITE
                )
                textSize = 16.5f
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif", Typeface.NORMAL)
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            row.addView(radioView)
            row.addView(labelTv)
            root.addView(row)

            if (!isMin && sCode < soundOptions.lastIndex) {
                val div = View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(1f)).apply {
                        setMargins(activity.dp(8f), 0, activity.dp(8f), activity.dp(4f))
                    }
                    setBackgroundColor(dividerCol)
                }
                root.addView(div)
            }
        }

        dlg.setContentView(root)
        dlg.show()
        val widthPx = (activity.resources.displayMetrics.widthPixels * 0.90f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    fun showUnifiedPrecisionPickerDialog(activity: MainActivity) {
        val options = listOf(2, 4, 6, 8, 10, 12)
        val labels = options.map { "$it ספרות אחרי הנקודה" }
        activity.showItemPicker("בחר דיוק עשרוני", labels) { chosen ->
            val num = chosen.substringBefore(" ").toIntOrNull() ?: 8
            activity.calcPrecision = num
            activity.savePreferences()
            activity.switchTab(19)
        }
    }

    private fun createCustomRadioCircleView(
        activity: MainActivity,
        isSelected: Boolean,
        isMin: Boolean,
        highlightCol: Int
    ): View {
        return object : View(activity) {
            private val d = resources.displayMetrics.density
            private val outerP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.2f * d
                color = if (isMin) TEXT_CHARCOAL else if (isSelected) highlightCol else Color.WHITE
            }
            private val innerP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (isMin) TEXT_CHARCOAL else highlightCol
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val r = min(width, height) * 0.40f
                canvas.drawCircle(cx, cy, r, outerP)
                if (isSelected) {
                    canvas.drawCircle(cx, cy, r * 0.52f, innerP)
                }
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(activity.dp(26f), activity.dp(26f)).apply {
                rightMargin = activity.dp(12f)
            }
        }
    }

    // =========================================================================
    // כפתורי מקלדת שטוחים התומכים בכל 10 ערוצי הצבע המותאמים אישית
    // =========================================================================
    fun createMinimalistKeypadButtonDrawable(
        activity: MainActivity,
        symbol: String,
        isPressed: Boolean = false
    ): Drawable {
        val radius = activity.dp(pillCornerPt.coerceIn(10f, 40f)).toFloat()
        val action1Col = getPrimaryActionColor(activity)
        val action2Col = getSecondaryActionColor(activity)
        val baseFill = when (symbol) {
            "MC" -> PILL_MC_BLUE
            "MR" -> PILL_MR_MINT
            "M+" -> PILL_MPLUS_LIME
            "M-" -> PILL_MMINUS_YELLOW
            "AC" -> ACCENT_SKY_BLUE
            "⌫", "%" -> ACCENT_PASTEL_BLUE
            "÷" -> if (slotCustomized[SLOT_ACTION_1]) action1Col else Color.parseColor("#F9B487")
            "−", "-" -> if (slotCustomized[SLOT_ACTION_1]) action1Col else DEFAULT_ACCENT_TERRACOTTA
            "+" -> if (slotCustomized[SLOT_ACTION_1]) action1Col else DEFAULT_ACCENT_TERRACOTTA
            "×" -> if (slotCustomized[SLOT_ACTION_2]) action2Col else if (slotCustomized[SLOT_ACTION_1]) action1Col else DEFAULT_ACCENT_MUTED_PEACH
            "=" -> if (slotCustomized[SLOT_ACTION_2]) action2Col else Color.parseColor("#F48484")
            else -> PILL_NUM_WHITE
        }

        val fillColor = if (isPressed) Utils.lightenColor(baseFill, -0.06f) else baseFill
        val pillBody = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillColor)
            setStroke(activity.dp(1f), Color.argb(18, 44, 44, 44))
        }
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#18000000")), pillBody, null)
    }

    fun createMinimalistCardDrawable(
        activity: MainActivity,
        cornerDp: Float = 20f,
        cardStyle: Int = 0
    ): Drawable {
        val radius = activity.dp(cornerDp).toFloat()
        val fillCol = when (cardStyle) {
            1 -> ACCENT_MUTED_GREEN
            2 -> if (slotCustomized[SLOT_CARDS_INPUTS]) CARD_WHITE else Color.parseColor("#F7F4EE")
            3 -> ACCENT_PASTEL_BLUE
            4 -> ACCENT_BUTTER_YELLOW
            else -> CARD_WHITE
        }
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillCol)
            val strokeAlpha = if (cardStyle == 2) 35 else 18
            setStroke(activity.dp(1f), Color.argb(strokeAlpha, 44, 44, 44))
        }
    }

    fun createMinimalistInputDrawable(activity: MainActivity, variant: Int = 0): Drawable {
        val radius = activity.dp(20f).toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            if (slotCustomized[SLOT_CARDS_INPUTS]) {
                setColor(CARD_WHITE)
                setStroke(activity.dp(1.2f), ACCENT_SKY_BLUE)
            } else {
                when (variant % 3) {
                    0 -> {
                        setColor(CARD_WHITE)
                        setStroke(activity.dp(1.2f), Color.parseColor("#E3DFD9"))
                    }
                    1 -> {
                        setColor(NEUTRAL_SECONDARY_BG)
                        setStroke(activity.dp(1f), Color.parseColor("#E0DBD3"))
                    }
                    else -> {
                        setColor(ACCENT_PASTEL_BLUE)
                        setStroke(activity.dp(1.2f), ACCENT_SKY_BLUE)
                    }
                }
            }
        }
    }

    fun createMinimalistActionButtonDrawable(
        activity: MainActivity,
        isPrimary: Boolean,
        pillTone: Int = 0
    ): Drawable {
        val radius = activity.dp(if (pillTone == 5) 20f else 999f).toFloat()
        val bgCol = when (pillTone) {
            1 -> getPrimaryActionColor(activity)
            2 -> ACCENT_BUTTER_YELLOW
            3 -> ACCENT_SKY_BLUE
            4 -> NEUTRAL_SECONDARY_BG
            5 -> CARD_WHITE
            else -> if (isPrimary) getPrimaryActionColor(activity) else NEUTRAL_SECONDARY_BG
        }
        val pill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgCol)
            if (pillTone == 5) {
                setStroke(activity.dp(1f), Color.parseColor("#EAE5DE"))
            }
        }
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#18000000")), pill, null)
    }

    fun createMinimalistActiveDrawerCapsule(activity: MainActivity): Drawable {
        val radius = activity.dp(999f).toFloat()
        return GradientDrawable().apply {
            cornerRadius = radius
            setColor(ACCENT_SAGE_GREEN)
        }
    }

    fun applyMinimalistStylingToHierarchy(root: View?, activity: MainActivity) {
        if (root == null || !activity.isMinimalistMode) return
        val isSettingsTab = (activity.currentTabIndex == 19)
        var inputCounter = 0

        fun traverse(v: View) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                v.elevation = 0f
                v.translationZ = 0f
            }
            if (v.tag == "MINIMALIST_CUSTOMIZER_IGNORE") return

            when (v) {
                is EditText -> {
                    val variant = inputCounter % 3
                    inputCounter++
                    v.background = createMinimalistInputDrawable(activity, variant)
                    v.setTextColor(TEXT_CHARCOAL)
                    v.setHintTextColor(TEXT_MUTED_GREY)
                    v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                }
                is CheckBox -> {
                    v.setTextColor(TEXT_CHARCOAL)
                    v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    v.buttonTintList = ColorStateList.valueOf(ACCENT_SKY_BLUE)
                }
                is TextView -> {
                    val txt = v.text?.toString() ?: ""
                    v.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)

                    if (v.tag == "PRO_STAR_CHIP") {
                        v.setTextColor(TEXT_CHARCOAL)
                        v.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        return
                    }

                    if (isSettingsTab && v.parent is LinearLayout && (v.parent?.parent is HorizontalScrollView)) {
                        return
                    }

                    if (v.isClickable && v.background != null) {
                        v.setTextColor(TEXT_CHARCOAL)
                        v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)

                        if (isSettingsTab) {
                            styleSettingsRowView(v, txt, activity)
                        } else {
                            val tone = when {
                                txt.contains("החלף כיוון") || txt.contains("הקפא") || txt.contains("Hold") || txt.contains("הקפה") -> 2
                                txt.contains("נעץ תוצאה") || txt.contains("רשימת סימון") || txt.contains("PDF") || txt.contains("CSV") || txt.contains("Drive") -> 3
                                txt.contains("כייל") || txt.contains("Tare") || txt.startsWith("חשב") || txt.startsWith("פתור") || txt.startsWith("נתח") || txt.startsWith("פענח") || txt.startsWith("צור") || txt.startsWith("התחל") || txt.startsWith("▶️") || txt.startsWith("🆘") || txt.startsWith("🔆") -> 1
                                else -> 4
                            }
                            v.background = createMinimalistActionButtonDrawable(activity, tone == 1, tone)
                            if (txt.contains("החלף כיוון")) {
                                v.text = "⇅ החלף כיוון"
                                val lp = v.layoutParams
                                if (lp is LinearLayout.LayoutParams) {
                                    lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
                                    lp.gravity = Gravity.CENTER_HORIZONTAL
                                    v.layoutParams = lp
                                }
                                v.setPadding(activity.dp(24f), activity.dp(10f), activity.dp(24f), activity.dp(10f))
                            }
                        }
                    } else {
                        v.setTextColor(TEXT_CHARCOAL)
                        v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    }
                }
                is ViewGroup -> {
                    for (i in 0 until v.childCount) {
                        traverse(v.getChildAt(i))
                    }
                }
            }
        }
        traverse(root)
    }

    private fun styleSettingsRowView(tv: TextView, rawText: String, activity: MainActivity) {
        val clean = IosGlassTheme.cleanCategoryTitle(rawText, true)
        tv.text = clean
        tv.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        tv.setPadding(activity.dp(18f), activity.dp(15f), activity.dp(18f), activity.dp(15f))
        tv.compoundDrawablePadding = activity.dp(12f)

        val isChecklistBtn = clean.contains("רשימת סימון")
        tv.background = createMinimalistActionButtonDrawable(
            activity,
            isPrimary = false,
            pillTone = if (isChecklistBtn) 3 else 5
        )

        val rightDrawable: Drawable? = when {
            isChecklistBtn -> createSettingsBadgeDrawable(activity, 7)
            clean.contains("רטט") || clean.contains("אנימציות") || clean.contains("מצב מקצועי") || clean.contains("מצב תצוגה") ->
                createSettingsBadgeDrawable(activity, if (clean.contains("כבוי")) 2 else 1)
            clean.contains("צלילי הקשה") -> createSettingsBadgeDrawable(activity, 3)
            clean.contains("סוג צליל") -> createSettingsBadgeDrawable(activity, 4)
            clean.contains("צליל AC") -> createSettingsBadgeDrawable(activity, 8)
            clean.contains("מסך תמיד דולק") -> createSettingsBadgeDrawable(activity, 5)
            clean.contains("צורת כפתורים") -> createSettingsBadgeDrawable(activity, 6)
            clean.contains("דיוק עשרוני") || clean.contains("הגדרות") -> createMinimalistVectorIcon(activity, 19, TEXT_CHARCOAL, 20f)
            clean.contains("יד דומיננטית") -> createSettingsBadgeDrawable(activity, 9)
            else -> null
        }
        tv.setCompoundDrawables(null, null, rightDrawable, null)
    }

    private fun createSettingsBadgeDrawable(context: Context, badgeType: Int): Drawable {
        val sizePx = (22f * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val d = context.resources.displayMetrics.density
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = TEXT_CHARCOAL
                style = Paint.Style.STROKE
                strokeWidth = 1.7f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = Math.min(bounds.width(), bounds.height()) * 0.38f

                when (badgeType) {
                    1 -> {
                        fillP.color = ACCENT_SAGE_GREEN
                        canvas.drawCircle(cx, cy, r * 0.85f, fillP)
                    }
                    2 -> {
                        fillP.color = NEUTRAL_SECONDARY_BG
                        canvas.drawCircle(cx, cy, r * 0.85f, fillP)
                        canvas.drawCircle(cx, cy, r * 0.85f, strokeP)
                    }
                    3 -> {
                        val p = Path().apply {
                            moveTo(cx - r * 0.65f, cy - r * 0.3f)
                            lineTo(cx - r * 0.25f, cy - r * 0.3f)
                            lineTo(cx + r * 0.25f, cy - r * 0.7f)
                            lineTo(cx + r * 0.25f, cy + r * 0.7f)
                            lineTo(cx - r * 0.25f, cy + r * 0.3f)
                            lineTo(cx - r * 0.65f, cy + r * 0.3f)
                            close()
                        }
                        canvas.drawPath(p, strokeP)
                        canvas.drawArc(RectF(cx + r * 0.1f, cy - r * 0.45f, cx + r * 0.7f, cy + r * 0.45f), -50f, 100f, false, strokeP)
                    }
                    4 -> {
                        canvas.drawArc(RectF(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f), 20f, 115f, false, strokeP)
                        canvas.drawCircle(cx - r * 0.45f, cy - r * 0.35f, r * 0.22f, strokeP)
                        canvas.drawCircle(cx + r * 0.45f, cy + r * 0.35f, r * 0.22f, strokeP)
                    }
                    5 -> {
                        val moon = Path().apply {
                            addArc(RectF(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f), 40f, 250f)
                        }
                        canvas.drawPath(moon, strokeP)
                    }
                    6 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), r * 0.22f, r * 0.22f, strokeP)
                    }
                    7 -> {
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.05f, cx - r * 0.1f, cy + r * 0.5f, strokeP)
                        canvas.drawLine(cx - r * 0.1f, cy + r * 0.5f, cx + r * 0.65f, cy - r * 0.45f, strokeP)
                    }
                    8 -> {
                        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = TEXT_CHARCOAL
                            textSize = 11.5f * d
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        }
                        canvas.drawText("AC", cx, cy + 4f * d, tp)
                    }
                    9 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.5f, cy - r * 0.1f, cx + r * 0.5f, cy + r * 0.75f), r * 0.25f, r * 0.25f, strokeP)
                        for (ox in listOf(-0.35f, -0.12f, 0.12f, 0.35f)) {
                            canvas.drawLine(cx + r * ox, cy - r * 0.1f, cx + r * ox, cy - r * 0.7f, strokeP)
                        }
                    }
                }
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    // =========================================================================
    // SCREEN 5: מנוע התאמת צבעים רב-אלמנטי (10 אלמנטים) עם גלגל צבעים 2D יחיד ופעיל!
    // מתאים את עצמו אוטומטית לכל ערכת נושא (מינימליסטית, קלאסית, זכוכית וזוהר הצפון)
    // =========================================================================
    fun createColorThemeCustomizerSection(activity: MainActivity): View {
        val isMin = activity.isMinimalistMode
        val root = LinearLayout(activity).apply {
            tag = "MINIMALIST_CUSTOMIZER_IGNORE"
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = activity.dp(10f); bottomMargin = activity.dp(14f) }
        }

        val sectionHeaderTv = TextView(activity).apply {
            text = "🎨 התאמת צבעים מתקדמת לכל אלמנט (בחר אלמנט לעריכה):"
            setTextColor(if (isMin) TEXT_CHARCOAL else Color.WHITE)
            textSize = 14.5f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(4f), 0, activity.dp(4f), activity.dp(8f))
        }
        root.addView(sectionHeaderTv)

        val elementSpecs = listOf(
            SLOT_ACTION_1 to "🎨 כפתורי פעולה 1 (ראשי / + − ÷)",
            SLOT_ACTION_2 to "🟠 כפתורי פעולה 2 (= / × / משני)",
            SLOT_LOGO_BG to "🏷️ צבע לוגו 1 (רקע באנר תפריט)",
            SLOT_LOGO_TEXT to "✒️ צבע לוגו 2 (טקסט וסמל לוגו)",
            SLOT_MAIN_BG to "🖼️ צבע רקע ראשי (מסך ותפריט)",
            SLOT_CARDS_INPUTS to "🗂️ כרטיסים ותיבות קלט",
            SLOT_MEM_CMD_KEYS to "🔵 מקשי זיכרון ופקודות (AC / MR)",
            SLOT_NUM_KEYS to "🔢 מקשי מספרים (0-9)",
            SLOT_ACTIVE_PILL to "🟢 סימון פעיל וכותרות Pro",
            SLOT_TEXT_ICONS to "🖋️ צבע טקסט ואייקונים"
        )

        val chipsScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(12f) }
        }
        val chipsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.dp(2f), activity.dp(2f), activity.dp(2f), activity.dp(2f))
        }
        chipsScroll.addView(chipsRow)
        root.addView(chipsScroll)

        val chipViews = mutableMapOf<Int, TextView>()
        lateinit var hexLabelTv: TextView
        lateinit var activeSlotTitleTv: TextView
        lateinit var previewCircleView: View
        lateinit var colorWheelView: Flat2DColorWheelView
        lateinit var brightnessSlider: MinimalistLineSliderView
        lateinit var opacitySlider: MinimalistLineSliderView

        fun applyAllCustomColorsLiveToActivity() {
            val effAction1 = getSlotEffectiveColor(SLOT_ACTION_1, if (isMin) DEFAULT_ACCENT_TERRACOTTA else activity.primaryColor)
            val effMainBg = getSlotEffectiveColor(SLOT_MAIN_BG, if (isMin) DEFAULT_BG_CREAM else activity.bgColor)

            if (slotCustomized[SLOT_ACTION_1]) {
                activity.primaryColor = effAction1
            }
            if (slotCustomized[SLOT_MAIN_BG]) {
                activity.bgColor = effMainBg
            }

            // עדכון חי של רקע המסך, שורת המצב, תפריט הצד ובאנר הלוגו בזמן גרירת גלגל הצבעים!
            val liveBg = if (isMin) getBackgroundColor(activity) else activity.bgColor
            activity.mainContent?.setBackgroundColor(liveBg)
            activity.window.statusBarColor = liveBg
            activity.window.navigationBarColor = liveBg
            activity.updateLiveDrawerAndHeaderColors()

            saveMinimalistPrefs(activity.prefs)
            activity.savePreferences()
        }

        fun updatePreviewHeaderAndChips() {
            val slot = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            val effCol = getSlotEffectiveColor(slot)
            val specTitle = elementSpecs.find { it.first == slot }?.second ?: ""

            activeSlotTitleTv.text = "עורך כעת: $specTitle"
            hexLabelTv.text = String.format(Locale.US, "#%08X", effCol)
            previewCircleView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(effCol)
                setStroke(activity.dp(2f), Color.WHITE)
            }

            for ((sIdx, chipTv) in chipViews) {
                val isSel = (sIdx == slot)
                val chipDotCol = getSlotEffectiveColor(sIdx)
                chipTv.background = GradientDrawable().apply {
                    cornerRadius = activity.dp(999f).toFloat()
                    if (isSel) {
                        setColor(if (isMin) DEFAULT_ACCENT_TERRACOTTA else activity.primaryColor)
                        setStroke(activity.dp(1.5f), if (isMin) DEFAULT_TEXT_CHARCOAL else Color.WHITE)
                    } else {
                        setColor(if (isMin) DEFAULT_NEUTRAL_SECONDARY_BG else Color.argb(45, 255, 255, 255))
                        setStroke(activity.dp(1f), chipDotCol)
                    }
                }
                chipTv.setTextColor(
                    if (isMin) DEFAULT_TEXT_CHARCOAL
                    else if (isSel) Color.parseColor("#08090C")
                    else Color.WHITE
                )
                chipTv.typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif", Typeface.NORMAL)
            }
        }

        fun selectSlot(slot: Int) {
            selectedElementSlot = slot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            colorWheelView.setSelectedColor(slotBaseColors[selectedElementSlot])
            brightnessSlider.progress = slotBrightness[selectedElementSlot]
            opacitySlider.progress = slotOpacity[selectedElementSlot]
            updatePreviewHeaderAndChips()
            saveMinimalistPrefs(activity.prefs)
        }

        for ((slotIdx, label) in elementSpecs) {
            val chip = TextView(activity).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(activity.dp(14f), activity.dp(9f), activity.dp(14f), activity.dp(9f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(activity.dp(4f), 0, activity.dp(4f), 0) }
                setOnClickListener {
                    activity.feedback(it)
                    selectSlot(slotIdx)
                }
            }
            chipViews[slotIdx] = chip
            chipsRow.addView(chip)
        }

        // הכרטיס הצהוב החם הראשי (#FFE094 במצב מינימליסטי) שמכיל את גלגל הצבעים 2D והסליידרים
        val yellowCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20f), activity.dp(18f), activity.dp(20f), activity.dp(20f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                if (isMin) {
                    setColor(DEFAULT_ACCENT_BUTTER_YELLOW)
                } else {
                    setColor(Utils.lightenColor(activity.bgColor, 0.10f))
                    setStroke(activity.dp(1.2f), activity.primaryColor)
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val cardInk = if (isMin) DEFAULT_TEXT_CHARCOAL else Color.WHITE

        activeSlotTitleTv = TextView(activity).apply {
            text = ""
            setTextColor(cardInk)
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, activity.dp(8f))
        }
        yellowCard.addView(activeSlotTitleTv)

        val hexRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }

        previewCircleView = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(activity.dp(38f), activity.dp(38f)).apply {
                rightMargin = activity.dp(10f)
            }
        }

        hexLabelTv = TextView(activity).apply {
            text = "#FFFFA48E"
            setTextColor(cardInk)
            textSize = 14.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        hexRow.addView(previewCircleView)
        hexRow.addView(hexLabelTv)
        yellowCard.addView(hexRow)

        colorWheelView = Flat2DColorWheelView(activity) { pickedColor ->
            val s = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            slotCustomized[s] = true
            slotBaseColors[s] = pickedColor
            updatePreviewHeaderAndChips()
            applyAllCustomColorsLiveToActivity()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(220f)
            ).apply { bottomMargin = activity.dp(14f) }
        }
        yellowCard.addView(colorWheelView)

        val brightLabel = TextView(activity).apply {
            text = "☀️ בהירות"
            setTextColor(cardInk)
            textSize = 13.5f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(2f), 0, activity.dp(2f))
        }
        brightnessSlider = MinimalistLineSliderView(activity, slotBrightness[selectedElementSlot]) { newVal ->
            val s = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            slotCustomized[s] = true
            slotBrightness[s] = newVal.coerceIn(0.20f, 1.0f)
            updatePreviewHeaderAndChips()
            applyAllCustomColorsLiveToActivity()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(34f)
            ).apply { bottomMargin = activity.dp(8f) }
        }
        yellowCard.addView(brightLabel)
        yellowCard.addView(brightnessSlider)

        val opacityLabel = TextView(activity).apply {
            text = "💧 שקיפות"
            setTextColor(cardInk)
            textSize = 13.5f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(2f), 0, activity.dp(2f))
        }
        opacitySlider = MinimalistLineSliderView(activity, slotOpacity[selectedElementSlot]) { newVal ->
            val s = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            slotCustomized[s] = true
            slotOpacity[s] = newVal.coerceIn(0.25f, 1.0f)
            updatePreviewHeaderAndChips()
            applyAllCustomColorsLiveToActivity()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(34f)
            ).apply { bottomMargin = activity.dp(12f) }
        }
        yellowCard.addView(opacityLabel)
        yellowCard.addView(opacitySlider)

        // שורת כפתורי איפוס: איפוס האלמנט הנוכחי או איפוס כל 10 האלמנטים לפלטת הפסטל המקורית
        val resetRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val resetCurrentSlotBtn = TextView(activity).apply {
            text = "↺ אפס אלמנט זה"
            setTextColor(DEFAULT_TEXT_CHARCOAL)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(activity.dp(10f), activity.dp(10f), activity.dp(10f), activity.dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(DEFAULT_NEUTRAL_SECONDARY_BG)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = activity.dp(5f)
            }
            setOnClickListener {
                activity.feedback(it)
                val s = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
                slotCustomized[s] = false
                slotBaseColors[s] = getDefaultSlotColors()[s]
                slotBrightness[s] = 1.0f
                slotOpacity[s] = 1.0f
                selectSlot(s)
                activity.applyTheme()
            }
        }

        val resetAllPastelBtn = TextView(activity).apply {
            text = "↺ אפס את כל הצבעים למקור"
            setTextColor(DEFAULT_TEXT_CHARCOAL)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(activity.dp(10f), activity.dp(10f), activity.dp(10f), activity.dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(DEFAULT_CARD_WHITE)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f).apply {
                leftMargin = activity.dp(5f)
            }
            setOnClickListener {
                activity.feedback(it)
                resetSlotsArrayToDefaults()
                if (activity.isMinimalistMode) {
                    activity.primaryColor = DEFAULT_ACCENT_TERRACOTTA
                    activity.bgColor = DEFAULT_BG_CREAM
                }
                saveMinimalistPrefs(activity.prefs)
                activity.savePreferences()
                activity.applyTheme()
            }
        }

        resetRow.addView(resetCurrentSlotBtn)
        resetRow.addView(resetAllPastelBtn)
        yellowCard.addView(resetRow)

        root.addView(yellowCard)
        selectSlot(selectedElementSlot)
        return root
    }

    // =========================================================================
    // חבילת 85+ סמלים וקטוריים ייחודיים לערכת הנושא המינימליסטית!
    // לכל מחשבון, ממיר, כלי Pro וקטגוריה יש סמל קווי ייחודי משלו (אפס כפילויות!)
    // =========================================================================
    fun createMinimalistVectorIcon(
        context: Context,
        iconCode: Int,
        tintColor: Int = TEXT_CHARCOAL,
        sizeDp: Float = 22f
    ): Drawable {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val d = context.resources.displayMetrics.density
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val b = bounds
                val cx = b.exactCenterX()
                val cy = b.exactCenterY()
                val r = Math.min(b.width(), b.height()) * 0.40f

                when (iconCode) {
                    0 -> { // 0. מחשבון רגיל: גוף מחשבון עם מסך ו-6 נקודות מקשים
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.84f, cx + r * 0.68f, cy + r * 0.84f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawRect(cx - r * 0.42f, cy - r * 0.56f, cx + r * 0.42f, cy - r * 0.22f, paint)
                        for (gx in listOf(-0.32f, 0f, 0.32f)) {
                            for (gy in listOf(0.14f, 0.48f)) {
                                canvas.drawCircle(cx + r * gx, cy + r * gy, r * 0.08f, dotPaint)
                            }
                        }
                    }
                    1 -> { // 1. מחשבון מדעי: עקומת סינוס f(x) על צירים
                        canvas.drawLine(cx - r * 0.82f, cy, cx + r * 0.82f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.78f, cx, cy + r * 0.78f, paint)
                        val wave = Path().apply {
                            moveTo(cx - r * 0.78f, cy)
                            cubicTo(cx - r * 0.35f, cy - r * 0.95f, cx + r * 0.35f, cy + r * 0.95f, cx + r * 0.78f, cy)
                        }
                        canvas.drawPath(wave, paint)
                    }
                    2 -> { // 2. מחשבון מע״מ: חשבונית משוננת עם סמל %
                        val rec = Path().apply {
                            moveTo(cx - r * 0.62f, cy - r * 0.82f)
                            lineTo(cx + r * 0.62f, cy - r * 0.82f)
                            lineTo(cx + r * 0.62f, cy + r * 0.80f)
                            lineTo(cx + r * 0.30f, cy + r * 0.58f)
                            lineTo(cx, cy + r * 0.80f)
                            lineTo(cx - r * 0.30f, cy + r * 0.58f)
                            lineTo(cx - r * 0.62f, cy + r * 0.80f)
                            close()
                        }
                        canvas.drawPath(rec, paint)
                        canvas.drawLine(cx - r * 0.28f, cy + r * 0.18f, cx + r * 0.28f, cy - r * 0.32f, paint)
                        canvas.drawCircle(cx - r * 0.20f, cy - r * 0.28f, r * 0.09f, dotPaint)
                        canvas.drawCircle(cx + r * 0.20f, cy + r * 0.14f, r * 0.09f, dotPaint)
                    }
                    3 -> { // 3. מחשבון הלוואה: מבנה בנק קלאסי עם גג משולש ו-3 עמודים
                        val roof = Path().apply {
                            moveTo(cx - r * 0.85f, cy - r * 0.24f)
                            lineTo(cx, cy - r * 0.82f)
                            lineTo(cx + r * 0.85f, cy - r * 0.24f)
                            close()
                        }
                        canvas.drawPath(roof, paint)
                        for (colX in listOf(-0.52f, 0f, 0.52f)) {
                            canvas.drawLine(cx + r * colX, cy - r * 0.22f, cx + r * colX, cy + r * 0.68f, paint)
                        }
                        canvas.drawLine(cx - r * 0.85f, cy + r * 0.72f, cx + r * 0.85f, cy + r * 0.72f, paint)
                    }
                    4 -> { // 4. המרת מרחקים: סרגל אופקי עם שנתות וחץ דו-כיווני מעליו
                        canvas.drawRoundRect(RectF(cx - r * 0.88f, cy + r * 0.05f, cx + r * 0.88f, cy + r * 0.68f), r * 0.14f, r * 0.14f, paint)
                        for (tx in listOf(-0.45f, 0f, 0.45f)) {
                            canvas.drawLine(cx + r * tx, cy + r * 0.05f, cx + r * tx, cy + r * 0.38f, paint)
                        }
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.42f, cx + r * 0.72f, cy - r * 0.42f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.42f, cx - r * 0.45f, cy - r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.42f, cx - r * 0.45f, cy - r * 0.19f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.42f, cx + r * 0.45f, cy - r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.42f, cx + r * 0.45f, cy - r * 0.19f, paint)
                    }
                    5 -> { // 5. המרת משקלים: מאזני כפות קלאסיים
                        canvas.drawLine(cx, cy - r * 0.78f, cx, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.45f, cy + r * 0.78f, cx + r * 0.45f, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy - r * 0.35f, cx + r * 0.75f, cy - r * 0.35f, paint)
                        canvas.drawArc(RectF(cx - r * 0.90f, cy - r * 0.05f, cx - r * 0.35f, cy + r * 0.42f), 0f, 180f, true, paint)
                        canvas.drawArc(RectF(cx + r * 0.35f, cy - r * 0.05f, cx + r * 0.90f, cy + r * 0.42f), 0f, 180f, true, paint)
                    }
                    6 -> { // 6. המרת טמפ׳: מדחום אנכי עם כדורית תחתונה וסימון מעלות
                        canvas.drawArc(RectF(cx - r * 0.44f, cy + r * 0.05f, cx + r * 0.44f, cy + r * 0.86f), -45f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.75f, cx - r * 0.22f, cy + r * 0.18f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy - r * 0.75f, cx + r * 0.22f, cy + r * 0.18f, paint)
                        canvas.drawArc(RectF(cx - r * 0.22f, cy - r * 0.92f, cx + r * 0.22f, cy - r * 0.55f), 180f, 180f, false, paint)
                        canvas.drawCircle(cx + r * 0.58f, cy - r * 0.45f, r * 0.13f, paint)
                    }
                    7 -> { // 7. מחשבון מתכנתים: סוגרי קוד < / >
                        canvas.drawLine(cx - r * 0.32f, cy - r * 0.48f, cx - r * 0.82f, cy, paint)
                        canvas.drawLine(cx - r * 0.82f, cy, cx - r * 0.32f, cy + r * 0.48f, paint)
                        canvas.drawLine(cx + r * 0.32f, cy - r * 0.48f, cx + r * 0.82f, cy, paint)
                        canvas.drawLine(cx + r * 0.82f, cy, cx + r * 0.32f, cy + r * 0.48f, paint)
                        canvas.drawLine(cx + r * 0.15f, cy - r * 0.58f, cx - r * 0.15f, cy + r * 0.58f, paint)
                    }
                    8 -> { // 8. חישוב אחוזים: סמל %
                        canvas.drawLine(cx - r * 0.62f, cy + r * 0.62f, cx + r * 0.62f, cy - r * 0.62f, paint)
                        canvas.drawCircle(cx - r * 0.40f, cy - r * 0.40f, r * 0.22f, paint)
                        canvas.drawCircle(cx + r * 0.40f, cy + r * 0.40f, r * 0.22f, paint)
                    }
                    9 -> { // 9. חישוב הנחות: תגית מחיר אלכסונית עם חור תלייה
                        val tag = Path().apply {
                            moveTo(cx - r * 0.78f, cy - r * 0.78f)
                            lineTo(cx + r * 0.12f, cy - r * 0.78f)
                            lineTo(cx + r * 0.84f, cy - r * 0.04f)
                            lineTo(cx - r * 0.04f, cy + r * 0.84f)
                            lineTo(cx - r * 0.78f, cy + r * 0.12f)
                            close()
                        }
                        canvas.drawPath(tag, paint)
                        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.38f, r * 0.13f, paint)
                    }
                    10 -> { // 10. חלוקת טיפים: ספל קפה חם על תחתית
                        canvas.drawRoundRect(RectF(cx - r * 0.65f, cy - r * 0.35f, cx + r * 0.42f, cy + r * 0.52f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawArc(RectF(cx + r * 0.25f, cy - r * 0.20f, cx + r * 0.82f, cy + r * 0.32f), -90f, 180f, false, paint)
                        canvas.drawLine(cx - r * 0.82f, cy + r * 0.72f, cx + r * 0.65f, cy + r * 0.72f, paint)
                        canvas.drawLine(cx - r * 0.25f, cy - r * 0.58f, cx - r * 0.25f, cy - r * 0.82f, paint)
                        canvas.drawLine(cx + r * 0.08f, cy - r * 0.58f, cx + r * 0.08f, cy - r * 0.82f, paint)
                    }
                    11 -> { // 11. מדד BMI: לב עם פעימת אק"ג (ECG Heart)
                        val ecg = Path().apply {
                            moveTo(cx - r * 0.88f, cy + r * 0.05f)
                            lineTo(cx - r * 0.35f, cy + r * 0.05f)
                            lineTo(cx - r * 0.16f, cy - r * 0.65f)
                            lineTo(cx + r * 0.16f, cy + r * 0.65f)
                            lineTo(cx + r * 0.35f, cy + r * 0.05f)
                            lineTo(cx + r * 0.88f, cy + r * 0.05f)
                        }
                        canvas.drawPath(ecg, paint)
                    }
                    12 -> { // 12. חישוב גיל: עוגת יום הולדת עם נר דולק
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy + r * 0.02f, cx + r * 0.76f, cy + r * 0.78f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.38f, cx + r * 0.76f, cy + r * 0.38f, paint)
                        canvas.drawLine(cx, cy + r * 0.02f, cx, cy - r * 0.48f, paint)
                        canvas.drawCircle(cx, cy - r * 0.68f, r * 0.13f, paint)
                    }
                    13 -> { // 13. מטבעות לייב: גלובוס עם קווי אורך ורוחב
                        canvas.drawCircle(cx, cy, r * 0.80f, paint)
                        canvas.drawOval(RectF(cx - r * 0.40f, cy - r * 0.80f, cx + r * 0.40f, cy + r * 0.80f), paint)
                        canvas.drawLine(cx - r * 0.80f, cy, cx + r * 0.80f, cy, paint)
                    }
                    14 -> { // 14. מחשבון BMR: להבת אנרגיה ומטבוליזם
                        val flame = Path().apply {
                            moveTo(cx, cy - r * 0.88f)
                            cubicTo(cx + r * 0.82f, cy - r * 0.10f, cx + r * 0.65f, cy + r * 0.78f, cx, cy + r * 0.86f)
                            cubicTo(cx - r * 0.65f, cy + r * 0.78f, cx - r * 0.82f, cy - r * 0.10f, cx, cy - r * 0.88f)
                        }
                        canvas.drawPath(flame, paint)
                        canvas.drawCircle(cx, cy + r * 0.35f, r * 0.22f, paint)
                    }
                    15 -> { // 15. משוואות ריבועיות: סמל שורש √
                        val rootPath = Path().apply {
                            moveTo(cx - r * 0.84f, cy + r * 0.12f)
                            lineTo(cx - r * 0.48f, cy + r * 0.12f)
                            lineTo(cx - r * 0.18f, cy + r * 0.72f)
                            lineTo(cx + r * 0.32f, cy - r * 0.68f)
                            lineTo(cx + r * 0.84f, cy - r * 0.68f)
                        }
                        canvas.drawPath(rootPath, paint)
                    }
                    16 -> { // 16. מחשבון שברים: 1/2
                        canvas.drawLine(cx - r * 0.62f, cy + r * 0.62f, cx + r * 0.62f, cy - r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.38f, cy - r * 0.66f, cx - r * 0.38f, cy - r * 0.16f, paint)
                        canvas.drawLine(cx + r * 0.24f, cy + r * 0.24f, cx + r * 0.60f, cy + r * 0.24f, paint)
                        canvas.drawLine(cx + r * 0.24f, cy + r * 0.62f, cx + r * 0.60f, cy + r * 0.62f, paint)
                    }
                    17 -> { // 17. ריבית דריבית: גרף צמיחה מעריכי עם חץ עליון
                        val trend = Path().apply {
                            moveTo(cx - r * 0.78f, cy + r * 0.68f)
                            lineTo(cx - r * 0.22f, cy + r * 0.18f)
                            lineTo(cx + r * 0.15f, cy + r * 0.38f)
                            lineTo(cx + r * 0.78f, cy - r * 0.58f)
                        }
                        canvas.drawPath(trend, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.58f, cx + r * 0.78f, cy - r * 0.58f, paint)
                        canvas.drawLine(cx + r * 0.78f, cy - r * 0.58f, cx + r * 0.78f, cy - r * 0.16f, paint)
                    }
                    18 -> { // 18. המרת מהירות: מד מהירות (Speedometer) עם מחוג
                        canvas.drawArc(RectF(cx - r * 0.84f, cy - r * 0.62f, cx + r * 0.84f, cy + r * 0.84f), 165f, 210f, false, paint)
                        canvas.drawLine(cx, cy + r * 0.22f, cx + r * 0.48f, cy - r * 0.34f, paint)
                        canvas.drawCircle(cx, cy + r * 0.22f, r * 0.12f, dotPaint)
                    }
                    19 -> { // 19. הגדרות: גלגל שיניים קווי 8 שיניים
                        canvas.drawCircle(cx, cy, r * 0.34f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.44f).toFloat(),
                                (cy + sin(a) * r * 0.44f).toFloat(),
                                (cx + cos(a) * r * 0.80f).toFloat(),
                                (cy + sin(a) * r * 0.80f).toFloat(),
                                paint
                            )
                        }
                    }
                    20 -> { // 20. מחשבון עלות דלק: משאבת דלק עם ידית תדלוק
                        canvas.drawRoundRect(RectF(cx - r * 0.58f, cy - r * 0.76f, cx + r * 0.34f, cy + r * 0.76f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawRect(cx - r * 0.36f, cy - r * 0.52f, cx + r * 0.12f, cy - r * 0.14f, paint)
                        canvas.drawLine(cx + r * 0.34f, cy - r * 0.28f, cx + r * 0.74f, cy + r * 0.12f, paint)
                        canvas.drawLine(cx + r * 0.74f, cy + r * 0.12f, cx + r * 0.74f, cy + r * 0.58f, paint)
                    }
                    21 -> { // 21. יעילות שריפת דלק: עלה אקולוגי (Eco Leaf) עם עורק מרכזי
                        val leaf = Path().apply {
                            moveTo(cx - r * 0.68f, cy + r * 0.68f)
                            quadTo(cx - r * 0.75f, cy - r * 0.65f, cx + r * 0.75f, cy - r * 0.75f)
                            quadTo(cx + r * 0.65f, cy + r * 0.75f, cx - r * 0.68f, cy + r * 0.68f)
                        }
                        canvas.drawPath(leaf, paint)
                        canvas.drawLine(cx - r * 0.68f, cy + r * 0.68f, cx + r * 0.38f, cy - r * 0.38f, paint)
                    }
                    22 -> { // 22. ממוצע ציונים: כובע אקדמי (Graduation Cap)
                        val cap = Path().apply {
                            moveTo(cx, cy - r * 0.72f)
                            lineTo(cx + r * 0.88f, cy - r * 0.18f)
                            lineTo(cx, cy + r * 0.36f)
                            lineTo(cx - r * 0.88f, cy - r * 0.18f)
                            close()
                        }
                        canvas.drawPath(cap, paint)
                        canvas.drawLine(cx + r * 0.68f, cy - r * 0.05f, cx + r * 0.68f, cy + r * 0.68f, paint)
                    }
                    23 -> { // 23. מחשבון תשר מתקדם: פעמון הגשה למסעדה (Cloche)
                        canvas.drawArc(RectF(cx - r * 0.76f, cy - r * 0.48f, cx + r * 0.76f, cy + r * 0.85f), 180f, 180f, true, paint)
                        canvas.drawCircle(cx, cy - r * 0.62f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.88f, cy + r * 0.42f, cx + r * 0.88f, cy + r * 0.42f, paint)
                    }
                    24 -> { // 24. מחשבון מס מכירות: שקית קניות עם ידית קשתית
                        canvas.drawRoundRect(RectF(cx - r * 0.66f, cy - r * 0.32f, cx + r * 0.66f, cy + r * 0.80f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawArc(RectF(cx - r * 0.36f, cy - r * 0.78f, cx + r * 0.36f, cy + r * 0.02f), 180f, 180f, false, paint)
                    }
                    25 -> { // 25. מחשבון מחיר יחידתי: שתי קופסאות להשוואה עם חץ ביניהן
                        canvas.drawRect(cx - r * 0.82f, cy - r * 0.45f, cx - r * 0.18f, cy + r * 0.45f, paint)
                        canvas.drawRect(cx + r * 0.18f, cy - r * 0.65f, cx + r * 0.82f, cy + r * 0.45f, paint)
                    }
                    26 -> { // 26. ממיר שעון עולמי: פני שעון עם מחוגים ונקודות שעה
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawLine(cx, cy - r * 0.48f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.38f, cy + r * 0.22f, paint)
                        canvas.drawCircle(cx, cy - r * 0.66f, r * 0.06f, dotPaint)
                        canvas.drawCircle(cx + r * 0.66f, cy, r * 0.06f, dotPaint)
                    }
                    27 -> { // 27. מחשבון ביוץ: פרח 6 עלי כותרת
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawCircle(
                                (cx + cos(a) * r * 0.50f).toFloat(),
                                (cy + sin(a) * r * 0.50f).toFloat(),
                                r * 0.26f,
                                paint
                            )
                        }
                    }
                    28 -> { // 28. ממיר הקסדצימלי: סולמית # (Hex Grid)
                        canvas.drawLine(cx - r * 0.32f, cy - r * 0.76f, cx - r * 0.32f, cy + r * 0.76f, paint)
                        canvas.drawLine(cx + r * 0.32f, cy - r * 0.76f, cx + r * 0.32f, cy + r * 0.76f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.32f, cx + r * 0.76f, cy - r * 0.32f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.32f, cx + r * 0.76f, cy + r * 0.32f, paint)
                    }
                    29 -> { // 29. מחשבון חסכונות: יהלום מלוטש (Diamond Gem)
                        val gem = Path().apply {
                            moveTo(cx - r * 0.48f, cy - r * 0.72f)
                            lineTo(cx + r * 0.48f, cy - r * 0.72f)
                            lineTo(cx + r * 0.84f, cy - r * 0.18f)
                            lineTo(cx, cy + r * 0.82f)
                            lineTo(cx - r * 0.84f, cy - r * 0.18f)
                            close()
                        }
                        canvas.drawPath(gem, paint)
                        canvas.drawLine(cx - r * 0.84f, cy - r * 0.18f, cx + r * 0.84f, cy - r * 0.18f, paint)
                    }
                    30 -> { // 30. מחשבון תאריך: לוח שנה עם טבעות תלייה עליונות
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.62f, cx + r * 0.76f, cy + r * 0.78f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.18f, cx + r * 0.76f, cy - r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.84f, cx - r * 0.36f, cy - r * 0.42f, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.84f, cx + r * 0.36f, cy - r * 0.42f, paint)
                    }
                    // --- סמלים וקטוריים ייחודיים לכל כלי ה-Pro (31..65) ---
                    31, 305 -> { // 31. כל 87 קטגוריות היחידות: קליבר מדידה הנדסי (Vernier Caliper)
                        canvas.drawLine(cx - r * 0.84f, cy - r * 0.36f, cx + r * 0.84f, cy - r * 0.36f, paint)
                        canvas.drawLine(cx - r * 0.84f, cy - r * 0.36f, cx - r * 0.84f, cy + r * 0.66f, paint)
                        canvas.drawLine(cx - r * 0.16f, cy - r * 0.56f, cx - r * 0.16f, cy + r * 0.56f, paint)
                        canvas.drawRect(cx - r * 0.16f, cy - r * 0.56f, cx + r * 0.36f, cy - r * 0.16f, paint)
                    }
                    32, 306 -> { // 32. אינסטלציה, צנרת, ספיקה ולחץ: צינור עם ברז וטיפה
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.18f, cx + r * 0.82f, cy + r * 0.18f), r * 0.10f, r * 0.10f, paint)
                        canvas.drawLine(cx, cy - r * 0.18f, cx, cy - r * 0.66f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.66f, cx + r * 0.42f, cy - r * 0.66f, paint)
                        canvas.drawCircle(cx, cy + r * 0.58f, r * 0.16f, paint)
                    }
                    33 -> { // 33. מינון תרופות, עירוי IV וסוכר: שקית עירוי עם צלב רפואי
                        canvas.drawRoundRect(RectF(cx - r * 0.52f, cy - r * 0.74f, cx + r * 0.52f, cy + r * 0.42f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.16f, cx + r * 0.24f, cy - r * 0.16f, paint)
                        canvas.drawLine(cx, cy - r * 0.40f, cx, cy + r * 0.08f, paint)
                        canvas.drawLine(cx, cy + r * 0.42f, cx, cy + r * 0.84f, paint)
                    }
                    34 -> { // 34. בנייה, בטון, ריצוף ומיזוג BTU: קובייה איזומטרית תלת-ממדית
                        val cube = Path().apply {
                            moveTo(cx, cy - r * 0.82f)
                            lineTo(cx + r * 0.74f, cy - r * 0.38f)
                            lineTo(cx + r * 0.74f, cy + r * 0.40f)
                            lineTo(cx, cy + r * 0.82f)
                            lineTo(cx - r * 0.74f, cy + r * 0.40f)
                            lineTo(cx - r * 0.74f, cy - r * 0.38f)
                            close()
                        }
                        canvas.drawPath(cube, paint)
                        canvas.drawLine(cx - r * 0.74f, cy - r * 0.38f, cx, cy + r * 0.05f, paint)
                        canvas.drawLine(cx + r * 0.74f, cy - r * 0.38f, cx, cy + r * 0.05f, paint)
                        canvas.drawLine(cx, cy + r * 0.05f, cx, cy + r * 0.82f, paint)
                    }
                    35 -> { // 35. קודי צבע של נגדים: נגד חשמלי עם 3 פסי צבע
                        canvas.drawLine(cx - r * 0.90f, cy, cx - r * 0.56f, cy, paint)
                        canvas.drawLine(cx + r * 0.56f, cy, cx + r * 0.90f, cy, paint)
                        canvas.drawRoundRect(RectF(cx - r * 0.56f, cy - r * 0.34f, cx + r * 0.56f, cy + r * 0.34f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.25f, cy - r * 0.34f, cx - r * 0.25f, cy + r * 0.34f, paint)
                        canvas.drawLine(cx, cy - r * 0.34f, cx, cy + r * 0.34f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy - r * 0.34f, cx + r * 0.25f, cy + r * 0.34f, paint)
                    }
                    36 -> { // 36. מפענח קודי קבלים: קבל לוחות מקבילים
                        canvas.drawLine(cx - r * 0.86f, cy, cx - r * 0.18f, cy, paint)
                        canvas.drawLine(cx + r * 0.18f, cy, cx + r * 0.86f, cy, paint)
                        canvas.drawLine(cx - r * 0.18f, cy - r * 0.62f, cx - r * 0.18f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx + r * 0.18f, cy - r * 0.62f, cx + r * 0.18f, cy + r * 0.62f, paint)
                    }
                    37 -> { // 37. עוביי חוטים AWG/SWG: חתך כבל רב-גידי
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawCircle(cx, cy, r * 0.20f, dotPaint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawCircle((cx + cos(a) * r * 0.46f).toFloat(), (cy + sin(a) * r * 0.46f).toFloat(), r * 0.12f, dotPaint)
                        }
                    }
                    38, 307 -> { // 38. מעבדת חוק אוהם: סמל אומגה (Ω)
                        canvas.drawArc(RectF(cx - r * 0.66f, cy - r * 0.72f, cx + r * 0.66f, cy + r * 0.48f), 135f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.56f, cx - r * 0.28f, cy + r * 0.56f, paint)
                        canvas.drawLine(cx + r * 0.28f, cy + r * 0.56f, cx + r * 0.78f, cy + r * 0.56f, paint)
                    }
                    39 -> { // 39. פלס בנאים דו-צירי: מבחנת פלס אופקית עם בועה במרכז
                        canvas.drawRoundRect(RectF(cx - r * 0.86f, cy - r * 0.32f, cx + r * 0.86f, cy + r * 0.32f), r * 0.32f, r * 0.32f, paint)
                        canvas.drawLine(cx - r * 0.26f, cy - r * 0.32f, cx - r * 0.26f, cy + r * 0.32f, paint)
                        canvas.drawLine(cx + r * 0.26f, cy - r * 0.32f, cx + r * 0.26f, cy + r * 0.32f, paint)
                        canvas.drawCircle(cx, cy, r * 0.16f, dotPaint)
                    }
                    40, 308 -> { // 40. מצפן 3D מכויל: חוגת מצפן עם מחט מעוינת
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        val needle = Path().apply {
                            moveTo(cx + r * 0.46f, cy - r * 0.46f)
                            lineTo(cx + r * 0.13f, cy + r * 0.13f)
                            lineTo(cx - r * 0.46f, cy + r * 0.46f)
                            lineTo(cx - r * 0.13f, cy - r * 0.13f)
                            close()
                        }
                        canvas.drawPath(needle, paint)
                        canvas.drawCircle(cx, cy, r * 0.09f, dotPaint)
                    }
                    41 -> { // 41. מד-זווית דו-זרועי וסרגל: קשת מד-זווית עם זרוע
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.40f, cx + r * 0.82f, cy + r * 1.12f), 180f, 180f, true, paint)
                        canvas.drawLine(cx, cy + r * 0.36f, cx + r * 0.54f, cy - r * 0.24f, paint)
                        canvas.drawCircle(cx, cy + r * 0.36f, r * 0.11f, dotPaint)
                    }
                    42 -> { // 42. שעון עצר כרונוגרף: סטופר עם לחצן עליון
                        canvas.drawCircle(cx, cy + r * 0.10f, r * 0.70f, paint)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.82f, cx + r * 0.24f, cy - r * 0.82f, paint)
                        canvas.drawLine(cx, cy - r * 0.82f, cx, cy - r * 0.60f, paint)
                        canvas.drawLine(cx + r * 0.46f, cy - r * 0.56f, cx + r * 0.66f, cy - r * 0.72f, paint)
                        canvas.drawLine(cx, cy + r * 0.10f, cx, cy - r * 0.30f, paint)
                        canvas.drawLine(cx, cy + r * 0.10f, cx + r * 0.34f, cy + r * 0.10f, paint)
                    }
                    43 -> { // 43. פנס LED פיזי ואיתות SOS: פנס יד עם קרני אור
                        val body = Path().apply {
                            moveTo(cx - r * 0.26f, cy + r * 0.78f)
                            lineTo(cx + r * 0.26f, cy + r * 0.78f)
                            lineTo(cx + r * 0.26f, cy + r * 0.10f)
                            lineTo(cx + r * 0.52f, cy - r * 0.30f)
                            lineTo(cx - r * 0.52f, cy - r * 0.30f)
                            lineTo(cx - r * 0.26f, cy + r * 0.10f)
                            close()
                        }
                        canvas.drawPath(body, paint)
                        canvas.drawLine(cx, cy - r * 0.50f, cx, cy - r * 0.84f, paint)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.46f, cx - r * 0.60f, cy - r * 0.76f, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.46f, cx + r * 0.60f, cy - r * 0.76f, paint)
                    }
                    44 -> { // 44. הטבלה המחזורית ואטום: מודל אטום עם 2 מסלולים אליפטיים וגרעין
                        canvas.save(); canvas.rotate(30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.86f, cy - r * 0.34f, cx + r * 0.86f, cy + r * 0.34f), paint)
                        canvas.restore(); canvas.save(); canvas.rotate(-30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.86f, cy - r * 0.34f, cx + r * 0.86f, cy + r * 0.34f), paint)
                        canvas.restore()
                        canvas.drawCircle(cx, cy, r * 0.15f, dotPaint)
                    }
                    45 -> { // 45. מתרגם ומשדר קוד מורס: בועית שידור עם נקודה-קו-נקודה
                        canvas.drawRoundRect(RectF(cx - r * 0.84f, cy - r * 0.58f, cx + r * 0.84f, cy + r * 0.52f), r * 0.30f, r * 0.30f, paint)
                        canvas.drawCircle(cx - r * 0.48f, cy - r * 0.03f, r * 0.11f, dotPaint)
                        canvas.drawLine(cx - r * 0.18f, cy - r * 0.03f, cx + r * 0.22f, cy - r * 0.03f, paint)
                        canvas.drawCircle(cx + r * 0.52f, cy - r * 0.03f, r * 0.11f, dotPaint)
                    }
                    46 -> { // 46. מחולל סיסמאות ומד אנטרופיה: מנעול אבטחה (Padlock)
                        canvas.drawRoundRect(RectF(cx - r * 0.56f, cy - r * 0.08f, cx + r * 0.56f, cy + r * 0.76f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawArc(RectF(cx - r * 0.36f, cy - r * 0.76f, cx + r * 0.36f, cy + r * 0.08f), 180f, 180f, false, paint)
                        canvas.drawCircle(cx, cy + r * 0.32f, r * 0.13f, dotPaint)
                    }
                    47 -> { // 47. גימטריה, אתב״ש וספרות רומיות: לוח ספרות רומיות I V
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.62f, cx + r * 0.72f, cy - r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy + r * 0.62f, cx + r * 0.72f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.62f, cx - r * 0.42f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx, cy - r * 0.62f, cx + r * 0.30f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx + r * 0.60f, cy - r * 0.62f, cx + r * 0.30f, cy + r * 0.62f, paint)
                    }
                    48 -> { // 48. מתכות יקרות: שני מטילי זהב טרפזיים מונחים זה על זה
                        val ingotBot = Path().apply {
                            moveTo(cx - r * 0.52f, cy + r * 0.22f)
                            lineTo(cx + r * 0.52f, cy + r * 0.22f)
                            lineTo(cx + r * 0.78f, cy + r * 0.72f)
                            lineTo(cx - r * 0.78f, cy + r * 0.72f)
                            close()
                        }
                        val ingotTop = Path().apply {
                            moveTo(cx - r * 0.35f, cy - r * 0.42f)
                            lineTo(cx + r * 0.35f, cy - r * 0.42f)
                            lineTo(cx + r * 0.55f, cy + r * 0.05f)
                            lineTo(cx - r * 0.55f, cy + r * 0.05f)
                            close()
                        }
                        canvas.drawPath(ingotBot, paint)
                        canvas.drawPath(ingotTop, paint)
                    }
                    49 -> { // 49. משכנתא ולוח סילוקין שפיצר: בית עם גג ודלת
                        val roof = Path().apply {
                            moveTo(cx - r * 0.84f, cy - r * 0.05f)
                            lineTo(cx, cy - r * 0.80f)
                            lineTo(cx + r * 0.84f, cy - r * 0.05f)
                        }
                        canvas.drawPath(roof, paint)
                        canvas.drawRect(cx - r * 0.56f, cy - r * 0.05f, cx + r * 0.56f, cy + r * 0.76f, paint)
                        canvas.drawRect(cx - r * 0.16f, cy + r * 0.32f, cx + r * 0.16f, cy + r * 0.76f, paint)
                    }
                    50, 310 -> { // 50. תשואת ROI ו-CAGR: גרף 3 עמודות עולות
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.72f, cx + r * 0.80f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx - r * 0.65f, cy + r * 0.15f, cx - r * 0.35f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx - r * 0.15f, cy - r * 0.20f, cx + r * 0.15f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx + r * 0.35f, cy - r * 0.65f, cx + r * 0.65f, cy + r * 0.72f, paint)
                    }
                    51 -> { // 51. מרווח רווח ונקודת איזון: מזוודת עסקים
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.38f, cx + r * 0.76f, cy + r * 0.72f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawRect(cx - r * 0.30f, cy - r * 0.72f, cx + r * 0.30f, cy - r * 0.38f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.12f, cx + r * 0.76f, cy + r * 0.12f, paint)
                    }
                    52 -> { // 52. סטטיסטיקה ועקומת פעמון: פעמון גאוס עם קו ממוצע
                        val bell = Path().apply {
                            moveTo(cx - r * 0.84f, cy + r * 0.62f)
                            cubicTo(cx - r * 0.34f, cy + r * 0.62f, cx - r * 0.30f, cy - r * 0.72f, cx, cy - r * 0.72f)
                            cubicTo(cx + r * 0.30f, cy - r * 0.72f, cx + r * 0.34f, cy + r * 0.62f, cx + r * 0.84f, cy + r * 0.62f)
                        }
                        canvas.drawPath(bell, paint)
                        canvas.drawLine(cx, cy - r * 0.72f, cx, cy + r * 0.62f, paint)
                    }
                    53 -> { // 53. גאומטריה 2D/3D ופיתגורס: משולש ישר-זווית עם סימון 90°
                        val tri = Path().apply {
                            moveTo(cx - r * 0.72f, cy + r * 0.68f)
                            lineTo(cx + r * 0.74f, cy + r * 0.68f)
                            lineTo(cx - r * 0.72f, cy - r * 0.72f)
                            close()
                        }
                        canvas.drawPath(tri, paint)
                        canvas.drawRect(cx - r * 0.72f, cy + r * 0.38f, cx - r * 0.42f, cy + r * 0.68f, paint)
                    }
                    54, 311 -> { // 54. תורת המספרים וקומבינטוריקה: סמל סכום סיגמא (Σ)
                        val sigma = Path().apply {
                            moveTo(cx + r * 0.62f, cy - r * 0.70f)
                            lineTo(cx - r * 0.62f, cy - r * 0.70f)
                            lineTo(cx + r * 0.10f, cy)
                            lineTo(cx - r * 0.62f, cy + r * 0.70f)
                            lineTo(cx + r * 0.62f, cy + r * 0.70f)
                        }
                        canvas.drawPath(sigma, paint)
                    }
                    55 -> { // 55. ייצוא PDF/CSV וגיבוי Drive: ענן עם חץ העלאה
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.25f, cx + r * 0.76f, cy + r * 0.45f), r * 0.35f, r * 0.35f, paint)
                        canvas.drawArc(RectF(cx - r * 0.40f, cy - r * 0.68f, cx + r * 0.36f, cy + r * 0.05f), 180f, 180f, false, paint)
                        canvas.drawLine(cx, cy + r * 0.76f, cx, cy - r * 0.10f, paint)
                        canvas.drawLine(cx - r * 0.22f, cy + r * 0.12f, cx, cy - r * 0.10f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy + r * 0.12f, cx, cy - r * 0.10f, paint)
                    }
                    56, 102, 103, 300 -> { // 56. כוכב 4-קודקודים קווי (✧)
                        val star4 = Path().apply {
                            moveTo(cx, cy - r * 0.95f)
                            quadTo(cx + r * 0.12f, cy - r * 0.12f, cx + r * 0.95f, cy)
                            quadTo(cx + r * 0.12f, cy + r * 0.12f, cx, cy + r * 0.95f)
                            quadTo(cx - r * 0.12f, cy + r * 0.12f, cx - r * 0.95f, cy)
                            quadTo(cx - r * 0.12f, cy - r * 0.12f, cx, cy - r * 0.95f)
                            close()
                        }
                        if (iconCode == 102) {
                            canvas.drawPath(star4, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = ACCENT_SAGE_GREEN
                                style = Paint.Style.FILL
                            })
                        }
                        canvas.drawPath(star4, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    57 -> { // 57. ממירי מכניקה, כוח ומומנט: מפתח ברגים / גלגל תנופה
                        canvas.drawCircle(cx, cy, r * 0.78f, paint)
                        canvas.drawCircle(cx, cy, r * 0.26f, paint)
                        for (i in 0 until 3) {
                            val a = Math.toRadians((i * 120).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.26f).toFloat(),
                                (cy + sin(a) * r * 0.26f).toFloat(),
                                (cx + cos(a) * r * 0.78f).toFloat(),
                                (cy + sin(a) * r * 0.78f).toFloat(),
                                paint
                            )
                        }
                    }
                    58 -> { // 58. ממירי זורמים, צמיגות וספיקה: טיפת מים קווית
                        val drop = Path().apply {
                            moveTo(cx, cy - r * 0.85f)
                            quadTo(cx + r * 0.76f, cy + r * 0.15f, cx, cy + r * 0.82f)
                            quadTo(cx - r * 0.76f, cy + r * 0.15f, cx, cy - r * 0.85f)
                        }
                        canvas.drawPath(drop, paint)
                    }
                    59 -> { // 59. ממירי תרמודינמיקה וחום: 3 גלי חום אנכיים
                        for (offset in listOf(-0.38f, 0f, 0.38f)) {
                            val ox = cx + r * offset
                            val wPath = Path().apply {
                                moveTo(ox, cy + r * 0.68f)
                                quadTo(ox - r * 0.22f, cy + r * 0.20f, ox, cy)
                                quadTo(ox + r * 0.22f, cy - r * 0.20f, ox, cy - r * 0.68f)
                            }
                            canvas.drawPath(wPath, paint)
                        }
                    }
                    60 -> { // 60. ממירי חשמל, מגנטיות ושדה: ברק אנרגיה (Lightning Bolt)
                        val bolt = Path().apply {
                            moveTo(cx + r * 0.16f, cy - r * 0.86f)
                            lineTo(cx - r * 0.54f, cy + r * 0.06f)
                            lineTo(cx + r * 0.06f, cy + r * 0.06f)
                            lineTo(cx - r * 0.16f, cy + r * 0.86f)
                            lineTo(cx + r * 0.54f, cy - r * 0.06f)
                            lineTo(cx - r * 0.06f, cy - r * 0.06f)
                            close()
                        }
                        canvas.drawPath(bolt, paint)
                    }
                    61 -> { // 61. ממירי אופטיקה, אקוסטיקה וקרינה: שמש אופטית עם קרניים
                        canvas.drawCircle(cx, cy, r * 0.38f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.54f).toFloat(),
                                (cy + sin(a) * r * 0.54f).toFloat(),
                                (cx + cos(a) * r * 0.84f).toFloat(),
                                (cy + sin(a) * r * 0.84f).toFloat(),
                                paint
                            )
                        }
                    }
                    62 -> { // 62. ממירי רפואה, כימיה ומעבדה: בקבוק מעבדה קוני (Erlenmeyer Flask)
                        val flask = Path().apply {
                            moveTo(cx - r * 0.24f, cy - r * 0.78f)
                            lineTo(cx + r * 0.24f, cy - r * 0.78f)
                            lineTo(cx + r * 0.24f, cy - r * 0.28f)
                            lineTo(cx + r * 0.74f, cy + r * 0.76f)
                            lineTo(cx - r * 0.74f, cy + r * 0.76f)
                            lineTo(cx - r * 0.24f, cy - r * 0.28f)
                            close()
                        }
                        canvas.drawPath(flask, paint)
                        canvas.drawLine(cx - r * 0.48f, cy + r * 0.28f, cx + r * 0.48f, cy + r * 0.28f, paint)
                    }
                    63 -> { // 63. ממירי תקשורת, טיפוגרפיה, חלל ובישול: כוכב לכת מוקף טבעת
                        canvas.drawCircle(cx, cy, r * 0.42f, paint)
                        canvas.save(); canvas.rotate(-25f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.88f, cy - r * 0.20f, cx + r * 0.88f, cy + r * 0.20f), paint)
                        canvas.restore()
                    }
                    64 -> { // 64. מחשבון שכר, שעות נוספות, פנסיה ונטו: תלוש שכר ושטר כסף
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.48f, cx + r * 0.82f, cy + r * 0.48f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                    }
                    65 -> { // 65. מטריצות, דטרמיננטה ומערכת משוואות: סוגרי מטריצה [ :: ]
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.70f, cx - r * 0.44f, cy - r * 0.70f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.70f, cx - r * 0.72f, cy + r * 0.70f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy + r * 0.70f, cx - r * 0.44f, cy + r * 0.70f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.70f, cx + r * 0.44f, cy - r * 0.70f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.70f, cx + r * 0.72f, cy + r * 0.70f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy + r * 0.70f, cx + r * 0.44f, cy + r * 0.70f, paint)
                        canvas.drawCircle(cx - r * 0.26f, cy - r * 0.26f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy - r * 0.26f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx - r * 0.26f, cy + r * 0.26f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy + r * 0.26f, r * 0.10f, dotPaint)
                    }
                    100 -> { // 100. חלון צף PiP
                        canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.80f, cx + r * 0.85f, cy + r * 0.80f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawLine(cx - r * 0.85f, cy - r * 0.35f, cx + r * 0.85f, cy - r * 0.35f, paint)
                        canvas.drawRoundRect(RectF(cx + r * 0.08f, cy + r * 0.05f, cx + r * 0.58f, cy + r * 0.52f), r * 0.08f, r * 0.08f, dotPaint)
                    }
                    101 -> { // 101. שעון היסטוריה
                        canvas.drawCircle(cx, cy, r * 0.86f, paint)
                        canvas.drawLine(cx, cy - r * 0.48f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.36f, cy + r * 0.24f, paint)
                    }
                    200 -> { // 200. חיפוש: זכוכית מגדלת
                        canvas.drawCircle(cx - r * 0.12f, cy - r * 0.12f, r * 0.52f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.25f, cx + r * 0.78f, cy + r * 0.78f, paint)
                    }
                    201 -> { // 201. צליל: גלי קול
                        canvas.drawLine(cx - r * 0.60f, cy - r * 0.25f, cx - r * 0.60f, cy + r * 0.25f, paint)
                        canvas.drawLine(cx - r * 0.20f, cy - r * 0.65f, cx - r * 0.20f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.20f, cy - r * 0.42f, cx + r * 0.20f, cy + r * 0.42f, paint)
                        canvas.drawLine(cx + r * 0.60f, cy - r * 0.75f, cx + r * 0.60f, cy + r * 0.75f, paint)
                    }
                    202 -> { // 202. בהירות
                        canvas.drawCircle(cx, cy, r * 0.42f, paint)
                    }
                    203 -> { // 203. שקיפות
                        canvas.drawCircle(cx - r * 0.22f, cy, r * 0.48f, paint)
                        canvas.drawCircle(cx + r * 0.22f, cy, r * 0.48f, paint)
                    }
                    // --- סמלי קטגוריות העל בתפריט הצד (301..304) ---
                    301 -> { // 301. מתמטיקה ומדע: ארבע פעולות החשבון (+ − × ÷) בריבוע
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy, cx + r * 0.35f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.35f, cx, cy + r * 0.35f, paint)
                    }
                    302 -> { // 302. פיננסים וצרכנות: שטר כסף ומטבע
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.48f, cx + r * 0.82f, cy + r * 0.48f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                    }
                    303 -> { // 303. המרות, רכב ודלק: שני חצי החלפה מעגליים (🔄)
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), -150f, 130f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), 30f, 130f, false, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.72f, cx + r * 0.68f, cy - r * 0.38f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.72f, cx - r * 0.68f, cy + r * 0.38f, paint)
                    }
                    304 -> { // 304. בריאות, זמן ותאריכים: לב קווי קלאסי (❤️)
                        val heart = Path().apply {
                            moveTo(cx, cy + r * 0.76f)
                            lineTo(cx - r * 0.78f, cy - r * 0.10f)
                            arcTo(RectF(cx - r * 0.78f, cy - r * 0.72f, cx, cy - r * 0.05f), 180f, 180f, false)
                            arcTo(RectF(cx, cy - r * 0.72f, cx + r * 0.78f, cy - r * 0.05f), 180f, 180f, false)
                            close()
                        }
                        canvas.drawPath(heart, paint)
                    }
                    else -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), r * 0.20f, r * 0.20f, paint)
                        canvas.drawCircle(cx, cy, r * 0.20f, dotPaint)
                    }
                }
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply {
            setBounds(0, 0, sizePx, sizePx)
        }
    }
}

// =========================================================================
// רכיב גלגל הצבעים השטוח 2D (Flat 2D Color Wheel) עבור Screen 5
// =========================================================================
class Flat2DColorWheelView(
    context: Context,
    private val onColorPicked: (Int) -> Unit
) : View(context) {

    private var currentHue: Float = 36f
    private var currentSat: Float = 0.90f
    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
    }

    fun setSelectedColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentSat = hsv[1]
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val cx = width / 2f
            val cy = height / 2f
            val maxR = min(width, height) * 0.44f
            val dx = event.x - cx
            val dy = event.y - cy
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (angle < 0f) angle += 360f
            currentHue = angle
            currentSat = (dist / maxR).coerceIn(0f, 1f)
            val col = Color.HSVToColor(floatArrayOf(currentHue, currentSat, 1f))
            onColorPicked(col)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) * 0.44f
        if (r <= 0f) return

        val sweepColors = intArrayOf(
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        )
        wheelPaint.shader = SweepGradient(cx, cy, sweepColors, null)
        satPaint.shader = RadialGradient(cx, cy, r, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)

        canvas.drawCircle(cx, cy, r, wheelPaint)
        canvas.drawCircle(cx, cy, r, satPaint)

        val rad = Math.toRadians(currentHue.toDouble())
        val kx = (cx + cos(rad) * (currentSat * r)).toFloat()
        val ky = (cy + sin(rad) * (currentSat * r)).toFloat()
        val knobR = 11f * resources.displayMetrics.density

        val pickedCol = Color.HSVToColor(floatArrayOf(currentHue, currentSat, 1f))
        canvas.drawCircle(kx, ky, knobR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pickedCol
            style = Paint.Style.FILL
        })
        canvas.drawCircle(kx, ky, knobR, knobPaint)
    }
}

// =========================================================================
// רכיב הסליידר השטוח (Thin #2C2C2C Line + Sky Blue #B1D4F9 Knob) עבור Screen 5
// =========================================================================
class MinimalistLineSliderView(
    context: Context,
    initialProgress: Float = 0.85f,
    private val onValueChanged: (Float) -> Unit
) : View(context) {

    var progress: Float = initialProgress.coerceIn(0f, 1f)
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val d = resources.displayMetrics.density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 44, 44, 44)
        strokeWidth = 2.0f * d
        strokeCap = Paint.Cap.ROUND
    }
    private val knobFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MinimalistTheme.ACCENT_SKY_BLUE
        style = Paint.Style.FILL
    }
    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 44, 44, 44)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * d
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val pad = 16f * d
            val usableW = (width - 2 * pad).coerceAtLeast(1f)
            progress = ((event.x - pad) / usableW).coerceIn(0f, 1f)
            onValueChanged(progress)
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 16f * d
        val cy = height / 2f
        canvas.drawLine(pad, cy, width - pad, cy, trackPaint)
        val kx = pad + progress * (width - 2 * pad)
        val kr = 7.5f * d
        canvas.drawCircle(kx, cy, kr, knobFillPaint)
        canvas.drawCircle(kx, cy, kr, knobBorderPaint)
    }
}
 

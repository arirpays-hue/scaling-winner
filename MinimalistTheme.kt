package com.example.calculator

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.math.*

object MinimalistTheme {

    const val MINIMALIST_THEME_NAME = "עיצוב מינימליסטי"

    // =========================================================================
    // GLOBAL DESIGN SYSTEM & BRANDING (Pastel Light / Material You - Flat 2D)
    // =========================================================================
    val BG_CREAM = Color.parseColor("#FCF9F5")
    val DRAWER_BG = Color.parseColor("#FCF9F5")
    val DRAWER_HEADER_YELLOW = Color.parseColor("#FFE094")
    val TEXT_CHARCOAL = Color.parseColor("#2C2C2C")
    val TEXT_MUTED_GREY = Color.parseColor("#424242")
    val NEUTRAL_SECONDARY_BG = Color.parseColor("#F1EDE9")
    val CARD_WHITE = Color.parseColor("#FFFFFF")

    // צבעי הדגשה פסטליים שטוחים (Solid Pastel Palette)
    val ACCENT_SAGE_GREEN = Color.parseColor("#A3D5B5")
    val ACCENT_MUTED_GREEN = Color.parseColor("#E2F1E7")
    val ACCENT_SKY_BLUE = Color.parseColor("#B1D4F9")
    val ACCENT_PASTEL_BLUE = Color.parseColor("#D7E9FC")
    val ACCENT_MUTED_PEACH = Color.parseColor("#FF9E80")
    val ACCENT_TERRACOTTA = Color.parseColor("#FFA48E")
    val ACCENT_BUTTER_YELLOW = Color.parseColor("#FFE094")
    val ACCENT_WARM_MUSTARD = Color.parseColor("#FFD54F")

    // תאימות לשמות קבועים בקבצים אחרים
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
    val PILL_MC_BLUE: Int get() = Color.parseColor("#CBE4F0")
    val PILL_MR_MINT: Int get() = ACCENT_SAGE_GREEN
    val PILL_MPLUS_LIME: Int get() = Color.parseColor("#D4E8B0")
    val PILL_MMINUS_YELLOW: Int get() = ACCENT_BUTTER_YELLOW
    val PILL_AC_PERIWINKLE: Int get() = ACCENT_SKY_BLUE
    val PILL_CMD_LAVENDER: Int get() = ACCENT_PASTEL_BLUE
    val PILL_NUM_WHITE: Int get() = CARD_WHITE

    // משתני התאמה אישית פעילים בערכת הנושא המינימליסטית
    var pillCornerPt: Float = 24f
    var shadowSoftness: Int = 0 // עיצוב שטוח לחלוטין (0 = ללא צל)
    var useCustomAccentTint: Boolean = false
    var useCustomBgTint: Boolean = false
    var warmPaperTintMode: Int = 0
    var customAccentColor: Int = ACCENT_TERRACOTTA
    var customBgColor: Int = BG_CREAM
    var customBrightness: Float = 1.0f
    var customOpacity: Float = 1.0f
    var isEditingBackgroundTab: Boolean = false // false = צבע נושא מותאם, true = צבע רקע מותאם

    fun loadMinimalistPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        pillCornerPt = prefs.getFloat("min_pill_corner", 24f)
        shadowSoftness = prefs.getInt("min_shadow_softness", 0)
        useCustomAccentTint = prefs.getBoolean("min_use_custom_accent", false)
        useCustomBgTint = prefs.getBoolean("min_use_custom_bg", false)
        warmPaperTintMode = prefs.getInt("min_paper_tint_mode", 0)
        customAccentColor = prefs.getInt("min_custom_accent_col", ACCENT_TERRACOTTA)
        customBgColor = prefs.getInt("min_custom_bg_col", BG_CREAM)
        customBrightness = prefs.getFloat("min_custom_brightness", 1.0f).coerceIn(0.25f, 1.0f)
        customOpacity = prefs.getFloat("min_custom_opacity", 1.0f).coerceIn(0.25f, 1.0f)
    }

    fun saveMinimalistPrefs(prefs: SharedPreferences?) {
        prefs?.edit()
            ?.putFloat("min_pill_corner", pillCornerPt)
            ?.putInt("min_shadow_softness", shadowSoftness)
            ?.putBoolean("min_use_custom_accent", useCustomAccentTint)
            ?.putBoolean("min_use_custom_bg", useCustomBgTint)
            ?.putInt("min_paper_tint_mode", warmPaperTintMode)
            ?.putInt("min_custom_accent_col", customAccentColor)
            ?.putInt("min_custom_bg_col", customBgColor)
            ?.putFloat("min_custom_brightness", customBrightness)
            ?.putFloat("min_custom_opacity", customOpacity)
            ?.apply()
    }

    private fun applyBrightnessAndOpacity(baseColor: Int, brightness: Float, opacity: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        hsv[2] = (hsv[2] * brightness).coerceIn(0.15f, 1.0f)
        val alpha = (opacity * 255f).toInt().coerceIn(65, 255)
        return Color.HSVToColor(alpha, hsv)
    }

    fun getBackgroundColor(activity: MainActivity? = null): Int {
        if (useCustomBgTint) {
            val src = if (activity != null) activity.bgColor else customBgColor
            return applyBrightnessAndOpacity(src, customBrightness, 1.0f)
        }
        return when (warmPaperTintMode) {
            1 -> Color.parseColor("#FFFFFF")
            2 -> Color.parseColor("#F2F7F4")
            else -> BG_CREAM
        }
    }

    fun getPrimaryActionColor(activity: MainActivity): Int {
        return if (useCustomAccentTint) {
            applyBrightnessAndOpacity(customAccentColor, customBrightness, customOpacity)
        } else {
            ACCENT_TERRACOTTA
        }
    }

    // =========================================================================
    // כפתורי מקלדת שטוחים לחלוטין (Zero 3D, Zero Shadows, Zero Gradients)
    // =========================================================================
    fun createMinimalistKeypadButtonDrawable(
        activity: MainActivity,
        symbol: String,
        isPressed: Boolean = false
    ): Drawable {
        val radius = activity.dp(pillCornerPt.coerceIn(14f, 40f)).toFloat()
        val customAccent = getPrimaryActionColor(activity)
        val baseFill = when (symbol) {
            "MC" -> PILL_MC_BLUE
            "MR" -> ACCENT_SAGE_GREEN
            "M+" -> PILL_MPLUS_LIME
            "M-" -> ACCENT_BUTTER_YELLOW
            "AC" -> ACCENT_SKY_BLUE
            "⌫", "%" -> ACCENT_PASTEL_BLUE
            "÷" -> if (useCustomAccentTint) customAccent else Color.parseColor("#F9B487")
            "−", "-" -> if (useCustomAccentTint) customAccent else ACCENT_TERRACOTTA
            "×" -> if (useCustomAccentTint) customAccent else ACCENT_MUTED_PEACH
            "+" -> if (useCustomAccentTint) customAccent else ACCENT_TERRACOTTA
            "=" -> if (useCustomAccentTint) customAccent else Color.parseColor("#F48484")
            else -> CARD_WHITE
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

    // כרטיסים שטוחים (Flat Cards) ללא הברקות או הצללות תלת-ממד
    fun createMinimalistCardDrawable(
        activity: MainActivity,
        cornerDp: Float = 20f,
        cardStyle: Int = 0 // 0 = לבן שטוח, 1 = ירוק מרווה בהיר לכותרת, 2 = קרם/מנטה לקנבס, 3 = תכלת שמיים, 4 = צהוב חמאה
    ): Drawable {
        val radius = activity.dp(cornerDp).toFloat()
        val fillCol = when (cardStyle) {
            1 -> ACCENT_MUTED_GREEN
            2 -> Color.parseColor("#F7F4EE")
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

    // תיבות קלט שטוחות ומעוגלות (16dp-22dp)
    fun createMinimalistInputDrawable(activity: MainActivity, variant: Int = 0): Drawable {
        val radius = activity.dp(20f).toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
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

    // כפתורי פעולה בצורת קפסולה מלאה (999dp Pill Shape) וללא צללים
    fun createMinimalistActionButtonDrawable(
        activity: MainActivity,
        isPrimary: Boolean,
        pillTone: Int = 0 // 0 = אוטומטי, 1 = Terracotta #FFA48E, 2 = Butter Yellow #FFE094, 3 = Sky Blue #B1D4F9, 4 = Soft Grey #F1EDE9, 5 = White Card Row
    ): Drawable {
        val radius = activity.dp(if (pillTone == 5) 20f else 999f).toFloat()
        val bgCol = when (pillTone) {
            1 -> if (useCustomAccentTint) getPrimaryActionColor(activity) else ACCENT_TERRACOTTA
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

    // קפסולת פריט פעיל בתפריט הצד בגוון ירוק מרווה (#A3D5B5) בדיוק כמו ב-Screen 1
    fun createMinimalistActiveDrawerCapsule(activity: MainActivity): Drawable {
        val radius = activity.dp(999f).toFloat()
        val activeCol = if (useCustomAccentTint) getPrimaryActionColor(activity) else ACCENT_SAGE_GREEN
        return GradientDrawable().apply {
            cornerRadius = radius
            setColor(activeCol)
        }
    }

    // =========================================================================
    // החלת עיצוב שטוח, טיפוגרפיה דקה ונקייה וצבעי פסטל על כל מסכי האפליקציה
    // =========================================================================
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

                    if (v.isClickable && v.background != null) {
                        v.setTextColor(TEXT_CHARCOAL)
                        v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)

                        if (isSettingsTab) {
                            // במסך ההגדרות (Screens 3 & 4): כרטיסים לבנים שטוחים עם אייקוני קו / חיווי ירוק מרווה (#A3D5B5)
                            styleSettingsRowView(v, txt, activity)
                        } else {
                            // במחשבונים ובכלי ה-Pro (כמו פלס בנאים ב-Screen 2): קפסולות בצבעי טרקוטה / צהוב חמאה / תכלת שמיים
                            val tone = when {
                                txt.contains("החלף כיוון") || txt.contains("הקפא") || txt.contains("Hold") || txt.contains("הקפה") -> 2 // Butter Yellow #FFE094
                                txt.contains("נעץ תוצאה") || txt.contains("רשימת סימון") || txt.contains("PDF") || txt.contains("CSV") || txt.contains("Drive") -> 3 // Sky Blue #B1D4F9
                                txt.contains("כייל") || txt.contains("Tare") || txt.startsWith("חשב") || txt.startsWith("פתור") || txt.startsWith("נתח") || txt.startsWith("פענח") || txt.startsWith("צור") || txt.startsWith("התחל") || txt.startsWith("▶️") || txt.startsWith("🆘") || txt.startsWith("🔆") -> 1 // Terracotta #FFA48E
                                else -> 4 // Neutral #F1EDE9
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
                        // כותרות וטקסטים רגילים
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

    // מעצב שורת הגדרה במסך ההגדרות בדיוק לפי Screen 3 ו-Screen 4 בתמונה!
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
            pillTone = if (isChecklistBtn) 3 else 5 // תכלת שמיים #B1D4F9 לרשימת הסימון, לבן שטוח #FFFFFF לשאר השורות
        )

        val rightDrawable: Drawable? = when {
            isChecklistBtn -> createSettingsBadgeDrawable(activity, 7) // וי כחול
            clean.contains("רטט") -> createSettingsBadgeDrawable(activity, if (clean.contains("מופעל")) 1 else 2)
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

    // יוצר את הסמלים המינימליסטיים בצד ימין של שורות ההגדרות (נקודת מרווה #A3D5B5, רמקול, שפופרת, ירח, תיבת סימון, יד, AC)
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
                    1 -> { // עיגול ירוק מרווה (#A3D5B5) למצב מופעל
                        fillP.color = ACCENT_SAGE_GREEN
                        canvas.drawCircle(cx, cy, r * 0.85f, fillP)
                    }
                    2 -> { // עיגול אפור בהיר למצב כבוי
                        fillP.color = NEUTRAL_SECONDARY_BG
                        canvas.drawCircle(cx, cy, r * 0.85f, fillP)
                        canvas.drawCircle(cx, cy, r * 0.85f, strokeP)
                    }
                    3 -> { // רמקול קווי (Speaker)
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
                    4 -> { // שפופרת טלפון קווית
                        canvas.drawArc(RectF(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f), 20f, 115f, false, strokeP)
                        canvas.drawCircle(cx - r * 0.45f, cy - r * 0.35f, r * 0.22f, strokeP)
                        canvas.drawCircle(cx + r * 0.45f, cy + r * 0.35f, r * 0.22f, strokeP)
                    }
                    5 -> { // ירח (Crescent Moon)
                        val moon = Path().apply {
                            addArc(RectF(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f), 40f, 250f)
                        }
                        canvas.drawPath(moon, strokeP)
                    }
                    6 -> { // תיבת סימון מרובעת מעוגלת
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), r * 0.22f, r * 0.22f, strokeP)
                    }
                    7 -> { // סימן וי (Checkmark)
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.05f, cx - r * 0.1f, cy + r * 0.5f, strokeP)
                        canvas.drawLine(cx - r * 0.1f, cy + r * 0.5f, cx + r * 0.65f, cy - r * 0.45f, strokeP)
                    }
                    8 -> { // כיתוב AC נקי
                        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = TEXT_CHARCOAL
                            textSize = 11.5f * d
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        }
                        canvas.drawText("AC", cx, cy + 4f * d, tp)
                    }
                    9 -> { // כף יד קווית
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
    // SCREEN 5: Color Theme Customizer (התאמת צבעים אינטראקטיבית מלאה!)
    // מופיע אוטומטית במסך ההגדרות כאשר "עיצוב מינימליסטי" נבחר
    // =========================================================================
    fun createColorThemeCustomizerSection(activity: MainActivity): View {
        val root = LinearLayout(activity).apply {
            tag = "MINIMALIST_CUSTOMIZER_IGNORE"
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = activity.dp(8f); bottomMargin = activity.dp(14f) }
        }

        // 1. שורת טאבים לבחירה: "צבע נושא מותאם" (#FFA48E) מול "צבע רקע מותאם" (#F1EDE9)
        val tabRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(12f) }
        }

        lateinit var tabAccentBtn: TextView
        lateinit var tabBgBtn: TextView
        lateinit var hexLabelTv: TextView
        lateinit var previewCircleView: View
        lateinit var colorWheelView: Flat2DColorWheelView
        lateinit var brightnessSlider: MinimalistLineSliderView
        lateinit var opacitySlider: MinimalistLineSliderView

        fun currentEditedColor(): Int {
            return if (isEditingBackgroundTab) customBgColor else customAccentColor
        }

        fun updateLivePreviewAndApply(pushToScreen: Boolean) {
            val baseCol = currentEditedColor()
            val finalCol = applyBrightnessAndOpacity(baseCol, customBrightness, if (isEditingBackgroundTab) 1.0f else customOpacity)
            hexLabelTv.text = String.format(Locale.US, "#%08X", finalCol)
            previewCircleView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(finalCol)
                setStroke(activity.dp(2f), Color.WHITE)
            }
            if (isEditingBackgroundTab) {
                useCustomBgTint = true
                activity.bgColor = finalCol
                if (pushToScreen) {
                    activity.mainContent?.setBackgroundColor(getBackgroundColor(activity))
                    activity.window.statusBarColor = getBackgroundColor(activity)
                    activity.window.navigationBarColor = getBackgroundColor(activity)
                }
            } else {
                useCustomAccentTint = true
                activity.primaryColor = finalCol
                if (pushToScreen) {
                    activity.updateDrawerSelection()
                }
            }
            saveMinimalistPrefs(activity.prefs)
            activity.savePreferences()
        }

        fun refreshTabsUi() {
            tabAccentBtn.background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(if (!isEditingBackgroundTab) ACCENT_TERRACOTTA else NEUTRAL_SECONDARY_BG)
            }
            tabBgBtn.background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(if (isEditingBackgroundTab) ACCENT_TERRACOTTA else NEUTRAL_SECONDARY_BG)
            }
            colorWheelView.setSelectedColor(currentEditedColor())
            updateLivePreviewAndApply(false)
        }

        tabAccentBtn = TextView(activity).apply {
            text = "🎨 צבע נושא מותאם"
            setTextColor(TEXT_CHARCOAL)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(activity.dp(14f), activity.dp(10f), activity.dp(14f), activity.dp(10f))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = activity.dp(5f)
            }
            setOnClickListener {
                activity.feedback(it)
                isEditingBackgroundTab = false
                refreshTabsUi()
            }
        }

        tabBgBtn = TextView(activity).apply {
            text = "❄️ צבע רקע מותאם"
            setTextColor(TEXT_CHARCOAL)
            textSize = 13.5f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(activity.dp(14f), activity.dp(10f), activity.dp(14f), activity.dp(10f))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = activity.dp(5f)
            }
            setOnClickListener {
                activity.feedback(it)
                isEditingBackgroundTab = true
                refreshTabsUi()
            }
        }

        // בסדר RTL: ימין הוא צבע נושא, שמאל הוא צבע רקע (או להפך לפי התמונה)
        tabRow.addView(tabAccentBtn)
        tabRow.addView(tabBgBtn)
        root.addView(tabRow)

        // 2. הכרטיס הצהוב החם הראשי (#FFE094) שמכיל את תצוגת ה-Hex, גלגל הצבעים 2D והסליידרים
        val yellowCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20f), activity.dp(18f), activity.dp(20f), activity.dp(20f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(ACCENT_BUTTER_YELLOW)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // שורת תצוגה מקדימה: עיגול צבע עם מסגרת לבנה + קוד Hex (כמו #FFFF9F0A בתמונה)
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
            text = "#FFFF9F0A"
            setTextColor(TEXT_CHARCOAL)
            textSize = 14.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        hexRow.addView(previewCircleView)
        hexRow.addView(hexLabelTv)
        yellowCard.addView(hexRow)

        // גלגל הצבעים השטוח 2D במרכז הכרטיס
        colorWheelView = Flat2DColorWheelView(activity) { pickedColor ->
            if (isEditingBackgroundTab) {
                customBgColor = pickedColor
            } else {
                customAccentColor = pickedColor
            }
            updateLivePreviewAndApply(true)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(220f)
            ).apply { bottomMargin = activity.dp(14f) }
        }
        yellowCard.addView(colorWheelView)

        // סליידר 1: בהירות (Brightness) עם ידית תכלת #B1D4F9
        val brightLabel = TextView(activity).apply {
            text = "☀️ בהירות"
            setTextColor(TEXT_CHARCOAL)
            textSize = 13.5f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(2f), 0, activity.dp(2f))
        }
        brightnessSlider = MinimalistLineSliderView(activity, customBrightness) { newVal ->
            customBrightness = newVal.coerceIn(0.25f, 1.0f)
            updateLivePreviewAndApply(true)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(34f)
            ).apply { bottomMargin = activity.dp(8f) }
        }
        yellowCard.addView(brightLabel)
        yellowCard.addView(brightnessSlider)

        // סליידר 2: שקיפות (Opacity) עם ידית תכלת #B1D4F9
        val opacityLabel = TextView(activity).apply {
            text = "💧 שקיפות"
            setTextColor(TEXT_CHARCOAL)
            textSize = 13.5f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(2f), 0, activity.dp(2f))
        }
        opacitySlider = MinimalistLineSliderView(activity, customOpacity) { newVal ->
            customOpacity = newVal.coerceIn(0.25f, 1.0f)
            updateLivePreviewAndApply(true)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(34f)
            ).apply { bottomMargin = activity.dp(10f) }
        }
        yellowCard.addView(opacityLabel)
        yellowCard.addView(opacitySlider)

        // כפתור איפוס לפלטת הפסטל המקורית של העיצוב המינימליסטי
        val resetPastelBtn = TextView(activity).apply {
            text = "↺ איפוס לפלטת הפסטל המקורית (#FCF9F5 / #FFA48E)"
            setTextColor(TEXT_CHARCOAL)
            textSize = 13.5f
            gravity = Gravity.CENTER
            setPadding(activity.dp(14f), activity.dp(10f), activity.dp(14f), activity.dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(CARD_WHITE)
            }
            setOnClickListener {
                activity.feedback(it)
                useCustomAccentTint = false
                useCustomBgTint = false
                customAccentColor = ACCENT_TERRACOTTA
                customBgColor = BG_CREAM
                customBrightness = 1.0f
                customOpacity = 1.0f
                brightnessSlider.progress = 1.0f
                opacitySlider.progress = 1.0f
                saveMinimalistPrefs(activity.prefs)
                activity.applyTheme()
            }
        }
        yellowCard.addView(resetPastelBtn)

        root.addView(yellowCard)
        refreshTabsUi()
        return root
    }

    // =========================================================================
    // חבילת האייקונים הקוויים המינימליסטיים (2px Stroke Outline Icons - ללא רקע קופסה!)
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
                    0, 301 -> { // מחשבון קווי (כמו ב-Screen 1 בתמונה)
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.82f, cx + r * 0.68f, cy + r * 0.82f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawRect(cx - r * 0.42f, cy - r * 0.55f, cx + r * 0.42f, cy - r * 0.22f, paint)
                        for (gx in listOf(-0.32f, 0f, 0.32f)) {
                            for (gy in listOf(0.12f, 0.45f)) {
                                canvas.drawCircle(cx + r * gx, cy + r * gy, r * 0.08f, dotPaint)
                            }
                        }
                    }
                    1, 44, 309 -> { // מבחנה / מדעי
                        canvas.drawRoundRect(RectF(cx - r * 0.28f, cy - r * 0.78f, cx + r * 0.28f, cy + r * 0.75f), r * 0.28f, r * 0.28f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.78f, cx + r * 0.42f, cy - r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.28f, cy + r * 0.1f, cx + r * 0.28f, cy + r * 0.1f, paint)
                    }
                    8 -> { // אחוז %
                        canvas.drawLine(cx - r * 0.58f, cy + r * 0.58f, cx + r * 0.58f, cy - r * 0.58f, paint)
                        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.38f, r * 0.2f, paint)
                        canvas.drawCircle(cx + r * 0.38f, cy + r * 0.38f, r * 0.2f, paint)
                    }
                    15 -> { // שורש √ (משוואות ריבועיות)
                        val p = Path().apply {
                            moveTo(cx - r * 0.8f, cy + r * 0.1f)
                            lineTo(cx - r * 0.45f, cy + r * 0.1f)
                            lineTo(cx - r * 0.15f, cy + r * 0.7f)
                            lineTo(cx + r * 0.35f, cy - r * 0.65f)
                            lineTo(cx + r * 0.8f, cy - r * 0.65f)
                        }
                        canvas.drawPath(p, paint)
                    }
                    16 -> { // שברים 1/2
                        canvas.drawLine(cx - r * 0.6f, cy + r * 0.6f, cx + r * 0.6f, cy - r * 0.6f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.65f, cx - r * 0.35f, cy - r * 0.15f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.25f, cx + r * 0.58f, cy + r * 0.25f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.6f, cx + r * 0.58f, cy + r * 0.6f, paint)
                    }
                    7, 28 -> { // מתכנתים < >
                        canvas.drawLine(cx - r * 0.25f, cy - r * 0.45f, cx - r * 0.75f, cy, paint)
                        canvas.drawLine(cx - r * 0.75f, cy, cx - r * 0.25f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy - r * 0.45f, cx + r * 0.75f, cy, paint)
                        canvas.drawLine(cx + r * 0.75f, cy, cx + r * 0.25f, cy + r * 0.45f, paint)
                    }
                    13, 64, 302 -> { // מטבעות / שטר כסף קווי
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.48f, cx + r * 0.82f, cy + r * 0.48f), r * 0.12f, r * 0.12f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                    }
                    19, 57 -> { // גלגל שיניים קווי דק (Settings)
                        canvas.drawCircle(cx, cy, r * 0.34f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.44f).toFloat(),
                                (cy + sin(a) * r * 0.44f).toFloat(),
                                (cx + cos(a) * r * 0.78f).toFloat(),
                                (cy + sin(a) * r * 0.78f).toFloat(),
                                paint
                            )
                        }
                    }
                    56, 102, 103, 300 -> { // כוכב 4-קודקודים קווי (✧) בראש המסך
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
                    100 -> { // חלון צף PiP קווי בראש המסך
                        canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.80f, cx + r * 0.85f, cy + r * 0.80f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawLine(cx - r * 0.85f, cy - r * 0.35f, cx + r * 0.85f, cy - r * 0.35f, paint)
                        canvas.drawRoundRect(RectF(cx + r * 0.08f, cy + r * 0.05f, cx + r * 0.58f, cy + r * 0.52f), r * 0.08f, r * 0.08f, dotPaint)
                    }
                    101 -> { // שעון היסטוריה קווי בראש המסך
                        canvas.drawCircle(cx, cy, r * 0.86f, paint)
                        canvas.drawLine(cx, cy - r * 0.48f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.36f, cy + r * 0.24f, paint)
                    }
                    else -> {
                        // ציור קווי נקי של סמל הקטגוריה ללא ריבוע רקע
                        canvas.drawCircle(cx, cy, r * 0.72f, paint)
                        canvas.drawLine(cx - r * 0.32f, cy, cx + r * 0.32f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.32f, cx, cy + r * 0.32f, paint)
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
 

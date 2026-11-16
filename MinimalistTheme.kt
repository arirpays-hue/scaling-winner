package com.example.calculator

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

object MinimalistTheme {

    const val MINIMALIST_THEME_NAME = "עיצוב מינימליסטי"

    // פלטת הצבעים המדויקת מתמונות הקנבה של "עיצוב מינימליסטי"
    val BG_CREAM = Color.parseColor("#FAF7F2")
    val DRAWER_CREAM = Color.parseColor("#F4EFE6")
    val TEXT_DARK = Color.parseColor("#2B2825")
    val TEXT_MUTED = Color.parseColor("#6E685F")
    val HEADER_PISTACHIO_CARD = Color.parseColor("#EAE6CE")
    val HEADER_STAR_PILL = Color.parseColor("#D8D4BC")
    val COMPASS_MINT_CARD = Color.parseColor("#DFEFE4")
    val COMPASS_DIAL_RING_BG = Color.parseColor("#D0DDD4")
    val ALERT_PINK_PILL = Color.parseColor("#EDD0CC")
    val ALERT_RED_TEXT = Color.parseColor("#B9382C")

    // צבעי מקשי המחשבון הפסטליים
    val PILL_MC_BLUE = Color.parseColor("#CBE4F0")
    val PILL_MR_MINT = Color.parseColor("#B2E2CE")
    val PILL_MPLUS_LIME = Color.parseColor("#D4E8B0")
    val PILL_MMINUS_YELLOW = Color.parseColor("#FBE89C")
    val PILL_AC_PERIWINKLE = Color.parseColor("#A9C7EB")
    val PILL_CMD_LAVENDER = Color.parseColor("#DCE8F7")
    val PILL_NUM_WHITE = Color.parseColor("#FFFFFF")

    // עמודת הפעולות בגווני אפרסק-קורל חמים
    val OP_DIV_PEACH = Color.parseColor("#F9B487")
    val OP_MUL_CORAL = Color.parseColor("#F5967B")
    val OP_SUB_SALMON = Color.parseColor("#F69E7A")
    val OP_ADD_TERRACOTTA = Color.parseColor("#F48E78")
    val OP_EQ_ROSE = Color.parseColor("#F48484")

    // רכיבי טפסים ומחשבונים פנימיים
    val SELECTOR_GREY_PILL = Color.parseColor("#E6E6E6")
    val SWAP_YELLOW_PILL = Color.parseColor("#F8DF8C")
    val INPUT_ICY_BLUE = Color.parseColor("#EBF3FA")
    val INPUT_ICY_BORDER = Color.parseColor("#9EC5E8")
    val INPUT_PERIWINKLE_FILL = Color.parseColor("#BDD4F2")
    val ACTION_CORAL_PRIMARY = Color.parseColor("#F69E7A")
    val FAB_MINT_GREEN = Color.parseColor("#9ED9C8")

    // הגדרות התאמה אישית של ערכת הנושא המינימליסטית
    var pillCornerPt: Float = 28f
    var shadowSoftness: Int = 22
    var useCustomAccentTint: Boolean = false
    var warmPaperTintMode: Int = 0 // 0 = קרם חם (#FAF7F2), 1 = שלג נקי (#F7F9FC), 2 = מנטה בהיר (#F2F8F5)

    fun loadMinimalistPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        pillCornerPt = prefs.getFloat("min_pill_corner", 28f)
        shadowSoftness = prefs.getInt("min_shadow_softness", 22)
        useCustomAccentTint = prefs.getBoolean("min_use_custom_accent", false)
        warmPaperTintMode = prefs.getInt("min_paper_tint_mode", 0)
    }

    fun saveMinimalistPrefs(prefs: SharedPreferences?) {
        prefs?.edit()
            ?.putFloat("min_pill_corner", pillCornerPt)
            ?.putInt("min_shadow_softness", shadowSoftness)
            ?.putBoolean("min_use_custom_accent", useCustomAccentTint)
            ?.putInt("min_paper_tint_mode", warmPaperTintMode)
            ?.apply()
    }

    fun getBackgroundColor(): Int {
        return when (warmPaperTintMode) {
            1 -> Color.parseColor("#F6F8FB")
            2 -> Color.parseColor("#F2F7F4")
            else -> BG_CREAM
        }
    }

    fun getPrimaryActionColor(activity: MainActivity): Int {
        return if (useCustomAccentTint) {
            // מרכך את הצבע שבחר המשתמש לגוון פסטל נעים לעין
            val r = (Color.red(activity.primaryColor) + 255) / 2
            val g = (Color.green(activity.primaryColor) + 245) / 2
            val b = (Color.blue(activity.primaryColor) + 235) / 2
            Color.rgb(r.coerceIn(140, 250), g.coerceIn(130, 240), b.coerceIn(120, 240))
        } else {
            ACTION_CORAL_PRIMARY
        }
    }

    // מעצב כפתור קפסולה במחשבון הרגיל והמדעי בהתאם לתמונה (MC/MR/M+/M-/AC/מספרים/פעולות)
    fun createMinimalistKeypadButtonDrawable(
        activity: MainActivity,
        symbol: String,
        isPressed: Boolean = false
    ): Drawable {
        val radius = activity.dp(pillCornerPt).toFloat()
        val baseFill = when (symbol) {
            "MC" -> PILL_MC_BLUE
            "MR" -> PILL_MR_MINT
            "M+" -> PILL_MPLUS_LIME
            "M-" -> PILL_MMINUS_YELLOW
            "AC" -> PILL_AC_PERIWINKLE
            "⌫", "%" -> PILL_CMD_LAVENDER
            "÷" -> if (useCustomAccentTint) getPrimaryActionColor(activity) else OP_DIV_PEACH
            "−", "-" -> if (useCustomAccentTint) getPrimaryActionColor(activity) else OP_SUB_SALMON
            "×" -> if (useCustomAccentTint) getPrimaryActionColor(activity) else OP_MUL_CORAL
            "+" -> if (useCustomAccentTint) getPrimaryActionColor(activity) else OP_ADD_TERRACOTTA
            "=" -> if (useCustomAccentTint) getPrimaryActionColor(activity) else OP_EQ_ROSE
            else -> PILL_NUM_WHITE
        }

        val fillColor = if (isPressed) Utils.lightenColor(baseFill, -0.07f) else baseFill
        val shadowAlpha = (if (isPressed) shadowSoftness / 2 else shadowSoftness).coerceIn(0, 65)

        val shadowLayer = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.argb(shadowAlpha, 60, 50, 40))
        }

        val pillBody = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillColor)
            setStroke(activity.dp(1f), Color.argb(28, 0, 0, 0))
        }

        val layers = LayerDrawable(arrayOf(shadowLayer, pillBody)).apply {
            val bottomOffset = if (isPressed) activity.dp(1f) else activity.dp(2.5f)
            setLayerInset(0, activity.dp(1f), activity.dp(2f), activity.dp(1f), 0)
            setLayerInset(1, 0, 0, 0, bottomOffset)
        }

        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#22000000")), layers, null)
    }

    // מעצב כרטיסי מידע, כרטיסי Pro ורקע קנבס בסגנון קנבה מינימליסטי
    fun createMinimalistCardDrawable(
        activity: MainActivity,
        cornerDp: Float = 22f,
        cardStyle: Int = 0 // 0 = לבן נקי, 1 = פיסטוק-בז' לכותרת, 2 = מנטה למצפן/קנבס, 3 = תכלת קלט
    ): Drawable {
        val radius = activity.dp(cornerDp).toFloat()
        val fillCol = when (cardStyle) {
            1 -> HEADER_PISTACHIO_CARD
            2 -> COMPASS_MINT_CARD
            3 -> INPUT_PERIWINKLE_FILL
            else -> Color.WHITE
        }
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillCol)
            setStroke(activity.dp(1f), Color.argb(22, 45, 42, 38))
        }
    }

    // מעצב תיבות קלט (EditText) בסגנון הקנבה מהתמונות (מסגרת תכלת עדינה או מילוי כחול-פסטל/לבן)
    fun createMinimalistInputDrawable(activity: MainActivity, variant: Int = 0): Drawable {
        val radius = activity.dp(18f).toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            when (variant % 3) {
                0 -> { // תיבת קלט עם רקע תכלת בהיר ומסגרת תכלת כמו במטבעות לייב
                    setColor(INPUT_ICY_BLUE)
                    setStroke(activity.dp(1.6f), INPUT_ICY_BORDER)
                }
                1 -> { // תיבת קלט לבנה נקייה כמו השדה הראשון במחשבון מס מכירות
                    setColor(Color.WHITE)
                    setStroke(activity.dp(1.2f), Color.parseColor("#E5E0D8"))
                }
                else -> { // תיבת קלט במילוי כחול-פסטל כמו שדה ה-17 במחשבון מס מכירות
                    setColor(INPUT_PERIWINKLE_FILL)
                    setStroke(activity.dp(1.2f), Color.parseColor("#A5C2E8"))
                }
            }
        }
    }

    // מעצב כפתורי פעולה (כפתור ראשי אפרסק-קורל, כפתור בחירה אפור-חם, או כפתור החלפת כיוון צהוב קומפקטי)
    fun createMinimalistActionButtonDrawable(
        activity: MainActivity,
        isPrimary: Boolean,
        isYellowSwapPill: Boolean = false
    ): Drawable {
        val radius = activity.dp(pillCornerPt).toFloat()
        val bgCol = when {
            isYellowSwapPill -> SWAP_YELLOW_PILL
            isPrimary -> getPrimaryActionColor(activity)
            else -> SELECTOR_GREY_PILL
        }
        val pill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgCol)
        }
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#22000000")), pill, null)
    }

    fun createMinimalistActiveDrawerCapsule(activity: MainActivity): Drawable {
        val radius = activity.dp(16f).toFloat()
        return GradientDrawable().apply {
            cornerRadius = radius
            setColor(HEADER_PISTACHIO_CARD)
            setStroke(activity.dp(1.2f), Color.parseColor("#D0C9A8"))
        }
    }

    // סורק ומעצב אוטומטית כל מסך מחשבון כדי שיתאים ב-100% לעיצוב הקנבה בתמונות
    fun applyMinimalistStylingToHierarchy(root: View?, activity: MainActivity) {
        if (root == null || !activity.isMinimalistMode) return
        var inputCounter = 0

        fun traverse(v: View) {
            when (v) {
                is EditText -> {
                    val hintStr = v.hint?.toString() ?: ""
                    val textStr = v.text?.toString() ?: ""
                    val variant = when {
                        hintStr.contains("הזן כמות") || hintStr.contains("נוסחה") -> 0
                        textStr == "17" || textStr == "18" || inputCounter % 2 == 1 -> 2
                        else -> 1
                    }
                    inputCounter++
                    v.background = createMinimalistInputDrawable(activity, variant)
                    v.setTextColor(TEXT_DARK)
                    v.setHintTextColor(TEXT_MUTED)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        v.elevation = activity.dp(1.5f).toFloat()
                    }
                }
                is TextView -> {
                    val txt = v.text?.toString() ?: ""
                    // זיהוי כפתור "החלף כיוון" והפיכתו לקפסולה צהובה קומפקטית במרכז המסך בדיוק כמו בתמונה!
                    if (txt.contains("החלף כיוון")) {
                        v.background = createMinimalistActionButtonDrawable(activity, isPrimary = false, isYellowSwapPill = true)
                        v.setTextColor(TEXT_DARK)
                        v.text = "⇅ החלף כיוון"
                        v.setPadding(activity.dp(22f), activity.dp(10f), activity.dp(22f), activity.dp(10f))
                        val lp = v.layoutParams
                        if (lp is LinearLayout.LayoutParams) {
                            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
                            lp.gravity = Gravity.CENTER_HORIZONTAL
                            v.layoutParams = lp
                        }
                    } else if (v.isClickable && v.background != null && v.tag != "PRO_STAR_CHIP") {
                        // כפתורים לחיצים רגילים או ראשיים
                        val isPrimaryBtn = txt.startsWith("חשב") || txt.startsWith("פתור") ||
                                txt.startsWith("נתח") || txt.startsWith("פענח") ||
                                txt.startsWith("צור סיסמה") || txt.startsWith("התחל") ||
                                txt.startsWith("▶️") || txt.startsWith("סיום")
                        v.background = createMinimalistActionButtonDrawable(activity, isPrimaryBtn, false)
                        v.setTextColor(TEXT_DARK)
                    } else {
                        // כותרות וטקסטים רגילים – המרה מלבן לפחם כהה לקריאות מושלמת על הרקע הבהיר
                        val curCol = v.currentTextColor
                        if (curCol == Color.WHITE || curCol == Color.LTGRAY || curCol == Color.parseColor("#B0BEC5")) {
                            v.setTextColor(if (curCol == Color.WHITE) TEXT_DARK else TEXT_MUTED)
                        }
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

    // חבילת האייקונים המינימליסטית (Minimalist Fine Line-Art Icons) כולל כוכב 4-הקודקודים מהתמונה
    fun createMinimalistVectorIcon(
        context: Context,
        iconCode: Int,
        tintColor: Int = TEXT_DARK,
        sizeDp: Float = 22f
    ): Drawable {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val d = context.resources.displayMetrics.density
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.75f * d
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
                    // סמל הכוכב המינימליסטי בראש המסך (כוכב 4-קודקודים קעור עם נקודה במרכז בדיוק כמו בתמונה!)
                    56, 102, 103, 300 -> {
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
                                color = Color.argb(75, 246, 158, 122)
                                style = Paint.Style.FILL
                            })
                        }
                        canvas.drawPath(star4, paint)
                        canvas.drawCircle(cx, cy, r * 0.13f, dotPaint)
                    }
                    // סמל חלון צף PiP בראש המסך (ריבוע עם פס עליון וריבוע פנימי מלא בפינה)
                    100 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.80f, cx + r * 0.85f, cy + r * 0.80f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawLine(cx - r * 0.85f, cy - r * 0.35f, cx + r * 0.85f, cy - r * 0.35f, paint)
                        canvas.drawRoundRect(RectF(cx + r * 0.08f, cy + r * 0.05f, cx + r * 0.58f, cy + r * 0.52f), r * 0.08f, r * 0.08f, dotPaint)
                    }
                    // סמל שעון היסטוריה בראש המסך (מעגל נקי עם שני מחוגים)
                    101 -> {
                        canvas.drawCircle(cx, cy, r * 0.86f, paint)
                        canvas.drawLine(cx, cy - r * 0.48f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.36f, cy + r * 0.24f, paint)
                    }
                    else -> {
                        // עבור שאר הקטגוריות בתפריט הצד: שימוש בסמלי הקו הנקיים בצבע פחם כהה ללא רקע כהה
                        val sf = IosGlassTheme.createSfSymbolDrawable(context, iconCode, tintColor, sizeDp)
                        sf.bounds = bounds
                        sf.draw(canvas)
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
 

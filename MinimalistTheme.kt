package com.example.calculator

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

object MinimalistTheme {

    const val MINIMALIST_THEME_NAME = "עיצוב מינימליסטי"

    // גווני בסיס חמים ומינימליסטיים התואמים לעיצוב הקנבה
    val CREAM_BG_DEFAULT = Color.parseColor("#FBF8F1")
    val DRAWER_CREAM_BG = Color.parseColor("#F6F2E9")
    val CHARCOAL_INK = Color.parseColor("#322E27")
    val MUTED_STONE_INK = Color.parseColor("#6E685F")
    val HINT_STONE_INK = Color.parseColor("#8E877D")
    val MINT_CANVAS_BG = Color.parseColor("#E1ECE6")
    val OLIVE_CARD_BG = Color.parseColor("#ECE6C2")
    val OLIVE_BADGE_BG = Color.parseColor("#D8CF9E")
    val OLIVE_TITLE_INK = Color.parseColor("#3A3520")
    val OLIVE_SUBTITLE_INK = Color.parseColor("#635B36")
    val ROSE_ALERT_BG = Color.parseColor("#F2C8C6")
    val ROSE_ALERT_INK = Color.parseColor("#6E2B28")
    val STONE_PICKER_BG = Color.parseColor("#E6E3DC")
    val BUTTER_SWAP_BG = Color.parseColor("#F3E08B")
    val PEACH_ACTION_BG = Color.parseColor("#F59E76")
    val SKY_INPUT_BG = Color.parseColor("#CFE0F5")
    val SKY_INPUT_STROKE = Color.parseColor("#75A8DC")
    val MINT_SQUARE_ACCENT = Color.parseColor("#A6DEC8")

    // פרמטרים להתאמה אישית מלאה בהגדרות
    var paperWarmth: Int = 50          // 0 = לבן צחור, 50 = שמנת חמה (#FBF8F1), 100 = חול רך
    var pastelVibrancy: Int = 100      // 55% עד 130% עוצמת צבעי פסטל
    var minimalistCornerPt: Float = 32f // רדיוס גלולה (Pill) ברירת מחדל 32pt
    var hairlineBorderDp: Float = 1.0f // עובי קו מתאר עדין
    var pastelPaletteIndex: Int = 0    // 0 = קלאסי חם (אפרסק ומנטה), 1 = מנטה ולבנדר, 2 = אפרסק ורוד, 3 = תכלת סקנדינבי
    var showSubtleBorders: Boolean = true

    val paletteNames = listOf(
        "קלאסי חם (שמנת, אפרסק ומנטה)",
        "מנטה ולבנדר רגוע (Mint & Lavender)",
        "אפרסק ופודרה (Peach & Rose)",
        "תכלת סקנדינבי (Nordic Sky)"
    )

    fun loadMinimalistPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        paperWarmth = prefs.getInt("min_paperWarmth", 50)
        pastelVibrancy = prefs.getInt("min_pastelVibrancy", 100)
        minimalistCornerPt = prefs.getFloat("min_cornerPt", 32f)
        hairlineBorderDp = prefs.getFloat("min_hairlineDp", 1.0f)
        pastelPaletteIndex = prefs.getInt("min_paletteIdx", 0).coerceIn(0, 3)
        showSubtleBorders = prefs.getBoolean("min_showBorders", true)
    }

    fun saveMinimalistPrefs(prefs: SharedPreferences?) {
        prefs?.edit()
            ?.putInt("min_paperWarmth", paperWarmth)
            ?.putInt("min_pastelVibrancy", pastelVibrancy)
            ?.putFloat("min_cornerPt", minimalistCornerPt)
            ?.putFloat("min_hairlineDp", hairlineBorderDp)
            ?.putInt("min_paletteIdx", pastelPaletteIndex)
            ?.putBoolean("min_showBorders", showSubtleBorders)
            ?.apply()
    }

    private fun adjustPastelVibrancy(baseColor: Int): Int {
        val factor = (pastelVibrancy / 100f).coerceIn(0.55f, 1.30f)
        if (kotlin.math.abs(factor - 1f) < 0.02f) return baseColor
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        hsv[1] = (hsv[1] * factor).coerceIn(0.05f, 0.72f)
        return Color.HSVToColor(Color.alpha(baseColor), hsv)
    }

    fun getSurfaceBg(): Int {
        val t = (paperWarmth.coerceIn(0, 100)) / 100f
        val r = (255 - (12 * t)).toInt()
        val g = (255 - (14 * t)).toInt()
        val b = (255 - (28 * t)).toInt()
        return Color.rgb(r, g, b)
    }

    fun getDrawerBg(): Int {
        val surf = getSurfaceBg()
        return Color.rgb(
            (Color.red(surf) - 6).coerceAtLeast(225),
            (Color.green(surf) - 7).coerceAtLeast(220),
            (Color.blue(surf) - 10).coerceAtLeast(210)
        )
    }

    fun getPrimaryText(): Int = CHARCOAL_INK
    fun getSecondaryText(): Int = MUTED_STONE_INK
    fun getHintText(): Int = HINT_STONE_INK

    fun getCanvasBg(): Int {
        val base = when (pastelPaletteIndex) {
            1 -> Color.parseColor("#E3E7F2") // לבנדר רך
            2 -> Color.parseColor("#F3E5E4") // פודרה רך
            3 -> Color.parseColor("#DFECF2") // תכלת קרח רך
            else -> MINT_CANVAS_BG           // מנטה רך (#E1ECE6)
        }
        return adjustPastelVibrancy(base)
    }

    fun getCanvasInk(): Int = Color.parseColor("#2E3732")

    fun getHeaderCardBg(): Int {
        val base = when (pastelPaletteIndex) {
            1 -> Color.parseColor("#E2E0F0")
            2 -> Color.parseColor("#F2DFD8")
            3 -> Color.parseColor("#DCE8F2")
            else -> OLIVE_CARD_BG
        }
        return adjustPastelVibrancy(base)
    }

    fun getHeaderBadgeBg(): Int {
        val base = when (pastelPaletteIndex) {
            1 -> Color.parseColor("#CBC7E6")
            2 -> Color.parseColor("#E5C5B8")
            3 -> Color.parseColor("#C2D8EB")
            else -> OLIVE_BADGE_BG
        }
        return adjustPastelVibrancy(base)
    }

    fun getHeaderTitleText(): Int = OLIVE_TITLE_INK
    fun getHeaderSubtitleText(): Int = OLIVE_SUBTITLE_INK

    fun getAlertBannerBg(): Int = adjustPastelVibrancy(ROSE_ALERT_BG)
    fun getAlertBannerText(): Int = ROSE_ALERT_INK

    fun getPickerPillBg(): Int = STONE_PICKER_BG
    fun getSwapPillBg(): Int = adjustPastelVibrancy(BUTTER_SWAP_BG)
    fun getPrimaryActionBg(): Int {
        val base = when (pastelPaletteIndex) {
            1 -> Color.parseColor("#B5C6EC")
            2 -> Color.parseColor("#F2959E")
            3 -> Color.parseColor("#9DC6E8")
            else -> PEACH_ACTION_BG
        }
        return adjustPastelVibrancy(base)
    }

    fun getInputBg(isHighlighted: Boolean = false): Int {
        return if (isHighlighted) adjustPastelVibrancy(SKY_INPUT_BG) else Color.parseColor("#F2EFE9")
    }

    fun getMintSquareBg(): Int = adjustPastelVibrancy(MINT_SQUARE_ACCENT)

    fun styleMinimalistSymbol(tv: TextView, symbol: String) {
        tv.setTextColor(CHARCOAL_INK)
        tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        val displaySym = when (symbol) {
            "-" -> "−"
            else -> symbol
        }
        if (displaySym in listOf("sin", "cos", "tan", "log", "ln", "x²", "x³", "x^y", "e")) {
            tv.text = SpannableString(displaySym).apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, displaySym.length, 0)
            }
        } else {
            tv.text = displaySym
        }
    }

    private fun getKeypadPillColor(symbol: String): Int {
        val raw = when (symbol) {
            // שורת זיכרון (MC, MR, M+, M-)
            "MC" -> Color.parseColor("#D2E4F3")
            "MR" -> Color.parseColor("#BCE3D2")
            "M+" -> Color.parseColor("#DCE8B8")
            "M-" -> Color.parseColor("#FCE9A6")
            // פעולות עריכה ופקודות
            "AC", "⌫", "%" -> Color.parseColor("#C2D8F2")
            // עמודת פעולות חשבון מדורגת (אפרסק -> קורל -> ורוד סלמון)
            "÷" -> when (pastelPaletteIndex) {
                1 -> Color.parseColor("#C8D4F5")
                2 -> Color.parseColor("#F8C0A8")
                3 -> Color.parseColor("#B8DBF2")
                else -> Color.parseColor("#F7B686")
            }
            "×" -> when (pastelPaletteIndex) {
                1 -> Color.parseColor("#D2C8F2")
                2 -> Color.parseColor("#F6A89E")
                3 -> Color.parseColor("#A8CEF0")
                else -> Color.parseColor("#F48A78")
            }
            "−", "-" -> when (pastelPaletteIndex) {
                1 -> Color.parseColor("#C2DDF0")
                2 -> Color.parseColor("#F7B29C")
                3 -> Color.parseColor("#B0D5F2")
                else -> Color.parseColor("#F59E72")
            }
            "+" -> when (pastelPaletteIndex) {
                1 -> Color.parseColor("#BCE3D8")
                2 -> Color.parseColor("#F5989E")
                3 -> Color.parseColor("#9EC4EC")
                else -> Color.parseColor("#F49276")
            }
            "=" -> when (pastelPaletteIndex) {
                1 -> Color.parseColor("#B6C4EE")
                2 -> Color.parseColor("#F28292")
                3 -> Color.parseColor("#90BCE8")
                else -> Color.parseColor("#F27983")
            }
            // מקשים מדעיים
            "sin", "cos", "tan", "log", "ln", "√", "x²", "x³", "x^y", "!", "π", "e" ->
                Color.parseColor("#E5ECE8")
            // מקשי ספרות לבנים נקיים
            else -> Color.parseColor("#FFFFFF")
        }
        return if (raw == Color.WHITE) Color.WHITE else adjustPastelVibrancy(raw)
    }

    fun createMinimalistButtonDrawable(
        activity: MainActivity,
        symbol: String,
        isPressed: Boolean = false
    ): Drawable {
        val radius = activity.dp(minimalistCornerPt).toFloat()
        val baseCol = getKeypadPillColor(symbol)
        val fillCol = if (isPressed) {
            Color.rgb(
                (Color.red(baseCol) - 14).coerceAtLeast(0),
                (Color.green(baseCol) - 14).coerceAtLeast(0),
                (Color.blue(baseCol) - 14).coerceAtLeast(0)
            )
        } else {
            baseCol
        }

        val shape = GradientDrawable().apply {
            this.shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillCol)
            if (showSubtleBorders && hairlineBorderDp > 0f) {
                val strokeCol = if (baseCol == Color.WHITE) {
                    Color.parseColor("#EAE4D7")
                } else {
                    Color.argb(35, 50, 46, 39)
                }
                setStroke(activity.dp(hairlineBorderDp), strokeCol)
            }
        }
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#22322E27")), shape, null)
    }

    fun createMinimalistCardDrawable(
        activity: MainActivity,
        cornerDp: Float = 22f,
        customFillColor: Int? = null,
        customStrokeColor: Int? = null
    ): Drawable {
        val radius = activity.dp(cornerDp).toFloat()
        val fill = customFillColor ?: Color.parseColor("#F3EFE6")
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fill)
            if (customStrokeColor != null) {
                setStroke(activity.dp(1.2f), customStrokeColor)
            } else if (showSubtleBorders && hairlineBorderDp > 0f) {
                setStroke(activity.dp(hairlineBorderDp), Color.parseColor("#E4DDD0"))
            }
        }
    }

    fun createActiveDrawerCapsule(activity: MainActivity): Drawable {
        val radius = activity.dp(minimalistCornerPt.coerceAtMost(24f)).toFloat()
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(adjustPastelVibrancy(Color.parseColor("#E3DEC9")))
            if (showSubtleBorders && hairlineBorderDp > 0f) {
                setStroke(activity.dp(hairlineBorderDp), Color.parseColor("#CFC7AE"))
            }
        }
    }

    // אייקוני קו מינימליסטיים נקיים (ללא מסגרות כבדות או הילות) התואמים לעיצוב הקנבה
    fun createMinimalistIcon(
        context: Context,
        iconCode: Int,
        tintColor: Int = CHARCOAL_INK,
        sizeDp: Float = 22f
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.65f * d
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
                val r = Math.min(b.width(), b.height()) * 0.39f

                when (iconCode) {
                    // כוכב ניצוץ 4-קודקודי מינימליסטי (✦) כפי שמופיע בסרגל העליון בתמונות
                    56, 102, 103, 300 -> {
                        val sparkle = Path().apply {
                            moveTo(cx, cy - r * 0.92f)
                            quadTo(cx, cy, cx + r * 0.92f, cy)
                            quadTo(cx, cy, cx, cy + r * 0.92f)
                            quadTo(cx, cy, cx - r * 0.92f, cy)
                            quadTo(cx, cy, cx, cy - r * 0.92f)
                            close()
                        }
                        if (iconCode == 102) canvas.drawPath(sparkle, dotPaint)
                        canvas.drawPath(sparkle, paint)
                    }
                    // PiP / חלון צף מינימליסטי (מסך עם ריבוע קטן בפינה)
                    100 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.68f, cx + r * 0.85f, cy + r * 0.68f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.85f, cy - r * 0.25f, cx + r * 0.85f, cy - r * 0.25f, paint)
                        canvas.drawRect(RectF(cx + r * 0.08f, cy + r * 0.02f, cx + r * 0.55f, cy + r * 0.42f), dotPaint)
                    }
                    // היסטוריה מינימליסטית (שעון עגול נקי)
                    101 -> {
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawLine(cx, cy - r * 0.42f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.34f, cy + r * 0.22f, paint)
                    }
                    // מחשבון רגיל / מתמטיקה
                    0, 301 -> {
                        val o = r * 0.42f
                        canvas.drawLine(cx - o - r * 0.18f, cy - o, cx - o + r * 0.18f, cy - o, paint)
                        canvas.drawLine(cx - o, cy - o - r * 0.18f, cx - o, cy - o + r * 0.18f, paint)
                        canvas.drawLine(cx + o - r * 0.18f, cy - o, cx + o + r * 0.18f, cy - o, paint)
                        canvas.drawLine(cx - o - r * 0.16f, cy + o - r * 0.16f, cx - o + r * 0.16f, cy + o + r * 0.16f, paint)
                        canvas.drawLine(cx - o + r * 0.16f, cy + o - r * 0.16f, cx - o - r * 0.16f, cy + o + r * 0.16f, paint)
                        canvas.drawLine(cx + o - r * 0.18f, cy + o - r * 0.09f, cx + o + r * 0.18f, cy + o - r * 0.09f, paint)
                        canvas.drawLine(cx + o - r * 0.18f, cy + o + r * 0.09f, cx + o + r * 0.18f, cy + o + r * 0.09f, paint)
                    }
                    1, 44, 309 -> {
                        canvas.save(); canvas.rotate(30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.85f, cy - r * 0.34f, cx + r * 0.85f, cy + r * 0.34f), paint)
                        canvas.restore(); canvas.save(); canvas.rotate(-30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.85f, cy - r * 0.34f, cx + r * 0.85f, cy + r * 0.34f), paint)
                        canvas.restore()
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
                    }
                    2, 24, 64 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.65f, cy - r * 0.78f, cx + r * 0.65f, cy + r * 0.78f), r * 0.2f, r * 0.2f, paint)
                        canvas.drawLine(cx - r * 0.32f, cy - r * 0.25f, cx + r * 0.32f, cy - r * 0.25f, paint)
                        canvas.drawLine(cx - r * 0.32f, cy + r * 0.2f, cx + r * 0.18f, cy + r * 0.2f, paint)
                    }
                    3, 29, 302 -> {
                        canvas.drawCircle(cx, cy, r * 0.8f, paint)
                        canvas.drawLine(cx, cy - r * 0.42f, cx, cy + r * 0.42f, paint)
                        canvas.drawLine(cx - r * 0.28f, cy - r * 0.18f, cx + r * 0.28f, cy + r * 0.18f, paint)
                    }
                    4, 31, 303, 305 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.36f, cx + r * 0.85f, cy + r * 0.36f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.4f, cy - r * 0.36f, cx - r * 0.4f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.36f, cx, cy + r * 0.1f, paint)
                        canvas.drawLine(cx + r * 0.4f, cy - r * 0.36f, cx + r * 0.4f, cy, paint)
                    }
                    5 -> {
                        canvas.drawLine(cx, cy - r * 0.75f, cx, cy + r * 0.75f, paint)
                        canvas.drawLine(cx - r * 0.68f, cy - r * 0.32f, cx + r * 0.68f, cy - r * 0.32f, paint)
                    }
                    6, 59 -> {
                        canvas.drawArc(RectF(cx - r * 0.42f, cy + r * 0.08f, cx + r * 0.42f, cy + r * 0.84f), -45f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.2f, cy - r * 0.74f, cx - r * 0.2f, cy + r * 0.2f, paint)
                        canvas.drawLine(cx + r * 0.2f, cy - r * 0.74f, cx + r * 0.2f, cy + r * 0.2f, paint)
                    }
                    7, 28 -> {
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.45f, cx - r * 0.8f, cy, paint)
                        canvas.drawLine(cx - r * 0.8f, cy, cx - r * 0.35f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.45f, cx + r * 0.8f, cy, paint)
                        canvas.drawLine(cx + r * 0.8f, cy, cx + r * 0.35f, cy + r * 0.45f, paint)
                    }
                    8 -> {
                        canvas.drawLine(cx - r * 0.6f, cy + r * 0.6f, cx + r * 0.6f, cy - r * 0.6f, paint)
                        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.38f, r * 0.2f, paint)
                        canvas.drawCircle(cx + r * 0.38f, cy + r * 0.38f, r * 0.2f, paint)
                    }
                    11, 27, 33, 62, 304 -> {
                        val path = Path().apply {
                            moveTo(cx - r * 0.85f, cy); lineTo(cx - r * 0.35f, cy); lineTo(cx - r * 0.15f, cy - r * 0.62f)
                            lineTo(cx + r * 0.15f, cy + r * 0.62f); lineTo(cx + r * 0.35f, cy); lineTo(cx + r * 0.85f, cy)
                        }
                        canvas.drawPath(path, paint)
                    }
                    13, 26 -> {
                        canvas.drawCircle(cx, cy, r * 0.78f, paint)
                        canvas.drawOval(RectF(cx - r * 0.36f, cy - r * 0.78f, cx + r * 0.36f, cy + r * 0.78f), paint)
                        canvas.drawLine(cx - r * 0.78f, cy, cx + r * 0.78f, cy, paint)
                    }
                    14, 38, 60, 307 -> {
                        val bolt = Path().apply {
                            moveTo(cx + r * 0.15f, cy - r * 0.84f); lineTo(cx - r * 0.5f, cy + r * 0.08f); lineTo(cx + r * 0.08f, cy + r * 0.08f)
                            lineTo(cx - r * 0.15f, cy + r * 0.84f); lineTo(cx + r * 0.5f, cy - r * 0.08f); lineTo(cx - r * 0.08f, cy - r * 0.08f); close()
                        }
                        canvas.drawPath(bolt, paint)
                    }
                    17, 50, 51, 310 -> {
                        val path = Path().apply {
                            moveTo(cx - r * 0.75f, cy + r * 0.6f); lineTo(cx - r * 0.2f, cy + r * 0.12f)
                            lineTo(cx + r * 0.15f, cy + r * 0.32f); lineTo(cx + r * 0.75f, cy - r * 0.55f)
                        }
                        canvas.drawPath(path, paint)
                    }
                    19, 57 -> {
                        canvas.drawLine(cx - r * 0.75f, cy - r * 0.42f, cx + r * 0.75f, cy - r * 0.42f, paint)
                        canvas.drawCircle(cx - r * 0.2f, cy - r * 0.42f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.42f, cx + r * 0.75f, cy + r * 0.42f, paint)
                        canvas.drawCircle(cx + r * 0.25f, cy + r * 0.42f, r * 0.18f, paint)
                    }
                    32, 58, 306 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.8f, cy - r * 0.2f, cx + r * 0.8f, cy + r * 0.2f), r * 0.12f, r * 0.12f, paint)
                        canvas.drawLine(cx, cy - r * 0.2f, cx, cy - r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.38f, cy - r * 0.65f, cx + r * 0.38f, cy - r * 0.65f, paint)
                    }
                    34, 49 -> {
                        val roof = Path().apply {
                            moveTo(cx - r * 0.82f, cy - r * 0.05f); lineTo(cx, cy - r * 0.8f); lineTo(cx + r * 0.82f, cy - r * 0.05f)
                        }
                        canvas.drawPath(roof, paint)
                        canvas.drawRect(RectF(cx - r * 0.56f, cy - r * 0.05f, cx + r * 0.56f, cy + r * 0.76f), paint)
                    }
                    35, 36, 37 -> {
                        canvas.drawLine(cx - r * 0.85f, cy, cx - r * 0.5f, cy, paint)
                        canvas.drawLine(cx + r * 0.5f, cy, cx + r * 0.85f, cy, paint)
                        canvas.drawRoundRect(RectF(cx - r * 0.5f, cy - r * 0.32f, cx + r * 0.5f, cy + r * 0.32f), r * 0.14f, r * 0.14f, paint)
                    }
                    39 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.3f, cx + r * 0.85f, cy + r * 0.3f), r * 0.3f, r * 0.3f, paint)
                        canvas.drawCircle(cx, cy, r * 0.16f, dotPaint)
                    }
                    40, 308 -> {
                        canvas.drawCircle(cx, cy, r * 0.8f, paint)
                        val needle = Path().apply {
                            moveTo(cx + r * 0.44f, cy - r * 0.44f)
                            lineTo(cx + r * 0.12f, cy + r * 0.12f)
                            lineTo(cx - r * 0.44f, cy + r * 0.44f)
                            lineTo(cx - r * 0.12f, cy - r * 0.12f)
                            close()
                        }
                        canvas.drawPath(needle, paint)
                    }
                    41, 53 -> {
                        val tri = Path().apply {
                            moveTo(cx - r * 0.7f, cy + r * 0.66f); lineTo(cx + r * 0.72f, cy + r * 0.66f)
                            lineTo(cx - r * 0.7f, cy - r * 0.7f); close()
                        }
                        canvas.drawPath(tri, paint)
                    }
                    42 -> {
                        canvas.drawCircle(cx, cy + r * 0.08f, r * 0.7f, paint)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.8f, cx + r * 0.22f, cy - r * 0.8f, paint)
                        canvas.drawLine(cx, cy + r * 0.08f, cx, cy - r * 0.32f, paint)
                    }
                    43, 61 -> {
                        canvas.drawCircle(cx, cy, r * 0.36f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine((cx + cos(a) * r * 0.52f).toFloat(), (cy + sin(a) * r * 0.52f).toFloat(), (cx + cos(a) * r * 0.82f).toFloat(), (cy + sin(a) * r * 0.82f).toFloat(), paint)
                        }
                    }
                    45 -> {
                        canvas.drawCircle(cx - r * 0.55f, cy, r * 0.12f, dotPaint)
                        canvas.drawLine(cx - r * 0.2f, cy, cx + r * 0.25f, cy, paint)
                        canvas.drawCircle(cx + r * 0.55f, cy, r * 0.12f, dotPaint)
                    }
                    46 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.55f, cy - r * 0.08f, cx + r * 0.55f, cy + r * 0.75f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawArc(RectF(cx - r * 0.35f, cy - r * 0.75f, cx + r * 0.35f, cy + r * 0.08f), 180f, 180f, false, paint)
                    }
                    52 -> {
                        val bell = Path().apply {
                            moveTo(cx - r * 0.82f, cy + r * 0.6f)
                            cubicTo(cx - r * 0.34f, cy + r * 0.6f, cx - r * 0.3f, cy - r * 0.7f, cx, cy - r * 0.7f)
                            cubicTo(cx + r * 0.3f, cy - r * 0.7f, cx + r * 0.34f, cy + r * 0.6f, cx + r * 0.82f, cy + r * 0.6f)
                        }
                        canvas.drawPath(bell, paint)
                    }
                    54, 311 -> {
                        val sigma = Path().apply {
                            moveTo(cx + r * 0.58f, cy - r * 0.68f); lineTo(cx - r * 0.58f, cy - r * 0.68f)
                            lineTo(cx + r * 0.1f, cy); lineTo(cx - r * 0.58f, cy + r * 0.68f)
                            lineTo(cx + r * 0.58f, cy + r * 0.68f)
                        }
                        canvas.drawPath(sigma, paint)
                    }
                    55 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.75f, cy - r * 0.25f, cx + r * 0.75f, cy + r * 0.45f), r * 0.35f, r * 0.35f, paint)
                        canvas.drawLine(cx, cy + r * 0.72f, cx, cy - r * 0.12f, paint)
                    }
                    63 -> {
                        canvas.drawCircle(cx, cy, r * 0.42f, paint)
                        canvas.save(); canvas.rotate(-25f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.88f, cy - r * 0.2f, cx + r * 0.88f, cy + r * 0.2f), paint)
                        canvas.restore()
                    }
                    65 -> {
                        canvas.drawLine(cx - r * 0.68f, cy - r * 0.68f, cx - r * 0.42f, cy - r * 0.68f, paint)
                        canvas.drawLine(cx - r * 0.68f, cy - r * 0.68f, cx - r * 0.68f, cy + r * 0.68f, paint)
                        canvas.drawLine(cx - r * 0.68f, cy + r * 0.68f, cx - r * 0.42f, cy + r * 0.68f, paint)
                        canvas.drawLine(cx + r * 0.68f, cy - r * 0.68f, cx + r * 0.42f, cy - r * 0.68f, paint)
                        canvas.drawLine(cx + r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f, paint)
                        canvas.drawLine(cx + r * 0.68f, cy + r * 0.68f, cx + r * 0.42f, cy + r * 0.68f, paint)
                    }
                    else -> {
                        canvas.drawCircle(cx, cy, r * 0.68f, paint)
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
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

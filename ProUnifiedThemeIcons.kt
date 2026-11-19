package com.example.calculator

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import kotlin.math.*

// =========================================================================
// מנוע סמלים וקטוריים אישיים בקנבס לכל 4 ערכות הנושא
// מכסה את כל 14 שורות ההגדרות + ממיר צבעים (73) + כלים 81..99
// =========================================================================
object ProUnifiedThemeIcons {

    private fun drawThemeBackdrop(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        d: Float,
        tintColor: Int,
        isMinimalist: Boolean,
        isGlass: Boolean,
        isAurora: Boolean,
        pastelTone: Int
    ) {
        val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        when {
            isMinimalist -> {
                val basePastel = when (pastelTone % 4) {
                    0 -> MinimalistTheme.ACCENT_PASTEL_BLUE
                    1 -> MinimalistTheme.ACCENT_MUTED_GREEN
                    2 -> MinimalistTheme.ACCENT_BUTTER_YELLOW
                    else -> Color.argb(75, Color.red(MinimalistTheme.ACCENT_TERRACOTTA), Color.green(MinimalistTheme.ACCENT_TERRACOTTA), Color.blue(MinimalistTheme.ACCENT_TERRACOTTA))
                }
                bgP.color = Color.argb(145, Color.red(basePastel), Color.green(basePastel), Color.blue(basePastel))
                canvas.drawCircle(cx, cy, r * 1.08f, bgP)
            }
            isGlass -> {
                val tile = RectF(cx - r * 1.10f, cy - r * 1.10f, cx + r * 1.10f, cy + r * 1.10f)
                bgP.color = Color.argb(46, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                canvas.drawRoundRect(tile, r * 0.44f, r * 0.44f, bgP)
                val glossP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.0f * d
                    color = Color.argb(95, 255, 255, 255)
                }
                canvas.drawRoundRect(tile, r * 0.44f, r * 0.44f, glossP)
            }
            isAurora -> {
                bgP.color = Color.argb(38, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                canvas.drawCircle(cx, cy, r * 1.12f, bgP)
            }
            else -> {
                val tile = RectF(cx - r * 1.08f, cy - r * 1.08f, cx + r * 1.08f, cy + r * 1.08f)
                bgP.color = Color.argb(28, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                canvas.drawRoundRect(tile, r * 0.36f, r * 0.36f, bgP)
            }
        }
    }

    // =========================================================================
    // 1. סמלים אישיים לכל 14 ההגדרות (תומך ב-Minimalist, Glass, Aurora ו-Classic)
    // =========================================================================
    fun createSettingsIcon(
        context: Context,
        settingKey: String,
        tintColor: Int,
        sizeDp: Float = 22f,
        isMinimalist: Boolean = false,
        isGlass: Boolean = false,
        isAurora: Boolean = false
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(24)

        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = (if (isMinimalist) 1.75f else 1.85f) * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isAurora) {
                    setShadowLayer(5f * d, 0f, 0f, Color.argb(175, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.37f

                val tone = abs(settingKey.hashCode()) % 4
                drawThemeBackdrop(canvas, cx, cy, r, d, tintColor, isMinimalist, isGlass, isAurora, tone)

                when (settingKey) {
                    "display" -> { // מצב תצוגה: שתי שכבות ערכות נושא חופפות + ניצוץ
                        canvas.drawRoundRect(RectF(cx - r * 0.72f, cy - r * 0.68f, cx + r * 0.28f, cy + r * 0.32f), r * 0.22f, r * 0.22f, strokeP)
                        canvas.drawRoundRect(RectF(cx - r * 0.22f, cy - r * 0.22f, cx + r * 0.74f, cy + r * 0.72f), r * 0.22f, r * 0.22f, strokeP)
                        canvas.drawCircle(cx + r * 0.26f, cy + r * 0.25f, r * 0.14f, fillP)
                    }
                    "customize" -> { // התאמה אישית: פלטת צבעים עם 3 נקודות צבע
                        canvas.drawArc(RectF(cx - r * 0.76f, cy - r * 0.76f, cx + r * 0.76f, cy + r * 0.76f), 40f, 300f, false, strokeP)
                        canvas.drawCircle(cx - r * 0.30f, cy - r * 0.26f, r * 0.13f, fillP)
                        canvas.drawCircle(cx + r * 0.18f, cy - r * 0.36f, r * 0.13f, fillP)
                        canvas.drawCircle(cx - r * 0.32f, cy + r * 0.24f, r * 0.13f, fillP)
                        canvas.drawCircle(cx + r * 0.28f, cy + r * 0.14f, r * 0.16f, strokeP)
                    }
                    "haptic" -> { // רטט: טלפון במרכז עם קווי רטט משני הצדדים
                        canvas.drawRoundRect(RectF(cx - r * 0.34f, cy - r * 0.68f, cx + r * 0.34f, cy + r * 0.68f), r * 0.14f, r * 0.14f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.42f, r * 0.07f, fillP)
                        canvas.drawLine(cx - r * 0.58f, cy - r * 0.34f, cx - r * 0.58f, cy + r * 0.34f, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy - r * 0.18f, cx - r * 0.80f, cy + r * 0.18f, strokeP)
                        canvas.drawLine(cx + r * 0.58f, cy - r * 0.34f, cx + r * 0.58f, cy + r * 0.34f, strokeP)
                        canvas.drawLine(cx + r * 0.80f, cy - r * 0.18f, cx + r * 0.80f, cy + r * 0.18f, strokeP)
                    }
                    "sound_toggle" -> { // צלילי הקשה: רמקול + שני גלי קול
                        val spk = Path().apply {
                            moveTo(cx - r * 0.72f, cy - r * 0.26f)
                            lineTo(cx - r * 0.34f, cy - r * 0.26f)
                            lineTo(cx + r * 0.12f, cy - r * 0.64f)
                            lineTo(cx + r * 0.12f, cy + r * 0.64f)
                            lineTo(cx - r * 0.34f, cy + r * 0.26f)
                            lineTo(cx - r * 0.72f, cy + r * 0.26f)
                            close()
                        }
                        canvas.drawPath(spk, strokeP)
                        canvas.drawArc(RectF(cx - r * 0.05f, cy - r * 0.38f, cx + r * 0.52f, cy + r * 0.38f), -52f, 104f, false, strokeP)
                        canvas.drawArc(RectF(cx + r * 0.05f, cy - r * 0.62f, cx + r * 0.80f, cy + r * 0.62f), -52f, 104f, false, strokeP)
                    }
                    "sound_type" -> { // סוג צליל: תו מוזיקלי כפול
                        canvas.drawLine(cx - r * 0.28f, cy + r * 0.42f, cx - r * 0.28f, cy - r * 0.56f, strokeP)
                        canvas.drawLine(cx + r * 0.44f, cy + r * 0.26f, cx + r * 0.44f, cy - r * 0.70f, strokeP)
                        canvas.drawLine(cx - r * 0.28f, cy - r * 0.56f, cx + r * 0.44f, cy - r * 0.70f, strokeP)
                        canvas.drawCircle(cx - r * 0.48f, cy + r * 0.46f, r * 0.20f, fillP)
                        canvas.drawCircle(cx + r * 0.24f, cy + r * 0.30f, r * 0.20f, fillP)
                    }
                    "ac_sound" -> { // צליל AC: תגית AC עם קשת צליל
                        canvas.drawRoundRect(RectF(cx - r * 0.80f, cy - r * 0.54f, cx + r * 0.50f, cy + r * 0.54f), r * 0.26f, r * 0.26f, strokeP)
                        fillP.textSize = r * 0.76f
                        canvas.drawText("AC", cx - r * 0.15f, cy + r * 0.26f, fillP)
                        canvas.drawArc(RectF(cx + r * 0.42f, cy - r * 0.34f, cx + r * 0.88f, cy + r * 0.34f), -50f, 100f, false, strokeP)
                    }
                    "animations" -> { // אנימציות: כוכב תנועה + שובלי קשת
                        val star = Path().apply {
                            moveTo(cx + r * 0.18f, cy - r * 0.72f)
                            quadTo(cx + r * 0.26f, cy - r * 0.16f, cx + r * 0.78f, cy - r * 0.08f)
                            quadTo(cx + r * 0.26f, cy + r * 0.02f, cx + r * 0.18f, cy + r * 0.58f)
                            quadTo(cx + r * 0.10f, cy + r * 0.02f, cx - r * 0.42f, cy - r * 0.08f)
                            quadTo(cx + r * 0.10f, cy - r * 0.16f, cx + r * 0.18f, cy - r * 0.72f)
                            close()
                        }
                        canvas.drawPath(star, strokeP)
                        canvas.drawLine(cx - r * 0.74f, cy + r * 0.42f, cx - r * 0.30f, cy + r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.64f, cy + r * 0.68f, cx - r * 0.18f, cy + r * 0.48f, strokeP)
                    }
                    "keep_screen" -> { // מסך תמיד דולק: שמש/נורה ערה + מסך
                        canvas.drawCircle(cx, cy - r * 0.12f, r * 0.34f, strokeP)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60 - 90).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.52f).toFloat(),
                                (cy - r * 0.12f + sin(a) * r * 0.52f).toFloat(),
                                (cx + cos(a) * r * 0.74f).toFloat(),
                                (cy - r * 0.12f + sin(a) * r * 0.74f).toFloat(),
                                strokeP
                            )
                        }
                        canvas.drawLine(cx - r * 0.48f, cy + r * 0.72f, cx + r * 0.48f, cy + r * 0.72f, strokeP)
                    }
                    "btn_shape" -> { // צורת כפתורים: 4 צורות כפתור (מרובע, מעוגל, עיגול, קפסולה)
                        canvas.drawRect(cx - r * 0.72f, cy - r * 0.72f, cx - r * 0.10f, cy - r * 0.10f, strokeP)
                        canvas.drawRoundRect(RectF(cx + r * 0.12f, cy - r * 0.72f, cx + r * 0.74f, cy - r * 0.10f), r * 0.22f, r * 0.22f, strokeP)
                        canvas.drawCircle(cx - r * 0.41f, cy + r * 0.41f, r * 0.31f, strokeP)
                        canvas.drawRoundRect(RectF(cx + r * 0.08f, cy + r * 0.16f, cx + r * 0.78f, cy + r * 0.66f), r * 0.25f, r * 0.25f, strokeP)
                    }
                    "precision" -> { // דיוק עשרוני: ".00" עם קו מידה תחתון
                        fillP.textSize = r * 0.92f
                        canvas.drawText(".00", cx, cy + r * 0.20f, fillP)
                        canvas.drawLine(cx - r * 0.68f, cy + r * 0.56f, cx + r * 0.68f, cy + r * 0.56f, strokeP)
                        canvas.drawCircle(cx + r * 0.34f, cy + r * 0.56f, r * 0.11f, fillP)
                    }
                    "pro_mode" -> { // מצב מקצועי: יהלום Pro עם ברק
                        val gem = Path().apply {
                            moveTo(cx - r * 0.48f, cy - r * 0.64f)
                            lineTo(cx + r * 0.48f, cy - r * 0.64f)
                            lineTo(cx + r * 0.80f, cy - r * 0.12f)
                            lineTo(cx, cy + r * 0.74f)
                            lineTo(cx - r * 0.80f, cy - r * 0.12f)
                            close()
                        }
                        canvas.drawPath(gem, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy - r * 0.12f, cx + r * 0.80f, cy - r * 0.12f, strokeP)
                        canvas.drawLine(cx, cy - r * 0.64f, cx, cy + r * 0.74f, strokeP)
                    }
                    "handedness" -> { // יד דומיננטית: כף יד מינימליסטית
                        canvas.drawRoundRect(RectF(cx - r * 0.46f, cy - r * 0.06f, cx + r * 0.46f, cy + r * 0.72f), r * 0.24f, r * 0.24f, strokeP)
                        for (ox in listOf(-0.34f, -0.11f, 0.12f, 0.35f)) {
                            canvas.drawLine(cx + r * ox, cy - r * 0.06f, cx + r * ox, cy - r * 0.66f, strokeP)
                        }
                        canvas.drawLine(cx - r * 0.46f, cy + r * 0.32f, cx - r * 0.76f, cy + r * 0.02f, strokeP)
                    }
                    "checklist" -> { // רשימת סימון: שורות עם סימני V
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.36f, cx - r * 0.52f, cy - r * 0.16f, strokeP)
                        canvas.drawLine(cx - r * 0.52f, cy - r * 0.16f, cx - r * 0.22f, cy - r * 0.56f, strokeP)
                        canvas.drawLine(cx + r * 0.04f, cy - r * 0.34f, cx + r * 0.74f, cy - r * 0.34f, strokeP)

                        canvas.drawLine(cx - r * 0.72f, cy + r * 0.30f, cx - r * 0.52f, cy + r * 0.50f, strokeP)
                        canvas.drawLine(cx - r * 0.52f, cy + r * 0.50f, cx - r * 0.22f, cy + r * 0.10f, strokeP)
                        canvas.drawLine(cx + r * 0.04f, cy + r * 0.32f, cx + r * 0.74f, cy + r * 0.32f, strokeP)
                    }
                    "reset" -> { // איפוס הגדרות: חץ מעגלי נגד כיוון השעון
                        canvas.drawArc(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), -50f, 285f, false, strokeP)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.62f, cx - r * 0.74f, cy - r * 0.54f, strokeP)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.62f, cx - r * 0.26f, cy - r * 0.24f, strokeP)
                    }
                }
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; fillP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; fillP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    // =========================================================================
    // 2. סמלים אישיים מעוצבים לכל 4 ערכות הנושא עבור Tab 73 ו-Tabs 81..99
    // =========================================================================
    fun createToolIcon(
        context: Context,
        tabIndex: Int,
        tintColor: Int,
        sizeDp: Float = 20f,
        isMinimalist: Boolean = false,
        isGlass: Boolean = false,
        isAurora: Boolean = false
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(22)

        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = (if (isMinimalist) 1.70f else 1.85f) * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isAurora) {
                    setShadowLayer(4.5f * d, 0f, 0f, Color.argb(170, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.37f

                drawThemeBackdrop(canvas, cx, cy, r, d, tintColor, isMinimalist, isGlass, isAurora, tabIndex % 4)

                when (tabIndex) {
                    73 -> { // תיקון ממיר צבעים HEX/RGB/CMYK: 3 מעגלי RGB חופפים (במקום [<>])
                        canvas.drawCircle(cx, cy - r * 0.30f, r * 0.42f, strokeP)
                        canvas.drawCircle(cx - r * 0.32f, cy + r * 0.24f, r * 0.42f, strokeP)
                        canvas.drawCircle(cx + r * 0.32f, cy + r * 0.24f, r * 0.42f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.06f, r * 0.11f, fillP)
                    }
                    81 -> { // כלי טקסט: Aa + שני קווי עריכה
                        fillP.textSize = r * 0.96f
                        canvas.drawText("Aa", cx, cy + r * 0.18f, fillP)
                        canvas.drawLine(cx - r * 0.66f, cy + r * 0.52f, cx + r * 0.66f, cy + r * 0.52f, strokeP)
                        canvas.drawLine(cx - r * 0.42f, cy + r * 0.76f, cx + r * 0.42f, cy + r * 0.76f, strokeP)
                    }
                    82 -> { // ספרות רומיות: כותרת ובסיס עמוד רומי + XII
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.62f, cx + r * 0.76f, cy - r * 0.62f, strokeP)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.62f, cx + r * 0.76f, cy + r * 0.62f, strokeP)
                        fillP.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        fillP.textSize = r * 0.88f
                        canvas.drawText("XII", cx, cy + r * 0.30f, fillP)
                        fillP.typeface = Typeface.DEFAULT_BOLD
                    }
                    83 -> { // סדרת מספרים: 3 עמודות מדורגות + קו מגמה
                        canvas.drawLine(cx - r * 0.54f, cy + r * 0.66f, cx - r * 0.54f, cy + r * 0.20f, strokeP)
                        canvas.drawLine(cx, cy + r * 0.66f, cx, cy - r * 0.12f, strokeP)
                        canvas.drawLine(cx + r * 0.54f, cy + r * 0.66f, cx + r * 0.54f, cy - r * 0.50f, strokeP)
                        canvas.drawCircle(cx - r * 0.54f, cy + r * 0.04f, r * 0.10f, fillP)
                        canvas.drawCircle(cx, cy - r * 0.28f, r * 0.10f, fillP)
                        canvas.drawCircle(cx + r * 0.54f, cy - r * 0.66f, r * 0.10f, fillP)
                    }
                    84 -> { // פרופורציות ויחס: עוגת יחסים מחולקת
                        canvas.drawCircle(cx, cy, r * 0.74f, strokeP)
                        canvas.drawLine(cx, cy, cx, cy - r * 0.74f, strokeP)
                        canvas.drawLine(cx, cy, cx + r * 0.64f, cy + r * 0.34f, strokeP)
                        canvas.drawCircle(cx - r * 0.28f, cy + r * 0.16f, r * 0.10f, fillP)
                    }
                    85 -> { // משוואה לינארית: מערכת צירים + ישר אלכסוני ונקודת חיתוך
                        canvas.drawLine(cx - r * 0.72f, cy + r * 0.58f, cx + r * 0.72f, cy + r * 0.58f, strokeP)
                        canvas.drawLine(cx - r * 0.58f, cy - r * 0.72f, cx - r * 0.58f, cy + r * 0.72f, strokeP)
                        canvas.drawLine(cx - r * 0.44f, cy + r * 0.36f, cx + r * 0.66f, cy - r * 0.60f, strokeP)
                        canvas.drawCircle(cx + r * 0.14f, cy - r * 0.14f, r * 0.12f, fillP)
                    }
                    86 -> { // יעד חיסכון וזמן הכפלה: מטרה עם חץ
                        canvas.drawCircle(cx, cy, r * 0.76f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.40f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.13f, fillP)
                        canvas.drawLine(cx + r * 0.12f, cy - r * 0.12f, cx + r * 0.76f, cy - r * 0.76f, strokeP)
                    }
                    87 -> { // ממיר שכר: שטר כסף עם מטבע וחיצי המרה
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.42f, cx + r * 0.78f, cy + r * 0.42f), r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.20f, strokeP)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.72f, cx + r * 0.42f, cy - r * 0.72f, strokeP)
                        canvas.drawLine(cx + r * 0.24f, cy - r * 0.86f, cx + r * 0.42f, cy - r * 0.72f, strokeP)
                    }
                    88 -> { // הנחות מצטברות: שתי תגיות מחיר חופפות מדורגות
                        val tag1 = Path().apply {
                            moveTo(cx - r * 0.74f, cy - r * 0.66f)
                            lineTo(cx - r * 0.06f, cy - r * 0.66f)
                            lineTo(cx + r * 0.44f, cy - r * 0.14f)
                            lineTo(cx - r * 0.14f, cy + r * 0.44f)
                            lineTo(cx - r * 0.74f, cy - r * 0.14f)
                            close()
                        }
                        canvas.drawPath(tag1, strokeP)
                        canvas.drawCircle(cx - r * 0.42f, cy - r * 0.34f, r * 0.09f, fillP)
                        canvas.drawLine(cx + r * 0.18f, cy - r * 0.62f, cx + r * 0.76f, cy - r * 0.04f, strokeP)
                        canvas.drawLine(cx + r * 0.76f, cy - r * 0.04f, cx + r * 0.14f, cy + r * 0.62f, strokeP)
                    }
                    89 -> { // תשואת מניות ודיבידנד: גרף מניה עולה + נקודת שיא
                        canvas.drawLine(cx - r * 0.74f, cy + r * 0.46f, cx - r * 0.22f, cy - r * 0.08f, strokeP)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.08f, cx + r * 0.14f, cy + r * 0.24f, strokeP)
                        canvas.drawLine(cx + r * 0.14f, cy + r * 0.24f, cx + r * 0.72f, cy - r * 0.52f, strokeP)
                        canvas.drawCircle(cx + r * 0.72f, cy - r * 0.52f, r * 0.14f, fillP)
                    }
                    90 -> { // פיקדון קבוע וריבית פשוטה: בניין בנק עם 3 עמודים
                        val roof = Path().apply {
                            moveTo(cx - r * 0.80f, cy - r * 0.22f)
                            lineTo(cx, cy - r * 0.76f)
                            lineTo(cx + r * 0.80f, cy - r * 0.22f)
                            close()
                        }
                        canvas.drawPath(roof, strokeP)
                        for (ox in listOf(-0.46f, 0f, 0.46f)) {
                            canvas.drawLine(cx + r * ox, cy - r * 0.20f, cx + r * ox, cy + r * 0.62f, strokeP)
                        }
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.66f, cx + r * 0.78f, cy + r * 0.66f, strokeP)
                    }
                    91 -> { // מחשבון SIP ואינפלציה: טבעת הפקדה מחזורית + פלוס צמיחה
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), -65f, 295f, false, strokeP)
                        canvas.drawLine(cx, cy - r * 0.34f, cx, cy + r * 0.34f, strokeP)
                        canvas.drawLine(cx - r * 0.34f, cy, cx + r * 0.34f, cy, strokeP)
                    }
                    92 -> { // הלוואת רכב ו-EMI: צללית רכב + גלגלים
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.10f, cx + r * 0.76f, cy + r * 0.44f), r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawLine(cx - r * 0.46f, cy - r * 0.10f, cx - r * 0.26f, cy - r * 0.54f, strokeP)
                        canvas.drawLine(cx - r * 0.26f, cy - r * 0.54f, cx + r * 0.26f, cy - r * 0.54f, strokeP)
                        canvas.drawLine(cx + r * 0.26f, cy - r * 0.54f, cx + r * 0.46f, cy - r * 0.10f, strokeP)
                        canvas.drawCircle(cx - r * 0.42f, cy + r * 0.54f, r * 0.15f, fillP)
                        canvas.drawCircle(cx + r * 0.42f, cy + r * 0.54f, r * 0.15f, fillP)
                    }
                    93 -> { // ערך עתידי ונוכחי FV/PV: מסגרת זמן + חץ דו-כיווני
                        canvas.drawRoundRect(RectF(cx - r * 0.74f, cy - r * 0.60f, cx + r * 0.74f, cy + r * 0.66f), r * 0.18f, r * 0.18f, strokeP)
                        canvas.drawLine(cx - r * 0.40f, cy + r * 0.04f, cx + r * 0.40f, cy + r * 0.04f, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy - r * 0.22f, cx + r * 0.40f, cy + r * 0.04f, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy + r * 0.30f, cx + r * 0.40f, cy + r * 0.04f, strokeP)
                    }
                    94 -> { // אנונה ואנונה נצחית: סמל אינסוף (∞) חלק
                        val inf = Path().apply {
                            moveTo(cx, cy)
                            cubicTo(cx - r * 0.28f, cy - r * 0.56f, cx - r * 0.84f, cy - r * 0.52f, cx - r * 0.84f, cy)
                            cubicTo(cx - r * 0.84f, cy + r * 0.52f, cx - r * 0.28f, cy + r * 0.56f, cx, cy)
                            cubicTo(cx + r * 0.28f, cy - r * 0.56f, cx + r * 0.84f, cy - r * 0.52f, cx + r * 0.84f, cy)
                            cubicTo(cx + r * 0.84f, cy + r * 0.52f, cx + r * 0.28f, cy + r * 0.56f, cx, cy)
                        }
                        canvas.drawPath(inf, strokeP)
                    }
                    95 -> { // מחשבון פרישה וקצבה: שמש מעל קו אופק וגל מרגיע
                        canvas.drawArc(RectF(cx - r * 0.48f, cy - r * 0.42f, cx + r * 0.48f, cy + r * 0.54f), 180f, 180f, false, strokeP)
                        canvas.drawLine(cx, cy - r * 0.58f, cx, cy - r * 0.80f, strokeP)
                        canvas.drawLine(cx - r * 0.56f, cy - r * 0.44f, cx - r * 0.72f, cy - r * 0.60f, strokeP)
                        canvas.drawLine(cx + r * 0.56f, cy - r * 0.44f, cx + r * 0.72f, cy - r * 0.60f, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.14f, cx + r * 0.80f, cy + r * 0.14f, strokeP)
                        val wave = Path().apply {
                            moveTo(cx - r * 0.66f, cy + r * 0.52f)
                            quadTo(cx - r * 0.33f, cy + r * 0.30f, cx, cy + r * 0.52f)
                            quadTo(cx + r * 0.33f, cy + r * 0.74f, cx + r * 0.66f, cy + r * 0.52f)
                        }
                        canvas.drawPath(wave, strokeP)
                    }
                    96 -> { // מד אור (Lux): נורת חיישן אור עם קרניים עליונות
                        canvas.drawArc(RectF(cx - r * 0.42f, cy - r * 0.46f, cx + r * 0.42f, cy + r * 0.38f), 135f, 270f, false, strokeP)
                        canvas.drawLine(cx - r * 0.24f, cy + r * 0.42f, cx + r * 0.24f, cy + r * 0.42f, strokeP)
                        canvas.drawLine(cx - r * 0.16f, cy + r * 0.66f, cx + r * 0.16f, cy + r * 0.66f, strokeP)
                        canvas.drawLine(cx, cy - r * 0.62f, cx, cy - r * 0.82f, strokeP)
                        canvas.drawLine(cx - r * 0.58f, cy - r * 0.38f, cx - r * 0.74f, cy - r * 0.52f, strokeP)
                        canvas.drawLine(cx + r * 0.58f, cy - r * 0.38f, cx + r * 0.74f, cy - r * 0.52f, strokeP)
                    }
                    97 -> { // בדיקת מהירות אינטרנט: גל Wi-Fi עליון + מד מחוג מהירות
                        canvas.drawArc(RectF(cx - r * 0.78f, cy - r * 0.42f, cx + r * 0.78f, cy + r * 0.94f), 195f, 150f, false, strokeP)
                        canvas.drawArc(RectF(cx - r * 0.54f, cy - r * 0.80f, cx + r * 0.54f, cy + r * 0.10f), 225f, 90f, false, strokeP)
                        canvas.drawLine(cx, cy + r * 0.44f, cx + r * 0.44f, cy - r * 0.04f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.44f, r * 0.13f, fillP)
                    }
                    98 -> { // קלינומטר: עיגול + קו אופק מוטה + זווית שיפוע
                        canvas.drawCircle(cx, cy, r * 0.78f, strokeP)
                        canvas.drawLine(cx - r * 0.64f, cy + r * 0.28f, cx + r * 0.64f, cy - r * 0.28f, strokeP)
                        canvas.drawLine(cx - r * 0.52f, cy + r * 0.28f, cx + r * 0.52f, cy + r * 0.28f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.14f, fillP)
                    }
                    else -> { // 99: פרטי רשת ו-IP: תגית IP + קו צומת רשת ו-3 נקודות
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.72f, cx + r * 0.68f, cy + r * 0.16f), r * 0.20f, r * 0.20f, strokeP)
                        fillP.textSize = r * 0.74f
                        canvas.drawText("IP", cx, cy - r * 0.04f, fillP)
                        canvas.drawLine(cx, cy + r * 0.16f, cx, cy + r * 0.64f, strokeP)
                        canvas.drawLine(cx - r * 0.64f, cy + r * 0.64f, cx + r * 0.64f, cy + r * 0.64f, strokeP)
                        canvas.drawCircle(cx - r * 0.64f, cy + r * 0.64f, r * 0.11f, fillP)
                        canvas.drawCircle(cx, cy + r * 0.64f, r * 0.11f, fillP)
                        canvas.drawCircle(cx + r * 0.64f, cy + r * 0.64f, r * 0.11f, fillP)
                    }
                }
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; fillP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; fillP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }
}

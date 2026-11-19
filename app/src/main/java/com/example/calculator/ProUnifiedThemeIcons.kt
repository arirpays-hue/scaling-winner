package com.example.calculator

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import kotlin.math.*

// =========================================================================
// מנוע סמלים וקטוריים נפרדים ומובחנים לכל אחת מ-4 ערכות הנושא:
// 1. Minimalist: קווי מתאר דקים ונקיים ללא רקע (אחיד עם שאר התפריט המינימליסטי)
// 2. Classic:    תגיות Material צבעוניות מלאות עם סמל לבן בולט
// 3. Glass:      אריח זכוכית iOS Squircle עם השתקפות אור עליונה וברק זכוכיתי
// 4. Aurora:     אייקוני ניאון זוהרים עם הילה רדיאלית
// =========================================================================
object ProUnifiedThemeIcons {

    private val classicPalette = intArrayOf(
        Color.parseColor("#1E88E5"), // כחול רויאל
        Color.parseColor("#00897B"), // טורקיז אמרלד
        Color.parseColor("#FB8C00"), // כתום ענבר
        Color.parseColor("#8E24AA"), // סגול עמוק
        Color.parseColor("#E53935"), // אדום אלמוג
        Color.parseColor("#3949AB"), // אינדיגו
        Color.parseColor("#43A047"), // ירוק יער
        Color.parseColor("#00ACC1"), // ציאן
        Color.parseColor("#D81B60"), // מג'נטה
        Color.parseColor("#F4511E")  // טרקוטה חם
    )

    private fun classicColorForKey(seed: Int): Int {
        return classicPalette[(seed and 0x7FFFFFFF) % classicPalette.size]
    }

    // עוטף אייקונים קיימים (0..80, כותרות 301..312, אודות והחלקות) למסגרת אריח זכוכית iOS
    fun wrapForLiquidGlass(
        context: Context,
        innerDrawable: Drawable,
        accentColor: Int,
        sizeDp: Float = 24f
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(26)
        return object : Drawable() {
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            private val glossP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            private val rimP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.15f * d
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val rect = RectF(
                    bounds.left + 0.8f * d,
                    bounds.top + 0.8f * d,
                    bounds.right - 0.8f * d,
                    bounds.bottom - 0.8f * d
                )
                val corner = rect.width() * 0.28f

                fillP.shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    Color.argb(95, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                    Color.argb(38, 255, 255, 255),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(rect, corner, corner, fillP)

                val topHalf = RectF(rect.left + 1f * d, rect.top + 1f * d, rect.right - 1f * d, rect.centerY())
                glossP.shader = LinearGradient(
                    topHalf.left, topHalf.top, topHalf.left, topHalf.bottom,
                    Color.argb(95, 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(topHalf, corner * 0.85f, corner * 0.85f, glossP)

                rimP.shader = LinearGradient(
                    rect.left, rect.top, rect.left, rect.bottom,
                    Color.argb(175, 255, 255, 255),
                    Color.argb(55, 255, 255, 255),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(rect, corner, corner, rimP)

                val pad = (bounds.width() * 0.19f).toInt()
                innerDrawable.setBounds(
                    bounds.left + pad,
                    bounds.top + pad,
                    bounds.right - pad,
                    bounds.bottom - pad
                )
                innerDrawable.draw(canvas)
            }

            override fun setAlpha(alpha: Int) { innerDrawable.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { innerDrawable.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    // עוטף אייקונים קיימים (0..80, כותרות 301..312, אודות והחלקות) לתגית Material צבעונית למצב הקלאסי
    fun wrapForClassicBadge(
        context: Context,
        innerDrawable: Drawable,
        seed: Int,
        sizeDp: Float = 24f
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(26)
        val badgeColor = classicColorForKey(seed)
        return object : Drawable() {
            private val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = badgeColor
            }
            private val shadeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1.0f * d
                color = Color.argb(65, 255, 255, 255)
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.46f
                canvas.drawCircle(cx, cy, r, bgP)
                canvas.drawCircle(cx, cy, r - 0.6f * d, shadeP)

                val pad = (bounds.width() * 0.21f).toInt()
                innerDrawable.setBounds(
                    bounds.left + pad,
                    bounds.top + pad,
                    bounds.right - pad,
                    bounds.bottom - pad
                )
                innerDrawable.draw(canvas)
            }

            override fun setAlpha(alpha: Int) { bgP.alpha = alpha; innerDrawable.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { innerDrawable.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    // מעטפת רקע מותאמת לפי ערכת הנושא הפעילה (מינימליסטי = ללא רקע כלל)
    private fun drawDistinctBackdropAndResolveGlyphColor(
        canvas: Canvas,
        bounds: Rect,
        d: Float,
        tintColor: Int,
        seed: Int,
        isMinimalist: Boolean,
        isGlass: Boolean,
        isAurora: Boolean
    ): Int {
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()

        return when {
            isMinimalist -> tintColor
            isGlass -> {
                val rect = RectF(bounds.left + 0.8f * d, bounds.top + 0.8f * d, bounds.right - 0.8f * d, bounds.bottom - 0.8f * d)
                val corner = rect.width() * 0.28f

                val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        rect.left, rect.top, rect.right, rect.bottom,
                        Color.argb(90, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                        Color.argb(34, 255, 255, 255),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, corner, corner, fillP)

                val topGlow = RectF(rect.left + 1f * d, rect.top + 1f * d, rect.right - 1f * d, rect.centerY())
                val glossP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        topGlow.left, topGlow.top, topGlow.left, topGlow.bottom,
                        Color.argb(95, 255, 255, 255),
                        Color.argb(0, 255, 255, 255),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(topGlow, corner * 0.85f, corner * 0.85f, glossP)

                val rimP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.1f * d
                    shader = LinearGradient(
                        rect.left, rect.top, rect.left, rect.bottom,
                        Color.argb(180, 255, 255, 255),
                        Color.argb(55, 255, 255, 255),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, corner, corner, rimP)
                Color.WHITE
            }
            isAurora -> {
                val glowR = min(w, h) * 0.48f
                val haloP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = RadialGradient(
                        cx, cy, glowR.coerceAtLeast(1f),
                        Color.argb(72, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(cx, cy, glowR, haloP)
                tintColor
            }
            else -> {
                val badgeColor = classicColorForKey(seed)
                val r = min(w, h) * 0.46f
                val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = badgeColor
                }
                canvas.drawCircle(cx, cy, r, bgP)
                val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 0.9f * d
                    color = Color.argb(65, 255, 255, 255)
                }
                canvas.drawCircle(cx, cy, r - 0.5f * d, borderP)
                Color.WHITE
            }
        }
    }

    // =========================================================================
    // 1. סמלים אישיים לכל 14 ההגדרות (4 סגנונות שונים לפי ערכת הנושא)
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
        val effectiveSizeDp = if (isMinimalist) sizeDp else (sizeDp + 3f)
        val sizePx = (effectiveSizeDp * d).toInt().coerceAtLeast(24)

        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            private val duotoneP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val glyphScale = if (isMinimalist || isAurora) 0.40f else 0.30f
                val r = min(bounds.width(), bounds.height()) * glyphScale

                val glyphColor = drawDistinctBackdropAndResolveGlyphColor(
                    canvas, bounds, d, tintColor, settingKey.hashCode(), isMinimalist, isGlass, isAurora
                )
                strokeP.color = glyphColor
                strokeP.strokeWidth = when {
                    isMinimalist -> 1.65f * d
                    isGlass -> 1.60f * d
                    isAurora -> 1.90f * d
                    else -> 1.75f * d
                }
                if (isAurora) {
                    strokeP.setShadowLayer(5f * d, 0f, 0f, Color.argb(180, Color.red(glyphColor), Color.green(glyphColor), Color.blue(glyphColor)))
                } else {
                    strokeP.clearShadowLayer()
                }
                fillP.color = glyphColor
                duotoneP.color = Color.argb(if (isGlass) 75 else 55, Color.red(glyphColor), Color.green(glyphColor), Color.blue(glyphColor))

                when (settingKey) {
                    "display" -> {
                        val back = RectF(cx - r * 0.72f, cy - r * 0.68f, cx + r * 0.28f, cy + r * 0.32f)
                        val front = RectF(cx - r * 0.22f, cy - r * 0.22f, cx + r * 0.74f, cy + r * 0.72f)
                        if (!isMinimalist) canvas.drawRoundRect(front, r * 0.22f, r * 0.22f, duotoneP)
                        canvas.drawRoundRect(back, r * 0.22f, r * 0.22f, strokeP)
                        canvas.drawRoundRect(front, r * 0.22f, r * 0.22f, strokeP)
                        canvas.drawCircle(cx + r * 0.26f, cy + r * 0.25f, r * 0.14f, fillP)
                    }
                    "customize" -> {
                        if (!isMinimalist) canvas.drawCircle(cx, cy, r * 0.70f, duotoneP)
                        canvas.drawArc(RectF(cx - r * 0.76f, cy - r * 0.76f, cx + r * 0.76f, cy + r * 0.76f), 40f, 300f, false, strokeP)
                        canvas.drawCircle(cx - r * 0.30f, cy - r * 0.26f, r * 0.13f, fillP)
                        canvas.drawCircle(cx + r * 0.18f, cy - r * 0.36f, r * 0.13f, fillP)
                        canvas.drawCircle(cx - r * 0.32f, cy + r * 0.24f, r * 0.13f, fillP)
                        canvas.drawCircle(cx + r * 0.28f, cy + r * 0.14f, r * 0.16f, strokeP)
                    }
                    "haptic" -> {
                        val phone = RectF(cx - r * 0.34f, cy - r * 0.68f, cx + r * 0.34f, cy + r * 0.68f)
                        if (!isMinimalist) canvas.drawRoundRect(phone, r * 0.14f, r * 0.14f, duotoneP)
                        canvas.drawRoundRect(phone, r * 0.14f, r * 0.14f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.42f, r * 0.08f, fillP)
                        canvas.drawLine(cx - r * 0.58f, cy - r * 0.34f, cx - r * 0.58f, cy + r * 0.34f, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy - r * 0.18f, cx - r * 0.80f, cy + r * 0.18f, strokeP)
                        canvas.drawLine(cx + r * 0.58f, cy - r * 0.34f, cx + r * 0.58f, cy + r * 0.34f, strokeP)
                        canvas.drawLine(cx + r * 0.80f, cy - r * 0.18f, cx + r * 0.80f, cy + r * 0.18f, strokeP)
                    }
                    "sound_toggle" -> {
                        val spk = Path().apply {
                            moveTo(cx - r * 0.72f, cy - r * 0.26f)
                            lineTo(cx - r * 0.34f, cy - r * 0.26f)
                            lineTo(cx + r * 0.12f, cy - r * 0.64f)
                            lineTo(cx + r * 0.12f, cy + r * 0.64f)
                            lineTo(cx - r * 0.34f, cy + r * 0.26f)
                            lineTo(cx - r * 0.72f, cy + r * 0.26f)
                            close()
                        }
                        if (!isMinimalist) canvas.drawPath(spk, duotoneP)
                        canvas.drawPath(spk, strokeP)
                        canvas.drawArc(RectF(cx - r * 0.05f, cy - r * 0.38f, cx + r * 0.52f, cy + r * 0.38f), -52f, 104f, false, strokeP)
                        canvas.drawArc(RectF(cx + r * 0.05f, cy - r * 0.62f, cx + r * 0.80f, cy + r * 0.62f), -52f, 104f, false, strokeP)
                    }
                    "sound_type" -> {
                        canvas.drawLine(cx - r * 0.28f, cy + r * 0.42f, cx - r * 0.28f, cy - r * 0.56f, strokeP)
                        canvas.drawLine(cx + r * 0.44f, cy + r * 0.26f, cx + r * 0.44f, cy - r * 0.70f, strokeP)
                        canvas.drawLine(cx - r * 0.28f, cy - r * 0.56f, cx + r * 0.44f, cy - r * 0.70f, strokeP)
                        canvas.drawCircle(cx - r * 0.48f, cy + r * 0.46f, r * 0.20f, fillP)
                        canvas.drawCircle(cx + r * 0.24f, cy + r * 0.30f, r * 0.20f, fillP)
                    }
                    "ac_sound" -> {
                        val badge = RectF(cx - r * 0.80f, cy - r * 0.54f, cx + r * 0.50f, cy + r * 0.54f)
                        if (!isMinimalist) canvas.drawRoundRect(badge, r * 0.26f, r * 0.26f, duotoneP)
                        canvas.drawRoundRect(badge, r * 0.26f, r * 0.26f, strokeP)
                        fillP.textSize = r * 0.78f
                        canvas.drawText("AC", cx - r * 0.15f, cy + r * 0.26f, fillP)
                        canvas.drawArc(RectF(cx + r * 0.42f, cy - r * 0.34f, cx + r * 0.88f, cy + r * 0.34f), -50f, 100f, false, strokeP)
                    }
                    "animations" -> {
                        val star = Path().apply {
                            moveTo(cx + r * 0.18f, cy - r * 0.72f)
                            quadTo(cx + r * 0.26f, cy - r * 0.16f, cx + r * 0.78f, cy - r * 0.08f)
                            quadTo(cx + r * 0.26f, cy + r * 0.02f, cx + r * 0.18f, cy + r * 0.58f)
                            quadTo(cx + r * 0.10f, cy + r * 0.02f, cx - r * 0.42f, cy - r * 0.08f)
                            quadTo(cx + r * 0.10f, cy - r * 0.16f, cx + r * 0.18f, cy - r * 0.72f)
                            close()
                        }
                        if (!isMinimalist) canvas.drawPath(star, duotoneP)
                        canvas.drawPath(star, strokeP)
                        canvas.drawLine(cx - r * 0.74f, cy + r * 0.42f, cx - r * 0.30f, cy + r * 0.22f, strokeP)
                        canvas.drawLine(cx - r * 0.64f, cy + r * 0.68f, cx - r * 0.18f, cy + r * 0.48f, strokeP)
                    }
                    "keep_screen" -> {
                        if (!isMinimalist) canvas.drawCircle(cx, cy - r * 0.12f, r * 0.34f, duotoneP)
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
                    "btn_shape" -> {
                        canvas.drawRect(cx - r * 0.72f, cy - r * 0.72f, cx - r * 0.10f, cy - r * 0.10f, strokeP)
                        canvas.drawRoundRect(RectF(cx + r * 0.12f, cy - r * 0.72f, cx + r * 0.74f, cy - r * 0.10f), r * 0.22f, r * 0.22f, strokeP)
                        canvas.drawCircle(cx - r * 0.41f, cy + r * 0.41f, r * 0.31f, strokeP)
                        canvas.drawRoundRect(RectF(cx + r * 0.08f, cy + r * 0.16f, cx + r * 0.78f, cy + r * 0.66f), r * 0.25f, r * 0.25f, strokeP)
                    }
                    "precision" -> {
                        fillP.textSize = r * 0.96f
                        canvas.drawText(".00", cx, cy + r * 0.20f, fillP)
                        canvas.drawLine(cx - r * 0.68f, cy + r * 0.56f, cx + r * 0.68f, cy + r * 0.56f, strokeP)
                        canvas.drawCircle(cx + r * 0.34f, cy + r * 0.56f, r * 0.12f, fillP)
                    }
                    "pro_mode" -> {
                        val gem = Path().apply {
                            moveTo(cx - r * 0.48f, cy - r * 0.64f)
                            lineTo(cx + r * 0.48f, cy - r * 0.64f)
                            lineTo(cx + r * 0.80f, cy - r * 0.12f)
                            lineTo(cx, cy + r * 0.74f)
                            lineTo(cx - r * 0.80f, cy - r * 0.12f)
                            close()
                        }
                        if (!isMinimalist) canvas.drawPath(gem, duotoneP)
                        canvas.drawPath(gem, strokeP)
                        canvas.drawLine(cx - r * 0.80f, cy - r * 0.12f, cx + r * 0.80f, cy - r * 0.12f, strokeP)
                        canvas.drawLine(cx, cy - r * 0.64f, cx, cy + r * 0.74f, strokeP)
                    }
                    "handedness" -> {
                        val palm = RectF(cx - r * 0.46f, cy - r * 0.06f, cx + r * 0.46f, cy + r * 0.72f)
                        if (!isMinimalist) canvas.drawRoundRect(palm, r * 0.24f, r * 0.24f, duotoneP)
                        canvas.drawRoundRect(palm, r * 0.24f, r * 0.24f, strokeP)
                        for (ox in listOf(-0.34f, -0.11f, 0.12f, 0.35f)) {
                            canvas.drawLine(cx + r * ox, cy - r * 0.06f, cx + r * ox, cy - r * 0.66f, strokeP)
                        }
                        canvas.drawLine(cx - r * 0.46f, cy + r * 0.32f, cx - r * 0.76f, cy + r * 0.02f, strokeP)
                    }
                    "checklist" -> {
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.36f, cx - r * 0.52f, cy - r * 0.16f, strokeP)
                        canvas.drawLine(cx - r * 0.52f, cy - r * 0.16f, cx - r * 0.22f, cy - r * 0.56f, strokeP)
                        canvas.drawLine(cx + r * 0.04f, cy - r * 0.34f, cx + r * 0.74f, cy - r * 0.34f, strokeP)
                        canvas.drawLine(cx - r * 0.72f, cy + r * 0.30f, cx - r * 0.52f, cy + r * 0.50f, strokeP)
                        canvas.drawLine(cx - r * 0.52f, cy + r * 0.50f, cx - r * 0.22f, cy + r * 0.10f, strokeP)
                        canvas.drawLine(cx + r * 0.04f, cy + r * 0.32f, cx + r * 0.74f, cy + r * 0.32f, strokeP)
                    }
                    "reset" -> {
                        canvas.drawArc(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), -50f, 285f, false, strokeP)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.62f, cx - r * 0.74f, cy - r * 0.54f, strokeP)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.62f, cx - r * 0.26f, cy - r * 0.24f, strokeP)
                    }
                    else -> {
                        canvas.drawCircle(cx, cy, r * 0.34f, strokeP)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.44f).toFloat(),
                                (cy + sin(a) * r * 0.44f).toFloat(),
                                (cx + cos(a) * r * 0.76f).toFloat(),
                                (cy + sin(a) * r * 0.76f).toFloat(),
                                strokeP
                            )
                        }
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
        val effectiveSizeDp = if (isMinimalist) sizeDp else (sizeDp + 4f)
        val sizePx = (effectiveSizeDp * d).toInt().coerceAtLeast(22)

        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            private val duotoneP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val glyphScale = if (isMinimalist || isAurora) 0.40f else 0.30f
                val r = min(bounds.width(), bounds.height()) * glyphScale

                val glyphColor = drawDistinctBackdropAndResolveGlyphColor(
                    canvas, bounds, d, tintColor, tabIndex, isMinimalist, isGlass, isAurora
                )
                strokeP.color = glyphColor
                strokeP.strokeWidth = when {
                    isMinimalist -> 1.75f * d
                    isGlass -> 1.55f * d
                    isAurora -> 1.85f * d
                    else -> 1.75f * d
                }
                if (isAurora) {
                    strokeP.setShadowLayer(4.5f * d, 0f, 0f, Color.argb(175, Color.red(glyphColor), Color.green(glyphColor), Color.blue(glyphColor)))
                } else {
                    strokeP.clearShadowLayer()
                }
                fillP.color = glyphColor
                duotoneP.color = Color.argb(if (isGlass) 75 else 50, Color.red(glyphColor), Color.green(glyphColor), Color.blue(glyphColor))

                when (tabIndex) {
                    73 -> { // ממיר צבעים HEX/RGB/CMYK: 3 מעגלי RGB חופפים
                        if (!isMinimalist) {
                            canvas.drawCircle(cx, cy - r * 0.30f, r * 0.42f, duotoneP)
                            canvas.drawCircle(cx - r * 0.32f, cy + r * 0.24f, r * 0.42f, duotoneP)
                            canvas.drawCircle(cx + r * 0.32f, cy + r * 0.24f, r * 0.42f, duotoneP)
                        }
                        canvas.drawCircle(cx, cy - r * 0.30f, r * 0.42f, strokeP)
                        canvas.drawCircle(cx - r * 0.32f, cy + r * 0.24f, r * 0.42f, strokeP)
                        canvas.drawCircle(cx + r * 0.32f, cy + r * 0.24f, r * 0.42f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.06f, r * 0.11f, fillP)
                    }
                    81 -> { // כלי טקסט: Aa + שני קווי עריכה
                        fillP.textSize = r * 1.00f
                        canvas.drawText("Aa", cx, cy + r * 0.18f, fillP)
                        canvas.drawLine(cx - r * 0.66f, cy + r * 0.52f, cx + r * 0.66f, cy + r * 0.52f, strokeP)
                        canvas.drawLine(cx - r * 0.42f, cy + r * 0.76f, cx + r * 0.42f, cy + r * 0.76f, strokeP)
                    }
                    82 -> { // ספרות רומיות: כותרת ובסיס עמוד רומי + XII
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.62f, cx + r * 0.76f, cy - r * 0.62f, strokeP)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.62f, cx + r * 0.76f, cy + r * 0.62f, strokeP)
                        fillP.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                        fillP.textSize = r * 0.90f
                        canvas.drawText("XII", cx, cy + r * 0.30f, fillP)
                        fillP.typeface = Typeface.DEFAULT_BOLD
                    }
                    83 -> { // סדרת מספרים: 3 עמודות מדורגות + נקודות שיא
                        canvas.drawLine(cx - r * 0.54f, cy + r * 0.66f, cx - r * 0.54f, cy + r * 0.20f, strokeP)
                        canvas.drawLine(cx, cy + r * 0.66f, cx, cy - r * 0.12f, strokeP)
                        canvas.drawLine(cx + r * 0.54f, cy + r * 0.66f, cx + r * 0.54f, cy - r * 0.50f, strokeP)
                        canvas.drawCircle(cx - r * 0.54f, cy + r * 0.04f, r * 0.10f, fillP)
                        canvas.drawCircle(cx, cy - r * 0.28f, r * 0.10f, fillP)
                        canvas.drawCircle(cx + r * 0.54f, cy - r * 0.66f, r * 0.10f, fillP)
                    }
                    84 -> { // פרופורציות ויחס: עוגת יחסים מחולקת
                        if (!isMinimalist) canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), -90f, 118f, true, duotoneP)
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
                        val bill = RectF(cx - r * 0.78f, cy - r * 0.42f, cx + r * 0.78f, cy + r * 0.42f)
                        if (!isMinimalist) canvas.drawRoundRect(bill, r * 0.16f, r * 0.16f, duotoneP)
                        canvas.drawRoundRect(bill, r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.20f, strokeP)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.72f, cx + r * 0.42f, cy - r * 0.72f, strokeP)
                        canvas.drawLine(cx + r * 0.24f, cy - r * 0.86f, cx + r * 0.42f, cy - r * 0.72f, strokeP)
                    }
                    88 -> { // הנחות מצטברות: שתי תגיות מחיר מדורגות
                        val tag1 = Path().apply {
                            moveTo(cx - r * 0.74f, cy - r * 0.66f)
                            lineTo(cx - r * 0.06f, cy - r * 0.66f)
                            lineTo(cx + r * 0.44f, cy - r * 0.14f)
                            lineTo(cx - r * 0.14f, cy + r * 0.44f)
                            lineTo(cx - r * 0.74f, cy - r * 0.14f)
                            close()
                        }
                        if (!isMinimalist) canvas.drawPath(tag1, duotoneP)
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
                    90 -> { // פיקדון קבוע וריבית פשוטה: בניין בנק
                        val roof = Path().apply {
                            moveTo(cx - r * 0.80f, cy - r * 0.22f)
                            lineTo(cx, cy - r * 0.76f)
                            lineTo(cx + r * 0.80f, cy - r * 0.22f)
                            close()
                        }
                        if (!isMinimalist) canvas.drawPath(roof, duotoneP)
                        canvas.drawPath(roof, strokeP)
                        for (ox in listOf(-0.46f, 0f, 0.46f)) {
                            canvas.drawLine(cx + r * ox, cy - r * 0.20f, cx + r * ox, cy + r * 0.62f, strokeP)
                        }
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.66f, cx + r * 0.78f, cy + r * 0.66f, strokeP)
                    }
                    91 -> { // מחשבון SIP ואינפלציה: טבעת הפקדה + פלוס צמיחה
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), -65f, 295f, false, strokeP)
                        canvas.drawLine(cx, cy - r * 0.34f, cx, cy + r * 0.34f, strokeP)
                        canvas.drawLine(cx - r * 0.34f, cy, cx + r * 0.34f, cy, strokeP)
                    }
                    92 -> { // הלוואת רכב ו-EMI: צללית רכב + גלגלים
                        val body = RectF(cx - r * 0.76f, cy - r * 0.10f, cx + r * 0.76f, cy + r * 0.44f)
                        if (!isMinimalist) canvas.drawRoundRect(body, r * 0.16f, r * 0.16f, duotoneP)
                        canvas.drawRoundRect(body, r * 0.16f, r * 0.16f, strokeP)
                        canvas.drawLine(cx - r * 0.46f, cy - r * 0.10f, cx - r * 0.26f, cy - r * 0.54f, strokeP)
                        canvas.drawLine(cx - r * 0.26f, cy - r * 0.54f, cx + r * 0.26f, cy - r * 0.54f, strokeP)
                        canvas.drawLine(cx + r * 0.26f, cy - r * 0.54f, cx + r * 0.46f, cy - r * 0.10f, strokeP)
                        canvas.drawCircle(cx - r * 0.42f, cy + r * 0.54f, r * 0.15f, fillP)
                        canvas.drawCircle(cx + r * 0.42f, cy + r * 0.54f, r * 0.15f, fillP)
                    }
                    93 -> { // ערך עתידי ונוכחי FV/PV: מסגרת זמן + חץ
                        val frame = RectF(cx - r * 0.74f, cy - r * 0.60f, cx + r * 0.74f, cy + r * 0.66f)
                        if (!isMinimalist) canvas.drawRoundRect(frame, r * 0.18f, r * 0.18f, duotoneP)
                        canvas.drawRoundRect(frame, r * 0.18f, r * 0.18f, strokeP)
                        canvas.drawLine(cx - r * 0.40f, cy + r * 0.04f, cx + r * 0.40f, cy + r * 0.04f, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy - r * 0.22f, cx + r * 0.40f, cy + r * 0.04f, strokeP)
                        canvas.drawLine(cx + r * 0.16f, cy + r * 0.30f, cx + r * 0.40f, cy + r * 0.04f, strokeP)
                    }
                    94 -> { // אנונה ואנונה נצחית: סמל אינסוף (∞)
                        val inf = Path().apply {
                            moveTo(cx, cy)
                            cubicTo(cx - r * 0.28f, cy - r * 0.56f, cx - r * 0.84f, cy - r * 0.52f, cx - r * 0.84f, cy)
                            cubicTo(cx - r * 0.84f, cy + r * 0.52f, cx - r * 0.28f, cy + r * 0.56f, cx, cy)
                            cubicTo(cx + r * 0.28f, cy - r * 0.56f, cx + r * 0.84f, cy - r * 0.52f, cx + r * 0.84f, cy)
                            cubicTo(cx + r * 0.84f, cy + r * 0.52f, cx + r * 0.28f, cy + r * 0.56f, cx, cy)
                        }
                        canvas.drawPath(inf, strokeP)
                    }
                    95 -> { // מחשבון פרישה וקצבה: שמש מעל אופק וגל
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
                    96 -> { // מד אור (Lux): נורת חיישן אור עם קרניים
                        if (!isMinimalist) canvas.drawCircle(cx, cy - r * 0.04f, r * 0.40f, duotoneP)
                        canvas.drawArc(RectF(cx - r * 0.42f, cy - r * 0.46f, cx + r * 0.42f, cy + r * 0.38f), 135f, 270f, false, strokeP)
                        canvas.drawLine(cx - r * 0.24f, cy + r * 0.42f, cx + r * 0.24f, cy + r * 0.42f, strokeP)
                        canvas.drawLine(cx - r * 0.16f, cy + r * 0.66f, cx + r * 0.16f, cy + r * 0.66f, strokeP)
                        canvas.drawLine(cx, cy - r * 0.62f, cx, cy - r * 0.82f, strokeP)
                        canvas.drawLine(cx - r * 0.58f, cy - r * 0.38f, cx - r * 0.74f, cy - r * 0.52f, strokeP)
                        canvas.drawLine(cx + r * 0.58f, cy - r * 0.38f, cx + r * 0.74f, cy - r * 0.52f, strokeP)
                    }
                    97 -> { // בדיקת מהירות אינטרנט: גל Wi-Fi + מד מחוג
                        canvas.drawArc(RectF(cx - r * 0.78f, cy - r * 0.42f, cx + r * 0.78f, cy + r * 0.94f), 195f, 150f, false, strokeP)
                        canvas.drawArc(RectF(cx - r * 0.54f, cy - r * 0.80f, cx + r * 0.54f, cy + r * 0.10f), 225f, 90f, false, strokeP)
                        canvas.drawLine(cx, cy + r * 0.44f, cx + r * 0.44f, cy - r * 0.04f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.44f, r * 0.13f, fillP)
                    }
                    98 -> { // קלינומטר: עיגול + קו אופק מוטה + זווית
                        canvas.drawCircle(cx, cy, r * 0.78f, strokeP)
                        canvas.drawLine(cx - r * 0.64f, cy + r * 0.28f, cx + r * 0.64f, cy - r * 0.28f, strokeP)
                        canvas.drawLine(cx - r * 0.52f, cy + r * 0.28f, cx + r * 0.52f, cy + r * 0.28f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.14f, fillP)
                    }
                    else -> { // 99: פרטי רשת ו-IP: תגית IP + קו צומת ו-3 נקודות
                        val ipBox = RectF(cx - r * 0.68f, cy - r * 0.72f, cx + r * 0.68f, cy + r * 0.16f)
                        if (!isMinimalist) canvas.drawRoundRect(ipBox, r * 0.20f, r * 0.20f, duotoneP)
                        canvas.drawRoundRect(ipBox, r * 0.20f, r * 0.20f, strokeP)
                        fillP.textSize = r * 0.76f
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

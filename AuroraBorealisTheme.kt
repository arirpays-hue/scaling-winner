package com.example.calculator

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

object AuroraBorealisTheme {

    val EMERALD_NIGHT = Color.parseColor("#0A1C1A")
    val ARCTIC_BLUE = Color.parseColor("#07111E")
    val COSMIC_PURPLE = Color.parseColor("#1B0F2E")
    val AURORA_PRIMARY = Color.parseColor("#00F5D4")
    val AURORA_SECONDARY = Color.parseColor("#10B981")
    val EXPRESSION_CYAN_GREY = Color.parseColor("#A0B2C6")

    var auroraOpacity: Int = 195
    var auroraBlurRadius: Float = 40f
    var auroraGlowIntensity: Int = 230
    var auroraCornerPt: Float = 24f

    fun loadAuroraPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        auroraOpacity = prefs.getInt("aurora_opacity", 195)
        auroraBlurRadius = prefs.getFloat("aurora_blur", 40f)
        auroraGlowIntensity = prefs.getInt("aurora_glow", 230)
        auroraCornerPt = prefs.getFloat("aurora_corner", 24f)
    }

    fun saveAuroraPrefs(prefs: SharedPreferences?) {
        prefs?.edit()
            ?.putInt("aurora_opacity", auroraOpacity)
            ?.putFloat("aurora_blur", auroraBlurRadius)
            ?.putInt("aurora_glow", auroraGlowIntensity)
            ?.putFloat("aurora_corner", auroraCornerPt)
            ?.apply()
    }

    fun applyHeaderMetallicGradient(tv: TextView?, isAurora: Boolean, isLiquidGlass: Boolean) {
        if (tv == null) return
        tv.post {
            if (tv.height > 0) {
                val isMin = (tv.context as? MainActivity)?.isMinimalistMode == true
                if (isMin) {
                    tv.paint.shader = null
                    tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                    tv.setTextColor(MinimalistTheme.TEXT_CHARCOAL)
                    tv.invalidate()
                    return@post
                }
                val bottomCol = when {
                    isAurora -> Color.parseColor("#70C1B3")
                    isLiquidGlass -> Color.parseColor("#A0AAB8")
                    else -> Color.WHITE
                }
                if (isAurora || isLiquidGlass) {
                    tv.paint.shader = LinearGradient(
                        0f, 0f, 0f, tv.height.toFloat(),
                        Color.WHITE, bottomCol, Shader.TileMode.CLAMP
                    )
                    if (isAurora) {
                        tv.setShadowLayer(14f, 0f, 0f, Color.argb(120, 0, 245, 212))
                    } else {
                        tv.setShadowLayer(8f, 0f, 2f, Color.argb(90, 0, 0, 0))
                    }
                    tv.invalidate()
                } else {
                    tv.paint.shader = null
                    tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
                    tv.invalidate()
                }
            }
        }
    }

    fun createAuroraAtmosphericBackground(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(EMERALD_NIGHT, ARCTIC_BLUE, COSMIC_PURPLE)
        )
    }

    fun styleAuroraSymbol(tv: TextView, symbol: String, isPressed: Boolean = false) {
        val isOperator = symbol in listOf("÷", "×", "−", "+", "=")
        val isCommand = symbol in listOf("AC", "⌫", "%", "MC", "MR", "M+", "M-")

        val textColor = when {
            isPressed -> Color.WHITE
            isOperator -> EMERALD_NIGHT
            isCommand -> AURORA_PRIMARY
            else -> Color.WHITE
        }
        tv.setTextColor(textColor)

        val displaySym = if (symbol == "-") "−" else symbol
        if (displaySym in listOf("sin", "cos", "tan", "log", "ln", "x²", "x³", "x^y", "e")) {
            tv.text = SpannableString(displaySym).apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, displaySym.length, 0)
            }
        } else {
            tv.text = displaySym
        }

        if (isPressed) {
            tv.setShadowLayer(20f, 0f, 0f, Color.argb(235, 0, 245, 212))
        } else {
            if (isOperator) {
                tv.setShadowLayer(4f, 0f, 1f, Color.argb(120, 255, 255, 255))
            } else {
                tv.setShadowLayer(8f, 0f, -1.2f, Color.argb(150, 0, 245, 212))
            }
        }
    }

    fun createAuroraButtonDrawable(
        activity: MainActivity,
        symbol: String,
        isMem: Boolean,
        isPressed: Boolean = false
    ): Drawable {
        if (activity.isMinimalistMode) {
            return MinimalistTheme.createMinimalistKeypadButtonDrawable(activity, symbol, isPressed)
        }
        val radius = activity.dp(auroraCornerPt).toFloat()
        val isOperator = symbol in listOf("÷", "×", "−", "+", "=")
        val isCommand = symbol in listOf("AC", "⌫", "%") || isMem
        val boost = if (isPressed) 50 else 0

        val (topColor, bottomColor) = when {
            isOperator -> {
                Color.argb((205 + boost).coerceAtMost(255), 0, 245, 212) to
                        Color.argb((160 + boost).coerceAtMost(255), 16, 185, 129)
            }
            isCommand -> {
                Color.argb((85 + boost).coerceAtMost(255), 200, 220, 230) to
                        Color.argb((55 + boost).coerceAtMost(255), 150, 180, 195)
            }
            else -> {
                Color.argb((165 + boost).coerceAtMost(255), 24, 42, 54) to
                        Color.argb((135 + boost).coerceAtMost(255), 15, 28, 36)
            }
        }

        val ambientShadow = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.argb(if (isPressed) 50 else 125, 0, 0, 0))
        }

        val glassBody = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(topColor, bottomColor)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setStroke(activity.dp(1f), Color.argb(if (isPressed) 220 else 110, 0, 245, 212))
        }

        val specularTopEdge = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(185, 255, 255, 255), Color.argb(45, 0, 245, 212))
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(radius, radius, radius, radius, radius * 0.25f, radius * 0.25f, radius * 0.25f, radius * 0.25f)
            setStroke(activity.dp(1f), Color.argb(190, 255, 255, 255))
        }

        val radialPulse = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = radius * 1.5f
            val alpha = if (isPressed) 165 else 25
            colors = intArrayOf(Color.argb(alpha, 0, 245, 212), Color.TRANSPARENT)
        }

        val layers = LayerDrawable(arrayOf(ambientShadow, glassBody, radialPulse, specularTopEdge)).apply {
            val bottomInset = if (isPressed) activity.dp(1f) else activity.dp(2.5f)
            setLayerInset(1, 0, 0, 0, bottomInset)
            setLayerInset(2, activity.dp(1f), activity.dp(1f), activity.dp(1f), bottomInset + activity.dp(1f))
            setLayerInset(3, activity.dp(1.5f), activity.dp(1f), activity.dp(1.5f), activity.dp(18f))
        }

        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#6600F5D4")), layers, null)
    }

    fun createAuroraCardDrawable(activity: MainActivity, cornerDp: Float = 28f, isDrawer: Boolean = false): Drawable {
        if (activity.isMinimalistMode) {
            return MinimalistTheme.createMinimalistCardDrawable(activity, cornerDp, 0)
        }
        val radius = if (isDrawer) 0f else activity.dp(cornerDp).toFloat()
        val glass = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.argb(if (isDrawer) 215 else 190, 15, 28, 36),
                Color.argb(if (isDrawer) 205 else 175, 10, 20, 26),
                Color.argb(if (isDrawer) 195 else 160, 18, 32, 40)
            )
        ).apply {
            cornerRadius = radius
            if (!isDrawer) {
                setStroke(activity.dp(1.2f), Color.argb(120, 0, 245, 212))
            }
        }

        if (isDrawer) return glass

        val specularRim = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(140, 255, 255, 255), Color.argb(30, 0, 245, 212))
        ).apply { cornerRadius = radius }

        return LayerDrawable(arrayOf(glass, specularRim)).apply {
            setLayerInset(1, activity.dp(1f), activity.dp(1f), activity.dp(1f), activity.dp(22f))
        }
    }

    fun createActiveDrawerCapsule(activity: MainActivity, isAurora: Boolean): Drawable {
        if (activity.isMinimalistMode) {
            return MinimalistTheme.createMinimalistActiveDrawerCapsule(activity)
        }
        val radius = activity.dp(if (isAurora) 14f else 12f).toFloat()
        val glowCol = if (isAurora) AURORA_PRIMARY else activity.primaryColor
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.argb(75, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
                Color.argb(35, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol))
            )
        ).apply {
            cornerRadius = radius
            setStroke(activity.dp(1f), Color.argb(155, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)))
        }
    }

    fun createVectorIcon(
        context: Context,
        iconCode: Int,
        tintColor: Int = AURORA_PRIMARY,
        sizeDp: Float = 22f,
        isGlowing: Boolean = true
    ): Drawable {
        if ((context as? MainActivity)?.isMinimalistMode == true) {
            return MinimalistTheme.createMinimalistVectorIcon(context, iconCode, tintColor, sizeDp)
        }
        if (!isGlowing) {
            return IosGlassTheme.createSfSymbolDrawable(context, iconCode, tintColor, sizeDp)
        }

        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val d = context.resources.displayMetrics.density
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                setShadowLayer(
                    6.0f * d,
                    0f,
                    0f,
                    Color.argb(190, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                )
            }
            private val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AURORA_SECONDARY
                style = Paint.Style.STROKE
                strokeWidth = 1.4f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(115, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                style = Paint.Style.STROKE
                strokeWidth = 1.1f * d
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            private fun createCrystalBadgePath(cx: Float, cy: Float, rad: Float): Path {
                val c = rad * 0.42f
                return Path().apply {
                    moveTo(cx - rad + c, cy - rad)
                    lineTo(cx + rad - c, cy - rad)
                    lineTo(cx + rad, cy - rad + c)
                    lineTo(cx + rad, cy + rad - c)
                    lineTo(cx + rad - c, cy + rad)
                    lineTo(cx - rad + c, cy + rad)
                    lineTo(cx - rad, cy + rad - c)
                    lineTo(cx - rad, cy - rad + c)
                    close()
                }
            }

            override fun draw(canvas: Canvas) {
                val b = bounds
                val cx = b.exactCenterX()
                val cy = b.exactCenterY()
                val r = Math.min(b.width(), b.height()) * 0.38f

                val badgeR = r * 1.18f
                fillPaint.shader = LinearGradient(
                    cx - badgeR, cy - badgeR, cx + badgeR, cy + badgeR,
                    intArrayOf(
                        Color.argb(55, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                        Color.argb(30, 16, 185, 129),
                        Color.argb(50, 88, 28, 135)
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                val crystalBadge = createCrystalBadgePath(cx, cy, badgeR)
                canvas.drawPath(crystalBadge, fillPaint)
                canvas.drawPath(crystalBadge, borderPaint)

                when (iconCode) {
                    0, 301 -> {
                        val p = Path().apply {
                            moveTo(cx, cy - r * 0.9f); lineTo(cx + r * 0.9f, cy); lineTo(cx, cy + r * 0.9f); lineTo(cx - r * 0.9f, cy); close()
                        }
                        canvas.drawPath(p, paint)
                        canvas.drawLine(cx - r * 0.42f, cy, cx + r * 0.42f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.42f, cx, cy + r * 0.42f, paint)
                        canvas.drawCircle(cx, cy - r * 0.9f, r * 0.1f, dotPaint)
                        canvas.drawCircle(cx, cy + r * 0.9f, r * 0.1f, dotPaint)
                    }
                    1, 44, 309 -> {
                        for (ang in listOf(0f, 60f, -60f)) {
                            canvas.save(); canvas.rotate(ang, cx, cy)
                            canvas.drawOval(RectF(cx - r * 0.9f, cy - r * 0.34f, cx + r * 0.9f, cy + r * 0.34f), paint)
                            canvas.restore()
                        }
                        canvas.drawCircle(cx, cy, r * 0.18f, dotPaint)
                        canvas.drawCircle(cx + r * 0.65f, cy - r * 0.35f, r * 0.1f, dotPaint)
                        canvas.drawCircle(cx - r * 0.65f, cy + r * 0.35f, r * 0.1f, dotPaint)
                    }
                    2, 24 -> {
                        val p = Path().apply {
                            moveTo(cx - r * 0.58f, cy - r * 0.8f); lineTo(cx + r * 0.58f, cy - r * 0.8f); lineTo(cx + r * 0.58f, cy + r * 0.78f)
                            lineTo(cx + r * 0.28f, cy + r * 0.54f); lineTo(cx, cy + r * 0.78f); lineTo(cx - r * 0.28f, cy + r * 0.54f); lineTo(cx - r * 0.58f, cy + r * 0.78f); close()
                        }
                        canvas.drawPath(p, paint)
                        canvas.drawLine(cx - r * 0.28f, cy + r * 0.15f, cx + r * 0.28f, cy - r * 0.35f, paint)
                        canvas.drawCircle(cx - r * 0.2f, cy - r * 0.3f, r * 0.1f, dotPaint)
                        canvas.drawCircle(cx + r * 0.2f, cy + r * 0.1f, r * 0.1f, dotPaint)
                    }
                    3, 29, 302 -> {
                        val hex = Path()
                        for (i in 0..5) {
                            val a = Math.toRadians((i * 60 - 30).toDouble())
                            val x = (cx + cos(a) * r * 0.85f).toFloat()
                            val y = (cy + sin(a) * r * 0.85f).toFloat()
                            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
                        }
                        hex.close()
                        canvas.drawPath(hex, paint)
                        canvas.drawCircle(cx, cy, r * 0.32f, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    4, 303 -> {
                        val star = Path().apply {
                            moveTo(cx, cy - r * 0.92f); lineTo(cx + r * 0.24f, cy - r * 0.24f); lineTo(cx + r * 0.92f, cy); lineTo(cx + r * 0.24f, cy + r * 0.24f)
                            lineTo(cx, cy + r * 0.92f); lineTo(cx - r * 0.24f, cy + r * 0.24f); lineTo(cx - r * 0.92f, cy); lineTo(cx - r * 0.24f, cy - r * 0.24f); close()
                        }
                        canvas.drawPath(star, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    5 -> {
                        canvas.drawCircle(cx, cy - r * 0.62f, r * 0.2f, paint)
                        val body = Path().apply {
                            moveTo(cx - r * 0.45f, cy - r * 0.35f); lineTo(cx + r * 0.45f, cy - r * 0.35f)
                            lineTo(cx + r * 0.78f, cy + r * 0.75f); lineTo(cx - r * 0.78f, cy + r * 0.75f); close()
                        }
                        canvas.drawPath(body, paint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.25f, cx + r * 0.35f, cy + r * 0.25f, secondaryPaint)
                    }
                    6 -> {
                        for (i in 0..2) {
                            val a = Math.toRadians((i * 60).toDouble())
                            val dx = (cos(a) * r * 0.85f).toFloat(); val dy = (sin(a) * r * 0.85f).toFloat()
                            canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, paint)
                        }
                        canvas.drawCircle(cx, cy, r * 0.28f, paint)
                    }
                    7, 28 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.48f, cy - r * 0.48f, cx + r * 0.48f, cy + r * 0.48f), r * 0.12f, r * 0.12f, paint)
                        canvas.drawCircle(cx, cy, r * 0.18f, dotPaint)
                        for (o in listOf(-0.24f * r, 0.24f * r)) {
                            canvas.drawLine(cx + o, cy - r * 0.88f, cx + o, cy - r * 0.48f, paint)
                            canvas.drawLine(cx + o, cy + r * 0.48f, cx + o, cy + r * 0.88f, paint)
                            canvas.drawLine(cx - r * 0.88f, cy + o, cx - r * 0.48f, cy + o, paint)
                            canvas.drawLine(cx + r * 0.48f, cy + o, cx + r * 0.88f, cy + o, paint)
                        }
                    }
                    8 -> {
                        canvas.drawArc(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), 25f, 240f, false, paint)
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.55f, cx + r * 0.55f, cy - r * 0.55f, paint)
                        canvas.drawCircle(cx - r * 0.42f, cy - r * 0.42f, r * 0.16f, dotPaint)
                        canvas.drawCircle(cx + r * 0.42f, cy + r * 0.42f, r * 0.16f, dotPaint)
                    }
                    9, 25 -> {
                        val gem = Path().apply {
                            moveTo(cx - r * 0.48f, cy - r * 0.72f); lineTo(cx + r * 0.48f, cy - r * 0.72f); lineTo(cx + r * 0.86f, cy - r * 0.18f)
                            lineTo(cx, cy + r * 0.84f); lineTo(cx - r * 0.86f, cy - r * 0.18f); close()
                        }
                        canvas.drawPath(gem, paint)
                        canvas.drawLine(cx - r * 0.86f, cy - r * 0.18f, cx + r * 0.86f, cy - r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.48f, cy - r * 0.72f, cx, cy + r * 0.84f, secondaryPaint)
                        canvas.drawLine(cx + r * 0.48f, cy - r * 0.72f, cx, cy + r * 0.84f, secondaryPaint)
                    }
                    10, 23 -> {
                        val glass = Path().apply {
                            moveTo(cx - r * 0.74f, cy - r * 0.62f); lineTo(cx + r * 0.74f, cy - r * 0.62f); lineTo(cx, cy + r * 0.15f); close()
                        }
                        canvas.drawPath(glass, paint)
                        canvas.drawLine(cx, cy + r * 0.15f, cx, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.44f, cy + r * 0.78f, cx + r * 0.44f, cy + r * 0.78f, paint)
                        canvas.drawCircle(cx, cy - r * 0.32f, r * 0.13f, dotPaint)
                    }
                    11, 27, 304 -> {
                        val heart = Path().apply {
                            moveTo(cx, cy + r * 0.8f); lineTo(cx - r * 0.84f, cy - r * 0.12f); lineTo(cx - r * 0.45f, cy - r * 0.7f)
                            lineTo(cx, cy - r * 0.28f); lineTo(cx + r * 0.45f, cy - r * 0.7f); lineTo(cx + r * 0.84f, cy - r * 0.12f); close()
                        }
                        canvas.drawPath(heart, paint)
                        canvas.drawCircle(cx, cy + r * 0.1f, r * 0.14f, dotPaint)
                    }
                    12, 30 -> {
                        val hg = Path().apply {
                            moveTo(cx - r * 0.65f, cy - r * 0.78f); lineTo(cx + r * 0.65f, cy - r * 0.78f)
                            lineTo(cx - r * 0.65f, cy + r * 0.78f); lineTo(cx + r * 0.65f, cy + r * 0.78f); close()
                        }
                        canvas.drawPath(hg, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx, cy + r * 0.48f, r * 0.14f, dotPaint)
                    }
                    13, 26 -> {
                        canvas.drawCircle(cx, cy, r * 0.76f, paint)
                        canvas.drawOval(RectF(cx - r * 0.36f, cy - r * 0.76f, cx + r * 0.36f, cy + r * 0.76f), paint)
                        canvas.drawLine(cx - r * 0.76f, cy, cx + r * 0.76f, cy, paint)
                    }
                    14 -> {
                        val flame = Path().apply {
                            moveTo(cx, cy - r * 0.9f)
                            quadTo(cx + r * 0.85f, cy - r * 0.1f, cx, cy + r * 0.85f)
                            quadTo(cx - r * 0.85f, cy - r * 0.1f, cx, cy - r * 0.9f)
                        }
                        canvas.drawPath(flame, paint)
                        canvas.drawCircle(cx, cy + r * 0.25f, r * 0.2f, dotPaint)
                    }
                    15 -> {
                        val w1 = Path().apply {
                            moveTo(cx - r * 0.85f, cy + r * 0.2f)
                            cubicTo(cx - r * 0.3f, cy - r * 0.9f, cx + r * 0.3f, cy + r * 0.9f, cx + r * 0.85f, cy - r * 0.2f)
                        }
                        val w2 = Path().apply {
                            moveTo(cx - r * 0.85f, cy - r * 0.2f)
                            cubicTo(cx - r * 0.3f, cy + r * 0.9f, cx + r * 0.3f, cy - r * 0.9f, cx + r * 0.85f, cy + r * 0.2f)
                        }
                        canvas.drawPath(w1, paint)
                        canvas.drawPath(w2, secondaryPaint)
                    }
                    16 -> {
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.55f, cx + r * 0.75f, cy - r * 0.55f, paint)
                        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.42f, r * 0.24f, paint)
                        canvas.drawCircle(cx + r * 0.35f, cy + r * 0.42f, r * 0.24f, paint)
                    }
                    17 -> {
                        canvas.drawLine(cx - r * 0.6f, cy + r * 0.75f, cx - r * 0.6f, cy + r * 0.2f, paint)
                        canvas.drawLine(cx, cy + r * 0.75f, cx, cy - r * 0.15f, paint)
                        canvas.drawLine(cx + r * 0.6f, cy + r * 0.75f, cx + r * 0.6f, cy - r * 0.6f, paint)
                        canvas.drawCircle(cx - r * 0.6f, cy + r * 0.2f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx, cy - r * 0.15f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx + r * 0.6f, cy - r * 0.6f, r * 0.12f, dotPaint)
                        canvas.drawLine(cx - r * 0.6f, cy + r * 0.2f, cx, cy - r * 0.15f, secondaryPaint)
                        canvas.drawLine(cx, cy - r * 0.15f, cx + r * 0.6f, cy - r * 0.6f, secondaryPaint)
                    }
                    18, 21 -> {
                        val rocket = Path().apply {
                            moveTo(cx, cy - r * 0.9f); quadTo(cx + r * 0.65f, cy - r * 0.1f, cx + r * 0.45f, cy + r * 0.52f)
                            lineTo(cx - r * 0.45f, cy + r * 0.52f); quadTo(cx - r * 0.65f, cy - r * 0.1f, cx, cy - r * 0.9f)
                        }
                        canvas.drawPath(rocket, paint)
                        canvas.drawCircle(cx, cy + r * 0.78f, r * 0.15f, dotPaint)
                    }
                    19 -> {
                        canvas.drawCircle(cx, cy, r * 0.34f, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawLine((cx + cos(a) * r * 0.42f).toFloat(), (cy + sin(a) * r * 0.42f).toFloat(), (cx + cos(a) * r * 0.86f).toFloat(), (cy + sin(a) * r * 0.86f).toFloat(), paint)
                        }
                    }
                    20 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.55f, cy - r * 0.78f, cx + r * 0.55f, cy + r * 0.78f), r * 0.2f, r * 0.2f, paint)
                        canvas.drawLine(cx + r * 0.15f, cy - r * 0.4f, cx - r * 0.2f, cy + r * 0.05f, paint)
                        canvas.drawLine(cx - r * 0.2f, cy + r * 0.05f, cx + r * 0.2f, cy + r * 0.05f, paint)
                        canvas.drawLine(cx + r * 0.2f, cy + r * 0.05f, cx - r * 0.15f, cy + r * 0.45f, paint)
                    }
                    22 -> {
                        val cap = Path().apply {
                            moveTo(cx, cy - r * 0.75f); lineTo(cx + r * 0.9f, cy - r * 0.2f); lineTo(cx, cy + r * 0.35f); lineTo(cx - r * 0.9f, cy - r * 0.2f); close()
                        }
                        canvas.drawPath(cap, paint)
                        canvas.drawCircle(cx, cy - r * 0.2f, r * 0.13f, dotPaint)
                    }
                    31, 305 -> {
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        val diamond = Path().apply {
                            moveTo(cx, cy - r * 0.82f); lineTo(cx + r * 0.82f, cy)
                            lineTo(cx, cy + r * 0.82f); lineTo(cx - r * 0.82f, cy); close()
                        }
                        canvas.drawPath(diamond, paint)
                        canvas.drawCircle(cx, cy, r * 0.2f, dotPaint)
                    }
                    32, 58, 306 -> {
                        for (dy in listOf(-0.45f * r, 0f, 0.45f * r)) {
                            val wave = Path().apply {
                                moveTo(cx - r * 0.8f, cy + dy)
                                quadTo(cx - r * 0.4f, cy + dy - r * 0.28f, cx, cy + dy)
                                quadTo(cx + r * 0.4f, cy + dy + r * 0.28f, cx + r * 0.8f, cy + dy)
                            }
                            canvas.drawPath(wave, paint)
                        }
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
                    }
                    33, 62 -> {
                        val strand1 = Path().apply {
                            moveTo(cx - r * 0.55f, cy - r * 0.82f)
                            cubicTo(cx + r * 0.75f, cy - r * 0.25f, cx - r * 0.75f, cy + r * 0.25f, cx + r * 0.55f, cy + r * 0.82f)
                        }
                        val strand2 = Path().apply {
                            moveTo(cx + r * 0.55f, cy - r * 0.82f)
                            cubicTo(cx - r * 0.75f, cy - r * 0.25f, cx + r * 0.75f, cy + r * 0.25f, cx - r * 0.55f, cy + r * 0.82f)
                        }
                        canvas.drawPath(strand1, paint)
                        canvas.drawPath(strand2, paint)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.5f, cx + r * 0.35f, cy - r * 0.5f, secondaryPaint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.5f, cx + r * 0.35f, cy + r * 0.5f, secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
                    }
                    34 -> {
                        val pyr = Path().apply {
                            moveTo(cx, cy - r * 0.86f)
                            lineTo(cx + r * 0.82f, cy + r * 0.48f)
                            lineTo(cx, cy + r * 0.84f)
                            lineTo(cx - r * 0.82f, cy + r * 0.48f)
                            close()
                        }
                        canvas.drawPath(pyr, paint)
                        canvas.drawLine(cx, cy - r * 0.86f, cx, cy + r * 0.84f, paint)
                        canvas.drawCircle(cx, cy - r * 0.86f, r * 0.14f, dotPaint)
                    }
                    35 -> {
                        val zig = Path().apply {
                            moveTo(cx - r * 0.9f, cy)
                            lineTo(cx - r * 0.55f, cy)
                            lineTo(cx - r * 0.38f, cy - r * 0.52f)
                            lineTo(cx - r * 0.12f, cy + r * 0.52f)
                            lineTo(cx + r * 0.12f, cy - r * 0.52f)
                            lineTo(cx + r * 0.38f, cy + r * 0.52f)
                            lineTo(cx + r * 0.55f, cy)
                            lineTo(cx + r * 0.9f, cy)
                        }
                        canvas.drawPath(zig, paint)
                        canvas.drawCircle(cx - r * 0.88f, cy, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.88f, cy, r * 0.11f, dotPaint)
                    }
                    36 -> {
                        canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 0.65f, cx - r * 0.15f, cy + r * 0.65f), -65f, 130f, false, paint)
                        canvas.drawArc(RectF(cx + r * 0.15f, cy - r * 0.65f, cx + r * 0.85f, cy + r * 0.65f), 115f, 130f, false, paint)
                        canvas.drawCircle(cx, cy, r * 0.2f, dotPaint)
                        canvas.drawLine(cx - r * 0.92f, cy, cx - r * 0.38f, cy, paint)
                        canvas.drawLine(cx + r * 0.38f, cy, cx + r * 0.92f, cy, paint)
                    }
                    37 -> {
                        val braid1 = Path().apply {
                            moveTo(cx - r * 0.85f, cy - r * 0.35f)
                            cubicTo(cx - r * 0.2f, cy + r * 0.7f, cx + r * 0.2f, cy - r * 0.7f, cx + r * 0.85f, cy + r * 0.35f)
                        }
                        val braid2 = Path().apply {
                            moveTo(cx - r * 0.85f, cy + r * 0.35f)
                            cubicTo(cx - r * 0.2f, cy - r * 0.7f, cx + r * 0.2f, cy + r * 0.7f, cx + r * 0.85f, cy - r * 0.35f)
                        }
                        canvas.drawPath(braid1, paint)
                        canvas.drawPath(braid2, paint)
                        canvas.drawCircle(cx + r * 0.85f, cy - r * 0.35f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx + r * 0.85f, cy + r * 0.35f, r * 0.12f, dotPaint)
                    }
                    38, 60, 307 -> {
                        canvas.drawCircle(cx, cy, r * 0.32f, dotPaint)
                        canvas.drawCircle(cx, cy, r * 0.82f, secondaryPaint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60 + 15).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.32f).toFloat(),
                                (cy + sin(a) * r * 0.32f).toFloat(),
                                (cx + cos(a) * r * 0.82f).toFloat(),
                                (cy + sin(a) * r * 0.82f).toFloat(),
                                paint
                            )
                        }
                    }
                    39 -> {
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawOval(RectF(cx - r * 0.82f, cy - r * 0.32f, cx + r * 0.82f, cy + r * 0.32f), paint)
                        canvas.drawOval(RectF(cx - r * 0.32f, cy - r * 0.82f, cx + r * 0.32f, cy + r * 0.82f), secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.18f, dotPaint)
                    }
                    40, 308 -> {
                        canvas.drawCircle(cx, cy, r * 0.86f, secondaryPaint)
                        val star4 = Path().apply {
                            moveTo(cx, cy - r * 0.88f); lineTo(cx + r * 0.22f, cy - r * 0.22f)
                            lineTo(cx + r * 0.88f, cy); lineTo(cx + r * 0.22f, cy + r * 0.22f)
                            lineTo(cx, cy + r * 0.88f); lineTo(cx - r * 0.22f, cy + r * 0.22f)
                            lineTo(cx - r * 0.88f, cy); lineTo(cx - r * 0.22f, cy - r * 0.22f); close()
                        }
                        canvas.drawPath(star4, paint)
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
                    }
                    41 -> {
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.65f, cx + r * 0.82f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.65f, cx + r * 0.55f, cy - r * 0.68f, paint)
                        canvas.drawArc(RectF(cx - r * 1.45f, cy - r * 0.05f, cx - r * 0.05f, cy + r * 1.35f), -48f, 48f, false, secondaryPaint)
                        canvas.drawCircle(cx - r * 0.75f, cy + r * 0.65f, r * 0.16f, dotPaint)
                    }
                    42 -> {
                        canvas.drawArc(RectF(cx - r * 0.84f, cy - r * 0.84f, cx + r * 0.84f, cy + r * 0.84f), -90f, 290f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.54f, cy - r * 0.54f, cx + r * 0.54f, cy + r * 0.54f), -90f, 210f, false, secondaryPaint)
                        canvas.drawLine(cx, cy, cx + r * 0.42f, cy - r * 0.42f, paint)
                        canvas.drawCircle(cx, cy - r * 0.84f, r * 0.14f, dotPaint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    43, 61 -> {
                        val prism = Path().apply {
                            moveTo(cx - r * 0.1f, cy - r * 0.72f)
                            lineTo(cx + r * 0.48f, cy + r * 0.68f)
                            lineTo(cx - r * 0.68f, cy + r * 0.68f)
                            close()
                        }
                        canvas.drawPath(prism, paint)
                        canvas.drawLine(cx - r * 0.92f, cy + r * 0.1f, cx - r * 0.35f, cy + r * 0.1f, paint)
                        canvas.drawLine(cx + r * 0.2f, cy - r * 0.05f, cx + r * 0.9f, cy - r * 0.55f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.1f, cx + r * 0.92f, cy, secondaryPaint)
                        canvas.drawLine(cx + r * 0.3f, cy + r * 0.25f, cx + r * 0.9f, cy + r * 0.55f, paint)
                    }
                    45 -> {
                        canvas.drawCircle(cx, cy - r * 0.25f, r * 0.15f, dotPaint)
                        canvas.drawLine(cx, cy - r * 0.1f, cx - r * 0.42f, cy + r * 0.8f, paint)
                        canvas.drawLine(cx, cy - r * 0.1f, cx + r * 0.42f, cy + r * 0.8f, paint)
                        canvas.drawArc(RectF(cx - r * 0.55f, cy - r * 0.8f, cx + r * 0.55f, cy + r * 0.3f), -150f, 120f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 1.05f, cx + r * 0.85f, cy + r * 0.55f), -140f, 100f, false, secondaryPaint)
                    }
                    46 -> {
                        val shield = Path().apply {
                            moveTo(cx, cy - r * 0.86f)
                            lineTo(cx + r * 0.72f, cy - r * 0.45f)
                            lineTo(cx + r * 0.58f, cy + r * 0.35f)
                            lineTo(cx, cy + r * 0.86f)
                            lineTo(cx - r * 0.58f, cy + r * 0.35f)
                            lineTo(cx - r * 0.72f, cy - r * 0.45f)
                            close()
                        }
                        canvas.drawPath(shield, paint)
                        canvas.drawCircle(cx, cy - r * 0.1f, r * 0.18f, dotPaint)
                        canvas.drawLine(cx, cy + r * 0.08f, cx, cy + r * 0.45f, paint)
                    }
                    47 -> {
                        val t1 = Path().apply {
                            moveTo(cx, cy - r * 0.85f); lineTo(cx + r * 0.74f, cy + r * 0.42f); lineTo(cx - r * 0.74f, cy + r * 0.42f); close()
                        }
                        val t2 = Path().apply {
                            moveTo(cx, cy + r * 0.85f); lineTo(cx + r * 0.74f, cy - r * 0.42f); lineTo(cx - r * 0.74f, cy - r * 0.42f); close()
                        }
                        canvas.drawPath(t1, paint)
                        canvas.drawPath(t2, paint)
                        canvas.drawCircle(cx, cy, r * 0.13f, dotPaint)
                    }
                    48 -> {
                        val crown = Path().apply {
                            moveTo(cx - r * 0.78f, cy + r * 0.65f)
                            lineTo(cx - r * 0.68f, cy - r * 0.35f)
                            lineTo(cx - r * 0.28f, cy + r * 0.15f)
                            lineTo(cx, cy - r * 0.68f)
                            lineTo(cx + r * 0.28f, cy + r * 0.15f)
                            lineTo(cx + r * 0.68f, cy - r * 0.35f)
                            lineTo(cx + r * 0.78f, cy + r * 0.65f)
                            close()
                        }
                        canvas.drawPath(crown, paint)
                        canvas.drawCircle(cx, cy - r * 0.68f, r * 0.12f, dotPaint)
                    }
                    49 -> {
                        canvas.drawRect(cx - r * 0.68f, cy - r * 0.1f, cx - r * 0.2f, cy + r * 0.78f, paint)
                        canvas.drawRect(cx - r * 0.1f, cy - r * 0.55f, cx + r * 0.45f, cy + r * 0.78f, paint)
                        canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 0.85f, cx + r * 0.85f, cy + r * 0.25f), 200f, 140f, false, secondaryPaint)
                    }
                    50, 310 -> {
                        canvas.drawOval(RectF(cx - r * 0.82f, cy - r * 0.32f, cx + r * 0.82f, cy + r * 0.32f), secondaryPaint)
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.7f, cx + r * 0.55f, cy - r * 0.7f, paint)
                        canvas.drawLine(cx + r * 0.1f, cy - r * 0.7f, cx + r * 0.55f, cy - r * 0.7f, paint)
                        canvas.drawLine(cx + r * 0.55f, cy - r * 0.7f, cx + r * 0.55f, cy - r * 0.25f, paint)
                        canvas.drawCircle(cx + r * 0.55f, cy - r * 0.7f, r * 0.12f, dotPaint)
                    }
                    51 -> {
                        canvas.drawLine(cx - r * 0.8f, cy + r * 0.7f, cx + r * 0.8f, cy - r * 0.7f, paint)
                        canvas.drawLine(cx - r * 0.8f, cy - r * 0.4f, cx + r * 0.8f, cy + r * 0.5f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, dotPaint)
                        canvas.drawCircle(cx, cy, r * 0.45f, secondaryPaint)
                    }
                    52 -> {
                        val bell1 = Path().apply {
                            moveTo(cx - r * 0.88f, cy + r * 0.62f)
                            cubicTo(cx - r * 0.35f, cy + r * 0.62f, cx - r * 0.32f, cy - r * 0.72f, cx, cy - r * 0.72f)
                            cubicTo(cx + r * 0.32f, cy - r * 0.72f, cx + r * 0.35f, cy + r * 0.62f, cx + r * 0.88f, cy + r * 0.62f)
                        }
                        val bell2 = Path().apply {
                            moveTo(cx - r * 0.75f, cy + r * 0.62f)
                            cubicTo(cx - r * 0.28f, cy + r * 0.62f, cx - r * 0.25f, cy - r * 0.32f, cx, cy - r * 0.32f)
                            cubicTo(cx + r * 0.25f, cy - r * 0.32f, cx + r * 0.28f, cy + r * 0.62f, cx + r * 0.75f, cy + r * 0.62f)
                        }
                        canvas.drawPath(bell2, secondaryPaint)
                        canvas.drawPath(bell1, paint)
                        canvas.drawCircle(cx, cy - r * 0.72f, r * 0.14f, dotPaint)
                    }
                    53 -> {
                        canvas.drawCircle(cx, cy, r * 0.84f, secondaryPaint)
                        val tri = Path().apply {
                            moveTo(cx, cy - r * 0.84f)
                            lineTo(cx + r * 0.73f, cy + r * 0.42f)
                            lineTo(cx - r * 0.73f, cy + r * 0.42f)
                            close()
                        }
                        canvas.drawPath(tri, paint)
                        canvas.drawCircle(cx, cy, r * 0.38f, paint)
                        canvas.drawCircle(cx, cy, r * 0.11f, dotPaint)
                    }
                    54, 311 -> {
                        val topY = cy - r * 0.68f
                        val midY = cy
                        val botY = cy + r * 0.68f
                        canvas.drawLine(cx, topY, cx - r * 0.45f, midY, paint)
                        canvas.drawLine(cx, topY, cx + r * 0.45f, midY, paint)
                        canvas.drawLine(cx - r * 0.45f, midY, cx - r * 0.7f, botY, paint)
                        canvas.drawLine(cx - r * 0.45f, midY, cx - r * 0.18f, botY, paint)
                        canvas.drawLine(cx + r * 0.45f, midY, cx + r * 0.18f, botY, paint)
                        canvas.drawLine(cx + r * 0.45f, midY, cx + r * 0.7f, botY, paint)
                        canvas.drawCircle(cx, topY, r * 0.15f, dotPaint)
                        canvas.drawCircle(cx - r * 0.45f, midY, r * 0.13f, dotPaint)
                        canvas.drawCircle(cx + r * 0.45f, midY, r * 0.13f, dotPaint)
                    }
                    55 -> {
                        canvas.drawOval(RectF(cx - r * 0.85f, cy + r * 0.15f, cx + r * 0.85f, cy + r * 0.72f), paint)
                        canvas.drawLine(cx, cy + r * 0.45f, cx, cy - r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.42f, cx, cy - r * 0.78f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.42f, cx, cy - r * 0.78f, paint)
                        canvas.drawCircle(cx, cy - r * 0.78f, r * 0.12f, dotPaint)
                    }
                    56, 102, 103, 300 -> {
                        val polaris = Path()
                        for (i in 0 until 8) {
                            val rad = if (i % 2 == 0) r * 0.92f else r * 0.28f
                            val a = Math.toRadians((i * 45 - 90).toDouble())
                            val x = (cx + cos(a) * rad).toFloat()
                            val y = (cy + sin(a) * rad).toFloat()
                            if (i == 0) polaris.moveTo(x, y) else polaris.lineTo(x, y)
                        }
                        polaris.close()
                        if (iconCode == 102) canvas.drawPath(polaris, dotPaint)
                        canvas.drawPath(polaris, paint)
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
                    }
                    57 -> {
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, dotPaint)
                        for (i in 0 until 3) {
                            val a = Math.toRadians((i * 120).toDouble())
                            canvas.drawLine(cx, cy, (cx + cos(a) * r * 0.82f).toFloat(), (cy + sin(a) * r * 0.82f).toFloat(), paint)
                        }
                    }
                    59 -> {
                        canvas.drawCircle(cx, cy, r * 0.38f, dotPaint)
                        canvas.drawCircle(cx, cy, r * 0.78f, paint)
                    }
                    63 -> {
                        canvas.drawCircle(cx, cy, r * 0.24f, dotPaint)
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.55f, cx + r * 0.82f, cy + r * 0.55f), -30f, 150f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.55f, cx + r * 0.82f, cy + r * 0.55f), 150f, 150f, false, paint)
                        canvas.drawCircle(cx + r * 0.68f, cy - r * 0.42f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx - r * 0.68f, cy + r * 0.42f, r * 0.11f, dotPaint)
                    }
                    64 -> {
                        val hex = Path()
                        for (i in 0..5) {
                            val a = Math.toRadians((i * 60).toDouble())
                            val x = (cx + cos(a) * r * 0.84f).toFloat()
                            val y = (cy + sin(a) * r * 0.84f).toFloat()
                            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
                        }
                        hex.close()
                        canvas.drawPath(hex, paint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.15f, cx, cy - r * 0.25f, paint)
                        canvas.drawLine(cx, cy - r * 0.25f, cx + r * 0.35f, cy + r * 0.15f, paint)
                    }
                    65 -> {
                        val dOff = r * 0.52f
                        val pts = listOf(
                            cx - dOff to cy - dOff,
                            cx + dOff to cy - dOff,
                            cx - dOff to cy + dOff,
                            cx + dOff to cy + dOff
                        )
                        canvas.drawRect(cx - dOff, cy - dOff, cx + dOff, cy + dOff, secondaryPaint)
                        canvas.drawLine(cx - dOff, cy - dOff, cx + dOff, cy + dOff, paint)
                        canvas.drawLine(cx + dOff, cy - dOff, cx - dOff, cy + dOff, paint)
                        for ((px, py) in pts) {
                            canvas.drawCircle(px, py, r * 0.16f, dotPaint)
                        }
                    }
                    100 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.72f, cx + r * 0.82f, cy + r * 0.72f), r * 0.2f, r * 0.2f, secondaryPaint)
                        canvas.drawRoundRect(RectF(cx - r * 0.05f, cy - r * 0.05f, cx + r * 0.72f, cy + r * 0.62f), r * 0.15f, r * 0.15f, dotPaint)
                    }
                    101 -> {
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.82f, cx + r * 0.82f, cy + r * 0.82f), -60f, 290f, false, paint)
                        canvas.drawLine(cx, cy - r * 0.45f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.35f, cy + r * 0.2f, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    200 -> {
                        val lens = Path().apply {
                            moveTo(cx - r * 0.18f, cy - r * 0.78f); lineTo(cx + r * 0.42f, cy - r * 0.18f); lineTo(cx - r * 0.18f, cy + r * 0.42f); lineTo(cx - r * 0.78f, cy - r * 0.18f); close()
                        }
                        canvas.drawPath(lens, paint)
                        canvas.drawLine(cx + r * 0.18f, cy + r * 0.18f, cx + r * 0.78f, cy + r * 0.78f, paint)
                    }
                    201 -> {
                        canvas.drawLine(cx - r * 0.65f, cy - r * 0.25f, cx - r * 0.65f, cy + r * 0.25f, paint)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.68f, cx - r * 0.22f, cy + r * 0.68f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy - r * 0.45f, cx + r * 0.22f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.65f, cy - r * 0.78f, cx + r * 0.65f, cy + r * 0.78f, paint)
                    }
                    202 -> {
                        val wave = Path().apply {
                            moveTo(cx - r * 0.8f, cy + r * 0.45f); quadTo(cx - r * 0.3f, cy - r * 0.05f, cx, cy + r * 0.25f); quadTo(cx + r * 0.35f, cy + r * 0.55f, cx + r * 0.8f, cy + r * 0.05f)
                        }
                        canvas.drawPath(wave, paint)
                    }
                    203 -> {
                        val p1 = Path().apply {
                            moveTo(cx - r * 0.2f, cy - r * 0.78f); lineTo(cx + r * 0.38f, cy); lineTo(cx - r * 0.2f, cy + r * 0.78f); lineTo(cx - r * 0.78f, cy); close()
                        }
                        canvas.drawPath(p1, paint)
                        canvas.drawCircle(cx + r * 0.25f, cy, r * 0.45f, paint)
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
 

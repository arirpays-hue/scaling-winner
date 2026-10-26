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
        if (!isGlowing && (iconCode in 0..30 || iconCode in 100..101 || iconCode in 301..304)) {
            return IosGlassTheme.createSfSymbolDrawable(context, iconCode, tintColor, sizeDp)
        }

        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.85f * context.resources.displayMetrics.density
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isGlowing) {
                    setShadowLayer(
                        5.5f * context.resources.displayMetrics.density,
                        0f,
                        0f,
                        Color.argb(175, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                    )
                }
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(34, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                style = Paint.Style.FILL
            }
            private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(85, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                style = Paint.Style.STROKE
                strokeWidth = 1f * context.resources.displayMetrics.density
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val b = bounds
                val cx = b.exactCenterX()
                val cy = b.exactCenterY()
                val r = Math.min(b.width(), b.height()) * 0.38f

                val bgRect = RectF(cx - r * 1.18f, cy - r * 1.18f, cx + r * 1.18f, cy + r * 1.18f)
                canvas.drawRoundRect(bgRect, r * 0.35f, r * 0.35f, fillPaint)
                if (isGlowing) {
                    canvas.drawRoundRect(bgRect, r * 0.35f, r * 0.35f, borderPaint)
                }

                when (iconCode) {
                    0, 301 -> { // מחשבון רגיל / קטגוריית מתמטיקה
                        val p = Path().apply {
                            moveTo(cx, cy - r * 0.88f); lineTo(cx + r * 0.88f, cy); lineTo(cx, cy + r * 0.88f); lineTo(cx - r * 0.88f, cy); close()
                        }
                        canvas.drawPath(p, paint)
                        canvas.drawLine(cx - r * 0.45f, cy, cx + r * 0.45f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.45f, cx, cy + r * 0.45f, paint)
                    }
                    1 -> { // מחשבון מדעי
                        canvas.save(); canvas.rotate(30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.88f, cy - r * 0.36f, cx + r * 0.88f, cy + r * 0.36f), paint)
                        canvas.restore(); canvas.save(); canvas.rotate(-30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.88f, cy - r * 0.36f, cx + r * 0.88f, cy + r * 0.36f), paint)
                        canvas.restore()
                        canvas.drawCircle(cx, cy, r * 0.16f, dotPaint)
                    }
                    2, 24 -> { // מע"מ / מס מכירות
                        val p = Path().apply {
                            moveTo(cx - r * 0.6f, cy - r * 0.78f); lineTo(cx + r * 0.6f, cy - r * 0.78f); lineTo(cx + r * 0.6f, cy + r * 0.75f)
                            lineTo(cx + r * 0.3f, cy + r * 0.52f); lineTo(cx, cy + r * 0.75f); lineTo(cx - r * 0.3f, cy + r * 0.52f); lineTo(cx - r * 0.6f, cy + r * 0.75f); close()
                        }
                        canvas.drawPath(p, paint)
                        canvas.drawLine(cx - r * 0.25f, cy + r * 0.12f, cx + r * 0.25f, cy - r * 0.32f, paint)
                    }
                    3, 29, 302 -> { // הלוואה / חסכונות / קטגוריית פיננסים
                        val hex = Path()
                        for (i in 0..5) {
                            val a = Math.toRadians((i * 60 - 30).toDouble())
                            val x = (cx + cos(a) * r * 0.82f).toFloat()
                            val y = (cy + sin(a) * r * 0.82f).toFloat()
                            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
                        }
                        hex.close()
                        canvas.drawPath(hex, paint)
                        canvas.drawCircle(cx, cy, r * 0.28f, paint)
                    }
                    4, 303 -> { // המרת מרחקים / קטגוריית המרות ורכב
                        val star = Path().apply {
                            moveTo(cx, cy - r * 0.9f); lineTo(cx + r * 0.24f, cy - r * 0.24f); lineTo(cx + r * 0.9f, cy); lineTo(cx + r * 0.24f, cy + r * 0.24f)
                            lineTo(cx, cy + r * 0.9f); lineTo(cx - r * 0.24f, cy + r * 0.24f); lineTo(cx - r * 0.9f, cy); lineTo(cx - r * 0.24f, cy - r * 0.24f); close()
                        }
                        canvas.drawPath(star, paint)
                    }
                    5 -> { // המרת משקלים
                        canvas.drawArc(RectF(cx - r * 0.38f, cy - r * 0.82f, cx + r * 0.38f, cy - r * 0.1f), 180f, 180f, false, paint)
                        val body = Path().apply {
                            moveTo(cx - r * 0.52f, cy - r * 0.35f); lineTo(cx + r * 0.52f, cy - r * 0.35f); lineTo(cx + r * 0.75f, cy + r * 0.72f); lineTo(cx - r * 0.75f, cy + r * 0.72f); close()
                        }
                        canvas.drawPath(body, paint)
                    }
                    6 -> { // המרת טמפ'
                        for (i in 0..2) {
                            val a = Math.toRadians((i * 60).toDouble())
                            val dx = (cos(a) * r * 0.82f).toFloat(); val dy = (sin(a) * r * 0.82f).toFloat()
                            canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, paint)
                        }
                        canvas.drawCircle(cx, cy, r * 0.24f, paint)
                    }
                    7, 28 -> { // מחשבון מתכנתים / ממיר הקסדצימלי
                        canvas.drawRoundRect(RectF(cx - r * 0.5f, cy - r * 0.5f, cx + r * 0.5f, cy + r * 0.5f), r * 0.12f, r * 0.12f, paint)
                        canvas.drawRect(cx - r * 0.2f, cy - r * 0.2f, cx + r * 0.2f, cy + r * 0.2f, dotPaint)
                        for (pOffset in listOf(-0.25f, 0.25f)) {
                            val o = r * pOffset
                            canvas.drawLine(cx + o, cy - r * 0.85f, cx + o, cy - r * 0.5f, paint)
                            canvas.drawLine(cx + o, cy + r * 0.5f, cx + o, cy + r * 0.85f, paint)
                        }
                    }
                    8 -> { // חישוב אחוזים
                        canvas.drawArc(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), 15f, 255f, true, paint)
                    }
                    9, 25 -> { // חישוב הנחות / מחיר יחידתי
                        val gem = Path().apply {
                            moveTo(cx - r * 0.48f, cy - r * 0.72f); lineTo(cx + r * 0.48f, cy - r * 0.72f); lineTo(cx + r * 0.85f, cy - r * 0.2f)
                            lineTo(cx, cy + r * 0.82f); lineTo(cx - r * 0.85f, cy - r * 0.2f); close()
                        }
                        canvas.drawPath(gem, paint)
                        canvas.drawLine(cx - r * 0.85f, cy - r * 0.2f, cx + r * 0.85f, cy - r * 0.2f, paint)
                    }
                    10, 23 -> { // חלוקת טיפים / מחשבון תשר מתקדם
                        val glass = Path().apply {
                            moveTo(cx - r * 0.75f, cy - r * 0.68f); lineTo(cx + r * 0.75f, cy - r * 0.68f); lineTo(cx, cy + r * 0.12f); close()
                        }
                        canvas.drawPath(glass, paint)
                        canvas.drawLine(cx, cy + r * 0.12f, cx, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy + r * 0.78f, cx + r * 0.42f, cy + r * 0.78f, paint)
                    }
                    11, 27, 304 -> { // מדד BMI / ביוץ / קטגוריית בריאות וזמן
                        val heart = Path().apply {
                            moveTo(cx, cy + r * 0.78f); lineTo(cx - r * 0.82f, cy - r * 0.12f); lineTo(cx - r * 0.45f, cy - r * 0.68f)
                            lineTo(cx, cy - r * 0.28f); lineTo(cx + r * 0.45f, cy - r * 0.68f); lineTo(cx + r * 0.82f, cy - r * 0.12f); close()
                        }
                        canvas.drawPath(heart, paint)
                    }
                    12, 30 -> { // חישוב גיל / מחשבון תאריך
                        val hg = Path().apply {
                            moveTo(cx - r * 0.65f, cy - r * 0.78f); lineTo(cx + r * 0.65f, cy - r * 0.78f)
                            lineTo(cx - r * 0.65f, cy + r * 0.78f); lineTo(cx + r * 0.65f, cy + r * 0.78f); close()
                        }
                        canvas.drawPath(hg, paint)
                        canvas.drawCircle(cx, cy, r * 0.1f, dotPaint)
                    }
                    13, 26 -> { // מטבעות לייב / ממיר שעון עולמי
                        canvas.drawCircle(cx, cy, r * 0.8f, paint)
                        canvas.drawOval(RectF(cx - r * 0.38f, cy - r * 0.8f, cx + r * 0.38f, cy + r * 0.8f), paint)
                        canvas.drawLine(cx - r * 0.8f, cy, cx + r * 0.8f, cy, paint)
                    }
                    14 -> { // מחשבון BMR
                        val bolt = Path().apply {
                            moveTo(cx + r * 0.18f, cy - r * 0.88f); lineTo(cx - r * 0.55f, cy + r * 0.08f); lineTo(cx + r * 0.08f, cy + r * 0.08f)
                            lineTo(cx - r * 0.18f, cy + r * 0.88f); lineTo(cx + r * 0.55f, cy - r * 0.08f); lineTo(cx - r * 0.08f, cy - r * 0.08f); close()
                        }
                        canvas.drawPath(bolt, paint)
                    }
                    15 -> { // משוואות ריבועיות
                        val w1 = Path().apply {
                            moveTo(cx - r * 0.82f, cy + r * 0.25f)
                            cubicTo(cx - r * 0.3f, cy - r * 0.85f, cx + r * 0.3f, cy + r * 0.85f, cx + r * 0.82f, cy - r * 0.25f)
                        }
                        canvas.drawPath(w1, paint)
                    }
                    16 -> { // מחשבון שברים
                        canvas.drawLine(cx - r * 0.8f, cy, cx + r * 0.8f, cy, paint)
                        canvas.drawCircle(cx, cy - r * 0.45f, r * 0.2f, paint)
                        canvas.drawCircle(cx, cy + r * 0.45f, r * 0.2f, paint)
                    }
                    17 -> { // ריבית דריבית
                        canvas.drawRect(cx - r * 0.75f, cy + r * 0.25f, cx - r * 0.38f, cy + r * 0.78f, paint)
                        canvas.drawRect(cx - r * 0.18f, cy - r * 0.15f, cx + r * 0.18f, cy + r * 0.78f, paint)
                        canvas.drawRect(cx + r * 0.38f, cy - r * 0.65f, cx + r * 0.75f, cy + r * 0.78f, paint)
                    }
                    18, 21 -> { // המרת מהירות / יעילות שריפת דלק
                        val rocket = Path().apply {
                            moveTo(cx, cy - r * 0.88f); quadTo(cx + r * 0.65f, cy - r * 0.1f, cx + r * 0.45f, cy + r * 0.55f)
                            lineTo(cx - r * 0.45f, cy + r * 0.55f); quadTo(cx - r * 0.65f, cy - r * 0.1f, cx, cy - r * 0.88f)
                        }
                        canvas.drawPath(rocket, paint)
                    }
                    19 -> { // הגדרות
                        canvas.drawCircle(cx, cy, r * 0.35f, paint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawLine((cx + cos(a) * r * 0.42f).toFloat(), (cy + sin(a) * r * 0.42f).toFloat(), (cx + cos(a) * r * 0.85f).toFloat(), (cy + sin(a) * r * 0.85f).toFloat(), paint)
                        }
                    }
                    20 -> { // עלות דלק - משאבת דלק קריסטלית
                        canvas.drawRoundRect(RectF(cx - r * 0.55f, cy - r * 0.75f, cx + r * 0.35f, cy + r * 0.75f), r * 0.15f, r * 0.15f, paint)
                        canvas.drawRect(cx - r * 0.35f, cy - r * 0.5f, cx + r * 0.15f, cy - r * 0.1f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.3f, cx + r * 0.72f, cy + r * 0.1f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy + r * 0.1f, cx + r * 0.72f, cy + r * 0.55f, paint)
                    }
                    22 -> { // ממוצע ציונים - כובע אקדמי זוהר
                        val cap = Path().apply {
                            moveTo(cx, cy - r * 0.75f); lineTo(cx + r * 0.9f, cy - r * 0.2f); lineTo(cx, cy + r * 0.35f); lineTo(cx - r * 0.9f, cy - r * 0.2f); close()
                        }
                        canvas.drawPath(cap, paint)
                        canvas.drawLine(cx + r * 0.65f, cy - r * 0.05f, cx + r * 0.65f, cy + r * 0.65f, paint)
                    }
                    100 -> { // PiP
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.72f, cx + r * 0.35f, cy + r * 0.35f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawRoundRect(RectF(cx - r * 0.15f, cy - r * 0.15f, cx + r * 0.82f, cy + r * 0.72f), r * 0.18f, r * 0.18f, paint)
                    }
                    101 -> { // History
                        canvas.drawArc(RectF(cx - r * 0.8f, cy - r * 0.8f, cx + r * 0.8f, cy + r * 0.8f), -45f, 285f, false, paint)
                        canvas.drawLine(cx, cy - r * 0.42f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx - r * 0.35f, cy + r * 0.22f, paint)
                    }
                    200 -> { // Search
                        val lens = Path().apply {
                            moveTo(cx - r * 0.18f, cy - r * 0.78f); lineTo(cx + r * 0.42f, cy - r * 0.18f); lineTo(cx - r * 0.18f, cy + r * 0.42f); lineTo(cx - r * 0.78f, cy - r * 0.18f); close()
                        }
                        canvas.drawPath(lens, paint)
                        canvas.drawLine(cx + r * 0.18f, cy + r * 0.18f, cx + r * 0.78f, cy + r * 0.78f, paint)
                    }
                    201 -> { // Sound
                        canvas.drawLine(cx - r * 0.65f, cy - r * 0.25f, cx - r * 0.65f, cy + r * 0.25f, paint)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.68f, cx - r * 0.22f, cy + r * 0.68f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy - r * 0.45f, cx + r * 0.22f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.65f, cy - r * 0.78f, cx + r * 0.65f, cy + r * 0.78f, paint)
                    }
                    202 -> { // Brightness
                        val wave = Path().apply {
                            moveTo(cx - r * 0.8f, cy + r * 0.45f); quadTo(cx - r * 0.3f, cy - r * 0.05f, cx, cy + r * 0.25f); quadTo(cx + r * 0.35f, cy + r * 0.55f, cx + r * 0.8f, cy + r * 0.05f)
                        }
                        canvas.drawPath(wave, paint)
                    }
                    203 -> { // Opacity
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
 

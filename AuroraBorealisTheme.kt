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

object AuroraBorealisTheme {

    // --- Color Palette & Atmospheric Tokens (Aurora Borealis Spec) ---
    val EMERALD_NIGHT = Color.parseColor("#0A1C1A")
    val ARCTIC_BLUE = Color.parseColor("#07111E")
    val COSMIC_PURPLE = Color.parseColor("#1B0F2E")
    val AURORA_PRIMARY = Color.parseColor("#00F5D4") // Luminous Mint-Cyan
    val AURORA_SECONDARY = Color.parseColor("#10B981") // Ethereal Green
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

    // אפקט מתכתי זוהר לכותרת תפריט הצד: linear-gradient(180deg, #FFFFFF 0%, #70C1B3 100%)
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

    // רקע אווירתי של זוהר הצפון: Deep Emerald Night -> Midnight Arctic Blue -> Soft Cosmic Purple
    fun createAuroraAtmosphericBackground(): Drawable {
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(EMERALD_NIGHT, ARCTIC_BLUE, COSMIC_PURPLE)
        )
    }

    // עיצוב טיפוגרפי למקשים במצב "Etched & Glowing" (חרוט וזוהר בניאון ציאן)
    fun styleAuroraSymbol(tv: TextView, symbol: String, isPressed: Boolean = false) {
        val isOperator = symbol in listOf("÷", "×", "−", "+", "=")
        val isCommand = symbol in listOf("AC", "⌫", "%", "MC", "MR", "M+", "M-")

        val textColor = when {
            isPressed -> Color.WHITE
            isOperator -> EMERALD_NIGHT // Deep dark-slate (#0A1C1A) for high contrast on vibrant green
            isCommand -> AURORA_PRIMARY // Mint-Cyan (#00F5D4)
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
            // Internal Glow Pulse: drop-shadow(0 0 12px rgba(0, 245, 212, 0.9))
            tv.setShadowLayer(20f, 0f, 0f, Color.argb(235, 0, 245, 212))
        } else {
            // Etched & Glowing mode: dark inner shadow + soft backlit neon rim
            if (isOperator) {
                tv.setShadowLayer(4f, 0f, 1f, Color.argb(120, 255, 255, 255))
            } else {
                tv.setShadowLayer(8f, 0f, -1.2f, Color.argb(150, 0, 245, 212))
            }
        }
    }

    // יצירת כפתורי זכוכית לפי 3 הקטגוריות במפרט Aurora Borealis
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
                // Vibrant Aurora Green Liquid Glass: linear-gradient(135deg, rgba(0, 245, 212, 0.8), rgba(16, 185, 129, 0.6))
                Color.argb((205 + boost).coerceAtMost(255), 0, 245, 212) to
                        Color.argb((160 + boost).coerceAtMost(255), 16, 185, 129)
            }
            isCommand -> {
                // Frosted Light Ice-Grey Glass: rgba(200, 220, 230, 0.25)
                Color.argb((85 + boost).coerceAtMost(255), 200, 220, 230) to
                        Color.argb((55 + boost).coerceAtMost(255), 150, 180, 195)
            }
            else -> {
                // Number Keys (0–9): Deep Translucent Slate Glass: rgba(20, 35, 45, 0.6)
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

        // Specular Edge: 1px ultra-thin top-inner border from rgba(255,255,255,0.7) to rgba(0,245,212,0.2)
        val specularTopEdge = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(185, 255, 255, 255), Color.argb(45, 0, 245, 212))
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(radius, radius, radius, radius, radius * 0.25f, radius * 0.25f, radius * 0.25f, radius * 0.25f)
            setStroke(activity.dp(1f), Color.argb(190, 255, 255, 255))
        }

        // Internal Glow Pulse wave from center on active tap
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

    // כרטיסי זכוכית, Display Canvas (radius 28pt) ורקע דיאלוגים ותפריטים (במגירת התפריט מוסרת המסגרת הלבנה בלבד)
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

    // קפסולה מוארת לפריט פעיל בתפריט הצד (border-radius: 14pt + inner glow)
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

    // ציור אייקונים וקטוריים מוארים עבור כל התפריטים, ההגדרות, הצלילים והמחשבונים (ללא אימוג'ים)
    fun createVectorIcon(
        context: Context,
        iconCode: Int,
        tintColor: Int = AURORA_PRIMARY,
        sizeDp: Float = 22f,
        isGlowing: Boolean = true
    ): Drawable {
        if (iconCode in 0..19 || iconCode in 100..101) {
            return IosGlassTheme.createSfSymbolDrawable(context, iconCode, tintColor, sizeDp)
        }

        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.9f * context.resources.displayMetrics.density
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isGlowing) {
                    setShadowLayer(5f * context.resources.displayMetrics.density, 0f, 0f, Color.argb(150, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(38, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val b = bounds
                val cx = b.exactCenterX()
                val cy = b.exactCenterY()
                val r = Math.min(b.width(), b.height()) * 0.38f
                canvas.drawRoundRect(cx - r * 1.15f, cy - r * 1.15f, cx + r * 1.15f, cy + r * 1.15f, r * 0.45f, r * 0.45f, fillPaint)

                when (iconCode) {
                    200 -> { // זכוכית מגדלת לחיפוש (Search)
                        canvas.drawCircle(cx - r * 0.15f, cy - r * 0.15f, r * 0.48f, paint)
                        canvas.drawLine(cx + r * 0.2f, cy + r * 0.2f, cx + r * 0.7f, cy + r * 0.7f, paint)
                    }
                    201 -> { // גלי קול / צליל מערכת (Sound Wave / Speaker)
                        val p = Path().apply {
                            moveTo(cx - r * 0.65f, cy - r * 0.25f)
                            lineTo(cx - r * 0.3f, cy - r * 0.25f)
                            lineTo(cx + r * 0.1f, cy - r * 0.65f)
                            lineTo(cx + r * 0.1f, cy + r * 0.65f)
                            lineTo(cx - r * 0.3f, cy + r * 0.25f)
                            lineTo(cx - r * 0.65f, cy + r * 0.25f)
                            close()
                        }
                        canvas.drawPath(p, paint)
                        canvas.drawArc(cx - r * 0.1f, cy - r * 0.45f, cx + r * 0.55f, cy + r * 0.45f, -55f, 110f, false, paint)
                        canvas.drawArc(cx - r * 0.2f, cy - r * 0.75f, cx + r * 0.85f, cy + r * 0.75f, -55f, 110f, false, paint)
                    }
                    202 -> { // בהירות (Brightness Sun)
                        canvas.drawCircle(cx, cy, r * 0.36f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + Math.cos(a) * r * 0.55f).toFloat(),
                                (cy + Math.sin(a) * r * 0.55f).toFloat(),
                                (cx + Math.cos(a) * r * 0.82f).toFloat(),
                                (cy + Math.sin(a) * r * 0.82f).toFloat(),
                                paint
                            )
                        }
                    }
                    203 -> { // שקיפות זכוכית (Opacity Drop)
                        val p = Path().apply {
                            moveTo(cx, cy - r * 0.75f)
                            quadTo(cx + r * 0.7f, cy + r * 0.1f, cx, cy + r * 0.75f)
                            quadTo(cx - r * 0.7f, cy + r * 0.1f, cx, cy - r * 0.75f)
                        }
                        canvas.drawPath(p, paint)
                    }
                    204 -> { // מחיקה / נקה הכל (Trash / Clear)
                        canvas.drawLine(cx - r * 0.65f, cy - r * 0.45f, cx + r * 0.65f, cy - r * 0.45f, paint)
                        canvas.drawRoundRect(cx - r * 0.5f, cy - r * 0.45f, cx + r * 0.5f, cy + r * 0.7f, r * 0.15f, r * 0.15f, paint)
                    }
                    205 -> { // החלף כיוון (Swap Arrows)
                        canvas.drawLine(cx - r * 0.3f, cy - r * 0.65f, cx - r * 0.3f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.6f, cy - r * 0.3f, cx - r * 0.3f, cy - r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.3f, cy - r * 0.65f, cx + r * 0.3f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.6f, cy + r * 0.3f, cx + r * 0.3f, cy + r * 0.65f, paint)
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
 

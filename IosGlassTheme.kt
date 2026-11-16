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
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

object IosGlassTheme {

    const val LIQUID_GLASS_THEME_NAME = "Liquid Glass Pro"
    const val AURORA_THEME_NAME = "Aurora Borealis (זוהר הצפון)"
    const val MINIMALIST_THEME_NAME = "עיצוב מינימליסטי"

    var glassOpacity: Int = 185
    var glassBlurRadius: Float = 28f
    var specularIntensity: Int = 210
    var glassCornerPt: Float = 24f
    var symbolDepthMode: Int = 0
    var pressGlowIntensity: Int = 220
    var useClassicOrangeOps: Boolean = true

    fun loadIosPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        glassOpacity = prefs.getInt("ios_glassOpacity", 185)
        glassBlurRadius = prefs.getFloat("ios_glassBlurRadius", 28f)
        specularIntensity = prefs.getInt("ios_specularIntensity", 210)
        glassCornerPt = prefs.getFloat("ios_glassCornerPt", 24f)
        symbolDepthMode = prefs.getInt("ios_symbolDepthMode", 0)
        pressGlowIntensity = prefs.getInt("ios_pressGlowIntensity", 220)
        useClassicOrangeOps = prefs.getBoolean("ios_useClassicOrangeOps", true)
        AuroraBorealisTheme.loadAuroraPrefs(prefs)
        MinimalistTheme.loadMinimalistPrefs(prefs)
    }

    fun saveIosPrefs(prefs: SharedPreferences?) {
        AuroraBorealisTheme.saveAuroraPrefs(prefs)
        MinimalistTheme.saveMinimalistPrefs(prefs)
        prefs?.edit()
            ?.putInt("ios_glassOpacity", glassOpacity)
            ?.putFloat("ios_glassBlurRadius", glassBlurRadius)
            ?.putInt("ios_specularIntensity", specularIntensity)
            ?.putFloat("ios_glassCornerPt", glassCornerPt)
            ?.putInt("ios_symbolDepthMode", symbolDepthMode)
            ?.putInt("ios_pressGlowIntensity", pressGlowIntensity)
            ?.putBoolean("ios_useClassicOrangeOps", useClassicOrangeOps)
            ?.apply()
    }

    fun cleanCategoryTitle(rawTitle: String, isCleanMode: Boolean): String {
        if (!isCleanMode) return rawTitle
        return rawTitle.replace(Regex("[\\p{So}\\p{Cn}]"), "").trim()
    }

    fun formatSymbolText(symbol: String, isCleanMode: Boolean): CharSequence {
        if (!isCleanMode) return symbol
        val displaySym = when (symbol) {
            "⌫" -> "⌫"
            "-" -> "−"
            else -> symbol
        }
        if (displaySym in listOf("sin", "cos", "tan", "log", "ln", "x²", "x³", "x^y", "e")) {
            return SpannableString(displaySym).apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, displaySym.length, 0)
            }
        }
        return displaySym
    }

    fun styleSymbolTextView(tv: TextView, activity: MainActivity, symbol: String, isPressed: Boolean = false) {
        if (activity.isMinimalistMode) {
            val resolvedCol = MinimalistTheme.resolveKeypadSymbolTextColor(activity, symbol)
            tv.setTextColor(resolvedCol)
            tv.text = formatSymbolText(symbol, true)
            tv.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            return
        }
        if (activity.isAuroraMode) {
            AuroraBorealisTheme.styleAuroraSymbol(tv, symbol, isPressed)
            return
        }
        if (!activity.isIosGlassMode) return

        val isOperator = symbol in listOf("÷", "×", "−", "-", "+", "=")
        val isCommand = symbol in listOf("AC", "⌫", "%", "MC", "MR", "M+", "M-")
        val opBaseColor = when {
            MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_ACTION_1] ->
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_ACTION_1, activity.primaryColor)
            useClassicOrangeOps -> Color.parseColor("#FF9F0A")
            else -> activity.primaryColor
        }

        val hasCustomSymbolColor = MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_NUM_TEXT] ||
                MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_OP_TEXT] ||
                MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_MEM_CMD_TEXT] ||
                MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_TEXT_ICONS]

        val approxBtnBg = when {
            isOperator -> opBaseColor
            isCommand -> if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_MEM_CMD_KEYS]) {
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_MEM_CMD_KEYS, Color.rgb(180, 190, 205))
            } else Color.rgb(180, 190, 205)
            else -> if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_NUM_KEYS]) {
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_NUM_KEYS, Color.rgb(55, 64, 80))
            } else Color.rgb(55, 64, 80)
        }

        val textColor = when {
            isPressed -> MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, approxBtnBg, 2.2)
            hasCustomSymbolColor -> MinimalistTheme.resolveKeypadSymbolTextColor(activity, symbol, approxBtnBg)
            isOperator -> MinimalistTheme.ensureReadableTextOnBg(Color.argb(245, 255, 255, 255), approxBtnBg, 2.3)
            isCommand -> MinimalistTheme.ensureReadableTextOnBg(Color.parseColor("#F2F4F8"), approxBtnBg, 2.3)
            else -> MinimalistTheme.ensureReadableTextOnBg(Color.WHITE, approxBtnBg, 2.3)
        }
        tv.setTextColor(textColor)
        tv.text = formatSymbolText(symbol, true)

        if (isPressed) {
            val glowColor = if (isOperator) Color.WHITE else opBaseColor
            tv.setShadowLayer(18f, 0f, 0f, Color.argb(pressGlowIntensity, Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor)))
        } else {
            val tintGlow = if (isOperator) opBaseColor else activity.primaryColor
            if (symbolDepthMode == 0) {
                tv.setShadowLayer(3.5f, 0f, -1.2f, Color.argb(135, 0, 0, 0))
            } else {
                val shadowCol = Color.argb(95, Color.red(tintGlow), Color.green(tintGlow), Color.blue(tintGlow))
                tv.setShadowLayer(8f, 0f, 3.5f, shadowCol)
            }
        }
    }

    fun createGlassButtonDrawable(
        activity: MainActivity,
        symbol: String,
        isMem: Boolean,
        isPressed: Boolean = false
    ): Drawable {
        if (activity.isMinimalistMode) {
            return MinimalistTheme.createMinimalistKeypadButtonDrawable(activity, symbol, isPressed)
        }
        if (activity.isAuroraMode) {
            return AuroraBorealisTheme.createAuroraButtonDrawable(activity, symbol, isMem, isPressed)
        }
        val radius = activity.dp(glassCornerPt).toFloat()
        val isEqualsOrMul = symbol in listOf("=", "×")
        val isOperator = symbol in listOf("÷", "×", "−", "-", "+", "=")
        val isCommand = symbol in listOf("AC", "⌫", "%") || isMem

        val opColor = when {
            isEqualsOrMul && MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_ACTION_2] ->
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_ACTION_2, activity.primaryColor)
            MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_ACTION_1] ->
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_ACTION_1, activity.primaryColor)
            useClassicOrangeOps -> Color.parseColor("#FF9F0A")
            else -> activity.primaryColor
        }

        val cmdBaseCol = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_MEM_CMD_KEYS]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_MEM_CMD_KEYS, Color.rgb(190, 200, 215))
        } else Color.rgb(210, 218, 230)

        val numBaseCol = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_NUM_KEYS]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_NUM_KEYS, Color.rgb(72, 82, 102))
        } else Color.rgb(72, 82, 102)

        val alphaScale = (glassOpacity / 255f).coerceIn(0.25f, 1f)
        val pressBoost = if (isPressed) 55 else 0

        val (topColor, bottomColor) = when {
            isOperator -> {
                val aTop = ((215 * alphaScale).toInt() + pressBoost).coerceAtMost(255)
                val aBot = ((165 * alphaScale).toInt() + pressBoost).coerceAtMost(255)
                val r = (Color.red(opColor) + if (isPressed) 35 else 20).coerceAtMost(255)
                val g = (Color.green(opColor) + if (isPressed) 35 else 15).coerceAtMost(255)
                val b = (Color.blue(opColor) + if (isPressed) 35 else 10).coerceAtMost(255)
                Color.argb(aTop, r, g, b) to Color.argb(aBot, Color.red(opColor), Color.green(opColor), Color.blue(opColor))
            }
            isCommand -> {
                val aTop = ((145 * alphaScale).toInt() + pressBoost).coerceAtMost(255)
                val aBot = ((95 * alphaScale).toInt() + pressBoost).coerceAtMost(255)
                Color.argb(aTop, Color.red(cmdBaseCol), Color.green(cmdBaseCol), Color.blue(cmdBaseCol)) to
                        Color.argb(aBot, (Color.red(cmdBaseCol) * 0.7f).toInt(), (Color.green(cmdBaseCol) * 0.7f).toInt(), (Color.blue(cmdBaseCol) * 0.7f).toInt())
            }
            else -> {
                val aTop = ((125 * alphaScale).toInt() + pressBoost).coerceAtMost(255)
                val aBot = ((80 * alphaScale).toInt() + pressBoost).coerceAtMost(255)
                Color.argb(aTop, Color.red(numBaseCol), Color.green(numBaseCol), Color.blue(numBaseCol)) to
                        Color.argb(aBot, (Color.red(numBaseCol) * 0.55f).toInt(), (Color.green(numBaseCol) * 0.55f).toInt(), (Color.blue(numBaseCol) * 0.55f).toInt())
            }
        }

        val ambientShadow = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.argb(if (isPressed) 45 else 110, 0, 0, 0))
        }

        val defaultBorderCol = Color.argb((specularIntensity * 0.55f).toInt().coerceIn(0, 255), 255, 255, 255)
        val borderStrokeCol = MinimalistTheme.resolveBorderStrokeColor(activity, defaultBorderCol)

        val glassBody = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(topColor, bottomColor)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setStroke(activity.dp(1.2f), borderStrokeCol)
        }

        val specAlpha = (if (isPressed) (specularIntensity + 45) else specularIntensity).coerceIn(0, 255)
        val specularTopEdge = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(specAlpha, 255, 255, 255), Color.argb(0, 255, 255, 255))
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(radius, radius, radius, radius, radius * 0.25f, radius * 0.25f, radius * 0.25f, radius * 0.25f)
            setStroke(activity.dp(1f), borderStrokeCol)
        }

        val innerCoreGlow = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = radius * 1.6f
            val glowAlpha = if (isPressed) (pressGlowIntensity * 0.65f).toInt().coerceIn(0, 255) else 20
            colors = intArrayOf(Color.argb(glowAlpha, 255, 255, 255), Color.TRANSPARENT)
        }

        val layers = LayerDrawable(arrayOf(ambientShadow, glassBody, innerCoreGlow, specularTopEdge)).apply {
            val bottomInset = if (isPressed) activity.dp(1f) else activity.dp(2.5f)
            setLayerInset(1, 0, 0, 0, bottomInset)
            setLayerInset(2, activity.dp(1f), activity.dp(1f), activity.dp(1f), bottomInset + activity.dp(1f))
            setLayerInset(3, activity.dp(1.5f), activity.dp(1f), activity.dp(1.5f), activity.dp(18f))
        }

        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#66FFFFFF")), layers, null)
    }

    fun createGlassCardDrawable(activity: MainActivity, cornerDp: Float = glassCornerPt): Drawable {
        if (activity.isMinimalistMode) {
            return MinimalistTheme.createMinimalistCardDrawable(activity, cornerDp, 0)
        }
        if (activity.isAuroraMode) {
            return AuroraBorealisTheme.createAuroraCardDrawable(activity, cornerDp, cornerDp == 0f)
        }
        val isDrawer = (cornerDp == 0f)
        val radius = activity.dp(cornerDp).toFloat()
        val alphaScale = (glassOpacity / 255f).coerceIn(0.25f, 1f)

        val cardTint = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_CARDS_INPUTS]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_CARDS_INPUTS, Color.rgb(42, 50, 66))
        } else Color.rgb(42, 50, 66)

        val r = Color.red(cardTint)
        val g = Color.green(cardTint)
        val b = Color.blue(cardTint)

        val glass = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.argb((185 * alphaScale).toInt().coerceAtLeast(135), r, g, b),
                Color.argb((165 * alphaScale).toInt().coerceAtLeast(120), (r * 0.65f).toInt(), (g * 0.65f).toInt(), (b * 0.65f).toInt()),
                Color.argb((150 * alphaScale).toInt().coerceAtLeast(110), Color.red(activity.primaryColor) / 3, Color.green(activity.primaryColor) / 3, Color.blue(activity.primaryColor) / 3)
            )
        ).apply {
            cornerRadius = radius
            if (!isDrawer) {
                val specBorder = (specularIntensity * 0.65f).toInt().coerceIn(0, 255)
                val borderCol = MinimalistTheme.resolveBorderStrokeColor(activity, Color.argb(specBorder, 255, 255, 255))
                setStroke(activity.dp(1.2f), borderCol)
            }
        }

        if (isDrawer) return glass

        val topEdge = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb((specularIntensity * 0.5f).toInt(), 255, 255, 255), Color.TRANSPARENT)
        ).apply { cornerRadius = radius }

        return LayerDrawable(arrayOf(glass, topEdge)).apply {
            setLayerInset(1, activity.dp(1.5f), activity.dp(1f), activity.dp(1.5f), activity.dp(24f))
        }
    }

    fun createLiquidAmbientBackground(activity: MainActivity): Drawable {
        if (activity.isMinimalistMode) {
            return GradientDrawable().apply { setColor(MinimalistTheme.getBackgroundColor(activity)) }
        }
        if (activity.isAuroraMode) {
            return AuroraBorealisTheme.createAuroraAtmosphericBackground()
        }
        val opCol = when {
            MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_ACTION_2] ->
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_ACTION_2, activity.primaryColor)
            useClassicOrangeOps -> Color.parseColor("#FF9F0A")
            else -> activity.primaryColor
        }
        val baseBg = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_MAIN_BG]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_MAIN_BG, activity.bgColor)
        } else activity.bgColor

        val topGlow = Color.argb(
            255,
            (Color.red(baseBg) + Color.red(activity.primaryColor) / 6).coerceAtMost(255),
            (Color.green(baseBg) + Color.green(activity.primaryColor) / 6).coerceAtMost(255),
            (Color.blue(baseBg) + Color.blue(activity.primaryColor) / 5).coerceAtMost(255)
        )
        val bottomGlow = Color.argb(
            255,
            (Color.red(baseBg) + Color.red(opCol) / 7).coerceAtMost(255),
            (Color.green(baseBg) + Color.green(opCol) / 8).coerceAtMost(255),
            (Color.blue(baseBg) + Color.blue(opCol) / 8).coerceAtMost(255)
        )
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(topGlow, baseBg, bottomGlow)
        )
    }

    fun apply3DElevation(view: View, dpElevation: Float = 8f) {
        if ((view.context as? MainActivity)?.isMinimalistMode == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.elevation = 0f
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val scale = (glassBlurRadius / 28f).coerceIn(0.4f, 1.8f)
            view.elevation = dpElevation * scale * view.resources.displayMetrics.density
        }
    }

    fun applyRealtimeGlassBlur(targetView: View?, enable: Boolean) {
        if (targetView == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val radius = glassBlurRadius.coerceAtLeast(25f)
            if (enable && radius > 1f) {
                targetView.setRenderEffect(
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.MIRROR)
                )
            } else {
                targetView.setRenderEffect(null)
            }
        }
    }

    // =========================================================================
    // חבילת 95 סמלים וקטוריים ייחודיים לערכת הזכוכית (Apple SF Symbols 6 Frosted Squircle)
    // אפס כפילויות: לכל מחשבון (0..65), קטגוריה (300..311), כפתור (100..103) והגדרה (200..214) סמל ייחודי!
    // =========================================================================
    fun createSfSymbolDrawable(
        context: Context,
        categoryIndex: Int,
        tintColor: Int,
        sizeDp: Float = 22f
    ): Drawable {
        if ((context as? MainActivity)?.isMinimalistMode == true) {
            return MinimalistTheme.createMinimalistVectorIcon(context, categoryIndex, tintColor, sizeDp)
        }
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val d = context.resources.displayMetrics.density
            private val isDarkTint = MinimalistTheme.computeLuminance(tintColor) < 0.25
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.85f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (symbolDepthMode == 1 && !isDarkTint) {
                    setShadowLayer(4f * d, 0f, 1.5f, Color.argb(120, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTint) {
                    Color.argb(32, 0, 0, 0)
                } else {
                    Color.argb(42, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                }
                style = Paint.Style.FILL
            }
            private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTint) {
                    Color.argb(115, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                } else {
                    Color.argb(95, 255, 255, 255)
                }
                style = Paint.Style.STROKE
                strokeWidth = 1.0f * d
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val b = bounds
                val w = b.width().toFloat()
                val h = b.height().toFloat()
                val cx = b.left + w / 2f
                val cy = b.top + h / 2f
                val r = Math.min(w, h) * 0.38f

                val sqRect = RectF(cx - r * 1.15f, cy - r * 1.15f, cx + r * 1.15f, cy + r * 1.15f)
                canvas.drawRoundRect(sqRect, r * 0.48f, r * 0.48f, fillPaint)
                canvas.drawRoundRect(sqRect, r * 0.48f, r * 0.48f, rimPaint)

                when (categoryIndex) {
                    0 -> { // 0. מחשבון רגיל: גוף מחשבון עם מסך עליון ו-4 כפתורים
                        canvas.drawRoundRect(RectF(cx - r * 0.65f, cy - r * 0.80f, cx + r * 0.65f, cy + r * 0.80f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.40f, cy - r * 0.42f, cx + r * 0.40f, cy - r * 0.42f, paint)
                        canvas.drawCircle(cx - r * 0.26f, cy + r * 0.05f, r * 0.09f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy + r * 0.05f, r * 0.09f, dotPaint)
                        canvas.drawCircle(cx - r * 0.26f, cy + r * 0.44f, r * 0.09f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy + r * 0.44f, r * 0.09f, dotPaint)
                    }
                    1 -> { // 1. מחשבון מדעי: גל סינוס f(x) חלק על ציר אופקי
                        canvas.drawLine(cx - r * 0.78f, cy, cx + r * 0.78f, cy, rimPaint)
                        val path = Path().apply {
                            moveTo(cx - r * 0.80f, cy)
                            cubicTo(cx - r * 0.32f, cy - r * 0.92f, cx + r * 0.32f, cy + r * 0.92f, cx + r * 0.80f, cy)
                        }
                        canvas.drawPath(path, paint)
                    }
                    2 -> { // 2. מחשבון מע״מ: חשבונית מס עם שוליים משוננים וסימן %
                        val rec = Path().apply {
                            moveTo(cx - r * 0.60f, cy - r * 0.78f)
                            lineTo(cx + r * 0.60f, cy - r * 0.78f)
                            lineTo(cx + r * 0.60f, cy + r * 0.76f)
                            lineTo(cx + r * 0.28f, cy + r * 0.55f)
                            lineTo(cx, cy + r * 0.76f)
                            lineTo(cx - r * 0.28f, cy + r * 0.55f)
                            lineTo(cx - r * 0.60f, cy + r * 0.76f)
                            close()
                        }
                        canvas.drawPath(rec, paint)
                        canvas.drawLine(cx - r * 0.26f, cy + r * 0.16f, cx + r * 0.26f, cy - r * 0.30f, paint)
                        canvas.drawCircle(cx - r * 0.20f, cy - r * 0.26f, r * 0.09f, dotPaint)
                        canvas.drawCircle(cx + r * 0.20f, cy + r * 0.12f, r * 0.09f, dotPaint)
                    }
                    3 -> { // 3. מחשבון הלוואה: מבנה בנק קלאסי עם גג משולש ו-3 עמודים
                        val path = Path().apply {
                            moveTo(cx - r * 0.80f, cy - r * 0.25f)
                            lineTo(cx, cy - r * 0.80f)
                            lineTo(cx + r * 0.80f, cy - r * 0.25f)
                            close()
                        }
                        canvas.drawPath(path, paint)
                        canvas.drawLine(cx - r * 0.50f, cy - r * 0.20f, cx - r * 0.50f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx, cy - r * 0.20f, cx, cy + r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.50f, cy - r * 0.20f, cx + r * 0.50f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.70f, cx + r * 0.80f, cy + r * 0.70f, paint)
                    }
                    4 -> { // 4. המרת מרחקים: סרגל אופקי עם שנתות מדידה
                        canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.38f, cx + r * 0.85f, cy + r * 0.38f), r * 0.15f, r * 0.15f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.38f, cx - r * 0.42f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.38f, cx, cy + r * 0.12f, paint)
                        canvas.drawLine(cx + r * 0.42f, cy - r * 0.38f, cx + r * 0.42f, cy, paint)
                    }
                    5 -> { // 5. המרת משקלים: מאזני כפות מאוזנים
                        canvas.drawLine(cx, cy - r * 0.75f, cx, cy + r * 0.75f, paint)
                        canvas.drawLine(cx - r * 0.70f, cy - r * 0.35f, cx + r * 0.70f, cy - r * 0.35f, paint)
                        canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 0.05f, cx - r * 0.35f, cy + r * 0.40f), 0f, 180f, false, paint)
                        canvas.drawArc(RectF(cx + r * 0.35f, cy - r * 0.05f, cx + r * 0.85f, cy + r * 0.40f), 0f, 180f, false, paint)
                    }
                    6 -> { // 6. המרת טמפ׳: מדחום זכוכית עם כדורית ומעלה
                        canvas.drawArc(RectF(cx - r * 0.45f, cy + r * 0.05f, cx + r * 0.45f, cy + r * 0.85f), -45f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.75f, cx - r * 0.22f, cy + r * 0.20f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy - r * 0.75f, cx + r * 0.22f, cy + r * 0.20f, paint)
                        canvas.drawCircle(cx + r * 0.56f, cy - r * 0.45f, r * 0.12f, paint)
                    }
                    7 -> { // 7. מחשבון מתכנתים: סוגרי קוד < / >
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.45f, cx - r * 0.80f, cy, paint)
                        canvas.drawLine(cx - r * 0.80f, cy, cx - r * 0.35f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.45f, cx + r * 0.80f, cy, paint)
                        canvas.drawLine(cx + r * 0.80f, cy, cx + r * 0.35f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.15f, cy - r * 0.55f, cx - r * 0.15f, cy + r * 0.55f, paint)
                    }
                    8 -> { // 8. חישוב אחוזים: סמל % קלאסי
                        canvas.drawLine(cx - r * 0.60f, cy + r * 0.60f, cx + r * 0.60f, cy - r * 0.60f, paint)
                        canvas.drawCircle(cx - r * 0.40f, cy - r * 0.40f, r * 0.22f, paint)
                        canvas.drawCircle(cx + r * 0.40f, cy + r * 0.40f, r * 0.22f, paint)
                    }
                    9 -> { // 9. חישוב הנחות: תגית מחיר אלכסונית עם חור
                        val path = Path().apply {
                            moveTo(cx - r * 0.75f, cy - r * 0.75f); lineTo(cx + r * 0.10f, cy - r * 0.75f)
                            lineTo(cx + r * 0.80f, cy); lineTo(cx, cy + r * 0.80f); lineTo(cx - r * 0.75f, cy + r * 0.10f); close()
                        }
                        canvas.drawPath(path, paint)
                        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.12f, paint)
                    }
                    10 -> { // 10. חלוקת טיפים: שני מטבעות חופפים
                        canvas.drawCircle(cx - r * 0.25f, cy - r * 0.10f, r * 0.52f, paint)
                        canvas.drawCircle(cx + r * 0.30f, cy + r * 0.20f, r * 0.44f, paint)
                    }
                    11 -> { // 11. מדד BMI: קו דופק אק"ג (ECG Pulse)
                        val path = Path().apply {
                            moveTo(cx - r * 0.85f, cy); lineTo(cx - r * 0.35f, cy); lineTo(cx - r * 0.15f, cy - r * 0.65f)
                            lineTo(cx + r * 0.15f, cy + r * 0.65f); lineTo(cx + r * 0.35f, cy); lineTo(cx + r * 0.85f, cy)
                        }
                        canvas.drawPath(path, paint)
                    }
                    12 -> { // 12. חישוב גיל: עוגת יום הולדת דו-שכבתית עם נר דולק
                        canvas.drawRoundRect(RectF(cx - r * 0.74f, cy + r * 0.05f, cx + r * 0.74f, cy + r * 0.78f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawLine(cx - r * 0.74f, cy + r * 0.40f, cx + r * 0.74f, cy + r * 0.40f, paint)
                        canvas.drawLine(cx, cy + r * 0.05f, cx, cy - r * 0.45f, paint)
                        canvas.drawCircle(cx, cy - r * 0.65f, r * 0.13f, dotPaint)
                    }
                    13 -> { // 13. מטבעות לייב: גלובוס רשת קווי אורך ורוחב
                        canvas.drawCircle(cx, cy, r * 0.76f, paint)
                        canvas.drawOval(RectF(cx - r * 0.38f, cy - r * 0.76f, cx + r * 0.38f, cy + r * 0.76f), paint)
                        canvas.drawLine(cx - r * 0.76f, cy, cx + r * 0.76f, cy, paint)
                    }
                    14 -> { // 14. מחשבון BMR: להבת אנרגיה (Flame)
                        val path = Path().apply {
                            moveTo(cx, cy - r * 0.85f)
                            cubicTo(cx + r * 0.80f, cy - r * 0.10f, cx + r * 0.65f, cy + r * 0.75f, cx, cy + r * 0.85f)
                            cubicTo(cx - r * 0.65f, cy + r * 0.75f, cx - r * 0.80f, cy - r * 0.10f, cx, cy - r * 0.85f)
                        }
                        canvas.drawPath(path, paint)
                    }
                    15 -> { // 15. משוואות ריבועיות: פרבולה על ציר X
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.48f, cx + r * 0.80f, cy + r * 0.48f, paint)
                        val path = Path().apply { moveTo(cx - r * 0.65f, cy - r * 0.65f); quadTo(cx, cy + r * 1.15f, cx + r * 0.65f, cy - r * 0.65f) }
                        canvas.drawPath(path, paint)
                    }
                    16 -> { // 16. מחשבון שברים: 1/2
                        canvas.drawLine(cx - r * 0.65f, cy + r * 0.65f, cx + r * 0.65f, cy - r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.40f, cy - r * 0.65f, cx - r * 0.40f, cy - r * 0.15f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.25f, cx + r * 0.60f, cy + r * 0.25f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.60f, cx + r * 0.60f, cy + r * 0.60f, paint)
                    }
                    17 -> { // 17. ריבית דריבית: גרף צמיחה שבור עם חץ עליון
                        val path = Path().apply {
                            moveTo(cx - r * 0.75f, cy + r * 0.65f); lineTo(cx - r * 0.20f, cy + r * 0.15f)
                            lineTo(cx + r * 0.15f, cy + r * 0.35f); lineTo(cx + r * 0.75f, cy - r * 0.55f)
                        }
                        canvas.drawPath(path, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.55f, cx + r * 0.75f, cy - r * 0.55f, paint)
                        canvas.drawLine(cx + r * 0.75f, cy - r * 0.55f, cx + r * 0.75f, cy - r * 0.15f, paint)
                    }
                    18 -> { // 18. המרת מהירות: מד מהירות (Speedometer) עם מחוג
                        canvas.drawArc(RectF(cx - r * 0.80f, cy - r * 0.60f, cx + r * 0.80f, cy + r * 0.80f), 165f, 210f, false, paint)
                        canvas.drawLine(cx, cy + r * 0.20f, cx + r * 0.45f, cy - r * 0.35f, paint)
                        canvas.drawCircle(cx, cy + r * 0.20f, r * 0.11f, dotPaint)
                    }
                    19 -> { // 19. הגדרות: שני סליידרים אופקיים עם כפתורי כיוון
                        canvas.drawLine(cx - r * 0.75f, cy - r * 0.42f, cx + r * 0.75f, cy - r * 0.42f, paint)
                        canvas.drawCircle(cx - r * 0.22f, cy - r * 0.42f, r * 0.20f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.42f, cx + r * 0.75f, cy + r * 0.42f, paint)
                        canvas.drawCircle(cx + r * 0.25f, cy + r * 0.42f, r * 0.20f, paint)
                    }
                    20 -> { // 20. מחשבון עלות דלק: משאבת דלק עם צינור תדלוק
                        canvas.drawRoundRect(RectF(cx - r * 0.55f, cy - r * 0.75f, cx + r * 0.35f, cy + r * 0.75f), r * 0.15f, r * 0.15f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.30f, cx + r * 0.70f, cy + r * 0.10f, paint)
                        canvas.drawLine(cx + r * 0.70f, cy + r * 0.10f, cx + r * 0.70f, cy + r * 0.55f, paint)
                    }
                    21 -> { // 21. יעילות שריפת דלק: עלה אקולוגי (Eco Leaf) עם עורק מרכזי
                        val leaf = Path().apply {
                            moveTo(cx - r * 0.65f, cy + r * 0.65f)
                            quadTo(cx - r * 0.72f, cy - r * 0.65f, cx + r * 0.72f, cy - r * 0.72f)
                            quadTo(cx + r * 0.65f, cy + r * 0.72f, cx - r * 0.65f, cy + r * 0.65f)
                        }
                        canvas.drawPath(leaf, paint)
                        canvas.drawLine(cx - r * 0.65f, cy + r * 0.65f, cx + r * 0.35f, cy - r * 0.35f, paint)
                    }
                    22 -> { // 22. ממוצע ציונים: כובע בוגרים אקדמי עם גדיל
                        val cap = Path().apply {
                            moveTo(cx, cy - r * 0.72f); lineTo(cx + r * 0.88f, cy - r * 0.18f); lineTo(cx, cy + r * 0.35f); lineTo(cx - r * 0.88f, cy - r * 0.18f); close()
                        }
                        canvas.drawPath(cap, paint)
                        canvas.drawLine(cx + r * 0.68f, cy - r * 0.05f, cx + r * 0.68f, cy + r * 0.65f, paint)
                    }
                    23 -> { // 23. מחשבון תשר מתקדם: פעמון הגשה למסעדה (Cloche) על מגש
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.45f, cx + r * 0.74f, cy + r * 0.85f), 180f, 180f, true, paint)
                        canvas.drawCircle(cx, cy - r * 0.60f, r * 0.13f, paint)
                        canvas.drawLine(cx - r * 0.86f, cy + r * 0.42f, cx + r * 0.86f, cy + r * 0.42f, paint)
                    }
                    24 -> { // 24. מחשבון מס מכירות: שקית קניות עם ידית קשתית
                        canvas.drawRoundRect(RectF(cx - r * 0.64f, cy - r * 0.30f, cx + r * 0.64f, cy + r * 0.78f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawArc(RectF(cx - r * 0.35f, cy - r * 0.76f, cx + r * 0.35f, cy + r * 0.02f), 180f, 180f, false, paint)
                    }
                    25 -> { // 25. מחשבון מחיר יחידתי: שתי קופסאות בגדלים שונים להשוואה
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.35f, cx - r * 0.16f, cy + r * 0.55f), r * 0.12f, r * 0.12f, paint)
                        canvas.drawRoundRect(RectF(cx + r * 0.14f, cy - r * 0.68f, cx + r * 0.82f, cy + r * 0.55f), r * 0.12f, r * 0.12f, paint)
                    }
                    26 -> { // 26. ממיר שעון עולמי: פני שעון מחוגים עם נקודות שעה
                        canvas.drawCircle(cx, cy, r * 0.78f, paint)
                        canvas.drawLine(cx, cy - r * 0.45f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.36f, cy + r * 0.22f, paint)
                        canvas.drawCircle(cx, cy - r * 0.62f, r * 0.06f, dotPaint)
                        canvas.drawCircle(cx + r * 0.62f, cy, r * 0.06f, dotPaint)
                    }
                    27 -> { // 27. מחשבון ביוץ: פרח 6 עלי כותרת
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawCircle((cx + cos(a) * r * 0.48f).toFloat(), (cy + sin(a) * r * 0.48f).toFloat(), r * 0.24f, paint)
                        }
                    }
                    28 -> { // 28. ממיר הקסדצימלי: רשת סולמית # (Hex Grid)
                        canvas.drawLine(cx - r * 0.30f, cy - r * 0.74f, cx - r * 0.30f, cy + r * 0.74f, paint)
                        canvas.drawLine(cx + r * 0.30f, cy - r * 0.74f, cx + r * 0.30f, cy + r * 0.74f, paint)
                        canvas.drawLine(cx - r * 0.74f, cy - r * 0.30f, cx + r * 0.74f, cy - r * 0.30f, paint)
                        canvas.drawLine(cx - r * 0.74f, cy + r * 0.30f, cx + r * 0.74f, cy + r * 0.30f, paint)
                    }
                    29 -> { // 29. מחשבון חסכונות: יהלום מלוטש (Faceted Diamond)
                        val gem = Path().apply {
                            moveTo(cx - r * 0.46f, cy - r * 0.70f); lineTo(cx + r * 0.46f, cy - r * 0.70f)
                            lineTo(cx + r * 0.82f, cy - r * 0.16f); lineTo(cx, cy + r * 0.80f); lineTo(cx - r * 0.82f, cy - r * 0.16f); close()
                        }
                        canvas.drawPath(gem, paint)
                        canvas.drawLine(cx - r * 0.82f, cy - r * 0.16f, cx + r * 0.82f, cy - r * 0.16f, paint)
                    }
                    30 -> { // 30. מחשבון תאריך: לוח שנה עם טבעות תלייה
                        canvas.drawRoundRect(RectF(cx - r * 0.75f, cy - r * 0.65f, cx + r * 0.75f, cy + r * 0.75f), r * 0.20f, r * 0.20f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy - r * 0.20f, cx + r * 0.75f, cy - r * 0.20f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.85f, cx - r * 0.35f, cy - r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.85f, cx + r * 0.35f, cy - r * 0.45f, paint)
                    }
                    31 -> { // 31. כל 87 קטגוריות היחידות: קליבר מדידה הנדסי (Vernier Caliper)
                        canvas.drawLine(cx - r * 0.82f, cy - r * 0.35f, cx + r * 0.82f, cy - r * 0.35f, paint)
                        canvas.drawLine(cx - r * 0.82f, cy - r * 0.35f, cx - r * 0.82f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.15f, cy - r * 0.55f, cx - r * 0.15f, cy + r * 0.55f, paint)
                        canvas.drawRect(cx - r * 0.15f, cy - r * 0.55f, cx + r * 0.35f, cy - r * 0.15f, paint)
                    }
                    32 -> { // 32. אינסטלציה, צנרת, ספיקה ולחץ: צינור עם גלגל שסתום וטיפה
                        canvas.drawRoundRect(RectF(cx - r * 0.80f, cy - r * 0.18f, cx + r * 0.80f, cy + r * 0.18f), r * 0.10f, r * 0.10f, paint)
                        canvas.drawLine(cx, cy - r * 0.18f, cx, cy - r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.65f, cx + r * 0.42f, cy - r * 0.65f, paint)
                        canvas.drawCircle(cx, cy + r * 0.58f, r * 0.16f, paint)
                    }
                    33 -> { // 33. מינון תרופות, עירוי IV וסוכר: שקית עירוי עם צלב רפואי
                        canvas.drawRoundRect(RectF(cx - r * 0.52f, cy - r * 0.72f, cx + r * 0.52f, cy + r * 0.42f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.15f, cx + r * 0.24f, cy - r * 0.15f, paint)
                        canvas.drawLine(cx, cy - r * 0.38f, cx, cy + r * 0.08f, paint)
                        canvas.drawLine(cx, cy + r * 0.42f, cx, cy + r * 0.82f, paint)
                    }
                    34 -> { // 34. בנייה, בטון, ריצוף ומיזוג BTU: קובייה תלת-ממדית איזומטרית
                        val cube = Path().apply {
                            moveTo(cx, cy - r * 0.80f); lineTo(cx + r * 0.72f, cy - r * 0.36f)
                            lineTo(cx + r * 0.72f, cy + r * 0.40f); lineTo(cx, cy + r * 0.80f)
                            lineTo(cx - r * 0.72f, cy + r * 0.40f); lineTo(cx - r * 0.72f, cy - r * 0.36f); close()
                        }
                        canvas.drawPath(cube, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.36f, cx, cy + r * 0.05f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.36f, cx, cy + r * 0.05f, paint)
                        canvas.drawLine(cx, cy + r * 0.05f, cx, cy + r * 0.80f, paint)
                    }
                    35 -> { // 35. קודי צבע של נגדים: נגד חשמלי עם 3 פסי צבע
                        canvas.drawLine(cx - r * 0.90f, cy, cx - r * 0.55f, cy, paint)
                        canvas.drawLine(cx + r * 0.55f, cy, cx + r * 0.90f, cy, paint)
                        canvas.drawRoundRect(RectF(cx - r * 0.55f, cy - r * 0.34f, cx + r * 0.55f, cy + r * 0.34f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.25f, cy - r * 0.34f, cx - r * 0.25f, cy + r * 0.34f, paint)
                        canvas.drawLine(cx, cy - r * 0.34f, cx, cy + r * 0.34f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy - r * 0.34f, cx + r * 0.25f, cy + r * 0.34f, paint)
                    }
                    36 -> { // 36. מפענח קודי קבלים: קבל לוחות מקבילים
                        canvas.drawLine(cx - r * 0.85f, cy, cx - r * 0.18f, cy, paint)
                        canvas.drawLine(cx + r * 0.18f, cy, cx + r * 0.85f, cy, paint)
                        canvas.drawLine(cx - r * 0.18f, cy - r * 0.62f, cx - r * 0.18f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx + r * 0.18f, cy - r * 0.62f, cx + r * 0.18f, cy + r * 0.62f, paint)
                    }
                    37 -> { // 37. עוביי חוטים AWG/SWG: חתך כבל רב-גידי
                        canvas.drawCircle(cx, cy, r * 0.80f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, dotPaint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawCircle((cx + cos(a) * r * 0.46f).toFloat(), (cy + sin(a) * r * 0.46f).toFloat(), r * 0.13f, dotPaint)
                        }
                    }
                    38 -> { // 38. מעבדת חוק אוהם: סמל אומגה (Ω)
                        canvas.drawArc(RectF(cx - r * 0.65f, cy - r * 0.72f, cx + r * 0.65f, cy + r * 0.48f), 135f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.55f, cx - r * 0.28f, cy + r * 0.55f, paint)
                        canvas.drawLine(cx + r * 0.28f, cy + r * 0.55f, cx + r * 0.78f, cy + r * 0.55f, paint)
                    }
                    39 -> { // 39. פלס בנאים דו-צירי: מבחנת פלס עם בועה במרכז
                        canvas.drawRoundRect(RectF(cx - r * 0.86f, cy - r * 0.32f, cx + r * 0.86f, cy + r * 0.32f), r * 0.32f, r * 0.32f, paint)
                        canvas.drawLine(cx - r * 0.26f, cy - r * 0.32f, cx - r * 0.26f, cy + r * 0.32f, paint)
                        canvas.drawLine(cx + r * 0.26f, cy - r * 0.32f, cx + r * 0.26f, cy + r * 0.32f, paint)
                        canvas.drawCircle(cx, cy, r * 0.17f, dotPaint)
                    }
                    40 -> { // 40. מצפן 3D מכויל: חוגת מצפן עם מחט מעוינת
                        canvas.drawCircle(cx, cy, r * 0.80f, paint)
                        val needle = Path().apply {
                            moveTo(cx + r * 0.46f, cy - r * 0.46f); lineTo(cx + r * 0.13f, cy + r * 0.13f)
                            lineTo(cx - r * 0.46f, cy + r * 0.46f); lineTo(cx - r * 0.13f, cy - r * 0.13f); close()
                        }
                        canvas.drawPath(needle, paint)
                        canvas.drawCircle(cx, cy, r * 0.10f, dotPaint)
                    }
                    41 -> { // 41. מד-זווית דו-זרועי וסרגל: קשת מד-זווית עם זרוע
                        canvas.drawArc(RectF(cx - r * 0.80f, cy - r * 0.40f, cx + r * 0.80f, cy + r * 1.12f), 180f, 180f, true, paint)
                        canvas.drawLine(cx, cy + r * 0.35f, cx + r * 0.52f, cy - r * 0.24f, paint)
                        canvas.drawCircle(cx, cy + r * 0.35f, r * 0.11f, dotPaint)
                    }
                    42 -> { // 42. שעון עצר כרונוגרף: סטופר עם לחצן עליון
                        canvas.drawCircle(cx, cy + r * 0.10f, r * 0.70f, paint)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.80f, cx + r * 0.24f, cy - r * 0.80f, paint)
                        canvas.drawLine(cx, cy - r * 0.80f, cx, cy - r * 0.60f, paint)
                        canvas.drawLine(cx + r * 0.45f, cy - r * 0.55f, cx + r * 0.65f, cy - r * 0.72f, paint)
                        canvas.drawLine(cx, cy + r * 0.10f, cx, cy - r * 0.30f, paint)
                        canvas.drawLine(cx, cy + r * 0.10f, cx + r * 0.34f, cy + r * 0.10f, paint)
                    }
                    43 -> { // 43. פנס LED פיזי ואיתות SOS: פנס יד מאיר
                        val body = Path().apply {
                            moveTo(cx - r * 0.26f, cy + r * 0.76f); lineTo(cx + r * 0.26f, cy + r * 0.76f)
                            lineTo(cx + r * 0.26f, cy + r * 0.10f); lineTo(cx + r * 0.52f, cy - r * 0.30f)
                            lineTo(cx - r * 0.52f, cy - r * 0.30f); lineTo(cx - r * 0.26f, cy + r * 0.10f); close()
                        }
                        canvas.drawPath(body, paint)
                        canvas.drawLine(cx, cy - r * 0.50f, cx, cy - r * 0.82f, paint)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.46f, cx - r * 0.60f, cy - r * 0.75f, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.46f, cx + r * 0.60f, cy - r * 0.75f, paint)
                    }
                    44 -> { // 44. הטבלה המחזורית ואטום: מודל אטום בוהר עם מסלולים מצטלבים
                        canvas.save(); canvas.rotate(30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.84f, cy - r * 0.32f, cx + r * 0.84f, cy + r * 0.32f), paint)
                        canvas.restore(); canvas.save(); canvas.rotate(-30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.84f, cy - r * 0.32f, cx + r * 0.84f, cy + r * 0.32f), paint)
                        canvas.restore()
                        canvas.drawCircle(cx, cy, r * 0.15f, dotPaint)
                    }
                    45 -> { // 45. מתרגם ומשדר קוד מורס: תיבת שידור עם נקודה-קו-נקודה
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.60f, cx + r * 0.82f, cy + r * 0.50f), r * 0.30f, r * 0.30f, paint)
                        canvas.drawCircle(cx - r * 0.48f, cy - r * 0.05f, r * 0.11f, dotPaint)
                        canvas.drawLine(cx - r * 0.18f, cy - r * 0.05f, cx + r * 0.22f, cy - r * 0.05f, paint)
                        canvas.drawCircle(cx + r * 0.52f, cy - r * 0.05f, r * 0.11f, dotPaint)
                    }
                    46 -> { // 46. מחולל סיסמאות ומד אנטרופיה: מנעול אבטחה
                        canvas.drawRoundRect(RectF(cx - r * 0.56f, cy - r * 0.08f, cx + r * 0.56f, cy + r * 0.76f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawArc(RectF(cx - r * 0.36f, cy - r * 0.76f, cx + r * 0.36f, cy + r * 0.08f), 180f, 180f, false, paint)
                        canvas.drawCircle(cx, cy + r * 0.32f, r * 0.13f, dotPaint)
                    }
                    47 -> { // 47. גימטריה, אתב״ש וספרות רומיות: לוח ספרות רומיות I V
                        canvas.drawLine(cx - r * 0.70f, cy - r * 0.62f, cx + r * 0.70f, cy - r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.70f, cy + r * 0.62f, cx + r * 0.70f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.62f, cx - r * 0.42f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx, cy - r * 0.62f, cx + r * 0.30f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx + r * 0.60f, cy - r * 0.62f, cx + r * 0.30f, cy + r * 0.62f, paint)
                    }
                    48 -> { // 48. מתכות יקרות: שני מטילי זהב טרפזיים
                        val ingotBot = Path().apply {
                            moveTo(cx - r * 0.52f, cy + r * 0.22f); lineTo(cx + r * 0.52f, cy + r * 0.22f)
                            lineTo(cx + r * 0.78f, cy + r * 0.72f); lineTo(cx - r * 0.78f, cy + r * 0.72f); close()
                        }
                        val ingotTop = Path().apply {
                            moveTo(cx - r * 0.35f, cy - r * 0.42f); lineTo(cx + r * 0.35f, cy - r * 0.42f)
                            lineTo(cx + r * 0.55f, cy + r * 0.05f); lineTo(cx - r * 0.55f, cy + r * 0.05f); close()
                        }
                        canvas.drawPath(ingotBot, paint)
                        canvas.drawPath(ingotTop, paint)
                    }
                    49 -> { // 49. משכנתא ולוח סילוקין: בית עם גג משופע
                        val roof = Path().apply {
                            moveTo(cx - r * 0.82f, cy - r * 0.05f); lineTo(cx, cy - r * 0.80f); lineTo(cx + r * 0.82f, cy - r * 0.05f)
                        }
                        canvas.drawPath(roof, paint)
                        canvas.drawRect(cx - r * 0.56f, cy - r * 0.05f, cx + r * 0.56f, cy + r * 0.76f, paint)
                    }
                    50 -> { // 50. תשואת ROI ו-CAGR: גרף 3 עמודות עולות
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.72f, cx + r * 0.80f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx - r * 0.65f, cy + r * 0.15f, cx - r * 0.35f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx - r * 0.15f, cy - r * 0.20f, cx + r * 0.15f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx + r * 0.35f, cy - r * 0.65f, cx + r * 0.65f, cy + r * 0.72f, paint)
                    }
                    51 -> { // 51. מרווח רווח ונקודת איזון: מזוודת עסקים
                        canvas.drawRoundRect(RectF(cx - r * 0.75f, cy - r * 0.38f, cx + r * 0.75f, cy + r * 0.72f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawRect(cx - r * 0.30f, cy - r * 0.72f, cx + r * 0.30f, cy - r * 0.38f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.12f, cx + r * 0.75f, cy + r * 0.12f, paint)
                    }
                    52 -> { // 52. סטטיסטיקה ועקומת פעמון: פעמון גאוס עם ציר מרכזי
                        val bell = Path().apply {
                            moveTo(cx - r * 0.82f, cy + r * 0.62f)
                            cubicTo(cx - r * 0.34f, cy + r * 0.62f, cx - r * 0.30f, cy - r * 0.72f, cx, cy - r * 0.72f)
                            cubicTo(cx + r * 0.30f, cy - r * 0.72f, cx + r * 0.34f, cy + r * 0.62f, cx + r * 0.82f, cy + r * 0.62f)
                        }
                        canvas.drawPath(bell, paint)
                        canvas.drawLine(cx, cy - r * 0.72f, cx, cy + r * 0.62f, paint)
                    }
                    53 -> { // 53. גאומטריה ופיתגורס: משולש ישר-זווית עם סימון 90°
                        val tri = Path().apply {
                            moveTo(cx - r * 0.70f, cy + r * 0.66f); lineTo(cx + r * 0.72f, cy + r * 0.66f)
                            lineTo(cx - r * 0.70f, cy - r * 0.70f); close()
                        }
                        canvas.drawPath(tri, paint)
                        canvas.drawRect(cx - r * 0.70f, cy + r * 0.36f, cx - r * 0.40f, cy + r * 0.66f, paint)
                    }
                    54 -> { // 54. תורת המספרים וקומבינטוריקה: סמל סכום סיגמא (Σ)
                        val sigma = Path().apply {
                            moveTo(cx + r * 0.60f, cy - r * 0.70f); lineTo(cx - r * 0.60f, cy - r * 0.70f)
                            lineTo(cx + r * 0.10f, cy); lineTo(cx - r * 0.60f, cy + r * 0.70f)
                            lineTo(cx + r * 0.60f, cy + r * 0.70f)
                        }
                        canvas.drawPath(sigma, paint)
                    }
                    55 -> { // 55. ייצוא PDF/CSV וגיבוי Drive: ענן עם חץ העלאה
                        canvas.drawRoundRect(RectF(cx - r * 0.75f, cy - r * 0.25f, cx + r * 0.75f, cy + r * 0.45f), r * 0.35f, r * 0.35f, paint)
                        canvas.drawArc(RectF(cx - r * 0.40f, cy - r * 0.68f, cx + r * 0.35f, cy + r * 0.05f), 180f, 180f, false, paint)
                        canvas.drawLine(cx, cy + r * 0.75f, cx, cy - r * 0.10f, paint)
                        canvas.drawLine(cx - r * 0.22f, cy + r * 0.12f, cx, cy - r * 0.10f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy + r * 0.12f, cx, cy - r * 0.10f, paint)
                    }
                    56 -> { // 56. מרכז המועדפים וחישובים נעוצים: כוכב בתוך מדליון עגול
                        canvas.drawCircle(cx, cy, r * 0.82f, rimPaint)
                        val starPath = Path()
                        for (i in 0 until 10) {
                            val rad = if (i % 2 == 0) r * 0.68f else r * 0.30f
                            val a = Math.toRadians((i * 36 - 90).toDouble())
                            val x = (cx + cos(a) * rad).toFloat()
                            val y = (cy + sin(a) * rad).toFloat()
                            if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                        }
                        starPath.close()
                        canvas.drawPath(starPath, paint)
                    }
                    57 -> { // 57. ממירי מכניקה, כוח ומומנט: גלגל שיניים מכני 8 שיניים
                        canvas.drawCircle(cx, cy, r * 0.36f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.42f).toFloat(),
                                (cy + sin(a) * r * 0.42f).toFloat(),
                                (cx + cos(a) * r * 0.80f).toFloat(),
                                (cy + sin(a) * r * 0.80f).toFloat(),
                                paint
                            )
                        }
                    }
                    58 -> { // 58. ממירי זורמים, צמיגות וספיקה: טיפה הידרודינמית עם גל פנימי
                        val drop = Path().apply {
                            moveTo(cx, cy - r * 0.82f)
                            quadTo(cx + r * 0.74f, cy + r * 0.15f, cx, cy + r * 0.80f)
                            quadTo(cx - r * 0.74f, cy + r * 0.15f, cx, cy - r * 0.82f)
                        }
                        canvas.drawPath(drop, paint)
                        canvas.drawArc(RectF(cx - r * 0.35f, cy + r * 0.05f, cx + r * 0.35f, cy + r * 0.55f), 20f, 140f, false, paint)
                    }
                    59 -> { // 59. ממירי תרמודינמיקה וחום: 3 גלי חום אנכיים
                        for (offset in listOf(-0.38f, 0f, 0.38f)) {
                            val ox = cx + r * offset
                            val wPath = Path().apply {
                                moveTo(ox, cy + r * 0.68f)
                                quadTo(ox - r * 0.20f, cy + r * 0.20f, ox, cy)
                                quadTo(ox + r * 0.20f, cy - r * 0.20f, ox, cy - r * 0.68f)
                            }
                            canvas.drawPath(wPath, paint)
                        }
                    }
                    60 -> { // 60. ממירי חשמל, מגנטיות ושדה: ברק אנרגיה (Lightning Bolt)
                        val bolt = Path().apply {
                            moveTo(cx + r * 0.15f, cy - r * 0.84f)
                            lineTo(cx - r * 0.52f, cy + r * 0.05f)
                            lineTo(cx + r * 0.05f, cy + r * 0.05f)
                            lineTo(cx - r * 0.15f, cy + r * 0.84f)
                            lineTo(cx + r * 0.52f, cy - r * 0.05f)
                            lineTo(cx - r * 0.05f, cy - r * 0.05f)
                            close()
                        }
                        canvas.drawPath(bolt, paint)
                    }
                    61 -> { // 61. ממירי אופטיקה, אקוסטיקה וקרינה: מנסרה אופטית השוברת קרן אור
                        val prism = Path().apply {
                            moveTo(cx - r * 0.08f, cy - r * 0.68f)
                            lineTo(cx + r * 0.46f, cy + r * 0.66f)
                            lineTo(cx - r * 0.64f, cy + r * 0.66f)
                            close()
                        }
                        canvas.drawPath(prism, paint)
                        canvas.drawLine(cx - r * 0.88f, cy + r * 0.10f, cx - r * 0.32f, cy + r * 0.10f, paint)
                        canvas.drawLine(cx + r * 0.20f, cy - r * 0.05f, cx + r * 0.86f, cy - r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.26f, cy + r * 0.18f, cx + r * 0.86f, cy + r * 0.35f, paint)
                    }
                    62 -> { // 62. ממירי רפואה, כימיה ומעבדה: בקבוק מעבדה קוני (Erlenmeyer Flask)
                        val flask = Path().apply {
                            moveTo(cx - r * 0.22f, cy - r * 0.76f); lineTo(cx + r * 0.22f, cy - r * 0.76f)
                            lineTo(cx + r * 0.22f, cy - r * 0.26f); lineTo(cx + r * 0.72f, cy + r * 0.74f)
                            lineTo(cx - r * 0.72f, cy + r * 0.74f); lineTo(cx - r * 0.22f, cy - r * 0.26f); close()
                        }
                        canvas.drawPath(flask, paint)
                        canvas.drawLine(cx - r * 0.46f, cy + r * 0.28f, cx + r * 0.46f, cy + r * 0.28f, paint)
                    }
                    63 -> { // 63. ממירי תקשורת, טיפוגרפיה, חלל ובישול: כוכב לכת מוקף טבעת
                        canvas.drawCircle(cx, cy, r * 0.42f, paint)
                        canvas.save(); canvas.rotate(-25f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.88f, cy - r * 0.20f, cx + r * 0.88f, cy + r * 0.20f), paint)
                        canvas.restore()
                    }
                    64 -> { // 64. מחשבון שכר, שעות נוספות, פנסיה ונטו: שטר כסף עם מדליון
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
                        canvas.drawCircle(cx - r * 0.26f, cy - r * 0.26f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy - r * 0.26f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx - r * 0.26f, cy + r * 0.26f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy + r * 0.26f, r * 0.11f, dotPaint)
                    }
                    // --- סמלי סרגל עליון (100..103) ---
                    100 -> { // 100. חלון צף PiP
                        canvas.drawRoundRect(RectF(cx - r * 0.80f, cy - r * 0.65f, cx + r * 0.80f, cy + r * 0.65f), r * 0.20f, r * 0.20f, paint)
                        canvas.drawRoundRect(RectF(cx + r * 0.05f, cy - r * 0.05f, cx + r * 0.65f, cy + r * 0.50f), r * 0.10f, r * 0.10f, paint)
                    }
                    101 -> { // 101. היסטוריית חישובים
                        canvas.drawArc(RectF(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f), -60f, 300f, false, paint)
                        canvas.drawLine(cx, cy - r * 0.40f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.30f, cy + r * 0.20f, paint)
                    }
                    102, 103 -> { // 102/103. כוכב מועדפים בסרגל העליון
                        val starPath = Path()
                        for (i in 0 until 10) {
                            val rad = if (i % 2 == 0) r * 0.82f else r * 0.36f
                            val a = Math.toRadians((i * 36 - 90).toDouble())
                            val x = (cx + cos(a) * rad).toFloat()
                            val y = (cy + sin(a) * rad).toFloat()
                            if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
                        }
                        starPath.close()
                        if (categoryIndex == 102) canvas.drawPath(starPath, dotPaint)
                        canvas.drawPath(starPath, paint)
                    }
                    // --- סמלי הגדרות וכלים (200..214) ---
                    200 -> { // 200. חיפוש
                        canvas.drawCircle(cx - r * 0.12f, cy - r * 0.12f, r * 0.52f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.25f, cx + r * 0.75f, cy + r * 0.75f, paint)
                    }
                    201 -> { // 201. צלילי הקשה: אקוולייזר קולי
                        canvas.drawLine(cx - r * 0.60f, cy - r * 0.22f, cx - r * 0.60f, cy + r * 0.22f, paint)
                        canvas.drawLine(cx - r * 0.20f, cy - r * 0.65f, cx - r * 0.20f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.20f, cy - r * 0.42f, cx + r * 0.20f, cy + r * 0.42f, paint)
                        canvas.drawLine(cx + r * 0.60f, cy - r * 0.75f, cx + r * 0.60f, cy + r * 0.75f, paint)
                    }
                    202 -> { // 202. בהירות: שמש
                        canvas.drawCircle(cx, cy, r * 0.38f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine((cx + cos(a) * r * 0.52f).toFloat(), (cy + sin(a) * r * 0.52f).toFloat(), (cx + cos(a) * r * 0.82f).toFloat(), (cy + sin(a) * r * 0.82f).toFloat(), paint)
                        }
                    }
                    203 -> { // 203. שקיפות: שני מעגלים חופפים
                        canvas.drawCircle(cx - r * 0.22f, cy, r * 0.48f, paint)
                        canvas.drawCircle(cx + r * 0.22f, cy, r * 0.48f, paint)
                    }
                    204 -> { // 204. רטט בלחיצה: סמארטפון עם גלי רטט בצדדים
                        canvas.drawRoundRect(RectF(cx - r * 0.40f, cy - r * 0.72f, cx + r * 0.40f, cy + r * 0.72f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.35f, cx - r * 0.72f, cy + r * 0.35f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.35f, cx + r * 0.72f, cy + r * 0.35f, paint)
                    }
                    205 -> { // 205. סוג צליל: תו מוזיקלי (♪)
                        canvas.drawCircle(cx - r * 0.22f, cy + r * 0.45f, r * 0.24f, paint)
                        canvas.drawLine(cx + r * 0.02f, cy + r * 0.45f, cx + r * 0.02f, cy - r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.02f, cy - r * 0.65f, cx + r * 0.55f, cy - r * 0.40f, paint)
                    }
                    206 -> { // 206. צליל AC: רמקול עם גלי קול
                        val spk = Path().apply {
                            moveTo(cx - r * 0.65f, cy - r * 0.28f); lineTo(cx - r * 0.25f, cy - r * 0.28f)
                            lineTo(cx + r * 0.22f, cy - r * 0.68f); lineTo(cx + r * 0.22f, cy + r * 0.68f)
                            lineTo(cx - r * 0.25f, cy + r * 0.28f); lineTo(cx - r * 0.65f, cy + r * 0.28f); close()
                        }
                        canvas.drawPath(spk, paint)
                        canvas.drawArc(RectF(cx + r * 0.05f, cy - r * 0.45f, cx + r * 0.68f, cy + r * 0.45f), -50f, 100f, false, paint)
                    }
                    207 -> { // 207. אנימציות: כוכב שביט עם שובל תנועה
                        canvas.drawCircle(cx + r * 0.35f, cy - r * 0.35f, r * 0.24f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.75f, cx + r * 0.15f, cy - r * 0.15f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.30f, cx - r * 0.15f, cy - r * 0.30f, paint)
                    }
                    208 -> { // 208. מסך תמיד דולק: ירח וכוכב
                        canvas.drawArc(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), 35f, 250f, false, paint)
                        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.38f, r * 0.12f, dotPaint)
                    }
                    209 -> { // 209. צורת כפתורים: ריבוע מעוגל משולב במעגל
                        canvas.drawRoundRect(RectF(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.28f, cy + r * 0.28f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawCircle(cx + r * 0.22f, cy + r * 0.22f, r * 0.48f, paint)
                    }
                    210 -> { // 210. דיוק עשרוני: כוונת מדויקת (Crosshair Target)
                        canvas.drawCircle(cx, cy, r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.85f, cy, cx + r * 0.85f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.85f, cx, cy + r * 0.85f, paint)
                    }
                    211 -> { // 211. מצב מקצועי Pro Mode: טיל משוגר
                        val rocket = Path().apply {
                            moveTo(cx, cy - r * 0.85f)
                            quadTo(cx + r * 0.55f, cy - r * 0.10f, cx + r * 0.38f, cy + r * 0.55f)
                            lineTo(cx - r * 0.38f, cy + r * 0.55f)
                            quadTo(cx - r * 0.55f, cy - r * 0.10f, cx, cy - r * 0.85f)
                        }
                        canvas.drawPath(rocket, paint)
                        canvas.drawCircle(cx, cy - r * 0.10f, r * 0.14f, dotPaint)
                    }
                    212 -> { // 212. יד דומיננטית: כף יד פתוחה
                        canvas.drawRoundRect(RectF(cx - r * 0.48f, cy - r * 0.05f, cx + r * 0.48f, cy + r * 0.75f), r * 0.24f, r * 0.24f, paint)
                        for (ox in listOf(-0.34f, -0.12f, 0.12f, 0.34f)) {
                            canvas.drawLine(cx + r * ox, cy - r * 0.05f, cx + r * ox, cy - r * 0.68f, paint)
                        }
                    }
                    213 -> { // 213. רשימת סימון: שורות תפריט עם וי
                        for (oy in listOf(-0.42f, 0f, 0.42f)) {
                            canvas.drawLine(cx - r * 0.72f, cy + r * oy, cx + r * 0.22f, cy + r * oy, paint)
                            canvas.drawCircle(cx + r * 0.58f, cy + r * oy, r * 0.11f, dotPaint)
                        }
                    }
                    214 -> { // 214. מצב תצוגה: פלטת צבעים עם 3 נקודות גוון
                        canvas.drawCircle(cx, cy, r * 0.78f, paint)
                        canvas.drawCircle(cx - r * 0.28f, cy - r * 0.22f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy - r * 0.22f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx, cy + r * 0.30f, r * 0.14f, paint)
                    }
                    // --- סמלי 12 קטגוריות העל בתפריט הצד (300..311) - ייחודיים לחלוטין! ---
                    300 -> { // 300. סרט סימנייה (Bookmark Ribbon)
                        val bm = Path().apply {
                            moveTo(cx - r * 0.52f, cy - r * 0.78f); lineTo(cx + r * 0.52f, cy - r * 0.78f)
                            lineTo(cx + r * 0.52f, cy + r * 0.78f); lineTo(cx, cy + r * 0.38f)
                            lineTo(cx - r * 0.52f, cy + r * 0.78f); close()
                        }
                        canvas.drawPath(bm, paint)
                    }
                    301 -> { // 301. קטגוריית מתמטיקה ומדע: ארבעת סימני החשבון (+ − × =) בחלוקה רבעונית
                        val o = r * 0.45f
                        canvas.drawLine(cx - o - r * 0.20f, cy - o, cx - o + r * 0.20f, cy - o, paint)
                        canvas.drawLine(cx - o, cy - o - r * 0.20f, cx - o, cy - o + r * 0.20f, paint)
                        canvas.drawLine(cx + o - r * 0.20f, cy - o, cx + o + r * 0.20f, cy - o, paint)
                        canvas.drawLine(cx - o - r * 0.18f, cy + o - r * 0.18f, cx - o + r * 0.18f, cy + o + r * 0.18f, paint)
                        canvas.drawLine(cx - o + r * 0.18f, cy + o - r * 0.18f, cx - o - r * 0.18f, cy + o + r * 0.18f, paint)
                        canvas.drawLine(cx + o - r * 0.20f, cy + o - r * 0.10f, cx + o + r * 0.20f, cy + o - r * 0.10f, paint)
                        canvas.drawLine(cx + o - r * 0.20f, cy + o + r * 0.10f, cx + o + r * 0.20f, cy + o + r * 0.10f, paint)
                    }
                    302 -> { // 302. קטגוריית פיננסים וצרכנות: כרטיס אשראי עם פס מגנטי ושבב
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.54f, cx + r * 0.82f, cy + r * 0.54f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawLine(cx - r * 0.82f, cy - r * 0.16f, cx + r * 0.82f, cy - r * 0.16f, paint)
                        canvas.drawRect(cx - r * 0.52f, cy + r * 0.08f, cx - r * 0.18f, cy + r * 0.32f, paint)
                    }
                    303 -> { // 303. קטגוריית המרות, רכב ודלק: שני חצי המרה מעגליים (Sync Arrows)
                        canvas.drawArc(RectF(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f), -150f, 130f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f), 30f, 130f, false, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.70f, cx + r * 0.68f, cy - r * 0.36f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.70f, cx - r * 0.68f, cy + r * 0.36f, paint)
                    }
                    304 -> { // 304. קטגוריית בריאות, זמן ותאריכים: לב שלם קלאסי
                        val heart = Path().apply {
                            moveTo(cx, cy + r * 0.76f)
                            lineTo(cx - r * 0.76f, cy - r * 0.10f)
                            arcTo(RectF(cx - r * 0.76f, cy - r * 0.70f, cx, cy - r * 0.05f), 180f, 180f, false)
                            arcTo(RectF(cx, cy - r * 0.70f, cx + r * 0.76f, cy - r * 0.05f), 180f, 180f, false)
                            close()
                        }
                        canvas.drawPath(heart, paint)
                    }
                    305 -> { // 305. קטגוריית Pro 87 יחידות: מערכת צירים תלת-ממדית (X, Y, Z)
                        canvas.drawLine(cx, cy, cx, cy - r * 0.82f, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.76f, cy + r * 0.46f, paint)
                        canvas.drawLine(cx, cy, cx - r * 0.76f, cy + r * 0.46f, paint)
                        canvas.drawCircle(cx, cy, r * 0.13f, dotPaint)
                    }
                    306 -> { // 306. קטגוריית Pro הנדסה, אינסטלציה ובנייה: מחוגת שרטוט הנדסית (Drafting Compass)
                        canvas.drawCircle(cx, cy - r * 0.68f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.08f, cy - r * 0.54f, cx - r * 0.62f, cy + r * 0.78f, paint)
                        canvas.drawLine(cx + r * 0.08f, cy - r * 0.54f, cx + r * 0.62f, cy + r * 0.78f, paint)
                        canvas.drawArc(RectF(cx - r * 0.65f, cy - r * 0.15f, cx + r * 0.65f, cy + r * 0.45f), 20f, 140f, false, paint)
                    }
                    307 -> { // 307. קטגוריית Pro מעבדת אלקטרוניקה: שבב מעגל משולב (IC Microchip)
                        canvas.drawRoundRect(RectF(cx - r * 0.50f, cy - r * 0.50f, cx + r * 0.50f, cy + r * 0.50f), r * 0.12f, r * 0.12f, paint)
                        for (o in listOf(-0.26f * r, 0.26f * r)) {
                            canvas.drawLine(cx + o, cy - r * 0.85f, cx + o, cy - r * 0.50f, paint)
                            canvas.drawLine(cx + o, cy + r * 0.50f, cx + o, cy + r * 0.85f, paint)
                            canvas.drawLine(cx - r * 0.85f, cy + o, cx - r * 0.50f, cy + o, paint)
                            canvas.drawLine(cx + r * 0.50f, cy + o, cx + r * 0.85f, cy + o, paint)
                        }
                    }
                    308 -> { // 308. קטגוריית Pro חיישנים ומדידה: ג'ירוסקופ דו-טבעתי עם ציר
                        canvas.drawCircle(cx, cy, r * 0.80f, paint)
                        canvas.drawOval(RectF(cx - r * 0.80f, cy - r * 0.34f, cx + r * 0.80f, cy + r * 0.34f), paint)
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
                    }
                    309 -> { // 309. קטגוריית Pro מדע, אטום, מורס והצפנה: מיקרוסקופ / סליל כפול
                        val s1 = Path().apply {
                            moveTo(cx - r * 0.55f, cy - r * 0.78f)
                            cubicTo(cx + r * 0.70f, cy - r * 0.25f, cx - r * 0.70f, cy + r * 0.25f, cx + r * 0.55f, cy + r * 0.78f)
                        }
                        val s2 = Path().apply {
                            moveTo(cx + r * 0.55f, cy - r * 0.78f)
                            cubicTo(cx - r * 0.70f, cy - r * 0.25f, cx + r * 0.70f, cy + r * 0.25f, cx - r * 0.55f, cy + r * 0.78f)
                        }
                        canvas.drawPath(s1, paint)
                        canvas.drawPath(s2, paint)
                    }
                    310 -> { // 310. קטגוריית Pro השקעות, משכנתא ועסקים: גרף עוגה (Pie Chart) עם פלח נשלף
                        canvas.drawArc(RectF(cx - r * 0.78f, cy - r * 0.72f, cx + r * 0.66f, cy + r * 0.78f), 15f, 255f, true, paint)
                        canvas.drawArc(RectF(cx - r * 0.62f, cy - r * 0.84f, cx + r * 0.82f, cy + r * 0.60f), -75f, 75f, true, paint)
                    }
                    311 -> { // 311. קטגוריית Pro סטטיסטיקה, גאומטריה ומטריצות: סמל אינסוף (∞)
                        val inf = Path().apply {
                            moveTo(cx, cy)
                            cubicTo(cx + r * 0.45f, cy - r * 0.65f, cx + r * 0.88f, cy - r * 0.55f, cx + r * 0.88f, cy)
                            cubicTo(cx + r * 0.88f, cy + r * 0.55f, cx + r * 0.45f, cy + r * 0.65f, cx, cy)
                            cubicTo(cx - r * 0.45f, cy - r * 0.65f, cx - r * 0.88f, cy - r * 0.55f, cx - r * 0.88f, cy)
                            cubicTo(cx - r * 0.88f, cy + r * 0.55f, cx - r * 0.45f, cy + r * 0.65f, cx, cy)
                        }
                        canvas.drawPath(inf, paint)
                    }
                    else -> {
                        canvas.drawCircle(cx, cy, r * 0.55f, paint)
                        canvas.drawCircle(cx, cy, r * 0.15f, dotPaint)
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

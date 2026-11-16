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
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.9f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (symbolDepthMode == 1) {
                    setShadowLayer(4f * d, 0f, 1.5f, Color.argb(120, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(42, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                style = Paint.Style.FILL
            }
            private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(95, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 0.9f * d
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
                    0, 301 -> {
                        val o = r * 0.45f
                        canvas.drawLine(cx - o - r * 0.2f, cy - o, cx - o + r * 0.2f, cy - o, paint)
                        canvas.drawLine(cx - o, cy - o - r * 0.2f, cx - o, cy - o + r * 0.2f, paint)
                        canvas.drawLine(cx + o - r * 0.2f, cy - o, cx + o + r * 0.2f, cy - o, paint)
                        canvas.drawLine(cx - o - r * 0.18f, cy + o - r * 0.18f, cx - o + r * 0.18f, cy + o + r * 0.18f, paint)
                        canvas.drawLine(cx - o + r * 0.18f, cy + o - r * 0.18f, cx - o - r * 0.18f, cy + o + r * 0.18f, paint)
                        canvas.drawLine(cx + o - r * 0.2f, cy + o - r * 0.1f, cx + o + r * 0.2f, cy + o - r * 0.1f, paint)
                        canvas.drawLine(cx + o - r * 0.2f, cy + o + r * 0.1f, cx + o + r * 0.2f, cy + o + r * 0.1f, paint)
                    }
                    1 -> {
                        val path = Path().apply { moveTo(cx - r * 0.8f, cy); cubicTo(cx - r * 0.3f, cy - r * 0.9f, cx + r * 0.3f, cy + r * 0.9f, cx + r * 0.8f, cy) }
                        canvas.drawPath(path, paint)
                    }
                    2, 24 -> {
                        canvas.drawRoundRect(cx - r * 0.65f, cy - r * 0.8f, cx + r * 0.65f, cy + r * 0.8f, r * 0.2f, r * 0.2f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.3f, cx + r * 0.35f, cy - r * 0.3f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.2f, cx + r * 0.15f, cy + r * 0.2f, paint)
                    }
                    3, 29, 302 -> {
                        val path = Path().apply { moveTo(cx - r * 0.8f, cy - r * 0.25f); lineTo(cx, cy - r * 0.8f); lineTo(cx + r * 0.8f, cy - r * 0.25f); close() }
                        canvas.drawPath(path, paint)
                        canvas.drawLine(cx - r * 0.5f, cy - r * 0.2f, cx - r * 0.5f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx, cy - r * 0.2f, cx, cy + r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.5f, cy - r * 0.2f, cx + r * 0.5f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.8f, cy + r * 0.7f, cx + r * 0.8f, cy + r * 0.7f, paint)
                    }
                    4, 303 -> {
                        canvas.drawRoundRect(cx - r * 0.85f, cy - r * 0.4f, cx + r * 0.85f, cy + r * 0.4f, r * 0.15f, r * 0.15f, paint)
                        canvas.drawLine(cx - r * 0.4f, cy - r * 0.4f, cx - r * 0.4f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.4f, cx, cy + r * 0.1f, paint)
                        canvas.drawLine(cx + r * 0.4f, cy - r * 0.4f, cx + r * 0.4f, cy, paint)
                    }
                    5 -> {
                        canvas.drawLine(cx, cy - r * 0.75f, cx, cy + r * 0.75f, paint)
                        canvas.drawLine(cx - r * 0.7f, cy - r * 0.35f, cx + r * 0.7f, cy - r * 0.35f, paint)
                        canvas.drawArc(cx - r * 0.85f, cy - r * 0.05f, cx - r * 0.35f, cy + r * 0.4f, 0f, 180f, false, paint)
                        canvas.drawArc(cx + r * 0.35f, cy - r * 0.05f, cx + r * 0.85f, cy + r * 0.4f, 0f, 180f, false, paint)
                    }
                    6 -> {
                        canvas.drawArc(cx - r * 0.45f, cy + r * 0.05f, cx + r * 0.45f, cy + r * 0.85f, -45f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.75f, cx - r * 0.22f, cy + r * 0.2f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy - r * 0.75f, cx + r * 0.22f, cy + r * 0.2f, paint)
                    }
                    7, 28 -> {
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.45f, cx - r * 0.8f, cy, paint)
                        canvas.drawLine(cx - r * 0.8f, cy, cx - r * 0.35f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.45f, cx + r * 0.8f, cy, paint)
                        canvas.drawLine(cx + r * 0.8f, cy, cx + r * 0.35f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.15f, cy - r * 0.55f, cx - r * 0.15f, cy + r * 0.55f, paint)
                    }
                    8 -> {
                        canvas.drawLine(cx - r * 0.6f, cy + r * 0.6f, cx + r * 0.6f, cy - r * 0.6f, paint)
                        canvas.drawCircle(cx - r * 0.4f, cy - r * 0.4f, r * 0.22f, paint)
                        canvas.drawCircle(cx + r * 0.4f, cy + r * 0.4f, r * 0.22f, paint)
                    }
                    9, 25 -> {
                        val path = Path().apply {
                            moveTo(cx - r * 0.75f, cy - r * 0.75f); lineTo(cx + r * 0.1f, cy - r * 0.75f)
                            lineTo(cx + r * 0.8f, cy); lineTo(cx, cy + r * 0.8f); lineTo(cx - r * 0.75f, cy + r * 0.1f); close()
                        }
                        canvas.drawPath(path, paint)
                        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.12f, paint)
                    }
                    10, 23 -> {
                        canvas.drawCircle(cx - r * 0.25f, cy - r * 0.1f, r * 0.55f, paint)
                        canvas.drawCircle(cx + r * 0.3f, cy + r * 0.2f, r * 0.45f, paint)
                    }
                    11, 27, 304 -> {
                        val path = Path().apply {
                            moveTo(cx - r * 0.85f, cy); lineTo(cx - r * 0.35f, cy); lineTo(cx - r * 0.15f, cy - r * 0.65f)
                            lineTo(cx + r * 0.15f, cy + r * 0.65f); lineTo(cx + r * 0.35f, cy); lineTo(cx + r * 0.85f, cy)
                        }
                        canvas.drawPath(path, paint)
                    }
                    12, 30 -> {
                        canvas.drawRoundRect(cx - r * 0.75f, cy - r * 0.65f, cx + r * 0.75f, cy + r * 0.75f, r * 0.2f, r * 0.2f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy - r * 0.2f, cx + r * 0.75f, cy - r * 0.2f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.85f, cx - r * 0.35f, cy - r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.85f, cx + r * 0.35f, cy - r * 0.45f, paint)
                    }
                    13, 26 -> {
                        canvas.drawArc(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f, -150f, 120f, false, paint)
                        canvas.drawArc(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f, 30f, 120f, false, paint)
                        canvas.drawLine(cx, cy - r * 0.4f, cx, cy + r * 0.4f, paint)
                    }
                    14 -> {
                        val path = Path().apply {
                            moveTo(cx, cy - r * 0.85f)
                            cubicTo(cx + r * 0.8f, cy - r * 0.1f, cx + r * 0.65f, cy + r * 0.75f, cx, cy + r * 0.85f)
                            cubicTo(cx - r * 0.65f, cy + r * 0.75f, cx - r * 0.8f, cy - r * 0.1f, cx, cy - r * 0.85f)
                        }
                        canvas.drawPath(path, paint)
                    }
                    15 -> {
                        canvas.drawLine(cx - r * 0.8f, cy + r * 0.5f, cx + r * 0.8f, cy + r * 0.5f, paint)
                        val path = Path().apply { moveTo(cx - r * 0.65f, cy - r * 0.65f); quadTo(cx, cy + r * 1.1f, cx + r * 0.65f, cy - r * 0.65f) }
                        canvas.drawPath(path, paint)
                    }
                    16 -> {
                        canvas.drawLine(cx - r * 0.65f, cy + r * 0.65f, cx + r * 0.65f, cy - r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.4f, cy - r * 0.65f, cx - r * 0.4f, cy - r * 0.15f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.25f, cx + r * 0.6f, cy + r * 0.25f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.6f, cx + r * 0.6f, cy + r * 0.6f, paint)
                    }
                    17 -> {
                        val path = Path().apply {
                            moveTo(cx - r * 0.75f, cy + r * 0.65f); lineTo(cx - r * 0.2f, cy + r * 0.15f)
                            lineTo(cx + r * 0.15f, cy + r * 0.35f); lineTo(cx + r * 0.75f, cy - r * 0.55f)
                        }
                        canvas.drawPath(path, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.55f, cx + r * 0.75f, cy - r * 0.55f, paint)
                        canvas.drawLine(cx + r * 0.75f, cy - r * 0.55f, cx + r * 0.75f, cy - r * 0.15f, paint)
                    }
                    18, 21 -> {
                        canvas.drawArc(cx - r * 0.8f, cy - r * 0.6f, cx + r * 0.8f, cy + r * 0.8f, 165f, 210f, false, paint)
                        canvas.drawLine(cx, cy + r * 0.2f, cx + r * 0.45f, cy - r * 0.35f, paint)
                    }
                    19 -> {
                        canvas.drawLine(cx - r * 0.75f, cy - r * 0.45f, cx + r * 0.75f, cy - r * 0.45f, paint)
                        canvas.drawCircle(cx - r * 0.2f, cy - r * 0.45f, r * 0.2f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.45f, cx + r * 0.75f, cy + r * 0.45f, paint)
                        canvas.drawCircle(cx + r * 0.25f, cy + r * 0.45f, r * 0.2f, paint)
                    }
                    20 -> {
                        canvas.drawRoundRect(cx - r * 0.55f, cy - r * 0.75f, cx + r * 0.35f, cy + r * 0.75f, r * 0.15f, r * 0.15f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.3f, cx + r * 0.7f, cy + r * 0.1f, paint)
                        canvas.drawLine(cx + r * 0.7f, cy + r * 0.1f, cx + r * 0.7f, cy + r * 0.55f, paint)
                    }
                    22 -> {
                        val cap = Path().apply {
                            moveTo(cx, cy - r * 0.75f); lineTo(cx + r * 0.88f, cy - r * 0.2f); lineTo(cx, cy + r * 0.35f); lineTo(cx - r * 0.88f, cy - r * 0.2f); close()
                        }
                        canvas.drawPath(cap, paint)
                    }
                    31, 305 -> {
                        canvas.drawLine(cx - r * 0.82f, cy - r * 0.35f, cx + r * 0.82f, cy - r * 0.35f, paint)
                        canvas.drawLine(cx - r * 0.82f, cy - r * 0.35f, cx - r * 0.82f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.15f, cy - r * 0.55f, cx - r * 0.15f, cy + r * 0.55f, paint)
                        canvas.drawRect(cx - r * 0.15f, cy - r * 0.55f, cx + r * 0.35f, cy - r * 0.15f, paint)
                    }
                    32, 58, 306 -> {
                        canvas.drawRoundRect(cx - r * 0.8f, cy - r * 0.18f, cx + r * 0.8f, cy + r * 0.18f, r * 0.1f, r * 0.1f, paint)
                        canvas.drawLine(cx, cy - r * 0.18f, cx, cy - r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.65f, cx + r * 0.42f, cy - r * 0.65f, paint)
                        canvas.drawCircle(cx, cy + r * 0.58f, r * 0.16f, paint)
                    }
                    33, 62 -> {
                        canvas.drawRoundRect(cx - r * 0.52f, cy - r * 0.72f, cx + r * 0.52f, cy + r * 0.42f, r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.15f, cx + r * 0.24f, cy - r * 0.15f, paint)
                        canvas.drawLine(cx, cy - r * 0.38f, cx, cy + r * 0.08f, paint)
                        canvas.drawLine(cx, cy + r * 0.42f, cx, cy + r * 0.82f, paint)
                    }
                    34 -> {
                        val cube = Path().apply {
                            moveTo(cx, cy - r * 0.8f); lineTo(cx + r * 0.72f, cy - r * 0.36f)
                            lineTo(cx + r * 0.72f, cy + r * 0.4f); lineTo(cx, cy + r * 0.8f)
                            lineTo(cx - r * 0.72f, cy + r * 0.4f); lineTo(cx - r * 0.72f, cy - r * 0.36f); close()
                        }
                        canvas.drawPath(cube, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.36f, cx, cy + r * 0.05f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.36f, cx, cy + r * 0.05f, paint)
                        canvas.drawLine(cx, cy + r * 0.05f, cx, cy + r * 0.8f, paint)
                    }
                    35 -> {
                        canvas.drawLine(cx - r * 0.9f, cy, cx - r * 0.55f, cy, paint)
                        canvas.drawLine(cx + r * 0.55f, cy, cx + r * 0.9f, cy, paint)
                        canvas.drawRoundRect(cx - r * 0.55f, cy - r * 0.34f, cx + r * 0.55f, cy + r * 0.34f, r * 0.14f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.25f, cy - r * 0.34f, cx - r * 0.25f, cy + r * 0.34f, paint)
                        canvas.drawLine(cx, cy - r * 0.34f, cx, cy + r * 0.34f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy - r * 0.34f, cx + r * 0.25f, cy + r * 0.34f, paint)
                    }
                    36 -> {
                        canvas.drawLine(cx - r * 0.85f, cy, cx - r * 0.18f, cy, paint)
                        canvas.drawLine(cx + r * 0.18f, cy, cx + r * 0.85f, cy, paint)
                        canvas.drawLine(cx - r * 0.18f, cy - r * 0.62f, cx - r * 0.18f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx + r * 0.18f, cy - r * 0.62f, cx + r * 0.18f, cy + r * 0.62f, paint)
                    }
                    37 -> {
                        canvas.drawCircle(cx, cy, r * 0.8f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, dotPaint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawCircle((cx + cos(a) * r * 0.46f).toFloat(), (cy + sin(a) * r * 0.46f).toFloat(), r * 0.13f, dotPaint)
                        }
                    }
                    38, 60, 307 -> {
                        canvas.drawArc(cx - r * 0.65f, cy - r * 0.72f, cx + r * 0.65f, cy + r * 0.48f, 135f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.55f, cx - r * 0.28f, cy + r * 0.55f, paint)
                        canvas.drawLine(cx + r * 0.28f, cy + r * 0.55f, cx + r * 0.78f, cy + r * 0.55f, paint)
                    }
                    39 -> {
                        canvas.drawRoundRect(cx - r * 0.86f, cy - r * 0.32f, cx + r * 0.86f, cy + r * 0.32f, r * 0.32f, r * 0.32f, paint)
                        canvas.drawLine(cx - r * 0.26f, cy - r * 0.32f, cx - r * 0.26f, cy + r * 0.32f, paint)
                        canvas.drawLine(cx + r * 0.26f, cy - r * 0.32f, cx + r * 0.26f, cy + r * 0.32f, paint)
                        canvas.drawCircle(cx, cy, r * 0.17f, dotPaint)
                    }
                    40, 308 -> {
                        canvas.drawCircle(cx, cy, r * 0.8f, paint)
                        val needle = Path().apply {
                            moveTo(cx + r * 0.46f, cy - r * 0.46f)
                            lineTo(cx + r * 0.13f, cy + r * 0.13f)
                            lineTo(cx - r * 0.46f, cy + r * 0.46f)
                            lineTo(cx - r * 0.13f, cy - r * 0.13f)
                            close()
                        }
                        canvas.drawPath(needle, paint)
                        canvas.drawCircle(cx, cy, r * 0.1f, dotPaint)
                    }
                    41 -> {
                        canvas.drawArc(cx - r * 0.8f, cy - r * 0.4f, cx + r * 0.8f, cy + r * 1.12f, 180f, 180f, true, paint)
                        canvas.drawLine(cx, cy + r * 0.35f, cx + r * 0.52f, cy - r * 0.24f, paint)
                        canvas.drawCircle(cx, cy + r * 0.35f, r * 0.11f, dotPaint)
                    }
                    42 -> {
                        canvas.drawCircle(cx, cy + r * 0.1f, r * 0.7f, paint)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.8f, cx + r * 0.24f, cy - r * 0.8f, paint)
                        canvas.drawLine(cx, cy - r * 0.8f, cx, cy - r * 0.6f, paint)
                        canvas.drawLine(cx + r * 0.45f, cy - r * 0.55f, cx + r * 0.65f, cy - r * 0.72f, paint)
                        canvas.drawLine(cx, cy + r * 0.1f, cx, cy - r * 0.3f, paint)
                        canvas.drawLine(cx, cy + r * 0.1f, cx + r * 0.34f, cy + r * 0.1f, paint)
                    }
                    43, 61 -> {
                        val body = Path().apply {
                            moveTo(cx - r * 0.26f, cy + r * 0.76f); lineTo(cx + r * 0.26f, cy + r * 0.76f)
                            lineTo(cx + r * 0.26f, cy + r * 0.1f); lineTo(cx + r * 0.52f, cy - r * 0.3f)
                            lineTo(cx - r * 0.52f, cy - r * 0.3f); lineTo(cx - r * 0.26f, cy + r * 0.1f); close()
                        }
                        canvas.drawPath(body, paint)
                        canvas.drawLine(cx, cy - r * 0.5f, cx, cy - r * 0.82f, paint)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.46f, cx - r * 0.6f, cy - r * 0.75f, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.46f, cx + r * 0.6f, cy - r * 0.75f, paint)
                    }
                    44, 309 -> {
                        canvas.drawRoundRect(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f, r * 0.16f, r * 0.16f, paint)
                        canvas.drawLine(cx - r * 0.45f, cy - r * 0.42f, cx - r * 0.15f, cy - r * 0.42f, paint)
                        canvas.drawLine(cx - r * 0.3f, cy + r * 0.35f, cx, cy - r * 0.2f, paint)
                        canvas.drawLine(cx, cy - r * 0.2f, cx + r * 0.3f, cy + r * 0.35f, paint)
                        canvas.drawLine(cx - r * 0.2f, cy + r * 0.15f, cx + r * 0.2f, cy + r * 0.15f, paint)
                    }
                    45 -> {
                        canvas.drawRoundRect(cx - r * 0.82f, cy - r * 0.6f, cx + r * 0.82f, cy + r * 0.5f, r * 0.3f, r * 0.3f, paint)
                        canvas.drawCircle(cx - r * 0.48f, cy - r * 0.05f, r * 0.11f, dotPaint)
                        canvas.drawLine(cx - r * 0.18f, cy - r * 0.05f, cx + r * 0.22f, cy - r * 0.05f, paint)
                        canvas.drawCircle(cx + r * 0.52f, cy - r * 0.05f, r * 0.11f, dotPaint)
                    }
                    46 -> {
                        canvas.drawRoundRect(cx - r * 0.56f, cy - r * 0.08f, cx + r * 0.56f, cy + r * 0.76f, r * 0.16f, r * 0.16f, paint)
                        canvas.drawArc(cx - r * 0.36f, cy - r * 0.76f, cx + r * 0.36f, cy + r * 0.08f, 180f, 180f, false, paint)
                        canvas.drawCircle(cx, cy + r * 0.32f, r * 0.13f, dotPaint)
                    }
                    47 -> {
                        canvas.drawLine(cx - r * 0.7f, cy - r * 0.62f, cx + r * 0.7f, cy - r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.7f, cy + r * 0.62f, cx + r * 0.7f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.62f, cx - r * 0.42f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx, cy - r * 0.62f, cx + r * 0.3f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx + r * 0.6f, cy - r * 0.62f, cx + r * 0.3f, cy + r * 0.62f, paint)
                    }
                    48 -> {
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
                    49 -> {
                        val roof = Path().apply {
                            moveTo(cx - r * 0.82f, cy - r * 0.05f); lineTo(cx, cy - r * 0.8f); lineTo(cx + r * 0.82f, cy - r * 0.05f)
                        }
                        canvas.drawPath(roof, paint)
                        canvas.drawRect(cx - r * 0.56f, cy - r * 0.05f, cx + r * 0.56f, cy + r * 0.76f, paint)
                    }
                    50, 310 -> {
                        canvas.drawLine(cx - r * 0.8f, cy + r * 0.72f, cx + r * 0.8f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx - r * 0.65f, cy + r * 0.15f, cx - r * 0.35f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx - r * 0.15f, cy - r * 0.2f, cx + r * 0.15f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx + r * 0.35f, cy - r * 0.65f, cx + r * 0.65f, cy + r * 0.72f, paint)
                    }
                    51 -> {
                        canvas.drawRoundRect(cx - r * 0.75f, cy - r * 0.38f, cx + r * 0.75f, cy + r * 0.72f, r * 0.16f, r * 0.16f, paint)
                        canvas.drawRect(cx - r * 0.3f, cy - r * 0.72f, cx + r * 0.3f, cy - r * 0.38f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.12f, cx + r * 0.75f, cy + r * 0.12f, paint)
                    }
                    52 -> {
                        val bell = Path().apply {
                            moveTo(cx - r * 0.82f, cy + r * 0.62f)
                            cubicTo(cx - r * 0.34f, cy + r * 0.62f, cx - r * 0.3f, cy - r * 0.72f, cx, cy - r * 0.72f)
                            cubicTo(cx + r * 0.3f, cy - r * 0.72f, cx + r * 0.34f, cy + r * 0.62f, cx + r * 0.82f, cy + r * 0.62f)
                        }
                        canvas.drawPath(bell, paint)
                        canvas.drawLine(cx, cy - r * 0.72f, cx, cy + r * 0.62f, paint)
                    }
                    53 -> {
                        val tri = Path().apply {
                            moveTo(cx - r * 0.7f, cy + r * 0.66f); lineTo(cx + r * 0.72f, cy + r * 0.66f)
                            lineTo(cx - r * 0.7f, cy - r * 0.7f); close()
                        }
                        canvas.drawPath(tri, paint)
                        canvas.drawRect(cx - r * 0.7f, cy + r * 0.36f, cx - r * 0.4f, cy + r * 0.66f, paint)
                    }
                    54, 311 -> {
                        val sigma = Path().apply {
                            moveTo(cx + r * 0.6f, cy - r * 0.7f); lineTo(cx - r * 0.6f, cy - r * 0.7f)
                            lineTo(cx + r * 0.1f, cy); lineTo(cx - r * 0.6f, cy + r * 0.7f)
                            lineTo(cx + r * 0.6f, cy + r * 0.7f)
                        }
                        canvas.drawPath(sigma, paint)
                    }
                    55 -> {
                        canvas.drawRoundRect(cx - r * 0.75f, cy - r * 0.25f, cx + r * 0.75f, cy + r * 0.45f, r * 0.35f, r * 0.35f, paint)
                        canvas.drawArc(cx - r * 0.4f, cy - r * 0.68f, cx + r * 0.35f, cy + r * 0.05f, 180f, 180f, false, paint)
                        canvas.drawLine(cx, cy + r * 0.75f, cx, cy - r * 0.1f, paint)
                        canvas.drawLine(cx - r * 0.22f, cy + r * 0.12f, cx, cy - r * 0.1f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy + r * 0.12f, cx, cy - r * 0.1f, paint)
                    }
                    56, 102, 103, 300 -> {
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
                    57 -> {
                        canvas.drawCircle(cx, cy, r * 0.36f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.42f).toFloat(),
                                (cy + sin(a) * r * 0.42f).toFloat(),
                                (cx + cos(a) * r * 0.8f).toFloat(),
                                (cy + sin(a) * r * 0.8f).toFloat(),
                                paint
                            )
                        }
                    }
                    59 -> {
                        for (offset in listOf(-0.38f, 0f, 0.38f)) {
                            val ox = cx + r * offset
                            val wPath = Path().apply {
                                moveTo(ox, cy + r * 0.68f)
                                quadTo(ox - r * 0.2f, cy + r * 0.2f, ox, cy)
                                quadTo(ox + r * 0.2f, cy - r * 0.2f, ox, cy - r * 0.68f)
                            }
                            canvas.drawPath(wPath, paint)
                        }
                    }
                    63 -> {
                        canvas.drawCircle(cx, cy, r * 0.42f, paint)
                        canvas.save(); canvas.rotate(-25f, cx, cy)
                        canvas.drawOval(cx - r * 0.88f, cy - r * 0.2f, cx + r * 0.88f, cy + r * 0.2f, paint)
                        canvas.restore()
                    }
                    64 -> {
                        canvas.drawRoundRect(cx - r * 0.82f, cy - r * 0.48f, cx + r * 0.82f, cy + r * 0.48f, r * 0.14f, r * 0.14f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                    }
                    65 -> {
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.7f, cx - r * 0.44f, cy - r * 0.7f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.7f, cx - r * 0.72f, cy + r * 0.7f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy + r * 0.7f, cx - r * 0.44f, cy + r * 0.7f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.7f, cx + r * 0.44f, cy - r * 0.7f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.7f, cx + r * 0.72f, cy + r * 0.7f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy + r * 0.7f, cx + r * 0.44f, cy + r * 0.7f, paint)
                        canvas.drawCircle(cx - r * 0.26f, cy - r * 0.26f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy - r * 0.26f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx - r * 0.26f, cy + r * 0.26f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy + r * 0.26f, r * 0.11f, dotPaint)
                    }
                    100 -> {
                        canvas.drawRoundRect(cx - r * 0.8f, cy - r * 0.65f, cx + r * 0.8f, cy + r * 0.65f, r * 0.2f, r * 0.2f, paint)
                        canvas.drawRoundRect(cx + r * 0.05f, cy - r * 0.05f, cx + r * 0.65f, cy + r * 0.5f, r * 0.1f, r * 0.1f, paint)
                    }
                    101 -> {
                        canvas.drawArc(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f, -60f, 300f, false, paint)
                        canvas.drawLine(cx, cy - r * 0.4f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.3f, cy + r * 0.2f, paint)
                    }
                    200 -> {
                        canvas.drawCircle(cx - r * 0.12f, cy - r * 0.12f, r * 0.52f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.25f, cx + r * 0.75f, cy + r * 0.75f, paint)
                    }
                    201 -> {
                        canvas.drawLine(cx - r * 0.6f, cy - r * 0.22f, cx - r * 0.6f, cy + r * 0.22f, paint)
                        canvas.drawLine(cx - r * 0.2f, cy - r * 0.65f, cx - r * 0.2f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.2f, cy - r * 0.42f, cx + r * 0.2f, cy + r * 0.42f, paint)
                        canvas.drawLine(cx + r * 0.6f, cy - r * 0.75f, cx + r * 0.6f, cy + r * 0.75f, paint)
                    }
                    202 -> {
                        canvas.drawCircle(cx, cy, r * 0.38f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine((cx + cos(a) * r * 0.52f).toFloat(), (cy + sin(a) * r * 0.52f).toFloat(), (cx + cos(a) * r * 0.82f).toFloat(), (cy + sin(a) * r * 0.82f).toFloat(), paint)
                        }
                    }
                    203 -> {
                        canvas.drawCircle(cx - r * 0.22f, cy, r * 0.48f, paint)
                        canvas.drawCircle(cx + r * 0.22f, cy, r * 0.48f, paint)
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

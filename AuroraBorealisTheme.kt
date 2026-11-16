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

    var EMERALD_NIGHT = Color.parseColor("#0A1C1A")
    var ARCTIC_BLUE = Color.parseColor("#07111E")
    var COSMIC_PURPLE = Color.parseColor("#1B0F2E")
    var AURORA_PRIMARY = Color.parseColor("#00F5D4")
    var AURORA_SECONDARY = Color.parseColor("#10B981")
    var EXPRESSION_CYAN_GREY = Color.parseColor("#A0B2C6")

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
        EMERALD_NIGHT = prefs.getInt("aurora_col_bg1", Color.parseColor("#0A1C1A"))
        ARCTIC_BLUE = prefs.getInt("aurora_col_bg2", Color.parseColor("#07111E"))
        COSMIC_PURPLE = prefs.getInt("aurora_col_bg3", Color.parseColor("#1B0F2E"))
        AURORA_PRIMARY = prefs.getInt("aurora_col_primary", Color.parseColor("#00F5D4"))
        AURORA_SECONDARY = prefs.getInt("aurora_col_secondary", Color.parseColor("#10B981"))
    }

    fun saveAuroraPrefs(prefs: SharedPreferences?) {
        prefs?.edit()
            ?.putInt("aurora_opacity", auroraOpacity)
            ?.putFloat("aurora_blur", auroraBlurRadius)
            ?.putInt("aurora_glow", auroraGlowIntensity)
            ?.putFloat("aurora_corner", auroraCornerPt)
            ?.putInt("aurora_col_bg1", EMERALD_NIGHT)
            ?.putInt("aurora_col_bg2", ARCTIC_BLUE)
            ?.putInt("aurora_col_bg3", COSMIC_PURPLE)
            ?.putInt("aurora_col_primary", AURORA_PRIMARY)
            ?.putInt("aurora_col_secondary", AURORA_SECONDARY)
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
                    tv.setTextColor(MinimalistTheme.LOGO_TEXT_COLOR)
                    tv.invalidate()
                    return@post
                }
                if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_TEXT]) {
                    tv.paint.shader = null
                    tv.setTextColor(MinimalistTheme.LOGO_TEXT_COLOR)
                    tv.setShadowLayer(8f, 0f, 2f, Color.argb(90, 0, 0, 0))
                    tv.invalidate()
                    return@post
                }
                val bottomCol = when {
                    isAurora -> AURORA_PRIMARY
                    isLiquidGlass -> Color.parseColor("#A0AAB8")
                    else -> Color.WHITE
                }
                if (isAurora || isLiquidGlass) {
                    tv.paint.shader = LinearGradient(
                        0f, 0f, 0f, tv.height.toFloat(),
                        Color.WHITE, bottomCol, Shader.TileMode.CLAMP
                    )
                    if (isAurora) {
                        tv.setShadowLayer(
                            14f, 0f, 0f,
                            Color.argb(120, Color.red(AURORA_PRIMARY), Color.green(AURORA_PRIMARY), Color.blue(AURORA_PRIMARY))
                        )
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
        val bg1 = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_MAIN_BG]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_MAIN_BG, EMERALD_NIGHT)
        } else EMERALD_NIGHT
        return GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(bg1, ARCTIC_BLUE, COSMIC_PURPLE)
        )
    }

    fun styleAuroraSymbol(tv: TextView, symbol: String, isPressed: Boolean = false) {
        val activity = tv.context as? MainActivity
        val isOperator = symbol in listOf("÷", "×", "−", "-", "+", "=")
        val isCommand = symbol in listOf("AC", "⌫", "%", "MC", "MR", "M+", "M-")

        val approxBtnBg = when {
            isOperator -> AURORA_PRIMARY
            isCommand -> if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_MEM_CMD_KEYS]) {
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_MEM_CMD_KEYS, Color.rgb(160, 195, 210))
            } else Color.rgb(45, 70, 85)
            else -> if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_NUM_KEYS]) {
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_NUM_KEYS, Color.rgb(24, 42, 54))
            } else Color.rgb(24, 42, 54)
        }

        val hasCustomSymbolColor = MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_NUM_TEXT] ||
                MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_OP_TEXT] ||
                MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_MEM_CMD_TEXT] ||
                MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_TEXT_ICONS]

        val rawColor = when {
            isPressed -> Color.WHITE
            hasCustomSymbolColor && activity != null ->
                MinimalistTheme.resolveKeypadSymbolTextColor(activity, symbol, approxBtnBg)
            isOperator -> EMERALD_NIGHT
            isCommand -> AURORA_PRIMARY
            else -> Color.WHITE
        }
        val safeTextColor = MinimalistTheme.ensureReadableTextOnBg(rawColor, approxBtnBg, 2.3)
        tv.setTextColor(safeTextColor)

        val displaySym = if (symbol == "-") "−" else symbol
        if (displaySym in listOf("sin", "cos", "tan", "log", "ln", "x²", "x³", "x^y", "e")) {
            tv.text = SpannableString(displaySym).apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, displaySym.length, 0)
            }
        } else {
            tv.text = displaySym
        }

        val glowCol = AURORA_PRIMARY
        if (isPressed) {
            tv.setShadowLayer(20f, 0f, 0f, Color.argb(235, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)))
        } else {
            if (isOperator) {
                tv.setShadowLayer(4f, 0f, 1f, Color.argb(120, 255, 255, 255))
            } else {
                tv.setShadowLayer(8f, 0f, -1.2f, Color.argb(150, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)))
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
        val isEqualsOrMul = symbol in listOf("=", "×")
        val isOperator = symbol in listOf("÷", "×", "−", "-", "+", "=")
        val isCommand = symbol in listOf("AC", "⌫", "%") || isMem
        val boost = if (isPressed) 50 else 0

        val opPrimary = when {
            isEqualsOrMul && MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_ACTION_2] ->
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_ACTION_2, AURORA_SECONDARY)
            MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_ACTION_1] ->
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_ACTION_1, AURORA_PRIMARY)
            else -> AURORA_PRIMARY
        }
        val opSecondary = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_ACTION_2]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_ACTION_2, AURORA_SECONDARY)
        } else AURORA_SECONDARY

        val cmdCol = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_MEM_CMD_KEYS]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_MEM_CMD_KEYS, Color.rgb(180, 210, 225))
        } else Color.rgb(200, 220, 230)

        val numCol = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_NUM_KEYS]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_NUM_KEYS, Color.rgb(24, 42, 54))
        } else Color.rgb(24, 42, 54)

        val (topColor, bottomColor) = when {
            isOperator -> {
                Color.argb((205 + boost).coerceAtMost(255), Color.red(opPrimary), Color.green(opPrimary), Color.blue(opPrimary)) to
                        Color.argb((160 + boost).coerceAtMost(255), Color.red(opSecondary), Color.green(opSecondary), Color.blue(opSecondary))
            }
            isCommand -> {
                Color.argb((105 + boost).coerceAtMost(255), Color.red(cmdCol), Color.green(cmdCol), Color.blue(cmdCol)) to
                        Color.argb((65 + boost).coerceAtMost(255), (Color.red(cmdCol) * 0.75f).toInt(), (Color.green(cmdCol) * 0.75f).toInt(), (Color.blue(cmdCol) * 0.75f).toInt())
            }
            else -> {
                Color.argb((175 + boost).coerceAtMost(255), Color.red(numCol), Color.green(numCol), Color.blue(numCol)) to
                        Color.argb((140 + boost).coerceAtMost(255), (Color.red(numCol) * 0.65f).toInt(), (Color.green(numCol) * 0.65f).toInt(), (Color.blue(numCol) * 0.65f).toInt())
            }
        }

        val ambientShadow = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.argb(if (isPressed) 50 else 125, 0, 0, 0))
        }

        val defaultBorderCol = Color.argb(if (isPressed) 220 else 110, Color.red(AURORA_PRIMARY), Color.green(AURORA_PRIMARY), Color.blue(AURORA_PRIMARY))
        val borderStrokeCol = MinimalistTheme.resolveBorderStrokeColor(activity, defaultBorderCol)

        val glassBody = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(topColor, bottomColor)
        ).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setStroke(activity.dp(1.2f), borderStrokeCol)
        }

        val specularTopEdge = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(185, 255, 255, 255), Color.argb(45, Color.red(AURORA_PRIMARY), Color.green(AURORA_PRIMARY), Color.blue(AURORA_PRIMARY)))
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
            colors = intArrayOf(Color.argb(alpha, Color.red(AURORA_PRIMARY), Color.green(AURORA_PRIMARY), Color.blue(AURORA_PRIMARY)), Color.TRANSPARENT)
        }

        val layers = LayerDrawable(arrayOf(ambientShadow, glassBody, radialPulse, specularTopEdge)).apply {
            val bottomInset = if (isPressed) activity.dp(1f) else activity.dp(2.5f)
            setLayerInset(1, 0, 0, 0, bottomInset)
            setLayerInset(2, activity.dp(1f), activity.dp(1f), activity.dp(1f), bottomInset + activity.dp(1f))
            setLayerInset(3, activity.dp(1.5f), activity.dp(1f), activity.dp(1.5f), activity.dp(18f))
        }

        val rippleCol = Color.argb(102, Color.red(AURORA_PRIMARY), Color.green(AURORA_PRIMARY), Color.blue(AURORA_PRIMARY))
        return RippleDrawable(ColorStateList.valueOf(rippleCol), layers, null)
    }

    fun createAuroraCardDrawable(activity: MainActivity, cornerDp: Float = 28f, isDrawer: Boolean = false): Drawable {
        if (activity.isMinimalistMode) {
            return MinimalistTheme.createMinimalistCardDrawable(activity, cornerDp, 0)
        }
        val radius = if (isDrawer) 0f else activity.dp(cornerDp).toFloat()
        val baseCard = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_CARDS_INPUTS]) {
            MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_CARDS_INPUTS, ARCTIC_BLUE)
        } else ARCTIC_BLUE

        val r = Color.red(baseCard)
        val g = Color.green(baseCard)
        val b = Color.blue(baseCard)

        val glass = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.argb(if (isDrawer) 225 else 195, (r + 12).coerceAtMost(255), (g + 16).coerceAtMost(255), (b + 20).coerceAtMost(255)),
                Color.argb(if (isDrawer) 215 else 180, r, g, b),
                Color.argb(if (isDrawer) 205 else 165, (r + 18).coerceAtMost(255), (g + 22).coerceAtMost(255), (b + 28).coerceAtMost(255))
            )
        ).apply {
            cornerRadius = radius
            if (!isDrawer) {
                val defBorder = Color.argb(125, Color.red(AURORA_PRIMARY), Color.green(AURORA_PRIMARY), Color.blue(AURORA_PRIMARY))
                setStroke(activity.dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(activity, defBorder))
            }
        }

        if (isDrawer) return glass

        val specularRim = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.argb(140, 255, 255, 255), Color.argb(30, Color.red(AURORA_PRIMARY), Color.green(AURORA_PRIMARY), Color.blue(AURORA_PRIMARY)))
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
        val glowCol = when {
            MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_ACTIVE_PILL] ->
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_ACTIVE_PILL, AURORA_PRIMARY)
            isAurora -> AURORA_PRIMARY
            else -> activity.primaryColor
        }
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.argb(85, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)),
                Color.argb(40, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol))
            )
        ).apply {
            cornerRadius = radius
            setStroke(activity.dp(1f), Color.argb(165, Color.red(glowCol), Color.green(glowCol), Color.blue(glowCol)))
        }
    }

    // =========================================================================
    // חבילת 95 סמלים וקטוריים זוהרים ייחודיים לערכת זוהר הצפון (Aurora Crystal Neon)
    // אפס כפילויות: לכל מחשבון (0..65), קטגוריה (300..311), כפתור (100..103) והגדרה (200..214) סמל ייחודי!
    // =========================================================================
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
            private val isDarkTint = MinimalistTheme.computeLuminance(tintColor) < 0.25
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (!isDarkTint) {
                    setShadowLayer(
                        6.0f * d,
                        0f,
                        0f,
                        Color.argb(190, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor))
                    )
                }
            }
            private val secondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = if (isDarkTint) tintColor else AURORA_SECONDARY
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
                    if (isDarkTint) intArrayOf(
                        Color.argb(35, 0, 0, 0),
                        Color.argb(20, 0, 0, 0),
                        Color.argb(35, 0, 0, 0)
                    ) else intArrayOf(
                        Color.argb(55, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                        Color.argb(30, Color.red(AURORA_SECONDARY), Color.green(AURORA_SECONDARY), Color.blue(AURORA_SECONDARY)),
                        Color.argb(50, Color.red(COSMIC_PURPLE), Color.green(COSMIC_PURPLE), Color.blue(COSMIC_PURPLE))
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                val crystalBadge = createCrystalBadgePath(cx, cy, badgeR)
                canvas.drawPath(crystalBadge, fillPaint)
                canvas.drawPath(crystalBadge, borderPaint)

                when (iconCode) {
                    0 -> { // 0. מחשבון רגיל: גביש יהלום עם צלב חיבור מרכזי ונקודות קודקוד
                        val p = Path().apply {
                            moveTo(cx, cy - r * 0.88f); lineTo(cx + r * 0.88f, cy); lineTo(cx, cy + r * 0.88f); lineTo(cx - r * 0.88f, cy); close()
                        }
                        canvas.drawPath(p, paint)
                        canvas.drawLine(cx - r * 0.40f, cy, cx + r * 0.40f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.40f, cx, cy + r * 0.40f, paint)
                        canvas.drawCircle(cx, cy - r * 0.88f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx, cy + r * 0.88f, r * 0.10f, dotPaint)
                    }
                    1 -> { // 1. מחשבון מדעי: גל סינוס וקוסינוס כפול על ציר זוהר
                        canvas.drawLine(cx - r * 0.82f, cy, cx + r * 0.82f, cy, secondaryPaint)
                        val sWave = Path().apply {
                            moveTo(cx - r * 0.82f, cy)
                            cubicTo(cx - r * 0.35f, cy - r * 0.95f, cx + r * 0.35f, cy + r * 0.95f, cx + r * 0.82f, cy)
                        }
                        canvas.drawPath(sWave, paint)
                        canvas.drawCircle(cx - r * 0.42f, cy - r * 0.42f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.42f, cy + r * 0.42f, r * 0.11f, dotPaint)
                    }
                    2 -> { // 2. מחשבון מע״מ: מגילת קריסטל משוננת עם קו % אלכסוני
                        val p = Path().apply {
                            moveTo(cx - r * 0.58f, cy - r * 0.80f); lineTo(cx + r * 0.58f, cy - r * 0.80f); lineTo(cx + r * 0.58f, cy + r * 0.78f)
                            lineTo(cx + r * 0.28f, cy + r * 0.54f); lineTo(cx, cy + r * 0.78f); lineTo(cx - r * 0.28f, cy + r * 0.54f); lineTo(cx - r * 0.58f, cy + r * 0.78f); close()
                        }
                        canvas.drawPath(p, paint)
                        canvas.drawLine(cx - r * 0.28f, cy + r * 0.15f, cx + r * 0.28f, cy - r * 0.35f, paint)
                        canvas.drawCircle(cx - r * 0.20f, cy - r * 0.30f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx + r * 0.20f, cy + r * 0.10f, r * 0.10f, dotPaint)
                    }
                    3 -> { // 3. מחשבון הלוואה: היכל בנק גבישי עם גג משולש ו-3 עמודי אור
                        val roof = Path().apply {
                            moveTo(cx - r * 0.82f, cy - r * 0.24f); lineTo(cx, cy - r * 0.82f); lineTo(cx + r * 0.82f, cy - r * 0.24f); close()
                        }
                        canvas.drawPath(roof, paint)
                        for (colX in listOf(-0.48f, 0f, 0.48f)) {
                            canvas.drawLine(cx + r * colX, cy - r * 0.20f, cx + r * colX, cy + r * 0.68f, secondaryPaint)
                        }
                        canvas.drawLine(cx - r * 0.82f, cy + r * 0.72f, cx + r * 0.82f, cy + r * 0.72f, paint)
                    }
                    4 -> { // 4. המרת מרחקים: שושנת רוחות של כוכב הצפון 4 קודקודים
                        val star = Path().apply {
                            moveTo(cx, cy - r * 0.90f); lineTo(cx + r * 0.24f, cy - r * 0.24f); lineTo(cx + r * 0.90f, cy); lineTo(cx + r * 0.24f, cy + r * 0.24f)
                            lineTo(cx, cy + r * 0.90f); lineTo(cx - r * 0.24f, cy + r * 0.24f); lineTo(cx - r * 0.90f, cy); lineTo(cx - r * 0.24f, cy - r * 0.24f); close()
                        }
                        canvas.drawPath(star, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    5 -> { // 5. המרת משקלים: משקולת כבידה טרפזית עם טבעת אחיזה עליונה
                        canvas.drawCircle(cx, cy - r * 0.62f, r * 0.20f, paint)
                        val body = Path().apply {
                            moveTo(cx - r * 0.45f, cy - r * 0.35f); lineTo(cx + r * 0.45f, cy - r * 0.35f)
                            lineTo(cx + r * 0.78f, cy + r * 0.75f); lineTo(cx - r * 0.78f, cy + r * 0.75f); close()
                        }
                        canvas.drawPath(body, paint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.25f, cx + r * 0.35f, cy + r * 0.25f, secondaryPaint)
                    }
                    6 -> { // 6. המרת טמפ׳: פתית שלג גבישי 6 זרועות עם ליבה תרמית
                        for (i in 0..2) {
                            val a = Math.toRadians((i * 60).toDouble())
                            val dx = (cos(a) * r * 0.85f).toFloat(); val dy = (sin(a) * r * 0.85f).toFloat()
                            canvas.drawLine(cx - dx, cy - dy, cx + dx, cy + dy, paint)
                        }
                        canvas.drawCircle(cx, cy, r * 0.28f, secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.10f, dotPaint)
                    }
                    7 -> { // 7. מחשבון מתכנתים: שבב סייבר מרובע עם 8 רגלי הולכה
                        canvas.drawRoundRect(RectF(cx - r * 0.48f, cy - r * 0.48f, cx + r * 0.48f, cy + r * 0.48f), r * 0.12f, r * 0.12f, paint)
                        canvas.drawCircle(cx, cy, r * 0.18f, dotPaint)
                        for (o in listOf(-0.24f * r, 0.24f * r)) {
                            canvas.drawLine(cx + o, cy - r * 0.88f, cx + o, cy - r * 0.48f, paint)
                            canvas.drawLine(cx + o, cy + r * 0.48f, cx + o, cy + r * 0.88f, paint)
                            canvas.drawLine(cx - r * 0.88f, cy + o, cx - r * 0.48f, cy + o, paint)
                            canvas.drawLine(cx + r * 0.48f, cy + o, cx + r * 0.88f, cy + o, paint)
                        }
                    }
                    8 -> { // 8. חישוב אחוזים: קשת מסלולית עם חתך % ושני כוכבים
                        canvas.drawArc(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), 25f, 240f, false, secondaryPaint)
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.55f, cx + r * 0.55f, cy - r * 0.55f, paint)
                        canvas.drawCircle(cx - r * 0.42f, cy - r * 0.42f, r * 0.16f, dotPaint)
                        canvas.drawCircle(cx + r * 0.42f, cy + r * 0.42f, r * 0.16f, dotPaint)
                    }
                    9 -> { // 9. חישוב הנחות: יהלום מלוטש 5 פאות עם קווי שבירה פנימיים
                        val gem = Path().apply {
                            moveTo(cx - r * 0.48f, cy - r * 0.72f); lineTo(cx + r * 0.48f, cy - r * 0.72f); lineTo(cx + r * 0.86f, cy - r * 0.18f)
                            lineTo(cx, cy + r * 0.84f); lineTo(cx - r * 0.86f, cy - r * 0.18f); close()
                        }
                        canvas.drawPath(gem, paint)
                        canvas.drawLine(cx - r * 0.86f, cy - r * 0.18f, cx + r * 0.86f, cy - r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.48f, cy - r * 0.72f, cx, cy + r * 0.84f, secondaryPaint)
                        canvas.drawLine(cx + r * 0.48f, cy - r * 0.72f, cx, cy + r * 0.84f, secondaryPaint)
                    }
                    10 -> { // 10. חלוקת טיפים: גביע קריסטל משולש על רגל דקה
                        val glass = Path().apply {
                            moveTo(cx - r * 0.74f, cy - r * 0.62f); lineTo(cx + r * 0.74f, cy - r * 0.62f); lineTo(cx, cy + r * 0.15f); close()
                        }
                        canvas.drawPath(glass, paint)
                        canvas.drawLine(cx, cy + r * 0.15f, cx, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.44f, cy + r * 0.78f, cx + r * 0.44f, cy + r * 0.78f, paint)
                        canvas.drawCircle(cx, cy - r * 0.32f, r * 0.13f, dotPaint)
                    }
                    11 -> { // 11. מדד BMI: לב גאומטרי מלוטש עם ליבת דופק
                        val heart = Path().apply {
                            moveTo(cx, cy + r * 0.80f); lineTo(cx - r * 0.84f, cy - r * 0.12f); lineTo(cx - r * 0.45f, cy - r * 0.70f)
                            lineTo(cx, cy - r * 0.28f); lineTo(cx + r * 0.45f, cy - r * 0.70f); lineTo(cx + r * 0.84f, cy - r * 0.12f); close()
                        }
                        canvas.drawPath(heart, paint)
                        canvas.drawCircle(cx, cy + r * 0.10f, r * 0.14f, dotPaint)
                    }
                    12 -> { // 12. חישוב גיל: שעון חול קוסמי עם גרגרי אור נופלים
                        val hg = Path().apply {
                            moveTo(cx - r * 0.65f, cy - r * 0.78f); lineTo(cx + r * 0.65f, cy - r * 0.78f)
                            lineTo(cx - r * 0.65f, cy + r * 0.78f); lineTo(cx + r * 0.65f, cy + r * 0.78f); close()
                        }
                        canvas.drawPath(hg, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx, cy + r * 0.48f, r * 0.14f, dotPaint)
                    }
                    13 -> { // 13. מטבעות לייב: כדור הארץ עם קו משווה ומרידיאן
                        canvas.drawCircle(cx, cy, r * 0.76f, paint)
                        canvas.drawOval(RectF(cx - r * 0.36f, cy - r * 0.76f, cx + r * 0.36f, cy + r * 0.76f), secondaryPaint)
                        canvas.drawLine(cx - r * 0.76f, cy, cx + r * 0.76f, cy, paint)
                    }
                    14 -> { // 14. מחשבון BMR: להבת פלזמה עם גרעין חום זוהר
                        val flame = Path().apply {
                            moveTo(cx, cy - r * 0.90f)
                            quadTo(cx + r * 0.85f, cy - r * 0.10f, cx, cy + r * 0.85f)
                            quadTo(cx - r * 0.85f, cy - r * 0.10f, cx, cy - r * 0.90f)
                        }
                        canvas.drawPath(flame, paint)
                        canvas.drawCircle(cx, cy + r * 0.25f, r * 0.20f, dotPaint)
                    }
                    15 -> { // 15. משוואות ריבועיות: עקומת פרבולה גבישית עם מוקד ושורשים
                        canvas.drawLine(cx - r * 0.82f, cy + r * 0.42f, cx + r * 0.82f, cy + r * 0.42f, secondaryPaint)
                        val para = Path().apply {
                            moveTo(cx - r * 0.68f, cy - r * 0.68f)
                            quadTo(cx, cy + r * 1.20f, cx + r * 0.68f, cy - r * 0.68f)
                        }
                        canvas.drawPath(para, paint)
                        canvas.drawCircle(cx - r * 0.36f, cy + r * 0.42f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.36f, cy + r * 0.42f, r * 0.11f, dotPaint)
                    }
                    16 -> { // 16. מחשבון שברים: קו שבר אלכסוני עם שתי טבעות חלולות
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.55f, cx + r * 0.75f, cy - r * 0.55f, paint)
                        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.42f, r * 0.24f, paint)
                        canvas.drawCircle(cx + r * 0.35f, cy + r * 0.42f, r * 0.24f, paint)
                    }
                    17 -> { // 17. ריבית דריבית: 3 עמודי אור עולים מחוברים בקו מגמה
                        canvas.drawLine(cx - r * 0.60f, cy + r * 0.75f, cx - r * 0.60f, cy + r * 0.20f, paint)
                        canvas.drawLine(cx, cy + r * 0.75f, cx, cy - r * 0.15f, paint)
                        canvas.drawLine(cx + r * 0.60f, cy + r * 0.75f, cx + r * 0.60f, cy - r * 0.60f, paint)
                        canvas.drawCircle(cx - r * 0.60f, cy + r * 0.20f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx, cy - r * 0.15f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx + r * 0.60f, cy - r * 0.60f, r * 0.12f, dotPaint)
                        canvas.drawLine(cx - r * 0.60f, cy + r * 0.20f, cx, cy - r * 0.15f, secondaryPaint)
                        canvas.drawLine(cx, cy - r * 0.15f, cx + r * 0.60f, cy - r * 0.60f, secondaryPaint)
                    }
                    18 -> { // 18. המרת מהירות: חללית על-קולית עם שובל דחף
                        val rocket = Path().apply {
                            moveTo(cx, cy - r * 0.90f); quadTo(cx + r * 0.65f, cy - r * 0.10f, cx + r * 0.45f, cy + r * 0.52f)
                            lineTo(cx - r * 0.45f, cy + r * 0.52f); quadTo(cx - r * 0.65f, cy - r * 0.10f, cx, cy - r * 0.90f)
                        }
                        canvas.drawPath(rocket, paint)
                        canvas.drawCircle(cx, cy + r * 0.78f, r * 0.15f, dotPaint)
                    }
                    19 -> { // 19. הגדרות: גלגל שיניים 6 חישורים עם טבעת כפולה
                        canvas.drawCircle(cx, cy, r * 0.34f, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawLine((cx + cos(a) * r * 0.42f).toFloat(), (cy + sin(a) * r * 0.42f).toFloat(), (cx + cos(a) * r * 0.86f).toFloat(), (cy + sin(a) * r * 0.86f).toFloat(), paint)
                        }
                    }
                    20 -> { // 20. מחשבון עלות דלק: תא אנרגיה גלילי עם ברק פנימי
                        canvas.drawRoundRect(RectF(cx - r * 0.55f, cy - r * 0.78f, cx + r * 0.55f, cy + r * 0.78f), r * 0.20f, r * 0.20f, paint)
                        canvas.drawLine(cx + r * 0.15f, cy - r * 0.40f, cx - r * 0.20f, cy + r * 0.05f, paint)
                        canvas.drawLine(cx - r * 0.20f, cy + r * 0.05f, cx + r * 0.20f, cy + r * 0.05f, paint)
                        canvas.drawLine(cx + r * 0.20f, cy + r * 0.05f, cx - r * 0.15f, cy + r * 0.45f, paint)
                    }
                    21 -> { // 21. יעילות שריפת דלק: עלה ביו-אנרגטי זוהר עם עורק מרכזי
                        val leaf = Path().apply {
                            moveTo(cx - r * 0.68f, cy + r * 0.68f)
                            quadTo(cx - r * 0.75f, cy - r * 0.65f, cx + r * 0.75f, cy - r * 0.75f)
                            quadTo(cx + r * 0.65f, cy + r * 0.75f, cx - r * 0.68f, cy + r * 0.68f)
                        }
                        canvas.drawPath(leaf, paint)
                        canvas.drawLine(cx - r * 0.68f, cy + r * 0.68f, cx + r * 0.40f, cy - r * 0.40f, secondaryPaint)
                        canvas.drawCircle(cx + r * 0.40f, cy - r * 0.40f, r * 0.11f, dotPaint)
                    }
                    22 -> { // 22. ממוצע ציונים: כובע אקדמי מעוין עם אבן חן מרכזית
                        val cap = Path().apply {
                            moveTo(cx, cy - r * 0.75f); lineTo(cx + r * 0.90f, cy - r * 0.20f); lineTo(cx, cy + r * 0.35f); lineTo(cx - r * 0.90f, cy - r * 0.20f); close()
                        }
                        canvas.drawPath(cap, paint)
                        canvas.drawCircle(cx, cy - r * 0.20f, r * 0.13f, dotPaint)
                    }
                    23 -> { // 23. מחשבון תשר מתקדם: כיפת הגשה מלכותית עם יהלום עילי
                        canvas.drawArc(RectF(cx - r * 0.76f, cy - r * 0.45f, cx + r * 0.76f, cy + r * 0.85f), 180f, 180f, true, paint)
                        canvas.drawCircle(cx, cy - r * 0.62f, r * 0.14f, dotPaint)
                        canvas.drawLine(cx - r * 0.88f, cy + r * 0.42f, cx + r * 0.88f, cy + r * 0.42f, secondaryPaint)
                    }
                    24 -> { // 24. מחשבון מס מכירות: תיק נשיאה גבישי עם קשת אחיזה ונקודת אבזם
                        val bag = Path().apply {
                            moveTo(cx - r * 0.56f, cy - r * 0.28f); lineTo(cx + r * 0.56f, cy - r * 0.28f)
                            lineTo(cx + r * 0.70f, cy + r * 0.76f); lineTo(cx - r * 0.70f, cy + r * 0.76f); close()
                        }
                        canvas.drawPath(bag, paint)
                        canvas.drawArc(RectF(cx - r * 0.34f, cy - r * 0.76f, cx + r * 0.34f, cy + r * 0.05f), 180f, 180f, false, secondaryPaint)
                        canvas.drawCircle(cx, cy + r * 0.22f, r * 0.13f, dotPaint)
                    }
                    25 -> { // 25. מחשבון מחיר יחידתי: שתי מנסרות מרחפות עם קו השוואה
                        canvas.drawRect(cx - r * 0.80f, cy - r * 0.30f, cx - r * 0.18f, cy + r * 0.58f, paint)
                        canvas.drawRect(cx + r * 0.18f, cy - r * 0.68f, cx + r * 0.80f, cy + r * 0.58f, paint)
                        canvas.drawLine(cx - r * 0.49f, cy - r * 0.52f, cx + r * 0.49f, cy - r * 0.84f, secondaryPaint)
                    }
                    26 -> { // 26. ממיר שעון עולמי: שעון אסטרונומי עם מחוגים וטבעת היקפית
                        canvas.drawCircle(cx, cy, r * 0.80f, paint)
                        canvas.drawCircle(cx, cy, r * 0.58f, secondaryPaint)
                        canvas.drawLine(cx, cy, cx, cy - r * 0.42f, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.34f, cy + r * 0.20f, paint)
                        canvas.drawCircle(cx, cy, r * 0.11f, dotPaint)
                    }
                    27 -> { // 27. מחשבון ביוץ: כוכב פרח לוטוס 6 עלים
                        for (i in 0 until 3) {
                            canvas.save(); canvas.rotate(i * 60f, cx, cy)
                            canvas.drawOval(RectF(cx - r * 0.82f, cy - r * 0.26f, cx + r * 0.82f, cy + r * 0.26f), paint)
                            canvas.restore()
                        }
                        canvas.drawCircle(cx, cy, r * 0.18f, dotPaint)
                    }
                    28 -> { // 28. ממיר הקסדצימלי: מטריצת סולמית # עם 4 צמתים זוהרים
                        canvas.drawLine(cx - r * 0.32f, cy - r * 0.78f, cx - r * 0.32f, cy + r * 0.78f, paint)
                        canvas.drawLine(cx + r * 0.32f, cy - r * 0.78f, cx + r * 0.32f, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.78f, cy - r * 0.32f, cx + r * 0.78f, cy - r * 0.32f, secondaryPaint)
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.32f, cx + r * 0.78f, cy + r * 0.32f, secondaryPaint)
                        canvas.drawCircle(cx - r * 0.32f, cy - r * 0.32f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.32f, cy + r * 0.32f, r * 0.11f, dotPaint)
                    }
                    29 -> { // 29. מחשבון חסכונות: כספת משושה כפולה עם ליבת יהלום
                        val hex = Path()
                        for (i in 0..5) {
                            val a = Math.toRadians((i * 60 - 30).toDouble())
                            val x = (cx + cos(a) * r * 0.84f).toFloat()
                            val y = (cy + sin(a) * r * 0.84f).toFloat()
                            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
                        }
                        hex.close()
                        canvas.drawPath(hex, paint)
                        canvas.drawCircle(cx, cy, r * 0.34f, secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.13f, dotPaint)
                    }
                    30 -> { // 30. מחשבון תאריך: לוח שנה קוסמי עם 4 כוכבי תאריך
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.62f, cx + r * 0.76f, cy + r * 0.76f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.18f, cx + r * 0.76f, cy - r * 0.18f, secondaryPaint)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.84f, cx - r * 0.36f, cy - r * 0.42f, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.84f, cx + r * 0.36f, cy - r * 0.42f, paint)
                        canvas.drawCircle(cx - r * 0.30f, cy + r * 0.25f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.30f, cy + r * 0.25f, r * 0.11f, dotPaint)
                    }
                    31 -> { // 31. כל 87 קטגוריות היחידות: מעגל חוסם מעוין עם ליבה
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        val diamond = Path().apply {
                            moveTo(cx, cy - r * 0.82f); lineTo(cx + r * 0.82f, cy)
                            lineTo(cx, cy + r * 0.82f); lineTo(cx - r * 0.82f, cy); close()
                        }
                        canvas.drawPath(diamond, secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.20f, dotPaint)
                    }
                    32 -> { // 32. אינסטלציה, צנרת, ספיקה ולחץ: 3 גלי זרימה למינריים עם צומת לחץ
                        for (dy in listOf(-0.45f * r, 0f, 0.45f * r)) {
                            val wave = Path().apply {
                                moveTo(cx - r * 0.80f, cy + dy)
                                quadTo(cx - r * 0.40f, cy + dy - r * 0.28f, cx, cy + dy)
                                quadTo(cx + r * 0.40f, cy + dy + r * 0.28f, cx + r * 0.80f, cy + dy)
                            }
                            canvas.drawPath(wave, paint)
                        }
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
                    }
                    33 -> { // 33. מינון תרופות, עירוי IV וסוכר: סליל DNA כפול
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
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.50f, cx + r * 0.35f, cy - r * 0.50f, secondaryPaint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.50f, cx + r * 0.35f, cy + r * 0.50f, secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.14f, dotPaint)
                    }
                    34 -> { // 34. בנייה, בטון, ריצוף ומיזוג BTU: פירמידה תלת-ממדית עם קודקוד מואר
                        val pyr = Path().apply {
                            moveTo(cx, cy - r * 0.86f); lineTo(cx + r * 0.82f, cy + r * 0.48f)
                            lineTo(cx, cy + r * 0.84f); lineTo(cx - r * 0.82f, cy + r * 0.48f); close()
                        }
                        canvas.drawPath(pyr, paint)
                        canvas.drawLine(cx, cy - r * 0.86f, cx, cy + r * 0.84f, secondaryPaint)
                        canvas.drawCircle(cx, cy - r * 0.86f, r * 0.14f, dotPaint)
                    }
                    35 -> { // 35. קודי צבע של נגדים: גל זיג-זג של נגד חשמלי
                        val zig = Path().apply {
                            moveTo(cx - r * 0.90f, cy); lineTo(cx - r * 0.55f, cy)
                            lineTo(cx - r * 0.38f, cy - r * 0.52f); lineTo(cx - r * 0.12f, cy + r * 0.52f)
                            lineTo(cx + r * 0.12f, cy - r * 0.52f); lineTo(cx + r * 0.38f, cy + r * 0.52f)
                            lineTo(cx + r * 0.55f, cy); lineTo(cx + r * 0.90f, cy)
                        }
                        canvas.drawPath(zig, paint)
                        canvas.drawCircle(cx - r * 0.88f, cy, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.88f, cy, r * 0.11f, dotPaint)
                    }
                    36 -> { // 36. מפענח קודי קבלים: קשתות שדה אלקטרוסטטי של קבל
                        canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 0.65f, cx - r * 0.15f, cy + r * 0.65f), -65f, 130f, false, paint)
                        canvas.drawArc(RectF(cx + r * 0.15f, cy - r * 0.65f, cx + r * 0.85f, cy + r * 0.65f), 115f, 130f, false, paint)
                        canvas.drawCircle(cx, cy, r * 0.20f, dotPaint)
                        canvas.drawLine(cx - r * 0.92f, cy, cx - r * 0.38f, cy, paint)
                        canvas.drawLine(cx + r * 0.38f, cy, cx + r * 0.92f, cy, paint)
                    }
                    37 -> { // 37. עוביי חוטים AWG/SWG: צמת מוליכים שזורה
                        val braid1 = Path().apply {
                            moveTo(cx - r * 0.85f, cy - r * 0.35f)
                            cubicTo(cx - r * 0.20f, cy + r * 0.70f, cx + r * 0.20f, cy - r * 0.70f, cx + r * 0.85f, cy + r * 0.35f)
                        }
                        val braid2 = Path().apply {
                            moveTo(cx - r * 0.85f, cy + r * 0.35f)
                            cubicTo(cx - r * 0.20f, cy - r * 0.70f, cx + r * 0.20f, cy + r * 0.70f, cx + r * 0.85f, cy - r * 0.35f)
                        }
                        canvas.drawPath(braid1, paint)
                        canvas.drawPath(braid2, secondaryPaint)
                        canvas.drawCircle(cx + r * 0.85f, cy - r * 0.35f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx + r * 0.85f, cy + r * 0.35f, r * 0.12f, dotPaint)
                    }
                    38 -> { // 38. מעבדת חוק אוהם: ליבת אנרגיה 6 קרניים בתוך טבעת
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
                    39 -> { // 39. פלס בנאים דו-צירי: כדור איזון תלת-ממדי עם 2 טבעות ובועה
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawOval(RectF(cx - r * 0.82f, cy - r * 0.32f, cx + r * 0.82f, cy + r * 0.32f), paint)
                        canvas.drawOval(RectF(cx - r * 0.32f, cy - r * 0.82f, cx + r * 0.32f, cy + r * 0.82f), secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.18f, dotPaint)
                    }
                    40 -> { // 40. מצפן 3D מכויל: כוכב מצפן 4 קודקודים בתוך טבעת אזימוט
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
                    41 -> { // 41. מד-זווית דו-זרועי וסרגל: זווית לייזר עם קשת מדידה
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.65f, cx + r * 0.82f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy + r * 0.65f, cx + r * 0.55f, cy - r * 0.68f, paint)
                        canvas.drawArc(RectF(cx - r * 1.45f, cy - r * 0.05f, cx - r * 0.05f, cy + r * 1.35f), -48f, 48f, false, secondaryPaint)
                        canvas.drawCircle(cx - r * 0.75f, cy + r * 0.65f, r * 0.16f, dotPaint)
                    }
                    42 -> { // 42. שעון עצר כרונוגרף: שתי קשתות תזמון עם מחוג
                        canvas.drawArc(RectF(cx - r * 0.84f, cy - r * 0.84f, cx + r * 0.84f, cy + r * 0.84f), -90f, 290f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.54f, cy - r * 0.54f, cx + r * 0.54f, cy + r * 0.54f), -90f, 210f, false, secondaryPaint)
                        canvas.drawLine(cx, cy, cx + r * 0.42f, cy - r * 0.42f, paint)
                        canvas.drawCircle(cx, cy - r * 0.84f, r * 0.14f, dotPaint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    43 -> { // 43. פנס LED פיזי ואיתות SOS: משדר לייזר המקרין 3 אלומות אור
                        val emitter = Path().apply {
                            moveTo(cx - r * 0.78f, cy - r * 0.28f); lineTo(cx - r * 0.25f, cy - r * 0.48f)
                            lineTo(cx - r * 0.25f, cy + r * 0.48f); lineTo(cx - r * 0.78f, cy + r * 0.28f); close()
                        }
                        canvas.drawPath(emitter, paint)
                        canvas.drawLine(cx - r * 0.15f, cy, cx + r * 0.88f, cy, paint)
                        canvas.drawLine(cx - r * 0.15f, cy - r * 0.20f, cx + r * 0.82f, cy - r * 0.65f, secondaryPaint)
                        canvas.drawLine(cx - r * 0.15f, cy + r * 0.20f, cx + r * 0.82f, cy + r * 0.65f, secondaryPaint)
                        canvas.drawCircle(cx - r * 0.52f, cy, r * 0.12f, dotPaint)
                    }
                    44 -> { // 44. הטבלה המחזורית ואטום: 3 מסלולי אלקטרונים עם גרעין זוהר
                        for (ang in listOf(0f, 60f, -60f)) {
                            canvas.save(); canvas.rotate(ang, cx, cy)
                            canvas.drawOval(RectF(cx - r * 0.90f, cy - r * 0.34f, cx + r * 0.90f, cy + r * 0.34f), paint)
                            canvas.restore()
                        }
                        canvas.drawCircle(cx, cy, r * 0.18f, dotPaint)
                        canvas.drawCircle(cx + r * 0.65f, cy - r * 0.35f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx - r * 0.65f, cy + r * 0.35f, r * 0.10f, dotPaint)
                    }
                    45 -> { // 45. מתרגם ומשדר קוד מורס: אנטנת שידור עם גלי רדיו
                        canvas.drawCircle(cx, cy - r * 0.25f, r * 0.15f, dotPaint)
                        canvas.drawLine(cx, cy - r * 0.10f, cx - r * 0.42f, cy + r * 0.80f, paint)
                        canvas.drawLine(cx, cy - r * 0.10f, cx + r * 0.42f, cy + r * 0.80f, paint)
                        canvas.drawArc(RectF(cx - r * 0.55f, cy - r * 0.80f, cx + r * 0.55f, cy + r * 0.30f), -150f, 120f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 1.05f, cx + r * 0.85f, cy + r * 0.55f), -140f, 100f, false, secondaryPaint)
                    }
                    46 -> { // 46. מחולל סיסמאות ומד אנטרופיה: מגן סייבר עם חור מנעול
                        val shield = Path().apply {
                            moveTo(cx, cy - r * 0.86f); lineTo(cx + r * 0.72f, cy - r * 0.45f)
                            lineTo(cx + r * 0.58f, cy + r * 0.35f); lineTo(cx, cy + r * 0.86f)
                            lineTo(cx - r * 0.58f, cy + r * 0.35f); lineTo(cx - r * 0.72f, cy - r * 0.45f); close()
                        }
                        canvas.drawPath(shield, paint)
                        canvas.drawCircle(cx, cy - r * 0.10f, r * 0.18f, dotPaint)
                        canvas.drawLine(cx, cy + r * 0.08f, cx, cy + r * 0.45f, paint)
                    }
                    47 -> { // 47. גימטריה, אתב״ש וספרות רומיות: כוכב משושה גאומטרי
                        val t1 = Path().apply {
                            moveTo(cx, cy - r * 0.85f); lineTo(cx + r * 0.74f, cy + r * 0.42f); lineTo(cx - r * 0.74f, cy + r * 0.42f); close()
                        }
                        val t2 = Path().apply {
                            moveTo(cx, cy + r * 0.85f); lineTo(cx + r * 0.74f, cy - r * 0.42f); lineTo(cx - r * 0.74f, cy - r * 0.42f); close()
                        }
                        canvas.drawPath(t1, paint)
                        canvas.drawPath(t2, secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.13f, dotPaint)
                    }
                    48 -> { // 48. מתכות יקרות: כתר מלכותי 3 קודקודים
                        val crown = Path().apply {
                            moveTo(cx - r * 0.78f, cy + r * 0.65f); lineTo(cx - r * 0.68f, cy - r * 0.35f)
                            lineTo(cx - r * 0.28f, cy + r * 0.15f); lineTo(cx, cy - r * 0.68f)
                            lineTo(cx + r * 0.28f, cy + r * 0.15f); lineTo(cx + r * 0.68f, cy - r * 0.35f)
                            lineTo(cx + r * 0.78f, cy + r * 0.65f); close()
                        }
                        canvas.drawPath(crown, paint)
                        canvas.drawCircle(cx, cy - r * 0.68f, r * 0.12f, dotPaint)
                    }
                    49 -> { // 49. משכנתא ולוח סילוקין: שני מגדלים תחת קשת הגנה
                        canvas.drawRect(cx - r * 0.68f, cy - r * 0.10f, cx - r * 0.20f, cy + r * 0.78f, paint)
                        canvas.drawRect(cx - r * 0.10f, cy - r * 0.55f, cx + r * 0.45f, cy + r * 0.78f, paint)
                        canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 0.85f, cx + r * 0.85f, cy + r * 0.25f), 200f, 140f, false, secondaryPaint)
                    }
                    50 -> { // 50. תשואת ROI ו-CAGR: טבעת מסלולית עם חץ פריצה אלכסוני
                        canvas.drawOval(RectF(cx - r * 0.82f, cy - r * 0.32f, cx + r * 0.82f, cy + r * 0.32f), secondaryPaint)
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.70f, cx + r * 0.55f, cy - r * 0.70f, paint)
                        canvas.drawLine(cx + r * 0.10f, cy - r * 0.70f, cx + r * 0.55f, cy - r * 0.70f, paint)
                        canvas.drawLine(cx + r * 0.55f, cy - r * 0.70f, cx + r * 0.55f, cy - r * 0.25f, paint)
                        canvas.drawCircle(cx + r * 0.55f, cy - r * 0.70f, r * 0.12f, dotPaint)
                    }
                    51 -> { // 51. מרווח רווח ונקודת איזון: צומת קווים עם הילת איזון
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.70f, cx + r * 0.80f, cy - r * 0.70f, paint)
                        canvas.drawLine(cx - r * 0.80f, cy - r * 0.40f, cx + r * 0.80f, cy + r * 0.50f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, dotPaint)
                        canvas.drawCircle(cx, cy, r * 0.45f, secondaryPaint)
                    }
                    52 -> { // 52. סטטיסטיקה ועקומת פעמון: פעמון גאוס כפול עם שיא מואר
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
                    53 -> { // 53. גאומטריה ופיתגורס: משולש חסום במעגל עם מעגל פנימי
                        canvas.drawCircle(cx, cy, r * 0.84f, secondaryPaint)
                        val tri = Path().apply {
                            moveTo(cx, cy - r * 0.84f); lineTo(cx + r * 0.73f, cy + r * 0.42f); lineTo(cx - r * 0.73f, cy + r * 0.42f); close()
                        }
                        canvas.drawPath(tri, paint)
                        canvas.drawCircle(cx, cy, r * 0.38f, paint)
                        canvas.drawCircle(cx, cy, r * 0.11f, dotPaint)
                    }
                    54 -> { // 54. תורת המספרים וקומבינטוריקה: עץ גורמים בינארי עם צמתים זוהרים
                        val topY = cy - r * 0.68f; val midY = cy; val botY = cy + r * 0.68f
                        canvas.drawLine(cx, topY, cx - r * 0.45f, midY, paint)
                        canvas.drawLine(cx, topY, cx + r * 0.45f, midY, paint)
                        canvas.drawLine(cx - r * 0.45f, midY, cx - r * 0.70f, botY, secondaryPaint)
                        canvas.drawLine(cx - r * 0.45f, midY, cx - r * 0.18f, botY, secondaryPaint)
                        canvas.drawLine(cx + r * 0.45f, midY, cx + r * 0.18f, botY, secondaryPaint)
                        canvas.drawLine(cx + r * 0.45f, midY, cx + r * 0.70f, botY, secondaryPaint)
                        canvas.drawCircle(cx, topY, r * 0.15f, dotPaint)
                        canvas.drawCircle(cx - r * 0.45f, midY, r * 0.13f, dotPaint)
                        canvas.drawCircle(cx + r * 0.45f, midY, r * 0.13f, dotPaint)
                    }
                    55 -> { // 55. ייצוא PDF/CSV וגיבוי Drive: פורטל ענן אליפטי עם חץ העלאה
                        canvas.drawOval(RectF(cx - r * 0.85f, cy + r * 0.15f, cx + r * 0.85f, cy + r * 0.72f), secondaryPaint)
                        canvas.drawLine(cx, cy + r * 0.45f, cx, cy - r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy - r * 0.42f, cx, cy - r * 0.78f, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.42f, cx, cy - r * 0.78f, paint)
                        canvas.drawCircle(cx, cy - r * 0.78f, r * 0.12f, dotPaint)
                    }
                    56 -> { // 56. מרכז המועדפים וחישובים נעוצים: סופרנובה 8 קודקודים בתוך הילה
                        canvas.drawCircle(cx, cy, r * 0.88f, secondaryPaint)
                        val polaris = Path()
                        for (i in 0 until 8) {
                            val rad = if (i % 2 == 0) r * 0.82f else r * 0.26f
                            val a = Math.toRadians((i * 45 - 90).toDouble())
                            val x = (cx + cos(a) * rad).toFloat()
                            val y = (cy + sin(a) * rad).toFloat()
                            if (i == 0) polaris.moveTo(x, y) else polaris.lineTo(x, y)
                        }
                        polaris.close()
                        canvas.drawPath(polaris, paint)
                        canvas.drawCircle(cx, cy, r * 0.13f, dotPaint)
                    }
                    57 -> { // 57. ממירי מכניקה, כוח ומומנט: רוטור טורבינה 3 להבים
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, dotPaint)
                        for (i in 0 until 3) {
                            val a = Math.toRadians((i * 120).toDouble())
                            canvas.drawLine(cx, cy, (cx + cos(a) * r * 0.82f).toFloat(), (cy + sin(a) * r * 0.82f).toFloat(), paint)
                        }
                    }
                    58 -> { // 58. ממירי זורמים, צמיגות וספיקה: מערבולת הידרודינמית ספירלית
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.82f, cx + r * 0.82f, cy + r * 0.82f), 0f, 250f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.50f, cy - r * 0.50f, cx + r * 0.50f, cy + r * 0.50f), 90f, 250f, false, secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.16f, dotPaint)
                    }
                    59 -> { // 59. ממירי תרמודינמיקה וחום: ליבה תרמית עם טבעת התפשטות
                        canvas.drawCircle(cx, cy, r * 0.36f, dotPaint)
                        canvas.drawCircle(cx, cy, r * 0.78f, paint)
                    }
                    60 -> { // 60. ממירי חשמל, מגנטיות ושדה: מגנט פרסה עם קווי שדה חשמלי
                        canvas.drawArc(RectF(cx - r * 0.65f, cy - r * 0.20f, cx + r * 0.65f, cy + r * 0.80f), 0f, 180f, false, paint)
                        canvas.drawLine(cx - r * 0.65f, cy + r * 0.30f, cx - r * 0.65f, cy - r * 0.55f, paint)
                        canvas.drawLine(cx + r * 0.65f, cy + r * 0.30f, cx + r * 0.65f, cy - r * 0.55f, paint)
                        canvas.drawArc(RectF(cx - r * 0.45f, cy - r * 0.85f, cx + r * 0.45f, cy - r * 0.35f), 200f, 140f, false, secondaryPaint)
                    }
                    61 -> { // 61. ממירי אופטיקה, אקוסטיקה וקרינה: מנסרה אופטית השוברת ספקטרום אור
                        val prism = Path().apply {
                            moveTo(cx - r * 0.10f, cy - r * 0.72f); lineTo(cx + r * 0.48f, cy + r * 0.68f); lineTo(cx - r * 0.68f, cy + r * 0.68f); close()
                        }
                        canvas.drawPath(prism, paint)
                        canvas.drawLine(cx - r * 0.92f, cy + r * 0.10f, cx - r * 0.35f, cy + r * 0.10f, paint)
                        canvas.drawLine(cx + r * 0.20f, cy - r * 0.05f, cx + r * 0.90f, cy - r * 0.55f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.10f, cx + r * 0.92f, cy, secondaryPaint)
                        canvas.drawLine(cx + r * 0.30f, cy + r * 0.25f, cx + r * 0.90f, cy + r * 0.55f, paint)
                    }
                    62 -> { // 62. ממירי רפואה, כימיה ומעבדה: בקבוק מעבדה קוני מבעבע
                        val flask = Path().apply {
                            moveTo(cx - r * 0.22f, cy - r * 0.78f); lineTo(cx + r * 0.22f, cy - r * 0.78f)
                            lineTo(cx + r * 0.22f, cy - r * 0.28f); lineTo(cx + r * 0.74f, cy + r * 0.76f)
                            lineTo(cx - r * 0.74f, cy + r * 0.76f); lineTo(cx - r * 0.22f, cy - r * 0.28f); close()
                        }
                        canvas.drawPath(flask, paint)
                        canvas.drawLine(cx - r * 0.48f, cy + r * 0.30f, cx + r * 0.48f, cy + r * 0.30f, secondaryPaint)
                        canvas.drawCircle(cx, cy + r * 0.52f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx, cy - r * 0.50f, r * 0.09f, dotPaint)
                    }
                    63 -> { // 63. ממירי תקשורת, טיפוגרפיה, חלל ובישול: כוכב שבתאי עם טבעות וירחים
                        canvas.drawCircle(cx, cy, r * 0.24f, dotPaint)
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.55f, cx + r * 0.82f, cy + r * 0.55f), -30f, 150f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.55f, cx + r * 0.82f, cy + r * 0.55f), 150f, 150f, false, paint)
                        canvas.drawCircle(cx + r * 0.68f, cy - r * 0.42f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx - r * 0.68f, cy + r * 0.42f, r * 0.11f, dotPaint)
                    }
                    64 -> { // 64. מחשבון שכר, שעות נוספות, פנסיה ונטו: מטבע משושה עם סימן וי
                        val hex = Path()
                        for (i in 0..5) {
                            val a = Math.toRadians((i * 60).toDouble())
                            val x = (cx + cos(a) * r * 0.84f).toFloat()
                            val y = (cy + sin(a) * r * 0.84f).toFloat()
                            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
                        }
                        hex.close()
                        canvas.drawPath(hex, paint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.05f, cx - r * 0.05f, cy + r * 0.35f, paint)
                        canvas.drawLine(cx - r * 0.05f, cy + r * 0.35f, cx + r * 0.42f, cy - r * 0.25f, paint)
                    }
                    65 -> { // 65. מטריצות, דטרמיננטה ומערכת משוואות: רשת 4 צמתים אלכסונית
                        val dOff = r * 0.52f
                        val pts = listOf(cx - dOff to cy - dOff, cx + dOff to cy - dOff, cx - dOff to cy + dOff, cx + dOff to cy + dOff)
                        canvas.drawRect(cx - dOff, cy - dOff, cx + dOff, cy + dOff, secondaryPaint)
                        canvas.drawLine(cx - dOff, cy - dOff, cx + dOff, cy + dOff, paint)
                        canvas.drawLine(cx + dOff, cy - dOff, cx - dOff, cy + dOff, paint)
                        for ((px, py) in pts) {
                            canvas.drawCircle(px, py, r * 0.16f, dotPaint)
                        }
                    }
                    // --- סמלי סרגל עליון (100..103) ---
                    100 -> { // 100. חלון צף PiP
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.72f, cx + r * 0.82f, cy + r * 0.72f), r * 0.20f, r * 0.20f, secondaryPaint)
                        canvas.drawRoundRect(RectF(cx - r * 0.05f, cy - r * 0.05f, cx + r * 0.72f, cy + r * 0.62f), r * 0.15f, r * 0.15f, dotPaint)
                    }
                    101 -> { // 101. היסטוריית חישובים
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.82f, cx + r * 0.82f, cy + r * 0.82f), -60f, 290f, false, paint)
                        canvas.drawLine(cx, cy - r * 0.45f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.35f, cy + r * 0.20f, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    102, 103 -> { // 102/103. כוכב מועדפים בסרגל העליון
                        val polaris = Path()
                        for (i in 0 until 8) {
                            val rad = if (i % 2 == 0) r * 0.90f else r * 0.28f
                            val a = Math.toRadians((i * 45 - 90).toDouble())
                            val x = (cx + cos(a) * rad).toFloat()
                            val y = (cy + sin(a) * rad).toFloat()
                            if (i == 0) polaris.moveTo(x, y) else polaris.lineTo(x, y)
                        }
                        polaris.close()
                        if (iconCode == 102) canvas.drawPath(polaris, dotPaint)
                        canvas.drawPath(polaris, paint)
                    }
                    // --- סמלי הגדרות וכלים (200..214) ---
                    200 -> { // 200. חיפוש
                        val lens = Path().apply {
                            moveTo(cx - r * 0.18f, cy - r * 0.78f); lineTo(cx + r * 0.42f, cy - r * 0.18f); lineTo(cx - r * 0.18f, cy + r * 0.42f); lineTo(cx - r * 0.78f, cy - r * 0.18f); close()
                        }
                        canvas.drawPath(lens, paint)
                        canvas.drawLine(cx + r * 0.18f, cy + r * 0.18f, cx + r * 0.78f, cy + r * 0.78f, paint)
                    }
                    201 -> { // 201. צלילי הקשה: ספקטרום אודיו
                        canvas.drawLine(cx - r * 0.65f, cy - r * 0.25f, cx - r * 0.65f, cy + r * 0.25f, paint)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.68f, cx - r * 0.22f, cy + r * 0.68f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy - r * 0.45f, cx + r * 0.22f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx + r * 0.65f, cy - r * 0.78f, cx + r * 0.65f, cy + r * 0.78f, paint)
                    }
                    202 -> { // 202. בהירות
                        val wave = Path().apply {
                            moveTo(cx - r * 0.80f, cy + r * 0.45f); quadTo(cx - r * 0.30f, cy - r * 0.05f, cx, cy + r * 0.25f); quadTo(cx + r * 0.35f, cy + r * 0.55f, cx + r * 0.80f, cy + r * 0.05f)
                        }
                        canvas.drawPath(wave, paint)
                    }
                    203 -> { // 203. שקיפות
                        val p1 = Path().apply {
                            moveTo(cx - r * 0.20f, cy - r * 0.78f); lineTo(cx + r * 0.38f, cy); lineTo(cx - r * 0.20f, cy + r * 0.78f); lineTo(cx - r * 0.78f, cy); close()
                        }
                        canvas.drawPath(p1, paint)
                        canvas.drawCircle(cx + r * 0.25f, cy, r * 0.45f, secondaryPaint)
                    }
                    204 -> { // 204. רטט בלחיצה: מעוין מרחף עם גלי הדף בצדדים
                        val dPath = Path().apply {
                            moveTo(cx, cy - r * 0.62f); lineTo(cx + r * 0.42f, cy)
                            lineTo(cx, cy + r * 0.62f); lineTo(cx - r * 0.42f, cy); close()
                        }
                        canvas.drawPath(dPath, paint)
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.40f, cx - r * 0.76f, cy + r * 0.40f, secondaryPaint)
                        canvas.drawLine(cx + r * 0.76f, cy - r * 0.40f, cx + r * 0.76f, cy + r * 0.40f, secondaryPaint)
                    }
                    205 -> { // 205. סוג צליל: קולן תהודה גבישי (Tuning Fork)
                        canvas.drawArc(RectF(cx - r * 0.42f, cy - r * 0.75f, cx + r * 0.42f, cy + r * 0.15f), 0f, 180f, false, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.30f, cx - r * 0.42f, cy - r * 0.75f, paint)
                        canvas.drawLine(cx + r * 0.42f, cy - r * 0.30f, cx + r * 0.42f, cy - r * 0.75f, paint)
                        canvas.drawLine(cx, cy + r * 0.15f, cx, cy + r * 0.80f, paint)
                    }
                    206 -> { // 206. צליל AC: גל הלם אקוסטי מתפשט
                        canvas.drawCircle(cx - r * 0.45f, cy, r * 0.18f, dotPaint)
                        canvas.drawArc(RectF(cx - r * 0.85f, cy - r * 0.50f, cx + r * 0.25f, cy + r * 0.50f), -55f, 110f, false, paint)
                        canvas.drawArc(RectF(cx - r * 1.05f, cy - r * 0.82f, cx + r * 0.68f, cy + r * 0.82f), -55f, 110f, false, secondaryPaint)
                    }
                    207 -> { // 207. אנימציות: כוכב שביט זוהר עם 3 שובלי אור
                        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.38f, r * 0.22f, dotPaint)
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.78f, cx + r * 0.20f, cy - r * 0.20f, paint)
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.28f, cx - r * 0.10f, cy - r * 0.40f, secondaryPaint)
                        canvas.drawLine(cx - r * 0.28f, cy + r * 0.78f, cx + r * 0.40f, cy + r * 0.10f, secondaryPaint)
                    }
                    208 -> { // 208. מסך תמיד דולק: סהר ירח ארקטי עם כוכב צפוני
                        canvas.drawArc(RectF(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f), 40f, 240f, false, paint)
                        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.38f, r * 0.14f, dotPaint)
                    }
                    209 -> { // 209. צורת כפתורים: משושה משולב במעגל
                        canvas.drawCircle(cx - r * 0.20f, cy - r * 0.20f, r * 0.48f, secondaryPaint)
                        canvas.drawRoundRect(RectF(cx - r * 0.15f, cy - r * 0.15f, cx + r * 0.72f, cy + r * 0.72f), r * 0.20f, r * 0.20f, paint)
                    }
                    210 -> { // 210. דיוק עשרוני: כוונת לייזר טלסקופית
                        canvas.drawCircle(cx, cy, r * 0.65f, secondaryPaint)
                        canvas.drawLine(cx - r * 0.88f, cy, cx + r * 0.88f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.88f, cx, cy + r * 0.88f, paint)
                        canvas.drawCircle(cx, cy, r * 0.15f, dotPaint)
                    }
                    211 -> { // 211. מצב מקצועי Pro Mode: ליבת כור היתוך (Fusion Core)
                        canvas.drawCircle(cx, cy, r * 0.25f, dotPaint)
                        for (i in 0 until 4) {
                            val a = Math.toRadians((i * 90 + 45).toDouble())
                            canvas.drawCircle((cx + cos(a) * r * 0.62f).toFloat(), (cy + sin(a) * r * 0.62f).toFloat(), r * 0.13f, paint)
                        }
                        canvas.drawCircle(cx, cy, r * 0.62f, secondaryPaint)
                    }
                    212 -> { // 212. יד דומיננטית: כפפת הולוגרמה 5 קרניים
                        canvas.drawCircle(cx, cy + r * 0.35f, r * 0.28f, paint)
                        for (ang in listOf(-60, -30, 0, 30, 60)) {
                            val a = Math.toRadians((ang - 90).toDouble())
                            canvas.drawLine(cx, cy + r * 0.10f, (cx + cos(a) * r * 0.82f).toFloat(), (cy + sin(a) * r * 0.82f).toFloat(), paint)
                        }
                    }
                    213 -> { // 213. רשימת סימון: 3 שורות הולוגרמה עם צמתים
                        for (oy in listOf(-0.42f, 0f, 0.42f)) {
                            canvas.drawLine(cx - r * 0.72f, cy + r * oy, cx + r * 0.25f, cy + r * oy, paint)
                            canvas.drawCircle(cx + r * 0.60f, cy + r * oy, r * 0.12f, dotPaint)
                        }
                    }
                    214 -> { // 214. מצב תצוגה: מנסרת אור תלת-גונית
                        val tri = Path().apply {
                            moveTo(cx, cy - r * 0.78f); lineTo(cx + r * 0.76f, cy + r * 0.58f); lineTo(cx - r * 0.76f, cy + r * 0.58f); close()
                        }
                        canvas.drawPath(tri, paint)
                        canvas.drawCircle(cx, cy - r * 0.25f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx - r * 0.28f, cy + r * 0.32f, r * 0.12f, dotPaint)
                        canvas.drawCircle(cx + r * 0.28f, cy + r * 0.32f, r * 0.12f, dotPaint)
                    }
                    // --- סמלי 12 קטגוריות העל בתפריט הצד (300..311) - ייחודיים לחלוטין! ---
                    300 -> { // 300. תגית סימנייה גבישית
                        val bm = Path().apply {
                            moveTo(cx - r * 0.50f, cy - r * 0.78f); lineTo(cx + r * 0.50f, cy - r * 0.78f)
                            lineTo(cx + r * 0.50f, cy + r * 0.78f); lineTo(cx, cy + r * 0.36f)
                            lineTo(cx - r * 0.50f, cy + r * 0.78f); close()
                        }
                        canvas.drawPath(bm, paint)
                    }
                    301 -> { // 301. קטגוריית מתמטיקה ומדע: 4 ריבועי פעולות חשבון זוהרים
                        val s = r * 0.34f
                        canvas.drawRect(cx - s * 2.1f, cy - s * 2.1f, cx - s * 0.3f, cy - s * 0.3f, paint)
                        canvas.drawRect(cx + s * 0.3f, cy - s * 2.1f, cx + s * 2.1f, cy - s * 0.3f, secondaryPaint)
                        canvas.drawRect(cx - s * 2.1f, cy + s * 0.3f, cx - s * 0.3f, cy + s * 2.1f, secondaryPaint)
                        canvas.drawRect(cx + s * 0.3f, cy + s * 0.3f, cx + s * 2.1f, cy + s * 2.1f, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    302 -> { // 302. קטגוריית פיננסים וצרכנות: אסימון קריפטו עם טבעת מסחר כפולה
                        canvas.drawCircle(cx, cy, r * 0.80f, secondaryPaint)
                        val innerDiamond = Path().apply {
                            moveTo(cx, cy - r * 0.50f); lineTo(cx + r * 0.42f, cy)
                            lineTo(cx, cy + r * 0.50f); lineTo(cx - r * 0.42f, cy); close()
                        }
                        canvas.drawPath(innerDiamond, paint)
                    }
                    303 -> { // 303. קטגוריית המרות, רכב ודלק: שני חצי מסלול הפוכים עם ליבת המרה
                        canvas.drawArc(RectF(cx - r * 0.76f, cy - r * 0.76f, cx + r * 0.76f, cy + r * 0.76f), -145f, 125f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.76f, cy - r * 0.76f, cx + r * 0.76f, cy + r * 0.76f), 35f, 125f, false, secondaryPaint)
                        canvas.drawCircle(cx, cy, r * 0.18f, dotPaint)
                    }
                    304 -> { // 304. קטגוריית בריאות, זמן ותאריכים: צלב חיוניות זוהר בתוך טבעת
                        canvas.drawCircle(cx, cy, r * 0.80f, secondaryPaint)
                        canvas.drawLine(cx - r * 0.42f, cy, cx + r * 0.42f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.42f, cx, cy + r * 0.42f, paint)
                    }
                    305 -> { // 305. קטגוריית Pro 87 יחידות: 3 צירים מרחביים (X, Y, Z) עם קודקודים
                        canvas.drawLine(cx, cy, cx, cy - r * 0.82f, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.74f, cy + r * 0.45f, paint)
                        canvas.drawLine(cx, cy, cx - r * 0.74f, cy + r * 0.45f, paint)
                        canvas.drawCircle(cx, cy - r * 0.82f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx + r * 0.74f, cy + r * 0.45f, r * 0.11f, dotPaint)
                        canvas.drawCircle(cx - r * 0.74f, cy + r * 0.45f, r * 0.11f, dotPaint)
                    }
                    306 -> { // 306. קטגוריית Pro הנדסה, אינסטלציה ובנייה: מחוגת שרטוט לייזר
                        canvas.drawCircle(cx, cy - r * 0.68f, r * 0.14f, dotPaint)
                        canvas.drawLine(cx, cy - r * 0.54f, cx - r * 0.62f, cy + r * 0.78f, paint)
                        canvas.drawLine(cx, cy - r * 0.54f, cx + r * 0.62f, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy + r * 0.22f, cx + r * 0.42f, cy + r * 0.22f, secondaryPaint)
                    }
                    307 -> { // 307. קטגוריית Pro מעבדת אלקטרוניקה: גל סינוס חשמלי בתוך טבעת אוסצילוסקופ
                        canvas.drawCircle(cx, cy, r * 0.82f, secondaryPaint)
                        val osc = Path().apply {
                            moveTo(cx - r * 0.65f, cy)
                            lineTo(cx - r * 0.30f, cy - r * 0.48f)
                            lineTo(cx + r * 0.30f, cy + r * 0.48f)
                            lineTo(cx + r * 0.65f, cy)
                        }
                        canvas.drawPath(osc, paint)
                    }
                    308 -> { // 308. קטגוריית Pro חיישנים ומדידה: רדאר סריקה עם טבעות ומחוג
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawCircle(cx, cy, r * 0.44f, secondaryPaint)
                        canvas.drawLine(cx, cy, cx + r * 0.58f, cy - r * 0.58f, paint)
                        canvas.drawCircle(cx + r * 0.32f, cy - r * 0.32f, r * 0.12f, dotPaint)
                    }
                    309 -> { // 309. קטגוריית Pro מדע, אטום, מורס והצפנה: גביש קוונטי משושה עם ליבה משולשת
                        val tri = Path().apply {
                            moveTo(cx, cy - r * 0.75f); lineTo(cx + r * 0.68f, cy + r * 0.45f); lineTo(cx - r * 0.68f, cy + r * 0.45f); close()
                        }
                        canvas.drawPath(tri, paint)
                        canvas.drawCircle(cx, cy + r * 0.05f, r * 0.22f, dotPaint)
                    }
                    310 -> { // 310. קטגוריית Pro השקעות, משכנתא ועסקים: גרף עוגה זוהר עם פלח מופרד
                        canvas.drawArc(RectF(cx - r * 0.76f, cy - r * 0.70f, cx + r * 0.64f, cy + r * 0.76f), 15f, 255f, true, secondaryPaint)
                        canvas.drawArc(RectF(cx - r * 0.60f, cy - r * 0.84f, cx + r * 0.82f, cy + r * 0.58f), -75f, 75f, true, paint)
                    }
                    311 -> { // 311. קטגוריית Pro סטטיסטיקה, גאומטריה ומטריצות: לולאת אינסוף (∞) זוהרת
                        val inf = Path().apply {
                            moveTo(cx, cy)
                            cubicTo(cx + r * 0.45f, cy - r * 0.65f, cx + r * 0.88f, cy - r * 0.55f, cx + r * 0.88f, cy)
                            cubicTo(cx + r * 0.88f, cy + r * 0.55f, cx + r * 0.45f, cy + r * 0.65f, cx, cy)
                            cubicTo(cx - r * 0.45f, cy - r * 0.65f, cx - r * 0.88f, cy - r * 0.55f, cx - r * 0.88f, cy)
                            cubicTo(cx - r * 0.88f, cy + r * 0.55f, cx - r * 0.45f, cy + r * 0.65f, cx, cy)
                        }
                        canvas.drawPath(inf, paint)
                        canvas.drawCircle(cx, cy, r * 0.11f, dotPaint)
                    }
                    else -> {
                        canvas.drawCircle(cx, cy, r * 0.52f, paint)
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

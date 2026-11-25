package com.example.calculator

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

// =========================================================================
// כלי זמן, סטופרים ואימונים מתקדמים (Tabs 118..120)
// 118: סטופר מרווחים ואימון (HIIT / Tabata)
// 119: טיימר ספירה לאחור מרובה (Multi-Timer)
// 120: מחשבון הפרשי שעות, משך ודדליין (כולל חציית חצות)
// =========================================================================

object ProTimeAndTimersTools {

    const val TAB_HIIT_INTERVAL = 118
    const val TAB_MULTI_TIMER = 119
    const val TAB_TIME_INTERVAL_CALC = 120

    const val FIRST_TAB_INDEX = 118
    const val LAST_TAB_INDEX = 120
    const val SECTION_ICON_CODE = 314

    val extraCategories: List<String> = listOf(
        "🏋️ סטופר מרווחים ואימון (HIIT / Tabata)",   // 118
        "⏲️ טיימר ספירה לאחור מרובה (Multi-Timer)",   // 119
        "🕒 מחשבון הפרשי שעות ודדליין"                // 120
    )

    fun resolveAmbientSpec(tabIndex: Int): ProCanvasAnimations.ToolAnimSpec = when (tabIndex) {
        TAB_HIIT_INTERVAL -> ProCanvasAnimations.ToolAnimSpec(
            ProCanvasAnimations.SceneArchetype.RADAR_COMPASS_SWEEP,
            arrayOf("HIIT", "WORK", "REST", "20/10", "45/15"),
            1, 1.08f, 0
        )
        TAB_MULTI_TIMER -> ProCanvasAnimations.ToolAnimSpec(
            ProCanvasAnimations.SceneArchetype.CLOCKWORK_TIME_GEARS,
            arrayOf("01:00", "05:00", "25:00", "00:00.00"),
            2, 0.94f, 1
        )
        else -> ProCanvasAnimations.ToolAnimSpec(
            ProCanvasAnimations.SceneArchetype.NUMBER_SERIES_MATRIX,
            arrayOf("HH:MM", "Δt", "+45m", "24:00"),
            0, 0.90f, 2
        )
    }

    internal fun playShortBeep(activity: MainActivity, highPitch: Boolean = true) {
        if (!activity.useSound) return
        try {
            val toneType = if (highPitch) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
            val tg = ToneGenerator(AudioManager.STREAM_ALARM, 75)
            tg.startTone(toneType, 180)
            Handler(Looper.getMainLooper()).postDelayed({
                try { tg.release() } catch (_: Throwable) {}
            }, 260L)
        } catch (_: Throwable) {}
    }

    internal fun formatClockMs(ms: Long, showCentis: Boolean = true): String {
        val safe = ms.coerceAtLeast(0L)
        val totalSec = safe / 1000L
        val h = totalSec / 3600L
        val m = (totalSec % 3600L) / 60L
        val s = totalSec % 60L
        val cs = (safe % 1000L) / 10L
        return when {
            h > 0L && !showCentis -> String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
            h > 0L -> String.format(Locale.US, "%02d:%02d:%02d.%02d", h, m, s, cs)
            showCentis -> String.format(Locale.US, "%02d:%02d.%02d", m, s, cs)
            else -> String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    internal fun formatShortDuration(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val h = safe / 3600L
        val m = (safe % 3600L) / 60L
        val s = safe % 60L
        return if (h > 0L) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    // =========================================================================
    // סמלים וקטוריים אישיים לכל 4 ערכות הנושא (Tabs 118..120 + כותרת 314)
    // =========================================================================
    fun createSectionIcon(activity: MainActivity, tintColor: Int, sizeDp: Float = 22f): Drawable {
        return createToolIcon(activity, SECTION_ICON_CODE, tintColor, sizeDp)
    }

    fun createToolIcon(
        activity: MainActivity,
        iconCode: Int,
        tintColor: Int,
        sizeDp: Float = 20f
    ): Drawable {
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode

        val innerTint = if (isMin || isAur) tintColor else Color.WHITE
        val innerSizeDp = if (isMin || isAur) sizeDp else (sizeDp * 0.72f)
        val rawGlyph = createRawGlyphDrawable(activity, iconCode, innerTint, innerSizeDp, isMin, isAur)

        return when {
            isMin || isAur -> rawGlyph
            isGlass -> ProUnifiedThemeIcons.wrapForLiquidGlass(activity, rawGlyph, tintColor, sizeDp + 4f)
            else -> ProUnifiedThemeIcons.wrapForClassicBadge(activity, rawGlyph, iconCode, sizeDp + 4f)
        }
    }

    private fun createRawGlyphDrawable(
        context: Context,
        iconCode: Int,
        tintColor: Int,
        sizeDp: Float,
        isMinimalist: Boolean,
        isAurora: Boolean
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(22)
        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = (if (isMinimalist) 1.72f else 1.85f) * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isAurora) {
                    setShadowLayer(4.5f * d, 0f, 0f, Color.argb(175, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.42f

                if (isAurora) {
                    val haloP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        shader = RadialGradient(
                            cx, cy, (r * 1.15f).coerceAtLeast(1f),
                            Color.argb(65, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(cx, cy, r * 1.15f, haloP)
                }

                when (iconCode) {
                    TAB_HIIT_INTERVAL -> { // סטופר מרווחים: טבעת מחולקת + ברק אימון + כפתור עליון
                        val topY = cy - r * 0.84f
                        canvas.drawLine(cx - r * 0.24f, topY, cx + r * 0.24f, topY, strokeP)
                        canvas.drawArc(RectF(cx - r * 0.68f, cy - r * 0.58f, cx + r * 0.68f, cy + r * 0.78f), -70f, 210f, false, strokeP)
                        canvas.drawArc(RectF(cx - r * 0.68f, cy - r * 0.58f, cx + r * 0.68f, cy + r * 0.78f), 165f, 100f, false, strokeP)
                        val bolt = Path().apply {
                            moveTo(cx + r * 0.08f, cy - r * 0.26f)
                            lineTo(cx - r * 0.16f, cy + r * 0.12f)
                            lineTo(cx + r * 0.04f, cy + r * 0.12f)
                            lineTo(cx - r * 0.08f, cy + r * 0.46f)
                        }
                        canvas.drawPath(bolt, strokeP)
                    }
                    TAB_MULTI_TIMER -> { // טיימר מרובה: שני שעוני טיימר חופפים
                        val r1 = r * 0.54f
                        canvas.drawCircle(cx - r * 0.22f, cy + r * 0.14f, r1, strokeP)
                        canvas.drawLine(cx - r * 0.22f, cy + r * 0.14f, cx - r * 0.22f, cy - r * 0.16f, strokeP)
                        canvas.drawLine(cx - r * 0.22f, cy + r * 0.14f, cx + r * 0.04f, cy + r * 0.24f, strokeP)
                        canvas.drawArc(RectF(cx - r * 0.08f, cy - r * 0.76f, cx + r * 0.82f, cy + r * 0.14f), -110f, 220f, false, strokeP)
                        canvas.drawLine(cx + r * 0.37f, cy - r * 0.31f, cx + r * 0.37f, cy - r * 0.54f, strokeP)
                    }
                    TAB_TIME_INTERVAL_CALC -> { // הפרש שעות ודדליין: קשת 24 שעות + חץ מרווח
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), -90f, 250f, false, strokeP)
                        canvas.drawLine(cx, cy, cx, cy - r * 0.46f, strokeP)
                        canvas.drawLine(cx, cy, cx + r * 0.42f, cy + r * 0.18f, strokeP)
                        canvas.drawCircle(cx, cy, r * 0.11f, fillP)
                        // ראש חץ דדליין
                        canvas.drawLine(cx - r * 0.74f, cy - r * 0.16f, cx - r * 0.56f, cy - r * 0.36f, strokeP)
                        canvas.drawLine(cx - r * 0.74f, cy - r * 0.16f, cx - r * 0.50f, cy - r * 0.04f, strokeP)
                    }
                    else -> { // 314: כותרת קטגוריית כלי זמן וסטופרים
                        canvas.drawLine(cx - r * 0.28f, cy - r * 0.84f, cx + r * 0.28f, cy - r * 0.84f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.10f, r * 0.68f, strokeP)
                        canvas.drawLine(cx, cy + r * 0.10f, cx, cy - r * 0.30f, strokeP)
                        canvas.drawLine(cx, cy + r * 0.10f, cx + r * 0.34f, cy + r * 0.24f, strokeP)
                        canvas.drawCircle(cx, cy + r * 0.10f, r * 0.10f, fillP)
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

// =========================================================================
// עזר ציור רקע כרטיס קנבס אחיד לכל 4 ערכות הנושא
// =========================================================================
private fun drawThemeCardBackground(
    canvas: Canvas,
    activity: MainActivity,
    w: Float,
    h: Float,
    d: Float,
    accent: Int,
    pulse: Float,
    rect: RectF,
    fillP: Paint,
    strokeP: Paint
) {
    val style = ProCanvasAnimations.resolveThemeStyle(activity)
    fun withA(col: Int, a: Int) = Color.argb(a.coerceIn(0, 255), Color.red(col), Color.green(col), Color.blue(col))

    rect.set(2f * d, 2f * d, w - 2f * d, h - 2f * d)
    val corner = 22f * d
    val extra = pulse * 1.3f * d
    when (style) {
        ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> {
            fillP.shader = LinearGradient(0f, 0f, w, h, MinimalistTheme.CARD_WHITE, withA(accent, 34 + (pulse * 22).toInt()), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, corner, corner, fillP)
            fillP.shader = null
            strokeP.clearShadowLayer()
            strokeP.strokeWidth = 1.2f * d + extra
            strokeP.color = MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#E3DFD9"))
            canvas.drawRoundRect(rect, corner, corner, strokeP)
        }
        ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> {
            fillP.shader = LinearGradient(0f, 0f, w, h, Color.argb(72, 255, 255, 255), withA(accent, 42 + (pulse * 26).toInt()), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, corner, corner, fillP)
            fillP.shader = null
            strokeP.clearShadowLayer()
            strokeP.strokeWidth = 1.3f * d + extra
            strokeP.color = Color.argb((125 + pulse * 70).toInt().coerceAtMost(255), 255, 255, 255)
            canvas.drawRoundRect(rect, corner, corner, strokeP)
        }
        ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> {
            fillP.shader = LinearGradient(0f, 0f, w, h, withA(AuroraBorealisTheme.EMERALD_NIGHT, 236), withA(accent, 48 + (pulse * 32).toInt()), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(rect, corner, corner, fillP)
            fillP.shader = null
            strokeP.strokeWidth = 1.4f * d + extra
            strokeP.color = withA(accent, 190)
            strokeP.setShadowLayer((6f + pulse * 4f) * d, 0f, 0f, withA(accent, 170))
            canvas.drawRoundRect(rect, corner, corner, strokeP)
        }
        ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> {
            fillP.shader = null
            fillP.color = Utils.lightenColor(activity.bgColor, 0.08f)
            canvas.drawRoundRect(rect, corner, corner, fillP)
            strokeP.clearShadowLayer()
            strokeP.strokeWidth = 0.7f * d
            strokeP.color = withA(accent, 22)
            var gx = 20f * d
            while (gx < w - 10f * d) { canvas.drawLine(gx, 8f * d, gx, h - 8f * d, strokeP); gx += 20f * d }
            strokeP.strokeWidth = 1.3f * d + extra
            strokeP.color = withA(accent, 135 + (pulse * 80).toInt())
            canvas.drawRoundRect(rect, corner, corner, strokeP)
        }
    }
}

private fun MainActivity.createThemedCardBg(cornerDp: Float = 20f, minVariant: Int = 0): Drawable {
    val accentCol = when {
        isMinimalistMode -> MinimalistTheme.ACCENT_SAGE_GREEN
        isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
        else -> primaryColor
    }
    return when {
        isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this, cornerDp, minVariant)
        isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this, cornerDp)
        else -> GradientDrawable().apply {
            cornerRadius = dp(cornerDp).toFloat()
            setColor(Utils.lightenColor(bgColor, 0.07f))
            setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@createThemedCardBg, accentCol))
        }
    }
}

private fun MainActivity.createStyledChip(
    label: String,
    isActive: Boolean,
    accentColor: Int,
    onClick: (View) -> Unit
): TextView {
    val isMin = isMinimalistMode
    val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
    val bgCol = when {
        isActive -> accentColor
        isMin -> MinimalistTheme.NEUTRAL_SECONDARY_BG
        else -> Color.argb(42, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
    }
    val txtCol = MinimalistTheme.ensureReadableTextOnBg(
        if (isActive && !isMin && !isAuroraMode) Color.parseColor("#08090C") else textPrimary,
        bgCol,
        2.3
    )
    return TextView(this).apply {
        tag = "PRO_STAR_CHIP"
        text = label
        setTextColor(txtCol)
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(13f), dp(8f), dp(13f), dp(8f))
        background = GradientDrawable().apply {
            cornerRadius = dp(999f).toFloat()
            setColor(bgCol)
            setStroke(dp(if (isActive) 1.4f else 1f), accentColor)
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(dp(4f), 0, dp(4f), 0) }
        setOnClickListener {
            feedback(it)
            onClick(it)
        }
    }
}

// =========================================================================
// 1. סטופר מרווחים ואימון — HIIT / Tabata (Tab 118)
// =========================================================================
private enum class HiitPhase { IDLE, PREP, WORK, REST, FINISHED }

private object HiitTimerStore {
    var workSec: Int = 45
    var restSec: Int = 15
    var totalRounds: Int = 8
    var prepSec: Int = 5
    var usePrep: Boolean = true

    var phase: HiitPhase = HiitPhase.IDLE
    var currentRound: Int = 1
    var isRunning: Boolean = false
    var phaseTotalMs: Long = 45_000L
    var remainingMs: Long = 45_000L
    var lastTickElapsedMs: Long = 0L

    fun applyPreset(work: Int, rest: Int, rounds: Int) {
        workSec = work.coerceIn(5, 3600)
        restSec = rest.coerceIn(0, 3600)
        totalRounds = rounds.coerceIn(1, 99)
        reset()
    }

    fun reset() {
        isRunning = false
        phase = HiitPhase.IDLE
        currentRound = 1
        phaseTotalMs = workSec * 1000L
        remainingMs = phaseTotalMs
        lastTickElapsedMs = 0L
    }

    fun startOrPause() {
        if (phase == HiitPhase.FINISHED) reset()
        if (!isRunning) {
            if (phase == HiitPhase.IDLE) {
                if (usePrep && prepSec > 0) {
                    phase = HiitPhase.PREP
                    phaseTotalMs = prepSec * 1000L
                    remainingMs = phaseTotalMs
                } else {
                    phase = HiitPhase.WORK
                    phaseTotalMs = workSec * 1000L
                    remainingMs = phaseTotalMs
                }
            }
            lastTickElapsedMs = SystemClock.elapsedRealtime()
            isRunning = true
        } else {
            syncTick(null)
            isRunning = false
        }
    }

    fun adjustCurrentTime(deltaSec: Int) {
        if (phase == HiitPhase.IDLE || phase == HiitPhase.FINISHED) {
            workSec = (workSec + deltaSec).coerceIn(5, 3600)
            phaseTotalMs = workSec * 1000L
            remainingMs = phaseTotalMs
        } else {
            val deltaMs = deltaSec * 1000L
            remainingMs = (remainingMs + deltaMs).coerceIn(1000L, 3_600_000L)
            if (remainingMs > phaseTotalMs) phaseTotalMs = remainingMs
        }
    }

    fun skipPhase(activity: MainActivity?) {
        if (phase == HiitPhase.IDLE) {
            startOrPause()
            return
        }
        if (phase == HiitPhase.FINISHED) return
        advancePhase(activity)
        if (isRunning) lastTickElapsedMs = SystemClock.elapsedRealtime()
    }

    fun syncTick(activity: MainActivity?): Boolean {
        if (!isRunning) return false
        val now = SystemClock.elapsedRealtime()
        val delta = (now - lastTickElapsedMs).coerceAtLeast(0L)
        lastTickElapsedMs = now
        remainingMs -= delta
        var phaseChanged = false
        while (remainingMs <= 0L && isRunning) {
            val overshoot = -remainingMs
            advancePhase(activity)
            phaseChanged = true
            if (isRunning) remainingMs = (remainingMs - overshoot).coerceAtLeast(0L)
        }
        return phaseChanged
    }

    private fun advancePhase(activity: MainActivity?) {
        when (phase) {
            HiitPhase.PREP -> {
                phase = HiitPhase.WORK
                phaseTotalMs = workSec * 1000L
                remainingMs = phaseTotalMs
                if (activity != null) ProTimeAndTimersTools.playShortBeep(activity, true)
            }
            HiitPhase.WORK -> {
                if (currentRound >= totalRounds) {
                    phase = HiitPhase.FINISHED
                    isRunning = false
                    remainingMs = 0L
                    if (activity != null) ProTimeAndTimersTools.playShortBeep(activity, true)
                } else if (restSec > 0) {
                    phase = HiitPhase.REST
                    phaseTotalMs = restSec * 1000L
                    remainingMs = phaseTotalMs
                    if (activity != null) ProTimeAndTimersTools.playShortBeep(activity, false)
                } else {
                    currentRound++
                    phase = HiitPhase.WORK
                    phaseTotalMs = workSec * 1000L
                    remainingMs = phaseTotalMs
                    if (activity != null) ProTimeAndTimersTools.playShortBeep(activity, true)
                }
            }
            HiitPhase.REST -> {
                currentRound++
                phase = HiitPhase.WORK
                phaseTotalMs = workSec * 1000L
                remainingMs = phaseTotalMs
                if (activity != null) ProTimeAndTimersTools.playShortBeep(activity, true)
            }
            else -> {
                isRunning = false
            }
        }
    }

    fun totalWorkoutSeconds(): Long {
        val workTotal = workSec.toLong() * totalRounds
        val restTotal = restSec.toLong() * (totalRounds - 1).coerceAtLeast(0)
        return workTotal + restTotal + if (usePrep) prepSec else 0
    }
}

private class HiitHeroCanvasView(private val activity: MainActivity) : View(activity) {
    private val d = resources.displayMetrics.density
    private var pulsePhase = 0f
    private var anim: ValueAnimator? = null

    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val rect = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ProCanvasAnimations.isCanvasAnimationsEnabled) {
            anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 10_000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    pulsePhase += if (HiitTimerStore.isRunning) 0.055f else 0.018f
                    if (pulsePhase > 6283f) pulsePhase = 0f
                    postInvalidateOnAnimation()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 2f || h <= 2f) return

        val pal = activity.getProCanvasColors()
        val ink = if (pal.isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val phaseCol = when (HiitTimerStore.phase) {
            HiitPhase.WORK -> Color.parseColor(if (pal.isMin) "#43A047" else "#00E676")
            HiitPhase.REST -> Color.parseColor(if (pal.isMin) "#1E88E5" else "#40C4FF")
            HiitPhase.PREP -> Color.parseColor("#FFB300")
            HiitPhase.FINISHED -> pal.accent1
            HiitPhase.IDLE -> pal.positive
        }
        val phaseTitle = when (HiitTimerStore.phase) {
            HiitPhase.WORK -> "זמן עבודה"
            HiitPhase.REST -> "מנוחה והתאוששות"
            HiitPhase.PREP -> "היכון..."
            HiitPhase.FINISHED -> "האימון הושלם!"
            HiitPhase.IDLE -> "מוכן להתחלה"
        }

        val pulse = if (HiitTimerStore.isRunning && ProCanvasAnimations.isCanvasAnimationsEnabled) {
            (sin(pulsePhase * 2f) * 0.5f + 0.5f)
        } else 0f

        drawThemeCardBackground(canvas, activity, w, h, d, phaseCol, pulse * 0.6f, rect, fillP, strokeP)

        val cx = w * 0.5f
        val cy = h * 0.50f
        val r = min(w, h) * 0.37f

        // טבעת רקע חיצונית
        strokeP.clearShadowLayer()
        strokeP.strokeWidth = 9.5f * d
        strokeP.color = Color.argb(34, Color.red(ink), Color.green(ink), Color.blue(ink))
        canvas.drawCircle(cx, cy, r, strokeP)

        // קשת התקדמות של השלב הפעיל
        val frac = if (HiitTimerStore.phaseTotalMs > 0L) {
            (HiitTimerStore.remainingMs.toFloat() / HiitTimerStore.phaseTotalMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
        rect.set(cx - r, cy - r, cx + r, cy + r)
        strokeP.color = phaseCol
        if (ProCanvasAnimations.resolveThemeStyle(activity) == ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON) {
            strokeP.setShadowLayer(7f * d, 0f, 0f, Color.argb(180, Color.red(phaseCol), Color.green(phaseCol), Color.blue(phaseCol)))
        }
        val sweep = if (HiitTimerStore.phase == HiitPhase.FINISHED) 360f else (360f * frac)
        canvas.drawArc(rect, -90f, sweep, false, strokeP)
        strokeP.clearShadowLayer()

        // טבעת פנימית מחולקת לסבבים
        val innerR = r - 15f * d
        val rounds = HiitTimerStore.totalRounds.coerceIn(1, 24)
        val segGap = if (rounds > 12) 3f else 5f
        val segSweep = (360f / rounds) - segGap
        rect.set(cx - innerR, cy - innerR, cx + innerR, cy + innerR)
        strokeP.strokeWidth = 3.4f * d
        for (idx in 0 until rounds) {
            val done = (idx + 1 < HiitTimerStore.currentRound) || (HiitTimerStore.phase == HiitPhase.FINISHED)
            val active = (idx + 1 == HiitTimerStore.currentRound && HiitTimerStore.phase != HiitPhase.IDLE && HiitTimerStore.phase != HiitPhase.FINISHED)
            strokeP.color = when {
                done -> pal.positive
                active -> phaseCol
                else -> Color.argb(40, Color.red(ink), Color.green(ink), Color.blue(ink))
            }
            val startA = -90f + idx * (360f / rounds) + segGap * 0.5f
            canvas.drawArc(rect, startA, segSweep.coerceAtLeast(2f), false, strokeP)
        }

        // נקודת מחוג בקצה הטבעת
        if (HiitTimerStore.phase != HiitPhase.FINISHED) {
            val endRad = Math.toRadians((-90.0 + sweep))
            fillP.color = phaseCol
            canvas.drawCircle(
                cx + (cos(endRad) * r).toFloat(),
                cy + (sin(endRad) * r).toFloat(),
                (5.2f + pulse * 1.8f) * d,
                fillP
            )
        }

        // תצוגת זמן מרכזית (LTR) + שם שלב + מונה סבבים
        textP.textSize = 11.5f * d
        textP.color = phaseCol
        canvas.drawText(phaseTitle, cx, cy - 20f * d, textP)

        textP.textSize = 27f * d
        textP.color = ink
        canvas.drawText("\u200E${ProTimeAndTimersTools.formatClockMs(HiitTimerStore.remainingMs, true)}", cx, cy + 9f * d, textP)

        subP.textSize = 11.5f * d
        subP.color = Color.argb(185, Color.red(ink), Color.green(ink), Color.blue(ink))
        canvas.drawText("סבב ${HiitTimerStore.currentRound} מתוך ${HiitTimerStore.totalRounds}", cx, cy + 28f * d, subP)
    }
}

private fun MainActivity.createHiitIntervalScreen(): View {
    val isMin = isMinimalistMode
    val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
    val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5")
    val accentCol = when {
        isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
        isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
        else -> primaryColor
    }

    val scroll = ScrollView(this).apply { isFillViewport = true }
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(8f), dp(16f), dp(26f))
    }
    scroll.addView(root)

    // 1. כותרת
    val headerCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        background = createThemedCardBg(20f, 1)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    headerCard.addView(TextView(this).apply {
        text = "\u200Fסטופר מרווחים ואימון (HIIT / Tabata)"
        setTextColor(textPrimary); textSize = 18.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
    })
    headerCard.addView(TextView(this).apply {
        text = "\u200Fסבבי עבודה ומנוחה עם התרעת מעבר שלב, הוספת/הפחתת זמן חי ומד סבבים"
        setTextColor(textSecondary); textSize = 13.2f; gravity = Gravity.RIGHT; setPadding(0, dp(3f), 0, 0)
    })
    root.addView(headerCard)

    // 2. קנבס טבעת התקדמות
    val heroCanvas = HiitHeroCanvasView(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(204f)).apply {
            bottomMargin = dp(10f)
        }
    }
    root.addView(heroCanvas)

    // 3. שורת תבניות מהירות (Presets)
    val presetScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    presetScroll.addView(presetRow)
    root.addView(presetScroll)

    lateinit var startPauseBtn: TextView
    lateinit var summaryTv: TextView
    lateinit var workValTv: TextView
    lateinit var restValTv: TextView
    lateinit var roundsValTv: TextView
    lateinit var prepToggleBtn: TextView

    fun refreshUi() {
        startPauseBtn.text = when {
            HiitTimerStore.isRunning -> "השהה ⏸"
            HiitTimerStore.phase == HiitPhase.FINISHED -> "התחל מחדש ↻"
            HiitTimerStore.phase != HiitPhase.IDLE -> "המשך ▶"
            else -> "התחל אימון ▶"
        }
        workValTv.text = "\u200E${HiitTimerStore.workSec}s"
        restValTv.text = "\u200E${HiitTimerStore.restSec}s"
        roundsValTv.text = "\u200E${HiitTimerStore.totalRounds}"
        prepToggleBtn.text = if (HiitTimerStore.usePrep) "ספירת הכנה (5s): מופעל" else "ספירת הכנה (5s): כבוי"

        val totalSec = HiitTimerStore.totalWorkoutSeconds()
        val workSum = HiitTimerStore.workSec.toLong() * HiitTimerStore.totalRounds
        val restSum = HiitTimerStore.restSec.toLong() * (HiitTimerStore.totalRounds - 1).coerceAtLeast(0)
        val cal = Calendar.getInstance().apply { add(Calendar.SECOND, totalSec.toInt()) }
        val finishClock = String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

        summaryTv.text = BidiFmt.rtlParagraph(
            "משך אימון כולל: ${BidiFmt.ltr(ProTimeAndTimersTools.formatShortDuration(totalSec))}",
            "עבודה נטו: ${BidiFmt.ltr(ProTimeAndTimersTools.formatShortDuration(workSum))}  |  מנוחה: ${BidiFmt.ltr(ProTimeAndTimersTools.formatShortDuration(restSum))}",
            "שעת סיום משוערת מהרגע: ${BidiFmt.ltr(finishClock)}"
        )
        heroCanvas.invalidate()
    }

    val presets = listOf(
        Triple("HIIT 45/15 ×8", 45 to 15, 8),
        Triple("Tabata 20/10 ×8", 20 to 10, 8),
        Triple("כוח 60/90 ×5", 60 to 90, 5),
        Triple("EMOM 50/10 ×10", 50 to 10, 10),
        Triple("מנוחת סט 90s ×4", 30 to 90, 4)
    )
    for ((lbl, wr, rnd) in presets) {
        presetRow.addView(createStyledChip(lbl, false, accentCol) {
            HiitTimerStore.applyPreset(wr.first, wr.second, rnd)
            refreshUi()
        })
    }

    // 4. כפתורי שליטה מרכזיים
    val controlsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = if (isRightHanded) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(6f)
        }
    }
    startPauseBtn = createActionBtn("התחל אימון ▶", true) {
        HiitTimerStore.startOrPause()
        refreshUi()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f).apply {
            setMargins(dp(4f), 0, dp(4f), 0)
        }
    }
    val resetBtn = createActionBtn("איפוס ↻", false) {
        HiitTimerStore.reset()
        refreshUi()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f).apply {
            setMargins(dp(4f), 0, dp(4f), 0)
        }
    }
    controlsRow.addView(startPauseBtn)
    controlsRow.addView(resetBtn)
    root.addView(controlsRow)

    // 5. התאמת זמן בזמן אמת (-15s / +15s / דלג שלב)
    val liveAdjustRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12f)
        }
    }
    val minus15 = createStyledChip("-15s", false, accentCol) {
        HiitTimerStore.adjustCurrentTime(-15)
        refreshUi()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4f), 0, dp(4f), 0) } }
    val skipBtn = createStyledChip("דלג שלב ⏭", false, accentCol) {
        HiitTimerStore.skipPhase(this)
        refreshUi()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply { setMargins(dp(4f), 0, dp(4f), 0) } }
    val plus15 = createStyledChip("+15s", false, accentCol) {
        HiitTimerStore.adjustCurrentTime(15)
        refreshUi()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4f), 0, dp(4f), 0) } }
    liveAdjustRow.addView(minus15)
    liveAdjustRow.addView(skipBtn)
    liveAdjustRow.addView(plus15)
    root.addView(liveAdjustRow)

    // 6. כרטיס הגדרת זמני עבודה, מנוחה וסבבים
    val configCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        background = createThemedCardBg(20f, 0)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }

    fun buildStepperRow(label: String, onDelta: (Int) -> Unit): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, dp(6f), 0, dp(6f))
        }
        val minusBtn = createStyledChip("−", false, accentCol) { onDelta(-1); refreshUi() }.apply {
            minimumWidth = dp(44f)
        }
        val valTv = TextView(this).apply {
            setTextColor(textPrimary); textSize = 16.5f; typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR
            layoutParams = LinearLayout.LayoutParams(dp(72f), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val plusBtn = createStyledChip("+", false, accentCol) { onDelta(1); refreshUi() }.apply {
            minimumWidth = dp(44f)
        }
        val titleTv = TextView(this).apply {
            text = "\u200F$label"
            setTextColor(textPrimary); textSize = 15f; gravity = Gravity.RIGHT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(8f), 0, dp(4f), 0)
            }
        }
        row.addView(minusBtn); row.addView(valTv); row.addView(plusBtn); row.addView(titleTv)
        configCard.addView(row)
        return valTv
    }

    workValTv = buildStepperRow("זמן עבודה (קפיצות 5 שניות)") { dir ->
        HiitTimerStore.workSec = (HiitTimerStore.workSec + dir * 5).coerceIn(5, 3600)
        if (HiitTimerStore.phase == HiitPhase.IDLE) HiitTimerStore.reset()
    }
    restValTv = buildStepperRow("זמן מנוחה (קפיצות 5 שניות)") { dir ->
        HiitTimerStore.restSec = (HiitTimerStore.restSec + dir * 5).coerceIn(0, 3600)
        if (HiitTimerStore.phase == HiitPhase.IDLE) HiitTimerStore.reset()
    }
    roundsValTv = buildStepperRow("מספר סבבים") { dir ->
        HiitTimerStore.totalRounds = (HiitTimerStore.totalRounds + dir).coerceIn(1, 99)
    }

    prepToggleBtn = createActionBtn("ספירת הכנה (5s): מופעל", false) {
        HiitTimerStore.usePrep = !HiitTimerStore.usePrep
        refreshUi()
    }.apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(8f)
        }
    }
    configCard.addView(prepToggleBtn)
    root.addView(configCard)

    // 7. כרטיס סיכום אימון
    val summaryCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(12f), dp(16f), dp(12f))
        background = createThemedCardBg(20f, 0)
    }
    summaryTv = createResultTextView().apply {
        background = null; setPadding(0, 0, 0, 0); textSize = 15.5f
    }
    summaryCard.addView(summaryTv)
    root.addView(summaryCard)

    // לולאת עדכון מדויקת המנוקה אוטומטית ביציאה מהמסך
    val handler = Handler(Looper.getMainLooper())
    val ticker = object : Runnable {
        override fun run() {
            if (HiitTimerStore.isRunning) {
                val changed = HiitTimerStore.syncTick(this@createHiitIntervalScreen)
                if (changed) refreshUi() else heroCanvas.invalidate()
            }
            handler.postDelayed(this, 45L)
        }
    }
    scroll.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            HiitTimerStore.syncTick(this@createHiitIntervalScreen)
            refreshUi()
            handler.post(ticker)
        }
        override fun onViewDetachedFromWindow(v: View) {
            handler.removeCallbacks(ticker)
        }
    })

    refreshUi()
    return scroll
}

// =========================================================================
// 2. טיימר ספירה לאחור מרובה — Multi-Timer (Tab 119)
// =========================================================================
private data class CountdownItem(
    val id: Int,
    var label: String,
    val targetMs: Long,
    var remainingMs: Long = targetMs,
    var isRunning: Boolean = false,
    var isFinished: Boolean = false,
    var lastTickElapsedMs: Long = 0L
)

private object MultiTimerStore {
    private var nextId = 1
    var showCentis: Boolean = true
    val timers: MutableList<CountdownItem> = mutableListOf(
        CountdownItem(nextId++, "מנוחת סט / פוקוס קצר", 3 * 60_000L),
        CountdownItem(nextId++, "סשן פומודורו", 25 * 60_000L)
    )

    fun addTimer(label: String, durationMs: Long, autoStart: Boolean = false): CountdownItem {
        val safeMs = durationMs.coerceIn(1000L, 99 * 3_600_000L)
        val cleanName = label.trim().ifEmpty { "טיימר #${timers.size + 1}" }
        val item = CountdownItem(
            id = nextId++,
            label = cleanName,
            targetMs = safeMs,
            remainingMs = safeMs,
            isRunning = autoStart,
            isFinished = false,
            lastTickElapsedMs = if (autoStart) SystemClock.elapsedRealtime() else 0L
        )
        timers.add(0, item)
        return item
    }

    fun toggleTimer(item: CountdownItem) {
        if (item.isFinished) {
            item.remainingMs = item.targetMs
            item.isFinished = false
        }
        if (!item.isRunning) {
            item.lastTickElapsedMs = SystemClock.elapsedRealtime()
            item.isRunning = true
        } else {
            syncSingle(item, null)
            item.isRunning = false
        }
    }

    fun resetTimer(item: CountdownItem) {
        item.isRunning = false
        item.isFinished = false
        item.remainingMs = item.targetMs
        item.lastTickElapsedMs = 0L
    }

    fun addMinute(item: CountdownItem) {
        item.remainingMs = (item.remainingMs + 60_000L).coerceAtMost(99 * 3_600_000L)
        item.isFinished = false
    }

    fun syncAll(activity: MainActivity?): Boolean {
        var anyFinishedNow = false
        for (t in timers) {
            if (syncSingle(t, activity)) anyFinishedNow = true
        }
        return anyFinishedNow
    }

    private fun syncSingle(t: CountdownItem, activity: MainActivity?): Boolean {
        if (!t.isRunning) return false
        val now = SystemClock.elapsedRealtime()
        val delta = (now - t.lastTickElapsedMs).coerceAtLeast(0L)
        t.lastTickElapsedMs = now
        t.remainingMs -= delta
        if (t.remainingMs <= 0L) {
            t.remainingMs = 0L
            t.isRunning = false
            t.isFinished = true
            if (activity != null) ProTimeAndTimersTools.playShortBeep(activity, true)
            return true
        }
        return false
    }
}

private class MultiTimerHeroCanvasView(private val activity: MainActivity) : View(activity) {
    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var anim: ValueAnimator? = null

    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val rect = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ProCanvasAnimations.isCanvasAnimationsEnabled) {
            anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 10_000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    phase += 0.045f
                    if (phase > 6283f) phase = 0f
                    postInvalidateOnAnimation()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 2f || h <= 2f) return

        val pal = activity.getProCanvasColors()
        val ink = if (pal.isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val anyRunning = MultiTimerStore.timers.any { it.isRunning }
        val anyFinished = MultiTimerStore.timers.any { it.isFinished }
        val accent = if (anyFinished) Color.parseColor("#EF5350") else pal.accent1
        val pulse = if ((anyRunning || anyFinished) && ProCanvasAnimations.isCanvasAnimationsEnabled) {
            (sin(phase * 2.2f) * 0.5f + 0.5f)
        } else 0f

        drawThemeCardBackground(canvas, activity, w, h, d, accent, pulse * 0.6f, rect, fillP, strokeP)

        val visible = MultiTimerStore.timers.take(4)
        if (visible.isEmpty()) {
            textP.textSize = 14f * d; textP.color = ink
            canvas.drawText("אין טיימרים פעילים — בחר קיצור מהיר למטה", w * 0.5f, h * 0.52f, textP)
            return
        }

        val cols = visible.size
        val slotW = (w - 20f * d) / cols
        val ringR = min(slotW * 0.36f, h * 0.27f).coerceIn(22f * d, 40f * d)
        val cy = h * 0.48f
        val ringColors = listOf(pal.accent1, pal.positive, pal.accent3, pal.warning)

        for ((i, item) in visible.withIndex()) {
            val cx = 10f * d + slotW * (i + 0.5f)
            val col = if (item.isFinished) Color.parseColor("#EF5350") else ringColors[i % ringColors.size]
            val frac = if (item.targetMs > 0L) (item.remainingMs.toFloat() / item.targetMs.toFloat()).coerceIn(0f, 1f) else 0f

            strokeP.clearShadowLayer()
            strokeP.strokeWidth = 5.5f * d
            strokeP.color = Color.argb(35, Color.red(ink), Color.green(ink), Color.blue(ink))
            canvas.drawCircle(cx, cy, ringR, strokeP)

            rect.set(cx - ringR, cy - ringR, cx + ringR, cy + ringR)
            strokeP.color = col
            canvas.drawArc(rect, -90f, 360f * frac, false, strokeP)

            textP.textSize = (if (cols >= 3) 9.8f else 12.2f) * d
            textP.color = if (item.isFinished) col else ink
            val shortTime = if (item.isFinished) "תם!" else ProTimeAndTimersTools.formatClockMs(item.remainingMs, MultiTimerStore.showCentis && cols <= 2)
            canvas.drawText("\u200E$shortTime", cx, cy + 4f * d, textP)

            subP.textSize = 9.2f * d
            subP.color = Color.argb(185, Color.red(ink), Color.green(ink), Color.blue(ink))
            val trimmedTitle = if (item.label.length > 10) item.label.take(9) + "…" else item.label
            canvas.drawText(trimmedTitle, cx, cy + ringR + 15f * d, subP)
        }

        val runningCount = MultiTimerStore.timers.count { it.isRunning }
        subP.textSize = 10f * d
        subP.color = Color.argb(175, Color.red(ink), Color.green(ink), Color.blue(ink))
        canvas.drawText("פעילים כרגע: $runningCount מתוך ${MultiTimerStore.timers.size}", w * 0.5f, 18f * d, subP)
    }
}

private fun MainActivity.createMultiTimerScreen(): View {
    val isMin = isMinimalistMode
    val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
    val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5")
    val accentCol = when {
        isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
        isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
        else -> primaryColor
    }

    val scroll = ScrollView(this).apply { isFillViewport = true }
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(8f), dp(16f), dp(26f))
    }
    scroll.addView(root)

    // 1. כותרת
    val headerCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        background = createThemedCardBg(20f, 1)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    headerCard.addView(TextView(this).apply {
        text = "\u200Fטיימר ספירה לאחור מרובה (Multi-Timer)"
        setTextColor(textPrimary); textSize = 18.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
    })
    headerCard.addView(TextView(this).apply {
        text = "\u200Fהפעל מספר טיימרים במקביל עם שמירת זמן היעד המקורי לאיפוס מיידי"
        setTextColor(textSecondary); textSize = 13.2f; gravity = Gravity.RIGHT; setPadding(0, dp(3f), 0, 0)
    })
    root.addView(headerCard)

    // 2. קנבס טבעות מסונכרנות
    val heroCanvas = MultiTimerHeroCanvasView(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(156f)).apply {
            bottomMargin = dp(10f)
        }
    }
    root.addView(heroCanvas)

    // 3. קיצורים מהירים (1, 3, 5, 10, 25, 45 דק׳)
    val presetScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    val presetRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    presetScroll.addView(presetRow)
    root.addView(presetScroll)

    val listContainer = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    // מיפוי תצוגות חיות לעדכון מהיר בלי לבנות מחדש את העץ בכל Frame
    data class TimerCardHandles(
        val item: CountdownItem,
        val timeTv: TextView,
        val statusTv: TextView,
        val progressFill: View,
        val progressTrack: FrameLayout,
        val toggleBtn: TextView
    )
    val cardHandles = mutableListOf<TimerCardHandles>()

    fun updateLiveTimerTexts() {
        for (h in cardHandles) {
            val item = h.item
            h.timeTv.text = "\u200E${ProTimeAndTimersTools.formatClockMs(item.remainingMs, MultiTimerStore.showCentis)}"
            h.statusTv.text = when {
                item.isFinished -> "הזמן תם!"
                item.isRunning -> "פועל..."
                item.remainingMs < item.targetMs -> "מושהה"
                else -> "מוכן"
            }
            h.statusTv.setTextColor(
                when {
                    item.isFinished -> Color.parseColor("#EF5350")
                    item.isRunning -> if (isMin) Color.parseColor("#2E7D32") else Color.parseColor("#00E676")
                    else -> textSecondary
                }
            )
            h.toggleBtn.text = if (item.isRunning) "השהה" else if (item.isFinished) "הפעל שוב" else "התחל"
            val trackW = h.progressTrack.width
            if (trackW > 0 && item.targetMs > 0L) {
                val frac = (item.remainingMs.toFloat() / item.targetMs.toFloat()).coerceIn(0f, 1f)
                val lp = h.progressFill.layoutParams
                val targetW = (trackW * frac).toInt().coerceAtLeast(0)
                if (lp.width != targetW) {
                    lp.width = targetW
                    h.progressFill.layoutParams = lp
                }
            }
        }
        heroCanvas.invalidate()
    }

    lateinit var rebuildList: () -> Unit

    rebuildList = {
        listContainer.removeAllViews()
        cardHandles.clear()

        if (MultiTimerStore.timers.isEmpty()) {
            listContainer.addView(TextView(this).apply {
                text = "אין טיימרים ברשימה. הוסף טיימר מהקיצורים או בנה טיימר מותאם למטה."
                setTextColor(textSecondary); textSize = 14f; gravity = Gravity.CENTER; setPadding(dp(12f), dp(22f), dp(12f), dp(22f))
            })
        }

        for (item in MultiTimerStore.timers) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
                background = createThemedCardBg(18f, 0)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(10f)
                }
            }

            // שורה עליונה: שם + תווית יעד מקורי + סטטוס
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }
            val statusTv = TextView(this).apply {
                textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.LEFT
            }
            val titleCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.RIGHT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            titleCol.addView(TextView(this).apply {
                text = "\u200F${item.label}"
                setTextColor(textPrimary); textSize = 15.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
            })
            titleCol.addView(TextView(this).apply {
                text = "\u200Fיעד מקורי: ${BidiFmt.ltr(ProTimeAndTimersTools.formatShortDuration(item.targetMs / 1000L))}"
                setTextColor(textSecondary); textSize = 12f; gravity = Gravity.RIGHT
            })
            topRow.addView(statusTv)
            topRow.addView(titleCol)
            card.addView(topRow)

            // תצוגת זמן גדולה (LTR)
            val timeTv = TextView(this).apply {
                text = "\u200E${ProTimeAndTimersTools.formatClockMs(item.remainingMs, MultiTimerStore.showCentis)}"
                textDirection = View.TEXT_DIRECTION_LTR
                setTextColor(textPrimary)
                textSize = 28f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(0, dp(6f), 0, dp(6f))
            }
            card.addView(timeTv)

            // פס התקדמות
            val track = FrameLayout(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(6f).toFloat()
                    setColor(Color.argb(32, Color.red(textPrimary), Color.green(textPrimary), Color.blue(textPrimary)))
                }
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6f)).apply {
                    bottomMargin = dp(10f)
                }
            }
            val fill = View(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(6f).toFloat()
                    setColor(accentCol)
                }
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            }
            track.addView(fill)
            card.addView(track)

            // שורת כפתורים לכרטיס
            val btnRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = if (isRightHanded) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
            }
            lateinit var toggleBtn: TextView
            toggleBtn = createStyledChip(if (item.isRunning) "השהה" else "התחל", true, accentCol) {
                MultiTimerStore.toggleTimer(item)
                updateLiveTimerTexts()
            }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply { setMargins(dp(3f), 0, dp(3f), 0) } }

            val plus1mBtn = createStyledChip("+1m", false, accentCol) {
                MultiTimerStore.addMinute(item)
                updateLiveTimerTexts()
            }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f).apply { setMargins(dp(3f), 0, dp(3f), 0) } }

            val resetItemBtn = createStyledChip("איפוס", false, accentCol) {
                MultiTimerStore.resetTimer(item)
                updateLiveTimerTexts()
            }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f).apply { setMargins(dp(3f), 0, dp(3f), 0) } }

            val delBtn = createStyledChip("✕", false, Color.parseColor("#E57373")) {
                MultiTimerStore.timers.remove(item)
                rebuildList()
            }.apply { layoutParams = LinearLayout.LayoutParams(dp(42f), LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(3f), 0, dp(3f), 0) } }

            btnRow.addView(toggleBtn)
            btnRow.addView(plus1mBtn)
            btnRow.addView(resetItemBtn)
            btnRow.addView(delBtn)
            card.addView(btnRow)

            listContainer.addView(card)
            cardHandles.add(TimerCardHandles(item, timeTv, statusTv, fill, track, toggleBtn))
        }
        listContainer.post { updateLiveTimerTexts() }
    }

    val quickMinutes = listOf(1 to "1 דק׳", 3 to "3 דק׳", 5 to "5 דק׳", 10 to "10 דק׳", 25 to "25 דק׳ (פומודורו)", 45 to "45 דק׳")
    for ((mins, lbl) in quickMinutes) {
        presetRow.addView(createStyledChip("+$lbl", false, accentCol) {
            MultiTimerStore.addTimer(lbl, mins * 60_000L, autoStart = true)
            rebuildList()
        })
    }

    // 4. שורת בקרה כללית + מתג פורמט שברי שנייה
    val globalRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutDirection = if (isRightHanded) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    lateinit var fmtBtn: TextView
    fmtBtn = createStyledChip(if (MultiTimerStore.showCentis) "פורמט: 00:00.00" else "פורמט: HH:MM:SS", false, accentCol) {
        MultiTimerStore.showCentis = !MultiTimerStore.showCentis
        fmtBtn.text = if (MultiTimerStore.showCentis) "פורמט: 00:00.00" else "פורמט: HH:MM:SS"
        updateLiveTimerTexts()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f).apply { setMargins(dp(3f), 0, dp(3f), 0) } }

    val startAllBtn = createStyledChip("התחל הכל", false, accentCol) {
        for (t in MultiTimerStore.timers) if (!t.isRunning && !t.isFinished) MultiTimerStore.toggleTimer(t)
        updateLiveTimerTexts()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3f), 0, dp(3f), 0) } }

    val pauseAllBtn = createStyledChip("השהה הכל", false, accentCol) {
        for (t in MultiTimerStore.timers) if (t.isRunning) MultiTimerStore.toggleTimer(t)
        updateLiveTimerTexts()
    }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3f), 0, dp(3f), 0) } }

    globalRow.addView(fmtBtn)
    globalRow.addView(startAllBtn)
    globalRow.addView(pauseAllBtn)
    root.addView(globalRow)

    // 5. הוספת טיימר מותאם אישית
    val addCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
        background = createThemedCardBg(20f, 0)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12f)
        }
    }
    addCard.addView(TextView(this).apply {
        text = "הוסף טיימר מותאם אישית:"
        setTextColor(textPrimary); textSize = 14.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
        setPadding(0, 0, 0, dp(8f))
    })

    val nameInput = createInputBox("שם הטיימר (אופציונלי, למשל: תנור / למידה)").apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_TEXT
    }
    addCard.addView(nameInput)

    val hmsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
    }
    fun createNumField(hintTxt: String, def: String): EditText = createInputBox(hintTxt).apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_NUMBER
        textDirection = View.TEXT_DIRECTION_LTR
        gravity = Gravity.CENTER
        setText(def)
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dp(4f), 0, dp(4f), dp(10f))
        }
    }
    val hBox = createNumField("שעות", "0")
    val mBox = createNumField("דקות", "5")
    val sBox = createNumField("שניות", "0")
    hmsRow.addView(hBox); hmsRow.addView(mBox); hmsRow.addView(sBox)
    addCard.addView(hmsRow)

    val addTimerBtn = createActionBtn("+ הוסף והפעל טיימר", true) {
        val hh = hBox.text.toString().toLongOrNull()?.coerceIn(0, 99) ?: 0L
        val mm = mBox.text.toString().toLongOrNull()?.coerceIn(0, 599) ?: 0L
        val ss = sBox.text.toString().toLongOrNull()?.coerceIn(0, 599) ?: 0L
        val totalMs = (hh * 3600L + mm * 60L + ss) * 1000L
        if (totalMs <= 0L) {
            Toast.makeText(this, "הזן משך זמן גדול מאפס", Toast.LENGTH_SHORT).show()
            return@createActionBtn
        }
        MultiTimerStore.addTimer(nameInput.text.toString(), totalMs, autoStart = true)
        nameInput.setText("")
        rebuildList()
    }
    addCard.addView(addTimerBtn)
    root.addView(addCard)
    root.addView(listContainer)

    val handler = Handler(Looper.getMainLooper())
    val ticker = object : Runnable {
        override fun run() {
            if (MultiTimerStore.timers.any { it.isRunning }) {
                MultiTimerStore.syncAll(this@createMultiTimerScreen)
                updateLiveTimerTexts()
            }
            handler.postDelayed(this, 45L)
        }
    }
    scroll.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            MultiTimerStore.syncAll(this@createMultiTimerScreen)
            rebuildList()
            handler.post(ticker)
        }
        override fun onViewDetachedFromWindow(v: View) {
            handler.removeCallbacks(ticker)
        }
    })

    rebuildList()
    return scroll
}

// =========================================================================
// 3. מחשבון הפרשי שעות, משך ודדליין (Tab 120)
// =========================================================================
private class TimeDeltaHeroCanvasView(private val activity: MainActivity) : View(activity) {
    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var reactPulse = 0f
    private var anim: ValueAnimator? = null

    var startSecOfDay: Int = 9 * 3600
    var endSecOfDay: Int = 17 * 3600 + 30 * 60
    var crossesMidnight: Boolean = false
    var dayShift: Int = 0
    var topBadge: String = "הפרש זמן (Δt)"
    var bottomSummary: String = "\u200E09:00:00 → 17:30:00  |  Δt = 08:30:00"
    var isSubtractMode: Boolean = false

    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dashedP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.3f * d
        pathEffect = DashPathEffect(floatArrayOf(4f * d, 3f * d), 0f)
    }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val rect = RectF()

    fun updateTimeline(
        startSec: Int,
        endSec: Int,
        midnight: Boolean,
        shiftDays: Int,
        badge: String,
        summary: String,
        subtract: Boolean
    ) {
        startSecOfDay = ((startSec % 86400) + 86400) % 86400
        endSecOfDay = ((endSec % 86400) + 86400) % 86400
        crossesMidnight = midnight
        dayShift = shiftDays
        topBadge = badge
        bottomSummary = summary
        isSubtractMode = subtract
        reactPulse = 1f
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ProCanvasAnimations.isCanvasAnimationsEnabled) {
            anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 12_000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    phase += 0.025f
                    if (phase > 6283f) phase = 0f
                    if (reactPulse > 0f) reactPulse = (reactPulse - 0.08f).coerceAtLeast(0f)
                    postInvalidateOnAnimation()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 2f || h <= 2f) return

        val pal = activity.getProCanvasColors()
        val ink = if (pal.isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val arcCol = if (crossesMidnight) pal.warning else pal.accent1

        drawThemeCardBackground(canvas, activity, w, h, d, arcCol, reactPulse * 0.7f, rect, fillP, strokeP)

        val cx = w * 0.5f
        val cy = h * 0.47f
        val r = min(w, h) * 0.30f

        // חוגת 24 שעות (00 למעלה, 06 מימין, 12 למטה, 18 משמאל)
        strokeP.clearShadowLayer()
        strokeP.strokeWidth = 6f * d
        strokeP.color = Color.argb(34, Color.red(ink), Color.green(ink), Color.blue(ink))
        canvas.drawCircle(cx, cy, r, strokeP)

        // סימון חצות מקווקו (00:00)
        dashedP.color = Color.argb(145, Color.red(pal.warning), Color.green(pal.warning), Color.blue(pal.warning))
        canvas.drawLine(cx, cy - r - 9f * d, cx, cy - r + 7f * d, dashedP)

        // קשת הזמן הפעילה
        val startDeg = -90f + (startSecOfDay / 86400f) * 360f
        val endDeg = -90f + (endSecOfDay / 86400f) * 360f
        var sweepDeg = if (isSubtractMode) {
            var diff = endDeg - startDeg
            if (diff > 0f) diff -= 360f
            diff
        } else {
            var diff = endDeg - startDeg
            if (diff <= 0f && (crossesMidnight || dayShift > 0)) diff += 360f
            diff
        }
        if (abs(sweepDeg) < 2f && dayShift != 0) sweepDeg = if (dayShift > 0) 358f else -358f

        rect.set(cx - r, cy - r, cx + r, cy + r)
        strokeP.strokeWidth = 7f * d
        strokeP.color = arcCol
        canvas.drawArc(rect, startDeg, sweepDeg, false, strokeP)

        // נקודות התחלה וסיום על ההיקף
        val sRad = Math.toRadians(startDeg.toDouble())
        val eRad = Math.toRadians(endDeg.toDouble())
        fillP.color = pal.positive
        canvas.drawCircle(cx + (cos(sRad) * r).toFloat(), cy + (sin(sRad) * r).toFloat(), 4.8f * d, fillP)
        fillP.color = pal.accent2
        canvas.drawCircle(cx + (cos(eRad) * r).toFloat(), cy + (sin(eRad) * r).toFloat(), (5.2f + reactPulse * 1.5f) * d, fillP)

        // תוויות שעות 24h
        subP.textSize = 8.8f * d
        subP.color = Color.argb(180, Color.red(ink), Color.green(ink), Color.blue(ink))
        canvas.drawText("00:00", cx, cy - r - 11f * d, subP)
        canvas.drawText("06:00", cx + r + 20f * d, cy + 3f * d, subP)
        canvas.drawText("12:00", cx, cy + r + 13f * d, subP)
        canvas.drawText("18:00", cx - r - 20f * d, cy + 3f * d, subP)

        // טקסט מרכזי בחוגה
        textP.textSize = 10.5f * d; textP.color = arcCol
        canvas.drawText(topBadge, cx, cy - 3f * d, textP)
        subP.textSize = 9.5f * d; subP.color = Color.argb(200, Color.red(ink), Color.green(ink), Color.blue(ink))
        val shiftText = when {
            crossesMidnight -> "חוצה חצות (+1 יום)"
            dayShift > 0 -> "+$dayShift ימים"
            dayShift < 0 -> "$dayShift ימים"
            else -> "באותה יממה"
        }
        canvas.drawText(shiftText, cx, cy + 12f * d, subP)

        // שורת סיכום תחתונה (LTR)
        textP.textSize = 11.8f * d; textP.color = ink
        canvas.drawText(bottomSummary, cx, h - 9f * d, textP)
    }
}

private fun MainActivity.createTimeIntervalDeadlineScreen(): View {
    val isMin = isMinimalistMode
    val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
    val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5")
    val accentCol = when {
        isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
        isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
        else -> primaryColor
    }

    val scroll = ScrollView(this).apply { isFillViewport = true }
    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(8f), dp(16f), dp(26f))
    }
    scroll.addView(root)

    // 1. כותרת
    val headerCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        background = createThemedCardBg(20f, 1)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    headerCard.addView(TextView(this).apply {
        text = "\u200Fמחשבון הפרשי שעות, משך ודדליין"
        setTextColor(textPrimary); textSize = 18.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
    })
    headerCard.addView(TextView(this).apply {
        text = "\u200Fחישוב הפרש מדויק כולל חציית חצות, או הוספה/חיסור משך לשעת יעד"
        setTextColor(textSecondary); textSize = 13.2f; gravity = Gravity.RIGHT; setPadding(0, dp(3f), 0, 0)
    })
    root.addView(headerCard)

    var activeMode = 0 // 0 = הפרש בין שעות, 1 = הוספה/חיסור משך ודדליין
    var isAddOperation = true

    // 2. סרגל 2 תת-מצבים
    val tabsRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10f)
        }
    }
    root.addView(tabsRow)

    // 3. קנבס חוגת 24 שעות
    val heroCanvas = TimeDeltaHeroCanvasView(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(188f)).apply {
            bottomMargin = dp(10f)
        }
    }
    root.addView(heroCanvas)

    // קונטיינרים לשני המצבים
    val diffContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    val addContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }

    fun createSmallNumInput(hintStr: String, init: String): EditText = createInputBox(hintStr).apply {
        tag = "HAS_CUSTOM_LIVE_WATCHER"
        inputType = InputType.TYPE_CLASS_NUMBER
        textDirection = View.TEXT_DIRECTION_LTR
        gravity = Gravity.CENTER
        setText(init)
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(dp(3f), 0, dp(3f), dp(8f))
        }
    }

    // --- שדות מצב 1: הפרש בין שתי שעות ---
    val nowCal = Calendar.getInstance()
    val startH = createSmallNumInput("שעה", String.format(Locale.US, "%02d", nowCal.get(Calendar.HOUR_OF_DAY)))
    val startM = createSmallNumInput("דקה", String.format(Locale.US, "%02d", nowCal.get(Calendar.MINUTE)))
    val startS = createSmallNumInput("שנייה", "00")

    val endH = createSmallNumInput("שעה", String.format(Locale.US, "%02d", (nowCal.get(Calendar.HOUR_OF_DAY) + 2) % 24))
    val endM = createSmallNumInput("דקה", "30")
    val endS = createSmallNumInput("שנייה", "00")
    val extraDaysBox = createSmallNumInput("ימים נוספים", "0")

    fun buildTimeInputGroup(parent: LinearLayout, title: String, h: EditText, m: EditText, s: EditText, onNow: () -> Unit) {
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(2f), dp(2f), dp(2f), dp(6f))
        }
        val nowChip = createStyledChip("עכשיו", false, accentCol) { onNow() }
        val lbl = TextView(this).apply {
            text = "\u200F$title"; setTextColor(textPrimary); textSize = 14.5f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        top.addView(nowChip); top.addView(lbl)
        parent.addView(top)

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_LTR }
        row.addView(h); row.addView(m); row.addView(s)
        parent.addView(row)
    }

    lateinit var recalc: () -> Unit

    buildTimeInputGroup(diffContainer, "שעת התחלה (HH : MM : SS)", startH, startM, startS) {
        val c = Calendar.getInstance()
        startH.setText(String.format(Locale.US, "%02d", c.get(Calendar.HOUR_OF_DAY)))
        startM.setText(String.format(Locale.US, "%02d", c.get(Calendar.MINUTE)))
        startS.setText(String.format(Locale.US, "%02d", c.get(Calendar.SECOND)))
        recalc()
    }
    buildTimeInputGroup(diffContainer, "שעת סיום (HH : MM : SS)", endH, endM, endS) {
        val c = Calendar.getInstance()
        endH.setText(String.format(Locale.US, "%02d", c.get(Calendar.HOUR_OF_DAY)))
        endM.setText(String.format(Locale.US, "%02d", c.get(Calendar.MINUTE)))
        endS.setText(String.format(Locale.US, "%02d", c.get(Calendar.SECOND)))
        recalc()
    }
    val extraDaysRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_LTR
    }
    extraDaysBox.layoutParams = LinearLayout.LayoutParams(dp(96f), LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8f) }
    extraDaysRow.addView(extraDaysBox)
    extraDaysRow.addView(TextView(this).apply {
        text = "\u200Fימים שלמים נוספים בין התאריכים (אופציונלי):"
        setTextColor(textSecondary); textSize = 13.5f; gravity = Gravity.RIGHT
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    })
    diffContainer.addView(extraDaysRow)

    // --- שדות מצב 2: הוספה / חיסור משך לשעת יעד ---
    val baseH = createSmallNumInput("שעה", String.format(Locale.US, "%02d", nowCal.get(Calendar.HOUR_OF_DAY)))
    val baseM = createSmallNumInput("דקה", String.format(Locale.US, "%02d", nowCal.get(Calendar.MINUTE)))
    val baseS = createSmallNumInput("שנייה", "00")

    val durD = createSmallNumInput("ימים", "0")
    val durH = createSmallNumInput("שעות", "1")
    val durM = createSmallNumInput("דקות", "30")
    val durS = createSmallNumInput("שניות", "0")

    buildTimeInputGroup(addContainer, "שעת בסיס (HH : MM : SS)", baseH, baseM, baseS) {
        val c = Calendar.getInstance()
        baseH.setText(String.format(Locale.US, "%02d", c.get(Calendar.HOUR_OF_DAY)))
        baseM.setText(String.format(Locale.US, "%02d", c.get(Calendar.MINUTE)))
        baseS.setText(String.format(Locale.US, "%02d", c.get(Calendar.SECOND)))
        recalc()
    }

    val opToggleRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8f)
        }
    }
    addContainer.addView(opToggleRow)

    val quickDurScroll = HorizontalScrollView(this).apply {
        isHorizontalScrollBarEnabled = false
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(8f)
        }
    }
    val quickDurRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
    quickDurScroll.addView(quickDurRow)
    addContainer.addView(quickDurScroll)

    addContainer.addView(TextView(this).apply {
        text = "\u200Fמשך להוספה / חיסור (ימים : שעות : דקות : שניות)"
        setTextColor(textPrimary); textSize = 14f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.RIGHT
        setPadding(dp(2f), dp(2f), dp(2f), dp(6f))
    })
    val durRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutDirection = View.LAYOUT_DIRECTION_LTR }
    durRow.addView(durD); durRow.addView(durH); durRow.addView(durM); durRow.addView(durS)
    addContainer.addView(durRow)

    root.addView(diffContainer)
    root.addView(addContainer)

    // 4. כרטיס תוצאה נקי (לחיצה מעתיקה ללוח)
    val resultCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
        background = createThemedCardBg(20f, 0)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(6f); bottomMargin = dp(12f)
        }
    }
    val resultTv = createResultTextView().apply {
        background = null; setPadding(0, 0, 0, 0); textSize = 16.2f
        setLineSpacing(dp(3f).toFloat(), 1.06f)
    }
    resultCard.addView(resultTv)
    resultCard.setOnClickListener {
        if (resultTv.text.isNotEmpty()) {
            feedback(it)
            copyToClipboard(resultTv.text.toString())
        }
    }
    root.addView(resultCard)

    fun parseIntClamp(et: EditText, minV: Int, maxV: Int): Int =
        (et.text.toString().trim().toIntOrNull() ?: 0).coerceIn(minV, maxV)

    recalc = {
        if (activeMode == 0) {
            val sh = parseIntClamp(startH, 0, 23); val sm = parseIntClamp(startM, 0, 59); val ss = parseIntClamp(startS, 0, 59)
            val eh = parseIntClamp(endH, 0, 23); val em = parseIntClamp(endM, 0, 59); val es = parseIntClamp(endS, 0, 59)
            val addDays = parseIntClamp(extraDaysBox, 0, 9999)

            val sTotal = sh * 3600 + sm * 60 + ss
            val eTotal = eh * 3600 + em * 60 + es
            val crossed = (eTotal < sTotal)
            val dayDiffSec = if (crossed) (86400 - sTotal + eTotal) else (eTotal - sTotal)
            val totalSec = dayDiffSec.toLong() + addDays.toLong() * 86400L

            val outDays = totalSec / 86400L
            val remSec = totalSec % 86400L
            val oh = remSec / 3600L
            val om = (remSec % 3600L) / 60L
            val os = remSec % 60L

            val startFmt = String.format(Locale.US, "%02d:%02d:%02d", sh, sm, ss)
            val endFmt = String.format(Locale.US, "%02d:%02d:%02d", eh, em, es)
            val deltaFmt = String.format(Locale.US, "%02d:%02d:%02d", oh + outDays * 24L, om, os)
            val decHours = totalSec / 3600.0
            val totalMins = totalSec / 60.0
            val pctDay = (dayDiffSec / 86400.0) * 100.0

            heroCanvas.updateTimeline(
                startSec = sTotal,
                endSec = eTotal,
                midnight = crossed,
                shiftDays = addDays + if (crossed) 1 else 0,
                badge = "הפרש Δt",
                summary = "\u200E$startFmt → $endFmt  |  Δt = $deltaFmt",
                subtract = false
            )

            val midnightNote = if (crossed) "המרווח חוצה את חצות (מסתיים ביום שלמחרת)" else "מרווח בתוך אותה יממה"
            val daysPrefix = if (outDays > 0) "${outDays} ימים ו-" else ""
            resultTv.text = BidiFmt.rtlParagraph(
                "הפרש מדויק: $daysPrefix${BidiFmt.ltr(String.format(Locale.US, "%02d:%02d:%02d", oh, om, os))}",
                "בשעות עשרוניות: ${BidiFmt.ltr("${Utils.formatResult(decHours, 3)} h")}",
                "סה״כ דקות: ${BidiFmt.ltr("${Utils.formatResult(totalMins, 2)} min")}  |  שניות: ${BidiFmt.ltr("$totalSec s")}",
                "אחוז מהיממה (24 שעות): ${BidiFmt.ltr("${Utils.formatResult(pctDay, 1)}%")}",
                "",
                "סטטוס חצות: $midnightNote"
            )
        } else {
            val bh = parseIntClamp(baseH, 0, 23); val bm = parseIntClamp(baseM, 0, 59); val bs = parseIntClamp(baseS, 0, 59)
            val dd = parseIntClamp(durD, 0, 9999); val dh = parseIntClamp(durH, 0, 999); val dm = parseIntClamp(durM, 0, 999); val ds = parseIntClamp(durS, 0, 999)

            val baseSec = bh * 3600L + bm * 60L + bs
            val deltaSec = dd * 86400L + dh * 3600L + dm * 60L + ds
            val signedDelta = if (isAddOperation) deltaSec else -deltaSec
            val targetRawSec = baseSec + signedDelta

            val dayShift = floor(targetRawSec.toDouble() / 86400.0).toInt()
            val normTargetSec = (((targetRawSec % 86400L) + 86400L) % 86400L).toInt()
            val th = normTargetSec / 3600
            val tm = (normTargetSec % 3600) / 60
            val ts = normTargetSec % 60

            val targetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, bh); set(Calendar.MINUTE, bm); set(Calendar.SECOND, bs)
                add(Calendar.SECOND, signedDelta.coerceIn(-2_000_000_000L, 2_000_000_000L).toInt())
            }
            val targetDateStr = String.format(
                Locale.US, "%02d/%02d/%04d",
                targetCal.get(Calendar.DAY_OF_MONTH),
                targetCal.get(Calendar.MONTH) + 1,
                targetCal.get(Calendar.YEAR)
            )
            val baseFmt = String.format(Locale.US, "%02d:%02d:%02d", bh, bm, bs)
            val targetFmt = String.format(Locale.US, "%02d:%02d:%02d", th, tm, ts)
            val opSym = if (isAddOperation) "+" else "−"

            heroCanvas.updateTimeline(
                startSec = baseSec.toInt(),
                endSec = normTargetSec,
                midnight = (dayShift == 1),
                shiftDays = dayShift,
                badge = if (isAddOperation) "שעת דדליין (+)" else "שעה מוקדמת (−)",
                summary = "\u200E$baseFmt $opSym ${ProTimeAndTimersTools.formatShortDuration(deltaSec)} = $targetFmt",
                subtract = !isAddOperation
            )

            val shiftLabel = when {
                dayShift == 0 -> "היום"
                dayShift == 1 -> "מחר (+1 יום)"
                dayShift == -1 -> "אתמול (−1 יום)"
                dayShift > 1 -> "בעוד $dayShift ימים"
                else -> "לפני ${abs(dayShift)} ימים"
            }
            resultTv.text = BidiFmt.rtlParagraph(
                "שעת יעד / דדליין: ${BidiFmt.ltr(targetFmt)}",
                "שינוי יום: $shiftLabel (${BidiFmt.ltr(targetDateStr)})",
                "משך שנוסף/הופחת: ${BidiFmt.ltr("$opSym${ProTimeAndTimersTools.formatShortDuration(deltaSec)}")}",
                "בשעות עשרוניות: ${BidiFmt.ltr("${Utils.formatResult(deltaSec / 3600.0, 2)} h")}"
            )
        }
    }

    fun rebuildOpToggle() {
        opToggleRow.removeAllViews()
        val addChip = createStyledChip("הוספה (+) לדדליין", isAddOperation, accentCol) {
            isAddOperation = true; rebuildOpToggle(); recalc()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4f), 0, dp(4f), 0) } }
        val subChip = createStyledChip("חיסור (−) אחורה", !isAddOperation, accentCol) {
            isAddOperation = false; rebuildOpToggle(); recalc()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4f), 0, dp(4f), 0) } }
        opToggleRow.addView(addChip)
        opToggleRow.addView(subChip)
    }

    val quickDurs = listOf(
        Triple("+15 דק׳", true, Triple(0, 0, 15)),
        Triple("+45 דק׳", true, Triple(0, 0, 45)),
        Triple("+1.5 שעות", true, Triple(0, 1, 30)),
        Triple("+8 שעות", true, Triple(0, 8, 0)),
        Triple("−30 דק׳", false, Triple(0, 0, 30))
    )
    for ((lbl, isAdd, dhm) in quickDurs) {
        quickDurRow.addView(createStyledChip(lbl, false, accentCol) {
            isAddOperation = isAdd
            durD.setText(dhm.first.toString())
            durH.setText(dhm.second.toString())
            durM.setText(dhm.third.toString())
            durS.setText("0")
            rebuildOpToggle()
            recalc()
        })
    }

    fun rebuildModeTabs() {
        tabsRow.removeAllViews()
        val t0 = createStyledChip("הפרש שעות (Δt)", activeMode == 0, accentCol) {
            activeMode = 0
            diffContainer.visibility = View.VISIBLE
            addContainer.visibility = View.GONE
            rebuildModeTabs()
            recalc()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4f), 0, dp(4f), 0) } }

        val t1 = createStyledChip("הוספה / דדליין (+/−)", activeMode == 1, accentCol) {
            activeMode = 1
            diffContainer.visibility = View.GONE
            addContainer.visibility = View.VISIBLE
            rebuildModeTabs()
            recalc()
        }.apply { layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(4f), 0, dp(4f), 0) } }

        tabsRow.addView(t0)
        tabsRow.addView(t1)
    }

    val liveWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { recalc() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }
    listOf(startH, startM, startS, endH, endM, endS, extraDaysBox, baseH, baseM, baseS, durD, durH, durM, durS)
        .forEach { it.addTextChangedListener(liveWatcher) }

    rebuildModeTabs()
    rebuildOpToggle()
    recalc()
    return scroll
}

// =========================================================================
// נקודת כניסה מ-MainActivity עבור Tabs 118..120
// =========================================================================
fun MainActivity.createProTimeAndTimersScreen(tabIndex: Int): View = when (tabIndex) {
    ProTimeAndTimersTools.TAB_HIIT_INTERVAL -> createHiitIntervalScreen()
    ProTimeAndTimersTools.TAB_MULTI_TIMER -> createMultiTimerScreen()
    else -> createTimeIntervalDeadlineScreen()
}

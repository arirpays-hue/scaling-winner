package com.example.calculator

import android.animation.ValueAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.lang.ref.WeakReference
import kotlin.math.*
import kotlin.random.Random

// =========================================================================
// מנוע 30 אנימציות קנבס מותאמות אישית ל-4 ערכות הנושא (ProCanvasAnimations)
// תומך בכיבוי/הפעלה מיידיים מההגדרות ובהשעיה אוטומטית ברקע לחיסכון בסוללה
// =========================================================================
object ProCanvasAnimations {

    private const val PREF_KEY_ENABLED = "pro_canvas_animations_enabled_v1"

    var isCanvasAnimationsEnabled: Boolean = true
        private set

    private var overlayRef: WeakReference<ProCanvasOverlayView>? = null
    private var ambientRef: WeakReference<ProAmbientCanvasView>? = null
    private var drawerOpenFlag: Boolean = false

    enum class AmbientSceneType {
        FLOATING_MATH_SYMBOLS,      // 1. מחשבון רגיל
        HARMONIC_SINE_WAVES,        // 2. מדעי, אלגברה, סטטיסטיקה, סדרות
        BINARY_HEX_MATRIX,          // 3. מתכנתים, הקסדצימלי, לוגיקה, הצפנה
        FINANCE_CANDLES_COINS,      // 4. פיננסים, משכנתא, מניות, SIP, פרישה
        MEASUREMENT_CALIPER_GRID,   // 5. המרות יחידות, 87 קטגוריות, מידות
        SPEED_AERO_LINES,           // 6. מהירות, רכב ודלק
        MEDICAL_ECG_PULSE,          // 7. בריאות, BMI, BMR, רפואה
        CLOCKWORK_GEARS_PENDULUM,   // 8. זמן, גיל, שעון עצר, מטרונום
        FLUID_WATER_WAVES,          // 9. אינסטלציה וזורמים
        BLUEPRINT_3D_POLYHEDRA,     // 10. בנייה, גאומטריה ומיזוג
        CIRCUIT_PCB_ELECTRONS,      // 11. אלקטרוניקה, נגדים, קבלים, אוהם, סוללה
        RADAR_SONAR_SWEEP,          // 12. מצפן, פלס, מתכות, רטט, קלינומטר
        PRISM_LIGHT_RAYS,           // 13. פנס, צבעים, אופטיקה, מד אור
        ATOMIC_BOHR_ORBITS,         // 14. טבלה מחזורית וכימיה
        NETWORK_MESH_RADIO,         // 15. מורס, סירנה, מהירות רשת ו-IP
        THEME_ATMOSPHERE_SIGNATURE  // 16. טקסט, פתקים, מועדפים, ענן והגדרות
    }

    fun loadPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        isCanvasAnimationsEnabled = prefs.getBoolean(PREF_KEY_ENABLED, true)
    }

    fun setEnabled(activity: MainActivity, enabled: Boolean) {
        isCanvasAnimationsEnabled = enabled
        activity.prefs?.edit()?.putBoolean(PREF_KEY_ENABLED, enabled)?.apply()
        if (enabled) {
            overlayRef?.get()?.visibility = View.VISIBLE
            overlayRef?.get()?.resumeAnimator()
            ambientRef?.get()?.visibility = View.VISIBLE
            ambientRef?.get()?.resumeAnimator()
        } else {
            overlayRef?.get()?.clearEffectsAndPause()
            overlayRef?.get()?.visibility = View.GONE
            ambientRef?.get()?.pauseAnimator()
            ambientRef?.get()?.visibility = View.GONE
        }
    }

    fun getSettingsButtonLabel(activity: MainActivity): String {
        val status = if (isCanvasAnimationsEnabled) {
            if (activity.isCleanGlassActive) "מופעל" else "מופעל 🟢"
        } else {
            if (activity.isCleanGlassActive) "כבוי" else "כבוי 🔴"
        }
        return if (activity.isCleanGlassActive) {
            "אנימציות קנבס ואפקטים (30 אנימציות): $status"
        } else {
            "✨ אנימציות קנבס ואפקטים (30 אנימציות): $status"
        }
    }

    fun createSettingsToggleButton(activity: MainActivity): TextView {
        return activity.createActionBtn(getSettingsButtonLabel(activity), false) {
            val next = !isCanvasAnimationsEnabled
            setEnabled(activity, next)
            Toast.makeText(
                activity,
                if (next) "✨ אנימציות הקנבס הופעלו (30 אנימציות מותאמות לערכת הנושא)" else "אנימציות הקנבס כובו",
                Toast.LENGTH_SHORT
            ).show()
            activity.switchTab(19)
        }.apply {
            tag = "PRO_CANVAS_ANIM_TOGGLE_BTN"
        }
    }

    fun installOverlay(activity: MainActivity, rootFrame: FrameLayout) {
        val existing = rootFrame.findViewWithTag<ProCanvasOverlayView>("PRO_CANVAS_OVERLAY_VIEW")
        if (existing != null) {
            overlayRef = WeakReference(existing)
            return
        }
        val overlay = ProCanvasOverlayView(activity).apply {
            tag = "PRO_CANVAS_OVERLAY_VIEW"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = if (isCanvasAnimationsEnabled) View.VISIBLE else View.GONE
        }
        rootFrame.addView(overlay)
        overlayRef = WeakReference(overlay)
    }

    fun resolveSceneForTab(tabIndex: Int): AmbientSceneType = when (tabIndex) {
        0 -> AmbientSceneType.FLOATING_MATH_SYMBOLS
        1, 8, 15, 16, 22, 52, 54, 65, 82, 83, 84, 85 -> AmbientSceneType.HARMONIC_SINE_WAVES
        7, 28, 46, 72, 77 -> AmbientSceneType.BINARY_HEX_MATRIX
        2, 3, 9, 10, 13, 17, 23, 24, 25, 29, 48, 49, 50, 51, 64, in 86..95 -> AmbientSceneType.FINANCE_CANDLES_COINS
        4, 5, 6, 31, 57, 63, 74, 79, 80 -> AmbientSceneType.MEASUREMENT_CALIPER_GRID
        18, 20, 21 -> AmbientSceneType.SPEED_AERO_LINES
        11, 14, 27, 33, 62 -> AmbientSceneType.MEDICAL_ECG_PULSE
        12, 26, 30, 42, 69 -> AmbientSceneType.CLOCKWORK_GEARS_PENDULUM
        32, 58 -> AmbientSceneType.FLUID_WATER_WAVES
        34, 53, 59 -> AmbientSceneType.BLUEPRINT_3D_POLYHEDRA
        35, 36, 37, 38, 60, 71, 75, 76 -> AmbientSceneType.CIRCUIT_PCB_ELECTRONS
        39, 40, 41, 66, 67, 98 -> AmbientSceneType.RADAR_SONAR_SWEEP
        43, 61, 73, 96 -> AmbientSceneType.PRISM_LIGHT_RAYS
        44 -> AmbientSceneType.ATOMIC_BOHR_ORBITS
        45, 68, 97, 99 -> AmbientSceneType.NETWORK_MESH_RADIO
        else -> AmbientSceneType.THEME_ATMOSPHERE_SIGNATURE
    }

    fun attachAmbientToContent(activity: MainActivity, contentFrame: FrameLayout, tabIndex: Int) {
        ambientRef?.get()?.pauseAnimator()
        val scene = resolveSceneForTab(tabIndex)
        val ambient = ProAmbientCanvasView(activity, scene).apply {
            tag = "PRO_CANVAS_AMBIENT_VIEW"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = if (isCanvasAnimationsEnabled) View.VISIBLE else View.GONE
        }
        contentFrame.addView(ambient, 0)
        ambientRef = WeakReference(ambient)
        if (isCanvasAnimationsEnabled) ambient.resumeAnimator()
    }

    fun playTabTransition(activity: MainActivity, contentRoot: View?, tabIndex: Int, wasInSettings: Boolean) {
        if (!isCanvasAnimationsEnabled || wasInSettings) return
        overlayRef?.get()?.triggerTabTransition()
        if (contentRoot == null) return

        // אנימציית מפל כניסה מדורג לכרטיסים או גל אלכסוני למקשי המחשבון
        if (tabIndex == 0 || tabIndex == 1) {
            animateKeypadWave(activity)
        } else {
            animateContentCascade(activity, contentRoot)
        }
    }

    private fun animateKeypadWave(activity: MainActivity) {
        val buttons = activity.keypadButtons.values.toList()
        for ((idx, btn) in buttons.withIndex()) {
            val delayMs = (idx * 14L).coerceAtMost(260L)
            btn.alpha = 0f
            btn.scaleX = 0.84f
            btn.scaleY = 0.84f
            btn.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(delayMs)
                .setDuration(230L)
                .setInterpolator(OvershootInterpolator(1.15f))
                .start()
        }
    }

    private fun animateContentCascade(activity: MainActivity, root: View) {
        val targetContainer: ViewGroup? = when (root) {
            is ScrollView -> root.getChildAt(0) as? ViewGroup
            is ViewGroup -> root
            else -> null
        }
        if (targetContainer == null) return
        val maxAnimated = min(targetContainer.childCount, 9)
        val slidePx = activity.dp(18f).toFloat()
        for (i in 0 until maxAnimated) {
            val child = targetContainer.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue
            child.alpha = 0f
            child.translationY = slidePx
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(i * 28L)
                .setDuration(260L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        overlayRef?.get()?.triggerHeaderShimmer()
    }

    fun triggerTouchRipple(activity: MainActivity, sourceView: View, localX: Float, localY: Float, symbol: String = "") {
        if (!isCanvasAnimationsEnabled) return
        val overlay = overlayRef?.get() ?: return
        val sLoc = IntArray(2)
        val oLoc = IntArray(2)
        sourceView.getLocationInWindow(sLoc)
        overlay.getLocationInWindow(oLoc)
        val x = (sLoc[0] - oLoc[0]) + localX
        val y = (sLoc[1] - oLoc[1]) + localY
        overlay.spawnMultiRingRipple(x, y, isAccent = symbol in listOf("=", "+", "−", "×", "÷", "AC"))
    }

    fun triggerViewPulseRipple(activity: MainActivity, sourceView: View) {
        if (!isCanvasAnimationsEnabled) return
        val overlay = overlayRef?.get() ?: return
        val sLoc = IntArray(2)
        val oLoc = IntArray(2)
        sourceView.getLocationInWindow(sLoc)
        overlay.getLocationInWindow(oLoc)
        val x = (sLoc[0] - oLoc[0]) + sourceView.width * 0.5f
        val y = (sLoc[1] - oLoc[1]) + sourceView.height * 0.5f
        overlay.spawnMultiRingRipple(x, y, isAccent = true)
    }

    fun triggerEqualsCelebration(activity: MainActivity) {
        if (!isCanvasAnimationsEnabled) return
        val overlay = overlayRef?.get() ?: return
        val target = activity.currentView
        if (target != null && target.width > 0) {
            val sLoc = IntArray(2)
            val oLoc = IntArray(2)
            target.getLocationInWindow(sLoc)
            overlay.getLocationInWindow(oLoc)
            val cx = (sLoc[0] - oLoc[0]) + target.width * 0.55f
            val cy = (sLoc[1] - oLoc[1]) + target.height * 0.5f
            overlay.spawnCelebrationBurst(cx, cy)
        } else {
            overlay.spawnCelebrationBurst(overlay.width * 0.5f, overlay.height * 0.25f)
        }
    }

    fun triggerClearWave(activity: MainActivity) {
        if (!isCanvasAnimationsEnabled) return
        overlayRef?.get()?.spawnClearWave()
    }

    fun triggerMemoryPulse(activity: MainActivity) {
        if (!isCanvasAnimationsEnabled) return
        val overlay = overlayRef?.get() ?: return
        val target = activity.currentView
        if (target != null && target.width > 0) {
            val sLoc = IntArray(2)
            val oLoc = IntArray(2)
            target.getLocationInWindow(sLoc)
            overlay.getLocationInWindow(oLoc)
            overlay.spawnMemoryOrbit((sLoc[0] - oLoc[0]) + target.width * 0.5f, (sLoc[1] - oLoc[1]) + target.height * 0.5f)
        }
    }

    fun onDrawerStateChanged(activity: MainActivity, isOpen: Boolean, drawerMenu: ViewGroup?) {
        drawerOpenFlag = isOpen
        overlayRef?.get()?.setDrawerAmbientActive(isOpen, activity.isRightHanded)
        if (!isCanvasAnimationsEnabled || !isOpen || drawerMenu == null) return

        val dirSign = if (activity.isRightHanded) 1f else -1f
        val offsetPx = activity.dp(24f) * dirSign
        var animIdx = 0
        for (i in 0 until drawerMenu.childCount) {
            val child = drawerMenu.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue
            if (animIdx > 10) break
            child.alpha = 0f
            child.translationX = offsetPx
            child.animate()
                .alpha(1f)
                .translationX(0f)
                .setStartDelay(animIdx * 22L)
                .setDuration(240L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            animIdx++
        }
    }

    fun refreshThemePalette(activity: MainActivity) {
        overlayRef?.get()?.invalidate()
        ambientRef?.get()?.invalidate()
    }

    fun onActivityResume() {
        if (!isCanvasAnimationsEnabled) return
        overlayRef?.get()?.resumeAnimator()
        ambientRef?.get()?.resumeAnimator()
    }

    fun onActivityPause() {
        overlayRef?.get()?.pauseAnimator()
        ambientRef?.get()?.pauseAnimator()
    }
}

// =========================================================================
// שכבת רקע אווירה חיה בקנבס (16 אנימציות נושאיות המותאמות ל-4 ערכות הנושא)
// =========================================================================
private class ProAmbientCanvasView(
    private val activity: MainActivity,
    private val scene: ProCanvasAnimations.AmbientSceneType
) : View(activity) {

    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var animator: ValueAnimator? = null

    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val wavePath = Path()
    private val reusableRect = RectF()

    // זרעי מיקום קבועים מראש (ללא הקצאות בזיכרון ב-onDraw!)
    private val seedX = FloatArray(18) { Random.nextFloat() }
    private val seedY = FloatArray(18) { Random.nextFloat() }
    private val seedSpd = FloatArray(18) { 0.45f + Random.nextFloat() * 0.95f }
    private val seedSz = FloatArray(18) { 0.7f + Random.nextFloat() * 0.9f }
    private val mathGlyphs = arrayOf("+", "−", "×", "÷", "=", "%", "π", "√", "∑", "∞")
    private val hexGlyphs = arrayOf("0", "1", "01", "10", "A", "F", "0x", "11")

    init {
        isClickable = false
        isFocusable = false
    }

    fun resumeAnimator() {
        if (!ProCanvasAnimations.isCanvasAnimationsEnabled) return
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase += 0.016f
                if (phase > 6283f) phase = 0f
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    fun pauseAnimator() {
        animator?.cancel()
        animator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ProCanvasAnimations.isCanvasAnimationsEnabled) resumeAnimator()
    }

    override fun onDetachedFromWindow() {
        pauseAnimator()
        super.onDetachedFromWindow()
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        val a = alpha.coerceIn(0, 255)
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!ProCanvasAnimations.isCanvasAnimationsEnabled) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 2f || h <= 2f) return

        val pal = activity.getProCanvasColors()
        val c1 = pal.accent1
        val c2 = pal.accent2
        val c3 = pal.accent3
        val baseAlpha = when {
            pal.isMin -> 46
            pal.isAurora -> 62
            activity.isIosGlassMode -> 52
            else -> 44
        }

        // שכבת חתימה דקה המבדילה את 4 ערכות הנושא גם ברקע
        drawThemeAmbientBackdrop(canvas, w, h, c1, c2, pal)

        when (scene) {
            ProCanvasAnimations.AmbientSceneType.FLOATING_MATH_SYMBOLS -> {
                for (i in 0 until 12) {
                    val px = ((seedX[i] * w + sin(phase * seedSpd[i] + i) * 26f * d) + w) % w
                    val py = ((seedY[i] * h - phase * 18f * seedSpd[i] * d) % h + h) % h
                    val col = if (i % 3 == 0) c1 else if (i % 3 == 1) c2 else c3
                    textP.color = withAlpha(col, baseAlpha + 12)
                    textP.textSize = (18f + seedSz[i] * 14f) * d
                    canvas.save()
                    canvas.rotate(sin(phase * 0.7f + i) * 18f, px, py)
                    canvas.drawText(mathGlyphs[i % mathGlyphs.size], px, py, textP)
                    canvas.restore()
                }
            }

            ProCanvasAnimations.AmbientSceneType.HARMONIC_SINE_WAVES -> {
                for (layer in 0 until 3) {
                    wavePath.reset()
                    val cy = h * (0.28f + layer * 0.22f)
                    val amp = (20f + layer * 10f) * d
                    val freq = 0.011f + layer * 0.004f
                    val speed = phase * (1.2f + layer * 0.45f)
                    var x = 0f
                    while (x <= w + 16f * d) {
                        val y = cy + sin(x * freq + speed) * amp * cos(x * 0.003f - phase * 0.3f)
                        if (x == 0f) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                        x += 14f * d
                    }
                    strokeP.strokeWidth = (1.6f + layer * 0.4f) * d
                    strokeP.color = withAlpha(if (layer == 0) c1 else if (layer == 1) c2 else c3, baseAlpha + 16)
                    canvas.drawPath(wavePath, strokeP)
                }
            }

            ProCanvasAnimations.AmbientSceneType.BINARY_HEX_MATRIX -> {
                val cols = 9
                val colW = w / cols
                textP.textSize = 13f * d
                for (c in 0 until cols) {
                    val colX = colW * (c + 0.5f)
                    val headY = ((seedY[c] * h + phase * 42f * seedSpd[c] * d) % (h + 120f * d)) - 60f * d
                    for (k in 0 until 5) {
                        val y = headY - k * 24f * d
                        if (y < -20f || y > h + 20f) continue
                        val fade = ((5 - k) / 5f)
                        textP.color = withAlpha(if (c % 2 == 0) pal.positive else c1, (baseAlpha * 1.4f * fade).toInt())
                        val sym = hexGlyphs[(c + k + (phase * 2).toInt()) % hexGlyphs.size]
                        canvas.drawText(sym, colX, y, textP)
                    }
                }
            }

            ProCanvasAnimations.AmbientSceneType.FINANCE_CANDLES_COINS -> {
                // קו מגמה פיננסי זורם + נרות שוק עדינים
                wavePath.reset()
                val baseY = h * 0.72f
                val step = w / 10f
                for (i in 0..10) {
                    val px = i * step
                    val py = baseY - (i * 11f * d) + sin(phase * 1.1f + i * 0.9f) * 26f * d
                    if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
                    if (i in 1..9) {
                        val candleH = (16f + abs(sin(phase * 0.6f + i)) * 26f) * d
                        fillP.color = withAlpha(if (i % 2 == 0) pal.positive else c2, baseAlpha)
                        reusableRect.set(px - 4.5f * d, py - candleH * 0.5f, px + 4.5f * d, py + candleH * 0.5f)
                        canvas.drawRoundRect(reusableRect, 3f * d, 3f * d, fillP)
                    }
                }
                strokeP.strokeWidth = 2f * d
                strokeP.color = withAlpha(c1, baseAlpha + 18)
                canvas.drawPath(wavePath, strokeP)
            }

            ProCanvasAnimations.AmbientSceneType.MEASUREMENT_CALIPER_GRID -> {
                val shift = (phase * 22f * d) % (36f * d)
                strokeP.strokeWidth = 1.1f * d
                strokeP.color = withAlpha(c1, baseAlpha)
                var x = -shift
                var tickIdx = 0
                while (x < w + 36f * d) {
                    val len = if (tickIdx % 4 == 0) 22f * d else 11f * d
                    canvas.drawLine(x, h - len, x, h, strokeP)
                    canvas.drawLine(w - x, 0f, w - x, len, strokeP)
                    x += 18f * d
                    tickIdx++
                }
                // חיצי המרה נעים במרכז
                val midY = h * 0.5f
                val ox = sin(phase * 1.4f) * 26f * d
                strokeP.strokeWidth = 1.8f * d
                strokeP.color = withAlpha(c2, baseAlpha + 12)
                canvas.drawLine(w * 0.25f + ox, midY - 14f * d, w * 0.75f + ox, midY - 14f * d, strokeP)
                canvas.drawLine(w * 0.75f - ox, midY + 14f * d, w * 0.25f - ox, midY + 14f * d, strokeP)
            }

            ProCanvasAnimations.AmbientSceneType.SPEED_AERO_LINES -> {
                strokeP.strokeWidth = 1.8f * d
                for (i in 0 until 12) {
                    val lineLen = (48f + seedSz[i] * 75f) * d
                    val px = ((seedX[i] * w + phase * 140f * seedSpd[i] * d) % (w + lineLen)) - lineLen
                    val py = seedY[i] * h
                    strokeP.color = withAlpha(if (i % 2 == 0) c1 else c2, baseAlpha + 10)
                    canvas.drawLine(px, py, px + lineLen, py, strokeP)
                }
            }

            ProCanvasAnimations.AmbientSceneType.MEDICAL_ECG_PULSE -> {
                wavePath.reset()
                val cy = h * 0.52f
                val cycleW = 130f * d
                val offset = (phase * 58f * d) % cycleW
                var x = -offset - cycleW
                var started = false
                while (x < w + cycleW) {
                    val pts = floatArrayOf(
                        x, cy,
                        x + cycleW * 0.30f, cy,
                        x + cycleW * 0.38f, cy - 12f * d,
                        x + cycleW * 0.44f, cy + 16f * d,
                        x + cycleW * 0.50f, cy - 46f * d,
                        x + cycleW * 0.56f, cy + 24f * d,
                        x + cycleW * 0.64f, cy,
                        x + cycleW, cy
                    )
                    for (j in pts.indices step 2) {
                        if (!started) {
                            wavePath.moveTo(pts[j], pts[j + 1])
                            started = true
                        } else {
                            wavePath.lineTo(pts[j], pts[j + 1])
                        }
                    }
                    x += cycleW
                }
                strokeP.strokeWidth = 2.1f * d
                strokeP.color = withAlpha(pal.positive, baseAlpha + 22)
                canvas.drawPath(wavePath, strokeP)
            }

            ProCanvasAnimations.AmbientSceneType.CLOCKWORK_GEARS_PENDULUM -> {
                val cx1 = w * 0.22f; val cy1 = h * 0.30f; val r1 = 44f * d
                val cx2 = w * 0.78f; val cy2 = h * 0.68f; val r2 = 58f * d
                drawGear(canvas, cx1, cy1, r1, 10, phase * 18f, withAlpha(c1, baseAlpha + 10))
                drawGear(canvas, cx2, cy2, r2, 12, -phase * 14f, withAlpha(c2, baseAlpha + 10))
            }

            ProCanvasAnimations.AmbientSceneType.FLUID_WATER_WAVES -> {
                for (layer in 0 until 2) {
                    wavePath.reset()
                    val baseY = h * (0.76f + layer * 0.08f)
                    wavePath.moveTo(0f, h)
                    var x = 0f
                    while (x <= w + 16f * d) {
                        val y = baseY + sin(x * 0.015f + phase * (1.4f + layer * 0.5f)) * (14f + layer * 6f) * d
                        wavePath.lineTo(x, y)
                        x += 16f * d
                    }
                    wavePath.lineTo(w, h)
                    wavePath.close()
                    fillP.color = withAlpha(if (layer == 0) c3 else c1, baseAlpha)
                    canvas.drawPath(wavePath, fillP)
                }
                for (i in 0 until 8) {
                    val bx = seedX[i] * w + sin(phase + i) * 12f * d
                    val by = ((seedY[i] * h - phase * 28f * seedSpd[i] * d) % h + h) % h
                    strokeP.strokeWidth = 1.3f * d
                    strokeP.color = withAlpha(c3, baseAlpha + 14)
                    canvas.drawCircle(bx, by, (5f + seedSz[i] * 7f) * d, strokeP)
                }
            }

            ProCanvasAnimations.AmbientSceneType.BLUEPRINT_3D_POLYHEDRA -> {
                val cx = w * 0.5f; val cy = h * 0.48f
                val rad = min(w, h) * 0.26f
                strokeP.strokeWidth = 1.5f * d
                strokeP.color = withAlpha(c1, baseAlpha + 14)
                drawRotatingPolygon(canvas, cx, cy, rad, 6, phase * 0.4f)
                strokeP.color = withAlpha(c2, baseAlpha + 8)
                drawRotatingPolygon(canvas, cx, cy, rad * 0.62f, 3, -phase * 0.7f)
            }

            ProCanvasAnimations.AmbientSceneType.CIRCUIT_PCB_ELECTRONS -> {
                strokeP.strokeWidth = 1.4f * d
                for (i in 0 until 5) {
                    val y = h * (0.16f + i * 0.17f)
                    val bendX = w * (0.28f + (i % 3) * 0.20f)
                    strokeP.color = withAlpha(c1, baseAlpha)
                    canvas.drawLine(0f, y, bendX, y, strokeP)
                    canvas.drawLine(bendX, y, bendX + 26f * d, y + 20f * d, strokeP)
                    canvas.drawLine(bendX + 26f * d, y + 20f * d, w, y + 20f * d, strokeP)

                    // פולס אלקטרון נע לאורך הקו
                    val t = ((phase * 0.35f * seedSpd[i] + i * 0.21f) % 1f)
                    val ex = t * w
                    val ey = if (ex < bendX) y else if (ex < bendX + 26f * d) y + ((ex - bendX) / (26f * d)) * 20f * d else y + 20f * d
                    fillP.color = withAlpha(if (i % 2 == 0) pal.warning else c1, baseAlpha + 55)
                    canvas.drawCircle(ex, ey, 3.6f * d, fillP)
                }
            }

            ProCanvasAnimations.AmbientSceneType.RADAR_SONAR_SWEEP -> {
                val cx = w * 0.5f; val cy = h * 0.46f
                val maxR = min(w, h) * 0.42f
                strokeP.strokeWidth = 1.4f * d
                for (k in 0 until 3) {
                    val prog = (phase * 0.28f + k * 0.33f) % 1f
                    val r = prog * maxR
                    strokeP.color = withAlpha(c1, ((1f - prog) * (baseAlpha + 30)).toInt())
                    canvas.drawCircle(cx, cy, r, strokeP)
                }
                val sweepAng = phase * 1.3f
                strokeP.color = withAlpha(c2, baseAlpha + 25)
                canvas.drawLine(cx, cy, cx + cos(sweepAng) * maxR, cy + sin(sweepAng) * maxR, strokeP)
            }

            ProCanvasAnimations.AmbientSceneType.PRISM_LIGHT_RAYS -> {
                val originX = w * 0.5f; val originY = -20f * d
                for (i in 0 until 6) {
                    val baseAng = Math.toRadians(55.0 + i * 14.0 + sin(phase * 0.8f + i) * 5.0)
                    val ex = originX + (cos(baseAng) * h * 1.1f).toFloat()
                    val ey = originY + (sin(baseAng) * h * 1.1f).toFloat()
                    val rayCol = when (i % 3) { 0 -> c1; 1 -> c2; else -> c3 }
                    strokeP.strokeWidth = (10f + i * 2f) * d
                    strokeP.color = withAlpha(rayCol, (baseAlpha * 0.45f).toInt())
                    canvas.drawLine(originX, originY, ex, ey, strokeP)
                }
            }

            ProCanvasAnimations.AmbientSceneType.ATOMIC_BOHR_ORBITS -> {
                val cx = w * 0.5f; val cy = h * 0.48f
                val rx = min(w, h) * 0.34f; val ry = rx * 0.38f
                strokeP.strokeWidth = 1.4f * d
                for (k in 0 until 3) {
                    canvas.save()
                    canvas.rotate(k * 60f, cx, cy)
                    strokeP.color = withAlpha(if (k == 0) c1 else if (k == 1) c2 else c3, baseAlpha + 12)
                    reusableRect.set(cx - rx, cy - ry, cx + rx, cy + ry)
                    canvas.drawOval(reusableRect, strokeP)
                    val a = phase * (1.3f + k * 0.3f) + k * 2f
                    fillP.color = withAlpha(c1, baseAlpha + 45)
                    canvas.drawCircle(cx + cos(a) * rx, cy + sin(a) * ry, 4.2f * d, fillP)
                    canvas.restore()
                }
            }

            ProCanvasAnimations.AmbientSceneType.NETWORK_MESH_RADIO -> {
                val count = 8
                val xs = FloatArray(count)
                val ys = FloatArray(count)
                for (i in 0 until count) {
                    xs[i] = w * (0.14f + seedX[i] * 0.72f) + cos(phase * seedSpd[i] + i) * 22f * d
                    ys[i] = h * (0.14f + seedY[i] * 0.72f) + sin(phase * seedSpd[i] + i) * 22f * d
                }
                strokeP.strokeWidth = 1.2f * d
                for (i in 0 until count) {
                    for (j in i + 1 until count) {
                        val dist = hypot(xs[i] - xs[j], ys[i] - ys[j])
                        if (dist < 170f * d) {
                            strokeP.color = withAlpha(c1, ((1f - dist / (170f * d)) * (baseAlpha + 15)).toInt())
                            canvas.drawLine(xs[i], ys[i], xs[j], ys[j], strokeP)
                        }
                    }
                    fillP.color = withAlpha(if (i % 2 == 0) c1 else c2, baseAlpha + 35)
                    canvas.drawCircle(xs[i], ys[i], 4f * d, fillP)
                }
            }

            ProCanvasAnimations.AmbientSceneType.THEME_ATMOSPHERE_SIGNATURE -> {
                for (i in 0 until 10) {
                    val px = seedX[i] * w + sin(phase * seedSpd[i] + i) * 24f * d
                    val py = seedY[i] * h + cos(phase * seedSpd[i] * 0.8f + i) * 24f * d
                    val rad = (10f + seedSz[i] * 18f) * d
                    val col = if (i % 3 == 0) c1 else if (i % 3 == 1) c2 else c3
                    if (activity.isIosGlassMode) {
                        strokeP.strokeWidth = 1.3f * d
                        strokeP.color = withAlpha(Color.WHITE, baseAlpha + 10)
                        canvas.drawCircle(px, py, rad, strokeP)
                    } else {
                        fillP.color = withAlpha(col, (baseAlpha * 0.65f).toInt())
                        canvas.drawCircle(px, py, rad, fillP)
                    }
                }
            }
        }
    }

    private fun drawThemeAmbientBackdrop(
        canvas: Canvas,
        w: Float,
        h: Float,
        c1: Int,
        c2: Int,
        pal: ProThemeCanvasColors
    ) {
        when {
            pal.isAurora -> {
                // סרט זוהר צפוני נשימתי עדין בחלק העליון
                val gx = w * (0.5f + sin(phase * 0.35f) * 0.28f)
                val gy = h * 0.16f
                val gr = (min(w, h) * 0.55f).coerceAtLeast(1f)
                fillP.shader = RadialGradient(gx, gy, gr, withAlpha(c1, 36), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(gx, gy, gr, fillP)
                fillP.shader = null
            }
            activity.isIosGlassMode -> {
                // הברקת עדשת זכוכית צפה
                val gx = w * (0.25f + cos(phase * 0.25f) * 0.18f)
                val gy = h * (0.22f + sin(phase * 0.25f) * 0.10f)
                val gr = (min(w, h) * 0.42f).coerceAtLeast(1f)
                fillP.shader = RadialGradient(gx, gy, gr, Color.argb(26, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(gx, gy, gr, fillP)
                fillP.shader = null
            }
            pal.isMin -> {
                // אורב פסטל עדין מאוד בפינה
                val gx = w * 0.85f + sin(phase * 0.3f) * 18f * d
                val gy = h * 0.12f
                val gr = (min(w, h) * 0.36f).coerceAtLeast(1f)
                fillP.shader = RadialGradient(gx, gy, gr, withAlpha(c2, 24), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(gx, gy, gr, fillP)
                fillP.shader = null
            }
        }
    }

    private fun drawGear(canvas: Canvas, cx: Float, cy: Float, radius: Float, teeth: Int, rotDeg: Float, col: Int) {
        canvas.save()
        canvas.rotate(rotDeg, cx, cy)
        strokeP.strokeWidth = 1.6f * d
        strokeP.color = col
        canvas.drawCircle(cx, cy, radius * 0.72f, strokeP)
        canvas.drawCircle(cx, cy, radius * 0.28f, strokeP)
        for (i in 0 until teeth) {
            val a = Math.toRadians((i * 360.0 / teeth))
            canvas.drawLine(
                cx + (cos(a) * radius * 0.72f).toFloat(),
                cy + (sin(a) * radius * 0.72f).toFloat(),
                cx + (cos(a) * radius).toFloat(),
                cy + (sin(a) * radius).toFloat(),
                strokeP
            )
        }
        canvas.restore()
    }

    private fun drawRotatingPolygon(canvas: Canvas, cx: Float, cy: Float, radius: Float, sides: Int, rotRad: Float) {
        wavePath.reset()
        for (i in 0 until sides) {
            val a = rotRad + (i * 2.0 * PI / sides).toFloat()
            val px = cx + cos(a) * radius
            val py = cy + sin(a) * radius
            if (i == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
        }
        wavePath.close()
        canvas.drawPath(wavePath, strokeP)
    }
}

// =========================================================================
// שכבת אפקטים אינטראקטיביים מעל האפליקציה (Ripples, = Burst, AC Wave, Transitions, Drawer)
// =========================================================================
private class ProCanvasOverlayView(
    private val activity: MainActivity
) : View(activity) {

    private data class RippleWave(
        val x: Float,
        val y: Float,
        val maxR: Float,
        val isAccent: Boolean,
        var progress: Float = 0f
    )

    private data class SparkParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        val size: Float,
        val color: Int,
        val shapeType: Int,
        var life: Float = 1f
    )

    private val d = resources.displayMetrics.density
    private val ripples = ArrayList<RippleWave>(16)
    private val sparks = ArrayList<SparkParticle>(64)

    private var transitionProgress = 1f
    private var clearWaveProgress = 1f
    private var shimmerProgress = 1f
    private var memoryOrbitProgress = 1f
    private var memX = 0f
    private var memY = 0f

    private var drawerSparklesActive = false
    private var drawerRightSide = true
    private var drawerPhase = 0f
    private val dSeedX = FloatArray(14) { Random.nextFloat() }
    private val dSeedY = FloatArray(14) { Random.nextFloat() }

    private var animator: ValueAnimator? = null
    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    init {
        isClickable = false
        isFocusable = false
    }

    private fun hasActiveWork(): Boolean {
        return ripples.isNotEmpty() ||
            sparks.isNotEmpty() ||
            transitionProgress < 1f ||
            clearWaveProgress < 1f ||
            shimmerProgress < 1f ||
            memoryOrbitProgress < 1f ||
            drawerSparklesActive
    }

    fun resumeAnimator() {
        if (!ProCanvasAnimations.isCanvasAnimationsEnabled) return
        if (!hasActiveWork()) return
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                stepFrame()
                postInvalidateOnAnimation()
                if (!hasActiveWork()) pauseAnimator()
            }
            start()
        }
    }

    fun pauseAnimator() {
        animator?.cancel()
        animator = null
    }

    fun clearEffectsAndPause() {
        ripples.clear()
        sparks.clear()
        transitionProgress = 1f
        clearWaveProgress = 1f
        shimmerProgress = 1f
        memoryOrbitProgress = 1f
        drawerSparklesActive = false
        pauseAnimator()
        invalidate()
    }

    fun setDrawerAmbientActive(active: Boolean, isRightHanded: Boolean) {
        drawerSparklesActive = active && ProCanvasAnimations.isCanvasAnimationsEnabled
        drawerRightSide = isRightHanded
        if (drawerSparklesActive) resumeAnimator() else invalidate()
    }

    fun spawnMultiRingRipple(x: Float, y: Float, isAccent: Boolean) {
        if (ripples.size > 10) ripples.removeAt(0)
        ripples.add(RippleWave(x, y, maxR = (if (isAccent) 72f else 52f) * d, isAccent = isAccent))
        resumeAnimator()
    }

    fun spawnCelebrationBurst(cx: Float, cy: Float) {
        val pal = activity.getProCanvasColors()
        val colors = intArrayOf(pal.accent1, pal.accent2, pal.accent3, pal.positive, pal.warning)
        val count = if (pal.isMin) 18 else 26
        for (i in 0 until count) {
            val ang = (i.toFloat() / count) * (2f * PI.toFloat()) + Random.nextFloat() * 0.35f
            val speed = (3.5f + Random.nextFloat() * 7.5f) * d
            sparks.add(
                SparkParticle(
                    x = cx,
                    y = cy,
                    vx = cos(ang) * speed,
                    vy = sin(ang) * speed - 2.2f * d,
                    size = (3.2f + Random.nextFloat() * 4.2f) * d,
                    color = colors[i % colors.size],
                    shapeType = i % 3
                )
            )
        }
        resumeAnimator()
    }

    fun spawnClearWave() {
        clearWaveProgress = 0f
        resumeAnimator()
    }

    fun spawnMemoryOrbit(cx: Float, cy: Float) {
        memX = cx
        memY = cy
        memoryOrbitProgress = 0f
        resumeAnimator()
    }

    fun triggerTabTransition() {
        transitionProgress = 0f
        resumeAnimator()
    }

    fun triggerHeaderShimmer() {
        shimmerProgress = 0f
        resumeAnimator()
    }

    private fun stepFrame() {
        for (i in ripples.size - 1 downTo 0) {
            ripples[i].progress += 0.055f
            if (ripples[i].progress >= 1f) ripples.removeAt(i)
        }
        for (i in sparks.size - 1 downTo 0) {
            val s = sparks[i]
            s.x += s.vx
            s.y += s.vy
            s.vy += 0.24f * d
            s.vx *= 0.96f
            s.life -= 0.028f
            if (s.life <= 0f) sparks.removeAt(i)
        }
        if (transitionProgress < 1f) transitionProgress = (transitionProgress + 0.055f).coerceAtMost(1f)
        if (clearWaveProgress < 1f) clearWaveProgress = (clearWaveProgress + 0.065f).coerceAtMost(1f)
        if (shimmerProgress < 1f) shimmerProgress = (shimmerProgress + 0.045f).coerceAtMost(1f)
        if (memoryOrbitProgress < 1f) memoryOrbitProgress = (memoryOrbitProgress + 0.045f).coerceAtMost(1f)
        if (drawerSparklesActive) {
            drawerPhase += 0.035f
            if (drawerPhase > 6283f) drawerPhase = 0f
        }
    }

    override fun onDetachedFromWindow() {
        pauseAnimator()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!ProCanvasAnimations.isCanvasAnimationsEnabled) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 2f || h <= 2f) return

        val pal = activity.getProCanvasColors()

        // 1. אדוות מגע רב-טבעתיות
        for (rip in ripples) {
            val ease = 1f - (1f - rip.progress).pow(2)
            val col = if (rip.isAccent) pal.accent1 else pal.accent2
            val alpha = ((1f - rip.progress) * (if (pal.isMin) 130f else 175f)).toInt().coerceIn(0, 255)
            strokeP.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
            strokeP.strokeWidth = (2.4f * (1f - rip.progress * 0.6f)) * d
            canvas.drawCircle(rip.x, rip.y, rip.maxR * ease, strokeP)

            if (rip.progress > 0.14f) {
                val r2 = rip.maxR * ((rip.progress - 0.14f) / 0.86f) * 0.68f
                strokeP.color = Color.argb((alpha * 0.6f).toInt(), Color.red(pal.accent3), Color.green(pal.accent3), Color.blue(pal.accent3))
                canvas.drawCircle(rip.x, rip.y, r2, strokeP)
            }
        }

        // 2. פיצוץ ניצוצות חגיגי בלחיצה על "="
        for (s in sparks) {
            val a = (s.life * 230f).toInt().coerceIn(0, 255)
            fillP.color = Color.argb(a, Color.red(s.color), Color.green(s.color), Color.blue(s.color))
            when (s.shapeType) {
                0 -> canvas.drawCircle(s.x, s.y, s.size * s.life, fillP)
                1 -> {
                    val half = s.size * s.life
                    canvas.drawRect(s.x - half, s.y - half, s.x + half, s.y + half, fillP)
                }
                else -> {
                    strokeP.color = fillP.color
                    strokeP.strokeWidth = 1.6f * d
                    val arm = s.size * 1.25f * s.life
                    canvas.drawLine(s.x - arm, s.y, s.x + arm, s.y, strokeP)
                    canvas.drawLine(s.x, s.y - arm, s.x, s.y + arm, strokeP)
                }
            }
        }

        // 3. גל ניקוי רוחבי בלחיצה על AC
        if (clearWaveProgress < 1f) {
            val bandW = 90f * d
            val cx = -bandW + clearWaveProgress * (w + bandW * 2f)
            val a = (sin(clearWaveProgress * PI).toFloat() * 110f).toInt().coerceIn(0, 255)
            val waveCol = if (pal.isMin) pal.accent3 else pal.accent1
            fillP.shader = LinearGradient(
                cx - bandW, 0f, cx + bandW, 0f,
                intArrayOf(Color.TRANSPARENT, Color.argb(a, Color.red(waveCol), Color.green(waveCol), Color.blue(waveCol)), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(cx - bandW, 0f, cx + bandW, h * 0.42f, fillP)
            fillP.shader = null
        }

        // 4. טבעת אנרגיה בפעולות זיכרון (MC/MR/M+/M-)
        if (memoryOrbitProgress < 1f) {
            val r = (24f + memoryOrbitProgress * 64f) * d
            val a = ((1f - memoryOrbitProgress) * 190f).toInt().coerceIn(0, 255)
            strokeP.color = Color.argb(a, Color.red(pal.warning), Color.green(pal.warning), Color.blue(pal.warning))
            strokeP.strokeWidth = 2.6f * d
            canvas.drawArc(RectF(memX - r, memY - r, memX + r, memY + r), memoryOrbitProgress * 220f, 140f, false, strokeP)
        }

        // 5. אנימציות מעבר מסכים מותאמות ל-4 ערכות הנושא
        if (transitionProgress < 1f) {
            val fade = sin(transitionProgress * PI).toFloat().coerceIn(0f, 1f)
            when {
                pal.isMin -> {
                    val r = transitionProgress * hypot(w, h) * 0.65f
                    strokeP.strokeWidth = 18f * d * (1f - transitionProgress)
                    strokeP.color = Color.argb((fade * 65f).toInt(), Color.red(pal.accent1), Color.green(pal.accent1), Color.blue(pal.accent1))
                    canvas.drawCircle(w * 0.5f, h * 0.15f, r, strokeP)
                }
                activity.isIosGlassMode -> {
                    val x = -w * 0.3f + transitionProgress * w * 1.6f
                    fillP.shader = LinearGradient(
                        x - 70f * d, 0f, x + 70f * d, 120f * d,
                        intArrayOf(Color.TRANSPARENT, Color.argb((fade * 75f).toInt(), 255, 255, 255), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, 0f, w, h, fillP)
                    fillP.shader = null
                }
                pal.isAurora -> {
                    val y = transitionProgress * h
                    val band = 85f * d
                    fillP.shader = LinearGradient(
                        0f, y - band, 0f, y + band,
                        intArrayOf(Color.TRANSPARENT, Color.argb((fade * 85f).toInt(), Color.red(pal.accent1), Color.green(pal.accent1), Color.blue(pal.accent1)), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, y - band, w, y + band, fillP)
                    fillP.shader = null
                }
                else -> {
                    val y = transitionProgress * h
                    strokeP.strokeWidth = 3f * d
                    strokeP.color = Color.argb((fade * 170f).toInt(), Color.red(pal.accent1), Color.green(pal.accent1), Color.blue(pal.accent1))
                    canvas.drawLine(0f, y, w, y, strokeP)
                }
            }
        }

        // 6. הברקת אור חולפת על אזור הכותרת בכניסה לכלים
        if (shimmerProgress < 1f) {
            val sx = -80f * d + shimmerProgress * (w + 160f * d)
            val a = (sin(shimmerProgress * PI).toFloat() * 55f).toInt().coerceIn(0, 255)
            fillP.shader = LinearGradient(
                sx - 60f * d, 0f, sx + 60f * d, 40f * d,
                intArrayOf(Color.TRANSPARENT, Color.argb(a, 255, 255, 255), Color.TRANSPARENT),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 56f * d, w, 155f * d, fillP)
            fillP.shader = null
        }

        // 7. חלקיקי אווירה עדינים על תפריט הצד בזמן שהוא פתוח
        if (drawerSparklesActive) {
            val drawerW = w * 0.72f
            val startX = if (drawerRightSide) (w - drawerW) else 0f
            for (i in dSeedX.indices) {
                val px = startX + dSeedX[i] * drawerW + sin(drawerPhase + i) * 10f * d
                val py = ((dSeedY[i] * h - drawerPhase * 12f * d) % h + h) % h
                val col = if (i % 2 == 0) pal.accent1 else pal.accent2
                val alpha = (24 + (sin(drawerPhase * 2f + i) * 18f).toInt()).coerceIn(8, 52)
                fillP.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
                canvas.drawCircle(px, py, (2.2f + (i % 3) * 1.4f) * d, fillP)
            }
        }
    }
}

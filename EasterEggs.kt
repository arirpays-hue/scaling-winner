package com.example.calculator

import android.animation.ValueAnimator
import android.graphics.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*
import kotlin.random.Random

var activeMediaPlayer: MediaPlayer? = null

// מנגינת יזכור שקטה המיוצרת בזמן אמת (מכבדת את מתג הצליל ומוגנת מול minBufferSize)
fun playSadMemorialMelody(enabled: Boolean = true) {
    if (!enabled) return
    val sampleRate = 22050
    val notes = listOf(440.0 to 460, 523.25 to 460, 493.88 to 460, 392.0 to 520, 329.63 to 640, 440.0 to 900)
    EasterEggEngine.playSynthNotes(sampleRate, notes, 0.36)
}

internal enum class EasterEggVisualType {
    CHEER_PARTY,
    SIREN_911,
    FBI_999,
    MEMORIAL_0710,
    MATRIX_1999,
    EARTHQUAKE_1906,
    ANSWER_42,
    DEVIL_666,
    MOON_420,
    MOAI_69,
    ERROR_404,
    NOKIA_3310,
    GIGACHAD,
    SPELL_8008135,
    SKILL_ISSUE,
    HACKER_NASA,
    PI_314159,
    APOLLO_1969,
    AGENT_007,
    FUTURE_2026,
    EULER_271828
}

internal data class EasterEggSpec(
    val type: EasterEggVisualType,
    val badge: String,
    val title: String,
    val subtitle: String,
    val accentDark: Int,
    val accentLight: Int,
    val secondaryAccent: Int = Color.parseColor("#FFD166"),
    val isRtlTitle: Boolean = false
)

object EasterEggEngine {

    internal data class EggThemePalette(
        val cardTop: Int,
        val cardBottom: Int,
        val cardBorder: Int,
        val panelBg: Int,
        val inkPrimary: Int,
        val inkSecondary: Int,
        val accent: Int,
        val secondary: Int,
        val isMinimalist: Boolean,
        val isAurora: Boolean,
        val isGlass: Boolean
    )

    private val handler = Handler(Looper.getMainLooper())
    private val activeSynthCount = AtomicInteger(0)

    private var stageView: EasterEggStageView? = null
    private var attachedCard: LinearLayout? = null
    private var savedPadding: IntArray? = null
    private var dismissWatcher: TextWatcher? = null
    private var ignoreTextChange = false

    private var flashOverlay: View? = null
    private var flashJob: Runnable? = null

    internal var activeSpec: EasterEggSpec? = null
    internal var shownAtMs: Long = 0L

    private fun isAlive(activity: MainActivity): Boolean =
        !activity.isFinishing && !activity.isDestroyed

    internal fun resolvePalette(activity: MainActivity, spec: EasterEggSpec): EggThemePalette {
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode

        return when {
            isMin -> EggThemePalette(
                cardTop = MinimalistTheme.CARD_WHITE,
                cardBottom = MinimalistTheme.BG_CREAM,
                cardBorder = MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#DDD6CC")),
                panelBg = Color.parseColor("#F1ECE3"),
                inkPrimary = MinimalistTheme.TEXT_CHARCOAL,
                inkSecondary = MinimalistTheme.TEXT_MUTED_GREY,
                accent = MinimalistTheme.ensureReadableTextOnBg(spec.accentLight, MinimalistTheme.CARD_WHITE, 2.3),
                secondary = MinimalistTheme.ensureReadableTextOnBg(spec.secondaryAccent, MinimalistTheme.CARD_WHITE, 2.1),
                isMinimalist = true,
                isAurora = false,
                isGlass = false
            )
            isAur -> EggThemePalette(
                cardTop = AuroraBorealisTheme.EMERALD_NIGHT,
                cardBottom = Color.parseColor("#061722"),
                cardBorder = Color.argb(190, Color.red(spec.accentDark), Color.green(spec.accentDark), Color.blue(spec.accentDark)),
                panelBg = Color.argb(180, 4, 14, 22),
                inkPrimary = Color.WHITE,
                inkSecondary = AuroraBorealisTheme.EXPRESSION_CYAN_GREY,
                accent = spec.accentDark,
                secondary = spec.secondaryAccent,
                isMinimalist = false,
                isAurora = true,
                isGlass = false
            )
            isGlass -> EggThemePalette(
                cardTop = Color.argb(88, 255, 255, 255),
                cardBottom = Color.argb(34, 255, 255, 255),
                cardBorder = Color.argb(150, 255, 255, 255),
                panelBg = Color.argb(66, 12, 20, 36),
                inkPrimary = Color.WHITE,
                inkSecondary = Color.parseColor("#CBD5E1"),
                accent = spec.accentDark,
                secondary = spec.secondaryAccent,
                isMinimalist = false,
                isAurora = false,
                isGlass = true
            )
            else -> {
                val topBg = Utils.lightenColor(activity.bgColor, 0.09f)
                val safeAcc = MinimalistTheme.ensureReadableTextOnBg(spec.accentDark, topBg, 2.3)
                EggThemePalette(
                    cardTop = topBg,
                    cardBottom = Utils.lightenColor(activity.bgColor, 0.04f),
                    cardBorder = Color.argb(145, Color.red(safeAcc), Color.green(safeAcc), Color.blue(safeAcc)),
                    panelBg = activity.bgColor,
                    inkPrimary = Color.WHITE,
                    inkSecondary = Color.parseColor("#94A3B8"),
                    accent = safeAcc,
                    secondary = spec.secondaryAccent,
                    isMinimalist = false,
                    isAurora = false,
                    isGlass = false
                )
            }
        }
    }

    // =========================================================================
    // הצגה וסגירה נקייה של במת ביצת הפסחא בתוך צג המחשבון (ללא שינוי MainActivity)
    // =========================================================================
    internal fun show(activity: MainActivity, spec: EasterEggSpec) {
        if (!isAlive(activity)) return
        dismiss(activity, restoreZeroText = false, stopAudio = false)

        val displayCard = activity.currentView?.parent as? LinearLayout ?: return
        activeSpec = spec
        shownAtMs = System.currentTimeMillis()
        attachedCard = displayCard
        savedPadding = intArrayOf(
            displayCard.paddingLeft,
            displayCard.paddingTop,
            displayCard.paddingRight,
            displayCard.paddingBottom
        )

        activity.currentInput = ""
        activity.justCalculated = true

        // טריגר נקי שמבטיח שהקשה על כל מקש (כולל AC או ספרה) תסגור מיד את הבמה ותחזיר את הצג
        ignoreTextChange = true
        activity.currentView?.text = "\u200B"
        activity.expressionView?.text = ""
        ignoreTextChange = false

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!ignoreTextChange) {
                    dismiss(activity, restoreZeroText = false, stopAudio = true)
                }
            }
        }
        dismissWatcher = watcher
        activity.currentView?.addTextChangedListener(watcher)

        activity.memoryIndicator?.visibility = View.GONE
        activity.expressionView?.visibility = View.GONE
        activity.currentView?.visibility = View.GONE

        val pad = activity.dp(4f)
        displayCard.setPadding(pad, pad, pad, pad)

        val stage = EasterEggStageView(activity, spec).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setOnClickListener {
                activity.feedback(it)
                dismiss(activity, restoreZeroText = true, stopAudio = true)
            }
        }
        stageView = stage
        displayCard.addView(stage)
    }

    internal fun dismiss(activity: MainActivity, restoreZeroText: Boolean = true, stopAudio: Boolean = true) {
        stopFlashOverlay()
        if (stopAudio) releaseActivePlayer()

        val w = dismissWatcher
        dismissWatcher = null
        if (w != null) {
            try { activity.currentView?.removeTextChangedListener(w) } catch (_: Throwable) {}
        }

        val st = stageView
        stageView = null
        if (st != null) {
            try { (st.parent as? ViewGroup)?.removeView(st) } catch (_: Throwable) {}
        }

        val card = attachedCard
        val pads = savedPadding
        attachedCard = null
        savedPadding = null
        if (card != null && pads != null && pads.size == 4) {
            card.setPadding(pads[0], pads[1], pads[2], pads[3])
        }

        activeSpec = null
        if (isAlive(activity)) {
            if (restoreZeroText) {
                ignoreTextChange = true
                activity.currentView?.text = if (activity.currentInput.isEmpty()) "0" else activity.currentInput
                ignoreTextChange = false
            }
            activity.memoryIndicator?.visibility = View.VISIBLE
            activity.expressionView?.visibility = View.VISIBLE
            activity.currentView?.visibility = View.VISIBLE
        }
    }

    internal fun onStageDetached(activity: MainActivity) {
        stopFlashOverlay()
        val w = dismissWatcher
        dismissWatcher = null
        if (w != null) {
            try { activity.currentView?.removeTextChangedListener(w) } catch (_: Throwable) {}
        }
        stageView = null
        attachedCard = null
        savedPadding = null
        activeSpec = null
    }

    // =========================================================================
    // שכבת הבהוב צבעונית לא-הרסנית (שומרת לחלוטין על רקעי זכוכית / זוהר / מינימליסטי)
    // =========================================================================
    internal fun startAmbientFlash(
        activity: MainActivity,
        colors: List<Int>,
        steps: Int,
        intervalMs: Long
    ) {
        stopFlashOverlay()
        if (!isAlive(activity) || colors.isEmpty()) return
        val parent = activity.mainContent?.parent as? FrameLayout ?: return

        val maxAlpha = when {
            activity.isMinimalistMode -> 0.16f
            activity.isIosGlassMode -> 0.24f
            activity.isAuroraMode -> 0.28f
            else -> 0.30f
        }

        val overlay = View(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isClickable = false
            isFocusable = false
            alpha = maxAlpha
            setBackgroundColor(colors.first())
        }
        flashOverlay = overlay
        val mainIdx = parent.indexOfChild(activity.mainContent)
        if (mainIdx >= 0) parent.addView(overlay, mainIdx + 1) else parent.addView(overlay)

        var step = 0
        val job = object : Runnable {
            override fun run() {
                val ov = flashOverlay ?: return
                if (!isAlive(activity) || step >= steps) {
                    stopFlashOverlay()
                    return
                }
                ov.setBackgroundColor(colors[step % colors.size])
                step++
                handler.postDelayed(this, intervalMs)
            }
        }
        flashJob = job
        handler.postDelayed(job, intervalMs)
    }

    private fun stopFlashOverlay() {
        flashJob?.let { handler.removeCallbacks(it) }
        flashJob = null
        val ov = flashOverlay
        flashOverlay = null
        if (ov != null) {
            try { (ov.parent as? ViewGroup)?.removeView(ov) } catch (_: Throwable) {}
        }
    }

    // =========================================================================
    // מערכת אודיו בטוחה (raw + גיבוי סינתטי אוטומטי)
    // =========================================================================
    internal fun releaseActivePlayer() {
        val p = activeMediaPlayer
        activeMediaPlayer = null
        if (p != null) {
            try { p.setOnCompletionListener(null) } catch (_: Throwable) {}
            try { if (p.isPlaying) p.stop() } catch (_: Throwable) {}
            try { p.release() } catch (_: Throwable) {}
        }
    }

    internal fun playRawOrFallback(
        activity: MainActivity,
        rawName: String,
        fallbackNotes: List<Pair<Double, Int>>? = null,
        fallbackGain: Double = 0.45
    ) {
        if (!activity.useSound || !isAlive(activity)) return
        val resId = activity.resources.getIdentifier(rawName, "raw", activity.packageName)
        if (resId != 0) {
            try {
                releaseActivePlayer()
                val mp = MediaPlayer.create(activity.applicationContext, resId)
                if (mp != null) {
                    activeMediaPlayer = mp
                    mp.setOnCompletionListener { player ->
                        if (activeMediaPlayer === player) activeMediaPlayer = null
                        try { player.release() } catch (_: Throwable) {}
                    }
                    mp.setOnErrorListener { player, _, _ ->
                        if (activeMediaPlayer === player) activeMediaPlayer = null
                        try { player.release() } catch (_: Throwable) {}
                        true
                    }
                    mp.start()
                    return
                }
            } catch (_: Throwable) {}
        }
        if (!fallbackNotes.isNullOrEmpty()) {
            playSynthNotes(22050, fallbackNotes, fallbackGain)
        }
    }

    internal fun playSynthNotes(sampleRate: Int, notes: List<Pair<Double, Int>>, gain: Double = 0.44) {
        if (notes.isEmpty() || activeSynthCount.get() >= 3) return
        activeSynthCount.incrementAndGet()
        Thread {
            var track: AudioTrack? = null
            try {
                val totalSamples = notes.sumOf { (it.second * sampleRate) / 1000 }
                if (totalSamples <= 0) return@Thread
                val buffer = ShortArray(totalSamples)
                var offset = 0
                for ((freq, dur) in notes) {
                    val count = (dur * sampleRate) / 1000
                    for (i in 0 until count) {
                        val env = sin((i.toDouble() / count) * PI)
                        val wave = sin(2.0 * PI * freq * i / sampleRate) * 0.80 +
                            sin(4.0 * PI * freq * i / sampleRate) * 0.20
                        buffer[offset + i] = (wave * env * Short.MAX_VALUE * gain).toInt().toShort()
                    }
                    offset += count
                }
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)
                val created = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(buffer.size * 2, minBuf))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track = created
                created.write(buffer, 0, buffer.size)
                created.play()
                Thread.sleep(notes.sumOf { it.second }.toLong() + 40L)
            } catch (_: Throwable) {
            } finally {
                try { track?.stop() } catch (_: Throwable) {}
                try { track?.release() } catch (_: Throwable) {}
                activeSynthCount.decrementAndGet()
            }
        }.start()
    }
}

// =========================================================================
// נקודת הכניסה מ-MainActivity — ביצי פסחא ומיני-משחקים
// =========================================================================
fun MainActivity.checkEasterEggs(input: String): Boolean {
    when (input) {
        // --- מיני-משחקים אינטראקטיביים בתוך המחשבון ---
        "1337" -> {
            EasterEggEngine.dismiss(this, restoreZeroText = false, stopAudio = true)
            MiniGameEngine.startTicTacToe(this)
            return true
        }
        "8080" -> {
            EasterEggEngine.dismiss(this, restoreZeroText = false, stopAudio = true)
            MiniGameEngine.startCalcInvaders(this)
            return true
        }
        "5555" -> {
            EasterEggEngine.dismiss(this, restoreZeroText = false, stopAudio = true)
            MiniGameEngine.startMathRacer(this)
            return true
        }
        "1010" -> {
            EasterEggEngine.dismiss(this, restoreZeroText = false, stopAudio = true)
            MiniGameEngine.startBombDefusal(this)
            return true
        }
        "7777" -> {
            EasterEggEngine.dismiss(this, restoreZeroText = false, stopAudio = true)
            MiniGameEngine.startAnimalParty(this)
            return true
        }

        // --- ביצי פסחא קלאסיות ומודרניות מותאמות ל-4 ערכות הנושא ---
        "123456789" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.CHEER_PARTY,
                    badge = "SEQUENCE COMPLETE",
                    title = "1-2-3-4-5-6-7-8-9!",
                    subtitle = "רצף מושלם • הקש לחזרה",
                    accentDark = Color.parseColor("#FFD166"),
                    accentLight = Color.parseColor("#C67C00"),
                    secondaryAccent = Color.parseColor("#00E676")
                )
            )
            EasterEggEngine.playRawOrFallback(
                this, "cheer",
                listOf(523.25 to 110, 659.25 to 110, 783.99 to 130, 1046.50 to 280)
            )
            EasterEggEngine.startAmbientFlash(
                this,
                listOf(Color.parseColor("#EF4444"), Color.parseColor("#10B981"), Color.parseColor("#3B82F6"), Color.parseColor("#F59E0B"), Color.parseColor("#A855F7")),
                steps = 14, intervalMs = 110L
            )
            return true
        }

        "911" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.SIREN_911,
                    badge = "EMERGENCY 911",
                    title = "סירנת חירום פעילה",
                    subtitle = "הקש או לחץ AC להשתקה",
                    accentDark = Color.parseColor("#FF453A"),
                    accentLight = Color.parseColor("#C62828"),
                    secondaryAccent = Color.parseColor("#38BDF8"),
                    isRtlTitle = true
                )
            )
            EasterEggEngine.playRawOrFallback(
                this, "siren",
                listOf(720.0 to 260, 540.0 to 260, 720.0 to 260, 540.0 to 260, 720.0 to 260, 540.0 to 260)
            )
            EasterEggEngine.startAmbientFlash(
                this,
                listOf(Color.parseColor("#EF4444"), Color.parseColor("#2563EB")),
                steps = 18, intervalMs = 150L
            )
            return true
        }

        "999" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.FBI_999,
                    badge = "FBI OPEN UP",
                    title = "We Know What You Did",
                    subtitle = "סוכני ה-FBI בדרך אליך • הקש לסגירה",
                    accentDark = Color.parseColor("#FF3B30"),
                    accentLight = Color.parseColor("#B91C1C"),
                    secondaryAccent = Color.parseColor("#38BDF8")
                )
            )
            shakeScreen()
            EasterEggEngine.playRawOrFallback(
                this, "fbiopenupsfx",
                listOf(180.0 to 160, 820.0 to 220, 580.0 to 220, 820.0 to 220)
            )
            EasterEggEngine.startAmbientFlash(
                this,
                listOf(Color.parseColor("#DC2626"), Color.parseColor("#1D4ED8")),
                steps = 14, intervalMs = 120L
            )
            return true
        }

        "0710", "710" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.MEMORIAL_0710,
                    badge = "07 / 10 / 2023",
                    title = "לא נשכח ולא נסלח",
                    subtitle = "יהי זכרם ברוך",
                    accentDark = Color.parseColor("#FFD700"),
                    accentLight = Color.parseColor("#B58900"),
                    secondaryAccent = Color.parseColor("#FFA000"),
                    isRtlTitle = true
                )
            )
            playSadMemorialMelody(useSound)
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }

        "1999" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.MATRIX_1999,
                    badge = "THE MATRIX • 1999",
                    title = "Wake up, Neo...",
                    subtitle = "Follow the white rabbit",
                    accentDark = Color.parseColor("#00FF41"),
                    accentLight = Color.parseColor("#15803D"),
                    secondaryAccent = Color.parseColor("#EF4444")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(220.0 to 90, 261.6 to 90, 329.6 to 110, 440.0 to 180), 0.35)
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return true
        }

        "1906" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.EARTHQUAKE_1906,
                    badge = "SAN FRANCISCO • 1906",
                    title = "Earthquake! 7.9 Mw",
                    subtitle = "רישום סיסמוגרף בזמן אמת",
                    accentDark = Color.parseColor("#FB923C"),
                    accentLight = Color.parseColor("#C2410C"),
                    secondaryAccent = Color.parseColor("#EF4444")
                )
            )
            shakeScreen()
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(85.0 to 180, 68.0 to 200, 92.0 to 180, 60.0 to 260), 0.50)
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }

        "42" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.ANSWER_42,
                    badge = "THE ANSWER TO EVERYTHING",
                    title = "42",
                    subtitle = "התשובה לחיים, ליקום וכל השאר",
                    accentDark = Color.parseColor("#38BDF8"),
                    accentLight = Color.parseColor("#0369A1"),
                    secondaryAccent = Color.parseColor("#FFD166")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(392.0 to 120, 493.88 to 120, 587.33 to 200), 0.38)
            return true
        }

        "666" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.DEVIL_666,
                    badge = "666 • UNDERWORLD",
                    title = "Not Today",
                    subtitle = "הקש או לחץ AC לחזרה לאור",
                    accentDark = Color.parseColor("#FF3B30"),
                    accentLight = Color.parseColor("#B91C1C"),
                    secondaryAccent = Color.parseColor("#FB923C")
                )
            )
            EasterEggEngine.playRawOrFallback(
                this, "evil_laugh",
                listOf(185.0 to 140, 165.0 to 140, 147.0 to 160, 130.0 to 260)
            )
            if (useHaptic) mainContent?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            return true
        }

        "420" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.MOON_420,
                    badge = "MUCH CALCULATOR • VERY MATH",
                    title = "To The Moon!",
                    subtitle = "Wow! Such Profit • Stonks Only Go Up",
                    accentDark = Color.parseColor("#00E676"),
                    accentLight = Color.parseColor("#15803D"),
                    secondaryAccent = Color.parseColor("#FFD166")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(440.0 to 80, 554.37 to 80, 659.25 to 90, 880.0 to 200), 0.40)
            return true
        }

        "69" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.MOAI_69,
                    badge = "MOAI APPROVED",
                    title = "Nice.",
                    subtitle = "אישור רשמי של פסל המואי",
                    accentDark = Color.parseColor("#C084FC"),
                    accentLight = Color.parseColor("#7E22CE"),
                    secondaryAccent = Color.parseColor("#F59E0B")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(110.0 to 220, 82.4 to 340), 0.50)
            EasterEggEngine.startAmbientFlash(
                this,
                listOf(Color.parseColor("#7B1FA2"), Color.parseColor("#E040FB")),
                steps = 8, intervalMs = 110L
            )
            return true
        }

        "404" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.ERROR_404,
                    badge = "ERROR 404 • NOT FOUND",
                    title = "Task Failed Successfully",
                    subtitle = "החישוב שביקשת הלך לאיבוד ברשת",
                    accentDark = Color.parseColor("#F87171"),
                    accentLight = Color.parseColor("#B91C1C"),
                    secondaryAccent = Color.parseColor("#38BDF8")
                )
            )
            shakeScreen()
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(300.0 to 120, 220.0 to 220), 0.42)
            return true
        }

        "3310" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.NOKIA_3310,
                    badge = "NOKIA 3310 MODE",
                    title = "Indestructible!",
                    subtitle = "המכשיר שלך כעת חסין מפני אסטרואיד",
                    accentDark = Color.parseColor("#A3E635"),
                    accentLight = Color.parseColor("#3F6212"),
                    secondaryAccent = Color.parseColor("#38BDF8")
                )
            )
            shakeScreen()
            // רינגטון נוקיה קצרצר מבוסס סינתזה
            if (useSound) {
                EasterEggEngine.playSynthNotes(
                    22050,
                    listOf(659.25 to 110, 587.33 to 110, 369.99 to 190, 415.30 to 190, 554.37 to 110, 493.88 to 110, 293.66 to 190, 329.63 to 240),
                    0.42
                )
            }
            return true
        }

        "6942069" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.GIGACHAD,
                    badge = "GIGACHAD DETECTED",
                    title = "Absolute Legend",
                    subtitle = "השגת מעמד של אגדה מוחלטת",
                    accentDark = Color.parseColor("#FFD700"),
                    accentLight = Color.parseColor("#B45309"),
                    secondaryAccent = Color.parseColor("#38BDF8")
                )
            )
            shakeScreen()
            MiniGameEngine.playTttResultSound(this, true)
            return true
        }

        "8008135" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.SPELL_8008135,
                    badge = "RETRO LCD SPELLING",
                    title = "BOOBIES",
                    subtitle = "הפכת את המחשבון כמו בכיתה ו׳",
                    accentDark = Color.parseColor("#F472B6"),
                    accentLight = Color.parseColor("#BE185D"),
                    secondaryAccent = Color.parseColor("#22D3EE")
                )
            )
            MiniGameEngine.playSqueakSound(useSound)
            EasterEggEngine.startAmbientFlash(
                this,
                listOf(Color.parseColor("#EC4899"), Color.parseColor("#06B6D4"), Color.parseColor("#FACC15")),
                steps = 9, intervalMs = 105L
            )
            return true
        }

        "404404404" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.SKILL_ISSUE,
                    badge = "CRITICAL SYSTEM ALERT",
                    title = "Skill Issue",
                    subtitle = "נשמר בהיסטוריה למזכרת",
                    accentDark = Color.parseColor("#FB7185"),
                    accentLight = Color.parseColor("#BE123C"),
                    secondaryAccent = Color.parseColor("#FBBF24")
                )
            )
            shakeScreen()
            MiniGameEngine.playTttResultSound(this, false)
            historyList.add(0, "404404404 = Skill Issue")
            savePreferences()
            return true
        }

        "13371337" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.HACKER_NASA,
                    badge = "ROOT@MAINFRAME:~#",
                    title = "> ACCESS GRANTED",
                    subtitle = "מתחבר ללוויין של נאס״א...",
                    accentDark = Color.parseColor("#00FF41"),
                    accentLight = Color.parseColor("#15803D"),
                    secondaryAccent = Color.parseColor("#38BDF8")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(880.0 to 65, 1174.6 to 65, 1318.5 to 65, 1760.0 to 160), 0.36)
            return true
        }

        // --- 5 ביצי פסחא מודרניות חדשות ---
        "314159" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.PI_314159,
                    badge = "PI DAY • ARCHIMEDES",
                    title = "π = 3.14159265...",
                    subtitle = "יחס ההיקף לקוטר — אינסופי ומדויק",
                    accentDark = Color.parseColor("#FBBF24"),
                    accentLight = Color.parseColor("#B45309"),
                    secondaryAccent = Color.parseColor("#38BDF8")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(523.25 to 110, 392.0 to 110, 440.0 to 110, 659.25 to 220), 0.38)
            return true
        }

        "1969" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.APOLLO_1969,
                    badge = "APOLLO 11 • 1969",
                    title = "Eagle Has Landed",
                    subtitle = "צעד קטן למחשבון, צעד גדול למתמטיקה",
                    accentDark = Color.parseColor("#7DD3FC"),
                    accentLight = Color.parseColor("#0369A1"),
                    secondaryAccent = Color.parseColor("#FDE047")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(261.63 to 180, 392.0 to 180, 523.25 to 300), 0.42)
            return true
        }

        "007" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.AGENT_007,
                    badge = "CLASSIFIED • MI6",
                    title = "Bond. James Bond.",
                    subtitle = "רישיון לחשב • הקש לחזרה",
                    accentDark = Color.parseColor("#F43F5E"),
                    accentLight = Color.parseColor("#9F1239"),
                    secondaryAccent = Color.parseColor("#E2E8F0")
                )
            )
            // מוטיב מרגלים קלאסי בסינתזה
            if (useSound) {
                EasterEggEngine.playSynthNotes(
                    22050,
                    listOf(164.81 to 150, 174.61 to 150, 185.00 to 150, 174.61 to 150, 164.81 to 260),
                    0.46
                )
            }
            return true
        }

        "2026" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.FUTURE_2026,
                    badge = "TIME CAPSULE • 2026",
                    title = "SwissKnife Pro 2026",
                    subtitle = "העתיד כבר כאן בכיס שלך",
                    accentDark = Color.parseColor("#00F5D4"),
                    accentLight = Color.parseColor("#0F766E"),
                    secondaryAccent = Color.parseColor("#A78BFA")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(440.0 to 90, 659.25 to 90, 880.0 to 90, 1318.5 to 220), 0.38)
            return true
        }

        "271828" -> {
            EasterEggEngine.show(
                this,
                EasterEggSpec(
                    type = EasterEggVisualType.EULER_271828,
                    badge = "EULER'S NUMBER",
                    title = "e ≈ 2.718281828",
                    subtitle = "בסיס הלוגריתם הטבעי והגדילה המעריכית",
                    accentDark = Color.parseColor("#34D399"),
                    accentLight = Color.parseColor("#047857"),
                    secondaryAccent = Color.parseColor("#FBBF24")
                )
            )
            if (useSound) EasterEggEngine.playSynthNotes(22050, listOf(329.63 to 100, 440.0 to 100, 587.33 to 120, 783.99 to 220), 0.38)
            return true
        }
    }
    return false
}

// =========================================================================
// במה וקטורית מונפשת ב-60fps לביצי הפסחא — מותאמת לכל 4 ערכות הנושא
// =========================================================================
private class EasterEggStageView(
    private val activity: MainActivity,
    private val spec: EasterEggSpec
) : View(activity) {

    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var anim: ValueAnimator? = null

    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val badgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT_BOLD
    }
    private val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val centerTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val rect = RectF()
    private val path = Path()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (ProCanvasAnimations.isCanvasAnimationsEnabled && activity.useAnimations) {
            anim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 10_000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    phase += 0.055f
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
        EasterEggEngine.onStageDetached(activity)
        super.onDetachedFromWindow()
    }

    private fun withAlpha(col: Int, a: Int): Int =
        Color.argb(a.coerceIn(0, 255), Color.red(col), Color.green(col), Color.blue(col))

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 4f || h <= 4f) return

        val pal = EasterEggEngine.resolvePalette(activity, spec)
        drawCardSurface(canvas, w, h, pal)
        drawAmbientScene(canvas, w, h, pal)

        // אזור הסמל הווקטורי האישי משמאל
        val emblemCx = w * 0.21f
        val emblemCy = h * 0.50f
        val emblemR = min(h * 0.34f, w * 0.145f).coerceIn(22f * d, 42f * d)

        // הילה רכה מאחורי הסמל
        fillP.shader = RadialGradient(
            emblemCx, emblemCy, (emblemR * 1.45f).coerceAtLeast(1f),
            withAlpha(pal.accent, if (pal.isMinimalist) 42 else 75),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(emblemCx, emblemCy, emblemR * 1.45f, fillP)
        fillP.shader = null

        drawCustomVectorEmblem(canvas, emblemCx, emblemCy, emblemR, pal)

        // טקסט מיושר לימין בצד הימני של הבמה
        val textRight = w - 16f * d
        val maxTextW = (w - (emblemCx + emblemR + 24f * d)).coerceAtLeast(80f * d)

        // תג קטגוריה עליון (Pill)
        badgeP.textSize = 9.5f * d
        val badgeW = badgeP.measureText(spec.badge).coerceAtMost(maxTextW - 14f * d) + 14f * d
        val badgeTop = h * 0.14f
        val badgeBot = badgeTop + 17f * d
        rect.set(textRight - badgeW, badgeTop, textRight, badgeBot)
        fillP.color = withAlpha(pal.accent, if (pal.isMinimalist) 34 else 52)
        canvas.drawRoundRect(rect, 9f * d, 9f * d, fillP)
        strokeP.clearShadowLayer()
        strokeP.strokeWidth = 1f * d
        strokeP.color = withAlpha(pal.accent, 145)
        canvas.drawRoundRect(rect, 9f * d, 9f * d, strokeP)

        centerTextP.textSize = 9.2f * d
        centerTextP.color = pal.accent
        canvas.drawText(spec.badge, rect.centerX(), rect.centerY() + 3.2f * d, centerTextP)

        // כותרת ראשית מותאמת רוחב אוטומטית
        var tSize = 20f * d
        titleP.textSize = tSize
        val titleDisp = if (spec.isRtlTitle) "\u200F${spec.title}" else "\u200E${spec.title}"
        while (titleP.measureText(titleDisp) > maxTextW && tSize > 11.5f * d) {
            tSize -= 1f * d
            titleP.textSize = tSize
        }
        titleP.color = pal.inkPrimary
        if (pal.isAurora) {
            titleP.setShadowLayer(7f * d, 0f, 0f, withAlpha(pal.accent, 160))
        } else {
            titleP.clearShadowLayer()
        }
        canvas.drawText(titleDisp, textRight, h * 0.56f, titleP)
        titleP.clearShadowLayer()

        // כותרת משנה
        var sSize = 11.8f * d
        subP.textSize = sSize
        val subDisp = "\u200F${spec.subtitle}"
        while (subP.measureText(subDisp) > maxTextW && sSize > 9f * d) {
            sSize -= 0.7f * d
            subP.textSize = sSize
        }
        subP.color = pal.inkSecondary
        canvas.drawText(subDisp, textRight, h * 0.76f, subP)

        // רמז סגירה עדין בתחתית
        subP.textSize = 8.8f * d
        subP.color = withAlpha(pal.inkSecondary, 160)
        canvas.drawText("\u200Fהקש לסגירה או המשך לחשב", textRight, h - 8f * d, subP)
    }

    private fun drawCardSurface(canvas: Canvas, w: Float, h: Float, pal: EasterEggEngine.EggThemePalette) {
        rect.set(2f * d, 2f * d, w - 2f * d, h - 2f * d)
        val corner = 20f * d
        fillP.shader = LinearGradient(0f, 0f, w, h, pal.cardTop, pal.cardBottom, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, corner, corner, fillP)
        fillP.shader = null

        if (pal.isGlass) {
            val gloss = RectF(10f * d, 4f * d, w - 10f * d, h * 0.24f)
            fillP.shader = LinearGradient(0f, gloss.top, 0f, gloss.bottom, Color.argb(62, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(gloss, 14f * d, 14f * d, fillP)
            fillP.shader = null
        } else if (!pal.isMinimalist && !pal.isAurora) {
            strokeP.clearShadowLayer()
            strokeP.strokeWidth = 0.7f * d
            strokeP.color = withAlpha(pal.accent, 24)
            var gx = 20f * d
            while (gx < w - 10f * d) {
                canvas.drawLine(gx, 8f * d, gx, h - 8f * d, strokeP)
                gx += 20f * d
            }
        }

        strokeP.strokeWidth = 1.4f * d
        strokeP.color = pal.cardBorder
        if (pal.isAurora) {
            strokeP.setShadowLayer(6f * d, 0f, 0f, withAlpha(pal.accent, 175))
        } else {
            strokeP.clearShadowLayer()
        }
        canvas.drawRoundRect(rect, corner, corner, strokeP)
        strokeP.clearShadowLayer()
    }

    // אפקטי רקע חיים עדינים המותאמים לכל ביצת פסחא
    private fun drawAmbientScene(canvas: Canvas, w: Float, h: Float, pal: EasterEggEngine.EggThemePalette) {
        when (spec.type) {
            EasterEggVisualType.MATRIX_1999, EasterEggVisualType.HACKER_NASA -> {
                val glyphs = charArrayOf('0', '1', '7', 'A', 'F', 'Σ', 'λ', '9')
                centerTextP.textSize = 9.5f * d
                for (col in 0 until 12) {
                    val x = 14f * d + col * ((w - 28f * d) / 11f)
                    val dropY = ((phase * 28f * d + col * 37f * d) % (h + 24f * d)) - 12f * d
                    centerTextP.color = withAlpha(pal.accent, if (pal.isMinimalist) 48 else 82)
                    canvas.drawText(glyphs[(col + (phase * 2f).toInt()) % glyphs.size].toString(), x, dropY, centerTextP)
                    centerTextP.color = withAlpha(pal.accent, if (pal.isMinimalist) 24 else 40)
                    canvas.drawText(glyphs[(col + 3) % glyphs.size].toString(), x, dropY - 12f * d, centerTextP)
                }
            }
            EasterEggVisualType.CHEER_PARTY, EasterEggVisualType.GIGACHAD, EasterEggVisualType.SPELL_8008135 -> {
                val confettiCols = intArrayOf(pal.accent, pal.secondary, Color.parseColor("#38BDF8"), Color.parseColor("#F472B6"))
                for (i in 0 until 14) {
                    val cx = 14f * d + ((i * 53f * d + sin(phase + i) * 14f * d) % (w - 28f * d).coerceAtLeast(20f))
                    val cy = ((phase * 22f * d + i * 29f * d) % (h - 14f * d).coerceAtLeast(20f)) + 7f * d
                    fillP.color = withAlpha(confettiCols[i % confettiCols.size], if (pal.isMinimalist) 115 else 155)
                    canvas.save()
                    canvas.rotate(phase * 45f + i * 30f, cx, cy)
                    canvas.drawRect(cx - 2.6f * d, cy - 1.4f * d, cx + 2.6f * d, cy + 1.4f * d, fillP)
                    canvas.restore()
                }
            }
            EasterEggVisualType.SIREN_911, EasterEggVisualType.FBI_999 -> {
                val leftRed = ((phase * 3.5f).toInt() % 2 == 0)
                val colL = if (leftRed) Color.parseColor("#EF4444") else Color.parseColor("#3B82F6")
                val colR = if (leftRed) Color.parseColor("#3B82F6") else Color.parseColor("#EF4444")
                val a = if (pal.isMinimalist) 32 else 55
                fillP.shader = LinearGradient(0f, 0f, w * 0.45f, 0f, withAlpha(colL, a), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawRoundRect(RectF(4f * d, 4f * d, w * 0.45f, h - 4f * d), 18f * d, 18f * d, fillP)
                fillP.shader = LinearGradient(w, 0f, w * 0.55f, 0f, withAlpha(colR, a), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawRoundRect(RectF(w * 0.55f, 4f * d, w - 4f * d, h - 4f * d), 18f * d, 18f * d, fillP)
                fillP.shader = null
            }
            EasterEggVisualType.EARTHQUAKE_1906 -> {
                // גל סיסמוגרף חי לרוחב התחתית
                path.reset()
                val baseY = h * 0.86f
                var x = 14f * d
                path.moveTo(x, baseY)
                while (x < w - 14f * d) {
                    val t = (x / w) * 16f + phase * 3.2f
                    val amp = (sin(t * 2.7f) * cos(t * 1.1f)) * 7f * d
                    path.lineTo(x, baseY + amp)
                    x += 5f * d
                }
                strokeP.strokeWidth = 1.4f * d
                strokeP.color = withAlpha(pal.accent, 115)
                canvas.drawPath(path, strokeP)
            }
            EasterEggVisualType.ERROR_404, EasterEggVisualType.SKILL_ISSUE -> {
                // פסי גליץ' אופקיים עדינים
                for (g in 0 until 3) {
                    val gy = ((phase * 34f * d + g * 43f * d) % (h - 16f * d).coerceAtLeast(16f)) + 8f * d
                    fillP.color = withAlpha(if (g % 2 == 0) pal.accent else pal.secondary, 36)
                    canvas.drawRect(10f * d, gy, w - 10f * d, gy + 2f * d, fillP)
                }
            }
            EasterEggVisualType.APOLLO_1969, EasterEggVisualType.ANSWER_42 -> {
                for (s in 0 until 10) {
                    val sx = 14f * d + ((s * 67f * d) % (w - 28f * d).coerceAtLeast(20f))
                    val sy = 10f * d + ((s * 31f * d) % (h - 20f * d).coerceAtLeast(20f))
                    val tw = (0.4f + 0.6f * (sin(phase * 2f + s) * 0.5f + 0.5f))
                    fillP.color = withAlpha(pal.inkSecondary, (tw * 120f).toInt())
                    canvas.drawCircle(sx, sy, 1.4f * d, fillP)
                }
            }
            else -> {}
        }
    }

    // =========================================================================
    // 21 סמלים וקטוריים אישיים ומעוצבים בקנבס לכל ביצת פסחא
    // =========================================================================
    private fun drawCustomVectorEmblem(
        canvas: Canvas, cx: Float, cy: Float, r: Float, pal: EasterEggEngine.EggThemePalette
    ) {
        strokeP.clearShadowLayer()
        if (pal.isAurora) {
            strokeP.setShadowLayer(6f * d, 0f, 0f, withAlpha(pal.accent, 165))
        }

        when (spec.type) {
            EasterEggVisualType.MEMORIAL_0710 -> {
                // סרט מודעות צהוב וקטורי + נר זיכרון מהבהב עדין
                val rx = cx - r * 0.24f
                strokeP.strokeWidth = r * 0.24f
                strokeP.color = pal.accent
                path.reset()
                path.moveTo(rx - r * 0.42f, cy + r * 0.72f)
                path.cubicTo(rx - r * 0.05f, cy + r * 0.10f, rx + r * 0.46f, cy - r * 0.35f, rx, cy - r * 0.72f)
                path.cubicTo(rx - r * 0.46f, cy - r * 0.35f, rx + r * 0.05f, cy + r * 0.10f, rx + r * 0.42f, cy + r * 0.72f)
                canvas.drawPath(path, strokeP)

                // נר זיכרון מימין לסרט
                val candleX = cx + r * 0.62f
                rect.set(candleX - r * 0.16f, cy - r * 0.02f, candleX + r * 0.16f, cy + r * 0.72f)
                fillP.color = if (pal.isMinimalist) Color.parseColor("#E5DFD3") else Color.argb(220, 245, 245, 245)
                canvas.drawRoundRect(rect, 3f * d, 3f * d, fillP)
                // להבה מרצדת בעדינות
                val flicker = sin(phase * 3.6f) * r * 0.06f
                path.reset()
                path.moveTo(candleX, cy - r * 0.56f + flicker)
                path.quadTo(candleX + r * 0.22f, cy - r * 0.20f, candleX, cy - r * 0.05f)
                path.quadTo(candleX - r * 0.22f, cy - r * 0.20f, candleX, cy - r * 0.56f + flicker)
                path.close()
                fillP.color = Color.parseColor("#FF9800")
                canvas.drawPath(path, fillP)
                fillP.color = Color.parseColor("#FFF176")
                canvas.drawCircle(candleX, cy - r * 0.18f, r * 0.08f, fillP)
            }

            EasterEggVisualType.SIREN_911 -> {
                // צ'קלקת משטרה דו-צבעית מונפשת
                val flash = (phase * 3.5f).toInt() % 2 == 0
                val leftCol = if (flash) Color.parseColor("#EF4444") else Color.parseColor("#7F1D1D")
                val rightCol = if (!flash) Color.parseColor("#3B82F6") else Color.parseColor("#1E3A8A")
                // בסיס מתכת
                rect.set(cx - r * 0.88f, cy + r * 0.18f, cx + r * 0.88f, cy + r * 0.48f)
                fillP.color = pal.inkPrimary
                canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
                // כיפת אדום + כיפת כחול
                fillP.color = leftCol
                canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.36f, cx - r * 0.06f, cy + r * 0.22f), 8f * d, 8f * d, fillP)
                fillP.color = rightCol
                canvas.drawRoundRect(RectF(cx + r * 0.06f, cy - r * 0.36f, cx + r * 0.82f, cy + r * 0.22f), 8f * d, 8f * d, fillP)
                // קרני אור מסתובבות
                strokeP.strokeWidth = 2f * d
                strokeP.color = if (flash) Color.parseColor("#EF4444") else Color.parseColor("#38BDF8")
                for (i in -2..2) {
                    val ang = Math.toRadians(-90.0 + i * 28.0)
                    canvas.drawLine(
                        cx + (cos(ang) * r * 0.56f).toFloat(),
                        cy - r * 0.20f + (sin(ang) * r * 0.32f).toFloat(),
                        cx + (cos(ang) * r * 0.98f).toFloat(),
                        cy - r * 0.20f + (sin(ang) * r * 0.72f).toFloat(),
                        strokeP
                    )
                }
            }

            EasterEggVisualType.FBI_999 -> {
                // תג סוכן פדרלי וקטורי + כוכב 5 קודקודים
                path.reset()
                path.moveTo(cx, cy - r * 0.88f)
                path.lineTo(cx + r * 0.76f, cy - r * 0.52f)
                path.lineTo(cx + r * 0.72f, cy + r * 0.12f)
                path.quadTo(cx + r * 0.65f, cy + r * 0.68f, cx, cy + r * 0.90f)
                path.quadTo(cx - r * 0.65f, cy + r * 0.68f, cx - r * 0.72f, cy + r * 0.12f)
                path.lineTo(cx - r * 0.76f, cy - r * 0.52f)
                path.close()
                fillP.color = withAlpha(pal.accent, 52)
                canvas.drawPath(path, fillP)
                strokeP.strokeWidth = 2.4f * d
                strokeP.color = pal.accent
                canvas.drawPath(path, strokeP)
                drawStarShape(canvas, cx, cy - r * 0.05f, r * 0.38f, pal.secondary)
                centerTextP.textSize = r * 0.34f
                centerTextP.color = pal.inkPrimary
                canvas.drawText("FBI", cx, cy + r * 0.56f, centerTextP)
            }

            EasterEggVisualType.MATRIX_1999 -> {
                // גלולה אדומה וכחולה מרחפות + קוד בינארי
                val bob = sin(phase * 2.2f) * 3.5f * d
                canvas.save()
                canvas.rotate(-26f, cx - r * 0.30f, cy + bob)
                rect.set(cx - r * 0.66f, cy - r * 0.22f + bob, cx + r * 0.06f, cy + r * 0.22f + bob)
                fillP.color = Color.parseColor("#EF4444")
                canvas.drawRoundRect(rect, r * 0.22f, r * 0.22f, fillP)
                canvas.restore()

                canvas.save()
                canvas.rotate(26f, cx + r * 0.30f, cy - bob)
                rect.set(cx - r * 0.06f, cy - r * 0.22f - bob, cx + r * 0.66f, cy + r * 0.22f - bob)
                fillP.color = Color.parseColor("#3B82F6")
                canvas.drawRoundRect(rect, r * 0.22f, r * 0.22f, fillP)
                canvas.restore()

                strokeP.strokeWidth = 1.6f * d
                strokeP.color = pal.accent
                canvas.drawCircle(cx, cy, r * 0.88f, strokeP)
            }

            EasterEggVisualType.EARTHQUAKE_1906 -> {
                // כדור הארץ עם שבר טקטוני מזוגזג
                val jitter = sin(phase * 9f) * 2.5f * d
                fillP.color = withAlpha(pal.accent, 42)
                canvas.drawCircle(cx + jitter, cy, r * 0.80f, fillP)
                strokeP.strokeWidth = 2.2f * d
                strokeP.color = pal.accent
                canvas.drawCircle(cx + jitter, cy, r * 0.80f, strokeP)

                path.reset()
                path.moveTo(cx + jitter - r * 0.25f, cy - r * 0.72f)
                path.lineTo(cx + jitter + r * 0.14f, cy - r * 0.20f)
                path.lineTo(cx + jitter - r * 0.18f, cy + r * 0.15f)
                path.lineTo(cx + jitter + r * 0.22f, cy + r * 0.72f)
                strokeP.strokeWidth = 2.6f * d
                strokeP.color = pal.secondary
                canvas.drawPath(path, strokeP)
            }

            EasterEggVisualType.ANSWER_42 -> {
                // גלקסיה אטומית עם 2 טבעות אורביטליות מסתובבות + ליבת 42
                strokeP.strokeWidth = 1.8f * d
                for (i in 0..1) {
                    canvas.save()
                    canvas.rotate(phase * 30f + i * 65f, cx, cy)
                    rect.set(cx - r * 0.92f, cy - r * 0.36f, cx + r * 0.92f, cy + r * 0.36f)
                    strokeP.color = if (i == 0) pal.accent else pal.secondary
                    canvas.drawOval(rect, strokeP)
                    canvas.restore()
                }
                fillP.color = pal.panelBg
                canvas.drawCircle(cx, cy, r * 0.48f, fillP)
                strokeP.color = pal.accent
                canvas.drawCircle(cx, cy, r * 0.48f, strokeP)
                centerTextP.textSize = r * 0.56f
                centerTextP.color = pal.inkPrimary
                canvas.drawText("42", cx, cy + r * 0.20f, centerTextP)
            }

            EasterEggVisualType.DEVIL_666 -> {
                // קרניים גאומטריות + להבת גהנום נקייה
                fillP.color = withAlpha(pal.accent, 48)
                canvas.drawCircle(cx, cy + r * 0.12f, r * 0.64f, fillP)
                strokeP.strokeWidth = 2.4f * d
                strokeP.color = pal.accent
                canvas.drawCircle(cx, cy + r * 0.12f, r * 0.64f, strokeP)

                // 2 קרניים מעוקלות
                fillP.color = pal.accent
                path.reset()
                path.moveTo(cx - r * 0.50f, cy - r * 0.22f)
                path.quadTo(cx - r * 0.85f, cy - r * 0.76f, cx - r * 0.36f, cy - r * 0.88f)
                path.quadTo(cx - r * 0.45f, cy - r * 0.46f, cx - r * 0.20f, cy - r * 0.42f)
                path.close()
                canvas.drawPath(path, fillP)

                path.reset()
                path.moveTo(cx + r * 0.50f, cy - r * 0.22f)
                path.quadTo(cx + r * 0.85f, cy - r * 0.76f, cx + r * 0.36f, cy - r * 0.88f)
                path.quadTo(cx + r * 0.45f, cy - r * 0.46f, cx + r * 0.20f, cy - r * 0.42f)
                path.close()
                canvas.drawPath(path, fillP)

                centerTextP.textSize = r * 0.52f
                centerTextP.color = pal.secondary
                canvas.drawText("666", cx, cy + r * 0.30f, centerTextP)
            }

            EasterEggVisualType.MOON_420 -> {
                // טיל וקטורי ממריא לעבר סהר ירח + גרף עולה
                // סהר ירח בפינה העליונה-ימנית
                fillP.color = pal.secondary
                canvas.drawCircle(cx + r * 0.48f, cy - r * 0.44f, r * 0.28f, fillP)
                fillP.color = pal.cardTop
                canvas.drawCircle(cx + r * 0.58f, cy - r * 0.50f, r * 0.24f, fillP)

                // גרף עולה למטה
                path.reset()
                path.moveTo(cx - r * 0.82f, cy + r * 0.68f)
                path.lineTo(cx - r * 0.32f, cy + r * 0.38f)
                path.lineTo(cx - r * 0.08f, cy + r * 0.52f)
                path.lineTo(cx + r * 0.62f, cy - r * 0.02f)
                strokeP.strokeWidth = 2.2f * d
                strokeP.color = pal.accent
                canvas.drawPath(path, strokeP)

                // טיל ממריא באלכסון
                val boost = sin(phase * 3f) * 3f * d
                canvas.save()
                canvas.rotate(38f, cx - r * 0.14f, cy - r * 0.06f - boost)
                val rx = cx - r * 0.14f; val ry = cy - r * 0.06f - boost
                path.reset()
                path.moveTo(rx, ry - r * 0.58f)
                path.quadTo(rx + r * 0.28f, ry - r * 0.12f, rx + r * 0.20f, ry + r * 0.34f)
                path.lineTo(rx - r * 0.20f, ry + r * 0.34f)
                path.quadTo(rx - r * 0.28f, ry - r * 0.12f, rx, ry - r * 0.58f)
                path.close()
                fillP.color = pal.accent
                canvas.drawPath(path, fillP)
                fillP.color = pal.cardTop
                canvas.drawCircle(rx, ry - r * 0.10f, r * 0.11f, fillP)
                fillP.color = pal.secondary
                canvas.drawCircle(rx, ry + r * 0.46f, r * 0.14f, fillP)
                canvas.restore()
            }

            EasterEggVisualType.MOAI_69 -> {
                // פסל מואי גאומטרי מפוסל
                path.reset()
                path.moveTo(cx - r * 0.34f, cy - r * 0.82f)
                path.lineTo(cx + r * 0.36f, cy - r * 0.82f)
                path.lineTo(cx + r * 0.46f, cy + r * 0.18f)
                path.lineTo(cx + r * 0.32f, cy + r * 0.82f)
                path.lineTo(cx - r * 0.34f, cy + r * 0.82f)
                path.lineTo(cx - r * 0.46f, cy + r * 0.10f)
                path.close()
                fillP.color = withAlpha(pal.accent, 65)
                canvas.drawPath(path, fillP)
                strokeP.strokeWidth = 2.2f * d
                strokeP.color = pal.accent
                canvas.drawPath(path, strokeP)

                // גבה בולטת, אף ארוך ושפתיים קפוצות
                strokeP.color = pal.inkPrimary
                strokeP.strokeWidth = 2.4f * d
                canvas.drawLine(cx - r * 0.36f, cy - r * 0.36f, cx + r * 0.36f, cy - r * 0.36f, strokeP)
                path.reset()
                path.moveTo(cx, cy - r * 0.36f)
                path.lineTo(cx + r * 0.14f, cy + r * 0.24f)
                path.lineTo(cx - r * 0.14f, cy + r * 0.24f)
                canvas.drawPath(path, strokeP)
                canvas.drawLine(cx - r * 0.22f, cy + r * 0.52f, cx + r * 0.22f, cy + r * 0.52f, strokeP)
            }

            EasterEggVisualType.ERROR_404 -> {
                // מסך תקלה וקטורי עם X וגליץ'
                rect.set(cx - r * 0.82f, cy - r * 0.58f, cx + r * 0.82f, cy + r * 0.42f)
                fillP.color = pal.panelBg
                canvas.drawRoundRect(rect, 8f * d, 8f * d, fillP)
                strokeP.strokeWidth = 2.2f * d
                strokeP.color = pal.accent
                canvas.drawRoundRect(rect, 8f * d, 8f * d, strokeP)
                // רגלית מסך
                canvas.drawLine(cx, cy + r * 0.42f, cx, cy + r * 0.72f, strokeP)
                canvas.drawLine(cx - r * 0.36f, cy + r * 0.72f, cx + r * 0.36f, cy + r * 0.72f, strokeP)
                centerTextP.textSize = r * 0.52f
                centerTextP.color = pal.accent
                val gx = sin(phase * 10f) * 1.6f * d
                canvas.drawText("404", cx + gx, cy + r * 0.10f, centerTextP)
            }

            EasterEggVisualType.NOKIA_3310 -> {
                // טלפון נוקיה 3310 אייקוני + מגן ניצוצות
                rect.set(cx - r * 0.46f, cy - r * 0.84f, cx + r * 0.46f, cy + r * 0.84f)
                fillP.color = Color.parseColor("#2E3B4E")
                canvas.drawRoundRect(rect, r * 0.24f, r * 0.24f, fillP)
                strokeP.strokeWidth = 2f * d
                strokeP.color = pal.accent
                canvas.drawRoundRect(rect, r * 0.24f, r * 0.24f, strokeP)

                // מסך LCD ירקרק קלאסי
                rect.set(cx - r * 0.32f, cy - r * 0.58f, cx + r * 0.32f, cy - r * 0.08f)
                fillP.color = Color.parseColor("#9CCC65")
                canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
                centerTextP.textSize = r * 0.24f
                centerTextP.color = Color.parseColor("#1B2E0D")
                canvas.drawText("3310", cx, cy - r * 0.24f, centerTextP)

                // מקשי המספרים הזעירים
                fillP.color = Color.parseColor("#CFD8DC")
                for (row in 0..2) {
                    for (col in -1..1) {
                        canvas.drawCircle(cx + col * r * 0.20f, cy + r * (0.16f + row * 0.22f), r * 0.06f, fillP)
                    }
                }
            }

            EasterEggVisualType.GIGACHAD -> {
                // כתר מלכותי + קרני שמש מסתובבות
                strokeP.strokeWidth = 1.4f * d
                strokeP.color = withAlpha(pal.accent, 115)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0 + phase * 18.0)
                    canvas.drawLine(
                        cx + (cos(a) * r * 0.72f).toFloat(), cy + (sin(a) * r * 0.72f).toFloat(),
                        cx + (cos(a) * r * 0.96f).toFloat(), cy + (sin(a) * r * 0.96f).toFloat(),
                        strokeP
                    )
                }
                path.reset()
                path.moveTo(cx - r * 0.60f, cy + r * 0.42f)
                path.lineTo(cx - r * 0.70f, cy - r * 0.28f)
                path.lineTo(cx - r * 0.28f, cy + r * 0.06f)
                path.lineTo(cx, cy - r * 0.52f)
                path.lineTo(cx + r * 0.28f, cy + r * 0.06f)
                path.lineTo(cx + r * 0.70f, cy - r * 0.28f)
                path.lineTo(cx + r * 0.60f, cy + r * 0.42f)
                path.close()
                fillP.color = pal.accent
                canvas.drawPath(path, fillP)
                fillP.color = pal.secondary
                canvas.drawCircle(cx, cy + r * 0.16f, r * 0.12f, fillP)
            }

            EasterEggVisualType.SPELL_8008135 -> {
                // צג 7-סגמנטים שמסתובב 180° כמו מחשבון בית ספר
                val rotDeg = (sin(phase * 1.6f) * 0.5f + 0.5f) * 180f
                canvas.save()
                canvas.rotate(rotDeg, cx, cy)
                rect.set(cx - r * 0.84f, cy - r * 0.44f, cx + r * 0.84f, cy + r * 0.44f)
                fillP.color = pal.panelBg
                canvas.drawRoundRect(rect, 9f * d, 9f * d, fillP)
                strokeP.strokeWidth = 2f * d
                strokeP.color = pal.accent
                canvas.drawRoundRect(rect, 9f * d, 9f * d, strokeP)
                centerTextP.textSize = r * 0.36f
                centerTextP.color = pal.accent
                canvas.drawText("5318008", cx, cy + r * 0.14f, centerTextP)
                canvas.restore()
            }

            EasterEggVisualType.SKILL_ISSUE -> {
                // גולגולת גאומטרית וקטורית מסוגננת
                fillP.color = withAlpha(pal.accent, 52)
                canvas.drawCircle(cx, cy - r * 0.14f, r * 0.62f, fillP)
                rect.set(cx - r * 0.34f, cy + r * 0.26f, cx + r * 0.34f, cy + r * 0.68f)
                canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)

                strokeP.strokeWidth = 2.2f * d
                strokeP.color = pal.accent
                canvas.drawCircle(cx, cy - r * 0.14f, r * 0.62f, strokeP)
                canvas.drawRoundRect(rect, 4f * d, 4f * d, strokeP)

                // עיני X
                val er = r * 0.14f
                for (side in intArrayOf(-1, 1)) {
                    val ex = cx + side * r * 0.25f
                    val ey = cy - r * 0.16f
                    canvas.drawLine(ex - er, ey - er, ex + er, ey + er, strokeP)
                    canvas.drawLine(ex + er, ey - er, ex - er, ey + er, strokeP)
                }
                // שיניים
                canvas.drawLine(cx - r * 0.11f, cy + r * 0.34f, cx - r * 0.11f, cy + r * 0.66f, strokeP)
                canvas.drawLine(cx + r * 0.11f, cy + r * 0.34f, cx + r * 0.11f, cy + r * 0.66f, strokeP)
            }

            EasterEggVisualType.HACKER_NASA -> {
                // צלחת לוויין + גלי רדאר מתפשטים
                strokeP.strokeWidth = 2.2f * d
                strokeP.color = pal.accent
                rect.set(cx - r * 0.72f, cy - r * 0.52f, cx + r * 0.42f, cy + r * 0.62f)
                canvas.drawArc(rect, 110f, 160f, false, strokeP)
                canvas.drawLine(cx - r * 0.15f, cy + r * 0.05f, cx + r * 0.26f, cy - r * 0.36f, strokeP)
                fillP.color = pal.secondary
                canvas.drawCircle(cx + r * 0.28f, cy - r * 0.38f, r * 0.10f, fillP)

                // גלי שידור
                for (k in 1..2) {
                    val waveR = r * (0.28f * k + ((phase * 0.4f) % 0.28f))
                    strokeP.strokeWidth = 1.6f * d
                    strokeP.color = withAlpha(pal.accent, (220 - k * 75).coerceAtLeast(50))
                    rect.set(cx + r * 0.28f - waveR, cy - r * 0.38f - waveR, cx + r * 0.28f + waveR, cy - r * 0.38f + waveR)
                    canvas.drawArc(rect, -70f, 95f, false, strokeP)
                }
            }

            EasterEggVisualType.CHEER_PARTY -> {
                // גביע וקטורי חגיגי + כוכב
                path.reset()
                path.moveTo(cx - r * 0.56f, cy - r * 0.64f)
                path.lineTo(cx + r * 0.56f, cy - r * 0.64f)
                path.quadTo(cx + r * 0.52f, cy + r * 0.14f, cx, cy + r * 0.24f)
                path.quadTo(cx - r * 0.52f, cy + r * 0.14f, cx - r * 0.56f, cy - r * 0.64f)
                path.close()
                fillP.color = pal.accent
                canvas.drawPath(path, fillP)

                strokeP.strokeWidth = 2.4f * d
                strokeP.color = pal.accent
                canvas.drawLine(cx, cy + r * 0.24f, cx, cy + r * 0.62f, strokeP)
                canvas.drawLine(cx - r * 0.38f, cy + r * 0.66f, cx + r * 0.38f, cy + r * 0.66f, strokeP)
                drawStarShape(canvas, cx, cy - r * 0.26f, r * 0.24f, pal.cardTop)
            }

            EasterEggVisualType.PI_314159 -> {
                // מעגל פאי עם רדיוס מסתובב + סימן π חד
                strokeP.strokeWidth = 2.4f * d
                strokeP.color = withAlpha(pal.accent, 85)
                canvas.drawCircle(cx, cy, r * 0.80f, strokeP)
                rect.set(cx - r * 0.80f, cy - r * 0.80f, cx + r * 0.80f, cy + r * 0.80f)
                strokeP.color = pal.accent
                canvas.drawArc(rect, -90f, ((phase * 48f) % 360f), false, strokeP)

                centerTextP.textSize = r * 0.95f
                centerTextP.color = pal.inkPrimary
                canvas.drawText("π", cx, cy + r * 0.30f, centerTextP)
            }

            EasterEggVisualType.APOLLO_1969 -> {
                // ירח מכתשי + דגל על הפסגה
                fillP.color = withAlpha(pal.secondary, 85)
                canvas.drawCircle(cx, cy + r * 0.26f, r * 0.58f, fillP)
                strokeP.strokeWidth = 1.8f * d
                strokeP.color = pal.accent
                canvas.drawCircle(cx, cy + r * 0.26f, r * 0.58f, strokeP)
                // מכתשים
                fillP.color = withAlpha(pal.inkPrimary, 45)
                canvas.drawCircle(cx - r * 0.20f, cy + r * 0.22f, r * 0.11f, fillP)
                canvas.drawCircle(cx + r * 0.18f, cy + r * 0.42f, r * 0.08f, fillP)
                // תורן ודגל מתנפנף
                strokeP.strokeWidth = 2f * d
                strokeP.color = pal.inkPrimary
                canvas.drawLine(cx, cy - r * 0.30f, cx, cy - r * 0.86f, strokeP)
                fillP.color = pal.accent
                val wave = sin(phase * 3f) * 2.5f * d
                path.reset()
                path.moveTo(cx, cy - r * 0.84f)
                path.quadTo(cx + r * 0.28f, cy - r * 0.92f + wave, cx + r * 0.54f, cy - r * 0.80f)
                path.lineTo(cx + r * 0.54f, cy - r * 0.54f)
                path.quadTo(cx + r * 0.28f, cy - r * 0.64f + wave, cx, cy - r * 0.56f)
                path.close()
                canvas.drawPath(path, fillP)
            }

            EasterEggVisualType.AGENT_007 -> {
                // צמצם קנה אקדח 007 + פפיון טוקסידו חד
                strokeP.strokeWidth = 2.2f * d
                strokeP.color = pal.accent
                canvas.drawCircle(cx, cy, r * 0.84f, strokeP)
                strokeP.strokeWidth = 1.3f * d
                for (i in 0 until 6) {
                    val a = Math.toRadians(i * 60.0 + phase * 16.0)
                    canvas.drawLine(
                        cx + (cos(a) * r * 0.52f).toFloat(), cy + (sin(a) * r * 0.52f).toFloat(),
                        cx + (cos(a) * r * 0.84f).toFloat(), cy + (sin(a) * r * 0.84f).toFloat(),
                        strokeP
                    )
                }
                // פפיון טוקסידו במרכז
                fillP.color = pal.inkPrimary
                path.reset()
                path.moveTo(cx, cy)
                path.lineTo(cx - r * 0.44f, cy - r * 0.24f)
                path.lineTo(cx - r * 0.44f, cy + r * 0.24f)
                path.close()
                canvas.drawPath(path, fillP)
                path.reset()
                path.moveTo(cx, cy)
                path.lineTo(cx + r * 0.44f, cy - r * 0.24f)
                path.lineTo(cx + r * 0.44f, cy + r * 0.24f)
                path.close()
                canvas.drawPath(path, fillP)
                fillP.color = pal.accent
                canvas.drawCircle(cx, cy, r * 0.11f, fillP)
            }

            EasterEggVisualType.FUTURE_2026 -> {
                // שעון חול עתידני עם חול זורם
                strokeP.strokeWidth = 2.2f * d
                strokeP.color = pal.accent
                canvas.drawLine(cx - r * 0.52f, cy - r * 0.72f, cx + r * 0.52f, cy - r * 0.72f, strokeP)
                canvas.drawLine(cx - r * 0.52f, cy + r * 0.72f, cx + r * 0.52f, cy + r * 0.72f, strokeP)
                path.reset()
                path.moveTo(cx - r * 0.44f, cy - r * 0.70f)
                path.lineTo(cx + r * 0.44f, cy - r * 0.70f)
                path.lineTo(cx, cy)
                path.close()
                fillP.color = withAlpha(pal.accent, 65)
                canvas.drawPath(path, fillP)
                canvas.drawPath(path, strokeP)

                path.reset()
                path.moveTo(cx - r * 0.44f, cy + r * 0.70f)
                path.lineTo(cx + r * 0.44f, cy + r * 0.70f)
                path.lineTo(cx, cy)
                path.close()
                fillP.color = withAlpha(pal.secondary, 85)
                canvas.drawPath(path, fillP)
                canvas.drawPath(path, strokeP)

                // גרגיר נופל במרכז
                val sandY = cy + ((phase * 14f * d) % (r * 0.58f))
                fillP.color = pal.secondary
                canvas.drawCircle(cx, sandY, 2.2f * d, fillP)
            }

            EasterEggVisualType.EULER_271828 -> {
                // עקומה מעריכית e^x + נקודה נעה זוהרת
                strokeP.strokeWidth = 1.4f * d
                strokeP.color = withAlpha(pal.inkSecondary, 110)
                canvas.drawLine(cx - r * 0.76f, cy + r * 0.56f, cx + r * 0.76f, cy + r * 0.56f, strokeP)
                canvas.drawLine(cx - r * 0.58f, cy + r * 0.72f, cx - r * 0.58f, cy - r * 0.72f, strokeP)

                path.reset()
                val steps = 20
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val px = cx - r * 0.62f + t * (r * 1.32f)
                    val py = cy + r * 0.52f - (exp((t * 2.2).toDouble()).toFloat() / exp(2.2).toFloat()) * (r * 1.18f)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                strokeP.strokeWidth = 2.6f * d
                strokeP.color = pal.accent
                canvas.drawPath(path, strokeP)

                val pt = (sin(phase * 1.5f) * 0.5f + 0.5f)
                val dotX = cx - r * 0.62f + pt * (r * 1.32f)
                val dotY = cy + r * 0.52f - (exp((pt * 2.2).toDouble()).toFloat() / exp(2.2).toFloat()) * (r * 1.18f)
                fillP.color = pal.secondary
                canvas.drawCircle(dotX, dotY, 4.2f * d, fillP)
            }
        }
        strokeP.clearShadowLayer()
    }

    private fun drawStarShape(canvas: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        path.reset()
        for (i in 0 until 10) {
            val a = Math.toRadians(-90.0 + i * 36.0)
            val rad = if (i % 2 == 0) r else r * 0.45f
            val px = cx + (cos(a) * rad).toFloat()
            val py = cy + (sin(a) * rad).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        fillP.color = col
        canvas.drawPath(path, fillP)
    }
}

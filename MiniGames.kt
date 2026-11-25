package com.example.calculator

import android.animation.ValueAnimator
import android.graphics.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*
import kotlin.random.Random

object MiniGameEngine {

    internal data class GameThemePalette(
        val cardBgTop: Int,
        val cardBgBottom: Int,
        val cardBorder: Int,
        val arenaBg: Int,
        val arenaGrid: Int,
        val inkPrimary: Int,
        val inkSecondary: Int,
        val playerColor: Int,
        val enemyColor: Int,
        val bonusColor: Int,
        val controlAccent: Int,
        val laserColor: Int,
        val shieldColor: Int,
        val glowColor: Int,
        val isMinimalist: Boolean,
        val isAurora: Boolean,
        val isGlass: Boolean
    )

    internal data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        val color: Int,
        var life: Float = 1f,
        val decay: Float = 0.032f,
        val shape: Int = 0 // 0=עיגול, 1=קונפטי, 2=ניצוץ
    )

    internal data class FloatingCallout(
        var x: Float,
        var y: Float,
        val text: String,
        val color: Int,
        var life: Float = 1f
    )

    var isActive = false
    internal var currentGame = ""
    private val handler = Handler(Looper.getMainLooper())
    private var gameLoop: Runnable? = null
    private var cooldownTicker: Runnable? = null
    private var roundResetRunnable: Runnable? = null
    private var gameOverDelayRunnable: Runnable? = null
    private var gameEnding = false

    // מארח הקנבס הווקטורי בתוך displayCard
    private var vectorStageView: MiniGameVectorStageView? = null
    private var attachedDisplayCard: LinearLayout? = null
    private var savedDisplayPadding: IntArray? = null

    // אודיו מופרד (לולאת מנוע לעומת אפקטים חד-פעמיים)
    private var engineLoopPlayer: MediaPlayer? = null
    private var currentEngineStage = 0
    private var sfxPlayer: MediaPlayer? = null
    private val activeSynthThreads = AtomicInteger(0)
    @Volatile private var synthEngineRunning = false

    // --- 1. איקס-עיגול Arena Pro (1337) ---
    internal val tttBoard = Array(9) { "" }
    internal val tttPlacedAt = LongArray(9) { 0L }
    internal var tttStatusText = ""
    internal var tttWinningLine: IntArray? = null
    internal var tttDifficulty = 2 // 0=קל, 1=בינוני, 2=בלתי מנוצח
    internal var tttPvpMode = false
    internal var tttCurrentTurn = "×"
    internal var tttWinsX = 0
    internal var tttWinsO = 0
    internal var tttTies = 0
    internal var tttStreak = 0
    internal var tttLastMoveIdx = -1
    internal var tttHintIdx = -1
    internal var tttHintTime = 0L
    private var tttRoundLocked = false

    // --- 2. פולשי החשבון Galactic Pro (8080) ---
    internal data class InvaderEntity(
        val symbol: String,
        var hp: Int = 1,
        val maxHp: Int = 1,
        val isBoss: Boolean = false
    )
    internal var invaderScore = 0
    internal var invaderBest = 0
    internal var invaderLives = 3
    internal var invaderWave = 1
    internal var invaderCombo = 0
    internal var invaderEmpCharge = 0 // 0..6
    internal var invaderEmpWaveTime = 0L
    internal var invaderTrack = Array<InvaderEntity?>(6) { null }
    internal var invaderLaserCol = -1
    internal var invaderLaserTime = 0L
    internal var invaderBossSeq = mutableListOf<String>()
    private val invaderSymbols = listOf("+", "−", "×", "÷", "sin", "cos", "tan", "√")

    // --- 3. מירוץ מכוניות Turbo Street Racer Pro (5555) ---
    internal var carLane = 1 // 0=עליון (8), 1=אמצעי, 2=תחתון (2)
    internal var racerScore = 0
    internal var racerBest = 0
    internal var racerCombo = 0
    internal var racerPaused = false
    internal var racerHasShield = false
    internal var racerMagnetUntil = 0L
    internal var racerSlowMoUntil = 0L
    internal var racerSlowMoCharge = 100 // 0..100
    internal var racerNitroCharge = 100  // 0..100
    internal var racerNitroBurstUntil = 0L
    internal val racerTracks = Array(3) { MutableList(6) { "." } }
    internal var lastShotTime = 0L
    internal var racerLaserLane = -1
    internal var racerLaserEndCol = 0
    internal var racerLaserTime = 0L
    internal var racerShieldBreakTime = 0L
    internal var crashAnimStartTime = 0L
    internal var crashLane = 1
    private var lastCooldownToastTime = 0L
    internal const val SHOT_COOLDOWN_MS = 3000L
    private var pendingGuardedStarLane = -1

    // --- 4. נטרול פצצה Pro (1010) ---
    internal var bombTimeLeft = 18
    internal var bombScore = 0
    internal var bombBest = 0
    internal var bombStreak = 0
    internal var bombLevel = 1
    internal var bombEqDisplay = "2 [?] 3 = 6"
    internal var bombFreezeUntil = 0L
    internal var bombFreezeCharges = 1
    internal var bombCutCharges = 1
    internal val bombEliminatedOps = mutableSetOf<String>()
    internal var bombShakeTime = 0L
    private var bombValidator: (String) -> Boolean = { false }

    // --- 5. מסיבת חיות מוזיקלית Pro (7777) ---
    var isAnimalParty = false
    internal var animalRhythmMode = false // false=DJ חופשי, true=אתגר תפוס את החיה
    internal var animalScore = 0
    internal var animalBest = 0
    internal var animalCombo = 0
    internal var animalTimeLeft = 20
    internal var animalTotalTime = 20
    internal var animalActiveIdx = 0
    internal var animalTargetIdx = 0
    internal var animalBounceTime = 0L
    internal var animalDiscoUntil = 0L
    internal val animalTargetKeys = listOf("1", "2", "3", "4", "5")
    private val pentatonicScale = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 587.33, 659.25, 783.99, 880.00)
    private val savedKeyLabels = mutableMapOf<String, CharSequence>()
    private val animalEmojis = listOf(
        "🐶", "🐱", "🦊", "🐼", "🐸", "🦁", "🐯", "🐰",
        "🐨", "🐻", "🐵", "🐮", "🐷", "🐔", "🐹", "🐭"
    )

    // --- שיאים אישיים ---
    private fun loadHighScore(activity: MainActivity, key: String): Int =
        activity.prefs?.getInt("minigame_best_$key", 0) ?: 0

    private fun saveHighScoreIfBeaten(activity: MainActivity, key: String, score: Int): Boolean {
        val prev = loadHighScore(activity, key)
        if (score > prev) {
            activity.prefs?.edit()?.putInt("minigame_best_$key", score)?.apply()
            return true
        }
        return false
    }

    // =========================================================================
    // פלטת צבעים וקטורית מותאמת לכל 4 ערכות הנושא
    // =========================================================================
    internal fun resolvePalette(activity: MainActivity): GameThemePalette {
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode

        return when {
            isMin -> GameThemePalette(
                cardBgTop = MinimalistTheme.CARD_WHITE,
                cardBgBottom = MinimalistTheme.BG_CREAM,
                cardBorder = MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#DCD5CA")),
                arenaBg = Color.parseColor("#F1ECE4"),
                arenaGrid = Color.argb(28, 44, 44, 44),
                inkPrimary = MinimalistTheme.TEXT_CHARCOAL,
                inkSecondary = MinimalistTheme.TEXT_MUTED_GREY,
                playerColor = Color.parseColor("#2E7D32"),
                enemyColor = Color.parseColor("#C62828"),
                bonusColor = Color.parseColor("#C67C00"),
                controlAccent = Color.parseColor("#1976D2"),
                laserColor = Color.parseColor("#00897B"),
                shieldColor = Color.parseColor("#0288D1"),
                glowColor = Color.TRANSPARENT,
                isMinimalist = true,
                isAurora = false,
                isGlass = false
            )
            isAur -> GameThemePalette(
                cardBgTop = AuroraBorealisTheme.EMERALD_NIGHT,
                cardBgBottom = Color.parseColor("#061622"),
                cardBorder = Color.argb(190, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY)),
                arenaBg = Color.argb(185, 5, 16, 25),
                arenaGrid = Color.argb(38, 0, 245, 212),
                inkPrimary = Color.WHITE,
                inkSecondary = AuroraBorealisTheme.EXPRESSION_CYAN_GREY,
                playerColor = Color.parseColor("#00F5D4"),
                enemyColor = Color.parseColor("#FF4D8D"),
                bonusColor = Color.parseColor("#FFD166"),
                controlAccent = Color.parseColor("#38BDF8"),
                laserColor = Color.parseColor("#00F5D4"),
                shieldColor = Color.parseColor("#60A5FA"),
                glowColor = AuroraBorealisTheme.AURORA_PRIMARY,
                isMinimalist = false,
                isAurora = true,
                isGlass = false
            )
            isGlass -> GameThemePalette(
                cardBgTop = Color.argb(85, 255, 255, 255),
                cardBgBottom = Color.argb(36, 255, 255, 255),
                cardBorder = Color.argb(145, 255, 255, 255),
                arenaBg = Color.argb(66, 12, 20, 36),
                arenaGrid = Color.argb(38, 255, 255, 255),
                inkPrimary = Color.WHITE,
                inkSecondary = Color.parseColor("#CBD5E1"),
                playerColor = Color.parseColor("#34D399"),
                enemyColor = Color.parseColor("#FB7185"),
                bonusColor = Color.parseColor("#FBBF24"),
                controlAccent = Color.parseColor("#7DD3FC"),
                laserColor = Color.parseColor("#38BDF8"),
                shieldColor = Color.parseColor("#60A5FA"),
                glowColor = Color.argb(120, 255, 255, 255),
                isMinimalist = false,
                isAurora = false,
                isGlass = true
            )
            else -> {
                val baseBg = Utils.lightenColor(activity.bgColor, 0.06f)
                val accent = MinimalistTheme.ensureReadableTextOnBg(activity.primaryColor, baseBg, 2.2)
                GameThemePalette(
                    cardBgTop = Utils.lightenColor(activity.bgColor, 0.10f),
                    cardBgBottom = Utils.lightenColor(activity.bgColor, 0.04f),
                    cardBorder = Color.argb(145, Color.red(accent), Color.green(accent), Color.blue(accent)),
                    arenaBg = activity.bgColor,
                    arenaGrid = Color.argb(34, Color.red(accent), Color.green(accent), Color.blue(accent)),
                    inkPrimary = Color.WHITE,
                    inkSecondary = Color.parseColor("#94A3B8"),
                    playerColor = Color.parseColor("#4ADE80"),
                    enemyColor = Color.parseColor("#F87171"),
                    bonusColor = Color.parseColor("#FACC15"),
                    controlAccent = accent,
                    laserColor = accent,
                    shieldColor = Color.parseColor("#38BDF8"),
                    glowColor = Color.TRANSPARENT,
                    isMinimalist = false,
                    isAurora = false,
                    isGlass = false
                )
            }
        }
    }

    private fun isActivityValid(activity: MainActivity): Boolean =
        !activity.isFinishing && !activity.isDestroyed

    private fun spawnBurst(normX: Float, normY: Float, color: Int, count: Int = 14, shape: Int = 0) {
        vectorStageView?.spawnBurstNormalized(normX, normY, color, count, shape)
    }

    private fun spawnCallout(normX: Float, normY: Float, text: String, color: Int) {
        vectorStageView?.spawnCalloutNormalized(normX, normY, text, color)
    }

    // =========================================================================
    // הטמעה והסרה נקייה של מסך המשחק הווקטורי בתוך displayCard
    // =========================================================================
    private fun attachVectorStage(activity: MainActivity) {
        detachVectorStage(activity)
        val displayCard = activity.currentView?.parent as? LinearLayout ?: return
        attachedDisplayCard = displayCard
        savedDisplayPadding = intArrayOf(
            displayCard.paddingLeft,
            displayCard.paddingTop,
            displayCard.paddingRight,
            displayCard.paddingBottom
        )

        activity.memoryIndicator?.visibility = View.GONE
        activity.expressionView?.visibility = View.GONE
        activity.currentView?.visibility = View.GONE

        val pad = activity.dp(4f)
        displayCard.setPadding(pad, pad, pad, pad)

        val stage = MiniGameVectorStageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        vectorStageView = stage
        displayCard.addView(stage)
    }

    private fun detachVectorStage(activity: MainActivity) {
        val stage = vectorStageView
        vectorStageView = null
        if (stage != null) {
            (stage.parent as? ViewGroup)?.removeView(stage)
        }
        val card = attachedDisplayCard
        val pads = savedDisplayPadding
        attachedDisplayCard = null
        savedDisplayPadding = null
        if (card != null && pads != null && pads.size == 4) {
            card.setPadding(pads[0], pads[1], pads[2], pads[3])
        }
        activity.memoryIndicator?.visibility = View.VISIBLE
        activity.expressionView?.visibility = View.VISIBLE
        activity.currentView?.visibility = View.VISIBLE
    }

    private fun invalidateStage() {
        vectorStageView?.invalidate()
    }

    private fun styleKeyForGame(
        activity: MainActivity,
        symbol: String,
        desiredColor: Int,
        labelOverride: CharSequence? = null,
        glow: Boolean = false
    ) {
        val btn = activity.keypadButtons[symbol] ?: return
        if (labelOverride != null) btn.text = labelOverride
        val keyBg = MinimalistTheme.resolveKeypadButtonBgColor(activity, symbol)
        val safeCol = MinimalistTheme.ensureReadableTextOnBg(desiredColor, keyBg, 2.4)
        btn.setTextColor(safeCol)
        if (glow && activity.isAuroraMode) {
            btn.setShadowLayer(
                8f, 0f, 0f,
                Color.argb(170, Color.red(safeCol), Color.green(safeCol), Color.blue(safeCol))
            )
        } else if (!activity.isIosGlassMode) {
            btn.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        }
    }

    private fun saveKeys(activity: MainActivity, keys: List<String>) {
        for (k in keys) {
            if (!savedKeyLabels.containsKey(k)) {
                activity.keypadButtons[k]?.let { savedKeyLabels[k] = it.text }
            }
        }
    }

    private fun restoreAllKeyColorsAndLabels(activity: MainActivity) {
        for ((k, text) in savedKeyLabels) {
            activity.keypadButtons[k]?.text = text
        }
        savedKeyLabels.clear()
        for ((sym, btn) in activity.keypadButtons) {
            val bg = MinimalistTheme.resolveKeypadButtonBgColor(activity, sym)
            btn.setTextColor(MinimalistTheme.resolveKeypadSymbolTextColor(activity, sym, bg))
            if (!activity.isIosGlassMode && !activity.isAuroraMode) {
                btn.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }
    }

    private fun prepareNewGame(activity: MainActivity, gameId: String) {
        stopPreviousLoop()
        stopRacerAudio()
        releaseSfxPlayerSafely()
        restoreAllKeyColorsAndLabels(activity)
        isAnimalParty = false
        tttRoundLocked = false
        tttWinningLine = null
        tttHintIdx = -1
        gameEnding = false
        crashAnimStartTime = 0L
        racerLaserTime = 0L
        invaderLaserTime = 0L
        invaderEmpWaveTime = 0L
        bombShakeTime = 0L
        isActive = true
        currentGame = gameId
        attachVectorStage(activity)
    }

    fun stopGame(activity: MainActivity, message: String = "יצאת מהמשחק 🎮") {
        stopGameInternal(activity, message, stopSfx = true)
    }

    private fun stopGameInternal(activity: MainActivity, message: String, stopSfx: Boolean) {
        val wasActive = isActive
        isActive = false
        gameEnding = false
        currentGame = ""
        isAnimalParty = false
        tttRoundLocked = false
        stopPreviousLoop()
        stopRacerAudio()
        if (stopSfx) releaseSfxPlayerSafely()

        if (!isActivityValid(activity)) return
        detachVectorStage(activity)
        restoreAllKeyColorsAndLabels(activity)
        activity.currentInput = ""
        activity.justCalculated = false
        activity.applyTheme()
        if (wasActive && message.isNotBlank()) {
            val cleanMsg = if (activity.isCleanGlassActive) {
                IosGlassTheme.cleanCategoryTitle(message, true)
            } else message
            Toast.makeText(activity, cleanMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================================
    // מערכת אודיו חכמה (זיהוי אוטומטי ב-res/raw + סינתיסייזר עשיר)
    // =========================================================================
    private fun findRawResId(
        activity: MainActivity,
        exactCandidates: List<String>,
        keywords: List<String> = emptyList(),
        exclude: List<String> = emptyList()
    ): Int {
        for (name in exactCandidates) {
            val id = activity.resources.getIdentifier(name, "raw", activity.packageName)
            if (id != 0) return id
        }
        if (keywords.isEmpty()) return 0
        try {
            val rawClass = Class.forName("${activity.packageName}.R\$raw")
            for (field in rawClass.fields) {
                val fname = field.name.lowercase(Locale.US)
                if (exclude.any { fname.contains(it) }) continue
                if (keywords.any { fname.contains(it) }) {
                    val resId = field.getInt(null)
                    if (resId != 0) return resId
                }
            }
        } catch (_: Throwable) {}
        return 0
    }

    private fun releaseSfxPlayerSafely() {
        val p = sfxPlayer
        sfxPlayer = null
        if (p != null) {
            try { p.setOnCompletionListener(null) } catch (_: Throwable) {}
            try { if (p.isPlaying) p.stop() } catch (_: Throwable) {}
            try { p.release() } catch (_: Throwable) {}
        }
    }

    private fun playRawSfx(activity: MainActivity, resId: Int, volume: Float = 1.0f): Boolean {
        if (resId == 0 || !isActivityValid(activity)) return false
        return try {
            releaseSfxPlayerSafely()
            val mp = MediaPlayer.create(activity.applicationContext, resId) ?: return false
            sfxPlayer = mp
            mp.setVolume(volume, volume)
            mp.setOnCompletionListener { player ->
                if (sfxPlayer === player) sfxPlayer = null
                try { player.release() } catch (_: Throwable) {}
            }
            mp.setOnErrorListener { player, _, _ ->
                if (sfxPlayer === player) sfxPlayer = null
                try { player.release() } catch (_: Throwable) {}
                true
            }
            mp.start()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun playStaticPcmAsync(sampleRate: Int, buffer: ShortArray, durationMs: Long) {
        if (activeSynthThreads.get() >= 3) return
        activeSynthThreads.incrementAndGet()
        Thread {
            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)
                track = AudioTrack.Builder()
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
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs + 30L)
            } catch (_: Throwable) {
            } finally {
                try { track?.stop() } catch (_: Throwable) {}
                try { track?.release() } catch (_: Throwable) {}
                activeSynthThreads.decrementAndGet()
            }
        }.start()
    }

    private fun playSynthMelody(activity: MainActivity, notes: List<Pair<Double, Int>>, gain: Double = 0.50) {
        if (!activity.useSound || notes.isEmpty()) return
        val sampleRate = 22050
        val totalSamples = notes.sumOf { (it.second * sampleRate) / 1000 }
        val buffer = ShortArray(totalSamples)
        var offset = 0
        for ((freq, dur) in notes) {
            val count = (dur * sampleRate) / 1000
            for (i in 0 until count) {
                val env = sin((i.toDouble() / count) * PI)
                val wave = sin(2.0 * PI * freq * i / sampleRate) * 0.78 + sin(4.0 * PI * freq * i / sampleRate) * 0.22
                buffer[offset + i] = (wave * env * Short.MAX_VALUE * gain).toInt().toShort()
            }
            offset += count
        }
        playStaticPcmAsync(sampleRate, buffer, notes.sumOf { it.second }.toLong())
    }

    fun playTttResultSound(activity: MainActivity, isWin: Boolean) {
        if (!activity.useSound) return
        val baseExcludes = listOf("sound1", "sound2", "sound3", "ac_sound", "loop", "shot", "shoot")
        val resId = if (isWin) {
            findRawResId(
                activity,
                listOf("ttt_win", "win", "game_win", "tuomas_data_game_win_471625", "victory", "cheer"),
                keywords = listOf("win", "victory", "cheer", "tada"),
                exclude = baseExcludes + listOf("coin", "crash", "lose")
            )
        } else {
            findRawResId(
                activity,
                listOf("ttt_lose", "lose", "game_over", "universfield_game_over_deep_male_voice_clip_352695", "gameover", "fail"),
                keywords = listOf("gameover", "game_over", "lose", "fail"),
                exclude = baseExcludes + listOf("win")
            )
        }
        if (playRawSfx(activity, resId)) return
        playSynthMelody(
            activity,
            if (isWin) listOf(523.25 to 110, 659.25 to 110, 783.99 to 130, 1046.50 to 250)
            else listOf(311.13 to 160, 277.18 to 160, 246.94 to 160, 196.00 to 300)
        )
    }

    private fun startRacerEngineLoop(activity: MainActivity) {
        stopRacerAudio()
        if (!activity.useSound) return

        val stageResIds = mutableListOf<Int>()
        for (i in 0..5) {
            val id = findRawResId(activity, listOf("loop$i", "loop_$i", "car_loop$i", "engine$i"))
            if (id != 0) stageResIds.add(id)
        }
        if (stageResIds.isNotEmpty()) {
            currentEngineStage = 0
            playEngineStage(activity, stageResIds)
            return
        }

        val singleResId = findRawResId(
            activity,
            listOf("car_loop", "engine", "race_car", "car_engine", "car", "motor"),
            keywords = listOf("loop", "engine", "motor"),
            exclude = listOf("crash", "shot", "shoot", "win", "lose", "ac_sound", "sound1", "sound2", "sound3")
        )
        if (singleResId != 0) {
            try {
                engineLoopPlayer = MediaPlayer.create(activity.applicationContext, singleResId)?.apply {
                    isLooping = true
                    setVolume(0.55f, 0.55f)
                    start()
                }
                return
            } catch (_: Throwable) {}
        }

        synthEngineRunning = true
        Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val chunkSamples = sampleRate / 10
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(chunkSamples * 2)
                val buffer = ShortArray(chunkSamples)
                var phase = 0.0
                var tick = 0
                track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(maxOf(chunkSamples * 2, minBuf))
                    .setTransferMode(AudioTrack.MODE_STREAM).build()
                track.play()
                while (synthEngineRunning && isActive && !gameEnding && currentGame == "RACER" && isActivityValid(activity)) {
                    if (racerPaused) {
                        Thread.sleep(80L)
                        continue
                    }
                    val gearStage = (racerScore / 15).coerceIn(0, 5)
                    val slowFactor = if (System.currentTimeMillis() < racerSlowMoUntil) 0.65 else 1.0
                    val baseFreq = (65.0 + gearStage * 20.0 + (if (tick % 4 < 2) 4.0 else -3.0)) * slowFactor
                    tick++
                    for (i in 0 until chunkSamples) {
                        phase += 2.0 * PI * baseFreq / sampleRate
                        val rumble = sin(phase) * 0.55 + sin(phase * 2.0) * 0.30 + sin(phase * 3.0) * 0.15
                        buffer[i] = (rumble * Short.MAX_VALUE * 0.17).toInt().toShort()
                    }
                    track.write(buffer, 0, chunkSamples)
                }
            } catch (_: Throwable) {
            } finally {
                try { track?.stop() } catch (_: Throwable) {}
                try { track?.release() } catch (_: Throwable) {}
            }
        }.start()
    }

    private fun playEngineStage(activity: MainActivity, stageResIds: List<Int>) {
        if (!isActive || gameEnding || currentGame != "RACER" || !activity.useSound || !isActivityValid(activity)) return
        val resId = stageResIds[currentEngineStage.coerceIn(0, stageResIds.lastIndex)]
        try {
            val prev = engineLoopPlayer
            engineLoopPlayer = null
            try { prev?.setOnCompletionListener(null); prev?.stop(); prev?.release() } catch (_: Throwable) {}

            engineLoopPlayer = MediaPlayer.create(activity.applicationContext, resId)?.apply {
                setVolume(0.58f, 0.58f)
                if (currentEngineStage >= stageResIds.lastIndex) {
                    isLooping = true
                } else {
                    isLooping = false
                    setOnCompletionListener {
                        if (isActive && !gameEnding && currentGame == "RACER" && isActivityValid(activity)) {
                            currentEngineStage = (currentEngineStage + 1).coerceAtMost(stageResIds.lastIndex)
                            playEngineStage(activity, stageResIds)
                        }
                    }
                }
                start()
            }
        } catch (_: Throwable) {}
    }

    private fun stopRacerAudio() {
        synthEngineRunning = false
        val p = engineLoopPlayer
        engineLoopPlayer = null
        if (p != null) {
            try { p.setOnCompletionListener(null) } catch (_: Throwable) {}
            try { if (p.isPlaying) p.stop() } catch (_: Throwable) {}
            try { p.release() } catch (_: Throwable) {}
        }
    }

    private fun playRacerSoundEffect(activity: MainActivity, type: String) {
        if (!activity.useSound) return
        val commonExcludes = listOf("sound1", "sound2", "sound3", "ac_sound", "loop")
        val resId = when (type) {
            "SHOOT" -> findRawResId(
                activity,
                listOf("shoot", "shot", "universfield_arcade_shot_152053", "arcade_shot", "laser", "fire", "gun"),
                keywords = listOf("shot", "shoot", "laser", "gun", "fire"),
                exclude = commonExcludes + listOf("crash", "win", "lose", "coin")
            )
            "CRASH" -> findRawResId(
                activity,
                listOf("crash", "car_crash", "carcrash", "collision", "explosion", "boom", "hit", "smash", "impact"),
                keywords = listOf("crash", "collis", "explos", "boom", "smash", "wreck", "impact", "accident", "debris", "hit"),
                exclude = commonExcludes + listOf("shot", "shoot", "coin", "star", "win")
            )
            "STAR" -> findRawResId(
                activity,
                listOf("star", "coin", "mixkit_winning_a_coin_video_game_2069", "winning_a_coin", "bonus", "powerup", "pickup"),
                keywords = listOf("coin", "star", "bonus", "pickup", "powerup"),
                exclude = commonExcludes + listOf("crash", "shot", "shoot", "lose")
            )
            else -> 0
        }
        if (playRawSfx(activity, resId, volume = 1.0f)) return

        when (type) {
            "POWERUP" -> {
                playSynthMelody(activity, listOf(587.33 to 75, 880.00 to 85, 1174.66 to 140), 0.48)
                return
            }
            "EMP" -> {
                playSynthMelody(activity, listOf(980.0 to 70, 620.0 to 90, 310.0 to 160, 160.0 to 210), 0.58)
                return
            }
            "FREEZE" -> {
                playSynthMelody(activity, listOf(1318.5 to 70, 1568.0 to 80, 2093.0 to 160), 0.42)
                return
            }
        }

        val sampleRate = 22050
        val durationMs = when (type) { "SHOOT" -> 155; "CRASH" -> 480; else -> 230 }
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val p = i.toDouble() / numSamples
            val sample = when (type) {
                "SHOOT" -> {
                    val freq = 1520.0 - (p * 1180.0)
                    phase += 2.0 * PI * freq / sampleRate
                    sin(phase) * (1.0 - p)
                }
                "CRASH" -> {
                    val noise = (Random.nextDouble() * 2.0 - 1.0)
                    phase += 2.0 * PI * (95.0 * (1.0 - p * 0.5)) / sampleRate
                    (noise * 0.78 + sin(phase) * 0.22) * (1.0 - p)
                }
                else -> {
                    val freq = if (p < 0.45) 880.0 else 1318.5
                    phase += 2.0 * PI * freq / sampleRate
                    sin(phase) * sin(p * PI)
                }
            }
            buffer[i] = (sample * Short.MAX_VALUE * 0.64).toInt().toShort()
        }
        playStaticPcmAsync(sampleRate, buffer, durationMs.toLong())
    }

    fun playSqueakSound(enabled: Boolean = true, pitchMul: Double = 1.0) {
        if (!enabled) return
        val sampleRate = 22050
        val durationMs = 140
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val baseF = if (progress < 0.45) 950.0 + (progress / 0.45) * 1400.0 else 2350.0 - ((progress - 0.45) / 0.55) * 1100.0
            val freq = (baseF * pitchMul).coerceIn(320.0, 4200.0)
            val envelope = sin(progress * PI)
            phase += 2.0 * PI * freq / sampleRate
            val sample = (sin(phase) * 0.8 + sin(phase * 2.0) * 0.2) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.65).toInt().toShort()
        }
        playStaticPcmAsync(sampleRate, buffer, durationMs.toLong())
    }

    // סיום דרמטי עם פיצוץ חלקיקים והשארת צליל ההתרסקות פועל עד תומו!
    private fun triggerCrashAndExit(activity: MainActivity, endMessage: String, bestKey: String? = null, finalScore: Int = 0) {
        if (gameEnding) return
        gameEnding = true
        stopPreviousLoop()
        stopRacerAudio()
        crashAnimStartTime = System.currentTimeMillis()
        crashLane = carLane
        playRacerSoundEffect(activity, "CRASH")
        activity.shakeScreen()

        val pal = resolvePalette(activity)
        spawnBurst(if (currentGame == "RACER") 0.80f else 0.25f, 0.55f, pal.enemyColor, 26, 2)
        spawnBurst(if (currentGame == "RACER") 0.80f else 0.25f, 0.55f, pal.bonusColor, 18, 0)

        var msg = endMessage
        if (bestKey != null && saveHighScoreIfBeaten(activity, bestKey, finalScore)) {
            msg += "  |  שיא חדש!"
            spawnCallout(0.5f, 0.35f, "NEW BEST!", pal.bonusColor)
        }
        invalidateStage()

        val exitTask = Runnable {
            if (isActive && isActivityValid(activity)) {
                stopGameInternal(activity, msg, stopSfx = false)
            }
        }
        gameOverDelayRunnable = exitTask
        handler.postDelayed(exitTask, 900L)
    }

    // =========================================================================
    // משחק 0: מסיבת חיות מוזיקלית Pro (קוד 7777) — DJ + אתגר תפוס את החיה
    // =========================================================================
    fun startAnimalParty(activity: MainActivity) {
        prepareNewGame(activity, "ANIMALS")
        isAnimalParty = true
        animalRhythmMode = false
        animalScore = 0
        animalCombo = 0
        animalBest = loadHighScore(activity, "animals")
        animalTimeLeft = 20
        animalTotalTime = 20
        animalActiveIdx = 0
        animalTargetIdx = Random.nextInt(5)
        animalBounceTime = System.currentTimeMillis()
        animalDiscoUntil = 0L

        val shuffled = animalEmojis.shuffled()
        var idx = 0
        for ((k, btn) in activity.keypadButtons) {
            savedKeyLabels[k] = btn.text
            if (k !in listOf("AC", "0", "=")) {
                btn.text = shuffled[idx % shuffled.size]
                idx++
            }
        }
        updateAnimalSpecialKeys(activity)
        playSqueakSound(activity.useSound, 1.0)
        invalidateStage()

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "ANIMALS" || !isActivityValid(activity)) return
                animalTimeLeft--
                if (animalTimeLeft <= 0) {
                    val isNew = animalRhythmMode && saveHighScoreIfBeaten(activity, "animals", animalScore)
                    val endTxt = if (animalRhythmMode) {
                        "🐾 נגמר הזמן! ניקוד: $animalScore" + if (isNew) " (שיא חדש!)" else ""
                    } else {
                        "🐾 החיות חזרו לגן החיות!"
                    }
                    stopGameInternal(activity, endTxt, stopSfx = false)
                    return
                }
                invalidateStage()
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(gameLoop!!, 1000L)
    }

    private fun updateAnimalSpecialKeys(activity: MainActivity) {
        val pal = resolvePalette(activity)
        styleKeyForGame(activity, "0", pal.controlAccent, if (animalRhythmMode) "DJ" else "אתגר", glow = true)
        styleKeyForGame(activity, "=", pal.bonusColor, "🪩", glow = true)
        if (animalRhythmMode) {
            val faces = listOf("🐶", "🐱", "🦊", "🐼", "🐸")
            for (i in 0..4) {
                val k = animalTargetKeys[i]
                styleKeyForGame(activity, k, if (i == animalTargetIdx) pal.bonusColor else pal.inkPrimary, "${i + 1}${faces[i]}", glow = (i == animalTargetIdx))
            }
        }
    }

    // =========================================================================
    // משחק 1: איקס-עיגול Arena Pro (קוד 1337) — 3 רמות + 2 שחקנים + רמז חכם
    // =========================================================================
    fun startTicTacToe(activity: MainActivity) {
        val keepScores = (currentGame == "TTT")
        prepareNewGame(activity, "TTT")
        if (!keepScores) {
            tttWinsX = 0; tttWinsO = 0; tttTies = 0; tttStreak = 0
        }
        tttCurrentTurn = "×"
        tttLastMoveIdx = -1
        tttHintIdx = -1
        saveKeys(activity, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "+", "÷"))
        for (i in 0..8) {
            tttBoard[i] = (i + 1).toString()
            tttPlacedAt[i] = 0L
        }
        updateTicTacToeUI(activity, if (tttPvpMode) "תור שחקן × (1–9)" else "תורך! בחר משבצת 1–9")
    }

    private fun tttDifficultyLabel(): String = when {
        tttPvpMode -> "2 שחקנים"
        tttDifficulty == 0 -> "קל"
        tttDifficulty == 1 -> "בינוני"
        else -> "בלתי מנוצח"
    }

    private fun updateTicTacToeUI(activity: MainActivity, status: String) {
        tttStatusText = status
        val pal = resolvePalette(activity)
        for (i in 0..8) {
            val key = (i + 1).toString()
            val mark = tttBoard[i]
            val keyBg = MinimalistTheme.resolveKeypadButtonBgColor(activity, key)
            val neutralColor = MinimalistTheme.resolveKeypadSymbolTextColor(activity, key, keyBg)
            when (mark) {
                "×" -> styleKeyForGame(activity, key, pal.playerColor, mark, glow = true)
                "0" -> styleKeyForGame(activity, key, pal.enemyColor, mark, glow = true)
                else -> styleKeyForGame(activity, key, if (i == tttHintIdx) pal.bonusColor else neutralColor, mark, glow = (i == tttHintIdx))
            }
        }
        // מקשי אפשרויות משחקיות: 0=רמת קושי, ÷=מצב 2 שחקנים, +=רמז חכם
        styleKeyForGame(activity, "0", pal.controlAccent, "רמה", glow = false)
        styleKeyForGame(activity, "÷", if (tttPvpMode) pal.bonusColor else pal.controlAccent, if (tttPvpMode) "2P" else "AI", glow = tttPvpMode)
        styleKeyForGame(activity, "+", pal.bonusColor, "רמז", glow = false)
        invalidateStage()
    }

    private fun findWinningLine(): Pair<String, IntArray?>? {
        val wins = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
        for (w in wins) {
            if (tttBoard[w[0]] == tttBoard[w[1]] && tttBoard[w[1]] == tttBoard[w[2]]) {
                return tttBoard[w[0]] to w
            }
        }
        if (tttBoard.none { it in "1".."9" }) return "TIE" to null
        return null
    }

    private fun checkWinner(): String? = findWinningLine()?.first

    private fun getBestMoveFor(mark: String, smartChance: Float): Int {
        val free = (0..8).filter { tttBoard[it] !in listOf("×", "0") }
        if (free.isEmpty()) return -1
        val opp = if (mark == "0") "×" else "0"

        // ניצחון מיידי
        for (i in free) {
            val orig = tttBoard[i]; tttBoard[i] = mark
            if (checkWinner() == mark) { tttBoard[i] = orig; return i }
            tttBoard[i] = orig
        }
        // חסימה מיידית (ברמה בינונית ומעלה)
        if (smartChance >= 0.45f) {
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = opp
                if (checkWinner() == opp) { tttBoard[i] = orig; return i }
                tttBoard[i] = orig
            }
        }
        if (Random.nextFloat() < smartChance) {
            var bestScore = Int.MIN_VALUE
            val bestMoves = mutableListOf<Int>()
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = mark
                val score = minimaxFor(0, false, mark, opp)
                tttBoard[i] = orig
                if (score > bestScore) { bestScore = score; bestMoves.clear(); bestMoves.add(i) }
                else if (score == bestScore) bestMoves.add(i)
            }
            if (bestMoves.isNotEmpty()) return bestMoves.random()
        }
        return free.random()
    }

    private fun minimaxFor(depth: Int, isMax: Boolean, aiMark: String, humanMark: String): Int {
        when (checkWinner()) {
            aiMark -> return 10 - depth
            humanMark -> return depth - 10
            "TIE" -> return 0
        }
        val free = (0..8).filter { tttBoard[it] !in listOf("×", "0") }
        if (isMax) {
            var best = Int.MIN_VALUE
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = aiMark
                best = maxOf(best, minimaxFor(depth + 1, false, aiMark, humanMark))
                tttBoard[i] = orig
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = humanMark
                best = minOf(best, minimaxFor(depth + 1, true, aiMark, humanMark))
                tttBoard[i] = orig
            }
            return best
        }
    }

    private fun finishTicTacToe(activity: MainActivity, result: Pair<String, IntArray?>) {
        tttRoundLocked = true
        tttWinningLine = result.second
        val winner = result.first
        val pal = resolvePalette(activity)
        when (winner) {
            "×" -> {
                tttWinsX++; tttStreak++
                playTttResultSound(activity, true)
                spawnBurst(0.28f, 0.50f, pal.playerColor, 22, 1)
                spawnBurst(0.28f, 0.50f, pal.bonusColor, 18, 1)
                spawnCallout(0.72f, 0.35f, if (tttPvpMode) "× ניצח!" else "ניצחת! ×", pal.playerColor)
            }
            "0" -> {
                tttWinsO++; tttStreak = 0
                playTttResultSound(activity, tttPvpMode)
                spawnBurst(0.28f, 0.50f, pal.enemyColor, 20, 1)
                spawnCallout(0.72f, 0.35f, if (tttPvpMode) "0 ניצח!" else "המחשב ניצח", pal.enemyColor)
            }
            else -> {
                tttTies++
                spawnCallout(0.72f, 0.35f, "תיקו!", pal.bonusColor)
            }
        }
        val msg = when (winner) {
            "×" -> if (tttPvpMode) "שחקן × ניצח! סיבוב חדש..." else "ניצחת! רצף: $tttStreak"
            "0" -> if (tttPvpMode) "שחקן 0 ניצח! סיבוב חדש..." else "המחשב ניצח — נסה שוב"
            else -> "תיקו! משחק צמוד"
        }
        updateTicTacToeUI(activity, msg)
        roundResetRunnable?.let { handler.removeCallbacks(it) }
        val resetTask = Runnable {
            if (isActive && currentGame == "TTT" && isActivityValid(activity)) {
                startTicTacToe(activity)
            }
        }
        roundResetRunnable = resetTask
        handler.postDelayed(resetTask, 2000L)
    }

    // =========================================================================
    // משחק 2: פולשי החשבון Galactic Pro (קוד 8080) — לבבות, גלים, בוס ו-EMP
    // =========================================================================
    fun startCalcInvaders(activity: MainActivity) {
        val hasScientificKeys = activity.keypadButtons.containsKey("sin") && activity.keypadButtons.containsKey("√")
        if (!hasScientificKeys) {
            Toast.makeText(activity, "🔬 משחק זה זמין במחשבון המדעי בלבד!", Toast.LENGTH_LONG).show()
            activity.currentInput = ""
            activity.currentView?.text = "0"
            return
        }
        prepareNewGame(activity, "INVADERS")
        invaderScore = 0
        invaderBest = loadHighScore(activity, "invaders")
        invaderLives = 3
        invaderWave = 1
        invaderCombo = 0
        invaderEmpCharge = 0
        invaderBossSeq.clear()
        for (i in invaderTrack.indices) invaderTrack[i] = null
        saveKeys(activity, listOf("0", "="))

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "INVADERS" || !isActivityValid(activity)) return
                val reachedShip = invaderTrack[0]
                for (i in 0 until invaderTrack.lastIndex) {
                    invaderTrack[i] = invaderTrack[i + 1]
                }
                invaderTrack[invaderTrack.lastIndex] = null

                if (reachedShip != null) {
                    invaderLives--
                    invaderCombo = 0
                    val pal = resolvePalette(activity)
                    spawnBurst(0.16f, 0.55f, pal.enemyColor, 18, 2)
                    if (invaderLives <= 0) {
                        triggerCrashAndExit(activity, "💥 החללית הושמדה! ניקוד: $invaderScore (גל $invaderWave)", "invaders", invaderScore)
                        return
                    } else {
                        playRacerSoundEffect(activity, "CRASH")
                        activity.shakeScreen()
                        spawnCallout(0.24f, 0.40f, "−1 ❤️", pal.enemyColor)
                    }
                }

                // הגרלת אויב חדש (רגיל, משוריין או בוס גל)
                val spawnProb = (0.58f + invaderWave * 0.04f).coerceAtMost(0.82f)
                if (Random.nextFloat() < spawnProb) {
                    val roll = Random.nextFloat()
                    val hasBossAlready = invaderTrack.any { it?.isBoss == true }
                    val entity = when {
                        invaderWave >= 2 && !hasBossAlready && roll < 0.14f -> {
                            InvaderEntity(invaderSymbols.random(), hp = 3, maxHp = 3, isBoss = true)
                        }
                        invaderWave >= 2 && roll < 0.34f -> {
                            InvaderEntity(invaderSymbols.random(), hp = 2, maxHp = 2, isBoss = false)
                        }
                        else -> InvaderEntity(invaderSymbols.random(), hp = 1, maxHp = 1, isBoss = false)
                    }
                    invaderTrack[invaderTrack.lastIndex] = entity
                }

                renderInvaders(activity)
                val speed = (940L - (invaderWave * 65L) - (invaderScore * 2L)).coerceAtLeast(360L)
                handler.postDelayed(this, speed)
            }
        }
        renderInvaders(activity)
        handler.postDelayed(gameLoop!!, 850L)
    }

    private fun renderInvaders(activity: MainActivity) {
        val pal = resolvePalette(activity)
        val firstEnemy = invaderTrack.firstOrNull { it != null }
        for (sym in invaderSymbols) {
            val isNext = (firstEnemy?.symbol == sym)
            styleKeyForGame(activity, sym, if (isNext) pal.bonusColor else pal.controlAccent, glow = isNext)
        }
        val empReady = invaderEmpCharge >= 6
        styleKeyForGame(activity, "0", if (empReady) pal.laserColor else pal.inkSecondary, if (empReady) "EMP!" else "EMP", glow = empReady)
        invalidateStage()
    }

    // =========================================================================
    // משחק 3: מירוץ מכוניות Turbo Street Racer Pro (קוד 5555)
    // =========================================================================
    fun startMathRacer(activity: MainActivity) {
        prepareNewGame(activity, "RACER")
        carLane = 1
        racerScore = 0
        racerBest = loadHighScore(activity, "racer")
        racerCombo = 0
        racerPaused = false
        racerHasShield = false
        racerMagnetUntil = 0L
        racerSlowMoUntil = 0L
        racerSlowMoCharge = 100
        racerNitroCharge = 100
        racerNitroBurstUntil = 0L
        lastShotTime = 0L
        lastCooldownToastTime = 0L
        pendingGuardedStarLane = -1
        for (lane in 0..2) racerTracks[lane] = MutableList(6) { "." }

        saveKeys(activity, listOf("8", "2", "5", "6", "4", "0"))
        startRacerEngineLoop(activity)

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "RACER" || !isActivityValid(activity)) return
                if (!racerPaused) {
                    advanceRacerTracks(activity)
                    if (!isActive || gameEnding) return
                    racerNitroCharge = (racerNitroCharge + 6).coerceAtMost(100)
                    racerSlowMoCharge = (racerSlowMoCharge + 4).coerceAtMost(100)
                }
                renderRacer(activity)
                handler.postDelayed(this, currentRacerDelayMs())
            }
        }
        cooldownTicker = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "RACER" || !isActivityValid(activity)) return
                renderRacer(activity)
                handler.postDelayed(this, 220L)
            }
        }
        renderRacer(activity)
        handler.postDelayed(gameLoop!!, currentRacerDelayMs())
        handler.postDelayed(cooldownTicker!!, 220L)
    }

    private fun currentRacerDelayMs(): Long {
        val now = System.currentTimeMillis()
        val base = (720L - (racerScore * 6L)).coerceAtLeast(250L)
        return when {
            now < racerSlowMoUntil -> (base * 1.85).toLong()
            now < racerNitroBurstUntil -> (base * 0.62).toLong().coerceAtLeast(180L)
            else -> base
        }
    }

    private fun advanceRacerTracks(activity: MainActivity) {
        val now = System.currentTimeMillis()
        val magnetActive = now < racerMagnetUntil
        val pal = resolvePalette(activity)

        // מגנט כוכבים שואב כוכבים מנתיבים שכנים אל נתיב המכונית
        if (magnetActive) {
            for (lane in 0..2) {
                if (lane == carLane) continue
                val lastIdx = racerTracks[lane].lastIndex
                if (racerTracks[lane][lastIdx] == "⭐") {
                    racerTracks[lane][lastIdx] = "."
                    collectRacerStar(activity, pal)
                }
            }
        }

        for (lane in 0..2) {
            val reachedCar = racerTracks[lane].removeAt(racerTracks[lane].lastIndex)
            if (reachedCar != ".") {
                if (lane == carLane) {
                    when (reachedCar) {
                        "🚧", "🛢️" -> {
                            if (racerHasShield) {
                                racerHasShield = false
                                racerShieldBreakTime = now
                                playRacerSoundEffect(activity, "EMP")
                                activity.shakeScreen()
                                spawnBurst(0.80f, 0.30f + carLane * 0.24f, pal.shieldColor, 20, 2)
                                spawnCallout(0.65f, 0.25f + carLane * 0.22f, "SHIELD SAVED!", pal.shieldColor)
                            } else {
                                triggerCrashAndExit(activity, "💥 התנגשת במכשול! ניקוד סופי: $racerScore 🏆", "racer", racerScore)
                                return
                            }
                        }
                        "⭐" -> collectRacerStar(activity, pal)
                        "🛡️" -> {
                            racerHasShield = true
                            playRacerSoundEffect(activity, "POWERUP")
                            spawnBurst(0.80f, 0.30f + carLane * 0.24f, pal.shieldColor, 16, 0)
                            spawnCallout(0.66f, 0.28f + carLane * 0.22f, "SHIELD ON!", pal.shieldColor)
                        }
                        "🧲" -> {
                            racerMagnetUntil = now + 8000L
                            playRacerSoundEffect(activity, "POWERUP")
                            spawnBurst(0.80f, 0.30f + carLane * 0.24f, pal.bonusColor, 16, 1)
                            spawnCallout(0.66f, 0.28f + carLane * 0.22f, "MAGNET 8s!", pal.bonusColor)
                        }
                    }
                } else if (reachedCar == "🚧" || reachedCar == "🛢️") {
                    racerScore += if (now < racerNitroBurstUntil) 2 else 1
                }
            }
        }

        if (pendingGuardedStarLane in 0..2) {
            val starLane = pendingGuardedStarLane
            pendingGuardedStarLane = -1
            for (lane in 0..2) racerTracks[lane].add(0, if (lane == starLane) "⭐" else ".")
            return
        }

        val recentlySpawned = (0..2).any { racerTracks[it].firstOrNull() != "." }
        if (!recentlySpawned && Random.nextFloat() < 0.76f) {
            val spawnLane = Random.nextInt(3)
            val roll = Random.nextFloat()
            when {
                roll < 0.08f && !racerHasShield -> {
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "🛡️" else ".")
                }
                roll < 0.14f && now >= racerMagnetUntil -> {
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "🧲" else ".")
                }
                roll < 0.25f -> {
                    pendingGuardedStarLane = spawnLane
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "🚧" else ".")
                }
                roll < 0.34f -> {
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "🛢️" else ".")
                }
                roll < 0.42f -> {
                    val blockerLane = (spawnLane + 1 + Random.nextInt(2)) % 3
                    for (lane in 0..2) {
                        val sym = when (lane) { spawnLane -> "⭐"; blockerLane -> "🚧"; else -> "." }
                        racerTracks[lane].add(0, sym)
                    }
                }
                else -> {
                    val secondBlocker = if (racerScore >= 18 && Random.nextFloat() < 0.36f) (spawnLane + 1 + Random.nextInt(2)) % 3 else -1
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane || lane == secondBlocker) "🚧" else ".")
                }
            }
        } else {
            for (lane in 0..2) racerTracks[lane].add(0, ".")
        }
    }

    private fun collectRacerStar(activity: MainActivity, pal: GameThemePalette) {
        playRacerSoundEffect(activity, "STAR")
        racerCombo = (racerCombo + 1).coerceAtMost(5)
        val gain = 10 * racerCombo
        racerScore += gain
        lastShotTime = 0L // איפוס לייזר מיידי!
        racerNitroCharge = (racerNitroCharge + 25).coerceAtMost(100)
        spawnBurst(0.80f, 0.30f + carLane * 0.24f, pal.bonusColor, 16, 1)
        val label = if (racerCombo > 1) "+$gain x$racerCombo!" else "+$gain ⭐"
        spawnCallout(0.70f, 0.26f + carLane * 0.22f, label, pal.bonusColor)
    }

    internal fun getShotCooldownRemainingMs(): Long {
        val elapsed = System.currentTimeMillis() - lastShotTime
        return (SHOT_COOLDOWN_MS - elapsed).coerceAtLeast(0L)
    }

    private fun renderRacer(activity: MainActivity) {
        val pal = resolvePalette(activity)
        val remMs = getShotCooldownRemainingMs()
        val gunReady = (remMs == 0L)
        val sec = ceil(remMs / 1000.0).toInt()
        val slowReady = racerSlowMoCharge >= 100
        val nitroReady = racerNitroCharge >= 40

        styleKeyForGame(activity, "8", pal.controlAccent, if (pal.isMinimalist) "8 ↑" else "8⬆️", glow = true)
        styleKeyForGame(activity, "2", pal.controlAccent, if (pal.isMinimalist) "2 ↓" else "2⬇️", glow = true)
        styleKeyForGame(
            activity, "5",
            if (gunReady) pal.playerColor else pal.inkSecondary,
            if (gunReady) (if (pal.isMinimalist) "5 ✦" else "5💥") else "${sec}s",
            glow = gunReady
        )
        styleKeyForGame(activity, "6", if (nitroReady) pal.bonusColor else pal.inkSecondary, if (pal.isMinimalist) "6 »" else "6⚡", glow = nitroReady)
        styleKeyForGame(activity, "4", if (slowReady) pal.shieldColor else pal.inkSecondary, if (slowReady) "4 ❄" else "${racerSlowMoCharge}%", glow = slowReady)
        styleKeyForGame(activity, "0", pal.inkSecondary, if (racerPaused) "▶" else "⏸", glow = racerPaused)
        invalidateStage()
    }

    // =========================================================================
    // משחק 4: נטרול פצצה Pro (קוד 1010) — דרגות קושי, הקפאת זמן ו-50/50
    // =========================================================================
    fun startBombDefusal(activity: MainActivity) {
        prepareNewGame(activity, "BOMB")
        bombTimeLeft = 18
        bombScore = 0
        bombBest = loadHighScore(activity, "bomb")
        bombStreak = 0
        bombLevel = 1
        bombFreezeUntil = 0L
        bombFreezeCharges = 1
        bombCutCharges = 1
        bombEliminatedOps.clear()
        saveKeys(activity, listOf("0", "5"))
        generateBombQuestion(activity)

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "BOMB" || !isActivityValid(activity)) return
                val now = System.currentTimeMillis()
                if (now >= bombFreezeUntil) {
                    bombTimeLeft--
                    if (bombTimeLeft <= 0) {
                        triggerCrashAndExit(activity, "💥 בום! הזמן נגמר! פצצות שנוטרלו: $bombScore", "bomb", bombScore)
                        return
                    }
                }
                renderBombUI(activity)
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(gameLoop!!, 1000L)
    }

    private fun evalOp(a: Int, b: Int, op: String): Int? = when (op) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> if (b != 0 && a % b == 0) a / b else null
        else -> null
    }

    private fun generateBombQuestion(activity: MainActivity) {
        bombEliminatedOps.clear()
        bombLevel = (1 + bombScore / 4).coerceAtMost(4)
        val ops = listOf("+", "−", "×", "÷")

        // מרמה 3 ומעלה — משולבות גם משוואות 2 שלבים עם סוגריים!
        val useTwoStep = bombLevel >= 3 && Random.nextFloat() < 0.55f
        for (attempt in 0 until 25) {
            if (useTwoStep) {
                val x = Random.nextInt(2, 9)
                val y = Random.nextInt(2, 8)
                val innerAdd = Random.nextBoolean()
                val leftVal = if (innerAdd) x + y else (x + y) - y + 2
                val innerStr = if (innerAdd) "($x + $y)" else "(${x + y + 2} − $y)"
                val rhs = Random.nextInt(2, 9)
                val op = ops.random()
                val res = evalOp(leftVal, rhs, op) ?: continue
                val matches = ops.count { evalOp(leftVal, rhs, it) == res }
                if (matches == 1 || attempt == 24) {
                    bombEqDisplay = "$innerStr [?] $rhs = $res"
                    expectedOp = op
                    bombValidator = { candidate -> evalOp(leftVal, rhs, candidate) == res }
                    break
                }
            } else {
                val maxA = if (bombLevel == 1) 12 else 18
                val maxB = if (bombLevel == 1) 10 else 12
                val a = Random.nextInt(2, maxA)
                val b = Random.nextInt(2, maxB)
                val op = ops.random()
                val (lhs, rhs, target) = when (op) {
                    "+" -> Triple(a, b, a + b)
                    "−" -> Triple(a + b, b, a)
                    "×" -> Triple(a, b, a * b)
                    else -> Triple(a * b, b, a)
                }
                val matches = ops.count { evalOp(lhs, rhs, it) == target }
                if (matches == 1 || attempt == 24) {
                    bombLhs = lhs; bombRhs = rhs; bombTarget = target
                    bombEqDisplay = "$lhs [?] $rhs = $target"
                    expectedOp = op
                    bombValidator = { candidate -> evalOp(lhs, rhs, candidate) == target }
                    break
                }
            }
        }
        renderBombUI(activity)
    }

    private fun renderBombUI(activity: MainActivity) {
        val pal = resolvePalette(activity)
        val isUrgent = bombTimeLeft <= 5 && System.currentTimeMillis() >= bombFreezeUntil
        for (op in listOf("+", "−", "×", "÷")) {
            val isCut = op in bombEliminatedOps
            val col = when {
                isCut -> pal.inkSecondary
                isUrgent -> pal.enemyColor
                else -> pal.controlAccent
            }
            styleKeyForGame(activity, op, col, glow = !isCut)
        }
        styleKeyForGame(
            activity, "0",
            if (bombFreezeCharges > 0) pal.shieldColor else pal.inkSecondary,
            "❄×$bombFreezeCharges",
            glow = bombFreezeCharges > 0
        )
        styleKeyForGame(
            activity, "5",
            if (bombCutCharges > 0 && bombEliminatedOps.isEmpty()) pal.bonusColor else pal.inkSecondary,
            "50/50",
            glow = bombCutCharges > 0 && bombEliminatedOps.isEmpty()
        )
        invalidateStage()
    }

    // =========================================================================
    // קליטת מקשים בזמן משחק
    // =========================================================================
    fun onKeyPress(activity: MainActivity, key: String): Boolean {
        if (!isActive) return false
        if (key == "AC") {
            stopGameInternal(activity, "יצאת מהמשחק 🎮", stopSfx = true)
            return true
        }
        if (gameEnding) return true

        val pal = resolvePalette(activity)

        when (currentGame) {
            "ANIMALS" -> {
                if (key == "0") {
                    animalRhythmMode = !animalRhythmMode
                    animalScore = 0; animalCombo = 0; animalTimeLeft = 20
                    animalTargetIdx = Random.nextInt(5)
                    updateAnimalSpecialKeys(activity)
                    spawnCallout(0.5f, 0.35f, if (animalRhythmMode) "מצב אתגר!" else "מצב DJ חופשי!", pal.bonusColor)
                    invalidateStage()
                    return true
                }
                if (key == "=") {
                    animalDiscoUntil = System.currentTimeMillis() + 2200L
                    animalBounceTime = System.currentTimeMillis()
                    playSynthMelody(activity, listOf(523.25 to 90, 659.25 to 90, 783.99 to 110, 1046.5 to 220), 0.48)
                    spawnBurst(0.25f, 0.55f, pal.bonusColor, 18, 1)
                    spawnBurst(0.75f, 0.55f, pal.playerColor, 18, 1)
                    spawnCallout(0.5f, 0.32f, "DISCO PARTY!", pal.bonusColor)
                    invalidateStage()
                    return true
                }

                if (animalRhythmMode) {
                    val pressedIdx = animalTargetKeys.indexOf(key)
                    if (pressedIdx == animalTargetIdx) {
                        animalCombo++
                        animalScore += 5 * animalCombo.coerceAtMost(5)
                        animalTimeLeft = (animalTimeLeft + 1).coerceAtMost(25)
                        animalActiveIdx = pressedIdx
                        animalBounceTime = System.currentTimeMillis()
                        playSynthMelody(activity, listOf(pentatonicScale[pressedIdx + 3] to 130), 0.55)
                        spawnBurst(0.14f + pressedIdx * 0.18f, 0.56f, pal.bonusColor, 14, 1)
                        spawnCallout(0.14f + pressedIdx * 0.18f, 0.38f, "+${5 * animalCombo.coerceAtMost(5)}", pal.playerColor)
                        animalTargetIdx = Random.nextInt(5)
                        updateAnimalSpecialKeys(activity)
                    } else if (pressedIdx >= 0) {
                        animalCombo = 0
                        animalTimeLeft = (animalTimeLeft - 2).coerceAtLeast(1)
                        playSqueakSound(activity.useSound, 0.65)
                        activity.shakeScreen()
                    }
                } else {
                    // מצב DJ חופשי — כל מקש מנגן תו שמח ומאריך את הכיף
                    val noteIdx = (key.hashCode().absoluteValue) % pentatonicScale.size
                    animalActiveIdx = noteIdx % 5
                    animalBounceTime = System.currentTimeMillis()
                    animalTimeLeft = (animalTimeLeft + 1).coerceAtMost(20)
                    playSynthMelody(activity, listOf(pentatonicScale[noteIdx] to 145), 0.52)
                    spawnBurst(0.14f + animalActiveIdx * 0.18f, 0.56f, pal.bonusColor, 10, 1)
                }
                invalidateStage()
                return true
            }

            "TTT" -> {
                if (key == "0") {
                    tttPvpMode = false
                    tttDifficulty = (tttDifficulty + 1) % 3
                    spawnCallout(0.72f, 0.25f, "רמה: ${tttDifficultyLabel()}", pal.controlAccent)
                    startTicTacToe(activity)
                    return true
                }
                if (key == "÷") {
                    tttPvpMode = !tttPvpMode
                    spawnCallout(0.72f, 0.25f, if (tttPvpMode) "מצב 2 שחקנים!" else "נגד המחשב", pal.bonusColor)
                    startTicTacToe(activity)
                    return true
                }
                if (key == "+") {
                    if (!tttRoundLocked) {
                        val best = getBestMoveFor(if (tttPvpMode) tttCurrentTurn else "×", 1.0f)
                        if (best in 0..8) {
                            tttHintIdx = best
                            tttHintTime = System.currentTimeMillis()
                            playSynthMelody(activity, listOf(783.99 to 90, 1046.5 to 110), 0.38)
                            updateTicTacToeUI(activity, "רמז: משבצת ${best + 1}")
                        }
                    }
                    return true
                }
                if (tttRoundLocked) {
                    updateTicTacToeUI(activity, tttStatusText)
                    return true
                }
                val idx = key.toIntOrNull()?.minus(1)
                if (idx != null && idx in 0..8 && tttBoard[idx] !in listOf("×", "0")) {
                    tttHintIdx = -1
                    if (tttPvpMode) {
                        tttBoard[idx] = tttCurrentTurn
                        tttPlacedAt[idx] = System.currentTimeMillis()
                        tttLastMoveIdx = idx
                        val res = findWinningLine()
                        if (res != null) {
                            finishTicTacToe(activity, res)
                            return true
                        }
                        tttCurrentTurn = if (tttCurrentTurn == "×") "0" else "×"
                        updateTicTacToeUI(activity, "תור שחקן $tttCurrentTurn (1–9)")
                    } else {
                        tttBoard[idx] = "×"
                        tttPlacedAt[idx] = System.currentTimeMillis()
                        tttLastMoveIdx = idx
                        var res = findWinningLine()
                        if (res != null) { finishTicTacToe(activity, res); return true }

                        val smartChance = when (tttDifficulty) { 0 -> 0.15f; 1 -> 0.65f; else -> 1.0f }
                        val comp = getBestMoveFor("0", smartChance)
                        if (comp != -1) {
                            tttBoard[comp] = "0"
                            tttPlacedAt[comp] = System.currentTimeMillis()
                            tttLastMoveIdx = comp
                        }
                        res = findWinningLine()
                        if (res != null) finishTicTacToe(activity, res) else updateTicTacToeUI(activity, "תורך! בחר משבצת 1–9")
                    }
                } else {
                    updateTicTacToeUI(activity, tttStatusText)
                }
                return true
            }

            "INVADERS" -> {
                if ((key == "0" || key == "=") && invaderEmpCharge >= 6) {
                    invaderEmpCharge = 0
                    invaderEmpWaveTime = System.currentTimeMillis()
                    var cleared = 0
                    for (i in invaderTrack.indices) {
                        if (invaderTrack[i] != null) {
                            invaderTrack[i] = null
                            cleared++
                        }
                    }
                    val bonus = cleared * 12
                    invaderScore += bonus
                    playRacerSoundEffect(activity, "EMP")
                    spawnBurst(0.50f, 0.55f, pal.laserColor, 26, 2)
                    spawnCallout(0.50f, 0.35f, "EMP BLAST +$bonus!", pal.laserColor)
                    renderInvaders(activity)
                    return true
                }

                val targetIndex = invaderTrack.indexOfFirst { it != null }
                val target = if (targetIndex != -1) invaderTrack[targetIndex] else null
                if (target != null && target.symbol == key) {
                    invaderLaserCol = targetIndex
                    invaderLaserTime = System.currentTimeMillis()
                    playRacerSoundEffect(activity, "SHOOT")
                    if (activity.useHaptic) activity.mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    target.hp--
                    val colNormX = 0.25f + (targetIndex + 0.5f) * 0.12f
                    if (target.hp <= 0) {
                        invaderTrack[targetIndex] = null
                        invaderCombo = (invaderCombo + 1).coerceAtMost(5)
                        invaderEmpCharge = (invaderEmpCharge + 1).coerceAtMost(6)
                        val pts = (if (target.isBoss) 35 else if (target.maxHp > 1) 18 else 10) * invaderCombo
                        invaderScore += pts
                        invaderWave = (1 + invaderScore / 120).coerceAtMost(15)
                        spawnBurst(colNormX, 0.56f, if (target.isBoss) pal.bonusColor else pal.laserColor, if (target.isBoss) 26 else 14, 2)
                        spawnCallout(colNormX, 0.36f, if (invaderCombo > 1) "+$pts x$invaderCombo" else "+$pts", pal.bonusColor)
                    } else {
                        // אויב משוריין / בוס מחליף סימן לפגיעה הבאה!
                        val nextSym = invaderSymbols.filter { it != target.symbol }.random()
                        invaderTrack[targetIndex] = target.copy(symbol = nextSym)
                        spawnBurst(colNormX, 0.56f, pal.enemyColor, 10, 0)
                        spawnCallout(colNormX, 0.36f, "HIT! (${target.hp} HP)", pal.enemyColor)
                    }
                } else {
                    invaderCombo = 0
                    invaderScore = (invaderScore - 2).coerceAtLeast(0)
                }
                renderInvaders(activity)
                return true
            }

            "RACER" -> {
                if (key == "0" || key == "−") {
                    racerPaused = !racerPaused
                    spawnCallout(0.5f, 0.45f, if (racerPaused) "PAUSED" else "GO!", pal.bonusColor)
                    renderRacer(activity)
                    return true
                }
                if (racerPaused) {
                    renderRacer(activity)
                    return true
                }
                when {
                    key == "8" || key.startsWith("8") -> carLane = (carLane - 1).coerceAtLeast(0)
                    key == "2" || key.startsWith("2") -> carLane = (carLane + 1).coerceAtMost(2)
                    key == "4" || key.startsWith("4") -> {
                        if (racerSlowMoCharge >= 100) {
                            racerSlowMoCharge = 0
                            racerSlowMoUntil = System.currentTimeMillis() + 5000L
                            playRacerSoundEffect(activity, "FREEZE")
                            spawnCallout(0.5f, 0.36f, "SLOW-MO 5s!", pal.shieldColor)
                        }
                    }
                    key == "5" || key.startsWith("5") || key.endsWith("s") || key == "×" -> {
                        val rem = getShotCooldownRemainingMs()
                        if (rem == 0L) {
                            lastShotTime = System.currentTimeMillis()
                            racerLaserLane = carLane
                            racerLaserTime = lastShotTime
                            playRacerSoundEffect(activity, "SHOOT")
                            val idx = racerTracks[carLane].indexOfLast { it == "🚧" || it == "🛢️" }
                            if (idx != -1) {
                                val hitType = racerTracks[carLane][idx]
                                racerLaserEndCol = idx
                                racerTracks[carLane][idx] = "."
                                val normX = 0.12f + idx * 0.11f
                                val normY = 0.32f + carLane * 0.24f
                                if (hitType == "🛢️") {
                                    // חבית נפץ משמידה גם מכשולים סמוכים!
                                    for (l in (carLane - 1).coerceAtLeast(0)..(carLane + 1).coerceAtMost(2)) {
                                        if (racerTracks[l][idx] == "🚧" || racerTracks[l][idx] == "🛢️") {
                                            racerTracks[l][idx] = "."
                                        }
                                    }
                                    racerScore += 6
                                    spawnBurst(normX, normY, pal.bonusColor, 22, 2)
                                    spawnCallout(normX, normY - 0.1f, "BOOM +6!", pal.bonusColor)
                                } else {
                                    racerScore += 3
                                    spawnBurst(normX, normY, pal.laserColor, 14, 2)
                                    spawnCallout(normX, normY - 0.1f, "+3", pal.laserColor)
                                }
                            } else {
                                racerLaserEndCol = 0
                            }
                        } else {
                            val now = System.currentTimeMillis()
                            if (now - lastCooldownToastTime > 1200L) {
                                lastCooldownToastTime = now
                                val sec = ceil(rem / 1000.0).toInt()
                                Toast.makeText(activity, "הירייה בטעינה — עוד $sec שניות", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    key == "6" || key.startsWith("6") || key == "+" -> {
                        if (racerNitroCharge >= 40) {
                            racerNitroCharge = (racerNitroCharge - 40).coerceAtLeast(0)
                            racerNitroBurstUntil = System.currentTimeMillis() + 2400L
                            playRacerSoundEffect(activity, "POWERUP")
                            spawnBurst(0.88f, 0.32f + carLane * 0.24f, pal.bonusColor, 14, 2)
                            spawnCallout(0.70f, 0.26f + carLane * 0.22f, "NITRO x2!", pal.bonusColor)
                        }
                        racerScore += 2
                        advanceRacerTracks(activity)
                        if (isActive && !gameEnding && gameLoop != null) {
                            handler.removeCallbacks(gameLoop!!)
                            handler.postDelayed(gameLoop!!, currentRacerDelayMs())
                        }
                    }
                }
                if (isActive && !gameEnding) renderRacer(activity)
                return true
            }

            "BOMB" -> {
                if (key == "0" && bombFreezeCharges > 0) {
                    bombFreezeCharges--
                    bombFreezeUntil = System.currentTimeMillis() + 5000L
                    playRacerSoundEffect(activity, "FREEZE")
                    spawnBurst(0.22f, 0.54f, pal.shieldColor, 18, 0)
                    spawnCallout(0.22f, 0.30f, "FREEZE 5s!", pal.shieldColor)
                    renderBombUI(activity)
                    return true
                }
                if (key == "5" && bombCutCharges > 0 && bombEliminatedOps.isEmpty()) {
                    bombCutCharges--
                    val wrongOps = listOf("+", "−", "×", "÷").filter { !bombValidator(it) }.shuffled().take(2)
                    bombEliminatedOps.addAll(wrongOps)
                    playRacerSoundEffect(activity, "SHOOT")
                    spawnCallout(0.68f, 0.65f, "50/50 CUT!", pal.bonusColor)
                    renderBombUI(activity)
                    return true
                }
                if (key in listOf("+", "−", "×", "÷")) {
                    if (bombValidator(key)) {
                        bombScore++
                        bombStreak++
                        val timeBonus = if (bombStreak % 3 == 0) 5 else 3
                        bombTimeLeft = (bombTimeLeft + timeBonus).coerceAtMost(24)
                        if (bombScore % 4 == 0) bombFreezeCharges = (bombFreezeCharges + 1).coerceAtMost(3)
                        if (bombScore % 3 == 0) bombCutCharges = (bombCutCharges + 1).coerceAtMost(3)
                        playRacerSoundEffect(activity, "STAR")
                        spawnBurst(0.68f, 0.48f, pal.playerColor, 18, 1)
                        spawnCallout(0.68f, 0.32f, "+${timeBonus}s!", pal.playerColor)
                        generateBombQuestion(activity)
                    } else {
                        bombStreak = 0
                        bombTimeLeft = (bombTimeLeft - 3).coerceAtLeast(1)
                        bombShakeTime = System.currentTimeMillis()
                        playRacerSoundEffect(activity, "CRASH")
                        activity.shakeScreen()
                        spawnBurst(0.22f, 0.54f, pal.enemyColor, 14, 2)
                        spawnCallout(0.22f, 0.32f, "−3s!", pal.enemyColor)
                        renderBombUI(activity)
                    }
                } else {
                    renderBombUI(activity)
                }
                return true
            }
        }
        return true
    }

    private fun stopPreviousLoop() {
        gameLoop?.let { handler.removeCallbacks(it) }
        cooldownTicker?.let { handler.removeCallbacks(it) }
        roundResetRunnable?.let { handler.removeCallbacks(it) }
        gameOverDelayRunnable?.let { handler.removeCallbacks(it) }
        gameLoop = null
        cooldownTicker = null
        roundResetRunnable = null
        gameOverDelayRunnable = null
    }
}

// =========================================================================
// קנבס משחקים וקטורי ב-60fps עם מערכת חלקיקים, טקסט צף ו-4 ערכות נושא
// =========================================================================
private class MiniGameVectorStageView(
    private val activity: MainActivity
) : View(activity) {

    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var smoothCarLane = 1f
    private var anim: ValueAnimator? = null

    private val particles = mutableListOf<MiniGameEngine.Particle>()
    private val callouts = mutableListOf<MiniGameEngine.FloatingCallout>()

    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dashedP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * d
    }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val rect = RectF()
    private val path = Path()

    fun spawnBurstNormalized(nx: Float, ny: Float, color: Int, count: Int, shape: Int) {
        val w = width.toFloat().coerceAtLeast(240f * d)
        val h = height.toFloat().coerceAtLeast(140f * d)
        val ox = nx * w
        val oy = ny * h
        for (i in 0 until count) {
            if (particles.size >= 70) particles.removeAt(0)
            val ang = Random.nextDouble(0.0, 2.0 * PI)
            val spd = (1.4f + Random.nextFloat() * 3.8f) * d
            particles.add(
                MiniGameEngine.Particle(
                    x = ox,
                    y = oy,
                    vx = (cos(ang) * spd).toFloat(),
                    vy = (sin(ang) * spd).toFloat() - (if (shape == 1) 1.5f * d else 0f),
                    size = (2.2f + Random.nextFloat() * 3.0f) * d,
                    color = color,
                    life = 1f,
                    decay = 0.024f + Random.nextFloat() * 0.022f,
                    shape = shape
                )
            )
        }
    }

    fun spawnCalloutNormalized(nx: Float, ny: Float, text: String, color: Int) {
        val w = width.toFloat().coerceAtLeast(240f * d)
        val h = height.toFloat().coerceAtLeast(140f * d)
        if (callouts.size >= 8) callouts.removeAt(0)
        callouts.add(MiniGameEngine.FloatingCallout(nx * w, ny * h, text, color, 1f))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        smoothCarLane = MiniGameEngine.carLane.toFloat()
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val slow = (MiniGameEngine.currentGame == "RACER" && System.currentTimeMillis() < MiniGameEngine.racerSlowMoUntil)
                val nitro = (MiniGameEngine.currentGame == "RACER" && System.currentTimeMillis() < MiniGameEngine.racerNitroBurstUntil)
                val paused = (MiniGameEngine.currentGame == "RACER" && MiniGameEngine.racerPaused)
                if (!paused) {
                    phase += when {
                        slow -> 0.022f
                        nitro -> 0.095f
                        else -> 0.052f
                    }
                    if (phase > 6283f) phase = 0f
                }
                val targetLane = MiniGameEngine.carLane.toFloat()
                smoothCarLane += (targetLane - smoothCarLane) * 0.28f

                // עדכון חלקיקים וטקסטים צפים
                val pIt = particles.iterator()
                while (pIt.hasNext()) {
                    val p = pIt.next()
                    p.x += p.vx
                    p.y += p.vy
                    if (p.shape == 1) p.vy += 0.09f * d // כבידה קלה לקונפטי
                    p.vx *= 0.96f
                    p.vy *= 0.96f
                    p.life -= p.decay
                    if (p.life <= 0f) pIt.remove()
                }
                val cIt = callouts.iterator()
                while (cIt.hasNext()) {
                    val c = cIt.next()
                    c.y -= 0.75f * d
                    c.life -= 0.026f
                    if (c.life <= 0f) cIt.remove()
                }
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        particles.clear()
        callouts.clear()
        super.onDetachedFromWindow()
    }

    private fun withAlpha(color: Int, a: Int): Int =
        Color.argb(a.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 4f || h <= 4f) return

        val pal = MiniGameEngine.resolvePalette(activity)
        drawStageBackdrop(canvas, w, h, pal)

        when (MiniGameEngine.currentGame) {
            "RACER" -> drawVectorRacer(canvas, w, h, pal)
            "TTT" -> drawVectorTicTacToe(canvas, w, h, pal)
            "INVADERS" -> drawVectorInvaders(canvas, w, h, pal)
            "BOMB" -> drawVectorBomb(canvas, w, h, pal)
            "ANIMALS" -> drawVectorAnimalParty(canvas, w, h, pal)
        }

        drawParticlesAndCallouts(canvas)
    }

    private fun drawParticlesAndCallouts(canvas: Canvas) {
        for (p in particles) {
            val a = (p.life * 255f).toInt().coerceIn(0, 255)
            fillP.color = withAlpha(p.color, a)
            when (p.shape) {
                1 -> {
                    canvas.save()
                    canvas.rotate(p.life * 240f, p.x, p.y)
                    canvas.drawRect(p.x - p.size * 0.7f, p.y - p.size * 0.4f, p.x + p.size * 0.7f, p.y + p.size * 0.4f, fillP)
                    canvas.restore()
                }
                2 -> {
                    strokeP.strokeWidth = 2f * d
                    strokeP.color = withAlpha(p.color, a)
                    canvas.drawLine(p.x, p.y, p.x - p.vx * 2.2f, p.y - p.vy * 2.2f, strokeP)
                }
                else -> canvas.drawCircle(p.x, p.y, p.size * p.life, fillP)
            }
        }
        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 12.5f * d
        for (c in callouts) {
            val a = (c.life * 255f).toInt().coerceIn(0, 255)
            textP.color = withAlpha(c.color, a)
            canvas.drawText(c.text, c.x, c.y, textP)
        }
    }

    private fun drawStageBackdrop(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        rect.set(2f * d, 2f * d, w - 2f * d, h - 2f * d)
        val r = 20f * d
        fillP.shader = LinearGradient(0f, 0f, w, h, pal.cardBgTop, pal.cardBgBottom, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, r, r, fillP)
        fillP.shader = null

        if (pal.isGlass) {
            val gloss = RectF(10f * d, 4f * d, w - 10f * d, h * 0.24f)
            fillP.shader = LinearGradient(0f, gloss.top, 0f, gloss.bottom, Color.argb(62, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(gloss, 14f * d, 14f * d, fillP)
            fillP.shader = null
        } else if (pal.isAurora) {
            // סרט זוהר צפוני עליון עדין
            val aur = RectF(6f * d, 4f * d, w - 6f * d, h * 0.30f)
            fillP.shader = LinearGradient(0f, aur.top, w, aur.bottom, withAlpha(pal.playerColor, 34), withAlpha(pal.enemyColor, 20), Shader.TileMode.CLAMP)
            canvas.drawRoundRect(aur, 16f * d, 16f * d, fillP)
            fillP.shader = null
        } else if (!pal.isMinimalist) {
            strokeP.clearShadowLayer()
            strokeP.strokeWidth = 0.7f * d
            strokeP.color = pal.arenaGrid
            var x = 22f * d
            while (x < w - 10f * d) { canvas.drawLine(x, 8f * d, x, h - 8f * d, strokeP); x += 22f * d }
        }

        strokeP.strokeWidth = 1.4f * d
        strokeP.color = pal.cardBorder
        if (pal.isAurora) strokeP.setShadowLayer(6f * d, 0f, 0f, withAlpha(pal.glowColor, 165)) else strokeP.clearShadowLayer()
        canvas.drawRoundRect(rect, r, r, strokeP)
        strokeP.clearShadowLayer()
    }

    // =========================================================================
    // 1. מירוץ מכוניות Turbo Street Racer Pro (5555)
    // =========================================================================
    private fun drawVectorRacer(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val now = System.currentTimeMillis()
        val padX = 12f * d
        val hudY = 19f * d
        val roadTop = 28f * d
        val roadBot = h - 10f * d
        val roadH = (roadBot - roadTop).coerceAtLeast(36f * d)
        val laneH = roadH / 3f
        val isNitro = now < MiniGameEngine.racerNitroBurstUntil
        val isSlow = now < MiniGameEngine.racerSlowMoUntil
        val isMagnet = now < MiniGameEngine.racerMagnetUntil
        val gear = (1 + MiniGameEngine.racerScore / 15).coerceAtMost(6)

        // HUD עליון: ניקוד + שיא + הילוך + מד לייזר
        textP.textAlign = Paint.Align.LEFT
        textP.textSize = 11.2f * d
        textP.color = pal.inkPrimary
        canvas.drawText("SC ${MiniGameEngine.racerScore}  •  G$gear", padX + 4f * d, hudY, textP)

        val bestDisplay = maxOf(MiniGameEngine.racerBest, MiniGameEngine.racerScore)
        subTextP.textAlign = Paint.Align.CENTER
        subTextP.textSize = 9.8f * d
        subTextP.color = when {
            isSlow -> pal.shieldColor
            isNitro -> pal.bonusColor
            isMagnet -> pal.playerColor
            else -> pal.inkSecondary
        }
        val midStatus = when {
            MiniGameEngine.racerPaused -> "PAUSED"
            isSlow -> "SLOW-MO"
            isNitro -> "NITRO x2"
            isMagnet -> "MAGNET"
            else -> "BEST $bestDisplay"
        }
        canvas.drawText(midStatus, w * 0.50f, hudY, subTextP)

        val remMs = MiniGameEngine.getShotCooldownRemainingMs()
        val readyFrac = 1f - (remMs.toFloat() / MiniGameEngine.SHOT_COOLDOWN_MS.toFloat()).coerceIn(0f, 1f)
        val barW = 62f * d
        val barRight = w - padX - 4f * d
        val barLeft = barRight - barW
        val barY = hudY - 4f * d

        rect.set(barLeft, barY - 3.5f * d, barRight, barY + 3.5f * d)
        fillP.color = withAlpha(pal.inkPrimary, 28)
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
        rect.set(barLeft, barY - 3.5f * d, barLeft + barW * readyFrac, barY + 3.5f * d)
        fillP.color = if (remMs == 0L) pal.playerColor else pal.bonusColor
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)

        // רקע הכביש
        rect.set(padX, roadTop, w - padX, roadBot)
        fillP.color = if (isSlow) withAlpha(pal.shieldColor, if (pal.isMinimalist) 28 else 46) else pal.arenaBg
        canvas.drawRoundRect(rect, 14f * d, 14f * d, fillP)

        // שולי מירוץ אדומים-לבנים עליון ותחתון נעים (Racing Curbs)
        val curbShift = (phase * 22f * d) % (20f * d)
        val curbYTop = roadTop + 2.5f * d
        val curbYBot = roadBot - 2.5f * d
        var cxCurb = padX + 10f * d - curbShift
        var toggle = 0
        while (cxCurb < w - padX - 10f * d) {
            val x1 = cxCurb.coerceAtLeast(padX + 8f * d)
            val x2 = (cxCurb + 10f * d).coerceAtMost(w - padX - 8f * d)
            if (x2 > x1) {
                strokeP.strokeWidth = 2.6f * d
                strokeP.color = if (toggle % 2 == 0) withAlpha(pal.enemyColor, 165) else withAlpha(pal.inkPrimary, 55)
                canvas.drawLine(x1, curbYTop, x2, curbYTop, strokeP)
                canvas.drawLine(x1, curbYBot, x2, curbYBot, strokeP)
            }
            cxCurb += 10f * d
            toggle++
        }

        // קווי נתיבים זורמים
        val dashShift = (phase * 24f * d) % (18f * d)
        dashedP.pathEffect = DashPathEffect(floatArrayOf(10f * d, 8f * d), -dashShift)
        dashedP.color = withAlpha(pal.inkSecondary, 115)
        for (div in 1..2) {
            val y = roadTop + laneH * div
            canvas.drawLine(padX + 8f * d, y, w - padX - 8f * d, y, dashedP)
        }

        // קווי מהירות בזמן ניטרו
        if (isNitro) {
            strokeP.strokeWidth = 1.2f * d
            strokeP.color = withAlpha(pal.bonusColor, 130)
            for (s in 0 until 6) {
                val sy = roadTop + 8f * d + ((s * 23f) % (roadH - 16f * d).coerceAtLeast(10f))
                val sx = padX + ((phase * 90f * d + s * 53f * d) % (w - padX * 2 - 28f * d))
                canvas.drawLine(sx, sy, sx + 22f * d, sy, strokeP)
            }
        }

        val innerL = padX + 10f * d
        val innerR = w - padX - 10f * d
        val usableW = (innerR - innerL).coerceAtLeast(60f * d)
        val colW = usableW / 7.2f
        val carX = innerR - colW * 0.65f
        val carY = roadTop + (smoothCarLane + 0.5f) * laneH

        // קרן לייזר
        val laserAge = now - MiniGameEngine.racerLaserTime
        if (laserAge in 0..240L && MiniGameEngine.racerLaserLane in 0..2) {
            val alpha = ((1f - laserAge / 240f) * 255f).toInt().coerceIn(0, 255)
            val ly = roadTop + (MiniGameEngine.racerLaserLane + 0.5f) * laneH
            val endX = innerL + (MiniGameEngine.racerLaserEndCol + 0.5f) * colW
            strokeP.strokeWidth = 4.2f * d
            strokeP.color = withAlpha(pal.laserColor, alpha)
            if (pal.isAurora) strokeP.setShadowLayer(8f * d, 0f, 0f, pal.laserColor)
            canvas.drawLine(carX - 20f * d, ly, endX, ly, strokeP)
            strokeP.clearShadowLayer()
            fillP.color = withAlpha(pal.bonusColor, alpha)
            canvas.drawCircle(endX, ly, (5f + (laserAge / 240f) * 10f) * d, fillP)
        }

        // מכשולים ובונוסים על המסלול
        for (lane in 0..2) {
            val cy = roadTop + (lane + 0.5f) * laneH
            for (col in 0 until MiniGameEngine.racerTracks[lane].size) {
                val cell = MiniGameEngine.racerTracks[lane][col]
                val cx = innerL + (col + 0.5f) * colW
                when (cell) {
                    "🚧" -> drawVectorBarrier(canvas, cx, cy, min(laneH * 0.66f, 22f * d), pal)
                    "🛢️" -> drawVectorBarrel(canvas, cx, cy, min(laneH * 0.64f, 21f * d), pal)
                    "⭐" -> drawVectorStar(canvas, cx, cy, min(laneH * 0.34f, 10.5f * d), pal.bonusColor, pal.inkPrimary)
                    "🛡️" -> drawVectorShieldPickup(canvas, cx, cy, min(laneH * 0.36f, 11f * d), pal.shieldColor)
                    "🧲" -> drawVectorMagnetPickup(canvas, cx, cy, min(laneH * 0.34f, 10f * d), pal.enemyColor, pal.controlAccent)
                }
            }
        }

        // מכונית ספורט וקטורית + בועת מגן + הילת מגנט + שובל ניטרו
        drawVectorSportsCar(canvas, carX, carY, min(laneH * 0.72f, 24f * d), pal, isNitro, MiniGameEngine.racerHasShield, isMagnet)

        // אפקט פיצוץ בעת התרסקות
        val crashAge = now - MiniGameEngine.crashAnimStartTime
        if (MiniGameEngine.crashAnimStartTime > 0L && crashAge in 0..950L) {
            val p = (crashAge / 900f).coerceIn(0f, 1f)
            val ex = carX - 10f * d
            val ey = roadTop + (MiniGameEngine.crashLane + 0.5f) * laneH
            val blastR = (10f + p * 36f) * d
            val alpha = ((1f - p * 0.60f) * 255f).toInt().coerceIn(0, 255)
            fillP.color = withAlpha(pal.enemyColor, (alpha * 0.42f).toInt())
            canvas.drawCircle(ex, ey, blastR, fillP)
            strokeP.strokeWidth = 2.6f * d
            strokeP.color = withAlpha(pal.bonusColor, alpha)
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0 + p * 50.0)
                canvas.drawLine(
                    ex + (cos(a) * blastR * 0.35f).toFloat(),
                    ey + (sin(a) * blastR * 0.35f).toFloat(),
                    ex + (cos(a) * blastR * 1.18f).toFloat(),
                    ey + (sin(a) * blastR * 1.18f).toFloat(),
                    strokeP
                )
            }
            textP.textAlign = Paint.Align.CENTER
            textP.textSize = 15f * d
            textP.color = pal.enemyColor
            canvas.drawText("CRASH!", w * 0.5f, roadTop + roadH * 0.5f + 5f * d, textP)
        }
    }

    private fun drawVectorBarrier(canvas: Canvas, cx: Float, cy: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val bw = h * 0.62f; val bh = h * 0.90f
        rect.set(cx - bw * 0.5f, cy - bh * 0.5f, cx + bw * 0.5f, cy + bh * 0.5f)
        fillP.color = pal.enemyColor
        canvas.drawRoundRect(rect, 3.5f * d, 3.5f * d, fillP)
        strokeP.strokeWidth = 2f * d
        strokeP.color = if (pal.isMinimalist) Color.WHITE else pal.bonusColor
        canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.28f, rect.right - 2f * d, rect.top + bh * 0.14f, strokeP)
        canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.60f, rect.right - 2f * d, rect.top + bh * 0.46f, strokeP)
        canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.88f, rect.right - 2f * d, rect.top + bh * 0.74f, strokeP)
    }

    private fun drawVectorBarrel(canvas: Canvas, cx: Float, cy: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val r = h * 0.44f
        fillP.color = Color.parseColor("#E65100")
        canvas.drawCircle(cx, cy, r, fillP)
        strokeP.strokeWidth = 1.6f * d
        strokeP.color = pal.bonusColor
        canvas.drawCircle(cx, cy, r, strokeP)
        textP.textAlign = Paint.Align.CENTER
        textP.textSize = r * 0.95f
        textP.color = Color.WHITE
        canvas.drawText("!", cx, cy + r * 0.34f, textP)
    }

    private fun drawVectorShieldPickup(canvas: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        val pulse = 1f + sin(phase * 2.4f) * 0.10f
        val sr = r * pulse
        path.reset()
        path.moveTo(cx, cy - sr)
        path.lineTo(cx + sr * 0.82f, cy - sr * 0.55f)
        path.lineTo(cx + sr * 0.82f, cy + sr * 0.12f)
        path.quadTo(cx + sr * 0.82f, cy + sr * 0.78f, cx, cy + sr)
        path.quadTo(cx - sr * 0.82f, cy + sr * 0.78f, cx - sr * 0.82f, cy + sr * 0.12f)
        path.lineTo(cx - sr * 0.82f, cy - sr * 0.55f)
        path.close()
        fillP.color = withAlpha(col, 80)
        canvas.drawPath(path, fillP)
        strokeP.strokeWidth = 1.8f * d
        strokeP.color = col
        canvas.drawPath(path, strokeP)
    }

    private fun drawVectorMagnetPickup(canvas: Canvas, cx: Float, cy: Float, r: Float, col1: Int, col2: Int) {
        rect.set(cx - r * 0.75f, cy - r * 0.75f, cx + r * 0.75f, cy + r * 0.75f)
        strokeP.strokeWidth = 3.2f * d
        strokeP.color = col1
        canvas.drawArc(rect, 180f, 180f, false, strokeP)
        strokeP.color = col2
        canvas.drawLine(cx - r * 0.75f, cy, cx - r * 0.75f, cy + r * 0.65f, strokeP)
        canvas.drawLine(cx + r * 0.75f, cy, cx + r * 0.75f, cy + r * 0.65f, strokeP)
    }

    private fun drawVectorStar(canvas: Canvas, cx: Float, cy: Float, r: Float, fillCol: Int, rimCol: Int) {
        val pulse = 1f + sin(phase * 2.3f) * 0.12f
        val ro = r * pulse; val ri = ro * 0.46f
        path.reset()
        for (i in 0 until 10) {
            val a = Math.toRadians(-90.0 + i * 36.0)
            val rad = if (i % 2 == 0) ro else ri
            val px = cx + (cos(a) * rad).toFloat()
            val py = cy + (sin(a) * rad).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        fillP.color = fillCol
        canvas.drawPath(path, fillP)
        strokeP.strokeWidth = 1.1f * d
        strokeP.color = withAlpha(rimCol, 120)
        canvas.drawPath(path, strokeP)
    }

    private fun drawVectorSportsCar(
        canvas: Canvas, cx: Float, cy: Float, h: Float,
        pal: MiniGameEngine.GameThemePalette,
        nitro: Boolean, shielded: Boolean, magnet: Boolean
    ) {
        val w = h * 1.65f; val halfW = w * 0.5f; val halfH = h * 0.42f

        // שובל אש מאחורי האגזוז (ימין) בזמן ניטרו
        if (nitro) {
            val flame = (10f + sin(phase * 8f) * 5f) * d
            path.reset()
            path.moveTo(cx + halfW, cy - halfH * 0.45f)
            path.lineTo(cx + halfW + flame, cy)
            path.lineTo(cx + halfW, cy + halfH * 0.45f)
            path.close()
            fillP.color = pal.bonusColor
            canvas.drawPath(path, fillP)
        }

        // אלומות פנסים קדימה
        path.reset()
        path.moveTo(cx - halfW, cy - halfH * 0.55f)
        path.lineTo(cx - halfW - 22f * d, cy - halfH * 1.15f)
        path.lineTo(cx - halfW - 22f * d, cy + halfH * 1.15f)
        path.lineTo(cx - halfW, cy + halfH * 0.55f)
        path.close()
        fillP.shader = LinearGradient(cx - halfW, cy, cx - halfW - 22f * d, cy, withAlpha(pal.bonusColor, 78), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(path, fillP)
        fillP.shader = null

        // גלגלים
        fillP.color = pal.inkPrimary
        val wheelW = w * 0.22f; val wheelH = 3.2f * d
        canvas.drawRoundRect(RectF(cx - halfW * 0.72f, cy - halfH - wheelH * 0.5f, cx - halfW * 0.72f + wheelW, cy - halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx + halfW * 0.26f, cy - halfH - wheelH * 0.5f, cx + halfW * 0.26f + wheelW, cy - halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx - halfW * 0.72f, cy + halfH - wheelH * 0.5f, cx - halfW * 0.72f + wheelW, cy + halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx + halfW * 0.26f, cy + halfH - wheelH * 0.5f, cx + halfW * 0.26f + wheelW, cy + halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)

        // שלדת המכונית
        rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        fillP.color = if (nitro) pal.bonusColor else pal.playerColor
        canvas.drawRoundRect(rect, halfH * 0.72f, halfH * 0.72f, fillP)

        // שמשה ותא נהג
        rect.set(cx - halfW * 0.30f, cy - halfH * 0.66f, cx + halfW * 0.38f, cy + halfH * 0.66f)
        fillP.color = withAlpha(pal.cardBgTop, 218)
        canvas.drawRoundRect(rect, halfH * 0.45f, halfH * 0.45f, fillP)

        // ספוילר ופנסים
        strokeP.strokeWidth = 2.4f * d; strokeP.color = pal.controlAccent
        canvas.drawLine(cx + halfW - 2f * d, cy - halfH * 0.85f, cx + halfW - 2f * d, cy + halfH * 0.85f, strokeP)
        fillP.color = pal.bonusColor
        canvas.drawCircle(cx - halfW + 2.5f * d, cy - halfH * 0.45f, 2f * d, fillP)
        canvas.drawCircle(cx - halfW + 2.5f * d, cy + halfH * 0.45f, 2f * d, fillP)

        // בועת מגן אנרגיה מסתובבת
        if (shielded) {
            rect.set(cx - halfW - 5f * d, cy - halfH - 5f * d, cx + halfW + 5f * d, cy + halfH + 5f * d)
            fillP.color = withAlpha(pal.shieldColor, 36)
            canvas.drawOval(rect, fillP)
            strokeP.strokeWidth = 2.0f * d; strokeP.color = pal.shieldColor
            canvas.drawArc(rect, phase * 90f, 260f, false, strokeP)
        }

        // הילת מגנט
        if (magnet) {
            val magR = halfW + 7f * d + sin(phase * 4f) * 3f * d
            strokeP.strokeWidth = 1.4f * d; strokeP.color = withAlpha(pal.bonusColor, 155)
            canvas.drawCircle(cx, cy, magR, strokeP)
        }
    }

    // =========================================================================
    // 2. איקס-עיגול Arena Pro (1337)
    // =========================================================================
    private fun drawVectorTicTacToe(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val boardSize = (h - 20f * d).coerceAtMost(w * 0.45f).coerceAtLeast(60f * d)
        val boardLeft = 16f * d
        val boardTop = (h - boardSize) * 0.5f
        val cellSize = boardSize / 3f
        val now = System.currentTimeMillis()

        rect.set(boardLeft - 4f * d, boardTop - 4f * d, boardLeft + boardSize + 4f * d, boardTop + boardSize + 4f * d)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 12f * d, 12f * d, fillP)

        fun cellCenter(idx: Int): Pair<Float, Float> {
            val col = idx % 3
            val row = 2 - (idx / 3)
            return (boardLeft + (col + 0.5f) * cellSize) to (boardTop + (row + 0.5f) * cellSize)
        }

        // הדגשת מהלך אחרון או רמז חכם
        for (idx in 0..8) {
            val (cx, cy) = cellCenter(idx)
            val isHint = (idx == MiniGameEngine.tttHintIdx && now - MiniGameEngine.tttHintTime < 3500L)
            val isLast = (idx == MiniGameEngine.tttLastMoveIdx)
            if (isHint || isLast) {
                rect.set(cx - cellSize * 0.44f, cy - cellSize * 0.44f, cx + cellSize * 0.44f, cy + cellSize * 0.44f)
                val alpha = if (isHint) (45 + ((sin(phase * 5f) * 0.5f + 0.5f) * 75).toInt()) else 26
                fillP.color = withAlpha(if (isHint) pal.bonusColor else pal.controlAccent, alpha)
                canvas.drawRoundRect(rect, 6f * d, 6f * d, fillP)
            }
        }

        strokeP.strokeWidth = 1.5f * d; strokeP.color = withAlpha(pal.inkPrimary, 52)
        for (i in 1..2) {
            canvas.drawLine(boardLeft + cellSize * i, boardTop + 4f * d, boardLeft + cellSize * i, boardTop + boardSize - 4f * d, strokeP)
            canvas.drawLine(boardLeft + 4f * d, boardTop + cellSize * i, boardLeft + boardSize - 4f * d, boardTop + cellSize * i, strokeP)
        }

        val markR = cellSize * 0.28f
        for (idx in 0..8) {
            val (cx, cy) = cellCenter(idx)
            val animT = if (MiniGameEngine.tttPlacedAt[idx] > 0L) {
                ((now - MiniGameEngine.tttPlacedAt[idx]) / 220f).coerceIn(0.15f, 1f)
            } else 1f
            when (val mark = MiniGameEngine.tttBoard[idx]) {
                "×" -> {
                    val r = markR * animT
                    strokeP.strokeWidth = 3.0f * d; strokeP.color = pal.playerColor
                    if (pal.isAurora) strokeP.setShadowLayer(6f * d, 0f, 0f, pal.playerColor)
                    canvas.drawLine(cx - r, cy - r, cx + r, cy + r, strokeP)
                    canvas.drawLine(cx + r, cy - r, cx - r, cy + r, strokeP)
                    strokeP.clearShadowLayer()
                }
                "0" -> {
                    strokeP.strokeWidth = 3.0f * d; strokeP.color = pal.enemyColor
                    if (pal.isAurora) strokeP.setShadowLayer(6f * d, 0f, 0f, pal.enemyColor)
                    rect.set(cx - markR, cy - markR, cx + markR, cy + markR)
                    canvas.drawArc(rect, -90f, 360f * animT, false, strokeP)
                    strokeP.clearShadowLayer()
                }
                else -> {
                    subTextP.textAlign = Paint.Align.CENTER
                    subTextP.textSize = cellSize * 0.34f
                    subTextP.color = withAlpha(pal.inkSecondary, 145)
                    canvas.drawText(mark, cx, cy + cellSize * 0.12f, subTextP)
                }
            }
        }

        MiniGameEngine.tttWinningLine?.let { line ->
            if (line.size == 3) {
                val (x1, y1) = cellCenter(line[0])
                val (x2, y2) = cellCenter(line[2])
                strokeP.strokeWidth = 4.2f * d; strokeP.color = pal.bonusColor
                canvas.drawLine(x1, y1, x2, y2, strokeP)
            }
        }

        // פאנל לוח תוצאות ואפשרויות מימין
        val rightCx = (boardLeft + boardSize + w - 10f * d) * 0.5f
        val modeLabel = when {
            MiniGameEngine.tttPvpMode -> "מצב 2 שחקנים (÷)"
            MiniGameEngine.tttDifficulty == 0 -> "רמה: קל (0)"
            MiniGameEngine.tttDifficulty == 1 -> "רמה: בינוני (0)"
            else -> "רמה: בלתי מנוצח (0)"
        }
        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 11.8f * d; textP.color = pal.controlAccent
        canvas.drawText(modeLabel, rightCx, h * 0.22f, textP)

        // לוח ניקוד (× | תיקו | 0)
        textP.textSize = 13f * d
        textP.color = pal.playerColor
        canvas.drawText("× ${MiniGameEngine.tttWinsX}", rightCx + 42f * d, h * 0.44f, textP)
        textP.color = pal.inkSecondary
        canvas.drawText("= ${MiniGameEngine.tttTies}", rightCx, h * 0.44f, textP)
        textP.color = pal.enemyColor
        canvas.drawText("0 ${MiniGameEngine.tttWinsO}", rightCx - 42f * d, h * 0.44f, textP)

        textP.textSize = 12f * d; textP.color = pal.inkPrimary
        canvas.drawText(MiniGameEngine.tttStatusText, rightCx, h * 0.67f, textP)

        subTextP.textAlign = Paint.Align.CENTER
        subTextP.textSize = 9.5f * d; subTextP.color = pal.inkSecondary
        canvas.drawText("+ רמז  •  0 רמה  •  ÷ 2P  •  AC יציאה", rightCx, h * 0.87f, subTextP)
    }

    // =========================================================================
    // 3. פולשי החשבון Galactic Pro (8080)
    // =========================================================================
    private fun drawVectorInvaders(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val now = System.currentTimeMillis()
        val padX = 12f * d
        val hudY = 19f * d
        val arenaTop = 28f * d
        val arenaBot = h - 10f * d
        val arenaH = (arenaBot - arenaTop).coerceAtLeast(36f * d)
        val cy = arenaTop + arenaH * 0.5f

        // HUD: ניקוד + גל + לבבות + מד EMP
        textP.textAlign = Paint.Align.LEFT
        textP.textSize = 11.2f * d; textP.color = pal.inkPrimary
        canvas.drawText("SC ${MiniGameEngine.invaderScore}  W${MiniGameEngine.invaderWave}", padX + 4f * d, hudY, textP)

        // לבבות חיים במרכז
        val heartCx = w * 0.50f
        for (i in 0 until 3) {
            val hx = heartCx + (i - 1) * 14f * d
            fillP.color = if (i < MiniGameEngine.invaderLives) pal.enemyColor else withAlpha(pal.inkSecondary, 55)
            canvas.drawCircle(hx - 2.3f * d, hudY - 5.5f * d, 3.0f * d, fillP)
            canvas.drawCircle(hx + 2.3f * d, hudY - 5.5f * d, 3.0f * d, fillP)
            path.reset()
            path.moveTo(hx - 5.2f * d, hudY - 4.5f * d)
            path.lineTo(hx + 5.2f * d, hudY - 4.5f * d)
            path.lineTo(hx, hudY + 1.5f * d)
            path.close()
            canvas.drawPath(path, fillP)
        }

        // מד EMP בימין
        val empBarW = 54f * d
        val empR = w - padX - 4f * d
        val empL = empR - empBarW
        val empFrac = (MiniGameEngine.invaderEmpCharge / 6f).coerceIn(0f, 1f)
        rect.set(empL, hudY - 7.5f * d, empR, hudY - 0.5f * d)
        fillP.color = withAlpha(pal.inkPrimary, 28)
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
        rect.set(empL, hudY - 7.5f * d, empL + empBarW * empFrac, hudY - 0.5f * d)
        fillP.color = if (empFrac >= 1f) pal.laserColor else pal.bonusColor
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)

        // זירת החלל (Parallax Starfield)
        rect.set(padX, arenaTop, w - padX, arenaBot)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 14f * d, 14f * d, fillP)

        val spanX = (w - padX * 2 - 20f * d).coerceAtLeast(20f)
        for (s in 0 until 14) {
            val layerSpeed = if (s % 2 == 0) 14f else 7f
            val sx = padX + 10f * d + ((((s * 41f * d - phase * layerSpeed * d) % spanX) + spanX) % spanX)
            val sy = arenaTop + 7f * d + ((s * 29f) % (arenaH - 14f * d).coerceAtLeast(10f))
            fillP.color = withAlpha(pal.inkSecondary, if (s % 2 == 0) 105 else 55)
            canvas.drawCircle(sx, sy, if (s % 2 == 0) 1.7f * d else 1.1f * d, fillP)
        }

        // גל הדף EMP
        val empAge = now - MiniGameEngine.invaderEmpWaveTime
        if (empAge in 0..450L) {
            val ep = empAge / 450f
            strokeP.strokeWidth = 3.5f * d
            strokeP.color = withAlpha(pal.laserColor, ((1f - ep) * 240f).toInt())
            canvas.drawCircle(padX + 26f * d, cy, ep * w * 0.95f, strokeP)
        }

        // חללית וקטורית משמאל
        val shipX = padX + 24f * d
        val shipR = min(arenaH * 0.30f, 16f * d)
        val flameLen = (7f + sin(phase * 5f) * 3.5f) * d
        fillP.color = pal.bonusColor
        path.reset()
        path.moveTo(shipX - shipR * 0.8f, cy - shipR * 0.36f)
        path.lineTo(shipX - shipR * 0.8f - flameLen, cy)
        path.lineTo(shipX - shipR * 0.8f, cy + shipR * 0.36f)
        path.close()
        canvas.drawPath(path, fillP)

        path.reset()
        path.moveTo(shipX + shipR * 1.18f, cy)
        path.lineTo(shipX - shipR * 0.85f, cy - shipR * 0.84f)
        path.lineTo(shipX - shipR * 0.42f, cy)
        path.lineTo(shipX - shipR * 0.85f, cy + shipR * 0.84f)
        path.close()
        fillP.color = pal.playerColor
        canvas.drawPath(path, fillP)

        val trackL = shipX + shipR + 14f * d
        val trackR = w - padX - 10f * d
        val colW = ((trackR - trackL) / 6f).coerceAtLeast(20f * d)

        // קרן יירוט
        val laserAge = now - MiniGameEngine.invaderLaserTime
        if (laserAge in 0..220L && MiniGameEngine.invaderLaserCol in 0..5) {
            val alpha = ((1f - laserAge / 220f) * 255f).toInt().coerceIn(0, 255)
            val tx = trackL + (MiniGameEngine.invaderLaserCol + 0.5f) * colW
            strokeP.strokeWidth = 3.5f * d; strokeP.color = withAlpha(pal.laserColor, alpha)
            canvas.drawLine(shipX + shipR, cy, tx, cy, strokeP)
        שדרגתי את כל 5 המיני-משחקים בקובץ **`MiniGames.kt`** למנוע ארקייד וקטורי מלא ב-60fps — עם **שיאים שמורים (High Scores)** לכל משחק, **מערכת חלקיקים, פיצוצים וטקסטים מרחפים (`+10`, `COMBO ×3`, `SHIELD!`)**, מכניקות עומק חדשות, והתאמה מלאה לכל 4 ערכות הנושא (מינימליסטי, זכוכית, זוהר צפוני וקלאסי) — ללא צורך לשנות אף שורה ב-`MainActivity.kt` או ב-`EasterEggs.kt`.

### מה שודרג בכל משחק

* **🏎️ מירוץ ארקייד (`5555`):**
  * **חיים, מגן ובוסים בדרך:** 2 לבבות התחלתיים (ועד 3), איסוף **מגן אנרגיה (`🛡️`)** שסופג התנגשות עם אפקט התנפצות, **לב תיקון (`❤️`)**, **מחסומים משוריינים** (דורשים 2 יריות) ו**כוכבי קומבו (`⭐`)** שמכפילים ניקוד (`×2`–`×4`).
  * **טורבו (`6`/`+`) וגל הלם EMP (`0`/`=`):** טורבו מצייר פסי מהירות ולהבת אגזוז כפולה; טעינת מד ה-EMP מוחקת את כל המחסומים ב-3 הנתיבים עם טבעת אנרגיה מתפשטת.
  * **גרפיקה וסאונד:** מכונית ספורט וקטורית שנוטה בזווית בעת החלפת נתיב, הילת מגן נושמת, קרן פלזמה, גיצים, ושימור מלא של צליל ההתרסקות מהתיקייה עד לסיום הניגון.
* **🎮 איקס-עיגול טקטי (`1337`):**
  * **4 מצבי משחק חיים מהמקלדת:** `+` (קל), `−` (בינוני), `×` (בלתי מנוצח — Minimax), ו-`÷` (**2 שחקנים** זה נגד זה על אותה מקלדת).
  * **רמז חכם (`0`) ולוח תוצאות:** לחיצה על `0` מדליקה טבעת הבהוב על המהלך האופטימלי, עם מונה ניצחונות, רצף ושיא.
  * **גרפיקה:** ציור מונפש חלק של `×` ו-`0`, קו ניצחון מתארך בזמן אמת וקונפטי וקטורי.
* **🚀 פולשי החשבון (`8080` — מחשבון מדעי):**
  * **גלים, בוסים ומגן חללית:** 3 נקודות שריון לחללית, התקדמות בגלים, ו**ספינת בוס** כל 80 נקודות עם מד חיים שדורשת רצף פגיעות ומחזירה שריון.
  * **קומבו ופיצוץ נובה (`=`):** פגיעות רצופות בלי פספוס טוענות גל **NOVA (`=`)** שמשמיד את כל הפולשים על המסך.
  * **גרפיקה:** רקע כוכבים בשתי שכבות פרלקסה, חללית עם רתיעה (Recoil) ולהבת מנוע, רחפנים משושים וקרני לייזר.
* **💣 נטרול פצצה מתקדם (`1010`):**
  * **שלבי קושי והקפאת זמן (`=`):** מעבר ממשוואות פשוטות למשוואות עם מספרים גדולים יותר; 3 נטרולים רצופים טוענים **הקפאת שעון ל-5 שניות (`=`)** עם אפקט כפור על הפצצה.
  * **גרפיקה:** פצצה טקטית עם פתיל מתקצר וניצוצות, דופק LED מואץ מתחת ל-5 שניות ו-4 חוטי אופרטורים צבעוניים עם אפקט חיתוך.
* **🐾 תזמורת חיות + אתגר זיכרון (`7777`):**
  * **2 מצבים:** מצב חופשי שבו כל מקש `1–9` מנגן תו ייחודי ומקפיץ חיה וקטורית עם תווי מוזיקה מרחפים (`0` = מקהלה של כולן יחד), ומצב **אתגר זיכרון (`=`, Simon Says)** שבו החיות מדגימות רצף צלילים וצריך לחזור עליו במקשים `1–5` לצבירת שיא.

---

### החלף את הקובץ `MiniGames.kt` בקוד המלא הבא

```kotlin
package com.example.calculator

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.Toast
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*
import kotlin.random.Random

object MiniGameEngine {

    internal data class GameThemePalette(
        val cardBgTop: Int,
        val cardBgBottom: Int,
        val cardBorder: Int,
        val arenaBg: Int,
        val arenaGrid: Int,
        val inkPrimary: Int,
        val inkSecondary: Int,
        val playerColor: Int,
        val enemyColor: Int,
        val bonusColor: Int,
        val shieldColor: Int,
        val controlAccent: Int,
        val laserColor: Int,
        val glowColor: Int,
        val isMinimalist: Boolean,
        val isAurora: Boolean,
        val isGlass: Boolean
    )

    internal data class FxParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var maxLife: Float,
        var size: Float,
        var color: Int,
        var isStar: Boolean = false
    )

    internal data class FloatingPopup(
        var text: String,
        var xFrac: Float,
        var yFrac: Float,
        var color: Int,
        var createdAt: Long = System.currentTimeMillis()
    )

    var isActive = false
    internal var currentGame = ""
    private val handler = Handler(Looper.getMainLooper())
    private var gameLoop: Runnable? = null
    private var cooldownTicker: Runnable? = null
    private var roundResetRunnable: Runnable? = null
    private var gameOverDelayRunnable: Runnable? = null
    private var simonStepRunnable: Runnable? = null
    private var gameEnding = false

    // מארח הקנבס הווקטורי בתוך displayCard
    private var vectorStageView: MiniGameVectorStageView? = null
    private var attachedDisplayCard: LinearLayout? = null
    private var savedDisplayPadding: IntArray? = null

    // אפקטים גלובליים (חלקיקים וטקסטים קופצים)
    internal val particles = mutableListOf<FxParticle>()
    internal val popups = mutableListOf<FloatingPopup>()

    // אודיו — הפרדה מוחלטת בין לולאת המנוע לבין אפקטים חד-פעמיים (לשימור צליל התרסקות)
    private var engineLoopPlayer: MediaPlayer? = null
    private var currentEngineStage = 0
    private var sfxPlayer: MediaPlayer? = null
    private val activeSynthThreads = AtomicInteger(0)
    @Volatile private var synthEngineRunning = false

    // --- 1. איקס-עיגול טקטי (1337) ---
    internal val tttBoard = Array(9) { "" }
    internal val tttCellAnimStart = LongArray(9) { 0L }
    internal var tttStatusText = ""
    internal var tttWinningLine: IntArray? = null
    internal var tttWinAnimStart = 0L
    internal var tttMode = 2 // 0=קל, 1=בינוני, 2=בלתי מנוצח, 3=2 שחקנים
    internal var tttCurrentTurn = "×"
    internal var tttPlayerWins = 0
    internal var tttCpuWins = 0
    internal var tttDraws = 0
    internal var tttStreak = 0
    internal var tttHintCell = -1
    internal var tttHintTime = 0L
    private var tttRoundLocked = false

    // --- 2. פולשי החשבון (8080) ---
    internal var invaderScore = 0
    internal var invaderBest = 0
    internal var invaderHp = 3
    internal var invaderWave = 1
    internal var invaderCombo = 0
    internal var invaderNovaCharge = 0 // מגיע ל-5 -> גל NOVA במקש '='
    internal var invaderNovaTime = 0L
    internal var invaderTrack = mutableListOf<String>()
    internal var invaderBossActive = false
    internal var invaderBossHp = 0
    internal var invaderBossMaxHp = 3
    internal var invaderBossSymbol = "sin"
    internal var invaderLaserCol = -1
    internal var invaderLaserTime = 0L
    internal var invaderShipRecoilTime = 0L
    internal var invaderHitFlashTime = 0L
    private val invaderSymbols = listOf("+", "−", "×", "÷", "sin", "cos", "tan", "√")

    // --- 3. מירוץ ארקייד (5555) ---
    internal var carLane = 1 // 0=עליון(8), 1=אמצעי, 2=תחתון(2)
    internal var racerScore = 0
    internal var racerBest = 0
    internal var racerLives = 2
    internal var racerShieldActive = false
    internal var racerCombo = 1
    internal var racerStreak = 0
    internal var racerEmpCharge = 0 // מגיע ל-5 -> EMP במקש 0 או '='
    internal var racerEmpTime = 0L
    internal var racerTurboBoostUntil = 0L
    internal var racerInvulnUntil = 0L
    internal var racerShieldBreakTime = 0L
    internal val racerTracks = Array(3) { MutableList(5) { "." } }
    internal var lastShotTime = 0L
    internal var racerLaserLane = -1
    internal var racerLaserEndCol = 0
    internal var racerLaserTime = 0L
    internal var crashAnimStartTime = 0L
    internal var crashLane = 1
    private var lastCooldownToastTime = 0L
    internal const val SHOT_COOLDOWN_MS = 2600L
    private var pendingGuardedStarLane = -1

    // --- 4. נטרול פצצה מתקדם (1010) ---
    internal var bombTimeLeft = 15
    internal var bombMaxTime = 20
    internal var bombScore = 0
    internal var bombBest = 0
    internal var bombStreak = 0
    internal var bombFreezeCharges = 0
    internal var bombFrozenUntil = 0L
    internal var bombLhs = 2
    internal var bombRhs = 3
    internal var bombTarget = 6
    internal var bombLastCutOp = ""
    internal var bombLastCutCorrect = true
    internal var bombLastCutTime = 0L
    private var expectedOp = "×"

    // --- 5. תזמורת חיות + אתגר זיכרון Simon Says (7777) ---
    var isAnimalParty = false
    internal var animalTimeLeft = 25
    internal var animalTotalTime = 25
    internal var animalActiveIdx = 0
    internal var animalBounceTime = 0L
    internal var animalChorusTime = 0L
    internal var animalSimonMode = false
    internal var animalSimonShowing = false
    internal var animalSimonScore = 0
    internal var animalSimonBest = 0
    internal val animalSimonSeq = mutableListOf<Int>()
    internal var animalSimonInputPos = 0
    private val savedKeyLabels = mutableMapOf<String, CharSequence>()
    private val animalEmojis = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
        "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔"
    )
    private val animalNotes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.0, 987.77, 1046.5, 1174.66, 1318.5)

    // =========================================================================
    // שיאים שמורים (High Scores) ב-SharedPreferences
    // =========================================================================
    private fun getHighScore(activity: MainActivity, gameKey: String): Int =
        activity.prefs?.getInt("minigame_best_$gameKey", 0) ?: 0

    private fun updateHighScore(activity: MainActivity, gameKey: String, score: Int): Int {
        val prev = getHighScore(activity, gameKey)
        if (score > prev) {
            activity.prefs?.edit()?.putInt("minigame_best_$gameKey", score)?.apply()
            return score
        }
        return prev
    }

    // =========================================================================
    // מערכת חלקיקים ופופאפים
    // =========================================================================
    internal fun spawnPopup(text: String, xFrac: Float, yFrac: Float, color: Int) {
        if (popups.size >= 10) popups.removeAt(0)
        popups.add(FloatingPopup(text, xFrac, yFrac, color, System.currentTimeMillis()))
    }

    internal fun spawnBurstNormalized(xFrac: Float, yFrac: Float, color: Int, count: Int = 12, isStar: Boolean = false) {
        val stage = vectorStageView ?: return
        val w = stage.width.toFloat().takeIf { it > 10f } ?: return
        val h = stage.height.toFloat().takeIf { it > 10f } ?: return
        val d = stage.resources.displayMetrics.density
        val cx = w * xFrac
        val cy = h * yFrac
        repeat(count) {
            if (particles.size >= 56) particles.removeAt(0)
            val ang = Random.nextFloat() * (2f * PI.toFloat())
            val spd = (1.4f + Random.nextFloat() * 3.8f) * d
            val maxL = 0.55f + Random.nextFloat() * 0.45f
            particles.add(
                FxParticle(
                    x = cx, y = cy,
                    vx = cos(ang) * spd, vy = sin(ang) * spd,
                    life = maxL, maxLife = maxL,
                    size = (2.2f + Random.nextFloat() * 2.8f) * d,
                    color = color,
                    isStar = isStar
                )
            )
        }
    }

    // =========================================================================
    // פלטת צבעים וקטורית מותאמת לכל 4 ערכות הנושא
    // =========================================================================
    internal fun resolvePalette(activity: MainActivity): GameThemePalette {
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode

        return when {
            isMin -> GameThemePalette(
                cardBgTop = MinimalistTheme.CARD_WHITE,
                cardBgBottom = MinimalistTheme.BG_CREAM,
                cardBorder = MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#DDD6CC")),
                arenaBg = Color.parseColor("#F1ECE3"),
                arenaGrid = Color.argb(30, 44, 44, 44),
                inkPrimary = MinimalistTheme.TEXT_CHARCOAL,
                inkSecondary = MinimalistTheme.TEXT_MUTED_GREY,
                playerColor = Color.parseColor("#2E7D32"),
                enemyColor = Color.parseColor("#C62828"),
                bonusColor = Color.parseColor("#C67C00"),
                shieldColor = Color.parseColor("#0277BD"),
                controlAccent = Color.parseColor("#1976D2"),
                laserColor = Color.parseColor("#00897B"),
                glowColor = Color.TRANSPARENT,
                isMinimalist = true,
                isAurora = false,
                isGlass = false
            )
            isAur -> GameThemePalette(
                cardBgTop = AuroraBorealisTheme.EMERALD_NIGHT,
                cardBgBottom = Color.parseColor("#071822"),
                cardBorder = Color.argb(190, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY)),
                arenaBg = Color.argb(180, 4, 14, 22),
                arenaGrid = Color.argb(40, 0, 245, 212),
                inkPrimary = Color.WHITE,
                inkSecondary = AuroraBorealisTheme.EXPRESSION_CYAN_GREY,
                playerColor = Color.parseColor("#00F5D4"),
                enemyColor = Color.parseColor("#FF4D8D"),
                bonusColor = Color.parseColor("#FFD166"),
                shieldColor = Color.parseColor("#38BDF8"),
                controlAccent = Color.parseColor("#38BDF8"),
                laserColor = Color.parseColor("#00F5D4"),
                glowColor = AuroraBorealisTheme.AURORA_PRIMARY,
                isMinimalist = false,
                isAurora = true,
                isGlass = false
            )
            isGlass -> GameThemePalette(
                cardBgTop = Color.argb(85, 255, 255, 255),
                cardBgBottom = Color.argb(34, 255, 255, 255),
                cardBorder = Color.argb(145, 255, 255, 255),
                arenaBg = Color.argb(66, 12, 20, 36),
                arenaGrid = Color.argb(38, 255, 255, 255),
                inkPrimary = Color.WHITE,
                inkSecondary = Color.parseColor("#CBD5E1"),
                playerColor = Color.parseColor("#34D399"),
                enemyColor = Color.parseColor("#FB7185"),
                bonusColor = Color.parseColor("#FBBF24"),
                shieldColor = Color.parseColor("#60A5FA"),
                controlAccent = Color.parseColor("#7DD3FC"),
                laserColor = Color.parseColor("#38BDF8"),
                glowColor = Color.argb(115, 255, 255, 255),
                isMinimalist = false,
                isAurora = false,
                isGlass = true
            )
            else -> {
                val baseBg = Utils.lightenColor(activity.bgColor, 0.06f)
                val accent = MinimalistTheme.ensureReadableTextOnBg(activity.primaryColor, baseBg, 2.2)
                GameThemePalette(
                    cardBgTop = Utils.lightenColor(activity.bgColor, 0.09f),
                    cardBgBottom = Utils.lightenColor(activity.bgColor, 0.04f),
                    cardBorder = Color.argb(140, Color.red(accent), Color.green(accent), Color.blue(accent)),
                    arenaBg = activity.bgColor,
                    arenaGrid = Color.argb(34, Color.red(accent), Color.green(accent), Color.blue(accent)),
                    inkPrimary = Color.WHITE,
                    inkSecondary = Color.parseColor("#94A3B8"),
                    playerColor = Color.parseColor("#4ADE80"),
                    enemyColor = Color.parseColor("#F87171"),
                    bonusColor = Color.parseColor("#FACC15"),
                    shieldColor = Color.parseColor("#38BDF8"),
                    controlAccent = accent,
                    laserColor = accent,
                    glowColor = Color.TRANSPARENT,
                    isMinimalist = false,
                    isAurora = false,
                    isGlass = false
                )
            }
        }
    }

    private fun isActivityValid(activity: MainActivity): Boolean =
        !activity.isFinishing && !activity.isDestroyed

    // =========================================================================
    // הטמעה והסרה נקייה של מסך המשחק הווקטורי בתוך displayCard
    // =========================================================================
    private fun attachVectorStage(activity: MainActivity) {
        detachVectorStage(activity)
        val displayCard = activity.currentView?.parent as? LinearLayout ?: return
        attachedDisplayCard = displayCard
        savedDisplayPadding = intArrayOf(
            displayCard.paddingLeft,
            displayCard.paddingTop,
            displayCard.paddingRight,
            displayCard.paddingBottom
        )

        activity.memoryIndicator?.visibility = View.GONE
        activity.expressionView?.visibility = View.GONE
        activity.currentView?.visibility = View.GONE

        val pad = activity.dp(4f)
        displayCard.setPadding(pad, pad, pad, pad)

        val stage = MiniGameVectorStageView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        vectorStageView = stage
        displayCard.addView(stage)
    }

    private fun detachVectorStage(activity: MainActivity) {
        val stage = vectorStageView
        vectorStageView = null
        if (stage != null) {
            (stage.parent as? ViewGroup)?.removeView(stage)
        }
        val card = attachedDisplayCard
        val pads = savedDisplayPadding
        attachedDisplayCard = null
        savedDisplayPadding = null
        if (card != null && pads != null && pads.size == 4) {
            card.setPadding(pads[0], pads[1], pads[2], pads[3])
        }
        activity.memoryIndicator?.visibility = View.VISIBLE
        activity.expressionView?.visibility = View.VISIBLE
        activity.currentView?.visibility = View.VISIBLE
    }

    private fun invalidateStage() {
        vectorStageView?.invalidate()
    }

    private fun styleKeyForGame(
        activity: MainActivity,
        symbol: String,
        desiredColor: Int,
        labelOverride: CharSequence? = null,
        glow: Boolean = false
    ) {
        val btn = activity.keypadButtons[symbol] ?: return
        if (labelOverride != null) btn.text = labelOverride
        val keyBg = MinimalistTheme.resolveKeypadButtonBgColor(activity, symbol)
        val safeCol = MinimalistTheme.ensureReadableTextOnBg(desiredColor, keyBg, 2.4)
        btn.setTextColor(safeCol)
        if (glow && activity.isAuroraMode) {
            btn.setShadowLayer(
                8f, 0f, 0f,
                Color.argb(170, Color.red(safeCol), Color.green(safeCol), Color.blue(safeCol))
            )
        } else if (!activity.isIosGlassMode) {
            btn.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        }
    }

    private fun saveKeysLabels(activity: MainActivity, keys: List<String>) {
        for (k in keys) {
            if (!savedKeyLabels.containsKey(k)) {
                activity.keypadButtons[k]?.let { savedKeyLabels[k] = it.text }
            }
        }
    }

    private fun restoreAllKeyColorsAndLabels(activity: MainActivity) {
        for ((k, text) in savedKeyLabels) {
            activity.keypadButtons[k]?.text = text
        }
        savedKeyLabels.clear()
        for ((sym, btn) in activity.keypadButtons) {
            val bg = MinimalistTheme.resolveKeypadButtonBgColor(activity, sym)
            btn.setTextColor(MinimalistTheme.resolveKeypadSymbolTextColor(activity, sym, bg))
            if (!activity.isIosGlassMode && !activity.isAuroraMode) {
                btn.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }
    }

    private fun prepareNewGame(activity: MainActivity, gameId: String) {
        stopPreviousLoop()
        stopRacerAudio()
        releaseSfxPlayerSafely()
        restoreAllKeyColorsAndLabels(activity)
        particles.clear()
        popups.clear()
        isAnimalParty = false
        tttRoundLocked = false
        tttWinningLine = null
        tttHintCell = -1
        gameEnding = false
        crashAnimStartTime = 0L
        racerLaserTime = 0L
        racerEmpTime = 0L
        invaderLaserTime = 0L
        invaderNovaTime = 0L
        bombLastCutTime = 0L
        isActive = true
        currentGame = gameId
        attachVectorStage(activity)
    }

    fun stopGame(activity: MainActivity, message: String = "יצאת מהמשחק 🎮") {
        stopGameInternal(activity, message, stopSfx = true)
    }

    private fun stopGameInternal(activity: MainActivity, message: String, stopSfx: Boolean) {
        val wasActive = isActive
        isActive = false
        gameEnding = false
        currentGame = ""
        isAnimalParty = false
        tttRoundLocked = false
        stopPreviousLoop()
        stopRacerAudio()
        if (stopSfx) releaseSfxPlayerSafely()
        particles.clear()
        popups.clear()

        if (!isActivityValid(activity)) return
        detachVectorStage(activity)
        restoreAllKeyColorsAndLabels(activity)
        activity.currentInput = ""
        activity.justCalculated = false
        activity.applyTheme()
        if (wasActive && message.isNotBlank()) {
            val cleanMsg = if (activity.isCleanGlassActive) {
                IosGlassTheme.cleanCategoryTitle(message, true)
            } else message
            Toast.makeText(activity, cleanMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // =========================================================================
    // מערכת אודיו חכמה (חיבור אוטומטי לכל קובץ ב-res/raw + גיבוי סינתטי)
    // =========================================================================
    private fun findRawResId(
        activity: MainActivity,
        exactCandidates: List<String>,
        keywords: List<String> = emptyList(),
        exclude: List<String> = emptyList()
    ): Int {
        for (name in exactCandidates) {
            val id = activity.resources.getIdentifier(name, "raw", activity.packageName)
            if (id != 0) return id
        }
        if (keywords.isEmpty()) return 0
        try {
            val rawClass = Class.forName("${activity.packageName}.R\$raw")
            for (field in rawClass.fields) {
                val fname = field.name.lowercase(Locale.US)
                if (exclude.any { fname.contains(it) }) continue
                if (keywords.any { fname.contains(it) }) {
                    val resId = field.getInt(null)
                    if (resId != 0) return resId
                }
            }
        } catch (_: Throwable) {}
        return 0
    }

    private fun releaseSfxPlayerSafely() {
        val p = sfxPlayer
        sfxPlayer = null
        if (p != null) {
            try { p.setOnCompletionListener(null) } catch (_: Throwable) {}
            try { if (p.isPlaying) p.stop() } catch (_: Throwable) {}
            try { p.release() } catch (_: Throwable) {}
        }
    }

    private fun playRawSfx(activity: MainActivity, resId: Int, volume: Float = 1.0f): Boolean {
        if (resId == 0 || !isActivityValid(activity)) return false
        return try {
            releaseSfxPlayerSafely()
            val mp = MediaPlayer.create(activity.applicationContext, resId) ?: return false
            sfxPlayer = mp
            mp.setVolume(volume, volume)
            mp.setOnCompletionListener { player ->
                if (sfxPlayer === player) sfxPlayer = null
                try { player.release() } catch (_: Throwable) {}
            }
            mp.setOnErrorListener { player, _, _ ->
                if (sfxPlayer === player) sfxPlayer = null
                try { player.release() } catch (_: Throwable) {}
                true
            }
            mp.start()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun playStaticPcmAsync(sampleRate: Int, buffer: ShortArray, durationMs: Long) {
        if (activeSynthThreads.get() >= 3) return
        activeSynthThreads.incrementAndGet()
        Thread {
            var track: AudioTrack? = null
            try {
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(2048)
                track = AudioTrack.Builder()
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
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs + 30L)
            } catch (_: Throwable) {
            } finally {
                try { track?.stop() } catch (_: Throwable) {}
                try { track?.release() } catch (_: Throwable) {}
                activeSynthThreads.decrementAndGet()
            }
        }.start()
    }

    private fun playSynthTone(freqStart: Double, freqEnd: Double, durationMs: Int, gain: Double = 0.55) {
        val sampleRate = 22050
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val p = i.toDouble() / numSamples
            val freq = freqStart + (freqEnd - freqStart) * p
            phase += 2.0 * PI * freq / sampleRate
            val env = sin(p * PI)
            buffer[i] = (sin(phase) * env * Short.MAX_VALUE * gain).toInt().toShort()
        }
        playStaticPcmAsync(sampleRate, buffer, durationMs.toLong())
    }

    fun playTttResultSound(activity: MainActivity, isWin: Boolean) {
        if (!activity.useSound) return
        val baseExcludes = listOf("sound1", "sound2", "sound3", "ac_sound", "loop", "shot", "shoot")
        val resId = if (isWin) {
            findRawResId(
                activity,
                listOf("ttt_win", "win", "game_win", "tuomas_data_game_win_471625", "victory", "cheer"),
                keywords = listOf("win", "victory", "cheer", "tada"),
                exclude = baseExcludes + listOf("coin", "crash", "lose")
            )
        } else {
            findRawResId(
                activity,
                listOf("ttt_lose", "lose", "game_over", "universfield_game_over_deep_male_voice_clip_352695", "gameover", "fail"),
                keywords = listOf("gameover", "game_over", "lose", "fail"),
                exclude = baseExcludes + listOf("win")
            )
        }
        if (playRawSfx(activity, resId)) return

        val sampleRate = 22050
        val notes = if (isWin) {
            listOf(523.25 to 110, 659.25 to 110, 783.99 to 130, 1046.50 to 250)
        } else {
            listOf(311.13 to 170, 277.18 to 170, 246.94 to 170, 196.00 to 300)
        }
        val totalSamples = notes.sumOf { (it.second * sampleRate) / 1000 }
        val buffer = ShortArray(totalSamples)
        var offset = 0
        for ((freq, dur) in notes) {
            val count = (dur * sampleRate) / 1000
            for (i in 0 until count) {
                val env = sin((i.toDouble() / count) * PI)
                buffer[offset + i] = (sin(2.0 * PI * freq * i / sampleRate) * env * Short.MAX_VALUE * 0.52).toInt().toShort()
            }
            offset += count
        }
        playStaticPcmAsync(sampleRate, buffer, notes.sumOf { it.second }.toLong())
    }

    private fun startRacerEngineLoop(activity: MainActivity) {
        stopRacerAudio()
        if (!activity.useSound) return

        val stageResIds = mutableListOf<Int>()
        for (i in 0..5) {
            val id = findRawResId(activity, listOf("loop$i", "loop_$i", "car_loop$i", "engine$i"))
            if (id != 0) stageResIds.add(id)
        }
        if (stageResIds.isNotEmpty()) {
            currentEngineStage = 0
            playEngineStage(activity, stageResIds)
            return
        }

        val singleResId = findRawResId(
            activity,
            listOf("car_loop", "engine", "race_car", "car_engine", "car", "motor"),
            keywords = listOf("loop", "engine", "motor"),
            exclude = listOf("crash", "shot", "shoot", "win", "lose", "ac_sound", "sound1", "sound2", "sound3")
        )
        if (singleResId != 0) {
            try {
                engineLoopPlayer = MediaPlayer.create(activity.applicationContext, singleResId)?.apply {
                    isLooping = true
                    setVolume(0.55f, 0.55f)
                    start()
                }
                return
            } catch (_: Throwable) {}
        }

        synthEngineRunning = true
        Thread {
            var track: AudioTrack? = null
            try {
                val sampleRate = 22050
                val chunkSamples = sampleRate / 10
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(chunkSamples * 2)
                val buffer = ShortArray(chunkSamples)
                var phase = 0.0
                var tick = 0
                track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(maxOf(chunkSamples * 2, minBuf))
                    .setTransferMode(AudioTrack.MODE_STREAM).build()
                track.play()
                while (synthEngineRunning && isActive && !gameEnding && currentGame == "RACER" && isActivityValid(activity)) {
                    val gearStage = (tick / 18).coerceAtMost(5)
                    val turboBoostHz = if (System.currentTimeMillis() < racerTurboBoostUntil) 38.0 else 0.0
                    val baseFreq = 65.0 + (gearStage * 22.0) + turboBoostHz + (if (tick % 4 < 2) 5.0 else -3.0)
                    tick++
                    for (i in 0 until chunkSamples) {
                        phase += 2.0 * PI * baseFreq / sampleRate
                        val rumble = sin(phase) * 0.55 + sin(phase * 2.0) * 0.30 + sin(phase * 3.0) * 0.15
                        buffer[i] = (rumble * Short.MAX_VALUE * 0.18).toInt().toShort()
                    }
                    track.write(buffer, 0, chunkSamples)
                }
            } catch (_: Throwable) {
            } finally {
                try { track?.stop() } catch (_: Throwable) {}
                try { track?.release() } catch (_: Throwable) {}
            }
        }.start()
    }

    private fun playEngineStage(activity: MainActivity, stageResIds: List<Int>) {
        if (!isActive || gameEnding || currentGame != "RACER" || !activity.useSound || !isActivityValid(activity)) return
        val resId = stageResIds[currentEngineStage.coerceIn(0, stageResIds.lastIndex)]
        try {
            val prev = engineLoopPlayer
            engineLoopPlayer = null
            try { prev?.setOnCompletionListener(null); prev?.stop(); prev?.release() } catch (_: Throwable) {}

            engineLoopPlayer = MediaPlayer.create(activity.applicationContext, resId)?.apply {
                setVolume(0.6f, 0.6f)
                if (currentEngineStage >= stageResIds.lastIndex) {
                    isLooping = true
                } else {
                    isLooping = false
                    setOnCompletionListener {
                        if (isActive && !gameEnding && currentGame == "RACER" && isActivityValid(activity)) {
                            currentEngineStage = (currentEngineStage + 1).coerceAtMost(stageResIds.lastIndex)
                            playEngineStage(activity, stageResIds)
                        }
                    }
                }
                start()
            }
        } catch (_: Throwable) {}
    }

    private fun stopRacerAudio() {
        synthEngineRunning = false
        val p = engineLoopPlayer
        engineLoopPlayer = null
        if (p != null) {
            try { p.setOnCompletionListener(null) } catch (_: Throwable) {}
            try { if (p.isPlaying) p.stop() } catch (_: Throwable) {}
            try { p.release() } catch (_: Throwable) {}
        }
    }

    private fun playRacerSoundEffect(activity: MainActivity, type: String) {
        if (!activity.useSound) return
        val commonExcludes = listOf("sound1", "sound2", "sound3", "ac_sound", "loop")
        val resId = when (type) {
            "SHOOT" -> findRawResId(
                activity,
                listOf("shoot", "shot", "universfield_arcade_shot_152053", "arcade_shot", "laser", "fire", "gun"),
                keywords = listOf("shot", "shoot", "laser", "gun", "fire"),
                exclude = commonExcludes + listOf("crash", "win", "lose", "coin")
            )
            "CRASH" -> findRawResId(
                activity,
                listOf("crash", "car_crash", "carcrash", "collision", "explosion", "boom", "hit", "smash", "impact"),
                keywords = listOf("crash", "collis", "explos", "boom", "smash", "wreck", "impact", "accident", "debris", "hit"),
                exclude = commonExcludes + listOf("shot", "shoot", "coin", "star", "win")
            )
            "STAR" -> findRawResId(
                activity,
                listOf("star", "coin", "mixkit_winning_a_coin_video_game_2069", "winning_a_coin", "bonus", "powerup", "pickup"),
                keywords = listOf("coin", "star", "bonus", "pickup", "powerup"),
                exclude = commonExcludes + listOf("crash", "shot", "shoot", "lose")
            )
            else -> 0
        }
        if (playRawSfx(activity, resId, volume = 1.0f)) return

        when (type) {
            "SHIELD" -> playSynthTone(620.0, 1240.0, 220, 0.52)
            "EMP" -> playSynthTone(980.0, 140.0, 360, 0.60)
            "FREEZE" -> playSynthTone(1480.0, 1980.0, 240, 0.45)
            "SHOOT" -> playSynthTone(1520.0, 320.0, 150, 0.60)
            "STAR" -> playSynthTone(880.0, 1320.0, 210, 0.55)
            else -> {
                // גיבוי סינתטי לפיצוץ/התרסקות
                val sampleRate = 22050
                val durationMs = 460
                val numSamples = (durationMs * sampleRate) / 1000
                val buffer = ShortArray(numSamples)
                var phase = 0.0
                for (i in 0 until numSamples) {
                    val p = i.toDouble() / numSamples
                    val noise = (Random.nextDouble() * 2.0 - 1.0)
                    phase += 2.0 * PI * (95.0 * (1.0 - p * 0.5)) / sampleRate
                    buffer[i] = ((noise * 0.78 + sin(phase) * 0.22) * (1.0 - p) * Short.MAX_VALUE * 0.68).toInt().toShort()
                }
                playStaticPcmAsync(sampleRate, buffer, durationMs.toLong())
            }
        }
    }

    fun playSqueakSound(enabled: Boolean = true, pitchMul: Double = 1.0) {
        if (!enabled) return
        val sampleRate = 22050
        val durationMs = 145
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val baseF = if (progress < 0.45) 950.0 + (progress / 0.45) * 1450.0 else 2400.0 - ((progress - 0.45) / 0.55) * 1100.0
            val freq = (baseF * pitchMul).coerceIn(300.0, 4200.0)
            val envelope = sin(progress * PI)
            phase += 2.0 * PI * freq / sampleRate
            val sample = (sin(phase) * 0.8 + sin(phase * 2.0) * 0.2) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.66).toInt().toShort()
        }
        playStaticPcmAsync(sampleRate, buffer, durationMs.toLong())
    }

    private fun triggerCrashAndExit(activity: MainActivity, endMessage: String, bestKey: String = "", score: Int = 0) {
        if (gameEnding) return
        gameEnding = true
        stopPreviousLoop()
        stopRacerAudio() // שומר על צליל ההתרסקות מנגן עד הסוף!
        if (bestKey.isNotEmpty()) updateHighScore(activity, bestKey, score)
        crashAnimStartTime = System.currentTimeMillis()
        crashLane = carLane
        val pal = resolvePalette(activity)
        spawnBurstNormalized(0.75f, 0.55f, pal.enemyColor, 20, isStar = false)
        spawnBurstNormalized(0.75f, 0.55f, pal.bonusColor, 12, isStar = true)
        playRacerSoundEffect(activity, "CRASH")
        activity.shakeScreen()
        invalidateStage()

        val exitTask = Runnable {
            if (isActive && isActivityValid(activity)) {
                stopGameInternal(activity, endMessage, stopSfx = false)
            }
        }
        gameOverDelayRunnable = exitTask
        handler.postDelayed(exitTask, 900L)
    }

    // =========================================================================
    // משחק 0: תזמורת חיות + אתגר זיכרון Simon Says (קוד 7777)
    // =========================================================================
    fun startAnimalParty(activity: MainActivity) {
        prepareNewGame(activity, "ANIMALS")
        isAnimalParty = true
        animalTimeLeft = 25
        animalTotalTime = 25
        animalActiveIdx = 0
        animalSimonMode = false
        animalSimonShowing = false
        animalSimonScore = 0
        animalSimonBest = getHighScore(activity, "animal_simon")
        animalSimonSeq.clear()
        animalSimonInputPos = 0
        animalBounceTime = System.currentTimeMillis()

        val shuffled = animalEmojis.shuffled()
        var idx = 0
        for ((k, btn) in activity.keypadButtons) {
            savedKeyLabels[k] = btn.text
            when (k) {
                "AC" -> {}
                "=" -> btn.text = "🎵"
                "0" -> btn.text = "🎉"
                else -> {
                    btn.text = shuffled[idx % shuffled.size]
                    idx++
                }
            }
        }
        playSqueakSound(activity.useSound, 1.0)
        invalidateStage()

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "ANIMALS" || !isActivityValid(activity)) return
                if (!animalSimonMode) {
                    animalTimeLeft--
                    if (animalTimeLeft <= 0) {
                        stopGameInternal(activity, "🐾 החיות חזרו לגן החיות!", stopSfx = false)
                        return
                    }
                }
                invalidateStage()
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(gameLoop!!, 1000L)
    }

    private fun startNextAnimalSimonRound(activity: MainActivity) {
        animalSimonShowing = true
        animalSimonInputPos = 0
        animalSimonSeq.add(Random.nextInt(5))
        playAnimalSimonSequenceStep(activity, 0)
    }

    private fun playAnimalSimonSequenceStep(activity: MainActivity, step: Int) {
        if (!isActive || currentGame != "ANIMALS" || !animalSimonMode || !isActivityValid(activity)) return
        if (step >= animalSimonSeq.size) {
            animalSimonShowing = false
            invalidateStage()
            return
        }
        val aIdx = animalSimonSeq[step]
        animalActiveIdx = aIdx
        animalBounceTime = System.currentTimeMillis()
        if (activity.useSound) {
            playSynthTone(animalNotes[aIdx], animalNotes[aIdx] * 1.08, 220, 0.55)
        }
        invalidateStage()
        val next = Runnable { playAnimalSimonSequenceStep(activity, step + 1) }
        simonStepRunnable = next
        handler.postDelayed(next, 520L)
    }

    // =========================================================================
    // משחק 1: איקס-עיגול טקטי (קוד 1337) — 3 רמות קושי + 2 שחקנים + רמז
    // =========================================================================
    fun startTicTacToe(activity: MainActivity) {
        val keepScores = (currentGame == "TTT")
        prepareNewGame(activity, "TTT")
        if (!keepScores) {
            tttPlayerWins = 0; tttCpuWins = 0; tttDraws = 0; tttStreak = 0
            tttMode = 2
        }
        tttCurrentTurn = "×"
        saveKeysLabels(activity, listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "−", "×", "÷", "0"))
        for (i in 0..8) {
            tttBoard[i] = (i + 1).toString()
            tttCellAnimStart[i] = 0L
        }
        updateTicTacToeUI(activity, "תור × — בחר 1–9")
    }

    internal fun tttModeLabel(): String = when (tttMode) {
        0 -> "קל"
        1 -> "בינוני"
        2 -> "מומחה"
        else -> "2 שחקנים"
    }

    private fun updateTicTacToeUI(activity: MainActivity, status: String) {
        tttStatusText = status
        val pal = resolvePalette(activity)
        for (i in 0..8) {
            val key = (i + 1).toString()
            val mark = tttBoard[i]
            val keyBg = MinimalistTheme.resolveKeypadButtonBgColor(activity, key)
            val neutralColor = MinimalistTheme.resolveKeypadSymbolTextColor(activity, key, keyBg)
            when (mark) {
                "×" -> styleKeyForGame(activity, key, pal.playerColor, mark, glow = true)
                "0" -> styleKeyForGame(activity, key, pal.enemyColor, mark, glow = true)
                else -> styleKeyForGame(activity, key, neutralColor, mark, glow = false)
            }
        }
        styleKeyForGame(activity, "+", if (tttMode == 0) pal.playerColor else pal.inkSecondary, "קל", glow = tttMode == 0)
        styleKeyForGame(activity, "−", if (tttMode == 1) pal.bonusColor else pal.inkSecondary, "בינוני", glow = tttMode == 1)
        styleKeyForGame(activity, "×", if (tttMode == 2) pal.enemyColor else pal.inkSecondary, "מומחה", glow = tttMode == 2)
        styleKeyForGame(activity, "÷", if (tttMode == 3) pal.controlAccent else pal.inkSecondary, "2P", glow = tttMode == 3)
        styleKeyForGame(activity, "0", pal.bonusColor, "רמז", glow = false)
        invalidateStage()
    }

    private fun findWinningLine(): Pair<String, IntArray?>? {
        val wins = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
        for (w in wins) {
            if (tttBoard[w[0]] == tttBoard[w[1]] && tttBoard[w[1]] == tttBoard[w[2]]) {
                return tttBoard[w[0]] to w
            }
        }
        if (tttBoard.none { it in "1".."9" }) return "TIE" to null
        return null
    }

    private fun checkWinner(): String? = findWinningLine()?.first

    private fun getBestMoveFor(mark: String, smartChance: Float): Int {
        val opp = if (mark == "0") "×" else "0"
        val free = (0..8).filter { tttBoard[it] !in listOf("×", "0") }
        if (free.isEmpty()) return -1
        if (Random.nextFloat() > smartChance) return free.random()
        for (i in free) {
            val orig = tttBoard[i]; tttBoard[i] = mark
            if (checkWinner() == mark) { tttBoard[i] = orig; return i }
            tttBoard[i] = orig
        }
        for (i in free) {
            val orig = tttBoard[i]; tttBoard[i] = opp
            if (checkWinner() == opp) { tttBoard[i] = orig; return i }
            tttBoard[i] = orig
        }
        var bestScore = Int.MIN_VALUE
        val bestMoves = mutableListOf<Int>()
        for (i in free) {
            val orig = tttBoard[i]; tttBoard[i] = mark
            val score = minimaxFor(0, false, mark, opp)
            tttBoard[i] = orig
            if (score > bestScore) { bestScore = score; bestMoves.clear(); bestMoves.add(i) }
            else if (score == bestScore) bestMoves.add(i)
        }
        return if (bestMoves.isNotEmpty()) bestMoves.random() else free.random()
    }

    private fun minimaxFor(depth: Int, isMax: Boolean, aiMark: String, humanMark: String): Int {
        when (checkWinner()) {
            aiMark -> return 10 - depth
            humanMark -> return depth - 10
            "TIE" -> return 0
        }
        val free = (0..8).filter { tttBoard[it] !in listOf("×", "0") }
        return if (isMax) {
            var best = Int.MIN_VALUE
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = aiMark
                best = maxOf(best, minimaxFor(depth + 1, false, aiMark, humanMark))
                tttBoard[i] = orig
            }
            best
        } else {
            var best = Int.MAX_VALUE
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = humanMark
                best = minOf(best, minimaxFor(depth + 1, true, aiMark, humanMark))
                tttBoard[i] = orig
            }
            best
        }
    }

    private fun finishTicTacToe(activity: MainActivity, result: Pair<String, IntArray?>) {
        tttRoundLocked = true
        tttWinningLine = result.second
        tttWinAnimStart = System.currentTimeMillis()
        val pal = resolvePalette(activity)
        val winner = result.first
        when (winner) {
            "×" -> {
                tttPlayerWins++; tttStreak++
                playTttResultSound(activity, true)
                spawnBurstNormalized(0.28f, 0.50f, pal.playerColor, 18, isStar = true)
                spawnBurstNormalized(0.28f, 0.50f, pal.bonusColor, 12, isStar = true)
            }
            "0" -> {
                tttCpuWins++; tttStreak = 0
                playTttResultSound(activity, tttMode == 3)
                spawnBurstNormalized(0.28f, 0.50f, pal.enemyColor, 14, isStar = false)
            }
            else -> tttDraws++
        }
        val msg = when {
            winner == "×" && tttMode == 3 -> "שחקן × ניצח!"
            winner == "0" && tttMode == 3 -> "שחקן 0 ניצח!"
            winner == "×" -> "ניצחת! רצף $tttStreak"
            winner == "0" -> "המחשב ניצח!"
            else -> "תיקו צמוד!"
        }
        updateTicTacToeUI(activity, msg)
        roundResetRunnable?.let { handler.removeCallbacks(it) }
        val resetTask = Runnable {
            if (isActive && currentGame == "TTT" && isActivityValid(activity)) {
                startTicTacToe(activity)
            }
        }
        roundResetRunnable = resetTask
        handler.postDelayed(resetTask, 2000L)
    }

    // =========================================================================
    // משחק 2: פולשי החשבון — גלים, שריון, בוסים וגל NOVA (קוד 8080)
    // =========================================================================
    fun startCalcInvaders(activity: MainActivity) {
        val hasScientificKeys = activity.keypadButtons.containsKey("sin") && activity.keypadButtons.containsKey("√")
        if (!hasScientificKeys) {
            Toast.makeText(activity, "🔬 משחק זה זמין במחשבון המדעי בלבד!", Toast.LENGTH_LONG).show()
            activity.currentInput = ""
            activity.currentView?.text = "0"
            return
        }
        prepareNewGame(activity, "INVADERS")
        invaderScore = 0
        invaderBest = getHighScore(activity, "invaders")
        invaderHp = 3
        invaderWave = 1
        invaderCombo = 0
        invaderNovaCharge = 0
        invaderBossActive = false
        invaderBossHp = 0
        invaderTrack = MutableList(6) { "." }
        saveKeysLabels(activity, listOf("="))

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "INVADERS" || !isActivityValid(activity)) return
                val reachedShip = invaderTrack.removeAt(0)
                if (reachedShip != ".") {
                    invaderHp--
                    invaderCombo = 0
                    invaderHitFlashTime = System.currentTimeMillis()
                    if (reachedShip == "BOSS") invaderBossActive = false
                    if (invaderHp <= 0) {
                        triggerCrashAndExit(
                            activity,
                            "💥 החללית נהרסה! גל $invaderWave | ניקוד: $invaderScore",
                            bestKey = "invaders",
                            score = invaderScore
                        )
                        return
                    } else {
                        playRacerSoundEffect(activity, "CRASH")
                        activity.shakeScreen()
                        spawnPopup("−1 HP", 0.20f, 0.45f, resolvePalette(activity).enemyColor)
                    }
                }

                // התקדמות גלים והזמנת בוס בכל מעבר גל
                val targetWave = 1 + (invaderScore / 80)
                if (targetWave > invaderWave && !invaderBossActive) {
                    invaderWave = targetWave
                    invaderBossActive = true
                    invaderBossMaxHp = (2 + invaderWave).coerceAtMost(5)
                    invaderBossHp = invaderBossMaxHp
                    invaderBossSymbol = invaderSymbols.random()
                    invaderTrack.add("BOSS")
                    spawnPopup("WAVE $invaderWave BOSS!", 0.55f, 0.30f, resolvePalette(activity).enemyColor)
                } else {
                    val spawnRate = (0.58f + invaderWave * 0.04f).coerceAtMost(0.80f)
                    val nextSpawn = if (Random.nextFloat() < spawnRate) invaderSymbols.random() else "."
                    invaderTrack.add(nextSpawn)
                }

                renderInvaders(activity)
                val speed = (940L - (invaderWave * 65L) - (invaderScore * 2L)).coerceAtLeast(360L)
                handler.postDelayed(this, speed)
            }
        }
        renderInvaders(activity)
        handler.postDelayed(gameLoop!!, 900L)
    }

    private fun renderInvaders(activity: MainActivity) {
        val pal = resolvePalette(activity)
        val firstRaw = invaderTrack.firstOrNull { it != "." }
        val nextTargetSym = if (firstRaw == "BOSS") invaderBossSymbol else firstRaw
        for (sym in invaderSymbols) {
            val isNext = (sym == nextTargetSym)
            styleKeyForGame(activity, sym, if (isNext) pal.bonusColor else pal.controlAccent, glow = isNext)
        }
        val novaReady = invaderNovaCharge >= 5
        styleKeyForGame(
            activity, "=",
            if (novaReady) pal.playerColor else pal.inkSecondary,
            if (novaReady) "NOVA" else "⚡${invaderNovaCharge}/5",
            glow = novaReady
        )
        invalidateStage()
    }

    // =========================================================================
    // משחק 3: מירוץ ארקייד — חיים, מגן, טורבו, מחסום משוריין ו-EMP (קוד 5555)
    // =========================================================================
    fun startMathRacer(activity: MainActivity) {
        prepareNewGame(activity, "RACER")
        carLane = 1
        racerScore = 0
        racerBest = getHighScore(activity, "racer")
        racerLives = 2
        racerShieldActive = false
        racerCombo = 1
        racerStreak = 0
        racerEmpCharge = 0
        racerTurboBoostUntil = 0L
        racerInvulnUntil = 0L
        racerShieldBreakTime = 0L
        lastShotTime = 0L
        lastCooldownToastTime = 0L
        pendingGuardedStarLane = -1
        for (lane in 0..2) racerTracks[lane] = MutableList(5) { "." }

        saveKeysLabels(activity, listOf("8", "2", "5", "6", "0", "="))
        startRacerEngineLoop(activity)

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "RACER" || !isActivityValid(activity)) return
                advanceRacerTracks(activity)
                if (!isActive || gameEnding) return
                renderRacer(activity)
                handler.postDelayed(this, currentRacerDelayMs())
            }
        }
        cooldownTicker = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "RACER" || !isActivityValid(activity)) return
                renderRacer(activity)
                handler.postDelayed(this, 220L)
            }
        }
        renderRacer(activity)
        handler.postDelayed(gameLoop!!, currentRacerDelayMs())
        handler.postDelayed(cooldownTicker!!, 220L)
    }

    private fun currentRacerDelayMs(): Long {
        val turboFactor = if (System.currentTimeMillis() < racerTurboBoostUntil) 0.72 else 1.0
        return ((730L - (racerScore * 7L)).coerceAtLeast(250L) * turboFactor).toLong()
    }

    private fun advanceRacerTracks(activity: MainActivity) {
        val pal = resolvePalette(activity)
        val now = System.currentTimeMillis()
        for (lane in 0..2) {
            val reachedCar = racerTracks[lane].removeAt(racerTracks[lane].lastIndex)
            if (reachedCar != ".") {
                if (lane == carLane) {
                    when (reachedCar) {
                        "🚧", "🧱" -> {
                            if (now < racerInvulnUntil) {
                                // חסינות זמנית אחרי פגיעה
                            } else if (racerShieldActive) {
                                racerShieldActive = false
                                racerShieldBreakTime = now
                                racerInvulnUntil = now + 900L
                                playRacerSoundEffect(activity, "SHIELD")
                                spawnBurstNormalized(0.80f, 0.30f + lane * 0.25f, pal.shieldColor, 16)
                                spawnPopup("מגן נספג!", 0.72f, 0.35f, pal.shieldColor)
                                activity.shakeScreen()
                            } else {
                                racerLives--
                                racerStreak = 0
                                racerCombo = 1
                                if (racerLives <= 0) {
                                    triggerCrashAndExit(
                                        activity,
                                        "💥 התנגשת במחסום! ניקוד סופי: $racerScore 🏆",
                                        bestKey = "racer",
                                        score = racerScore
                                    )
                                    return
                                } else {
                                    racerInvulnUntil = now + 1300L
                                    playRacerSoundEffect(activity, "CRASH")
                                    spawnBurstNormalized(0.80f, 0.30f + lane * 0.25f, pal.enemyColor, 14)
                                    spawnPopup("−1 ❤️", 0.74f, 0.35f, pal.enemyColor)
                                    activity.shakeScreen()
                                }
                            }
                        }
                        "⭐" -> {
                            playRacerSoundEffect(activity, "STAR")
                            racerStreak++
                            racerCombo = (1 + racerStreak / 3).coerceAtMost(4)
                            val pts = 10 * racerCombo
                            racerScore += pts
                            racerEmpCharge = (racerEmpCharge + 1).coerceAtMost(5)
                            lastShotTime = 0L // איפוס טעינת לייזר מיידי
                            spawnBurstNormalized(0.80f, 0.30f + lane * 0.25f, pal.bonusColor, 12, isStar = true)
                            spawnPopup("+$pts ★", 0.74f, 0.35f, pal.bonusColor)
                        }
                        "🛡️" -> {
                            racerShieldActive = true
                            racerScore += 5
                            playRacerSoundEffect(activity, "SHIELD")
                            spawnBurstNormalized(0.80f, 0.30f + lane * 0.25f, pal.shieldColor, 12)
                            spawnPopup("SHIELD!", 0.74f, 0.35f, pal.shieldColor)
                        }
                        "❤️" -> {
                            racerLives = (racerLives + 1).coerceAtMost(3)
                            racerScore += 5
                            playRacerSoundEffect(activity, "STAR")
                            spawnBurstNormalized(0.80f, 0.30f + lane * 0.25f, pal.playerColor, 12)
                            spawnPopup("+1 ❤️", 0.74f, 0.35f, pal.playerColor)
                        }
                    }
                } else if (reachedCar == "🚧" || reachedCar == "🧱") {
                    val gain = (if (now < racerTurboBoostUntil) 2 else 1) * racerCombo
                    racerScore += gain
                    racerStreak++
                    racerCombo = (1 + racerStreak / 6).coerceAtMost(4)
                }
            }
        }
        racerBest = maxOf(racerBest, racerScore)

        if (pendingGuardedStarLane in 0..2) {
            val starLane = pendingGuardedStarLane
            pendingGuardedStarLane = -1
            for (lane in 0..2) racerTracks[lane].add(0, if (lane == starLane) "⭐" else ".")
            return
        }

        val recentlySpawned = (0..2).any { racerTracks[it].firstOrNull() != "." }
        if (!recentlySpawned && Random.nextFloat() < 0.76f) {
            val spawnLane = Random.nextInt(3)
            val roll = Random.nextFloat()
            when {
                roll < 0.06f && !racerShieldActive -> {
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "🛡️" else ".")
                }
                roll < 0.10f && racerLives < 3 -> {
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "❤️" else ".")
                }
                roll < 0.20f -> {
                    pendingGuardedStarLane = spawnLane
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "🚧" else ".")
                }
                roll < 0.26f -> {
                    val blockerLane = (spawnLane + 1 + Random.nextInt(2)) % 3
                    for (lane in 0..2) {
                        val sym = when (lane) { spawnLane -> "⭐"; blockerLane -> "🚧"; else -> "." }
                        racerTracks[lane].add(0, sym)
                    }
                }
                roll < 0.38f && racerScore >= 15 -> {
                    // מחסום משוריין (2 יריות להשמדה)
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "🧱" else ".")
                }
                else -> {
                    val secondBlocker = if (racerScore >= 18 && Random.nextFloat() < 0.32f) (spawnLane + 1 + Random.nextInt(2)) % 3 else -1
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane || lane == secondBlocker) "🚧" else ".")
                }
            }
        } else {
            for (lane in 0..2) racerTracks[lane].add(0, ".")
        }
    }

    internal fun getShotCooldownRemainingMs(): Long {
        val elapsed = System.currentTimeMillis() - lastShotTime
        return (SHOT_COOLDOWN_MS - elapsed).coerceAtLeast(0L)
    }

    private fun renderRacer(activity: MainActivity) {
        val pal = resolvePalette(activity)
        val remMs = getShotCooldownRemainingMs()
        val gunReady = (remMs == 0L)
        val sec = ceil(remMs / 1000.0).toInt()
        val empReady = racerEmpCharge >= 5

        styleKeyForGame(activity, "8", pal.controlAccent, if (pal.isMinimalist) "8 ↑" else "8⬆️", glow = true)
        styleKeyForGame(activity, "2", pal.controlAccent, if (pal.isMinimalist) "2 ↓" else "2⬇️", glow = true)
        styleKeyForGame(
            activity, "5",
            if (gunReady) pal.playerColor else pal.inkSecondary,
            if (gunReady) (if (pal.isMinimalist) "5 ✦" else "5💥") else "${sec}s",
            glow = gunReady
        )
        styleKeyForGame(activity, "6", pal.bonusColor, if (pal.isMinimalist) "6 »" else "6⚡", glow = true)
        styleKeyForGame(
            activity, "0",
            if (empReady) pal.shieldColor else pal.inkSecondary,
            if (empReady) "EMP" else "⚡${racerEmpCharge}/5",
            glow = empReady
        )
        invalidateStage()
    }

    // =========================================================================
    // משחק 4: נטרול פצצה מתקדם — קומבו + הקפאת זמן ('=') (קוד 1010)
    // =========================================================================
    fun startBombDefusal(activity: MainActivity) {
        prepareNewGame(activity, "BOMB")
        bombTimeLeft = 15
        bombScore = 0
        bombBest = getHighScore(activity, "bomb")
        bombStreak = 0
        bombFreezeCharges = 1 // מתחיל עם הקפאת זמן אחת לשעת חירום!
        bombFrozenUntil = 0L
        saveKeysLabels(activity, listOf("="))
        generateBombQuestion(activity)

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "BOMB" || !isActivityValid(activity)) return
                val frozen = System.currentTimeMillis() < bombFrozenUntil
                if (!frozen) {
                    bombTimeLeft--
                    if (bombTimeLeft <= 0) {
                        triggerCrashAndExit(
                            activity,
                            "💥 בום! הזמן נגמר! פצצות שנוטרלו: $bombScore",
                            bestKey = "bomb",
                            score = bombScore
                        )
                        return
                    }
                }
                renderBombUI(activity)
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(gameLoop!!, 1000L)
    }

    private fun isValidBombAnswer(lhs: Int, rhs: Int, target: Int, op: String): Boolean = when (op) {
        "+" -> lhs + rhs == target
        "−" -> lhs - rhs == target
        "×" -> lhs * rhs == target
        "÷" -> rhs != 0 && lhs % rhs == 0 && (lhs / rhs) == target
        else -> false
    }

    private fun generateBombQuestion(activity: MainActivity) {
        val ops = listOf("+", "−", "×", "÷")
        val maxA = (12 + bombScore).coerceAtMost(35)
        val maxB = (10 + bombScore / 2).coerceAtMost(19)
        for (attempt in 0 until 24) {
            val a = Random.nextInt(2, maxA)
            val b = Random.nextInt(2, maxB)
            val op = ops.random()
            val (lhs, rhs, target) = when (op) {
                "+" -> Triple(a, b, a + b)
                "−" -> Triple(a + b, b, a)
                "×" -> Triple(a, b, a * b)
                else -> Triple(a * b, b, a)
            }
            if (ops.count { isValidBombAnswer(lhs, rhs, target, it) } == 1 || attempt == 23) {
                bombLhs = lhs
                bombRhs = rhs
                bombTarget = target
                expectedOp = op
                break
            }
        }
        renderBombUI(activity)
    }

    private fun renderBombUI(activity: MainActivity) {
        val pal = resolvePalette(activity)
        val isUrgent = bombTimeLeft <= 5 && System.currentTimeMillis() >= bombFrozenUntil
        for (op in listOf("+", "−", "×", "÷")) {
            styleKeyForGame(activity, op, if (isUrgent) pal.enemyColor else pal.controlAccent, glow = true)
        }
        styleKeyForGame(
            activity, "=",
            if (bombFreezeCharges > 0) pal.shieldColor else pal.inkSecondary,
            if (bombFreezeCharges > 0) "❄×$bombFreezeCharges" else "❄0",
            glow = bombFreezeCharges > 0
        )
        invalidateStage()
    }

    // =========================================================================
    // קליטת מקשים בזמן משחק
    // =========================================================================
    fun onKeyPress(activity: MainActivity, key: String): Boolean {
        if (!isActive) return false
        if (key == "AC") {
            stopGameInternal(activity, "יצאת מהמשחק 🎮", stopSfx = true)
            return true
        }
        if (gameEnding) return true

        val pal = resolvePalette(activity)
        when (currentGame) {
            "ANIMALS" -> {
                if (key == "=") {
                    animalSimonMode = !animalSimonMode
                    simonStepRunnable?.let { handler.removeCallbacks(it) }
                    if (animalSimonMode) {
                        animalSimonScore = 0
                        animalSimonSeq.clear()
                        spawnPopup("אתגר זיכרון 1–5!", 0.5f, 0.30f, pal.bonusColor)
                        startNextAnimalSimonRound(activity)
                    } else {
                        animalSimonShowing = false
                        animalTimeLeft = 25
                        invalidateStage()
                    }
                    return true
                }
                if (animalSimonMode) {
                    if (animalSimonShowing) return true
                    val picked = (key.toIntOrNull()?.minus(1)) ?: return true
                    if (picked !in 0..4) return true
                    animalActiveIdx = picked
                    animalBounceTime = System.currentTimeMillis()
                    if (activity.useSound) playSynthTone(animalNotes[picked], animalNotes[picked] * 1.08, 190, 0.55)
                    if (picked == animalSimonSeq.getOrNull(animalSimonInputPos)) {
                        spawnBurstNormalized(0.14f + picked * 0.18f, 0.62f, pal.playerColor, 8, isStar = true)
                        animalSimonInputPos++
                        if (animalSimonInputPos >= animalSimonSeq.size) {
                            animalSimonScore++
                            animalSimonBest = updateHighScore(activity, "animal_simon", animalSimonScore)
                            spawnPopup("שלב ${animalSimonScore + 1}!", 0.5f, 0.35f, pal.playerColor)
                            val nextRound = Runnable { startNextAnimalSimonRound(activity) }
                            simonStepRunnable = nextRound
                            handler.postDelayed(nextRound, 650L)
                        }
                    } else {
                        playRacerSoundEffect(activity, "CRASH")
                        activity.shakeScreen()
                        spawnPopup("טעות! ניקוד: $animalSimonScore", 0.5f, 0.35f, pal.enemyColor)
                        animalSimonScore = 0
                        animalSimonSeq.clear()
                        val retry = Runnable { startNextAnimalSimonRound(activity) }
                        simonStepRunnable = retry
                        handler.postDelayed(retry, 950L)
                    }
                    invalidateStage()
                    return true
                }

                // מצב תזמורת חופשי
                if (key == "0") {
                    animalChorusTime = System.currentTimeMillis()
                    animalBounceTime = animalChorusTime
                    playSqueakSound(activity.useSound, 1.25)
                    spawnBurstNormalized(0.5f, 0.55f, pal.bonusColor, 18, isStar = true)
                    spawnPopup("מקהלה! ♫", 0.5f, 0.35f, pal.bonusColor)
                } else {
                    val num = key.toIntOrNull()
                    val idx = if (num != null && num in 1..9) (num - 1) % 5 else (animalActiveIdx + 1) % 5
                    animalActiveIdx = idx
                    animalBounceTime = System.currentTimeMillis()
                    val pitch = 0.78 + (idx * 0.14)
                    playSqueakSound(activity.useSound, pitch)
                    spawnBurstNormalized(0.14f + idx * 0.18f, 0.58f, pal.playerColor, 7, isStar = true)
                }
                invalidateStage()
                return true
            }

            "TTT" -> {
                // החלפת רמת קושי / מצב 2 שחקנים
                when (key) {
                    "+" -> { tttMode = 0; startTicTacToe(activity); return true }
                    "−" -> { tttMode = 1; startTicTacToe(activity); return true }
                    "×" -> { tttMode = 2; startTicTacToe(activity); return true }
                    "÷" -> { tttMode = 3; startTicTacToe(activity); return true }
                    "0" -> {
                        if (!tttRoundLocked) {
                            tttHintCell = getBestMoveFor(tttCurrentTurn, 1.0f)
                            tttHintTime = System.currentTimeMillis()
                            invalidateStage()
                        }
                        return true
                    }
                }
                if (tttRoundLocked) {
                    updateTicTacToeUI(activity, tttStatusText)
                    return true
                }
                val idx = key.toIntOrNull()?.minus(1)
                if (idx != null && idx in 0..8 && tttBoard[idx] !in listOf("×", "0")) {
                    tttHintCell = -1
                    tttBoard[idx] = tttCurrentTurn
                    tttCellAnimStart[idx] = System.currentTimeMillis()
                    var res = findWinningLine()
                    if (res != null) { finishTicTacToe(activity, res); return true }

                    if (tttMode == 3) {
                        // 2 שחקנים
                        tttCurrentTurn = if (tttCurrentTurn == "×") "0" else "×"
                        updateTicTacToeUI(activity, "תור $tttCurrentTurn — בחר 1–9")
                    } else {
                        val smartness = when (tttMode) { 0 -> 0.25f; 1 -> 0.68f; else -> 1.0f }
                        val comp = getBestMoveFor("0", smartness)
                        if (comp != -1) {
                            tttBoard[comp] = "0"
                            tttCellAnimStart[comp] = System.currentTimeMillis()
                        }
                        res = findWinningLine()
                        if (res != null) finishTicTacToe(activity, res)
                        else updateTicTacToeUI(activity, "תורך (×) — בחר 1–9")
                    }
                } else {
                    updateTicTacToeUI(activity, tttStatusText)
                }
                return true
            }

            "INVADERS" -> {
                if (key == "=" && invaderNovaCharge >= 5) {
                    invaderNovaCharge = 0
                    invaderNovaTime = System.currentTimeMillis()
                    var cleared = 0
                    for (i in invaderTrack.indices) {
                        if (invaderTrack[i] != ".") {
                            invaderTrack[i] = "."
                            cleared++
                        }
                    }
                    invaderBossActive = false
                    invaderBossHp = 0
                    val bonus = cleared * 15
                    invaderScore += bonus
                    invaderBest = updateHighScore(activity, "invaders", invaderScore)
                    playRacerSoundEffect(activity, "EMP")
                    spawnBurstNormalized(0.55f, 0.55f, pal.playerColor, 24, isStar = true)
                    spawnPopup("NOVA +$bonus!", 0.55f, 0.35f, pal.playerColor)
                    renderInvaders(activity)
                    return true
                }

                val targetIndex = invaderTrack.indexOfFirst { it != "." }
                val targetCell = if (targetIndex != -1) invaderTrack[targetIndex] else null
                val expectedSym = if (targetCell == "BOSS") invaderBossSymbol else targetCell

                if (targetIndex != -1 && expectedSym == key) {
                    invaderLaserCol = targetIndex
                    invaderLaserTime = System.currentTimeMillis()
                    invaderShipRecoilTime = invaderLaserTime
                    invaderCombo++
                    invaderNovaCharge = (invaderNovaCharge + 1).coerceAtMost(5)
                    val mult = (1 + invaderCombo / 4).coerceAtMost(4)
                    playRacerSoundEffect(activity, "SHOOT")
                    if (activity.useHaptic) activity.mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                    if (targetCell == "BOSS") {
                        invaderBossHp--
                        spawnBurstNormalized(0.30f + targetIndex * 0.11f, 0.58f, pal.enemyColor, 10)
                        if (invaderBossHp <= 0) {
                            invaderTrack[targetIndex] = "."
                            invaderBossActive = false
                            invaderHp = (invaderHp + 1).coerceAtMost(3)
                            val pts = 40 * mult
                            invaderScore += pts
                            playRacerSoundEffect(activity, "STAR")
                            spawnBurstNormalized(0.30f + targetIndex * 0.11f, 0.58f, pal.bonusColor, 22, isStar = true)
                            spawnPopup("BOSS DOWN +$pts!", 0.55f, 0.35f, pal.bonusColor)
                        } else {
                            invaderBossSymbol = invaderSymbols.random()
                            spawnPopup("HIT!", 0.30f + targetIndex * 0.11f, 0.38f, pal.bonusColor)
                        }
                    } else {
                        invaderTrack[targetIndex] = "."
                        val pts = 10 * mult
                        invaderScore += pts
                        spawnBurstNormalized(0.30f + targetIndex * 0.11f, 0.58f, pal.bonusColor, 10, isStar = true)
                        spawnPopup("+$pts", 0.30f + targetIndex * 0.11f, 0.38f, pal.bonusColor)
                    }
                    invaderBest = updateHighScore(activity, "invaders", invaderScore)
                } else {
                    invaderCombo = 0
                    invaderScore = (invaderScore - 2).coerceAtLeast(0)
                }
                renderInvaders(activity)
                return true
            }

            "RACER" -> {
                when {
                    key == "8" || key.startsWith("8") -> carLane = (carLane - 1).coerceAtLeast(0)
                    key == "2" || key.startsWith("2") -> carLane = (carLane + 1).coerceAtMost(2)
                    key == "0" || key == "=" || key == "EMP" -> {
                        if (racerEmpCharge >= 5) {
                            racerEmpCharge = 0
                            racerEmpTime = System.currentTimeMillis()
                            var destroyed = 0
                            for (ln in 0..2) {
                                for (c in 0..4) {
                                    if (racerTracks[ln][c] == "🚧" || racerTracks[ln][c] == "🧱") {
                                        racerTracks[ln][c] = "."
                                        destroyed++
                                    }
                                }
                            }
                            val bonus = destroyed * 4
                            racerScore += bonus
                            playRacerSoundEffect(activity, "EMP")
                            spawnBurstNormalized(0.55f, 0.55f, pal.shieldColor, 26, isStar = true)
                            spawnPopup("EMP +$bonus!", 0.52f, 0.35f, pal.shieldColor)
                        }
                    }
                    key == "5" || key.startsWith("5") || key.endsWith("s") || key == "×" -> {
                        val rem = getShotCooldownRemainingMs()
                        if (rem == 0L) {
                            lastShotTime = System.currentTimeMillis()
                            racerLaserLane = carLane
                            racerLaserTime = lastShotTime
                            playRacerSoundEffect(activity, "SHOOT")
                            val idx = racerTracks[carLane].indexOfLast { it == "🚧" || it == "🧱" }
                            if (idx != -1) {
                                racerLaserEndCol = idx
                                if (racerTracks[carLane][idx] == "🧱") {
                                    // מחסום משוריין הופך לרגיל בפגיעה הראשונה
                                    racerTracks[carLane][idx] = "🚧"
                                    racerScore += 3
                                    spawnBurstNormalized(0.16f + idx * 0.13f, 0.34f + carLane * 0.24f, pal.bonusColor, 10)
                                    spawnPopup("CRACK!", 0.18f + idx * 0.13f, 0.30f + carLane * 0.22f, pal.bonusColor)
                                } else {
                                    racerTracks[carLane][idx] = "."
                                    val pts = 3 * racerCombo
                                    racerScore += pts
                                    racerEmpCharge = (racerEmpCharge + 1).coerceAtMost(5)
                                    spawnBurstNormalized(0.16f + idx * 0.13f, 0.34f + carLane * 0.24f, pal.laserColor, 12)
                                    spawnPopup("+$pts", 0.18f + idx * 0.13f, 0.30f + carLane * 0.22f, pal.laserColor)
                                }
                            } else {
                                racerLaserEndCol = 0
                            }
                        } else {
                            val now = System.currentTimeMillis()
                            if (now - lastCooldownToastTime > 1200L) {
                                lastCooldownToastTime = now
                                val sec = ceil(rem / 1000.0).toInt()
                                Toast.makeText(activity, "הירייה בטעינה — עוד $sec שניות", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    key == "6" || key.startsWith("6") || key == "+" -> {
                        racerTurboBoostUntil = System.currentTimeMillis() + 750L
                        racerScore += racerCombo
                        advanceRacerTracks(activity)
                        if (isActive && !gameEnding && gameLoop != null) {
                            handler.removeCallbacks(gameLoop!!)
                            handler.postDelayed(gameLoop!!, currentRacerDelayMs())
                        }
                    }
                }
                if (isActive && !gameEnding) renderRacer(activity)
                return true
            }

            "BOMB" -> {
                if (key == "=" && bombFreezeCharges > 0) {
                    bombFreezeCharges--
                    bombFrozenUntil = System.currentTimeMillis() + 5000L
                    playRacerSoundEffect(activity, "FREEZE")
                    spawnBurstNormalized(0.22f, 0.54f, pal.shieldColor, 16, isStar = true)
                    spawnPopup("❄ הוקפא 5s!", 0.55f, 0.28f, pal.shieldColor)
                    renderBombUI(activity)
                    return true
                }
                if (key in listOf("+", "−", "×", "÷")) {
                    bombLastCutOp = key
                    bombLastCutTime = System.currentTimeMillis()
                    if (isValidBombAnswer(bombLhs, bombRhs, bombTarget, key)) {
                        bombLastCutCorrect = true
                        bombScore++
                        bombStreak++
                        bombBest = updateHighScore(activity, "bomb", bombScore)
                        val timeBonus = if (bombStreak >= 3) 4 else 3
                        bombTimeLeft = (bombTimeLeft + timeBonus).coerceAtMost(bombMaxTime)
                        if (bombStreak % 3 == 0) {
                            bombFreezeCharges = (bombFreezeCharges + 1).coerceAtMost(3)
                            spawnPopup("+1 ❄ FREEZE!", 0.65f, 0.30f, pal.shieldColor)
                        } else {
                            spawnPopup("+${timeBonus}s ✓", 0.65f, 0.30f, pal.playerColor)
                        }
                        spawnBurstNormalized(0.68f, 0.50f, pal.playerColor, 12, isStar = true)
                        playRacerSoundEffect(activity, "STAR")
                        generateBombQuestion(activity)
                    } else {
                        bombLastCutCorrect = false
                        bombStreak = 0
                        bombTimeLeft = (bombTimeLeft - 3).coerceAtLeast(1)
                        playRacerSoundEffect(activity, "CRASH")
                        spawnBurstNormalized(0.68f, 0.50f, pal.enemyColor, 12)
                        spawnPopup("−3s ✕", 0.65f, 0.30f, pal.enemyColor)
                        activity.shakeScreen()
                        renderBombUI(activity)
                    }
                } else {
                    renderBombUI(activity)
                }
                return true
            }
        }
        return true
    }

    private fun stopPreviousLoop() {
        gameLoop?.let { handler.removeCallbacks(it) }
        cooldownTicker?.let { handler.removeCallbacks(it) }
        roundResetRunnable?.let { handler.removeCallbacks(it) }
        gameOverDelayRunnable?.let { handler.removeCallbacks(it) }
        simonStepRunnable?.let { handler.removeCallbacks(it) }
        gameLoop = null
        cooldownTicker = null
        roundResetRunnable = null
        gameOverDelayRunnable = null
        simonStepRunnable = null
    }
}

// =========================================================================
// קנבס ארקייד וקטורי ב-60fps לכל 5 המיני-משחקים, מותאם אישית ל-4 ערכות הנושא
// =========================================================================
private class MiniGameVectorStageView(
    private val activity: MainActivity
) : View(activity) {

    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var smoothCarLane = 1f
    private var carTiltDeg = 0f
    private var anim: ValueAnimator? = null

    private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dashedP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f * d
    }
    private val textP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subTextP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val rect = RectF()
    private val path = Path()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        smoothCarLane = MiniGameEngine.carLane.toFloat()
        carTiltDeg = 0f
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase += 0.055f
                if (phase > 6283f) phase = 0f
                val targetLane = MiniGameEngine.carLane.toFloat()
                val diff = targetLane - smoothCarLane
                smoothCarLane += diff * 0.28f
                carTiltDeg += (diff * 26f - carTiltDeg) * 0.30f
                stepParticles()
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        anim?.cancel()
        anim = null
        super.onDetachedFromWindow()
    }

    private fun stepParticles() {
        val iter = MiniGameEngine.particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            p.vx *= 0.94f
            p.vy *= 0.94f
            p.life -= 0.032f
            if (p.life <= 0f) iter.remove()
        }
        val now = System.currentTimeMillis()
        val pIter = MiniGameEngine.popups.iterator()
        while (pIter.hasNext()) {
            if (now - pIter.next().createdAt > 780L) pIter.remove()
        }
    }

    private fun withAlpha(color: Int, a: Int): Int =
        Color.argb(a.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 4f || h <= 4f) return

        val pal = MiniGameEngine.resolvePalette(activity)
        drawStageBackdrop(canvas, w, h, pal)

        when (MiniGameEngine.currentGame) {
            "RACER" -> drawVectorRacer(canvas, w, h, pal)
            "TTT" -> drawVectorTicTacToe(canvas, w, h, pal)
            "INVADERS" -> drawVectorInvaders(canvas, w, h, pal)
            "BOMB" -> drawVectorBomb(canvas, w, h, pal)
            "ANIMALS" -> drawVectorAnimalParty(canvas, w, h, pal)
        }

        drawParticlesAndPopups(canvas, w, h)
    }

    private fun drawParticlesAndPopups(canvas: Canvas, w: Float, h: Float) {
        for (p in MiniGameEngine.particles) {
            val alpha = ((p.life / p.maxLife).coerceIn(0f, 1f) * 255f).toInt()
            if (p.isStar) {
                drawMiniStar(canvas, p.x, p.y, p.size * 1.1f, withAlpha(p.color, alpha))
            } else {
                fillP.color = withAlpha(p.color, alpha)
                canvas.drawCircle(p.x, p.y, p.size * (p.life / p.maxLife).coerceIn(0.3f, 1f), fillP)
            }
        }

        val now = System.currentTimeMillis()
        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 12f * d
        for (pop in MiniGameEngine.popups) {
            val age = (now - pop.createdAt).coerceIn(0L, 780L) / 780f
            val alpha = ((1f - age) * 255f).toInt().coerceIn(0, 255)
            textP.color = withAlpha(pop.color, alpha)
            canvas.drawText(pop.text, w * pop.xFrac, h * pop.yFrac - age * 22f * d, textP)
        }
    }

    private fun drawStageBackdrop(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        rect.set(2f * d, 2f * d, w - 2f * d, h - 2f * d)
        val r = 20f * d
        fillP.shader = LinearGradient(0f, 0f, w, h, pal.cardBgTop, pal.cardBgBottom, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, r, r, fillP)
        fillP.shader = null

        if (pal.isGlass) {
            val gloss = RectF(10f * d, 4f * d, w - 10f * d, h * 0.22f)
            fillP.shader = LinearGradient(0f, gloss.top, 0f, gloss.bottom, Color.argb(60, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(gloss, 14f * d, 14f * d, fillP)
            fillP.shader = null
        } else if (!pal.isMinimalist && !pal.isAurora) {
            strokeP.clearShadowLayer()
            strokeP.strokeWidth = 0.7f * d
            strokeP.color = pal.arenaGrid
            var x = 22f * d
            while (x < w - 10f * d) { canvas.drawLine(x, 8f * d, x, h - 8f * d, strokeP); x += 22f * d }
        }

        strokeP.strokeWidth = 1.4f * d
        strokeP.color = pal.cardBorder
        if (pal.isAurora) strokeP.setShadowLayer(6f * d, 0f, 0f, withAlpha(pal.glowColor, 165)) else strokeP.clearShadowLayer()
        canvas.drawRoundRect(rect, r, r, strokeP)
        strokeP.clearShadowLayer()
    }

    private fun drawHeartIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int, filled: Boolean) {
        path.reset()
        path.moveTo(cx, cy + r * 0.85f)
        path.cubicTo(cx - r * 1.15f, cy + r * 0.15f, cx - r * 0.95f, cy - r * 0.85f, cx, cy - r * 0.25f)
        path.cubicTo(cx + r * 0.95f, cy - r * 0.85f, cx + r * 1.15f, cy + r * 0.15f, cx, cy + r * 0.85f)
        path.close()
        if (filled) {
            fillP.color = color
            canvas.drawPath(path, fillP)
        } else {
            strokeP.strokeWidth = 1.2f * d
            strokeP.color = withAlpha(color, 110)
            canvas.drawPath(path, strokeP)
        }
    }

    private fun drawMiniStar(canvas: Canvas, cx: Float, cy: Float, r: Float, color: Int) {
        path.reset()
        for (i in 0 until 10) {
            val a = Math.toRadians(-90.0 + i * 36.0)
            val rad = if (i % 2 == 0) r else r * 0.46f
            val px = cx + (cos(a) * rad).toFloat()
            val py = cy + (sin(a) * rad).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        fillP.color = color
        canvas.drawPath(path, fillP)
    }

    // =========================================================================
    // 1. מירוץ ארקייד וקטורי (5555)
    // =========================================================================
    private fun drawVectorRacer(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val now = System.currentTimeMillis()
        val isTurbo = now < MiniGameEngine.racerTurboBoostUntil
        val padX = 10f * d
        val hudY = 18f * d
        val roadTop = 28f * d
        val roadBot = h - 9f * d
        val roadH = (roadBot - roadTop).coerceAtLeast(36f * d)
        val laneH = roadH / 3f

        // --- HUD: ניקוד + קומבו + לבבות + מד לייזר ---
        textP.textAlign = Paint.Align.LEFT
        textP.textSize = 11.2f * d
        textP.color = pal.inkPrimary
        val comboTag = if (MiniGameEngine.racerCombo > 1) " ×${MiniGameEngine.racerCombo}" else ""
        canvas.drawText("PTS ${MiniGameEngine.racerScore}$comboTag  (HI ${MiniGameEngine.racerBest})", padX + 4f * d, hudY, textP)

        // לבבות חיים במרכז-ימין
        val heartBaseX = w * 0.58f
        for (hp in 0 until 3) {
            drawHeartIcon(canvas, heartBaseX + hp * 13f * d, hudY - 4f * d, 4.6f * d, pal.enemyColor, hp < MiniGameEngine.racerLives)
        }

        // מד טעינת לייזר בימין
        val remMs = MiniGameEngine.getShotCooldownRemainingMs()
        val readyFrac = 1f - (remMs.toFloat() / MiniGameEngine.SHOT_COOLDOWN_MS.toFloat()).coerceIn(0f, 1f)
        val barW = 54f * d
        val barRight = w - padX - 4f * d
        val barLeft = barRight - barW
        val barY = hudY - 4f * d
        rect.set(barLeft, barY - 3.2f * d, barRight, barY + 3.2f * d)
        fillP.color = withAlpha(pal.inkPrimary, 28)
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
        rect.set(barLeft, barY - 3.2f * d, barLeft + barW * readyFrac, barY + 3.2f * d)
        fillP.color = if (remMs == 0L) pal.playerColor else pal.bonusColor
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)

        // --- רקע כביש + שוליים ---
        rect.set(padX, roadTop, w - padX, roadBot)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 14f * d, 14f * d, fillP)
        strokeP.strokeWidth = 1.2f * d
        strokeP.color = if (isTurbo) pal.bonusColor else withAlpha(pal.inkPrimary, 40)
        canvas.drawRoundRect(rect, 14f * d, 14f * d, strokeP)

        // שוליים זוהרים עליון/תחתון
        strokeP.strokeWidth = 2f * d
        strokeP.color = withAlpha(if (isTurbo) pal.bonusColor else pal.controlAccent, 110)
        canvas.drawLine(padX + 12f * d, roadTop + 2f * d, w - padX - 12f * d, roadTop + 2f * d, strokeP)
        canvas.drawLine(padX + 12f * d, roadBot - 2f * d, w - padX - 12f * d, roadBot - 2f * d, strokeP)

        // קווי נתיב זורמים (מהירים יותר בטורבו)
        val speedMul = if (isTurbo) 36f else 18f
        val dashShift = (phase * speedMul * d) % (18f * d)
        dashedP.pathEffect = DashPathEffect(floatArrayOf(10f * d, 8f * d), -dashShift)
        dashedP.color = withAlpha(pal.inkSecondary, 115)
        for (div in 1..2) {
            val y = roadTop + laneH * div
            canvas.drawLine(padX + 8f * d, y, w - padX - 8f * d, y, dashedP)
        }

        // קווי מהירות (Speed Lines) בזמן טורבו
        if (isTurbo) {
            strokeP.strokeWidth = 1.3f * d
            strokeP.color = withAlpha(pal.bonusColor, 140)
            for (sl in 0..4) {
                val sx = padX + 12f * d + (((sl * 57f * d + phase * 90f * d)) % (w - padX * 2 - 40f * d))
                val sy = roadTop + 8f * d + sl * (roadH - 16f * d) / 4f
                canvas.drawLine(sx, sy, sx + 22f * d, sy, strokeP)
            }
        }

        val innerL = padX + 10f * d
        val innerR = w - padX - 10f * d
        val colW = (innerR - innerL).coerceAtLeast(60f * d) / 6.2f
        val carX = innerR - colW * 0.62f
        val carY = roadTop + (smoothCarLane + 0.5f) * laneH

        // גל EMP מתפשט על כל הכביש
        val empAge = now - MiniGameEngine.racerEmpTime
        if (empAge in 0..420L) {
            val ep = empAge / 420f
            strokeP.strokeWidth = (4f * (1f - ep) + 1f) * d
            strokeP.color = withAlpha(pal.shieldColor, ((1f - ep) * 230f).toInt())
            canvas.drawCircle(carX, carY, ep * (w * 0.85f), strokeP)
        }

        // קרן לייזר חיה
        val laserAge = now - MiniGameEngine.racerLaserTime
        if (laserAge in 0..240L && MiniGameEngine.racerLaserLane in 0..2) {
            val alpha = ((1f - laserAge / 240f) * 255f).toInt().coerceIn(0, 255)
            val ly = roadTop + (MiniGameEngine.racerLaserLane + 0.5f) * laneH
            val endX = innerL + (MiniGameEngine.racerLaserEndCol + 0.5f) * colW
            strokeP.strokeWidth = 4.4f * d
            strokeP.color = withAlpha(pal.laserColor, alpha)
            if (pal.isAurora) strokeP.setShadowLayer(8f * d, 0f, 0f, pal.laserColor)
            canvas.drawLine(carX - 18f * d, ly, endX, ly, strokeP)
            strokeP.clearShadowLayer()
            fillP.color = withAlpha(pal.bonusColor, alpha)
            canvas.drawCircle(endX, ly, (5f + (laserAge / 240f) * 9f) * d, fillP)
        }

        // ציור אובייקטים ב-3 הנתיבים
        val itemH = min(laneH * 0.72f, 24f * d)
        for (lane in 0..2) {
            val cy = roadTop + (lane + 0.5f) * laneH
            for (col in 0..4) {
                val cell = MiniGameEngine.racerTracks[lane].getOrElse(col) { "." }
                val cx = innerL + (col + 0.5f) * colW
                when (cell) {
                    "🚧" -> drawVectorBarrier(canvas, cx, cy, itemH, pal, armored = false)
                    "🧱" -> drawVectorBarrier(canvas, cx, cy, itemH, pal, armored = true)
                    "⭐" -> drawMiniStar(canvas, cx, cy, itemH * 0.44f * (1f + sin(phase * 2.2f) * 0.1f), pal.bonusColor)
                    "🛡️" -> drawVectorShieldPickup(canvas, cx, cy, itemH * 0.44f, pal.shieldColor)
                    "❤️" -> drawHeartIcon(canvas, cx, cy, itemH * 0.36f * (1f + sin(phase * 3f) * 0.08f), pal.enemyColor, filled = true)
                }
            }
        }

        // מכונית ספורט עם הטיית פנייה + הבהוב חסינות + בועת מגן
        val isInvuln = now < MiniGameEngine.racerInvulnUntil
        val blinkHide = isInvuln && ((now / 90L) % 2L == 0L)
        if (!blinkHide) {
            canvas.save()
            canvas.rotate(carTiltDeg.coerceIn(-16f, 16f), carX, carY)
            drawVectorSportsCar(canvas, carX, carY, min(laneH * 0.76f, 26f * d), pal, isTurbo)
            canvas.restore()
        }

        if (MiniGameEngine.racerShieldActive) {
            val sr = min(laneH * 0.54f, 24f * d)
            fillP.color = withAlpha(pal.shieldColor, 42)
            canvas.drawCircle(carX, carY, sr, fillP)
            strokeP.strokeWidth = 2f * d
            strokeP.color = withAlpha(pal.shieldColor, (175 + sin(phase * 3f) * 55).toInt())
            canvas.drawCircle(carX, carY, sr, strokeP)
        }

        // פיצוץ סיום התרסקות
        val crashAge = now - MiniGameEngine.crashAnimStartTime
        if (MiniGameEngine.crashAnimStartTime > 0L && crashAge in 0..950L) {
            val p = (crashAge / 900f).coerceIn(0f, 1f)
            val ex = carX - 8f * d
            val ey = roadTop + (MiniGameEngine.crashLane + 0.5f) * laneH
            val blastR = (10f + p * 38f) * d
            val alpha = ((1f - p * 0.6f) * 255f).toInt().coerceIn(0, 255)
            fillP.color = withAlpha(pal.enemyColor, (alpha * 0.45f).toInt())
            canvas.drawCircle(ex, ey, blastR, fillP)
            textP.textAlign = Paint.Align.CENTER
            textP.textSize = 15f * d
            textP.color = pal.enemyColor
            canvas.drawText("CRASH!", w * 0.5f, roadTop + roadH * 0.5f + 5f * d, textP)
        }
    }

    private fun drawVectorBarrier(canvas: Canvas, cx: Float, cy: Float, h: Float, pal: MiniGameEngine.GameThemePalette, armored: Boolean) {
        val bw = h * (if (armored) 0.78f else 0.60f)
        val bh = h * 0.92f
        rect.set(cx - bw * 0.5f, cy - bh * 0.5f, cx + bw * 0.5f, cy + bh * 0.5f)
        fillP.color = if (armored) Color.parseColor("#6D28D9") else pal.enemyColor
        canvas.drawRoundRect(rect, 3.5f * d, 3.5f * d, fillP)
        if (armored) {
            strokeP.strokeWidth = 1.8f * d
            strokeP.color = pal.bonusColor
            canvas.drawRoundRect(rect, 3.5f * d, 3.5f * d, strokeP)
            canvas.drawLine(rect.left + 3f * d, cy, rect.right - 3f * d, cy, strokeP)
        } else {
            strokeP.strokeWidth = 2.0f * d
            strokeP.color = if (pal.isMinimalist) Color.WHITE else pal.bonusColor
            canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.28f, rect.right - 2f * d, rect.top + bh * 0.14f, strokeP)
            canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.60f, rect.right - 2f * d, rect.top + bh * 0.46f, strokeP)
            canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.88f, rect.right - 2f * d, rect.top + bh * 0.74f, strokeP)
        }
    }

    private fun drawVectorShieldPickup(canvas: Canvas, cx: Float, cy: Float, r: Float, col: Int) {
        path.reset()
        path.moveTo(cx, cy - r)
        path.lineTo(cx + r * 0.85f, cy - r * 0.55f)
        path.lineTo(cx + r * 0.85f, cy + r * 0.15f)
        path.quadTo(cx + r * 0.85f, cy + r * 0.8f, cx, cy + r)
        path.quadTo(cx - r * 0.85f, cy + r * 0.8f, cx - r * 0.85f, cy + r * 0.15f)
        path.lineTo(cx - r * 0.85f, cy - r * 0.55f)
        path.close()
        fillP.color = withAlpha(col, 85)
        canvas.drawPath(path, fillP)
        strokeP.strokeWidth = 1.8f * d
        strokeP.color = col
        canvas.drawPath(path, strokeP)
    }

    private fun drawVectorSportsCar(canvas: Canvas, cx: Float, cy: Float, h: Float, pal: MiniGameEngine.GameThemePalette, isTurbo: Boolean) {
        val w = h * 1.68f
        val halfW = w * 0.5f
        val halfH = h * 0.42f

        // אלומות פנסים
        path.reset()
        path.moveTo(cx - halfW, cy - halfH * 0.55f)
        path.lineTo(cx - halfW - 24f * d, cy - halfH * 1.2f)
        path.lineTo(cx - halfW - 24f * d, cy + halfH * 1.2f)
        path.lineTo(cx - halfW, cy + halfH * 0.55f)
        path.close()
        fillP.shader = LinearGradient(cx - halfW, cy, cx - halfW - 24f * d, cy, withAlpha(pal.bonusColor, 82), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawPath(path, fillP)
        fillP.shader = null

        // להבת אגזוז מאחורה (ימין) — מתארכת בטורבו
        val flameLen = (if (isTurbo) 14f else 5f) * d + sin(phase * 6f) * 2.5f * d
        fillP.color = if (isTurbo) pal.shieldColor else pal.bonusColor
        path.reset()
        path.moveTo(cx + halfW, cy - halfH * 0.35f)
        path.lineTo(cx + halfW + flameLen, cy)
        path.lineTo(cx + halfW, cy + halfH * 0.35f)
        path.close()
        canvas.drawPath(path, fillP)

        // גלגלים
        fillP.color = pal.inkPrimary
        val wheelW = w * 0.22f; val wheelH = 3.2f * d
        canvas.drawRoundRect(RectF(cx - halfW * 0.72f, cy - halfH - wheelH * 0.5f, cx - halfW * 0.72f + wheelW, cy - halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx + halfW * 0.26f, cy - halfH - wheelH * 0.5f, cx + halfW * 0.26f + wheelW, cy - halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx - halfW * 0.72f, cy + halfH - wheelH * 0.5f, cx - halfW * 0.72f + wheelW, cy + halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx + halfW * 0.26f, cy + halfH - wheelH * 0.5f, cx + halfW * 0.26f + wheelW, cy + halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)

        // שלדה אווירודינמית + פס מרוץ
        rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        fillP.color = pal.playerColor
        canvas.drawRoundRect(rect, halfH * 0.72f, halfH * 0.72f, fillP)

        rect.set(cx - halfW * 0.30f, cy - halfH * 0.66f, cx + halfW * 0.40f, cy + halfH * 0.66f)
        fillP.color = withAlpha(pal.cardBgTop, 220)
        canvas.drawRoundRect(rect, halfH * 0.45f, halfH * 0.45f, fillP)

        strokeP.strokeWidth = 2.4f * d
        strokeP.color = pal.controlAccent
        canvas.drawLine(cx + halfW - 2f * d, cy - halfH * 0.85f, cx + halfW - 2f * d, cy + halfH * 0.85f, strokeP)

        fillP.color = pal.bonusColor
        canvas.drawCircle(cx - halfW + 2.5f * d, cy - halfH * 0.45f, 2f * d, fillP)
        canvas.drawCircle(cx - halfW + 2.5f * d, cy + halfH * 0.45f, 2f * d, fillP)
    }

    // =========================================================================
    // 2. איקס-עיגול טקטי וקטורי (1337)
    // =========================================================================
    private fun drawVectorTicTacToe(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val now = System.currentTimeMillis()
        val boardSize = (h - 20f * d).coerceAtMost(w * 0.44f).coerceAtLeast(62f * d)
        val boardLeft = 16f * d
        val boardTop = (h - boardSize) * 0.5f
        val cellSize = boardSize / 3f

        rect.set(boardLeft - 4f * d, boardTop - 4f * d, boardLeft + boardSize + 4f * d, boardTop + boardSize + 4f * d)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 12f * d, 12f * d, fillP)

        fun cellCenter(idx: Int): Pair<Float, Float> {
            val col = idx % 3
            val row = 2 - (idx / 3)
            return (boardLeft + (col + 0.5f) * cellSize) to (boardTop + (row + 0.5f) * cellSize)
        }

        strokeP.strokeWidth = 1.6f * d
        strokeP.color = withAlpha(pal.inkPrimary, 55)
        for (i in 1..2) {
            val x = boardLeft + cellSize * i
            val y = boardTop + cellSize * i
            canvas.drawLine(x, boardTop + 4f * d, x, boardTop + boardSize - 4f * d, strokeP)
            canvas.drawLine(boardLeft + 4f * d, y, boardLeft + boardSize - 4f * d, y, strokeP)
        }

        // טבעת רמז מונפשת (אם נלחץ 0)
        if (MiniGameEngine.tttHintCell in 0..8 && now - MiniGameEngine.tttHintTime < 2200L) {
            val (hx, hy) = cellCenter(MiniGameEngine.tttHintCell)
            val hr = cellSize * (0.34f + sin(phase * 4f) * 0.05f)
            strokeP.strokeWidth = 2.2f * d
            strokeP.color = pal.bonusColor
            canvas.drawCircle(hx, hy, hr, strokeP)
        }

        val markR = cellSize * 0.28f
        for (idx in 0..8) {
            val (cx, cy) = cellCenter(idx)
            val animP = if (MiniGameEngine.tttCellAnimStart[idx] > 0L) {
                ((now - MiniGameEngine.tttCellAnimStart[idx]) / 220f).coerceIn(0.15f, 1f)
            } else 1f
            when (val mark = MiniGameEngine.tttBoard[idx]) {
                "×" -> {
                    val rr = markR * animP
                    strokeP.strokeWidth = 3.0f * d
                    strokeP.color = pal.playerColor
                    if (pal.isAurora) strokeP.setShadowLayer(6f * d, 0f, 0f, pal.playerColor)
                    canvas.drawLine(cx - rr, cy - rr, cx + rr, cy + rr, strokeP)
                    canvas.drawLine(cx + rr, cy - rr, cx - rr, cy + rr, strokeP)
                    strokeP.clearShadowLayer()
                }
                "0" -> {
                    strokeP.strokeWidth = 3.0f * d
                    strokeP.color = pal.enemyColor
                    if (pal.isAurora) strokeP.setShadowLayer(6f * d, 0f, 0f, pal.enemyColor)
                    rect.set(cx - markR, cy - markR, cx + markR, cy + markR)
                    canvas.drawArc(rect, -90f, 360f * animP, false, strokeP)
                    strokeP.clearShadowLayer()
                }
                else -> {
                    subTextP.textAlign = Paint.Align.CENTER
                    subTextP.textSize = cellSize * 0.32f
                    subTextP.color = withAlpha(pal.inkSecondary, 130)
                    canvas.drawText(mark, cx, cy + cellSize * 0.11f, subTextP)
                }
            }
        }

        // קו ניצחון מתארך בזמן אמת
        MiniGameEngine.tttWinningLine?.let { line ->
            if (line.size == 3) {
                val lp = ((now - MiniGameEngine.tttWinAnimStart) / 280f).coerceIn(0.1f, 1f)
                val (x1, y1) = cellCenter(line[0])
                val (x2, y2) = cellCenter(line[2])
                strokeP.strokeWidth = 4.2f * d
                strokeP.color = pal.bonusColor
                canvas.drawLine(x1, y1, x1 + (x2 - x1) * lp, y1 + (y2 - y1) * lp, strokeP)
            }
        }

        // פאנל לוח תוצאות ומצבים מימין
        val rightCx = (boardLeft + boardSize + w - 10f * d) * 0.5f
        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 13f * d
        textP.color = pal.inkPrimary
        canvas.drawText("XO  •  מצב: ${MiniGameEngine.tttModeLabel()}", rightCx, h * 0.22f, textP)

        // ניקוד טורניר
        textP.textSize = 11.5f * d
        textP.color = pal.playerColor
        canvas.drawText("× ${MiniGameEngine.tttPlayerWins}", rightCx + 44f * d, h * 0.42f, textP)
        textP.color = pal.inkSecondary
        canvas.drawText("= ${MiniGameEngine.tttDraws}", rightCx, h * 0.42f, textP)
        textP.color = pal.enemyColor
        canvas.drawText("0 ${MiniGameEngine.tttCpuWins}", rightCx - 44f * d, h * 0.42f, textP)

        textP.textSize = 12.2f * d
        textP.color = pal.controlAccent
        canvas.drawText(MiniGameEngine.tttStatusText, rightCx, h * 0.65f, textP)

        subTextP.textAlign = Paint.Align.CENTER
        subTextP.textSize = 9.2f * d
        subTextP.color = pal.inkSecondary
        canvas.drawText("+קל | −בינוני | ×מומחה | ÷2P | 0רמז", rightCx, h * 0.85f, subTextP)
    }

    // =========================================================================
    // 3. פולשי החשבון וקטורי — גלים, שריון, בוס וגל NOVA (8080)
    // =========================================================================
    private fun drawVectorInvaders(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val now = System.currentTimeMillis()
        val padX = 10f * d
        val hudY = 18f * d
        val arenaTop = 28f * d
        val arenaBot = h - 9f * d
        val arenaH = (arenaBot - arenaTop).coerceAtLeast(36f * d)
        val cy = arenaTop + arenaH * 0.5f

        textP.textAlign = Paint.Align.LEFT
        textP.textSize = 11.2f * d
        textP.color = pal.inkPrimary
        val comboTxt = if (MiniGameEngine.invaderCombo >= 4) " ×${(1 + MiniGameEngine.invaderCombo / 4).coerceAtMost(4)}" else ""
        canvas.drawText("W${MiniGameEngine.invaderWave} • ${MiniGameEngine.invaderScore}$comboTxt (HI ${MiniGameEngine.invaderBest})", padX + 4f * d, hudY, textP)

        for (hp in 0 until 3) {
            drawHeartIcon(canvas, w - padX - 10f * d - hp * 13f * d, hudY - 4f * d, 4.6f * d, pal.enemyColor, hp < MiniGameEngine.invaderHp)
        }

        rect.set(padX, arenaTop, w - padX, arenaBot)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 14f * d, 14f * d, fillP)

        if (now - MiniGameEngine.invaderHitFlashTime < 220L) {
            fillP.color = withAlpha(pal.enemyColor, 55)
            canvas.drawRoundRect(rect, 14f * d, 14f * d, fillP)
        }

        // שדה כוכבים דו-שכבתי (Parallax)
        val spanW = (w - padX * 2 - 20f * d).coerceAtLeast(20f)
        for (s in 0 until 12) {
            val layerSpd = if (s % 2 == 0) 14f else 7f
            val sx = padX + 10f * d + (((s * 39f * d - phase * layerSpd * d) % spanW) + spanW) % spanW
            val sy = arenaTop + 7f * d + ((s * 23f) % (arenaH - 14f * d).coerceAtLeast(10f))
            fillP.color = withAlpha(pal.inkSecondary, if (s % 2 == 0) 95 else 55)
            canvas.drawCircle(sx, sy, if (s % 2 == 0) 1.7f * d else 1.1f * d, fillP)
        }

        // חללית וקטורית עם רתיעה (Recoil) בעת ירי
        val recoilAge = now - MiniGameEngine.invaderShipRecoilTime
        val recoilDx = if (recoilAge in 0..140L) -(1f - recoilAge / 140f) * 5f * d else 0f
        val shipX = padX + 24f * d + recoilDx
        val shipR = min(arenaH * 0.32f, 16f * d)

        // להבת מנוע
        fillP.color = pal.bonusColor
        val flameLen = (7f + sin(phase * 5f) * 3f) * d
        path.reset()
        path.moveTo(shipX - shipR * 0.8f, cy - shipR * 0.38f)
        path.lineTo(shipX - shipR * 0.8f - flameLen, cy)
        path.lineTo(shipX - shipR * 0.8f, cy + shipR * 0.38f)
        path.close()
        canvas.drawPath(path, fillP)

        // גוף החללית + חופה
        path.reset()
        path.moveTo(shipX + shipR * 1.2f, cy)
        path.lineTo(shipX - shipR * 0.85f, cy - shipR * 0.86f)
        path.lineTo(shipX - shipR * 0.42f, cy)
        path.lineTo(shipX - shipR * 0.85f, cy + shipR * 0.86f)
        path.close()
        fillP.color = pal.playerColor
        canvas.drawPath(path, fillP)
        fillP.color = pal.cardBgTop
        canvas.drawCircle(shipX + shipR * 0.1f, cy, shipR * 0.24f, fillP)

        // גל NOVA מתפשט
        val novaAge = now - MiniGameEngine.invaderNovaTime
        if (novaAge in 0..420L) {
            val np = novaAge / 420f
            strokeP.strokeWidth = (4.5f * (1f - np) + 1f) * d
            strokeP.color = withAlpha(pal.playerColor, ((1f - np) * 240f).toInt())
            canvas.drawCircle(shipX, cy, np * w, strokeP)
        }

        val trackL = shipX + shipR + 12f * d
        val trackR = w - padX - 10f * d
        val colW = ((trackR - trackL) / 6f).coerceAtLeast(20f * d)

        val laserAge = now - MiniGameEngine.invaderLaserTime
        if (laserAge in 0..220L && MiniGameEngine.invaderLaserCol in 0..5) {
            val alpha = ((1f - laserAge / 220f) * 255f).toInt().coerceIn(0, 255)
            val tx = trackL + (MiniGameEngine.invaderLaserCol + 0.5f) * colW
            strokeP.strokeWidth = 3.8f * d
            strokeP.color = withAlpha(pal.laserColor, alpha)
            canvas.drawLine(shipX + shipR, cy, tx, cy, strokeP)
            fillP.color = withAlpha(pal.bonusColor, alpha)
            canvas.drawCircle(tx, cy, (6f + (laserAge / 220f) * 10f) * d, fillP)
        }

        val firstEnemyIdx = MiniGameEngine.invaderTrack.indexOfFirst { it != "." }
        val podR = min(colW * 0.43f, arenaH * 0.35f).coerceAtLeast(12f * d)
        for (i in 0 until MiniGameEngine.invaderTrack.size) {
            val cell = MiniGameEngine.invaderTrack[i]
            val px = trackL + (i + 0.5f) * colW
            if (cell == ".") {
                fillP.color = withAlpha(pal.inkSecondary, 55)
                canvas.drawCircle(px, cy, 1.8f * d, fillP)
                continue
            }
            val isBoss = (cell == "BOSS")
            val sym = if (isBoss) MiniGameEngine.invaderBossSymbol else cell
            val isTarget = (i == firstEnemyIdx)
            val podCol = when {
                isBoss -> pal.enemyColor
                i == 0 -> pal.enemyColor
                isTarget -> pal.bonusColor
                else -> pal.controlAccent
            }
            val rr = podR * (if (isBoss) 1.24f else if (isTarget) 1.08f else 0.95f)
            drawHexPod(canvas, px, cy, rr, withAlpha(podCol, if (isBoss) 72 else 42), podCol)

            if (isBoss) {
                // מד חיים מעל ספינת הבוס
                val bw = rr * 1.6f
                val hpFrac = (MiniGameEngine.invaderBossHp.toFloat() / MiniGameEngine.invaderBossMaxHp.coerceAtLeast(1)).coerceIn(0f, 1f)
                rect.set(px - bw * 0.5f, cy - rr - 7f * d, px + bw * 0.5f, cy - rr - 3f * d)
                fillP.color = withAlpha(pal.inkPrimary, 45)
                canvas.drawRoundRect(rect, 2f * d, 2f * d, fillP)
                rect.set(px - bw * 0.5f, cy - rr - 7f * d, px - bw * 0.5f + bw * hpFrac, cy - rr - 3f * d)
                fillP.color = pal.enemyColor
                canvas.drawRoundRect(rect, 2f * d, 2f * d, fillP)
            }

            textP.textAlign = Paint.Align.CENTER
            textP.textSize = (if (sym.length >= 3) 9.5f else 13f) * d
            textP.color = pal.inkPrimary
            canvas.drawText(sym, px, cy + 4f * d, textP)
        }
    }

    private fun drawHexPod(canvas: Canvas, cx: Float, cy: Float, r: Float, fillCol: Int, strokeCol: Int) {
        path.reset()
        for (k in 0 until 6) {
            val a = Math.toRadians(k * 60.0 - 30.0)
            val x = cx + (cos(a) * r).toFloat()
            val y = cy + (sin(a) * r).toFloat()
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        fillP.color = fillCol
        canvas.drawPath(path, fillP)
        strokeP.strokeWidth = 1.9f * d
        strokeP.color = strokeCol
        canvas.drawPath(path, strokeP)
    }

    // =========================================================================
    // 4. נטרול פצצה מתקדם וקטורי — הקפאת זמן, דופק LED ו-4 חוטים (1010)
    // =========================================================================
    private fun drawVectorBomb(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val now = System.currentTimeMillis()
        val isFrozen = now < MiniGameEngine.bombFrozenUntil
        val isUrgent = !isFrozen && MiniGameEngine.bombTimeLeft <= 5
        val ringCol = when {
            isFrozen -> pal.shieldColor
            isUrgent -> pal.enemyColor
            MiniGameEngine.bombTimeLeft <= 9 -> pal.bonusColor
            else -> pal.playerColor
        }

        val bombCx = w * 0.21f
        val bombCy = h * 0.54f
        val heartbeat = if (isUrgent) 1f + sin(phase * 6f) * 0.05f else 1f
        val bombR = min(h * 0.29f, 33f * d) * heartbeat
        val frac = (MiniGameEngine.bombTimeLeft.toFloat() / MiniGameEngine.bombMaxTime).coerceIn(0f, 1f)

        // טבעת זמן חיצונית
        val arcR = bombR + 7f * d
        rect.set(bombCx - arcR, bombCy - arcR, bombCx + arcR, bombCy + arcR)
        strokeP.strokeWidth = 4.6f * d
        strokeP.color = withAlpha(pal.inkPrimary, 30)
        canvas.drawCircle(bombCx, bombCy, arcR, strokeP)
        strokeP.color = ringCol
        canvas.drawArc(rect, -90f, 360f * frac, false, strokeP)

        // גוף הפצצה + כפור בהקפאת זמן
        fillP.color = if (isFrozen) withAlpha(pal.shieldColor, 95) else if (pal.isMinimalist) Color.parseColor("#374151") else pal.arenaBg
        canvas.drawCircle(bombCx, bombCy, bombR, fillP)
        strokeP.strokeWidth = 2f * d
        strokeP.color = ringCol
        canvas.drawCircle(bombCx, bombCy, bombR, strokeP)

        // פתיל מתקצר וניצוץ
        val fuseLen = (4f + frac * 10f) * d
        strokeP.strokeWidth = 2.2f * d
        strokeP.color = pal.inkSecondary
        canvas.drawLine(bombCx, bombCy - bombR, bombCx + fuseLen * 0.7f, bombCy - bombR - fuseLen, strokeP)
        if (!isFrozen) {
            fillP.color = if ((phase * 5f).toInt() % 2 == 0) pal.bonusColor else pal.enemyColor
            canvas.drawCircle(bombCx + fuseLen * 0.7f, bombCy - bombR - fuseLen, (3f + sin(phase * 6f) * 1.2f) * d, fillP)
        }

        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 15.5f * d
        textP.color = if (pal.isMinimalist && !isFrozen) Color.WHITE else ringCol
        canvas.drawText(if (isFrozen) "❄${MiniGameEngine.bombTimeLeft}s" else "${MiniGameEngine.bombTimeLeft}s", bombCx, bombCy + 5.5f * d, textP)

        // פאנל משוואה וחוטי נטרול מימין
        val panelL = w * 0.40f
        val panelR = w - 12f * d
        val panelCx = (panelL + panelR) * 0.5f

        subTextP.textAlign = Paint.Align.CENTER
        subTextP.textSize = 10.6f * d
        subTextP.color = pal.inkSecondary
        val streakStr = if (MiniGameEngine.bombStreak >= 2) " | רצף ${MiniGameEngine.bombStreak}" else ""
        canvas.drawText("נוטרלו: ${MiniGameEngine.bombScore} (HI ${MiniGameEngine.bombBest})$streakStr", panelCx, 20f * d, subTextP)

        rect.set(panelL, 28f * d, panelR, h * 0.63f)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 12f * d, 12f * d, fillP)
        strokeP.strokeWidth = 1.5f * d
        strokeP.color = if (isFrozen) pal.shieldColor else if (isUrgent) pal.enemyColor else pal.controlAccent
        canvas.drawRoundRect(rect, 12f * d, 12f * d, strokeP)

        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 18f * d
        textP.color = pal.inkPrimary
        val eq = "\u200E${MiniGameEngine.bombLhs}  [ ? ]  ${MiniGameEngine.bombRhs}  =  ${MiniGameEngine.bombTarget}"
        canvas.drawText(eq, panelCx, (28f * d + h * 0.63f) * 0.5f + 6.5f * d, textP)

        // 4 תגי חוטי אופרטורים עם הבהוב פידבק
        val ops = listOf("+", "−", "×", "÷")
        val wireCols = listOf(pal.playerColor, pal.enemyColor, pal.bonusColor, pal.controlAccent)
        val chipW = ((panelR - panelL) - 18f * d) / 4f
        val chipTop = h * 0.71f
        val chipBot = h - 10f * d
        val cutFresh = (now - MiniGameEngine.bombLastCutTime) < 280L

        for (i in 0..3) {
            val cl = panelL + i * (chipW + 6f * d)
            val isCutWire = cutFresh && MiniGameEngine.bombLastCutOp == ops[i]
            val col = if (isCutWire) (if (MiniGameEngine.bombLastCutCorrect) pal.playerColor else pal.enemyColor) else wireCols[i]
            rect.set(cl, chipTop, cl + chipW, chipBot)
            fillP.color = withAlpha(col, if (isCutWire) 140 else 45)
            canvas.drawRoundRect(rect, 8f * d, 8f * d, fillP)
            strokeP.strokeWidth = 1.5f * d
            strokeP.color = col
            canvas.drawRoundRect(rect, 8f * d, 8f * d, strokeP)
            textP.textSize = 14f * d
            textP.color = pal.inkPrimary
            canvas.drawText(ops[i], cl + chipW * 0.5f, (chipTop + chipBot) * 0.5f + 5f * d, textP)
        }
    }

    // =========================================================================
    // 5. תזמורת חיות וקטוריות + אתגר זיכרון (7777)
    // =========================================================================
    private fun drawVectorAnimalParty(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val now = System.currentTimeMillis()
        val padX = 12f * d

        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 11.5f * d
        textP.color = pal.inkPrimary
        val title = if (MiniGameEngine.animalSimonMode) {
            val turn = if (MiniGameEngine.animalSimonShowing) "הקשב לרצף..." else "חזור במקשים 1–5!"
            "זיכרון חיות: ${MiniGameEngine.animalSimonScore} (HI ${MiniGameEngine.animalSimonBest}) • $turn"
        } else {
            "תזמורת חיות (${MiniGameEngine.animalTimeLeft}ש') • 0=מקהלה | =אתגר זיכרון"
        }
        canvas.drawText(title, w * 0.5f, 20f * d, textP)

        // פס זמן או פס התקדמות שלב
        val frac = if (MiniGameEngine.animalSimonMode) {
            val total = MiniGameEngine.animalSimonSeq.size.coerceAtLeast(1)
            (MiniGameEngine.animalSimonInputPos.toFloat() / total).coerceIn(0f, 1f)
        } else {
            (MiniGameEngine.animalTimeLeft.toFloat() / MiniGameEngine.animalTotalTime.coerceAtLeast(1)).coerceIn(0f, 1f)
        }
        rect.set(padX, 27f * d, w - padX, 33f * d)
        fillP.color = withAlpha(pal.inkPrimary, 28)
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
        rect.set(padX, 27f * d, padX + (w - padX * 2) * frac, 33f * d)
        fillP.color = if (MiniGameEngine.animalSimonMode) pal.bonusColor else pal.playerColor
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)

        val cy = (36f * d + h - 10f * d) * 0.52f
        val slotW = (w - padX * 2) / 5f
        val r = min(slotW * 0.34f, (h - 56f * d) * 0.34f).coerceAtLeast(13f * d)
        val animalColors = listOf(
            Color.parseColor("#E6A96B"), // 1: כלב
            Color.parseColor("#F48FB1"), // 2: חתול
            Color.parseColor("#FF8A65"), // 3: שועל
            Color.parseColor("#90A4AE"), // 4: פנדה
            Color.parseColor("#81C784")  // 5: צפרדע
        )
        val bounceAge = now - MiniGameEngine.animalBounceTime
        val chorusActive = (now - MiniGameEngine.animalChorusTime) in 0..420L

        for (i in 0 until 5) {
            val cx = padX + slotW * (i + 0.5f)
            val isActive = chorusActive || (i == MiniGameEngine.animalActiveIdx)
            val hop = if (isActive && bounceAge in 0..340L) {
                -sin((bounceAge / 340f) * PI).toFloat() * 13f * d
            } else {
                sin(phase * 1.8f + i * 1.2f) * 2.5f * d
            }
            val ay = cy + hop

            // זרקור במה מתחת לחיה הפעילה
            if (isActive && bounceAge in 0..420L) {
                val wp = bounceAge / 420f
                strokeP.strokeWidth = 2f * d
                strokeP.color = withAlpha(pal.bonusColor, ((1f - wp) * 220f).toInt())
                canvas.drawCircle(cx, ay, r + wp * 18f * d, strokeP)
                // תו מוזיקלי צף
                textP.textSize = 11f * d
                textP.color = withAlpha(pal.controlAccent, ((1f - wp) * 255f).toInt())
                canvas.drawText(if (i % 2 == 0) "♪" else "♫", cx + r * 0.8f, ay - r - wp * 10f * d, textP)
            }

            val bodyCol = animalColors[i]
            fillP.color = bodyCol
            when (i) {
                0 -> {
                    canvas.drawOval(RectF(cx - r * 1.12f, ay - r * 0.45f, cx - r * 0.58f, ay + r * 0.55f), fillP)
                    canvas.drawOval(RectF(cx + r * 0.58f, ay - r * 0.45f, cx + r * 1.12f, ay + r * 0.55f), fillP)
                }
                1, 2 -> {
                    path.reset()
                    path.moveTo(cx - r * 0.78f, ay - r * 0.25f)
                    path.lineTo(cx - r * 0.68f, ay - r * 1.18f)
                    path.lineTo(cx - r * 0.12f, ay - r * 0.65f)
                    path.close()
                    canvas.drawPath(path, fillP)
                    path.reset()
                    path.moveTo(cx + r * 0.78f, ay - r * 0.25f)
                    path.lineTo(cx + r * 0.68f, ay - r * 1.18f)
                    path.lineTo(cx + r * 0.12f, ay - r * 0.65f)
                    path.close()
                    canvas.drawPath(path, fillP)
                }
                else -> {
                    fillP.color = if (i == 3) Color.parseColor("#263238") else bodyCol
                    canvas.drawCircle(cx - r * 0.62f, ay - r * 0.72f, r * 0.34f, fillP)
                    canvas.drawCircle(cx + r * 0.62f, ay - r * 0.72f, r * 0.34f, fillP)
                }
            }

            fillP.color = if (i == 3) Color.WHITE else bodyCol
            canvas.drawCircle(cx, ay, r, fillP)
            strokeP.strokeWidth = 1.4f * d
            strokeP.color = withAlpha(pal.inkPrimary, 95)
            canvas.drawCircle(cx, ay, r, strokeP)

            // עיניים מחייכות בקפיצה או עגולות במנוחה + לחיים ורודות
            val isHappy = isActive && bounceAge in 0..340L
            if (isHappy) {
                strokeP.strokeWidth = 1.8f * d
                strokeP.color = Color.parseColor("#1F2937")
                canvas.drawArc(RectF(cx - r * 0.50f, ay - r * 0.24f, cx - r * 0.18f, ay + r * 0.04f), 180f, 180f, false, strokeP)
                canvas.drawArc(RectF(cx + r * 0.18f, ay - r * 0.24f, cx + r * 0.50f, ay + r * 0.04f), 180f, 180f, false, strokeP)
            } else {
                fillP.color = Color.parseColor("#1F2937")
                canvas.drawCircle(cx - r * 0.34f, ay - r * 0.12f, r * 0.13f, fillP)
                canvas.drawCircle(cx + r * 0.34f, ay - r * 0.12f, r * 0.13f, fillP)
            }
            fillP.color = Color.argb(95, 244, 114, 182)
            canvas.drawCircle(cx - r * 0.52f, ay + r * 0.16f, r * 0.13f, fillP)
            canvas.drawCircle(cx + r * 0.52f, ay + r * 0.16f, r * 0.13f, fillP)

            fillP.color = Color.parseColor("#1F2937")
            canvas.drawCircle(cx, ay + r * 0.18f, r * 0.11f, fillP)

            // מספר מקש קטן מתחת לחיה (שימושי במיוחד באתגר הזיכרון)
            subTextP.textAlign = Paint.Align.CENTER
            subTextP.textSize = 9f * d
            subTextP.color = pal.inkSecondary
            canvas.drawText("${i + 1}", cx, cy + r + 11f * d, subTextP)
        }
    }
}

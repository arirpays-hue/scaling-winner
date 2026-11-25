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
        val controlAccent: Int,
        val laserColor: Int,
        val glowColor: Int,
        val isMinimalist: Boolean,
        val isAurora: Boolean,
        val isGlass: Boolean
    )

    var isActive = false
    internal var currentGame = ""
    private val handler = Handler(Looper.getMainLooper())
    private var gameLoop: Runnable? = null
    private var cooldownTicker: Runnable? = null
    private var roundResetRunnable: Runnable? = null
    private var gameOverDelayRunnable: Runnable? = null
    private var gameEnding = false

    // מארח הקנבס הווקטורי בתוך צג המחשבון (displayCard)
    private var vectorStageView: MiniGameVectorStageView? = null
    private var attachedDisplayCard: LinearLayout? = null
    private var savedDisplayPadding: IntArray? = null

    // נגני אודיו — הפרדה מלאה בין לולאת המנוע לצלילי אפקטים חד-פעמיים
    private var engineLoopPlayer: MediaPlayer? = null
    private var currentEngineStage = 0
    private var sfxPlayer: MediaPlayer? = null
    private val activeSynthThreads = AtomicInteger(0)
    @Volatile private var synthEngineRunning = false

    // איקס עיגול חכם (1337)
    internal val tttBoard = Array(9) { "" }
    internal var tttStatusText = ""
    internal var tttWinningLine: IntArray? = null
    private var tttRoundLocked = false

    // פולשי החשבון (8080)
    internal var invaderScore = 0
    internal var invaderTrack = mutableListOf<String>()
    internal var invaderLaserCol = -1
    internal var invaderLaserTime = 0L
    private val invaderSymbols = listOf("+", "−", "×", "÷", "sin", "cos", "tan", "√")

    // מירוץ מכוניות (5555)
    internal var carLane = 1 // 0 = עליון (8), 1 = אמצעי, 2 = תחתון (2)
    internal var racerScore = 0
    internal val racerTracks = Array(3) { MutableList(5) { "." } }
    internal var lastShotTime = 0L
    internal var racerLaserLane = -1
    internal var racerLaserEndCol = 0
    internal var racerLaserTime = 0L
    internal var crashAnimStartTime = 0L
    internal var crashLane = 1
    private var lastCooldownToastTime = 0L
    internal const val SHOT_COOLDOWN_MS = 3000L
    private var pendingGuardedStarLane = -1

    // נטרול פצצה (1010)
    internal var bombTimeLeft = 15
    internal var bombScore = 0
    internal var bombLhs = 2
    internal var bombRhs = 3
    internal var bombTarget = 6
    internal var bombShakeTime = 0L
    private var expectedOp = "×"

    // מסיבת חיות מצפצפות (7777)
    var isAnimalParty = false
    internal var animalTimeLeft = 15
    internal var animalTotalTime = 15
    internal var animalActiveIdx = 0
    internal var animalBounceTime = 0L
    private val savedKeyLabels = mutableMapOf<String, CharSequence>()
    private val animalEmojis = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
        "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔"
    )

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
                arenaBg = Color.parseColor("#F3EFE8"),
                arenaGrid = Color.argb(32, 44, 44, 44),
                inkPrimary = MinimalistTheme.TEXT_CHARCOAL,
                inkSecondary = MinimalistTheme.TEXT_MUTED_GREY,
                playerColor = Color.parseColor("#2E7D32"),
                enemyColor = Color.parseColor("#C62828"),
                bonusColor = Color.parseColor("#C67C00"),
                controlAccent = Color.parseColor("#1976D2"),
                laserColor = Color.parseColor("#00897B"),
                glowColor = Color.TRANSPARENT,
                isMinimalist = true,
                isAurora = false,
                isGlass = false
            )
            isAur -> GameThemePalette(
                cardBgTop = AuroraBorealisTheme.EMERALD_NIGHT,
                cardBgBottom = Color.parseColor("#081B26"),
                cardBorder = Color.argb(185, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY)),
                arenaBg = Color.argb(170, 5, 16, 24),
                arenaGrid = Color.argb(38, 0, 245, 212),
                inkPrimary = Color.WHITE,
                inkSecondary = AuroraBorealisTheme.EXPRESSION_CYAN_GREY,
                playerColor = Color.parseColor("#00F5D4"),
                enemyColor = Color.parseColor("#FF4D8D"),
                bonusColor = Color.parseColor("#FFD166"),
                controlAccent = Color.parseColor("#38BDF8"),
                laserColor = Color.parseColor("#00F5D4"),
                glowColor = AuroraBorealisTheme.AURORA_PRIMARY,
                isMinimalist = false,
                isAurora = true,
                isGlass = false
            )
            isGlass -> GameThemePalette(
                cardBgTop = Color.argb(82, 255, 255, 255),
                cardBgBottom = Color.argb(34, 255, 255, 255),
                cardBorder = Color.argb(135, 255, 255, 255),
                arenaBg = Color.argb(56, 12, 20, 34),
                arenaGrid = Color.argb(36, 255, 255, 255),
                inkPrimary = Color.WHITE,
                inkSecondary = Color.parseColor("#CBD5E1"),
                playerColor = Color.parseColor("#34D399"),
                enemyColor = Color.parseColor("#FB7185"),
                bonusColor = Color.parseColor("#FBBF24"),
                controlAccent = Color.parseColor("#7DD3FC"),
                laserColor = Color.parseColor("#38BDF8"),
                glowColor = Color.argb(110, 255, 255, 255),
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
                    cardBorder = Color.argb(130, Color.red(accent), Color.green(accent), Color.blue(accent)),
                    arenaBg = activity.bgColor,
                    arenaGrid = Color.argb(32, Color.red(accent), Color.green(accent), Color.blue(accent)),
                    inkPrimary = Color.WHITE,
                    inkSecondary = Color.parseColor("#94A3B8"),
                    playerColor = Color.parseColor("#4ADE80"),
                    enemyColor = Color.parseColor("#F87171"),
                    bonusColor = Color.parseColor("#FACC15"),
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
        gameEnding = false
        crashAnimStartTime = 0L
        racerLaserTime = 0L
        invaderLaserTime = 0L
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
    // מערכת אודיו חכמה: מוצאת אוטומטית כל קובץ crash/shot/star/engine ב-res/raw
    // ולא קוטעת את צליל ההתרסקות בסיום המשחק!
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
            listOf(523.25 to 120, 659.25 to 120, 783.99 to 140, 1046.50 to 260)
        } else {
            listOf(311.13 to 180, 277.18 to 180, 246.94 to 180, 196.00 to 320)
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
                    val baseFreq = 65.0 + (gearStage * 22.0) + (if (tick % 4 < 2) 5.0 else -3.0)
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

        val sampleRate = 22050
        val durationMs = when (type) { "SHOOT" -> 160; "CRASH" -> 480; else -> 240 }
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val p = i.toDouble() / numSamples
            val sample = when (type) {
                "SHOOT" -> {
                    val freq = 1500.0 - (p * 1180.0)
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
            buffer[i] = (sample * Short.MAX_VALUE * 0.65).toInt().toShort()
        }
        playStaticPcmAsync(sampleRate, buffer, durationMs.toLong())
    }

    fun playSqueakSound(enabled: Boolean = true) {
        if (!enabled) return
        val sampleRate = 22050
        val durationMs = 140
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = if (progress < 0.45) 1100.0 + (progress / 0.45) * 1600.0 else 2700.0 - ((progress - 0.45) / 0.55) * 1300.0
            val envelope = sin(progress * PI)
            phase += 2.0 * PI * freq / sampleRate
            val sample = (sin(phase) * 0.8 + sin(phase * 2.0) * 0.2) * envelope
            buffer[i] = (sample * Short.MAX_VALUE * 0.68).toInt().toShort()
        }
        playStaticPcmAsync(sampleRate, buffer, durationMs.toLong())
    }

    // סיום דרמטי עם שמירה מלאה על צליל ההתרסקות ואנימציית הפיצוץ לפני חזרה למחשבון
    private fun triggerCrashAndExit(activity: MainActivity, endMessage: String) {
        if (gameEnding) return
        gameEnding = true
        stopPreviousLoop()
        stopRacerAudio() // עוצר רק את המנוע הלולאתי — משאיר את צליל ההתרסקות פועל עד תומו!
        crashAnimStartTime = System.currentTimeMillis()
        crashLane = carLane
        playRacerSoundEffect(activity, "CRASH")
        activity.shakeScreen()
        invalidateStage()

        val exitTask = Runnable {
            if (isActive && isActivityValid(activity)) {
                // stopSfx = false כדי שצליל ההתרסקות יסיים להתנגן במלואו ולא ייחתך!
                stopGameInternal(activity, endMessage, stopSfx = false)
            }
        }
        gameOverDelayRunnable = exitTask
        handler.postDelayed(exitTask, 850L)
    }

    // =========================================================================
    // משחק 0: מסיבת חיות מצפצפות (קוד 7777)
    // =========================================================================
    fun startAnimalParty(activity: MainActivity) {
        prepareNewGame(activity, "ANIMALS")
        isAnimalParty = true
        animalTimeLeft = 15
        animalTotalTime = 15
        animalActiveIdx = 0
        animalBounceTime = System.currentTimeMillis()

        val shuffled = animalEmojis.shuffled()
        var idx = 0
        for ((k, btn) in activity.keypadButtons) {
            savedKeyLabels[k] = btn.text
            if (k != "AC") {
                btn.text = shuffled[idx % shuffled.size]
                idx++
            }
        }
        playSqueakSound(activity.useSound)
        invalidateStage()

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "ANIMALS" || !isActivityValid(activity)) return
                animalTimeLeft--
                if (animalTimeLeft <= 0) {
                    stopGameInternal(activity, "🐾 החיות חזרו לגן החיות!", stopSfx = false)
                    return
                }
                invalidateStage()
                handler.postDelayed(this, 1000L)
            }
        }
        handler.postDelayed(gameLoop!!, 1000L)
    }

    // =========================================================================
    // משחק 1: איקס עיגול חכם על מקלדת 1-9 (קוד 1337)
    // =========================================================================
    fun startTicTacToe(activity: MainActivity) {
        prepareNewGame(activity, "TTT")
        for (i in 0..8) {
            val k = (i + 1).toString()
            activity.keypadButtons[k]?.let { savedKeyLabels[k] = it.text }
            tttBoard[i] = k
        }
        updateTicTacToeUI(activity, "תורך! בחר 1–9")
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

    private fun getSmartComputerMove(): Int {
        val free = (0..8).filter { tttBoard[it] !in listOf("×", "0") }
        if (free.isEmpty()) return -1
        for (i in free) {
            val orig = tttBoard[i]; tttBoard[i] = "0"
            if (checkWinner() == "0") { tttBoard[i] = orig; return i }
            tttBoard[i] = orig
        }
        for (i in free) {
            val orig = tttBoard[i]; tttBoard[i] = "×"
            if (checkWinner() == "×") { tttBoard[i] = orig; return i }
            tttBoard[i] = orig
        }
        if (Random.nextFloat() < 0.82f) {
            var bestScore = Int.MIN_VALUE
            val bestMoves = mutableListOf<Int>()
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = "0"
                val score = minimax(0, false)
                tttBoard[i] = orig
                if (score > bestScore) { bestScore = score; bestMoves.clear(); bestMoves.add(i) }
                else if (score == bestScore) bestMoves.add(i)
            }
            if (bestMoves.isNotEmpty()) return bestMoves.random()
        }
        return free.random()
    }

    private fun minimax(depth: Int, isMaximizing: Boolean): Int {
        when (checkWinner()) {
            "0" -> return 10 - depth
            "×" -> return depth - 10
            "TIE" -> return 0
        }
        val free = (0..8).filter { tttBoard[it] !in listOf("×", "0") }
        if (isMaximizing) {
            var best = Int.MIN_VALUE
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = "0"
                best = maxOf(best, minimax(depth + 1, false))
                tttBoard[i] = orig
            }
            return best
        } else {
            var best = Int.MAX_VALUE
            for (i in free) {
                val orig = tttBoard[i]; tttBoard[i] = "×"
                best = minOf(best, minimax(depth + 1, true))
                tttBoard[i] = orig
            }
            return best
        }
    }

    private fun finishTicTacToe(activity: MainActivity, result: Pair<String, IntArray?>) {
        tttRoundLocked = true
        tttWinningLine = result.second
        val winner = result.first
        when (winner) {
            "×" -> playTttResultSound(activity, true)
            "0" -> playTttResultSound(activity, false)
        }
        val msg = when (winner) {
            "×" -> "ניצחת! סיבוב חדש..."
            "0" -> "המחשב ניצח! נסה שוב"
            else -> "תיקו! סיבוב חדש..."
        }
        updateTicTacToeUI(activity, msg)
        roundResetRunnable?.let { handler.removeCallbacks(it) }
        val resetTask = Runnable {
            if (isActive && currentGame == "TTT" && isActivityValid(activity)) {
                startTicTacToe(activity)
            }
        }
        roundResetRunnable = resetTask
        handler.postDelayed(resetTask, 2100L)
    }

    // =========================================================================
    // משחק 2: פולשי החשבון במחשבון המדעי (קוד 8080)
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
        invaderTrack = MutableList(6) { "." }

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "INVADERS" || !isActivityValid(activity)) return
                val reachedShip = invaderTrack.removeAt(0)
                if (reachedShip != ".") {
                    triggerCrashAndExit(activity, "💥 החללית נפגעה! ניקוד סופי: $invaderScore")
                    return
                }
                val nextSpawn = if (Random.nextFloat() < 0.65f) invaderSymbols.random() else "."
                invaderTrack.add(nextSpawn)
                renderInvaders(activity)
                val speed = (950L - (invaderScore * 25L)).coerceAtLeast(350L)
                handler.postDelayed(this, speed)
            }
        }
        renderInvaders(activity)
        handler.postDelayed(gameLoop!!, 900L)
    }

    private fun renderInvaders(activity: MainActivity) {
        val pal = resolvePalette(activity)
        val nextTarget = invaderTrack.firstOrNull { it != "." }
        for (sym in invaderSymbols) {
            val isNext = (sym == nextTarget)
            styleKeyForGame(activity, sym, if (isNext) pal.bonusColor else pal.controlAccent, glow = isNext)
        }
        invalidateStage()
    }

    // =========================================================================
    // משחק 3: מירוץ מכוניות וקטורי (קוד 5555)
    // =========================================================================
    fun startMathRacer(activity: MainActivity) {
        prepareNewGame(activity, "RACER")
        carLane = 1
        racerScore = 0
        lastShotTime = 0L
        lastCooldownToastTime = 0L
        pendingGuardedStarLane = -1
        for (lane in 0..2) racerTracks[lane] = MutableList(5) { "." }

        for (k in listOf("8", "2", "5", "6")) {
            activity.keypadButtons[k]?.let { savedKeyLabels[k] = it.text }
        }
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
                handler.postDelayed(this, 250L)
            }
        }
        renderRacer(activity)
        handler.postDelayed(gameLoop!!, currentRacerDelayMs())
        handler.postDelayed(cooldownTicker!!, 250L)
    }

    private fun currentRacerDelayMs(): Long =
        (740L - (racerScore * 11L)).coerceAtLeast(270L)

    private fun advanceRacerTracks(activity: MainActivity) {
        for (lane in 0..2) {
            val reachedCar = racerTracks[lane].removeAt(racerTracks[lane].lastIndex)
            if (reachedCar != ".") {
                if (lane == carLane) {
                    if (reachedCar == "🚧") {
                        triggerCrashAndExit(activity, "💥 התנגשת במחסום! ניקוד סופי: $racerScore 🏆")
                        return
                    } else if (reachedCar == "⭐") {
                        playRacerSoundEffect(activity, "STAR")
                        racerScore += 10
                        lastShotTime = 0L
                    }
                } else if (reachedCar == "🚧") {
                    racerScore++
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
        if (!recentlySpawned && Random.nextFloat() < 0.72f) {
            val spawnLane = Random.nextInt(3)
            val roll = Random.nextFloat()
            when {
                roll < 0.11f -> {
                    pendingGuardedStarLane = spawnLane
                    for (lane in 0..2) racerTracks[lane].add(0, if (lane == spawnLane) "🚧" else ".")
                }
                roll < 0.14f -> {
                    val blockerLane = (spawnLane + 1 + Random.nextInt(2)) % 3
                    for (lane in 0..2) {
                        val sym = when (lane) { spawnLane -> "⭐"; blockerLane -> "🚧"; else -> "." }
                        racerTracks[lane].add(0, sym)
                    }
                }
                else -> {
                    val secondBlocker = if (racerScore >= 20 && Random.nextFloat() < 0.28f) (spawnLane + 1 + Random.nextInt(2)) % 3 else -1
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

        styleKeyForGame(activity, "8", pal.controlAccent, if (pal.isMinimalist) "8 ↑" else "8⬆️", glow = true)
        styleKeyForGame(activity, "2", pal.controlAccent, if (pal.isMinimalist) "2 ↓" else "2⬇️", glow = true)
        styleKeyForGame(
            activity, "5",
            if (gunReady) pal.playerColor else pal.inkSecondary,
            if (gunReady) (if (pal.isMinimalist) "5 ✦" else "5💥") else "${sec}s",
            glow = gunReady
        )
        styleKeyForGame(activity, "6", pal.bonusColor, if (pal.isMinimalist) "6 »" else "6⚡", glow = true)
        invalidateStage()
    }

    // =========================================================================
    // משחק 4: נטרול פצצה מתמטית (קוד 1010) — ללא שאלות דו-משמעיות
    // =========================================================================
    fun startBombDefusal(activity: MainActivity) {
        prepareNewGame(activity, "BOMB")
        bombTimeLeft = 15
        bombScore = 0
        generateBombQuestion(activity)

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || gameEnding || currentGame != "BOMB" || !isActivityValid(activity)) return
                bombTimeLeft--
                if (bombTimeLeft <= 0) {
                    triggerCrashAndExit(activity, "💥 בום! הזמן נגמר! פצצות שנוטרלו: $bombScore")
                    return
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
        for (attempt in 0 until 20) {
            val a = Random.nextInt(2, 13)
            val b = Random.nextInt(2, 11)
            val op = ops.random()
            val (lhs, rhs, target) = when (op) {
                "+" -> Triple(a, b, a + b)
                "−" -> Triple(a + b, b, a)
                "×" -> Triple(a, b, a * b)
                else -> Triple(a * b, b, a)
            }
            val matchingCount = ops.count { isValidBombAnswer(lhs, rhs, target, it) }
            if (matchingCount == 1 || attempt == 19) {
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
        val isUrgent = bombTimeLeft <= 5
        for (op in listOf("+", "−", "×", "÷")) {
            styleKeyForGame(activity, op, if (isUrgent) pal.enemyColor else pal.controlAccent, glow = true)
        }
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

        when (currentGame) {
            "ANIMALS" -> {
                playSqueakSound(activity.useSound)
                animalActiveIdx = (animalActiveIdx + 1) % 5
                animalBounceTime = System.currentTimeMillis()
                invalidateStage()
                return true
            }
            "TTT" -> {
                if (tttRoundLocked) {
                    updateTicTacToeUI(activity, tttStatusText)
                    return true
                }
                val idx = key.toIntOrNull()?.minus(1)
                if (idx != null && idx in 0..8 && tttBoard[idx] !in listOf("×", "0")) {
                    tttBoard[idx] = "×"
                    var res = findWinningLine()
                    if (res != null) { finishTicTacToe(activity, res); return true }
                    val comp = getSmartComputerMove()
                    if (comp != -1) tttBoard[comp] = "0"
                    res = findWinningLine()
                    if (res != null) finishTicTacToe(activity, res) else updateTicTacToeUI(activity, "תורך! בחר משבצת (1–9)")
                } else {
                    updateTicTacToeUI(activity, tttStatusText)
                }
                return true
            }
            "INVADERS" -> {
                val targetIndex = invaderTrack.indexOfFirst { it != "." }
                if (targetIndex != -1 && invaderTrack[targetIndex] == key) {
                    invaderTrack[targetIndex] = "."
                    invaderScore += 10
                    invaderLaserCol = targetIndex
                    invaderLaserTime = System.currentTimeMillis()
                    playRacerSoundEffect(activity, "SHOOT")
                    if (activity.useHaptic) activity.mainContent?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } else {
                    invaderScore = (invaderScore - 2).coerceAtLeast(0)
                }
                renderInvaders(activity)
                return true
            }
            "RACER" -> {
                when {
                    key == "8" || key.startsWith("8") -> carLane = (carLane - 1).coerceAtLeast(0)
                    key == "2" || key.startsWith("2") -> carLane = (carLane + 1).coerceAtMost(2)
                    key == "5" || key.startsWith("5") || key.endsWith("s") || key == "×" -> {
                        val rem = getShotCooldownRemainingMs()
                        if (rem == 0L) {
                            lastShotTime = System.currentTimeMillis()
                            racerLaserLane = carLane
                            racerLaserTime = lastShotTime
                            playRacerSoundEffect(activity, "SHOOT")
                            val idx = racerTracks[carLane].indexOfLast { it == "🚧" }
                            if (idx != -1) {
                                racerLaserEndCol = idx
                                racerTracks[carLane][idx] = "."
                                racerScore += 2
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
                        racerScore++
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
                if (key in listOf("+", "−", "×", "÷")) {
                    if (isValidBombAnswer(bombLhs, bombRhs, bombTarget, key)) {
                        bombScore++
                        bombTimeLeft = (bombTimeLeft + 3).coerceAtMost(20)
                        playRacerSoundEffect(activity, "STAR")
                        generateBombQuestion(activity)
                    } else {
                        bombTimeLeft = (bombTimeLeft - 3).coerceAtLeast(1)
                        bombShakeTime = System.currentTimeMillis()
                        playRacerSoundEffect(activity, "CRASH")
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
        gameLoop = null
        cooldownTicker = null
        roundResetRunnable = null
        gameOverDelayRunnable = null
    }
}

// =========================================================================
// קנבס משחקים וקטורי ב-60fps לכל 5 המיני-משחקים, מותאם לכל 4 ערכות הנושא
// =========================================================================
private class MiniGameVectorStageView(
    private val activity: MainActivity
) : View(activity) {

    private val d = resources.displayMetrics.density
    private var phase = 0f
    private var smoothCarLane = 1f
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
        pathEffect = DashPathEffect(floatArrayOf(10f * d, 8f * d), 0f)
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
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 10_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                phase += 0.05f
                if (phase > 6283f) phase = 0f
                // החלקה רכה של מעבר נתיב במכונית
                val targetLane = MiniGameEngine.carLane.toFloat()
                smoothCarLane += (targetLane - smoothCarLane) * 0.28f
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
    }

    private fun drawStageBackdrop(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        rect.set(2f * d, 2f * d, w - 2f * d, h - 2f * d)
        val r = 20f * d
        fillP.shader = LinearGradient(0f, 0f, w, h, pal.cardBgTop, pal.cardBgBottom, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, r, r, fillP)
        fillP.shader = null

        // גימור ייחודי לערכת הנושא
        if (pal.isGlass) {
            // הברקת זכוכית עליונה
            val gloss = RectF(10f * d, 4f * d, w - 10f * d, h * 0.24f)
            fillP.shader = LinearGradient(0f, gloss.top, 0f, gloss.bottom, Color.argb(58, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            canvas.drawRoundRect(gloss, 14f * d, 14f * d, fillP)
            fillP.shader = null
        } else if (!pal.isMinimalist && !pal.isAurora) {
            // רשת קלאסית עדינה
            strokeP.clearShadowLayer()
            strokeP.strokeWidth = 0.7f * d
            strokeP.color = pal.arenaGrid
            var x = 22f * d
            while (x < w - 10f * d) { canvas.drawLine(x, 8f * d, x, h - 8f * d, strokeP); x += 22f * d }
        }

        strokeP.strokeWidth = 1.4f * d
        strokeP.color = pal.cardBorder
        if (pal.isAurora) {
            strokeP.setShadowLayer(6f * d, 0f, 0f, withAlpha(pal.glowColor, 165))
        } else {
            strokeP.clearShadowLayer()
        }
        canvas.drawRoundRect(rect, r, r, strokeP)
        strokeP.clearShadowLayer()
    }

    // =========================================================================
    // 1. מסך וקטורי: מירוץ מכוניות (5555)
    // =========================================================================
    private fun drawVectorRacer(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val padX = 12f * d
        val hudY = 20f * d
        val roadTop = 30f * d
        val roadBot = h - 10f * d
        val roadH = (roadBot - roadTop).coerceAtLeast(36f * d)
        val laneH = roadH / 3f

        // --- HUD עליון: ניקוד + מד טעינת לייזר ---
        textP.textAlign = Paint.Align.LEFT
        textP.textSize = 12f * d
        textP.color = pal.inkPrimary
        canvas.drawText("SCORE ${MiniGameEngine.racerScore}", padX + 4f * d, hudY, textP)

        val remMs = MiniGameEngine.getShotCooldownRemainingMs()
        val readyFrac = 1f - (remMs.toFloat() / MiniGameEngine.SHOT_COOLDOWN_MS.toFloat()).coerceIn(0f, 1f)
        val barW = 78f * d
        val barRight = w - padX - 4f * d
        val barLeft = barRight - barW
        val barY = hudY - 4f * d

        subTextP.textAlign = Paint.Align.RIGHT
        subTextP.textSize = 10.5f * d
        subTextP.color = if (remMs == 0L) pal.playerColor else pal.inkSecondary
        val gunTxt = if (remMs == 0L) "לייזר מוכן" else "טעינה ${ceil(remMs / 1000.0).toInt()}s"
        canvas.drawText(gunTxt, barLeft - 8f * d, hudY, subTextP)

        rect.set(barLeft, barY - 3.5f * d, barRight, barY + 3.5f * d)
        fillP.color = withAlpha(pal.inkPrimary, 28)
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
        rect.set(barLeft, barY - 3.5f * d, barLeft + barW * readyFrac, barY + 3.5f * d)
        fillP.color = if (remMs == 0L) pal.playerColor else pal.bonusColor
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)

        // --- רקע כביש 3 נתיבים ---
        rect.set(padX, roadTop, w - padX, roadBot)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 14f * d, 14f * d, fillP)
        strokeP.strokeWidth = 1.1f * d
        strokeP.color = withAlpha(pal.inkPrimary, 36)
        canvas.drawRoundRect(rect, 14f * d, 14f * d, strokeP)

        // קווי הפרדת נתיבים זורמים
        val dashShift = (phase * 18f * d) % (18f * d)
        dashedP.pathEffect = DashPathEffect(floatArrayOf(10f * d, 8f * d), -dashShift)
        dashedP.color = withAlpha(pal.inkSecondary, 110)
        for (div in 1..2) {
            val y = roadTop + laneH * div
            canvas.drawLine(padX + 8f * d, y, w - padX - 8f * d, y, dashedP)
        }

        // גיאומטריית עמודות (0..4 משמאל לימין + עמודת המכונית בימין)
        val innerL = padX + 10f * d
        val innerR = w - padX - 10f * d
        val usableW = (innerR - innerL).coerceAtLeast(60f * d)
        val colW = usableW / 6.2f
        val carX = innerR - colW * 0.62f
        val carY = roadTop + (smoothCarLane + 0.5f) * laneH

        // --- קרן לייזר חיה (אם נורתה ב-240ms האחרונים) ---
        val now = System.currentTimeMillis()
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

        // --- ציור מחסומים וקטוריים וכוכבי בונוס ---
        for (lane in 0..2) {
            val cy = roadTop + (lane + 0.5f) * laneH
            for (col in 0..4) {
                val cell = MiniGameEngine.racerTracks[lane].getOrElse(col) { "." }
                val cx = innerL + (col + 0.5f) * colW
                when (cell) {
                    "🚧" -> drawVectorBarrier(canvas, cx, cy, min(laneH * 0.68f, 24f * d), pal)
                    "⭐" -> drawVectorStar(canvas, cx, cy, min(laneH * 0.34f, 11f * d), pal.bonusColor, pal.inkPrimary)
                }
            }
        }

        // --- ציור מכונית הספורט הווקטורית (פונה שמאלה אל עבר המסלול) ---
        drawVectorSportsCar(canvas, carX, carY, min(laneH * 0.74f, 26f * d), pal)

        // --- אנימציית פיצוץ וקטורית בעת התרסקות ---
        val crashAge = now - MiniGameEngine.crashAnimStartTime
        if (MiniGameEngine.crashAnimStartTime > 0L && crashAge in 0..900L) {
            val p = (crashAge / 850f).coerceIn(0f, 1f)
            val ex = carX - 10f * d
            val ey = roadTop + (MiniGameEngine.crashLane + 0.5f) * laneH
            val blastR = (10f + p * 34f) * d
            val alpha = ((1f - p * 0.65f) * 255f).toInt().coerceIn(0, 255)

            fillP.color = withAlpha(pal.enemyColor, (alpha * 0.45f).toInt())
            canvas.drawCircle(ex, ey, blastR, fillP)
            strokeP.strokeWidth = 2.6f * d
            strokeP.color = withAlpha(pal.bonusColor, alpha)
            for (i in 0 until 10) {
                val a = Math.toRadians(i * 36.0 + p * 40.0)
                canvas.drawLine(
                    ex + (cos(a) * blastR * 0.35f).toFloat(),
                    ey + (sin(a) * blastR * 0.35f).toFloat(),
                    ex + (cos(a) * blastR * 1.15f).toFloat(),
                    ey + (sin(a) * blastR * 1.15f).toFloat(),
                    strokeP
                )
            }
            textP.textAlign = Paint.Align.CENTER
            textP.textSize = 14f * d
            textP.color = pal.enemyColor
            canvas.drawText("CRASH!", w * 0.5f, roadTop + roadH * 0.5f + 5f * d, textP)
        }
    }

    private fun drawVectorBarrier(canvas: Canvas, cx: Float, cy: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val bw = h * 0.62f
        val bh = h * 0.92f
        rect.set(cx - bw * 0.5f, cy - bh * 0.5f, cx + bw * 0.5f, cy + bh * 0.5f)
        fillP.color = pal.enemyColor
        canvas.drawRoundRect(rect, 3.5f * d, 3.5f * d, fillP)
        // פסי אזהרה אלכסוניים בהירים
        strokeP.strokeWidth = 2.0f * d
        strokeP.color = if (pal.isMinimalist) Color.WHITE else pal.bonusColor
        canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.28f, rect.right - 2f * d, rect.top + bh * 0.14f, strokeP)
        canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.60f, rect.right - 2f * d, rect.top + bh * 0.46f, strokeP)
        canvas.drawLine(rect.left + 2f * d, rect.top + bh * 0.88f, rect.right - 2f * d, rect.top + bh * 0.74f, strokeP)
    }

    private fun drawVectorStar(canvas: Canvas, cx: Float, cy: Float, r: Float, fillCol: Int, rimCol: Int) {
        val pulse = 1f + sin(phase * 2.2f) * 0.10f
        val ro = r * pulse
        val ri = ro * 0.46f
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

    private fun drawVectorSportsCar(canvas: Canvas, cx: Float, cy: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val w = h * 1.65f
        val halfW = w * 0.5f
        val halfH = h * 0.42f

        // אלומות פנסים עדינות קדימה (שמאלה)
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
        val wheelW = w * 0.22f
        val wheelH = 3.2f * d
        canvas.drawRoundRect(RectF(cx - halfW * 0.72f, cy - halfH - wheelH * 0.5f, cx - halfW * 0.72f + wheelW, cy - halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx + halfW * 0.26f, cy - halfH - wheelH * 0.5f, cx + halfW * 0.26f + wheelW, cy - halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx - halfW * 0.72f, cy + halfH - wheelH * 0.5f, cx - halfW * 0.72f + wheelW, cy + halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)
        canvas.drawRoundRect(RectF(cx + halfW * 0.26f, cy + halfH - wheelH * 0.5f, cx + halfW * 0.26f + wheelW, cy + halfH + wheelH * 0.5f), 2f * d, 2f * d, fillP)

        // שלדת מכונית אווירודינמית
        rect.set(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        fillP.color = pal.playerColor
        canvas.drawRoundRect(rect, halfH * 0.72f, halfH * 0.72f, fillP)

        // שמשה ותא נהג
        rect.set(cx - halfW * 0.32f, cy - halfH * 0.68f, cx + halfW * 0.40f, cy + halfH * 0.68f)
        fillP.color = withAlpha(pal.cardBgTop, 215)
        canvas.drawRoundRect(rect, halfH * 0.45f, halfH * 0.45f, fillP)

        // ספוילר אחורי (ימין)
        strokeP.strokeWidth = 2.4f * d
        strokeP.color = pal.controlAccent
        canvas.drawLine(cx + halfW - 2f * d, cy - halfH * 0.85f, cx + halfW - 2f * d, cy + halfH * 0.85f, strokeP)

        // פנסים קדמיים (שמאל)
        fillP.color = pal.bonusColor
        canvas.drawCircle(cx - halfW + 2.5f * d, cy - halfH * 0.45f, 2f * d, fillP)
        canvas.drawCircle(cx - halfW + 2.5f * d, cy + halfH * 0.45f, 2f * d, fillP)
    }

    // =========================================================================
    // 2. מסך וקטורי: איקס עיגול חכם (1337)
    // =========================================================================
    private fun drawVectorTicTacToe(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val boardSize = (h - 22f * d).coerceAtMost(w * 0.46f).coerceAtLeast(60f * d)
        val boardLeft = 18f * d
        val boardTop = (h - boardSize) * 0.5f
        val cellSize = boardSize / 3f

        // רקע לוח
        rect.set(boardLeft - 4f * d, boardTop - 4f * d, boardLeft + boardSize + 4f * d, boardTop + boardSize + 4f * d)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 12f * d, 12f * d, fillP)

        // מיפוי 0..8 כמו במקלדת המחשבון: שורה עליונה 6,7,8 (מקשים 7,8,9), אמצעית 3,4,5, תחתונה 0,1,2
        fun cellCenter(idx: Int): Pair<Float, Float> {
            val col = idx % 3
            val row = 2 - (idx / 3)
            return (boardLeft + (col + 0.5f) * cellSize) to (boardTop + (row + 0.5f) * cellSize)
        }

        // קווי רשת הלוח
        strokeP.strokeWidth = 1.6f * d
        strokeP.color = withAlpha(pal.inkPrimary, 55)
        for (i in 1..2) {
            val x = boardLeft + cellSize * i
            val y = boardTop + cellSize * i
            canvas.drawLine(x, boardTop + 4f * d, x, boardTop + boardSize - 4f * d, strokeP)
            canvas.drawLine(boardLeft + 4f * d, y, boardLeft + boardSize - 4f * d, y, strokeP)
        }

        // ציור תאים: X / O וקטוריים או מספר תא עדין
        val markR = cellSize * 0.28f
        for (idx in 0..8) {
            val (cx, cy) = cellCenter(idx)
            when (val mark = MiniGameEngine.tttBoard[idx]) {
                "×" -> {
                    strokeP.strokeWidth = 3.0f * d
                    strokeP.color = pal.playerColor
                    if (pal.isAurora) strokeP.setShadowLayer(6f * d, 0f, 0f, pal.playerColor)
                    canvas.drawLine(cx - markR, cy - markR, cx + markR, cy + markR, strokeP)
                    canvas.drawLine(cx + markR, cy - markR, cx - markR, cy + markR, strokeP)
                    strokeP.clearShadowLayer()
                }
                "0" -> {
                    strokeP.strokeWidth = 3.0f * d
                    strokeP.color = pal.enemyColor
                    if (pal.isAurora) strokeP.setShadowLayer(6f * d, 0f, 0f, pal.enemyColor)
                    canvas.drawCircle(cx, cy, markR, strokeP)
                    strokeP.clearShadowLayer()
                }
                else -> {
                    subTextP.textAlign = Paint.Align.CENTER
                    subTextP.textSize = cellSize * 0.34f
                    subTextP.color = withAlpha(pal.inkSecondary, 140)
                    canvas.drawText(mark, cx, cy + cellSize * 0.12f, subTextP)
                }
            }
        }

        // קו ניצחון חוצה
        MiniGameEngine.tttWinningLine?.let { line ->
            if (line.size == 3) {
                val (x1, y1) = cellCenter(line[0])
                val (x2, y2) = cellCenter(line[2])
                strokeP.strokeWidth = 4.2f * d
                strokeP.color = pal.bonusColor
                canvas.drawLine(x1, y1, x2, y2, strokeP)
            }
        }

        // פאנל מידע מימין ללוח
        val rightCx = (boardLeft + boardSize + w - 12f * d) * 0.5f
        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 14.5f * d
        textP.color = pal.inkPrimary
        canvas.drawText("איקס עיגול חכם", rightCx, h * 0.30f, textP)

        // מקרא × נגד 0
        textP.textSize = 12f * d
        textP.color = pal.playerColor
        canvas.drawText("× אתה", rightCx + 32f * d, h * 0.48f, textP)
        textP.color = pal.enemyColor
        canvas.drawText("0 מחשב", rightCx - 32f * d, h * 0.48f, textP)

        // סטטוס נוכחי
        textP.textSize = 12.5f * d
        textP.color = pal.controlAccent
        canvas.drawText(MiniGameEngine.tttStatusText, rightCx, h * 0.67f, textP)

        subTextP.textAlign = Paint.Align.CENTER
        subTextP.textSize = 10f * d
        subTextP.color = pal.inkSecondary
        canvas.drawText("AC ליציאה למחשבון", rightCx, h * 0.84f, subTextP)
    }

    // =========================================================================
    // 3. מסך וקטורי: פולשי החשבון (8080)
    // =========================================================================
    private fun drawVectorInvaders(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val padX = 12f * d
        val hudY = 20f * d
        val arenaTop = 30f * d
        val arenaBot = h - 10f * d
        val arenaH = (arenaBot - arenaTop).coerceAtLeast(36f * d)
        val cy = arenaTop + arenaH * 0.5f

        textP.textAlign = Paint.Align.LEFT
        textP.textSize = 12f * d
        textP.color = pal.inkPrimary
        canvas.drawText("SCORE ${MiniGameEngine.invaderScore}", padX + 4f * d, hudY, textP)

        subTextP.textAlign = Paint.Align.RIGHT
        subTextP.textSize = 10.5f * d
        subTextP.color = pal.inkSecondary
        canvas.drawText("לחץ על הסימן הקרוב לירוט | AC ליציאה", w - padX - 4f * d, hudY, subTextP)

        rect.set(padX, arenaTop, w - padX, arenaBot)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 14f * d, 14f * d, fillP)

        // כוכבי רקע נעים
        fillP.color = withAlpha(pal.inkSecondary, 75)
        for (s in 0 until 8) {
            val sx = padX + 10f * d + (((s * 43f * d - phase * 12f * d) % (w - padX * 2 - 20f * d)) + (w - padX * 2 - 20f * d)) % (w - padX * 2 - 20f * d)
            val sy = arenaTop + 8f * d + ((s * 29f) % (arenaH - 16f * d).coerceAtLeast(10f))
            canvas.drawCircle(sx, sy, 1.6f * d, fillP)
        }

        // חללית וקטורית משמאל פונה ימינה
        val shipX = padX + 24f * d
        val shipR = min(arenaH * 0.30f, 16f * d)
        // להבת מנוע
        fillP.color = pal.bonusColor
        val flameLen = (6f + sin(phase * 4f) * 3f) * d
        path.reset()
        path.moveTo(shipX - shipR * 0.8f, cy - shipR * 0.35f)
        path.lineTo(shipX - shipR * 0.8f - flameLen, cy)
        path.lineTo(shipX - shipR * 0.8f, cy + shipR * 0.35f)
        path.close()
        canvas.drawPath(path, fillP)

        // גוף החללית
        path.reset()
        path.moveTo(shipX + shipR * 1.15f, cy)
        path.lineTo(shipX - shipR * 0.85f, cy - shipR * 0.82f)
        path.lineTo(shipX - shipR * 0.45f, cy)
        path.lineTo(shipX - shipR * 0.85f, cy + shipR * 0.82f)
        path.close()
        fillP.color = pal.playerColor
        canvas.drawPath(path, fillP)

        // עמודות הפולשים (0..5)
        val trackL = shipX + shipR + 14f * d
        val trackR = w - padX - 10f * d
        val colW = ((trackR - trackL) / 6f).coerceAtLeast(20f * d)

        // קרן יירוט
        val laserAge = System.currentTimeMillis() - MiniGameEngine.invaderLaserTime
        if (laserAge in 0..220L && MiniGameEngine.invaderLaserCol in 0..5) {
            val alpha = ((1f - laserAge / 220f) * 255f).toInt().coerceIn(0, 255)
            val tx = trackL + (MiniGameEngine.invaderLaserCol + 0.5f) * colW
            strokeP.strokeWidth = 3.5f * d
            strokeP.color = withAlpha(pal.laserColor, alpha)
            canvas.drawLine(shipX + shipR, cy, tx, cy, strokeP)
            fillP.color = withAlpha(pal.bonusColor, alpha)
            canvas.drawCircle(tx, cy, (6f + (laserAge / 220f) * 10f) * d, fillP)
        }

        // פודים משושים של פולשי החשבון
        val firstEnemyIdx = MiniGameEngine.invaderTrack.indexOfFirst { it != "." }
        val podR = min(colW * 0.42f, arenaH * 0.34f).coerceAtLeast(12f * d)
        for (i in 0 until MiniGameEngine.invaderTrack.size) {
            val sym = MiniGameEngine.invaderTrack[i]
            val px = trackL + (i + 0.5f) * colW
            if (sym == ".") {
                fillP.color = withAlpha(pal.inkSecondary, 60)
                canvas.drawCircle(px, cy, 2f * d, fillP)
                continue
            }
            val isTarget = (i == firstEnemyIdx)
            val podCol = if (i == 0) pal.enemyColor else if (isTarget) pal.bonusColor else pal.controlAccent
            drawHexPod(canvas, px, cy, podR * (if (isTarget) 1.08f else 0.96f), withAlpha(podCol, 42), podCol)
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
        strokeP.strokeWidth = 1.8f * d
        strokeP.color = strokeCol
        canvas.drawPath(path, strokeP)
    }

    // =========================================================================
    // 4. מסך וקטורי: נטרול פצצה מתמטית (1010)
    // =========================================================================
    private fun drawVectorBomb(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val isUrgent = MiniGameEngine.bombTimeLeft <= 5
        val ringCol = when {
            isUrgent -> pal.enemyColor
            MiniGameEngine.bombTimeLeft <= 9 -> pal.bonusColor
            else -> pal.playerColor
        }

        // פצצה וקטורית משמאל עם טבעת זמן ופתיל בוער
        val bombCx = w * 0.22f
        val bombCy = h * 0.54f
        val bombR = min(h * 0.30f, 34f * d)
        val frac = (MiniGameEngine.bombTimeLeft / 20f).coerceIn(0f, 1f)

        // טבעת ספירה לאחור
        val arcR = bombR + 7f * d
        rect.set(bombCx - arcR, bombCy - arcR, bombCx + arcR, bombCy + arcR)
        strokeP.strokeWidth = 4.5f * d
        strokeP.color = withAlpha(pal.inkPrimary, 30)
        canvas.drawCircle(bombCx, bombCy, arcR, strokeP)
        strokeP.color = ringCol
        canvas.drawArc(rect, -90f, 360f * frac, false, strokeP)

        // גוף הפצצה
        fillP.color = if (pal.isMinimalist) Color.parseColor("#374151") else pal.arenaBg
        canvas.drawCircle(bombCx, bombCy, bombR, fillP)
        strokeP.strokeWidth = 2f * d
        strokeP.color = ringCol
        canvas.drawCircle(bombCx, bombCy, bombR, strokeP)

        // פתיל וניצוץ
        strokeP.strokeWidth = 2.2f * d
        strokeP.color = pal.inkSecondary
        canvas.drawLine(bombCx, bombCy - bombR, bombCx + 8f * d, bombCy - bombR - 10f * d, strokeP)
        fillP.color = if ((phase * 4f).toInt() % 2 == 0) pal.bonusColor else pal.enemyColor
        canvas.drawCircle(bombCx + 8f * d, bombCy - bombR - 10f * d, (3.2f + sin(phase * 5f) * 1.2f) * d, fillP)

        // זמן שנותר במרכז הפצצה
        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 16.5f * d
        textP.color = if (pal.isMinimalist) Color.WHITE else ringCol
        canvas.drawText("${MiniGameEngine.bombTimeLeft}s", bombCx, bombCy + 6f * d, textP)

        // פאנל משוואה וחוטי אופרטורים מימין
        val panelL = w * 0.41f
        val panelR = w - 14f * d
        val panelCx = (panelL + panelR) * 0.5f

        subTextP.textAlign = Paint.Align.CENTER
        subTextP.textSize = 11f * d
        subTextP.color = pal.inkSecondary
        canvas.drawText("פצצות שנוטרלו: ${MiniGameEngine.bombScore}  |  AC ליציאה", panelCx, 22f * d, subTextP)

        // תיבת המשוואה הדיגיטלית
        rect.set(panelL, 32f * d, panelR, h * 0.64f)
        fillP.color = pal.arenaBg
        canvas.drawRoundRect(rect, 12f * d, 12f * d, fillP)
        strokeP.strokeWidth = 1.4f * d
        strokeP.color = if (isUrgent) pal.enemyColor else pal.controlAccent
        canvas.drawRoundRect(rect, 12f * d, 12f * d, strokeP)

        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 18.5f * d
        textP.color = pal.inkPrimary
        val eq = "\u200E${MiniGameEngine.bombLhs}  [ ? ]  ${MiniGameEngine.bombRhs}  =  ${MiniGameEngine.bombTarget}"
        canvas.drawText(eq, panelCx, (32f * d + h * 0.64f) * 0.5f + 6.5f * d, textP)

        // 4 תגי חוטי אופרטורים (+, −, ×, ÷)
        val ops = listOf("+", "−", "×", "÷")
        val wireCols = listOf(pal.playerColor, pal.enemyColor, pal.bonusColor, pal.controlAccent)
        val chipW = ((panelR - panelL) - 18f * d) / 4f
        val chipTop = h * 0.72f
        val chipBot = h - 12f * d
        for (i in 0..3) {
            val cl = panelL + i * (chipW + 6f * d)
            rect.set(cl, chipTop, cl + chipW, chipBot)
            fillP.color = withAlpha(wireCols[i], 45)
            canvas.drawRoundRect(rect, 8f * d, 8f * d, fillP)
            strokeP.strokeWidth = 1.4f * d
            strokeP.color = wireCols[i]
            canvas.drawRoundRect(rect, 8f * d, 8f * d, strokeP)
            textP.textSize = 14f * d
            textP.color = pal.inkPrimary
            canvas.drawText(ops[i], cl + chipW * 0.5f, (chipTop + chipBot) * 0.5f + 5f * d, textP)
        }
    }

    // =========================================================================
    // 5. מסך וקטורי: מסיבת חיות מצפצפות (7777)
    // =========================================================================
    private fun drawVectorAnimalParty(canvas: Canvas, w: Float, h: Float, pal: MiniGameEngine.GameThemePalette) {
        val padX = 14f * d
        textP.textAlign = Paint.Align.CENTER
        textP.textSize = 12.5f * d
        textP.color = pal.inkPrimary
        canvas.drawText("מסיבת חיות מצפצפות — נותרו ${MiniGameEngine.animalTimeLeft}ש' (AC ליציאה)", w * 0.5f, 22f * d, textP)

        // פס זמן
        val frac = (MiniGameEngine.animalTimeLeft.toFloat() / MiniGameEngine.animalTotalTime.coerceAtLeast(1)).coerceIn(0f, 1f)
        rect.set(padX, 30f * d, w - padX, 36f * d)
        fillP.color = withAlpha(pal.inkPrimary, 28)
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)
        rect.set(padX, 30f * d, padX + (w - padX * 2) * frac, 36f * d)
        fillP.color = pal.playerColor
        canvas.drawRoundRect(rect, 4f * d, 4f * d, fillP)

        // 5 פרצופי חיות וקטוריים קופצים עם גלי קול
        val cy = (36f * d + h - 8f * d) * 0.5f
        val slotW = (w - padX * 2) / 5f
        val r = min(slotW * 0.34f, (h - 54f * d) * 0.34f).coerceAtLeast(14f * d)
        val animalColors = listOf(
            Color.parseColor("#E6A96B"), // כלב
            Color.parseColor("#F48FB1"), // חתול
            Color.parseColor("#FF8A65"), // שועל
            Color.parseColor("#90A4AE"), // פנדה
            Color.parseColor("#81C784")  // צפרדע
        )
        val bounceAge = System.currentTimeMillis() - MiniGameEngine.animalBounceTime

        for (i in 0 until 5) {
            val cx = padX + slotW * (i + 0.5f)
            val isActive = (i == MiniGameEngine.animalActiveIdx)
            val hop = if (isActive && bounceAge in 0..320L) {
                -sin((bounceAge / 320f) * PI).toFloat() * 12f * d
            } else {
                sin(phase * 1.8f + i * 1.2f) * 3f * d
            }
            val ay = cy + hop

            // גלי קול מתפשטים סביב החיה המצפצפת
            if (isActive && bounceAge in 0..400L) {
                val wp = bounceAge / 400f
                strokeP.strokeWidth = 2f * d
                strokeP.color = withAlpha(pal.bonusColor, ((1f - wp) * 210f).toInt())
                canvas.drawCircle(cx, ay, r + wp * 18f * d, strokeP)
            }

            val bodyCol = animalColors[i]
            // אוזניים וקטוריות לפי סוג החיה
            fillP.color = bodyCol
            when (i) {
                0 -> { // כלב: אוזניים שמוטות
                    canvas.drawOval(RectF(cx - r * 1.12f, ay - r * 0.45f, cx - r * 0.60f, ay + r * 0.55f), fillP)
                    canvas.drawOval(RectF(cx + r * 0.60f, ay - r * 0.45f, cx + r * 1.12f, ay + r * 0.55f), fillP)
                }
                1, 2 -> { // חתול / שועל: אוזניים משולשות
                    path.reset()
                    path.moveTo(cx - r * 0.78f, ay - r * 0.25f)
                    path.lineTo(cx - r * 0.68f, ay - r * 1.15f)
                    path.lineTo(cx - r * 0.12f, ay - r * 0.65f)
                    path.close()
                    canvas.drawPath(path, fillP)
                    path.reset()
                    path.moveTo(cx + r * 0.78f, ay - r * 0.25f)
                    path.lineTo(cx + r * 0.68f, ay - r * 1.15f)
                    path.lineTo(cx + r * 0.12f, ay - r * 0.65f)
                    path.close()
                    canvas.drawPath(path, fillP)
                }
                else -> { // פנדה / צפרדע: אוזניים/עיניים עגולות עליונות
                    fillP.color = if (i == 3) Color.parseColor("#263238") else bodyCol
                    canvas.drawCircle(cx - r * 0.62f, ay - r * 0.72f, r * 0.34f, fillP)
                    canvas.drawCircle(cx + r * 0.62f, ay - r * 0.72f, r * 0.34f, fillP)
                }
            }

            // ראש עגול
            fillP.color = if (i == 3) Color.WHITE else bodyCol
            canvas.drawCircle(cx, ay, r, fillP)
            strokeP.strokeWidth = 1.4f * d
            strokeP.color = withAlpha(pal.inkPrimary, 95)
            canvas.drawCircle(cx, ay, r, strokeP)

            // עיניים ואף
            fillP.color = Color.parseColor("#1F2937")
            canvas.drawCircle(cx - r * 0.34f, ay - r * 0.12f, r * 0.13f, fillP)
            canvas.drawCircle(cx + r * 0.34f, ay - r * 0.12f, r * 0.13f, fillP)
            canvas.drawCircle(cx, ay + r * 0.18f, r * 0.12f, fillP)
        }
    }
}

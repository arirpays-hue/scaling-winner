package com.example.calculator

import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.random.Random

object MiniGameEngine {

    private data class GameThemePalette(
        val displayPrimary: Int,
        val displaySub: Int,
        val headerAccent: Int,
        val playerColor: Int,
        val enemyColor: Int,
        val bonusColor: Int,
        val controlAccent: Int,
        val glowShadowColor: Int,
        val glowRadius: Float,
        val isMinimalist: Boolean,
        val isClean: Boolean
    )

    var isActive = false
    private var currentGame = ""
    private val handler = Handler(Looper.getMainLooper())
    private var gameLoop: Runnable? = null
    private var cooldownTicker: Runnable? = null
    private var roundResetRunnable: Runnable? = null

    // ניהול אודיו בטוח ללא דליפות או קריסות AudioTrack
    private var engineLoopPlayer: MediaPlayer? = null
    private var currentEngineStage = 0
    private var sfxPlayer: MediaPlayer? = null
    private val activeSynthThreads = AtomicInteger(0)
    @Volatile private var synthEngineRunning = false

    // איקס עיגול חכם (1337)
    private val tttBoard = Array(9) { "" }
    private var tttRoundLocked = false

    // פולשי החשבון (8080)
    private var invaderScore = 0
    private var invaderTrack = mutableListOf<String>()
    private val invaderSymbols = listOf("+", "−", "×", "÷", "sin", "cos", "tan", "√")

    // מירוץ מכוניות (5555)
    private var carLane = 1 // 0 = עליון (8), 1 = אמצעי, 2 = תחתון (2)
    private var racerScore = 0
    private val racerTracks = Array(3) { MutableList(5) { "." } }
    private var lastShotTime = 0L
    private var lastCooldownToastTime = 0L
    private val SHOT_COOLDOWN_MS = 3000L
    private var pendingGuardedStarLane = -1

    // נטרול פצצה (1010)
    private var bombTimeLeft = 15
    private var bombScore = 0
    private var bombLhs = 2
    private var bombRhs = 3
    private var bombTarget = 6
    private var expectedOp = "×"

    // מסיבת חיות מצפצפות (7777)
    var isAnimalParty = false
    private val savedKeyLabels = mutableMapOf<String, CharSequence>()
    private val animalEmojis = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
        "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔"
    )

    // =========================================================================
    // התאמה דינמית לכל 4 ערכות הנושא (מינימליסטי / זוהר / זכוכית / קלאסי)
    // =========================================================================
    private fun resolvePalette(activity: MainActivity): GameThemePalette {
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode
        val dispBg = if (isMin) MinimalistTheme.getBackgroundColor(activity) else Utils.lightenColor(activity.bgColor, 0.07f)

        return when {
            isMin -> GameThemePalette(
                displayPrimary = MinimalistTheme.ensureReadableTextOnBg(MinimalistTheme.TEXT_CHARCOAL, dispBg, 2.5),
                displaySub = MinimalistTheme.TEXT_MUTED_GREY,
                headerAccent = MinimalistTheme.ensureReadableTextOnBg(Color.parseColor("#2E6F5E"), dispBg, 2.3),
                playerColor = Color.parseColor("#2E7D32"),
                enemyColor = MinimalistTheme.ALERT_RED_TEXT,
                bonusColor = Color.parseColor("#B26A00"),
                controlAccent = Color.parseColor("#1565C0"),
                glowShadowColor = Color.TRANSPARENT,
                glowRadius = 0f,
                isMinimalist = true,
                isClean = true
            )
            isAur -> GameThemePalette(
                displayPrimary = Color.WHITE,
                displaySub = AuroraBorealisTheme.EXPRESSION_CYAN_GREY,
                headerAccent = AuroraBorealisTheme.AURORA_PRIMARY,
                playerColor = Color.parseColor("#00F5D4"),
                enemyColor = Color.parseColor("#FF4D8D"),
                bonusColor = Color.parseColor("#FFD166"),
                controlAccent = AuroraBorealisTheme.AURORA_PRIMARY,
                glowShadowColor = Color.argb(145, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY)),
                glowRadius = 10f,
                isMinimalist = false,
                isClean = true
            )
            isGlass -> GameThemePalette(
                displayPrimary = Color.WHITE,
                displaySub = Color.parseColor("#B8C7D9"),
                headerAccent = MinimalistTheme.ensureReadableTextOnBg(activity.primaryColor, dispBg, 2.2),
                playerColor = Color.parseColor("#34D399"),
                enemyColor = Color.parseColor("#FB7185"),
                bonusColor = Color.parseColor("#FBBF24"),
                controlAccent = Color.parseColor("#7DD3FC"),
                glowShadowColor = Color.argb(100, 0, 0, 0),
                glowRadius = 6f,
                isMinimalist = false,
                isClean = true
            )
            else -> GameThemePalette(
                displayPrimary = MinimalistTheme.ensureReadableTextOnBg(
                    MinimalistTheme.resolveDisplayTextColor(activity), dispBg, 2.5
                ),
                displaySub = Color.parseColor("#8A94A8"),
                headerAccent = MinimalistTheme.ensureReadableTextOnBg(activity.primaryColor, dispBg, 2.3),
                playerColor = MinimalistTheme.ensureReadableTextOnBg(Color.parseColor("#4CAF50"), dispBg, 2.2),
                enemyColor = MinimalistTheme.ensureReadableTextOnBg(Color.parseColor("#FF5252"), dispBg, 2.2),
                bonusColor = MinimalistTheme.ensureReadableTextOnBg(Color.parseColor("#FFD54F"), dispBg, 2.2),
                controlAccent = MinimalistTheme.ensureReadableTextOnBg(activity.primaryColor, dispBg, 2.3),
                glowShadowColor = Color.TRANSPARENT,
                glowRadius = 0f,
                isMinimalist = false,
                isClean = false
            )
        }
    }

    private fun isActivityValid(activity: MainActivity): Boolean =
        !activity.isFinishing && !activity.isDestroyed

    private fun applyDisplayTheme(
        activity: MainActivity,
        palette: GameThemePalette,
        headerText: String,
        exprText: String,
        mainText: String,
        mainAccentOverride: Int? = null
    ) {
        activity.memoryIndicator?.apply {
            text = "\u200F$headerText"
            setTextColor(palette.headerAccent)
        }
        activity.expressionView?.apply {
            text = "\u200F$exprText"
            setTextColor(palette.displaySub)
        }
        activity.currentView?.apply {
            textDirection = View.TEXT_DIRECTION_LTR
            text = "\u200E$mainText"
            val col = mainAccentOverride ?: palette.displayPrimary
            setTextColor(col)
            if (palette.glowRadius > 0f) {
                setShadowLayer(palette.glowRadius, 0f, if (activity.isIosGlassMode) 2f else 0f, palette.glowShadowColor)
            } else {
                setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
            }
        }
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
        stopAllAudio()
        restoreAllKeyColorsAndLabels(activity)
        isAnimalParty = false
        tttRoundLocked = false
        isActive = true
        currentGame = gameId
    }

    fun stopGame(activity: MainActivity, message: String = "יצאת מהמשחק 🎮") {
        val wasActive = isActive
        isActive = false
        currentGame = ""
        isAnimalParty = false
        tttRoundLocked = false
        stopPreviousLoop()
        stopAllAudio()
        if (!isActivityValid(activity)) return
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
    // מערכת סאונד מוגנת (מכבדת useSound, בודקת minBufferSize ומונעת דליפות)
    // =========================================================================
    private fun findRawResId(activity: MainActivity, candidates: List<String>): Int {
        for (name in candidates) {
            val id = activity.resources.getIdentifier(name, "raw", activity.packageName)
            if (id != 0) return id
        }
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

    private fun playRawSfx(activity: MainActivity, resId: Int): Boolean {
        if (resId == 0 || !isActivityValid(activity)) return false
        return try {
            releaseSfxPlayerSafely()
            val mp = MediaPlayer.create(activity.applicationContext, resId) ?: return false
            sfxPlayer = mp
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
                val bytesNeeded = maxOf(buffer.size * 2, minBuf)
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
                    .setBufferSizeInBytes(bytesNeeded)
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
        val candidates = if (isWin) {
            listOf("ttt_win", "win", "game_win", "tuomas_data_game_win_471625", "victory", "star", "cheer")
        } else {
            listOf("ttt_lose", "lose", "game_over", "universfield_game_over_deep_male_voice_clip_352695", "gameover", "fail", "crash")
        }
        if (playRawSfx(activity, findRawResId(activity, candidates))) return

        if (activeSynthThreads.get() >= 3) return
        activeSynthThreads.incrementAndGet()
        Thread {
            try {
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
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(2048)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(maxOf(buffer.size * 2, minBuf))
                    .setTransferMode(AudioTrack.MODE_STATIC).build()
                try {
                    track.write(buffer, 0, buffer.size)
                    track.play()
                    Thread.sleep(notes.sumOf { it.second }.toLong() + 40L)
                } finally {
                    try { track.stop() } catch (_: Throwable) {}
                    try { track.release() } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {
            } finally {
                activeSynthThreads.decrementAndGet()
            }
        }.start()
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

        val singleResId = findRawResId(activity, listOf("car_loop", "engine", "race_car", "car_engine", "car", "motor"))
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
                while (synthEngineRunning && isActive && currentGame == "RACER" && isActivityValid(activity)) {
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
        if (!isActive || currentGame != "RACER" || !activity.useSound || !isActivityValid(activity)) return
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
                        if (isActive && currentGame == "RACER" && isActivityValid(activity)) {
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

    private fun stopAllAudio() {
        stopRacerAudio()
        releaseSfxPlayerSafely()
    }

    private fun playRacerSoundEffect(activity: MainActivity, type: String) {
        if (!activity.useSound) return
        val candidates = when (type) {
            "SHOOT" -> listOf("shoot", "shot", "universfield_arcade_shot_152053", "arcade_shot", "laser", "fire", "gun")
            "CRASH" -> listOf("crash", "collision", "hit", "explosion", "boom")
            "STAR" -> listOf("star", "coin", "mixkit_winning_a_coin_video_game_2069", "winning_a_coin", "bonus", "powerup", "pickup")
            else -> emptyList()
        }
        if (playRawSfx(activity, findRawResId(activity, candidates))) return

        val sampleRate = 22050
        val durationMs = when (type) { "SHOOT" -> 160; "CRASH" -> 420; else -> 240 }
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
                    (noise * 0.75 + sin(phase) * 0.25) * (1.0 - p)
                }
                else -> {
                    val freq = if (p < 0.45) 880.0 else 1318.5
                    phase += 2.0 * PI * freq / sampleRate
                    sin(phase) * sin(p * PI)
                }
            }
            buffer[i] = (sample * Short.MAX_VALUE * 0.62).toInt().toShort()
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

    // =========================================================================
    // משחק 0: מסיבת חיות מצפצפות (קוד 7777)
    // =========================================================================
    fun startAnimalParty(activity: MainActivity) {
        prepareNewGame(activity, "ANIMALS")
        isAnimalParty = true
        val pal = resolvePalette(activity)

        val shuffled = animalEmojis.shuffled()
        var idx = 0
        for ((k, btn) in activity.keypadButtons) {
            savedKeyLabels[k] = btn.text
            if (k != "AC") {
                btn.text = shuffled[idx % shuffled.size]
                idx++
            }
        }
        applyDisplayTheme(
            activity, pal,
            headerText = if (pal.isClean) "מסיבת חיות! לחץ לצפצוף | AC ליציאה" else "🐾 מסיבת חיות! לחץ לצפצוף | AC ליציאה",
            exprText = "חוזר למצב רגיל בעוד 15 שניות...",
            mainText = "🐶 🐱 🐸 🦊 🐼"
        )
        playSqueakSound(activity.useSound)

        var left = 15
        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "ANIMALS" || !isActivityValid(activity)) {
                    stopAllAudio()
                    return
                }
                left--
                if (left <= 0) {
                    stopGame(activity, "🐾 החיות חזרו לגן החיות!")
                    return
                }
                activity.expressionView?.text = "\u200Fחוזר למצב רגיל בעוד $left שניות..."
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
        updateTicTacToeUI(activity, "תורך! לחץ על 1-9 להצבת × (AC ליציאה)")
    }

    private fun updateTicTacToeUI(activity: MainActivity, status: String, winnerHighlight: Int? = null) {
        val pal = resolvePalette(activity)
        val row1 = "[ ${tttBoard[6]} | ${tttBoard[7]} | ${tttBoard[8]} ]"
        val row2 = "[ ${tttBoard[3]} | ${tttBoard[4]} | ${tttBoard[5]} ]"
        val row3 = "[ ${tttBoard[0]} | ${tttBoard[1]} | ${tttBoard[2]} ]"

        applyDisplayTheme(
            activity, pal,
            headerText = if (pal.isClean) "איקס עיגול (× נגד 0) | AC ליציאה" else "🎮 איקס עיגול (× נגד 0) | AC ליציאה",
            exprText = status,
            mainText = "$row1\n$row2\n$row3",
            mainAccentOverride = winnerHighlight
        )

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
    }

    private fun checkWinner(): String? {
        val wins = arrayOf(
            intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
            intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
            intArrayOf(0, 4, 8), intArrayOf(2, 4, 6)
        )
        for (w in wins) {
            if (tttBoard[w[0]] == tttBoard[w[1]] && tttBoard[w[1]] == tttBoard[w[2]]) {
                return tttBoard[w[0]]
            }
        }
        if (tttBoard.none { it in "1".."9" }) return "TIE"
        return null
    }

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

    private fun finishTicTacToe(activity: MainActivity, winner: String) {
        tttRoundLocked = true
        val pal = resolvePalette(activity)
        when (winner) {
            "×" -> playTttResultSound(activity, true)
            "0" -> playTttResultSound(activity, false)
        }
        val (msg, highlight) = when (winner) {
            "×" -> (if (pal.isClean) "ניצחת! × ניצח את 0 (AC ליציאה)" else "🏆 ניצחת! × ניצח את 0! (AC ליציאה)") to pal.playerColor
            "0" -> (if (pal.isClean) "המחשב (0) ניצח — סיבוב חדש..." else "🤖 המחשב (0) ניצח! נסה שוב") to pal.enemyColor
            else -> (if (pal.isClean) "תיקו! משחק צמוד" else "🤝 תיקו! משחק צמוד") to pal.bonusColor
        }
        updateTicTacToeUI(activity, msg, winnerHighlight = highlight)
        roundResetRunnable?.let { handler.removeCallbacks(it) }
        val resetTask = Runnable {
            if (isActive && currentGame == "TTT" && isActivityValid(activity)) {
                startTicTacToe(activity)
            }
        }
        roundResetRunnable = resetTask
        handler.postDelayed(resetTask, 2200L)
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
                if (!isActive || currentGame != "INVADERS" || !isActivityValid(activity)) {
                    stopAllAudio()
                    return
                }
                val reachedShip = invaderTrack.removeAt(0)
                if (reachedShip != ".") {
                    playRacerSoundEffect(activity, "CRASH")
                    activity.shakeScreen()
                    stopGame(activity, "💥 החללית נפגעה! ניקוד סופי: $invaderScore")
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
        val ship = if (pal.isMinimalist) "▷" else "🚀"
        val dangerNear = invaderTrack.firstOrNull() != "."
        applyDisplayTheme(
            activity, pal,
            headerText = if (pal.isClean) "פולשי החשבון | לחץ על הסימן הקרוב | AC ליציאה" else "👾 פולשי החשבון | לחץ על הסימן כדי ליירט! | AC ליציאה",
            exprText = if (pal.isClean) "ניקוד: $invaderScore | יירט את הסימן הקרוב" else "ניקוד: $invaderScore ⭐ | יירט את הסימן הקרוב ל-🚀",
            mainText = "$ship ⬅ " + invaderTrack.joinToString("  "),
            mainAccentOverride = if (dangerNear) pal.enemyColor else pal.displayPrimary
        )

        // הדגשה עדינה של מקשי האופרטורים המדעיים בערכת הנושא
        for (sym in invaderSymbols) {
            val isNextTarget = invaderTrack.firstOrNull { it != "." } == sym
            val col = if (isNextTarget) pal.bonusColor else pal.controlAccent
            styleKeyForGame(activity, sym, col, glow = isNextTarget)
        }
    }

    // =========================================================================
    // משחק 3: מירוץ מכוניות (קוד 5555)
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
                if (!isActive || currentGame != "RACER" || !isActivityValid(activity)) {
                    stopAllAudio()
                    return
                }
                advanceRacerTracks(activity)
                if (!isActive) return
                renderRacer(activity)
                handler.postDelayed(this, currentRacerDelayMs())
            }
        }
        cooldownTicker = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "RACER" || !isActivityValid(activity)) return
                renderRacer(activity)
                handler.postDelayed(this, 300L)
            }
        }
        renderRacer(activity)
        handler.postDelayed(gameLoop!!, currentRacerDelayMs())
        handler.postDelayed(cooldownTicker!!, 300L)
    }

    private fun currentRacerDelayMs(): Long =
        (740L - (racerScore * 11L)).coerceAtLeast(270L)

    private fun advanceRacerTracks(activity: MainActivity) {
        for (lane in 0..2) {
            val reachedCar = racerTracks[lane].removeAt(racerTracks[lane].lastIndex)
            if (reachedCar != ".") {
                if (lane == carLane) {
                    if (reachedCar == "🚧") {
                        playRacerSoundEffect(activity, "CRASH")
                        activity.shakeScreen()
                        stopGame(activity, "💥 התנגשת במחסום! ניקוד סופי: $racerScore 🏆")
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

    private fun getShotCooldownRemainingMs(): Long {
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
            if (gunReady) pal.playerColor else pal.displaySub,
            if (gunReady) (if (pal.isMinimalist) "5 ✦" else "5💥") else "${sec}s",
            glow = gunReady
        )
        styleKeyForGame(activity, "6", pal.bonusColor, if (pal.isMinimalist) "6 »" else "6⚡", glow = true)

        val gunStatus = if (gunReady) {
            if (pal.isClean) "ירייה: מוכן" else "🎯 ירייה: מוכן!"
        } else {
            if (pal.isClean) "טעינה: ${sec}ש'" else "⏳ טעינה: ${sec}ש'"
        }

        val rows = (0..2).map { lane ->
            val trackDisplay = racerTracks[lane].joinToString("  ") { cell ->
                if (pal.isMinimalist) {
                    when (cell) {
                        "🚧" -> "▮"
                        "⭐" -> "★"
                        else -> "·"
                    }
                } else cell
            }
            val carIcon = if (lane == carLane) {
                if (pal.isMinimalist) " ▸ ◆" else " ➡ 🏎️"
            } else {
                if (pal.isMinimalist) "   ·" else " ➡ ▫️"
            }
            trackDisplay + carIcon
        }

        applyDisplayTheme(
            activity, pal,
            headerText = if (pal.isClean) "8=למעלה | 2=למטה | 5/×=ירייה | 6/+=טורבו" else "🏎️ 8=למעלה | 2=למטה | 5/×=ירייה (3ש') | 6/+=טורבו",
            exprText = if (pal.isClean) "ניקוד: $racerScore | $gunStatus" else "🏆 ניקוד: $racerScore | $gunStatus",
            mainText = rows.joinToString("\n")
        )
    }

    // =========================================================================
    // משחק 4: נטרול פצצה מתמטית (קוד 1010) — ללא משוואות דו-משמעיות!
    // =========================================================================
    fun startBombDefusal(activity: MainActivity) {
        prepareNewGame(activity, "BOMB")
        bombTimeLeft = 15
        bombScore = 0
        generateBombQuestion(activity)

        gameLoop = object : Runnable {
            override fun run() {
                if (!isActive || currentGame != "BOMB" || !isActivityValid(activity)) {
                    stopAllAudio()
                    return
                }
                bombTimeLeft--
                if (bombTimeLeft <= 0) {
                    playRacerSoundEffect(activity, "CRASH")
                    activity.shakeScreen()
                    stopGame(activity, "💥 בום! הזמן נגמר! פצצות שנוטרלו: $bombScore")
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
        // הגרלת משוואה עם פתרון יחיד וברור (מונע מקרים כמו 2 ? 2 = 4 או 4 ? 2 = 2)
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
        val eqText = "$bombLhs [?] $bombRhs = $bombTarget"
        applyDisplayTheme(
            activity, pal,
            headerText = if (pal.isClean) "נטרל את הפצצה! בחר +, −, × או ÷ | AC ליציאה" else "💣 נטרל את הפצצה! לחץ על +, −, × או ÷ | AC ליציאה",
            exprText = if (pal.isClean) "זמן: ${bombTimeLeft}ש' | נוטרלו: $bombScore" else "⏳ זמן: ${bombTimeLeft}ש' | נוטרלו: $bombScore 💣",
            mainText = eqText,
            mainAccentOverride = if (isUrgent) pal.enemyColor else pal.displayPrimary
        )
        for (op in listOf("+", "−", "×", "÷")) {
            styleKeyForGame(activity, op, if (isUrgent) pal.enemyColor else pal.controlAccent, glow = true)
        }
    }

    // =========================================================================
    // קליטת מקשים בזמן משחק (כולל שחזור עיצוב מקשים אם נלחץ מקש לא חוקי)
    // =========================================================================
    fun onKeyPress(activity: MainActivity, key: String): Boolean {
        if (!isActive) return false
        if (key == "AC") {
            stopGame(activity)
            return true
        }
        when (currentGame) {
            "ANIMALS" -> {
                playSqueakSound(activity.useSound)
                val pal = resolvePalette(activity)
                activity.currentView?.apply {
                    text = "\u200E" + List(5) { animalEmojis.random() }.joinToString(" ")
                    setTextColor(pal.displayPrimary)
                }
                return true
            }
            "TTT" -> {
                if (tttRoundLocked) {
                    updateTicTacToeUI(activity, activity.expressionView?.text?.toString() ?: "")
                    return true
                }
                val idx = key.toIntOrNull()?.minus(1)
                if (idx != null && idx in 0..8 && tttBoard[idx] !in listOf("×", "0")) {
                    tttBoard[idx] = "×"
                    var w = checkWinner()
                    if (w != null) { finishTicTacToe(activity, w); return true }
                    val comp = getSmartComputerMove()
                    if (comp != -1) tttBoard[comp] = "0"
                    w = checkWinner()
                    if (w != null) finishTicTacToe(activity, w) else updateTicTacToeUI(activity, "תורך! בחר משבצת פנויה (1-9)")
                } else {
                    // משמר את צבעי ה-× וה-0 גם אם השחקן לחץ על תא תפוס או מקש אחר
                    updateTicTacToeUI(activity, activity.expressionView?.text?.toString() ?: "בחר משבצת פנויה (1-9)")
                }
                return true
            }
            "INVADERS" -> {
                val targetIndex = invaderTrack.indexOfFirst { it != "." }
                if (targetIndex != -1 && invaderTrack[targetIndex] == key) {
                    invaderTrack[targetIndex] = "."
                    invaderScore += 10
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
                            playRacerSoundEffect(activity, "SHOOT")
                            val idx = racerTracks[carLane].indexOfLast { it == "🚧" }
                            if (idx != -1) {
                                racerTracks[carLane][idx] = "."
                                racerScore += 2
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
                        // איפוס טיימר הצעד האוטומטי כדי למנוע קפיצה כפולה מיידית אחרי טורבו
                        if (isActive && gameLoop != null) {
                            handler.removeCallbacks(gameLoop!!)
                            handler.postDelayed(gameLoop!!, currentRacerDelayMs())
                        }
                    }
                }
                if (isActive) renderRacer(activity)
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
        gameLoop = null
        cooldownTicker = null
        roundResetRunnable = null
    }
}

package com.example.calculator

import android.app.ActivityManager
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

// =========================================================================
// מנוע התאמת חומרה וביצועים בזמן אמת (ProHardwarePerformanceAdapter.kt)
// במצב אוטומטי: מאזין ברקע לחיסכון סוללה, טעינה, התחממות ועומס RAM
// ומתאים מיד את האיכות, האפקטים וחבילת האנימציות — וחוזר חזרה כשהעומס יורד
// =========================================================================
object ProHardwarePerformanceAdapter {

    private const val PREF_KEY_MODE = "pro_hw_perf_mode_v1"
    private const val PREF_KEY_AUTO_APPLIED_ONCE = "pro_hw_auto_applied_once_v1"
    private const val PREF_KEY_SAVED_RICH_PACK = "pro_hw_saved_rich_pack_v1"
    private const val LIVE_POLL_INTERVAL_MS = 18_000L

    const val SETTINGS_BTN_TAG = "PRO_HW_PERF_SETTINGS_BTN"

    enum class PerformanceTier(
        val level: Int,
        val hebrewName: String,
        val shortName: String,
        val targetFps: Int,
        val minFrameIntervalMs: Long,
        val particleMultiplier: Float,
        val allowRealtimeBlur: Boolean,
        val allowGlowShadows: Boolean,
        val allowAmbientCanvas: Boolean,
        val allowElevationShadows: Boolean,
        val durationScale: Float,
        val accentHex: String
    ) {
        ECO_LOW_END(
            level = 0,
            hebrewName = "חסכוני (מכשיר חלש / חיסכון)",
            shortName = "חסכוני",
            targetFps = 30,
            minFrameIntervalMs = 33L,
            particleMultiplier = 0.22f,
            allowRealtimeBlur = false,
            allowGlowShadows = false,
            allowAmbientCanvas = false,
            allowElevationShadows = false,
            durationScale = 0.65f,
            accentHex = "#22C55E"
        ),
        BALANCED(
            level = 1,
            hebrewName = "מאוזן",
            shortName = "מאוזן",
            targetFps = 45,
            minFrameIntervalMs = 22L,
            particleMultiplier = 0.50f,
            allowRealtimeBlur = false,
            allowGlowShadows = false,
            allowAmbientCanvas = true,
            allowElevationShadows = true,
            durationScale = 0.85f,
            accentHex = "#38BDF8"
        ),
        HIGH(
            level = 2,
            hebrewName = "גבוה",
            shortName = "גבוה",
            targetFps = 60,
            minFrameIntervalMs = 16L,
            particleMultiplier = 0.80f,
            allowRealtimeBlur = true,
            allowGlowShadows = true,
            allowAmbientCanvas = true,
            allowElevationShadows = true,
            durationScale = 1.0f,
            accentHex = "#A78BFA"
        ),
        ULTRA(
            level = 3,
            hebrewName = "אולטרה (מקסימלי)",
            shortName = "אולטרה",
            targetFps = 60,
            minFrameIntervalMs = 16L,
            particleMultiplier = 1.0f,
            allowRealtimeBlur = true,
            allowGlowShadows = true,
            allowAmbientCanvas = true,
            allowElevationShadows = true,
            durationScale = 1.0f,
            accentHex = "#00F5D4"
        )
    }

    enum class AdaptationMode(val id: Int, val label: String) {
        AUTO(0, "אוטומטי חי (מתעדכן לבד בזמן אמת)"),
        ULTRA(1, "נעול: אולטרה"),
        HIGH(2, "נעול: גבוה"),
        BALANCED(3, "נעול: מאוזן"),
        ECO(4, "נעול: חסכוני");

        companion object {
            fun fromId(id: Int): AdaptationMode = values().firstOrNull { it.id == id } ?: AUTO
        }
    }

    data class HardwareProfile(
        val totalRamGb: Float,
        val availRamGb: Float,
        val isLowRamDevice: Boolean,
        val cpuCores: Int,
        val maxCpuGhz: Float,
        val is64Bit: Boolean,
        val refreshRateHz: Float,
        val heapClassMb: Int,
        val isPowerSaveMode: Boolean,
        val isThermalThrottled: Boolean,
        val batteryPct: Int,
        val isCharging: Boolean,
        val systemReduceMotion: Boolean,
        val socLabel: String,
        val hardwareScore: Int,
        val recommendedTier: PerformanceTier
    )

    var adaptationMode: AdaptationMode = AdaptationMode.AUTO
        private set

    var activeTier: PerformanceTier = PerformanceTier.HIGH
        private set

    var lastProfile: HardwareProfile? = null
        private set

    private var memoryPressureLevel: Int = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hostRef: WeakReference<MainActivity>? = null
    private var systemStateReceiver: BroadcastReceiver? = null
    private var thermalListenerObj: Any? = null
    private var periodicCheckRunnable: Runnable? = null
    private var isMonitoringActive = false

    // =========================================================================
    // 1. אתחול, ניטור חי בזמן אמת והתאמה דו-כיוונית אוטומטית (ירידה ועלייה חזרה)
    // =========================================================================
    fun initAndAdapt(activity: MainActivity) {
        hostRef = WeakReference(activity)
        loadPrefs(activity.prefs)
        val profile = detectHardwareProfile(activity)
        lastProfile = profile
        recomputeActiveTier(profile)

        val alreadyApplied = activity.prefs?.getBoolean(PREF_KEY_AUTO_APPLIED_ONCE, false) ?: false
        applyTierToEngines(
            activity = activity,
            forcePackSwitch = (!alreadyApplied && adaptationMode == AdaptationMode.AUTO),
            notifyUser = false
        )

        if (!alreadyApplied) {
            activity.prefs?.edit()?.putBoolean(PREF_KEY_AUTO_APPLIED_ONCE, true)?.apply()
        }
    }

    fun onActivityResumed(activity: MainActivity) {
        hostRef = WeakReference(activity)
        startLiveMonitoring(activity)
        evaluateLiveConditions(activity, notifyOnTierChange = false)
    }

    fun onActivityPaused(activity: MainActivity) {
        stopLiveMonitoring(activity)
    }

    fun onTrimMemory(activity: MainActivity, level: Int) {
        memoryPressureLevel = level
        if (adaptationMode != AdaptationMode.AUTO) return
        evaluateLiveConditions(activity, notifyOnTierChange = false)
    }

    private fun startLiveMonitoring(activity: MainActivity) {
        if (isMonitoringActive) return
        isMonitoringActive = true

        // 1. האזנה מיידית לשינויי מצב חיסכון בסוללה, חיבור/ניתוק מטען וסוללה נמוכה
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val host = hostRef?.get() ?: return
                if (host.isFinishing || host.isDestroyed) return
                if (adaptationMode == AdaptationMode.AUTO) {
                    mainHandler.post { evaluateLiveConditions(host, notifyOnTierChange = true) }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                activity.registerReceiver(receiver, filter)
            }
            systemStateReceiver = receiver
        } catch (_: Throwable) {
            systemStateReceiver = null
        }

        // 2. האזנה בזמן אמת לחיישן ההתחממות של אנדרואיד (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val pm = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm != null) {
                    val listener = PowerManager.OnThermalStatusChangedListener { _ ->
                        val host = hostRef?.get() ?: return@OnThermalStatusChangedListener
                        if (host.isFinishing || host.isDestroyed) return@OnThermalStatusChangedListener
                        if (adaptationMode == AdaptationMode.AUTO) {
                            mainHandler.post { evaluateLiveConditions(host, notifyOnTierChange = true) }
                        }
                    }
                    pm.addThermalStatusListener(listener)
                    thermalListenerObj = listener
                }
            } catch (_: Throwable) {
                thermalListenerObj = null
            }
        }

        // 3. בדיקה מחזורית קלה של זיכרון פנוי / חום / סוללה כל 18 שניות (עובדת רק כשהאפליקציה בחזית)
        val poll = object : Runnable {
            override fun run() {
                if (!isMonitoringActive) return
                val host = hostRef?.get()
                if (host != null && !host.isFinishing && !host.isDestroyed) {
                    if (adaptationMode == AdaptationMode.AUTO) {
                        // אם הזיכרון התייצב — משחררים לחץ זיכרון ישן
                        if (memoryPressureLevel > 0) memoryPressureLevel = 0
                        evaluateLiveConditions(host, notifyOnTierChange = true)
                    }
                    mainHandler.postDelayed(this, LIVE_POLL_INTERVAL_MS)
                }
            }
        }
        periodicCheckRunnable = poll
        mainHandler.postDelayed(poll, LIVE_POLL_INTERVAL_MS)
    }

    private fun stopLiveMonitoring(activity: MainActivity) {
        isMonitoringActive = false
        periodicCheckRunnable?.let { mainHandler.removeCallbacks(it) }
        periodicCheckRunnable = null

        val recv = systemStateReceiver
        systemStateReceiver = null
        if (recv != null) {
            try { activity.unregisterReceiver(recv) } catch (_: Throwable) {}
        }

        val tObj = thermalListenerObj
        thermalListenerObj = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && tObj is PowerManager.OnThermalStatusChangedListener) {
            try {
                val pm = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.removeThermalStatusListener(tObj)
            } catch (_: Throwable) {}
        }
    }

    private fun evaluateLiveConditions(activity: MainActivity, notifyOnTierChange: Boolean) {
        if (activity.isFinishing || activity.isDestroyed) return
        val profile = detectHardwareProfile(activity)
        lastProfile = profile
        val prevTier = activeTier
        recomputeActiveTier(profile)

        if (activeTier != prevTier) {
            applyTierToEngines(
                activity = activity,
                forcePackSwitch = (adaptationMode == AdaptationMode.AUTO),
                notifyUser = notifyOnTierChange
            )
            // עדכון שקט של תווית הכפתור אם המשתמש נמצא כרגע בהגדרות
            if (activity.currentTabIndex == 19) {
                val btn = activity.contentFrame.findViewWithTag<TextView>(SETTINGS_BTN_TAG)
                btn?.text = IosGlassTheme.cleanCategoryTitle(getSettingsButtonLabel(activity), activity.isCleanGlassActive)
                applySettingsButtonIcon(activity.contentFrame, activity)
            }
        }
    }

    private fun loadPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        adaptationMode = AdaptationMode.fromId(prefs.getInt(PREF_KEY_MODE, AdaptationMode.AUTO.id))
    }

    fun setMode(activity: MainActivity, mode: AdaptationMode, applyPack: Boolean = true) {
        adaptationMode = mode
        memoryPressureLevel = 0
        activity.prefs?.edit()?.putInt(PREF_KEY_MODE, mode.id)?.apply()
        val profile = detectHardwareProfile(activity)
        lastProfile = profile
        recomputeActiveTier(profile)
        applyTierToEngines(activity, forcePackSwitch = applyPack, notifyUser = false)
    }

    private fun recomputeActiveTier(profile: HardwareProfile) {
        activeTier = when (adaptationMode) {
            AdaptationMode.AUTO -> {
                var tier = profile.recommendedTier
                if (memoryPressureLevel >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                    tier = PerformanceTier.ECO_LOW_END
                } else if (memoryPressureLevel >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW && tier.level > PerformanceTier.BALANCED.level) {
                    tier = PerformanceTier.BALANCED
                }
                tier
            }
            AdaptationMode.ULTRA -> PerformanceTier.ULTRA
            AdaptationMode.HIGH -> PerformanceTier.HIGH
            AdaptationMode.BALANCED -> PerformanceTier.BALANCED
            AdaptationMode.ECO -> PerformanceTier.ECO_LOW_END
        }
    }

    private fun applyTierToEngines(activity: MainActivity, forcePackSwitch: Boolean, notifyUser: Boolean) {
        val tier = activeTier
        val profile = lastProfile

        // 1. הפעלה/השהיה מיידית של אנימציות רקע
        if (!tier.allowAmbientCanvas) {
            ProCanvasAnimations.onActivityPause()
        } else {
            ProCanvasAnimations.onActivityResume()
        }

        // 2. כיבוי מיידי של טשטוש זכוכית כבד אם הורדנו רמה
        if (!tier.allowRealtimeBlur) {
            IosGlassTheme.applyRealtimeGlassBlur(activity.mainContent, false)
        }

        // 3. התאמת חבילת אנימציות אוטומטית (כולל שמירה ושחזור של החבילה המועדפת על המשתמש)
        if (adaptationMode == AdaptationMode.AUTO && profile?.systemReduceMotion == true) {
            saveCurrentRichPackIfApplicable(activity)
            ProUiMotionPacks.selectPack(activity, ProUiMotionPacks.MotionPack.LITE_SNAP, autoEnableIfOff = false)
        } else if (forcePackSwitch) {
            val currentPack = ProUiMotionPacks.currentPack
            when (tier) {
                PerformanceTier.ECO_LOW_END -> {
                    saveCurrentRichPackIfApplicable(activity)
                    val target = if (profile?.isPowerSaveMode == true || profile?.isThermalThrottled == true) {
                        ProUiMotionPacks.MotionPack.LITE_SNAP
                    } else {
                        ProUiMotionPacks.MotionPack.LITE_FADE
                    }
                    ProUiMotionPacks.selectPack(activity, target, autoEnableIfOff = false)
                }
                PerformanceTier.BALANCED -> {
                    saveCurrentRichPackIfApplicable(activity)
                    if (!currentPack.isLowPower) {
                        ProUiMotionPacks.selectPack(activity, ProUiMotionPacks.MotionPack.LITE_SOFT_LIFT, autoEnableIfOff = false)
                    }
                }
                PerformanceTier.HIGH, PerformanceTier.ULTRA -> {
                    // שחזור אוטומטי של החבילה העשירה שהמשתמש אהב כשהעומס/חיסכון הסוללה מסתיים!
                    val savedId = activity.prefs?.getInt(PREF_KEY_SAVED_RICH_PACK, -1) ?: -1
                    val restored = if (savedId >= 0) {
                        ProUiMotionPacks.MotionPack.fromId(savedId)
                    } else {
                        when {
                            activity.isMinimalistMode -> ProUiMotionPacks.MotionPack.ZEN_SILK
                            activity.isAuroraMode -> ProUiMotionPacks.MotionPack.AURORA_COSMIC
                            else -> ProUiMotionPacks.MotionPack.LIQUID_SPRING
                        }
                    }
                    if (currentPack.isLowPower) {
                        ProUiMotionPacks.selectPack(activity, restored, autoEnableIfOff = false)
                    }
                }
            }
        }

        if (notifyUser) {
            Toast.makeText(
                activity,
                "⚡ ביצועים הותאמו אוטומטית: ${tier.shortName}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveCurrentRichPackIfApplicable(activity: MainActivity) {
        val cur = ProUiMotionPacks.currentPack
        if (!cur.isLowPower) {
            activity.prefs?.edit()?.putInt(PREF_KEY_SAVED_RICH_PACK, cur.id)?.apply()
        }
    }

    // =========================================================================
    // 2. סורק חומרה מלא (RAM, CPU, Hz, סוללה, חום, מערכת)
    // =========================================================================
    fun detectHardwareProfile(activity: MainActivity): HardwareProfile {
        val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        try { am?.getMemoryInfo(memInfo) } catch (_: Throwable) {}

        val totalRamGb = (memInfo.totalMem.toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat().coerceAtLeast(1.0f)
        val availRamGb = (memInfo.availMem.toDouble() / (1024.0 * 1024.0 * 1024.0)).toFloat().coerceAtLeast(0.1f)
        val isLowRam = try { am?.isLowRamDevice == true || memInfo.lowMemory } catch (_: Throwable) { false }
        val heapMb = try { am?.memoryClass ?: 192 } catch (_: Throwable) { 192 }

        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val maxGhz = readMaxCpuFreqGhz()
        val is64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

        val hz = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display?.refreshRate ?: 60f
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay?.refreshRate ?: 60f
            }
        } catch (_: Throwable) { 60f }

        val pm = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = try { pm?.isPowerSaveMode == true } catch (_: Throwable) { false }
        val isThermal = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm != null) {
                pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
            } else false
        } catch (_: Throwable) { false }

        var battPct = 100
        var isCharging = false
        try {
            val battIntent = activity.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (battIntent != null) {
                val lvl = battIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = battIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                if (lvl >= 0) battPct = ((lvl * 100f) / scale).roundToInt().coerceIn(0, 100)
                val st = battIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = (st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL)
            }
        } catch (_: Throwable) {}

        val sysAnimScale = try {
            Settings.Global.getFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        } catch (_: Throwable) { 1f }
        val reduceMotion = sysAnimScale <= 0.05f

        val socName = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Build.SOC_MODEL.isNullOrBlank()) {
                Build.SOC_MODEL
            } else {
                Build.HARDWARE ?: Build.MODEL
            }
        } catch (_: Throwable) { Build.MODEL ?: "Android" }

        var score = 0
        score += when {
            totalRamGb >= 10.5f -> 36
            totalRamGb >= 7.2f -> 31
            totalRamGb >= 5.2f -> 24
            totalRamGb >= 3.4f -> 15
            else -> 6
        }
        score += when {
            cores >= 8 -> 16
            cores >= 6 -> 12
            cores >= 4 -> 7
            else -> 3
        }
        score += when {
            maxGhz >= 2.7f -> 13
            maxGhz >= 2.1f -> 10
            maxGhz >= 1.6f -> 6
            else -> 3
        }
        if (is64Bit) score += 5
        score += when {
            hz >= 115f -> 12
            hz >= 85f -> 9
            else -> 5
        }
        score += when {
            heapMb >= 384 -> 10
            heapMb >= 256 -> 7
            else -> 4
        }
        score += if (Build.VERSION.SDK_INT >= 33) 8 else if (Build.VERSION.SDK_INT >= 30) 6 else 3

        if (isLowRam) score = min(score, 32)
        score = score.coerceIn(5, 100)

        var tier = when {
            isLowRam || totalRamGb < 3.4f || availRamGb < 0.55f || score < 44 -> PerformanceTier.ECO_LOW_END
            availRamGb < 1.0f || score < 64 -> PerformanceTier.BALANCED
            score < 82 -> PerformanceTier.HIGH
            else -> PerformanceTier.ULTRA
        }

        if (isPowerSave || isThermal || reduceMotion || (!isCharging && battPct <= 15)) {
            tier = if (isThermal || isPowerSave || reduceMotion) {
                PerformanceTier.ECO_LOW_END
            } else {
                when (tier) {
                    PerformanceTier.ULTRA, PerformanceTier.HIGH -> PerformanceTier.BALANCED
                    else -> PerformanceTier.ECO_LOW_END
                }
            }
        }

        return HardwareProfile(
            totalRamGb = totalRamGb,
            availRamGb = availRamGb,
            isLowRamDevice = isLowRam,
            cpuCores = cores,
            maxCpuGhz = maxGhz,
            is64Bit = is64Bit,
            refreshRateHz = hz,
            heapClassMb = heapMb,
            isPowerSaveMode = isPowerSave,
            isThermalThrottled = isThermal,
            batteryPct = battPct,
            isCharging = isCharging,
            systemReduceMotion = reduceMotion,
            socLabel = socName,
            hardwareScore = score,
            recommendedTier = tier
        )
    }

    private fun readMaxCpuFreqGhz(): Float {
        var maxKhz = 0L
        for (i in 0 until 8) {
            try {
                val f = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (f.exists() && f.canRead()) {
                    val khz = f.readText().trim().toLongOrNull() ?: 0L
                    if (khz > maxKhz) maxKhz = khz
                }
            } catch (_: Throwable) {}
        }
        return if (maxKhz > 0L) (maxKhz / 1_000_000f) else 2.0f
    }

    // =========================================================================
    // 3. ממשק שאילתות מהיר לכל רכיבי האפליקציה
    // =========================================================================
    fun allowRealtimeBlur(): Boolean = activeTier.allowRealtimeBlur
    fun allowGlowShadows(): Boolean = activeTier.allowGlowShadows
    fun allowAmbientCanvas(): Boolean = activeTier.allowAmbientCanvas
    fun allowElevationShadows(): Boolean = activeTier.allowElevationShadows

    fun scaleParticleCount(baseCount: Int): Int {
        if (baseCount <= 0) return 0
        return (baseCount * activeTier.particleMultiplier).roundToInt().coerceAtLeast(2)
    }

    fun scaleDuration(baseMs: Long): Long =
        (baseMs * activeTier.durationScale).toLong().coerceAtLeast(45L)

    fun shouldRenderFrame(lastFrameElapsedMs: Long): Boolean {
        val interval = activeTier.minFrameIntervalMs
        if (interval <= 16L) return true
        return (SystemClock.elapsedRealtime() - lastFrameElapsedMs) >= interval
    }

    // =========================================================================
    // 4. כפתור בהגדרות + סמל שבב וקטורי + חלון בקרה
    // =========================================================================
    fun getSettingsButtonLabel(activity: MainActivity): String {
        val modeTag = if (adaptationMode == AdaptationMode.AUTO) "אוטומטי חי • ${activeTier.shortName}" else activeTier.shortName
        return if (activity.isCleanGlassActive) {
            "התאמה לחומרת המכשיר: $modeTag"
        } else {
            "⚡ התאמה לחומרת המכשיר: $modeTag"
        }
    }

    fun createSettingsButton(activity: MainActivity): TextView {
        return activity.createActionBtn(getSettingsButtonLabel(activity), false) {
            showHardwareDashboardDialog(activity)
        }.apply {
            tag = SETTINGS_BTN_TAG
        }
    }

    fun applySettingsButtonIcon(settingsRoot: View?, activity: MainActivity) {
        if (settingsRoot == null) return
        val btn = settingsRoot.findViewWithTag<TextView>(SETTINGS_BTN_TAG) ?: return
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode

        val isDarkText = MinimalistTheme.computeLuminance(btn.currentTextColor) < 0.25
        val tintCol = when {
            isMin -> MinimalistTheme.TEXT_CHARCOAL
            isDarkText -> btn.currentTextColor
            else -> MinimalistTheme.resolveDrawerIconColor(activity, isSelected = false, isExpandedHeader = false)
        }

        val innerTint = if (isMin || isAur) tintCol else Color.WHITE
        val innerSize = if (isMin || isAur) 22f else 15.5f
        val raw = createChipGlyphDrawable(activity, innerTint, innerSize, isMin, isAur)
        val icon = when {
            isMin || isAur -> raw
            isGlass -> ProUnifiedThemeIcons.wrapForLiquidGlass(activity, raw, tintCol, 25f)
            else -> ProUnifiedThemeIcons.wrapForClassicBadge(activity, raw, 909, 25f)
        }

        btn.compoundDrawablePadding = activity.dp(12f)
        if (activity.isRightHanded) {
            btn.setCompoundDrawables(null, null, icon, null)
        } else {
            btn.setCompoundDrawables(icon, null, null, null)
        }
    }

    private fun createChipGlyphDrawable(
        context: Context,
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

                val body = r * 0.56f
                canvas.drawRoundRect(RectF(cx - body, cy - body, cx + body, cy + body), r * 0.16f, r * 0.16f, strokeP)
                val core = r * 0.22f
                canvas.drawRoundRect(RectF(cx - core, cy - core, cx + core, cy + core), r * 0.08f, r * 0.08f, fillP)

                val pinOut = r * 0.88f
                val off = r * 0.26f
                for (sign in floatArrayOf(-1f, 1f)) {
                    canvas.drawLine(cx + sign * off, cy - body, cx + sign * off, cy - pinOut, strokeP)
                    canvas.drawLine(cx + sign * off, cy + body, cx + sign * off, cy + pinOut, strokeP)
                    canvas.drawLine(cx - body, cy + sign * off, cx - pinOut, cy + sign * off, strokeP)
                    canvas.drawLine(cx + body, cy + sign * off, cx + pinOut, cy + sign * off, strokeP)
                }
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; fillP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; fillP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    fun showHardwareDashboardDialog(activity: MainActivity) {
        val profile = detectHardwareProfile(activity)
        lastProfile = profile
        recomputeActiveTier(profile)

        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = activity.isMinimalistMode
        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#CBD5E1")
        val tierAccent = Color.parseColor(activeTier.accentHex)
        val accentCol = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }

        val dialogBg = when {
            isMin -> GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(MinimalistTheme.BG_CREAM)
                setStroke(activity.dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#E0DBD3")))
            }
            activity.isAuroraMode -> GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(Color.argb(244, Color.red(AuroraBorealisTheme.ARCTIC_BLUE), Color.green(AuroraBorealisTheme.ARCTIC_BLUE), Color.blue(AuroraBorealisTheme.ARCTIC_BLUE)))
                setStroke(activity.dp(1.4f), Color.argb(180, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY)))
            }
            activity.isIosGlassMode -> GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(Color.argb(238, 22, 29, 44))
                setStroke(activity.dp(1.3f), Color.argb(135, 255, 255, 255))
            }
            else -> GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(Utils.lightenColor(activity.bgColor, 0.08f))
                setStroke(activity.dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(activity, activity.primaryColor))
            }
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(18f), activity.dp(18f), activity.dp(18f), activity.dp(16f))
            background = dialogBg
        }

        root.addView(TextView(activity).apply {
            text = "\u200Fהתאמה חכמה לחומרת המכשיר"
            setTextColor(textPrimary)
            textSize = 18.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
        })

        root.addView(TextView(activity).apply {
            text = "\u200Fרמה פעילה: ${activeTier.hebrewName}  •  ציון חומרה: ${profile.hardwareScore}/100"
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(tierAccent, if (isMin) MinimalistTheme.BG_CREAM else Color.parseColor("#161D2C"), 2.3))
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(4f), 0, activity.dp(10f))
        })

        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.54f).toInt().coerceAtMost(activity.dp(430f))
            ).apply { bottomMargin = activity.dp(10f) }
        }
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(body)
        root.addView(scroll)

        val cardFill = if (isMin) MinimalistTheme.CARD_WHITE else Color.argb(34, 255, 255, 255)
        val specCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(14f), activity.dp(12f), activity.dp(14f), activity.dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(18f).toFloat()
                setColor(cardFill)
                setStroke(activity.dp(1f), if (isMin) Color.parseColor("#E5DFD7") else Color.argb(46, 255, 255, 255))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }

        val ramStr = String.format(Locale.US, "%.1f / %.1f GB", profile.availRamGb, profile.totalRamGb)
        val cpuStr = String.format(Locale.US, "%d ליבות • %.1f GHz", profile.cpuCores, profile.maxCpuGhz)
        val hzStr = "${profile.refreshRateHz.roundToInt()} Hz"
        val powerNote = when {
            profile.isThermalThrottled -> "התחממות פעילה (מוגבל אוטומטית)"
            profile.isPowerSaveMode -> "חיסכון סוללה פעיל (הותאם אוטומטית)"
            profile.isCharging -> "בטעינה (${profile.batteryPct}%)"
            else -> "תקין (${profile.batteryPct}%)"
        }

        specCard.addView(TextView(activity).apply {
            text = "\u200Fמפרט המכשיר שזוהה (ניטור חי פעיל):"
            setTextColor(textPrimary)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, activity.dp(4f))
        })
        specCard.addView(TextView(activity).apply {
            text = "\u200F• זיכרון RAM פנוי/כולל: ${BidiFmt.ltr(ramStr)}\n" +
                "• מעבד: ${BidiFmt.ltr(profile.socLabel)} ($cpuStr)\n" +
                "• קצב רענון מסך: ${BidiFmt.ltr(hzStr)}  |  Android ${Build.VERSION.RELEASE}\n" +
                "• מצב חום וסוללה: $powerNote"
            setTextColor(textSecondary)
            textSize = 12.4f
            gravity = Gravity.RIGHT
            setLineSpacing(activity.dp(3f).toFloat(), 1.05f)
        })
        body.addView(specCard)

        val effectCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(14f), activity.dp(12f), activity.dp(14f), activity.dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(18f).toFloat()
                setColor(cardFill)
                setStroke(activity.dp(1f), tierAccent)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }
        effectCard.addView(TextView(activity).apply {
            text = "\u200Fאיכות גרפיקה ואנימציות פעילה כעת:"
            setTextColor(textPrimary)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, activity.dp(4f))
        })
        effectCard.addView(TextView(activity).apply {
            val pctParticles = (activeTier.particleMultiplier * 100).roundToInt()
            text = "\u200F• קצב פריימים בקנבס ובמשחקים: ${BidiFmt.ltr("${activeTier.targetFps} FPS")}\n" +
                "• צפיפות חלקיקים ואפקטים: ${BidiFmt.ltr("$pctParticles%")}\n" +
                "• טשטוש זכוכית בזמן אמת: ${if (activeTier.allowRealtimeBlur) "מופעל" else "כבוי (חיסכון GPU)"}\n" +
                "• הילות ניאון והצללות זוהר: ${if (activeTier.allowGlowShadows) "מופעל" else "כבוי (חלק יותר)"}\n" +
                "• אנימציות רקע דינמיות: ${if (activeTier.allowAmbientCanvas) "מופעל" else "מושהה לחיסכון"}\n" +
                "• חבילת אנימציות ממשק: ${ProUiMotionPacks.currentPack.displayName}"
            setTextColor(textSecondary)
            textSize = 12.4f
            gravity = Gravity.RIGHT
            setLineSpacing(activity.dp(3f).toFloat(), 1.05f)
        })
        body.addView(effectCard)

        body.addView(TextView(activity).apply {
            text = "\u200Fבחר מצב התאמה:"
            setTextColor(textPrimary)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(2f), 0, activity.dp(6f))
        })

        for (mode in AdaptationMode.values()) {
            val isSel = (mode == adaptationMode)
            val rowFill = if (isSel) Color.argb(58, Color.red(accentCol), Color.green(accentCol), Color.blue(accentCol)) else cardFill
            val row = TextView(activity).apply {
                val suffix = if (mode == AdaptationMode.AUTO) " — כעת: ${profile.recommendedTier.shortName}" else ""
                text = "\u200F${if (isSel) "✓ " else ""}${mode.label}$suffix"
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, rowFill, 2.3))
                textSize = 13.6f
                typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                setPadding(activity.dp(14f), activity.dp(11f), activity.dp(14f), activity.dp(11f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(15f).toFloat()
                    setColor(rowFill)
                    setStroke(activity.dp(if (isSel) 1.5f else 1f), if (isSel) accentCol else Color.argb(40, 128, 128, 128))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = activity.dp(6f) }
                setOnClickListener {
                    activity.feedback(it)
                    setMode(activity, mode, applyPack = true)
                    Toast.makeText(activity, "✓ הוחל פרופיל: ${activeTier.hebrewName}", Toast.LENGTH_SHORT).show()
                    ProCrashGuardAndRecovery.safeDismissDialog(dlg)
                    activity.applyTheme()
                }
            }
            body.addView(row)
        }

        val closeBtn = TextView(activity).apply {
            text = "סגור"
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.parseColor("#08090C"), accentCol, 2.3))
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(activity.dp(12f), activity.dp(12f), activity.dp(12f), activity.dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(accentCol)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                activity.feedback(it)
                ProCrashGuardAndRecovery.safeDismissDialog(dlg)
            }
        }
        root.addView(closeBtn)

        dlg.setContentView(root)
        if (!ProCrashGuardAndRecovery.safeShowDialog(activity, dlg)) return
        ProUiMotionEngine.animateDialogEntrance(activity, root)
        val widthPx = (activity.resources.displayMetrics.widthPixels * 0.92f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}

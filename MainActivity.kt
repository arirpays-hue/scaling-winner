package com.example.calculator

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.RippleDrawable
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.Rational
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.*
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    var prefs: SharedPreferences? = null
    private lateinit var rootFrame: FrameLayout
    var mainContent: LinearLayout? = null
    private lateinit var contentFrame: FrameLayout
    private lateinit var dimView: View
    var drawerMenu: LinearLayout? = null
    private lateinit var drawerScroll: ScrollView
    private lateinit var topHeader: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var menuBtn: TextView
    private lateinit var pipBtn: TextView
    private lateinit var historyBtn: TextView
    private var appLogoView: TextView? = null
    private val drawerItemsViews = mutableListOf<View>()
    private val drawerSeparators = mutableListOf<View>()
    private val drawerSectionHeaders = mutableListOf<Triple<Int, TextView, LinearLayout>>()
    private val expandedSections = mutableSetOf<Int>()
    val keypadButtons = mutableMapOf<String, TextView>()
    var tts: TextToSpeech? = null

    private lateinit var soundPool: SoundPool
    var soundIds = IntArray(13)
    var acSoundId = 0

    var useHaptic = true; var useSound = true; var useAnimations = true; var keepScreenOn = false; var soundType = 0
    var isRightHanded = true
    var isIosGlassMode = false
    var isAuroraMode = false
    val isCleanGlassActive: Boolean get() = isIosGlassActiveOrAurora

    private val isIosGlassActiveOrAurora: Boolean get() = isIosGlassMode || isAuroraMode

    var settingsScrollY = 0
    var currentSettingsScroll: ScrollView? = null
    var calcPrecision = 8; var buttonCornerStyle = 24f
    var primaryColor = Color.parseColor("#64B5F6"); var bgColor = Color.parseColor("#08090C")
    var memoryValue = 0.0; var currentTabIndex = 0
    var historyList = mutableListOf<String>()
    var isDrawerOpen = false

    var fromCurrency = "USD"; var toCurrency = "ILS"
    var fromDist = "קילומטר (km)"; var toDist = "מייל (mi)"
    var fromWeight = "קילוגרם (kg)"; var toWeight = "פאונד (lbs)"
    var fromTemp = "צלזיוס (°C)"; var toTemp = "פרנהייט (°F)"
    var fromSpeed = "קמ״ש (km/h)"; var toSpeed = "מייל לשעה (mph)"

    val cachedRates = mutableMapOf<String, Double>()

    var allCurrencies = mutableListOf(
        "דולר אמריקאי (ארה״ב) - USD", "שקל חדש (ישראל) - ILS", "אירו (האיחוד האירופי) - EUR",
        "לירה שטרלינג (בריטניה) - GBP", "ין יפני (יפן) - JPY", "דולר קנדי (קנדה) - CAD",
        "דולר אוסטרלי (אוסטרליה) - AUD", "פרנק שווייצרי (שווייץ) - CHF"
    )

    val categories = listOf(
        "🧮 מחשבון רגיל",           // 0
        "🔬 מחשבון מדעי",           // 1
        "🧾 מחשבון מע״מ",           // 2
        "🏦 מחשבון הלוואה",         // 3
        "📏 המרת מרחקים",           // 4
        "⚖️ המרת משקלים",           // 5
        "🌡️ המרת טמפ׳",            // 6
        "💻 מחשבון מתכנתים",        // 7
        "📊 חישוב אחוזים",          // 8
        "🏷️ חישוב הנחות",           // 9
        "💰 חלוקת טיפים",           // 10
        "🏃 מדד BMI",              // 11
        "🎂 חישוב גיל",             // 12
        "💵 מטבעות לייב",           // 13
        "🔥 מחשבון BMR",            // 14
        "➗ משוואות ריבועיות",      // 15
        "⅟ מחשבון שברים",          // 16
        "📈 ריבית דריבית",          // 17
        "🚀 המרת מהירות",           // 18
        "⚙️ הגדרות",               // 19
        "⛽ מחשבון עלות דלק",       // 20
        "🚘 יעילות שריפת דלק",      // 21
        "🎓 ממוצע ציונים",          // 22
        "🍽️ מחשבון תשר מתקדם",      // 23
        "🛍️ מחשבון מס מכירות",      // 24
        "🏷️ מחשבון מחיר יחידתי",    // 25
        "🌍 ממיר שעון עולמי",       // 26
        "🌸 מחשבון ביוץ",           // 27
        "🔢 ממיר הקסדצימלי",        // 28
        "💎 מחשבון חסכונות",        // 29
        "📅 מחשבון תאריך"           // 30
    )

    // חלוקת התפריט לקטגוריות עם אייקון וקטורי לכל קטגוריה (למעט הגדרות)
    private val drawerSections = listOf(
        Triple(301, "📐 מתמטיקה ומדע", listOf(0, 1, 8, 15, 16, 7, 28, 22)),
        Triple(302, "💳 פיננסים וצרכנות", listOf(2, 24, 3, 17, 29, 9, 25, 10, 23, 13)),
        Triple(303, "🔄 המרות, רכב ודלק", listOf(4, 5, 6, 18, 20, 21)),
        Triple(304, "❤️ בריאות, זמן ותאריכים", listOf(11, 14, 27, 12, 30, 26))
    )

    var currentInput = ""; var justCalculated = false
    var expressionView: TextView? = null; var currentView: TextView? = null; var memoryIndicator: TextView? = null
    val VOICE_REQUEST_CODE = 999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("CalcProPrefs", Context.MODE_PRIVATE); loadPreferences()
        tts = TextToSpeech(this, this)

        val audioAttributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(audioAttributes).build()
        for (i in 1..3) {
            val rId = resources.getIdentifier("sound$i", "raw", packageName)
            if (rId != 0) soundIds[i] = soundPool.load(this, rId, 1)
        }

        val acResId = resources.getIdentifier("ac_sound", "raw", packageName)
        if (acResId != 0) acSoundId = soundPool.load(this, acResId, 1)

        fetchAndCacheCurrencies()

        rootFrame = FrameLayout(this)
        mainContent = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) }

        topHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(20f), dp(20f), dp(20f), dp(16f)) }
        menuBtn = TextView(this).apply { text = "☰"; setTextColor(Color.WHITE); textSize = 28f; setPadding(dp(8f), 0, dp(8f), 0); setOnClickListener { feedback(it); if (isDrawerOpen) closeDrawer() else openDrawer() } }
        titleText = TextView(this).apply { text = "מחשבון פרימיום"; setTextColor(Color.WHITE); textSize = 22f; typeface = Typeface.DEFAULT_BOLD; layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(12f), 0, dp(12f), 0) } }

        pipBtn = TextView(this).apply { text = "🧮"; textSize = 22f; gravity = Gravity.CENTER; setPadding(dp(8f), dp(8f), dp(12f), dp(8f)); setOnClickListener { feedback(it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(3, 4)).build()) } else { Toast.makeText(this@MainActivity, "חלון צף נתמך רק באנדרואיד 8 ומעלה", Toast.LENGTH_SHORT).show() }
        } }
        historyBtn = TextView(this).apply { text = "🕒"; textSize = 20f; gravity = Gravity.CENTER; setPadding(dp(8f), dp(8f), dp(4f), dp(8f)); setOnClickListener { feedback(it); showHistoryDialog() } }

        contentFrame = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        mainContent?.addView(topHeader); mainContent?.addView(contentFrame)

        dimView = View(this).apply { setBackgroundColor(Color.parseColor("#B3000000")); visibility = View.GONE; alpha = 0f; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT); setOnClickListener { if (isDrawerOpen) closeDrawer() } }
        drawerScroll = ScrollView(this).apply { isClickable = true }
        drawerMenu = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(30f), 0, dp(30f)) }

        val appLogo = TextView(this).apply { text = "אולר שווייצרי\nPro fx-991"; setTextColor(Color.WHITE); textSize = 24f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(22f)) }
        appLogoView = appLogo
        drawerMenu?.addView(appLogo)

        // יצירת כל פריטי המחשבונים מראש לפי אינדקס
        drawerItemsViews.clear()
        for ((index, title) in categories.withIndex()) {
            val isSettings = (index == 19)
            val item = TextView(this).apply {
                text = IosGlassTheme.cleanCategoryTitle(title, isCleanGlassActive)
                textSize = if (isSettings) 16.5f else 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                compoundDrawablePadding = dp(12f)
                setPadding(dp(if (isSettings) 20f else 28f), dp(13f), dp(if (isSettings) 20f else 28f), dp(13f))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(10f), dp(2f), dp(10f), dp(2f))
                }
                setOnClickListener { feedback(it); settingsScrollY = 0; switchTab(index); closeDrawer() }
            }
            drawerItemsViews.add(item)
        }

        // הוספת הקטגוריות לתפריט הצד כך שרק שם וסמל הקטגוריה מוצגים, ובלחיצה נפתחים המחשבונים שלה
        drawerSectionHeaders.clear()
        drawerSeparators.clear()
        for ((secIconCode, secTitle, itemIndices) in drawerSections) {
            val sectionItemsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
            }

            val headerTv = TextView(this).apply {
                tag = secTitle
                textSize = 16.5f
                typeface = Typeface.DEFAULT_BOLD
                compoundDrawablePadding = dp(12f)
                setPadding(dp(20f), dp(15f), dp(20f), dp(15f))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(10f), dp(4f), dp(10f), dp(4f))
                }
                setOnClickListener {
                    feedback(it)
                    val isCurrentlyOpen = expandedSections.contains(secIconCode)
                    if (isCurrentlyOpen) {
                        expandedSections.remove(secIconCode)
                        sectionItemsContainer.visibility = View.GONE
                    } else {
                        expandedSections.add(secIconCode)
                        sectionItemsContainer.visibility = View.VISIBLE
                        if (useAnimations) {
                            sectionItemsContainer.alpha = 0f
                            sectionItemsContainer.translationY = -dp(8f).toFloat()
                            sectionItemsContainer.animate().alpha(1f).translationY(0f).setDuration(200).setInterpolator(DecelerateInterpolator()).start()
                        }
                    }
                    updateDrawerSelection()
                }
            }

            for (idx in itemIndices) {
                sectionItemsContainer.addView(drawerItemsViews[idx])
                val sep = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)).apply {
                        setMargins(dp(28f), 0, dp(28f), 0)
                    }
                    setBackgroundColor(Color.TRANSPARENT)
                }
                drawerSeparators.add(sep)
                sectionItemsContainer.addView(sep)
            }

            drawerSectionHeaders.add(Triple(secIconCode, headerTv, sectionItemsContainer))
            drawerMenu?.addView(headerTv)

            val catSep = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)).apply {
                    setMargins(dp(20f), 0, dp(20f), 0)
                }
                setBackgroundColor(Color.TRANSPARENT)
            }
            drawerSeparators.add(catSep)
            drawerMenu?.addView(catSep)
            drawerMenu?.addView(sectionItemsContainer)
        }

        // הוספת "הגדרות" (אינדקס 19) בתחתית התפריט מחוץ לקטגוריות
        val settingsTopDivider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1.5f)).apply {
                setMargins(dp(16f), dp(14f), dp(16f), dp(6f))
            }
            setBackgroundColor(Color.TRANSPARENT)
        }
        drawerSeparators.add(settingsTopDivider)
        drawerMenu?.addView(settingsTopDivider)
        drawerMenu?.addView(drawerItemsViews[19])

        drawerScroll.addView(drawerMenu); rootFrame.addView(mainContent); rootFrame.addView(dimView); rootFrame.addView(drawerScroll)
        updateHandednessLayout()
        setContentView(rootFrame); setupShortcuts(); handleIntent(intent)
    }

    private fun getDrawerWidthPx(): Int {
        val screenW = resources.displayMetrics.widthPixels
        return if (isCleanGlassActive && screenW > 0) (screenW * 0.75f).toInt() else dp(285f)
    }

    fun updateHandednessLayout() {
        topHeader.layoutDirection = View.LAYOUT_DIRECTION_LTR
        topHeader.removeAllViews()
        if (isRightHanded) {
            topHeader.addView(historyBtn)
            topHeader.addView(pipBtn)
            topHeader.addView(titleText)
            topHeader.addView(menuBtn)
            titleText.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        } else {
            topHeader.addView(menuBtn)
            topHeader.addView(titleText)
            topHeader.addView(pipBtn)
            topHeader.addView(historyBtn)
            titleText.gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
        }
        for (v in drawerItemsViews) {
            (v as? TextView)?.gravity = (if (isRightHanded) Gravity.RIGHT else Gravity.LEFT) or Gravity.CENTER_VERTICAL
        }
        for ((_, headerTv, _) in drawerSectionHeaders) {
            headerTv.gravity = (if (isRightHanded) Gravity.RIGHT else Gravity.LEFT) or Gravity.CENTER_VERTICAL
        }
        val drawerW = getDrawerWidthPx()
        drawerScroll.layoutParams = FrameLayout.LayoutParams(drawerW, FrameLayout.LayoutParams.MATCH_PARENT).apply {
            gravity = if (isRightHanded) Gravity.RIGHT else Gravity.LEFT
        }
        if (!isDrawerOpen) {
            drawerScroll.translationX = if (isRightHanded) drawerW.toFloat() else -drawerW.toFloat()
        }
    }

    override fun onInit(status: Int) { if (status == TextToSpeech.SUCCESS) tts?.language = Locale("he", "IL") }
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) { super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig); if (isInPictureInPictureMode) { topHeader.visibility = View.GONE; if (isDrawerOpen) closeDrawer() } else { topHeader.visibility = View.VISIBLE } }
    override fun onNewIntent(intent: Intent?) { super.onNewIntent(intent); handleIntent(intent) }
    private fun handleIntent(intent: Intent?) { when (intent?.action) { "ACTION_CALC" -> { currentTabIndex = 0; applyTheme() }; "ACTION_SCI" -> { currentTabIndex = 1; applyTheme() }; "ACTION_COIN" -> { currentTabIndex = 13; applyTheme() }; else -> applyTheme() } }
    private fun setupShortcuts() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) { val sm = getSystemService(ShortcutManager::class.java); val s1 = ShortcutInfo.Builder(this, "coin").setShortLabel("מטבעות").setIcon(Icon.createWithResource(this, android.R.drawable.star_on)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_COIN" }).build(); val s2 = ShortcutInfo.Builder(this, "sci").setShortLabel("מדעית").setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_edit)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_SCI" }).build(); sm.dynamicShortcuts = listOf(s1, s2) } }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateHandednessLayout()
        if (currentTabIndex == 0 || currentTabIndex == 1) {
            contentFrame.removeAllViews()
            val isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
            val targetView = if (isLandscape) createScientificCalculator() else if (currentTabIndex == 1) createScientificCalculator() else createBasicCalculator()
            contentFrame.addView(targetView)
            if (useAnimations) {
                targetView.alpha = 0f
                targetView.translationX = -dp(42f).toFloat()
                targetView.scaleX = 0.94f
                targetView.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .scaleX(1f)
                    .setDuration(350)
                    .setInterpolator(OvershootInterpolator(1.1f))
                    .start()
            }
        }
    }

    private fun loadPreferences() {
        useHaptic = prefs?.getBoolean("useHaptic", true) ?: true; useSound = prefs?.getBoolean("useSound", true) ?: true; useAnimations = prefs?.getBoolean("useAnimations", true) ?: true
        keepScreenOn = prefs?.getBoolean("keepScreenOn", false) ?: false
        soundType = (prefs?.getInt("soundType", 0) ?: 0).coerceIn(0, 3)
        isRightHanded = prefs?.getBoolean("isRightHanded", true) ?: true
        isIosGlassMode = prefs?.getBoolean("isIosGlassMode", false) ?: false
        isAuroraMode = prefs?.getBoolean("isAuroraMode", false) ?: false
        if (isAuroraMode && isIosGlassMode) isIosGlassMode = false
        IosGlassTheme.loadIosPrefs(prefs)
        calcPrecision = prefs?.getInt("calcPrecision", 8) ?: 8; buttonCornerStyle = prefs?.getFloat("buttonCornerStyle", 24f) ?: 24f
        primaryColor = prefs?.getInt("primaryColor", Color.parseColor("#64B5F6")) ?: Color.parseColor("#64B5F6"); bgColor = prefs?.getInt("bgColor", Color.parseColor("#08090C")) ?: Color.parseColor("#08090C")
        memoryValue = Double.fromBits(prefs?.getLong("memoryValue", 0L) ?: 0L); val savedHist = prefs?.getString("history", "") ?: ""
        fromCurrency = extractCurrencyCode(prefs?.getString("fromCurrency", "USD") ?: "USD")
        toCurrency = extractCurrencyCode(prefs?.getString("toCurrency", "ILS") ?: "ILS")
        fromDist = prefs?.getString("fromDist", "קילומטר (km)") ?: "קילומטר (km)"; toDist = prefs?.getString("toDist", "מייל (mi)") ?: "מייל (mi)"
        fromWeight = prefs?.getString("fromWeight", "קילוגרם (kg)") ?: "קילוגרם (kg)"; toWeight = prefs?.getString("toWeight", "פאונד (lbs)") ?: "פאונד (lbs)"
        fromTemp = prefs?.getString("fromTemp", "צלזיוס (°C)") ?: "צלזיוס (°C)"; toTemp = prefs?.getString("toTemp", "פרנהייט (°F)") ?: "פרנהייט (°F)"
        fromSpeed = prefs?.getString("fromSpeed", "קמ״ש (km/h)") ?: "קמ״ש (km/h)"; toSpeed = prefs?.getString("toSpeed", "מייל לשעה (mph)") ?: "מייל לשעה (mph)"
        historyList.clear(); if (savedHist.isNotEmpty()) historyList.addAll(savedHist.split("||")); updateKeepScreenOn()
    }

    fun savePreferences() {
        IosGlassTheme.saveIosPrefs(prefs)
        prefs?.edit()?.putBoolean("useHaptic", useHaptic)?.putBoolean("useSound", useSound)?.putBoolean("useAnimations", useAnimations)?.putBoolean("keepScreenOn", keepScreenOn)?.putBoolean("isRightHanded", isRightHanded)?.putBoolean("isIosGlassMode", isIosGlassMode)?.putBoolean("isAuroraMode", isAuroraMode)?.putInt("soundType", soundType.coerceIn(0, 3))?.putInt("calcPrecision", calcPrecision)?.putFloat("buttonCornerStyle", buttonCornerStyle)?.putInt("primaryColor", primaryColor)?.putInt("bgColor", bgColor)?.putLong("memoryValue", memoryValue.toRawBits())?.putString("fromCurrency", fromCurrency)?.putString("toCurrency", toCurrency)?.putString("fromDist", fromDist)?.putString("toDist", toDist)?.putString("fromWeight", fromWeight)?.putString("toWeight", toWeight)?.putString("fromTemp", fromTemp)?.putString("toTemp", toTemp)?.putString("fromSpeed", fromSpeed)?.putString("toSpeed", toSpeed)?.putString("history", historyList.take(30).joinToString("||"))?.apply()
    }
    fun updateKeepScreenOn() { if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

    fun applyTheme() {
        if (!isCleanGlassActive || !isDrawerOpen) {
            IosGlassTheme.applyRealtimeGlassBlur(mainContent, false)
        }

        val statusNavCol = if (isAuroraMode) AuroraBorealisTheme.ARCTIC_BLUE else bgColor
        window.statusBarColor = statusNavCol
        window.navigationBarColor = statusNavCol

        if (isAuroraMode) {
            mainContent?.background = AuroraBorealisTheme.createAuroraAtmosphericBackground()
            drawerMenu?.background = AuroraBorealisTheme.createAuroraCardDrawable(this, 0f, isDrawer = true)
            val iconCol = AuroraBorealisTheme.AURORA_PRIMARY
            pipBtn.text = ""
            pipBtn.setCompoundDrawables(AuroraBorealisTheme.createVectorIcon(this, 100, iconCol, 22f, true), null, null, null)
            historyBtn.text = ""
            historyBtn.setCompoundDrawables(AuroraBorealisTheme.createVectorIcon(this, 101, iconCol, 22f, true), null, null, null)
        } else if (isIosGlassMode) {
            mainContent?.background = IosGlassTheme.createLiquidAmbientBackground(this)
            drawerMenu?.background = IosGlassTheme.createGlassCardDrawable(this, 0f)
            pipBtn.text = ""
            pipBtn.setCompoundDrawables(IosGlassTheme.createSfSymbolDrawable(this, 100, primaryColor, 22f), null, null, null)
            historyBtn.text = ""
            historyBtn.setCompoundDrawables(IosGlassTheme.createSfSymbolDrawable(this, 101, primaryColor, 22f), null, null, null)
        } else {
            mainContent?.setBackgroundColor(bgColor)
            drawerMenu?.setBackgroundColor(Utils.lightenColor(bgColor, 0.06f))
            pipBtn.setCompoundDrawables(null, null, null, null)
            historyBtn.setCompoundDrawables(null, null, null, null)
            pipBtn.text = "🧮"; historyBtn.text = "🕒"
            pipBtn.textSize = 22f; historyBtn.textSize = 20f
            pipBtn.setTextColor(Color.WHITE); historyBtn.setTextColor(Color.WHITE)
        }

        AuroraBorealisTheme.applyHeaderMetallicGradient(appLogoView, isAuroraMode, isIosGlassMode)
        updateHandednessLayout(); updateDrawerSelection(); switchTab(currentTabIndex)
    }

    private fun openDrawer() {
        updateHandednessLayout()
        if (isCleanGlassActive) IosGlassTheme.applyRealtimeGlassBlur(mainContent, true)
        dimView.visibility = View.VISIBLE; dimView.animate().alpha(1f).setDuration(250).start()
        drawerScroll.animate().translationX(0f).setDuration(330)
            .setInterpolator(if (isCleanGlassActive) OvershootInterpolator(0.85f) else DecelerateInterpolator())
            .start()
        isDrawerOpen = true
    }

    private fun closeDrawer() {
        IosGlassTheme.applyRealtimeGlassBlur(mainContent, false)
        val drawerW = getDrawerWidthPx()
        val targetX = if (isRightHanded) drawerW.toFloat() else -drawerW.toFloat()
        dimView.animate().alpha(0f).setDuration(250).withEndAction { dimView.visibility = View.GONE }.start()
        drawerScroll.animate().translationX(targetX).setDuration(250).setInterpolator(AccelerateInterpolator()).start()
        isDrawerOpen = false
    }

    fun feedback(view: View, symbol: String = "") {
        if (useHaptic) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (useSound && !MiniGameEngine.isAnimalParty) {
            if (symbol == "AC" && acSoundId != 0) {
                soundPool.play(acSoundId, 1f, 1f, 0, 0, 1f)
            } else if (soundType == 0) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            } else if (soundType in 1..3) {
                if (soundIds[soundType] != 0) soundPool.play(soundIds[soundType], 1f, 1f, 0, 0, 1f)
                else Toast.makeText(this, "⚠️ קובץ sound${soundType}.mp3 לא נמצא!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyToClipboard(text: String) { val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cb.setPrimaryClip(ClipData.newPlainText("CalcResult", text)); Toast.makeText(this, if (isCleanGlassActive) "הועתק ללוח" else "הועתק ללוח 📋", Toast.LENGTH_SHORT).show() }

    private fun updateDrawerSelection() {
        val activeAccent = if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor
        val sepColor = when {
            isAuroraMode -> Color.argb(38, 0, 245, 212)
            isIosGlassMode -> Color.argb(28, 255, 255, 255)
            else -> Color.argb(25, 255, 255, 255)
        }
        for (sep in drawerSeparators) sep.setBackgroundColor(sepColor)

        // עדכון כותרות הקטגוריות המתקפלות וסמליהן בכל 3 ערכות הנושא
        for ((secIconCode, headerTv, container) in drawerSectionHeaders) {
            val rawSecTitle = headerTv.tag as? String ?: ""
            val cleanTitle = IosGlassTheme.cleanCategoryTitle(rawSecTitle, isCleanGlassActive)
            val isExpanded = expandedSections.contains(secIconCode)
            val arrow = if (isExpanded) "▾" else if (isRightHanded) "◂" else "▸"
            headerTv.text = if (isRightHanded) "$cleanTitle  $arrow" else "$arrow  $cleanTitle"

            val hasActiveChild = drawerSections.find { it.first == secIconCode }?.third?.contains(currentTabIndex) == true
            val headerTint = if (isExpanded || hasActiveChild) activeAccent else Color.WHITE
            headerTv.setTextColor(headerTint)

            val secIcon = AuroraBorealisTheme.createVectorIcon(this, secIconCode, headerTint, 22f, isAuroraMode)
            if (isRightHanded) headerTv.setCompoundDrawables(null, null, secIcon, null)
            else headerTv.setCompoundDrawables(secIcon, null, null, null)

            if (isExpanded || hasActiveChild) {
                if (isCleanGlassActive) {
                    headerTv.background = AuroraBorealisTheme.createActiveDrawerCapsule(this, isAuroraMode)
                } else {
                    headerTv.setBackgroundColor(Color.argb(35, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)))
                }
            } else {
                headerTv.background = null
            }
            container.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        for (i in drawerItemsViews.indices) {
            val tv = drawerItemsViews[i] as TextView
            val isSelected = (i == currentTabIndex)
            val tint = if (isSelected) activeAccent else Color.WHITE
            tv.text = IosGlassTheme.cleanCategoryTitle(categories[i], isCleanGlassActive)
            if (isCleanGlassActive) {
                val icon = AuroraBorealisTheme.createVectorIcon(this, i, tint, 20f, isAuroraMode)
                if (isRightHanded) tv.setCompoundDrawables(null, null, icon, null)
                else tv.setCompoundDrawables(icon, null, null, null)
            } else {
                tv.setCompoundDrawables(null, null, null, null)
            }
            if (isSelected) {
                if (isCleanGlassActive) {
                    tv.background = AuroraBorealisTheme.createActiveDrawerCapsule(this, isAuroraMode)
                } else {
                    tv.setBackgroundColor(Color.argb(50, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)))
                }
                tv.setTextColor(activeAccent)
            } else {
                tv.background = null
                tv.setTextColor(Color.WHITE)
            }
        }
    }

    fun switchTab(index: Int) {
        val wasInSettings = (currentTabIndex == 19 && index == 19)
        if (wasInSettings) {
            settingsScrollY = currentSettingsScroll?.scrollY ?: settingsScrollY
        } else if (index != 19) {
            settingsScrollY = 0
        }
        if (MiniGameEngine.isActive) MiniGameEngine.stopGame(this)
        currentTabIndex = index
        titleText.text = categories[index].substringAfter(" ")
        updateDrawerSelection(); contentFrame.removeAllViews()
        val view = when (index) {
            0 -> createBasicCalculator(); 1 -> createScientificCalculator(); 2 -> createVatCalculator(); 3 -> createLoanCalculator()
            4 -> createDistanceCalculator(); 5 -> createWeightCalculator(); 6 -> createTempCalculator(); 7 -> createProgrammerCalculator()
            8 -> createPercentageCalculator(); 9 -> createDiscountCalculator(); 10 -> createTipCalculator(); 11 -> createBMICalculator()
            12 -> createAgeCalculator(); 13 -> createCurrencyCalculator(); 14 -> createBmrCalculator(); 15 -> createQuadraticCalculator()
            16 -> createFractionCalculator(); 17 -> createCompoundInterestCalculator(); 18 -> createSpeedCalculator()
            19 -> {
                val settingsView = createSettingsPage()
                enhanceSettingsHierarchy(settingsView)
                val scrollObj = settingsView as? ScrollView
                currentSettingsScroll = scrollObj
                val container = scrollObj?.getChildAt(0) as? LinearLayout ?: (settingsView as? LinearLayout)

                val handLabel = if (isCleanGlassActive) {
                    "יד דומיננטית: ${if (isRightHanded) "ימניים (תפריט ופעולות בימין)" else "שמאליים (תפריט ופעולות בשמאל)"}"
                } else {
                    "יד דומיננטית: ${if (isRightHanded) "ימניים (תפריט ופעולות בימין) 🤚" else "שמאליים (תפריט ופעולות בשמאל) ✋"}"
                }
                val handBtn = createActionBtn(handLabel, false) {
                    isRightHanded = !isRightHanded
                    savePreferences()
                    updateHandednessLayout()
                    switchTab(19)
                }
                container?.addView(handBtn)
                scrollObj?.viewTreeObserver?.addOnScrollChangedListener {
                    if (currentTabIndex == 19) settingsScrollY = scrollObj.scrollY
                }
                if (settingsScrollY > 0) {
                    scrollObj?.post { scrollObj.scrollTo(0, settingsScrollY) }
                }
                settingsView
            }
            20 -> createFuelCostCalculator()
            21 -> createFuelEfficiencyCalculator()
            22 -> createGpaCalculator()
            23 -> createAdvancedTipCalculator()
            24 -> createSalesTaxCalculator()
            25 -> createUnitPriceCalculator()
            26 -> createWorldClockConverter()
            27 -> createOvulationCalculator()
            28 -> createHexConverter()
            29 -> createSavingsCalculator()
            30 -> createDateAnniversaryCalculator()
            else -> createBasicCalculator()
        }
        contentFrame.addView(view)
        if (useAnimations && !wasInSettings) {
            view.alpha = 0f; view.translationY = dp(24f).toFloat()
            view.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun createBasicCalculator(): View {
        val rows = if (isRightHanded) {
            listOf(
                listOf("MC", "MR", "M+", "M-"),
                listOf("AC", "⌫", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("(", "0", ".", "=")
            )
        } else {
            listOf(
                listOf("M-", "M+", "MR", "MC"),
                listOf("÷", "%", "⌫", "AC"),
                listOf("×", "7", "8", "9"),
                listOf("−", "4", "5", "6"),
                listOf("+", "1", "2", "3"),
                listOf("=", ".", "0", "(")
            )
        }
        return createCalculatorLayout(rows, isGrid5x7 = false)
    }

    private fun createScientificCalculator(): View {
        val rows = if (isRightHanded) {
            listOf(
                listOf("sin", "cos", "tan", "log", "ln"),
                listOf("√", "x²", "x³", "x^y", "!"),
                listOf("π", "e", "(", ")", "÷"),
                listOf("7", "8", "9", "%", "×"),
                listOf("4", "5", "6", "MC", "−"),
                listOf("1", "2", "3", "MR", "+"),
                listOf("AC", "0", ".", "⌫", "=")
            )
        } else {
            listOf(
                listOf("ln", "log", "tan", "cos", "sin"),
                listOf("!", "x^y", "x³", "x²", "√"),
                listOf("÷", "(", ")", "e", "π"),
                listOf("×", "%", "7", "8", "9"),
                listOf("−", "MC", "4", "5", "6"),
                listOf("+", "MR", "1", "2", "3"),
                listOf("=", "⌫", "0", ".", "AC")
            )
        }
        return createCalculatorLayout(rows, isGrid5x7 = true)
    }

    private fun createCalculatorLayout(rows: List<List<String>>, isGrid5x7: Boolean = false): View {
        keypadButtons.clear()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutDirection = View.LAYOUT_DIRECTION_LTR; setPadding(dp(14f), dp(0f), dp(14f), dp(14f)) }
        val displayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM or Gravity.END; setPadding(dp(22f), dp(14f), dp(22f), dp(16f))
            background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@MainActivity, if (isAuroraMode) 28f else IosGlassTheme.glassCornerPt) else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.07f)); cornerRadius = dp(28f).toFloat(); setStroke(dp(1.5f), Color.argb(70, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))) }
            if (isCleanGlassActive) IosGlassTheme.apply3DElevation(this, 10f)
        }

        if (isCleanGlassActive) {
            val pillBar = View(this).apply {
                background = GradientDrawable().apply {
                    cornerRadius = dp(4f).toFloat()
                    setColor(Color.argb(102, 255, 255, 255))
                }
                layoutParams = LinearLayout.LayoutParams(dp(38f), dp(4f)).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    bottomMargin = dp(4f)
                }
            }
            displayCard.addView(pillBar)
        }

        memoryIndicator = TextView(this).apply { text = if (memoryValue != 0.0) "M = ${Utils.formatResult(memoryValue, calcPrecision)}" else ""; setTextColor(if (isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY else primaryColor); textSize = 12f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.START }
        expressionView = TextView(this).apply {
            text = ""
            setTextColor(when {
                isAuroraMode -> AuroraBorealisTheme.EXPRESSION_CYAN_GREY
                isIosGlassMode -> Color.parseColor("#B8C7D9")
                else -> Color.parseColor("#8A94A8")
            })
            setAutoSizeTextTypeUniformWithConfiguration(12, 22, 2, TypedValue.COMPLEX_UNIT_SP)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        currentView = TextView(this).apply {
            text = if(currentInput.isEmpty()) "0" else currentInput
            setTextColor(Color.WHITE)
            typeface = if (isCleanGlassActive) Typeface.create("sans-serif-medium", Typeface.BOLD) else Typeface.create("sans-serif-light", Typeface.NORMAL)
            setAutoSizeTextTypeUniformWithConfiguration(18, 56, 2, TypedValue.COMPLEX_UNIT_SP)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f)
            if (isAuroraMode) {
                setShadowLayer(12f, 0f, 0f, Color.argb(115, 0, 245, 212))
            } else if (isIosGlassMode) {
                setShadowLayer(8f, 0f, 2f, Color.argb(90, 0, 0, 0))
            }
        }

        displayCard.addView(memoryIndicator); displayCard.addView(expressionView); displayCard.addView(currentView)
        root.addView(displayCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, if(isGrid5x7) 0.28f else 0.32f).apply { bottomMargin = dp(12f) })

        var startX = 0f; var startY = 0f
        displayCard.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { startX = event.x; startY = event.y; true }
                MotionEvent.ACTION_UP -> {
                    val dx = event.x - startX; val dy = event.y - startY
                    if (Math.abs(dx) > 50f && Math.abs(dx) > Math.abs(dy)) {
                        onEquationButtonClick("⌫")
                    } else if (dy > 50f && Math.abs(dy) > Math.abs(dx)) {
                        if (isCleanGlassActive && startY < v.height * 0.42f && historyList.isNotEmpty()) {
                            showHistoryDialog()
                        } else {
                            val textToRead = if(currentInput.isEmpty() || currentInput == "0") "אפס" else currentInput
                            tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null)
                            Toast.makeText(this@MainActivity, if (isCleanGlassActive) "מקריא תוצאה" else "מקריא... 🔊", Toast.LENGTH_SHORT).show()
                        }
                    } else if (dy < -50f && Math.abs(dy) > Math.abs(dx)) {
                        feedback(v); startVoiceRecognition()
                    } else if (Math.abs(dx) < 10f && Math.abs(dy) < 10f) {
                        copyToClipboard(currentInput)
                    }
                    true
                }; else -> false
            }
        }
        val keypad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for (rowSymbols in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
            }
            for (symbol in rowSymbols) {
                val weight = if (!isGrid5x7 && symbol == "0") 2f else 1f
                val btn = createAnimatedButton(symbol, isGrid5x7)
                keypadButtons[symbol] = btn
                val marginDp = if (isCleanGlassActive) 4.5f else 3f
                rowLayout.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply { setMargins(dp(marginDp), dp(marginDp), dp(marginDp), dp(marginDp)) })
            }
            keypad.addView(rowLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        root.addView(keypad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, if(isGrid5x7) 0.72f else 0.68f)); return root
    }

    private fun createAnimatedButton(symbol: String, isSmall: Boolean): TextView {
        val isMem = symbol in listOf("MC", "MR", "M+", "M-")
        val isOp = symbol in listOf("÷", "×", "−", "+", "sin", "cos", "tan", "log", "ln", "√", "x²", "x³", "x^y", "!", "π", "e", "(", ")")
        val defaultBg = when { symbol == "AC" -> Utils.lightenColor(Color.RED, 0.2f); symbol == "=" -> primaryColor; isMem -> Utils.lightenColor(bgColor, 0.03f); symbol in listOf("⌫", "%", "÷", "×", "−", "+") -> Utils.lightenColor(bgColor, 0.1f); else -> Utils.lightenColor(bgColor, 0.06f) }
        val strokeColor = if (symbol == "=" || symbol == "AC") Color.TRANSPARENT else Utils.lightenColor(bgColor, 0.15f)
        val textColor = if (isCleanGlassActive) Color.WHITE else when { symbol == "AC" -> Color.WHITE; symbol == "=" -> Color.parseColor("#08090C"); isMem || symbol in listOf("⌫", "%", "÷", "×", "−", "+") -> primaryColor; else -> Color.WHITE }
        val shape = GradientDrawable().apply { this.shape = GradientDrawable.RECTANGLE; cornerRadius = dp(buttonCornerStyle).toFloat(); setColor(defaultBg); setStroke(dp(1.5f), strokeColor) }
        val btnDrawable = if (isCleanGlassActive) IosGlassTheme.createGlassButtonDrawable(this, symbol, isMem, false) else RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null)

        return TextView(this).apply {
            text = symbol
            setTextColor(textColor)
            minimumWidth = dp(44f)
            minimumHeight = dp(44f)
            setPadding(dp(6f), dp(4f), dp(6f), dp(4f))
            setAutoSizeTextTypeUniformWithConfiguration(10, if(isSmall) 21 else 27, 2, TypedValue.COMPLEX_UNIT_SP)
            typeface = if (isCleanGlassActive) Typeface.create("sans-serif-medium", Typeface.BOLD) else if (symbol == "=" || isOp || isMem) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            background = btnDrawable
            isClickable = true

            if (isCleanGlassActive) {
                IosGlassTheme.apply3DElevation(this, 6f)
                IosGlassTheme.styleSymbolTextView(this, this@MainActivity, symbol, false)
            }

            setOnTouchListener { v, e ->
                val tv = v as TextView
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        feedback(v, symbol)
                        if (isCleanGlassActive) {
                            tv.background = IosGlassTheme.createGlassButtonDrawable(this@MainActivity, symbol, isMem, true)
                            IosGlassTheme.styleSymbolTextView(tv, this@MainActivity, symbol, true)
                            IosGlassTheme.apply3DElevation(tv, 2f)
                        }
                        if (useAnimations) v.animate().scaleX(if (isCleanGlassActive) 0.91f else 0.84f).scaleY(if (isCleanGlassActive) 0.91f else 0.84f).translationY(if (isCleanGlassActive) dp(2f).toFloat() else 0f).setDuration(65).setInterpolator(DecelerateInterpolator()).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isCleanGlassActive) {
                            tv.background = IosGlassTheme.createGlassButtonDrawable(this@MainActivity, symbol, isMem, false)
                            IosGlassTheme.styleSymbolTextView(tv, this@MainActivity, symbol, false)
                            IosGlassTheme.apply3DElevation(tv, 6f)
                        }
                        if (useAnimations) v.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(360).setInterpolator(OvershootInterpolator(2.2f)).start()
                        if (e.actionMasked == MotionEvent.ACTION_UP) onEquationButtonClick(symbol)
                    }
                }
                false
            }
        }
    }

    fun onEquationButtonClick(s: String) {
        if (MiniGameEngine.isActive && MiniGameEngine.onKeyPress(this, s)) return
        if (justCalculated && s !in listOf("÷", "×", "−", "+", "^", "%")) { currentInput = ""; justCalculated = false }
        justCalculated = false
        when (s) {
            "MC" -> { memoryValue = 0.0; savePreferences() }; "MR" -> { currentInput += Utils.formatResult(memoryValue, calcPrecision) }
            "M+" -> { try { memoryValue += Utils.evalMathExpression(if (currentInput.isEmpty()) "0" else currentInput); savePreferences(); justCalculated = true } catch (e: Exception) {} }
            "M-" -> { try { memoryValue -= Utils.evalMathExpression(if (currentInput.isEmpty()) "0" else currentInput); savePreferences(); justCalculated = true } catch (e: Exception) {} }
            "AC" -> { currentInput = ""; expressionView?.text = "" }; "⌫" -> { if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1) }
            "sin", "cos", "tan", "log", "ln" -> { currentInput += "$s(" }; "√" -> { currentInput += "√(" }; "x²" -> { currentInput += "^2" }; "x³" -> { currentInput += "^3" }; "x^y" -> { currentInput += "^" }
            "=" -> {
                if (checkEasterEggs(currentInput)) return
                if (currentInput.isNotEmpty()) {
                    try {
                        val res = Utils.evalMathExpression(currentInput)
                        val formatted = Utils.formatResult(res, calcPrecision)
                        expressionView?.text = "$currentInput ="
                        historyList.add(0, "$currentInput = $formatted")
                        savePreferences(); currentInput = formatted; justCalculated = true
                    } catch (e: Exception) { currentInput = "Error"; justCalculated = true }
                }
            }
            else -> currentInput += s
        }
        memoryIndicator?.text = if (memoryValue != 0.0) "M = ${Utils.formatResult(memoryValue, calcPrecision)}" else ""
        val newText = if (currentInput.isEmpty()) "0" else currentInput
        if (currentView?.text != newText) {
            currentView?.text = newText
            if (useAnimations) {
                currentView?.alpha = 0.35f; currentView?.scaleX = 0.92f; currentView?.scaleY = 0.92f
                currentView?.animate()?.alpha(1f)?.scaleX(1f)?.scaleY(1f)?.setDuration(220)?.setInterpolator(OvershootInterpolator(1.2f))?.start()
            }
        }
    }

    fun createInputBox(hintText: String): EditText { return EditText(this).apply { hint = hintText; setHintTextColor(if (isCleanGlassActive) Color.parseColor("#B0BEC5") else Color.parseColor("#5C657A")); setTextColor(Color.WHITE); inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED; textSize = 17f; setPadding(dp(18f), dp(18f), dp(18f), dp(18f)); background = if (isCleanGlassActive) IosGlassTheme.createGlassCardDrawable(this@MainActivity, 16f) else GradientDrawable().apply { setColor(Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(16f).toFloat(); setStroke(dp(1.5f), Utils.lightenColor(bgColor, 0.15f)) }; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) } } }
    fun createActionBtn(textStr: String, isPrimary: Boolean = true, onClick: (View) -> Unit): TextView { val cleanText = IosGlassTheme.cleanCategoryTitle(textStr, isCleanGlassActive); val shape = GradientDrawable().apply { setColor(if (isPrimary) primaryColor else Utils.lightenColor(bgColor, 0.06f)); cornerRadius = dp(buttonCornerStyle).toFloat(); if (!isPrimary) setStroke(dp(1.5f), Utils.lightenColor(bgColor, 0.15f)) }; val bgDraw = if (isCleanGlassActive) IosGlassTheme.createGlassButtonDrawable(this, if (isPrimary) "=" else "0", false) else RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null); return TextView(this).apply { text = cleanText; gravity = Gravity.CENTER; setTextColor(if (isPrimary && isAuroraMode) AuroraBorealisTheme.EMERALD_NIGHT else if (isPrimary && !isCleanGlassActive) Color.parseColor("#08090C") else Color.WHITE); textSize = 15f; typeface = Typeface.DEFAULT_BOLD; background = bgDraw; setPadding(dp(12f), dp(16f), dp(12f), dp(16f)); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(10f) }; isClickable = true; if (isCleanGlassActive) IosGlassTheme.apply3DElevation(this, 5f); setOnClickListener { feedback(it); onClick(it) } } }
    fun createResultTextView(): TextView = TextView(this).apply { setTextColor(Color.WHITE); textSize = 20f; gravity = Gravity.CENTER; setPadding(0, dp(20f), 0, 0); setOnClickListener { if (text.isNotEmpty()) copyToClipboard(text.toString()) } }

    override fun onDestroy() { tts?.shutdown(); soundPool.release(); super.onDestroy() }
    fun dp(value: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()

    fun shakeScreen() {
        val animator = android.animation.ObjectAnimator.ofFloat(mainContent, "translationX", 0f, 35f, -35f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 600; animator.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleVoiceActivityResult(requestCode, resultCode, data)
    }
}
 

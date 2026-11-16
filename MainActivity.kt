package com.example.calculator

import android.app.Activity
import android.app.Dialog
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
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.graphics.drawable.RippleDrawable
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Rational
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
    private lateinit var favHeaderBtn: TextView
    private var appLogoView: TextView? = null
    private val drawerItemsViews = mutableListOf<View>()
    private val drawerItemRowsMap = mutableMapOf<Int, Pair<View, View>>()
    private val drawerSeparators = mutableListOf<View>()
    private val drawerSectionHeaders = mutableListOf<Triple<Int, TextView, LinearLayout>>()
    private val sectionDividersMap = mutableMapOf<Int, View>()
    private val proSectionDividers = mutableMapOf<Int, View>()
    private val expandedSections = mutableSetOf<Int>()
    val keypadButtons = mutableMapOf<String, TextView>()
    var tts: TextToSpeech? = null

    private lateinit var soundPool: SoundPool
    var soundIds = IntArray(13)
    var acSoundId = 0

    var useHaptic = true; var useSound = true; var useAnimations = true; var keepScreenOn = false; var soundType = 0
    var useCustomAcSound = true
    var isRightHanded = true
    var isIosGlassMode = false
    var isAuroraMode = false
    var isMinimalistMode = false
    val isCleanGlassActive: Boolean get() = isIosGlassActiveOrAurora

    private val isIosGlassActiveOrAurora: Boolean get() = isIosGlassMode || isAuroraMode || isMinimalistMode

    private val hiddenDrawerTabs = mutableSetOf<Int>()
    private var isDrawerChecklistExpanded = false

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
        "🧮 מחשבון רגיל",                        // 0
        "🔬 מחשבון מדעי",                        // 1
        "🧾 מחשבון מע״מ",                        // 2
        "🏦 מחשבון הלוואה",                      // 3
        "📏 המרת מרחקים",                        // 4
        "⚖️ המרת משקלים",                        // 5
        "🌡️ המרת טמפ׳",                         // 6
        "💻 מחשבון מתכנתים",                     // 7
        "📊 חישוב אחוזים",                       // 8
        "🏷️ חישוב הנחות",                        // 9
        "💰 חלוקת טיפים",                        // 10
        "🏃 מדד BMI",                           // 11
        "🎂 חישוב גיל",                          // 12
        "💵 מטבעות לייב",                        // 13
        "🔥 מחשבון BMR",                         // 14
        "➗ משוואות ריבועיות",                   // 15
        "⅟ מחשבון שברים",                       // 16
        "📈 ריבית דריבית",                       // 17
        "🚀 המרת מהירות",                        // 18
        "⚙️ הגדרות",                            // 19
        "⛽ מחשבון עלות דלק",                    // 20
        "🚘 יעילות שריפת דלק",                   // 21
        "🎓 ממוצע ציונים",                       // 22
        "🍽️ מחשבון תשר מתקדם",                   // 23
        "🛍️ מחשבון מס מכירות",                   // 24
        "🏷️ מחשבון מחיר יחידתי",                 // 25
        "🌍 ממיר שעון עולמי",                    // 26
        "🌸 מחשבון ביוץ",                        // 27
        "🔢 ממיר הקסדצימלי",                     // 28
        "💎 מחשבון חסכונות",                     // 29
        "📅 מחשבון תאריך",                       // 30
        // --- מחשבונים וכלים אישיים של מצב מקצועי (Pro Mode) ---
        "📐 כל 87 קטגוריות היחידות והמרה מרובה",   // 31
        "🚰 אינסטלציה, צנרת, ספיקה ולחץ",         // 32
        "🩺 מינון תרופות, עירוי IV וסוכר",        // 33
        "🏗️ בנייה, בטון, ריצוף ומיזוג BTU",       // 34
        "🎨 קודי צבע של נגדים (4/5 פסים)",        // 35
        "🔋 מפענח קודי קבלים, היגב ואנרגיה",      // 36
        "🔌 עוביי חוטים AWG/SWG ומפל מתח",        // 37
        "⚡ מעבדת חוק אוהם, מחלק מתח וסוללה",     // 38
        "🟢 פלס בנאים דו-צירי עם בועות וכיול",    // 39
        "🧭 מצפן 3D מכויל, מד אמינות ושדה µT",    // 40
        "📏 מד-זווית דו-זרועי וסרגל מכויל פיזית", // 41
        "⏱️ שעון עצר כרונוגרף דו-טבעתי והקפות",   // 42
        "🔦 פנס LED פיזי, מסך מלא ואיתות SOS",    // 43
        "⚛️ הטבלה המחזורית, אטום ומסה מולרית",    // 44
        "📡 מתרגם ומשדר קוד מורס דו-כיווני",      // 45
        "🔐 מחולל סיסמאות ומד אנטרופיה",          // 46
        "🔠 גימטריה, אתב״ש, רומית וטקסט",         // 47
        "🥇 מתכות יקרות (24K-14K), מטבעות וגרפים",// 48
        "🏠 משכנתא, שפיצר מול קרן שווה וסילוקין", // 49
        "📈 תשואת ROI, צמיחה CAGR ואינפלציה",     // 50
        "💼 מרווח רווח, גרף נקודת איזון ותקציב",  // 51
        "📊 סטטיסטיקה, Z-Score ועקומת פעמון",     // 52
        "📐 גאומטריה 2D/3D, פיתגורס ושרטוט",      // 53
        "🔢 תורת המספרים, GCD/LCM וקומבינטוריקה", // 54
        "☁️ ייצוא PDF/CSV, גיבוי Drive והיסטוריה",// 55
        "⭐ מרכז המועדפים וחישובים נעוצים",        // 56 (נגיש בלחיצה ארוכה על הכוכב בסרגל העליון)
        "⚙️ ממירי מכניקה, כוח, מומנט ותאוצה",     // 57
        "💧 ממירי זורמים, צמיגות, ספיקה וצנרת",   // 58
        "🔥 ממירי תרמודינמיקה, חום ומיזוג",       // 59
        "⚡ ממירי חשמל, מגנטיות, שדה והשראות",    // 60
        "☀️ ממירי אופטיקה, אקוסטיקה וקרינה",      // 61
        "🧪 ממירי רפואה, כימיה ומעבדה קלינית",    // 62
        "🌌 ממירי תקשורת, טיפוגרפיה, חלל ובישול", // 63
        "💳 מחשבון שכר, שעות נוספות, פנסיה ונטו", // 64
        "🧮 מטריצות, דטרמיננטה ומערכת משוואות"    // 65
    )

    val drawerSections = listOf(
        Triple(301, "📐 מתמטיקה ומדע", listOf(0, 1, 8, 15, 16, 7, 28, 22)),
        Triple(302, "💳 פיננסים וצרכנות", listOf(2, 24, 3, 17, 29, 9, 25, 10, 23, 13)),
        Triple(303, "🔄 המרות, רכב ודלק", listOf(4, 5, 6, 18, 20, 21)),
        Triple(304, "❤️ בריאות, זמן ותאריכים", listOf(11, 14, 27, 12, 30, 26)),
        Triple(305, "📐 Pro: 87 קטגוריות יחידות לפי תחום", listOf(31, 57, 58, 59, 60, 61, 62, 63)),
        Triple(306, "🔧 Pro: הנדסת אינסטלציה, רפואה ובנייה", listOf(32, 33, 34)),
        Triple(307, "⚡ Pro: מעבדת אלקטרוניקה וחשמל", listOf(35, 36, 37, 38)),
        Triple(308, "🧭 Pro: חיישנים, פלס, מצפן ומדידה", listOf(39, 40, 41, 42, 43)),
        Triple(309, "🔬 Pro: מדע, אטום, מורס, הצפנה וטקסט", listOf(44, 45, 46, 47)),
        Triple(310, "📊 Pro: השקעות, משכנתא, שכר, זהב ועסקים", listOf(48, 49, 50, 51, 64)),
        Triple(311, "🧮 Pro: סטטיסטיקה, גאומטריה, מטריצות וענן", listOf(52, 53, 54, 65, 55))
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

        topHeader = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16f), dp(18f), dp(16f), dp(14f)) }
        menuBtn = TextView(this).apply { text = "☰"; setTextColor(Color.WHITE); textSize = 26f; setPadding(dp(8f), 0, dp(8f), 0); setOnClickListener { feedback(it); if (isDrawerOpen) closeDrawer() else openDrawer() } }
        titleText = TextView(this).apply { text = "מחשבון פרימיום"; setTextColor(Color.WHITE); textSize = 20f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(8f), 0, dp(8f), 0) } }

        favHeaderBtn = TextView(this).apply {
            text = "☆"
            textSize = 21f
            gravity = Gravity.CENTER
            setPadding(dp(6f), dp(6f), dp(6f), dp(6f))
            setOnClickListener {
                feedback(it)
                if (currentTabIndex == ProFavoritesEngine.FAVORITES_CENTER_TAB_INDEX) {
                    return@setOnClickListener
                }
                ProFavoritesEngine.toggleFavoriteTab(this@MainActivity, currentTabIndex)
                refreshFavHeaderIcon()
                if (currentTabIndex >= 31) switchTab(currentTabIndex)
            }
            setOnLongClickListener {
                feedback(it)
                if (isDrawerOpen) closeDrawer()
                switchTab(ProFavoritesEngine.FAVORITES_CENTER_TAB_INDEX)
                true
            }
        }

        pipBtn = TextView(this).apply { text = "🧮"; textSize = 21f; gravity = Gravity.CENTER; setPadding(dp(6f), dp(6f), dp(8f), dp(6f)); setOnClickListener { feedback(it)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(3, 4)).build()) } else { Toast.makeText(this@MainActivity, "חלון צף נתמך רק באנדרואיד 8 ומעלה", Toast.LENGTH_SHORT).show() }
        } }
        historyBtn = TextView(this).apply { text = "🕒"; textSize = 20f; gravity = Gravity.CENTER; setPadding(dp(6f), dp(6f), dp(4f), dp(6f)); setOnClickListener { feedback(it); showHistoryDialog() } }

        contentFrame = FrameLayout(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f) }
        mainContent?.addView(topHeader); mainContent?.addView(contentFrame)

        dimView = View(this).apply { setBackgroundColor(Color.parseColor("#88000000")); visibility = View.GONE; alpha = 0f; layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT); setOnClickListener { if (isDrawerOpen) closeDrawer() } }

        drawerScroll = ScrollView(this).apply {
            isClickable = true
            isFillViewport = true
        }
        drawerMenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(24f), 0, dp(24f))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val appLogo = TextView(this).apply {
            text = "אולר שווייצרי\nPro fx-991"
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(14f), dp(6f), dp(14f), dp(14f))
            }
        }
        appLogoView = appLogo
        drawerMenu?.addView(appLogo)

        drawerItemsViews.clear()
        drawerItemRowsMap.clear()
        for ((index, title) in categories.withIndex()) {
            val isSettings = (index == 19)
            val item = TextView(this).apply {
                text = IosGlassTheme.cleanCategoryTitle(title, true)
                textSize = if (isSettings) 16.5f else 15f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                compoundDrawablePadding = dp(12f)
                setPadding(dp(if (isSettings) 20f else 24f), dp(13f), dp(if (isSettings) 20f else 24f), dp(13f))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(12f), dp(2f), dp(12f), dp(2f))
                }
                setOnClickListener { feedback(it); settingsScrollY = 0; switchTab(index); closeDrawer() }
            }
            drawerItemsViews.add(item)
        }

        drawerSectionHeaders.clear()
        drawerSeparators.clear()
        sectionDividersMap.clear()
        proSectionDividers.clear()
        for ((secIconCode, secTitle, itemIndices) in drawerSections) {
            val sectionItemsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
            }

            val headerTv = TextView(this).apply {
                tag = secTitle
                textSize = 15.5f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                compoundDrawablePadding = dp(12f)
                setPadding(dp(20f), dp(14f), dp(20f), dp(14f))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(dp(12f), dp(3f), dp(12f), dp(3f))
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
                drawerItemRowsMap[idx] = drawerItemsViews[idx] to sep
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
            sectionDividersMap[secIconCode] = catSep
            if (secIconCode in 305..311) proSectionDividers[secIconCode] = catSep
            drawerSeparators.add(catSep)
            drawerMenu?.addView(catSep)
            drawerMenu?.addView(sectionItemsContainer)
        }

        expandedSections.add(301)

        val bottomSpacer = Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        drawerMenu?.addView(bottomSpacer)

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
        CompassHomeWidgetHelper.renderCurrentStateToWidgets(this)
    }

    override fun onResume() {
        super.onResume()
        CompassHomeWidgetHelper.renderCurrentStateToWidgets(this)
    }

    fun updateLiveDrawerAndHeaderColors() {
        val hasCustomLogoBg = MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_BG]
        val hasCustomLogoText = MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_TEXT]
        val hasCustomTextIcons = MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_TEXT_ICONS]
        val topIconCol = MinimalistTheme.resolveTopHeaderIconColor(this)

        if (isMinimalistMode) {
            val minBg = MinimalistTheme.getBackgroundColor(this)
            mainContent?.setBackgroundColor(minBg)
            drawerScroll.setBackgroundColor(MinimalistTheme.DRAWER_BG)
            val inkCol = MinimalistTheme.ensureReadableTextOnBg(MinimalistTheme.TEXT_CHARCOAL, minBg, 2.2)
            titleText.setTextColor(inkCol)
            menuBtn.setTextColor(topIconCol)

            val logoBgCol = MinimalistTheme.DRAWER_HEADER_YELLOW
            val logoTxtCol = MinimalistTheme.ensureReadableTextOnBg(MinimalistTheme.LOGO_TEXT_COLOR, logoBgCol, 2.2)
            appLogoView?.apply {
                setTextColor(logoTxtCol)
                background = GradientDrawable().apply {
                    cornerRadius = dp(24f).toFloat()
                    setColor(logoBgCol)
                }
                val calcIcon = MinimalistTheme.createMinimalistVectorIcon(this@MainActivity, 0, logoTxtCol, 28f)
                setCompoundDrawables(calcIcon, null, null, null)
                compoundDrawablePadding = dp(10f)
            }

            pipBtn.text = ""
            pipBtn.setCompoundDrawables(MinimalistTheme.createMinimalistVectorIcon(this, 100, topIconCol, 22f), null, null, null)
            historyBtn.text = ""
            historyBtn.setCompoundDrawables(MinimalistTheme.createMinimalistVectorIcon(this, 101, topIconCol, 22f), null, null, null)
        } else {
            val headerInk = if (hasCustomTextIcons) MinimalistTheme.ensureReadableTextOnBg(MinimalistTheme.TEXT_CHARCOAL, bgColor, 2.2) else Color.WHITE
            titleText.setTextColor(headerInk)
            menuBtn.setTextColor(topIconCol)
            drawerScroll.setBackgroundColor(MinimalistTheme.resolveDrawerBgColor(this))

            appLogoView?.apply {
                val logoBgCol = if (hasCustomLogoBg) MinimalistTheme.DRAWER_HEADER_YELLOW else MinimalistTheme.resolveDrawerBgColor(this@MainActivity)
                val rawLogoTxt = if (hasCustomLogoText) MinimalistTheme.LOGO_TEXT_COLOR else Color.WHITE
                val safeLogoTxt = MinimalistTheme.ensureReadableTextOnBg(rawLogoTxt, logoBgCol, 2.2)
                setTextColor(safeLogoTxt)
                if (hasCustomLogoBg) {
                    background = GradientDrawable().apply {
                        cornerRadius = dp(24f).toFloat()
                        setColor(logoBgCol)
                    }
                } else {
                    background = null
                }
            }

            if (isAuroraMode) {
                pipBtn.text = ""
                pipBtn.setCompoundDrawables(AuroraBorealisTheme.createVectorIcon(this, 100, topIconCol, 22f, true), null, null, null)
                historyBtn.text = ""
                historyBtn.setCompoundDrawables(AuroraBorealisTheme.createVectorIcon(this, 101, topIconCol, 22f, true), null, null, null)
            } else if (isIosGlassMode) {
                pipBtn.text = ""
                pipBtn.setCompoundDrawables(IosGlassTheme.createSfSymbolDrawable(this, 100, topIconCol, 22f), null, null, null)
                historyBtn.text = ""
                historyBtn.setCompoundDrawables(IosGlassTheme.createSfSymbolDrawable(this, 101, topIconCol, 22f), null, null, null)
            } else {
                pipBtn.setTextColor(topIconCol)
                historyBtn.setTextColor(topIconCol)
            }
        }
        refreshFavHeaderIcon()
        updateDrawerSelection()
    }

    fun showHistoryDialog() {
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = isMinimalistMode
        val cardBg = when {
            isMin -> GradientDrawable().apply {
                cornerRadius = dp(26f).toFloat()
                setColor(MinimalistTheme.BG_CREAM)
                setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, Color.parseColor("#E0DBD3")))
            }
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this, 26f)
            else -> GradientDrawable().apply {
                cornerRadius = dp(26f).toFloat()
                setColor(Utils.lightenColor(bgColor, 0.08f))
                setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, primaryColor))
            }
        }

        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY
        val accentCol = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> primaryColor
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(20f), dp(20f), dp(18f))
            background = cardBg
        }

        val titleTv = TextView(this).apply {
            text = if (isMin) "היסטוריית חישובים (${historyList.size})" else "🕒 היסטוריית חישובים (${historyList.size})"
            setTextColor(textPrimary)
            textSize = 19.5f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            setPadding(dp(4f), 0, dp(4f), dp(12f))
            if (isMin) {
                val clk = MinimalistTheme.createMinimalistVectorIcon(this@MainActivity, 101, MinimalistTheme.TEXT_CHARCOAL, 20f)
                setCompoundDrawables(null, null, clk, null)
                compoundDrawablePadding = dp(10f)
            }
        }
        root.addView(titleTv)

        val searchBox = createInputBox("חפש תרגיל או תוצאה בהיסטוריה...").apply {
            inputType = InputType.TYPE_CLASS_TEXT
            visibility = if (historyList.size > 3) View.VISIBLE else View.GONE
        }
        root.addView(searchBox)

        val listScroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.46f).toInt()
            )
        }
        val itemsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        listScroll.addView(itemsContainer)
        root.addView(listScroll)

        fun renderHistoryItems(filterQuery: String) {
            itemsContainer.removeAllViews()
            val filtered = if (filterQuery.isBlank()) historyList else historyList.filter { it.contains(filterQuery, ignoreCase = true) }

            if (filtered.isEmpty()) {
                itemsContainer.addView(TextView(this).apply {
                    text = if (historyList.isEmpty()) "אין חישובים שמורים בהיסטוריה עדיין.\nכל חישוב שתבצע יישמר כאן אוטומטית!"
                    else "לא נמצאו חישובים התואמים לחיפוש."
                    setTextColor(textSecondary)
                    textSize = 15.5f
                    gravity = Gravity.CENTER
                    setPadding(dp(12f), dp(36f), dp(12f), dp(36f))
                })
                return
            }

            for (entry in filtered) {
                val exprPart = entry.substringBefore("=").trim()
                val resPart = entry.substringAfter("=", entry).trim()

                val itemCard = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14f), dp(12f), dp(14f), dp(12f))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(18f).toFloat()
                        if (isMin) {
                            setColor(MinimalistTheme.CARD_WHITE)
                            setStroke(dp(1f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, Color.parseColor("#EAE5DE")))
                        } else {
                            setColor(Color.argb(40, 255, 255, 255))
                            setStroke(dp(1f), Color.argb(35, 255, 255, 255))
                        }
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(8f) }
                }

                val delItemBtn = TextView(this).apply {
                    text = "✕"
                    setTextColor(if (isMin) MinimalistTheme.ALERT_RED_TEXT else Color.parseColor("#FF5252"))
                    textSize = 15f
                    setPadding(dp(8f), dp(4f), dp(8f), dp(4f))
                    setOnClickListener {
                        feedback(it)
                        historyList.remove(entry)
                        savePreferences()
                        titleTv.text = if (isMin) "היסטוריית חישובים (${historyList.size})" else "🕒 היסטוריית חישובים (${historyList.size})"
                        renderHistoryItems(searchBox.text.toString())
                    }
                }

                val calcTextCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.RIGHT
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        feedback(it)
                        currentInput = resPart
                        currentView?.text = resPart
                        expressionView?.text = "\u200E$exprPart ="
                        justCalculated = true
                        copyToClipboard(resPart)
                        dlg.dismiss()
                    }
                }

                val exprTv = TextView(this).apply {
                    text = "\u200E$exprPart ="
                    setTextColor(textSecondary)
                    textSize = 14f
                    gravity = Gravity.RIGHT
                }
                val resTv = TextView(this).apply {
                    text = "\u200E$resPart"
                    setTextColor(textPrimary)
                    textSize = 18.5f
                    typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
                    gravity = Gravity.RIGHT
                }

                calcTextCol.addView(exprTv)
                calcTextCol.addView(resTv)
                itemCard.addView(delItemBtn)
                itemCard.addView(calcTextCol)
                itemsContainer.addView(itemCard)
            }
        }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderHistoryItems(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        renderHistoryItems("")

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(12f) }
        }

        val clearBtnBg = if (isMin) MinimalistTheme.ACCENT_TERRACOTTA else Color.parseColor("#D32F2F")
        val clearAllBtn = TextView(this).apply {
            text = "נקה היסטוריה"
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE, clearBtnBg, 2.3))
            textSize = 14.5f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = dp(999f).toFloat()
                setColor(clearBtnBg)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6f)
            }
            setOnClickListener {
                feedback(it)
                historyList.clear()
                savePreferences()
                dlg.dismiss()
                Toast.makeText(this@MainActivity, "ההיסטוריה נוקתה", Toast.LENGTH_SHORT).show()
            }
        }

        val closeBtn = TextView(this).apply {
            text = "סגור"
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.parseColor("#08090C"), accentCol, 2.3))
            textSize = 14.5f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = dp(999f).toFloat()
                setColor(accentCol)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6f)
            }
            setOnClickListener {
                feedback(it)
                dlg.dismiss()
            }
        }

        bottomRow.addView(clearAllBtn)
        bottomRow.addView(closeBtn)
        root.addView(bottomRow)

        dlg.setContentView(root)
        dlg.show()
        val widthPx = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    fun showItemPicker(title: String, items: List<String>, onSelected: (String) -> Unit) {
        val dlg = Dialog(this)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = isMinimalistMode
        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val cardBg = when {
            isMin -> GradientDrawable().apply {
                cornerRadius = dp(26f).toFloat()
                setColor(MinimalistTheme.BG_CREAM)
                setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, Color.parseColor("#E0DBD3")))
            }
            isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this, 26f)
            else -> GradientDrawable().apply {
                cornerRadius = dp(26f).toFloat()
                setColor(Utils.lightenColor(bgColor, 0.08f))
                setStroke(dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, primaryColor))
            }
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(20f), dp(20f), dp(18f))
            background = cardBg
        }

        root.addView(TextView(this).apply {
            text = title
            setTextColor(textPrimary)
            textSize = 19f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(dp(4f), 0, dp(4f), dp(12f))
        })

        val searchBox = createInputBox("חפש ברשימה...").apply {
            inputType = InputType.TYPE_CLASS_TEXT
            visibility = if (items.size > 6) View.VISIBLE else View.GONE
        }
        root.addView(searchBox)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.48f).toInt().coerceAtMost(dp(380f))
            )
        }
        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listContainer)
        root.addView(scroll)

        fun populateList(q: String) {
            listContainer.removeAllViews()
            val filtered = if (q.isBlank()) items else items.filter { it.contains(q, ignoreCase = true) }
            for (item in filtered) {
                val rowBgCol = if (isMin) MinimalistTheme.CARD_WHITE else Color.argb(32, 255, 255, 255)
                val row = TextView(this).apply {
                    text = item
                    setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, rowBgCol, 2.3))
                    textSize = 16f
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                    setPadding(dp(16f), dp(13f), dp(16f), dp(13f))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(16f).toFloat()
                        setColor(rowBgCol)
                        if (isMin) setStroke(dp(1f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, Color.parseColor("#EAE5DE")))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(6f) }
                    setOnClickListener {
                        feedback(it)
                        dlg.dismiss()
                        onSelected(item)
                    }
                }
                listContainer.addView(row)
            }
        }

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { populateList(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        populateList("")

        dlg.setContentView(root)
        dlg.show()
        val widthPx = (resources.displayMetrics.widthPixels * 0.90f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    fun isCalculatorVisibleInDrawer(tabIndex: Int): Boolean {
        if (tabIndex == 19) return true
        return !hiddenDrawerTabs.contains(tabIndex)
    }

    fun setCalculatorVisibleInDrawer(tabIndex: Int, visible: Boolean) {
        if (tabIndex == 19) return
        if (visible) hiddenDrawerTabs.remove(tabIndex) else hiddenDrawerTabs.add(tabIndex)
        savePreferences()
        updateDrawerSelection()
    }

    private fun refreshFavHeaderIcon() {
        if (currentTabIndex == ProFavoritesEngine.FAVORITES_CENTER_TAB_INDEX) {
            favHeaderBtn.visibility = View.GONE
            return
        }
        favHeaderBtn.visibility = View.VISIBLE
        val isFav = ProFavoritesEngine.isFavoriteTab(prefs, currentTabIndex)
        val topIconCol = MinimalistTheme.resolveTopHeaderIconColor(this)
        if (isMinimalistMode) {
            favHeaderBtn.text = ""
            val iconCode = if (isFav) 102 else 103
            favHeaderBtn.setCompoundDrawables(MinimalistTheme.createMinimalistVectorIcon(this, iconCode, topIconCol, 22f), null, null, null)
            return
        }
        val starCol = if (isFav) Color.parseColor("#FFD700") else topIconCol
        if (isCleanGlassActive) {
            favHeaderBtn.text = ""
            val iconCode = if (isFav) 102 else 103
            favHeaderBtn.setCompoundDrawables(AuroraBorealisTheme.createVectorIcon(this, iconCode, starCol, 22f, isAuroraMode), null, null, null)
        } else {
            favHeaderBtn.setCompoundDrawables(null, null, null, null)
            favHeaderBtn.text = if (isFav) "⭐" else "☆"
            favHeaderBtn.setTextColor(starCol)
        }
    }

    private fun getDrawerWidthPx(): Int {
        val screenW = resources.displayMetrics.widthPixels
        if (screenW <= 0) return dp(295f)
        return when {
            isMinimalistMode -> (screenW * 0.72f).toInt()
            isCleanGlassActive -> (screenW * 0.78f).toInt()
            else -> dp(295f)
        }
    }

    fun updateHandednessLayout() {
        topHeader.layoutDirection = View.LAYOUT_DIRECTION_LTR
        topHeader.removeAllViews()
        if (isRightHanded) {
            topHeader.addView(historyBtn)
            topHeader.addView(pipBtn)
            topHeader.addView(favHeaderBtn)
            topHeader.addView(titleText)
            topHeader.addView(menuBtn)
            titleText.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        } else {
            topHeader.addView(menuBtn)
            topHeader.addView(titleText)
            topHeader.addView(favHeaderBtn)
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
    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            "ACTION_CALC" -> { currentTabIndex = 0; applyTheme() }
            "ACTION_SCI" -> { currentTabIndex = 1; applyTheme() }
            "ACTION_COIN" -> { currentTabIndex = 13; applyTheme() }
            "ACTION_OPEN_COMPASS" -> {
                ProState.setProEnabled(prefs, true)
                expandedSections.add(308)
                currentTabIndex = 40
                applyTheme()
            }
            else -> applyTheme()
        }
    }

    private fun setupShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val sm = getSystemService(ShortcutManager::class.java)
            val s1 = ShortcutInfo.Builder(this, "coin").setShortLabel("מטבעות").setIcon(Icon.createWithResource(this, android.R.drawable.star_on)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_COIN" }).build()
            val s2 = ShortcutInfo.Builder(this, "sci").setShortLabel("מדעית").setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_edit)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_SCI" }).build()
            val s3 = ShortcutInfo.Builder(this, "compass").setShortLabel("מצפן 3D").setIcon(Icon.createWithResource(this, android.R.drawable.ic_menu_compass)).setIntent(Intent(this, MainActivity::class.java).apply { action = "ACTION_OPEN_COMPASS" }).build()
            sm.dynamicShortcuts = listOf(s1, s2, s3)
        }
    }

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
        useCustomAcSound = prefs?.getBoolean("useCustomAcSound", true) ?: true
        isRightHanded = prefs?.getBoolean("isRightHanded", true) ?: true
        isIosGlassMode = prefs?.getBoolean("isIosGlassMode", false) ?: false
        isAuroraMode = prefs?.getBoolean("isAuroraMode", false) ?: false
        isMinimalistMode = prefs?.getBoolean("isMinimalistMode", false) ?: false
        if (isMinimalistMode) {
            isIosGlassMode = false
            isAuroraMode = false
        } else if (isAuroraMode && isIosGlassMode) {
            isIosGlassMode = false
        }
        IosGlassTheme.loadIosPrefs(prefs)
        MinimalistTheme.loadMinimalistPrefs(prefs)
        calcPrecision = prefs?.getInt("calcPrecision", 8) ?: 8; buttonCornerStyle = prefs?.getFloat("buttonCornerStyle", 24f) ?: 24f
        primaryColor = prefs?.getInt("primaryColor", Color.parseColor("#64B5F6")) ?: Color.parseColor("#64B5F6"); bgColor = prefs?.getInt("bgColor", Color.parseColor("#08090C")) ?: Color.parseColor("#08090C")
        memoryValue = Double.fromBits(prefs?.getLong("memoryValue", 0L) ?: 0L); val savedHist = prefs?.getString("history", "") ?: ""
        fromCurrency = extractCurrencyCode(prefs?.getString("fromCurrency", "USD") ?: "USD")
        toCurrency = extractCurrencyCode(prefs?.getString("toCurrency", "ILS") ?: "ILS")
        fromDist = prefs?.getString("fromDist", "קילומטר (km)") ?: "קילומטר (km)"; toDist = prefs?.getString("toDist", "מייל (mi)") ?: "מייל (mi)"
        fromWeight = prefs?.getString("fromWeight", "קילוגרם (kg)") ?: "קילוגרם (kg)"; toWeight = prefs?.getString("toWeight", "פאונד (lbs)") ?: "פאונד (lbs)"
        fromTemp = prefs?.getString("fromTemp", "צלזיוס (°C)") ?: "צלזיוס (°C)"; toTemp = prefs?.getString("toTemp", "פרנהייט (°F)") ?: "פרנהייט (°F)"
        fromSpeed = prefs?.getString("fromSpeed", "קמ״ש (km/h)") ?: "קמ״ש (km/h)"; toSpeed = prefs?.getString("toSpeed", "מייל לשעה (mph)") ?: "מייל לשעה (mph)"

        hiddenDrawerTabs.clear()
        val rawHidden = prefs?.getString("hidden_drawer_tabs", "") ?: ""
        if (rawHidden.isNotBlank()) {
            rawHidden.split(",").mapNotNull { it.trim().toIntOrNull() }.forEach {
                if (it != 19) hiddenDrawerTabs.add(it)
            }
        }

        historyList.clear(); if (savedHist.isNotEmpty()) historyList.addAll(savedHist.split("||")); updateKeepScreenOn()
    }

    fun savePreferences() {
        IosGlassTheme.saveIosPrefs(prefs)
        MinimalistTheme.saveMinimalistPrefs(prefs)
        val maxHist = if (ProState.isProEnabled(prefs)) 500 else 30
        prefs?.edit()
            ?.putBoolean("useHaptic", useHaptic)
            ?.putBoolean("useSound", useSound)
            ?.putBoolean("useAnimations", useAnimations)
            ?.putBoolean("keepScreenOn", keepScreenOn)
            ?.putBoolean("useCustomAcSound", useCustomAcSound)
            ?.putBoolean("isRightHanded", isRightHanded)
            ?.putBoolean("isIosGlassMode", isIosGlassMode)
            ?.putBoolean("isAuroraMode", isAuroraMode)
            ?.putBoolean("isMinimalistMode", isMinimalistMode)
            ?.putInt("soundType", soundType.coerceIn(0, 3))
            ?.putInt("calcPrecision", calcPrecision)
            ?.putFloat("buttonCornerStyle", buttonCornerStyle)
            ?.putInt("primaryColor", primaryColor)
            ?.putInt("bgColor", bgColor)
            ?.putLong("memoryValue", memoryValue.toRawBits())
            ?.putString("fromCurrency", fromCurrency)
            ?.putString("toCurrency", toCurrency)
            ?.putString("fromDist", fromDist)
            ?.putString("toDist", toDist)
            ?.putString("fromWeight", fromWeight)
            ?.putString("toWeight", toWeight)
            ?.putString("fromTemp", fromTemp)
            ?.putString("toTemp", toTemp)
            ?.putString("fromSpeed", fromSpeed)
            ?.putString("toSpeed", toSpeed)
            ?.putString("hidden_drawer_tabs", hiddenDrawerTabs.joinToString(","))
            ?.putString("history", historyList.take(maxHist).joinToString("||"))
            ?.apply()
    }

    fun updateKeepScreenOn() { if (keepScreenOn) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }

    fun applyTheme() {
        if (!isCleanGlassActive || !isDrawerOpen || isMinimalistMode) {
            IosGlassTheme.applyRealtimeGlassBlur(mainContent, false)
        }

        val statusNavCol = when {
            isMinimalistMode -> MinimalistTheme.getBackgroundColor(this)
            isAuroraMode -> AuroraBorealisTheme.ARCTIC_BLUE
            else -> bgColor
        }
        window.statusBarColor = statusNavCol
        window.navigationBarColor = statusNavCol

        val topIconCol = MinimalistTheme.resolveTopHeaderIconColor(this)

        if (isMinimalistMode) {
            val minBg = MinimalistTheme.getBackgroundColor(this)
            mainContent?.setBackgroundColor(minBg)
            drawerScroll.setBackgroundColor(MinimalistTheme.DRAWER_BG)
            drawerMenu?.background = null
            val headerInk = MinimalistTheme.ensureReadableTextOnBg(MinimalistTheme.TEXT_CHARCOAL, minBg, 2.2)
            titleText.setTextColor(headerInk)
            menuBtn.setTextColor(topIconCol)

            val logoBgCol = MinimalistTheme.DRAWER_HEADER_YELLOW
            val logoTxtCol = MinimalistTheme.ensureReadableTextOnBg(MinimalistTheme.LOGO_TEXT_COLOR, logoBgCol, 2.2)
            appLogoView?.apply {
                setTextColor(logoTxtCol)
                background = GradientDrawable().apply {
                    cornerRadius = dp(24f).toFloat()
                    setColor(logoBgCol)
                }
                val calcIcon = MinimalistTheme.createMinimalistVectorIcon(this@MainActivity, 0, logoTxtCol, 28f)
                setCompoundDrawables(calcIcon, null, null, null)
                compoundDrawablePadding = dp(10f)
            }

            pipBtn.text = ""
            pipBtn.setCompoundDrawables(MinimalistTheme.createMinimalistVectorIcon(this, 100, topIconCol, 22f), null, null, null)
            historyBtn.text = ""
            historyBtn.setCompoundDrawables(MinimalistTheme.createMinimalistVectorIcon(this, 101, topIconCol, 22f), null, null, null)
        } else if (isAuroraMode) {
            mainContent?.background = AuroraBorealisTheme.createAuroraAtmosphericBackground()
            val drawerBg = AuroraBorealisTheme.createAuroraCardDrawable(this, 0f, isDrawer = true)
            drawerScroll.background = drawerBg
            drawerMenu?.background = null
            titleText.setTextColor(Color.WHITE)
            menuBtn.setTextColor(topIconCol)
            appLogoView?.apply {
                val logoBgCol = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_BG]) MinimalistTheme.DRAWER_HEADER_YELLOW else AuroraBorealisTheme.ARCTIC_BLUE
                val rawLogoTxt = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_TEXT]) MinimalistTheme.LOGO_TEXT_COLOR else Color.WHITE
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(rawLogoTxt, logoBgCol, 2.2))
                background = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_BG]) {
                    GradientDrawable().apply {
                        cornerRadius = dp(24f).toFloat()
                        setColor(logoBgCol)
                    }
                } else null
                setCompoundDrawables(null, null, null, null)
            }
            pipBtn.text = ""
            pipBtn.setCompoundDrawables(AuroraBorealisTheme.createVectorIcon(this, 100, topIconCol, 22f, true), null, null, null)
            historyBtn.text = ""
            historyBtn.setCompoundDrawables(AuroraBorealisTheme.createVectorIcon(this, 101, topIconCol, 22f, true), null, null, null)
        } else if (isIosGlassMode) {
            mainContent?.background = IosGlassTheme.createLiquidAmbientBackground(this)
            val drawerBg = IosGlassTheme.createGlassCardDrawable(this, 0f)
            drawerScroll.background = drawerBg
            drawerMenu?.background = null
            titleText.setTextColor(Color.WHITE)
            menuBtn.setTextColor(topIconCol)
            appLogoView?.apply {
                val logoBgCol = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_BG]) MinimalistTheme.DRAWER_HEADER_YELLOW else bgColor
                val rawLogoTxt = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_TEXT]) MinimalistTheme.LOGO_TEXT_COLOR else Color.WHITE
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(rawLogoTxt, logoBgCol, 2.2))
                background = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_BG]) {
                    GradientDrawable().apply {
                        cornerRadius = dp(24f).toFloat()
                        setColor(logoBgCol)
                    }
                } else null
                setCompoundDrawables(null, null, null, null)
            }
            pipBtn.text = ""
            pipBtn.setCompoundDrawables(IosGlassTheme.createSfSymbolDrawable(this, 100, topIconCol, 22f), null, null, null)
            historyBtn.text = ""
            historyBtn.setCompoundDrawables(IosGlassTheme.createSfSymbolDrawable(this, 101, topIconCol, 22f), null, null, null)
        } else {
            mainContent?.setBackgroundColor(bgColor)
            drawerScroll.setBackgroundColor(MinimalistTheme.resolveDrawerBgColor(this))
            drawerMenu?.background = null
            val headerInk = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_TEXT_ICONS]) {
                MinimalistTheme.ensureReadableTextOnBg(MinimalistTheme.TEXT_CHARCOAL, bgColor, 2.2)
            } else Color.WHITE
            titleText.setTextColor(headerInk)
            menuBtn.setTextColor(topIconCol)
            appLogoView?.apply {
                val logoBgCol = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_BG]) MinimalistTheme.DRAWER_HEADER_YELLOW else MinimalistTheme.resolveDrawerBgColor(this@MainActivity)
                val rawLogoTxt = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_TEXT]) MinimalistTheme.LOGO_TEXT_COLOR else Color.WHITE
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(rawLogoTxt, logoBgCol, 2.2))
                background = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_LOGO_BG]) {
                    GradientDrawable().apply {
                        cornerRadius = dp(24f).toFloat()
                        setColor(logoBgCol)
                    }
                } else null
                setCompoundDrawables(null, null, null, null)
            }
            pipBtn.setCompoundDrawables(null, null, null, null)
            historyBtn.setCompoundDrawables(null, null, null, null)
            pipBtn.text = "🧮"; historyBtn.text = "🕒"
            pipBtn.textSize = 21f; historyBtn.textSize = 20f
            pipBtn.setTextColor(topIconCol); historyBtn.setTextColor(topIconCol)
        }

        refreshFavHeaderIcon()
        AuroraBorealisTheme.applyHeaderMetallicGradient(appLogoView, isAuroraMode, isIosGlassMode)
        updateHandednessLayout(); updateDrawerSelection(); switchTab(currentTabIndex)
    }

    private fun openDrawer() {
        updateHandednessLayout()
        if (isCleanGlassActive && !isMinimalistMode) IosGlassTheme.applyRealtimeGlassBlur(mainContent, true)
        dimView.visibility = View.VISIBLE; dimView.animate().alpha(1f).setDuration(250).start()
        drawerScroll.animate().translationX(0f).setDuration(300)
            .setInterpolator(DecelerateInterpolator())
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
            if (symbol == "AC" && useCustomAcSound && acSoundId != 0) {
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

    // =========================================================================
    // עדכון תפריט הצד והסמלים שבו עם הגנת ניגודיות מלאה לכל פריט ולפריט הנבחר!
    // =========================================================================
    fun updateDrawerSelection() {
        val proActive = ProState.isProEnabled(prefs)
        val sepColor = when {
            isMinimalistMode -> Color.argb(22, 44, 44, 44)
            isAuroraMode -> Color.argb(38, 0, 245, 212)
            isIosGlassMode -> Color.argb(28, 255, 255, 255)
            else -> Color.argb(25, 255, 255, 255)
        }
        for (sep in drawerSeparators) sep.setBackgroundColor(sepColor)

        for ((secIconCode, headerTv, container) in drawerSectionHeaders) {
            val secItemIndices = drawerSections.find { it.first == secIconCode }?.third ?: emptyList()

            for (idx in secItemIndices) {
                val pair = drawerItemRowsMap[idx]
                val vis = if (isCalculatorVisibleInDrawer(idx)) View.VISIBLE else View.GONE
                pair?.first?.visibility = vis
                pair?.second?.visibility = vis
            }

            val hasVisibleCalculators = secItemIndices.any { isCalculatorVisibleInDrawer(it) }

            if (secIconCode in 305..311 && !proActive) {
                headerTv.visibility = View.GONE
                proSectionDividers[secIconCode]?.visibility = View.GONE
                container.visibility = View.GONE
                continue
            }

            if (!hasVisibleCalculators) {
                headerTv.visibility = View.GONE
                sectionDividersMap[secIconCode]?.visibility = View.GONE
                container.visibility = View.GONE
                continue
            } else {
                headerTv.visibility = View.VISIBLE
                sectionDividersMap[secIconCode]?.visibility = View.VISIBLE
            }

            val rawSecTitle = headerTv.tag as? String ?: ""
            val cleanTitle = IosGlassTheme.cleanCategoryTitle(rawSecTitle, true)
            val isExpanded = expandedSections.contains(secIconCode)
            val arrow = if (isExpanded) "▾" else if (isRightHanded) "◂" else "▸"
            headerTv.text = if (isRightHanded) "$cleanTitle  $arrow" else "$arrow  $cleanTitle"

            val hasActiveChild = secItemIndices.contains(currentTabIndex) && isCalculatorVisibleInDrawer(currentTabIndex)
            val isHeaderHighlighted = isExpanded || hasActiveChild

            val headerTextCol = MinimalistTheme.resolveDrawerItemTextColor(
                this,
                isSelected = false,
                isExpandedHeader = isHeaderHighlighted
            )
            val headerIconCol = MinimalistTheme.resolveDrawerIconColor(
                this,
                isSelected = false,
                isExpandedHeader = isHeaderHighlighted
            )
            headerTv.setTextColor(headerTextCol)

            val secIcon = if (isMinimalistMode) {
                MinimalistTheme.createMinimalistVectorIcon(this, secIconCode, headerIconCol, 22f)
            } else {
                AuroraBorealisTheme.createVectorIcon(this, secIconCode, headerIconCol, 22f, isAuroraMode)
            }
            if (isRightHanded) headerTv.setCompoundDrawables(null, null, secIcon, null)
            else headerTv.setCompoundDrawables(secIcon, null, null, null)

            if (isHeaderHighlighted) {
                headerTv.background = when {
                    isMinimalistMode -> GradientDrawable().apply {
                        cornerRadius = dp(18f).toFloat()
                        setColor(MinimalistTheme.NEUTRAL_SECONDARY_BG)
                    }
                    else -> GradientDrawable().apply {
                        val pillCol = MinimalistTheme.resolveActivePillBgColor(this@MainActivity)
                        setColor(Color.argb(38, Color.red(pillCol), Color.green(pillCol), Color.blue(pillCol)))
                        cornerRadius = dp(14f).toFloat()
                        setStroke(dp(1f), Color.argb(95, Color.red(pillCol), Color.green(pillCol), Color.blue(pillCol)))
                    }
                }
            } else {
                headerTv.background = null
            }
            container.visibility = if (isExpanded) View.VISIBLE else View.GONE
        }

        for (i in drawerItemsViews.indices) {
            val tv = drawerItemsViews[i] as TextView
            val isSelected = (i == currentTabIndex)

            val itemTextCol = MinimalistTheme.resolveDrawerItemTextColor(
                this,
                isSelected = isSelected,
                isExpandedHeader = false
            )
            val itemIconCol = MinimalistTheme.resolveDrawerIconColor(
                this,
                isSelected = isSelected,
                isExpandedHeader = false
            )

            tv.text = IosGlassTheme.cleanCategoryTitle(categories[i], true)
            val icon = if (isMinimalistMode) {
                MinimalistTheme.createMinimalistVectorIcon(this, i, itemIconCol, 20f)
            } else {
                AuroraBorealisTheme.createVectorIcon(this, i, itemIconCol, 20f, isAuroraMode)
            }
            if (isRightHanded) tv.setCompoundDrawables(null, null, icon, null)
            else tv.setCompoundDrawables(icon, null, null, null)

            if (isSelected) {
                tv.background = when {
                    isMinimalistMode -> MinimalistTheme.createMinimalistActiveDrawerCapsule(this)
                    else -> GradientDrawable().apply {
                        val activeFill = MinimalistTheme.resolveActivePillBgColor(this@MainActivity)
                        setColor(activeFill)
                        cornerRadius = dp(if (isCleanGlassActive) 16f else 14f).toFloat()
                        setStroke(dp(1f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, Color.argb(140, 255, 255, 255)))
                    }
                }
                tv.setTextColor(itemTextCol)
            } else {
                tv.background = null
                tv.setTextColor(itemTextCol)
            }
        }
    }

    private fun unifyThemeSelectorAndCustomizerInSettings(settingsRoot: View?) {
        if (settingsRoot == null) return
        val scrollObj = settingsRoot as? ScrollView
        val mainContainer = scrollObj?.getChildAt(0) as? LinearLayout ?: (settingsRoot as? LinearLayout) ?: return

        fun currentThemeDisplayName(): String {
            return when {
                isMinimalistMode -> "🌿 עיצוב מינימליסטי"
                isAuroraMode -> "🌌 הזוהר הצפוני (Aurora Borealis)"
                isIosGlassMode -> "💎 עיצוב הזכוכית (Liquid Glass Pro)"
                else -> "🎨 עיצוב רגיל (קלאסי)"
            }
        }

        fun isValidSettingItemToKeep(v: View): Boolean {
            if (v is TextView) {
                val t = v.text?.toString() ?: ""
                return t.contains("רטט") ||
                        t.contains("צלילי הקשה") ||
                        t.contains("סוג צליל") ||
                        t.contains("צליל AC") ||
                        t.contains("אנימציות") ||
                        t.contains("מסך תמיד דולק") ||
                        t.contains("צורת כפתורים") ||
                        t.contains("דיוק עשרוני") ||
                        t.contains("מצב מקצועי") ||
                        t.contains("Pro Mode") ||
                        t.contains("יד דומיננטית") ||
                        t.contains("רשימת סימון") ||
                        t.contains("איפוס הגדרות")
            }
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) {
                    val sub = v.getChildAt(i)
                    if (sub is TextView && sub.text.toString().contains("רשימת סימון")) {
                        return true
                    }
                    if (!isMinimalistMode && (isIosGlassMode || isAuroraMode)) {
                        if (sub is TextView && (sub.text.contains("שקיפות זכוכית") || sub.text.contains("טשטוש") || sub.text.contains("עוצמת זוהר"))) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        for (i in mainContainer.childCount - 1 downTo 0) {
            val child = mainContainer.getChildAt(i)
            if (!isValidSettingItemToKeep(child)) {
                mainContainer.removeViewAt(i)
            }
        }

        val matchedPalettesBar = MinimalistTheme.createThemeMatchedPalettesBar(this)
        mainContainer.addView(matchedPalettesBar, 0)

        val displayModeBtn = createActionBtn("מצב תצוגה: ${currentThemeDisplayName()}", false) {
            MinimalistTheme.showUnifiedDisplayModeDialog(this)
        }
        mainContainer.addView(displayModeBtn, 1)

        val customizerPanel = MinimalistTheme.createColorThemeCustomizerSection(this)
        mainContainer.addView(customizerPanel, 2)
    }

    private fun applyButtonShapeVisibilityAndInsertAcSoundSetting(root: View?) {
        if (root == null) return
        if (root is ViewGroup) {
            var i = 0
            while (i < root.childCount) {
                val ch = root.getChildAt(i)
                if (ch is TextView) {
                    val txt = ch.text.toString()
                    if (txt.contains("צורת כפתורים")) {
                        ch.visibility = if (!isIosGlassMode && !isAuroraMode) View.VISIBLE else View.GONE
                        if (isMinimalistMode) {
                            val currentCorner = MinimalistTheme.pillCornerPt
                            val shapeLbl = when {
                                currentCorner >= 24f -> "מודרניים"
                                currentCorner >= 16f -> "מעוגלים"
                                else -> "מרובעים"
                            }
                            ch.text = "צורת כפתורים: $shapeLbl"
                            ch.setOnClickListener {
                                feedback(it)
                                MinimalistTheme.pillCornerPt = when {
                                    MinimalistTheme.pillCornerPt >= 24f -> 18f
                                    MinimalistTheme.pillCornerPt >= 16f -> 10f
                                    else -> 26f
                                }
                                buttonCornerStyle = MinimalistTheme.pillCornerPt
                                savePreferences()
                                switchTab(19)
                            }
                        }
                    } else if (txt.contains("דיוק עשרוני")) {
                        ch.text = "דיוק עשרוני: $calcPrecision ספרות"
                        ch.setOnClickListener {
                            feedback(it)
                            MinimalistTheme.showUnifiedPrecisionPickerDialog(this)
                        }
                    } else if (txt.contains("סוג צליל")) {
                        val soundNames = listOf("מערכת", "צליל 1", "צליל 2", "צליל 3")
                        ch.text = "סוג צליל: ${soundNames.getOrElse(soundType.coerceIn(0, 3)) { "מערכת" }}"
                        ch.setOnClickListener {
                            feedback(it)
                            MinimalistTheme.showUnifiedSoundPickerDialog(this)
                        }

                        val nextChild = if (i + 1 < root.childCount) root.getChildAt(i + 1) else null
                        val alreadyAdded = (nextChild is TextView && nextChild.tag == "AC_SOUND_MODE_BTN")
                        if (!alreadyAdded) {
                            val acModeText = if (useCustomAcSound) {
                                "צליל AC: צליל ה-AC הרגיל של המחשבון"
                            } else {
                                "צליל AC: לפי הצליל המוגדר בכפתור הצלילים"
                            }
                            val acSoundBtn = createActionBtn(acModeText, false) {
                                useCustomAcSound = !useCustomAcSound
                                savePreferences()
                                feedback(it, "AC")
                                switchTab(19)
                            }.apply {
                                tag = "AC_SOUND_MODE_BTN"
                            }
                            root.addView(acSoundBtn, i + 1)
                            i++
                        }
                    }
                } else if (ch is ViewGroup) {
                    applyButtonShapeVisibilityAndInsertAcSoundSetting(ch)
                }
                i++
            }
        }
    }

    private fun createDrawerCalculatorsChecklistSection(): View {
        val textCol = if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val accent = when {
            isMinimalistMode -> MinimalistTheme.ACCENT_SKY_BLUE
            isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> primaryColor
        }
        val proActive = ProState.isProEnabled(prefs)
        val availableSections = drawerSections.filter { (secCode, _, _) ->
            proActive || secCode !in 305..311
        }
        val allSelectableIndices = availableSections.flatMap { it.third }
        val visibleCount = allSelectableIndices.count { isCalculatorVisibleInDrawer(it) }

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6f); bottomMargin = dp(10f) }
        }

        lateinit var openChecklistBtn: TextView
        lateinit var listContainer: LinearLayout
        val checkBoxesList = mutableListOf<Pair<Int, CheckBox>>()

        fun updateOpenBtnCounterText() {
            val curVis = allSelectableIndices.count { isCalculatorVisibleInDrawer(it) }
            val rawLabel = "רשימת סימון: מחשבונים בתפריט הצד ($curVis/${allSelectableIndices.size})"
            openChecklistBtn.text = IosGlassTheme.cleanCategoryTitle(rawLabel, isCleanGlassActive)
        }

        fun closeChecklist() {
            isDrawerChecklistExpanded = false
            listContainer.visibility = View.GONE
            openChecklistBtn.visibility = View.VISIBLE
            updateOpenBtnCounterText()
            savePreferences()
            updateDrawerSelection()
        }

        openChecklistBtn = createActionBtn(
            "רשימת סימון: מחשבונים בתפריט הצד ($visibleCount/${allSelectableIndices.size})",
            false
        ) {
            isDrawerChecklistExpanded = true
            openChecklistBtn.visibility = View.GONE
            listContainer.visibility = View.VISIBLE
            if (useAnimations) {
                listContainer.alpha = 0f
                listContainer.animate().alpha(1f).setDuration(200).start()
            }
        }.apply {
            visibility = if (isDrawerChecklistExpanded) View.GONE else View.VISIBLE
        }

        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (isDrawerChecklistExpanded) View.VISIBLE else View.GONE
            setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
            background = when {
                isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@MainActivity, 20f, 0)
                isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@MainActivity, 20f)
                else -> GradientDrawable().apply {
                    setColor(Utils.lightenColor(bgColor, 0.06f))
                    cornerRadius = dp(20f).toFloat()
                    setStroke(dp(1f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, Utils.lightenColor(bgColor, 0.16f)))
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8f) }
        }

        val topDoneBtn = createActionBtn("סיום ✔️", true) {
            closeChecklist()
        }
        listContainer.addView(topDoneBtn)

        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8f) }
        }

        var isBulkUpdating = false

        val selectAllBtn = createActionBtn("סמן הכל ✔️", false) {
            isBulkUpdating = true
            for (idx in allSelectableIndices) hiddenDrawerTabs.remove(idx)
            for ((_, cb) in checkBoxesList) cb.isChecked = true
            isBulkUpdating = false
            savePreferences()
            updateDrawerSelection()
            updateOpenBtnCounterText()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(4f)
            }
        }

        val deselectAllBtn = createActionBtn("נקה הכל (למעט רגיל)", false) {
            isBulkUpdating = true
            for (idx in allSelectableIndices) {
                if (idx != 0) hiddenDrawerTabs.add(idx) else hiddenDrawerTabs.remove(0)
            }
            for ((idx, cb) in checkBoxesList) cb.isChecked = (idx == 0)
            isBulkUpdating = false
            savePreferences()
            updateDrawerSelection()
            updateOpenBtnCounterText()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(4f)
            }
        }

        actionsRow.addView(selectAllBtn)
        actionsRow.addView(deselectAllBtn)
        listContainer.addView(actionsRow)

        for ((_, secTitle, itemIndices) in availableSections) {
            val secTitleTv = TextView(this).apply {
                text = IosGlassTheme.cleanCategoryTitle(secTitle, isCleanGlassActive)
                setTextColor(if (isMinimalistMode) MinimalistTheme.TEXT_CHARCOAL else accent)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.RIGHT
                setPadding(dp(4f), dp(10f), dp(4f), dp(4f))
            }
            listContainer.addView(secTitleTv)

            for (tabIdx in itemIndices) {
                val cb = CheckBox(this).apply {
                    text = "  ${IosGlassTheme.cleanCategoryTitle(categories[tabIdx], isCleanGlassActive)}"
                    setTextColor(textCol)
                    textSize = 14.5f
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    buttonTintList = ColorStateList.valueOf(accent)
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                    isChecked = isCalculatorVisibleInDrawer(tabIdx)
                    setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
                    setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isBulkUpdating) return@setOnCheckedChangeListener
                        feedback(buttonView)
                        setCalculatorVisibleInDrawer(tabIdx, isChecked)
                        updateOpenBtnCounterText()
                    }
                }
                checkBoxesList.add(tabIdx to cb)
                listContainer.addView(cb)
            }
        }

        val bottomDoneBtn = createActionBtn("סיום ✔️", true) {
            closeChecklist()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(10f) }
        }
        listContainer.addView(bottomDoneBtn)

        wrapper.addView(openChecklistBtn)
        wrapper.addView(listContainer)
        return wrapper
    }

    fun switchTab(index: Int) {
        val wasInSettings = (currentTabIndex == 19 && index == 19)
        if (wasInSettings) {
            settingsScrollY = currentSettingsScroll?.scrollY ?: settingsScrollY
        } else if (index != 19) {
            settingsScrollY = 0
            isDrawerChecklistExpanded = false
        } else {
            isDrawerChecklistExpanded = false
        }
        if (MiniGameEngine.isActive) MiniGameEngine.stopGame(this)
        currentTabIndex = index
        titleText.text = IosGlassTheme.cleanCategoryTitle(categories.getOrElse(index) { "מחשבון" }, true)
        refreshFavHeaderIcon()
        updateDrawerSelection(); contentFrame.removeAllViews()
        val rawView = when (index) {
            0 -> createBasicCalculator(); 1 -> createScientificCalculator(); 2 -> createVatCalculator(); 3 -> createLoanCalculator()
            4 -> createDistanceCalculator(); 5 -> createWeightCalculator(); 6 -> createTempCalculator(); 7 -> createProgrammerCalculator()
            8 -> createPercentageCalculator(); 9 -> createDiscountCalculator(); 10 -> createTipCalculator(); 11 -> createBMICalculator()
            12 -> createAgeCalculator(); 13 -> createCurrencyCalculator(); 14 -> createBmrCalculator(); 15 -> createQuadraticCalculator()
            16 -> createFractionCalculator(); 17 -> createCompoundInterestCalculator(); 18 -> createSpeedCalculator()
            19 -> {
                val settingsView = createSettingsPage()
                enhanceSettingsHierarchy(settingsView)
                applyButtonShapeVisibilityAndInsertAcSoundSetting(settingsView)

                val scrollObj = settingsView as? ScrollView
                currentSettingsScroll = scrollObj
                val container = scrollObj?.getChildAt(0) as? LinearLayout ?: (settingsView as? LinearLayout)

                var hasProBtn = false
                if (container != null) {
                    for (i in 0 until container.childCount) {
                        val ch = container.getChildAt(i)
                        if (ch is TextView && ch.text.contains("Pro Mode")) {
                            hasProBtn = true
                            break
                        }
                    }
                    if (!hasProBtn) {
                        val proEnabled = ProState.isProEnabled(prefs)
                        val proBtn = createActionBtn(
                            if (proEnabled) "🚀 מצב מקצועי (Pro Mode - 87 יחידות ו-48 כלים): מופעל 🟢"
                            else "🚀 מצב מקצועי (Pro Mode - 87 יחידות ו-48 כלים): כבוי 🔴",
                            proEnabled
                        ) {
                            val nextState = !ProState.isProEnabled(prefs)
                            ProState.setProEnabled(prefs, nextState)
                            if (nextState) expandedSections.add(305)
                            Toast.makeText(
                                this,
                                if (nextState) "🚀 מצב מקצועי הופעל! קטגוריות ה-Pro נוספו לתפריט הצד" else "מצב מקצועי כובה",
                                Toast.LENGTH_SHORT
                            ).show()
                            applyTheme()
                        }
                        container.addView(proBtn)
                    }
                }

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
                container?.addView(createDrawerCalculatorsChecklistSection())

                unifyThemeSelectorAndCustomizerInSettings(settingsView)

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
            // --- כל מסכי ה-Pro המעוצבים בקנבה ---
            31 -> createPro87UnitConverter()
            32 -> createProPlumbingAndFluidsScreen()
            33 -> createProMedicalDosageScreen()
            34 -> createProConstructionAndAcScreen()
            35 -> createProResistorScreen()
            36 -> createProCapacitorScreen()
            37 -> createProAwgWireScreen()
            38 -> createProOhmAndPowerScreen()
            39 -> createProSpiritLevelScreen()
            40 -> createProCompassScreen()
            41 -> createProRulerProtractorScreen()
            42 -> createProStopwatchScreen()
            43 -> createProFlashlightScreen()
            44 -> createProPeriodicTableScreen()
            45 -> createProMorseScreen()
            46 -> createProPasswordGenScreen()
            47 -> createProGematriaAndTextScreen()
            48 -> createProCurrenciesAndMetalsHub()
            49 -> createProMortgageSpitzerScreen()
            50 -> createProRoiCagrScreen()
            51 -> createProBusinessMarginScreen()
            52 -> createProStatisticsBellScreen()
            53 -> createProGeometryBlueprintScreen()
            54 -> createProNumberTheoryScreen()
            55 -> createProPowerFeaturesCenter()
            56 -> createProFavoritesCenterScreen()
            57 -> createProMechanicsUnitsScreen()
            58 -> createProFluidsUnitsScreen()
            59 -> createProThermoUnitsScreen()
            60 -> createProElectricalUnitsScreen()
            61 -> createProOpticsRadiationUnitsScreen()
            62 -> createProMedicalChemUnitsScreen()
            63 -> createProDigitalAstroUnitsScreen()
            64 -> createProSalaryAndTaxScreen()
            65 -> createProMatrixAndEquationsScreen()
            else -> createBasicCalculator()
        }

        if (isMinimalistMode && index != 0 && index != 1) {
            MinimalistTheme.applyMinimalistStylingToHierarchy(rawView, this)
        }

        contentFrame.addView(rawView)
        if (useAnimations && !wasInSettings) {
            rawView.alpha = 0f; rawView.translationY = dp(24f).toFloat()
            rawView.animate().alpha(1f).translationY(0f).setDuration(260).setInterpolator(DecelerateInterpolator()).start()
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
        val displayCardBgColor = if (MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_CARDS_INPUTS]) {
            MinimalistTheme.CARD_WHITE
        } else if (isMinimalistMode) {
            MinimalistTheme.getBackgroundColor(this)
        } else {
            Utils.lightenColor(bgColor, 0.07f)
        }

        val displayCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM or Gravity.END; setPadding(dp(22f), dp(14f), dp(22f), dp(16f))
            background = when {
                isMinimalistMode && !MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_CARDS_INPUTS] && !MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_BORDERS] -> null
                isMinimalistMode -> MinimalistTheme.createMinimalistCardDrawable(this@MainActivity, 28f, 0)
                isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@MainActivity, if (isAuroraMode) 28f else IosGlassTheme.glassCornerPt)
                else -> {
                    val strokeCol = MinimalistTheme.resolveBorderStrokeColor(
                        this@MainActivity,
                        Color.argb(70, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
                    )
                    GradientDrawable().apply {
                        setColor(displayCardBgColor)
                        cornerRadius = dp(28f).toFloat()
                        setStroke(dp(1.5f), strokeCol)
                    }
                }
            }
            if (isCleanGlassActive && !isMinimalistMode) IosGlassTheme.apply3DElevation(this, 10f)
        }

        if (isCleanGlassActive && !isMinimalistMode) {
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

        val displayNumCol = MinimalistTheme.ensureReadableTextOnBg(
            MinimalistTheme.resolveDisplayTextColor(this),
            displayCardBgColor,
            2.2
        )

        memoryIndicator = TextView(this).apply {
            text = if (memoryValue != 0.0) "M = ${Utils.formatResult(memoryValue, calcPrecision)}" else ""
            setTextColor(when {
                isMinimalistMode -> MinimalistTheme.TEXT_MUTED_GREY
                isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
                else -> MinimalistTheme.ensureReadableTextOnBg(primaryColor, displayCardBgColor, 2.2)
            })
            textSize = 12f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.START
        }
        expressionView = TextView(this).apply {
            text = ""
            setTextColor(when {
                isMinimalistMode -> MinimalistTheme.TEXT_MUTED_GREY
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
            setTextColor(displayNumCol)
            typeface = if (isMinimalistMode) Typeface.create("sans-serif-light", Typeface.NORMAL)
            else if (isCleanGlassActive) Typeface.create("sans-serif-medium", Typeface.BOLD)
            else Typeface.create("sans-serif-light", Typeface.NORMAL)
            setAutoSizeTextTypeUniformWithConfiguration(18, 60, 2, TypedValue.COMPLEX_UNIT_SP)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f)
            if (isAuroraMode) {
                setShadowLayer(12f, 0f, 0f, Color.argb(115, Color.red(AuroraBorealisTheme.AURORA_PRIMARY), Color.green(AuroraBorealisTheme.AURORA_PRIMARY), Color.blue(AuroraBorealisTheme.AURORA_PRIMARY)))
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
                val weight = if (!isGrid5x7 && symbol == "0" && !isMinimalistMode) 2f else 1f
                val btn = createAnimatedButton(symbol, isGrid5x7)
                keypadButtons[symbol] = btn
                val marginDp = if (isMinimalistMode) 5.5f else if (isCleanGlassActive) 4.5f else 3f
                rowLayout.addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight).apply { setMargins(dp(marginDp), dp(marginDp), dp(marginDp), dp(marginDp)) })
            }
            keypad.addView(rowLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        root.addView(keypad, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, if(isGrid5x7) 0.72f else 0.68f)); return root
    }

    private fun createAnimatedButton(symbol: String, isSmall: Boolean): TextView {
        val isMem = symbol in listOf("MC", "MR", "M+", "M-")
        val isOp = symbol in listOf("÷", "×", "−", "+", "sin", "cos", "tan", "log", "ln", "√", "x²", "x³", "x^y", "!", "π", "e", "(", ")")

        val defaultBg = MinimalistTheme.resolveKeypadButtonBgColor(this, symbol)
        val textColor = MinimalistTheme.resolveKeypadSymbolTextColor(this, symbol, defaultBg)
        val baseStrokeColor = if (symbol == "=" || symbol == "AC") Color.TRANSPARENT else Utils.lightenColor(bgColor, 0.15f)
        val strokeColor = MinimalistTheme.resolveBorderStrokeColor(this, baseStrokeColor)

        val shape = GradientDrawable().apply {
            this.shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(buttonCornerStyle).toFloat()
            setColor(defaultBg)
            setStroke(dp(1.5f), strokeColor)
        }
        val btnDrawable = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistKeypadButtonDrawable(this, symbol, false)
            isCleanGlassActive -> IosGlassTheme.createGlassButtonDrawable(this, symbol, isMem, false)
            else -> RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null)
        }

        return TextView(this).apply {
            text = symbol
            setTextColor(textColor)
            minimumWidth = dp(44f)
            minimumHeight = dp(44f)
            setPadding(dp(6f), dp(4f), dp(6f), dp(4f))
            setAutoSizeTextTypeUniformWithConfiguration(10, if(isSmall) 21 else 26, 2, TypedValue.COMPLEX_UNIT_SP)
            typeface = if (isMinimalistMode) Typeface.create("sans-serif", Typeface.NORMAL)
            else if (isCleanGlassActive) Typeface.create("sans-serif-medium", Typeface.BOLD)
            else if (symbol == "=" || isOp || isMem) Typeface.DEFAULT_BOLD
            else Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            background = btnDrawable
            isClickable = true

            if (isCleanGlassActive) {
                if (!isMinimalistMode) IosGlassTheme.apply3DElevation(this, 6f)
                IosGlassTheme.styleSymbolTextView(this, this@MainActivity, symbol, false)
            }

            setOnTouchListener { v, e ->
                val tv = v as TextView
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        feedback(v, symbol)
                        if (isMinimalistMode) {
                            tv.background = MinimalistTheme.createMinimalistKeypadButtonDrawable(this@MainActivity, symbol, true)
                            tv.setTextColor(MinimalistTheme.resolveKeypadSymbolTextColor(this@MainActivity, symbol))
                        } else if (isCleanGlassActive) {
                            tv.background = IosGlassTheme.createGlassButtonDrawable(this@MainActivity, symbol, isMem, true)
                            IosGlassTheme.styleSymbolTextView(tv, this@MainActivity, symbol, true)
                            IosGlassTheme.apply3DElevation(tv, 2f)
                        }
                        if (useAnimations) v.animate().scaleX(if (isCleanGlassActive) 0.95f else 0.84f).scaleY(if (isCleanGlassActive) 0.95f else 0.84f).setDuration(65).setInterpolator(DecelerateInterpolator()).start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (isMinimalistMode) {
                            tv.background = MinimalistTheme.createMinimalistKeypadButtonDrawable(this@MainActivity, symbol, false)
                            tv.setTextColor(MinimalistTheme.resolveKeypadSymbolTextColor(this@MainActivity, symbol))
                        } else if (isCleanGlassActive) {
                            tv.background = IosGlassTheme.createGlassButtonDrawable(this@MainActivity, symbol, isMem, false)
                            IosGlassTheme.styleSymbolTextView(tv, this@MainActivity, symbol, false)
                            IosGlassTheme.apply3DElevation(tv, 6f)
                        }
                        if (useAnimations) v.animate().scaleX(1f).scaleY(1f).setDuration(300).setInterpolator(OvershootInterpolator(1.8f)).start()
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

    fun createInputBox(hintText: String): EditText {
        val boxBgCol = if (isMinimalistMode || MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_CARDS_INPUTS]) {
            MinimalistTheme.CARD_WHITE
        } else {
            Utils.lightenColor(bgColor, 0.06f)
        }
        val boxTextCol = MinimalistTheme.ensureReadableTextOnBg(
            if (isMinimalistMode || MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_TEXT_ICONS]) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE,
            boxBgCol,
            2.2
        )
        return EditText(this).apply {
            hint = hintText
            setHintTextColor(when {
                isMinimalistMode -> MinimalistTheme.TEXT_MUTED_GREY
                isCleanGlassActive -> Color.parseColor("#B0BEC5")
                else -> Color.parseColor("#5C657A")
            })
            setTextColor(boxTextCol)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            textSize = 17f
            setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
            background = when {
                isMinimalistMode -> MinimalistTheme.createMinimalistInputDrawable(this@MainActivity, 0)
                isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(this@MainActivity, 16f)
                else -> GradientDrawable().apply {
                    setColor(boxBgCol)
                    cornerRadius = dp(16f).toFloat()
                    setStroke(dp(1.5f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, Utils.lightenColor(bgColor, 0.15f)))
                }
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14f) }
        }
    }

    fun createActionBtn(textStr: String, isPrimary: Boolean = true, onClick: (View) -> Unit): TextView {
        val cleanText = IosGlassTheme.cleanCategoryTitle(textStr, isCleanGlassActive)
        val isSwapBtn = cleanText.contains("החלף כיוון")
        val fillCol = when {
            isMinimalistMode -> if (isSwapBtn) MinimalistTheme.ACCENT_BUTTER_YELLOW else if (isPrimary) MinimalistTheme.getPrimaryActionColor(this) else MinimalistTheme.NEUTRAL_SECONDARY_BG
            isPrimary -> MinimalistTheme.getPrimaryActionColor(this)
            else -> Utils.lightenColor(bgColor, 0.06f)
        }
        val shape = GradientDrawable().apply {
            setColor(fillCol)
            cornerRadius = dp(buttonCornerStyle).toFloat()
            if (!isPrimary || MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_BORDERS]) {
                setStroke(dp(1.5f), MinimalistTheme.resolveBorderStrokeColor(this@MainActivity, Utils.lightenColor(bgColor, 0.15f)))
            }
        }
        val bgDraw = when {
            isMinimalistMode -> MinimalistTheme.createMinimalistActionButtonDrawable(
                this,
                isPrimary,
                if (isSwapBtn) 2 else if (isPrimary) 1 else 4
            )
            isCleanGlassActive -> IosGlassTheme.createGlassButtonDrawable(this, if (isPrimary) "=" else "0", false)
            else -> RippleDrawable(ColorStateList.valueOf(Color.parseColor("#33FFFFFF")), shape, null)
        }
        val rawBtnTextCol = when {
            MinimalistTheme.slotCustomized[MinimalistTheme.SLOT_OP_TEXT] && isPrimary ->
                MinimalistTheme.getSlotEffectiveColor(MinimalistTheme.SLOT_OP_TEXT, MinimalistTheme.TEXT_CHARCOAL)
            isMinimalistMode -> MinimalistTheme.TEXT_CHARCOAL
            isPrimary && isAuroraMode -> AuroraBorealisTheme.EMERALD_NIGHT
            isPrimary && !isCleanGlassActive -> Color.parseColor("#08090C")
            else -> Color.WHITE
        }
        val safeBtnTextCol = MinimalistTheme.ensureReadableTextOnBg(rawBtnTextCol, fillCol, 2.3)

        return TextView(this).apply {
            text = if (isMinimalistMode && isSwapBtn) "⇅ החלף כיוון" else cleanText
            gravity = Gravity.CENTER
            setTextColor(safeBtnTextCol)
            textSize = 15.5f
            typeface = if (isMinimalistMode) Typeface.create("sans-serif", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            background = bgDraw
            setPadding(dp(if (isMinimalistMode && isSwapBtn) 24f else 14f), dp(if (isMinimalistMode && isSwapBtn) 10f else 15f), dp(if (isMinimalistMode && isSwapBtn) 24f else 14f), dp(if (isMinimalistMode && isSwapBtn) 10f else 15f))
            layoutParams = LinearLayout.LayoutParams(
                if (isMinimalistMode && isSwapBtn) LinearLayout.LayoutParams.WRAP_CONTENT else LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10f)
                if (isMinimalistMode && isSwapBtn) gravity = Gravity.CENTER_HORIZONTAL
            }
            isClickable = true
            if (isCleanGlassActive && !isMinimalistMode) IosGlassTheme.apply3DElevation(this, 5f)
            setOnClickListener { feedback(it); onClick(it) }
        }
    }

    fun createResultTextView(): TextView = TextView(this).apply {
        setTextColor(MinimalistTheme.resolveDisplayTextColor(this@MainActivity))
        textSize = 20f
        gravity = Gravity.CENTER
        setPadding(0, dp(20f), 0, 0)
        setOnClickListener { if (text.isNotEmpty()) copyToClipboard(text.toString()) }
    }

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
 

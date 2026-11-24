package com.example.calculator

import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.graphics.*
import android.view.MotionEvent
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
// מנוע אנימציות ואפקטים בקנבס לכל 100 הכלים × 4 ערכות נושא (ProCanvasAnimations)
// =========================================================================
object ProCanvasAnimations {

    private const val PREF_KEY_ENABLED = "pro_canvas_animations_enabled_v1"

    var isCanvasAnimationsEnabled: Boolean = true
        private set

    private var overlayRef: WeakReference<ProCanvasOverlayView>? = null
    private var ambientRef: WeakReference<ProAmbientCanvasView>? = null

    enum class ThemeRenderStyle {
        MINIMALIST_PASTEL,
        LIQUID_GLASS,
        AURORA_NEON,
        CLASSIC_VECTOR
    }

    enum class SceneArchetype {
        MATH_ORBIT_GLYPHS,        // סמלים מרחפים במסלולים גאומטריים
        TRIG_HARMONIC_WAVES,      // גלי סינוס/קוסינוס הרמוניים
        PARABOLA_AND_SLOPE,       // פרבולה / ישר לינארי / עקומת פעמון
        FRACTION_RATIO_SLICES,    // פרוסות עוגה / יחסים ומסגרות מסך
        BINARY_CIPHER_RAIN,       // מטריקס בינארי / HEX / גלגל הצפנה / אנטרופיה
        FINANCE_CANDLES_TREND,    // נרות שוק, עקומות מט״ח ומטילי מתכות
        COMPOUND_GROWTH_CURVE,    // עקומת צמיחה מעריכית, הלוואות, חיסכון ואנונה
        DISCOUNT_TAX_TAGS,        // תגיות הנחה, מע״מ, מפלי מחיר ושכר
        RETIREMENT_HORIZON,       // שמש עולה מעל אופק וגלים (פרישה)
        CAR_SPEED_AERO,           // קווי אווירודינמיקה ומד מהירות/דלק
        RULER_CALIPER_UNITS,      // סרגל מידות, מאזניים, טבעת ומידות
        THERMO_HEAT_WAVES,        // גלי חום תרמיים וקרינה
        MEDICAL_HEARTBEAT_ECG,    // דופק ECG חי, מינונים וביוץ
        CLOCKWORK_TIME_GEARS,     // גלגלי שיניים, מטוטלת BPM ולוח שנה
        FLUID_WATER_BUBBLES,      // גלי מים מרובדים ובועות עולות
        BLUEPRINT_POLYHEDRA,      // מצולעים תלת-ממדיים ושרטוט אדריכלי
        CIRCUIT_PCB_ELECTRONS,    // מעגלי PCB, פולסי אלקטרונים, סלילים וסוללה
        RADAR_COMPASS_SWEEP,      // רדאר, מצפן, בועת פלס, סיסמוגרף וקלינומטר
        PRISM_LIGHT_SPECTRUM,     // קרני מנסרה, פנס, ספקטרום צבעים ומד אור
        ATOMIC_BOHR_ORBITS,       // מודל אטום בוהר וטבלה מחזורית
        NETWORK_MESH_SIGNALS,     // רשת צמתים (Mesh), מורס, סירנה ו-IP
        NUMBER_SERIES_MATRIX,     // עמודות סדרות, מטריצות וספרות רומיות
        TYPOGRAPHY_CONSTELLATION, // קונסטלציות טקסט, פתקים וגימטריה
        CLOUD_FAVORITES_SETTINGS  // כוכבי מועדפים, ענן והגדרות
    }

    data class ToolAnimSpec(
        val archetype: SceneArchetype,
        val glyphs: Array<String>,
        val variant: Int = 0,
        val speedFactor: Float = 1.0f,
        val accentBias: Int = 0
    )

    fun resolveThemeStyle(activity: MainActivity): ThemeRenderStyle = when {
        activity.isMinimalistMode -> ThemeRenderStyle.MINIMALIST_PASTEL
        activity.isAuroraMode -> ThemeRenderStyle.AURORA_NEON
        activity.isIosGlassMode -> ThemeRenderStyle.LIQUID_GLASS
        else -> ThemeRenderStyle.CLASSIC_VECTOR
    }

    // הגדרה ייחודית מותאמת לכל אחד מ-100 הכלים והמחשבונים (Tabs 0..99)
    fun resolveSpecForTab(tabIndex: Int): ToolAnimSpec = when (tabIndex) {
        0 -> ToolAnimSpec(SceneArchetype.MATH_ORBIT_GLYPHS, arrayOf("+", "−", "×", "÷", "=", "%"), 0, 1.00f, 0)
        1 -> ToolAnimSpec(SceneArchetype.TRIG_HARMONIC_WAVES, arrayOf("sin", "cos", "tan", "π", "ln", "√"), 0, 1.05f, 1)
        2 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("18%", "₪", "VAT", "+"), 0, 0.90f, 0)
        3 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("₪", "%", "PMT", "Loan"), 0, 0.92f, 1)
        4 -> ToolAnimSpec(SceneArchetype.RULER_CALIPER_UNITS, arrayOf("km", "m", "mi", "ft"), 0, 1.00f, 0)
        5 -> ToolAnimSpec(SceneArchetype.RULER_CALIPER_UNITS, arrayOf("kg", "lb", "g", "oz"), 1, 0.92f, 2)
        6 -> ToolAnimSpec(SceneArchetype.THERMO_HEAT_WAVES, arrayOf("°C", "°F", "K"), 0, 0.95f, 1)
        7 -> ToolAnimSpec(SceneArchetype.BINARY_CIPHER_RAIN, arrayOf("0", "1", "01", "10", "<<", "&"), 0, 1.15f, 0)
        8 -> ToolAnimSpec(SceneArchetype.MATH_ORBIT_GLYPHS, arrayOf("%", "÷", "100", "×"), 1, 0.95f, 2)
        9 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("-20%", "-50%", "%", "SALE"), 1, 1.00f, 1)
        10 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("12%", "15%", "₪", "TIP"), 2, 0.90f, 2)
        11 -> ToolAnimSpec(SceneArchetype.MEDICAL_HEARTBEAT_ECG, arrayOf("BMI", "kg", "m²"), 0, 1.00f, 0)
        12 -> ToolAnimSpec(SceneArchetype.CLOCKWORK_TIME_GEARS, arrayOf("Y", "M", "D"), 0, 0.88f, 1)
        13 -> ToolAnimSpec(SceneArchetype.FINANCE_CANDLES_TREND, arrayOf("$", "₪", "€", "£", "¥"), 0, 1.08f, 0)
        14 -> ToolAnimSpec(SceneArchetype.THERMO_HEAT_WAVES, arrayOf("BMR", "kcal", "kJ"), 1, 1.05f, 2)
        15 -> ToolAnimSpec(SceneArchetype.PARABOLA_AND_SLOPE, arrayOf("x²", "Δ", "x₁", "x₂"), 0, 1.00f, 1)
        16 -> ToolAnimSpec(SceneArchetype.FRACTION_RATIO_SLICES, arrayOf("½", "¾", "⅓", "⅖", "a/b"), 0, 0.95f, 0)
        17 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("(1+r)ⁿ", "₪", "%", "↑"), 1, 1.00f, 0)
        18 -> ToolAnimSpec(SceneArchetype.CAR_SPEED_AERO, arrayOf("km/h", "mph", "m/s", "kt"), 0, 1.30f, 1)
        19 -> ToolAnimSpec(SceneArchetype.CLOUD_FAVORITES_SETTINGS, arrayOf("✦", "◈", "○", "✧"), 0, 0.80f, 0)
        20 -> ToolAnimSpec(SceneArchetype.CAR_SPEED_AERO, arrayOf("₪/L", "km", "L"), 1, 1.10f, 2)
        21 -> ToolAnimSpec(SceneArchetype.CAR_SPEED_AERO, arrayOf("km/L", "MPG", "ECO"), 2, 1.05f, 0)
        22 -> ToolAnimSpec(SceneArchetype.MATH_ORBIT_GLYPHS, arrayOf("GPA", "100", "A+", "∑"), 2, 0.90f, 1)
        23 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("10%", "15%", "20%", "÷"), 3, 0.95f, 0)
        24 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("TAX", "%", "₪", "+"), 0, 0.92f, 2)
        25 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("₪/u", "100g", "1kg"), 1, 0.94f, 1)
        26 -> ToolAnimSpec(SceneArchetype.CLOCKWORK_TIME_GEARS, arrayOf("UTC", "IST", "EST", "JST"), 1, 0.95f, 2)
        27 -> ToolAnimSpec(SceneArchetype.MEDICAL_HEARTBEAT_ECG, arrayOf("28d", "14d", "✦"), 1, 0.82f, 2)
        28 -> ToolAnimSpec(SceneArchetype.BINARY_CIPHER_RAIN, arrayOf("0x", "FF", "A5", "1C", "B0"), 1, 1.12f, 1)
        29 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("₪", "Save", "+", "%"), 2, 0.95f, 2)
        30 -> ToolAnimSpec(SceneArchetype.CLOCKWORK_TIME_GEARS, arrayOf("365", "DD", "MM", "YY"), 2, 0.85f, 0)
        31 -> ToolAnimSpec(SceneArchetype.RULER_CALIPER_UNITS, arrayOf("SI", "87", "↔", "mm"), 2, 1.05f, 1)
        32 -> ToolAnimSpec(SceneArchetype.FLUID_WATER_BUBBLES, arrayOf("bar", "PSI", "L/m", "DN"), 0, 1.00f, 2)
        33 -> ToolAnimSpec(SceneArchetype.MEDICAL_HEARTBEAT_ECG, arrayOf("mg", "mL", "IV", "gtt"), 2, 0.95f, 1)
        34 -> ToolAnimSpec(SceneArchetype.BLUEPRINT_POLYHEDRA, arrayOf("m³", "m²", "BTU"), 0, 0.92f, 0)
        35 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("Ω", "kΩ", "MΩ", "±1%"), 0, 1.05f, 0)
        36 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("µF", "nF", "pF", "Xc"), 1, 1.00f, 1)
        37 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("AWG", "mm²", "ΔV"), 2, 0.98f, 2)
        38 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("V=IR", "W", "A", "Ω"), 3, 1.12f, 0)
        39 -> ToolAnimSpec(SceneArchetype.RADAR_COMPASS_SWEEP, arrayOf("0.0°", "X", "Y"), 1, 0.88f, 0)
        40 -> ToolAnimSpec(SceneArchetype.RADAR_COMPASS_SWEEP, arrayOf("N", "E", "S", "W", "µT"), 0, 0.95f, 1)
        41 -> ToolAnimSpec(SceneArchetype.RULER_CALIPER_UNITS, arrayOf("90°", "45°", "cm", "mm"), 3, 0.90f, 1)
        42 -> ToolAnimSpec(SceneArchetype.CLOCKWORK_TIME_GEARS, arrayOf("00:00", "ms", "LAP"), 3, 1.20f, 0)
        43 -> ToolAnimSpec(SceneArchetype.PRISM_LIGHT_SPECTRUM, arrayOf("LED", "SOS", "lm"), 0, 1.10f, 2)
        44 -> ToolAnimSpec(SceneArchetype.ATOMIC_BOHR_ORBITS, arrayOf("H", "He", "C", "O", "Au", "Fe"), 0, 1.05f, 0)
        45 -> ToolAnimSpec(SceneArchetype.NETWORK_MESH_SIGNALS, arrayOf("•−", "−•••", "•••", "−−−"), 1, 1.05f, 1)
        46 -> ToolAnimSpec(SceneArchetype.BINARY_CIPHER_RAIN, arrayOf("256b", "#", "*", "!", "?"), 2, 1.08f, 2)
        47 -> ToolAnimSpec(SceneArchetype.TYPOGRAPHY_CONSTELLATION, arrayOf("א", "ב", "ג", "ש", "ת", "10"), 0, 0.90f, 1)
        48 -> ToolAnimSpec(SceneArchetype.FINANCE_CANDLES_TREND, arrayOf("24K", "Au", "Ag", "Pt"), 1, 0.96f, 2)
        49 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("₪", "Spitzer", "30y"), 0, 0.90f, 0)
        50 -> ToolAnimSpec(SceneArchetype.FINANCE_CANDLES_TREND, arrayOf("ROI", "CAGR", "%", "↑"), 2, 1.02f, 1)
        51 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("50%", "30%", "20%", "BEP"), 2, 0.94f, 0)
        52 -> ToolAnimSpec(SceneArchetype.PARABOLA_AND_SLOPE, arrayOf("σ", "µ", "Z", "95%"), 2, 0.92f, 2)
        53 -> ToolAnimSpec(SceneArchetype.BLUEPRINT_POLYHEDRA, arrayOf("a²+b²", "πr²", "V", "S"), 1, 0.98f, 1)
        54 -> ToolAnimSpec(SceneArchetype.NUMBER_SERIES_MATRIX, arrayOf("GCD", "LCM", "n!", "C(n,k)"), 0, 1.00f, 0)
        55 -> ToolAnimSpec(SceneArchetype.CLOUD_FAVORITES_SETTINGS, arrayOf("PDF", "CSV", "Drive"), 1, 0.90f, 1)
        56 -> ToolAnimSpec(SceneArchetype.CLOUD_FAVORITES_SETTINGS, arrayOf("★", "✦", "PIN"), 2, 0.88f, 2)
        57 -> ToolAnimSpec(SceneArchetype.RULER_CALIPER_UNITS, arrayOf("N", "N·m", "m/s²", "J"), 0, 1.08f, 2)
        58 -> ToolAnimSpec(SceneArchetype.FLUID_WATER_BUBBLES, arrayOf("Pa·s", "m³/h", "Re"), 1, 0.96f, 0)
        59 -> ToolAnimSpec(SceneArchetype.THERMO_HEAT_WAVES, arrayOf("W/mK", "BTU", "ΔT", "J"), 2, 1.00f, 1)
        60 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("T", "Wb", "H", "C"), 2, 1.15f, 1)
        61 -> ToolAnimSpec(SceneArchetype.PRISM_LIGHT_SPECTRUM, arrayOf("cd", "lx", "dB", "Sv"), 1, 1.02f, 0)
        62 -> ToolAnimSpec(SceneArchetype.MEDICAL_HEARTBEAT_ECG, arrayOf("mol", "pH", "mg/dL"), 3, 0.94f, 2)
        63 -> ToolAnimSpec(SceneArchetype.TYPOGRAPHY_CONSTELLATION, arrayOf("ly", "AU", "pt", "bps"), 1, 1.02f, 2)
        64 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("נטו", "ברוטו", "125%", "₪"), 3, 0.95f, 1)
        65 -> ToolAnimSpec(SceneArchetype.NUMBER_SERIES_MATRIX, arrayOf("[A]", "det", "A⁻¹", "λ"), 1, 1.00f, 1)
        66 -> ToolAnimSpec(SceneArchetype.RADAR_COMPASS_SWEEP, arrayOf("µT", "EMF", "Fe"), 2, 1.15f, 2)
        67 -> ToolAnimSpec(SceneArchetype.RADAR_COMPASS_SWEEP, arrayOf("Hz", "mm/s", "G"), 3, 1.25f, 0)
        68 -> ToolAnimSpec(SceneArchetype.NETWORK_MESH_SIGNALS, arrayOf("dB", "SOS", "Hz"), 2, 1.32f, 2)
        69 -> ToolAnimSpec(SceneArchetype.CLOCKWORK_TIME_GEARS, arrayOf("BPM", "4/4", "♩", "♪"), 3, 1.15f, 1)
        70 -> ToolAnimSpec(SceneArchetype.MATH_ORBIT_GLYPHS, arrayOf("+1", "+10", "001", "#"), 3, 0.96f, 0)
        71 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("µH", "mH", "XL", "L"), 1, 1.04f, 2)
        72 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("AND", "OR", "XOR", "NOT"), 3, 1.10f, 1)
        73 -> ToolAnimSpec(SceneArchetype.PRISM_LIGHT_SPECTRUM, arrayOf("HEX", "RGB", "HSL", "CMYK"), 2, 0.96f, 0)
        74 -> ToolAnimSpec(SceneArchetype.FRACTION_RATIO_SLICES, arrayOf("16:9", "4K", "PPI", "21:9"), 1, 0.92f, 2)
        75 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("mAh", "80%", "°C", "V"), 0, 0.94f, 0)
        76 -> ToolAnimSpec(SceneArchetype.CIRCUIT_PCB_ELECTRONS, arrayOf("kWh", "W", "₪", "⚡"), 2, 1.08f, 1)
        77 -> ToolAnimSpec(SceneArchetype.BINARY_CIPHER_RAIN, arrayOf("SHA", "MD5", "B64", "AES"), 3, 1.10f, 0)
        78 -> ToolAnimSpec(SceneArchetype.TYPOGRAPHY_CONSTELLATION, arrayOf("x=", "f(x)", "✎", "≡"), 2, 0.88f, 0)
        79 -> ToolAnimSpec(SceneArchetype.RULER_CALIPER_UNITS, arrayOf("Ø", "mm", "US", "EU"), 1, 0.86f, 2)
        80 -> ToolAnimSpec(SceneArchetype.RULER_CALIPER_UNITS, arrayOf("EU", "US", "UK", "cm"), 2, 0.90f, 0)
        81 -> ToolAnimSpec(SceneArchetype.TYPOGRAPHY_CONSTELLATION, arrayOf("Aa", "AZ", "123", "¶"), 3, 0.92f, 1)
        82 -> ToolAnimSpec(SceneArchetype.NUMBER_SERIES_MATRIX, arrayOf("I", "V", "X", "L", "C", "M"), 2, 0.88f, 2)
        83 -> ToolAnimSpec(SceneArchetype.NUMBER_SERIES_MATRIX, arrayOf("aₙ", "Sₙ", "d", "q", "φ"), 3, 1.00f, 0)
        84 -> ToolAnimSpec(SceneArchetype.FRACTION_RATIO_SLICES, arrayOf("A:B", "C:X", "=", "%"), 2, 0.95f, 1)
        85 -> ToolAnimSpec(SceneArchetype.PARABOLA_AND_SLOPE, arrayOf("ax+b", "m", "X", "Y"), 1, 0.98f, 0)
        86 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("72/r", "×2", "₪"), 3, 0.96f, 1)
        87 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("/h", "/d", "/m", "/y"), 0, 0.94f, 2)
        88 -> ToolAnimSpec(SceneArchetype.DISCOUNT_TAX_TAGS, arrayOf("-20%", "-10%", "+TAX"), 1, 0.98f, 0)
        89 -> ToolAnimSpec(SceneArchetype.FINANCE_CANDLES_TREND, arrayOf("DIV", "Yield", "25%", "↑"), 3, 1.04f, 1)
        90 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("FD", "CD", "r%", "₪"), 1, 0.90f, 2)
        91 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("SIP", "CPI", "Real", "₪"), 2, 0.98f, 0)
        92 -> ToolAnimSpec(SceneArchetype.CAR_SPEED_AERO, arrayOf("EMI", "₪/m", "60m", "%"), 3, 1.06f, 1)
        93 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("PV", "FV", "t", "r"), 0, 0.95f, 2)
        94 -> ToolAnimSpec(SceneArchetype.COMPOUND_GROWTH_CURVE, arrayOf("∞", "PMT", "r-g", "PV"), 3, 0.92f, 1)
        95 -> ToolAnimSpec(SceneArchetype.RETIREMENT_HORIZON, arrayOf("67", "4%", "₪", "☀"), 0, 0.85f, 0)
        96 -> ToolAnimSpec(SceneArchetype.PRISM_LIGHT_SPECTRUM, arrayOf("Lux", "fc", "☀"), 3, 0.96f, 1)
        97 -> ToolAnimSpec(SceneArchetype.NETWORK_MESH_SIGNALS, arrayOf("Mbps", "ms", "↓", "↑"), 0, 1.25f, 0)
        98 -> ToolAnimSpec(SceneArchetype.RADAR_COMPASS_SWEEP, arrayOf("Pitch", "Roll", "°", "%"), 1, 0.94f, 2)
        99 -> ToolAnimSpec(SceneArchetype.NETWORK_MESH_SIGNALS, arrayOf("IPv4", "DNS", "GW", "5G"), 3, 1.08f, 1)
        else -> ToolAnimSpec(SceneArchetype.CLOUD_FAVORITES_SETTINGS, arrayOf("✦", "◈"), 0, 0.90f, 0)
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
            "אנימציות ואפקטים: $status"
        } else {
            "✨ אנימציות ואפקטים: $status"
        }
    }

    fun createSettingsToggleButton(activity: MainActivity): TextView {
        return activity.createActionBtn(getSettingsButtonLabel(activity), false) {
            val next = !isCanvasAnimationsEnabled
            setEnabled(activity, next)
            Toast.makeText(
                activity,
                if (next) "✨ אנימציות ואפקטים הופעלו לכל הכלים" else "אנימציות ואפקטים כובו",
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

    fun attachAmbientToContent(activity: MainActivity, contentFrame: FrameLayout, tabIndex: Int) {
        ambientRef?.get()?.pauseAnimator()
        val spec = resolveSpecForTab(tabIndex)
        val ambient = ProAmbientCanvasView(activity, tabIndex, spec).apply {
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

        val themeStyle = resolveThemeStyle(activity)
        if (tabIndex == 0 || tabIndex == 1) {
            animateKeypadEntrance(activity, themeStyle)
        } else {
            animateContentCascade(activity, contentRoot, themeStyle)
        }
    }

    private fun animateKeypadEntrance(activity: MainActivity, style: ThemeRenderStyle) {
        val buttons = activity.keypadButtons.values.toList()
        val stepDelay = when (style) {
            ThemeRenderStyle.MINIMALIST_PASTEL -> 12L
            ThemeRenderStyle.LIQUID_GLASS -> 16L
            ThemeRenderStyle.AURORA_NEON -> 18L
            ThemeRenderStyle.CLASSIC_VECTOR -> 10L
        }
        val dur = when (style) {
            ThemeRenderStyle.MINIMALIST_PASTEL -> 220L
            ThemeRenderStyle.LIQUID_GLASS -> 270L
            ThemeRenderStyle.AURORA_NEON -> 290L
            ThemeRenderStyle.CLASSIC_VECTOR -> 190L
        }
        for ((idx, btn) in buttons.withIndex()) {
            val delayMs = (idx * stepDelay).coerceAtMost(280L)
            btn.alpha = 0f
            when (style) {
                ThemeRenderStyle.MINIMALIST_PASTEL -> {
                    btn.scaleX = 0.90f; btn.scaleY = 0.90f; btn.translationY = activity.dp(8f).toFloat()
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                        .setStartDelay(delayMs).setDuration(dur).setInterpolator(DecelerateInterpolator()).start()
                }
                ThemeRenderStyle.LIQUID_GLASS -> {
                    btn.scaleX = 0.82f; btn.scaleY = 0.82f
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delayMs).setDuration(dur).setInterpolator(OvershootInterpolator(1.25f)).start()
                }
                ThemeRenderStyle.AURORA_NEON -> {
                    btn.scaleX = 0.78f; btn.scaleY = 0.78f; btn.translationY = -activity.dp(10f).toFloat()
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                        .setStartDelay(delayMs).setDuration(dur).setInterpolator(OvershootInterpolator(1.35f)).start()
                }
                ThemeRenderStyle.CLASSIC_VECTOR -> {
                    btn.scaleX = 0.88f; btn.scaleY = 0.88f
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delayMs).setDuration(dur).setInterpolator(DecelerateInterpolator()).start()
                }
            }
        }
    }

    private fun animateContentCascade(activity: MainActivity, root: View, style: ThemeRenderStyle) {
        val targetContainer: ViewGroup? = when (root) {
            is ScrollView -> root.getChildAt(0) as? ViewGroup
            is ViewGroup -> root
            else -> null
        }
        if (targetContainer == null) return
        val maxAnimated = min(targetContainer.childCount, 9)
        val slidePx = activity.dp(if (style == ThemeRenderStyle.MINIMALIST_PASTEL) 14f else 22f).toFloat()
        val xSlidePx = if (style == ThemeRenderStyle.CLASSIC_VECTOR) activity.dp(16f).toFloat() else 0f

        for (i in 0 until maxAnimated) {
            val child = targetContainer.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue
            child.alpha = 0f
            child.translationY = slidePx
            child.translationX = if (i % 2 == 0) xSlidePx else -xSlidePx
            child.animate()
                .alpha(1f)
                .translationY(0f)
                .translationX(0f)
                .setStartDelay(i * 26L)
                .setDuration(if (style == ThemeRenderStyle.AURORA_NEON) 300L else 250L)
                .setInterpolator(if (style == ThemeRenderStyle.LIQUID_GLASS) OvershootInterpolator(0.9f) else DecelerateInterpolator())
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
        overlay.spawnThemedTouchRipple(x, y, isAccent = symbol in listOf("=", "+", "−", "×", "÷", "AC"))
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
        overlay.spawnThemedTouchRipple(x, y, isAccent = true)
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
                .setStartDelay(animIdx * 20L)
                .setDuration(235L)
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
// שכבת אווירה חיה ייחודית לכל כלי (0..99) ומומרת חזותית לכל 4 ערכות הנושא
// =========================================================================
private class ProAmbientCanvasView(
    private val activity: MainActivity,
    private val tabIndex: Int,
    private val spec: ProCanvasAnimations.ToolAnimSpec
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
    private val path = Path()
    private val rect = RectF()

    // זרעים דטרמיניסטיים לפי tabIndex כדי שכל כלי יקבל קומפוזיציה ייחודית ויציבה
    private val rng = Random(tabIndex * 9973 + 31)
    private val seedX = FloatArray(20) { rng.nextFloat() }
    private val seedY = FloatArray(20) { rng.nextFloat() }
    private val seedSpd = FloatArray(20) { 0.45f + rng.nextFloat() * 0.90f }
    private val seedSz = FloatArray(20) { 0.70f + rng.nextFloat() * 0.85f }

    init {
        isClickable = false
        isFocusable = false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean = false

    fun resumeAnimator() {
        if (!ProCanvasAnimations.isCanvasAnimationsEnabled) return
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 16_000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val styleMul = when (ProCanvasAnimations.resolveThemeStyle(activity)) {
                    ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> 0.68f
                    ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> 0.92f
                    ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> 1.22f
                    ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> 1.00f
                }
                phase += 0.015f * spec.speedFactor * styleMul
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

    private fun pickAccent(pal: ProThemeCanvasColors, index: Int): Int {
        return when ((index + spec.accentBias) % 4) {
            0 -> pal.accent1
            1 -> pal.accent2
            2 -> pal.accent3
            else -> pal.positive
        }
    }

    private fun applyThemedStroke(color: Int, widthDp: Float, alpha: Int, style: ProCanvasAnimations.ThemeRenderStyle) {
        strokeP.color = withAlpha(color, alpha)
        strokeP.strokeWidth = when (style) {
            ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> widthDp * 0.82f * d
            ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> widthDp * 0.90f * d
            ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> widthDp * 1.15f * d
            ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> widthDp * 1.00f * d
        }
        if (style == ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON) {
            strokeP.setShadowLayer(6f * d, 0f, 0f, withAlpha(color, (alpha * 1.3f).toInt()))
        } else {
            strokeP.clearShadowLayer()
        }
    }

    // ציור עיגול/אורב מותאם אישית לכל אחת מ-4 ערכות הנושא
    private fun drawThemedOrb(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        r: Float,
        color: Int,
        alpha: Int,
        style: ProCanvasAnimations.ThemeRenderStyle
    ) {
        when (style) {
            ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> {
                fillP.shader = null
                fillP.color = withAlpha(color, (alpha * 0.70f).toInt())
                canvas.drawCircle(cx, cy, r, fillP)
            }
            ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> {
                fillP.shader = null
                fillP.color = withAlpha(color, (alpha * 0.35f).toInt())
                canvas.drawCircle(cx, cy, r, fillP)
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 1.1f * d
                strokeP.color = Color.argb((alpha * 1.1f).toInt().coerceIn(0, 140), 255, 255, 255)
                canvas.drawCircle(cx, cy, r, strokeP)
                // הברקת זכוכית עליונה-שמאלית
                rect.set(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f)
                strokeP.strokeWidth = 1.4f * d
                strokeP.color = Color.argb((alpha * 1.35f).toInt().coerceIn(0, 175), 255, 255, 255)
                canvas.drawArc(rect, 205f, 70f, false, strokeP)
            }
            ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> {
                val safeR = (r * 1.6f).coerceAtLeast(1f)
                fillP.shader = RadialGradient(cx, cy, safeR, withAlpha(color, alpha + 25), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(cx, cy, safeR, fillP)
                fillP.shader = null
                fillP.color = withAlpha(Color.WHITE, (alpha * 0.9f).toInt())
                canvas.drawCircle(cx, cy, r * 0.32f, fillP)
            }
            ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> {
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 1.3f * d
                strokeP.color = withAlpha(color, alpha + 15)
                canvas.drawCircle(cx, cy, r, strokeP)
                fillP.shader = null
                fillP.color = withAlpha(color, alpha)
                canvas.drawCircle(cx, cy, r * 0.28f, fillP)
            }
        }
    }

    // שכבת האווירה הייחודית של ערכת הנושא (מאחורי האנימציה של הכלי)
    private fun drawThemeSignatureBackdrop(
        canvas: Canvas,
        w: Float,
        h: Float,
        pal: ProThemeCanvasColors,
        style: ProCanvasAnimations.ThemeRenderStyle
    ) {
        val c1 = pickAccent(pal, 0)
        val c2 = pickAccent(pal, 1)
        val c3 = pickAccent(pal, 2)

        when (style) {
            ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> {
                // שני כתמי צבעי מים רכים מאוד (Pastel Watercolor Wash)
                val x1 = w * (0.82f + sin(phase * 0.30f) * 0.08f)
                val y1 = h * (0.14f + cos(phase * 0.25f) * 0.04f)
                val r1 = (min(w, h) * 0.40f).coerceAtLeast(1f)
                fillP.shader = RadialGradient(x1, y1, r1, withAlpha(c1, 24), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(x1, y1, r1, fillP)

                val x2 = w * (0.16f + cos(phase * 0.28f) * 0.08f)
                val y2 = h * (0.84f + sin(phase * 0.22f) * 0.04f)
                val r2 = (min(w, h) * 0.36f).coerceAtLeast(1f)
                fillP.shader = RadialGradient(x2, y2, r2, withAlpha(c2, 22), Color.TRANSPARENT, Shader.TileMode.CLAMP)
                canvas.drawCircle(x2, y2, r2, fillP)
                fillP.shader = null
            }

            ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> {
                // פס השתקפות אלכסוני זז + הילת עדשת זכוכית
                val shift = sin(phase * 0.28f) * w * 0.22f
                fillP.shader = LinearGradient(
                    w * 0.15f + shift, 0f, w * 0.55f + shift, h * 0.7f,
                    intArrayOf(Color.TRANSPARENT, Color.argb(24, 255, 255, 255), Color.TRANSPARENT),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, w, h, fillP)
                fillP.shader = null
            }

            ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> {
                // וילון זוהר צפוני נשימתי + כוכבים מנצנצים
                path.reset()
                val baseY = h * 0.20f
                path.moveTo(0f, 0f)
                var x = 0f
                while (x <= w + 24f * d) {
                    val y = baseY + sin(x * 0.008f + phase * 0.65f) * 28f * d + cos(x * 0.015f - phase * 0.4f) * 14f * d
                    path.lineTo(x, y)
                    x += 24f * d
                }
                path.lineTo(w, 0f)
                path.close()
                fillP.shader = LinearGradient(
                    0f, 0f, w, baseY + 40f * d,
                    intArrayOf(withAlpha(c1, 34), withAlpha(c2, 28), withAlpha(c3, 24)),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                canvas.drawPath(path, fillP)
                fillP.shader = null

                for (i in 0 until 10) {
                    val tw = (0.3f + 0.7f * abs(sin(phase * 1.4f + i * 1.7f)))
                    fillP.color = Color.argb((tw * 65f).toInt(), 255, 255, 255)
                    canvas.drawCircle(seedX[i] * w, seedY[i] * h * 0.65f, (1.2f + (i % 2) * 0.9f) * d, fillP)
                }
            }

            ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> {
                // רשת הנדסית מדויקת דקה מאוד + פולס פינתי
                strokeP.clearShadowLayer()
                strokeP.strokeWidth = 0.8f * d
                strokeP.color = withAlpha(c1, 16)
                val step = 44f * d
                val ox = (phase * 6f * d) % step
                var x = ox
                while (x < w) {
                    canvas.drawLine(x, 0f, x, h, strokeP)
                    x += step
                }
                var y = ox
                while (y < h) {
                    canvas.drawLine(0f, y, w, y, strokeP)
                    y += step
                }
            }
        }
    }

    // ציור גליפים ייחודיים של הכלי הנוכחי עם עיצוב מותאם לערכת הנושא
    private fun drawFloatingToolGlyphs(
        canvas: Canvas,
        w: Float,
        h: Float,
        pal: ProThemeCanvasColors,
        style: ProCanvasAnimations.ThemeRenderStyle,
        baseAlpha: Int,
        count: Int = 7
    ) {
        val glyphs = spec.glyphs
        if (glyphs.isEmpty()) return
        val effectiveCount = min(count, 10)
        for (i in 0 until effectiveCount) {
            val g = glyphs[i % glyphs.size]
            val driftX = sin(phase * seedSpd[i] + i * 1.3f) * 24f * d
            val px = ((seedX[i] * w + driftX) % w + w) % w
            val py = ((seedY[i] * h - phase * (12f + spec.variant * 3f) * seedSpd[i] * d) % h + h) % h
            val col = pickAccent(pal, i)
            val txtAlpha = when (style) {
                ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> (baseAlpha * 0.95f).toInt()
                ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> (baseAlpha * 1.05f).toInt()
                ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> (baseAlpha * 1.25f).toInt()
                ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> (baseAlpha * 1.05f).toInt()
            }
            textP.color = withAlpha(if (style == ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS && i % 2 == 0) Color.WHITE else col, txtAlpha)
            textP.textSize = (if (g.length <= 2) 17f else 12.5f) * (0.85f + seedSz[i] * 0.35f) * d
            if (style == ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON) {
                textP.setShadowLayer(5f * d, 0f, 0f, withAlpha(col, txtAlpha))
            } else {
                textP.clearShadowLayer()
            }
            canvas.save()
            canvas.rotate(sin(phase * 0.6f + i) * 14f, px, py)
            canvas.drawText(g, px, py, textP)
            canvas.restore()
        }
        textP.clearShadowLayer()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!ProCanvasAnimations.isCanvasAnimationsEnabled) return
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 2f || h <= 2f) return

        val pal = activity.getProCanvasColors()
        val style = ProCanvasAnimations.resolveThemeStyle(activity)
        val c1 = pickAccent(pal, 0)
        val c2 = pickAccent(pal, 1)
        val c3 = pickAccent(pal, 2)
        val baseAlpha = when (style) {
            ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> 42
            ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> 50
            ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> 62
            ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> 48
        }

        // 1. רקע חתימה ייחודי לערכת הנושא
        drawThemeSignatureBackdrop(canvas, w, h, pal, style)

        // 2. שכבת הגאומטריה הייחודית לכל ארכיטיפ + וריאנט של הכלי
        when (spec.archetype) {
            ProCanvasAnimations.SceneArchetype.MATH_ORBIT_GLYPHS -> {
                val cx = w * 0.5f; val cy = h * 0.52f
                for (ring in 0 until 2) {
                    val rad = min(w, h) * (0.24f + ring * 0.14f)
                    applyThemedStroke(if (ring == 0) c1 else c2, 1.3f, baseAlpha - 6, style)
                    canvas.drawCircle(cx, cy, rad, strokeP)
                    val a = phase * (0.7f - ring * 0.25f) + spec.variant
                    drawThemedOrb(canvas, cx + cos(a) * rad, cy + sin(a) * rad, 6f * d, if (ring == 0) c1 else c3, baseAlpha + 20, style)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 8, 9)
            }

            ProCanvasAnimations.SceneArchetype.TRIG_HARMONIC_WAVES -> {
                for (layer in 0 until 3) {
                    path.reset()
                    val cy = h * (0.26f + layer * 0.23f)
                    val amp = (18f + layer * 9f) * d
                    val freq = 0.010f + layer * 0.004f
                    val spd = phase * (1.1f + layer * 0.4f)
                    var x = 0f
                    while (x <= w + 16f * d) {
                        val y = cy + sin(x * freq + spd) * amp * cos(x * 0.003f - phase * 0.25f)
                        if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                        x += 14f * d
                    }
                    applyThemedStroke(pickAccent(pal, layer), 1.7f + layer * 0.3f, baseAlpha + 14, style)
                    canvas.drawPath(path, strokeP)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 6)
            }

            ProCanvasAnimations.SceneArchetype.PARABOLA_AND_SLOPE -> {
                val cx = w * 0.5f; val cy = h * 0.54f
                applyThemedStroke(c2, 1.1f, baseAlpha - 8, style)
                canvas.drawLine(w * 0.12f, cy, w * 0.88f, cy, strokeP)
                canvas.drawLine(cx, h * 0.20f, cx, h * 0.86f, strokeP)

                path.reset()
                when (spec.variant) {
                    0 -> { // פרבולה נושמת (משוואה ריבועית)
                        val k = (0.0045f + sin(phase * 0.8f) * 0.0018f) / d
                        var x = -w * 0.38f
                        var first = true
                        while (x <= w * 0.38f) {
                            val y = cy + 48f * d - k * x * x
                            if (first) { path.moveTo(cx + x, y); first = false } else path.lineTo(cx + x, y)
                            x += 10f * d
                        }
                        applyThemedStroke(c1, 2.2f, baseAlpha + 22, style)
                        canvas.drawPath(path, strokeP)
                    }
                    1 -> { // ישר מסתובב (משוואה לינארית)
                        val slope = sin(phase * 0.7f) * 0.9f
                        applyThemedStroke(c1, 2.3f, baseAlpha + 22, style)
                        canvas.drawLine(cx - w * 0.38f, cy + w * 0.38f * slope, cx + w * 0.38f, cy - w * 0.38f * slope, strokeP)
                        drawThemedOrb(canvas, cx, cy, 7f * d, c3, baseAlpha + 24, style)
                    }
                    else -> { // עקומת פעמון גאוסיאנית (סטטיסטיקה)
                        val sigma = w * (0.14f + sin(phase * 0.7f) * 0.03f)
                        var x = -w * 0.42f
                        var first = true
                        while (x <= w * 0.42f) {
                            val g = exp(-0.5f * (x / sigma) * (x / sigma))
                            val y = cy + 42f * d - g * 115f * d
                            if (first) { path.moveTo(cx + x, y); first = false } else path.lineTo(cx + x, y)
                            x += 10f * d
                        }
                        applyThemedStroke(c1, 2.2f, baseAlpha + 22, style)
                        canvas.drawPath(path, strokeP)
                    }
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.FRACTION_RATIO_SLICES -> {
                val cx = w * 0.5f; val cy = h * 0.50f
                val rad = min(w, h) * 0.28f
                if (spec.variant == 1) {
                    // מסגרות יחס גובה-רוחב נושמות (16:9 / 4:3)
                    for (k in 0 until 3) {
                        val s = 0.55f + k * 0.22f + sin(phase + k) * 0.04f
                        val rw = rad * 1.45f * s; val rh = rad * 0.85f * s
                        rect.set(cx - rw, cy - rh, cx + rw, cy + rh)
                        applyThemedStroke(pickAccent(pal, k), 1.6f, baseAlpha + 12, style)
                        canvas.drawRoundRect(rect, 10f * d, 10f * d, strokeP)
                    }
                } else {
                    // פרוסות עוגה מסתובבות
                    rect.set(cx - rad, cy - rad, cx + rad, cy + rad)
                    val baseAng = phase * 22f
                    for (s in 0 until 3) {
                        applyThemedStroke(pickAccent(pal, s), 2.2f, baseAlpha + 14, style)
                        canvas.drawArc(rect, baseAng + s * 120f, 92f, true, strokeP)
                    }
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 6)
            }

            ProCanvasAnimations.SceneArchetype.BINARY_CIPHER_RAIN -> {
                if (spec.variant >= 2) {
                    // גלגלי הצפנה ואנטרופיה קונצנטריים
                    val cx = w * 0.5f; val cy = h * 0.48f
                    for (ring in 0 until 3) {
                        val rad = min(w, h) * (0.16f + ring * 0.11f)
                        val startDeg = phase * (if (ring % 2 == 0) 35f else -28f) + ring * 45f
                        rect.set(cx - rad, cy - rad, cx + rad, cy + rad)
                        applyThemedStroke(pickAccent(pal, ring), 2.0f, baseAlpha + 16, style)
                        canvas.drawArc(rect, startDeg, 220f, false, strokeP)
                    }
                } else {
                    // גשם קוד בינארי / הקסדצימלי
                    val cols = 8
                    val colW = w / cols
                    textP.textSize = 12.5f * d
                    for (c in 0 until cols) {
                        val colX = colW * (c + 0.5f)
                        val headY = ((seedY[c] * h + phase * 42f * seedSpd[c] * d) % (h + 110f * d)) - 55f * d
                        for (k in 0 until 5) {
                            val y = headY - k * 23f * d
                            if (y < -20f || y > h + 20f) continue
                            val fade = (5 - k) / 5f
                            textP.color = withAlpha(if (c % 2 == 0) pal.positive else c1, (baseAlpha * 1.45f * fade).toInt())
                            val sym = spec.glyphs[(c + k + (phase * 2).toInt()) % spec.glyphs.size]
                            canvas.drawText(sym, colX, y, textP)
                        }
                    }
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.FINANCE_CANDLES_TREND -> {
                path.reset()
                val baseY = h * 0.72f
                val step = w / 10f
                for (i in 0..10) {
                    val px = i * step
                    val py = baseY - (i * 10f * d) + sin(phase * 1.1f + i * 0.9f + spec.variant) * 24f * d
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    if (i in 1..9) {
                        val candleH = (16f + abs(sin(phase * 0.65f + i)) * 26f) * d
                        val col = if (i % 2 == 0) pal.positive else c2
                        fillP.color = withAlpha(col, baseAlpha)
                        rect.set(px - 4.5f * d, py - candleH * 0.5f, px + 4.5f * d, py + candleH * 0.5f)
                        canvas.drawRoundRect(rect, 3f * d, 3f * d, fillP)
                    }
                }
                applyThemedStroke(c1, 2.0f, baseAlpha + 18, style)
                canvas.drawPath(path, strokeP)
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 6, 6)
            }

            ProCanvasAnimations.SceneArchetype.COMPOUND_GROWTH_CURVE -> {
                path.reset()
                val padX = 18f * d
                val steps = 18
                for (s in 0..steps) {
                    val frac = s.toFloat() / steps
                    val curve = frac * frac * (0.75f + 0.25f * sin(phase * 0.7f))
                    val px = padX + frac * (w - padX * 2f)
                    val py = h * 0.80f - curve * h * 0.44f
                    if (s == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                applyThemedStroke(c1, 2.2f, baseAlpha + 20, style)
                canvas.drawPath(path, strokeP)

                // נקודה מאירה מטפסת לאורך עקומת הצמיחה
                val prog = (phase * 0.30f) % 1f
                val dotX = padX + prog * (w - padX * 2f)
                val dotY = h * 0.80f - (prog * prog * (0.75f + 0.25f * sin(phase * 0.7f))) * h * 0.44f
                drawThemedOrb(canvas, dotX, dotY, 7.5f * d, pal.positive, baseAlpha + 30, style)
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 6)
            }

            ProCanvasAnimations.SceneArchetype.DISCOUNT_TAX_TAGS -> {
                for (i in 0 until 5) {
                    val px = seedX[i] * w * 0.82f + w * 0.09f
                    val py = ((seedY[i] * h + phase * 18f * seedSpd[i] * d) % h + h) % h
                    val bw = (26f + seedSz[i] * 14f) * d
                    val bh = 16f * d
                    canvas.save()
                    canvas.rotate(sin(phase + i) * 18f, px, py)
                    rect.set(px - bw * 0.5f, py - bh * 0.5f, px + bw * 0.5f, py + bh * 0.5f)
                    applyThemedStroke(pickAccent(pal, i), 1.4f, baseAlpha + 10, style)
                    canvas.drawRoundRect(rect, 5f * d, 5f * d, strokeP)
                    canvas.restore()
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 8, 7)
            }

            ProCanvasAnimations.SceneArchetype.RETIREMENT_HORIZON -> {
                val cx = w * 0.5f; val horizonY = h * 0.62f
                val sunR = min(w, h) * 0.20f
                val sunY = horizonY - sunR * 0.35f + sin(phase * 0.6f) * 10f * d
                drawThemedOrb(canvas, cx, sunY, sunR, pal.warning, baseAlpha + 18, style)
                for (layer in 0 until 2) {
                    path.reset()
                    var x = 0f
                    while (x <= w + 16f * d) {
                        val y = horizonY + layer * 24f * d + sin(x * 0.015f + phase * (1.2f + layer * 0.4f)) * 9f * d
                        if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
                        x += 16f * d
                    }
                    applyThemedStroke(if (layer == 0) c1 else c2, 2.0f, baseAlpha + 16, style)
                    canvas.drawPath(path, strokeP)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 4)
            }

            ProCanvasAnimations.SceneArchetype.CAR_SPEED_AERO -> {
                for (i in 0 until 12) {
                    val lineLen = (44f + seedSz[i] * 78f) * d
                    val px = ((seedX[i] * w + phase * 135f * seedSpd[i] * d) % (w + lineLen)) - lineLen
                    val py = seedY[i] * h
                    applyThemedStroke(pickAccent(pal, i), 1.8f, baseAlpha + 12, style)
                    canvas.drawLine(px, py, px + lineLen, py, strokeP)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 4, 5)
            }

            ProCanvasAnimations.SceneArchetype.RULER_CALIPER_UNITS -> {
                val shift = (phase * 20f * d) % (36f * d)
                applyThemedStroke(c1, 1.2f, baseAlpha + 6, style)
                var x = -shift
                var tickIdx = 0
                while (x < w + 36f * d) {
                    val len = if (tickIdx % 4 == 0) 20f * d else 10f * d
                    canvas.drawLine(x, h - len, x, h, strokeP)
                    canvas.drawLine(w - x, 0f, w - x, len, strokeP)
                    x += 18f * d
                    tickIdx++
                }
                if (spec.variant == 1) {
                    // טבעות קונצנטריות (למדידת טבעת / משקלים)
                    val cx = w * 0.5f; val cy = h * 0.52f
                    val rPulse = min(w, h) * (0.20f + sin(phase * 0.8f) * 0.04f)
                    applyThemedStroke(c2, 2.0f, baseAlpha + 14, style)
                    canvas.drawCircle(cx, cy, rPulse, strokeP)
                    canvas.drawCircle(cx, cy, rPulse * 0.70f, strokeP)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 6, 6)
            }

            ProCanvasAnimations.SceneArchetype.THERMO_HEAT_WAVES -> {
                for (col in 0 until 5) {
                    path.reset()
                    val bx = w * (0.14f + col * 0.18f)
                    var y = h * 0.85f
                    var first = true
                    while (y >= h * 0.18f) {
                        val x = bx + sin(y * 0.025f + phase * 1.6f + col) * 12f * d
                        if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                        y -= 14f * d
                    }
                    applyThemedStroke(if (col % 2 == 0) pal.warning else c1, 1.8f, baseAlpha + 12, style)
                    canvas.drawPath(path, strokeP)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 4, 5)
            }

            ProCanvasAnimations.SceneArchetype.MEDICAL_HEARTBEAT_ECG -> {
                path.reset()
                val cy = h * 0.52f
                val cycleW = 130f * d
                val offset = (phase * 56f * d) % cycleW
                var x = -offset - cycleW
                var started = false
                while (x < w + cycleW) {
                    val pts = floatArrayOf(
                        x, cy,
                        x + cycleW * 0.30f, cy,
                        x + cycleW * 0.38f, cy - 11f * d,
                        x + cycleW * 0.44f, cy + 15f * d,
                        x + cycleW * 0.50f, cy - 42f * d,
                        x + cycleW * 0.56f, cy + 22f * d,
                        x + cycleW * 0.64f, cy,
                        x + cycleW, cy
                    )
                    for (j in pts.indices step 2) {
                        if (!started) { path.moveTo(pts[j], pts[j + 1]); started = true }
                        else path.lineTo(pts[j], pts[j + 1])
                    }
                    x += cycleW
                }
                applyThemedStroke(pal.positive, 2.0f, baseAlpha + 22, style)
                canvas.drawPath(path, strokeP)
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.CLOCKWORK_TIME_GEARS -> {
                drawGear(canvas, w * 0.22f, h * 0.30f, 42f * d, 10, phase * 18f, c1, baseAlpha + 10, style)
                drawGear(canvas, w * 0.78f, h * 0.68f, 54f * d, 12, -phase * 14f, c2, baseAlpha + 10, style)
                if (spec.variant == 3) {
                    // מטוטלת BPM / שעון עצר
                    val px = w * 0.5f; val py = h * 0.78f
                    val ang = sin(phase * 2.2f) * 0.55f
                    applyThemedStroke(c3, 2.2f, baseAlpha + 20, style)
                    val tx = px + sin(ang) * 95f * d
                    val ty = py - cos(ang) * 95f * d
                    canvas.drawLine(px, py, tx, ty, strokeP)
                    drawThemedOrb(canvas, tx, ty, 7f * d, c3, baseAlpha + 25, style)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.FLUID_WATER_BUBBLES -> {
                for (layer in 0 until 2) {
                    path.reset()
                    val baseY = h * (0.76f + layer * 0.08f)
                    path.moveTo(0f, h)
                    var x = 0f
                    while (x <= w + 16f * d) {
                        val y = baseY + sin(x * 0.015f + phase * (1.3f + layer * 0.5f)) * (13f + layer * 5f) * d
                        path.lineTo(x, y)
                        x += 16f * d
                    }
                    path.lineTo(w, h)
                    path.close()
                    fillP.color = withAlpha(if (layer == 0) c3 else c1, (baseAlpha * 0.85f).toInt())
                    canvas.drawPath(path, fillP)
                }
                for (i in 0 until 8) {
                    val bx = seedX[i] * w + sin(phase + i) * 12f * d
                    val by = ((seedY[i] * h - phase * 26f * seedSpd[i] * d) % h + h) % h
                    drawThemedOrb(canvas, bx, by, (5f + seedSz[i] * 7f) * d, c3, baseAlpha + 12, style)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.BLUEPRINT_POLYHEDRA -> {
                val cx = w * 0.5f; val cy = h * 0.48f
                val rad = min(w, h) * 0.27f
                applyThemedStroke(c1, 1.6f, baseAlpha + 14, style)
                drawRotatingPolygon(canvas, cx, cy, rad, 6, phase * 0.40f)
                applyThemedStroke(c2, 1.4f, baseAlpha + 10, style)
                drawRotatingPolygon(canvas, cx, cy, rad * 0.62f, if (spec.variant == 0) 4 else 3, -phase * 0.65f)
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.CIRCUIT_PCB_ELECTRONS -> {
                for (i in 0 until 5) {
                    val y = h * (0.16f + i * 0.17f)
                    val bendX = w * (0.26f + (i % 3) * 0.20f)
                    applyThemedStroke(c1, 1.4f, baseAlpha, style)
                    canvas.drawLine(0f, y, bendX, y, strokeP)
                    canvas.drawLine(bendX, y, bendX + 26f * d, y + 20f * d, strokeP)
                    canvas.drawLine(bendX + 26f * d, y + 20f * d, w, y + 20f * d, strokeP)

                    val t = (phase * 0.34f * seedSpd[i] + i * 0.21f) % 1f
                    val ex = t * w
                    val ey = when {
                        ex < bendX -> y
                        ex < bendX + 26f * d -> y + ((ex - bendX) / (26f * d)) * 20f * d
                        else -> y + 20f * d
                    }
                    drawThemedOrb(canvas, ex, ey, 4.5f * d, if (i % 2 == 0) pal.warning else c1, baseAlpha + 40, style)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.RADAR_COMPASS_SWEEP -> {
                val cx = w * 0.5f; val cy = h * 0.46f
                val maxR = min(w, h) * 0.40f
                for (k in 0 until 3) {
                    val prog = (phase * 0.26f + k * 0.33f) % 1f
                    applyThemedStroke(c1, 1.5f, ((1f - prog) * (baseAlpha + 28)).toInt(), style)
                    canvas.drawCircle(cx, cy, prog * maxR, strokeP)
                }
                val sweepAng = phase * 1.25f
                applyThemedStroke(c2, 1.8f, baseAlpha + 24, style)
                canvas.drawLine(cx, cy, cx + cos(sweepAng) * maxR, cy + sin(sweepAng) * maxR, strokeP)
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.PRISM_LIGHT_SPECTRUM -> {
                val originX = w * 0.5f; val originY = -16f * d
                for (i in 0 until 6) {
                    val baseAng = Math.toRadians(55.0 + i * 14.0 + sin(phase * 0.8f + i) * 5.0)
                    val ex = originX + (cos(baseAng) * h * 1.05f).toFloat()
                    val ey = originY + (sin(baseAng) * h * 1.05f).toFloat()
                    applyThemedStroke(pickAccent(pal, i), 10f + i * 1.8f, (baseAlpha * 0.42f).toInt(), style)
                    canvas.drawLine(originX, originY, ex, ey, strokeP)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 6, 5)
            }

            ProCanvasAnimations.SceneArchetype.ATOMIC_BOHR_ORBITS -> {
                val cx = w * 0.5f; val cy = h * 0.48f
                val rx = min(w, h) * 0.34f; val ry = rx * 0.38f
                for (k in 0 until 3) {
                    canvas.save()
                    canvas.rotate(k * 60f, cx, cy)
                    applyThemedStroke(pickAccent(pal, k), 1.5f, baseAlpha + 12, style)
                    rect.set(cx - rx, cy - ry, cx + rx, cy + ry)
                    canvas.drawOval(rect, strokeP)
                    val a = phase * (1.25f + k * 0.25f) + k * 2f
                    drawThemedOrb(canvas, cx + cos(a) * rx, cy + sin(a) * ry, 4.8f * d, c1, baseAlpha + 35, style)
                    canvas.restore()
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 6)
            }

            ProCanvasAnimations.SceneArchetype.NETWORK_MESH_SIGNALS -> {
                val count = 8
                val xs = FloatArray(count)
                val ys = FloatArray(count)
                for (i in 0 until count) {
                    xs[i] = w * (0.14f + seedX[i] * 0.72f) + cos(phase * seedSpd[i] + i) * 20f * d
                    ys[i] = h * (0.14f + seedY[i] * 0.72f) + sin(phase * seedSpd[i] + i) * 20f * d
                }
                for (i in 0 until count) {
                    for (j in i + 1 until count) {
                        val dist = hypot(xs[i] - xs[j], ys[i] - ys[j])
                        if (dist < 170f * d) {
                            applyThemedStroke(c1, 1.2f, ((1f - dist / (170f * d)) * (baseAlpha + 18)).toInt(), style)
                            canvas.drawLine(xs[i], ys[i], xs[j], ys[j], strokeP)
                        }
                    }
                    drawThemedOrb(canvas, xs[i], ys[i], 4.5f * d, pickAccent(pal, i), baseAlpha + 26, style)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha, 5)
            }

            ProCanvasAnimations.SceneArchetype.NUMBER_SERIES_MATRIX -> {
                val bars = 7
                val bw = (w * 0.72f) / bars
                val startX = w * 0.14f
                val botY = h * 0.78f
                for (i in 0 until bars) {
                    val ratio = (0.20f + (i.toFloat() / bars) * 0.65f) * (0.85f + 0.15f * sin(phase * 1.2f + i))
                    val bh = h * 0.34f * ratio
                    val bx = startX + i * bw + bw * 0.15f
                    rect.set(bx, botY - bh, bx + bw * 0.70f, botY)
                    applyThemedStroke(pickAccent(pal, i), 1.6f, baseAlpha + 14, style)
                    canvas.drawRoundRect(rect, 5f * d, 5f * d, strokeP)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 6, 6)
            }

            ProCanvasAnimations.SceneArchetype.TYPOGRAPHY_CONSTELLATION -> {
                for (i in 0 until 7) {
                    val px = seedX[i] * w + sin(phase * seedSpd[i] + i) * 20f * d
                    val py = seedY[i] * h + cos(phase * seedSpd[i] * 0.8f + i) * 20f * d
                    drawThemedOrb(canvas, px, py, (7f + seedSz[i] * 11f) * d, pickAccent(pal, i), baseAlpha, style)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 10, 8)
            }

            ProCanvasAnimations.SceneArchetype.CLOUD_FAVORITES_SETTINGS -> {
                for (i in 0 until 9) {
                    val px = seedX[i] * w + sin(phase * seedSpd[i] + i) * 22f * d
                    val py = seedY[i] * h + cos(phase * seedSpd[i] * 0.8f + i) * 22f * d
                    drawThemedOrb(canvas, px, py, (9f + seedSz[i] * 14f) * d, pickAccent(pal, i), baseAlpha, style)
                }
                drawFloatingToolGlyphs(canvas, w, h, pal, style, baseAlpha + 8, 5)
            }
        }
        strokeP.clearShadowLayer()
    }

    private fun drawGear(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        teeth: Int,
        rotDeg: Float,
        col: Int,
        alpha: Int,
        style: ProCanvasAnimations.ThemeRenderStyle
    ) {
        canvas.save()
        canvas.rotate(rotDeg, cx, cy)
        applyThemedStroke(col, 1.6f, alpha, style)
        canvas.drawCircle(cx, cy, radius * 0.72f, strokeP)
        canvas.drawCircle(cx, cy, radius * 0.28f, strokeP)
        for (i in 0 until teeth) {
            val a = Math.toRadians(i * 360.0 / teeth)
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
        path.reset()
        for (i in 0 until sides) {
            val a = rotRad + (i * 2.0 * PI / sides).toFloat()
            val px = cx + cos(a) * radius
            val py = cy + sin(a) * radius
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()
        canvas.drawPath(path, strokeP)
    }
}

// =========================================================================
// שכבת אפקטים אינטראקטיביים מעל האפליקציה (מותאמת ל-4 ערכות הנושא)
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
    private val reusableRect = RectF()

    init {
        isClickable = false
        isFocusable = false
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean = false

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

    fun spawnThemedTouchRipple(x: Float, y: Float, isAccent: Boolean) {
        if (ripples.size > 10) ripples.removeAt(0)
        ripples.add(RippleWave(x, y, maxR = (if (isAccent) 72f else 52f) * d, isAccent = isAccent))
        resumeAnimator()
    }

    fun spawnCelebrationBurst(cx: Float, cy: Float) {
        val pal = activity.getProCanvasColors()
        val style = ProCanvasAnimations.resolveThemeStyle(activity)
        val colors = intArrayOf(pal.accent1, pal.accent2, pal.accent3, pal.positive, pal.warning)
        val count = when (style) {
            ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> 16
            ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> 22
            ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> 30
            ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> 24
        }
        for (i in 0 until count) {
            val ang = (i.toFloat() / count) * (2f * PI.toFloat()) + Random.nextFloat() * 0.35f
            val speed = (3.4f + Random.nextFloat() * 7.2f) * d
            sparks.add(
                SparkParticle(
                    x = cx,
                    y = cy,
                    vx = cos(ang) * speed,
                    vy = sin(ang) * speed - 2.2f * d,
                    size = (3.2f + Random.nextFloat() * 4.2f) * d,
                    color = if (style == ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS && i % 2 == 0) Color.WHITE else colors[i % colors.size],
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
        val style = ProCanvasAnimations.resolveThemeStyle(activity)

        // 1. אדוות מגע מותאמות ל-4 ערכות הנושא
        for (rip in ripples) {
            val inv = 1f - rip.progress
            val ease = 1f - inv * inv
            val col = if (rip.isAccent) pal.accent1 else pal.accent2
            val alpha = (inv * (if (style == ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL) 115f else 175f)).toInt().coerceIn(0, 255)

            when (style) {
                ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> {
                    // התפשטות צבע מים פסטלית רכה
                    fillP.color = Color.argb((alpha * 0.35f).toInt(), Color.red(col), Color.green(col), Color.blue(col))
                    canvas.drawCircle(rip.x, rip.y, rip.maxR * ease, fillP)
                    strokeP.clearShadowLayer()
                    strokeP.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
                    strokeP.strokeWidth = 1.6f * d
                    canvas.drawCircle(rip.x, rip.y, rip.maxR * ease, strokeP)
                }
                ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> {
                    // טבעת זכוכית שקופה עם הברקה לבנה
                    strokeP.clearShadowLayer()
                    strokeP.color = Color.argb(alpha, 255, 255, 255)
                    strokeP.strokeWidth = (2.4f * inv + 0.8f) * d
                    canvas.drawCircle(rip.x, rip.y, rip.maxR * ease, strokeP)
                    if (rip.progress > 0.14f) {
                        val r2 = rip.maxR * ((rip.progress - 0.14f) / 0.86f) * 0.70f
                        strokeP.color = Color.argb((alpha * 0.65f).toInt(), Color.red(col), Color.green(col), Color.blue(col))
                        canvas.drawCircle(rip.x, rip.y, r2, strokeP)
                    }
                }
                ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> {
                    // טבעות ניאון זוהרות
                    strokeP.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
                    strokeP.strokeWidth = (2.6f * inv + 0.8f) * d
                    strokeP.setShadowLayer(7f * d, 0f, 0f, strokeP.color)
                    canvas.drawCircle(rip.x, rip.y, rip.maxR * ease, strokeP)
                    strokeP.clearShadowLayer()
                }
                ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> {
                    // מעוין וטבעת וקטורית מדויקת
                    strokeP.clearShadowLayer()
                    strokeP.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
                    strokeP.strokeWidth = 2.0f * d
                    val rad = rip.maxR * ease
                    canvas.drawCircle(rip.x, rip.y, rad, strokeP)
                    canvas.drawLine(rip.x - rad * 0.55f, rip.y, rip.x + rad * 0.55f, rip.y, strokeP)
                    canvas.drawLine(rip.x, rip.y - rad * 0.55f, rip.x, rip.y + rad * 0.55f, strokeP)
                }
            }
        }

        // 2. פיצוץ ניצוצות חגיגי בלחיצה על "=" (מותאם לערכת הנושא)
        for (s in sparks) {
            val a = (s.life * 225f).toInt().coerceIn(0, 255)
            fillP.color = Color.argb(a, Color.red(s.color), Color.green(s.color), Color.blue(s.color))
            when (style) {
                ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> {
                    strokeP.clearShadowLayer()
                    strokeP.color = Color.argb(a, 255, 255, 255)
                    strokeP.strokeWidth = 1.4f * d
                    canvas.drawCircle(s.x, s.y, s.size * s.life, fillP)
                    canvas.drawCircle(s.x, s.y, s.size * s.life, strokeP)
                }
                ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> {
                    strokeP.color = fillP.color
                    strokeP.strokeWidth = 1.8f * d
                    strokeP.setShadowLayer(5f * d, 0f, 0f, fillP.color)
                    val arm = s.size * 1.35f * s.life
                    canvas.drawLine(s.x - arm, s.y, s.x + arm, s.y, strokeP)
                    canvas.drawLine(s.x, s.y - arm, s.x, s.y + arm, strokeP)
                    strokeP.clearShadowLayer()
                }
                else -> {
                    if (s.shapeType == 0) {
                        canvas.drawCircle(s.x, s.y, s.size * s.life, fillP)
                    } else {
                        val half = s.size * s.life
                        canvas.drawRect(s.x - half, s.y - half, s.x + half, s.y + half, fillP)
                    }
                }
            }
        }

        // 3. גל ניקוי רוחבי בלחיצה על AC
        if (clearWaveProgress < 1f) {
            val bandW = 90f * d
            val cx = -bandW + clearWaveProgress * (w + bandW * 2f)
            val a = (sin(clearWaveProgress * PI).toFloat() * 110f).toInt().coerceIn(0, 255)
            val waveCol = if (style == ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL) pal.accent3 else pal.accent1
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
            strokeP.clearShadowLayer()
            strokeP.color = Color.argb(a, Color.red(pal.warning), Color.green(pal.warning), Color.blue(pal.warning))
            strokeP.strokeWidth = 2.6f * d
            reusableRect.set(memX - r, memY - r, memX + r, memY + r)
            canvas.drawArc(reusableRect, memoryOrbitProgress * 220f, 140f, false, strokeP)
        }

        // 5. אנימציות מעבר מסכים מותאמות ל-4 ערכות הנושא
        if (transitionProgress < 1f) {
            val fade = sin(transitionProgress * PI).toFloat().coerceIn(0f, 1f)
            when (style) {
                ProCanvasAnimations.ThemeRenderStyle.MINIMALIST_PASTEL -> {
                    val r = transitionProgress * hypot(w, h) * 0.65f
                    strokeP.clearShadowLayer()
                    strokeP.strokeWidth = 18f * d * (1f - transitionProgress)
                    strokeP.color = Color.argb((fade * 65f).toInt(), Color.red(pal.accent1), Color.green(pal.accent1), Color.blue(pal.accent1))
                    canvas.drawCircle(w * 0.5f, h * 0.15f, r, strokeP)
                }
                ProCanvasAnimations.ThemeRenderStyle.LIQUID_GLASS -> {
                    val x = -w * 0.3f + transitionProgress * w * 1.6f
                    fillP.shader = LinearGradient(
                        x - 75f * d, 0f, x + 75f * d, 130f * d,
                        intArrayOf(Color.TRANSPARENT, Color.argb((fade * 80f).toInt(), 255, 255, 255), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, 0f, w, h, fillP)
                    fillP.shader = null
                }
                ProCanvasAnimations.ThemeRenderStyle.AURORA_NEON -> {
                    val y = transitionProgress * h
                    val band = 90f * d
                    fillP.shader = LinearGradient(
                        0f, y - band, 0f, y + band,
                        intArrayOf(Color.TRANSPARENT, Color.argb((fade * 90f).toInt(), Color.red(pal.accent1), Color.green(pal.accent1), Color.blue(pal.accent1)), Color.TRANSPARENT),
                        floatArrayOf(0f, 0.5f, 1f),
                        Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(0f, y - band, w, y + band, fillP)
                    fillP.shader = null
                }
                ProCanvasAnimations.ThemeRenderStyle.CLASSIC_VECTOR -> {
                    val y = transitionProgress * h
                    strokeP.clearShadowLayer()
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
                val alpha = (24 + (sin(drawerPhase * 2f + i) * 18f).toInt()).coerceIn(8, 55)
                fillP.color = Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col))
                canvas.drawCircle(px, py, (2.2f + (i % 3) * 1.4f) * d, fillP)
            }
        }
    }
}

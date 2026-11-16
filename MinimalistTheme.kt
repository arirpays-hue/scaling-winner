package com.example.calculator

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import java.util.Locale
import kotlin.math.*

data class ThemePalettePreset(
    val id: String,
    val name: String,
    val primaryAccent: Int,
    val secondaryAccent: Int,
    val mainBg: Int,
    val cardBg: Int,
    val logoBg: Int,
    val logoText: Int,
    val memKeysBg: Int,
    val numKeysBg: Int,
    val activePillBg: Int,
    val textIconsColor: Int,
    val auroraBg3: Int = Color.parseColor("#1B0F2E")
)

object MinimalistTheme {

    const val MINIMALIST_THEME_NAME = "עיצוב מינימליסטי"

    // =========================================================================
    // צבעי ברירת המחדל המקוריים של "עיצוב מינימליסטי" (Pastel Light / Material You)
    // =========================================================================
    private val DEFAULT_BG_CREAM = Color.parseColor("#FCF9F5")
    private val DEFAULT_DRAWER_HEADER_YELLOW = Color.parseColor("#FFE094")
    private val DEFAULT_TEXT_CHARCOAL = Color.parseColor("#2C2C2C")
    private val DEFAULT_TEXT_MUTED_GREY = Color.parseColor("#424242")
    private val DEFAULT_NEUTRAL_SECONDARY_BG = Color.parseColor("#F1EDE9")
    private val DEFAULT_CARD_WHITE = Color.parseColor("#FFFFFF")
    private val DEFAULT_ACCENT_SAGE_GREEN = Color.parseColor("#A3D5B5")
    private val DEFAULT_ACCENT_MUTED_GREEN = Color.parseColor("#E2F1E7")
    private val DEFAULT_ACCENT_SKY_BLUE = Color.parseColor("#B1D4F9")
    private val DEFAULT_ACCENT_PASTEL_BLUE = Color.parseColor("#D7E9FC")
    private val DEFAULT_ACCENT_MUTED_PEACH = Color.parseColor("#FF9E80")
    private val DEFAULT_ACCENT_TERRACOTTA = Color.parseColor("#FFA48E")
    private val DEFAULT_ACCENT_BUTTER_YELLOW = Color.parseColor("#FFE094")
    private val DEFAULT_ACCENT_WARM_MUSTARD = Color.parseColor("#FFD54F")
    private val DEFAULT_BORDER_SUBTLE = Color.parseColor("#E3DFD9")

    // =========================================================================
    // מערכת 18 ערוצי צבע מותאמים אישית לכל אלמנט, ספרה, תפריט וסמל באפליקציה!
    // =========================================================================
    const val SLOT_ACTION_1 = 0          // 0: רקע כפתורי פעולה 1 (÷ − + וכפתורים ראשיים)
    const val SLOT_ACTION_2 = 1          // 1: רקע כפתורי פעולה 2 (= × וכפתורים משניים)
    const val SLOT_LOGO_BG = 2           // 2: צבע לוגו 1 (רקע באנר לוגו בתפריט הצד)
    const val SLOT_LOGO_TEXT = 3         // 3: צבע לוגו 2 (טקסט וסמל הלוגו בתפריט הצד)
    const val SLOT_MAIN_BG = 4           // 4: צבע רקע ראשי (מסך ותפריט צד)
    const val SLOT_CARDS_INPUTS = 5      // 5: רקע כרטיסים, צג ותיבות קלט
    const val SLOT_MEM_CMD_KEYS = 6      // 6: רקע מקשי זיכרון ופקודות (AC, ⌫, %, MC, MR, M+, M-)
    const val SLOT_NUM_KEYS = 7          // 7: רקע מקשי מספרים (0-9)
    const val SLOT_ACTIVE_PILL = 8       // 8: רקע קפסולת פריט נבחר בתפריט וכותרות Pro
    const val SLOT_TEXT_ICONS = 9        // 9: צבע טקסט כותרות ואייקונים כללי
    const val SLOT_NUM_TEXT = 10         // 10: צבע הספרות עצמן על המקשים (0-9 . ( ))
    const val SLOT_OP_TEXT = 11          // 11: צבע סימוני הפעולות (+ − × ÷ = ופונקציות מדעיות)
    const val SLOT_MEM_CMD_TEXT = 12     // 12: צבע כיתוב מקשי זיכרון ופקודות (AC, ⌫, %, MC..M-)
    const val SLOT_DISPLAY_TEXT = 13     // 13: צבע ספרות הצג הראשי והתוצאות
    const val SLOT_BORDERS = 14          // 14: צבע מסגרות כפתורים וכרטיסים
    const val SLOT_DRAWER_TEXT = 15      // 15: צבע טקסט פריטים וקטגוריות בתפריט הצד
    const val SLOT_DRAWER_ICONS = 16     // 16: צבע סמלים (אייקונים) בתפריט הצד ובסרגל
    const val SLOT_ACTIVE_PILL_TEXT = 17 // 17: צבע טקסט וסמל בתוך פריט נבחר בתפריט הצד
    const val TOTAL_COLOR_SLOTS = 18

    val slotCustomized = BooleanArray(TOTAL_COLOR_SLOTS) { false }
    val slotBaseColors = IntArray(TOTAL_COLOR_SLOTS)
    val slotBrightness = FloatArray(TOTAL_COLOR_SLOTS) { 1.0f }
    val slotOpacity = FloatArray(TOTAL_COLOR_SLOTS) { 1.0f }
    var selectedElementSlot: Int = SLOT_ACTION_1
    var activePaletteId: String = "min_default"

    // =========================================================================
    // 1. 12 פלטות פסטל ייעודיות לערכת הנושא "עיצוב מינימליסטי"
    // =========================================================================
    val minimalistPalettes: List<ThemePalettePreset> = listOf(
        ThemePalettePreset(
            id = "min_default", name = "🌿 מרווה ואפרסק (מקורי)",
            primaryAccent = Color.parseColor("#FFA48E"), secondaryAccent = Color.parseColor("#FFE094"),
            mainBg = Color.parseColor("#FCF9F5"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#FFE094"), logoText = Color.parseColor("#2C2C2C"),
            memKeysBg = Color.parseColor("#B1D4F9"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#A3D5B5"), textIconsColor = Color.parseColor("#2C2C2C")
        ),
        ThemePalettePreset(
            id = "min_sakura", name = "🌸 פריחת הדובדבן (Sakura)",
            primaryAccent = Color.parseColor("#F48FB1"), secondaryAccent = Color.parseColor("#FFCCBC"),
            mainBg = Color.parseColor("#FFF8FA"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#F8BBD0"), logoText = Color.parseColor("#3B232E"),
            memKeysBg = Color.parseColor("#E1BEE7"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#F8BBD0"), textIconsColor = Color.parseColor("#3B232E")
        ),
        ThemePalettePreset(
            id = "min_nordic", name = "🌊 חוף נורדי (Nordic Breeze)",
            primaryAccent = Color.parseColor("#7FB3D5"), secondaryAccent = Color.parseColor("#F9E79F"),
            mainBg = Color.parseColor("#F4F8FB"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#AED6F1"), logoText = Color.parseColor("#1B2A38"),
            memKeysBg = Color.parseColor("#A3E4D7"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#A2D9CE"), textIconsColor = Color.parseColor("#1B2A38")
        ),
        ThemePalettePreset(
            id = "min_citrus", name = "🍋 לימונענע קיצית (Citrus Mint)",
            primaryAccent = Color.parseColor("#FFD54F"), secondaryAccent = Color.parseColor("#FFAB91"),
            mainBg = Color.parseColor("#FEFCF3"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#FFF59D"), logoText = Color.parseColor("#283322"),
            memKeysBg = Color.parseColor("#80CBC4"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#C5E1A5"), textIconsColor = Color.parseColor("#283322")
        ),
        ThemePalettePreset(
            id = "min_lavender", name = "💜 שדות לבנדר (Provence)",
            primaryAccent = Color.parseColor("#B39DDB"), secondaryAccent = Color.parseColor("#F48FB1"),
            mainBg = Color.parseColor("#F8F5FC"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#D1C4E9"), logoText = Color.parseColor("#2A2138"),
            memKeysBg = Color.parseColor("#9FA8DA"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#CE93D8"), textIconsColor = Color.parseColor("#2A2138")
        ),
        ThemePalettePreset(
            id = "min_matcha", name = "🍵 מאצ׳ה יפנית (Matcha Latte)",
            primaryAccent = Color.parseColor("#81C784"), secondaryAccent = Color.parseColor("#DCE775"),
            mainBg = Color.parseColor("#F5F8F2"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#C8E6C9"), logoText = Color.parseColor("#1E2E22"),
            memKeysBg = Color.parseColor("#B2DFDB"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#A5D6A7"), textIconsColor = Color.parseColor("#1E2E22")
        ),
        ThemePalettePreset(
            id = "min_terracotta", name = "🍑 טרקוטה וחול (Desert Sand)",
            primaryAccent = Color.parseColor("#E59866"), secondaryAccent = Color.parseColor("#F8C471"),
            mainBg = Color.parseColor("#FAF3EB"), cardBg = Color.parseColor("#FFFDF9"),
            logoBg = Color.parseColor("#FAD7A0"), logoText = Color.parseColor("#36261E"),
            memKeysBg = Color.parseColor("#F5CBA7"), numKeysBg = Color.parseColor("#FFFDF9"),
            activePillBg = Color.parseColor("#A9DFBF"), textIconsColor = Color.parseColor("#36261E")
        ),
        ThemePalettePreset(
            id = "min_arctic", name = "☁️ ענני קוטב (Arctic Cotton)",
            primaryAccent = Color.parseColor("#90CAF9"), secondaryAccent = Color.parseColor("#80DEEA"),
            mainBg = Color.parseColor("#F7FAFC"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#BBDEFB"), logoText = Color.parseColor("#1E293B"),
            memKeysBg = Color.parseColor("#B3E5FC"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#9FA8DA"), textIconsColor = Color.parseColor("#1E293B")
        ),
        ThemePalettePreset(
            id = "min_berry", name = "🍓 פירות יער ושמנת (Berry Cream)",
            primaryAccent = Color.parseColor("#EF9A9A"), secondaryAccent = Color.parseColor("#FFCC80"),
            mainBg = Color.parseColor("#FDF7F7"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#FFCDD2"), logoText = Color.parseColor("#381F26"),
            memKeysBg = Color.parseColor("#F8BBD0"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#F48FB1"), textIconsColor = Color.parseColor("#381F26")
        ),
        ThemePalettePreset(
            id = "min_sunflower", name = "🌻 חמנייה ופשתן (Sunflower)",
            primaryAccent = Color.parseColor("#FFE082"), secondaryAccent = Color.parseColor("#FFAB91"),
            mainBg = Color.parseColor("#FBF9F2"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#FFECB3"), logoText = Color.parseColor("#2F281E"),
            memKeysBg = Color.parseColor("#C5E1A5"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#FFD54F"), textIconsColor = Color.parseColor("#2F281E")
        ),
        ThemePalettePreset(
            id = "min_pistachio", name = "🥜 פיסטוק וקרמל (Pistachio)",
            primaryAccent = Color.parseColor("#AED581"), secondaryAccent = Color.parseColor("#FFCC80"),
            mainBg = Color.parseColor("#F7F9F4"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#DCEDC8"), logoText = Color.parseColor("#2B2520"),
            memKeysBg = Color.parseColor("#A7FFEB"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#C5E1A5"), textIconsColor = Color.parseColor("#2B2520")
        ),
        ThemePalettePreset(
            id = "min_pearl", name = "🐚 צדף ופנינה (Pearl Shell)",
            primaryAccent = Color.parseColor("#F4A298"), secondaryAccent = Color.parseColor("#F5E1B8"),
            mainBg = Color.parseColor("#F6F6F8"), cardBg = Color.parseColor("#FFFFFF"),
            logoBg = Color.parseColor("#E8ECEF"), logoText = Color.parseColor("#2C2C2C"),
            memKeysBg = Color.parseColor("#B2DFDB"), numKeysBg = Color.parseColor("#FFFFFF"),
            activePillBg = Color.parseColor("#CFD8DC"), textIconsColor = Color.parseColor("#2C2C2C")
        )
    )

    // =========================================================================
    // 2. 12 פלטות זוהר צפוני ייעודיות לערכת הנושא "Aurora Borealis"
    // =========================================================================
    val auroraPalettes: List<ThemePalettePreset> = listOf(
        ThemePalettePreset(
            id = "aur_default", name = "🌌 זוהר ארקטי (Cyan & Emerald)",
            primaryAccent = Color.parseColor("#00F5D4"), secondaryAccent = Color.parseColor("#10B981"),
            mainBg = Color.parseColor("#0A1C1A"), cardBg = Color.parseColor("#07111E"),
            logoBg = Color.parseColor("#0F2830"), logoText = Color.parseColor("#00F5D4"),
            memKeysBg = Color.parseColor("#10B981"), numKeysBg = Color.parseColor("#152632"),
            activePillBg = Color.parseColor("#00F5D4"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#1B0F2E")
        ),
        ThemePalettePreset(
            id = "aur_magenta", name = "🔮 ערפילית סגולה (Cosmic Magenta)",
            primaryAccent = Color.parseColor("#FF4DFF"), secondaryAccent = Color.parseColor("#9D4EDD"),
            mainBg = Color.parseColor("#180A29"), cardBg = Color.parseColor("#11071F"),
            logoBg = Color.parseColor("#291042"), logoText = Color.parseColor("#FF80FF"),
            memKeysBg = Color.parseColor("#C77DFF"), numKeysBg = Color.parseColor("#221238"),
            activePillBg = Color.parseColor("#FF4DFF"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#3C096C")
        ),
        ThemePalettePreset(
            id = "aur_glacier", name = "❄️ קרחון כחול (Glacier Ice)",
            primaryAccent = Color.parseColor("#00E5FF"), secondaryAccent = Color.parseColor("#3A86FF"),
            mainBg = Color.parseColor("#061526"), cardBg = Color.parseColor("#040E1A"),
            logoBg = Color.parseColor("#0B2545"), logoText = Color.parseColor("#00E5FF"),
            memKeysBg = Color.parseColor("#48CAE4"), numKeysBg = Color.parseColor("#10253E"),
            activePillBg = Color.parseColor("#00E5FF"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#1D3557")
        ),
        ThemePalettePreset(
            id = "aur_solar", name = "☄️ שמש חצות (Solar Flare)",
            primaryAccent = Color.parseColor("#FF9E00"), secondaryAccent = Color.parseColor("#FF0055"),
            mainBg = Color.parseColor("#220A16"), cardBg = Color.parseColor("#17060F"),
            logoBg = Color.parseColor("#381022"), logoText = Color.parseColor("#FFB703"),
            memKeysBg = Color.parseColor("#FB5607"), numKeysBg = Color.parseColor("#2E1220"),
            activePillBg = Color.parseColor("#FF9E00"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#3D0814")
        ),
        ThemePalettePreset(
            id = "aur_krypton", name = "🧪 קריפטון ירוק (Bio-Luminescence)",
            primaryAccent = Color.parseColor("#39FF14"), secondaryAccent = Color.parseColor("#00E676"),
            mainBg = Color.parseColor("#071C0E"), cardBg = Color.parseColor("#041208"),
            logoBg = Color.parseColor("#0E3019"), logoText = Color.parseColor("#39FF14"),
            memKeysBg = Color.parseColor("#69F0AE"), numKeysBg = Color.parseColor("#102B18"),
            activePillBg = Color.parseColor("#39FF14"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#082E26")
        ),
        ThemePalettePreset(
            id = "aur_sakura", name = "🌸 סקורה לילי (Neon Sakura)",
            primaryAccent = Color.parseColor("#FF758F"), secondaryAccent = Color.parseColor("#FF4D6D"),
            mainBg = Color.parseColor("#1F0B15"), cardBg = Color.parseColor("#14070E"),
            logoBg = Color.parseColor("#361324"), logoText = Color.parseColor("#FF8FA3"),
            memKeysBg = Color.parseColor("#FFB3C1"), numKeysBg = Color.parseColor("#2B111E"),
            activePillBg = Color.parseColor("#FF758F"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#2D0F38")
        ),
        ThemePalettePreset(
            id = "aur_electric", name = "⚡ פלזמה חשמלית (Electric Indigo)",
            primaryAccent = Color.parseColor("#00BBF9"), secondaryAccent = Color.parseColor("#9B5DE5"),
            mainBg = Color.parseColor("#0E0B26"), cardBg = Color.parseColor("#08061A"),
            logoBg = Color.parseColor("#1B1445"), logoText = Color.parseColor("#00F5D4"),
            memKeysBg = Color.parseColor("#9B5DE5"), numKeysBg = Color.parseColor("#171338"),
            activePillBg = Color.parseColor("#00BBF9"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#280B45")
        ),
        ThemePalettePreset(
            id = "aur_gold", name = "👑 זהב צפוני (Royal Gold Aurora)",
            primaryAccent = Color.parseColor("#FFD700"), secondaryAccent = Color.parseColor("#FFA200"),
            mainBg = Color.parseColor("#1C1507"), cardBg = Color.parseColor("#120E04"),
            logoBg = Color.parseColor("#2E230C"), logoText = Color.parseColor("#FFD700"),
            memKeysBg = Color.parseColor("#FFCA3A"), numKeysBg = Color.parseColor("#261D0B"),
            activePillBg = Color.parseColor("#FFD700"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#24112E")
        ),
        ThemePalettePreset(
            id = "aur_crimson", name = "🩸 זוהר ארגמן (Crimson Aurora)",
            primaryAccent = Color.parseColor("#FF2A54"), secondaryAccent = Color.parseColor("#FF6B6B"),
            mainBg = Color.parseColor("#21070C"), cardBg = Color.parseColor("#140407"),
            logoBg = Color.parseColor("#380C15"), logoText = Color.parseColor("#FF4D6D"),
            memKeysBg = Color.parseColor("#FF758F"), numKeysBg = Color.parseColor("#2E0B13"),
            activePillBg = Color.parseColor("#FF2A54"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#2B0922")
        ),
        ThemePalettePreset(
            id = "aur_turquoise", name = "🐬 לגונה זוהרת (Teal Lagoon)",
            primaryAccent = Color.parseColor("#64DFDF"), secondaryAccent = Color.parseColor("#80FFDB"),
            mainBg = Color.parseColor("#061E24"), cardBg = Color.parseColor("#041418"),
            logoBg = Color.parseColor("#0C323B"), logoText = Color.parseColor("#80FFDB"),
            memKeysBg = Color.parseColor("#56CFE1"), numKeysBg = Color.parseColor("#0E2930"),
            activePillBg = Color.parseColor("#64DFDF"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#141836")
        ),
        ThemePalettePreset(
            id = "aur_ultraviolet", name = "🛸 אולטרה-סגול (Ultraviolet)",
            primaryAccent = Color.parseColor("#E0AAFF"), secondaryAccent = Color.parseColor("#7209B7"),
            mainBg = Color.parseColor("#10002B"), cardBg = Color.parseColor("#09001A"),
            logoBg = Color.parseColor("#240046"), logoText = Color.parseColor("#E0AAFF"),
            memKeysBg = Color.parseColor("#C77DFF"), numKeysBg = Color.parseColor("#1B063B"),
            activePillBg = Color.parseColor("#E0AAFF"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#3C096C")
        ),
        ThemePalettePreset(
            id = "aur_mint_berry", name = "🍧 מנטה ופטל (Cyber Mint Berry)",
            primaryAccent = Color.parseColor("#00F5D4"), secondaryAccent = Color.parseColor("#F15BB5"),
            mainBg = Color.parseColor("#0B1924"), cardBg = Color.parseColor("#170C24"),
            logoBg = Color.parseColor("#211233"), logoText = Color.parseColor("#00F5D4"),
            memKeysBg = Color.parseColor("#F15BB5"), numKeysBg = Color.parseColor("#152331"),
            activePillBg = Color.parseColor("#00F5D4"), textIconsColor = Color.WHITE,
            auroraBg3 = Color.parseColor("#2E0F29")
        )
    )

    // =========================================================================
    // 3. 12 פלטות זכוכית ייעודיות לערכת הנושא "Liquid Glass Pro"
    // =========================================================================
    val liquidGlassPalettes: List<ThemePalettePreset> = listOf(
        ThemePalettePreset(
            id = "gls_default", name = "💎 זכוכית קלאסית (iOS Amber & Blue)",
            primaryAccent = Color.parseColor("#FF9F0A"), secondaryAccent = Color.parseColor("#64B5F6"),
            mainBg = Color.parseColor("#0D111A"), cardBg = Color.parseColor("#1E2638"),
            logoBg = Color.parseColor("#1E2638"), logoText = Color.WHITE,
            memKeysBg = Color.parseColor("#90A4AE"), numKeysBg = Color.parseColor("#2A344A"),
            activePillBg = Color.parseColor("#64B5F6"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_crystal", name = "🧊 זכוכית קפואה (Frosted Cyan)",
            primaryAccent = Color.parseColor("#00E5FF"), secondaryAccent = Color.parseColor("#40C4FF"),
            mainBg = Color.parseColor("#09131C"), cardBg = Color.parseColor("#162838"),
            logoBg = Color.parseColor("#162838"), logoText = Color.parseColor("#E0F7FA"),
            memKeysBg = Color.parseColor("#80DEEA"), numKeysBg = Color.parseColor("#1F3547"),
            activePillBg = Color.parseColor("#00E5FF"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_ruby", name = "🍷 זכוכית רובי (Ruby Quartz)",
            primaryAccent = Color.parseColor("#FF4D6D"), secondaryAccent = Color.parseColor("#FF758F"),
            mainBg = Color.parseColor("#180A10"), cardBg = Color.parseColor("#2E1520"),
            logoBg = Color.parseColor("#2E1520"), logoText = Color.parseColor("#FFCCD5"),
            memKeysBg = Color.parseColor("#F48FB1"), numKeysBg = Color.parseColor("#3A1B28"),
            activePillBg = Color.parseColor("#FF4D6D"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_emerald", name = "🌿 זכוכית אמרלד (Emerald Jade)",
            primaryAccent = Color.parseColor("#00E676"), secondaryAccent = Color.parseColor("#69F0AE"),
            mainBg = Color.parseColor("#081610"), cardBg = Color.parseColor("#142E22"),
            logoBg = Color.parseColor("#142E22"), logoText = Color.parseColor("#B9F6CA"),
            memKeysBg = Color.parseColor("#80CBC4"), numKeysBg = Color.parseColor("#1C3B2D"),
            activePillBg = Color.parseColor("#00E676"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_amethyst", name = "🔮 זכוכית אמטיסט (Amethyst Pro)",
            primaryAccent = Color.parseColor("#B388FF"), secondaryAccent = Color.parseColor("#EA80FC"),
            mainBg = Color.parseColor("#110C1D"), cardBg = Color.parseColor("#241A3B"),
            logoBg = Color.parseColor("#241A3B"), logoText = Color.parseColor("#EDE7F6"),
            memKeysBg = Color.parseColor("#CE93D8"), numKeysBg = Color.parseColor("#30234D"),
            activePillBg = Color.parseColor("#B388FF"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_champagne", name = "🥂 זכוכית שמפניה (Champagne Gold)",
            primaryAccent = Color.parseColor("#FFD54F"), secondaryAccent = Color.parseColor("#FFB300"),
            mainBg = Color.parseColor("#17140B"), cardBg = Color.parseColor("#2E2817"),
            logoBg = Color.parseColor("#2E2817"), logoText = Color.parseColor("#FFF8E1"),
            memKeysBg = Color.parseColor("#FFE082"), numKeysBg = Color.parseColor("#3B331E"),
            activePillBg = Color.parseColor("#FFD54F"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_titanium", name = "🌑 טיטניום כהה (Natural Titanium)",
            primaryAccent = Color.parseColor("#90A4AE"), secondaryAccent = Color.parseColor("#B0BEC5"),
            mainBg = Color.parseColor("#0A0C0E"), cardBg = Color.parseColor("#1C2026"),
            logoBg = Color.parseColor("#1C2026"), logoText = Color.parseColor("#ECEFF1"),
            memKeysBg = Color.parseColor("#78909C"), numKeysBg = Color.parseColor("#262B33"),
            activePillBg = Color.parseColor("#B0BEC5"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_sapphire", name = "🌊 זכוכית ספיר (Deep Sapphire)",
            primaryAccent = Color.parseColor("#448AFF"), secondaryAccent = Color.parseColor("#82B1FF"),
            mainBg = Color.parseColor("#070E1E"), cardBg = Color.parseColor("#142240"),
            logoBg = Color.parseColor("#142240"), logoText = Color.parseColor("#E3F2FD"),
            memKeysBg = Color.parseColor("#90CAF9"), numKeysBg = Color.parseColor("#1D3059"),
            activePillBg = Color.parseColor("#448AFF"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_rosegold", name = "🍑 זכוכית רוז-גולד (Rose Gold)",
            primaryAccent = Color.parseColor("#FF80AB"), secondaryAccent = Color.parseColor("#FFAB91"),
            mainBg = Color.parseColor("#1A0E14"), cardBg = Color.parseColor("#331C28"),
            logoBg = Color.parseColor("#331C28"), logoText = Color.parseColor("#FCE4EC"),
            memKeysBg = Color.parseColor("#F8BBD0"), numKeysBg = Color.parseColor("#422534"),
            activePillBg = Color.parseColor("#FF80AB"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_cyber", name = "⚡ זכוכית סייבר (Cyber Neon)",
            primaryAccent = Color.parseColor("#18FFFF"), secondaryAccent = Color.parseColor("#FF4081"),
            mainBg = Color.parseColor("#090C18"), cardBg = Color.parseColor("#161D36"),
            logoBg = Color.parseColor("#161D36"), logoText = Color.parseColor("#18FFFF"),
            memKeysBg = Color.parseColor("#FF80AB"), numKeysBg = Color.parseColor("#1F2847"),
            activePillBg = Color.parseColor("#18FFFF"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_sunset", name = "🌅 זכוכית שקיעה (Sunset Horizon)",
            primaryAccent = Color.parseColor("#FF6E40"), secondaryAccent = Color.parseColor("#FFAB40"),
            mainBg = Color.parseColor("#1A0D0A"), cardBg = Color.parseColor("#361B15"),
            logoBg = Color.parseColor("#361B15"), logoText = Color.parseColor("#FBE9E7"),
            memKeysBg = Color.parseColor("#FFCCBC"), numKeysBg = Color.parseColor("#45231B"),
            activePillBg = Color.parseColor("#FF6E40"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "gls_mint", name = "🍃 זכוכית מנטה (Arctic Mint Glass)",
            primaryAccent = Color.parseColor("#64FFDA"), secondaryAccent = Color.parseColor("#1DE9B6"),
            mainBg = Color.parseColor("#071715"), cardBg = Color.parseColor("#12302C"),
            logoBg = Color.parseColor("#12302C"), logoText = Color.parseColor("#E0F2F1"),
            memKeysBg = Color.parseColor("#80CBC4"), numKeysBg = Color.parseColor("#1A403B"),
            activePillBg = Color.parseColor("#64FFDA"), textIconsColor = Color.WHITE
        )
    )

    // =========================================================================
    // 4. 12 פלטות קלאסיות ייעודיות לערכת הנושא "עיצוב רגיל (קלאסי)"
    // =========================================================================
    val classicPalettes: List<ThemePalettePreset> = listOf(
        ThemePalettePreset(
            id = "cls_default", name = "🔵 כחול קלאסי (Classic Pro)",
            primaryAccent = Color.parseColor("#64B5F6"), secondaryAccent = Color.parseColor("#FF9F0A"),
            mainBg = Color.parseColor("#08090C"), cardBg = Color.parseColor("#14171F"),
            logoBg = Color.parseColor("#14171F"), logoText = Color.WHITE,
            memKeysBg = Color.parseColor("#1E2430"), numKeysBg = Color.parseColor("#171A24"),
            activePillBg = Color.parseColor("#64B5F6"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_oled", name = "🌑 שחור מוחלט (Pure OLED)",
            primaryAccent = Color.parseColor("#00E5FF"), secondaryAccent = Color.parseColor("#00B0FF"),
            mainBg = Color.parseColor("#000000"), cardBg = Color.parseColor("#0D0D0D"),
            logoBg = Color.parseColor("#0D0D0D"), logoText = Color.parseColor("#00E5FF"),
            memKeysBg = Color.parseColor("#161616"), numKeysBg = Color.parseColor("#0F0F0F"),
            activePillBg = Color.parseColor("#00E5FF"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_matrix", name = "💻 מטריקס ירוק (Matrix Code)",
            primaryAccent = Color.parseColor("#00E676"), secondaryAccent = Color.parseColor("#76FF03"),
            mainBg = Color.parseColor("#040B06"), cardBg = Color.parseColor("#0B190F"),
            logoBg = Color.parseColor("#0B190F"), logoText = Color.parseColor("#00E676"),
            memKeysBg = Color.parseColor("#122B18"), numKeysBg = Color.parseColor("#0D1F12"),
            activePillBg = Color.parseColor("#00E676"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_cyberpunk", name = "⚡ סייברפאנק 2077 (Night City)",
            primaryAccent = Color.parseColor("#FCEE09"), secondaryAccent = Color.parseColor("#00F0FF"),
            mainBg = Color.parseColor("#0D0914"), cardBg = Color.parseColor("#1B122B"),
            logoBg = Color.parseColor("#FCEE09"), logoText = Color.BLACK,
            memKeysBg = Color.parseColor("#FF003C"), numKeysBg = Color.parseColor("#1F1530"),
            activePillBg = Color.parseColor("#00F0FF"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_dracula", name = "🧛 דרקולה כהה (Dracula IDE)",
            primaryAccent = Color.parseColor("#BD93F9"), secondaryAccent = Color.parseColor("#FF79C6"),
            mainBg = Color.parseColor("#1E1F29"), cardBg = Color.parseColor("#282A36"),
            logoBg = Color.parseColor("#282A36"), logoText = Color.parseColor("#F8F8F2"),
            memKeysBg = Color.parseColor("#44475A"), numKeysBg = Color.parseColor("#2D303E"),
            activePillBg = Color.parseColor("#BD93F9"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_gold", name = "👑 זהב יוקרתי (Luxury Gold)",
            primaryAccent = Color.parseColor("#FFD700"), secondaryAccent = Color.parseColor("#FFA000"),
            mainBg = Color.parseColor("#0C0A06"), cardBg = Color.parseColor("#1C170E"),
            logoBg = Color.parseColor("#1C170E"), logoText = Color.parseColor("#FFD700"),
            memKeysBg = Color.parseColor("#2B2315"), numKeysBg = Color.parseColor("#1F1910"),
            activePillBg = Color.parseColor("#FFD700"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_crimson", name = "🔥 אדום אש (Crimson Flame)",
            primaryAccent = Color.parseColor("#FF5252"), secondaryAccent = Color.parseColor("#FF1744"),
            mainBg = Color.parseColor("#0F0607"), cardBg = Color.parseColor("#210D10"),
            logoBg = Color.parseColor("#210D10"), logoText = Color.parseColor("#FF8A80"),
            memKeysBg = Color.parseColor("#331419"), numKeysBg = Color.parseColor("#260F13"),
            activePillBg = Color.parseColor("#FF5252"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_ocean", name = "🌊 אוקיינוס עמוק (Deep Ocean)",
            primaryAccent = Color.parseColor("#26C6DA"), secondaryAccent = Color.parseColor("#00ACC1"),
            mainBg = Color.parseColor("#051014"), cardBg = Color.parseColor("#0D222B"),
            logoBg = Color.parseColor("#0D222B"), logoText = Color.parseColor("#80DEEA"),
            memKeysBg = Color.parseColor("#143340"), numKeysBg = Color.parseColor("#0F2833"),
            activePillBg = Color.parseColor("#26C6DA"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_sunset", name = "🌇 כתום שקיעה (Sunset Orange)",
            primaryAccent = Color.parseColor("#FF9100"), secondaryAccent = Color.parseColor("#FF3D00"),
            mainBg = Color.parseColor("#120905"), cardBg = Color.parseColor("#26140C"),
            logoBg = Color.parseColor("#26140C"), logoText = Color.parseColor("#FFD180"),
            memKeysBg = Color.parseColor("#381E12"), numKeysBg = Color.parseColor("#2B170E"),
            activePillBg = Color.parseColor("#FF9100"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_synthwave", name = "🕹️ רטרו סינת׳ווייב (Synthwave 80s)",
            primaryAccent = Color.parseColor("#F72585"), secondaryAccent = Color.parseColor("#4CC9F0"),
            mainBg = Color.parseColor("#100720"), cardBg = Color.parseColor("#210F3D"),
            logoBg = Color.parseColor("#210F3D"), logoText = Color.parseColor("#4CC9F0"),
            memKeysBg = Color.parseColor("#3A0CA3"), numKeysBg = Color.parseColor("#28124A"),
            activePillBg = Color.parseColor("#F72585"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_emerald", name = "🌲 יער אזמרגד (Emerald Forest)",
            primaryAccent = Color.parseColor("#69F0AE"), secondaryAccent = Color.parseColor("#00E676"),
            mainBg = Color.parseColor("#06120C"), cardBg = Color.parseColor("#0E261B"),
            logoBg = Color.parseColor("#0E261B"), logoText = Color.parseColor("#B9F6CA"),
            memKeysBg = Color.parseColor("#173B2A"), numKeysBg = Color.parseColor("#112E20"),
            activePillBg = Color.parseColor("#69F0AE"), textIconsColor = Color.WHITE
        ),
        ThemePalettePreset(
            id = "cls_monochrome", name = "♟️ מונוכרום גרפיט (Graphite Mono)",
            primaryAccent = Color.parseColor("#E0E0E0"), secondaryAccent = Color.parseColor("#9E9E9E"),
            mainBg = Color.parseColor("#0B0C0E"), cardBg = Color.parseColor("#1A1C20"),
            logoBg = Color.parseColor("#1A1C20"), logoText = Color.WHITE,
            memKeysBg = Color.parseColor("#2C2F36"), numKeysBg = Color.parseColor("#202227"),
            activePillBg = Color.parseColor("#E0E0E0"), textIconsColor = Color.WHITE
        )
    )

    init {
        resetSlotsArrayToDefaults()
    }

    private fun resetSlotsArrayToDefaults() {
        val defaults = getDefaultSlotColors()
        for (i in 0 until TOTAL_COLOR_SLOTS) {
            slotCustomized[i] = false
            slotBaseColors[i] = defaults[i]
            slotBrightness[i] = 1.0f
            slotOpacity[i] = 1.0f
        }
    }

    private fun getDefaultSlotColors(): IntArray = intArrayOf(
        DEFAULT_ACCENT_TERRACOTTA,     // 0: SLOT_ACTION_1 (רקע כפתורי פעולה 1)
        DEFAULT_ACCENT_BUTTER_YELLOW,  // 1: SLOT_ACTION_2 (רקע כפתורי פעולה 2)
        DEFAULT_DRAWER_HEADER_YELLOW,  // 2: SLOT_LOGO_BG (רקע לוגו)
        DEFAULT_TEXT_CHARCOAL,         // 3: SLOT_LOGO_TEXT (טקסט וסמל לוגו)
        DEFAULT_BG_CREAM,              // 4: SLOT_MAIN_BG (רקע ראשי)
        DEFAULT_CARD_WHITE,            // 5: SLOT_CARDS_INPUTS (כרטיסים ותיבות קלט)
        DEFAULT_ACCENT_SKY_BLUE,       // 6: SLOT_MEM_CMD_KEYS (רקע מקשי זיכרון ופקודות)
        DEFAULT_CARD_WHITE,            // 7: SLOT_NUM_KEYS (רקע מקשי מספרים)
        DEFAULT_ACCENT_SAGE_GREEN,     // 8: SLOT_ACTIVE_PILL (רקע פריט נבחר בתפריט וכותרות)
        DEFAULT_TEXT_CHARCOAL,         // 9: SLOT_TEXT_ICONS (טקסט כותרות ואייקונים כללי)
        DEFAULT_TEXT_CHARCOAL,         // 10: SLOT_NUM_TEXT (צבע הספרות עצמן 0-9)
        DEFAULT_TEXT_CHARCOAL,         // 11: SLOT_OP_TEXT (צבע סימוני הפעולות + - × ÷ =)
        DEFAULT_TEXT_CHARCOAL,         // 12: SLOT_MEM_CMD_TEXT (צבע כיתוב זיכרון ופקודות)
        DEFAULT_TEXT_CHARCOAL,         // 13: SLOT_DISPLAY_TEXT (צבע ספרות הצג והתוצאות)
        DEFAULT_BORDER_SUBTLE,         // 14: SLOT_BORDERS (צבע מסגרות כפתורים וכרטיסים)
        DEFAULT_TEXT_CHARCOAL,         // 15: SLOT_DRAWER_TEXT (צבע טקסט תפריט הצד)
        DEFAULT_TEXT_CHARCOAL,         // 16: SLOT_DRAWER_ICONS (צבע סמלים בתפריט הצד ובסרגל)
        DEFAULT_TEXT_CHARCOAL          // 17: SLOT_ACTIVE_PILL_TEXT (צבע טקסט וסמל בפריט נבחר בתפריט)
    )

    // =========================================================================
    // מנוע הגנת ניגודיות חכם (Smart Contrast Guard) - מונע היעלמות במקשים ובתפריט הצד!
    // =========================================================================
    fun computeLuminance(color: Int): Double {
        fun chan(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * chan(Color.red(color)) + 0.7152 * chan(Color.green(color)) + 0.0722 * chan(Color.blue(color))
    }

    fun computeContrastRatio(fg: Int, bg: Int): Double {
        val l1 = computeLuminance(fg)
        val l2 = computeLuminance(bg)
        return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05)
    }

    fun ensureReadableTextOnBg(desiredTextCol: Int, bgCol: Int, minContrast: Double = 2.40): Int {
        if (Color.alpha(bgCol) < 70) return desiredTextCol
        val ratio = computeContrastRatio(desiredTextCol, bgCol)
        if (ratio >= minContrast) return desiredTextCol
        val bgLum = computeLuminance(bgCol)
        return if (bgLum > 0.38) Color.parseColor("#14171F") else Color.WHITE
    }

    // מחשב את צבע הרקע האמיתי של תפריט הצד
    fun resolveDrawerBgColor(activity: MainActivity): Int {
        return when {
            activity.isMinimalistMode -> DRAWER_BG
            slotCustomized[SLOT_MAIN_BG] -> getSlotEffectiveColor(SLOT_MAIN_BG, activity.bgColor)
            activity.isAuroraMode -> AuroraBorealisTheme.ARCTIC_BLUE
            else -> Utils.lightenColor(activity.bgColor, 0.06f)
        }
    }

    // מחשב את צבע הרקע האמיתי של קפסולת הפריט הנבחר בתפריט הצד (כדי לבדוק ניגודיות מולו!)
    fun resolveActivePillBgColor(activity: MainActivity): Int {
        return when {
            activity.isMinimalistMode -> ACCENT_SAGE_GREEN
            slotCustomized[SLOT_ACTIVE_PILL] -> getSlotEffectiveColor(SLOT_ACTIVE_PILL, activity.primaryColor)
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }
    }

    // מחשב את צבע הטקסט של פריט או קטגוריה בתפריט הצד (נבחר / פתוח / רגיל) עם הגנת ניגודיות מלאה!
    fun resolveDrawerItemTextColor(
        activity: MainActivity,
        isSelected: Boolean,
        isExpandedHeader: Boolean = false
    ): Int {
        val drawerBg = resolveDrawerBgColor(activity)
        if (isSelected) {
            val pillBg = resolveActivePillBgColor(activity)
            if (slotCustomized[SLOT_ACTIVE_PILL_TEXT]) {
                val chosen = getSlotEffectiveColor(SLOT_ACTIVE_PILL_TEXT, DEFAULT_TEXT_CHARCOAL)
                return ensureReadableTextOnBg(chosen, pillBg, minContrast = 1.65)
            }
            val defaultActiveText = when {
                activity.isMinimalistMode -> DEFAULT_TEXT_CHARCOAL
                else -> ensureReadableTextOnBg(Color.WHITE, pillBg, minContrast = 2.60)
            }
            return ensureReadableTextOnBg(defaultActiveText, pillBg, minContrast = 2.60)
        }

        val itemBg = if (isExpandedHeader && activity.isMinimalistMode) NEUTRAL_SECONDARY_BG else drawerBg
        if (slotCustomized[SLOT_DRAWER_TEXT]) {
            val chosen = getSlotEffectiveColor(SLOT_DRAWER_TEXT, DEFAULT_TEXT_CHARCOAL)
            return ensureReadableTextOnBg(chosen, itemBg, minContrast = 1.65)
        }
        if (slotCustomized[SLOT_TEXT_ICONS]) {
            return ensureReadableTextOnBg(TEXT_CHARCOAL, itemBg, minContrast = 2.35)
        }

        val defaultText = when {
            activity.isMinimalistMode -> DEFAULT_TEXT_CHARCOAL
            isExpandedHeader && activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            isExpandedHeader -> activity.primaryColor
            else -> Color.WHITE
        }
        return ensureReadableTextOnBg(defaultText, itemBg, minContrast = 2.40)
    }

    // מחשב את צבע הסמל (האייקון) בתפריט הצד עם הגנת ניגודיות מול הקפסולה הפעילה או רקע התפריט!
    fun resolveDrawerIconColor(
        activity: MainActivity,
        isSelected: Boolean,
        isExpandedHeader: Boolean = false
    ): Int {
        val drawerBg = resolveDrawerBgColor(activity)
        if (isSelected) {
            val pillBg = resolveActivePillBgColor(activity)
            if (slotCustomized[SLOT_ACTIVE_PILL_TEXT]) {
                val chosen = getSlotEffectiveColor(SLOT_ACTIVE_PILL_TEXT, DEFAULT_TEXT_CHARCOAL)
                return ensureReadableTextOnBg(chosen, pillBg, minContrast = 1.65)
            }
            if (slotCustomized[SLOT_DRAWER_ICONS]) {
                val chosen = getSlotEffectiveColor(SLOT_DRAWER_ICONS, DEFAULT_TEXT_CHARCOAL)
                return ensureReadableTextOnBg(chosen, pillBg, minContrast = 2.40)
            }
            val defaultActiveIcon = when {
                activity.isMinimalistMode -> DEFAULT_TEXT_CHARCOAL
                else -> ensureReadableTextOnBg(Color.WHITE, pillBg, minContrast = 2.60)
            }
            return ensureReadableTextOnBg(defaultActiveIcon, pillBg, minContrast = 2.60)
        }

        val itemBg = if (isExpandedHeader && activity.isMinimalistMode) NEUTRAL_SECONDARY_BG else drawerBg
        if (slotCustomized[SLOT_DRAWER_ICONS]) {
            val chosen = getSlotEffectiveColor(SLOT_DRAWER_ICONS, DEFAULT_TEXT_CHARCOAL)
            return ensureReadableTextOnBg(chosen, itemBg, minContrast = 1.65)
        }
        if (slotCustomized[SLOT_DRAWER_TEXT]) {
            val chosen = getSlotEffectiveColor(SLOT_DRAWER_TEXT, DEFAULT_TEXT_CHARCOAL)
            return ensureReadableTextOnBg(chosen, itemBg, minContrast = 2.20)
        }
        if (slotCustomized[SLOT_TEXT_ICONS]) {
            return ensureReadableTextOnBg(TEXT_CHARCOAL, itemBg, minContrast = 2.35)
        }

        val defaultIcon = when {
            activity.isMinimalistMode -> DEFAULT_TEXT_CHARCOAL
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            isExpandedHeader -> activity.primaryColor
            else -> Color.WHITE
        }
        return ensureReadableTextOnBg(defaultIcon, itemBg, minContrast = 2.35)
    }

    fun resolveTopHeaderIconColor(activity: MainActivity): Int {
        val headerBg = if (activity.isMinimalistMode) getBackgroundColor(activity) else activity.bgColor
        if (slotCustomized[SLOT_DRAWER_ICONS]) {
            val chosen = getSlotEffectiveColor(SLOT_DRAWER_ICONS, DEFAULT_TEXT_CHARCOAL)
            return ensureReadableTextOnBg(chosen, headerBg, minContrast = 1.85)
        }
        if (activity.isMinimalistMode || slotCustomized[SLOT_TEXT_ICONS]) {
            return ensureReadableTextOnBg(TEXT_CHARCOAL, headerBg, minContrast = 2.30)
        }
        return when {
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            activity.isIosGlassMode -> activity.primaryColor
            else -> Color.WHITE
        }
    }

    // מחשב את צבע הרקע המדויק של מקש במקלדת
    fun resolveKeypadButtonBgColor(activity: MainActivity, symbol: String): Int {
        val isMem = symbol in listOf("MC", "MR", "M+", "M-")
        val isCmd = symbol in listOf("AC", "⌫", "%") || isMem
        val isOp1 = symbol in listOf("÷", "−", "-", "+", "sin", "cos", "tan", "log", "ln", "√", "x²", "x³", "x^y", "!", "π", "e")
        val isOp2 = symbol in listOf("=", "×")

        if (activity.isMinimalistMode) {
            val action1Col = getPrimaryActionColor(activity)
            val action2Col = getSecondaryActionColor(activity)
            return when (symbol) {
                "MC" -> PILL_MC_BLUE
                "MR" -> PILL_MR_MINT
                "M+" -> PILL_MPLUS_LIME
                "M-" -> PILL_MMINUS_YELLOW
                "AC" -> ACCENT_SKY_BLUE
                "⌫", "%" -> ACCENT_PASTEL_BLUE
                "÷" -> if (slotCustomized[SLOT_ACTION_1]) action1Col else Color.parseColor("#F9B487")
                "−", "-" -> if (slotCustomized[SLOT_ACTION_1]) action1Col else DEFAULT_ACCENT_TERRACOTTA
                "+" -> if (slotCustomized[SLOT_ACTION_1]) action1Col else DEFAULT_ACCENT_TERRACOTTA
                "×" -> if (slotCustomized[SLOT_ACTION_2]) action2Col else if (slotCustomized[SLOT_ACTION_1]) action1Col else DEFAULT_ACCENT_MUTED_PEACH
                "=" -> if (slotCustomized[SLOT_ACTION_2]) action2Col else Color.parseColor("#F48484")
                else -> PILL_NUM_WHITE
            }
        }

        return when {
            symbol == "AC" -> if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_SKY_BLUE else Utils.lightenColor(Color.RED, 0.2f)
            isOp2 -> if (slotCustomized[SLOT_ACTION_2]) getSecondaryActionColor(activity) else activity.primaryColor
            isCmd -> if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_PASTEL_BLUE else Utils.lightenColor(activity.bgColor, 0.10f)
            isOp1 -> if (slotCustomized[SLOT_ACTION_1]) getPrimaryActionColor(activity) else Utils.lightenColor(activity.bgColor, 0.10f)
            else -> if (slotCustomized[SLOT_NUM_KEYS]) PILL_NUM_WHITE else Utils.lightenColor(activity.bgColor, 0.06f)
        }
    }

    fun resolveKeypadSymbolTextColor(activity: MainActivity, symbol: String, buttonBgColor: Int? = null): Int {
        val bgCol = buttonBgColor ?: resolveKeypadButtonBgColor(activity, symbol)
        val isMem = symbol in listOf("MC", "MR", "M+", "M-")
        val isCmd = symbol in listOf("AC", "⌫", "%") || isMem
        val isOperatorOrSci = symbol in listOf(
            "÷", "×", "−", "-", "+", "=",
            "sin", "cos", "tan", "log", "ln", "√", "x²", "x³", "x^y", "!", "π", "e"
        )

        if (isCmd && slotCustomized[SLOT_MEM_CMD_TEXT]) {
            val chosen = getSlotEffectiveColor(SLOT_MEM_CMD_TEXT, DEFAULT_TEXT_CHARCOAL)
            return ensureReadableTextOnBg(chosen, bgCol, minContrast = 1.45)
        }
        if (isOperatorOrSci && slotCustomized[SLOT_OP_TEXT]) {
            val chosen = getSlotEffectiveColor(SLOT_OP_TEXT, DEFAULT_TEXT_CHARCOAL)
            return ensureReadableTextOnBg(chosen, bgCol, minContrast = 1.45)
        }
        if (!isCmd && !isOperatorOrSci && slotCustomized[SLOT_NUM_TEXT]) {
            val chosen = getSlotEffectiveColor(SLOT_NUM_TEXT, DEFAULT_TEXT_CHARCOAL)
            return ensureReadableTextOnBg(chosen, bgCol, minContrast = 1.45)
        }

        if (slotCustomized[SLOT_TEXT_ICONS]) {
            val generalText = TEXT_CHARCOAL
            return ensureReadableTextOnBg(generalText, bgCol, minContrast = 2.35)
        }

        val candidate = when {
            activity.isMinimalistMode -> DEFAULT_TEXT_CHARCOAL
            activity.isAuroraMode -> {
                when {
                    symbol in listOf("÷", "×", "−", "-", "+", "=") -> AuroraBorealisTheme.EMERALD_NIGHT
                    isCmd -> AuroraBorealisTheme.AURORA_PRIMARY
                    else -> Color.WHITE
                }
            }
            activity.isIosGlassMode -> Color.WHITE
            else -> {
                when {
                    symbol == "AC" -> Color.WHITE
                    symbol == "=" -> Color.parseColor("#08090C")
                    isCmd || isOperatorOrSci -> activity.primaryColor
                    else -> Color.WHITE
                }
            }
        }
        return ensureReadableTextOnBg(candidate, bgCol, minContrast = 2.45)
    }

    fun resolveDisplayTextColor(activity: MainActivity): Int {
        if (slotCustomized[SLOT_DISPLAY_TEXT]) {
            return getSlotEffectiveColor(SLOT_DISPLAY_TEXT, DEFAULT_TEXT_CHARCOAL)
        }
        if (activity.isMinimalistMode || slotCustomized[SLOT_TEXT_ICONS]) {
            return ensureReadableTextOnBg(TEXT_CHARCOAL, getBackgroundColor(activity), minContrast = 2.2)
        }
        return Color.WHITE
    }

    fun resolveBorderStrokeColor(activity: MainActivity, defaultColor: Int): Int {
        if (slotCustomized[SLOT_BORDERS]) {
            return getSlotEffectiveColor(SLOT_BORDERS, defaultColor)
        }
        return defaultColor
    }

    fun getPalettesForCurrentTheme(activity: MainActivity): List<ThemePalettePreset> {
        return when {
            activity.isMinimalistMode -> minimalistPalettes
            activity.isAuroraMode -> auroraPalettes
            activity.isIosGlassMode -> liquidGlassPalettes
            else -> classicPalettes
        }
    }

    fun applyThemePalettePreset(activity: MainActivity, preset: ThemePalettePreset) {
        activePaletteId = preset.id

        val autoNumText = ensureReadableTextOnBg(preset.textIconsColor, preset.numKeysBg, 2.5)
        val autoOpText = ensureReadableTextOnBg(preset.textIconsColor, preset.primaryAccent, 2.5)
        val autoMemText = ensureReadableTextOnBg(preset.textIconsColor, preset.memKeysBg, 2.5)
        val autoDisplayText = ensureReadableTextOnBg(preset.textIconsColor, preset.mainBg, 2.5)
        val autoBorder = if (activity.isMinimalistMode) DEFAULT_BORDER_SUBTLE else preset.primaryAccent
        val autoDrawerText = ensureReadableTextOnBg(preset.textIconsColor, preset.mainBg, 2.5)
        val autoDrawerIcons = ensureReadableTextOnBg(
            if (activity.isMinimalistMode) preset.textIconsColor else preset.primaryAccent,
            preset.mainBg,
            2.4
        )
        val autoActivePillText = ensureReadableTextOnBg(preset.textIconsColor, preset.activePillBg, 2.6)

        val colors = intArrayOf(
            preset.primaryAccent,   // 0: SLOT_ACTION_1
            preset.secondaryAccent, // 1: SLOT_ACTION_2
            preset.logoBg,          // 2: SLOT_LOGO_BG
            preset.logoText,        // 3: SLOT_LOGO_TEXT
            preset.mainBg,          // 4: SLOT_MAIN_BG
            preset.cardBg,          // 5: SLOT_CARDS_INPUTS
            preset.memKeysBg,       // 6: SLOT_MEM_CMD_KEYS
            preset.numKeysBg,       // 7: SLOT_NUM_KEYS
            preset.activePillBg,    // 8: SLOT_ACTIVE_PILL
            preset.textIconsColor,  // 9: SLOT_TEXT_ICONS
            autoNumText,            // 10: SLOT_NUM_TEXT
            autoOpText,             // 11: SLOT_OP_TEXT
            autoMemText,            // 12: SLOT_MEM_CMD_TEXT
            autoDisplayText,        // 13: SLOT_DISPLAY_TEXT
            autoBorder,             // 14: SLOT_BORDERS
            autoDrawerText,         // 15: SLOT_DRAWER_TEXT
            autoDrawerIcons,        // 16: SLOT_DRAWER_ICONS
            autoActivePillText      // 17: SLOT_ACTIVE_PILL_TEXT
        )
        for (i in 0 until TOTAL_COLOR_SLOTS) {
            slotCustomized[i] = (i in 0..9)
            slotBaseColors[i] = colors[i]
            slotBrightness[i] = 1.0f
            slotOpacity[i] = 1.0f
        }

        activity.primaryColor = preset.primaryAccent
        activity.bgColor = preset.mainBg

        if (activity.isAuroraMode) {
            AuroraBorealisTheme.AURORA_PRIMARY = preset.primaryAccent
            AuroraBorealisTheme.AURORA_SECONDARY = preset.secondaryAccent
            AuroraBorealisTheme.EMERALD_NIGHT = preset.mainBg
            AuroraBorealisTheme.ARCTIC_BLUE = preset.cardBg
            AuroraBorealisTheme.COSMIC_PURPLE = preset.auroraBg3
            AuroraBorealisTheme.saveAuroraPrefs(activity.prefs)
        } else if (activity.isIosGlassMode) {
            IosGlassTheme.useClassicOrangeOps = (preset.id == "gls_default")
            IosGlassTheme.saveIosPrefs(activity.prefs)
        }

        saveMinimalistPrefs(activity.prefs)
        activity.savePreferences()
        activity.applyTheme()
    }

    // =========================================================================
    // בונה את סרגל הפלטות הדינמי בראש מסך ההגדרות (מותאם אישית לכל ערכת נושא!)
    // =========================================================================
    fun createThemeMatchedPalettesBar(activity: MainActivity): View {
        val isMin = activity.isMinimalistMode
        val palettes = getPalettesForCurrentTheme(activity)

        val wrapper = LinearLayout(activity).apply {
            tag = "THEME_MATCHED_PALETTES_BAR"
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }

        val headerTitle = when {
            activity.isMinimalistMode -> "🎨 12 פלטות פסטל מותאמות לעיצוב מינימליסטי"
            activity.isAuroraMode -> "🌌 12 פלטות זוהר צפוני מותאמות ל-Aurora Borealis"
            activity.isIosGlassMode -> "💎 12 פלטות זכוכית מותאמות ל-Liquid Glass Pro"
            else -> "🎨 12 פלטות צבעים מותאמות לעיצוב קלאסי"
        }

        val titleTv = TextView(activity).apply {
            text = headerTitle
            setTextColor(if (isMin) TEXT_CHARCOAL else Color.WHITE)
            textSize = 15.5f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(4f), activity.dp(4f), activity.dp(4f), activity.dp(8f))
        }
        wrapper.addView(titleTv)

        val hScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
        }
        val row = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.dp(2f), activity.dp(2f), activity.dp(2f), activity.dp(2f))
        }

        for (preset in palettes) {
            val isSelected = (activePaletteId == preset.id)
            val chipBgColor = if (isMin) preset.activePillBg else preset.cardBg
            val readableTextCol = ensureReadableTextOnBg(
                if (isMin) Color.parseColor("#242424") else Color.WHITE,
                chipBgColor,
                2.5
            )

            val chip = TextView(activity).apply {
                tag = "PRO_STAR_CHIP"
                text = if (isSelected) "✓ ${preset.name}" else preset.name
                setTextColor(readableTextCol)
                textSize = 13.5f
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif-medium", Typeface.NORMAL)
                gravity = Gravity.CENTER
                setPadding(activity.dp(16f), activity.dp(10f), activity.dp(16f), activity.dp(10f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(999f).toFloat()
                    setColor(chipBgColor)
                    val strokeW = activity.dp(if (isSelected) 2.2f else 1.4f)
                    val strokeCol = if (isSelected && isMin) Color.parseColor("#2C2C2C") else preset.primaryAccent
                    setStroke(strokeW, strokeCol)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(activity.dp(4f), activity.dp(2f), activity.dp(4f), activity.dp(2f)) }
                setOnClickListener {
                    activity.feedback(it)
                    applyThemePalettePreset(activity, preset)
                }
            }
            row.addView(chip)
        }

        hScroll.addView(row)
        wrapper.addView(hScroll)
        return wrapper
    }

    fun getSlotEffectiveColor(slot: Int, fallback: Int = getDefaultSlotColors()[slot]): Int {
        if (slot !in 0 until TOTAL_COLOR_SLOTS || !slotCustomized[slot]) return fallback
        val allowAlpha = (slot != SLOT_MAIN_BG)
        return applyBrightnessAndOpacity(
            slotBaseColors[slot],
            slotBrightness[slot],
            if (allowAlpha) slotOpacity[slot] else 1.0f
        )
    }

    val BG_CREAM: Int get() = getSlotEffectiveColor(SLOT_MAIN_BG, DEFAULT_BG_CREAM)
    val DRAWER_BG: Int get() = getSlotEffectiveColor(SLOT_MAIN_BG, DEFAULT_BG_CREAM)
    val DRAWER_HEADER_YELLOW: Int get() = getSlotEffectiveColor(SLOT_LOGO_BG, DEFAULT_DRAWER_HEADER_YELLOW)
    val LOGO_TEXT_COLOR: Int get() = getSlotEffectiveColor(SLOT_LOGO_TEXT, DEFAULT_TEXT_CHARCOAL)
    val TEXT_CHARCOAL: Int get() = getSlotEffectiveColor(SLOT_TEXT_ICONS, DEFAULT_TEXT_CHARCOAL)
    val TEXT_MUTED_GREY: Int get() = if (slotCustomized[SLOT_TEXT_ICONS]) {
        val c = getSlotEffectiveColor(SLOT_TEXT_ICONS, DEFAULT_TEXT_CHARCOAL)
        Color.argb(185, Color.red(c), Color.green(c), Color.blue(c))
    } else DEFAULT_TEXT_MUTED_GREY
    val NEUTRAL_SECONDARY_BG: Int get() = if (slotCustomized[SLOT_CARDS_INPUTS]) {
        Utils.lightenColor(getSlotEffectiveColor(SLOT_CARDS_INPUTS, DEFAULT_CARD_WHITE), -0.05f)
    } else DEFAULT_NEUTRAL_SECONDARY_BG
    val CARD_WHITE: Int get() = getSlotEffectiveColor(SLOT_CARDS_INPUTS, DEFAULT_CARD_WHITE)

    val ACCENT_SAGE_GREEN: Int get() = getSlotEffectiveColor(SLOT_ACTIVE_PILL, DEFAULT_ACCENT_SAGE_GREEN)
    val ACCENT_MUTED_GREEN: Int get() = if (slotCustomized[SLOT_ACTIVE_PILL]) {
        Utils.lightenColor(getSlotEffectiveColor(SLOT_ACTIVE_PILL, DEFAULT_ACCENT_SAGE_GREEN), 0.35f)
    } else DEFAULT_ACCENT_MUTED_GREEN
    val ACCENT_SKY_BLUE: Int get() = getSlotEffectiveColor(SLOT_MEM_CMD_KEYS, DEFAULT_ACCENT_SKY_BLUE)
    val ACCENT_PASTEL_BLUE: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) {
        Utils.lightenColor(getSlotEffectiveColor(SLOT_MEM_CMD_KEYS, DEFAULT_ACCENT_SKY_BLUE), 0.28f)
    } else DEFAULT_ACCENT_PASTEL_BLUE
    val ACCENT_MUTED_PEACH: Int get() = getSlotEffectiveColor(SLOT_ACTION_1, DEFAULT_ACCENT_MUTED_PEACH)
    val ACCENT_TERRACOTTA: Int get() = getSlotEffectiveColor(SLOT_ACTION_1, DEFAULT_ACCENT_TERRACOTTA)
    val ACCENT_BUTTER_YELLOW: Int get() = getSlotEffectiveColor(SLOT_ACTION_2, DEFAULT_ACCENT_BUTTER_YELLOW)
    val ACCENT_WARM_MUSTARD: Int get() = DEFAULT_ACCENT_WARM_MUSTARD

    val TEXT_DARK: Int get() = TEXT_CHARCOAL
    val TEXT_MUTED: Int get() = TEXT_MUTED_GREY
    val HEADER_PISTACHIO_CARD: Int get() = ACCENT_MUTED_GREEN
    val HEADER_STAR_PILL: Int get() = ACCENT_SAGE_GREEN
    val COMPASS_MINT_CARD: Int get() = ACCENT_MUTED_GREEN
    val COMPASS_DIAL_RING_BG: Int get() = Color.parseColor("#F4F1EA")
    val ALERT_PINK_PILL: Int get() = Color.parseColor("#F9DCD8")
    val ALERT_RED_TEXT: Int get() = Color.parseColor("#B9382C")
    val ACTION_CORAL_PRIMARY: Int get() = ACCENT_TERRACOTTA
    val SWAP_YELLOW_PILL: Int get() = ACCENT_BUTTER_YELLOW
    val PILL_MC_BLUE: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_SKY_BLUE else Color.parseColor("#CBE4F0")
    val PILL_MR_MINT: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_PASTEL_BLUE else DEFAULT_ACCENT_SAGE_GREEN
    val PILL_MPLUS_LIME: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_SKY_BLUE else Color.parseColor("#D4E8B0")
    val PILL_MMINUS_YELLOW: Int get() = if (slotCustomized[SLOT_MEM_CMD_KEYS]) ACCENT_PASTEL_BLUE else DEFAULT_ACCENT_BUTTER_YELLOW
    val PILL_AC_PERIWINKLE: Int get() = ACCENT_SKY_BLUE
    val PILL_CMD_LAVENDER: Int get() = ACCENT_PASTEL_BLUE
    val PILL_NUM_WHITE: Int get() = getSlotEffectiveColor(SLOT_NUM_KEYS, DEFAULT_CARD_WHITE)

    var pillCornerPt: Float = 24f
    var shadowSoftness: Int = 0
    var useCustomAccentTint: Boolean
        get() = slotCustomized[SLOT_ACTION_1]
        set(value) { slotCustomized[SLOT_ACTION_1] = value }
    var useCustomBgTint: Boolean
        get() = slotCustomized[SLOT_MAIN_BG]
        set(value) { slotCustomized[SLOT_MAIN_BG] = value }
    var warmPaperTintMode: Int = 0

    fun loadMinimalistPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        pillCornerPt = prefs.getFloat("min_pill_corner", 24f)
        shadowSoftness = prefs.getInt("min_shadow_softness", 0)
        warmPaperTintMode = prefs.getInt("min_paper_tint_mode", 0)
        selectedElementSlot = prefs.getInt("min_selected_slot", SLOT_ACTION_1).coerceIn(0, TOTAL_COLOR_SLOTS - 1)
        activePaletteId = prefs.getString("min_active_palette_id", "min_default") ?: "min_default"
        val defaults = getDefaultSlotColors()
        for (i in 0 until TOTAL_COLOR_SLOTS) {
            slotCustomized[i] = prefs.getBoolean("min_slot_cust_$i", false)
            slotBaseColors[i] = prefs.getInt("min_slot_col_$i", defaults[i])
            slotBrightness[i] = prefs.getFloat("min_slot_brt_$i", 1.0f).coerceIn(0.20f, 1.0f)
            slotOpacity[i] = prefs.getFloat("min_slot_opc_$i", 1.0f).coerceIn(0.25f, 1.0f)
        }
    }

    fun saveMinimalistPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        val ed = prefs.edit()
            .putFloat("min_pill_corner", pillCornerPt)
            .putInt("min_shadow_softness", shadowSoftness)
            .putInt("min_paper_tint_mode", warmPaperTintMode)
            .putInt("min_selected_slot", selectedElementSlot)
            .putString("min_active_palette_id", activePaletteId)
        for (i in 0 until TOTAL_COLOR_SLOTS) {
            ed.putBoolean("min_slot_cust_$i", slotCustomized[i])
            ed.putInt("min_slot_col_$i", slotBaseColors[i])
            ed.putFloat("min_slot_brt_$i", slotBrightness[i])
            ed.putFloat("min_slot_opc_$i", slotOpacity[i])
        }
        ed.apply()
    }

    private fun applyBrightnessAndOpacity(baseColor: Int, brightness: Float, opacity: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        hsv[2] = (hsv[2] * brightness).coerceIn(0.12f, 1.0f)
        val alpha = (opacity * 255f).toInt().coerceIn(65, 255)
        return Color.HSVToColor(alpha, hsv)
    }

    fun getBackgroundColor(activity: MainActivity? = null): Int {
        if (slotCustomized[SLOT_MAIN_BG]) {
            return getSlotEffectiveColor(SLOT_MAIN_BG, DEFAULT_BG_CREAM)
        }
        if (activity != null && !activity.isMinimalistMode) {
            return activity.bgColor
        }
        return when (warmPaperTintMode) {
            1 -> Color.parseColor("#FFFFFF")
            2 -> Color.parseColor("#F2F7F4")
            else -> DEFAULT_BG_CREAM
        }
    }

    fun getPrimaryActionColor(activity: MainActivity): Int {
        if (slotCustomized[SLOT_ACTION_1]) {
            return getSlotEffectiveColor(SLOT_ACTION_1, DEFAULT_ACCENT_TERRACOTTA)
        }
        return if (activity.isMinimalistMode) DEFAULT_ACCENT_TERRACOTTA else activity.primaryColor
    }

    fun getSecondaryActionColor(activity: MainActivity): Int {
        if (slotCustomized[SLOT_ACTION_2]) {
            return getSlotEffectiveColor(SLOT_ACTION_2, DEFAULT_ACCENT_BUTTER_YELLOW)
        }
        return if (activity.isMinimalistMode) Color.parseColor("#F48484") else activity.primaryColor
    }

    fun showUnifiedDisplayModeDialog(activity: MainActivity) {
        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = activity.isMinimalistMode
        val cardBgCol = if (isMin) BG_CREAM else Color.parseColor("#1E222B")
        val titleCol = if (isMin) TEXT_CHARCOAL else Color.WHITE
        val dividerCol = if (isMin) Color.argb(28, 44, 44, 44) else Color.argb(35, 255, 255, 255)

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20f), activity.dp(22f), activity.dp(20f), activity.dp(18f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(cardBgCol)
                setStroke(activity.dp(1.2f), if (isMin) Color.parseColor("#E0DBD3") else Color.argb(60, 255, 255, 255))
            }
        }

        val titleTv = TextView(activity).apply {
            text = "בחר מצב תצוגה"
            setTextColor(titleCol)
            textSize = 20f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(6f), 0, activity.dp(6f), activity.dp(16f))
        }
        root.addView(titleTv)

        val modes = listOf(
            Triple(0, "🎨 עיצוב רגיל (קלאסי)", Color.parseColor("#FF9F0A")),
            Triple(1, "💎 עיצוב הזכוכית (Liquid Glass Pro)", Color.parseColor("#64B5F6")),
            Triple(2, "🌌 הזוהר הצפוני (Aurora Borealis)", AuroraBorealisTheme.AURORA_PRIMARY),
            Triple(3, "🌿 עיצוב מינימליסטי (Pastel Light)", ACCENT_SAGE_GREEN)
        )

        val currentModeIdx = when {
            activity.isMinimalistMode -> 3
            activity.isAuroraMode -> 2
            activity.isIosGlassMode -> 1
            else -> 0
        }

        for ((idx, label, highlightCol) in modes) {
            val isSelected = (idx == currentModeIdx)
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(activity.dp(14f), activity.dp(14f), activity.dp(14f), activity.dp(14f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(16f).toFloat()
                    if (isSelected) {
                        val fillCol = if (isMin) ACCENT_SAGE_GREEN else Color.argb(65, Color.red(highlightCol), Color.green(highlightCol), Color.blue(highlightCol))
                        setColor(fillCol)
                    } else {
                        setColor(Color.TRANSPARENT)
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    activity.feedback(it)
                    activity.isMinimalistMode = (idx == 3)
                    activity.isAuroraMode = (idx == 2)
                    activity.isIosGlassMode = (idx == 1)
                    if (activity.isMinimalistMode) {
                        activity.buttonCornerStyle = pillCornerPt
                    }
                    val defaultPreset = getPalettesForCurrentTheme(activity).first()
                    dlg.dismiss()
                    applyThemePalettePreset(activity, defaultPreset)
                }
            }

            val radioView = createCustomRadioCircleView(activity, isSelected, isMin, highlightCol)
            val labelTv = TextView(activity).apply {
                text = label
                setTextColor(
                    if (isSelected && !isMin) highlightCol
                    else if (isMin) TEXT_CHARCOAL
                    else Color.WHITE
                )
                textSize = 16.5f
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif", Typeface.NORMAL)
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            row.addView(radioView)
            row.addView(labelTv)
            root.addView(row)

            if (idx < modes.lastIndex) {
                val div = View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(1f)).apply {
                        setMargins(activity.dp(8f), activity.dp(2f), activity.dp(8f), activity.dp(2f))
                    }
                    setBackgroundColor(dividerCol)
                }
                root.addView(div)
            }
        }

        dlg.setContentView(root)
        dlg.show()
        val widthPx = (activity.resources.displayMetrics.widthPixels * 0.90f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    fun showUnifiedSoundPickerDialog(activity: MainActivity) {
        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = activity.isMinimalistMode
        val cardBgCol = if (isMin) BG_CREAM else Color.parseColor("#1E222B")
        val titleCol = if (isMin) TEXT_CHARCOAL else Color.WHITE
        val dividerCol = if (isMin) Color.argb(28, 44, 44, 44) else Color.argb(35, 255, 255, 255)
        val activeAccent = when {
            isMin -> TEXT_CHARCOAL
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }

        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20f), activity.dp(22f), activity.dp(20f), activity.dp(18f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(cardBgCol)
                setStroke(activity.dp(1.2f), if (isMin) Color.parseColor("#E0DBD3") else Color.argb(60, 255, 255, 255))
            }
        }

        val titleTv = TextView(activity).apply {
            text = "בחר סוג צליל הקשה"
            setTextColor(titleCol)
            textSize = 20f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(6f), 0, activity.dp(6f), activity.dp(16f))
        }
        root.addView(titleTv)

        val soundOptions = listOf(
            0 to "🔊 מערכת (צליל אנדרואיד תקני)",
            1 to "🎵 צליל 1 (קליק מכני עדין)",
            2 to "🎵 צליל 2 (הקשת מעבדה)",
            3 to "🎵 צליל 3 (טון דיגיטלי)"
        )

        for ((sCode, sLabel) in soundOptions) {
            val isSelected = (activity.soundType == sCode)
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(activity.dp(14f), activity.dp(14f), activity.dp(14f), activity.dp(14f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(999f).toFloat()
                    if (isSelected) {
                        val fillCol = if (isMin) ACCENT_SAGE_GREEN else Color.argb(65, Color.red(activeAccent), Color.green(activeAccent), Color.blue(activeAccent))
                        setColor(fillCol)
                    } else {
                        setColor(if (isMin) CARD_WHITE else Color.TRANSPARENT)
                        if (isMin) setStroke(activity.dp(1f), Color.parseColor("#EAE5DE"))
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = activity.dp(6f) }
                setOnClickListener {
                    activity.soundType = sCode
                    activity.useSound = true
                    activity.savePreferences()
                    activity.feedback(it)
                    dlg.dismiss()
                    activity.switchTab(19)
                }
            }

            val radioView = createCustomRadioCircleView(activity, isSelected, isMin, activeAccent)
            val labelTv = TextView(activity).apply {
                text = sLabel
                setTextColor(
                    if (isMin) TEXT_CHARCOAL
                    else if (isSelected) activeAccent
                    else Color.WHITE
                )
                textSize = 16.5f
                typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif", Typeface.NORMAL)
                gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            row.addView(radioView)
            row.addView(labelTv)
            root.addView(row)

            if (!isMin && sCode < soundOptions.lastIndex) {
                val div = View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, activity.dp(1f)).apply {
                        setMargins(activity.dp(8f), 0, activity.dp(8f), activity.dp(4f))
                    }
                    setBackgroundColor(dividerCol)
                }
                root.addView(div)
            }
        }

        dlg.setContentView(root)
        dlg.show()
        val widthPx = (activity.resources.displayMetrics.widthPixels * 0.90f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    fun showUnifiedPrecisionPickerDialog(activity: MainActivity) {
        val options = listOf(2, 4, 6, 8, 10, 12)
        val labels = options.map { "$it ספרות אחרי הנקודה" }
        activity.showItemPicker("בחר דיוק עשרוני", labels) { chosen ->
            val num = chosen.substringBefore(" ").toIntOrNull() ?: 8
            activity.calcPrecision = num
            activity.savePreferences()
            activity.switchTab(19)
        }
    }

    private fun createCustomRadioCircleView(
        activity: MainActivity,
        isSelected: Boolean,
        isMin: Boolean,
        highlightCol: Int
    ): View {
        return object : View(activity) {
            private val d = resources.displayMetrics.density
            private val outerP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.2f * d
                color = if (isMin) TEXT_CHARCOAL else if (isSelected) highlightCol else Color.WHITE
            }
            private val innerP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = if (isMin) TEXT_CHARCOAL else highlightCol
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val r = min(width, height) * 0.40f
                canvas.drawCircle(cx, cy, r, outerP)
                if (isSelected) {
                    canvas.drawCircle(cx, cy, r * 0.52f, innerP)
                }
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(activity.dp(26f), activity.dp(26f)).apply {
                rightMargin = activity.dp(12f)
            }
        }
    }

    fun createMinimalistKeypadButtonDrawable(
        activity: MainActivity,
        symbol: String,
        isPressed: Boolean = false
    ): Drawable {
        val radius = activity.dp(pillCornerPt.coerceIn(10f, 40f)).toFloat()
        val baseFill = resolveKeypadButtonBgColor(activity, symbol)

        val fillColor = if (isPressed) Utils.lightenColor(baseFill, -0.06f) else baseFill
        val borderStrokeCol = resolveBorderStrokeColor(activity, Color.argb(18, 44, 44, 44))
        val pillBody = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillColor)
            setStroke(activity.dp(1.2f), borderStrokeCol)
        }
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#18000000")), pillBody, null)
    }

    fun createMinimalistCardDrawable(
        activity: MainActivity,
        cornerDp: Float = 20f,
        cardStyle: Int = 0
    ): Drawable {
        val radius = activity.dp(cornerDp).toFloat()
        val fillCol = when (cardStyle) {
            1 -> ACCENT_MUTED_GREEN
            2 -> if (slotCustomized[SLOT_CARDS_INPUTS]) CARD_WHITE else Color.parseColor("#F7F4EE")
            3 -> ACCENT_PASTEL_BLUE
            4 -> ACCENT_BUTTER_YELLOW
            else -> CARD_WHITE
        }
        val strokeAlpha = if (cardStyle == 2) 35 else 18
        val borderCol = resolveBorderStrokeColor(activity, Color.argb(strokeAlpha, 44, 44, 44))
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillCol)
            setStroke(activity.dp(1.2f), borderCol)
        }
    }

    fun createMinimalistInputDrawable(activity: MainActivity, variant: Int = 0): Drawable {
        val radius = activity.dp(20f).toFloat()
        val customBorder = if (slotCustomized[SLOT_BORDERS]) getSlotEffectiveColor(SLOT_BORDERS) else null
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            if (slotCustomized[SLOT_CARDS_INPUTS]) {
                setColor(CARD_WHITE)
                setStroke(activity.dp(1.2f), customBorder ?: ACCENT_SKY_BLUE)
            } else {
                when (variant % 3) {
                    0 -> {
                        setColor(CARD_WHITE)
                        setStroke(activity.dp(1.2f), customBorder ?: Color.parseColor("#E3DFD9"))
                    }
                    1 -> {
                        setColor(NEUTRAL_SECONDARY_BG)
                        setStroke(activity.dp(1f), customBorder ?: Color.parseColor("#E0DBD3"))
                    }
                    else -> {
                        setColor(ACCENT_PASTEL_BLUE)
                        setStroke(activity.dp(1.2f), customBorder ?: ACCENT_SKY_BLUE)
                    }
                }
            }
        }
    }

    fun createMinimalistActionButtonDrawable(
        activity: MainActivity,
        isPrimary: Boolean,
        pillTone: Int = 0
    ): Drawable {
        val radius = activity.dp(if (pillTone == 5) 20f else 999f).toFloat()
        val bgCol = when (pillTone) {
            1 -> getPrimaryActionColor(activity)
            2 -> ACCENT_BUTTER_YELLOW
            3 -> ACCENT_SKY_BLUE
            4 -> NEUTRAL_SECONDARY_BG
            5 -> CARD_WHITE
            else -> if (isPrimary) getPrimaryActionColor(activity) else NEUTRAL_SECONDARY_BG
        }
        val pill = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(bgCol)
            if (pillTone == 5 || slotCustomized[SLOT_BORDERS]) {
                setStroke(activity.dp(1.2f), resolveBorderStrokeColor(activity, Color.parseColor("#EAE5DE")))
            }
        }
        return RippleDrawable(ColorStateList.valueOf(Color.parseColor("#18000000")), pill, null)
    }

    fun createMinimalistActiveDrawerCapsule(activity: MainActivity): Drawable {
        val radius = activity.dp(999f).toFloat()
        return GradientDrawable().apply {
            cornerRadius = radius
            setColor(ACCENT_SAGE_GREEN)
        }
    }

    fun applyMinimalistStylingToHierarchy(root: View?, activity: MainActivity) {
        if (root == null || !activity.isMinimalistMode) return
        val isSettingsTab = (activity.currentTabIndex == 19)
        var inputCounter = 0

        fun traverse(v: View) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                v.elevation = 0f
                v.translationZ = 0f
            }
            if (v.tag == "MINIMALIST_CUSTOMIZER_IGNORE" || v.tag == "THEME_MATCHED_PALETTES_BAR") return

            when (v) {
                is EditText -> {
                    val variant = inputCounter % 3
                    inputCounter++
                    v.background = createMinimalistInputDrawable(activity, variant)
                    v.setTextColor(ensureReadableTextOnBg(TEXT_CHARCOAL, CARD_WHITE, 2.2))
                    v.setHintTextColor(TEXT_MUTED_GREY)
                    v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                }
                is CheckBox -> {
                    v.setTextColor(TEXT_CHARCOAL)
                    v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    v.buttonTintList = ColorStateList.valueOf(ACCENT_SKY_BLUE)
                }
                is TextView -> {
                    val txt = v.text?.toString() ?: ""
                    v.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)

                    if (v.tag == "PRO_STAR_CHIP") {
                        return
                    }

                    if (isSettingsTab && v.parent is LinearLayout && (v.parent?.parent is HorizontalScrollView)) {
                        return
                    }

                    if (v.isClickable && v.background != null) {
                        v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)

                        if (isSettingsTab) {
                            styleSettingsRowView(v, txt, activity)
                        } else {
                            val tone = when {
                                txt.contains("החלף כיוון") || txt.contains("הקפא") || txt.contains("Hold") || txt.contains("הקפה") -> 2
                                txt.contains("נעץ תוצאה") || txt.contains("רשימת סימון") || txt.contains("PDF") || txt.contains("CSV") || txt.contains("Drive") -> 3
                                txt.contains("כייל") || txt.contains("Tare") || txt.startsWith("חשב") || txt.startsWith("פתור") || txt.startsWith("נתח") || txt.startsWith("פענח") || txt.startsWith("צור") || txt.startsWith("התחל") || txt.startsWith("▶️") || txt.startsWith("🆘") || txt.startsWith("🔆") -> 1
                                else -> 4
                            }
                            val btnBgCol = when (tone) {
                                1 -> getPrimaryActionColor(activity)
                                2 -> ACCENT_BUTTER_YELLOW
                                3 -> ACCENT_SKY_BLUE
                                else -> NEUTRAL_SECONDARY_BG
                            }
                            val desiredBtnTxt = if (slotCustomized[SLOT_OP_TEXT] && tone in 1..2) {
                                getSlotEffectiveColor(SLOT_OP_TEXT, TEXT_CHARCOAL)
                            } else TEXT_CHARCOAL
                            v.setTextColor(ensureReadableTextOnBg(desiredBtnTxt, btnBgCol, 2.3))
                            v.background = createMinimalistActionButtonDrawable(activity, tone == 1, tone)
                            if (txt.contains("החלף כיוון")) {
                                v.text = "⇅ החלף כיוון"
                                val lp = v.layoutParams
                                if (lp is LinearLayout.LayoutParams) {
                                    lp.width = LinearLayout.LayoutParams.WRAP_CONTENT
                                    lp.gravity = Gravity.CENTER_HORIZONTAL
                                    v.layoutParams = lp
                                }
                                v.setPadding(activity.dp(24f), activity.dp(10f), activity.dp(24f), activity.dp(10f))
                            }
                        }
                    } else {
                        v.setTextColor(TEXT_CHARCOAL)
                        v.typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    }
                }
                is ViewGroup -> {
                    for (i in 0 until v.childCount) {
                        traverse(v.getChildAt(i))
                    }
                }
            }
        }
        traverse(root)
    }

    private fun styleSettingsRowView(tv: TextView, rawText: String, activity: MainActivity) {
        val clean = IosGlassTheme.cleanCategoryTitle(rawText, true)
        tv.text = clean
        tv.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        tv.setPadding(activity.dp(18f), activity.dp(15f), activity.dp(18f), activity.dp(15f))
        tv.compoundDrawablePadding = activity.dp(12f)

        val isChecklistBtn = clean.contains("רשימת סימון")
        val rowBgCol = if (isChecklistBtn) ACCENT_SKY_BLUE else CARD_WHITE
        val rowTextCol = ensureReadableTextOnBg(TEXT_CHARCOAL, rowBgCol, 2.3)
        tv.setTextColor(rowTextCol)
        tv.background = createMinimalistActionButtonDrawable(
            activity,
            isPrimary = false,
            pillTone = if (isChecklistBtn) 3 else 5
        )

        val rightDrawable: Drawable? = when {
            isChecklistBtn -> createSettingsBadgeDrawable(activity, 7)
            clean.contains("רטט") || clean.contains("אנימציות") || clean.contains("מצב מקצועי") || clean.contains("מצב תצוגה") ->
                createSettingsBadgeDrawable(activity, if (clean.contains("כבוי")) 2 else 1)
            clean.contains("צלילי הקשה") -> createSettingsBadgeDrawable(activity, 3)
            clean.contains("סוג צליל") -> createSettingsBadgeDrawable(activity, 4)
            clean.contains("צליל AC") -> createSettingsBadgeDrawable(activity, 8)
            clean.contains("מסך תמיד דולק") -> createSettingsBadgeDrawable(activity, 5)
            clean.contains("צורת כפתורים") -> createSettingsBadgeDrawable(activity, 6)
            clean.contains("דיוק עשרוני") || clean.contains("הגדרות") -> createMinimalistVectorIcon(activity, 19, rowTextCol, 20f)
            clean.contains("יד דומיננטית") -> createSettingsBadgeDrawable(activity, 9)
            else -> null
        }
        tv.setCompoundDrawables(null, null, rightDrawable, null)
    }

    private fun createSettingsBadgeDrawable(context: Context, badgeType: Int): Drawable {
        val sizePx = (22f * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val d = context.resources.displayMetrics.density
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = TEXT_CHARCOAL
                style = Paint.Style.STROKE
                strokeWidth = 1.7f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = Math.min(bounds.width(), bounds.height()) * 0.38f

                when (badgeType) {
                    1 -> {
                        fillP.color = ACCENT_SAGE_GREEN
                        canvas.drawCircle(cx, cy, r * 0.85f, fillP)
                    }
                    2 -> {
                        fillP.color = NEUTRAL_SECONDARY_BG
                        canvas.drawCircle(cx, cy, r * 0.85f, fillP)
                        canvas.drawCircle(cx, cy, r * 0.85f, strokeP)
                    }
                    3 -> {
                        val p = Path().apply {
                            moveTo(cx - r * 0.65f, cy - r * 0.3f)
                            lineTo(cx - r * 0.25f, cy - r * 0.3f)
                            lineTo(cx + r * 0.25f, cy - r * 0.7f)
                            lineTo(cx + r * 0.25f, cy + r * 0.7f)
                            lineTo(cx - r * 0.25f, cy + r * 0.3f)
                            lineTo(cx - r * 0.65f, cy + r * 0.3f)
                            close()
                        }
                        canvas.drawPath(p, strokeP)
                        canvas.drawArc(RectF(cx + r * 0.1f, cy - r * 0.45f, cx + r * 0.7f, cy + r * 0.45f), -50f, 100f, false, strokeP)
                    }
                    4 -> {
                        canvas.drawArc(RectF(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f), 20f, 115f, false, strokeP)
                        canvas.drawCircle(cx - r * 0.45f, cy - r * 0.35f, r * 0.22f, strokeP)
                        canvas.drawCircle(cx + r * 0.45f, cy + r * 0.35f, r * 0.22f, strokeP)
                    }
                    5 -> {
                        val moon = Path().apply {
                            addArc(RectF(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f), 40f, 250f)
                        }
                        canvas.drawPath(moon, strokeP)
                    }
                    6 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), r * 0.22f, r * 0.22f, strokeP)
                    }
                    7 -> {
                        canvas.drawLine(cx - r * 0.55f, cy + r * 0.05f, cx - r * 0.1f, cy + r * 0.5f, strokeP)
                        canvas.drawLine(cx - r * 0.1f, cy + r * 0.5f, cx + r * 0.65f, cy - r * 0.45f, strokeP)
                    }
                    8 -> {
                        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = TEXT_CHARCOAL
                            textSize = 11.5f * d
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                        }
                        canvas.drawText("AC", cx, cy + 4f * d, tp)
                    }
                    9 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.5f, cy - r * 0.1f, cx + r * 0.5f, cy + r * 0.75f), r * 0.25f, r * 0.25f, strokeP)
                        for (ox in listOf(-0.35f, -0.12f, 0.12f, 0.35f)) {
                            canvas.drawLine(cx + r * ox, cy - r * 0.1f, cx + r * ox, cy - r * 0.7f, strokeP)
                        }
                    }
                }
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    // =========================================================================
    // SCREEN 5: מנוע התאמת צבעים רב-אלמנטי (18 אלמנטים כולל צבעי תפריט, סמלים, ספרות וסימונים!)
    // =========================================================================
    fun createColorThemeCustomizerSection(activity: MainActivity): View {
        val isMin = activity.isMinimalistMode
        val root = LinearLayout(activity).apply {
            tag = "MINIMALIST_CUSTOMIZER_IGNORE"
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = activity.dp(10f); bottomMargin = activity.dp(14f) }
        }

        val sectionHeaderTv = TextView(activity).apply {
            text = "🎨 התאמת צבעים ל-18 אלמנטים (כולל תפריט הצד, סמלים, ספרות וסימונים):"
            setTextColor(if (isMin) TEXT_CHARCOAL else Color.WHITE)
            textSize = 14.5f
            typeface = if (isMin) Typeface.create("sans-serif-medium", Typeface.NORMAL) else Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(activity.dp(4f), 0, activity.dp(4f), activity.dp(8f))
        }
        root.addView(sectionHeaderTv)

        val elementSpecs = listOf(
            SLOT_ACTIVE_PILL_TEXT to "✨ טקסט וסמל בפריט נבחר בתפריט",
            SLOT_DRAWER_ICONS to "💠 צבע סמלים (אייקונים) בתפריט ובסרגל",
            SLOT_DRAWER_TEXT to "📋 צבע טקסט תפריט הצד",
            SLOT_ACTIVE_PILL to "🟢 רקע קפסולת פריט נבחר בתפריט",
            SLOT_NUM_TEXT to "🔢 צבע הספרות עצמן (0-9 .)",
            SLOT_OP_TEXT to "➕ צבע סימוני הפעולות (+ − × ÷ =)",
            SLOT_MEM_CMD_TEXT to "🔤 צבע כיתוב זיכרון ופקודות (AC ⌫ MC)",
            SLOT_DISPLAY_TEXT to "🖥️ צבע ספרות הצג והתוצאות",
            SLOT_ACTION_1 to "🎨 רקע כפתורי פעולה 1 (+ − ÷)",
            SLOT_ACTION_2 to "🟠 רקע כפתורי פעולה 2 (= × / משני)",
            SLOT_MEM_CMD_KEYS to "🔵 רקע מקשי זיכרון ופקודות (AC / MR)",
            SLOT_NUM_KEYS to "🔢 רקע מקשי מספרים (0-9)",
            SLOT_LOGO_BG to "🏷️ צבע לוגו 1 (רקע באנר תפריט)",
            SLOT_LOGO_TEXT to "✒️ צבע לוגו 2 (טקסט וסמל לוגו)",
            SLOT_MAIN_BG to "🖼️ צבע רקע ראשי (מסך ותפריט)",
            SLOT_CARDS_INPUTS to "🗂️ רקע כרטיסים ותיבות קלט",
            SLOT_TEXT_ICONS to "🖋️ טקסט כותרות ואייקונים כללי",
            SLOT_BORDERS to "🔲 צבע מסגרות כפתורים וכרטיסים"
        )

        val chipsScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(12f) }
        }
        val chipsRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(activity.dp(2f), activity.dp(2f), activity.dp(2f), activity.dp(2f))
        }
        chipsScroll.addView(chipsRow)
        root.addView(chipsScroll)

        val chipViews = mutableMapOf<Int, TextView>()
        lateinit var hexLabelTv: TextView
        lateinit var activeSlotTitleTv: TextView
        lateinit var previewCircleView: View
        lateinit var colorWheelView: Flat2DColorWheelView
        lateinit var brightnessSlider: MinimalistLineSliderView
        lateinit var opacitySlider: MinimalistLineSliderView

        fun applyAllCustomColorsLiveToActivity() {
            val effAction1 = getSlotEffectiveColor(SLOT_ACTION_1, if (isMin) DEFAULT_ACCENT_TERRACOTTA else activity.primaryColor)
            val effMainBg = getSlotEffectiveColor(SLOT_MAIN_BG, if (isMin) DEFAULT_BG_CREAM else activity.bgColor)

            if (slotCustomized[SLOT_ACTION_1]) {
                activity.primaryColor = effAction1
                if (activity.isAuroraMode) AuroraBorealisTheme.AURORA_PRIMARY = effAction1
            }
            if (slotCustomized[SLOT_ACTION_2] && activity.isAuroraMode) {
                AuroraBorealisTheme.AURORA_SECONDARY = getSlotEffectiveColor(SLOT_ACTION_2, AuroraBorealisTheme.AURORA_SECONDARY)
            }
            if (slotCustomized[SLOT_MAIN_BG]) {
                activity.bgColor = effMainBg
                if (activity.isAuroraMode) AuroraBorealisTheme.EMERALD_NIGHT = effMainBg
            }

            if (isMin) {
                val liveBg = getBackgroundColor(activity)
                activity.mainContent?.setBackgroundColor(liveBg)
                activity.window.statusBarColor = liveBg
                activity.window.navigationBarColor = liveBg
            } else if (activity.isAuroraMode) {
                activity.mainContent?.background = AuroraBorealisTheme.createAuroraAtmosphericBackground()
            } else if (activity.isIosGlassMode) {
                activity.mainContent?.background = IosGlassTheme.createLiquidAmbientBackground(activity)
            } else {
                activity.mainContent?.setBackgroundColor(activity.bgColor)
            }
            activity.updateLiveDrawerAndHeaderColors()

            saveMinimalistPrefs(activity.prefs)
            activity.savePreferences()
        }

        fun updatePreviewHeaderAndChips() {
            val slot = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            val effCol = getSlotEffectiveColor(slot)
            val specTitle = elementSpecs.find { it.first == slot }?.second ?: ""

            activeSlotTitleTv.text = "עורך כעת: $specTitle"
            hexLabelTv.text = String.format(Locale.US, "#%08X", effCol)
            previewCircleView.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(effCol)
                setStroke(activity.dp(2f), Color.WHITE)
            }

            for ((sIdx, chipTv) in chipViews) {
                val isSel = (sIdx == slot)
                val chipDotCol = getSlotEffectiveColor(sIdx)
                val selBg = if (isMin) DEFAULT_ACCENT_TERRACOTTA else activity.primaryColor
                val unselBg = if (isMin) DEFAULT_NEUTRAL_SECONDARY_BG else Color.argb(45, 255, 255, 255)
                chipTv.background = GradientDrawable().apply {
                    cornerRadius = activity.dp(999f).toFloat()
                    if (isSel) {
                        setColor(selBg)
                        setStroke(activity.dp(1.5f), if (isMin) DEFAULT_TEXT_CHARCOAL else Color.WHITE)
                    } else {
                        setColor(unselBg)
                        setStroke(activity.dp(1.2f), chipDotCol)
                    }
                }
                chipTv.setTextColor(
                    if (isSel) ensureReadableTextOnBg(Color.WHITE, selBg, 2.3)
                    else if (isMin) DEFAULT_TEXT_CHARCOAL
                    else Color.WHITE
                )
                chipTv.typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.create("sans-serif", Typeface.NORMAL)
            }
        }

        fun selectSlot(slot: Int) {
            selectedElementSlot = slot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            colorWheelView.setSelectedColor(slotBaseColors[selectedElementSlot])
            brightnessSlider.progress = slotBrightness[selectedElementSlot]
            opacitySlider.progress = slotOpacity[selectedElementSlot]
            updatePreviewHeaderAndChips()
            saveMinimalistPrefs(activity.prefs)
        }

        for ((slotIdx, label) in elementSpecs) {
            val chip = TextView(activity).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(activity.dp(14f), activity.dp(9f), activity.dp(14f), activity.dp(9f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(activity.dp(4f), 0, activity.dp(4f), 0) }
                setOnClickListener {
                    activity.feedback(it)
                    selectSlot(slotIdx)
                }
            }
            chipViews[slotIdx] = chip
            chipsRow.addView(chip)
        }

        val yellowCard = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(20f), activity.dp(18f), activity.dp(20f), activity.dp(20f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                if (isMin) {
                    setColor(DEFAULT_ACCENT_BUTTER_YELLOW)
                } else {
                    setColor(Utils.lightenColor(activity.bgColor, 0.10f))
                    setStroke(activity.dp(1.2f), activity.primaryColor)
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val cardInk = if (isMin) DEFAULT_TEXT_CHARCOAL else Color.WHITE

        activeSlotTitleTv = TextView(activity).apply {
            text = ""
            setTextColor(cardInk)
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
            setPadding(0, 0, 0, activity.dp(8f))
        }
        yellowCard.addView(activeSlotTitleTv)

        val hexRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }

        previewCircleView = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams(activity.dp(38f), activity.dp(38f)).apply {
                rightMargin = activity.dp(10f)
            }
        }

        hexLabelTv = TextView(activity).apply {
            text = "#FFFFA48E"
            setTextColor(cardInk)
            textSize = 14.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        hexRow.addView(previewCircleView)
        hexRow.addView(hexLabelTv)
        yellowCard.addView(hexRow)

        colorWheelView = Flat2DColorWheelView(activity) { pickedColor ->
            val s = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            slotCustomized[s] = true
            slotBaseColors[s] = pickedColor
            updatePreviewHeaderAndChips()
            applyAllCustomColorsLiveToActivity()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(220f)
            ).apply { bottomMargin = activity.dp(14f) }
        }
        yellowCard.addView(colorWheelView)

        val brightLabel = TextView(activity).apply {
            text = "☀️ בהירות"
            setTextColor(cardInk)
            textSize = 13.5f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(2f), 0, activity.dp(2f))
        }
        brightnessSlider = MinimalistLineSliderView(activity, slotBrightness[selectedElementSlot]) { newVal ->
            val s = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            slotCustomized[s] = true
            slotBrightness[s] = newVal.coerceIn(0.20f, 1.0f)
            updatePreviewHeaderAndChips()
            applyAllCustomColorsLiveToActivity()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(34f)
            ).apply { bottomMargin = activity.dp(8f) }
        }
        yellowCard.addView(brightLabel)
        yellowCard.addView(brightnessSlider)

        val opacityLabel = TextView(activity).apply {
            text = "💧 שקיפות"
            setTextColor(cardInk)
            textSize = 13.5f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(2f), 0, activity.dp(2f))
        }
        opacitySlider = MinimalistLineSliderView(activity, slotOpacity[selectedElementSlot]) { newVal ->
            val s = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
            slotCustomized[s] = true
            slotOpacity[s] = newVal.coerceIn(0.25f, 1.0f)
            updatePreviewHeaderAndChips()
            applyAllCustomColorsLiveToActivity()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(34f)
            ).apply { bottomMargin = activity.dp(12f) }
        }
        yellowCard.addView(opacityLabel)
        yellowCard.addView(opacitySlider)

        val resetRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val resetCurrentSlotBtn = TextView(activity).apply {
            text = "↺ אפס אלמנט זה"
            setTextColor(DEFAULT_TEXT_CHARCOAL)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(activity.dp(10f), activity.dp(10f), activity.dp(10f), activity.dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(DEFAULT_NEUTRAL_SECONDARY_BG)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = activity.dp(5f)
            }
            setOnClickListener {
                activity.feedback(it)
                val s = selectedElementSlot.coerceIn(0, TOTAL_COLOR_SLOTS - 1)
                slotCustomized[s] = false
                slotBaseColors[s] = getDefaultSlotColors()[s]
                slotBrightness[s] = 1.0f
                slotOpacity[s] = 1.0f
                selectSlot(s)
                activity.applyTheme()
            }
        }

        val resetAllPastelBtn = TextView(activity).apply {
            text = "↺ אפס לפלטת ברירת המחדל"
            setTextColor(DEFAULT_TEXT_CHARCOAL)
            textSize = 13f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            gravity = Gravity.CENTER
            setPadding(activity.dp(10f), activity.dp(10f), activity.dp(10f), activity.dp(10f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(DEFAULT_CARD_WHITE)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.3f).apply {
                leftMargin = activity.dp(5f)
            }
            setOnClickListener {
                activity.feedback(it)
                val defPreset = getPalettesForCurrentTheme(activity).first()
                applyThemePalettePreset(activity, defPreset)
                if (activity.isMinimalistMode) {
                    resetSlotsArrayToDefaults()
                    saveMinimalistPrefs(activity.prefs)
                    activity.applyTheme()
                }
            }
        }

        resetRow.addView(resetCurrentSlotBtn)
        resetRow.addView(resetAllPastelBtn)
        yellowCard.addView(resetRow)

        root.addView(yellowCard)
        selectSlot(selectedElementSlot)
        return root
    }

    // =========================================================================
    // חבילת 85+ סמלים וקטוריים ייחודיים לערכת הנושא המינימליסטית!
    // =========================================================================
    fun createMinimalistVectorIcon(
        context: Context,
        iconCode: Int,
        tintColor: Int = TEXT_CHARCOAL,
        sizeDp: Float = 22f
    ): Drawable {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        return object : Drawable() {
            private val d = context.resources.displayMetrics.density
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val b = bounds
                val cx = b.exactCenterX()
                val cy = b.exactCenterY()
                val r = Math.min(b.width(), b.height()) * 0.40f

                when (iconCode) {
                    0 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.84f, cx + r * 0.68f, cy + r * 0.84f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawRect(cx - r * 0.42f, cy - r * 0.56f, cx + r * 0.42f, cy - r * 0.22f, paint)
                        for (gx in listOf(-0.32f, 0f, 0.32f)) {
                            for (gy in listOf(0.14f, 0.48f)) {
                                canvas.drawCircle(cx + r * gx, cy + r * gy, r * 0.08f, dotPaint)
                            }
                        }
                    }
                    1 -> {
                        canvas.drawLine(cx - r * 0.82f, cy, cx + r * 0.82f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.78f, cx, cy + r * 0.78f, paint)
                        val wave = Path().apply {
                            moveTo(cx - r * 0.78f, cy)
                            cubicTo(cx - r * 0.35f, cy - r * 0.95f, cx + r * 0.35f, cy + r * 0.95f, cx + r * 0.78f, cy)
                        }
                        canvas.drawPath(wave, paint)
                    }
                    2 -> {
                        val rec = Path().apply {
                            moveTo(cx - r * 0.62f, cy - r * 0.82f)
                            lineTo(cx + r * 0.62f, cy - r * 0.82f)
                            lineTo(cx + r * 0.62f, cy + r * 0.80f)
                            lineTo(cx + r * 0.30f, cy + r * 0.58f)
                            lineTo(cx, cy + r * 0.80f)
                            lineTo(cx - r * 0.30f, cy + r * 0.58f)
                            lineTo(cx - r * 0.62f, cy + r * 0.80f)
                            close()
                        }
                        canvas.drawPath(rec, paint)
                        canvas.drawLine(cx - r * 0.28f, cy + r * 0.18f, cx + r * 0.28f, cy - r * 0.32f, paint)
                        canvas.drawCircle(cx - r * 0.20f, cy - r * 0.28f, r * 0.09f, dotPaint)
                        canvas.drawCircle(cx + r * 0.20f, cy + r * 0.14f, r * 0.09f, dotPaint)
                    }
                    3 -> {
                        val roof = Path().apply {
                            moveTo(cx - r * 0.85f, cy - r * 0.24f)
                            lineTo(cx, cy - r * 0.82f)
                            lineTo(cx + r * 0.85f, cy - r * 0.24f)
                            close()
                        }
                        canvas.drawPath(roof, paint)
                        for (colX in listOf(-0.52f, 0f, 0.52f)) {
                            canvas.drawLine(cx + r * colX, cy - r * 0.22f, cx + r * colX, cy + r * 0.68f, paint)
                        }
                        canvas.drawLine(cx - r * 0.85f, cy + r * 0.72f, cx + r * 0.85f, cy + r * 0.72f, paint)
                    }
                    4 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.88f, cy + r * 0.05f, cx + r * 0.88f, cy + r * 0.68f), r * 0.14f, r * 0.14f, paint)
                        for (tx in listOf(-0.45f, 0f, 0.45f)) {
                            canvas.drawLine(cx + r * tx, cy + r * 0.05f, cx + r * tx, cy + r * 0.38f, paint)
                        }
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.42f, cx + r * 0.72f, cy - r * 0.42f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.42f, cx - r * 0.45f, cy - r * 0.65f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.42f, cx - r * 0.45f, cy - r * 0.19f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.42f, cx + r * 0.45f, cy - r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.42f, cx + r * 0.45f, cy - r * 0.19f, paint)
                    }
                    5 -> {
                        canvas.drawLine(cx, cy - r * 0.78f, cx, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.45f, cy + r * 0.78f, cx + r * 0.45f, cy + r * 0.78f, paint)
                        canvas.drawLine(cx - r * 0.75f, cy - r * 0.35f, cx + r * 0.75f, cy - r * 0.35f, paint)
                        canvas.drawArc(RectF(cx - r * 0.90f, cy - r * 0.05f, cx - r * 0.35f, cy + r * 0.42f), 0f, 180f, true, paint)
                        canvas.drawArc(RectF(cx + r * 0.35f, cy - r * 0.05f, cx + r * 0.90f, cy + r * 0.42f), 0f, 180f, true, paint)
                    }
                    6 -> {
                        canvas.drawArc(RectF(cx - r * 0.44f, cy + r * 0.05f, cx + r * 0.44f, cy + r * 0.86f), -45f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.22f, cy - r * 0.75f, cx - r * 0.22f, cy + r * 0.18f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy - r * 0.75f, cx + r * 0.22f, cy + r * 0.18f, paint)
                        canvas.drawArc(RectF(cx - r * 0.22f, cy - r * 0.92f, cx + r * 0.22f, cy - r * 0.55f), 180f, 180f, false, paint)
                        canvas.drawCircle(cx + r * 0.58f, cy - r * 0.45f, r * 0.13f, paint)
                    }
                    7 -> {
                        canvas.drawLine(cx - r * 0.32f, cy - r * 0.48f, cx - r * 0.82f, cy, paint)
                        canvas.drawLine(cx - r * 0.82f, cy, cx - r * 0.32f, cy + r * 0.48f, paint)
                        canvas.drawLine(cx + r * 0.32f, cy - r * 0.48f, cx + r * 0.82f, cy, paint)
                        canvas.drawLine(cx + r * 0.82f, cy, cx + r * 0.32f, cy + r * 0.48f, paint)
                        canvas.drawLine(cx + r * 0.15f, cy - r * 0.58f, cx - r * 0.15f, cy + r * 0.58f, paint)
                    }
                    8 -> {
                        canvas.drawLine(cx - r * 0.62f, cy + r * 0.62f, cx + r * 0.62f, cy - r * 0.62f, paint)
                        canvas.drawCircle(cx - r * 0.40f, cy - r * 0.40f, r * 0.22f, paint)
                        canvas.drawCircle(cx + r * 0.40f, cy + r * 0.40f, r * 0.22f, paint)
                    }
                    9 -> {
                        val tag = Path().apply {
                            moveTo(cx - r * 0.78f, cy - r * 0.78f)
                            lineTo(cx + r * 0.12f, cy - r * 0.78f)
                            lineTo(cx + r * 0.84f, cy - r * 0.04f)
                            lineTo(cx - r * 0.04f, cy + r * 0.84f)
                            lineTo(cx - r * 0.78f, cy + r * 0.12f)
                            close()
                        }
                        canvas.drawPath(tag, paint)
                        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.38f, r * 0.13f, paint)
                    }
                    10 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.65f, cy - r * 0.35f, cx + r * 0.42f, cy + r * 0.52f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawArc(RectF(cx + r * 0.25f, cy - r * 0.20f, cx + r * 0.82f, cy + r * 0.32f), -90f, 180f, false, paint)
                        canvas.drawLine(cx - r * 0.82f, cy + r * 0.72f, cx + r * 0.65f, cy + r * 0.72f, paint)
                        canvas.drawLine(cx - r * 0.25f, cy - r * 0.58f, cx - r * 0.25f, cy - r * 0.82f, paint)
                        canvas.drawLine(cx + r * 0.08f, cy - r * 0.58f, cx + r * 0.08f, cy - r * 0.82f, paint)
                    }
                    11 -> {
                        val ecg = Path().apply {
                            moveTo(cx - r * 0.88f, cy + r * 0.05f)
                            lineTo(cx - r * 0.35f, cy + r * 0.05f)
                            lineTo(cx - r * 0.16f, cy - r * 0.65f)
                            lineTo(cx + r * 0.16f, cy + r * 0.65f)
                            lineTo(cx + r * 0.35f, cy + r * 0.05f)
                            lineTo(cx + r * 0.88f, cy + r * 0.05f)
                        }
                        canvas.drawPath(ecg, paint)
                    }
                    12 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy + r * 0.02f, cx + r * 0.76f, cy + r * 0.78f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.38f, cx + r * 0.76f, cy + r * 0.38f, paint)
                        canvas.drawLine(cx, cy + r * 0.02f, cx, cy - r * 0.48f, paint)
                        canvas.drawCircle(cx, cy - r * 0.68f, r * 0.13f, paint)
                    }
                    13 -> {
                        canvas.drawCircle(cx, cy, r * 0.80f, paint)
                        canvas.drawOval(RectF(cx - r * 0.40f, cy - r * 0.80f, cx + r * 0.40f, cy + r * 0.80f), paint)
                        canvas.drawLine(cx - r * 0.80f, cy, cx + r * 0.80f, cy, paint)
                    }
                    14 -> {
                        val flame = Path().apply {
                            moveTo(cx, cy - r * 0.88f)
                            cubicTo(cx + r * 0.82f, cy - r * 0.10f, cx + r * 0.65f, cy + r * 0.78f, cx, cy + r * 0.86f)
                            cubicTo(cx - r * 0.65f, cy + r * 0.78f, cx - r * 0.82f, cy - r * 0.10f, cx, cy - r * 0.88f)
                        }
                        canvas.drawPath(flame, paint)
                        canvas.drawCircle(cx, cy + r * 0.35f, r * 0.22f, paint)
                    }
                    15 -> {
                        val rootPath = Path().apply {
                            moveTo(cx - r * 0.84f, cy + r * 0.12f)
                            lineTo(cx - r * 0.48f, cy + r * 0.12f)
                            lineTo(cx - r * 0.18f, cy + r * 0.72f)
                            lineTo(cx + r * 0.32f, cy - r * 0.68f)
                            lineTo(cx + r * 0.84f, cy - r * 0.68f)
                        }
                        canvas.drawPath(rootPath, paint)
                    }
                    16 -> {
                        canvas.drawLine(cx - r * 0.62f, cy + r * 0.62f, cx + r * 0.62f, cy - r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.38f, cy - r * 0.66f, cx - r * 0.38f, cy - r * 0.16f, paint)
                        canvas.drawLine(cx + r * 0.24f, cy + r * 0.24f, cx + r * 0.60f, cy + r * 0.24f, paint)
                        canvas.drawLine(cx + r * 0.24f, cy + r * 0.62f, cx + r * 0.60f, cy + r * 0.62f, paint)
                    }
                    17 -> {
                        val trend = Path().apply {
                            moveTo(cx - r * 0.78f, cy + r * 0.68f)
                            lineTo(cx - r * 0.22f, cy + r * 0.18f)
                            lineTo(cx + r * 0.15f, cy + r * 0.38f)
                            lineTo(cx + r * 0.78f, cy - r * 0.58f)
                        }
                        canvas.drawPath(trend, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.58f, cx + r * 0.78f, cy - r * 0.58f, paint)
                        canvas.drawLine(cx + r * 0.78f, cy - r * 0.58f, cx + r * 0.78f, cy - r * 0.16f, paint)
                    }
                    18 -> {
                        canvas.drawArc(RectF(cx - r * 0.84f, cy - r * 0.62f, cx + r * 0.84f, cy + r * 0.84f), 165f, 210f, false, paint)
                        canvas.drawLine(cx, cy + r * 0.22f, cx + r * 0.48f, cy - r * 0.34f, paint)
                        canvas.drawCircle(cx, cy + r * 0.22f, r * 0.12f, dotPaint)
                    }
                    19 -> {
                        canvas.drawCircle(cx, cy, r * 0.34f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.44f).toFloat(),
                                (cy + sin(a) * r * 0.44f).toFloat(),
                                (cx + cos(a) * r * 0.80f).toFloat(),
                                (cy + sin(a) * r * 0.80f).toFloat(),
                                paint
                            )
                        }
                    }
                    20 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.58f, cy - r * 0.76f, cx + r * 0.34f, cy + r * 0.76f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawRect(cx - r * 0.36f, cy - r * 0.52f, cx + r * 0.12f, cy - r * 0.14f, paint)
                        canvas.drawLine(cx + r * 0.34f, cy - r * 0.28f, cx + r * 0.74f, cy + r * 0.12f, paint)
                        canvas.drawLine(cx + r * 0.74f, cy + r * 0.12f, cx + r * 0.74f, cy + r * 0.58f, paint)
                    }
                    21 -> {
                        val leaf = Path().apply {
                            moveTo(cx - r * 0.68f, cy + r * 0.68f)
                            quadTo(cx - r * 0.75f, cy - r * 0.65f, cx + r * 0.75f, cy - r * 0.75f)
                            quadTo(cx + r * 0.65f, cy + r * 0.75f, cx - r * 0.68f, cy + r * 0.68f)
                        }
                        canvas.drawPath(leaf, paint)
                        canvas.drawLine(cx - r * 0.68f, cy + r * 0.68f, cx + r * 0.38f, cy - r * 0.38f, paint)
                    }
                    22 -> {
                        val cap = Path().apply {
                            moveTo(cx, cy - r * 0.72f)
                            lineTo(cx + r * 0.88f, cy - r * 0.18f)
                            lineTo(cx, cy + r * 0.36f)
                            lineTo(cx - r * 0.88f, cy - r * 0.18f)
                            close()
                        }
                        canvas.drawPath(cap, paint)
                        canvas.drawLine(cx + r * 0.68f, cy - r * 0.05f, cx + r * 0.68f, cy + r * 0.68f, paint)
                    }
                    23 -> {
                        canvas.drawArc(RectF(cx - r * 0.76f, cy - r * 0.48f, cx + r * 0.76f, cy + r * 0.85f), 180f, 180f, true, paint)
                        canvas.drawCircle(cx, cy - r * 0.62f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.88f, cy + r * 0.42f, cx + r * 0.88f, cy + r * 0.42f, paint)
                    }
                    24 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.66f, cy - r * 0.32f, cx + r * 0.66f, cy + r * 0.80f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawArc(RectF(cx - r * 0.36f, cy - r * 0.78f, cx + r * 0.36f, cy + r * 0.02f), 180f, 180f, false, paint)
                    }
                    25 -> {
                        canvas.drawRect(cx - r * 0.82f, cy - r * 0.45f, cx - r * 0.18f, cy + r * 0.45f, paint)
                        canvas.drawRect(cx + r * 0.18f, cy - r * 0.65f, cx + r * 0.82f, cy + r * 0.45f, paint)
                    }
                    26 -> {
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawLine(cx, cy - r * 0.48f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.38f, cy + r * 0.22f, paint)
                        canvas.drawCircle(cx, cy - r * 0.66f, r * 0.06f, dotPaint)
                        canvas.drawCircle(cx + r * 0.66f, cy, r * 0.06f, dotPaint)
                    }
                    27 -> {
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawCircle(
                                (cx + cos(a) * r * 0.50f).toFloat(),
                                (cy + sin(a) * r * 0.50f).toFloat(),
                                r * 0.26f,
                                paint
                            )
                        }
                    }
                    28 -> {
                        canvas.drawLine(cx - r * 0.32f, cy - r * 0.76f, cx - r * 0.32f, cy + r * 0.76f, paint)
                        canvas.drawLine(cx + r * 0.32f, cy - r * 0.76f, cx + r * 0.32f, cy + r * 0.76f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.32f, cx + r * 0.76f, cy - r * 0.32f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.32f, cx + r * 0.76f, cy + r * 0.32f, paint)
                    }
                    29 -> {
                        val gem = Path().apply {
                            moveTo(cx - r * 0.48f, cy - r * 0.72f)
                            lineTo(cx + r * 0.48f, cy - r * 0.72f)
                            lineTo(cx + r * 0.84f, cy - r * 0.18f)
                            lineTo(cx, cy + r * 0.82f)
                            lineTo(cx - r * 0.84f, cy - r * 0.18f)
                            close()
                        }
                        canvas.drawPath(gem, paint)
                        canvas.drawLine(cx - r * 0.84f, cy - r * 0.18f, cx + r * 0.84f, cy - r * 0.18f, paint)
                    }
                    30 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.62f, cx + r * 0.76f, cy + r * 0.78f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy - r * 0.18f, cx + r * 0.76f, cy - r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.84f, cx - r * 0.36f, cy - r * 0.42f, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.84f, cx + r * 0.36f, cy - r * 0.42f, paint)
                    }
                    31, 305 -> {
                        canvas.drawLine(cx - r * 0.84f, cy - r * 0.36f, cx + r * 0.84f, cy - r * 0.36f, paint)
                        canvas.drawLine(cx - r * 0.84f, cy - r * 0.36f, cx - r * 0.84f, cy + r * 0.66f, paint)
                        canvas.drawLine(cx - r * 0.16f, cy - r * 0.56f, cx - r * 0.16f, cy + r * 0.56f, paint)
                        canvas.drawRect(cx - r * 0.16f, cy - r * 0.56f, cx + r * 0.36f, cy - r * 0.16f, paint)
                    }
                    32, 306 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.18f, cx + r * 0.82f, cy + r * 0.18f), r * 0.10f, r * 0.10f, paint)
                        canvas.drawLine(cx, cy - r * 0.18f, cx, cy - r * 0.66f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.66f, cx + r * 0.42f, cy - r * 0.66f, paint)
                        canvas.drawCircle(cx, cy + r * 0.58f, r * 0.16f, paint)
                    }
                    33 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.52f, cy - r * 0.74f, cx + r * 0.52f, cy + r * 0.42f), r * 0.18f, r * 0.18f, paint)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.16f, cx + r * 0.24f, cy - r * 0.16f, paint)
                        canvas.drawLine(cx, cy - r * 0.40f, cx, cy + r * 0.08f, paint)
                        canvas.drawLine(cx, cy + r * 0.42f, cx, cy + r * 0.84f, paint)
                    }
                    34 -> {
                        val cube = Path().apply {
                            moveTo(cx, cy - r * 0.82f)
                            lineTo(cx + r * 0.74f, cy - r * 0.38f)
                            lineTo(cx + r * 0.74f, cy + r * 0.40f)
                            lineTo(cx, cy + r * 0.82f)
                            lineTo(cx - r * 0.74f, cy + r * 0.40f)
                            lineTo(cx - r * 0.74f, cy - r * 0.38f)
                            close()
                        }
                        canvas.drawPath(cube, paint)
                        canvas.drawLine(cx - r * 0.74f, cy - r * 0.38f, cx, cy + r * 0.05f, paint)
                        canvas.drawLine(cx + r * 0.74f, cy - r * 0.38f, cx, cy + r * 0.05f, paint)
                        canvas.drawLine(cx, cy + r * 0.05f, cx, cy + r * 0.82f, paint)
                    }
                    35 -> {
                        canvas.drawLine(cx - r * 0.90f, cy, cx - r * 0.56f, cy, paint)
                        canvas.drawLine(cx + r * 0.56f, cy, cx + r * 0.90f, cy, paint)
                        canvas.drawRoundRect(RectF(cx - r * 0.56f, cy - r * 0.34f, cx + r * 0.56f, cy + r * 0.34f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawLine(cx - r * 0.25f, cy - r * 0.34f, cx - r * 0.25f, cy + r * 0.34f, paint)
                        canvas.drawLine(cx, cy - r * 0.34f, cx, cy + r * 0.34f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy - r * 0.34f, cx + r * 0.25f, cy + r * 0.34f, paint)
                    }
                    36 -> {
                        canvas.drawLine(cx - r * 0.86f, cy, cx - r * 0.18f, cy, paint)
                        canvas.drawLine(cx + r * 0.18f, cy, cx + r * 0.86f, cy, paint)
                        canvas.drawLine(cx - r * 0.18f, cy - r * 0.62f, cx - r * 0.18f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx + r * 0.18f, cy - r * 0.62f, cx + r * 0.18f, cy + r * 0.62f, paint)
                    }
                    37 -> {
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        canvas.drawCircle(cx, cy, r * 0.20f, dotPaint)
                        for (i in 0 until 6) {
                            val a = Math.toRadians((i * 60).toDouble())
                            canvas.drawCircle((cx + cos(a) * r * 0.46f).toFloat(), (cy + sin(a) * r * 0.46f).toFloat(), r * 0.12f, dotPaint)
                        }
                    }
                    38, 307 -> {
                        canvas.drawArc(RectF(cx - r * 0.66f, cy - r * 0.72f, cx + r * 0.66f, cy + r * 0.48f), 135f, 270f, false, paint)
                        canvas.drawLine(cx - r * 0.78f, cy + r * 0.56f, cx - r * 0.28f, cy + r * 0.56f, paint)
                        canvas.drawLine(cx + r * 0.28f, cy + r * 0.56f, cx + r * 0.78f, cy + r * 0.56f, paint)
                    }
                    39 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.86f, cy - r * 0.32f, cx + r * 0.86f, cy + r * 0.32f), r * 0.32f, r * 0.32f, paint)
                        canvas.drawLine(cx - r * 0.26f, cy - r * 0.32f, cx - r * 0.26f, cy + r * 0.32f, paint)
                        canvas.drawLine(cx + r * 0.26f, cy - r * 0.32f, cx + r * 0.26f, cy + r * 0.32f, paint)
                        canvas.drawCircle(cx, cy, r * 0.16f, dotPaint)
                    }
                    40, 308 -> {
                        canvas.drawCircle(cx, cy, r * 0.82f, paint)
                        val needle = Path().apply {
                            moveTo(cx + r * 0.46f, cy - r * 0.46f)
                            lineTo(cx + r * 0.13f, cy + r * 0.13f)
                            lineTo(cx - r * 0.46f, cy + r * 0.46f)
                            lineTo(cx - r * 0.13f, cy - r * 0.13f)
                            close()
                        }
                        canvas.drawPath(needle, paint)
                        canvas.drawCircle(cx, cy, r * 0.09f, dotPaint)
                    }
                    41 -> {
                        canvas.drawArc(RectF(cx - r * 0.82f, cy - r * 0.40f, cx + r * 0.82f, cy + r * 1.12f), 180f, 180f, true, paint)
                        canvas.drawLine(cx, cy + r * 0.36f, cx + r * 0.54f, cy - r * 0.24f, paint)
                        canvas.drawCircle(cx, cy + r * 0.36f, r * 0.11f, dotPaint)
                    }
                    42 -> {
                        canvas.drawCircle(cx, cy + r * 0.10f, r * 0.70f, paint)
                        canvas.drawLine(cx - r * 0.24f, cy - r * 0.82f, cx + r * 0.24f, cy - r * 0.82f, paint)
                        canvas.drawLine(cx, cy - r * 0.82f, cx, cy - r * 0.60f, paint)
                        canvas.drawLine(cx + r * 0.46f, cy - r * 0.56f, cx + r * 0.66f, cy - r * 0.72f, paint)
                        canvas.drawLine(cx, cy + r * 0.10f, cx, cy - r * 0.30f, paint)
                        canvas.drawLine(cx, cy + r * 0.10f, cx + r * 0.34f, cy + r * 0.10f, paint)
                    }
                    43 -> {
                        val body = Path().apply {
                            moveTo(cx - r * 0.26f, cy + r * 0.78f)
                            lineTo(cx + r * 0.26f, cy + r * 0.78f)
                            lineTo(cx + r * 0.26f, cy + r * 0.10f)
                            lineTo(cx + r * 0.52f, cy - r * 0.30f)
                            lineTo(cx - r * 0.52f, cy - r * 0.30f)
                            lineTo(cx - r * 0.26f, cy + r * 0.10f)
                            close()
                        }
                        canvas.drawPath(body, paint)
                        canvas.drawLine(cx, cy - r * 0.50f, cx, cy - r * 0.84f, paint)
                        canvas.drawLine(cx - r * 0.36f, cy - r * 0.46f, cx - r * 0.60f, cy - r * 0.76f, paint)
                        canvas.drawLine(cx + r * 0.36f, cy - r * 0.46f, cx + r * 0.60f, cy - r * 0.76f, paint)
                    }
                    44, 309 -> {
                        canvas.save(); canvas.rotate(30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.86f, cy - r * 0.34f, cx + r * 0.86f, cy + r * 0.34f), paint)
                        canvas.restore(); canvas.save(); canvas.rotate(-30f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.86f, cy - r * 0.34f, cx + r * 0.86f, cy + r * 0.34f), paint)
                        canvas.restore()
                        canvas.drawCircle(cx, cy, r * 0.15f, dotPaint)
                    }
                    45 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.84f, cy - r * 0.58f, cx + r * 0.84f, cy + r * 0.52f), r * 0.30f, r * 0.30f, paint)
                        canvas.drawCircle(cx - r * 0.48f, cy - r * 0.03f, r * 0.11f, dotPaint)
                        canvas.drawLine(cx - r * 0.18f, cy - r * 0.03f, cx + r * 0.22f, cy - r * 0.03f, paint)
                        canvas.drawCircle(cx + r * 0.52f, cy - r * 0.03f, r * 0.11f, dotPaint)
                    }
                    46 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.56f, cy - r * 0.08f, cx + r * 0.56f, cy + r * 0.76f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawArc(RectF(cx - r * 0.36f, cy - r * 0.76f, cx + r * 0.36f, cy + r * 0.08f), 180f, 180f, false, paint)
                        canvas.drawCircle(cx, cy + r * 0.32f, r * 0.13f, dotPaint)
                    }
                    47 -> {
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.62f, cx + r * 0.72f, cy - r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy + r * 0.62f, cx + r * 0.72f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx - r * 0.42f, cy - r * 0.62f, cx - r * 0.42f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx, cy - r * 0.62f, cx + r * 0.30f, cy + r * 0.62f, paint)
                        canvas.drawLine(cx + r * 0.60f, cy - r * 0.62f, cx + r * 0.30f, cy + r * 0.62f, paint)
                    }
                    48 -> {
                        val ingotBot = Path().apply {
                            moveTo(cx - r * 0.52f, cy + r * 0.22f)
                            lineTo(cx + r * 0.52f, cy + r * 0.22f)
                            lineTo(cx + r * 0.78f, cy + r * 0.72f)
                            lineTo(cx - r * 0.78f, cy + r * 0.72f)
                            close()
                        }
                        val ingotTop = Path().apply {
                            moveTo(cx - r * 0.35f, cy - r * 0.42f)
                            lineTo(cx + r * 0.35f, cy - r * 0.42f)
                            lineTo(cx + r * 0.55f, cy + r * 0.05f)
                            lineTo(cx - r * 0.55f, cy + r * 0.05f)
                            close()
                        }
                        canvas.drawPath(ingotBot, paint)
                        canvas.drawPath(ingotTop, paint)
                    }
                    49 -> {
                        val roof = Path().apply {
                            moveTo(cx - r * 0.84f, cy - r * 0.05f)
                            lineTo(cx, cy - r * 0.80f)
                            lineTo(cx + r * 0.84f, cy - r * 0.05f)
                        }
                        canvas.drawPath(roof, paint)
                        canvas.drawRect(cx - r * 0.56f, cy - r * 0.05f, cx + r * 0.56f, cy + r * 0.76f, paint)
                        canvas.drawRect(cx - r * 0.16f, cy + r * 0.32f, cx + r * 0.16f, cy + r * 0.76f, paint)
                    }
                    50, 310 -> {
                        canvas.drawLine(cx - r * 0.80f, cy + r * 0.72f, cx + r * 0.80f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx - r * 0.65f, cy + r * 0.15f, cx - r * 0.35f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx - r * 0.15f, cy - r * 0.20f, cx + r * 0.15f, cy + r * 0.72f, paint)
                        canvas.drawRect(cx + r * 0.35f, cy - r * 0.65f, cx + r * 0.65f, cy + r * 0.72f, paint)
                    }
                    51 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.38f, cx + r * 0.76f, cy + r * 0.72f), r * 0.16f, r * 0.16f, paint)
                        canvas.drawRect(cx - r * 0.30f, cy - r * 0.72f, cx + r * 0.30f, cy - r * 0.38f, paint)
                        canvas.drawLine(cx - r * 0.76f, cy + r * 0.12f, cx + r * 0.76f, cy + r * 0.12f, paint)
                    }
                    52 -> {
                        val bell = Path().apply {
                            moveTo(cx - r * 0.84f, cy + r * 0.62f)
                            cubicTo(cx - r * 0.34f, cy + r * 0.62f, cx - r * 0.30f, cy - r * 0.72f, cx, cy - r * 0.72f)
                            cubicTo(cx + r * 0.30f, cy - r * 0.72f, cx + r * 0.34f, cy + r * 0.62f, cx + r * 0.84f, cy + r * 0.62f)
                        }
                        canvas.drawPath(bell, paint)
                        canvas.drawLine(cx, cy - r * 0.72f, cx, cy + r * 0.62f, paint)
                    }
                    53 -> {
                        val tri = Path().apply {
                            moveTo(cx - r * 0.72f, cy + r * 0.68f)
                            lineTo(cx + r * 0.74f, cy + r * 0.68f)
                            lineTo(cx - r * 0.72f, cy - r * 0.72f)
                            close()
                        }
                        canvas.drawPath(tri, paint)
                        canvas.drawRect(cx - r * 0.72f, cy + r * 0.38f, cx - r * 0.42f, cy + r * 0.68f, paint)
                    }
                    54, 311 -> {
                        val sigma = Path().apply {
                            moveTo(cx + r * 0.62f, cy - r * 0.70f)
                            lineTo(cx - r * 0.62f, cy - r * 0.70f)
                            lineTo(cx + r * 0.10f, cy)
                            lineTo(cx - r * 0.62f, cy + r * 0.70f)
                            lineTo(cx + r * 0.62f, cy + r * 0.70f)
                        }
                        canvas.drawPath(sigma, paint)
                    }
                    55 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.76f, cy - r * 0.25f, cx + r * 0.76f, cy + r * 0.45f), r * 0.35f, r * 0.35f, paint)
                        canvas.drawArc(RectF(cx - r * 0.40f, cy - r * 0.68f, cx + r * 0.36f, cy + r * 0.05f), 180f, 180f, false, paint)
                        canvas.drawLine(cx, cy + r * 0.76f, cx, cy - r * 0.10f, paint)
                        canvas.drawLine(cx - r * 0.22f, cy + r * 0.12f, cx, cy - r * 0.10f, paint)
                        canvas.drawLine(cx + r * 0.22f, cy + r * 0.12f, cx, cy - r * 0.10f, paint)
                    }
                    56, 102, 103, 300 -> {
                        val star4 = Path().apply {
                            moveTo(cx, cy - r * 0.95f)
                            quadTo(cx + r * 0.12f, cy - r * 0.12f, cx + r * 0.95f, cy)
                            quadTo(cx + r * 0.12f, cy + r * 0.12f, cx, cy + r * 0.95f)
                            quadTo(cx - r * 0.12f, cy + r * 0.12f, cx - r * 0.95f, cy)
                            quadTo(cx - r * 0.12f, cy - r * 0.12f, cx, cy - r * 0.95f)
                            close()
                        }
                        if (iconCode == 102) {
                            canvas.drawPath(star4, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = ACCENT_SAGE_GREEN
                                style = Paint.Style.FILL
                            })
                        }
                        canvas.drawPath(star4, paint)
                        canvas.drawCircle(cx, cy, r * 0.12f, dotPaint)
                    }
                    57 -> {
                        canvas.drawCircle(cx, cy, r * 0.78f, paint)
                        canvas.drawCircle(cx, cy, r * 0.26f, paint)
                        for (i in 0 until 3) {
                            val a = Math.toRadians((i * 120).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.26f).toFloat(),
                                (cy + sin(a) * r * 0.26f).toFloat(),
                                (cx + cos(a) * r * 0.78f).toFloat(),
                                (cy + sin(a) * r * 0.78f).toFloat(),
                                paint
                            )
                        }
                    }
                    58 -> {
                        val drop = Path().apply {
                            moveTo(cx, cy - r * 0.85f)
                            quadTo(cx + r * 0.76f, cy + r * 0.15f, cx, cy + r * 0.82f)
                            quadTo(cx - r * 0.76f, cy + r * 0.15f, cx, cy - r * 0.85f)
                        }
                        canvas.drawPath(drop, paint)
                    }
                    59 -> {
                        for (offset in listOf(-0.38f, 0f, 0.38f)) {
                            val ox = cx + r * offset
                            val wPath = Path().apply {
                                moveTo(ox, cy + r * 0.68f)
                                quadTo(ox - r * 0.22f, cy + r * 0.20f, ox, cy)
                                quadTo(ox + r * 0.22f, cy - r * 0.20f, ox, cy - r * 0.68f)
                            }
                            canvas.drawPath(wPath, paint)
                        }
                    }
                    60 -> {
                        val bolt = Path().apply {
                            moveTo(cx + r * 0.16f, cy - r * 0.86f)
                            lineTo(cx - r * 0.54f, cy + r * 0.06f)
                            lineTo(cx + r * 0.06f, cy + r * 0.06f)
                            lineTo(cx - r * 0.16f, cy + r * 0.86f)
                            lineTo(cx + r * 0.54f, cy - r * 0.06f)
                            lineTo(cx - r * 0.06f, cy - r * 0.06f)
                            close()
                        }
                        canvas.drawPath(bolt, paint)
                    }
                    61 -> {
                        canvas.drawCircle(cx, cy, r * 0.38f, paint)
                        for (i in 0 until 8) {
                            val a = Math.toRadians((i * 45).toDouble())
                            canvas.drawLine(
                                (cx + cos(a) * r * 0.54f).toFloat(),
                                (cy + sin(a) * r * 0.54f).toFloat(),
                                (cx + cos(a) * r * 0.84f).toFloat(),
                                (cy + sin(a) * r * 0.84f).toFloat(),
                                paint
                            )
                        }
                    }
                    62 -> {
                        val flask = Path().apply {
                            moveTo(cx - r * 0.24f, cy - r * 0.78f)
                            lineTo(cx + r * 0.24f, cy - r * 0.78f)
                            lineTo(cx + r * 0.24f, cy - r * 0.28f)
                            lineTo(cx + r * 0.74f, cy + r * 0.76f)
                            lineTo(cx - r * 0.74f, cy + r * 0.76f)
                            lineTo(cx - r * 0.24f, cy - r * 0.28f)
                            close()
                        }
                        canvas.drawPath(flask, paint)
                        canvas.drawLine(cx - r * 0.48f, cy + r * 0.28f, cx + r * 0.48f, cy + r * 0.28f, paint)
                    }
                    63 -> {
                        canvas.drawCircle(cx, cy, r * 0.42f, paint)
                        canvas.save(); canvas.rotate(-25f, cx, cy)
                        canvas.drawOval(RectF(cx - r * 0.88f, cy - r * 0.20f, cx + r * 0.88f, cy + r * 0.20f), paint)
                        canvas.restore()
                    }
                    64 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.48f, cx + r * 0.82f, cy + r * 0.48f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                    }
                    65 -> {
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.70f, cx - r * 0.44f, cy - r * 0.70f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy - r * 0.70f, cx - r * 0.72f, cy + r * 0.70f, paint)
                        canvas.drawLine(cx - r * 0.72f, cy + r * 0.70f, cx - r * 0.44f, cy + r * 0.70f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.70f, cx + r * 0.44f, cy - r * 0.70f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy - r * 0.70f, cx + r * 0.72f, cy + r * 0.70f, paint)
                        canvas.drawLine(cx + r * 0.72f, cy + r * 0.70f, cx + r * 0.44f, cy + r * 0.70f, paint)
                        canvas.drawCircle(cx - r * 0.26f, cy - r * 0.26f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy - r * 0.26f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx - r * 0.26f, cy + r * 0.26f, r * 0.10f, dotPaint)
                        canvas.drawCircle(cx + r * 0.26f, cy + r * 0.26f, r * 0.10f, dotPaint)
                    }
                    100 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.85f, cy - r * 0.80f, cx + r * 0.85f, cy + r * 0.80f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawLine(cx - r * 0.85f, cy - r * 0.35f, cx + r * 0.85f, cy - r * 0.35f, paint)
                        canvas.drawRoundRect(RectF(cx + r * 0.08f, cy + r * 0.05f, cx + r * 0.58f, cy + r * 0.52f), r * 0.08f, r * 0.08f, dotPaint)
                    }
                    101 -> {
                        canvas.drawCircle(cx, cy, r * 0.86f, paint)
                        canvas.drawLine(cx, cy - r * 0.48f, cx, cy, paint)
                        canvas.drawLine(cx, cy, cx + r * 0.36f, cy + r * 0.24f, paint)
                    }
                    200 -> {
                        canvas.drawCircle(cx - r * 0.12f, cy - r * 0.12f, r * 0.52f, paint)
                        canvas.drawLine(cx + r * 0.25f, cy + r * 0.25f, cx + r * 0.78f, cy + r * 0.78f, paint)
                    }
                    201 -> {
                        canvas.drawLine(cx - r * 0.60f, cy - r * 0.25f, cx - r * 0.60f, cy + r * 0.25f, paint)
                        canvas.drawLine(cx - r * 0.20f, cy - r * 0.65f, cx - r * 0.20f, cy + r * 0.65f, paint)
                        canvas.drawLine(cx + r * 0.20f, cy - r * 0.42f, cx + r * 0.20f, cy + r * 0.42f, paint)
                        canvas.drawLine(cx + r * 0.60f, cy - r * 0.75f, cx + r * 0.60f, cy + r * 0.75f, paint)
                    }
                    202 -> {
                        canvas.drawCircle(cx, cy, r * 0.42f, paint)
                    }
                    203 -> {
                        canvas.drawCircle(cx - r * 0.22f, cy, r * 0.48f, paint)
                        canvas.drawCircle(cx + r * 0.22f, cy, r * 0.48f, paint)
                    }
                    301 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.78f, cy - r * 0.78f, cx + r * 0.78f, cy + r * 0.78f), r * 0.22f, r * 0.22f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy, cx + r * 0.35f, cy, paint)
                        canvas.drawLine(cx, cy - r * 0.35f, cx, cy + r * 0.35f, paint)
                    }
                    302 -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.82f, cy - r * 0.48f, cx + r * 0.82f, cy + r * 0.48f), r * 0.14f, r * 0.14f, paint)
                        canvas.drawCircle(cx, cy, r * 0.22f, paint)
                    }
                    303 -> {
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), -150f, 130f, false, paint)
                        canvas.drawArc(RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f), 30f, 130f, false, paint)
                        canvas.drawLine(cx + r * 0.35f, cy - r * 0.72f, cx + r * 0.68f, cy - r * 0.38f, paint)
                        canvas.drawLine(cx - r * 0.35f, cy + r * 0.72f, cx - r * 0.68f, cy + r * 0.38f, paint)
                    }
                    304 -> {
                        val heart = Path().apply {
                            moveTo(cx, cy + r * 0.76f)
                            lineTo(cx - r * 0.78f, cy - r * 0.10f)
                            arcTo(RectF(cx - r * 0.78f, cy - r * 0.72f, cx, cy - r * 0.05f), 180f, 180f, false)
                            arcTo(RectF(cx, cy - r * 0.72f, cx + r * 0.78f, cy - r * 0.05f), 180f, 180f, false)
                            close()
                        }
                        canvas.drawPath(heart, paint)
                    }
                    else -> {
                        canvas.drawRoundRect(RectF(cx - r * 0.68f, cy - r * 0.68f, cx + r * 0.68f, cy + r * 0.68f), r * 0.20f, r * 0.20f, paint)
                        canvas.drawCircle(cx, cy, r * 0.20f, dotPaint)
                    }
                }
            }

            override fun setAlpha(alpha: Int) { paint.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply {
            setBounds(0, 0, sizePx, sizePx)
        }
    }
}

// =========================================================================
// רכיב גלגל הצבעים השטוח 2D (Flat 2D Color Wheel) עבור Screen 5
// =========================================================================
class Flat2DColorWheelView(
    context: Context,
    private val onColorPicked: (Int) -> Unit
) : View(context) {

    private var currentHue: Float = 36f
    private var currentSat: Float = 0.90f
    private val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * resources.displayMetrics.density
    }

    fun setSelectedColor(color: Int) {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentSat = hsv[1]
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val cx = width / 2f
            val cy = height / 2f
            val maxR = min(width, height) * 0.44f
            val dx = event.x - cx
            val dy = event.y - cy
            val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            if (angle < 0f) angle += 360f
            currentHue = angle
            currentSat = (dist / maxR).coerceIn(0f, 1f)
            val col = Color.HSVToColor(floatArrayOf(currentHue, currentSat, 1f))
            onColorPicked(col)
            invalidate()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) * 0.44f
        if (r <= 0f) return

        val sweepColors = intArrayOf(
            Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED
        )
        wheelPaint.shader = SweepGradient(cx, cy, sweepColors, null)
        satPaint.shader = RadialGradient(cx, cy, r, Color.WHITE, Color.TRANSPARENT, Shader.TileMode.CLAMP)

        canvas.drawCircle(cx, cy, r, wheelPaint)
        canvas.drawCircle(cx, cy, r, satPaint)

        val rad = Math.toRadians(currentHue.toDouble())
        val kx = (cx + cos(rad) * (currentSat * r)).toFloat()
        val ky = (cy + sin(rad) * (currentSat * r)).toFloat()
        val knobR = 11f * resources.displayMetrics.density

        val pickedCol = Color.HSVToColor(floatArrayOf(currentHue, currentSat, 1f))
        canvas.drawCircle(kx, ky, knobR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pickedCol
            style = Paint.Style.FILL
        })
        canvas.drawCircle(kx, ky, knobR, knobPaint)
    }
}

// =========================================================================
// רכיב הסליידר השטוח (Thin #2C2C2C Line + Sky Blue #B1D4F9 Knob) עבור Screen 5
// =========================================================================
class MinimalistLineSliderView(
    context: Context,
    initialProgress: Float = 0.85f,
    private val onValueChanged: (Float) -> Unit
) : View(context) {

    var progress: Float = initialProgress.coerceIn(0f, 1f)
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private val d = resources.displayMetrics.density
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 44, 44, 44)
        strokeWidth = 2.0f * d
        strokeCap = Paint.Cap.ROUND
    }
    private val knobFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MinimalistTheme.ACCENT_SKY_BLUE
        style = Paint.Style.FILL
    }
    private val knobBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 44, 44, 44)
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * d
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            parent?.requestDisallowInterceptTouchEvent(true)
            val pad = 16f * d
            val usableW = (width - 2 * pad).coerceAtLeast(1f)
            progress = ((event.x - pad) / usableW).coerceIn(0f, 1f)
            onValueChanged(progress)
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 16f * d
        val cy = height / 2f
        canvas.drawLine(pad, cy, width - pad, cy, trackPaint)
        val kx = pad + progress * (width - 2 * pad)
        val kr = 7.5f * d
        canvas.drawCircle(kx, cy, kr, knobFillPaint)
        canvas.drawCircle(kx, cy, kr, knobBorderPaint)
    }
}

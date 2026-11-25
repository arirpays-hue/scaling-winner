package com.example.calculator

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

data class VersionReleaseNote(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: String,
    val title: String,
    val highlights: List<String>
)

object AppVersionTracker {

    const val DEVELOPER_NAME = "Harry"
    const val CURRENT_SEMANTIC_VERSION = "5.4.0 Pro"
    const val CURRENT_BUILD_NUMBER = 54
    const val CURRENT_RELEASE_DATE = "25/11/2026"
    private const val PREF_LAST_SEEN_BUILD = "app_tracker_last_seen_build"
    private const val PREF_LAST_PKG_UPDATE_TIME = "app_tracker_last_pkg_update_time"
    private const val PREF_UPDATE_COUNT = "app_tracker_update_count"
    private const val PREF_INSTALL_LOG = "app_tracker_install_log_v1"
    private const val PREF_CUSTOM_NOTES = "app_tracker_custom_notes_v1"

    val changelogHistory: List<VersionReleaseNote> = listOf(
                VersionReleaseNote(
            versionCode = 54,
            versionName = "v5.4.0 Pro",
            releaseDate = "25/11/2026",
            title = "כלי זמן וסטופרים (121 כלים), אנימציות ממשק, מחשבון צף לכל הכלים, ייצוא/גיבוי Drive והגנת קריסות",
            highlights = listOf(
                "הוספת 3 כלי זמן וסטופרים חדשים (Tabs 118..120 — סה״כ 121 כלים): סטופר מרווחים ואימון HIIT/Tabata, טיימר ספירה לאחור מרובה (Multi-Timer) ומחשבון הפרשי שעות ודדליין כולל חציית חצות (ProTimeAndTimersTools.kt)",
                "הוספת מנוע אנימציות ממשק מותאם לכל 4 ערכות הנושא (ProUiMotionEngine.kt) עם כפתור בהגדרות בשם 'אנימציות ממשק' וסמל וקטורי אישי",
                "הרחבת המחשבון המיני הצף (ProFloatingMiniCalc.kt) לתמיכה בכל המחשבונים והכלים, הצגת תוצאות ופקדים חיים בכלים מבוססי מקלדת ומעבר מהיר בין סוגי מחשבונים",
                "שדרוג מרכז הייצוא והגיבוי (ProExportBackupEngine.kt) לייצוא PDF ו-CSV אמיתי ולגיבוי/שחזור מלא מול Google Drive, וקיבוע כפתור 'הגדרות' בתחתית תפריט הצד בכל מצב גלילה",
                "שילוב מנוע הגנה שקט מפני קריסות, מצב בטוח (Safe Mode) וטיפול בכל מצבי הקצה המתמטיים והמערכתיים (ProCrashGuardAndRecovery.kt)"
            )
        ),       
        VersionReleaseNote(
            versionCode = 53,
            versionName = "v5.3.0 Pro",
            releaseDate = "24/11/2026",
            title = "מחשבון מיני צף פעיל, 18 ממירי יחידות חדשים ומנוע אנימציות אישי לכל כלי",
            highlights = listOf(
                "החלפת מצב ה-PiP הלא-לחיץ במחשבון מיני צף אמיתי ואינטראקטיבי מעל כל אפליקציה (ProFloatingMiniCalc.kt) — כולל מקלדת עובדת, גרירה חופשית לכל פינה במסך, מזעור לבועה עגולה, סנכרון תוצאות והיסטוריה והתאמה לכל 4 ערכות הנושא",
                "הוספת 18 ממירי יחידות מורחבים וייעודיים בקטגוריה חדשה (Tabs 100..117 — סה״כ 118 כלים): קידומות SI (10s), שטח קרקע (דונם/אקר/הקטר), מספרים גדולים (לאק/קרור/גוגול), זמן עברי ותרבותי (חלקים/יובל), צפיפות חוט (Tex/Denier/Ne), קלוריות מזון, משקל זהב (טרוי/טולה/קראט), נפח עץ (Board Foot/Cord), כושר קירור (TR/BTU), זרימת אוויר (CFM), קשיות מים (°dH/ppm), מתח פנים, פרמיטיביות ופרמאביליות, מומנט אינרציה, צפיפות וקיבול חום, נפח סגולי וצפיפות, מהירות ותאוצה זוויתית וצפיפות זרם/מוליכות/מטען",
                "עיצוב מסך אישי, קנבס מונפש חי, טבלת המרה מקבילה לכל היחידות, תובנות מקצועיות ו-4 שפות סמלים וקטוריים לכל אחד מ-18 הממירים החדשים (ProExtendedUnitConverters.kt)",
                "בניית מנוע 'אנימציות ואפקטים' בקנבס (ProCanvasAnimations.kt) המעניק לכל אחד מ-118 הכלים אנימציית רקע ומעבר ייחודית המשתנה לפי ערכת הנושא (פסטל מינימליסטי, הברקות זכוכית, ניאון זוהר צפוני וקלאסי), כולל כפתור הפעלה/כיבוי ייעודי בהגדרות"
            )
        ),
        VersionReleaseNote(
            versionCode = 52,
            versionName = "v5.2.0 Pro",
            releaseDate = "19/11/2026",
            title = "מנוע כלי טקסט מהיר (130,000+ תווים), שדרוג פרטי רשת ו-4 שפות אייקונים",
            highlights = listOf(
                "בניית מנוע כלי טקסט מואץ בקובץ ייעודי (ProTextToolsStudio.kt) עם עיבוד אסינכרוני ברקע ל-130,000+ תווים ללא תקיעות, הצגת כל 11 כלי הבסיס ו-13 כלי ה-Pro ברשת RTL גלויה, בטל/חזור, הדבקה וחיפוש והחלפה",
                "תיקון מיון וערבוב שורות כך ששורות ריקות עוברות תמיד לסוף ואינן גורמות למסך ריק בראש התיבה",
                "שדרוג מנוע 'פרטי רשת וכתובת IP' (Tab 99) באמצעות LinkProperties לעבודה מלאה גם ברשת סלולרית (4G/5G) וגם ב-Wi-Fi עם שליפה חיה של IP חיצוני, IPv4/IPv6 מקומי, Gateway, Subnet, DNS, ספק אינטרנט ו-Ping",
                "בניית 4 שפות סמלים וקטוריים נפרדות ומובחנות לכל 100 הכלים ו-14 ההגדרות (ProUnifiedThemeIcons.kt): קו דק ונקי במינימליסטי, תגיות Material צבעוניות בקלאסי, אריחי זכוכית iOS ב-Liquid Glass והילת ניאון ב-Aurora"
            )
        ),
        VersionReleaseNote(
            versionCode = 51,
            versionName = "v5.1.0 Pro",
            releaseDate = "18/11/2026",
            title = "19 כלים פיננסיים, מתמטיים, חיישנים ורשת חדשים עם קנבסים חיים",
            highlights = listOf(
                "הוספת 15 כלים פיננסיים ומתמטיים חדשים (Tabs 81..95): כלי טקסט, ספרות רומיות עם אבני פירוק, סדרות מספרים עם גרף דו-כיווני, פרופורציות ויחס עם תרשים עוגה ומשוואה לינארית עם מערכת צירים מדויקת",
                "הוספת מחשבוני יעד חיסכון וזמן הכפלה, ממיר שכר, הנחות מצטברות ומס שירותים (Waterfall), תשואת מניות ודיבידנד, פיקדון קבוע וריבית פשוטה, SIP ואינפלציה, הלוואת רכב ו-E.M.I, ערך עתידי/נוכחי, אנונה ומחשבון פרישה",
                "הוספת 4 כלי חיישנים ורשת חדשים (Tabs 96..99): מד אור סביבתי (Lux), בדיקת מהירות אינטרנט מונפשת, קלינומטר ומד שיפוע עם כיול Tare ופרטי רשת וכתובת IP"
            )
        ),
        VersionReleaseNote(
            versionCode = 50,
            versionName = "v5.0.0 Pro",
            releaseDate = "18/11/2026",
            title = "מנוע החלקות ומחוות במסך המחשבון וליטוש אולפן הצבעים",
            highlights = listOf(
                "הוספת מערכת 'החלקות במסך המחשבון' (CalculatorGestureManager) בהגדרות לתמיכה ב-13 סוגי החלקות ומחוות על צג המחשבון הרגיל והמדעי (4 כיוונים ראשיים, 4 אלכסונים, החלקות ב-2 אצבעות, נקישה בודדת, נקישה כפולה ולחיצה ארוכה)",
                "קטלוג של 36 פעולות לבחירה לכל החלקה: ניקוי כל המסך (AC), מחיקת ספרה אחרונה (⌫), מחיקת מספר אחרון (CE), חישוב (=), שליפת תוצאה קודמת (Ans), שורש, ריבוע, הוספה/הורדת מע״מ 18%, פעולות זיכרון, הקראה קולית, מעבר למדעי ועוד",
                "משטח בדיקת החלקות חי (Live Test Pad) בתוך ההגדרות, 3 פרופילים מהירים לבחירה ובקרת רגישות אורך החלקה",
                "שדרוג אולפן גלגל הצבעים: ייצוב הבורר בתוך גלגל הצבעים כך שלא יזוז בעת הזזת הסליידרים, הוספת סמלים וקטוריים תואמים ל-4 הסליידרים בכל ערכות הנושא, ומיזוג שקיפות חי בעריכת צבע הרקע הראשי"
            )
        ),
        VersionReleaseNote(
            versionCode = 49,
            versionName = "v4.9.0 Pro",
            releaseDate = "18/11/2026",
            title = "אולפן גלגל צבעים 60fps, 4 סליידרים חיים והרמוניות צבעים",
            highlights = listOf(
                "פתיחת טווח הצבעים המלא (16.7 מיליון גוונים מ-0% עד 100%) כולל שחור מוחלט (#000000), לבן טהור (#FFFFFF), גווני אפור, פסטלים וניאון ללא חסימות",
                "שדרוג ל-4 סליידרים גרפיים חיים ב-60fps (גוון Hue 0°–360°, רוויה Saturation, בהירות Brightness ושקיפות Opacity עם רשת משבצות) וכפתורי -/+ לדיוק של אחוז בודד",
                "תיקון תגובת הסליידרים וגלגל הצבעים בתוך מסך ההגדרות (נעילת ScrollView בזמן גרירה) והוספת זכוכית מגדלת (Loupe) וכוונת דיוק",
                "חישוב אוטומטי של 5 הרמוניות צבעים חיות (משלים, סמוכים וטריאדה), גווני קצה מהירים וחלונית להזנה ישירה של קוד HEX",
                "איגוד מנוע 18 האלמנטים וגלגל הצבעים בתוך כפתור 'התאמה אישית' מתקפל בהגדרות הנסגר בלחיצה על 'סיום'"
            )
        ),
        VersionReleaseNote(
            versionCode = 48,
            versionName = "v4.8.0 Pro",
            releaseDate = "18/11/2026",
            title = "מעקב גרסאות, מנוע משוואות בפתקים וצגים חיים",
            highlights = listOf(
                "הוספת מערכת מעקב גרסאות, זיהוי עדכונים אוטומטי וחלון 'אודות' בהגדרות (נבנה על ידי Harry)",
                "מנוע מתמטי ואלגברי בפתקים חכמים (SmartNotesMathEngine) הפותר תרגילים, משוואות לינאריות, ריבועיות, מערכות משוואות ואי-שוויונות",
                "מקלדת מתמטית קבועה ב-3 שורות בפתקים (+, −, ×, ÷, חזקות ונעלמים) ושמירת פתק כטקסט רגיל כאשר לא נלחץ כפתור הפתרון",
                "שדרוג ממיר הצבעים (HEX / RGB / HSL / HSV / CMYK) עם קוד HEX לצבע המשלים והעתקה בלחיצה",
                "צגים ויזואליים חיים בגאומטריה 2D/3D ובמרווח רווח, נקודת איזון וכלל תקציב 50/30/20"
            )
        ),
        VersionReleaseNote(
            versionCode = 47,
            versionName = "v4.7.0 Pro",
            releaseDate = "17/11/2026",
            title = "גלאי מתכות תת-קרקעי, Battery Care Pro ומטרונום אקוסטי",
            highlights = listOf(
                "שדרוג גלאי המתכות להבחנה בין השדה המגנטי הטבעי של כדור הארץ לבין מתכת חבויה באדמה או בקיר (פיצוי כבידה וזווית Dip)",
                "שדרוג כלי הסוללה ל-Battery Care Pro: אופטימיזציית צריכה וקירור בלחיצה, אשף כיול BMS ושומר טעינה ב-80%",
                "מנוע סאונד אקוסטי אמיתי למטרונום (סינתזת PCM WAV של נקישת עץ מכנית, קליק אולפן ומקל תופים)",
                "מנוע סידור כיוון טקסט דו-כיווני (BidiFmt RTL/LTR) למניעת היפוך מספרים, יחידות וסוגריים בעברית"
            )
        ),
        VersionReleaseNote(
            versionCode = 45,
            versionName = "v4.5.0 Pro",
            releaseDate = "16/11/2026",
            title = "חבילת כלי מדידה, אלקטרוניקה, הצפנה ומידות",
            highlights = listOf(
                "הוספת מד רטט וסיסמוגרף חי (Vibrometer), צופר חירום (Siren) ומונה לחיצות חכם (Tally Counter)",
                "הוספת מפענח קודי צבע של סלילים (Inductors), סימולטור שערים לוגיים וטבלאות אמת ומחשבון חשבון חשמל",
                "הוספת מעבדת הצפנה ו-Hash (צופן קיסר, Base64, MD5, SHA-256) עם גלגל צופן חי",
                "הוספת כלי מדידת טבעת מכויל על המסך (מול מטבע ישראלי או אינצ'ים) וממיר מידות נעליים, בגדים וכובעים"
            )
        ),
        VersionReleaseNote(
            versionCode = 40,
            versionName = "v4.0.0 Pro",
            releaseDate = "15/11/2026",
            title = "עיצוב מינימליסטי, 48 פלטות צבעים ווידג׳ט מצפן",
            highlights = listOf(
                "השקת ערכת הנושא 'עיצוב מינימליסטי (Pastel Light)' עם 105+ סמלים וקטוריים נקיים",
                "מנוע התאמת צבעים אישית ל-18 אלמנטים באפליקציה ו-12 פלטות צבעים מותאמות לכל ערכת נושא",
                "וידג׳ט מצפן 3D חי למסך הבית של אנדרואיד עם תמיכה בעדכון גרסה ללא הסרת התקנה",
                "רשימת סימון לבחירת מחשבונים בתפריט הצד, התאמת יד דומיננטית ואפשרות בחירת צליל AC"
            )
        ),
        VersionReleaseNote(
            versionCode = 30,
            versionName = "v3.0.0 Pro",
            releaseDate = "10/11/2026",
            title = "עיצוב הזכוכית, הזוהר הצפוני ו-87 קטגוריות יחידות",
            highlights = listOf(
                "הוספת ערכות הנושא 'Liquid Glass Pro' ו-'Aurora Borealis (זוהר הצפון)'",
                "פתיחת מצב מקצועי (Pro Mode) עם 87 קטגוריות להמרת יחידות הנדסיות, מדעיות ורפואיות",
                "מעבדות אינסטלציה, מינון תרופות, בנייה ומיזוג, משכנתא, שכר, זהב, סטטיסטיקה, מטריצות וייצוא PDF/CSV"
            )
        ),
        VersionReleaseNote(
            versionCode = 20,
            versionName = "v2.0.0",
            releaseDate = "01/11/2026",
            title = "מחשבונים פיננסיים, מטבעות לייב, זיהוי קולי ו-PiP",
            highlights = listOf(
                "התרחבות ל-30 מחשבונים: מע״מ, הלוואות, ריבית דריבית, חסכונות, דלק, ממוצע ציונים, BMI, BMR ושעון עולמי",
                "המרת שערי מטבעות בזמן אמת, קלט קולי בעברית, הקראת תוצאות (TTS) ומצב חלון צף (Picture-in-Picture)"
            )
        ),
        VersionReleaseNote(
            versionCode = 10,
            versionName = "v1.0.0",
            releaseDate = "20/10/2026",
            title = "השקת אולר שווייצרי Pro fx-991",
            highlights = listOf(
                "מחשבון רגיל ומדעי מלא עם סדר פעולות חשבון, פונקציות טריגונומטריות, לוגריתמים ועצרת",
                "ניהול זיכרון (MC, MR, M+, M-), היסטוריית חישובים עם חיפוש, צלילי הקשה ומשוב רטט"
            )
        )
    )

    fun onAppLaunched(activity: MainActivity) {
        val prefs = activity.prefs ?: return
        val pkgInfo = getSafePackageInfo(activity)
        val pkgUpdateTime = pkgInfo?.lastUpdateTime ?: System.currentTimeMillis()
        val firstInstallTime = pkgInfo?.firstInstallTime ?: pkgUpdateTime

        val lastSeenBuild = prefs.getInt(PREF_LAST_SEEN_BUILD, -1)
        val lastSavedPkgTime = prefs.getLong(PREF_LAST_PKG_UPDATE_TIME, -1L)
        var updateCount = prefs.getInt(PREF_UPDATE_COUNT, 0)

        val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        val nowStr = dateFmt.format(Date(pkgUpdateTime))

        if (lastSeenBuild == -1) {
            val installDateStr = dateFmt.format(Date(firstInstallTime))
            val initialLog = buildString {
                append("מותקן: גרסה $CURRENT_SEMANTIC_VERSION (Build #$CURRENT_BUILD_NUMBER) בתאריך $nowStr")
                if (pkgUpdateTime > firstInstallTime + 60_000L) {
                    append("||התקנה ראשונית במכשיר: $installDateStr")
                }
            }
            updateCount = if (pkgUpdateTime > firstInstallTime + 60_000L) 2 else 1
            prefs.edit()
                .putInt(PREF_LAST_SEEN_BUILD, CURRENT_BUILD_NUMBER)
                .putLong(PREF_LAST_PKG_UPDATE_TIME, pkgUpdateTime)
                .putInt(PREF_UPDATE_COUNT, updateCount)
                .putString(PREF_INSTALL_LOG, initialLog)
                .apply()
        } else if (CURRENT_BUILD_NUMBER > lastSeenBuild || (lastSavedPkgTime > 0 && pkgUpdateTime > lastSavedPkgTime + 5_000L)) {
            updateCount += 1
            val prevLog = prefs.getString(PREF_INSTALL_LOG, "") ?: ""
            val newEntry = "שודרג לגרסה $CURRENT_SEMANTIC_VERSION (Build #$CURRENT_BUILD_NUMBER) בתאריך $nowStr (עדכון #$updateCount)"
            val updatedLog = if (prevLog.isBlank()) newEntry else "$newEntry||$prevLog"
            prefs.edit()
                .putInt(PREF_LAST_SEEN_BUILD, CURRENT_BUILD_NUMBER)
                .putLong(PREF_LAST_PKG_UPDATE_TIME, pkgUpdateTime)
                .putInt(PREF_UPDATE_COUNT, updateCount)
                .putString(PREF_INSTALL_LOG, updatedLog)
                .apply()

            Toast.makeText(
                activity,
                "✨ האפליקציה עודכנה לגרסה $CURRENT_SEMANTIC_VERSION (Build #$CURRENT_BUILD_NUMBER)!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getSafePackageInfo(context: Context): android.content.pm.PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun getButtonSummaryLabel(activity: MainActivity): String {
        val pkgInfo = getSafePackageInfo(activity)
        val pkgVerName = pkgInfo?.versionName ?: "1.0"
        return if (activity.isCleanGlassActive) {
            "אודות ומעקב גרסאות: v$CURRENT_SEMANTIC_VERSION (Build #$CURRENT_BUILD_NUMBER | APK $pkgVerName)"
        } else {
            "ℹ️ אודות ומעקב גרסאות: v$CURRENT_SEMANTIC_VERSION (Build #$CURRENT_BUILD_NUMBER)"
        }
    }

    fun createAboutSettingsButton(activity: MainActivity): TextView {
        return activity.createActionBtn(getButtonSummaryLabel(activity), false) {
            showAboutAndVersionsDialog(activity)
        }.apply {
            tag = "ABOUT_VERSION_SETTINGS_BTN"
        }
    }

    fun applyAboutButtonIcon(settingsRoot: View?, activity: MainActivity) {
        if (settingsRoot == null) return
        val iconTint = when {
            activity.isMinimalistMode -> MinimalistTheme.TEXT_CHARCOAL
            else -> MinimalistTheme.resolveDrawerIconColor(activity, isSelected = false, isExpandedHeader = false)
        }

        val customizerBtn = settingsRoot.findViewWithTag<TextView>("PERSONALIZATION_SETTINGS_BTN")
        if (customizerBtn != null) {
            val paletteIcon = ProUnifiedThemeIcons.createSettingsIcon(
                context = activity,
                settingKey = "customize",
                tintColor = iconTint,
                sizeDp = 22f,
                isMinimalist = activity.isMinimalistMode,
                isGlass = activity.isIosGlassMode,
                isAurora = activity.isAuroraMode
            )
            customizerBtn.compoundDrawablePadding = activity.dp(12f)
            if (activity.isRightHanded) {
                customizerBtn.setCompoundDrawables(null, null, paletteIcon, null)
            } else {
                customizerBtn.setCompoundDrawables(paletteIcon, null, null, null)
            }
        }

        val aboutBtn = settingsRoot.findViewWithTag<TextView>("ABOUT_VERSION_SETTINGS_BTN") ?: return
        val baseAboutIcon = createAboutVectorIcon(
            context = activity,
            tintColor = if (!activity.isMinimalistMode && !activity.isAuroraMode) Color.WHITE else iconTint,
            sizeDp = if (!activity.isMinimalistMode && !activity.isAuroraMode) 15f else 22f,
            isMinimalist = activity.isMinimalistMode,
            isAurora = activity.isAuroraMode
        )
        val finalAboutIcon = when {
            activity.isMinimalistMode -> baseAboutIcon
            activity.isAuroraMode -> baseAboutIcon
            activity.isIosGlassMode -> ProUnifiedThemeIcons.wrapForLiquidGlass(activity, baseAboutIcon, iconTint, 25f)
            else -> ProUnifiedThemeIcons.wrapForClassicBadge(activity, baseAboutIcon, 901, 25f)
        }
        aboutBtn.compoundDrawablePadding = activity.dp(12f)
        if (activity.isRightHanded) {
            aboutBtn.setCompoundDrawables(null, null, finalAboutIcon, null)
        } else {
            aboutBtn.setCompoundDrawables(finalAboutIcon, null, null, null)
        }
    }

    fun createAboutVectorIcon(
        context: Context,
        tintColor: Int,
        sizeDp: Float = 22f,
        isMinimalist: Boolean = false,
        isAurora: Boolean = false
    ): Drawable {
        val sizePx = (sizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(22)
        return object : Drawable() {
            private val d = context.resources.displayMetrics.density
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.STROKE
                strokeWidth = (if (isMinimalist) 1.70f else 1.85f) * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isAurora) {
                    setShadowLayer(5f * d, 0f, 0f, Color.argb(170, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)))
                }
            }
            private val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = tintColor
                style = Paint.Style.FILL
            }

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.40f

                if (isAurora) {
                    val haloP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        shader = RadialGradient(
                            cx, cy, (r * 1.15f).coerceAtLeast(1f),
                            Color.argb(65, Color.red(tintColor), Color.green(tintColor), Color.blue(tintColor)),
                            Color.TRANSPARENT,
                            Shader.TileMode.CLAMP
                        )
                    }
                    canvas.drawCircle(cx, cy, r * 1.15f, haloP)
                }

                // קו מתאר נקי בלבד — ללא עיגול רקע מאחור במינימליסטי!
                canvas.drawCircle(cx, cy, r * 0.80f, strokeP)
                canvas.drawCircle(cx, cy - r * 0.34f, r * 0.11f, dotP)
                canvas.drawLine(cx, cy - r * 0.06f, cx, cy + r * 0.42f, strokeP)
                canvas.drawLine(cx - r * 0.16f, cy + r * 0.42f, cx + r * 0.16f, cy + r * 0.42f, strokeP)
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; dotP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; dotP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    fun showAboutAndVersionsDialog(activity: MainActivity) {
        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = activity.isMinimalistMode
        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.LTGRAY
        val accentCol = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }

        val rootCardBg = when {
            isMin -> GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(MinimalistTheme.BG_CREAM)
                setStroke(activity.dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(activity, Color.parseColor("#E0DBD3")))
            }
            activity.isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(activity, 26f)
            else -> GradientDrawable().apply {
                cornerRadius = activity.dp(26f).toFloat()
                setColor(Utils.lightenColor(activity.bgColor, 0.08f))
                setStroke(activity.dp(1.2f), MinimalistTheme.resolveBorderStrokeColor(activity, activity.primaryColor))
            }
        }

        val outerRoot = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(activity.dp(18f), activity.dp(18f), activity.dp(18f), activity.dp(16f))
            background = rootCardBg
        }

        val badgeCanvas = object : View(activity) {
            private val p = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val d = resources.displayMetrics.density
                val w = width.toFloat(); val h = height.toFloat()

                val bgRect = RectF(4f * d, 4f * d, w - 4f * d, h - 4f * d)
                p.style = Paint.Style.FILL
                p.color = if (isMin) MinimalistTheme.DRAWER_HEADER_YELLOW else Color.argb(55, Color.red(accentCol), Color.green(accentCol), Color.blue(accentCol))
                canvas.drawRoundRect(bgRect, 20f * d, 20f * d, p)

                p.style = Paint.Style.STROKE; p.strokeWidth = 1.4f * d
                p.color = if (isMin) MinimalistTheme.TEXT_CHARCOAL else accentCol
                canvas.drawRoundRect(bgRect, 20f * d, 20f * d, p)

                p.style = Paint.Style.FILL; p.textAlign = Paint.Align.CENTER; p.typeface = Typeface.DEFAULT_BOLD
                val ink = if (isMin) MinimalistTheme.LOGO_TEXT_COLOR else Color.WHITE
                p.color = MinimalistTheme.ensureReadableTextOnBg(ink, p.color, 2.2)

                p.textSize = 17.5f * d
                canvas.drawText("אולר שווייצרי Pro fx-991", w / 2f, h * 0.28f, p)

                p.textSize = 14.0f * d
                val creditLine = "\u200Fפותח ונבנה על ידי ${BidiFmt.ltr(DEVELOPER_NAME)} 👨‍💻"
                CanvasUiHelper.drawFittedText(canvas, creditLine, w / 2f, h * 0.52f, w - 28f * d, 14.0f * d, 10f * d, p)

                p.textSize = 13.0f * d
                val verLine = "\u200Fגרסה ${BidiFmt.ltr("v$CURRENT_SEMANTIC_VERSION")}  •  ${BidiFmt.ltr("Build #$CURRENT_BUILD_NUMBER")}  •  ${BidiFmt.ltr("100")} כלים"
                CanvasUiHelper.drawFittedText(canvas, verLine, w / 2f, h * 0.76f, w - 28f * d, 13.0f * d, 9.5f * d, p)

                p.textSize = 11.5f * d
                val statsLine = "\u200F${BidiFmt.ltr("87")} קטגוריות יחידות  |  ${BidiFmt.ltr("4")} שפות אייקונים  |  מעודכן ✓"
                CanvasUiHelper.drawFittedText(canvas, statsLine, w / 2f, h * 0.92f, w - 28f * d, 11.5f * d, 8.5f * d, p)
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                activity.dp(132f)
            ).apply { bottomMargin = activity.dp(10f) }
        }
        outerRoot.addView(badgeCanvas)

        var activeFilterMode = 0
        val filterScroll = HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = activity.dp(10f) }
        }
        val filterRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        filterScroll.addView(filterRow)
        outerRoot.addView(filterScroll)

        val contentScroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.50f).toInt()
            )
        }
        val contentContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        contentScroll.addView(contentContainer)
        outerRoot.addView(contentScroll)

        fun buildTelemetrySummaryText(): String {
            val prefs = activity.prefs
            val pkgInfo = getSafePackageInfo(activity)
            val pkgVerName = pkgInfo?.versionName ?: "1.0"
            val pkgVerCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo?.longVersionCode ?: 1L
            } else {
                @Suppress("DEPRECATION")
                (pkgInfo?.versionCode ?: 1).toLong()
            }
            val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
            val firstInstallStr = pkgInfo?.firstInstallTime?.let { dateFmt.format(Date(it)) } ?: CURRENT_RELEASE_DATE
            val lastUpdateStr = pkgInfo?.lastUpdateTime?.let { dateFmt.format(Date(it)) } ?: CURRENT_RELEASE_DATE
            val totalUpdates = prefs?.getInt(PREF_UPDATE_COUNT, 1) ?: 1
            val favCount = activity.categories.indices.count { ProFavoritesEngine.isFavoriteTab(prefs, it) }
            val histCount = activity.historyList.size
            val themeName = when {
                activity.isMinimalistMode -> "עיצוב מינימליסטי (Pastel Light)"
                activity.isAuroraMode -> "הזוהר הצפוני (Aurora Borealis)"
                activity.isIosGlassMode -> "עיצוב הזכוכית (Liquid Glass Pro)"
                else -> "עיצוב קלאסי (Classic Pro)"
            }

            return BidiFmt.rtlParagraph(
                "📱 מידע טכני ומעקב התקנה במכשיר:",
                "• מפתח ויוצר האפליקציה: ${BidiFmt.ltr(DEVELOPER_NAME)}",
                "• גרסת מערכת פנימית: ${BidiFmt.ltr("v$CURRENT_SEMANTIC_VERSION (Build #$CURRENT_BUILD_NUMBER)")}",
                "• גרסת חבילת APK: ${BidiFmt.ltr("v$pkgVerName (Code $pkgVerCode)")} | תאריך שחרור: ${BidiFmt.ltr(CURRENT_RELEASE_DATE)}",
                "• התקנה ראשונית במכשיר: ${BidiFmt.ltr(firstInstallStr)}",
                "• עדכון אחרון במכשיר: ${BidiFmt.ltr(lastUpdateStr)} (סה״כ עדכונים: ${BidiFmt.ltr(totalUpdates)})",
                "• סביבת אנדרואיד: ${BidiFmt.ltr("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")} • ${BidiFmt.ltr(Build.MODEL)}",
                "• ערכת נושא פעילה: $themeName",
                "• נתונים שמורים: ${BidiFmt.ltr(histCount)} חישובים בהיסטוריה | ${BidiFmt.ltr(favCount)} כלים במועדפים"
            )
        }

        fun createVersionCardView(title: String, subHeader: String, bullets: List<String>, isHighlighted: Boolean): View {
            val cardBgCol = if (isMin) {
                if (isHighlighted) MinimalistTheme.ACCENT_MUTED_GREEN else MinimalistTheme.CARD_WHITE
            } else {
                if (isHighlighted) Color.argb(55, Color.red(accentCol), Color.green(accentCol), Color.blue(accentCol))
                else Color.argb(34, 255, 255, 255)
            }
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(activity.dp(15f), activity.dp(13f), activity.dp(15f), activity.dp(13f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(18f).toFloat()
                    setColor(cardBgCol)
                    val strokeCol = if (isHighlighted) accentCol else if (isMin) Color.parseColor("#E3DFD9") else Color.argb(45, 255, 255, 255)
                    setStroke(activity.dp(if (isHighlighted) 1.6f else 1f), strokeCol)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = activity.dp(9f) }
            }

            val titleTv = TextView(activity).apply {
                text = "\u200F${BidiFmt.fixRtlLineTokens(title)}"
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, cardBgCol, 2.3))
                textSize = 15.5f
                typeface = Typeface.DEFAULT_BOLD
                textDirection = View.TEXT_DIRECTION_RTL
                gravity = Gravity.RIGHT
            }
            card.addView(titleTv)

            if (subHeader.isNotBlank()) {
                val subTv = TextView(activity).apply {
                    text = "\u200F${BidiFmt.fixRtlLineTokens(subHeader)}"
                    setTextColor(MinimalistTheme.ensureReadableTextOnBg(textSecondary, cardBgCol, 2.0))
                    textSize = 13.0f
                    textDirection = View.TEXT_DIRECTION_RTL
                    gravity = Gravity.RIGHT
                    setPadding(0, activity.dp(2f), 0, activity.dp(6f))
                }
                card.addView(subTv)
            }

            val bodyLines = bullets.map { "• $it" }.toTypedArray()
            val bodyTv = TextView(activity).apply {
                text = BidiFmt.rtlParagraph(*bodyLines)
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, cardBgCol, 2.2))
                textSize = 14.0f
                textDirection = View.TEXT_DIRECTION_RTL
                gravity = Gravity.RIGHT
                setLineSpacing(activity.dp(2f).toFloat(), 1.05f)
            }
            card.addView(bodyTv)
            return card
        }

        lateinit var renderContent: () -> Unit

        val filterLabels = listOf(
            0 to "📋 כל הגרסאות (${changelogHistory.size})",
            1 to "✨ מה חדש ב-v$CURRENT_SEMANTIC_VERSION",
            2 to "📲 יומן עדכונים במכשיר"
        )

        fun rebuildFilterChips() {
            filterRow.removeAllViews()
            for ((modeIdx, label) in filterLabels) {
                val isSel = (activeFilterMode == modeIdx)
                val chipBg = if (isSel) accentCol else if (isMin) MinimalistTheme.NEUTRAL_SECONDARY_BG else Color.argb(40, 255, 255, 255)
                val chipTxt = MinimalistTheme.ensureReadableTextOnBg(
                    if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE,
                    chipBg,
                    2.3
                )
                val chip = TextView(activity).apply {
                    text = label
                    setTextColor(chipTxt)
                    textSize = 13.0f
                    typeface = if (isSel) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    gravity = Gravity.CENTER
                    setPadding(activity.dp(14f), activity.dp(8f), activity.dp(14f), activity.dp(8f))
                    background = GradientDrawable().apply {
                        cornerRadius = activity.dp(999f).toFloat()
                        setColor(chipBg)
                        if (isSel) setStroke(activity.dp(1.4f), if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE)
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(activity.dp(4f), 0, activity.dp(4f), 0) }
                    setOnClickListener {
                        activity.feedback(it)
                        activeFilterMode = modeIdx
                        rebuildFilterChips()
                        renderContent()
                    }
                }
                filterRow.addView(chip)
            }
        }

        renderContent = {
            contentContainer.removeAllViews()

            contentContainer.addView(
                createVersionCardView(
                    title = "👨‍💻 קרדיט ופיתוח: נבנה על ידי $DEVELOPER_NAME",
                    subHeader = "יוצר ומפתח האפליקציה (Creator & Lead Developer)",
                    bullets = listOf(
                        "האפליקציה תוכננה, עוצבה ונבנתה מאפס על ידי $DEVELOPER_NAME",
                        "כוללת מנועי חישוב מדעיים ואלגבריים, 100 מחשבונים וכלי Pro, מנוע עריכת טקסטים מהיר, מחוות והחלקות, אולפן צבעים ב-60fps ו-4 שפות עיצוב ואייקונים"
                    ),
                    isHighlighted = true
                )
            )

            val telemetryLines = buildTelemetrySummaryText().lines().drop(1).map { it.removePrefix("\u200F• ").removePrefix("• ") }
            contentContainer.addView(
                createVersionCardView(
                    title = "📱 פרטי גרסה ומעקב מערכת",
                    subHeader = "לחץ על הכרטיס להעתקת דו״ח גרסה מלא ללוח",
                    bullets = telemetryLines,
                    isHighlighted = false
                ).apply {
                    setOnClickListener {
                        activity.feedback(it)
                        activity.copyToClipboard(buildTelemetrySummaryText())
                    }
                }
            )

            when (activeFilterMode) {
                0 -> {
                    for ((idx, rel) in changelogHistory.withIndex()) {
                        val isLatest = (idx == 0)
                        val badge = if (isLatest) "🟢 גרסה נוכחית" else "✔️ הושלם"
                        contentContainer.addView(
                            createVersionCardView(
                                title = "גרסה ${rel.versionName} (Build #${rel.versionCode}) — $badge",
                                subHeader = "${rel.title}  •  תאריך: ${rel.releaseDate}",
                                bullets = rel.highlights,
                                isHighlighted = isLatest
                            )
                        )
                    }
                }
                1 -> {
                    val latest = changelogHistory.first()
                    contentContainer.addView(
                        createVersionCardView(
                            title = "✨ מה חדש בגרסה ${latest.versionName} (Build #${latest.versionCode})",
                            subHeader = "${latest.title}  •  ${latest.releaseDate}",
                            bullets = latest.highlights,
                            isHighlighted = true
                        )
                    )
                }
                2 -> {
                    val installLogRaw = activity.prefs?.getString(PREF_INSTALL_LOG, "") ?: ""
                    val logEntries = installLogRaw.split("||").filter { it.isNotBlank() }
                    val customNotesRaw = activity.prefs?.getString(PREF_CUSTOM_NOTES, "") ?: ""
                    val customEntries = customNotesRaw.split("||").filter { it.isNotBlank() }

                    contentContainer.addView(
                        createVersionCardView(
                            title = "📲 יומן התקנות ושדרוגים שנרשמו במכשיר זה",
                            subHeader = "מעקב אוטומטי אחר עדכוני APK ומספרי Build",
                            bullets = if (logEntries.isNotEmpty()) logEntries else listOf("מותקנת גרסה $CURRENT_SEMANTIC_VERSION (Build #$CURRENT_BUILD_NUMBER)"),
                            isHighlighted = true
                        )
                    )

                    if (customEntries.isNotEmpty()) {
                        contentContainer.addView(
                            createVersionCardView(
                                title = "📝 הערות גרסה אישיות שנשמרו",
                                subHeader = "תיעוד אישי של בדיקות או שינויים",
                                bullets = customEntries,
                                isHighlighted = false
                            )
                        )
                    }

                    val noteBox = activity.createInputBox("הוסף הערת מעקב גרסה אישית ליומן...").apply {
                        tag = "HAS_CUSTOM_LIVE_WATCHER"
                        inputType = InputType.TYPE_CLASS_TEXT
                    }
                    val addNoteBtn = activity.createActionBtn("➕ שמור הערה ביומן הגרסאות", false) {
                        val txt = noteBox.text.toString().trim()
                        if (txt.isEmpty()) {
                            Toast.makeText(activity, "הזן טקסט להערת הגרסה", Toast.LENGTH_SHORT).show()
                            return@createActionBtn
                        }
                        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
                        val entry = "[Build #$CURRENT_BUILD_NUMBER | $nowStr]: $txt"
                        val prev = activity.prefs?.getString(PREF_CUSTOM_NOTES, "") ?: ""
                        val updated = if (prev.isBlank()) entry else "$entry||$prev"
                        activity.prefs?.edit()?.putString(PREF_CUSTOM_NOTES, updated)?.apply()
                        noteBox.setText("")
                        renderContent()
                        Toast.makeText(activity, "✅ הערת הגרסה נשמרה ביומן!", Toast.LENGTH_SHORT).show()
                    }
                    contentContainer.addView(noteBox)
                    contentContainer.addView(addNoteBtn)
                }
            }
        }

        rebuildFilterChips()
        renderContent()

        val bottomRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = activity.dp(10f) }
        }

        val copyReportBtn = TextView(activity).apply {
            val bgCol = if (isMin) MinimalistTheme.NEUTRAL_SECONDARY_BG else Color.argb(55, 255, 255, 255)
            text = "📋 העתק דו״ח גרסה"
            setTextColor(MinimalistTheme.ensureReadableTextOnBg(textPrimary, bgCol, 2.3))
            textSize = 14.0f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(activity.dp(12f), activity.dp(12f), activity.dp(12f), activity.dp(12f))
            background = GradientDrawable().apply {
                cornerRadius = activity.dp(999f).toFloat()
                setColor(bgCol)
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = activity.dp(6f)
            }
            setOnClickListener {
                activity.feedback(it)
                val fullReport = buildTelemetrySummaryText() + "\n\n" +
                    "✨ חידושי גרסה $CURRENT_SEMANTIC_VERSION (Build #$CURRENT_BUILD_NUMBER):\n" +
                    changelogHistory.first().highlights.joinToString("\n") { "• $it" }
                activity.copyToClipboard(fullReport)
            }
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
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = activity.dp(6f)
            }
            setOnClickListener {
                activity.feedback(it)
                dlg.dismiss()
            }
        }

        bottomRow.addView(copyReportBtn)
        bottomRow.addView(closeBtn)
        outerRoot.addView(bottomRow)

        dlg.setContentView(outerRoot)
        dlg.show()
        val widthPx = (activity.resources.displayMetrics.widthPixels * 0.92f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}

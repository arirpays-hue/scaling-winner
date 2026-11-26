package com.example.calculator

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import kotlin.math.min

// =========================================================================
// מרכז רישום מודולרי לחיבור קובצי מחשבונים עתידיים (ProFutureToolsRegistry.kt)
// מעכשיו הוספת מחשבונים חדשים דורשת רק יצירת הקובץ החדש + שורה אחת כאן ב-modules
// בלי צורך לעדכן שוב את MainActivity.kt!
// =========================================================================
object ProFutureToolsRegistry {

    /**
     * הגדרת מחשבון או כלי בודד בתוך מודול עתידי.
     */
    data class FutureToolSpec(
        val title: String,
        val useUniversalBidiVisualizer: Boolean = false,
        val iconProvider: ((activity: MainActivity, tintColor: Int, sizeDp: Float) -> Drawable)? = null,
        val screenBuilder: (activity: MainActivity) -> View
    )

    /**
     * הגדרת קטגוריה שלמה (מודול) המכילה כלי אחד או יותר בתפריט הצד.
     */
    data class FutureSectionModule(
        val sectionCode: Int, // התחל מ-315 והלאה (315, 316, 317...)
        val sectionTitle: String,
        val isProOnly: Boolean = true,
        val sectionIconProvider: ((activity: MainActivity, tintColor: Int, sizeDp: Float) -> Drawable)? = null,
        val tools: List<FutureToolSpec>,
        val onResume: ((MainActivity) -> Unit)? = null,
        val onPause: ((MainActivity) -> Unit)? = null,
        val onDestroy: ((MainActivity) -> Unit)? = null,
        val onActivityResult: ((MainActivity, Int, Int, Intent?) -> Boolean)? = null
    )

    // =========================================================================
    // כאן מוסיפים שורה אחת לכל קובץ מחשבונים חדש בעתיד!
    // דוגמה:
    //   ProChemistryLabTools.asModule(),
    //   ProFitnessCalcTools.asModule()
    // =========================================================================
    val modules: List<FutureSectionModule> = listOf(
        // הוסף כאן מודולים עתידיים (למשל sectionCode = 315 ומעלה)
    )

    // =========================================================================
    // API אוטומטי שנקרא מתוך MainActivity.kt (אין צורך לשנות בהמשך)
    // =========================================================================
    val extraCategories: List<String>
        get() = modules.flatMap { mod -> mod.tools.map { it.title } }

    fun getExtraDrawerSections(baseTabIndex: Int): List<Triple<Int, String, List<Int>>> {
        val out = mutableListOf<Triple<Int, String, List<Int>>>()
        var cursor = baseTabIndex
        for (mod in modules) {
            if (mod.tools.isEmpty()) continue
            val indices = (cursor until (cursor + mod.tools.size)).toList()
            out.add(Triple(mod.sectionCode, mod.sectionTitle, indices))
            cursor += mod.tools.size
        }
        return out
    }

    fun isProSection(secCode: Int): Boolean {
        if (secCode in 305..314) return true
        return modules.any { it.sectionCode == secCode && it.isProOnly }
    }

    fun isFutureTab(tabIndex: Int, baseTabIndex: Int): Boolean {
        val offset = tabIndex - baseTabIndex
        return offset >= 0 && offset < extraCategories.size
    }

    private fun resolveToolEntry(tabIndex: Int, baseTabIndex: Int): Pair<FutureSectionModule, FutureToolSpec>? {
        val offset = tabIndex - baseTabIndex
        if (offset < 0) return null
        var cursor = 0
        for (mod in modules) {
            if (offset < cursor + mod.tools.size) {
                return mod to mod.tools[offset - cursor]
            }
            cursor += mod.tools.size
        }
        return null
    }

    fun buildScreen(activity: MainActivity, tabIndex: Int, baseTabIndex: Int): View? {
        val pair = resolveToolEntry(tabIndex, baseTabIndex) ?: return null
        return pair.second.screenBuilder(activity)
    }

    fun shouldAttachUniversalBidiVisualizer(tabIndex: Int, baseTabIndex: Int): Boolean {
        val pair = resolveToolEntry(tabIndex, baseTabIndex) ?: return false
        return pair.second.useUniversalBidiVisualizer
    }

    fun createSectionIcon(
        activity: MainActivity,
        secCode: Int,
        tintColor: Int,
        sizeDp: Float
    ): Drawable? {
        val mod = modules.firstOrNull { it.sectionCode == secCode } ?: return null
        val custom = mod.sectionIconProvider?.invoke(activity, tintColor, sizeDp)
        return custom ?: createThemedDefaultIcon(activity, tintColor, sizeDp, secCode)
    }

    fun createToolIcon(
        activity: MainActivity,
        tabIndex: Int,
        baseTabIndex: Int,
        tintColor: Int,
        sizeDp: Float
    ): Drawable? {
        val pair = resolveToolEntry(tabIndex, baseTabIndex) ?: return null
        val custom = pair.second.iconProvider?.invoke(activity, tintColor, sizeDp)
        return custom ?: createThemedDefaultIcon(activity, tintColor, sizeDp, tabIndex)
    }

    // חיבור אוטומטי של אירועי מחזור חיים לכלים עתידיים שזקוקים לחיישנים או בוררי קבצים
    fun onHostResume(activity: MainActivity) {
        for (mod in modules) {
            ProCrashGuardAndRecovery.safeRun(Unit) { mod.onResume?.invoke(activity) }
        }
    }

    fun onHostPause(activity: MainActivity) {
        for (mod in modules) {
            ProCrashGuardAndRecovery.safeRun(Unit) { mod.onPause?.invoke(activity) }
        }
    }

    fun onHostDestroy(activity: MainActivity) {
        for (mod in modules) {
            ProCrashGuardAndRecovery.safeRun(Unit) { mod.onDestroy?.invoke(activity) }
        }
    }

    fun handleActivityResult(activity: MainActivity, requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        for (mod in modules) {
            val handled = ProCrashGuardAndRecovery.safeRun(false) {
                mod.onActivityResult?.invoke(activity, requestCode, resultCode, data) ?: false
            }
            if (handled) return true
        }
        return false
    }

    // אייקון ברירת מחדל וקטורי מותאם ל-4 ערכות הנושא עבור מודולים שלא הגדירו ציור מותאם
    fun createThemedDefaultIcon(
        activity: MainActivity,
        tintColor: Int,
        sizeDp: Float,
        badgeSeed: Int
    ): Drawable {
        val isMin = activity.isMinimalistMode
        val isAur = activity.isAuroraMode
        val isGlass = activity.isIosGlassMode
        val innerTint = if (isMin || isAur) tintColor else Color.WHITE
        val innerSize = if (isMin || isAur) sizeDp else sizeDp * 0.70f
        val d = activity.resources.displayMetrics.density
        val px = (innerSize * d).toInt().coerceAtLeast(20)

        val raw = object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = innerTint
                style = Paint.Style.STROKE
                strokeWidth = (if (isMin) 1.7f else 1.85f) * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                if (isAur) {
                    setShadowLayer(4f * d, 0f, 0f, Color.argb(170, Color.red(innerTint), Color.green(innerTint), Color.blue(innerTint)))
                }
            }

            override fun getIntrinsicWidth(): Int = px
            override fun getIntrinsicHeight(): Int = px

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.40f
                canvas.drawRoundRect(RectF(cx - r, cy - r, cx + r, cy + r), r * 0.28f, r * 0.28f, strokeP)
                canvas.drawLine(cx - r * 0.45f, cy, cx + r * 0.45f, cy, strokeP)
                canvas.drawLine(cx, cy - r * 0.45f, cx, cy + r * 0.45f, strokeP)
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, px, px) }

        return when {
            isMin || isAur -> raw
            isGlass -> ProUnifiedThemeIcons.wrapForLiquidGlass(activity, raw, tintColor, sizeDp + 3f)
            else -> ProUnifiedThemeIcons.wrapForClassicBadge(activity, raw, badgeSeed, sizeDp + 3f)
        }
    }
}

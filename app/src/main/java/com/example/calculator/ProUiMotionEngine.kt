package com.example.calculator

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.min

// =========================================================================
// מנוע אנימציות ממשק ושימוש (UI & Micro-Interaction Motion Engine)
// מחובר למאגר 10 החבילות (ProUiMotionPacks) ונשלט ע״י כפתור "אנימציות ממשק"
// לחיצה קצרה: הפעלה/כיבוי • לחיצה ארוכה: תפריט בחירת חבילת אנימציות
// =========================================================================
object ProUiMotionEngine {

    private const val PREF_KEY_UI_MOTION = "pro_ui_motion_enabled_v1"
    private const val SETTINGS_BTN_TAG = "PRO_UI_MOTION_TOGGLE_BTN"

    var isUiAnimationsEnabled: Boolean = true
        private set

    enum class UiMotionTheme {
        MINIMALIST_ZEN,
        LIQUID_GLASS_SPRING,
        AURORA_COSMIC,
        CLASSIC_PRECISION
    }

    fun resolveTheme(activity: MainActivity): UiMotionTheme = when {
        activity.isMinimalistMode -> UiMotionTheme.MINIMALIST_ZEN
        activity.isAuroraMode -> UiMotionTheme.AURORA_COSMIC
        activity.isIosGlassMode -> UiMotionTheme.LIQUID_GLASS_SPRING
        else -> UiMotionTheme.CLASSIC_PRECISION
    }

    fun loadPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        isUiAnimationsEnabled = prefs.getBoolean(PREF_KEY_UI_MOTION, true)
        ProUiMotionPacks.loadPrefs(prefs)
    }

    fun setEnabled(activity: MainActivity, enabled: Boolean) {
        isUiAnimationsEnabled = enabled
        activity.prefs?.edit()?.putBoolean(PREF_KEY_UI_MOTION, enabled)?.apply()
        if (!enabled) {
            activity.mainContent?.apply {
                animate().cancel()
                scaleX = 1f
                scaleY = 1f
                translationX = 0f
                alpha = 1f
            }
        }
    }

    // =========================================================================
    // כפתור הגדרות ייעודי "אנימציות ממשק" + סמל וקטורי ל-4 ערכות הנושא
    // =========================================================================
    fun getSettingsButtonLabel(activity: MainActivity): String {
        val packName = ProUiMotionPacks.currentPack.shortName
        val status = if (isUiAnimationsEnabled) {
            if (activity.isCleanGlassActive) "מופעל • $packName" else "מופעל ($packName) 🟢"
        } else {
            if (activity.isCleanGlassActive) "כבוי (לחיצה ארוכה לחבילות)" else "כבוי 🔴"
        }
        return if (activity.isCleanGlassActive) {
            "אנימציות ממשק: $status"
        } else {
            "🎞️ אנימציות ממשק: $status"
        }
    }

    fun createSettingsToggleButton(activity: MainActivity): TextView {
        return activity.createActionBtn(getSettingsButtonLabel(activity), false) {
            val next = !isUiAnimationsEnabled
            setEnabled(activity, next)
            Toast.makeText(
                activity,
                if (next) "אנימציות ממשק הופעלו (${ProUiMotionPacks.currentPack.displayName}) • לחיצה ארוכה להחלפת חבילה"
                else "אנימציות ממשק כובו",
                Toast.LENGTH_SHORT
            ).show()
            activity.switchTab(19)
        }.apply {
            tag = SETTINGS_BTN_TAG
            isLongClickable = true
            setOnLongClickListener { v ->
                v.animate().cancel()
                v.animate().scaleX(1f).scaleY(1f).setDuration(110L).start()
                activity.feedback(v)
                ProUiMotionPacks.showPackPickerDialog(activity)
                true
            }
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

        val icon = createUiMotionIcon(activity, tintCol, 22f, isMin, isGlass, isAur)
        btn.compoundDrawablePadding = activity.dp(12f)
        if (activity.isRightHanded) {
            btn.setCompoundDrawables(null, null, icon, null)
        } else {
            btn.setCompoundDrawables(icon, null, null, null)
        }
    }

    fun createUiMotionIcon(
        activity: MainActivity,
        tintColor: Int,
        sizeDp: Float,
        isMinimalist: Boolean,
        isIosGlass: Boolean,
        isAurora: Boolean
    ): Drawable {
        val innerTint = if (isMinimalist || isAurora) tintColor else Color.WHITE
        val innerSizeDp = if (isMinimalist || isAurora) sizeDp else (sizeDp * 0.70f)
        val raw = createRawMotionGlyph(activity, innerTint, innerSizeDp, isMinimalist, isAurora)
        return when {
            isMinimalist || isAurora -> raw
            isIosGlass -> ProUnifiedThemeIcons.wrapForLiquidGlass(activity, raw, tintColor, sizeDp + 3f)
            else -> ProUnifiedThemeIcons.wrapForClassicBadge(activity, raw, 905, sizeDp + 3f)
        }
    }

    private fun createRawMotionGlyph(
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
                strokeWidth = (if (isMinimalist) 1.75f else 1.85f) * d
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

                // מסגרת כרטיס נעה + 3 קווי שובל תנועה + עיגול פנימי
                val card = RectF(cx - r * 0.25f, cy - r * 0.65f, cx + r * 0.82f, cy + r * 0.55f)
                canvas.drawRoundRect(card, r * 0.24f, r * 0.24f, strokeP)

                canvas.drawLine(cx - r * 0.84f, cy - r * 0.28f, cx - r * 0.46f, cy - r * 0.28f, strokeP)
                canvas.drawLine(cx - r * 0.72f, cy + r * 0.04f, cx - r * 0.46f, cy + r * 0.04f, strokeP)
                canvas.drawLine(cx - r * 0.58f, cy + r * 0.36f, cx - r * 0.46f, cy + r * 0.36f, strokeP)

                canvas.drawCircle(cx + r * 0.28f, cy - r * 0.05f, r * 0.18f, fillP)
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; fillP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; fillP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    // =========================================================================
    // 1. מעבר מסכים, כותרת עליונה וכוריאוגרפיית כרטיסים לפי החבילה הנבחרת
    // =========================================================================
    fun animateTabSwitch(
        activity: MainActivity,
        contentRoot: View?,
        titleView: TextView?,
        tabIndex: Int,
        wasInSettings: Boolean
    ) {
        if (contentRoot == null) return
        if (tabIndex != 0 && tabIndex != 1) {
            attachInteractiveMotionToHierarchy(activity, contentRoot)
        }
        if (!isUiAnimationsEnabled || wasInSettings) return

        ProUiMotionPacks.animateHeaderTitle(activity, titleView)

        if (tabIndex == 0 || tabIndex == 1) {
            ProUiMotionPacks.animateCalculatorEntrance(activity, contentRoot)
        } else {
            ProUiMotionPacks.animateToolCardsCascade(activity, contentRoot)
        }
    }

    // =========================================================================
    // 2. אנימציות מקלדת (מגע ושחרור פיזיקליים לפי החבילה הנבחרת)
    // =========================================================================
    fun animateKeypadTouchDown(activity: MainActivity, v: View, eventX: Float, eventY: Float, symbol: String) {
        if (!isUiAnimationsEnabled) return
        ProUiMotionPacks.animateKeypadTouchDown(activity, v, eventX, eventY, symbol)
    }

    fun animateKeypadTouchUp(activity: MainActivity, v: View, symbol: String) {
        if (!isUiAnimationsEnabled) {
            v.scaleX = 1f; v.scaleY = 1f; v.translationX = 0f; v.translationY = 0f
            v.rotation = 0f; v.rotationX = 0f; v.rotationY = 0f; v.alpha = 1f
            return
        }
        ProUiMotionPacks.animateKeypadTouchUp(activity, v, symbol)
    }

    // =========================================================================
    // 3. אנימציות צג התוצאה (הקלדה, שווה '=', ניקוי AC, שגיאה)
    // =========================================================================
    fun animateDisplayChange(activity: MainActivity, displayView: TextView?, isEquals: Boolean, isError: Boolean) {
        if (displayView == null || !isUiAnimationsEnabled) return
        ProUiMotionPacks.animateDisplayChange(activity, displayView, isEquals, isError)
    }

    fun animateDisplayClear(activity: MainActivity, displayView: TextView?, exprView: TextView?) {
        if (!isUiAnimationsEnabled) return
        ProUiMotionPacks.animateDisplayClear(activity, displayView, exprView)
    }

    // =========================================================================
    // 4. תפריט צד + אקורדיון קטגוריות
    // =========================================================================
    fun animateDrawerToggle(
        activity: MainActivity,
        isOpening: Boolean,
        drawerScroll: View,
        dimView: View,
        mainContent: View?,
        drawerWidthPx: Int
    ) {
        val dirSign = if (activity.isRightHanded) 1f else -1f
        val closedX = drawerWidthPx * dirSign

        if (!isUiAnimationsEnabled) {
            dimView.visibility = if (isOpening) View.VISIBLE else View.GONE
            dimView.alpha = if (isOpening) 1f else 0f
            drawerScroll.translationX = if (isOpening) 0f else closedX
            drawerScroll.rotationY = 0f
            mainContent?.apply { scaleX = 1f; scaleY = 1f; translationX = 0f }
            return
        }
        ProUiMotionPacks.animateDrawerToggle(activity, isOpening, drawerScroll, dimView, mainContent, drawerWidthPx)
    }

    fun animateSectionAccordionExpand(activity: MainActivity, container: View) {
        if (!isUiAnimationsEnabled) return
        ProUiMotionPacks.animateSectionAccordionExpand(activity, container)
    }

    // =========================================================================
    // 5. מיקרו-אנימציות לסמלי הכותרת העליונה, דיאלוגים, כפתורים ושדות קלט
    // =========================================================================
    fun animateHeaderIconTap(activity: MainActivity, view: View, kind: String) {
        if (!isUiAnimationsEnabled) return
        ProUiMotionPacks.animateHeaderIconTap(view, kind)
    }

    fun animateDialogEntrance(activity: MainActivity, dialogRoot: View) {
        if (!isUiAnimationsEnabled) return
        ProUiMotionPacks.animateDialogEntrance(activity, dialogRoot)
    }

    fun attachTactilePress(activity: MainActivity, view: View) {
        view.setOnTouchListener { v, e ->
            if (!isUiAnimationsEnabled) return@setOnTouchListener false
            ProUiMotionPacks.handleTactilePress(v, e.actionMasked)
            false
        }
    }

    fun attachInteractiveMotionToHierarchy(activity: MainActivity, root: View) {
        fun walk(v: View) {
            if (v is EditText) {
                val existing = v.onFocusChangeListener
                v.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                    existing?.onFocusChange(view, hasFocus)
                    if (!isUiAnimationsEnabled) return@OnFocusChangeListener
                    ProUiMotionPacks.handleInputFocus(activity, view, hasFocus)
                }
                return
            }
            if (v is TextView && v.tag == "PRO_STAR_CHIP") {
                attachTactilePress(activity, v)
                return
            }
            if (v is ViewGroup && v !is HorizontalScrollView) {
                for (i in 0 until v.childCount) {
                    walk(v.getChildAt(i))
                }
            } else if (v is HorizontalScrollView) {
                for (i in 0 until v.childCount) {
                    walk(v.getChildAt(i))
                }
            }
        }
        walk(root)
    }
}

package com.example.calculator

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.min

// =========================================================================
// מנוע אנימציות ממשק ושימוש (UI & Micro-Interaction Motion Engine)
// מותאם אישית לכל אחת מ-4 ערכות הנושא ונשלט ע״י כפתור "אנימציות ממשק"
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
        val status = if (isUiAnimationsEnabled) {
            if (activity.isCleanGlassActive) "מופעל" else "מופעל 🟢"
        } else {
            if (activity.isCleanGlassActive) "כבוי" else "כבוי 🔴"
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
                if (next) "אנימציות ממשק הופעלו" else "אנימציות ממשק כובו",
                Toast.LENGTH_SHORT
            ).show()
            activity.switchTab(19)
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

                // מסגרת כרטיס נעה + 2 קווי שובל תנועה + כוכב קפיץ
                val card = RectF(cx - r * 0.25f, cy - r * 0.65f, cx + r * 0.82f, cy + r * 0.55f)
                canvas.drawRoundRect(card, r * 0.24f, r * 0.24f, strokeP)

                // שובלי תנועה מדורגים משמאל
                canvas.drawLine(cx - r * 0.84f, cy - r * 0.28f, cx - r * 0.46f, cy - r * 0.28f, strokeP)
                canvas.drawLine(cx - r * 0.72f, cy + r * 0.04f, cx - r * 0.46f, cy + r * 0.04f, strokeP)
                canvas.drawLine(cx - r * 0.58f, cy + r * 0.36f, cx - r * 0.46f, cy + r * 0.36f, strokeP)

                // עיגול תנועה פנימי
                canvas.drawCircle(cx + r * 0.28f, cy - r * 0.05f, r * 0.18f, fillP)
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; fillP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; fillP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }

    // =========================================================================
    // 1. מעבר מסכים, כותרת עליונה וכוריאוגרפיית כרטיסים לפי ערכת נושא
    // =========================================================================
    fun animateTabSwitch(
        activity: MainActivity,
        contentRoot: View?,
        titleView: TextView?,
        tabIndex: Int,
        wasInSettings: Boolean
    ) {
        if (contentRoot == null) return
        // תמיד מחברים אינטראקציות מגע ופוקוס לשדות וכרטיסים (יבדקו בזמן אמת אם האנימציות פעילות)
        if (tabIndex != 0 && tabIndex != 1) {
            attachInteractiveMotionToHierarchy(activity, contentRoot)
        }
        if (!isUiAnimationsEnabled || wasInSettings) return

        val theme = resolveTheme(activity)
        animateHeaderTitle(activity, titleView, theme)

        if (tabIndex == 0 || tabIndex == 1) {
            animateCalculatorEntrance(activity, contentRoot, theme)
        } else {
            animateToolCardsCascade(activity, contentRoot, theme)
        }
    }

    private fun animateHeaderTitle(activity: MainActivity, titleView: TextView?, theme: UiMotionTheme) {
        if (titleView == null) return
        titleView.animate().cancel()
        val dir = if (activity.isRightHanded) 1f else -1f
        when (theme) {
            UiMotionTheme.MINIMALIST_ZEN -> {
                titleView.alpha = 0f
                titleView.translationY = activity.dp(6f).toFloat()
                titleView.animate().alpha(1f).translationY(0f)
                    .setDuration(260L).setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            UiMotionTheme.LIQUID_GLASS_SPRING -> {
                titleView.alpha = 0f
                titleView.scaleX = 0.90f; titleView.scaleY = 0.90f
                titleView.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(300L).setInterpolator(OvershootInterpolator(1.4f)).start()
            }
            UiMotionTheme.AURORA_COSMIC -> {
                titleView.alpha = 0f
                titleView.translationX = activity.dp(18f) * dir
                titleView.translationY = -activity.dp(4f).toFloat()
                titleView.animate().alpha(1f).translationX(0f).translationY(0f)
                    .setDuration(320L).setInterpolator(OvershootInterpolator(1.1f)).start()
            }
            UiMotionTheme.CLASSIC_PRECISION -> {
                titleView.alpha = 0f
                titleView.translationX = activity.dp(14f) * dir
                titleView.animate().alpha(1f).translationX(0f)
                    .setDuration(200L).setInterpolator(DecelerateInterpolator(1.8f)).start()
            }
        }
    }

    private fun animateCalculatorEntrance(activity: MainActivity, root: View, theme: UiMotionTheme) {
        root.animate().cancel()
        root.alpha = 1f
        root.translationY = 0f

        val vg = root as? ViewGroup
        val displayCard = vg?.getChildAt(0)
        if (displayCard != null) {
            displayCard.animate().cancel()
            displayCard.alpha = 0f
            when (theme) {
                UiMotionTheme.MINIMALIST_ZEN -> {
                    displayCard.translationY = -activity.dp(10f).toFloat()
                    displayCard.animate().alpha(1f).translationY(0f)
                        .setDuration(280L).setInterpolator(DecelerateInterpolator(1.5f)).start()
                }
                UiMotionTheme.LIQUID_GLASS_SPRING -> {
                    displayCard.scaleX = 0.92f; displayCard.scaleY = 0.92f
                    displayCard.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(320L).setInterpolator(OvershootInterpolator(1.25f)).start()
                }
                UiMotionTheme.AURORA_COSMIC -> {
                    displayCard.translationY = -activity.dp(16f).toFloat()
                    displayCard.scaleX = 0.94f; displayCard.scaleY = 0.94f
                    displayCard.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setDuration(340L).setInterpolator(OvershootInterpolator(1.3f)).start()
                }
                UiMotionTheme.CLASSIC_PRECISION -> {
                    displayCard.translationY = -activity.dp(12f).toFloat()
                    displayCard.animate().alpha(1f).translationY(0f)
                        .setDuration(190L).setInterpolator(DecelerateInterpolator(2.0f)).start()
                }
            }
        }

        val buttons = activity.keypadButtons.values.toList()
        val d = activity.resources.displayMetrics.density
        for ((idx, btn) in buttons.withIndex()) {
            btn.animate().cancel()
            btn.alpha = 0f
            btn.rotationX = 0f; btn.rotationY = 0f
            val delay = (idx * when (theme) {
                UiMotionTheme.MINIMALIST_ZEN -> 10L
                UiMotionTheme.LIQUID_GLASS_SPRING -> 13L
                UiMotionTheme.AURORA_COSMIC -> 14L
                UiMotionTheme.CLASSIC_PRECISION -> 8L
            }).coerceAtMost(240L)

            when (theme) {
                UiMotionTheme.MINIMALIST_ZEN -> {
                    btn.translationY = 9f * d
                    btn.scaleX = 0.95f; btn.scaleY = 0.95f
                    btn.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(240L)
                        .setInterpolator(DecelerateInterpolator(1.5f)).start()
                }
                UiMotionTheme.LIQUID_GLASS_SPRING -> {
                    btn.scaleX = 0.80f; btn.scaleY = 0.80f
                    btn.translationY = 12f * d
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                        .setStartDelay(delay).setDuration(290L)
                        .setInterpolator(OvershootInterpolator(1.45f)).start()
                }
                UiMotionTheme.AURORA_COSMIC -> {
                    btn.cameraDistance = 6000f * d
                    btn.rotationX = -18f
                    btn.scaleX = 0.84f; btn.scaleY = 0.84f
                    btn.translationY = 14f * d
                    btn.animate().alpha(1f).rotationX(0f).scaleX(1f).scaleY(1f).translationY(0f)
                        .setStartDelay(delay).setDuration(310L)
                        .setInterpolator(OvershootInterpolator(1.35f)).start()
                }
                UiMotionTheme.CLASSIC_PRECISION -> {
                    btn.scaleX = 0.90f; btn.scaleY = 0.90f
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(175L)
                        .setInterpolator(DecelerateInterpolator(1.9f)).start()
                }
            }
        }
    }

    private fun animateToolCardsCascade(activity: MainActivity, root: View, theme: UiMotionTheme) {
        val container: ViewGroup? = when (root) {
            is ScrollView -> root.getChildAt(0) as? ViewGroup
            is ViewGroup -> root
            else -> null
        }
        if (container == null) {
            root.animate().cancel()
            root.alpha = 0f
            root.animate().alpha(1f).setDuration(220L).start()
            return
        }

        val d = activity.resources.displayMetrics.density
        val count = min(container.childCount, 12)
        for (i in 0 until count) {
            val child = container.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue
            child.animate().cancel()
            child.alpha = 0f
            val delay = i * when (theme) {
                UiMotionTheme.MINIMALIST_ZEN -> 26L
                UiMotionTheme.LIQUID_GLASS_SPRING -> 30L
                UiMotionTheme.AURORA_COSMIC -> 32L
                UiMotionTheme.CLASSIC_PRECISION -> 18L
            }

            when (theme) {
                UiMotionTheme.MINIMALIST_ZEN -> {
                    child.translationY = 14f * d
                    child.translationX = 0f
                    child.scaleX = 0.985f; child.scaleY = 0.985f
                    child.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(280L)
                        .setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                UiMotionTheme.LIQUID_GLASS_SPRING -> {
                    child.translationY = 22f * d
                    child.translationX = 0f
                    child.scaleX = 0.93f; child.scaleY = 0.93f
                    child.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(330L)
                        .setInterpolator(OvershootInterpolator(1.1f)).start()
                }
                UiMotionTheme.AURORA_COSMIC -> {
                    val xOff = (if (activity.isRightHanded) 16f else -16f) * d
                    child.translationX = xOff
                    child.translationY = 20f * d
                    child.scaleX = 0.94f; child.scaleY = 0.94f
                    child.animate().alpha(1f).translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(340L)
                        .setInterpolator(OvershootInterpolator(1.05f)).start()
                }
                UiMotionTheme.CLASSIC_PRECISION -> {
                    val xOff = (if (i % 2 == 0) 18f else -18f) * d
                    child.translationX = xOff
                    child.translationY = 8f * d
                    child.animate().alpha(1f).translationX(0f).translationY(0f)
                        .setStartDelay(delay).setDuration(210L)
                        .setInterpolator(DecelerateInterpolator(2.0f)).start()
                }
            }
        }
    }

    // =========================================================================
    // 2. אנימציות מקלדת (מגע ושחרור פיזיקליים + הטיה תלת-ממדית לפי ערכת נושא)
    // =========================================================================
    fun animateKeypadTouchDown(activity: MainActivity, v: View, eventX: Float, eventY: Float, symbol: String) {
        if (!isUiAnimationsEnabled) return
        val theme = resolveTheme(activity)
        val d = activity.resources.displayMetrics.density
        v.animate().cancel()

        when (theme) {
            UiMotionTheme.MINIMALIST_ZEN -> {
                v.animate().scaleX(0.94f).scaleY(0.94f).translationY(1.5f * d)
                    .setDuration(75L).setInterpolator(DecelerateInterpolator()).start()
            }
            UiMotionTheme.LIQUID_GLASS_SPRING -> {
                v.cameraDistance = 5500f * d
                val nx = if (v.width > 0) ((eventX / v.width) - 0.5f).coerceIn(-0.5f, 0.5f) else 0f
                val ny = if (v.height > 0) ((eventY / v.height) - 0.5f).coerceIn(-0.5f, 0.5f) else 0f
                v.animate()
                    .scaleX(0.91f).scaleY(0.91f)
                    .rotationY(nx * 14f).rotationX(-ny * 14f)
                    .setDuration(70L).setInterpolator(DecelerateInterpolator()).start()
            }
            UiMotionTheme.AURORA_COSMIC -> {
                v.cameraDistance = 5500f * d
                val isHighlight = symbol in listOf("=", "AC", "+", "−", "×", "÷")
                val scale = if (isHighlight) 0.87f else 0.90f
                val nx = if (v.width > 0) ((eventX / v.width) - 0.5f).coerceIn(-0.5f, 0.5f) else 0f
                v.animate()
                    .scaleX(scale).scaleY(scale)
                    .rotationY(nx * 16f)
                    .translationY(2f * d)
                    .setDuration(65L).setInterpolator(DecelerateInterpolator()).start()
            }
            UiMotionTheme.CLASSIC_PRECISION -> {
                v.animate().scaleX(0.91f).scaleY(0.91f)
                    .setDuration(50L).setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }

    fun animateKeypadTouchUp(activity: MainActivity, v: View, symbol: String) {
        if (!isUiAnimationsEnabled) {
            v.scaleX = 1f; v.scaleY = 1f; v.translationY = 0f; v.rotationX = 0f; v.rotationY = 0f
            return
        }
        val theme = resolveTheme(activity)
        v.animate().cancel()
        when (theme) {
            UiMotionTheme.MINIMALIST_ZEN -> {
                v.animate().scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f).rotationY(0f)
                    .setDuration(210L).setInterpolator(DecelerateInterpolator(1.5f)).start()
            }
            UiMotionTheme.LIQUID_GLASS_SPRING -> {
                v.animate().scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f).rotationY(0f)
                    .setDuration(320L).setInterpolator(OvershootInterpolator(2.0f)).start()
            }
            UiMotionTheme.AURORA_COSMIC -> {
                val overshoot = if (symbol == "=") 2.4f else 1.8f
                v.animate().scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f).rotationY(0f)
                    .setDuration(300L).setInterpolator(OvershootInterpolator(overshoot)).start()
            }
            UiMotionTheme.CLASSIC_PRECISION -> {
                v.animate().scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f).rotationY(0f)
                    .setDuration(150L).setInterpolator(OvershootInterpolator(1.2f)).start()
            }
        }
    }

    // =========================================================================
    // 3. אנימציות צג התוצאה (הקלדה, שווה '=', ניקוי AC, שגיאה)
    // =========================================================================
    fun animateDisplayChange(activity: MainActivity, displayView: TextView?, isEquals: Boolean, isError: Boolean) {
        if (displayView == null || !isUiAnimationsEnabled) return
        val theme = resolveTheme(activity)
        val d = activity.resources.displayMetrics.density
        displayView.animate().cancel()

        if (isError) {
            val shake = ObjectAnimator.ofFloat(
                displayView, "translationX",
                0f, 12f * d, -12f * d, 8f * d, -8f * d, 4f * d, 0f
            )
            shake.duration = 360L
            shake.start()
            return
        }

        if (isEquals) {
            when (theme) {
                UiMotionTheme.MINIMALIST_ZEN -> {
                    displayView.alpha = 0.45f
                    displayView.translationY = 10f * d
                    displayView.animate().alpha(1f).translationY(0f)
                        .setDuration(260L).setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                UiMotionTheme.LIQUID_GLASS_SPRING -> {
                    displayView.pivotX = displayView.width.toFloat()
                    displayView.pivotY = displayView.height * 0.5f
                    displayView.scaleX = 0.84f; displayView.scaleY = 0.84f
                    displayView.alpha = 0.55f
                    displayView.animate().scaleX(1f).scaleY(1f).alpha(1f)
                        .setDuration(340L).setInterpolator(OvershootInterpolator(1.8f)).start()
                }
                UiMotionTheme.AURORA_COSMIC -> {
                    displayView.pivotX = displayView.width.toFloat()
                    displayView.pivotY = displayView.height * 0.5f
                    displayView.scaleX = 1.14f; displayView.scaleY = 1.14f
                    displayView.translationY = -8f * d
                    displayView.animate().scaleX(1f).scaleY(1f).translationY(0f)
                        .setDuration(350L).setInterpolator(OvershootInterpolator(1.6f)).start()
                }
                UiMotionTheme.CLASSIC_PRECISION -> {
                    displayView.scaleX = 0.92f; displayView.scaleY = 0.92f
                    displayView.animate().scaleX(1f).scaleY(1f)
                        .setDuration(190L).setInterpolator(OvershootInterpolator(1.3f)).start()
                }
            }
            return
        }

        // עדכון ספרה רגיל בהקלדה
        when (theme) {
            UiMotionTheme.MINIMALIST_ZEN -> {
                displayView.alpha = 0.72f
                displayView.animate().alpha(1f).setDuration(140L).start()
            }
            UiMotionTheme.LIQUID_GLASS_SPRING -> {
                displayView.pivotX = displayView.width.toFloat()
                displayView.pivotY = displayView.height * 0.5f
                displayView.scaleX = 0.965f; displayView.scaleY = 0.965f
                displayView.animate().scaleX(1f).scaleY(1f)
                    .setDuration(180L).setInterpolator(OvershootInterpolator(1.3f)).start()
            }
            UiMotionTheme.AURORA_COSMIC -> {
                displayView.translationY = 4f * d
                displayView.alpha = 0.65f
                displayView.animate().translationY(0f).alpha(1f)
                    .setDuration(170L).setInterpolator(DecelerateInterpolator()).start()
            }
            UiMotionTheme.CLASSIC_PRECISION -> {
                displayView.alpha = 0.78f
                displayView.animate().alpha(1f).setDuration(110L).start()
            }
        }
    }

    fun animateDisplayClear(activity: MainActivity, displayView: TextView?, exprView: TextView?) {
        if (!isUiAnimationsEnabled) return
        val d = activity.resources.displayMetrics.density
        displayView?.apply {
            animate().cancel()
            alpha = 0.25f
            translationX = -14f * d
            animate().alpha(1f).translationX(0f).setDuration(220L).setInterpolator(DecelerateInterpolator()).start()
        }
        exprView?.apply {
            animate().cancel()
            alpha = 0.25f
            animate().alpha(1f).setDuration(200L).start()
        }
    }

    // =========================================================================
    // 4. תפריט צד: אפקט עומק-פרלקסה למסך הראשי + כוריאוגרפיית פתיחה וסגירה
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
            mainContent?.apply { scaleX = 1f; scaleY = 1f; translationX = 0f }
            return
        }

        val theme = resolveTheme(activity)
        val openDur = when (theme) {
            UiMotionTheme.MINIMALIST_ZEN -> 310L
            UiMotionTheme.LIQUID_GLASS_SPRING -> 360L
            UiMotionTheme.AURORA_COSMIC -> 350L
            UiMotionTheme.CLASSIC_PRECISION -> 230L
        }
        val closeDur = when (theme) {
            UiMotionTheme.CLASSIC_PRECISION -> 190L
            else -> 250L
        }

        if (isOpening) {
            dimView.visibility = View.VISIBLE
            dimView.animate().cancel()
            dimView.animate().alpha(1f).setDuration(openDur).start()

            drawerScroll.animate().cancel()
            val interp = when (theme) {
                UiMotionTheme.LIQUID_GLASS_SPRING -> OvershootInterpolator(0.55f)
                UiMotionTheme.AURORA_COSMIC -> OvershootInterpolator(0.45f)
                else -> DecelerateInterpolator(1.8f)
            }
            drawerScroll.animate().translationX(0f).setDuration(openDur).setInterpolator(interp).start()

            // אפקט עומק-פרלקסה עדין למסך הראשי בזכוכית ובזוהר הצפוני
            if (mainContent != null) {
                mainContent.animate().cancel()
                when (theme) {
                    UiMotionTheme.LIQUID_GLASS_SPRING, UiMotionTheme.AURORA_COSMIC -> {
                        mainContent.animate()
                            .scaleX(0.955f).scaleY(0.955f)
                            .translationX(-activity.dp(18f) * dirSign)
                            .setDuration(openDur)
                            .setInterpolator(DecelerateInterpolator(1.6f))
                            .start()
                    }
                    UiMotionTheme.MINIMALIST_ZEN -> {
                        mainContent.animate()
                            .translationX(-activity.dp(10f) * dirSign)
                            .setDuration(openDur)
                            .setInterpolator(DecelerateInterpolator(1.5f))
                            .start()
                    }
                    UiMotionTheme.CLASSIC_PRECISION -> {
                        mainContent.scaleX = 1f; mainContent.scaleY = 1f; mainContent.translationX = 0f
                    }
                }
            }
        } else {
            dimView.animate().cancel()
            dimView.animate().alpha(0f).setDuration(closeDur)
                .withEndAction { dimView.visibility = View.GONE }.start()

            drawerScroll.animate().cancel()
            drawerScroll.animate().translationX(closedX).setDuration(closeDur)
                .setInterpolator(AccelerateInterpolator(1.4f)).start()

            mainContent?.animate()?.cancel()
            mainContent?.animate()
                ?.scaleX(1f)?.scaleY(1f)?.translationX(0f)
                ?.setDuration(closeDur)
                ?.setInterpolator(DecelerateInterpolator(1.5f))
                ?.start()
        }
    }

    fun animateSectionAccordionExpand(activity: MainActivity, container: View) {
        if (!isUiAnimationsEnabled) return
        val theme = resolveTheme(activity)
        val d = activity.resources.displayMetrics.density
        container.animate().cancel()
        container.alpha = 0f
        when (theme) {
            UiMotionTheme.MINIMALIST_ZEN -> {
                container.translationY = -6f * d
                container.animate().alpha(1f).translationY(0f).setDuration(220L)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            UiMotionTheme.LIQUID_GLASS_SPRING -> {
                container.scaleY = 0.90f; container.pivotY = 0f
                container.animate().alpha(1f).scaleY(1f).setDuration(260L)
                    .setInterpolator(OvershootInterpolator(1.1f)).start()
            }
            UiMotionTheme.AURORA_COSMIC -> {
                container.translationY = -10f * d
                container.scaleX = 0.96f
                container.animate().alpha(1f).translationY(0f).scaleX(1f).setDuration(260L)
                    .setInterpolator(DecelerateInterpolator(1.5f)).start()
            }
            UiMotionTheme.CLASSIC_PRECISION -> {
                container.translationY = -8f * d
                container.animate().alpha(1f).translationY(0f).setDuration(160L)
                    .setInterpolator(DecelerateInterpolator(1.8f)).start()
            }
        }
    }

    // =========================================================================
    // 5. מיקרו-אנימציות לסמלי הכותרת העליונה, דיאלוגים, כפתורים ושדות קלט
    // =========================================================================
    fun animateHeaderIconTap(activity: MainActivity, view: View, kind: String) {
        if (!isUiAnimationsEnabled) return
        val theme = resolveTheme(activity)
        view.animate().cancel()
        when (kind) {
            "fav" -> {
                // סיבוב ושמחת קפיץ עדינה לכוכב המועדפים
                val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
                val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.32f, 1f)
                val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.32f, 1f)
                AnimatorSet().apply {
                    playTogether(rot, sx, sy)
                    duration = if (theme == UiMotionTheme.CLASSIC_PRECISION) 280L else 420L
                    interpolator = OvershootInterpolator(1.4f)
                    start()
                }
            }
            "history" -> {
                val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, -35f, 0f)
                rot.duration = 280L
                rot.interpolator = AccelerateDecelerateInterpolator()
                rot.start()
            }
            "menu" -> {
                val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.82f, 1.08f, 1f)
                val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.82f, 1.08f, 1f)
                AnimatorSet().apply { playTogether(sx, sy); duration = 260L; start() }
            }
            else -> {
                val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.85f, 1f)
                val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.85f, 1f)
                AnimatorSet().apply { playTogether(sx, sy); duration = 220L; start() }
            }
        }
    }

    fun animateDialogEntrance(activity: MainActivity, dialogRoot: View) {
        if (!isUiAnimationsEnabled) return
        val theme = resolveTheme(activity)
        val d = activity.resources.displayMetrics.density
        dialogRoot.animate().cancel()
        dialogRoot.alpha = 0f
        when (theme) {
            UiMotionTheme.MINIMALIST_ZEN -> {
                dialogRoot.translationY = 18f * d
                dialogRoot.scaleX = 0.97f; dialogRoot.scaleY = 0.97f
                dialogRoot.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                    .setDuration(260L).setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            UiMotionTheme.LIQUID_GLASS_SPRING -> {
                dialogRoot.scaleX = 0.84f; dialogRoot.scaleY = 0.84f
                dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(320L).setInterpolator(OvershootInterpolator(1.35f)).start()
            }
            UiMotionTheme.AURORA_COSMIC -> {
                dialogRoot.scaleX = 0.86f; dialogRoot.scaleY = 0.86f
                dialogRoot.translationY = 24f * d
                dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(340L).setInterpolator(OvershootInterpolator(1.25f)).start()
            }
            UiMotionTheme.CLASSIC_PRECISION -> {
                dialogRoot.scaleX = 0.92f; dialogRoot.scaleY = 0.92f
                dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(180L).setInterpolator(DecelerateInterpolator(2.0f)).start()
            }
        }
    }

    fun attachTactilePress(activity: MainActivity, view: View) {
        view.setOnTouchListener { v, e ->
            if (!isUiAnimationsEnabled) return@setOnTouchListener false
            val theme = resolveTheme(activity)
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().cancel()
                    val downScale = when (theme) {
                        UiMotionTheme.MINIMALIST_ZEN -> 0.975f
                        UiMotionTheme.LIQUID_GLASS_SPRING -> 0.955f
                        UiMotionTheme.AURORA_COSMIC -> 0.95f
                        UiMotionTheme.CLASSIC_PRECISION -> 0.965f
                    }
                    v.animate().scaleX(downScale).scaleY(downScale).setDuration(70L).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().cancel()
                    val interp = if (theme == UiMotionTheme.LIQUID_GLASS_SPRING || theme == UiMotionTheme.AURORA_COSMIC) {
                        OvershootInterpolator(1.5f)
                    } else {
                        DecelerateInterpolator()
                    }
                    v.animate().scaleX(1f).scaleY(1f)
                        .setDuration(if (theme == UiMotionTheme.CLASSIC_PRECISION) 140L else 230L)
                        .setInterpolator(interp).start()
                }
            }
            false
        }
    }

    fun attachInteractiveMotionToHierarchy(activity: MainActivity, root: View) {
        val d = activity.resources.displayMetrics.density
        fun walk(v: View) {
            if (v is EditText) {
                val existing = v.onFocusChangeListener
                v.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                    existing?.onFocusChange(view, hasFocus)
                    if (!isUiAnimationsEnabled) return@OnFocusChangeListener
                    val theme = resolveTheme(activity)
                    view.animate().cancel()
                    if (hasFocus) {
                        val liftScale = when (theme) {
                            UiMotionTheme.MINIMALIST_ZEN -> 1.012f
                            UiMotionTheme.LIQUID_GLASS_SPRING -> 1.022f
                            UiMotionTheme.AURORA_COSMIC -> 1.024f
                            UiMotionTheme.CLASSIC_PRECISION -> 1.010f
                        }
                        val liftY = if (theme == UiMotionTheme.AURORA_COSMIC) -2f * d else 0f
                        view.animate().scaleX(liftScale).scaleY(liftScale).translationY(liftY)
                            .setDuration(180L).setInterpolator(DecelerateInterpolator()).start()
                    } else {
                        view.animate().scaleX(1f).scaleY(1f).translationY(0f)
                            .setDuration(160L).setInterpolator(DecelerateInterpolator()).start()
                    }
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

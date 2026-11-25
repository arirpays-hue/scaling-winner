package com.example.calculator

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

// =========================================================================
// מאגר 25 חבילות אנימציות ממשק (20 עשירות + 5 חסכוניות למכשירים חלשים)
// נפתח בלחיצה ארוכה על כפתור "אנימציות ממשק" בהגדרות
// =========================================================================
object ProUiMotionPacks {

    private const val PREF_KEY_PACK_ID = "pro_ui_motion_pack_id_v1"

    enum class MotionPack(
        val id: Int,
        val displayName: String,
        val shortName: String,
        val description: String,
        val accentHex: String,
        val isLowPower: Boolean = false
    ) {
        // --- 20 חבילות עשירות ---
        ZEN_SILK(0, "משי זן", "משי זן", "ציפה איטית ומרגיעה, האטה רכה ומעברים שקטים ללא קפיצות", "#66BB6A"),
        LIQUID_SPRING(1, "קפיץ זכוכית", "קפיץ", "פיזיקת קפיץ אלסטית חזקה, הטיית 3D במקשים ופרלקסה", "#38BDF8"),
        AURORA_COSMIC(2, "זוהר קוסמי", "זוהר", "היפוך 3D קוסמי, זום חללי בתוצאות וכניסה אלכסונית", "#00F5D4"),
        CYBER_GLITCH(3, "סייבר גליץ׳", "גליץ׳", "רטט חשמלי רב-שלבי והבהוב סייבר חד בכל הקשה", "#F43F5E"),
        ORIGAMI_3D(4, "קיפול אוריגמי", "אוריגמי", "פתיחה בציר 90° כמו דף נייר ודלת תלת-ממדית לתפריט", "#A78BFA"),
        JELLY_BOUNCE(5, "ג׳לי אלסטי", "ג׳לי", "מעיכה ומתיחה קיצונית (Squash & Stretch) בכל פעולה", "#FB923C"),
        MAGNETIC_FLOAT(6, "ריחוף מגנטי", "מגנטי", "מקשים מתרוממים למעלה בלחיצה ונמשכים מגנטית", "#60A5FA"),
        MECHANICAL_CLICK(7, "מכני טקטי", "מכני", "שקיעה מהירה למטה וחזרה מיידית כמו מקלדת מכנית", "#94A3B8"),
        WAVE_CASCADE(8, "גל אדוות", "גל אדוות", "גל אלכסוני במקלדת + הרעדת מקשים שכנים בכל הקשה", "#2DD4BF"),
        CINEMA_ZOOM(9, "קולנוע רחב", "קולנוע", "זום מצלמה תיאטרלי רחב ופתיחת מסך דרמטית", "#FBBF24"),
        GRAVITY_DROP(10, "כבידת קפיץ", "כבידה", "צניחה מלמעלה עם הקפצה פיזיקלית על הרצפה (Bounce)", "#F59E0B"),
        TORNADO_SPIN(11, "סחרחורת טורנדו", "טורנדו", "כניסת מערבולת מסתובבת, טוויסט במקשים ו-360° ב-=", "#EC4899"),
        PENDULUM_SWING(12, "מטוטלת שעון", "מטוטלת", "התנדנדות הלוך-ושוב מציר עליון כמו שלט תלוי", "#EAB308"),
        COIN_FLIP_3D(13, "מטבע מתהפך", "מטבע 3D", "היפוך תלת-ממדי מלא סביב ציר ה-Y בכל הקשה ותוצאה", "#F97316"),
        RADIAL_BURST(14, "פיצוץ רדיאלי", "רדיאלי", "המקשים נורים החוצה ממרכז המקלדת כמו גל הדף", "#EF4444"),
        CARD_SHUFFLE(15, "חלוקת קלפים", "קלפים", "כרטיסים ומקשים מחולקים בזווית חפיסה משמאל ומימין", "#8B5CF6"),
        DOMINO_FALL(16, "דומינו נופל", "דומינו", "אבני דומינו שנעמדות זו אחר זו מציר תחתון", "#10B981"),
        SLINGSHOT_PULL(17, "רוגטקה אלסטית", "רוגטקה", "משיכה מוחשית אחורה לפני שיגור אלסטי קדימה", "#06B6D4"),
        HEARTBEAT_PULSE(18, "דופק לב", "דופק", "פעימה כפולה קצבית (בום-בום) בכל תגובה בממשק", "#E11D48"),
        PORTAL_WARP(19, "שער פורטל", "פורטל", "מתיחה בין-ממדית (Warp) בכניסה ובהופעת תוצאות", "#6366F1"),

        // --- 5 חבילות חסכוניות למכשירים חלשים (0% תלת-ממד, אפס עומס על GPU) ---
        LITE_FADE(20, "דהייה מהירה", "דהייה קלה", "מעבר שקיפות אחיד על המסך כולו — צריכת משאבים מינימלית", "#22C55E", true),
        LITE_SLIDE(21, "החלקה קלה", "החלקה קלה", "החלקה קצרה של המסך כיחידה אחת ללא סטאגר וללא 3D", "#14B8A6", true),
        LITE_TAP(22, "נגיעה נקייה", "נגיעה קלה", "תגובת כיווץ עדינה בלחיצות בלבד — מעברי מסך מיידיים", "#38BDF8", true),
        LITE_SNAP(23, "קליק סוללה", "קליק סוללה", "פידבק שקיפות מהיר במיוחד לחיסכון מקסימלי בסוללה", "#84CC16", true),
        LITE_SOFT_LIFT(24, "עלייה רכה", "עלייה קלה", "עלייה אנכית קצרה ורכה כיחידה אחת — חלקה בכל מכשיר", "#4ADE80", true);

        companion object {
            fun fromId(id: Int): MotionPack = values().firstOrNull { it.id == id } ?: LIQUID_SPRING
        }
    }

    var currentPack: MotionPack = MotionPack.LIQUID_SPRING
        private set

    private val runningAnimators = WeakHashMap<View, Animator>()

    fun loadPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        currentPack = MotionPack.fromId(prefs.getInt(PREF_KEY_PACK_ID, MotionPack.LIQUID_SPRING.id))
    }

    fun selectPack(activity: MainActivity, pack: MotionPack, autoEnableIfOff: Boolean = true) {
        currentPack = pack
        activity.prefs?.edit()?.putInt(PREF_KEY_PACK_ID, pack.id)?.apply()
        if (autoEnableIfOff && !ProUiMotionEngine.isUiAnimationsEnabled) {
            ProUiMotionEngine.setEnabled(activity, true)
        }
    }

    private fun cancelRunning(v: View) {
        v.animate().cancel()
        runningAnimators.remove(v)?.cancel()
    }

    private fun startTracked(v: View, anim: Animator) {
        cancelRunning(v)
        runningAnimators[v] = anim
        anim.start()
    }

    private fun resetViewTransforms(v: View, resetPivot: Boolean = true) {
        cancelRunning(v)
        v.alpha = 1f
        v.scaleX = 1f
        v.scaleY = 1f
        v.translationX = 0f
        v.translationY = 0f
        v.rotation = 0f
        v.rotationX = 0f
        v.rotationY = 0f
        if (resetPivot && v.width > 0 && v.height > 0) {
            v.pivotX = v.width * 0.5f
            v.pivotY = v.height * 0.5f
        }
    }

    private fun runWhenLaidOut(v: View, preHide: Boolean = false, block: () -> Unit) {
        if (v.width > 0 && v.height > 0) {
            block()
        } else {
            if (preHide) v.alpha = 0f
            v.post {
                if (v.isAttachedToWindow) block()
            }
        }
    }

    // =========================================================================
    // 1. אנימציית כותרת עליונה בעת מעבר לשונית
    // =========================================================================
    fun animateHeaderTitle(activity: MainActivity, titleView: TextView?) {
        if (titleView == null) return
        val d = activity.resources.displayMetrics.density
        val dir = if (activity.isRightHanded) 1f else -1f

        // נתיב חסכוני מהיר ללא השהיית Layout
        if (currentPack.isLowPower) {
            resetViewTransforms(titleView, resetPivot = false)
            when (currentPack) {
                MotionPack.LITE_FADE -> {
                    titleView.alpha = 0.25f
                    titleView.animate().alpha(1f).setDuration(140L).start()
                }
                MotionPack.LITE_SLIDE -> {
                    titleView.alpha = 0.35f; titleView.translationX = 12f * d * dir
                    titleView.animate().alpha(1f).translationX(0f).setDuration(150L)
                        .setInterpolator(DecelerateInterpolator()).start()
                }
                MotionPack.LITE_TAP -> {
                    titleView.alpha = 1f
                }
                MotionPack.LITE_SNAP -> {
                    titleView.alpha = 0.65f
                    titleView.animate().alpha(1f).setDuration(75L).start()
                }
                MotionPack.LITE_SOFT_LIFT -> {
                    titleView.alpha = 0.3f; titleView.translationY = 6f * d
                    titleView.animate().alpha(1f).translationY(0f).setDuration(160L)
                        .setInterpolator(DecelerateInterpolator()).start()
                }
                else -> {}
            }
            return
        }

        runWhenLaidOut(titleView, preHide = true) {
            resetViewTransforms(titleView)
            when (currentPack) {
                MotionPack.ZEN_SILK -> {
                    titleView.alpha = 0f; titleView.translationY = 10f * d
                    titleView.animate().alpha(1f).translationY(0f).setDuration(520L)
                        .setInterpolator(DecelerateInterpolator(2.2f)).start()
                }
                MotionPack.LIQUID_SPRING -> {
                    titleView.alpha = 0f; titleView.scaleX = 0.68f; titleView.scaleY = 0.68f
                    titleView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(380L)
                        .setInterpolator(OvershootInterpolator(2.4f)).start()
                }
                MotionPack.AURORA_COSMIC -> {
                    titleView.cameraDistance = 6000f * d
                    titleView.alpha = 0f; titleView.rotationX = -68f; titleView.translationX = 24f * d * dir
                    titleView.animate().alpha(1f).rotationX(0f).translationX(0f).setDuration(420L)
                        .setInterpolator(OvershootInterpolator(1.3f)).start()
                }
                MotionPack.CYBER_GLITCH -> {
                    val tx = ObjectAnimator.ofFloat(titleView, "translationX", 22f * d * dir, -14f * d * dir, 9f * d * dir, -5f * d * dir, 0f)
                    val al = ObjectAnimator.ofFloat(titleView, "alpha", 0.1f, 1f, 0.2f, 1f, 0.5f, 1f)
                    startTracked(titleView, AnimatorSet().apply { playTogether(tx, al); duration = 280L })
                }
                MotionPack.ORIGAMI_3D -> {
                    titleView.cameraDistance = 5500f * d
                    titleView.pivotY = 0f; titleView.alpha = 0f; titleView.rotationX = -90f
                    titleView.animate().alpha(1f).rotationX(0f).setDuration(400L)
                        .setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                MotionPack.JELLY_BOUNCE -> {
                    val sx = ObjectAnimator.ofFloat(titleView, "scaleX", 1.38f, 0.78f, 1.12f, 0.96f, 1f)
                    val sy = ObjectAnimator.ofFloat(titleView, "scaleY", 0.55f, 1.26f, 0.88f, 1.05f, 1f)
                    val al = ObjectAnimator.ofFloat(titleView, "alpha", 0f, 1f)
                    startTracked(titleView, AnimatorSet().apply { playTogether(sx, sy, al); duration = 450L })
                }
                MotionPack.MAGNETIC_FLOAT -> {
                    titleView.alpha = 0f; titleView.translationY = 26f * d
                    titleView.animate().alpha(1f).translationY(0f).setDuration(380L)
                        .setInterpolator(AnticipateOvershootInterpolator(1.4f)).start()
                }
                MotionPack.MECHANICAL_CLICK -> {
                    titleView.translationY = -10f * d
                    titleView.animate().translationY(0f).setDuration(100L)
                        .setInterpolator(DecelerateInterpolator(2.5f)).start()
                }
                MotionPack.WAVE_CASCADE -> {
                    titleView.alpha = 0f; titleView.translationX = 44f * d * dir; titleView.rotation = -8f * dir
                    titleView.animate().alpha(1f).translationX(0f).rotation(0f).setDuration(380L)
                        .setInterpolator(OvershootInterpolator(1.2f)).start()
                }
                MotionPack.CINEMA_ZOOM -> {
                    titleView.alpha = 0f; titleView.scaleX = 1.45f; titleView.scaleY = 1.45f
                    titleView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(520L)
                        .setInterpolator(DecelerateInterpolator(2.0f)).start()
                }
                MotionPack.GRAVITY_DROP -> {
                    titleView.translationY = -42f * d; titleView.alpha = 0f
                    titleView.animate().translationY(0f).alpha(1f).setDuration(520L)
                        .setInterpolator(BounceInterpolator()).start()
                }
                MotionPack.TORNADO_SPIN -> {
                    titleView.rotation = -180f; titleView.scaleX = 0.25f; titleView.scaleY = 0.25f; titleView.alpha = 0f
                    titleView.animate().rotation(0f).scaleX(1f).scaleY(1f).alpha(1f).setDuration(440L)
                        .setInterpolator(OvershootInterpolator(1.1f)).start()
                }
                MotionPack.PENDULUM_SWING -> {
                    titleView.pivotY = 0f
                    val rot = ObjectAnimator.ofFloat(titleView, "rotation", 26f, -15f, 9f, -4f, 0f)
                    val al = ObjectAnimator.ofFloat(titleView, "alpha", 0f, 1f)
                    startTracked(titleView, AnimatorSet().apply { playTogether(rot, al); duration = 560L })
                }
                MotionPack.COIN_FLIP_3D -> {
                    titleView.cameraDistance = 6000f * d
                    titleView.rotationY = -180f; titleView.alpha = 0f
                    titleView.animate().rotationY(0f).alpha(1f).setDuration(460L)
                        .setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                MotionPack.RADIAL_BURST -> {
                    titleView.scaleX = 0.15f; titleView.scaleY = 0.15f; titleView.alpha = 0f
                    titleView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(350L)
                        .setInterpolator(OvershootInterpolator(2.0f)).start()
                }
                MotionPack.CARD_SHUFFLE -> {
                    titleView.translationX = 85f * d * dir; titleView.rotation = 12f * dir; titleView.alpha = 0f
                    titleView.animate().translationX(0f).rotation(0f).alpha(1f).setDuration(380L)
                        .setInterpolator(DecelerateInterpolator(1.8f)).start()
                }
                MotionPack.DOMINO_FALL -> {
                    titleView.cameraDistance = 5500f * d
                    titleView.pivotY = titleView.height.toFloat(); titleView.rotationX = 82f; titleView.alpha = 0f
                    titleView.animate().rotationX(0f).alpha(1f).setDuration(400L)
                        .setInterpolator(OvershootInterpolator(1.3f)).start()
                }
                MotionPack.SLINGSHOT_PULL -> {
                    titleView.translationX = 56f * d * dir; titleView.alpha = 0f
                    titleView.animate().translationX(0f).alpha(1f).setDuration(460L)
                        .setInterpolator(AnticipateOvershootInterpolator(2.2f)).start()
                }
                MotionPack.HEARTBEAT_PULSE -> {
                    val sx = ObjectAnimator.ofFloat(titleView, "scaleX", 0.7f, 1.22f, 0.94f, 1.14f, 1f)
                    val sy = ObjectAnimator.ofFloat(titleView, "scaleY", 0.7f, 1.22f, 0.94f, 1.14f, 1f)
                    val al = ObjectAnimator.ofFloat(titleView, "alpha", 0f, 1f)
                    startTracked(titleView, AnimatorSet().apply { playTogether(sx, sy, al); duration = 500L })
                }
                MotionPack.PORTAL_WARP -> {
                    titleView.scaleX = 0.10f; titleView.scaleY = 1.95f; titleView.alpha = 0f
                    titleView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(400L)
                        .setInterpolator(OvershootInterpolator(1.3f)).start()
                }
                else -> {}
            }
        }
    }

    // מעבר אחיד וחסכוני במיוחד למסך שלם (מקש/כלי) בחבילות Low-Power
    private fun animateLowPowerContainerEntrance(activity: MainActivity, root: View) {
        val d = activity.resources.displayMetrics.density
        val dir = if (activity.isRightHanded) 1f else -1f
        resetViewTransforms(root, resetPivot = false)

        when (currentPack) {
            MotionPack.LITE_FADE -> {
                root.alpha = 0f
                root.animate().alpha(1f).setDuration(155L).setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.LITE_SLIDE -> {
                root.alpha = 0.25f; root.translationX = 20f * d * dir
                root.animate().alpha(1f).translationX(0f).setDuration(170L)
                    .setInterpolator(DecelerateInterpolator(1.5f)).start()
            }
            MotionPack.LITE_TAP -> {
                // מיידי לחלוטין — ללא שום עומס במעבר לשוניות
                root.alpha = 1f
            }
            MotionPack.LITE_SNAP -> {
                root.alpha = 0.55f
                root.animate().alpha(1f).setDuration(85L).start()
            }
            MotionPack.LITE_SOFT_LIFT -> {
                root.alpha = 0.20f; root.translationY = 12f * d
                root.animate().alpha(1f).translationY(0f).setDuration(180L)
                    .setInterpolator(DecelerateInterpolator(1.5f)).start()
            }
            else -> {}
        }
    }

    // =========================================================================
    // 2. כניסת מסך המחשבון (צג + מקלדת)
    // =========================================================================
    fun animateCalculatorEntrance(activity: MainActivity, root: View) {
        if (currentPack.isLowPower) {
            animateLowPowerContainerEntrance(activity, root)
            return
        }

        val vg = root as? ViewGroup ?: return
        val displayCard = vg.getChildAt(0)
        val buttons = activity.keypadButtons.values.toList()
        val d = activity.resources.displayMetrics.density
        val cols = if (buttons.size > 26) 5 else 4
        val rows = ((buttons.size + cols - 1) / cols).coerceAtLeast(1)

        displayCard?.alpha = 0f
        buttons.forEach { it.alpha = 0f }

        runWhenLaidOut(root, preHide = false) {
            if (displayCard != null) {
                animateSingleCardEntrance(activity, displayCard, 0, d, isDisplayCard = true)
            }

            val centerRow = (rows - 1) * 0.5f
            val centerCol = (cols - 1) * 0.5f

            for ((idx, btn) in buttons.withIndex()) {
                resetViewTransforms(btn)
                btn.alpha = 0f
                val row = idx / cols
                val col = idx % cols

                when (currentPack) {
                    MotionPack.ZEN_SILK -> {
                        val delay = (row * 45L).coerceAtMost(280L)
                        btn.translationY = 14f * d
                        btn.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(480L)
                            .setInterpolator(DecelerateInterpolator(2.2f)).start()
                    }
                    MotionPack.LIQUID_SPRING -> {
                        val delay = (idx * 14L).coerceAtMost(280L)
                        btn.scaleX = 0.45f; btn.scaleY = 0.45f; btn.translationY = 18f * d
                        btn.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                            .setStartDelay(delay).setDuration(380L).setInterpolator(OvershootInterpolator(2.4f)).start()
                    }
                    MotionPack.AURORA_COSMIC -> {
                        val delay = ((row + col) * 28L).coerceAtMost(280L)
                        btn.cameraDistance = 5200f * d
                        btn.rotationX = -72f; btn.scaleX = 0.72f; btn.scaleY = 0.72f; btn.translationY = 22f * d
                        btn.animate().alpha(1f).rotationX(0f).scaleX(1f).scaleY(1f).translationY(0f)
                            .setStartDelay(delay).setDuration(420L).setInterpolator(OvershootInterpolator(1.45f)).start()
                    }
                    MotionPack.CYBER_GLITCH -> {
                        val delay = ((idx * 11L) % 160L)
                        val side = if (idx % 2 == 0) 26f else -26f
                        val tx = ObjectAnimator.ofFloat(btn, "translationX", side * d, -side * 0.35f * d, side * 0.15f * d, 0f)
                        val al = ObjectAnimator.ofFloat(btn, "alpha", 0f, 1f, 0.25f, 1f)
                        startTracked(btn, AnimatorSet().apply { playTogether(tx, al); startDelay = delay; duration = 220L })
                    }
                    MotionPack.ORIGAMI_3D -> {
                        val delay = (row * 48L + col * 24L).coerceAtMost(320L)
                        btn.cameraDistance = 5000f * d
                        btn.pivotX = 0f; btn.rotationY = -88f
                        btn.animate().alpha(1f).rotationY(0f)
                            .setStartDelay(delay).setDuration(380L).setInterpolator(DecelerateInterpolator(1.6f)).start()
                    }
                    MotionPack.JELLY_BOUNCE -> {
                        val delay = (idx * 12L).coerceAtMost(240L)
                        val sx = ObjectAnimator.ofFloat(btn, "scaleX", 1.45f, 0.74f, 1.12f, 0.96f, 1f)
                        val sy = ObjectAnimator.ofFloat(btn, "scaleY", 0.42f, 1.28f, 0.88f, 1.05f, 1f)
                        val al = ObjectAnimator.ofFloat(btn, "alpha", 0f, 1f)
                        startTracked(btn, AnimatorSet().apply { playTogether(sx, sy, al); startDelay = delay; duration = 430L })
                    }
                    MotionPack.MAGNETIC_FLOAT -> {
                        val delay = ((rows - 1 - row) * 38L + col * 14L).coerceAtMost(280L)
                        btn.translationY = 48f * d; btn.scaleX = 0.80f; btn.scaleY = 0.80f
                        btn.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                            .setStartDelay(delay).setDuration(380L).setInterpolator(AnticipateOvershootInterpolator(1.3f)).start()
                    }
                    MotionPack.MECHANICAL_CLICK -> {
                        val delay = (row * 18L).coerceAtMost(110L)
                        btn.translationY = -8f * d; btn.alpha = 1f
                        btn.animate().translationY(0f).setStartDelay(delay).setDuration(90L)
                            .setInterpolator(DecelerateInterpolator(2.5f)).start()
                    }
                    MotionPack.WAVE_CASCADE -> {
                        val delay = ((row + col) * 42L).coerceAtMost(340L)
                        val ty = ObjectAnimator.ofFloat(btn, "translationY", 30f * d, -12f * d, 0f)
                        val rot = ObjectAnimator.ofFloat(btn, "rotation", -14f, 7f, 0f)
                        val al = ObjectAnimator.ofFloat(btn, "alpha", 0f, 1f)
                        startTracked(btn, AnimatorSet().apply { playTogether(ty, rot, al); startDelay = delay; duration = 380L })
                    }
                    MotionPack.CINEMA_ZOOM -> {
                        val delay = (row * 42L).coerceAtMost(240L)
                        btn.scaleX = 0.50f; btn.scaleY = 0.50f
                        btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                            .setStartDelay(delay).setDuration(480L).setInterpolator(DecelerateInterpolator(2.0f)).start()
                    }
                    MotionPack.GRAVITY_DROP -> {
                        val delay = ((rows - 1 - row) * 46L + col * 18L).coerceAtMost(300L)
                        btn.translationY = -85f * d
                        btn.animate().alpha(1f).translationY(0f)
                            .setStartDelay(delay).setDuration(560L).setInterpolator(BounceInterpolator()).start()
                    }
                    MotionPack.TORNADO_SPIN -> {
                        val delay = (idx * 14L).coerceAtMost(280L)
                        btn.rotation = -180f; btn.scaleX = 0.20f; btn.scaleY = 0.20f
                        btn.animate().alpha(1f).rotation(0f).scaleX(1f).scaleY(1f)
                            .setStartDelay(delay).setDuration(440L).setInterpolator(OvershootInterpolator(1.2f)).start()
                    }
                    MotionPack.PENDULUM_SWING -> {
                        val delay = (row * 48L + col * 16L).coerceAtMost(300L)
                        btn.pivotY = 0f
                        val rot = ObjectAnimator.ofFloat(btn, "rotation", 30f, -18f, 10f, -5f, 0f)
                        val al = ObjectAnimator.ofFloat(btn, "alpha", 0f, 1f)
                        startTracked(btn, AnimatorSet().apply { playTogether(rot, al); startDelay = delay; duration = 560L })
                    }
                    MotionPack.COIN_FLIP_3D -> {
                        val delay = (idx * 14L).coerceAtMost(280L)
                        btn.cameraDistance = 5200f * d
                        btn.rotationY = -180f
                        btn.animate().alpha(1f).rotationY(0f)
                            .setStartDelay(delay).setDuration(420L).setInterpolator(DecelerateInterpolator(1.5f)).start()
                    }
                    MotionPack.RADIAL_BURST -> {
                        val dx = (centerCol - col) * 44f * d
                        val dy = (centerRow - row) * 44f * d
                        val dist = hypot(dx.toDouble(), dy.toDouble()).toLong()
                        val delay = (dist / 3L).coerceAtMost(160L)
                        btn.translationX = dx; btn.translationY = dy; btn.scaleX = 0.25f; btn.scaleY = 0.25f
                        btn.animate().alpha(1f).translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                            .setStartDelay(delay).setDuration(400L).setInterpolator(OvershootInterpolator(1.6f)).start()
                    }
                    MotionPack.CARD_SHUFFLE -> {
                        val delay = (idx * 15L).coerceAtMost(280L)
                        val side = if (row % 2 == 0) 1f else -1f
                        btn.translationX = 110f * d * side; btn.rotation = 18f * side
                        btn.animate().alpha(1f).translationX(0f).rotation(0f)
                            .setStartDelay(delay).setDuration(380L).setInterpolator(DecelerateInterpolator(1.8f)).start()
                    }
                    MotionPack.DOMINO_FALL -> {
                        val delay = (idx * 18L).coerceAtMost(340L)
                        btn.cameraDistance = 5000f * d
                        btn.pivotY = btn.height.toFloat()
                        btn.rotationX = 86f
                        btn.animate().alpha(1f).rotationX(0f)
                            .setStartDelay(delay).setDuration(380L).setInterpolator(OvershootInterpolator(1.5f)).start()
                    }
                    MotionPack.SLINGSHOT_PULL -> {
                        val delay = (row * 44L + col * 14L).coerceAtMost(280L)
                        btn.translationY = 46f * d; btn.scaleX = 0.82f; btn.scaleY = 0.82f
                        btn.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                            .setStartDelay(delay).setDuration(460L).setInterpolator(AnticipateOvershootInterpolator(2.2f)).start()
                    }
                    MotionPack.HEARTBEAT_PULSE -> {
                        val delay = (row * 46L).coerceAtMost(240L)
                        val sx = ObjectAnimator.ofFloat(btn, "scaleX", 0.55f, 1.18f, 0.92f, 1.10f, 1f)
                        val sy = ObjectAnimator.ofFloat(btn, "scaleY", 0.55f, 1.18f, 0.92f, 1.10f, 1f)
                        val al = ObjectAnimator.ofFloat(btn, "alpha", 0f, 1f)
                        startTracked(btn, AnimatorSet().apply { playTogether(sx, sy, al); startDelay = delay; duration = 480L })
                    }
                    MotionPack.PORTAL_WARP -> {
                        val delay = (idx * 12L).coerceAtMost(240L)
                        btn.scaleX = 0.10f; btn.scaleY = 2.0f
                        btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                            .setStartDelay(delay).setDuration(380L).setInterpolator(OvershootInterpolator(1.3f)).start()
                    }
                    else -> {
                        btn.alpha = 1f
                    }
                }
            }
        }
    }

    // =========================================================================
    // 3. כניסת כרטיסים במסכי כלים והגדרות
    // =========================================================================
    fun animateToolCardsCascade(activity: MainActivity, root: View) {
        if (currentPack.isLowPower) {
            animateLowPowerContainerEntrance(activity, root)
            return
        }

        val container: ViewGroup? = when (root) {
            is ScrollView -> root.getChildAt(0) as? ViewGroup
            is ViewGroup -> root
            else -> null
        }
        if (container == null) {
            resetViewTransforms(root); root.alpha = 0f
            root.animate().alpha(1f).setDuration(240L).start()
            return
        }

        val d = activity.resources.displayMetrics.density
        val count = min(container.childCount, 12)
        for (i in 0 until count) {
            val ch = container.getChildAt(i)
            if (ch.visibility == View.VISIBLE) ch.alpha = 0f
        }

        runWhenLaidOut(container, preHide = false) {
            for (i in 0 until count) {
                val child = container.getChildAt(i)
                if (child.visibility != View.VISIBLE) continue
                animateSingleCardEntrance(activity, child, i, d, isDisplayCard = false)
            }
        }
    }

    private fun animateSingleCardEntrance(
        activity: MainActivity,
        v: View,
        index: Int,
        d: Float,
        isDisplayCard: Boolean
    ) {
        resetViewTransforms(v)
        v.alpha = 0f
        val delay = (index * 36L).coerceAtMost(320L)
        val dir = if (activity.isRightHanded) 1f else -1f

        when (currentPack) {
            MotionPack.ZEN_SILK -> {
                v.translationY = (if (isDisplayCard) -14f else 18f) * d
                v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(520L)
                    .setInterpolator(DecelerateInterpolator(2.2f)).start()
            }
            MotionPack.LIQUID_SPRING -> {
                v.scaleX = 0.78f; v.scaleY = 0.78f; v.translationY = 24f * d
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setStartDelay(delay)
                    .setDuration(400L).setInterpolator(OvershootInterpolator(1.9f)).start()
            }
            MotionPack.AURORA_COSMIC -> {
                v.cameraDistance = 7000f * d
                v.rotationX = -32f; v.translationX = 28f * d * dir; v.translationY = 20f * d
                v.animate().alpha(1f).rotationX(0f).translationX(0f).translationY(0f).setStartDelay(delay)
                    .setDuration(420L).setInterpolator(OvershootInterpolator(1.2f)).start()
            }
            MotionPack.CYBER_GLITCH -> {
                val side = if (index % 2 == 0) 36f else -36f
                val tx = ObjectAnimator.ofFloat(v, "translationX", side * d, -side * 0.35f * d, side * 0.12f * d, 0f)
                val al = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f, 0.25f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(tx, al); startDelay = index * 24L; duration = 240L })
            }
            MotionPack.ORIGAMI_3D -> {
                v.cameraDistance = 6800f * d
                v.pivotY = 0f; v.rotationX = -68f
                v.animate().alpha(1f).rotationX(0f).setStartDelay(delay)
                    .setDuration(420L).setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            MotionPack.JELLY_BOUNCE -> {
                val sx = ObjectAnimator.ofFloat(v, "scaleX", 1.20f, 0.88f, 1.06f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", 0.62f, 1.18f, 0.94f, 1f)
                val al = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(sx, sy, al); startDelay = delay; duration = 450L })
            }
            MotionPack.MAGNETIC_FLOAT -> {
                v.translationY = 46f * d; v.scaleX = 0.92f; v.scaleY = 0.92f
                v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(delay)
                    .setDuration(400L).setInterpolator(AnticipateOvershootInterpolator(1.2f)).start()
            }
            MotionPack.MECHANICAL_CLICK -> {
                v.translationY = -10f * d; v.alpha = 1f
                v.animate().translationY(0f).setStartDelay(index * 16L).setDuration(110L)
                    .setInterpolator(DecelerateInterpolator(2.5f)).start()
            }
            MotionPack.WAVE_CASCADE -> {
                val ty = ObjectAnimator.ofFloat(v, "translationY", 26f * d, -10f * d, 0f)
                val rot = ObjectAnimator.ofFloat(v, "rotation", -5f * dir, 2.5f * dir, 0f)
                val al = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(ty, rot, al); startDelay = delay; duration = 400L })
            }
            MotionPack.CINEMA_ZOOM -> {
                v.scaleX = if (isDisplayCard) 1.24f else 0.72f
                v.scaleY = if (isDisplayCard) 1.24f else 0.72f
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay)
                    .setDuration(520L).setInterpolator(DecelerateInterpolator(2.0f)).start()
            }
            MotionPack.GRAVITY_DROP -> {
                v.translationY = -68f * d
                v.animate().alpha(1f).translationY(0f).setStartDelay(delay)
                    .setDuration(580L).setInterpolator(BounceInterpolator()).start()
            }
            MotionPack.TORNADO_SPIN -> {
                v.rotation = if (index % 2 == 0) -24f else 24f; v.scaleX = 0.55f; v.scaleY = 0.55f
                v.animate().alpha(1f).rotation(0f).scaleX(1f).scaleY(1f).setStartDelay(delay)
                    .setDuration(440L).setInterpolator(OvershootInterpolator(1.2f)).start()
            }
            MotionPack.PENDULUM_SWING -> {
                v.pivotY = 0f
                val rot = ObjectAnimator.ofFloat(v, "rotation", 14f, -9f, 5f, -2f, 0f)
                val al = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(rot, al); startDelay = delay; duration = 560L })
            }
            MotionPack.COIN_FLIP_3D -> {
                v.cameraDistance = 7500f * d
                v.rotationY = -95f
                v.animate().alpha(1f).rotationY(0f).setStartDelay(delay)
                    .setDuration(440L).setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            MotionPack.RADIAL_BURST -> {
                v.scaleX = 0.35f; v.scaleY = 0.35f
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(index * 22L)
                    .setDuration(380L).setInterpolator(OvershootInterpolator(1.7f)).start()
            }
            MotionPack.CARD_SHUFFLE -> {
                val side = if (index % 2 == 0) 1f else -1f
                v.translationX = 120f * d * side; v.rotation = 9f * side
                v.animate().alpha(1f).translationX(0f).rotation(0f).setStartDelay(delay)
                    .setDuration(400L).setInterpolator(DecelerateInterpolator(1.8f)).start()
            }
            MotionPack.DOMINO_FALL -> {
                v.cameraDistance = 6800f * d
                v.pivotY = v.height.toFloat().coerceAtLeast(1f)
                v.rotationX = 72f
                v.animate().alpha(1f).rotationX(0f).setStartDelay(delay)
                    .setDuration(400L).setInterpolator(OvershootInterpolator(1.3f)).start()
            }
            MotionPack.SLINGSHOT_PULL -> {
                v.translationY = 52f * d
                v.animate().alpha(1f).translationY(0f).setStartDelay(delay)
                    .setDuration(480L).setInterpolator(AnticipateOvershootInterpolator(2.0f)).start()
            }
            MotionPack.HEARTBEAT_PULSE -> {
                val sx = ObjectAnimator.ofFloat(v, "scaleX", 0.78f, 1.08f, 0.95f, 1.04f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", 0.78f, 1.08f, 0.95f, 1.04f, 1f)
                val al = ObjectAnimator.ofFloat(v, "alpha", 0f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(sx, sy, al); startDelay = delay; duration = 480L })
            }
            MotionPack.PORTAL_WARP -> {
                v.scaleX = 0.18f; v.scaleY = 1.65f
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay)
                    .setDuration(400L).setInterpolator(OvershootInterpolator(1.25f)).start()
            }
            else -> {
                v.alpha = 1f
            }
        }
    }

    // =========================================================================
    // 4. פיזיקת לחיצה ושחרור מקשים (כולל 5 פרופילי חיסכון קלים במיוחד)
    // =========================================================================
    fun animateKeypadTouchDown(activity: MainActivity, v: View, eventX: Float, eventY: Float, symbol: String) {
        val d = activity.resources.displayMetrics.density
        cancelRunning(v)
        val nx = if (v.width > 0) ((eventX / v.width) - 0.5f).coerceIn(-0.5f, 0.5f) else 0f
        val ny = if (v.height > 0) ((eventY / v.height) - 0.5f).coerceIn(-0.5f, 0.5f) else 0f

        when (currentPack) {
            MotionPack.LITE_FADE -> v.animate().alpha(0.65f).setDuration(60L).start()
            MotionPack.LITE_SLIDE -> v.animate().translationY(2.5f * d).setDuration(45L).start()
            MotionPack.LITE_TAP -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(55L).start()
            MotionPack.LITE_SNAP -> v.animate().alpha(0.76f).setDuration(35L).start()
            MotionPack.LITE_SOFT_LIFT -> v.animate().scaleX(0.96f).scaleY(0.96f).alpha(0.84f).setDuration(60L).start()

            MotionPack.ZEN_SILK -> v.animate().scaleX(0.94f).scaleY(0.94f).alpha(0.75f).setDuration(130L).setInterpolator(DecelerateInterpolator()).start()
            MotionPack.LIQUID_SPRING -> {
                v.cameraDistance = 4200f * d
                v.animate().scaleX(0.84f).scaleY(0.84f).rotationY(nx * 26f).rotationX(-ny * 26f).setDuration(65L).setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.AURORA_COSMIC -> {
                v.cameraDistance = 4200f * d
                v.animate().scaleX(0.82f).scaleY(0.82f).rotationX(-24f).rotationY(nx * 24f).setDuration(65L).start()
            }
            MotionPack.CYBER_GLITCH -> {
                val tx = ObjectAnimator.ofFloat(v, "translationX", 0f, -6f * d, 6f * d, -3f * d)
                val al = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.45f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(tx, al); duration = 90L })
            }
            MotionPack.ORIGAMI_3D -> {
                v.cameraDistance = 4000f * d; v.pivotY = 0f
                v.animate().rotationX(-38f).scaleY(0.88f).setDuration(80L).start()
            }
            MotionPack.JELLY_BOUNCE -> v.animate().scaleX(1.22f).scaleY(0.70f).translationY(3f * d).setDuration(65L).start()
            MotionPack.MAGNETIC_FLOAT -> v.animate().scaleX(1.10f).scaleY(1.10f).translationY(-7f * d).setDuration(80L).setInterpolator(DecelerateInterpolator()).start()
            MotionPack.MECHANICAL_CLICK -> v.animate().translationY(6.5f * d).scaleX(0.96f).scaleY(0.94f).setDuration(35L).start()
            MotionPack.WAVE_CASCADE -> {
                v.animate().scaleX(0.84f).scaleY(0.84f).translationY(4f * d).setDuration(65L).start()
                triggerNeighborWaveRipple(activity, v, d)
            }
            MotionPack.CINEMA_ZOOM -> v.animate().scaleX(1.12f).scaleY(1.12f).alpha(0.82f).setDuration(110L).start()
            MotionPack.GRAVITY_DROP -> v.animate().translationY(-10f * d).scaleX(0.92f).scaleY(1.06f).setDuration(75L).start()
            MotionPack.TORNADO_SPIN -> v.animate().rotation(-24f).scaleX(0.86f).scaleY(0.86f).setDuration(70L).start()
            MotionPack.PENDULUM_SWING -> { v.pivotY = 0f; v.animate().rotation(if (nx >= 0f) 18f else -18f).setDuration(75L).start() }
            MotionPack.COIN_FLIP_3D -> { v.cameraDistance = 4200f * d; v.animate().rotationY(85f).scaleX(0.92f).setDuration(85L).start() }
            MotionPack.RADIAL_BURST -> v.animate().scaleX(0.76f).scaleY(0.76f).setDuration(60L).start()
            MotionPack.CARD_SHUFFLE -> v.animate().translationX((if (nx >= 0f) 8f else -8f) * d).rotation(if (nx >= 0f) 10f else -10f).setDuration(70L).start()
            MotionPack.DOMINO_FALL -> { v.cameraDistance = 4200f * d; v.pivotY = v.height.toFloat().coerceAtLeast(1f); v.animate().rotationX(36f).setDuration(75L).start() }
            MotionPack.SLINGSHOT_PULL -> v.animate().translationY(10f * d).scaleX(0.88f).scaleY(1.12f).setDuration(85L).start()
            MotionPack.HEARTBEAT_PULSE -> v.animate().scaleX(1.14f).scaleY(1.14f).setDuration(65L).start()
            MotionPack.PORTAL_WARP -> v.animate().scaleX(0.68f).scaleY(1.30f).alpha(0.75f).setDuration(75L).start()
        }
    }

    fun animateKeypadTouchUp(activity: MainActivity, v: View, symbol: String) {
        val d = activity.resources.displayMetrics.density
        cancelRunning(v)

        when (currentPack) {
            MotionPack.LITE_FADE -> v.animate().alpha(1f).setDuration(110L).start()
            MotionPack.LITE_SLIDE -> v.animate().translationY(0f).setDuration(90L).setInterpolator(DecelerateInterpolator()).start()
            MotionPack.LITE_TAP -> v.animate().scaleX(1f).scaleY(1f).setDuration(105L).setInterpolator(DecelerateInterpolator()).start()
            MotionPack.LITE_SNAP -> v.animate().alpha(1f).setDuration(70L).start()
            MotionPack.LITE_SOFT_LIFT -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(120L).setInterpolator(DecelerateInterpolator()).start()

            MotionPack.ZEN_SILK -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).translationY(0f).setDuration(380L).setInterpolator(DecelerateInterpolator(2.0f)).start()
            MotionPack.LIQUID_SPRING -> v.animate().scaleX(1f).scaleY(1f).rotationX(0f).rotationY(0f).setDuration(380L).setInterpolator(OvershootInterpolator(2.8f)).start()
            MotionPack.AURORA_COSMIC -> {
                val ov = if (symbol == "=") 3.0f else 2.1f
                v.animate().scaleX(1f).scaleY(1f).rotationX(0f).rotationY(0f).setDuration(350L).setInterpolator(OvershootInterpolator(ov)).start()
            }
            MotionPack.CYBER_GLITCH -> {
                val tx = ObjectAnimator.ofFloat(v, "translationX", v.translationX, 7f * d, -5f * d, 3f * d, 0f)
                val al = ObjectAnimator.ofFloat(v, "alpha", 0.35f, 1f, 0.55f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(tx, al); duration = 180L })
            }
            MotionPack.ORIGAMI_3D -> v.animate().rotationX(0f).scaleY(1f).setDuration(310L).setInterpolator(OvershootInterpolator(1.7f)).start()
            MotionPack.JELLY_BOUNCE -> {
                val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 0.78f, 1.12f, 0.95f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 1.26f, 0.88f, 1.05f, 1f)
                val ty = ObjectAnimator.ofFloat(v, "translationY", v.translationY, 0f)
                startTracked(v, AnimatorSet().apply { playTogether(sx, sy, ty); duration = 420L })
            }
            MotionPack.MAGNETIC_FLOAT -> v.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(340L).setInterpolator(AnticipateOvershootInterpolator(1.3f)).start()
            MotionPack.MECHANICAL_CLICK -> v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(75L).setInterpolator(DecelerateInterpolator(2.5f)).start()
            MotionPack.WAVE_CASCADE -> {
                val ty = ObjectAnimator.ofFloat(v, "translationY", v.translationY, -6f * d, 0f)
                val scX = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 1.08f, 1f)
                val scY = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 1.08f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(ty, scX, scY); duration = 300L })
            }
            MotionPack.CINEMA_ZOOM -> v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(380L).setInterpolator(DecelerateInterpolator(2.0f)).start()
            MotionPack.GRAVITY_DROP -> v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(460L).setInterpolator(BounceInterpolator()).start()
            MotionPack.TORNADO_SPIN -> {
                val endRot = if (symbol == "=" || symbol == "AC") 360f else 0f
                val rot = ObjectAnimator.ofFloat(v, "rotation", v.rotation, endRot)
                val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 1.12f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 1.12f, 1f)
                startTracked(v, AnimatorSet().apply {
                    playTogether(rot, sx, sy)
                    duration = if (endRot == 360f) 420L else 280L
                    interpolator = OvershootInterpolator(1.6f)
                })
            }
            MotionPack.PENDULUM_SWING -> {
                v.pivotY = 0f
                val startR = if (abs(v.rotation) < 2f) 18f else v.rotation
                startTracked(v, ObjectAnimator.ofFloat(v, "rotation", startR, -startR * 0.65f, startR * 0.35f, -startR * 0.15f, 0f).apply { duration = 480L })
            }
            MotionPack.COIN_FLIP_3D -> {
                v.cameraDistance = 4200f * d
                val targetY = if (symbol == "=") 360f else 180f
                val flip = ObjectAnimator.ofFloat(v, "rotationY", v.rotationY, targetY)
                val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 1f)
                startTracked(v, AnimatorSet().apply {
                    playTogether(flip, sx)
                    duration = 340L
                    interpolator = DecelerateInterpolator(1.4f)
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) { v.rotationY = 0f }
                    })
                })
            }
            MotionPack.RADIAL_BURST -> {
                val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 1.24f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 1.24f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(sx, sy); duration = 300L; interpolator = DecelerateInterpolator(1.5f) })
            }
            MotionPack.CARD_SHUFFLE -> v.animate().translationX(0f).rotation(0f).setDuration(300L).setInterpolator(OvershootInterpolator(1.8f)).start()
            MotionPack.DOMINO_FALL -> v.animate().rotationX(0f).setDuration(320L).setInterpolator(OvershootInterpolator(2.0f)).start()
            MotionPack.SLINGSHOT_PULL -> {
                val ty = ObjectAnimator.ofFloat(v, "translationY", v.translationY, -12f * d, 0f)
                val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 1.08f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 0.92f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(ty, sx, sy); duration = 360L; interpolator = OvershootInterpolator(1.6f) })
            }
            MotionPack.HEARTBEAT_PULSE -> {
                val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 0.92f, 1.18f, 0.96f, 1.10f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 0.92f, 1.18f, 0.96f, 1.10f, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(sx, sy); duration = 420L })
            }
            MotionPack.PORTAL_WARP -> {
                val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 1.26f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 0.78f, 1f)
                val al = ObjectAnimator.ofFloat(v, "alpha", v.alpha, 1f)
                startTracked(v, AnimatorSet().apply { playTogether(sx, sy, al); duration = 320L; interpolator = OvershootInterpolator(1.4f) })
            }
        }
    }

    private fun triggerNeighborWaveRipple(activity: MainActivity, pressedView: View, d: Float) {
        val list = activity.keypadButtons.values.toList()
        val idx = list.indexOf(pressedView)
        if (idx < 0) return
        val cols = if (list.size > 26) 5 else 4
        val pRow = idx / cols
        val pCol = idx % cols

        for ((i, nb) in list.withIndex()) {
            if (i == idx) continue
            val dist = abs(i / cols - pRow) + abs(i % cols - pCol)
            if (dist == 1) {
                val ty = ObjectAnimator.ofFloat(nb, "translationY", 0f, -5f * d, 0f)
                val sc = ObjectAnimator.ofFloat(nb, "scaleX", 1f, 0.93f, 1f)
                startTracked(nb, AnimatorSet().apply {
                    playTogether(ty, sc)
                    startDelay = 40L
                    duration = 260L
                    interpolator = DecelerateInterpolator()
                })
            }
        }
    }

    // =========================================================================
    // 5. אנימציות צג התוצאה (הקלדה, '=', שגיאה וניקוי AC)
    // =========================================================================
    fun animateDisplayChange(activity: MainActivity, displayView: TextView?, isEquals: Boolean, isError: Boolean) {
        if (displayView == null) return
        val d = activity.resources.displayMetrics.density
        resetViewTransforms(displayView)

        if (isError) {
            if (currentPack.isLowPower) {
                displayView.alpha = 0.35f
                displayView.animate().alpha(1f).setDuration(140L).start()
            } else {
                startTracked(
                    displayView,
                    ObjectAnimator.ofFloat(
                        displayView, "translationX",
                        0f, 14f * d, -14f * d, 10f * d, -10f * d, 5f * d, 0f
                    ).apply { duration = 380L }
                )
            }
            return
        }

        // נתיב חסכוני לצג התוצאה
        if (currentPack.isLowPower) {
            when (currentPack) {
                MotionPack.LITE_FADE -> {
                    displayView.alpha = if (isEquals) 0.25f else 0.72f
                    displayView.animate().alpha(1f).setDuration(if (isEquals) 150L else 85L).start()
                }
                MotionPack.LITE_SLIDE -> {
                    displayView.translationX = (if (isEquals) 14f else 5f) * d
                    displayView.animate().translationX(0f).setDuration(if (isEquals) 150L else 80L)
                        .setInterpolator(DecelerateInterpolator()).start()
                }
                MotionPack.LITE_TAP -> {
                    if (isEquals) {
                        displayView.scaleX = 0.94f; displayView.scaleY = 0.94f
                        displayView.animate().scaleX(1f).scaleY(1f).setDuration(130L).start()
                    }
                }
                MotionPack.LITE_SNAP -> {
                    displayView.alpha = if (isEquals) 0.55f else 0.85f
                    displayView.animate().alpha(1f).setDuration(75L).start()
                }
                MotionPack.LITE_SOFT_LIFT -> {
                    displayView.translationY = (if (isEquals) 8f else 3f) * d
                    displayView.alpha = if (isEquals) 0.45f else 0.82f
                    displayView.animate().translationY(0f).alpha(1f).setDuration(if (isEquals) 160L else 90L)
                        .setInterpolator(DecelerateInterpolator()).start()
                }
                else -> {}
            }
            return
        }

        if (displayView.width > 0) {
            displayView.pivotX = displayView.width.toFloat()
            displayView.pivotY = displayView.height * 0.5f
        }

        if (isEquals) {
            when (currentPack) {
                MotionPack.ZEN_SILK -> {
                    displayView.alpha = 0.25f; displayView.translationY = 16f * d
                    displayView.animate().alpha(1f).translationY(0f).setDuration(480L).setInterpolator(DecelerateInterpolator(2.2f)).start()
                }
                MotionPack.LIQUID_SPRING -> {
                    displayView.scaleX = 0.65f; displayView.scaleY = 0.65f; displayView.alpha = 0.4f
                    displayView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(400L).setInterpolator(OvershootInterpolator(2.4f)).start()
                }
                MotionPack.AURORA_COSMIC -> {
                    displayView.scaleX = 1.35f; displayView.scaleY = 1.35f; displayView.translationY = -14f * d; displayView.alpha = 0.3f
                    displayView.animate().scaleX(1f).scaleY(1f).translationY(0f).alpha(1f).setDuration(420L).setInterpolator(OvershootInterpolator(1.6f)).start()
                }
                MotionPack.CYBER_GLITCH -> {
                    val tx = ObjectAnimator.ofFloat(displayView, "translationX", -18f * d, 14f * d, -9f * d, 5f * d, 0f)
                    val al = ObjectAnimator.ofFloat(displayView, "alpha", 0.1f, 1f, 0.2f, 1f, 0.5f, 1f)
                    startTracked(displayView, AnimatorSet().apply { playTogether(tx, al); duration = 280L })
                }
                MotionPack.ORIGAMI_3D -> {
                    displayView.cameraDistance = 6000f * d; displayView.pivotY = 0f; displayView.rotationX = -90f; displayView.alpha = 0.2f
                    displayView.animate().rotationX(0f).alpha(1f).setDuration(420L).setInterpolator(OvershootInterpolator(1.3f)).start()
                }
                MotionPack.JELLY_BOUNCE -> {
                    val sx = ObjectAnimator.ofFloat(displayView, "scaleX", 0.72f, 1.24f, 0.88f, 1.07f, 1f)
                    val sy = ObjectAnimator.ofFloat(displayView, "scaleY", 1.34f, 0.72f, 1.14f, 0.94f, 1f)
                    startTracked(displayView, AnimatorSet().apply { playTogether(sx, sy); duration = 480L })
                }
                MotionPack.MAGNETIC_FLOAT -> {
                    displayView.translationY = 30f * d; displayView.alpha = 0.2f
                    displayView.animate().translationY(0f).alpha(1f).setDuration(400L).setInterpolator(AnticipateOvershootInterpolator(1.5f)).start()
                }
                MotionPack.MECHANICAL_CLICK -> {
                    displayView.translationY = -8f * d
                    displayView.animate().translationY(0f).setDuration(95L).setInterpolator(DecelerateInterpolator(2.5f)).start()
                }
                MotionPack.WAVE_CASCADE -> {
                    displayView.translationX = -40f * d; displayView.rotation = -4f; displayView.alpha = 0.3f
                    displayView.animate().translationX(0f).rotation(0f).alpha(1f).setDuration(380L).setInterpolator(OvershootInterpolator(1.4f)).start()
                }
                MotionPack.CINEMA_ZOOM -> {
                    displayView.scaleX = 1.48f; displayView.scaleY = 1.48f; displayView.alpha = 0.15f
                    displayView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(520L).setInterpolator(DecelerateInterpolator(2.0f)).start()
                }
                MotionPack.GRAVITY_DROP -> {
                    displayView.translationY = -44f * d; displayView.alpha = 0.3f
                    displayView.animate().translationY(0f).alpha(1f).setDuration(560L).setInterpolator(BounceInterpolator()).start()
                }
                MotionPack.TORNADO_SPIN -> {
                    displayView.pivotX = displayView.width * 0.5f
                    displayView.rotation = -360f; displayView.scaleX = 0.35f; displayView.scaleY = 0.35f
                    displayView.animate().rotation(0f).scaleX(1f).scaleY(1f).setDuration(520L).setInterpolator(OvershootInterpolator(1.1f)).start()
                }
                MotionPack.PENDULUM_SWING -> {
                    displayView.pivotX = displayView.width * 0.5f; displayView.pivotY = 0f
                    startTracked(displayView, ObjectAnimator.ofFloat(displayView, "rotation", 18f, -12f, 7f, -3f, 0f).apply { duration = 560L })
                }
                MotionPack.COIN_FLIP_3D -> {
                    displayView.cameraDistance = 6000f * d; displayView.pivotX = displayView.width * 0.5f
                    displayView.rotationY = -180f; displayView.alpha = 0.25f
                    displayView.animate().rotationY(0f).alpha(1f).setDuration(460L).setInterpolator(DecelerateInterpolator(1.5f)).start()
                }
                MotionPack.RADIAL_BURST -> {
                    displayView.pivotX = displayView.width * 0.5f
                    displayView.scaleX = 0.2f; displayView.scaleY = 0.2f; displayView.alpha = 0.2f
                    displayView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(360L).setInterpolator(OvershootInterpolator(2.2f)).start()
                }
                MotionPack.CARD_SHUFFLE -> {
                    displayView.translationX = 90f * d; displayView.rotation = 8f; displayView.alpha = 0.2f
                    displayView.animate().translationX(0f).rotation(0f).alpha(1f).setDuration(380L).setInterpolator(OvershootInterpolator(1.2f)).start()
                }
                MotionPack.DOMINO_FALL -> {
                    displayView.cameraDistance = 5500f * d; displayView.pivotY = displayView.height.toFloat()
                    displayView.rotationX = 85f; displayView.alpha = 0.25f
                    displayView.animate().rotationX(0f).alpha(1f).setDuration(420L).setInterpolator(OvershootInterpolator(1.5f)).start()
                }
                MotionPack.SLINGSHOT_PULL -> {
                    displayView.translationX = -56f * d; displayView.alpha = 0.3f
                    displayView.animate().translationX(0f).alpha(1f).setDuration(460L).setInterpolator(AnticipateOvershootInterpolator(2.2f)).start()
                }
                MotionPack.HEARTBEAT_PULSE -> {
                    val sx = ObjectAnimator.ofFloat(displayView, "scaleX", 0.85f, 1.24f, 0.94f, 1.15f, 1f)
                    val sy = ObjectAnimator.ofFloat(displayView, "scaleY", 0.85f, 1.24f, 0.94f, 1.15f, 1f)
                    startTracked(displayView, AnimatorSet().apply { playTogether(sx, sy); duration = 500L })
                }
                MotionPack.PORTAL_WARP -> {
                    displayView.scaleX = 0.12f; displayView.scaleY = 2.0f; displayView.alpha = 0.2f
                    displayView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(420L).setInterpolator(OvershootInterpolator(1.4f)).start()
                }
                else -> {}
            }
            return
        }

        // הקלדת ספרה רגילה ב-20 החבילות העשירות
        when (currentPack) {
            MotionPack.ZEN_SILK -> { displayView.alpha = 0.50f; displayView.translationY = 4f * d; displayView.animate().alpha(1f).translationY(0f).setDuration(260L).start() }
            MotionPack.LIQUID_SPRING -> { displayView.scaleX = 0.91f; displayView.scaleY = 0.91f; displayView.animate().scaleX(1f).scaleY(1f).setDuration(220L).setInterpolator(OvershootInterpolator(2.0f)).start() }
            MotionPack.AURORA_COSMIC -> { displayView.translationY = -7f * d; displayView.alpha = 0.55f; displayView.animate().translationY(0f).alpha(1f).setDuration(200L).start() }
            MotionPack.CYBER_GLITCH -> startTracked(displayView, ObjectAnimator.ofFloat(displayView, "translationX", -7f * d, 5f * d, 0f).apply { duration = 120L })
            MotionPack.ORIGAMI_3D -> { displayView.cameraDistance = 5500f * d; displayView.rotationX = -32f; displayView.animate().rotationX(0f).setDuration(190L).start() }
            MotionPack.JELLY_BOUNCE -> {
                val sx = ObjectAnimator.ofFloat(displayView, "scaleX", 1.10f, 0.95f, 1f)
                val sy = ObjectAnimator.ofFloat(displayView, "scaleY", 0.86f, 1.06f, 1f)
                startTracked(displayView, AnimatorSet().apply { playTogether(sx, sy); duration = 220L })
            }
            MotionPack.MAGNETIC_FLOAT -> { displayView.translationY = 10f * d; displayView.animate().translationY(0f).setDuration(200L).setInterpolator(OvershootInterpolator(1.5f)).start() }
            MotionPack.MECHANICAL_CLICK -> { displayView.translationY = 4.5f * d; displayView.animate().translationY(0f).setDuration(65L).start() }
            MotionPack.WAVE_CASCADE -> { displayView.translationX = -10f * d; displayView.rotation = -2f; displayView.animate().translationX(0f).rotation(0f).setDuration(190L).start() }
            MotionPack.CINEMA_ZOOM -> { displayView.scaleX = 1.08f; displayView.scaleY = 1.08f; displayView.alpha = 0.65f; displayView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(250L).start() }
            MotionPack.GRAVITY_DROP -> { displayView.translationY = -14f * d; displayView.animate().translationY(0f).setDuration(280L).setInterpolator(BounceInterpolator()).start() }
            MotionPack.TORNADO_SPIN -> { displayView.rotation = -8f; displayView.scaleX = 0.93f; displayView.scaleY = 0.93f; displayView.animate().rotation(0f).scaleX(1f).scaleY(1f).setDuration(210L).setInterpolator(OvershootInterpolator(1.4f)).start() }
            MotionPack.PENDULUM_SWING -> { displayView.pivotY = 0f; startTracked(displayView, ObjectAnimator.ofFloat(displayView, "rotation", 6f, -3.5f, 0f).apply { duration = 260L }) }
            MotionPack.COIN_FLIP_3D -> { displayView.cameraDistance = 5500f * d; displayView.rotationY = -28f; displayView.animate().rotationY(0f).setDuration(200L).start() }
            MotionPack.RADIAL_BURST -> { displayView.scaleX = 0.86f; displayView.scaleY = 0.86f; displayView.animate().scaleX(1f).scaleY(1f).setDuration(180L).setInterpolator(OvershootInterpolator(2.0f)).start() }
            MotionPack.CARD_SHUFFLE -> { displayView.translationX = 18f * d; displayView.alpha = 0.6f; displayView.animate().translationX(0f).alpha(1f).setDuration(170L).start() }
            MotionPack.DOMINO_FALL -> { displayView.cameraDistance = 5500f * d; displayView.pivotY = displayView.height.toFloat(); displayView.rotationX = 28f; displayView.animate().rotationX(0f).setDuration(190L).setInterpolator(OvershootInterpolator(1.5f)).start() }
            MotionPack.SLINGSHOT_PULL -> { displayView.translationX = -14f * d; displayView.animate().translationX(0f).setDuration(230L).setInterpolator(AnticipateOvershootInterpolator(1.6f)).start() }
            MotionPack.HEARTBEAT_PULSE -> {
                val sx = ObjectAnimator.ofFloat(displayView, "scaleX", 1f, 1.09f, 0.97f, 1.05f, 1f)
                val sy = ObjectAnimator.ofFloat(displayView, "scaleY", 1f, 1.09f, 0.97f, 1.05f, 1f)
                startTracked(displayView, AnimatorSet().apply { playTogether(sx, sy); duration = 240L })
            }
            MotionPack.PORTAL_WARP -> { displayView.scaleX = 0.78f; displayView.scaleY = 1.24f; displayView.animate().scaleX(1f).scaleY(1f).setDuration(200L).setInterpolator(OvershootInterpolator(1.3f)).start() }
            else -> {}
        }
    }

    fun animateDisplayClear(activity: MainActivity, displayView: TextView?, exprView: TextView?) {
        val d = activity.resources.displayMetrics.density
        if (currentPack.isLowPower) {
            displayView?.let { v ->
                resetViewTransforms(v, resetPivot = false)
                v.alpha = 0.45f
                v.animate().alpha(1f).setDuration(105L).start()
            }
            return
        }
        displayView?.let { v ->
            resetViewTransforms(v)
            when (currentPack) {
                MotionPack.ORIGAMI_3D, MotionPack.DOMINO_FALL -> {
                    v.cameraDistance = 5500f * d; v.rotationX = 75f; v.alpha = 0.2f
                    v.animate().rotationX(0f).alpha(1f).setDuration(300L).start()
                }
                MotionPack.TORNADO_SPIN -> {
                    v.rotation = 180f; v.scaleX = 0.3f; v.scaleY = 0.3f; v.alpha = 0.2f
                    v.animate().rotation(0f).scaleX(1f).scaleY(1f).alpha(1f).setDuration(360L).start()
                }
                MotionPack.COIN_FLIP_3D -> {
                    v.cameraDistance = 5500f * d; v.rotationY = 180f; v.alpha = 0.2f
                    v.animate().rotationY(0f).alpha(1f).setDuration(340L).start()
                }
                MotionPack.GRAVITY_DROP -> {
                    v.translationY = -36f * d; v.alpha = 0.2f
                    v.animate().translationY(0f).alpha(1f).setDuration(420L).setInterpolator(BounceInterpolator()).start()
                }
                MotionPack.PORTAL_WARP, MotionPack.RADIAL_BURST -> {
                    v.scaleX = 0.1f; v.scaleY = 0.1f; v.alpha = 0.15f
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(300L).setInterpolator(OvershootInterpolator(1.5f)).start()
                }
                MotionPack.CYBER_GLITCH -> {
                    val tx = ObjectAnimator.ofFloat(v, "translationX", -20f * d, 16f * d, -10f * d, 0f)
                    val al = ObjectAnimator.ofFloat(v, "alpha", 0.1f, 1f, 0.25f, 1f)
                    startTracked(v, AnimatorSet().apply { playTogether(tx, al); duration = 240L })
                }
                MotionPack.JELLY_BOUNCE -> {
                    val sx = ObjectAnimator.ofFloat(v, "scaleX", 1.3f, 0.8f, 1.08f, 1f)
                    val sy = ObjectAnimator.ofFloat(v, "scaleY", 0.55f, 1.2f, 0.92f, 1f)
                    startTracked(v, AnimatorSet().apply { playTogether(sx, sy); duration = 360L })
                }
                MotionPack.CINEMA_ZOOM -> {
                    v.scaleX = 1.4f; v.scaleY = 1.4f; v.alpha = 0.15f
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(420L).start()
                }
                else -> {
                    v.alpha = 0.2f; v.translationX = -26f * d
                    v.animate().alpha(1f).translationX(0f).setDuration(260L).setInterpolator(DecelerateInterpolator()).start()
                }
            }
        }
        exprView?.let { v ->
            resetViewTransforms(v); v.alpha = 0.2f
            v.animate().alpha(1f).setDuration(200L).start()
        }
    }

    // =========================================================================
    // 6. תפריט צד + אקורדיון קטגוריות (בחבילות חסכוניות — אפס פרלקסה על המסך!)
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
        val d = activity.resources.displayMetrics.density

        // נתיב חסכוני: מזיז רק את התפריט עצמו בלי להקטין או להזיז את המסך הראשי
        if (currentPack.isLowPower) {
            val dur = if (currentPack == MotionPack.LITE_SNAP) 135L else 195L
            mainContent?.apply { scaleX = 1f; scaleY = 1f; translationX = 0f; rotationY = 0f }
            cancelRunning(drawerScroll)
            drawerScroll.rotation = 0f; drawerScroll.rotationY = 0f; drawerScroll.scaleX = 1f; drawerScroll.scaleY = 1f; drawerScroll.alpha = 1f
            if (isOpening) {
                dimView.visibility = View.VISIBLE
                dimView.animate().cancel()
                dimView.animate().alpha(1f).setDuration(dur).start()
                drawerScroll.animate().translationX(0f).setDuration(dur)
                    .setInterpolator(DecelerateInterpolator(1.4f)).start()
            } else {
                dimView.animate().cancel()
                dimView.animate().alpha(0f).setDuration(dur - 30L)
                    .withEndAction { dimView.visibility = View.GONE }.start()
                drawerScroll.animate().translationX(closedX).setDuration(dur - 30L)
                    .setInterpolator(AccelerateInterpolator(1.2f)).start()
            }
            return
        }

        val openDur = when (currentPack) {
            MotionPack.MECHANICAL_CLICK, MotionPack.CYBER_GLITCH -> 190L
            MotionPack.ZEN_SILK, MotionPack.CINEMA_ZOOM -> 480L
            MotionPack.GRAVITY_DROP -> 520L
            MotionPack.LIQUID_SPRING, MotionPack.JELLY_BOUNCE, MotionPack.ORIGAMI_3D, MotionPack.SLINGSHOT_PULL -> 380L
            else -> 320L
        }
        val closeDur = if (currentPack == MotionPack.MECHANICAL_CLICK) 150L else 250L

        if (isOpening) {
            dimView.visibility = View.VISIBLE
            dimView.animate().cancel()
            dimView.animate().alpha(1f).setDuration(openDur).start()

            cancelRunning(drawerScroll)
            drawerScroll.rotation = 0f; drawerScroll.rotationY = 0f; drawerScroll.scaleX = 1f; drawerScroll.scaleY = 1f

            when (currentPack) {
                MotionPack.ORIGAMI_3D, MotionPack.COIN_FLIP_3D -> {
                    drawerScroll.cameraDistance = 9000f * d
                    drawerScroll.rotationY = 48f * dirSign
                    drawerScroll.animate().translationX(0f).rotationY(0f).setDuration(openDur).setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                MotionPack.GRAVITY_DROP -> drawerScroll.animate().translationX(0f).setDuration(openDur).setInterpolator(BounceInterpolator()).start()
                MotionPack.SLINGSHOT_PULL -> drawerScroll.animate().translationX(0f).setDuration(openDur).setInterpolator(AnticipateOvershootInterpolator(1.2f)).start()
                MotionPack.JELLY_BOUNCE -> {
                    drawerScroll.scaleX = 1.14f; drawerScroll.scaleY = 0.94f
                    drawerScroll.animate().translationX(0f).scaleX(1f).scaleY(1f).setDuration(openDur).setInterpolator(OvershootInterpolator(1.1f)).start()
                }
                MotionPack.PORTAL_WARP -> {
                    drawerScroll.scaleX = 0.45f; drawerScroll.alpha = 0.3f
                    drawerScroll.animate().translationX(0f).scaleX(1f).alpha(1f).setDuration(openDur).setInterpolator(OvershootInterpolator(0.9f)).start()
                }
                MotionPack.PENDULUM_SWING -> {
                    drawerScroll.pivotY = 0f; drawerScroll.rotation = -8f * dirSign
                    drawerScroll.animate().translationX(0f).rotation(0f).setDuration(openDur).setInterpolator(OvershootInterpolator(1.2f)).start()
                }
                MotionPack.LIQUID_SPRING, MotionPack.AURORA_COSMIC -> {
                    drawerScroll.animate().translationX(0f).setDuration(openDur).setInterpolator(OvershootInterpolator(0.75f)).start()
                }
                else -> drawerScroll.animate().translationX(0f).setDuration(openDur).setInterpolator(DecelerateInterpolator(1.9f)).start()
            }

            if (mainContent != null) {
                cancelRunning(mainContent)
                when (currentPack) {
                    MotionPack.LIQUID_SPRING, MotionPack.AURORA_COSMIC, MotionPack.CINEMA_ZOOM, MotionPack.PORTAL_WARP -> {
                        mainContent.animate().scaleX(0.91f).scaleY(0.91f).translationX(-26f * d * dirSign)
                            .setDuration(openDur).setInterpolator(DecelerateInterpolator(1.6f)).start()
                    }
                    MotionPack.ORIGAMI_3D -> {
                        mainContent.cameraDistance = 10000f * d
                        mainContent.animate().scaleX(0.92f).scaleY(0.92f).rotationY(-10f * dirSign)
                            .translationX(-20f * d * dirSign).setDuration(openDur).start()
                    }
                    MotionPack.MAGNETIC_FLOAT, MotionPack.WAVE_CASCADE, MotionPack.ZEN_SILK, MotionPack.CARD_SHUFFLE -> {
                        mainContent.animate().translationX(-18f * d * dirSign).setDuration(openDur)
                            .setInterpolator(DecelerateInterpolator(1.5f)).start()
                    }
                    else -> {
                        mainContent.scaleX = 1f; mainContent.scaleY = 1f; mainContent.translationX = 0f; mainContent.rotationY = 0f
                    }
                }
            }
        } else {
            dimView.animate().cancel()
            dimView.animate().alpha(0f).setDuration(closeDur).withEndAction { dimView.visibility = View.GONE }.start()

            cancelRunning(drawerScroll)
            drawerScroll.animate().translationX(closedX).rotationY(0f).rotation(0f).scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(closeDur).setInterpolator(AccelerateInterpolator(1.4f)).start()

            mainContent?.let { mc ->
                cancelRunning(mc)
                mc.animate().scaleX(1f).scaleY(1f).translationX(0f).rotationY(0f)
                    .setDuration(closeDur).setInterpolator(DecelerateInterpolator(1.5f)).start()
            }
        }
    }

    fun animateSectionAccordionExpand(activity: MainActivity, container: View) {
        if (currentPack.isLowPower) {
            resetViewTransforms(container, resetPivot = false)
            if (currentPack == MotionPack.LITE_TAP) return
            container.alpha = 0f
            container.animate().alpha(1f).setDuration(if (currentPack == MotionPack.LITE_SNAP) 85L else 140L).start()
            return
        }

        val d = activity.resources.displayMetrics.density
        runWhenLaidOut(container, preHide = true) {
            resetViewTransforms(container)
            container.alpha = 0f
            when (currentPack) {
                MotionPack.ORIGAMI_3D, MotionPack.DOMINO_FALL -> {
                    container.cameraDistance = 6000f * d; container.pivotY = 0f; container.rotationX = -48f
                    container.animate().alpha(1f).rotationX(0f).setDuration(320L).setInterpolator(DecelerateInterpolator()).start()
                }
                MotionPack.GRAVITY_DROP -> {
                    container.translationY = -28f * d
                    container.animate().alpha(1f).translationY(0f).setDuration(420L).setInterpolator(BounceInterpolator()).start()
                }
                MotionPack.JELLY_BOUNCE, MotionPack.LIQUID_SPRING -> {
                    container.pivotY = 0f; container.scaleY = 0.65f; container.scaleX = 1.06f
                    container.animate().alpha(1f).scaleY(1f).scaleX(1f).setDuration(340L).setInterpolator(OvershootInterpolator(1.6f)).start()
                }
                MotionPack.CARD_SHUFFLE, MotionPack.WAVE_CASCADE -> {
                    container.translationX = (if (activity.isRightHanded) 40f else -40f) * d
                    container.animate().alpha(1f).translationX(0f).setDuration(280L).setInterpolator(OvershootInterpolator(1.1f)).start()
                }
                MotionPack.PORTAL_WARP -> {
                    container.scaleX = 0.2f; container.scaleY = 1.4f
                    container.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300L).setInterpolator(OvershootInterpolator(1.2f)).start()
                }
                MotionPack.CYBER_GLITCH -> {
                    val tx = ObjectAnimator.ofFloat(container, "translationX", -14f * d, 10f * d, -4f * d, 0f)
                    val al = ObjectAnimator.ofFloat(container, "alpha", 0f, 1f, 0.35f, 1f)
                    startTracked(container, AnimatorSet().apply { playTogether(tx, al); duration = 200L })
                }
                MotionPack.MECHANICAL_CLICK -> {
                    container.translationY = -6f * d
                    container.animate().alpha(1f).translationY(0f).setDuration(110L).start()
                }
                else -> {
                    container.translationY = -14f * d
                    container.animate().alpha(1f).translationY(0f).setDuration(280L).setInterpolator(DecelerateInterpolator(1.7f)).start()
                }
            }
        }
    }

    // =========================================================================
    // 7. אייקוני כותרת, דיאלוגים, לחיצה טקטילית ופוקוס שדות קלט
    // =========================================================================
    fun animateHeaderIconTap(view: View, kind: String) {
        cancelRunning(view)
        if (currentPack.isLowPower) {
            view.alpha = 0.55f
            view.animate().alpha(1f).setDuration(105L).start()
            return
        }
        when (currentPack) {
            MotionPack.TORNADO_SPIN, MotionPack.COIN_FLIP_3D -> {
                val prop = if (currentPack == MotionPack.COIN_FLIP_3D) "rotationY" else "rotation"
                startTracked(view, ObjectAnimator.ofFloat(view, prop, 0f, 360f).apply {
                    duration = 420L; interpolator = OvershootInterpolator(1.2f)
                })
            }
            MotionPack.JELLY_BOUNCE -> {
                val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.38f, 0.78f, 1.12f, 1f)
                val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.70f, 1.30f, 0.90f, 1f)
                startTracked(view, AnimatorSet().apply { playTogether(sx, sy); duration = 380L })
            }
            MotionPack.HEARTBEAT_PULSE -> {
                val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.36f, 0.92f, 1.24f, 1f)
                val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.36f, 0.92f, 1.24f, 1f)
                startTracked(view, AnimatorSet().apply { playTogether(sx, sy); duration = 420L })
            }
            MotionPack.PENDULUM_SWING -> {
                startTracked(view, ObjectAnimator.ofFloat(view, "rotation", 0f, 28f, -22f, 12f, -6f, 0f).apply { duration = 460L })
            }
            MotionPack.CYBER_GLITCH -> {
                val tx = ObjectAnimator.ofFloat(view, "translationX", 0f, -8f, 8f, -4f, 0f)
                val al = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.3f, 1f, 0.5f, 1f)
                startTracked(view, AnimatorSet().apply { playTogether(tx, al); duration = 200L })
            }
            else -> {
                val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, if (kind == "fav") 360f else -36f, 0f)
                val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.28f, 1f)
                val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.28f, 1f)
                startTracked(view, AnimatorSet().apply {
                    playTogether(rot, sx, sy)
                    duration = if (currentPack == MotionPack.MECHANICAL_CLICK) 170L else 360L
                    interpolator = OvershootInterpolator(1.5f)
                })
            }
        }
    }

    fun animateDialogEntrance(activity: MainActivity, dialogRoot: View) {
        val d = activity.resources.displayMetrics.density
        if (currentPack.isLowPower) {
            resetViewTransforms(dialogRoot, resetPivot = false)
            when (currentPack) {
                MotionPack.LITE_TAP -> dialogRoot.alpha = 1f
                MotionPack.LITE_SNAP -> { dialogRoot.alpha = 0.45f; dialogRoot.animate().alpha(1f).setDuration(90L).start() }
                MotionPack.LITE_SOFT_LIFT -> { dialogRoot.alpha = 0f; dialogRoot.translationY = 10f * d; dialogRoot.animate().alpha(1f).translationY(0f).setDuration(165L).start() }
                else -> { dialogRoot.alpha = 0f; dialogRoot.animate().alpha(1f).setDuration(150L).start() }
            }
            return
        }

        runWhenLaidOut(dialogRoot, preHide = true) {
            resetViewTransforms(dialogRoot)
            dialogRoot.alpha = 0f
            when (currentPack) {
                MotionPack.ZEN_SILK -> { dialogRoot.translationY = 20f * d; dialogRoot.scaleX = 0.96f; dialogRoot.scaleY = 0.96f; dialogRoot.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(460L).setInterpolator(DecelerateInterpolator(2.0f)).start() }
                MotionPack.LIQUID_SPRING -> { dialogRoot.scaleX = 0.68f; dialogRoot.scaleY = 0.68f; dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(380L).setInterpolator(OvershootInterpolator(1.8f)).start() }
                MotionPack.AURORA_COSMIC -> { dialogRoot.cameraDistance = 8000f * d; dialogRoot.rotationX = -30f; dialogRoot.translationY = 30f * d; dialogRoot.scaleX = 0.82f; dialogRoot.scaleY = 0.82f; dialogRoot.animate().alpha(1f).rotationX(0f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(400L).setInterpolator(OvershootInterpolator(1.3f)).start() }
                MotionPack.CYBER_GLITCH -> {
                    val tx = ObjectAnimator.ofFloat(dialogRoot, "translationX", -22f * d, 16f * d, -8f * d, 0f)
                    val al = ObjectAnimator.ofFloat(dialogRoot, "alpha", 0f, 1f, 0.3f, 1f)
                    startTracked(dialogRoot, AnimatorSet().apply { playTogether(tx, al); duration = 240L })
                }
                MotionPack.ORIGAMI_3D -> { dialogRoot.cameraDistance = 8000f * d; dialogRoot.pivotY = 0f; dialogRoot.rotationX = -65f; dialogRoot.animate().alpha(1f).rotationX(0f).setDuration(420L).setInterpolator(DecelerateInterpolator(1.6f)).start() }
                MotionPack.JELLY_BOUNCE -> {
                    val sx = ObjectAnimator.ofFloat(dialogRoot, "scaleX", 1.26f, 0.84f, 1.08f, 1f)
                    val sy = ObjectAnimator.ofFloat(dialogRoot, "scaleY", 0.58f, 1.22f, 0.92f, 1f)
                    val al = ObjectAnimator.ofFloat(dialogRoot, "alpha", 0f, 1f)
                    startTracked(dialogRoot, AnimatorSet().apply { playTogether(sx, sy, al); duration = 450L })
                }
                MotionPack.MAGNETIC_FLOAT -> { dialogRoot.translationY = 65f * d; dialogRoot.animate().alpha(1f).translationY(0f).setDuration(400L).setInterpolator(AnticipateOvershootInterpolator(1.2f)).start() }
                MotionPack.MECHANICAL_CLICK -> { dialogRoot.scaleX = 0.94f; dialogRoot.scaleY = 0.94f; dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120L).setInterpolator(DecelerateInterpolator(2.4f)).start() }
                MotionPack.WAVE_CASCADE -> { dialogRoot.translationY = 30f * d; dialogRoot.rotation = -6f; dialogRoot.animate().alpha(1f).translationY(0f).rotation(0f).setDuration(380L).setInterpolator(OvershootInterpolator(1.3f)).start() }
                MotionPack.CINEMA_ZOOM -> { dialogRoot.scaleX = 1.32f; dialogRoot.scaleY = 1.32f; dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(480L).setInterpolator(DecelerateInterpolator(2.0f)).start() }
                MotionPack.GRAVITY_DROP -> { dialogRoot.translationY = -90f * d; dialogRoot.animate().alpha(1f).translationY(0f).setDuration(580L).setInterpolator(BounceInterpolator()).start() }
                MotionPack.TORNADO_SPIN -> { dialogRoot.rotation = -90f; dialogRoot.scaleX = 0.35f; dialogRoot.scaleY = 0.35f; dialogRoot.animate().alpha(1f).rotation(0f).scaleX(1f).scaleY(1f).setDuration(440L).setInterpolator(OvershootInterpolator(1.2f)).start() }
                MotionPack.PENDULUM_SWING -> {
                    dialogRoot.pivotY = 0f
                    val rot = ObjectAnimator.ofFloat(dialogRoot, "rotation", 18f, -11f, 6f, -3f, 0f)
                    val al = ObjectAnimator.ofFloat(dialogRoot, "alpha", 0f, 1f)
                    startTracked(dialogRoot, AnimatorSet().apply { playTogether(rot, al); duration = 560L })
                }
                MotionPack.COIN_FLIP_3D -> { dialogRoot.cameraDistance = 9000f * d; dialogRoot.rotationY = -120f; dialogRoot.animate().alpha(1f).rotationY(0f).setDuration(440L).setInterpolator(DecelerateInterpolator(1.5f)).start() }
                MotionPack.RADIAL_BURST -> { dialogRoot.scaleX = 0.18f; dialogRoot.scaleY = 0.18f; dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(360L).setInterpolator(OvershootInterpolator(2.0f)).start() }
                MotionPack.CARD_SHUFFLE -> { dialogRoot.translationX = 130f * d; dialogRoot.rotation = 12f; dialogRoot.animate().alpha(1f).translationX(0f).rotation(0f).setDuration(400L).setInterpolator(OvershootInterpolator(1.1f)).start() }
                MotionPack.DOMINO_FALL -> { dialogRoot.cameraDistance = 8000f * d; dialogRoot.pivotY = dialogRoot.height.toFloat(); dialogRoot.rotationX = 72f; dialogRoot.animate().alpha(1f).rotationX(0f).setDuration(420L).setInterpolator(OvershootInterpolator(1.3f)).start() }
                MotionPack.SLINGSHOT_PULL -> { dialogRoot.translationY = 70f * d; dialogRoot.animate().alpha(1f).translationY(0f).setDuration(480L).setInterpolator(AnticipateOvershootInterpolator(2.0f)).start() }
                MotionPack.HEARTBEAT_PULSE -> {
                    val sx = ObjectAnimator.ofFloat(dialogRoot, "scaleX", 0.72f, 1.12f, 0.94f, 1.06f, 1f)
                    val sy = ObjectAnimator.ofFloat(dialogRoot, "scaleY", 0.72f, 1.12f, 0.94f, 1.06f, 1f)
                    val al = ObjectAnimator.ofFloat(dialogRoot, "alpha", 0f, 1f)
                    startTracked(dialogRoot, AnimatorSet().apply { playTogether(sx, sy, al); duration = 480L })
                }
                MotionPack.PORTAL_WARP -> { dialogRoot.scaleX = 0.12f; dialogRoot.scaleY = 1.85f; dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(420L).setInterpolator(OvershootInterpolator(1.3f)).start() }
                else -> { dialogRoot.alpha = 1f }
            }
        }
    }

    fun handleTactilePress(v: View, actionMasked: Int) {
        val d = v.resources.displayMetrics.density
        if (currentPack.isLowPower) {
            when (actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    cancelRunning(v)
                    if (currentPack == MotionPack.LITE_TAP || currentPack == MotionPack.LITE_SOFT_LIFT) {
                        v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(55L).start()
                    } else {
                        v.animate().alpha(0.75f).setDuration(45L).start()
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    cancelRunning(v)
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(95L).start()
                }
            }
            return
        }

        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancelRunning(v)
                when (currentPack) {
                    MotionPack.JELLY_BOUNCE -> v.animate().scaleX(1.07f).scaleY(0.86f).setDuration(65L).start()
                    MotionPack.MAGNETIC_FLOAT -> v.animate().scaleX(1.04f).scaleY(1.04f).translationY(-4f * d).setDuration(75L).start()
                    MotionPack.MECHANICAL_CLICK -> v.animate().translationY(4.5f * d).setDuration(35L).start()
                    MotionPack.CYBER_GLITCH -> v.animate().translationX(-4f * d).alpha(0.75f).setDuration(45L).start()
                    MotionPack.ORIGAMI_3D -> { v.cameraDistance = 6000f * d; v.animate().rotationX(-14f).setDuration(75L).start() }
                    MotionPack.TORNADO_SPIN -> v.animate().rotation(-3f).scaleX(0.95f).scaleY(0.95f).setDuration(65L).start()
                    MotionPack.PENDULUM_SWING -> { v.pivotY = 0f; v.animate().rotation(4f).setDuration(70L).start() }
                    MotionPack.HEARTBEAT_PULSE -> v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(65L).start()
                    MotionPack.PORTAL_WARP -> v.animate().scaleX(0.90f).scaleY(1.10f).setDuration(70L).start()
                    else -> v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70L).start()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                cancelRunning(v)
                when (currentPack) {
                    MotionPack.GRAVITY_DROP -> v.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(360L).setInterpolator(BounceInterpolator()).start()
                    MotionPack.JELLY_BOUNCE -> {
                        val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 0.94f, 1.03f, 1f)
                        val sy = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 1.08f, 0.97f, 1f)
                        startTracked(v, AnimatorSet().apply { playTogether(sx, sy); duration = 300L })
                    }
                    MotionPack.MECHANICAL_CLICK -> v.animate().translationY(0f).scaleX(1f).scaleY(1f).setDuration(75L).start()
                    else -> {
                        val interp = when (currentPack) {
                            MotionPack.LIQUID_SPRING, MotionPack.AURORA_COSMIC, MotionPack.RADIAL_BURST -> OvershootInterpolator(2.0f)
                            MotionPack.SLINGSHOT_PULL -> AnticipateOvershootInterpolator(1.3f)
                            else -> DecelerateInterpolator()
                        }
                        v.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f).rotation(0f).rotationX(0f).alpha(1f)
                            .setDuration(250L).setInterpolator(interp).start()
                    }
                }
            }
        }
    }

    fun handleInputFocus(activity: MainActivity, view: View, hasFocus: Boolean) {
        if (currentPack.isLowPower) return // אפס עיבוד מיותר בשדות קלט בחבילות החסכוניות
        val d = activity.resources.displayMetrics.density
        cancelRunning(view)
        if (hasFocus) {
            when (currentPack) {
                MotionPack.MAGNETIC_FLOAT -> view.animate().scaleX(1.04f).scaleY(1.04f).translationY(-5f * d).setDuration(220L).setInterpolator(OvershootInterpolator(1.4f)).start()
                MotionPack.JELLY_BOUNCE -> {
                    val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.06f, 0.98f, 1.02f)
                    val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.92f, 1.05f, 1.02f)
                    startTracked(view, AnimatorSet().apply { playTogether(sx, sy); duration = 300L })
                }
                MotionPack.CYBER_GLITCH -> startTracked(view, ObjectAnimator.ofFloat(view, "translationX", 0f, -5f * d, 5f * d, -2f * d, 0f).apply { duration = 180L })
                MotionPack.HEARTBEAT_PULSE -> {
                    val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f, 0.99f, 1.04f, 1.02f)
                    val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f, 0.99f, 1.04f, 1.02f)
                    startTracked(view, AnimatorSet().apply { playTogether(sx, sy); duration = 340L })
                }
                MotionPack.PORTAL_WARP -> view.animate().scaleX(1.03f).scaleY(1.06f).setDuration(200L).setInterpolator(OvershootInterpolator(1.3f)).start()
                else -> view.animate().scaleX(1.022f).scaleY(1.022f).translationY(-1.5f * d).setDuration(190L).setInterpolator(DecelerateInterpolator()).start()
            }
        } else {
            view.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f).setDuration(160L).start()
        }
    }

    // =========================================================================
    // תצוגה מקדימה חיה על הכרטיס בתפריט הבחירה (25 חבילות)
    // =========================================================================
    private fun playPackPreviewOnCard(activity: MainActivity, cardView: View, iconView: View, pack: MotionPack) {
        val d = activity.resources.displayMetrics.density
        resetViewTransforms(cardView)
        resetViewTransforms(iconView)

        when (pack) {
            MotionPack.LITE_FADE -> {
                cardView.alpha = 0.2f
                cardView.animate().alpha(1f).setDuration(170L).start()
            }
            MotionPack.LITE_SLIDE -> {
                cardView.alpha = 0.35f; cardView.translationX = 22f * d
                cardView.animate().alpha(1f).translationX(0f).setDuration(180L).setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.LITE_TAP -> {
                cardView.scaleX = 0.94f; cardView.scaleY = 0.94f
                cardView.animate().scaleX(1f).scaleY(1f).setDuration(130L).setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.LITE_SNAP -> {
                cardView.alpha = 0.50f
                cardView.animate().alpha(1f).setDuration(90L).start()
            }
            MotionPack.LITE_SOFT_LIFT -> {
                cardView.alpha = 0.25f; cardView.translationY = 12f * d
                cardView.animate().alpha(1f).translationY(0f).setDuration(185L).setInterpolator(DecelerateInterpolator()).start()
            }

            MotionPack.ZEN_SILK -> { cardView.alpha = 0.25f; cardView.translationY = 14f * d; cardView.animate().alpha(1f).translationY(0f).setDuration(580L).setInterpolator(DecelerateInterpolator(2.2f)).start() }
            MotionPack.LIQUID_SPRING -> { cardView.scaleX = 0.78f; cardView.scaleY = 0.78f; cardView.animate().scaleX(1f).scaleY(1f).setDuration(420L).setInterpolator(OvershootInterpolator(2.6f)).start() }
            MotionPack.AURORA_COSMIC -> { cardView.cameraDistance = 6000f * d; cardView.rotationX = -48f; cardView.scaleX = 0.88f; cardView.animate().rotationX(0f).scaleX(1f).setDuration(440L).setInterpolator(OvershootInterpolator(1.6f)).start() }
            MotionPack.CYBER_GLITCH -> {
                val tx = ObjectAnimator.ofFloat(cardView, "translationX", 0f, -16f * d, 14f * d, -10f * d, 7f * d, -3f * d, 0f)
                val al = ObjectAnimator.ofFloat(cardView, "alpha", 1f, 0.15f, 1f, 0.25f, 1f, 0.5f, 1f)
                startTracked(cardView, AnimatorSet().apply { playTogether(tx, al); duration = 320L })
            }
            MotionPack.ORIGAMI_3D -> { cardView.cameraDistance = 6000f * d; cardView.pivotX = 0f; cardView.rotationY = -65f; cardView.animate().rotationY(0f).setDuration(480L).setInterpolator(DecelerateInterpolator(1.6f)).start() }
            MotionPack.JELLY_BOUNCE -> {
                val sx = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 1.18f, 0.84f, 1.09f, 0.96f, 1f)
                val sy = ObjectAnimator.ofFloat(cardView, "scaleY", 1f, 0.68f, 1.24f, 0.90f, 1.04f, 1f)
                startTracked(cardView, AnimatorSet().apply { playTogether(sx, sy); duration = 500L })
            }
            MotionPack.MAGNETIC_FLOAT -> { cardView.translationY = 24f * d; cardView.scaleX = 0.92f; cardView.animate().translationY(0f).scaleX(1f).setDuration(420L).setInterpolator(AnticipateOvershootInterpolator(1.6f)).start() }
            MotionPack.MECHANICAL_CLICK -> startTracked(cardView, ObjectAnimator.ofFloat(cardView, "translationY", 0f, 8f * d, 0f).apply { duration = 120L })
            MotionPack.WAVE_CASCADE -> {
                val ty = ObjectAnimator.ofFloat(cardView, "translationY", 0f, -12f * d, 6f * d, 0f)
                val rot = ObjectAnimator.ofFloat(cardView, "rotation", 0f, -5f, 4f, -2f, 0f)
                startTracked(cardView, AnimatorSet().apply { playTogether(ty, rot); duration = 450L })
            }
            MotionPack.CINEMA_ZOOM -> { cardView.scaleX = 1.20f; cardView.scaleY = 1.20f; cardView.alpha = 0.25f; cardView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(540L).setInterpolator(DecelerateInterpolator(2.0f)).start() }
            MotionPack.GRAVITY_DROP -> { cardView.translationY = -38f * d; cardView.animate().translationY(0f).setDuration(600L).setInterpolator(BounceInterpolator()).start() }
            MotionPack.TORNADO_SPIN -> {
                startTracked(iconView, ObjectAnimator.ofFloat(iconView, "rotation", 0f, 720f).apply { duration = 560L; interpolator = OvershootInterpolator(1.1f) })
                startTracked(cardView, ObjectAnimator.ofFloat(cardView, "rotation", 0f, -10f, 6f, 0f).apply { duration = 460L })
            }
            MotionPack.PENDULUM_SWING -> { cardView.pivotY = 0f; startTracked(cardView, ObjectAnimator.ofFloat(cardView, "rotation", 0f, 16f, -12f, 7f, -3f, 0f).apply { duration = 620L }) }
            MotionPack.COIN_FLIP_3D -> { cardView.cameraDistance = 6500f * d; startTracked(cardView, ObjectAnimator.ofFloat(cardView, "rotationY", 0f, 360f).apply { duration = 540L; interpolator = DecelerateInterpolator(1.4f) }) }
            MotionPack.RADIAL_BURST -> { cardView.scaleX = 0.25f; cardView.scaleY = 0.25f; cardView.animate().scaleX(1f).scaleY(1f).setDuration(400L).setInterpolator(OvershootInterpolator(2.0f)).start() }
            MotionPack.CARD_SHUFFLE -> { cardView.translationX = 85f * d; cardView.rotation = 10f; cardView.animate().translationX(0f).rotation(0f).setDuration(420L).setInterpolator(OvershootInterpolator(1.3f)).start() }
            MotionPack.DOMINO_FALL -> { cardView.cameraDistance = 6000f * d; cardView.pivotY = cardView.height.toFloat(); cardView.rotationX = 78f; cardView.animate().rotationX(0f).setDuration(460L).setInterpolator(OvershootInterpolator(1.6f)).start() }
            MotionPack.SLINGSHOT_PULL -> { cardView.translationX = -46f * d; cardView.animate().translationX(0f).setDuration(520L).setInterpolator(AnticipateOvershootInterpolator(2.3f)).start() }
            MotionPack.HEARTBEAT_PULSE -> {
                val sx = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 1.12f, 0.95f, 1.09f, 1f)
                val sy = ObjectAnimator.ofFloat(cardView, "scaleY", 1f, 1.12f, 0.95f, 1.09f, 1f)
                startTracked(cardView, AnimatorSet().apply { playTogether(sx, sy); duration = 520L })
            }
            MotionPack.PORTAL_WARP -> { cardView.scaleX = 0.14f; cardView.scaleY = 1.80f; cardView.alpha = 0.3f; cardView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(440L).setInterpolator(OvershootInterpolator(1.4f)).start() }
        }
    }

    // =========================================================================
    // תפריט בחירת חבילת האנימציות (25 סגנונות — 5 חסכוניות + 20 עשירות)
    // =========================================================================
    fun showPackPickerDialog(activity: MainActivity) {
        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = activity.isMinimalistMode
        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#CBD5E1")
        val accentCol = when {
            isMin -> MinimalistTheme.ACCENT_SAGE_GREEN
            activity.isAuroraMode -> AuroraBorealisTheme.AURORA_PRIMARY
            else -> activity.primaryColor
        }
        // רקע אטום וחד יותר במצב הזכוכית למניעת חדירת טקסט מאחורי הדיאלוג
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
                setStroke(activity.dp(1.3f), Color.argb(130, 255, 255, 255))
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
            text = "\u200Fחבילות אנימציות ממשק (25 סגנונות)"
            setTextColor(textPrimary)
            textSize = 18.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
        })

        root.addView(TextView(activity).apply {
            text = "\u200Fבחר חבילת תנועה • לחץ 'הדגם' לתצוגה מקדימה חיה"
            setTextColor(textSecondary)
            textSize = 12.6f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(3f), 0, activity.dp(10f))
        })

        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.56f).toInt().coerceAtMost(activity.dp(460f))
            ).apply { bottomMargin = activity.dp(10f) }
        }
        val listCol = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listCol)
        root.addView(scroll)

        fun addSectionHeader(title: String, color: Int) {
            listCol.addView(TextView(activity).apply {
                text = "\u200F$title"
                setTextColor(color)
                textSize = 12.8f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT
                setPadding(activity.dp(4f), activity.dp(6f), activity.dp(4f), activity.dp(6f))
            })
        }

        fun addPackCard(pack: MotionPack, displayNum: Int) {
            val isSelected = (pack == currentPack)
            val packAccent = Color.parseColor(pack.accentHex)
            val cardFill = when {
                isSelected -> Color.argb(56, Color.red(packAccent), Color.green(packAccent), Color.blue(packAccent))
                isMin -> MinimalistTheme.CARD_WHITE
                else -> Color.argb(34, 255, 255, 255)
            }
            val borderCol = when {
                isSelected -> packAccent
                isMin -> Color.parseColor("#E5DFD7")
                else -> Color.argb(46, 255, 255, 255)
            }

            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_LTR
                setPadding(activity.dp(12f), activity.dp(11f), activity.dp(12f), activity.dp(11f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(18f).toFloat()
                    setColor(cardFill)
                    setStroke(activity.dp(if (isSelected) 1.6f else 1f), borderCol)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = activity.dp(8f) }
            }

            val iconHolder = TextView(activity).apply {
                val badgeDrawable = createPackVectorIcon(activity, pack, packAccent, 28f)
                setCompoundDrawables(badgeDrawable, null, null, null)
                gravity = Gravity.CENTER
            }

            val previewChipBg = if (isMin) MinimalistTheme.BG_CREAM else Color.argb(54, Color.red(packAccent), Color.green(packAccent), Color.blue(packAccent))
            val previewBtn = TextView(activity).apply {
                text = "הדגם ▶"
                setTextColor(MinimalistTheme.ensureReadableTextOnBg(if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE, previewChipBg, 2.3))
                textSize = 11.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(activity.dp(10f), activity.dp(6f), activity.dp(10f), activity.dp(6f))
                background = GradientDrawable().apply {
                    cornerRadius = activity.dp(999f).toFloat()
                    setColor(previewChipBg)
                    setStroke(activity.dp(1f), packAccent)
                }
                setOnClickListener {
                    activity.feedback(it)
                    playPackPreviewOnCard(activity, card, iconHolder, pack)
                }
            }

            val textCol = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.RIGHT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = activity.dp(8f)
                    rightMargin = activity.dp(10f)
                }
            }
            val titleRowTxt = if (isSelected) "✓ ${pack.displayName} (פעיל)" else "$displayNum. ${pack.displayName}"
            textCol.addView(TextView(activity).apply {
                text = "\u200F$titleRowTxt"
                setTextColor(if (isSelected) MinimalistTheme.ensureReadableTextOnBg(packAccent, cardFill, 2.3) else textPrimary)
                textSize = 14.8f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT
            })

            // כיתוב קטן מתחת לשם החבילה עבור חבילות חסכוניות למכשירים חלשים
            if (pack.isLowPower) {
                val lowPowerCol = MinimalistTheme.ensureReadableTextOnBg(
                    if (isMin) Color.parseColor("#1B7A43") else Color.parseColor("#4ADE80"),
                    cardFill,
                    2.2
                )
                textCol.addView(TextView(activity).apply {
                    text = "\u200Fחסכונית למכשירים חלשים"
                    setTextColor(lowPowerCol)
                    textSize = 10.5f
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.RIGHT
                    setPadding(0, activity.dp(1f), 0, activity.dp(1f))
                })
            }

            textCol.addView(TextView(activity).apply {
                text = "\u200F${pack.description}"
                setTextColor(textSecondary)
                textSize = 11.6f
                gravity = Gravity.RIGHT
                setPadding(0, activity.dp(1.5f), 0, 0)
            })

            card.addView(previewBtn)
            card.addView(textCol)
            card.addView(iconHolder)

            card.setOnClickListener {
                activity.feedback(it)
                selectPack(activity, pack, autoEnableIfOff = true)
                Toast.makeText(activity, "✓ הופעלה חבילת אנימציות: ${pack.displayName}", Toast.LENGTH_SHORT).show()
                ProCrashGuardAndRecovery.safeDismissDialog(dlg)
                activity.switchTab(19)
            }
            listCol.addView(card)
        }

        val lowPowerPacks = MotionPack.values().filter { it.isLowPower }
        val richPacks = MotionPack.values().filter { !it.isLowPower }

        val liteHeaderCol = if (isMin) Color.parseColor("#1B7A43") else Color.parseColor("#4ADE80")
        addSectionHeader("חבילות חסכוניות למכשירים חלשים (${lowPowerPacks.size})", liteHeaderCol)
        lowPowerPacks.forEachIndexed { i, pack -> addPackCard(pack, i + 1) }

        addSectionHeader("חבילות אנימציה מלאות (${richPacks.size})", accentCol)
        richPacks.forEachIndexed { i, pack -> addPackCard(pack, lowPowerPacks.size + i + 1) }

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
        animateDialogEntrance(activity, root)
        val widthPx = (activity.resources.displayMetrics.widthPixels * 0.92f).toInt()
        dlg.window?.setLayout(widthPx, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    // =========================================================================
    // 25 סמלים וקטוריים אישיים (כולל 5 סמלים נקיים לחבילות החסכוניות)
    // =========================================================================
    private fun createPackVectorIcon(
        context: Context,
        pack: MotionPack,
        accentColor: Int,
        sizeDp: Float
    ): Drawable {
        val d = context.resources.displayMetrics.density
        val sizePx = (sizeDp * d).toInt().coerceAtLeast(28)
        return object : Drawable() {
            private val strokeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.STROKE
                strokeWidth = 1.85f * d
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            private val fillP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(38, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor))
                style = Paint.Style.FILL
            }
            private val dotP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accentColor
                style = Paint.Style.FILL
            }
            private val path = Path()

            override fun getIntrinsicWidth(): Int = sizePx
            override fun getIntrinsicHeight(): Int = sizePx

            override fun draw(canvas: Canvas) {
                val cx = bounds.exactCenterX()
                val cy = bounds.exactCenterY()
                val r = min(bounds.width(), bounds.height()) * 0.44f
                canvas.drawCircle(cx, cy, r, fillP)
                val ir = r * 0.56f

                when (pack) {
                    // 5 סמלי החבילות החסכוניות
                    MotionPack.LITE_FADE -> {
                        // עלה קל / נוצה
                        path.reset()
                        path.moveTo(cx - ir * 0.65f, cy + ir * 0.65f)
                        path.quadTo(cx - ir * 0.55f, cy - ir * 0.65f, cx + ir * 0.72f, cy - ir * 0.72f)
                        path.quadTo(cx + ir * 0.60f, cy + ir * 0.55f, cx - ir * 0.65f, cy + ir * 0.65f)
                        canvas.drawPath(path, strokeP)
                        canvas.drawLine(cx - ir * 0.65f, cy + ir * 0.65f, cx + ir * 0.25f, cy - ir * 0.25f, strokeP)
                    }
                    MotionPack.LITE_SLIDE -> {
                        canvas.drawLine(cx - ir * 0.75f, cy, cx + ir * 0.72f, cy, strokeP)
                        canvas.drawLine(cx + ir * 0.25f, cy - ir * 0.42f, cx + ir * 0.72f, cy, strokeP)
                        canvas.drawLine(cx + ir * 0.25f, cy + ir * 0.42f, cx + ir * 0.72f, cy, strokeP)
                    }
                    MotionPack.LITE_TAP -> {
                        canvas.drawCircle(cx, cy, ir * 0.74f, strokeP)
                        canvas.drawCircle(cx, cy, ir * 0.28f, dotP)
                    }
                    MotionPack.LITE_SNAP -> {
                        // ברק חיסכון סוללה
                        path.reset()
                        path.moveTo(cx + ir * 0.14f, cy - ir * 0.86f)
                        path.lineTo(cx - ir * 0.44f, cy + ir * 0.08f)
                        path.lineTo(cx + ir * 0.08f, cy + ir * 0.08f)
                        path.lineTo(cx - ir * 0.14f, cy + ir * 0.86f)
                        canvas.drawPath(path, strokeP)
                    }
                    MotionPack.LITE_SOFT_LIFT -> {
                        canvas.drawLine(cx, cy + ir * 0.72f, cx, cy - ir * 0.72f, strokeP)
                        canvas.drawLine(cx - ir * 0.42f, cy - ir * 0.24f, cx, cy - ir * 0.72f, strokeP)
                        canvas.drawLine(cx + ir * 0.42f, cy - ir * 0.24f, cx, cy - ir * 0.72f, strokeP)
                    }

                    // 20 הסמלים הקיימים
                    MotionPack.ZEN_SILK -> {
                        path.reset()
                        path.moveTo(cx - ir, cy + ir * 0.25f)
                        path.cubicTo(cx - ir * 0.3f, cy - ir * 0.7f, cx + ir * 0.3f, cy + ir * 0.7f, cx + ir, cy - ir * 0.25f)
                        canvas.drawPath(path, strokeP)
                    }
                    MotionPack.LIQUID_SPRING -> {
                        path.reset()
                        path.moveTo(cx - ir * 0.75f, cy + ir * 0.6f)
                        path.lineTo(cx + ir * 0.65f, cy + ir * 0.2f)
                        path.lineTo(cx - ir * 0.65f, cy - ir * 0.2f)
                        path.lineTo(cx + ir * 0.75f, cy - ir * 0.6f)
                        canvas.drawPath(path, strokeP)
                    }
                    MotionPack.AURORA_COSMIC -> {
                        canvas.drawCircle(cx, cy, ir * 0.34f, strokeP)
                        canvas.drawArc(RectF(cx - ir, cy - ir * 0.45f, cx + ir, cy + ir * 0.45f), 0f, 300f, false, strokeP)
                    }
                    MotionPack.CYBER_GLITCH -> {
                        canvas.drawLine(cx - ir, cy - ir * 0.45f, cx + ir * 0.45f, cy - ir * 0.45f, strokeP)
                        canvas.drawLine(cx - ir * 0.45f, cy, cx + ir, cy, strokeP)
                        canvas.drawLine(cx - ir * 0.85f, cy + ir * 0.45f, cx + ir * 0.55f, cy + ir * 0.45f, strokeP)
                    }
                    MotionPack.ORIGAMI_3D -> {
                        path.reset()
                        path.moveTo(cx, cy - ir); path.lineTo(cx + ir * 0.9f, cy + ir * 0.45f); path.lineTo(cx - ir * 0.9f, cy + ir * 0.45f); path.close()
                        canvas.drawPath(path, strokeP)
                        canvas.drawLine(cx, cy - ir, cx, cy + ir * 0.45f, strokeP)
                    }
                    MotionPack.JELLY_BOUNCE -> {
                        canvas.drawRoundRect(RectF(cx - ir * 0.90f, cy - ir * 0.56f, cx + ir * 0.90f, cy + ir * 0.62f), ir * 0.45f, ir * 0.45f, strokeP)
                    }
                    MotionPack.MAGNETIC_FLOAT -> {
                        canvas.drawArc(RectF(cx - ir * 0.7f, cy - ir * 0.7f, cx + ir * 0.7f, cy + ir * 0.7f), 180f, 180f, false, strokeP)
                        canvas.drawLine(cx - ir * 0.7f, cy, cx - ir * 0.7f, cy + ir * 0.65f, strokeP)
                        canvas.drawLine(cx + ir * 0.7f, cy, cx + ir * 0.7f, cy + ir * 0.65f, strokeP)
                    }
                    MotionPack.MECHANICAL_CLICK -> {
                        canvas.drawRect(cx - ir * 0.78f, cy - ir * 0.72f, cx + ir * 0.78f, cy + ir * 0.72f, strokeP)
                        canvas.drawLine(cx - ir * 0.34f, cy, cx + ir * 0.34f, cy, strokeP)
                    }
                    MotionPack.WAVE_CASCADE -> {
                        canvas.drawCircle(cx, cy, ir * 0.32f, strokeP)
                        canvas.drawCircle(cx, cy, ir * 0.86f, strokeP)
                    }
                    MotionPack.CINEMA_ZOOM -> {
                        val c = ir * 0.82f; val leg = ir * 0.38f
                        canvas.drawLine(cx - c, cy - c, cx - c + leg, cy - c, strokeP); canvas.drawLine(cx - c, cy - c, cx - c, cy - c + leg, strokeP)
                        canvas.drawLine(cx + c, cy - c, cx + c - leg, cy - c, strokeP); canvas.drawLine(cx + c, cy - c, cx + c, cy - c + leg, strokeP)
                        canvas.drawLine(cx - c, cy + c, cx - c + leg, cy + c, strokeP); canvas.drawLine(cx - c, cy + c, cx - c, cy + c - leg, strokeP)
                        canvas.drawLine(cx + c, cy + c, cx + c - leg, cy + c, strokeP); canvas.drawLine(cx + c, cy + c, cx + c, cy + c - leg, strokeP)
                    }
                    MotionPack.GRAVITY_DROP -> {
                        canvas.drawCircle(cx, cy - ir * 0.36f, ir * 0.36f, strokeP)
                        canvas.drawLine(cx - ir * 0.82f, cy + ir * 0.65f, cx + ir * 0.82f, cy + ir * 0.65f, strokeP)
                        canvas.drawLine(cx, cy + ir * 0.05f, cx, cy + ir * 0.45f, strokeP)
                    }
                    MotionPack.TORNADO_SPIN -> {
                        canvas.drawArc(RectF(cx - ir * 0.85f, cy - ir * 0.85f, cx + ir * 0.85f, cy + ir * 0.85f), -40f, 250f, false, strokeP)
                        canvas.drawArc(RectF(cx - ir * 0.45f, cy - ir * 0.45f, cx + ir * 0.45f, cy + ir * 0.45f), 140f, 210f, false, strokeP)
                    }
                    MotionPack.PENDULUM_SWING -> {
                        canvas.drawLine(cx - ir * 0.65f, cy - ir * 0.78f, cx + ir * 0.65f, cy - ir * 0.78f, strokeP)
                        canvas.drawLine(cx, cy - ir * 0.78f, cx + ir * 0.45f, cy + ir * 0.35f, strokeP)
                        canvas.drawCircle(cx + ir * 0.48f, cy + ir * 0.45f, ir * 0.28f, dotP)
                    }
                    MotionPack.COIN_FLIP_3D -> {
                        canvas.drawOval(RectF(cx - ir * 0.46f, cy - ir * 0.85f, cx + ir * 0.46f, cy + ir * 0.85f), strokeP)
                        canvas.drawLine(cx, cy - ir * 0.42f, cx, cy + ir * 0.42f, strokeP)
                    }
                    MotionPack.RADIAL_BURST -> {
                        for (i in 0 until 8) {
                            val a = Math.toRadians(i * 45.0)
                            canvas.drawLine(
                                cx + (cos(a) * ir * 0.34f).toFloat(), cy + (sin(a) * ir * 0.34f).toFloat(),
                                cx + (cos(a) * ir * 0.92f).toFloat(), cy + (sin(a) * ir * 0.92f).toFloat(),
                                strokeP
                            )
                        }
                    }
                    MotionPack.CARD_SHUFFLE -> {
                        canvas.drawRoundRect(RectF(cx - ir * 0.78f, cy - ir * 0.68f, cx + ir * 0.16f, cy + ir * 0.56f), 3f * d, 3f * d, strokeP)
                        canvas.drawRoundRect(RectF(cx - ir * 0.16f, cy - ir * 0.48f, cx + ir * 0.78f, cy + ir * 0.74f), 3f * d, 3f * d, strokeP)
                    }
                    MotionPack.DOMINO_FALL -> {
                        canvas.drawLine(cx - ir * 0.65f, cy - ir * 0.7f, cx - ir * 0.65f, cy + ir * 0.7f, strokeP)
                        canvas.drawLine(cx - ir * 0.10f, cy - ir * 0.6f, cx + ir * 0.10f, cy + ir * 0.7f, strokeP)
                        canvas.drawLine(cx + ir * 0.35f, cy - ir * 0.45f, cx + ir * 0.80f, cy + ir * 0.7f, strokeP)
                    }
                    MotionPack.SLINGSHOT_PULL -> {
                        path.reset()
                        path.moveTo(cx - ir * 0.78f, cy - ir * 0.65f)
                        path.lineTo(cx, cy + ir * 0.10f)
                        path.lineTo(cx + ir * 0.78f, cy - ir * 0.65f)
                        canvas.drawPath(path, strokeP)
                        canvas.drawLine(cx, cy + ir * 0.10f, cx, cy + ir * 0.82f, strokeP)
                    }
                    MotionPack.HEARTBEAT_PULSE -> {
                        path.reset()
                        path.moveTo(cx - ir, cy + ir * 0.08f)
                        path.lineTo(cx - ir * 0.45f, cy + ir * 0.08f)
                        path.lineTo(cx - ir * 0.18f, cy - ir * 0.68f)
                        path.lineTo(cx + ir * 0.16f, cy + ir * 0.68f)
                        path.lineTo(cx + ir * 0.44f, cy + ir * 0.08f)
                        path.lineTo(cx + ir, cy + ir * 0.08f)
                        canvas.drawPath(path, strokeP)
                    }
                    MotionPack.PORTAL_WARP -> {
                        canvas.drawOval(RectF(cx - ir * 0.42f, cy - ir * 0.90f, cx + ir * 0.42f, cy + ir * 0.90f), strokeP)
                        canvas.drawOval(RectF(cx - ir * 0.82f, cy - ir * 0.42f, cx + ir * 0.82f, cy + ir * 0.42f), strokeP)
                    }
                }
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; fillP.alpha = alpha; dotP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; fillP.colorFilter = colorFilter; dotP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }
}

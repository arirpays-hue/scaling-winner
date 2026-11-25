package com.example.calculator

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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// =========================================================================
// מאגר 10 חבילות אנימציות ממשק ייעודיות וייחודיות (ProUiMotionPacks.kt)
// נפתח בלחיצה ארוכה על כפתור "אנימציות ממשק" בהגדרות
// =========================================================================
object ProUiMotionPacks {

    private const val PREF_KEY_PACK_ID = "pro_ui_motion_pack_id_v1"

    enum class MotionPack(
        val id: Int,
        val displayName: String,
        val shortName: String,
        val description: String,
        val accentHex: String
    ) {
        ZEN_SILK(0, "משי זן", "משי זן", "ריחוף רגוע, האטה חלקה ומעברים עדינים ללא קפיצות", "#66BB6A"),
        LIQUID_SPRING(1, "קפיץ זכוכית", "קפיץ זכוכית", "פיזיקת קפיץ אלסטית, הטיית 3D במקשים ופרלקסה בתפריט", "#38BDF8"),
        AURORA_COSMIC(2, "זוהר קוסמי", "זוהר קוסמי", "קיפול 3D מדורג למקלדת, כניסה אלכסונית וזום חי בתוצאות", "#00F5D4"),
        CYBER_GLITCH(3, "סייבר גליץ׳", "גליץ׳", "רטט חשמלי חד, הבהוב סייבר וכניסה צדדית מהירה", "#F43F5E"),
        ORIGAMI_3D(4, "קיפול אוריגמי", "אוריגמי 3D", "כרטיסים ומקשים נפתחים בקיפול תלת-ממדי כמו דף נייר", "#A78BFA"),
        JELLY_BOUNCE(5, "ג׳לי אלסטי", "ג׳לי", "מעיכה ומתיחה קפיצית (Squash & Stretch) בכל הקשה", "#FB923C"),
        MAGNETIC_FLOAT(6, "ריחוף מגנטי", "מגנטי", "שדות וכרטיסים נמשכים ברכות למעלה ומרחפים בפוקוס", "#60A5FA"),
        MECHANICAL_CLICK(7, "מכני טקטי", "מכני", "קליק מהיר וחד כמו מקלדת מכנית — אפס השהיה", "#94A3B8"),
        WAVE_CASCADE(8, "גל אדוות", "גל אדוות", "גל אלכסוני מתגלגל על המקשים והכרטיסים ומחיקת גל", "#2DD4BF"),
        CINEMA_ZOOM(9, "קולנוע רחב", "קולנוע", "זום עומק קולנועי רחב ופתיחת וילון מלכותית", "#FBBF24");

        companion object {
            fun fromId(id: Int): MotionPack = entries.firstOrNull { it.id == id } ?: LIQUID_SPRING
        }
    }

    var currentPack: MotionPack = MotionPack.LIQUID_SPRING
        private set

    fun loadPrefs(prefs: SharedPreferences?) {
        if (prefs == null) return
        val saved = prefs.getInt(PREF_KEY_PACK_ID, MotionPack.LIQUID_SPRING.id)
        currentPack = MotionPack.fromId(saved)
    }

    fun selectPack(activity: MainActivity, pack: MotionPack, autoEnableIfOff: Boolean = true) {
        currentPack = pack
        activity.prefs?.edit()?.putInt(PREF_KEY_PACK_ID, pack.id)?.apply()
        if (autoEnableIfOff && !ProUiMotionEngine.isUiAnimationsEnabled) {
            ProUiMotionEngine.setEnabled(activity, true)
        }
    }

    private fun resetViewTransforms(v: View) {
        v.animate().cancel()
        v.alpha = 1f
        v.scaleX = 1f
        v.scaleY = 1f
        v.translationX = 0f
        v.translationY = 0f
        v.rotation = 0f
        v.rotationX = 0f
        v.rotationY = 0f
    }

    // =========================================================================
    // 1. אנימציית כותרת עליונה בעת מעבר לשונית
    // =========================================================================
    fun animateHeaderTitle(activity: MainActivity, titleView: TextView?) {
        if (titleView == null) return
        val d = activity.resources.displayMetrics.density
        val dir = if (activity.isRightHanded) 1f else -1f
        resetViewTransforms(titleView)

        when (currentPack) {
            MotionPack.ZEN_SILK -> {
                titleView.alpha = 0f
                titleView.translationY = 6f * d
                titleView.animate().alpha(1f).translationY(0f).setDuration(270L)
                    .setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            MotionPack.LIQUID_SPRING -> {
                titleView.alpha = 0f
                titleView.scaleX = 0.88f; titleView.scaleY = 0.88f
                titleView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300L)
                    .setInterpolator(OvershootInterpolator(1.5f)).start()
            }
            MotionPack.AURORA_COSMIC -> {
                titleView.alpha = 0f
                titleView.translationX = 18f * d * dir
                titleView.translationY = -5f * d
                titleView.animate().alpha(1f).translationX(0f).translationY(0f).setDuration(320L)
                    .setInterpolator(OvershootInterpolator(1.1f)).start()
            }
            MotionPack.CYBER_GLITCH -> {
                val tx = ObjectAnimator.ofFloat(titleView, "translationX", 14f * d * dir, -6f * d * dir, 4f * d * dir, 0f)
                val al = ObjectAnimator.ofFloat(titleView, "alpha", 0.2f, 1f, 0.45f, 1f)
                AnimatorSet().apply { playTogether(tx, al); duration = 220L; start() }
            }
            MotionPack.ORIGAMI_3D -> {
                titleView.cameraDistance = 6000f * d
                titleView.alpha = 0f
                titleView.rotationX = -65f
                titleView.animate().alpha(1f).rotationX(0f).setDuration(330L)
                    .setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            MotionPack.JELLY_BOUNCE -> {
                titleView.alpha = 0f
                titleView.scaleX = 1.24f; titleView.scaleY = 0.72f
                titleView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(360L)
                    .setInterpolator(OvershootInterpolator(2.1f)).start()
            }
            MotionPack.MAGNETIC_FLOAT -> {
                titleView.alpha = 0f
                titleView.translationY = 14f * d
                titleView.animate().alpha(1f).translationY(0f).setDuration(310L)
                    .setInterpolator(AnticipateOvershootInterpolator(0.9f)).start()
            }
            MotionPack.MECHANICAL_CLICK -> {
                titleView.alpha = 0f
                titleView.translationX = 12f * d * dir
                titleView.animate().alpha(1f).translationX(0f).setDuration(150L)
                    .setInterpolator(DecelerateInterpolator(2.2f)).start()
            }
            MotionPack.WAVE_CASCADE -> {
                titleView.alpha = 0f
                titleView.translationX = 24f * d * dir
                titleView.rotation = -3f * dir
                titleView.animate().alpha(1f).translationX(0f).rotation(0f).setDuration(300L)
                    .setInterpolator(DecelerateInterpolator(1.7f)).start()
            }
            MotionPack.CINEMA_ZOOM -> {
                titleView.alpha = 0f
                titleView.scaleX = 1.16f; titleView.scaleY = 1.16f
                titleView.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(360L)
                    .setInterpolator(DecelerateInterpolator(1.8f)).start()
            }
        }
    }

    // =========================================================================
    // 2. כניסת מסך מחשבון (צג + מקלדת) לפי החבילה הנבחרת
    // =========================================================================
    fun animateCalculatorEntrance(activity: MainActivity, root: View) {
        resetViewTransforms(root)
        val d = activity.resources.displayMetrics.density
        val vg = root as? ViewGroup
        val displayCard = vg?.getChildAt(0)

        if (displayCard != null) {
            resetViewTransforms(displayCard)
            displayCard.alpha = 0f
            when (currentPack) {
                MotionPack.ZEN_SILK -> {
                    displayCard.translationY = -10f * d
                    displayCard.animate().alpha(1f).translationY(0f).setDuration(290L)
                        .setInterpolator(DecelerateInterpolator(1.5f)).start()
                }
                MotionPack.LIQUID_SPRING -> {
                    displayCard.scaleX = 0.90f; displayCard.scaleY = 0.90f
                    displayCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(330L)
                        .setInterpolator(OvershootInterpolator(1.3f)).start()
                }
                MotionPack.AURORA_COSMIC -> {
                    displayCard.translationY = -16f * d; displayCard.scaleX = 0.93f; displayCard.scaleY = 0.93f
                    displayCard.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(340L)
                        .setInterpolator(OvershootInterpolator(1.25f)).start()
                }
                MotionPack.CYBER_GLITCH -> {
                    displayCard.alpha = 1f
                    val tx = ObjectAnimator.ofFloat(displayCard, "translationX", -18f * d, 9f * d, -4f * d, 0f)
                    val al = ObjectAnimator.ofFloat(displayCard, "alpha", 0.1f, 1f, 0.4f, 1f)
                    AnimatorSet().apply { playTogether(tx, al); duration = 230L; start() }
                }
                MotionPack.ORIGAMI_3D -> {
                    displayCard.cameraDistance = 7000f * d
                    displayCard.rotationX = -38f
                    displayCard.animate().alpha(1f).rotationX(0f).setDuration(340L)
                        .setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                MotionPack.JELLY_BOUNCE -> {
                    displayCard.scaleX = 1.10f; displayCard.scaleY = 0.78f
                    displayCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(370L)
                        .setInterpolator(OvershootInterpolator(2.0f)).start()
                }
                MotionPack.MAGNETIC_FLOAT -> {
                    displayCard.translationY = 18f * d
                    displayCard.animate().alpha(1f).translationY(0f).setDuration(320L)
                        .setInterpolator(OvershootInterpolator(1.0f)).start()
                }
                MotionPack.MECHANICAL_CLICK -> {
                    displayCard.translationY = -8f * d
                    displayCard.animate().alpha(1f).translationY(0f).setDuration(150L)
                        .setInterpolator(DecelerateInterpolator(2.2f)).start()
                }
                MotionPack.WAVE_CASCADE -> {
                    displayCard.translationX = -22f * d
                    displayCard.animate().alpha(1f).translationX(0f).setDuration(280L)
                        .setInterpolator(DecelerateInterpolator(1.7f)).start()
                }
                MotionPack.CINEMA_ZOOM -> {
                    displayCard.scaleX = 1.09f; displayCard.scaleY = 1.09f
                    displayCard.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(380L)
                        .setInterpolator(DecelerateInterpolator(1.8f)).start()
                }
            }
        }

        val buttons = activity.keypadButtons.values.toList()
        val cols = if (buttons.size > 26) 5 else 4
        for ((idx, btn) in buttons.withIndex()) {
            resetViewTransforms(btn)
            btn.alpha = 0f
            val row = idx / cols
            val col = idx % cols

            when (currentPack) {
                MotionPack.ZEN_SILK -> {
                    val delay = (row * 26L).coerceAtMost(180L)
                    btn.translationY = 8f * d
                    btn.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(240L)
                        .setInterpolator(DecelerateInterpolator(1.5f)).start()
                }
                MotionPack.LIQUID_SPRING -> {
                    val delay = (idx * 11L).coerceAtMost(230L)
                    btn.scaleX = 0.78f; btn.scaleY = 0.78f; btn.translationY = 12f * d
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                        .setStartDelay(delay).setDuration(290L).setInterpolator(OvershootInterpolator(1.45f)).start()
                }
                MotionPack.AURORA_COSMIC -> {
                    val delay = (idx * 12L).coerceAtMost(240L)
                    btn.cameraDistance = 6000f * d
                    btn.rotationX = -22f; btn.scaleX = 0.84f; btn.scaleY = 0.84f; btn.translationY = 14f * d
                    btn.animate().alpha(1f).rotationX(0f).scaleX(1f).scaleY(1f).translationY(0f)
                        .setStartDelay(delay).setDuration(310L).setInterpolator(OvershootInterpolator(1.35f)).start()
                }
                MotionPack.CYBER_GLITCH -> {
                    val delay = ((row * 22L) + (col * 10L)).coerceAtMost(180L)
                    btn.translationX = (if (idx % 2 == 0) 14f else -14f) * d
                    btn.animate().alpha(1f).translationX(0f)
                        .setStartDelay(delay).setDuration(150L).setInterpolator(DecelerateInterpolator(2.0f)).start()
                }
                MotionPack.ORIGAMI_3D -> {
                    val delay = (row * 34L + col * 14L).coerceAtMost(240L)
                    btn.cameraDistance = 5500f * d
                    btn.rotationY = -58f
                    btn.animate().alpha(1f).rotationY(0f)
                        .setStartDelay(delay).setDuration(310L).setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                MotionPack.JELLY_BOUNCE -> {
                    val delay = (idx * 10L).coerceAtMost(210L)
                    btn.scaleX = 1.26f; btn.scaleY = 0.65f
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(340L).setInterpolator(OvershootInterpolator(2.1f)).start()
                }
                MotionPack.MAGNETIC_FLOAT -> {
                    val delay = ((5 - row.coerceAtMost(5)) * 22L + col * 8L).coerceAtMost(200L)
                    btn.translationY = 22f * d; btn.scaleX = 0.90f; btn.scaleY = 0.90f
                    btn.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(300L).setInterpolator(OvershootInterpolator(1.1f)).start()
                }
                MotionPack.MECHANICAL_CLICK -> {
                    val delay = (row * 16L).coerceAtMost(110L)
                    btn.scaleX = 0.92f; btn.scaleY = 0.92f
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(130L).setInterpolator(DecelerateInterpolator(2.2f)).start()
                }
                MotionPack.WAVE_CASCADE -> {
                    // גל אלכסוני לפי סכום שורה + עמודה
                    val waveStep = row + col
                    val delay = (waveStep * 28L).coerceAtMost(250L)
                    btn.translationY = 16f * d; btn.scaleX = 0.85f; btn.scaleY = 0.85f
                    btn.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(280L).setInterpolator(OvershootInterpolator(1.3f)).start()
                }
                MotionPack.CINEMA_ZOOM -> {
                    val delay = (row * 26L).coerceAtMost(170L)
                    btn.scaleX = 0.84f; btn.scaleY = 0.84f
                    btn.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setStartDelay(delay).setDuration(350L).setInterpolator(DecelerateInterpolator(1.8f)).start()
                }
            }
        }
    }

    // =========================================================================
    // 3. כניסת כרטיסים במסכי כלים והגדרות
    // =========================================================================
    fun animateToolCardsCascade(activity: MainActivity, root: View) {
        val container: ViewGroup? = when (root) {
            is ScrollView -> root.getChildAt(0) as? ViewGroup
            is ViewGroup -> root
            else -> null
        }
        if (container == null) {
            root.animate().cancel(); root.alpha = 0f
            root.animate().alpha(1f).setDuration(220L).start()
            return
        }

        val d = activity.resources.displayMetrics.density
        val count = min(container.childCount, 12)
        for (i in 0 until count) {
            val child = container.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue
            resetViewTransforms(child)
            child.alpha = 0f

            when (currentPack) {
                MotionPack.ZEN_SILK -> {
                    child.translationY = 12f * d
                    child.animate().alpha(1f).translationY(0f).setStartDelay(i * 26L).setDuration(280L)
                        .setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                MotionPack.LIQUID_SPRING -> {
                    child.translationY = 22f * d; child.scaleX = 0.92f; child.scaleY = 0.92f
                    child.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(i * 28L)
                        .setDuration(330L).setInterpolator(OvershootInterpolator(1.15f)).start()
                }
                MotionPack.AURORA_COSMIC -> {
                    child.translationX = (if (activity.isRightHanded) 18f else -18f) * d
                    child.translationY = 18f * d; child.scaleX = 0.94f; child.scaleY = 0.94f
                    child.animate().alpha(1f).translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                        .setStartDelay(i * 30L).setDuration(330L).setInterpolator(OvershootInterpolator(1.08f)).start()
                }
                MotionPack.CYBER_GLITCH -> {
                    child.translationX = (if (i % 2 == 0) 28f else -28f) * d
                    child.animate().alpha(1f).translationX(0f).setStartDelay(i * 20L).setDuration(190L)
                        .setInterpolator(DecelerateInterpolator(2.2f)).start()
                }
                MotionPack.ORIGAMI_3D -> {
                    child.cameraDistance = 7000f * d
                    child.rotationX = -32f; child.translationY = 16f * d
                    child.animate().alpha(1f).rotationX(0f).translationY(0f).setStartDelay(i * 34L)
                        .setDuration(340L).setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                MotionPack.JELLY_BOUNCE -> {
                    child.scaleX = 1.12f; child.scaleY = 0.78f; child.translationY = 16f * d
                    child.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setStartDelay(i * 28L)
                        .setDuration(360L).setInterpolator(OvershootInterpolator(1.85f)).start()
                }
                MotionPack.MAGNETIC_FLOAT -> {
                    child.translationY = 30f * d; child.scaleX = 0.95f; child.scaleY = 0.95f
                    child.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(i * 30L)
                        .setDuration(340L).setInterpolator(AnticipateOvershootInterpolator(0.85f)).start()
                }
                MotionPack.MECHANICAL_CLICK -> {
                    child.translationY = 8f * d
                    child.animate().alpha(1f).translationY(0f).setStartDelay(i * 14L).setDuration(155L)
                        .setInterpolator(DecelerateInterpolator(2.2f)).start()
                }
                MotionPack.WAVE_CASCADE -> {
                    val dir = if (activity.isRightHanded) 1f else -1f
                    child.translationX = 24f * d * dir; child.translationY = 14f * d
                    child.animate().alpha(1f).translationX(0f).translationY(0f).setStartDelay(i * 32L)
                        .setDuration(310L).setInterpolator(OvershootInterpolator(1.0f)).start()
                }
                MotionPack.CINEMA_ZOOM -> {
                    child.scaleX = 0.88f; child.scaleY = 0.88f
                    child.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(i * 30L)
                        .setDuration(380L).setInterpolator(DecelerateInterpolator(1.8f)).start()
                }
            }
        }
    }

    // =========================================================================
    // 4. פיזיקת לחיצה ושחרור מקשים (Touch Down / Touch Up)
    // =========================================================================
    fun animateKeypadTouchDown(activity: MainActivity, v: View, eventX: Float, eventY: Float, symbol: String) {
        val d = activity.resources.displayMetrics.density
        v.animate().cancel()
        val nx = if (v.width > 0) ((eventX / v.width) - 0.5f).coerceIn(-0.5f, 0.5f) else 0f
        val ny = if (v.height > 0) ((eventY / v.height) - 0.5f).coerceIn(-0.5f, 0.5f) else 0f

        when (currentPack) {
            MotionPack.ZEN_SILK -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).translationY(1.2f * d).setDuration(80L)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.LIQUID_SPRING -> {
                v.cameraDistance = 5500f * d
                v.animate().scaleX(0.90f).scaleY(0.90f).rotationY(nx * 15f).rotationX(-ny * 15f)
                    .setDuration(70L).setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.AURORA_COSMIC -> {
                v.cameraDistance = 5500f * d
                val s = if (symbol in listOf("=", "AC", "+", "−", "×", "÷")) 0.86f else 0.90f
                v.animate().scaleX(s).scaleY(s).rotationY(nx * 18f).translationY(2f * d)
                    .setDuration(65L).setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.CYBER_GLITCH -> {
                v.animate().scaleX(0.93f).scaleY(0.93f).translationX((if (nx >= 0f) 2.5f else -2.5f) * d)
                    .setDuration(45L).start()
            }
            MotionPack.ORIGAMI_3D -> {
                v.cameraDistance = 5000f * d
                v.animate().rotationX(-18f).scaleX(0.94f).scaleY(0.92f).setDuration(75L).start()
            }
            MotionPack.JELLY_BOUNCE -> {
                // מעיכה לרוחב וכיווץ לגובה
                v.animate().scaleX(1.10f).scaleY(0.82f).translationY(2f * d).setDuration(65L)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.MAGNETIC_FLOAT -> {
                v.animate().scaleX(0.92f).scaleY(0.92f).translationY(-2.5f * d).setDuration(75L)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.MECHANICAL_CLICK -> {
                v.animate().scaleX(0.93f).scaleY(0.93f).translationY(3.2f * d).setDuration(40L).start()
            }
            MotionPack.WAVE_CASCADE -> {
                v.animate().scaleX(0.89f).scaleY(0.89f).rotation(nx * 6f).setDuration(70L).start()
            }
            MotionPack.CINEMA_ZOOM -> {
                v.animate().scaleX(0.92f).scaleY(0.92f).alpha(0.86f).setDuration(90L).start()
            }
        }
    }

    fun animateKeypadTouchUp(activity: MainActivity, v: View, symbol: String) {
        v.animate().cancel()
        when (currentPack) {
            MotionPack.ZEN_SILK -> {
                v.animate().scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f).rotationY(0f)
                    .setDuration(220L).setInterpolator(DecelerateInterpolator(1.5f)).start()
            }
            MotionPack.LIQUID_SPRING -> {
                v.animate().scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f).rotationY(0f)
                    .setDuration(330L).setInterpolator(OvershootInterpolator(2.1f)).start()
            }
            MotionPack.AURORA_COSMIC -> {
                val ov = if (symbol == "=") 2.5f else 1.85f
                v.animate().scaleX(1f).scaleY(1f).translationY(0f).rotationX(0f).rotationY(0f)
                    .setDuration(310L).setInterpolator(OvershootInterpolator(ov)).start()
            }
            MotionPack.CYBER_GLITCH -> {
                v.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f)
                    .setDuration(120L).setInterpolator(OvershootInterpolator(1.5f)).start()
            }
            MotionPack.ORIGAMI_3D -> {
                v.animate().scaleX(1f).scaleY(1f).rotationX(0f).rotationY(0f)
                    .setDuration(260L).setInterpolator(OvershootInterpolator(1.4f)).start()
            }
            MotionPack.JELLY_BOUNCE -> {
                val sx = ObjectAnimator.ofFloat(v, "scaleX", v.scaleX, 0.90f, 1.04f, 1f)
                val sy = ObjectAnimator.ofFloat(v, "scaleY", v.scaleY, 1.12f, 0.96f, 1f)
                val ty = ObjectAnimator.ofFloat(v, "translationY", v.translationY, 0f)
                AnimatorSet().apply { playTogether(sx, sy, ty); duration = 340L; start() }
            }
            MotionPack.MAGNETIC_FLOAT -> {
                v.animate().scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(270L).setInterpolator(OvershootInterpolator(1.6f)).start()
            }
            MotionPack.MECHANICAL_CLICK -> {
                v.animate().scaleX(1f).scaleY(1f).translationY(0f)
                    .setDuration(110L).setInterpolator(DecelerateInterpolator(2.0f)).start()
            }
            MotionPack.WAVE_CASCADE -> {
                v.animate().scaleX(1f).scaleY(1f).rotation(0f)
                    .setDuration(260L).setInterpolator(OvershootInterpolator(1.7f)).start()
            }
            MotionPack.CINEMA_ZOOM -> {
                v.animate().scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(250L).setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
        }
    }

    // =========================================================================
    // 5. אנימציות צג תוצאה (הקלדה, '=', שגיאה וניקוי AC)
    // =========================================================================
    fun animateDisplayChange(activity: MainActivity, displayView: TextView?, isEquals: Boolean, isError: Boolean) {
        if (displayView == null) return
        val d = activity.resources.displayMetrics.density
        resetViewTransforms(displayView)

        if (isError) {
            ObjectAnimator.ofFloat(
                displayView, "translationX",
                0f, 12f * d, -12f * d, 8f * d, -8f * d, 4f * d, 0f
            ).apply { duration = 360L; start() }
            return
        }

        if (isEquals) {
            displayView.pivotX = displayView.width.toFloat().coerceAtLeast(1f)
            displayView.pivotY = displayView.height * 0.5f
            when (currentPack) {
                MotionPack.ZEN_SILK -> {
                    displayView.alpha = 0.4f; displayView.translationY = 10f * d
                    displayView.animate().alpha(1f).translationY(0f).setDuration(270L)
                        .setInterpolator(DecelerateInterpolator(1.6f)).start()
                }
                MotionPack.LIQUID_SPRING -> {
                    displayView.scaleX = 0.82f; displayView.scaleY = 0.82f; displayView.alpha = 0.5f
                    displayView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(350L)
                        .setInterpolator(OvershootInterpolator(1.9f)).start()
                }
                MotionPack.AURORA_COSMIC -> {
                    displayView.scaleX = 1.15f; displayView.scaleY = 1.15f; displayView.translationY = -8f * d
                    displayView.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(360L)
                        .setInterpolator(OvershootInterpolator(1.6f)).start()
                }
                MotionPack.CYBER_GLITCH -> {
                    val tx = ObjectAnimator.ofFloat(displayView, "translationX", -10f * d, 8f * d, -4f * d, 0f)
                    val al = ObjectAnimator.ofFloat(displayView, "alpha", 0.25f, 1f, 0.5f, 1f)
                    AnimatorSet().apply { playTogether(tx, al); duration = 220L; start() }
                }
                MotionPack.ORIGAMI_3D -> {
                    displayView.cameraDistance = 6000f * d
                    displayView.rotationX = -75f; displayView.alpha = 0.3f
                    displayView.animate().rotationX(0f).alpha(1f).setDuration(340L)
                        .setInterpolator(OvershootInterpolator(1.1f)).start()
                }
                MotionPack.JELLY_BOUNCE -> {
                    val sx = ObjectAnimator.ofFloat(displayView, "scaleX", 0.84f, 1.12f, 0.96f, 1f)
                    val sy = ObjectAnimator.ofFloat(displayView, "scaleY", 1.18f, 0.86f, 1.04f, 1f)
                    AnimatorSet().apply { playTogether(sx, sy); duration = 380L; start() }
                }
                MotionPack.MAGNETIC_FLOAT -> {
                    displayView.translationY = 18f * d; displayView.alpha = 0.35f
                    displayView.animate().translationY(0f).alpha(1f).setDuration(320L)
                        .setInterpolator(OvershootInterpolator(1.3f)).start()
                }
                MotionPack.MECHANICAL_CLICK -> {
                    displayView.scaleX = 0.94f; displayView.scaleY = 0.94f
                    displayView.animate().scaleX(1f).scaleY(1f).setDuration(140L)
                        .setInterpolator(DecelerateInterpolator(2.0f)).start()
                }
                MotionPack.WAVE_CASCADE -> {
                    displayView.translationX = -22f * d; displayView.alpha = 0.4f
                    displayView.animate().translationX(0f).alpha(1f).setDuration(290L)
                        .setInterpolator(OvershootInterpolator(1.2f)).start()
                }
                MotionPack.CINEMA_ZOOM -> {
                    displayView.scaleX = 1.20f; displayView.scaleY = 1.20f; displayView.alpha = 0.35f
                    displayView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(390L)
                        .setInterpolator(DecelerateInterpolator(1.8f)).start()
                }
            }
            return
        }

        // הקלדת ספרה רגילה
        when (currentPack) {
            MotionPack.JELLY_BOUNCE -> {
                displayView.scaleX = 1.04f; displayView.scaleY = 0.94f
                displayView.animate().scaleX(1f).scaleY(1f).setDuration(170L)
                    .setInterpolator(OvershootInterpolator(1.4f)).start()
            }
            MotionPack.CYBER_GLITCH -> {
                displayView.translationX = 3f * d; displayView.alpha = 0.72f
                displayView.animate().translationX(0f).alpha(1f).setDuration(100L).start()
            }
            MotionPack.MECHANICAL_CLICK -> {
                displayView.translationY = 2.5f * d
                displayView.animate().translationY(0f).setDuration(90L).start()
            }
            else -> {
                displayView.alpha = 0.72f; displayView.translationY = 3f * d
                displayView.animate().alpha(1f).translationY(0f).setDuration(145L)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }

    fun animateDisplayClear(activity: MainActivity, displayView: TextView?, exprView: TextView?) {
        val d = activity.resources.displayMetrics.density
        displayView?.let { v ->
            resetViewTransforms(v)
            when (currentPack) {
                MotionPack.ORIGAMI_3D -> {
                    v.cameraDistance = 6000f * d; v.rotationX = 55f; v.alpha = 0.25f
                    v.animate().rotationX(0f).alpha(1f).setDuration(240L).start()
                }
                MotionPack.CINEMA_ZOOM -> {
                    v.scaleX = 0.85f; v.scaleY = 0.85f; v.alpha = 0.2f
                    v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260L).start()
                }
                else -> {
                    v.alpha = 0.25f; v.translationX = -16f * d
                    v.animate().alpha(1f).translationX(0f).setDuration(220L)
                        .setInterpolator(DecelerateInterpolator()).start()
                }
            }
        }
        exprView?.let { v ->
            v.animate().cancel(); v.alpha = 0.25f
            v.animate().alpha(1f).setDuration(190L).start()
        }
    }

    // =========================================================================
    // 6. תפריט צד + אקורדיון קטגוריות
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

        val openDur = when (currentPack) {
            MotionPack.MECHANICAL_CLICK, MotionPack.CYBER_GLITCH -> 210L
            MotionPack.CINEMA_ZOOM -> 390L
            MotionPack.LIQUID_SPRING, MotionPack.JELLY_BOUNCE, MotionPack.ORIGAMI_3D -> 350L
            else -> 300L
        }
        val closeDur = if (currentPack == MotionPack.MECHANICAL_CLICK) 170L else 240L

        if (isOpening) {
            dimView.visibility = View.VISIBLE
            dimView.animate().cancel()
            dimView.animate().alpha(1f).setDuration(openDur).start()

            drawerScroll.animate().cancel()
            drawerScroll.rotationY = 0f
            val interp = when (currentPack) {
                MotionPack.LIQUID_SPRING, MotionPack.JELLY_BOUNCE -> OvershootInterpolator(0.55f)
                MotionPack.AURORA_COSMIC, MotionPack.MAGNETIC_FLOAT -> OvershootInterpolator(0.40f)
                else -> DecelerateInterpolator(1.8f)
            }
            if (currentPack == MotionPack.ORIGAMI_3D) {
                drawerScroll.cameraDistance = 9000f * d
                drawerScroll.rotationY = 24f * dirSign
                drawerScroll.animate().translationX(0f).rotationY(0f).setDuration(openDur)
                    .setInterpolator(DecelerateInterpolator(1.6f)).start()
            } else {
                drawerScroll.animate().translationX(0f).setDuration(openDur).setInterpolator(interp).start()
            }

            if (mainContent != null) {
                mainContent.animate().cancel()
                when (currentPack) {
                    MotionPack.LIQUID_SPRING, MotionPack.AURORA_COSMIC, MotionPack.CINEMA_ZOOM -> {
                        mainContent.animate().scaleX(0.95f).scaleY(0.95f)
                            .translationX(-18f * d * dirSign).setDuration(openDur)
                            .setInterpolator(DecelerateInterpolator(1.6f)).start()
                    }
                    MotionPack.MAGNETIC_FLOAT, MotionPack.WAVE_CASCADE, MotionPack.ZEN_SILK -> {
                        mainContent.animate().translationX(-14f * d * dirSign).setDuration(openDur)
                            .setInterpolator(DecelerateInterpolator(1.5f)).start()
                    }
                    else -> {
                        mainContent.scaleX = 1f; mainContent.scaleY = 1f; mainContent.translationX = 0f
                    }
                }
            }
        } else {
            dimView.animate().cancel()
            dimView.animate().alpha(0f).setDuration(closeDur)
                .withEndAction { dimView.visibility = View.GONE }.start()

            drawerScroll.animate().cancel()
            drawerScroll.animate().translationX(closedX).rotationY(0f).setDuration(closeDur)
                .setInterpolator(AccelerateInterpolator(1.4f)).start()

            mainContent?.animate()?.cancel()
            mainContent?.animate()?.scaleX(1f)?.scaleY(1f)?.translationX(0f)
                ?.setDuration(closeDur)?.setInterpolator(DecelerateInterpolator(1.5f))?.start()
        }
    }

    fun animateSectionAccordionExpand(activity: MainActivity, container: View) {
        val d = activity.resources.displayMetrics.density
        resetViewTransforms(container)
        container.alpha = 0f
        when (currentPack) {
            MotionPack.ORIGAMI_3D -> {
                container.cameraDistance = 6000f * d
                container.rotationX = -26f
                container.animate().alpha(1f).rotationX(0f).setDuration(270L)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
            MotionPack.JELLY_BOUNCE, MotionPack.LIQUID_SPRING -> {
                container.scaleY = 0.88f; container.pivotY = 0f
                container.animate().alpha(1f).scaleY(1f).setDuration(280L)
                    .setInterpolator(OvershootInterpolator(1.2f)).start()
            }
            MotionPack.MECHANICAL_CLICK -> {
                container.translationY = -6f * d
                container.animate().alpha(1f).translationY(0f).setDuration(140L).start()
            }
            else -> {
                container.translationY = -10f * d
                container.animate().alpha(1f).translationY(0f).setDuration(230L)
                    .setInterpolator(DecelerateInterpolator(1.5f)).start()
            }
        }
    }

    // =========================================================================
    // 7. אייקוני כותרת, דיאלוגים, לחיצה טקטילית ופוקוס שדות קלט
    // =========================================================================
    fun animateHeaderIconTap(view: View, kind: String) {
        view.animate().cancel()
        when (kind) {
            "fav" -> {
                val rot = ObjectAnimator.ofFloat(view, "rotation", 0f, 360f)
                val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.34f, 1f)
                val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.34f, 1f)
                AnimatorSet().apply {
                    playTogether(rot, sx, sy)
                    duration = if (currentPack == MotionPack.MECHANICAL_CLICK) 240L else 420L
                    interpolator = OvershootInterpolator(1.5f)
                    start()
                }
            }
            "history" -> {
                ObjectAnimator.ofFloat(view, "rotation", 0f, -40f, 0f).apply {
                    duration = 260L; interpolator = AccelerateDecelerateInterpolator(); start()
                }
            }
            else -> {
                val sx = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.82f, 1.08f, 1f)
                val sy = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.82f, 1.08f, 1f)
                AnimatorSet().apply { playTogether(sx, sy); duration = 240L; start() }
            }
        }
    }

    fun animateDialogEntrance(activity: MainActivity, dialogRoot: View) {
        val d = activity.resources.displayMetrics.density
        resetViewTransforms(dialogRoot)
        dialogRoot.alpha = 0f
        when (currentPack) {
            MotionPack.ZEN_SILK -> {
                dialogRoot.translationY = 16f * d; dialogRoot.scaleX = 0.97f; dialogRoot.scaleY = 0.97f
                dialogRoot.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setDuration(270L)
                    .setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            MotionPack.LIQUID_SPRING -> {
                dialogRoot.scaleX = 0.82f; dialogRoot.scaleY = 0.82f
                dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(330L)
                    .setInterpolator(OvershootInterpolator(1.4f)).start()
            }
            MotionPack.AURORA_COSMIC -> {
                dialogRoot.scaleX = 0.86f; dialogRoot.scaleY = 0.86f; dialogRoot.translationY = 24f * d
                dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(340L)
                    .setInterpolator(OvershootInterpolator(1.25f)).start()
            }
            MotionPack.CYBER_GLITCH -> {
                dialogRoot.alpha = 1f
                val sx = ObjectAnimator.ofFloat(dialogRoot, "scaleX", 0.90f, 1.03f, 0.99f, 1f)
                val al = ObjectAnimator.ofFloat(dialogRoot, "alpha", 0.1f, 1f, 0.55f, 1f)
                AnimatorSet().apply { playTogether(sx, al); duration = 220L; start() }
            }
            MotionPack.ORIGAMI_3D -> {
                dialogRoot.cameraDistance = 8000f * d
                dialogRoot.rotationX = -32f; dialogRoot.scaleY = 0.86f
                dialogRoot.animate().alpha(1f).rotationX(0f).scaleY(1f).setDuration(340L)
                    .setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            MotionPack.JELLY_BOUNCE -> {
                dialogRoot.scaleX = 1.14f; dialogRoot.scaleY = 0.78f
                dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(370L)
                    .setInterpolator(OvershootInterpolator(1.9f)).start()
            }
            MotionPack.MAGNETIC_FLOAT -> {
                dialogRoot.translationY = 34f * d
                dialogRoot.animate().alpha(1f).translationY(0f).setDuration(330L)
                    .setInterpolator(OvershootInterpolator(1.1f)).start()
            }
            MotionPack.MECHANICAL_CLICK -> {
                dialogRoot.scaleX = 0.94f; dialogRoot.scaleY = 0.94f
                dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(155L)
                    .setInterpolator(DecelerateInterpolator(2.2f)).start()
            }
            MotionPack.WAVE_CASCADE -> {
                dialogRoot.translationY = 20f * d; dialogRoot.rotation = -2f
                dialogRoot.animate().alpha(1f).translationY(0f).rotation(0f).setDuration(310L)
                    .setInterpolator(OvershootInterpolator(1.15f)).start()
            }
            MotionPack.CINEMA_ZOOM -> {
                dialogRoot.scaleX = 1.12f; dialogRoot.scaleY = 1.12f
                dialogRoot.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(370L)
                    .setInterpolator(DecelerateInterpolator(1.8f)).start()
            }
        }
    }

    fun handleTactilePress(v: View, actionMasked: Int) {
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().cancel()
                when (currentPack) {
                    MotionPack.JELLY_BOUNCE -> v.animate().scaleX(1.04f).scaleY(0.92f).setDuration(65L).start()
                    MotionPack.MECHANICAL_CLICK -> v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(40L).start()
                    else -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70L).start()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate().cancel()
                val interp = when (currentPack) {
                    MotionPack.LIQUID_SPRING, MotionPack.JELLY_BOUNCE, MotionPack.AURORA_COSMIC -> OvershootInterpolator(1.6f)
                    else -> DecelerateInterpolator()
                }
                val dur = if (currentPack == MotionPack.MECHANICAL_CLICK) 120L else 230L
                v.animate().scaleX(1f).scaleY(1f).setDuration(dur).setInterpolator(interp).start()
            }
        }
    }

    fun handleInputFocus(activity: MainActivity, view: View, hasFocus: Boolean) {
        val d = activity.resources.displayMetrics.density
        view.animate().cancel()
        if (hasFocus) {
            val sc = when (currentPack) {
                MotionPack.MAGNETIC_FLOAT, MotionPack.AURORA_COSMIC -> 1.028f
                MotionPack.JELLY_BOUNCE, MotionPack.LIQUID_SPRING -> 1.022f
                else -> 1.012f
            }
            val liftY = when (currentPack) {
                MotionPack.MAGNETIC_FLOAT -> -3.5f * d
                MotionPack.AURORA_COSMIC -> -2f * d
                else -> 0f
            }
            view.animate().scaleX(sc).scaleY(sc).translationY(liftY).setDuration(190L)
                .setInterpolator(DecelerateInterpolator()).start()
        } else {
            view.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(160L)
                .setInterpolator(DecelerateInterpolator()).start()
        }
    }

    // =========================================================================
    // תצוגה מקדימה חיה של כל חבילה על גבי הכרטיס שלה בתפריט הבחירה
    // =========================================================================
    private fun playPackPreviewOnCard(activity: MainActivity, cardView: View, pack: MotionPack) {
        val d = activity.resources.displayMetrics.density
        resetViewTransforms(cardView)
        when (pack) {
            MotionPack.ZEN_SILK -> {
                cardView.alpha = 0.55f; cardView.translationY = 8f * d
                cardView.animate().alpha(1f).translationY(0f).setDuration(300L)
                    .setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            MotionPack.LIQUID_SPRING -> {
                cardView.scaleX = 0.90f; cardView.scaleY = 0.90f
                cardView.animate().scaleX(1f).scaleY(1f).setDuration(360L)
                    .setInterpolator(OvershootInterpolator(2.0f)).start()
            }
            MotionPack.AURORA_COSMIC -> {
                cardView.cameraDistance = 6000f * d
                cardView.rotationX = -24f; cardView.scaleX = 0.94f
                cardView.animate().rotationX(0f).scaleX(1f).setDuration(360L)
                    .setInterpolator(OvershootInterpolator(1.5f)).start()
            }
            MotionPack.CYBER_GLITCH -> {
                val tx = ObjectAnimator.ofFloat(cardView, "translationX", 0f, -10f * d, 9f * d, -5f * d, 4f * d, 0f)
                val al = ObjectAnimator.ofFloat(cardView, "alpha", 1f, 0.35f, 1f, 0.55f, 1f)
                AnimatorSet().apply { playTogether(tx, al); duration = 260L; start() }
            }
            MotionPack.ORIGAMI_3D -> {
                cardView.cameraDistance = 6500f * d
                cardView.rotationY = -42f
                cardView.animate().rotationY(0f).setDuration(380L)
                    .setInterpolator(DecelerateInterpolator(1.6f)).start()
            }
            MotionPack.JELLY_BOUNCE -> {
                val sx = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 1.09f, 0.94f, 1.03f, 1f)
                val sy = ObjectAnimator.ofFloat(cardView, "scaleY", 1f, 0.86f, 1.08f, 0.97f, 1f)
                AnimatorSet().apply { playTogether(sx, sy); duration = 420L; start() }
            }
            MotionPack.MAGNETIC_FLOAT -> {
                val ty = ObjectAnimator.ofFloat(cardView, "translationY", 0f, -10f * d, 0f)
                val sc = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 1.03f, 1f)
                AnimatorSet().apply { playTogether(ty, sc); duration = 380L; interpolator = BounceInterpolator(); start() }
            }
            MotionPack.MECHANICAL_CLICK -> {
                val ty = ObjectAnimator.ofFloat(cardView, "translationY", 0f, 4f * d, 0f)
                val sc = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 0.97f, 1f)
                AnimatorSet().apply { playTogether(ty, sc); duration = 140L; start() }
            }
            MotionPack.WAVE_CASCADE -> {
                val rot = ObjectAnimator.ofFloat(cardView, "rotation", 0f, -3.5f, 3.5f, -1.5f, 0f)
                val tx = ObjectAnimator.ofFloat(cardView, "translationX", 0f, -8f * d, 8f * d, 0f)
                AnimatorSet().apply { playTogether(rot, tx); duration = 380L; start() }
            }
            MotionPack.CINEMA_ZOOM -> {
                cardView.scaleX = 1.08f; cardView.scaleY = 1.08f; cardView.alpha = 0.45f
                cardView.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(400L)
                    .setInterpolator(DecelerateInterpolator(1.8f)).start()
            }
        }
    }

    // =========================================================================
    // תפריט בחירת חבילת האנימציות (נפתח בלחיצה ארוכה על "אנימציות ממשק")
    // =========================================================================
    fun showPackPickerDialog(activity: MainActivity) {
        val dlg = Dialog(activity)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val isMin = activity.isMinimalistMode
        val textPrimary = if (isMin) MinimalistTheme.TEXT_CHARCOAL else Color.WHITE
        val textSecondary = if (isMin) MinimalistTheme.TEXT_MUTED_GREY else Color.parseColor("#B0BEC5")
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
            activity.isCleanGlassActive -> IosGlassTheme.createGlassCardDrawable(activity, 26f)
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
            text = "\u200Fחבילות אנימציות ממשק (10 סגנונות)"
            setTextColor(textPrimary)
            textSize = 18.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.RIGHT
        })

        root.addView(TextView(activity).apply {
            text = "\u200Fבחר חבילת תנועה מועדפת • לחץ על 'הדגם' לתצוגה מקדימה חיה"
            setTextColor(textSecondary)
            textSize = 12.8f
            gravity = Gravity.RIGHT
            setPadding(0, activity.dp(3f), 0, activity.dp(12f))
        })

        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (activity.resources.displayMetrics.heightPixels * 0.54f).toInt().coerceAtMost(activity.dp(430f))
            ).apply { bottomMargin = activity.dp(10f) }
        }
        val listCol = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listCol)
        root.addView(scroll)

        for (pack in MotionPack.entries) {
            val isSelected = (pack == currentPack)
            val packAccent = Color.parseColor(pack.accentHex)
            val cardFill = when {
                isSelected && isMin -> Color.argb(48, Color.red(packAccent), Color.green(packAccent), Color.blue(packAccent))
                isSelected -> Color.argb(54, Color.red(packAccent), Color.green(packAccent), Color.blue(packAccent))
                isMin -> MinimalistTheme.CARD_WHITE
                else -> Color.argb(30, 255, 255, 255)
            }
            val borderCol = when {
                isSelected -> packAccent
                isMin -> Color.parseColor("#E5DFD7")
                else -> Color.argb(42, 255, 255, 255)
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

            // כפתור הדגמה חיה משמאל
            val previewChipBg = if (isMin) MinimalistTheme.BG_CREAM else Color.argb(52, Color.red(packAccent), Color.green(packAccent), Color.blue(packAccent))
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
                    playPackPreviewOnCard(activity, card, pack)
                }
            }

            // טקסט תיאור במרכז-ימין
            val textCol = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.RIGHT
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = activity.dp(8f)
                    rightMargin = activity.dp(10f)
                }
            }
            val titleRowTxt = if (isSelected) "✓ ${pack.displayName} (פעיל)" else "${pack.id + 1}. ${pack.displayName}"
            textCol.addView(TextView(activity).apply {
                text = "\u200F$titleRowTxt"
                setTextColor(if (isSelected) MinimalistTheme.ensureReadableTextOnBg(packAccent, cardFill, 2.3) else textPrimary)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.RIGHT
            })
            textCol.addView(TextView(activity).apply {
                text = "\u200F${pack.description}"
                setTextColor(textSecondary)
                textSize = 12f
                gravity = Gravity.RIGHT
                setPadding(0, activity.dp(2f), 0, 0)
            })

            // סמל וקטורי אישי לכל חבילה בצד ימין
            val iconHolder = TextView(activity).apply {
                val badgeDrawable = createPackVectorIcon(activity, pack, packAccent, 26f)
                setCompoundDrawables(badgeDrawable, null, null, null)
                gravity = Gravity.CENTER
            }

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
    // 10 סמלים וקטוריים מותאמים אישית לכל חבילה בתפריט הבחירה
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
                        path.moveTo(cx, cy - ir)
                        path.lineTo(cx + ir * 0.9f, cy + ir * 0.45f)
                        path.lineTo(cx - ir * 0.9f, cy + ir * 0.45f)
                        path.close()
                        canvas.drawPath(path, strokeP)
                        canvas.drawLine(cx, cy - ir, cx, cy + ir * 0.45f, strokeP)
                    }
                    MotionPack.JELLY_BOUNCE -> {
                        canvas.drawRoundRect(RectF(cx - ir * 0.88f, cy - ir * 0.62f, cx + ir * 0.88f, cy + ir * 0.68f), ir * 0.45f, ir * 0.45f, strokeP)
                    }
                    MotionPack.MAGNETIC_FLOAT -> {
                        canvas.drawArc(RectF(cx - ir * 0.7f, cy - ir * 0.7f, cx + ir * 0.7f, cy + ir * 0.7f), 180f, 180f, false, strokeP)
                        canvas.drawLine(cx - ir * 0.7f, cy, cx - ir * 0.7f, cy + ir * 0.65f, strokeP)
                        canvas.drawLine(cx + ir * 0.7f, cy, cx + ir * 0.7f, cy + ir * 0.65f, strokeP)
                    }
                    MotionPack.MECHANICAL_CLICK -> {
                        canvas.drawRect(cx - ir * 0.78f, cy - ir * 0.72f, cx + ir * 0.78f, cy + ir * 0.72f, strokeP)
                        canvas.drawLine(cx - ir * 0.32f, cy, cx + ir * 0.32f, cy, strokeP)
                    }
                    MotionPack.WAVE_CASCADE -> {
                        canvas.drawCircle(cx, cy, ir * 0.32f, strokeP)
                        canvas.drawCircle(cx, cy, ir * 0.86f, strokeP)
                    }
                    MotionPack.CINEMA_ZOOM -> {
                        val c = ir * 0.82f; val leg = ir * 0.38f
                        canvas.drawLine(cx - c, cy - c, cx - c + leg, cy - c, strokeP)
                        canvas.drawLine(cx - c, cy - c, cx - c, cy - c + leg, strokeP)
                        canvas.drawLine(cx + c, cy - c, cx + c - leg, cy - c, strokeP)
                        canvas.drawLine(cx + c, cy - c, cx + c, cy - c + leg, strokeP)
                        canvas.drawLine(cx - c, cy + c, cx - c + leg, cy + c, strokeP)
                        canvas.drawLine(cx - c, cy + c, cx - c, cy + c - leg, strokeP)
                        canvas.drawLine(cx + c, cy + c, cx + c - leg, cy + c, strokeP)
                        canvas.drawLine(cx + c, cy + c, cx + c, cy + c - leg, strokeP)
                    }
                }
            }

            override fun setAlpha(alpha: Int) { strokeP.alpha = alpha; fillP.alpha = alpha }
            override fun setColorFilter(colorFilter: ColorFilter?) { strokeP.colorFilter = colorFilter; fillP.colorFilter = colorFilter }
            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
        }.apply { setBounds(0, 0, sizePx, sizePx) }
    }
}

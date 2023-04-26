package code.name.monkey.retromusic.extensions

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.annotation.Px
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.RetroUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigationrail.NavigationRailView
import dev.chrisbanes.insetter.applyInsetter

const val ANIM_DURATION = 300L

@Suppress("UNCHECKED_CAST")
fun <T : View> ViewGroup.inflate(@LayoutRes layout: Int): T {
    return LayoutInflater.from(context).inflate(
        /* resource = */ layout,
        /* root = */ this,
        /* attachToRoot = */ false
    ) as T
}

fun View.show() {
    isVisible = true
}

fun View.hide() {
    isVisible = false
}

fun View.hidden() {
    isInvisible = true
}

fun EditText.appHandleColor(): EditText {
    if (PreferenceUtil.materialYou) return this
    TintHelper.colorHandles(/* view = */ this, /* color = */ ThemeStore.accentColor(context))
    return this
}

fun NavigationBarView.setItemColors(@ColorInt normalColor: Int, @ColorInt selectedColor: Int) {
    val csl = ColorStateList(
        /* states = */ arrayOf(
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_checked)
        ),
        /* colors = */ intArrayOf(normalColor, selectedColor)
    )
    itemIconTintList = csl
    itemTextColor = csl
}

/**
 * Potentially animate showing a [BottomNavigationView].
 *
 * Abruptly changing the visibility leads to a re-layout of main content, animating
 * `translationY` leaves a gap where the view was that content does not fill.
 *
 * Instead, take a snapshot of the view, and animate this in, only changing the visibility (and
 * thus layout) when the animation completes.
 */
fun NavigationBarView.show() {
    if (this is NavigationRailView) return
    if (isVisible) return

    val parent = parent as ViewGroup
    // View needs to be laid out to create a snapshot & know position to animate. If view isn't
    // laid out yet, need to do this manually.
    if (!isLaidOut) {
        measure(
            View.MeasureSpec.makeMeasureSpec(/* size = */ parent.width, /* mode = */
                View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.makeMeasureSpec(/* size = */ parent.height, /* mode = */
                View.MeasureSpec.AT_MOST
            )
        )
        layout(
            /* l = */ parent.left,
            /* t = */ parent.height - measuredHeight,
            /* r = */ parent.right,
            /* b = */ parent.height
        )
    }

    val drawable = BitmapDrawable(/* res = */ context.resources, /* bitmap = */ drawToBitmap())
    drawable.setBounds(
        /* left = */ left,
        /* top = */ parent.height,
        /* right = */ right,
        /* bottom = */ parent.height + height
    )
    parent.overlay.add(drawable)
    ValueAnimator.ofInt(/* ...values = */ parent.height, top).apply {
        duration = ANIM_DURATION
        interpolator = AnimationUtils.loadInterpolator(
            /* context = */ context,
            /* id = */ android.R.interpolator.accelerate_decelerate
        )
        addUpdateListener {
            val newTop = it.animatedValue as Int
            drawable.setBounds(
                /* left = */ left,
                /* top = */ newTop,
                /* right = */ right,
                /* bottom = */ newTop + height
            )
        }
        doOnEnd {
            parent.overlay.remove(drawable)
            isVisible = true
        }
        start()
    }
}

/**
 * Potentially animate hiding a [BottomNavigationView].
 *
 * Abruptly changing the visibility leads to a re-layout of main content, animating
 * `translationY` leaves a gap where the view was that content does not fill.
 *
 * Instead, take a snapshot, instantly hide the view (so content lays out to fill), then animate
 * out the snapshot.
 */
fun NavigationBarView.hide() {
    if (this is NavigationRailView) return
    if (isGone) return

    if (!isLaidOut) {
        isGone = true
        return
    }

    val drawable = BitmapDrawable(/* res = */ context.resources, /* bitmap = */ drawToBitmap())
    val parent = parent as ViewGroup
    drawable.setBounds(
        /* left = */ left,
        /* top = */ top,
        /* right = */ right,
        /* bottom = */ bottom
    )
    parent.overlay.add(drawable)
    isGone = true
    ValueAnimator.ofInt(top, parent.height).apply {
        duration = ANIM_DURATION
        interpolator = AnimationUtils.loadInterpolator(
            /* context = */ context,
            /* id = */ android.R.interpolator.accelerate_decelerate
        )
        addUpdateListener {
            val newTop = it.animatedValue as Int
            drawable.setBounds(
                /* left = */ left,
                /* top = */ newTop,
                /* right = */ right,
                /* bottom = */ newTop + height
            )
        }
        doOnEnd {
            parent.overlay.remove(drawable)
        }
        start()
    }
}

fun View.translateYAnimate(value: Float): Animator {
    return ObjectAnimator.ofFloat(
        /* target = */ this,
        /* propertyName = */ "translationY",
        /* ...values = */ value
    )
        .apply {
            duration = 300
            doOnStart {
                show()
                bringToFront()
            }
            doOnEnd {
                isGone = (value != 0f)
            }
            start()
        }
}

fun BottomSheetBehavior<*>.peekHeightAnimate(value: Int): Animator {
    return ObjectAnimator.ofInt(
        /* target = */ this,
        /* propertyName = */ "peekHeight",
        /* ...values = */ value
    )
        .apply {
            duration = ANIM_DURATION
            start()
        }
}

fun MaterialCardView.animateRadius(cornerRadius: Float, pause: Boolean = true) {
    ValueAnimator.ofFloat(radius, cornerRadius).apply {
        addUpdateListener { radius = animatedValue as Float }
        start()
    }
    ValueAnimator.ofInt(measuredWidth, if (pause) (height * 1.5).toInt() else height).apply {
        addUpdateListener {
            updateLayoutParams<ViewGroup.LayoutParams> { width = animatedValue as Int }
        }
        start()
    }
}

fun MaterialCardView.animateToCircle() {
    animateRadius(cornerRadius = measuredHeight / 2F, pause = false)
}

fun View.focusAndShowKeyboard() {
    /**
     * This is to be called when the window already has focus.
     */
    fun View.showTheKeyboardNow() {
        if (isFocused) {
            post {
                // We still post the call, just in case we are being notified of the windows focus
                // but InputMethodManager didn't get properly setup yet.
                val imm =
                    context.getSystemService<InputMethodManager>()
                imm?.showSoftInput(/* view = */ this, /* flags = */
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        }
    }

    requestFocus()
    if (hasWindowFocus()) {
        // No need to wait for the window to get focus.
        showTheKeyboardNow()
    } else {
        // We need to wait until the window gets focus.
        viewTreeObserver.addOnWindowFocusChangeListener(
            object : ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    // This notification will arrive just before the InputMethodManager gets set up.
                    if (hasFocus) {
                        this@focusAndShowKeyboard.showTheKeyboardNow()
                        // It’s very important to remove this listener once we are done.
                        viewTreeObserver.removeOnWindowFocusChangeListener(/* victim = */ this)
                    }
                }
            })
    }
}

/**
 * This will draw our view above the navigation bar instead of behind it by adding margins.
 */
fun View.drawAboveSystemBars(onlyPortrait: Boolean = true) {
    if (PreferenceUtil.isFullScreenMode) return
    if (onlyPortrait && RetroUtil.isLandscape) return
    applyInsetter {
        type(navigationBars = true) {
            margin()
        }
    }
}

/**
 * This will draw our view above the navigation bar instead of behind it by adding padding.
 */
fun View.drawAboveSystemBarsWithPadding() {
    if (PreferenceUtil.isFullScreenMode) return
    applyInsetter {
        type(navigationBars = true) {
            padding()
        }
    }
}

fun View.drawNextToNavbar() {
    if (PreferenceUtil.isFullScreenMode) return
    applyInsetter {
        type(statusBars = true, navigationBars = true) {
            padding(horizontal = true)
        }
    }
}

fun View.updateMargin(
    @Px left: Int = marginLeft,
    @Px top: Int = marginTop,
    @Px right: Int = marginRight,
    @Px bottom: Int = marginBottom,
) {
    (layoutParams as ViewGroup.MarginLayoutParams).updateMargins(left, top, right, bottom)
}

fun View.applyBottomInsets() {
    if (PreferenceUtil.isFullScreenMode) return
    val initialPadding = recordInitialPaddingForView(view = this)

    ViewCompat.setOnApplyWindowInsetsListener(
        (this)
    ) { v: View, windowInsets: WindowInsetsCompat ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.updatePadding(
            bottom = initialPadding.bottom + insets.bottom
        )
        windowInsets
    }
    requestApplyInsetsWhenAttached()
}

fun View.requestApplyInsetsWhenAttached() {
    if (isAttachedToWindow) {
        // We're already attached, just request as normal
        requestApplyInsets()
    } else {
        // We're not attached to the hierarchy, add a listener to
        // request when we are
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(/* listener = */ this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) = Unit
        })
    }
}

data class InitialPadding(
    val left: Int, val top: Int,
    val right: Int, val bottom: Int,
)

fun recordInitialPaddingForView(view: View) = InitialPadding(
    view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom
)

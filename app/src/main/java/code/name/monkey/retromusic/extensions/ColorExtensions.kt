package code.name.monkey.retromusic.extensions

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.ATHUtil
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.util.PreferenceUtil.materialYou
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout

fun Int.ripAlpha(): Int {
    return ColorUtil.stripAlpha(color = this)
}

fun Dialog.colorControlNormal() = resolveColor(android.R.attr.colorControlNormal)

fun Toolbar.backgroundTintList() {
    val surfaceColor =
        ATHUtil.resolveColor(
            context = context,
            attr = com.google.android.material.R.attr.colorSurface,
            fallback = Color.BLACK
        )
    val colorStateList = ColorStateList.valueOf(surfaceColor)
    backgroundTintList = colorStateList
}

fun Context.accentColor() = ThemeStore.accentColor(context = this)

fun Fragment.accentColor() = ThemeStore.accentColor(requireContext())

fun Context.surfaceColor() =
    resolveColor(
        attr = com.google.android.material.R.attr.colorSurface,
        fallBackColor = Color.WHITE
    )

fun Fragment.surfaceColor() =
    resolveColor(
        attr = com.google.android.material.R.attr.colorSurface,
        fallBackColor = Color.WHITE
    )

fun Context.surfaceColor(fallBackColor: Int) =
    resolveColor(
        attr = com.google.android.material.R.attr.colorSurface,
        fallBackColor = fallBackColor
    )

fun Fragment.surfaceColor(fallBackColor: Int) =
    resolveColor(
        attr = com.google.android.material.R.attr.colorSurface,
        fallBackColor = fallBackColor
    )

fun Context.textColorSecondary() = resolveColor(attr = android.R.attr.textColorSecondary)

fun Fragment.textColorSecondary() = resolveColor(attr = android.R.attr.textColorSecondary)

fun Context.colorControlNormal() = resolveColor(attr = android.R.attr.colorControlNormal)

fun Fragment.colorControlNormal() = resolveColor(attr = android.R.attr.colorControlNormal)

fun Context.colorBackground() = resolveColor(attr = android.R.attr.colorBackground)

fun Fragment.colorBackground() = resolveColor(attr = android.R.attr.colorBackground)

fun Context.textColorPrimary() = resolveColor(attr = android.R.attr.textColorPrimary)

fun Fragment.textColorPrimary() = resolveColor(attr = android.R.attr.textColorPrimary)

fun Context.defaultFooterColor() = resolveColor(attr = R.attr.defaultFooterColor)

fun Fragment.defaultFooterColor() = resolveColor(attr = R.attr.defaultFooterColor)

fun Context.resolveColor(@AttrRes attr: Int, fallBackColor: Int = 0) =
    ATHUtil.resolveColor(context = this, attr = attr, fallback = fallBackColor)

fun Fragment.resolveColor(@AttrRes attr: Int, fallBackColor: Int = 0) =
    ATHUtil.resolveColor(context = requireContext(), attr = attr, fallback = fallBackColor)

fun Dialog.resolveColor(@AttrRes attr: Int, fallBackColor: Int = 0) =
    ATHUtil.resolveColor(context = context, attr = attr, fallback = fallBackColor)

// Don't apply accent colors if Material You is enabled
// Material Components will take care of applying material you colors
fun CheckBox.addAccentColor() {
    if (materialYou) return
    buttonTintList = ColorStateList.valueOf(ThemeStore.accentColor(context))
}

fun SeekBar.addAccentColor() {
    if (materialYou) return
    val colorState = ColorStateList.valueOf(ThemeStore.accentColor(context))
    progressTintList = colorState
    thumbTintList = colorState
}

fun Slider.addAccentColor() {
    if (materialYou) return
    val accentColor = ThemeStore.accentColor(context)
    trackActiveTintList = accentColor.colorStateList
    trackInactiveTintList =
        ColorUtil.withAlpha(baseColor = accentColor, alpha = 0.5F).colorStateList
    thumbTintList = accentColor.colorStateList
}

fun Slider.accent() {
    if (materialYou) return
    val accentColor = context.accentColor()
    thumbTintList = accentColor.colorStateList
    trackActiveTintList = accentColor.colorStateList
    trackInactiveTintList =
        ColorUtil.withAlpha(baseColor = accentColor, alpha = 0.1F).colorStateList
}

fun Button.accentTextColor() {
    if (materialYou) return
    setTextColor(/* color = */ context.accentColor())
}

fun MaterialButton.accentBackgroundColor() {
    if (materialYou) return
    backgroundTintList = ColorStateList(
        /* states = */ arrayOf(intArrayOf(android.R.attr.state_enabled), intArrayOf()),
        /* colors = */
        intArrayOf(context.accentColor(), context.accentColor().addAlpha(alpha = 0.12f))
    )
}

fun MaterialButton.accentOutlineColor() {
    if (materialYou) return
    val color = ThemeStore.accentColor(context = context)
    val colorStateList = ColorStateList.valueOf(/* color = */ color)
    iconTint = colorStateList
    strokeColor = colorStateList
    setTextColor(colorStateList)
    rippleColor = colorStateList
}

fun MaterialButton.elevatedAccentColor() {
    if (materialYou) return
    val color = context.darkAccentColorVariant()
    rippleColor = ColorStateList.valueOf(color)
    setBackgroundColor(/* color = */ color)
    setTextColor(/* color = */ MaterialValueHelper.getPrimaryTextColor(
        context = context,
        dark = color.isColorLight
    )
    )
    iconTint = ColorStateList.valueOf(/* color = */ context.accentColor())
}

fun SeekBar.applyColor(@ColorInt color: Int) {
    thumbTintList = ColorStateList.valueOf(color)
    progressTintList = ColorStateList.valueOf(color)
    progressBackgroundTintList = ColorStateList.valueOf(color)
}

fun Slider.applyColor(@ColorInt color: Int) {
    ColorStateList.valueOf(color).run {
        thumbTintList = this
        trackActiveTintList = this
        trackInactiveTintList = ColorStateList.valueOf(color.addAlpha(alpha = 0.1f))
        haloTintList = this
    }
}

fun ExtendedFloatingActionButton.accentColor() {
    if (materialYou) return
    val color = ThemeStore.accentColor(context = context)
    val textColor = MaterialValueHelper.getPrimaryTextColor(
        context = context,
        dark = ColorUtil.isColorLight(color)
    )
    val colorStateList = ColorStateList.valueOf(color)
    val textColorStateList = ColorStateList.valueOf(textColor)
    backgroundTintList = colorStateList
    setTextColor(textColorStateList)
    iconTint = textColorStateList
}

fun FloatingActionButton.accentColor() {
    if (materialYou) return
    val color = ThemeStore.accentColor(context = context)
    val textColor = MaterialValueHelper.getPrimaryTextColor(
        context = context,
        dark = ColorUtil.isColorLight(color)
    )
    backgroundTintList = ColorStateList.valueOf(color)
    imageTintList = ColorStateList.valueOf(textColor)
}

fun MaterialButton.applyColor(color: Int) {
    val backgroundColorStateList = ColorStateList.valueOf(color)
    val textColorColorStateList = ColorStateList.valueOf(
        MaterialValueHelper.getPrimaryTextColor(
            context = context,
            dark = ColorUtil.isColorLight(color)
        )
    )
    backgroundTintList = backgroundColorStateList
    setTextColor(textColorColorStateList)
    iconTint = textColorColorStateList
}

fun MaterialButton.accentColor() {
    if (materialYou) return
    applyColor(ThemeStore.accentColor(context = context))
}

fun MaterialButton.applyOutlineColor(color: Int) {
    val colorStateList = ColorStateList.valueOf(color)
    iconTint = colorStateList
    strokeColor = colorStateList
    setTextColor(colorStateList)
    rippleColor = colorStateList
}

fun TextInputLayout.accentColor() {
    if (materialYou) return
    val accentColor = ThemeStore.accentColor(context = context)
    val colorState = ColorStateList.valueOf(/* color = */ accentColor)
    boxStrokeColor = accentColor
    defaultHintTextColor = colorState
    isHintAnimationEnabled = true
}

fun CircularProgressIndicator.accentColor() {
    if (materialYou) return
    val color = ThemeStore.accentColor(context = context)
    setIndicatorColor(/* ...indicatorColors = */ color)
    trackColor = ColorUtil.withAlpha(baseColor = color, alpha = 0.2f)
}

fun CircularProgressIndicator.applyColor(color: Int) {
    setIndicatorColor(color)
    trackColor = ColorUtil.withAlpha(baseColor = color, alpha = 0.2f)
}

fun AppCompatImageView.accentColor(): Int = ThemeStore.accentColor(context)

fun TextInputLayout.setTint(background: Boolean = true) {
    if (materialYou) return
    val accentColor = ThemeStore.accentColor(context)
    val colorState = ColorStateList.valueOf(accentColor)

    if (background) {
        backgroundTintList = colorState
        defaultHintTextColor = colorState
    } else {
        boxStrokeColor = accentColor
        defaultHintTextColor = colorState
        isHintAnimationEnabled = true
    }
}

@CheckResult
fun Drawable.tint(@ColorInt color: Int): Drawable {
    val tintedDrawable = DrawableCompat.wrap(/* drawable = */ this).mutate()
    setTint(color)
    return tintedDrawable
}

@CheckResult
fun Drawable.tint(context: Context, @ColorRes color: Int): Drawable =
    tint(context.getColorCompat(color))

@ColorInt
fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(/* context = */ this, /* id = */ colorRes)
}

@ColorInt
fun Context.darkAccentColor(): Int {
    return ColorUtils.blendARGB(
        /* color1 = */ accentColor(),
        /* color2 = */ surfaceColor(),
        /* ratio = */ if (surfaceColor().isColorLight) 0.9f else 0.92f
    )
}

@ColorInt
fun Context.darkAccentColorVariant(): Int {
    return ColorUtils.blendARGB(
        /* color1 = */ accentColor(),
        /* color2 = */ surfaceColor(),
        /* ratio = */ if (surfaceColor().isColorLight) 0.9f else 0.95f
    )
}

@ColorInt
fun Context.accentColorVariant(): Int {
    return if (surfaceColor().isColorLight) {
        accentColor().darkerColor
    } else {
        accentColor().lighterColor
    }
}

inline val @receiver:ColorInt Int.isColorLight
    get() = ColorUtil.isColorLight(color = this)

inline val @receiver:ColorInt Int.lightColor
    get() = ColorUtil.withAlpha(baseColor = this, alpha = 0.5F)

inline val @receiver:ColorInt Int.lighterColor
    get() = ColorUtil.lightenColor(color = this)

inline val @receiver:ColorInt Int.darkerColor
    get() = ColorUtil.darkenColor(color = this)

inline val Int.colorStateList: ColorStateList
    get() = ColorStateList.valueOf(/* color = */ this)

fun @receiver:ColorInt Int.addAlpha(alpha: Float): Int {
    return ColorUtil.withAlpha(baseColor = this, alpha = alpha)
}

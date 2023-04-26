package code.name.monkey.appthemehelper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.IntRange
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.util.ATHUtil.isWindowBackgroundDark
import code.name.monkey.appthemehelper.util.ATHUtil.resolveColor
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.VersionUtils

class ThemeStore
private constructor(private val mContext: Context) : ThemeStorePrefKeys, ThemeStoreInterface {

    private val mEditor: SharedPreferences.Editor = prefs(mContext).edit()

    override fun activityTheme(@StyleRes theme: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_ACTIVITY_THEME, theme)
        return this
    }

    override fun primaryColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_PRIMARY_COLOR, color)
        if (autoGeneratePrimaryDark(mContext))
            primaryColorDark(ColorUtil.darkenColor(color))
        return this
    }

    override fun primaryColorRes(@ColorRes colorRes: Int): ThemeStore {
        return primaryColor(ContextCompat.getColor(mContext, colorRes))
    }

    override fun primaryColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return primaryColor(resolveColor(mContext, colorAttr))
    }

    override fun primaryColorDark(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_PRIMARY_COLOR_DARK, color)
        return this
    }

    override fun primaryColorDarkRes(@ColorRes colorRes: Int): ThemeStore {
        return primaryColorDark(ContextCompat.getColor(mContext, colorRes))
    }

    override fun primaryColorDarkAttr(@AttrRes colorAttr: Int): ThemeStore {
        return primaryColorDark(resolveColor(mContext, colorAttr))
    }

    override fun accentColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_ACCENT_COLOR, color)
        return this
    }

    override fun wallpaperColor(context: Context, color: Int): ThemeStore {
        if (ColorUtil.isColorLight(color)) {
            mEditor.putInt(ThemeStorePrefKeys.KEY_WALLPAPER_COLOR_DARK, color)
            mEditor.putInt(
                /* p0 = */ ThemeStorePrefKeys.KEY_WALLPAPER_COLOR_LIGHT,
                /* p1 = */ ColorUtil.getReadableColorLight(
                    color = color,
                    bgColor = Color.WHITE
                )
            )
        } else {
            mEditor.putInt(ThemeStorePrefKeys.KEY_WALLPAPER_COLOR_LIGHT, color)
            mEditor.putInt(
                /* p0 = */ ThemeStorePrefKeys.KEY_WALLPAPER_COLOR_DARK,
                /* p1 = */ ColorUtil.getReadableColorDark(
                    color = color,
                    bgColor = Color.parseColor(/* colorString = */ "#202124")
                )
            )
        }
        return this
    }

    override fun accentColorRes(@ColorRes colorRes: Int): ThemeStore {
        return accentColor(ContextCompat.getColor(mContext, colorRes))
    }

    override fun accentColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return accentColor(resolveColor(mContext, colorAttr))
    }

    override fun statusBarColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_STATUS_BAR_COLOR, color)
        return this
    }

    override fun statusBarColorRes(@ColorRes colorRes: Int): ThemeStore {
        return statusBarColor(ContextCompat.getColor(mContext, colorRes))
    }

    override fun statusBarColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return statusBarColor(resolveColor(mContext, colorAttr))
    }

    override fun navigationBarColor(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_NAVIGATION_BAR_COLOR, color)
        return this
    }

    // Commit method

    override fun navigationBarColorRes(@ColorRes colorRes: Int): ThemeStore {
        return navigationBarColor(ContextCompat.getColor(mContext, colorRes))
    }

    // Static getters

    override fun navigationBarColorAttr(@AttrRes colorAttr: Int): ThemeStore {
        return navigationBarColor(resolveColor(mContext, colorAttr))
    }

    override fun textColorPrimary(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_TEXT_COLOR_PRIMARY, color)
        return this
    }

    override fun textColorPrimaryRes(@ColorRes colorRes: Int): ThemeStore {
        return textColorPrimary(ContextCompat.getColor(mContext, colorRes))
    }

    override fun textColorPrimaryAttr(@AttrRes colorAttr: Int): ThemeStore {
        return textColorPrimary(resolveColor(mContext, colorAttr))
    }

    override fun textColorPrimaryInverse(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_TEXT_COLOR_PRIMARY_INVERSE, color)
        return this
    }

    override fun textColorPrimaryInverseRes(@ColorRes colorRes: Int): ThemeStore {
        return textColorPrimaryInverse(ContextCompat.getColor(mContext, colorRes))
    }

    override fun textColorPrimaryInverseAttr(@AttrRes colorAttr: Int): ThemeStore {
        return textColorPrimaryInverse(resolveColor(mContext, colorAttr))
    }

    override fun textColorSecondary(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_TEXT_COLOR_SECONDARY, color)
        return this
    }

    override fun textColorSecondaryRes(@ColorRes colorRes: Int): ThemeStore {
        return textColorSecondary(ContextCompat.getColor(mContext, colorRes))
    }

    override fun textColorSecondaryAttr(@AttrRes colorAttr: Int): ThemeStore {
        return textColorSecondary(resolveColor(mContext, colorAttr))
    }

    override fun textColorSecondaryInverse(@ColorInt color: Int): ThemeStore {
        mEditor.putInt(ThemeStorePrefKeys.KEY_TEXT_COLOR_SECONDARY_INVERSE, color)
        return this
    }

    override fun textColorSecondaryInverseRes(@ColorRes colorRes: Int): ThemeStore {
        return textColorSecondaryInverse(ContextCompat.getColor(mContext, colorRes))
    }

    override fun textColorSecondaryInverseAttr(@AttrRes colorAttr: Int): ThemeStore {
        return textColorSecondaryInverse(resolveColor(mContext, colorAttr))
    }

    override fun coloredStatusBar(colored: Boolean): ThemeStore {
        mEditor.putBoolean(ThemeStorePrefKeys.KEY_APPLY_PRIMARYDARK_STATUSBAR, colored)
        return this
    }

    override fun coloredNavigationBar(applyToNavBar: Boolean): ThemeStore {
        mEditor.putBoolean(ThemeStorePrefKeys.KEY_APPLY_PRIMARY_NAVBAR, applyToNavBar)
        return this
    }

    override fun autoGeneratePrimaryDark(autoGenerate: Boolean): ThemeStore {
        mEditor.putBoolean(ThemeStorePrefKeys.KEY_AUTO_GENERATE_PRIMARYDARK, autoGenerate)
        return this
    }

    override fun commit() {
        mEditor.putLong(ThemeStorePrefKeys.VALUES_CHANGED, System.currentTimeMillis())
            .putBoolean(ThemeStorePrefKeys.IS_CONFIGURED_KEY, true)
            .commit()
    }

    companion object {

        fun editTheme(context: Context): ThemeStore {
            return ThemeStore(context)
        }

        @CheckResult
        fun prefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(
                /* p0 = */ ThemeStorePrefKeys.CONFIG_PREFS_KEY_DEFAULT,
                /* p1 = */ Context.MODE_PRIVATE
            )
        }

        fun markChanged(context: Context) {
            ThemeStore(context).commit()
        }

        @CheckResult
        @StyleRes
        fun activityTheme(context: Context): Int {
            return prefs(context).getInt(ThemeStorePrefKeys.KEY_ACTIVITY_THEME, 0)
        }

        @CheckResult
        @ColorInt
        fun primaryColor(context: Context): Int {
            return prefs(context).getInt(
                /* p0 = */ ThemeStorePrefKeys.KEY_PRIMARY_COLOR,
                /* p1 = */ resolveColor(
                    context = context,
                    attr = androidx.appcompat.R.attr.colorPrimary,
                    fallback = Color.parseColor(/* colorString = */ "#455A64")
                )
            )
        }

        @CheckResult
        @ColorInt
        fun accentColor(context: Context): Int {
            // Set MD3 accent if MD3 is enabled or in-app accent otherwise
            if (isMD3Enabled(context) && VersionUtils.hasS()) {
                return ContextCompat.getColor(context, R.color.m3_accent_color)
            }
            val desaturatedColor = prefs(context).getBoolean("desaturated_color", false)
            val color = if (isWallpaperAccentEnabled(context)) {
                wallpaperColor(context, isWindowBackgroundDark(context))
            } else {
                prefs(context).getInt(
                    /* p0 = */ ThemeStorePrefKeys.KEY_ACCENT_COLOR,
                    /* p1 = */ resolveColor(
                        context = context,
                        attr = androidx.appcompat.R.attr.colorAccent,
                        fallback = Color.parseColor(/* colorString = */ "#263238")
                    )
                )
            }
            return if (isWindowBackgroundDark(context) && desaturatedColor) ColorUtil.desaturateColor(
                color = color,
                ratio = 0.4f
            ) else color
        }

        @CheckResult
        @ColorInt
        fun wallpaperColor(context: Context, isDarkMode: Boolean): Int {
            return prefs(context).getInt(
                if (isDarkMode) ThemeStorePrefKeys.KEY_WALLPAPER_COLOR_DARK else ThemeStorePrefKeys.KEY_WALLPAPER_COLOR_LIGHT,
                resolveColor(
                    context = context,
                    attr = androidx.appcompat.R.attr.colorAccent,
                    fallback = Color.parseColor(/* colorString = */ "#263238")
                )
            )
        }

        @CheckResult
        @ColorInt
        fun navigationBarColor(context: Context): Int {
            return if (!coloredNavigationBar(context)) {
                Color.BLACK
            } else prefs(context).getInt(
                /* p0 = */ ThemeStorePrefKeys.KEY_NAVIGATION_BAR_COLOR,
                /* p1 = */ primaryColor(context)
            )
        }

        @CheckResult
        fun coloredStatusBar(context: Context): Boolean {
            return prefs(context).getBoolean(
                /* p0 = */ ThemeStorePrefKeys.KEY_APPLY_PRIMARYDARK_STATUSBAR,
                /* p1 = */ true
            )
        }

        @CheckResult
        fun coloredNavigationBar(context: Context): Boolean {
            return prefs(context).getBoolean(ThemeStorePrefKeys.KEY_APPLY_PRIMARY_NAVBAR, false)
        }

        @CheckResult
        fun autoGeneratePrimaryDark(context: Context): Boolean {
            return prefs(context).getBoolean(ThemeStorePrefKeys.KEY_AUTO_GENERATE_PRIMARYDARK, true)
        }

        @CheckResult
        fun isConfigured(context: Context): Boolean {
            return prefs(context).getBoolean(ThemeStorePrefKeys.IS_CONFIGURED_KEY, false)
        }

        @CheckResult
        @ColorInt
        fun textColorPrimary(context: Context): Int {
            return prefs(context).getInt(
                /* p0 = */ ThemeStorePrefKeys.KEY_TEXT_COLOR_PRIMARY,
                /* p1 = */ resolveColor(context, android.R.attr.textColorPrimary)
            )
        }

        @CheckResult
        @ColorInt
        fun textColorPrimaryInverse(context: Context): Int {
            return prefs(context).getInt(
                /* p0 = */ ThemeStorePrefKeys.KEY_TEXT_COLOR_PRIMARY_INVERSE,
                /* p1 = */ resolveColor(context, android.R.attr.textColorPrimaryInverse)
            )
        }

        @CheckResult
        @ColorInt
        fun textColorSecondary(context: Context): Int {
            return prefs(context).getInt(
                /* p0 = */ ThemeStorePrefKeys.KEY_TEXT_COLOR_SECONDARY,
                /* p1 = */ resolveColor(context, android.R.attr.textColorSecondary)
            )
        }

        @CheckResult
        @ColorInt
        fun textColorSecondaryInverse(context: Context): Int {
            return prefs(context).getInt(
                /* p0 = */ ThemeStorePrefKeys.KEY_TEXT_COLOR_SECONDARY_INVERSE,
                /* p1 = */ resolveColor(context, android.R.attr.textColorSecondaryInverse)
            )
        }

        fun isConfigured(
            context: Context,
            @IntRange(
                from = 0,
                to = Integer.MAX_VALUE.toLong()
            ) version: Int,
        ): Boolean {
            val prefs = prefs(context)
            val lastVersion = prefs.getInt(ThemeStorePrefKeys.IS_CONFIGURED_VERSION_KEY, -1)
            if (version > lastVersion) {
                prefs.edit { putInt(ThemeStorePrefKeys.IS_CONFIGURED_VERSION_KEY, version) }
                return false
            }
            return true
        }

        fun isMD3Enabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(ThemeStorePrefKeys.KEY_MATERIAL_YOU, VersionUtils.hasS())
        }

        private fun isWallpaperAccentEnabled(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean("wallpaper_accent", false)
        }
    }
}

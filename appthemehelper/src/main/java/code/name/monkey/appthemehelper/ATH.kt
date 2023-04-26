package code.name.monkey.appthemehelper

import android.content.Context
import android.view.View
import androidx.annotation.ColorInt
import code.name.monkey.appthemehelper.util.TintHelper

object ATH {

    fun didThemeValuesChange(context: Context, since: Long): Boolean {
        return ThemeStore.isConfigured(context) && ThemeStore.prefs(context).getLong(
            /* p0 = */ ThemeStorePrefKeys.VALUES_CHANGED,
            /* p1 = */ -1
        ) > since
    }

    fun setTint(view: View, @ColorInt color: Int) {
        TintHelper.setTintAuto(/* view = */ view, /* color = */ color, /* background = */ false)
    }

    fun setBackgroundTint(view: View, @ColorInt color: Int) {
        TintHelper.setTintAuto(/* view = */ view, /* color = */ color, /* background = */ true)
    }
}

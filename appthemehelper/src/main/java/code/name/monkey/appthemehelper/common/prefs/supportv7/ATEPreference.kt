package code.name.monkey.appthemehelper.common.prefs.supportv7

import android.content.Context
import android.util.AttributeSet
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.preference.Preference
import code.name.monkey.appthemehelper.util.ATHUtil

class ATEPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    init {
        icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            /* color = */ ATHUtil.resolveColor(
                context = context,
                attr = android.R.attr.colorControlNormal
            ), /* blendModeCompat = */ BlendModeCompat.SRC_IN
        )
    }
}

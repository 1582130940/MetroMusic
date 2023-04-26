package code.name.monkey.appthemehelper.common.prefs.supportv7

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.widget.doAfterTextChanged
import androidx.preference.PreferenceViewHolder
import androidx.preference.SeekBarPreference
import code.name.monkey.appthemehelper.R
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.ATHUtil
import code.name.monkey.appthemehelper.util.TintHelper

class ATESeekBarPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = -1,
    defStyleRes: Int = -1,
) : SeekBarPreference(context, attrs, defStyleAttr, defStyleRes) {

    var unit: String = ""

    init {
        context.withStyledAttributes(
            set = attrs,
            attrs = R.styleable.ATESeekBarPreference,
            defStyleAttr = 0,
            defStyleRes = 0
        ) {
            getString(R.styleable.ATESeekBarPreference_ateKey_pref_unit)?.let {
                unit = it
            }
        }
        icon?.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            /* color = */ ATHUtil.resolveColor(
                context = context,
                attr = android.R.attr.colorControlNormal
            ), /* blendModeCompat = */ BlendModeCompat.SRC_IN
        )
    }

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        super.onBindViewHolder(view)
        val seekBar = view.findViewById(androidx.preference.R.id.seekbar) as SeekBar
        TintHelper.setTintAuto(
            /* view = */ seekBar, // Set MD3 accent if MD3 is enabled or in-app accent otherwise
            /* color = */ ThemeStore.accentColor(context), /* background = */ false
        )
        (view.findViewById(androidx.preference.R.id.seekbar_value) as TextView).apply {
            appendUnit(editableText)
            doAfterTextChanged {
                appendUnit(it)
            }
        }
    }

    private fun TextView.appendUnit(editable: Editable?) {
        if (!editable.toString().endsWith(unit)) {
            append(unit)
        }
    }
}

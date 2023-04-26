package code.name.monkey.retromusic.extensions

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import code.name.monkey.retromusic.BuildConfig
import code.name.monkey.retromusic.R
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun Fragment.materialDialog(title: Int): MaterialAlertDialogBuilder {
    return if (BuildConfig.DEBUG) {
        MaterialAlertDialogBuilder(
            /* context = */ requireContext(),
            /* overrideThemeResId = */ R.style.MaterialAlertDialogTheme
        )
    } else {
        MaterialAlertDialogBuilder(
            /* context = */ requireContext()
        )
    }.setTitle(title)
}

fun AlertDialog.colorButtons(): AlertDialog {
    setOnShowListener {
        getButton(/* whichButton = */ AlertDialog.BUTTON_POSITIVE).accentTextColor()
        getButton(/* whichButton = */ AlertDialog.BUTTON_NEGATIVE).accentTextColor()
        getButton(/* whichButton = */ AlertDialog.BUTTON_NEUTRAL).accentTextColor()
    }
    return this
}

fun Fragment.materialDialog(): MaterialDialog {
    return MaterialDialog(requireContext())
        .cornerRadius(res = R.dimen.m3_dialog_corner_size)
}

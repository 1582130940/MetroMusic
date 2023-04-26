package code.name.monkey.retromusic.activities

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import code.name.monkey.appthemehelper.util.ATHUtil.isWindowBackgroundDark
import code.name.monkey.appthemehelper.util.ColorUtil.lightenColor
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.activities.base.AbsThemeActivity
import code.name.monkey.retromusic.databinding.ActivityLicenseBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.drawAboveSystemBars
import code.name.monkey.retromusic.extensions.surfaceColor
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/** Created by hemanths on 2019-09-27.  */
class LicenseActivity : AbsThemeActivity() {
    private lateinit var binding: ActivityLicenseBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        ToolbarContentTintHelper.colorBackButton(binding.toolbar)
        try {
            val buf = StringBuilder()
            val json = assets.open(/* fileName = */ "license.html")
            BufferedReader(InputStreamReader(json, StandardCharsets.UTF_8)).use { br ->
                var str: String?
                while (br.readLine().also { str = it } != null) {
                    buf.append(str)
                }
            }

            // Inject color values for WebView body background and links
            val isDark = isWindowBackgroundDark(context = this)
            val backgroundColor =
                colorToCSS(surfaceColor(Color.parseColor(if (isDark) "#424242" else "#ffffff")))
            val contentColor = colorToCSS(Color.parseColor(if (isDark) "#ffffff" else "#000000"))
            val changeLog = buf.toString()
                .replace(
                    oldValue = "{style-placeholder}",
                    newValue = String.format(
                        "body { background-color: %s; color: %s; }",
                        backgroundColor,
                        contentColor
                    )
                )
                .replace(oldValue = "{link-color}", newValue = colorToCSS(accentColor()))
                .replace(
                    oldValue = "{link-color-active}",
                    newValue = colorToCSS(lightenColor(accentColor()))
                )
            binding.license.loadData(/* data = */ changeLog, /* mimeType = */
                "text/html", /* encoding = */
                "UTF-8"
            )
        } catch (e: Throwable) {
            binding.license.loadData(/* data = */ "<h1>Unable to load</h1><p>" + e.localizedMessage + "</p>", /* mimeType = */
                "text/html", /* encoding = */
                "UTF-8"
            )
        }
        binding.license.drawAboveSystemBars()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun colorToCSS(color: Int): String {
        return String.format(
            "rgb(%d, %d, %d)",
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        ) // on API 29, WebView doesn't load with hex colors
    }
}

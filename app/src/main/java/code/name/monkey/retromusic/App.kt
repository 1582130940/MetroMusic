package code.name.monkey.retromusic

import android.app.Application
import androidx.preference.PreferenceManager
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.appshortcuts.DynamicShortcutManager
import code.name.monkey.retromusic.helper.WallpaperAccentManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
    private val wallpaperAccentManager = WallpaperAccentManager(context = this)

    override fun onCreate() {
        super.onCreate()
        instance = this

        startKoin {
            androidContext(this@App)
            modules(appModules)
        }
        // default theme
        if (!ThemeStore.isConfigured(context = this, version = 3)) {
            ThemeStore.editTheme(context = this)
                .accentColorRes(code.name.monkey.appthemehelper.R.color.md_deep_purple_A200)
                .coloredNavigationBar(applyToNavBar = true)
                .commit()
        }
        wallpaperAccentManager.init()

        if (VersionUtils.hasNougatMR())
            DynamicShortcutManager(context = this).initDynamicShortcuts()

        // Set Default values for now playing preferences
        // This will reduce startup time for now playing settings fragment as Preference listener of AbsSlidingMusicPanelActivity won't be called
        PreferenceManager.setDefaultValues(/* context = */ this, /* resId = */
            R.xml.pref_now_playing_screen, /* readAgain = */
            false
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        wallpaperAccentManager.release()
    }

    companion object {
        private var instance: App? = null

        fun getContext(): App {
            return instance!!
        }
    }
}

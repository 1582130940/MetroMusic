package code.name.monkey.retromusic.fragments.settings

import android.os.Bundle
import code.name.monkey.retromusic.R

class ImageSettingFragment : AbsSettingsFragment() {
    override fun invalidateSettings() {}

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_images)
    }
}

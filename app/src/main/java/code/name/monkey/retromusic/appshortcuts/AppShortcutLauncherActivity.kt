package code.name.monkey.retromusic.appshortcuts

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import code.name.monkey.retromusic.appshortcuts.shortcuttype.LastAddedShortcutType
import code.name.monkey.retromusic.appshortcuts.shortcuttype.ShuffleAllShortcutType
import code.name.monkey.retromusic.appshortcuts.shortcuttype.TopTracksShortcutType
import code.name.monkey.retromusic.extensions.extraNotNull
import code.name.monkey.retromusic.model.Playlist
import code.name.monkey.retromusic.model.smartplaylist.LastAddedPlaylist
import code.name.monkey.retromusic.model.smartplaylist.ShuffleAllPlaylist
import code.name.monkey.retromusic.model.smartplaylist.TopTracksPlaylist
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_PLAY_PLAYLIST
import code.name.monkey.retromusic.service.MusicService.Companion.INTENT_EXTRA_PLAYLIST
import code.name.monkey.retromusic.service.MusicService.Companion.INTENT_EXTRA_SHUFFLE_MODE
import code.name.monkey.retromusic.service.MusicService.Companion.SHUFFLE_MODE_NONE
import code.name.monkey.retromusic.service.MusicService.Companion.SHUFFLE_MODE_SHUFFLE

class AppShortcutLauncherActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (extraNotNull(KEY_SHORTCUT_TYPE, SHORTCUT_TYPE_NONE).value) {
            SHORTCUT_TYPE_SHUFFLE_ALL -> {
                startServiceWithPlaylist(
                    shuffleMode = SHUFFLE_MODE_SHUFFLE,
                    playlist = ShuffleAllPlaylist()
                )
                DynamicShortcutManager.reportShortcutUsed(
                    context = this,
                    shortcutId = ShuffleAllShortcutType.id
                )
            }

            SHORTCUT_TYPE_TOP_TRACKS -> {
                startServiceWithPlaylist(
                    shuffleMode = SHUFFLE_MODE_NONE,
                    playlist = TopTracksPlaylist()
                )
                DynamicShortcutManager.reportShortcutUsed(
                    context = this,
                    shortcutId = TopTracksShortcutType.id
                )
            }

            SHORTCUT_TYPE_LAST_ADDED -> {
                startServiceWithPlaylist(
                    shuffleMode = SHUFFLE_MODE_NONE,
                    playlist = LastAddedPlaylist()
                )
                DynamicShortcutManager.reportShortcutUsed(
                    context = this,
                    shortcutId = LastAddedShortcutType.id
                )
            }
        }
        finish()
    }

    private fun startServiceWithPlaylist(shuffleMode: Int, playlist: Playlist) {
        val intent = Intent(/* packageContext = */ this, /* cls = */ MusicService::class.java)
        intent.action = ACTION_PLAY_PLAYLIST

        val bundle = bundleOf(
            INTENT_EXTRA_PLAYLIST to playlist,
            INTENT_EXTRA_SHUFFLE_MODE to shuffleMode
        )

        intent.putExtras(bundle)
        startService(intent)
    }

    companion object {
        const val KEY_SHORTCUT_TYPE = "com.android.metromusic.appshortcuts.ShortcutType"
        const val SHORTCUT_TYPE_SHUFFLE_ALL = 0L
        const val SHORTCUT_TYPE_TOP_TRACKS = 1L
        const val SHORTCUT_TYPE_LAST_ADDED = 2L
        const val SHORTCUT_TYPE_NONE = 4L
    }
}

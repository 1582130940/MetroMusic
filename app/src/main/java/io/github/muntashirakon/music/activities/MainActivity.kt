package io.github.muntashirakon.music.activities

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import io.github.muntashirakon.music.*
import io.github.muntashirakon.music.activities.base.AbsSlidingMusicPanelActivity
import io.github.muntashirakon.music.extensions.findNavController
import io.github.muntashirakon.music.fragments.LibraryViewModel
import io.github.muntashirakon.music.helper.MusicPlayerRemote.openAndShuffleQueue
import io.github.muntashirakon.music.helper.MusicPlayerRemote.openQueue
import io.github.muntashirakon.music.helper.MusicPlayerRemote.playFromUri
import io.github.muntashirakon.music.helper.MusicPlayerRemote.shuffleMode
import io.github.muntashirakon.music.helper.SearchQueryHelper.getSongs
import io.github.muntashirakon.music.model.Song
import io.github.muntashirakon.music.repository.PlaylistSongsLoader.getPlaylistSongList
import io.github.muntashirakon.music.repository.Repository
import io.github.muntashirakon.music.service.MusicService
import io.github.muntashirakon.music.util.AppRater.appLaunched
import io.github.muntashirakon.music.util.PreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.*

class MainActivity : AbsSlidingMusicPanelActivity(), OnSharedPreferenceChangeListener {
    companion object {
        const val TAG = "MainActivity"
        const val EXPAND_PANEL = "expand_panel"
        const val APP_UPDATE_REQUEST_CODE = 9002
    }

    private val repository by inject<Repository>()
    private val libraryViewModel by inject<LibraryViewModel>()

    private var blockRequestPermissions = false

    override fun createContentView(): View {
        return wrapSlidingMusicPanel(R.layout.activity_main_content)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setDrawUnderStatusBar()
        super.onCreate(savedInstanceState)
        setStatusbarColorAuto()
        setNavigationbarColorAuto()
        setLightNavigationBar(true)
        setTaskDescriptionColorAuto()
        hideStatusBar()
        appLaunched(this)
        addMusicServiceEventListener(libraryViewModel)
        updateTabs()
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.fragment_container).navigateUp()

    override fun onResume() {
        super.onResume()
        PreferenceUtil.registerOnSharedPreferenceChangedListener(this)
        if (intent.hasExtra(EXPAND_PANEL) &&
            intent.getBooleanExtra(EXPAND_PANEL, false) &&
            PreferenceUtil.isExpandPanel
        ) {
            expandPanel()
            intent.removeExtra(EXPAND_PANEL)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceUtil.unregisterOnSharedPreferenceChangedListener(this)
    }

    override fun requestPermissions() {
        if (!blockRequestPermissions) {
            super.requestPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!hasPermissions()) {
            requestPermissions()
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == GENERAL_THEME || key == BLACK_THEME || key == ADAPTIVE_COLOR_APP || key == USER_NAME || key == TOGGLE_FULL_SCREEN || key == TOGGLE_VOLUME || key == ROUND_CORNERS || key == CAROUSEL_EFFECT || key == NOW_PLAYING_SCREEN_ID || key == TOGGLE_GENRE || key == BANNER_IMAGE_PATH || key == PROFILE_IMAGE_PATH || key == CIRCULAR_ALBUM_ART || key == KEEP_SCREEN_ON || key == TOGGLE_SEPARATE_LINE || key == TOGGLE_HOME_BANNER || key == TOGGLE_ADD_CONTROLS || key == ALBUM_COVER_STYLE || key == HOME_ARTIST_GRID_STYLE || key == ALBUM_COVER_TRANSFORM || key == DESATURATED_COLOR || key == EXTRA_SONG_INFO || key == TAB_TEXT_MODE || key == LANGUAGE_NAME || key == LIBRARY_CATEGORIES
        ) {
            postRecreate()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        handlePlaybackIntent(intent)
    }

    private fun handlePlaybackIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val uri = intent.data
        val mimeType = intent.type
        var handled = false
        if (intent.action != null && (intent.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
        ) {
            val songs: List<Song> =
                getSongs(this, intent.extras!!)
            if (shuffleMode == MusicService.SHUFFLE_MODE_SHUFFLE) {
                openAndShuffleQueue(songs, true)
            } else {
                openQueue(songs, 0, true)
            }
            handled = true
        }
        if (uri != null && uri.toString().isNotEmpty()) {
            playFromUri(uri)
            handled = true
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE == mimeType) {
            val id = parseIdFromIntent(intent, "playlistId", "playlist").toInt()
            if (id >= 0) {
                val position = intent.getIntExtra("position", 0)
                val songs: List<Song> =
                    ArrayList(getPlaylistSongList(this, id))
                openQueue(songs, position, true)
                handled = true
            }
        } else if (MediaStore.Audio.Albums.CONTENT_TYPE == mimeType) {
            val id = parseIdFromIntent(intent, "albumId", "album").toInt()
            if (id >= 0) {
                lifecycleScope.launch(Dispatchers.Main) {
                    val position = intent.getIntExtra("position", 0)
                    openQueue(repository.albumById(id).songs!!, position, true)
                    handled = true
                }
            }
        } else if (MediaStore.Audio.Artists.CONTENT_TYPE == mimeType) {
            val id = parseIdFromIntent(intent, "artistId", "artist").toInt()
            if (id >= 0) {
                lifecycleScope.launch {
                    val position = intent.getIntExtra("position", 0)
                    openQueue(repository.artistById(id).songs, position, true)
                    handled = true
                }
            }
        }
        if (handled) {
            setIntent(Intent())
        }
    }

    private fun parseIdFromIntent(
        intent: Intent, longKey: String,
        stringKey: String
    ): Long {
        var id = intent.getLongExtra(longKey, -1)
        if (id < 0) {
            val idString = intent.getStringExtra(stringKey)
            if (idString != null) {
                try {
                    id = idString.toLong()
                } catch (e: NumberFormatException) {
                    Log.e(TAG, e.message)
                }
            }
        }
        return id
    }
}
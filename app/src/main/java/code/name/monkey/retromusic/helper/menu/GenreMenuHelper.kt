package code.name.monkey.retromusic.helper.menu

import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.dialogs.AddToPlaylistDialog
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.model.Genre
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.repository.GenreRepository
import code.name.monkey.retromusic.repository.RealRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject

object GenreMenuHelper : KoinComponent {
    private val genreRepository by inject<GenreRepository>()
    fun handleMenuClick(activity: FragmentActivity, genre: Genre, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_play -> {
                MusicPlayerRemote.openQueue(
                    queue = getGenreSongs(genre),
                    startPosition = 0,
                    startPlaying = true
                )
                return true
            }

            R.id.action_play_next -> {
                MusicPlayerRemote.playNext(getGenreSongs(genre))
                return true
            }

            R.id.action_add_to_playlist -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val playlists = get<RealRepository>().fetchPlaylists()
                    withContext(Dispatchers.Main) {
                        AddToPlaylistDialog.create(playlists, getGenreSongs(genre))
                            .show(/* manager = */ activity.supportFragmentManager, /* tag = */
                                "ADD_PLAYLIST"
                            )
                    }
                }
                return true
            }

            R.id.action_add_to_current_playing -> {
                MusicPlayerRemote.enqueue(getGenreSongs(genre))
                return true
            }
        }
        return false
    }

    private fun getGenreSongs(genre: Genre): List<Song> {
        return genreRepository.songs(genre.id)
    }
}

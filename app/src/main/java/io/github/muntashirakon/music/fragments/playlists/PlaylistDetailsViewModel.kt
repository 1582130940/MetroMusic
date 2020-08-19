package io.github.muntashirakon.music.fragments.playlists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntashirakon.music.App
import io.github.muntashirakon.music.interfaces.MusicServiceEventListener
import io.github.muntashirakon.music.model.AbsCustomPlaylist
import io.github.muntashirakon.music.model.Playlist
import io.github.muntashirakon.music.model.Song
import io.github.muntashirakon.music.repository.RealRepository
import io.github.muntashirakon.music.util.PlaylistsUtil
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistDetailsViewModel(
    private val realRepository: RealRepository,
    private var playlist: Playlist
) : ViewModel(), MusicServiceEventListener {
    private val _playListSongs = MutableLiveData<List<Song>>()
    private val _playlist = MutableLiveData<Playlist>().apply {
        postValue(playlist)
    }

    fun getPlaylist(): LiveData<Playlist> = _playlist

    fun getSongs(): LiveData<List<Song>> = _playListSongs

    init {
        loadPlaylistSongs(playlist)
    }

    private fun loadPlaylistSongs(playlist: Playlist) = viewModelScope.launch {
        val songs = realRepository.getPlaylistSongs(playlist)
        withContext(Main) { _playListSongs.postValue(songs) }
    }

    override fun onMediaStoreChanged() {
        if (playlist !is AbsCustomPlaylist) {
            // Playlist deleted
            if (!PlaylistsUtil.doesPlaylistExist(App.getContext(), playlist.id)) {
                //TODO Finish the page
                return
            }
            // Playlist renamed
            val playlistName =
                PlaylistsUtil.getNameForPlaylist(App.getContext(), playlist.id.toLong())
            if (playlistName != playlist.name) {
                viewModelScope.launch {
                    playlist = realRepository.playlist(playlist.id)
                    _playlist.postValue(playlist)
                }
            }
        }
        loadPlaylistSongs(playlist)
    }

    override fun onServiceConnected() {}
    override fun onServiceDisconnected() {}
    override fun onQueueChanged() {}
    override fun onPlayingMetaChanged() {}
    override fun onPlayStateChanged() {}
    override fun onRepeatModeChanged() {}
    override fun onShuffleModeChanged() {}
}
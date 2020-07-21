package io.github.muntashirakon.music.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntashirakon.music.adapter.HomeAdapter
import io.github.muntashirakon.music.fragments.ReloadType.*
import io.github.muntashirakon.music.interfaces.MusicServiceEventListener
import io.github.muntashirakon.music.model.*
import io.github.muntashirakon.music.providers.RepositoryImpl
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: RepositoryImpl
) : ViewModel(), MusicServiceEventListener {

    private val _albums = MutableLiveData<List<Album>>()
    private val _songs = MutableLiveData<List<Song>>()
    private val _artists = MutableLiveData<List<Artist>>()
    private val _playlist = MutableLiveData<List<Playlist>>()
    private val _genre = MutableLiveData<List<Genre>>()
    private val _homeSections = MutableLiveData<List<Home>>()

    fun homeSections(): LiveData<List<Home>> = _homeSections
    fun allAlbums(): LiveData<List<Album>> = _albums
    fun allSongs(): LiveData<List<Song>> = _songs
    fun allArtists(): LiveData<List<Artist>> = _artists
    fun allPlaylisits(): LiveData<List<Playlist>> = _playlist
    fun allGenres(): LiveData<List<Genre>> = _genre

    init {
        viewModelScope.launch {
            loadLibraryContent()
        }
    }

    private fun loadLibraryContent() = viewModelScope.launch {
        _songs.value = loadSongs.await()
        _albums.value = loadAlbums.await()
        _artists.value = loadArtists.await()
        _playlist.value = loadPlaylists.await()
        _genre.value = loadGenres.await()
        loadHomeSections()
    }

    private fun loadHomeSections() = viewModelScope.launch {
        val list = mutableListOf<Home>()
        val result = listOf(
            repository.topArtists(),
            repository.topAlbums(),
            repository.recentArtists(),
            repository.recentAlbums(),
            repository.suggestions(),
            repository.favoritePlaylist()
        )
        result.forEach {
            if (it != null && it.arrayList.isNotEmpty()) {
                if (it.homeSection == HomeAdapter.SUGGESTIONS) {
                    if (it.arrayList.size > 9) {
                        list.add(it)
                    }
                } else {
                    list.add(it)
                }
            }
        }
        _homeSections.value = list
    }

    private val loadSongs: Deferred<List<Song>>
        get() = viewModelScope.async(IO) {
            repository.allSongs()
        }

    private val loadAlbums: Deferred<List<Album>>
        get() = viewModelScope.async(IO) {
            repository.allAlbums()
        }

    private val loadArtists: Deferred<List<Artist>>
        get() = viewModelScope.async(IO) {
            repository.allArtists()
        }

    private val loadPlaylists: Deferred<List<Playlist>>
        get() = viewModelScope.async(IO) {
            repository.allPlaylists()
        }

    private val loadGenres: Deferred<List<Genre>>
        get() = viewModelScope.async(IO) {
            repository.allGenres()
        }

    fun forceReload(reloadType: ReloadType) = viewModelScope.launch {
        when (reloadType) {
            Songs -> _songs.value = loadSongs.await()
            Albums -> _albums.value = loadAlbums.await()
            Artists -> _artists.value = loadArtists.await()
            HomeSections -> _songs.value = loadSongs.await()
        }
    }

    override fun onMediaStoreChanged() {
        loadLibraryContent()
    }

    override fun onServiceConnected() {}
    override fun onServiceDisconnected() {}
    override fun onQueueChanged() {}
    override fun onPlayingMetaChanged() {}
    override fun onPlayStateChanged() {}
    override fun onRepeatModeChanged() {}
    override fun onShuffleModeChanged() {}
}

enum class ReloadType {
    Songs,
    Albums,
    Artists,
    HomeSections
}
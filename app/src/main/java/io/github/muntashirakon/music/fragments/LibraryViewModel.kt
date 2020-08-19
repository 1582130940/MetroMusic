package io.github.muntashirakon.music.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntashirakon.music.fragments.ReloadType.*
import io.github.muntashirakon.music.interfaces.MusicServiceEventListener
import io.github.muntashirakon.music.model.*
import io.github.muntashirakon.music.repository.RealRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val realRepository: RealRepository
) : ViewModel(), MusicServiceEventListener {

    private val paletteColor = MutableLiveData<Int>()
    private val albums = MutableLiveData<List<Album>>()
    private val songs = MutableLiveData<List<Song>>()
    private val artists = MutableLiveData<List<Artist>>()
    private val playlists = MutableLiveData<List<Playlist>>()
    private val genres = MutableLiveData<List<Genre>>()
    private val home = MutableLiveData<List<Home>>()

    val paletteColorLiveData: LiveData<Int> = paletteColor
    val homeLiveData: LiveData<List<Home>> = home
    val albumsLiveData: LiveData<List<Album>> = albums
    val songsLiveData: LiveData<List<Song>> = songs
    val artistsLiveData: LiveData<List<Artist>> = artists
    val playlisitsLiveData: LiveData<List<Playlist>> = playlists
    val genresLiveData: LiveData<List<Genre>> = genres

    init {
        viewModelScope.launch {
            loadLibraryContent()
        }
    }

    private fun loadLibraryContent() = viewModelScope.launch {
        songs.value = loadSongs.await()
        albums.value = loadAlbums.await()
        artists.value = loadArtists.await()
        playlists.value = loadPlaylists.await()
        genres.value = loadGenres.await()
        home.value = loadHome.await()
    }

    private val loadHome: Deferred<List<Home>>
        get() = viewModelScope.async { realRepository.homeSections() }

    private val loadSongs: Deferred<List<Song>>
        get() = viewModelScope.async(IO) { realRepository.allSongs() }

    private val loadAlbums: Deferred<List<Album>>
        get() = viewModelScope.async(IO) {
            realRepository.allAlbums()
        }

    private val loadArtists: Deferred<List<Artist>>
        get() = viewModelScope.async(IO) {
            realRepository.albumArtists()
        }

    private val loadPlaylists: Deferred<List<Playlist>>
        get() = viewModelScope.async(IO) {
            realRepository.allPlaylists()
        }

    private val loadGenres: Deferred<List<Genre>>
        get() = viewModelScope.async(IO) {
            realRepository.allGenres()
        }


    fun forceReload(reloadType: ReloadType) = viewModelScope.launch {
        when (reloadType) {
            Songs -> songs.value = loadSongs.await()
            Albums -> albums.value = loadAlbums.await()
            Artists -> artists.value = loadArtists.await()
            HomeSections -> songs.value = loadSongs.await()
        }
    }

    fun updateColor(newColor: Int) {
        paletteColor.postValue(newColor)
    }

    override fun onMediaStoreChanged() {
        loadLibraryContent()
        println("onMediaStoreChanged")
    }


    override fun onServiceConnected() {
        println("onServiceConnected")
    }

    override fun onServiceDisconnected() {
        println("onServiceDisconnected")
    }

    override fun onQueueChanged() {
        println("onQueueChanged")
    }

    override fun onPlayingMetaChanged() {
        println("onPlayingMetaChanged")
    }

    override fun onPlayStateChanged() {
        println("onPlayStateChanged")
    }

    override fun onRepeatModeChanged() {
        println("onRepeatModeChanged")
    }

    override fun onShuffleModeChanged() {
        println("onShuffleModeChanged")
    }

}

enum class ReloadType {
    Songs,
    Albums,
    Artists,
    HomeSections
}
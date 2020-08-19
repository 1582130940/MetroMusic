package io.github.muntashirakon.music.fragments.albums

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntashirakon.music.interfaces.MusicServiceEventListener
import io.github.muntashirakon.music.model.Album
import io.github.muntashirakon.music.model.Artist
import io.github.muntashirakon.music.network.model.LastFmAlbum
import io.github.muntashirakon.music.repository.RealRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AlbumDetailsViewModel(
    private val realRepository: RealRepository,
    private val albumId: Int
) : ViewModel(), MusicServiceEventListener {

    private val _album = MutableLiveData<Album>()
    private val _artist = MutableLiveData<Artist>()
    private val _lastFmAlbum = MutableLiveData<LastFmAlbum>()
    private val _moreAlbums = MutableLiveData<List<Album>>()

    fun getAlbum(): LiveData<Album> = _album
    fun getArtist(): LiveData<Artist> = _artist
    fun getAlbumInfo(): LiveData<LastFmAlbum> = _lastFmAlbum
    fun getMoreAlbums(): LiveData<List<Album>> = _moreAlbums

    init {
        loadAlbumDetails()
    }

    private fun loadAlbumDetails() = viewModelScope.launch {
        val album = loadAlbumAsync.await() ?: throw NullPointerException("Album couldn't found")
        _album.postValue(album)
    }

    fun loadAlbumInfo(album: Album) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val lastFmAlbum = realRepository.albumInfo(
                album.artistName ?: "-", album.title ?: "-"
            )
            _lastFmAlbum.postValue(lastFmAlbum)
        } catch (ignored: Exception) {}
    }

    fun loadArtist(artistId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val artist = realRepository.artistById(artistId)
        _artist.postValue(artist)

        artist.albums?.filter { item -> item.id != albumId }?.let { albums ->
            if (albums.isNotEmpty()) _moreAlbums.postValue(albums)
        }
    }

    private val loadAlbumAsync: Deferred<Album?>
        get() = viewModelScope.async(Dispatchers.IO) {
            realRepository.albumById(albumId)
        }

    override fun onMediaStoreChanged() {
        loadAlbumDetails()
    }

    override fun onServiceConnected() {}
    override fun onServiceDisconnected() {}
    override fun onQueueChanged() {}
    override fun onPlayingMetaChanged() {}
    override fun onPlayStateChanged() {}
    override fun onRepeatModeChanged() {}
    override fun onShuffleModeChanged() {}
}
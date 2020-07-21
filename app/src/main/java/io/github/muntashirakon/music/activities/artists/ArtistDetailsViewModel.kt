package io.github.muntashirakon.music.activities.artists

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.muntashirakon.music.interfaces.MusicServiceEventListener
import io.github.muntashirakon.music.model.Artist
import io.github.muntashirakon.music.providers.RepositoryImpl
import io.github.muntashirakon.music.network.model.LastFmArtist
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ArtistDetailsViewModel(
    private val repository: RepositoryImpl,
    private val artistId: Int
) : ViewModel(), MusicServiceEventListener {

    private val loadArtistDetailsAsync: Deferred<Artist?>
        get() = viewModelScope.async(Dispatchers.IO) {
            repository.artistById(artistId)
        }

    private val _artist = MutableLiveData<Artist>()
    private val _lastFmArtist = MutableLiveData<LastFmArtist>()

    fun getArtist(): LiveData<Artist> = _artist
    fun getArtistInfo(): LiveData<LastFmArtist> = _lastFmArtist

    init {
        loadArtistDetails()
    }

    private fun loadArtistDetails() = viewModelScope.launch {
        val artist =
            loadArtistDetailsAsync.await() ?: throw NullPointerException("Album couldn't found")
        _artist.postValue(artist)
    }

    fun loadBiography(name: String, lang: String?, cache: String?) = viewModelScope.launch {
        val info = repository.artistInfo(name, lang, cache)
        _lastFmArtist.postValue(info)
    }

    override fun onMediaStoreChanged() {
        loadArtistDetails()
    }

    override fun onServiceConnected() {}
    override fun onServiceDisconnected() {}
    override fun onQueueChanged() {}
    override fun onPlayingMetaChanged() {}
    override fun onPlayStateChanged() {}
    override fun onRepeatModeChanged() {}
    override fun onShuffleModeChanged() {}
}
package io.github.muntashirakon.music.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.ThemeStore
import com.bumptech.glide.Glide
import io.github.muntashirakon.music.*
import io.github.muntashirakon.music.adapter.base.MediaEntryViewHolder
import io.github.muntashirakon.music.glide.AlbumGlideRequest
import io.github.muntashirakon.music.glide.ArtistGlideRequest
import io.github.muntashirakon.music.helper.MusicPlayerRemote
import io.github.muntashirakon.music.helper.menu.SongMenuHelper
import io.github.muntashirakon.music.model.*
import io.github.muntashirakon.music.model.smartplaylist.AbsSmartPlaylist
import io.github.muntashirakon.music.repository.PlaylistSongsLoader
import io.github.muntashirakon.music.util.MusicUtil

class SearchAdapter(
    private val activity: FragmentActivity,
    private var dataSet: List<Any>
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    fun swapDataSet(dataSet: MutableList<Any>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (dataSet[position] is Album) return ALBUM
        if (dataSet[position] is Artist) return ARTIST
        if (dataSet[position] is Genre) return GENRE
        if (dataSet[position] is Playlist) return PLAYLIST
        return if (dataSet[position] is Song) SONG else HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == HEADER) ViewHolder(
            LayoutInflater.from(activity).inflate(
                R.layout.sub_header,
                parent,
                false
            ), viewType
        )
        else
            ViewHolder(
                LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false),
                viewType
            )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ALBUM -> {
                val album = dataSet.get(position) as Album
                holder.title?.text = album.title
                holder.text?.text = album.artistName
                AlbumGlideRequest.Builder.from(Glide.with(activity), album.safeGetFirstSong())
                    .checkIgnoreMediaStore(activity).build().into(holder.image)
            }
            ARTIST -> {
                val artist = dataSet.get(position) as Artist
                holder.title?.text = artist.name
                holder.text?.text = MusicUtil.getArtistInfoString(activity, artist)
                ArtistGlideRequest.Builder.from(Glide.with(activity), artist).build()
                    .into(holder.image)
            }
            SONG -> {
                val song = dataSet.get(position) as Song
                holder.title?.text = song.title
                holder.text?.text = song.albumName
            }
            GENRE -> {
                val genre = dataSet.get(position) as Genre
                holder.title?.text = genre.name
            }
            PLAYLIST -> {
                val playlist = dataSet.get(position) as Playlist
                holder.title?.text = playlist.name
                holder.text?.text = MusicUtil.getPlaylistInfoString(activity, getSongs(playlist))
            }
            else -> {
                holder.title?.text = dataSet.get(position).toString()
                holder.title?.setTextColor(ThemeStore.accentColor(activity))
            }
        }
    }

    private fun getSongs(playlist: Playlist): java.util.ArrayList<Song> {
        val songs = java.util.ArrayList<Song>()
        if (playlist is AbsSmartPlaylist) {
            songs.addAll(playlist.getSongs())
        } else {
            songs.addAll(PlaylistSongsLoader.getPlaylistSongList(activity, playlist.id))
        }
        return songs
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    inner class ViewHolder(itemView: View, itemViewType: Int) : MediaEntryViewHolder(itemView) {
        init {
            itemView.setOnLongClickListener(null)

            if (itemViewType == SONG) {
                menu?.visibility = View.VISIBLE
                menu?.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                    override val song: Song
                        get() = dataSet[layoutPosition] as Song
                })
            } else {
                menu?.visibility = View.GONE
            }

            when (itemViewType) {
                ALBUM -> setImageTransitionName(activity.getString(R.string.transition_album_art))
                ARTIST -> setImageTransitionName(activity.getString(R.string.transition_artist_image))
                else -> {
                    val container = itemView.findViewById<View>(R.id.imageContainer)
                    container?.visibility = View.GONE
                }
            }
        }

        override fun onClick(v: View?) {
            val item = dataSet[layoutPosition]
            when (itemViewType) {
                ALBUM -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.albumDetailsFragment,
                        bundleOf(EXTRA_ALBUM_ID to (item as Album).id)
                    )
                }
                ARTIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.artistDetailsFragment,
                        bundleOf(EXTRA_ARTIST_ID to (item as Artist).id)
                    )
                }
                GENRE -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.genreDetailsFragment,
                        bundleOf(EXTRA_GENRE to (item as Genre))
                    )
                }
                PLAYLIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        R.id.artistDetailsFragment,
                        bundleOf(EXTRA_PLAYLIST to (item as Playlist))
                    )
                }
                SONG -> {
                    val playList = ArrayList<Song>()
                    playList.add(item as Song)
                    MusicPlayerRemote.openQueue(playList, 0, true)
                }
            }
        }
    }

    companion object {
        private const val HEADER = 0
        private const val ALBUM = 1
        private const val ARTIST = 2
        private const val SONG = 3
        private const val GENRE = 4
        private const val PLAYLIST = 5
    }
}

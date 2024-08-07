package code.name.monkey.retromusic.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.retromusic.EXTRA_ALBUM_ID
import code.name.monkey.retromusic.EXTRA_ARTIST_ID
import code.name.monkey.retromusic.EXTRA_ARTIST_NAME
import code.name.monkey.retromusic.EXTRA_GENRE
import code.name.monkey.retromusic.EXTRA_PLAYLIST_ID
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.adapter.base.MediaEntryViewHolder
import code.name.monkey.retromusic.db.PlaylistWithSongs
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.albumCoverOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.artistImageOptions
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.menu.SongMenuHelper
import code.name.monkey.retromusic.model.Album
import code.name.monkey.retromusic.model.Artist
import code.name.monkey.retromusic.model.Genre
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil
import com.bumptech.glide.Glide
import java.util.Locale

class SearchAdapter(
    private val activity: FragmentActivity,
    private var dataSet: List<Any>,
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    @SuppressLint("NotifyDataSetChanged")
    fun swapDataSet(dataSet: List<Any>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (dataSet[position] is Album) return ALBUM
        if (dataSet[position] is Artist) return if ((dataSet[position] as Artist).isAlbumArtist) ALBUM_ARTIST else ARTIST
        if (dataSet[position] is Genre) return GENRE
        if (dataSet[position] is PlaylistWithSongs) return PLAYLIST
        return if (dataSet[position] is Song) SONG else HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            HEADER -> ViewHolder(
                itemView = LayoutInflater.from(activity).inflate(
                    /* resource = */ R.layout.sub_header,
                    /* root = */ parent,
                    /* attachToRoot = */ false
                ), itemViewType = viewType
            )

            ALBUM, ARTIST, ALBUM_ARTIST -> ViewHolder(
                itemView = LayoutInflater.from(activity).inflate(
                    /* resource = */ R.layout.item_list_big,
                    /* root = */ parent,
                    /* attachToRoot = */ false
                ), itemViewType = viewType
            )

            else -> ViewHolder(
                LayoutInflater.from(activity).inflate(R.layout.item_list, parent, false),
                viewType
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ALBUM -> {
                holder.imageTextContainer?.isVisible = true
                val album = dataSet[position] as Album
                holder.title?.text = album.title
                holder.text?.text = album.artistName
                Glide.with(activity).asDrawable().albumCoverOptions(album.safeGetFirstSong())
                    .load(RetroGlideExtension.getSongModel(album.safeGetFirstSong()))
                    .into(holder.image!!)
            }

            ARTIST -> {
                holder.imageTextContainer?.isVisible = true
                val artist = dataSet[position] as Artist
                holder.title?.text = artist.name
                holder.text?.text = MusicUtil.getArtistInfoString(activity, artist)
                Glide.with(activity).asDrawable().artistImageOptions(artist).load(
                    RetroGlideExtension.getArtistModel(artist)
                ).into(holder.image!!)
            }

            SONG -> {
                holder.imageTextContainer?.isVisible = true
                val song = dataSet[position] as Song
                holder.title?.text = song.title
                holder.text?.text = song.albumName
                Glide.with(activity).asDrawable().songCoverOptions(song)
                    .load(RetroGlideExtension.getSongModel(song)).into(holder.image!!)
            }

            GENRE -> {
                val genre = dataSet[position] as Genre
                holder.title?.text = genre.name
                holder.text?.text = String.format(
                    Locale.getDefault(),
                    format = "%d %s",
                    genre.songCount,
                    if (genre.songCount > 1) activity.getString(R.string.songs) else activity.getString(
                        R.string.song
                    )
                )
            }

            PLAYLIST -> {
                val playlist = dataSet[position] as PlaylistWithSongs
                holder.title?.text = playlist.playlistEntity.playlistName
                //holder.text?.text = MusicUtil.playlistInfoString(activity, playlist.songs)
            }

            ALBUM_ARTIST -> {
                holder.imageTextContainer?.isVisible = true
                val artist = dataSet[position] as Artist
                holder.title?.text = artist.name
                holder.text?.text = MusicUtil.getArtistInfoString(activity, artist)
                Glide.with(activity).asDrawable().artistImageOptions(artist).load(
                    RetroGlideExtension.getArtistModel(artist)
                ).into(holder.image!!)
            }

            else -> {
                holder.title?.text = dataSet[position].toString()
                holder.title?.setTextColor(ThemeStore.accentColor(activity))
            }
        }
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    inner class ViewHolder(itemView: View, itemViewType: Int) : MediaEntryViewHolder(itemView) {
        init {
            itemView.setOnLongClickListener(null)
            imageTextContainer?.isInvisible = true
            if (itemViewType == SONG) {
                imageTextContainer?.isGone = true
                menu?.isVisible = true
                menu?.setOnClickListener(object : SongMenuHelper.OnClickSongMenu(activity) {
                    override val song: Song
                        get() = dataSet[layoutPosition] as Song
                })
            } else {
                menu?.isVisible = false
            }

            when (itemViewType) {
                ALBUM -> setImageTransitionName(activity.getString(R.string.transition_album_art))
                ARTIST -> setImageTransitionName(activity.getString(R.string.transition_artist_image))
                else -> {
                    val container = itemView.findViewById<View>(R.id.imageContainer)
                    container?.isVisible = false
                }
            }
        }

        override fun onClick(v: View?) {
            val item = dataSet[layoutPosition]
            when (itemViewType) {
                ALBUM -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        resId = R.id.albumDetailsFragment,
                        args = bundleOf(EXTRA_ALBUM_ID to (item as Album).id)
                    )
                }

                ARTIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        resId = R.id.artistDetailsFragment,
                        args = bundleOf(EXTRA_ARTIST_ID to (item as Artist).id)
                    )
                }

                ALBUM_ARTIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        resId = R.id.albumArtistDetailsFragment,
                        args = bundleOf(EXTRA_ARTIST_NAME to (item as Artist).name)
                    )
                }

                GENRE -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        resId = R.id.genreDetailsFragment,
                        args = bundleOf(EXTRA_GENRE to (item as Genre))
                    )
                }

                PLAYLIST -> {
                    activity.findNavController(R.id.fragment_container).navigate(
                        resId = R.id.playlistDetailsFragment,
                        args = bundleOf(EXTRA_PLAYLIST_ID to (item as PlaylistWithSongs).playlistEntity.playListId)
                    )
                }

                SONG -> {
                    MusicPlayerRemote.playNext(item as Song)
                    MusicPlayerRemote.playNextSong()
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
        private const val ALBUM_ARTIST = 6
    }
}

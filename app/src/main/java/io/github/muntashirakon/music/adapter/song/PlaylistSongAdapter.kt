package io.github.muntashirakon.music.adapter.song

import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.google.android.material.button.MaterialButton
import io.github.muntashirakon.music.EXTRA_ALBUM_ID
import io.github.muntashirakon.music.R
import io.github.muntashirakon.music.helper.MusicPlayerRemote
import io.github.muntashirakon.music.interfaces.CabHolder
import io.github.muntashirakon.music.model.Song

open class PlaylistSongAdapter(
    activity: AppCompatActivity,
    dataSet: MutableList<Song>,
    itemLayoutRes: Int,
    cabHolder: CabHolder?
) : AbsOffsetSongAdapter(activity, dataSet, itemLayoutRes, cabHolder) {

    init {
        this.setMultiSelectMenuRes(R.menu.menu_cannot_delete_single_songs_playlist_songs_selection)
    }

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongAdapter.ViewHolder, position: Int) {
        if (holder.itemViewType == OFFSET_ITEM) {
            val viewHolder = holder as ViewHolder
            viewHolder.playAction?.let {
                it.setOnClickListener {
                    MusicPlayerRemote.openQueue(dataSet, 0, true)
                }
            }
            viewHolder.shuffleAction?.let {
                it.setOnClickListener {
                    MusicPlayerRemote.openAndShuffleQueue(dataSet, true)
                }
            }
        } else {
            super.onBindViewHolder(holder, position - 1)
        }
    }

    open inner class ViewHolder(itemView: View) : AbsOffsetSongAdapter.ViewHolder(itemView) {

        val playAction: MaterialButton? = itemView.findViewById(R.id.playAction)
        val shuffleAction: MaterialButton? = itemView.findViewById(R.id.shuffleAction)

        override var songMenuRes: Int
            get() = R.menu.menu_item_cannot_delete_single_songs_playlist_song
            set(value) {
                super.songMenuRes = value
            }

        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            if (item.itemId == R.id.action_go_to_album) {
                activity.findNavController(R.id.fragment_container)
                    .navigate(
                        R.id.albumDetailsFragment,
                        bundleOf(EXTRA_ALBUM_ID to song.albumId)
                    )
                return true
            }
            return super.onSongMenuItemClick(item)
        }
    }
}
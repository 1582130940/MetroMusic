/*
 * Copyright (c) 2019 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package io.github.muntashirakon.music.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import io.github.muntashirakon.music.EXTRA_SONG
import io.github.muntashirakon.music.R
import io.github.muntashirakon.music.extensions.colorButtons
import io.github.muntashirakon.music.extensions.extraNotNull
import io.github.muntashirakon.music.extensions.materialDialog
import io.github.muntashirakon.music.model.Song
import io.github.muntashirakon.music.repository.PlaylistRepository
import io.github.muntashirakon.music.util.PlaylistsUtil
import org.koin.android.ext.android.inject

class AddToPlaylistDialog : DialogFragment() {
    private val playlistRepository by inject<PlaylistRepository>()
    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {
        val playlists = playlistRepository.playlists()
        val playlistNames = mutableListOf<String>()
        playlistNames.add(requireContext().resources.getString(R.string.action_new_playlist))
        for (p in playlists) {
            playlistNames.add(p.name)
        }
        return materialDialog(R.string.add_playlist_title)
            .setItems(playlistNames.toTypedArray()) { _, which ->
                val songs = extraNotNull<ArrayList<Song>>(EXTRA_SONG).value
                if (which == 0) {
                    CreatePlaylistDialog.create(songs)
                        .show(requireActivity().supportFragmentManager, "ADD_TO_PLAYLIST")
                } else {
                    PlaylistsUtil.addToPlaylist(
                        requireContext(),
                        songs,
                        playlists[which - 1].id,
                        true
                    )
                }
                dismiss()
            }
            .create().colorButtons()
    }

    companion object {

        fun create(song: Song): AddToPlaylistDialog {
            val list = ArrayList<Song>()
            list.add(song)
            return create(list)
        }

        fun create(songs: List<Song>): AddToPlaylistDialog {
            val dialog = AddToPlaylistDialog()
            val args = Bundle()
            args.putParcelableArrayList(EXTRA_SONG, ArrayList(songs))
            dialog.arguments = args
            return dialog
        }
    }
}
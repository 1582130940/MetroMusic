package code.name.monkey.retromusic.dialogs

import android.app.Dialog
import android.media.MediaScannerConnection
import android.os.Bundle
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.EXTRA_PLAYLIST
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.db.PlaylistWithSongs
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.createNewFile
import code.name.monkey.retromusic.extensions.extraNotNull
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.helper.M3UWriter
import code.name.monkey.retromusic.util.PlaylistsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavePlaylistDialog : DialogFragment() {
    companion object {
        fun create(playlistWithSongs: PlaylistWithSongs): SavePlaylistDialog {
            return SavePlaylistDialog().apply {
                arguments = bundleOf(
                    EXTRA_PLAYLIST to playlistWithSongs
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val playlistWithSongs = extraNotNull<PlaylistWithSongs>(EXTRA_PLAYLIST).value

        if (VersionUtils.hasR()) {
            createNewFile(
                mimeType = "audio/mpegurl",
                fileName = playlistWithSongs.playlistEntity.playlistName
            ) { outputStream, data ->
                try {
                    if (outputStream != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            M3UWriter.writeIO(
                                outputStream = outputStream,
                                playlistWithSongs = playlistWithSongs
                            )
                            withContext(Dispatchers.Main) {
                                showToast(
                                    message = requireContext().getString(
                                        /* resId = */ R.string.saved_playlist_to,
                                        /* ...formatArgs = */ data?.lastPathSegment
                                    ),
                                    duration = Toast.LENGTH_LONG
                                )
                                dismiss()
                            }
                        }
                    }
                } catch (e: Exception) {
                    showToast(
                        message = "Something went wrong : " + e.message
                    )
                }
            }
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val file = PlaylistsUtil.savePlaylistWithSongs(playlistWithSongs)
                MediaScannerConnection.scanFile(
                    /* context = */ requireActivity(),
                    /* paths = */ arrayOf<String>(file.path),
                    /* mimeTypes = */ null
                ) { _, _ ->
                }
                withContext(Dispatchers.Main) {
                    showToast(
                        message = getString(/* resId = */ R.string.saved_playlist_to, /* ...formatArgs = */
                            file
                        ),
                        duration = Toast.LENGTH_LONG
                    )
                    dismiss()
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return materialDialog(R.string.save_playlist_title)
            .setView(R.layout.loading)
            .create().colorButtons()
    }
}

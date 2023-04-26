package code.name.monkey.retromusic.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import code.name.monkey.retromusic.EXTRA_SONG
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.ShareInstagramStory
import code.name.monkey.retromusic.extensions.colorButtons
import code.name.monkey.retromusic.extensions.materialDialog
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.MusicUtil

class SongShareDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song: Song? =
            BundleCompat.getParcelable(
                /* in = */ requireArguments(),
                /* key = */ EXTRA_SONG,
                /* clazz = */ Song::class.java
            )
        val listening: String =
            String.format(
                getString(R.string.currently_listening_to_x_by_x),
                song?.title,
                song?.artistName
            )
        return materialDialog(title = R.string.what_do_you_want_to_share)
            .setItems(
                /* items = */ arrayOf(
                    getString(/* resId = */ R.string.the_audio_file),
                    "\u201C" + listening + "\u201D",
                    getString(/* resId = */ R.string.social_stories)
                )
            ) { _, which ->
                withAction(which = which, song = song, currentlyListening = listening)
            }
            .setNegativeButton(
                /* textId = */ R.string.action_cancel,
                /* listener = */ null
            )
            .create()
            .colorButtons()
    }

    private fun withAction(
        which: Int,
        song: Song?,
        currentlyListening: String,
    ) {
        when (which) {
            0 -> {
                startActivity(
                    Intent.createChooser(
                        /* target = */
                        song?.let {
                            MusicUtil.createShareSongFileIntent(
                                requireContext(), it,
                            )
                        },
                        /* title = */ null,
                    )
                )
            }

            1 -> {
                startActivity(
                    /* intent = */ Intent.createChooser(
                        /* target = */ Intent()
                            .setAction(Intent.ACTION_SEND)
                            .putExtra(Intent.EXTRA_TEXT, currentlyListening)
                            .setType("text/plain"),
                        /* title = */ null
                    )
                )
            }

            2 -> {
                if (song != null) {
                    startActivity(
                        Intent(
                            requireContext(),
                            ShareInstagramStory::class.java
                        ).putExtra(
                            ShareInstagramStory.EXTRA_SONG,
                            song
                        )
                    )
                }
            }
        }
    }

    companion object {

        fun create(song: Song): SongShareDialog {
            return SongShareDialog().apply {
                arguments = bundleOf(
                    EXTRA_SONG to song
                )
            }
        }
    }
}

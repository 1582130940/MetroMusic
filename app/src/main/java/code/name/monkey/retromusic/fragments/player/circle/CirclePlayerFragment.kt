package code.name.monkey.retromusic.fragments.player.circle

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.Toolbar
import androidx.core.content.getSystemService
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentCirclePlayerBinding
import code.name.monkey.retromusic.extensions.accentColor
import code.name.monkey.retromusic.extensions.applyColor
import code.name.monkey.retromusic.extensions.colorControlNormal
import code.name.monkey.retromusic.extensions.drawAboveSystemBars
import code.name.monkey.retromusic.extensions.getSongInfo
import code.name.monkey.retromusic.extensions.hide
import code.name.monkey.retromusic.extensions.isColorLight
import code.name.monkey.retromusic.extensions.show
import code.name.monkey.retromusic.fragments.MusicSeekSkipTouchListener
import code.name.monkey.retromusic.fragments.base.AbsPlayerFragment
import code.name.monkey.retromusic.fragments.base.goToAlbum
import code.name.monkey.retromusic.fragments.base.goToArtist
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.simpleSongCoverOptions
import code.name.monkey.retromusic.glide.crossfadeListener
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper.Callback
import code.name.monkey.retromusic.helper.PlayPauseButtonOnClickHandler
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.color.MediaNotificationProcessor
import code.name.monkey.retromusic.volume.AudioVolumeObserver
import code.name.monkey.retromusic.volume.OnAudioVolumeChangedListener
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.google.android.material.slider.Slider
import me.tankery.lib.circularseekbar.CircularSeekBar

class CirclePlayerFragment : AbsPlayerFragment(R.layout.fragment_circle_player), Callback,
    OnAudioVolumeChangedListener,
    CircularSeekBar.OnCircularSeekBarChangeListener {

    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper
    private var audioVolumeObserver: AudioVolumeObserver? = null

    private val audioManager: AudioManager
        get() = requireContext().getSystemService()!!

    private var _binding: FragmentCirclePlayerBinding? = null
    private val binding get() = _binding!!

    private var rotateAnimator: ObjectAnimator? = null
    private var lastRequest: RequestBuilder<Drawable>? = null

    var isSeeking = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(callback = this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCirclePlayerBinding.bind(view)

        setupViews()
        binding.title.isSelected = true
        binding.title.setOnClickListener {
            goToAlbum(requireActivity())
        }
        binding.text.setOnClickListener {
            goToArtist(requireActivity())
        }
        binding.songInfo.drawAboveSystemBars()
    }

    private fun setUpPlayerToolbar() {
        binding.playerToolbar.apply {
            inflateMenu(R.menu.menu_player)
            setNavigationOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
            setOnMenuItemClickListener(this@CirclePlayerFragment)
            ToolbarContentTintHelper.colorizeToolbar(
                /* toolbarView = */ this,
                /* toolbarIconsColor = */ colorControlNormal(),
                /* activity = */ requireActivity()
            )
        }
    }

    private fun setupViews() {
        setUpProgressSlider()
        binding.volumeSeekBar.circleProgressColor = accentColor()
        binding.volumeSeekBar.circleColor = ColorUtil.withAlpha(
            baseColor = accentColor(),
            alpha = 0.25f
        )
        setUpPlayPauseFab()
        setUpPrevNext()
        setUpPlayerToolbar()
        binding.albumCoverOverlay.background = ColorDrawable(
            MaterialValueHelper.getPrimaryTextColor(
                context = requireContext(),
                dark = accentColor().isColorLight
            )
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPrevNext() {
        updatePrevNextColor()
        binding.nextButton.setOnTouchListener(
            MusicSeekSkipTouchListener(
                activity = requireActivity(),
                next = true
            )
        )
        binding.previousButton.setOnTouchListener(
            MusicSeekSkipTouchListener(
                activity = requireActivity(),
                next = false
            )
        )
    }

    private fun updatePrevNextColor() {
        val accentColor = accentColor()
        binding.nextButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
        binding.previousButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
    }

    private fun setUpPlayPauseFab() {
        TintHelper.setTintAuto(
            /* view = */ binding.playPauseButton,
            /* color = */ accentColor(),
            /* background = */ false
        )
        binding.playPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    private fun setupRotateAnimation() {
        rotateAnimator = ObjectAnimator.ofFloat(
            /* target = */ binding.albumCover,
            /* property = */ View.ROTATION,
            /* ...values = */ 360F
        ).apply {
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            duration = 10000
            if (MusicPlayerRemote.isPlaying) {
                start()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
        if (audioVolumeObserver == null) {
            audioVolumeObserver = AudioVolumeObserver(requireActivity())
        }
        audioVolumeObserver?.register(
            audioStreamType = AudioManager.STREAM_MUSIC,
            listener = this
        )

        val audioManager = audioManager
        binding.volumeSeekBar.max =
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
        binding.volumeSeekBar.progress =
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
        binding.volumeSeekBar.setOnSeekBarChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        lastRequest = null
        progressViewUpdateHelper.stop()
    }

    override fun playerToolbar(): Toolbar {
        return binding.playerToolbar
    }

    override fun onShow() {
    }

    override fun onHide() {
    }

    override fun toolbarIconColor(): Int =
        colorControlNormal()

    override val paletteColor: Int
        get() = Color.BLACK

    override fun onColorChanged(color: MediaNotificationProcessor) {
    }

    override fun onFavoriteToggled() {
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
        if (MusicPlayerRemote.isPlaying) {
            if (rotateAnimator?.isStarted == true) rotateAnimator?.resume() else rotateAnimator?.start()
        } else {
            rotateAnimator?.pause()
        }
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateSong()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateSong()
        updatePlayPauseDrawableState()
        setupRotateAnimation()
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        binding.title.text = song.title
        binding.text.text = song.artistName

        if (PreferenceUtil.isSongInfo) {
            binding.songInfo.text = getSongInfo(song)
            binding.songInfo.show()
        } else {
            binding.songInfo.hide()
        }
        Glide.with(/* fragment = */ this)
            .load(RetroGlideExtension.getSongModel(MusicPlayerRemote.currentSong))
            .simpleSongCoverOptions(MusicPlayerRemote.currentSong)
            .thumbnail(lastRequest)
            .error(Glide.with(/* fragment = */ this).load(R.drawable.default_audio_art).fitCenter())
            .fitCenter().also {
                lastRequest = it.clone()
                it.crossfadeListener()
                    .into(binding.albumCover)
            }
    }

    private fun updatePlayPauseDrawableState() {
        when {
            MusicPlayerRemote.isPlaying -> binding.playPauseButton.setImageResource(R.drawable.ic_pause)
            else -> binding.playPauseButton.setImageResource(R.drawable.ic_play_arrow)
        }
    }

    override fun onAudioVolumeChanged(currentVolume: Int, maxVolume: Int) {
        _binding?.volumeSeekBar?.max = maxVolume.toFloat()
        _binding?.volumeSeekBar?.progress = currentVolume.toFloat()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (audioVolumeObserver != null) {
            audioVolumeObserver!!.unregister()
        }
        _binding = null
    }


    override fun onProgressChanged(
        circularSeekBar: CircularSeekBar?,
        progress: Float,
        fromUser: Boolean,
    ) {
        val audioManager = audioManager
        audioManager.setStreamVolume(
            /* streamType = */ AudioManager.STREAM_MUSIC,
            /* index = */ progress.toInt(),
            /* flags = */ 0
        )
    }

    override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {
    }

    override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {
    }

    private fun setUpProgressSlider() {
        binding.progressSlider.applyColor(accentColor())
        val progressSlider = binding.progressSlider
        progressSlider.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
            if (fromUser) {
                onUpdateProgressViews(
                    progress = value.toInt(),
                    total = MusicPlayerRemote.songDurationMillis
                )
            }
        })
        progressSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isSeeking = true
                progressViewUpdateHelper.stop()
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isSeeking = false
                MusicPlayerRemote.seekTo(slider.value.toInt())
                progressViewUpdateHelper.start()
            }
        })
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        val progressSlider = binding.progressSlider
        progressSlider.valueTo = total.toFloat()

        progressSlider.valueTo = total.toFloat()

        progressSlider.value =
            progress.toFloat().coerceIn(progressSlider.valueFrom, progressSlider.valueTo)

        binding.songTotalTime.text = MusicUtil.getReadableDurationString(total.toLong())
        binding.songCurrentProgress.text = MusicUtil.getReadableDurationString(progress.toLong())
    }
}

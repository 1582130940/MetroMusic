package code.name.monkey.retromusic.activities.tageditor

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.Slide
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.ActivityAlbumTagEditorBinding
import code.name.monkey.retromusic.extensions.appHandleColor
import code.name.monkey.retromusic.extensions.defaultFooterColor
import code.name.monkey.retromusic.extensions.isColorLight
import code.name.monkey.retromusic.extensions.setTint
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.palette.BitmapPaletteWrapper
import code.name.monkey.retromusic.model.ArtworkInfo
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.util.ImageUtil
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.RetroColorUtil.generatePalette
import code.name.monkey.retromusic.util.RetroColorUtil.getColor
import code.name.monkey.retromusic.util.logD
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.shape.MaterialShapeDrawable
import org.jaudiotagger.tag.FieldKey
import java.util.EnumMap

class AlbumTagEditorActivity : AbsTagEditorActivity<ActivityAlbumTagEditorBinding>() {

    override val bindingInflater: (LayoutInflater) -> ActivityAlbumTagEditorBinding =
        ActivityAlbumTagEditorBinding::inflate

    private fun windowEnterTransition() {
        val slide = Slide()
        slide.excludeTarget(/* targetId = */ R.id.appBarLayout, /* exclude = */ true)
        slide.excludeTarget(/* targetId = */ R.id.status_bar, /* exclude = */ true)
        slide.excludeTarget(/* targetId = */ android.R.id.statusBarBackground, /* exclude = */ true)
        slide.excludeTarget(/* targetId = */ android.R.id.navigationBarBackground, /* exclude = */
            true
        )

        window.enterTransition = slide
    }

    private var albumArtBitmap: Bitmap? = null
    private var deleteAlbumArt: Boolean = false

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.appBarLayout?.statusBarForeground =
            MaterialShapeDrawable.createWithElevationOverlay(/* context = */ this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.sharedElementsUseOverlay = true
        binding.imageContainer.transitionName = getString(R.string.transition_album_art)
        windowEnterTransition()
        setUpViews()
        setupToolbar()
    }

    private fun setUpViews() {
        fillViewsWithFileTags()

        binding.yearContainer.setTint(false)
        binding.genreContainer.setTint(false)
        binding.albumTitleContainer.setTint(false)
        binding.albumArtistContainer.setTint(false)

        binding.albumText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.albumArtistText.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.genreTitle.appHandleColor().doAfterTextChanged { dataChanged() }
        binding.yearTitle.appHandleColor().doAfterTextChanged { dataChanged() }
    }

    private fun fillViewsWithFileTags() {
        binding.albumText.setText(albumTitle)
        binding.albumArtistText.setText(albumArtistName)
        binding.genreTitle.setText(genreName)
        binding.yearTitle.setText(songYear)
        logD(albumTitle + albumArtistName)
    }

    override fun loadCurrentImage() {
        val bitmap = albumArt
        setImageBitmap(
            bitmap = bitmap,
            bgColor = getColor(/* palette = */ generatePalette(/* bitmap = */ bitmap), /* fallback = */
                defaultFooterColor()
            )
        )
        deleteAlbumArt = false
    }

    private fun toastLoadingFailed() {
        showToast(R.string.could_not_download_album_cover)
    }

    override fun searchImageOnWeb() {
        searchWebFor(
            binding.albumText.text.toString(),
            binding.albumArtistText.text.toString()
        )
    }

    override fun deleteImage() {
        setImageBitmap(
            bitmap = BitmapFactory.decodeResource(
                resources,
                R.drawable.default_audio_art
            ), bgColor = defaultFooterColor()
        )
        deleteAlbumArt = true
        dataChanged()
    }

    override fun loadImageFromFile(selectedFile: Uri?) {
        Glide.with(/* activity = */ this@AlbumTagEditorActivity)
            .asBitmapPalette()
            .load(selectedFile)
            .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(/* skip = */ true)
            .into(object : ImageViewTarget<BitmapPaletteWrapper>(binding.editorImage) {
                override fun onResourceReady(
                    resource: BitmapPaletteWrapper,
                    transition: Transition<in BitmapPaletteWrapper>?,
                ) {
                    getColor(resource.palette, Color.TRANSPARENT)
                    albumArtBitmap = resource.bitmap?.let {
                        ImageUtil.resizeBitmap(/* src = */ it, /* maxForSmallerSize = */ 2048)
                    }
                    setImageBitmap(
                        bitmap = albumArtBitmap,
                        bgColor = getColor(/* palette = */ resource.palette, /* fallback = */
                            defaultFooterColor()
                        )
                    )
                    deleteAlbumArt = false
                    dataChanged()
                    setResult(Activity.RESULT_OK)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    showToast(stringRes = R.string.error_load_failed, duration = Toast.LENGTH_LONG)
                }

                override fun setResource(resource: BitmapPaletteWrapper?) {}
            })
    }

    override fun save() {
        val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
        fieldKeyValueMap[FieldKey.ALBUM] = binding.albumText.text.toString()
        // android seems not to recognize album_artist field so we additionally write the normal artist field
        fieldKeyValueMap[FieldKey.ARTIST] = binding.albumArtistText.text.toString()
        fieldKeyValueMap[FieldKey.ALBUM_ARTIST] = binding.albumArtistText.text.toString()
        fieldKeyValueMap[FieldKey.GENRE] = binding.genreTitle.text.toString()
        fieldKeyValueMap[FieldKey.YEAR] = binding.yearTitle.text.toString()

        writeValuesToFiles(
            fieldKeyValueMap,
            when {
                deleteAlbumArt -> ArtworkInfo(albumId = id, artwork = null)
                albumArtBitmap == null -> null
                else -> ArtworkInfo(id, albumArtBitmap!!)
            }
        )
    }

    override fun getSongPaths(): List<String> {
        return repository.albumById(id).songs
            .map(Song::data)
    }

    override fun getSongUris(): List<Uri> = repository.albumById(id).songs.map {
        MusicUtil.getSongFileUri(it.id)
    }

    override fun setColors(color: Int) {
        super.setColors(color)
        saveFab.backgroundTintList = ColorStateList.valueOf(color)
        saveFab.backgroundTintList = ColorStateList.valueOf(color)
        ColorStateList.valueOf(/* color = */ MaterialValueHelper.getPrimaryTextColor(
            context = this,
            dark = color.isColorLight
        )
        ).also {
            saveFab.iconTint = it
            saveFab.setTextColor(it)
        }
    }


    override val editorImage: ImageView
        get() = binding.editorImage

    companion object {

        val TAG: String = AlbumTagEditorActivity::class.java.simpleName
    }
}

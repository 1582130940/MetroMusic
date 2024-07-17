package code.name.monkey.retromusic.appwidgets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.drawable.toBitmap
import code.name.monkey.appthemehelper.util.MaterialValueHelper
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.MainActivity
import code.name.monkey.retromusic.appwidgets.base.BaseAppWidget
import code.name.monkey.retromusic.extensions.getTintedDrawable
import code.name.monkey.retromusic.glide.RetroGlideExtension
import code.name.monkey.retromusic.glide.RetroGlideExtension.asBitmapPalette
import code.name.monkey.retromusic.glide.RetroGlideExtension.songCoverOptions
import code.name.monkey.retromusic.glide.palette.BitmapPaletteWrapper
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_REWIND
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_SKIP
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_TOGGLE_PAUSE
import code.name.monkey.retromusic.util.DensityUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition

class AppWidgetMD3 : BaseAppWidget() {
    private var target: Target<BitmapPaletteWrapper>? = null // for cancellation

    /**
     * Initialize given widgets to default state, where we launch Music on default click and hide
     * actions if service not running.
     */
    override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(/* packageName = */ context.packageName, /* layoutId = */
            R.layout.app_widget_md3
        )

        appWidgetView.setViewVisibility(/* viewId = */ R.id.media_titles, /* visibility = */
            View.INVISIBLE
        )
        appWidgetView.setImageViewResource(/* viewId = */ R.id.image, /* srcId = */
            R.drawable.default_audio_art
        )
        val secondaryColor = MaterialValueHelper.getSecondaryTextColor(
            context = context,
            dark = true
        )
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_next,
            /* bitmap = */ context.getTintedDrawable(
                id = R.drawable.ic_skip_next,
                color = secondaryColor
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_prev,
            /* bitmap = */ context.getTintedDrawable(
                id = R.drawable.ic_skip_previous,
                color = secondaryColor
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_toggle_play_pause,
            /* bitmap = */ context.getTintedDrawable(
                id = R.drawable.ic_play_arrow_white_32dp,
                color = secondaryColor
            ).toBitmap()
        )

        linkButtons(context = context, views = appWidgetView)
        pushUpdate(context = context, appWidgetIds = appWidgetIds, views = appWidgetView)
    }

    /**
     * Update all active widget instances by pushing changes
     */
    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(/* packageName = */ service.packageName, /* layoutId = */
            R.layout.app_widget_md3
        )

        val isPlaying = service.isPlaying
        val song = service.currentSong

        // Set the titles and artwork
        if (song.title.isEmpty() && song.artistName.isEmpty()) {
            appWidgetView.setViewVisibility(/* viewId = */ R.id.media_titles, /* visibility = */
                View.INVISIBLE
            )
        } else {
            appWidgetView.setViewVisibility(/* viewId = */ R.id.media_titles, /* visibility = */
                View.VISIBLE
            )
            appWidgetView.setTextViewText(/* viewId = */ R.id.title, /* text = */ song.title)
            appWidgetView.setTextViewText(/* viewId = */ R.id.text, /* text = */
                getSongArtistAndAlbum(song)
            )
        }

        // Set correct drawable for pause state
        val playPauseRes =
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow_white_32dp
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_toggle_play_pause,
            /* bitmap = */ service.getTintedDrawable(
                id = playPauseRes,
                color = MaterialValueHelper.getSecondaryTextColor(context = service, dark = true)
            ).toBitmap()
        )

        // Set prev/next button drawables
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_next,
            /* bitmap = */ service.getTintedDrawable(
                id = R.drawable.ic_skip_next,
                color = MaterialValueHelper.getSecondaryTextColor(context = service, dark = true)
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_prev,
            /* bitmap = */ service.getTintedDrawable(
                id = R.drawable.ic_skip_previous,
                color = MaterialValueHelper.getSecondaryTextColor(context = service, dark = true)
            ).toBitmap()
        )

        // Link actions buttons to intents
        linkButtons(context = service, views = appWidgetView)

        if (imageSize == 0) {
            imageSize =
                service.resources.getDimensionPixelSize(/* id = */ R.dimen.app_widget_card_image_size)
        }
        if (cardRadius == 0f) {
            cardRadius =
                DensityUtil.dip2px(context = service, dpVale = 8F).toFloat()
        }

        // Load the album cover async and push the update on completion
        service.runOnUiThread {
            if (target != null) {
                Glide.with(service).clear(target)
            }
            target = Glide.with(service)
                .asBitmapPalette()
                .songCoverOptions(song)
                .load(RetroGlideExtension.getSongModel(song))
                .centerCrop()
                .into(object : CustomTarget<BitmapPaletteWrapper>(imageSize, imageSize) {
                    override fun onResourceReady(
                        resource: BitmapPaletteWrapper,
                        transition: Transition<in BitmapPaletteWrapper>?,
                    ) {
                        val palette = resource.palette
                        update(
                            resource.bitmap, palette.getVibrantColor(
                                palette.getMutedColor(
                                    MaterialValueHelper.getSecondaryTextColor(
                                        context = service, dark = true
                                    )
                                )
                            )
                        )
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        update(
                            bitmap = null,
                            color = MaterialValueHelper.getSecondaryTextColor(
                                context = service,
                                dark = true
                            )
                        )
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    private fun update(bitmap: Bitmap?, color: Int) {
                        // Set correct drawable for pause state
                        appWidgetView.setImageViewBitmap(
                            /* viewId = */ R.id.button_toggle_play_pause,
                            /* bitmap = */ service.getTintedDrawable(
                                id = playPauseRes,
                                color = color
                            ).toBitmap()
                        )

                        // Set prev/next button drawables
                        appWidgetView.setImageViewBitmap(
                            /* viewId = */ R.id.button_next,
                            /* bitmap = */
                            service.getTintedDrawable(id = R.drawable.ic_skip_next, color = color)
                                .toBitmap()
                        )
                        appWidgetView.setImageViewBitmap(
                            /* viewId = */ R.id.button_prev,
                            /* bitmap = */
                            service.getTintedDrawable(
                                id = R.drawable.ic_skip_previous,
                                color = color
                            ).toBitmap()
                        )

                        val image = getAlbumArtDrawable(service, bitmap)
                        val roundedBitmap = createRoundedBitmap(
                            drawable = image,
                            width = imageSize,
                            height = imageSize,
                            tl = cardRadius,
                            tr = cardRadius,
                            bl = cardRadius,
                            br = cardRadius
                        )
                        appWidgetView.setImageViewBitmap(/* viewId = */ R.id.image, /* bitmap = */
                            roundedBitmap
                        )

                        pushUpdate(
                            context = service,
                            appWidgetIds = appWidgetIds,
                            views = appWidgetView
                        )
                    }
                })
        }
    }

    /**
     * Link up various button actions using [PendingIntent].
     */
    private fun linkButtons(context: Context, views: RemoteViews) {
        val action = Intent(/* packageContext = */ context, /* cls = */ MainActivity::class.java)
            .putExtra(
                MainActivity.EXPAND_PANEL,
                PreferenceUtil.isExpandPanel
            )

        val serviceName = ComponentName(/* pkg = */ context, /* cls = */ MusicService::class.java)

        // Home
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var pendingIntent =
            PendingIntent.getActivity(
                /* context = */ context,
                /* requestCode = */ 0,
                /* intent = */ action,
                /* flags = */ if (VersionUtils.hasMarshmallow())
                    PendingIntent.FLAG_IMMUTABLE
                else 0
            )
        views.setOnClickPendingIntent(/* viewId = */ R.id.image, /* pendingIntent = */
            pendingIntent
        )
        views.setOnClickPendingIntent(/* viewId = */ R.id.media_titles, /* pendingIntent = */
            pendingIntent
        )

        // Previous track
        pendingIntent = buildPendingIntent(
            context = context,
            action = ACTION_REWIND,
            serviceName = serviceName
        )
        views.setOnClickPendingIntent(/* viewId = */ R.id.button_prev, /* pendingIntent = */
            pendingIntent
        )

        // Play and pause
        pendingIntent = buildPendingIntent(
            context = context,
            action = ACTION_TOGGLE_PAUSE,
            serviceName = serviceName
        )
        views.setOnClickPendingIntent(/* viewId = */ R.id.button_toggle_play_pause, /* pendingIntent = */
            pendingIntent
        )

        // Next track
        pendingIntent = buildPendingIntent(
            context = context,
            action = ACTION_SKIP,
            serviceName = serviceName
        )
        views.setOnClickPendingIntent(/* viewId = */ R.id.button_next, /* pendingIntent = */
            pendingIntent
        )
    }

    companion object {

        const val NAME = "app_widget_md3"

        private var mInstance: AppWidgetMD3? = null
        private var imageSize = 0
        private var cardRadius = 0F

        val instance: AppWidgetMD3
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetMD3()
                }
                return mInstance!!
            }
    }
}
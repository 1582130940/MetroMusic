package code.name.monkey.retromusic.appwidgets

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.activities.MainActivity
import code.name.monkey.retromusic.appwidgets.base.BaseAppWidget
import code.name.monkey.retromusic.extensions.getTintedDrawable
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_REWIND
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_SKIP
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_TOGGLE_PAUSE
import code.name.monkey.retromusic.util.PreferenceUtil

class AppWidgetText : BaseAppWidget() {
    override fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val appWidgetView = RemoteViews(/* packageName = */ context.packageName, /* layoutId = */
            R.layout.app_widget_text
        )

        appWidgetView.setImageViewBitmap(/* viewId = */ R.id.button_next, /* bitmap = */
            context.getTintedDrawable(
                id = R.drawable.ic_skip_next,
                color = ContextCompat.getColor(/* context = */ context, /* id = */
                    code.name.monkey.appthemehelper.R.color.md_white_1000
                )
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(/* viewId = */ R.id.button_prev, /* bitmap = */
            context.getTintedDrawable(
                id = R.drawable.ic_skip_previous,
                color = ContextCompat.getColor(/* context = */ context, /* id = */
                    code.name.monkey.appthemehelper.R.color.md_white_1000
                )
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(/* viewId = */ R.id.button_toggle_play_pause, /* bitmap = */
            context.getTintedDrawable(
                id = R.drawable.ic_play_arrow_white_32dp,
                color = ContextCompat.getColor(/* context = */ context, /* id = */
                    code.name.monkey.appthemehelper.R.color.md_white_1000
                )
            ).toBitmap()
        )

        appWidgetView.setTextColor(/* viewId = */ R.id.title, /* color = */
            ContextCompat.getColor(/* context = */ context, /* id = */
                code.name.monkey.appthemehelper.R.color.md_white_1000
            )
        )
        appWidgetView.setTextColor(/* viewId = */ R.id.text, /* color = */
            ContextCompat.getColor(/* context = */ context, /* id = */
                code.name.monkey.appthemehelper.R.color.md_white_1000
            )
        )

        linkButtons(context = context, views = appWidgetView)
        pushUpdate(context = context, appWidgetIds = appWidgetIds, views = appWidgetView)
    }

    /**
     * Link up various button actions using [PendingIntent].
     */
    private fun linkButtons(context: Context, views: RemoteViews) {
        val action = Intent(/* packageContext = */ context, /* cls = */ MainActivity::class.java)
            .putExtra(/* name = */ MainActivity.EXPAND_PANEL, /* value = */
                PreferenceUtil.isExpandPanel
            )

        val serviceName = ComponentName(/* pkg = */ context, /* cls = */ MusicService::class.java)

        // Home
        action.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        var pendingIntent = PendingIntent.getActivity(
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

    override fun performUpdate(service: MusicService, appWidgetIds: IntArray?) {
        val appWidgetView = RemoteViews(/* packageName = */ service.packageName, /* layoutId = */
            R.layout.app_widget_text
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
            appWidgetView.setTextViewText(/* viewId = */ R.id.text, /* text = */ song.artistName)
        }
        // Link actions buttons to intents
        linkButtons(context = service, views = appWidgetView)

        // Set correct drawable for pause state
        val playPauseRes = if (isPlaying) R.drawable.ic_pause
        else R.drawable.ic_play_arrow_white_32dp
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_toggle_play_pause,
            /* bitmap = */ service.getTintedDrawable(
                id = playPauseRes, color = ContextCompat.getColor(
                    /* context = */ service, /* id = */
                    code.name.monkey.appthemehelper.R.color.md_white_1000
                )
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_next,
            /* bitmap = */ service.getTintedDrawable(
                id = R.drawable.ic_skip_next,
                color = ContextCompat.getColor(
                    /* context = */ service,
                    /* id = */ code.name.monkey.appthemehelper.R.color.md_white_1000
                )
            ).toBitmap()
        )
        appWidgetView.setImageViewBitmap(
            /* viewId = */ R.id.button_prev,
            /* bitmap = */ service.getTintedDrawable(
                id = R.drawable.ic_skip_previous,
                color = ContextCompat.getColor(
                    /* context = */ service, /* id = */
                    code.name.monkey.appthemehelper.R.color.md_white_1000
                )
            ).toBitmap()
        )

        pushUpdate(
            context = service.applicationContext,
            appWidgetIds = appWidgetIds,
            views = appWidgetView
        )
    }

    companion object {

        const val NAME: String = "app_widget_text"

        private var mInstance: AppWidgetText? = null

        val instance: AppWidgetText
            @Synchronized get() {
                if (mInstance == null) {
                    mInstance = AppWidgetText()
                }
                return mInstance!!
            }
    }
}

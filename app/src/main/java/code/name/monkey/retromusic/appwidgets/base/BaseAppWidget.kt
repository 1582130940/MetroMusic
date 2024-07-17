package code.name.monkey.retromusic.appwidgets.base

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import code.name.monkey.appthemehelper.util.VersionUtils
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.model.Song
import code.name.monkey.retromusic.service.MusicService
import code.name.monkey.retromusic.service.MusicService.Companion.APP_WIDGET_UPDATE
import code.name.monkey.retromusic.service.MusicService.Companion.EXTRA_APP_WIDGET_NAME
import code.name.monkey.retromusic.service.MusicService.Companion.FAVORITE_STATE_CHANGED
import code.name.monkey.retromusic.service.MusicService.Companion.META_CHANGED
import code.name.monkey.retromusic.service.MusicService.Companion.PLAY_STATE_CHANGED

abstract class BaseAppWidget : AppWidgetProvider() {

    /**
     * {@inheritDoc}
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        defaultAppWidget(context, appWidgetIds)
        val updateIntent = Intent(APP_WIDGET_UPDATE)
        updateIntent.putExtra(EXTRA_APP_WIDGET_NAME, NAME)
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY)
        context.sendBroadcast(updateIntent)
    }

    /**
     * Handle a change notification coming over from [MusicService]
     */
    fun notifyChange(service: MusicService, what: String) {
        if (hasInstances(service)) {
            if (META_CHANGED == what || PLAY_STATE_CHANGED == what || FAVORITE_STATE_CHANGED == what) {
                performUpdate(service = service, appWidgetIds = null)
            }
        }
    }

    protected fun pushUpdate(
        context: Context,
        appWidgetIds: IntArray?,
        views: RemoteViews,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        if (appWidgetIds != null) {
            appWidgetManager.updateAppWidget(/* appWidgetIds = */ appWidgetIds, /* views = */ views)
        } else {
            appWidgetManager.updateAppWidget(ComponentName(context, javaClass), views)
        }
    }

    /**
     * Check against [AppWidgetManager] if there are any instances of this widget.
     */
    private fun hasInstances(context: Context): Boolean {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val mAppWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                /* pkg = */ context, /* cls = */ javaClass
            )
        )
        return mAppWidgetIds.isNotEmpty()
    }

    protected fun buildPendingIntent(
        context: Context,
        action: String,
        serviceName: ComponentName,
    ): PendingIntent {
        val intent = Intent(action)
        intent.component = serviceName
        return if (VersionUtils.hasOreo()) {
            PendingIntent.getForegroundService(
                /* context = */ context,
                /* requestCode = */ 0,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                /* context = */ context,
                /* requestCode = */ 0,
                /* intent = */ intent,
                /* flags = */ if (VersionUtils.hasMarshmallow())
                    PendingIntent.FLAG_IMMUTABLE
                else 0
            )
        }
    }

    protected abstract fun defaultAppWidget(context: Context, appWidgetIds: IntArray)

    abstract fun performUpdate(service: MusicService, appWidgetIds: IntArray?)

    protected fun getAlbumArtDrawable(context: Context, bitmap: Bitmap?): Drawable {
        return if (bitmap == null) {
            ContextCompat.getDrawable(/* context = */ context, /* id = */
                R.drawable.default_audio_art
            )!!
        } else {
            BitmapDrawable(/* res = */ context.resources, /* bitmap = */ bitmap)
        }
    }

    protected fun getSongArtistAndAlbum(song: Song): String {
        val builder = StringBuilder()
        builder.append(song.artistName)
        if (song.artistName.isNotEmpty() && song.albumName.isNotEmpty()) {
            builder.append(" • ")
        }
        builder.append(song.albumName)
        return builder.toString()
    }

    companion object {

        const val NAME: String = "app_widget"

        fun createRoundedBitmap(
            drawable: Drawable?,
            width: Int,
            height: Int,
            tl: Float,
            tr: Float,
            bl: Float,
            br: Float,
        ): Bitmap? {
            if (drawable == null) {
                return null
            }

            val bitmap = Bitmap.createBitmap(
                /* width = */ width,
                /* height = */ height,
                /* config = */ Bitmap.Config.ARGB_8888
            )
            val c = Canvas(bitmap)
            drawable.setBounds(
                /* left = */ 0,
                /* top = */ 0,
                /* right = */width,
                /* bottom = */ height
            )
            drawable.draw(/* canvas = */ c)

            val rounded = Bitmap.createBitmap(
                /* width = */ width,
                /* height = */ height,
                /* config = */ Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(rounded)
            val paint = Paint()
            paint.shader = BitmapShader(
                /* bitmap = */ bitmap,
                /* tileX = */ Shader.TileMode.CLAMP,
                /* tileY = */ Shader.TileMode.CLAMP
            )
            paint.isAntiAlias = true
            canvas.drawPath(
                composeRoundedRectPath(
                    rect = RectF(
                        /* left = */ 0f,
                        /* top = */ 0f,
                        /* right = */ width.toFloat(),
                        /* bottom = */ height.toFloat()
                    ),
                    tl = tl,
                    tr = tr,
                    bl = bl,
                    br = br
                ), paint
            )

            return rounded
        }

        protected fun composeRoundedRectPath(
            rect: RectF,
            tl: Float,
            tr: Float,
            bl: Float,
            br: Float,
        ): Path {
            val path = Path()
            path.moveTo(/* x = */ rect.left + tl, /* y = */ rect.top)
            path.lineTo(/* x = */ rect.right - tr, /* y = */ rect.top)
            path.quadTo(
                /* x1 = */ rect.right,
                /* y1 = */ rect.top,
                /* x2 = */ rect.right,
                /* y2 = */rect.top + tr
            )
            path.lineTo(/* x = */ rect.right, /* y = */ rect.bottom - br)
            path.quadTo(
                /* x1 = */ rect.right,
                /* y1 = */ rect.bottom,
                /* x2 = */ rect.right - br,
                /* y2 = */rect.bottom
            )
            path.lineTo(/* x = */ rect.left + bl, /* y = */ rect.bottom)
            path.quadTo(
                /* x1 = */ rect.left,
                /* y1 = */ rect.bottom,
                /* x2 = */ rect.left,
                /* y2 = */ rect.bottom - bl
            )
            path.lineTo(/* x = */ rect.left, /* y = */ rect.top + tl)
            path.quadTo(
                /* x1 = */ rect.left,
                /* y1 = */ rect.top,
                /* x2 = */ rect.left + tl,
                /* y2 = */ rect.top
            )
            path.close()

            return path
        }
    }
}

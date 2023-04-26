package code.name.monkey.retromusic.service

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.BundleCompat
import androidx.media.session.MediaButtonReceiver
import code.name.monkey.retromusic.BuildConfig
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_PAUSE
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_PLAY
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_REWIND
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_SKIP
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_STOP
import code.name.monkey.retromusic.service.MusicService.Companion.ACTION_TOGGLE_PAUSE

/**
 * Used to control headset playback.
 * Single press: pause/resume
 * Double press: actionNext track
 * Triple press: previous track
 */
class MediaButtonIntentReceiver : MediaButtonReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (DEBUG) Log.v(/* tag = */ TAG, /* msg = */ "Received intent: $intent")
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

    companion object {
        val TAG: String = MediaButtonIntentReceiver::class.java.simpleName
        private val DEBUG = BuildConfig.DEBUG
        private const val MSG_HEADSET_DOUBLE_CLICK_TIMEOUT = 2

        private const val DOUBLE_CLICK = 400

        private var wakeLock: WakeLock? = null
        private var mClickCounter = 0
        private var mLastClickTime: Long = 0

        private val mHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_HEADSET_DOUBLE_CLICK_TIMEOUT -> {
                        val clickCount = msg.arg1

                        if (DEBUG) Log.v(/* tag = */ TAG, /* msg = */
                            "Handling headset click, count = $clickCount"
                        )
                        val command = when (clickCount) {
                            1 -> ACTION_TOGGLE_PAUSE
                            2 -> ACTION_SKIP
                            3 -> ACTION_REWIND
                            else -> null
                        }

                        if (command != null) {
                            val context = msg.obj as Context
                            startService(context, command)
                        }
                    }
                }
                releaseWakeLockIfHandlerIdle()
            }
        }

        fun handleIntent(context: Context, intent: Intent): Boolean {
            println("Intent Action: ${intent.action}")
            val intentAction = intent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                val event = intent.extras?.let {
                    BundleCompat.getParcelable(/* in = */ it, /* key = */
                        Intent.EXTRA_KEY_EVENT, /* clazz = */
                        KeyEvent::class.java
                    )
                }
                    ?: return false

                val keycode = event.keyCode
                val action = event.action
                val eventTime = if (event.eventTime != 0L)
                    event.eventTime
                else
                    System.currentTimeMillis()

                var command: String? = null
                when (keycode) {
                    KeyEvent.KEYCODE_MEDIA_STOP -> command = ACTION_STOP
                    KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> command =
                        ACTION_TOGGLE_PAUSE

                    KeyEvent.KEYCODE_MEDIA_NEXT -> command = ACTION_SKIP
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> command = ACTION_REWIND
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> command = ACTION_PAUSE
                    KeyEvent.KEYCODE_MEDIA_PLAY -> command = ACTION_PLAY
                }
                if (command != null) {
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (event.repeatCount == 0) {
                            // Only consider the first event in a sequence, not the repeat events,
                            // so that we don't trigger in cases where the first event went to
                            // a different app (e.g. when the user ends a phone call by
                            // long pressing the headset button)

                            // The service may or may not be running, but we need to send it
                            // a command.
                            if (keycode == KeyEvent.KEYCODE_HEADSETHOOK || keycode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                                if (eventTime - mLastClickTime >= DOUBLE_CLICK) {
                                    mClickCounter = 0
                                }

                                mClickCounter++
                                if (DEBUG) Log.v(/* tag = */ TAG, /* msg = */
                                    "Got headset click, count = $mClickCounter"
                                )
                                mHandler.removeMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)

                                val msg = mHandler.obtainMessage(
                                    /* what = */ MSG_HEADSET_DOUBLE_CLICK_TIMEOUT,
                                    /* arg1 = */ mClickCounter,
                                    /* arg2 = */ 0,
                                    /* obj = */ context
                                )

                                val delay = (if (mClickCounter < 3) DOUBLE_CLICK else 0).toLong()
                                if (mClickCounter >= 3) {
                                    mClickCounter = 0
                                }
                                mLastClickTime = eventTime
                                acquireWakeLockAndSendMessage(context, msg, delay)
                            } else {
                                startService(context, command)
                            }
                            return true
                        }
                    }
                }
            }
            return false
        }

        private fun startService(context: Context, command: String?) {
            val intent = Intent(context, MusicService::class.java)
            intent.action = command
            try {
                context.startService(intent)
            } catch (e: Exception) {
                ContextCompat.startForegroundService(context, intent)
            }
        }

        private fun acquireWakeLockAndSendMessage(context: Context, msg: Message, delay: Long) {
            if (wakeLock == null) {
                val appContext = context.applicationContext
                val pm = appContext.getSystemService<PowerManager>()
                wakeLock = pm?.newWakeLock(
                    /* levelAndFlags = */ PowerManager.PARTIAL_WAKE_LOCK,
                    /* tag = */ "RetroMusicApp:Wakelock headset button"
                )
                wakeLock!!.setReferenceCounted(false)
            }
            if (DEBUG) Log.v(/* tag = */ TAG, /* msg = */
                "Acquiring wake lock and sending " + msg.what
            )
            // Make sure we don't indefinitely hold the wake lock under any circumstances
            wakeLock!!.acquire(/* timeout = */ 10000)

            mHandler.sendMessageDelayed(msg, delay)
        }

        private fun releaseWakeLockIfHandlerIdle() {
            if (mHandler.hasMessages(MSG_HEADSET_DOUBLE_CLICK_TIMEOUT)) {
                if (DEBUG) Log.v(/* tag = */ TAG, /* msg = */
                    "Handler still has messages pending, not releasing wake lock"
                )
                return
            }

            if (wakeLock != null) {
                if (DEBUG) Log.v(/* tag = */ TAG, /* msg = */ "Releasing wake lock")
                wakeLock!!.release()
                wakeLock = null
            }
        }
    }
}

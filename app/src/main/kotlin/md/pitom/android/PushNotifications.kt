package md.pitom.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * The data-payload -> notification mapping for PitoMessagingService. Data-only
 * FCM messages arrive as a plain `Map<String, String>` with keys `message`,
 * `level`, and an optional `title`; `message` feeds the notification body and
 * `title` (when present) feeds the notification title — `level` rides along
 * in the payload but has no native styling yet — reserved for the web/server
 * side, or a future severity treatment here).
 *
 * Pulled out of the Service so the mapping is plain-JVM testable without a
 * live FirebaseMessagingService instance (matching AsciiLogo/Instance: the
 * Android glue stays thin, the logic is a pure, tested layer underneath it).
 */
object PushNotifications {
    const val CHANNEL_ID = "pito"
    const val NOTIFICATION_ID = 1
    private const val DATA_KEY_MESSAGE = "message"
    private const val DATA_KEY_TITLE = "title"

    fun channel(context: Context): NotificationChannel = NotificationChannel(
        CHANNEL_ID,
        context.getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT,
    )

    /** Pure extraction: a missing or blank `message` means no notification. */
    fun contentText(data: Map<String, String>): String? =
        data[DATA_KEY_MESSAGE]?.takeIf { it.isNotBlank() }

    /** Pure extraction: a missing or blank `title` means the caller falls
     *  back to the app name — the server omits `title` entirely when a
     *  notification has none. */
    fun contentTitle(data: Map<String, String>): String? =
        data[DATA_KEY_TITLE]?.takeIf { it.isNotBlank() }

    /** Null when the payload carries no usable `message` — the caller posts
     *  nothing in that case. */
    fun build(context: Context, data: Map<String, String>, contentIntent: PendingIntent): Notification? {
        val text = contentText(data) ?: return null
        val title = contentTitle(data) ?: context.getString(R.string.app_name)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.pito_blue))
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
    }
}

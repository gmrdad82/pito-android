package md.pitom.android

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Receives PITO's push notifications. Messages are DATA-ONLY (never a
 * FirebaseMessagingService "notification" payload) so delivery is identical
 * whether the app is foregrounded, backgrounded, or killed — this is the one
 * codepath that builds and posts the system notification. The actual
 * data -> Notification mapping lives in PushNotifications (plain-JVM
 * testable); this class is the thin Android glue around it, same split as
 * Instance/MainActivity.
 */
class PitoMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val notification = PushNotifications.build(this, remoteMessage.data, contentIntent()) ?: return
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(PushNotifications.channel(this))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // Nothing native can do here: permission requests need an
            // Activity, and this fires from a background service. The
            // bridge's "register" flow is the only place that asks.
            return
        }
        manager.notify(PushNotifications.NOTIFICATION_ID, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PendingPushToken.save(this, token)
    }

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )
}

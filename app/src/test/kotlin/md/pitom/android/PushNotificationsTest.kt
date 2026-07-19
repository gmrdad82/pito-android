package md.pitom.android

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushNotificationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val contentIntent: PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    // --- contentText (pure) --------------------------------------------------

    @Test
    fun `contentText reads the message key`() {
        assertThat(PushNotifications.contentText(mapOf("message" to "New comment on your video")))
            .isEqualTo("New comment on your video")
    }

    @Test
    fun `contentText is null when the message key is missing`() {
        assertThat(PushNotifications.contentText(mapOf("level" to "info"))).isNull()
    }

    @Test
    fun `contentText is null when the message key is blank`() {
        assertThat(PushNotifications.contentText(mapOf("message" to "   "))).isNull()
    }

    @Test
    fun `contentText ignores unrelated keys like level`() {
        assertThat(PushNotifications.contentText(mapOf("message" to "hi", "level" to "warning")))
            .isEqualTo("hi")
    }

    // --- channel ---------------------------------------------------------------

    @Test
    fun `channel carries the fixed pito id and default importance`() {
        val channel = PushNotifications.channel(context)
        assertThat(channel.id).isEqualTo("pito")
        assertThat(channel.name).isEqualTo("PITO")
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
    }

    // --- build -------------------------------------------------------------

    @Test
    fun `build returns null when there is no usable message`() {
        assertThat(PushNotifications.build(context, emptyMap(), contentIntent)).isNull()
    }

    @Test
    fun `build sets the content text, icon, accent color, and content intent`() {
        val notification = PushNotifications.build(
            context,
            mapOf("message" to "Your upload finished processing", "level" to "info"),
            contentIntent,
        )

        assertThat(notification).isNotNull()
        assertThat(notification!!.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString())
            .isEqualTo("Your upload finished processing")
        assertThat(notification.icon).isEqualTo(R.drawable.ic_notification)
        assertThat(notification.color).isEqualTo(ContextCompat.getColor(context, R.color.pito_blue))
        assertThat(notification.contentIntent).isEqualTo(contentIntent)
    }

    @Test
    fun `build sets the content title from data when present`() {
        val notification = PushNotifications.build(
            context,
            mapOf("message" to "Your upload finished processing", "title" to "Upload complete"),
            contentIntent,
        )

        assertThat(notification).isNotNull()
        assertThat(notification!!.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString())
            .isEqualTo("Upload complete")
    }

    @Test
    fun `build falls back to the app name when data carries no title`() {
        val notification = PushNotifications.build(
            context,
            mapOf("message" to "Your upload finished processing"),
            contentIntent,
        )

        assertThat(notification).isNotNull()
        assertThat(notification!!.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString())
            .isEqualTo("PITO")
    }
}

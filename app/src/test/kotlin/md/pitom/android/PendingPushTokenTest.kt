package md.pitom.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingPushTokenTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearPrefs() {
        context.getSharedPreferences("pito", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun `peek is null before anything is saved`() {
        assertThat(PendingPushToken.peek(context)).isNull()
    }

    @Test
    fun `save persists the token for peek`() {
        PendingPushToken.save(context, "abc123token")
        assertThat(PendingPushToken.peek(context)).isEqualTo("abc123token")
    }

    @Test
    fun `a later save overwrites the earlier token`() {
        PendingPushToken.save(context, "first-token")
        PendingPushToken.save(context, "rotated-token")
        assertThat(PendingPushToken.peek(context)).isEqualTo("rotated-token")
    }

    @Test
    fun `token storage shares the pito prefs file with Instance`() {
        PendingPushToken.save(context, "shared-file-token")
        val raw = context.getSharedPreferences("pito", Context.MODE_PRIVATE)
            .getString("pending_push_token", null)
        assertThat(raw).isEqualTo("shared-file-token")
    }
}

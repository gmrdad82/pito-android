package md.pitom.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import md.pitom.android.Instance.SaveResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InstanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun clearPrefs() {
        context.getSharedPreferences("pito", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    // --- normalize ---------------------------------------------------------

    @Test
    fun `normalize strips trailing slashes`() {
        assertThat(Instance.normalize("https://pito.example.com/"))
            .isEqualTo("https://pito.example.com")
        assertThat(Instance.normalize("https://pito.example.com///"))
            .isEqualTo("https://pito.example.com")
    }

    @Test
    fun `normalize assumes https when scheme is missing`() {
        assertThat(Instance.normalize("pito.example.com"))
            .isEqualTo("https://pito.example.com")
    }

    @Test
    fun `normalize trims whitespace`() {
        assertThat(Instance.normalize("  https://pito.example.com  "))
            .isEqualTo("https://pito.example.com")
    }

    @Test
    fun `normalize keeps ports and paths`() {
        assertThat(Instance.normalize("https://pito.example.com:8443"))
            .isEqualTo("https://pito.example.com:8443")
    }

    @Test
    fun `normalize rejects garbage`() {
        assertThat(Instance.normalize("")).isNull()
        assertThat(Instance.normalize("   ")).isNull()
        assertThat(Instance.normalize("ftp://pito.example.com")).isNull()
        assertThat(Instance.normalize("https://")).isNull()
        assertThat(Instance.normalize("https:///")).isNull()
        assertThat(Instance.normalize("not a url at all")).isNull()
    }

    @Test
    fun `normalize lowercases the scheme`() {
        // Also load-bearing for security: validate() gates cleartext via
        // startsWith("http://"), which an uppercase scheme must not bypass.
        assertThat(Instance.normalize("HTTPS://pito.example.com"))
            .isEqualTo("https://pito.example.com")
        assertThat(Instance.validate("HTTP://pito.example.com", allowHttp = false))
            .isEqualTo(SaveResult.HTTPS_REQUIRED)
    }

    // --- validate: both build types' rules from one variant ----------------

    @Test
    fun `validate rejects http when cleartext is not allowed (release rule)`() {
        assertThat(Instance.validate("http://192.168.1.10:3000", allowHttp = false))
            .isEqualTo(SaveResult.HTTPS_REQUIRED)
    }

    @Test
    fun `validate accepts http when cleartext is allowed (debug rule)`() {
        assertThat(Instance.validate("http://10.0.2.2:3000", allowHttp = true))
            .isEqualTo(SaveResult.OK)
    }

    @Test
    fun `validate accepts https under both rules`() {
        assertThat(Instance.validate("https://pito.example.com", allowHttp = false))
            .isEqualTo(SaveResult.OK)
        assertThat(Instance.validate("https://pito.example.com", allowHttp = true))
            .isEqualTo(SaveResult.OK)
    }

    @Test
    fun `validate flags invalid input as INVALID not HTTPS_REQUIRED`() {
        assertThat(Instance.validate("ftp://x", allowHttp = false))
            .isEqualTo(SaveResult.INVALID)
    }

    // --- persistence --------------------------------------------------------

    @Test
    fun `urlOrNull is null before anything is saved`() {
        assertThat(Instance.urlOrNull(context)).isNull()
    }

    @Test
    fun `save persists the normalized url`() {
        val result = Instance.save(context, "pito.example.com/")
        assertThat(result).isEqualTo(SaveResult.OK)
        assertThat(Instance.urlOrNull(context)).isEqualTo("https://pito.example.com")
    }

    @Test
    fun `failed save leaves the stored url untouched`() {
        Instance.save(context, "https://pito.example.com")
        Instance.save(context, "ftp://nope")
        assertThat(Instance.urlOrNull(context)).isEqualTo("https://pito.example.com")
    }

    // --- prefill -------------------------------------------------------------

    @Test
    fun `prefill falls back to the build-type default when nothing is stored`() {
        // Asserted against BuildConfig, NOT a hardcoded host: release ships ""
        // (empty field + hint), debug ships the owner's dev instance.
        assertThat(Instance.prefill(context)).isEqualTo(BuildConfig.DEFAULT_INSTANCE_URL)
    }

    @Test
    fun `prefill prefers the stored url`() {
        Instance.save(context, "https://my.own.host")
        assertThat(Instance.prefill(context)).isEqualTo("https://my.own.host")
    }

    // --- resume (last location) ---------------------------------------------

    @Test
    fun `startLocation is null before an instance is saved`() {
        assertThat(Instance.startLocation(context)).isNull()
    }

    @Test
    fun `startLocation falls back to the instance root without a resume point`() {
        Instance.save(context, "https://pito.example.com")
        assertThat(Instance.startLocation(context)).isEqualTo("https://pito.example.com")
    }

    @Test
    fun `startLocation resumes the last same-origin location`() {
        Instance.save(context, "https://pito.example.com")
        Instance.saveLastLocation(context, "https://pito.example.com/conversations/42")
        assertThat(Instance.startLocation(context))
            .isEqualTo("https://pito.example.com/conversations/42")
    }

    @Test
    fun `cross-origin locations are never kept as resume points`() {
        Instance.save(context, "https://pito.example.com")
        Instance.saveLastLocation(context, "https://accounts.google.com/o/oauth2")
        Instance.saveLastLocation(context, "http://pito.example.com/downgraded")
        Instance.saveLastLocation(context, "https://pito.example.com:8443/other-port")
        assertThat(Instance.startLocation(context)).isEqualTo("https://pito.example.com")
    }

    @Test
    fun `switching instances clears the resume point`() {
        Instance.save(context, "https://pito.example.com")
        Instance.saveLastLocation(context, "https://pito.example.com/conversations/42")
        Instance.save(context, "https://other.example.com")
        assertThat(Instance.startLocation(context)).isEqualTo("https://other.example.com")
    }

    @Test
    fun `re-saving the same instance keeps the resume point`() {
        Instance.save(context, "https://pito.example.com")
        Instance.saveLastLocation(context, "https://pito.example.com/conversations/42")
        Instance.save(context, "https://pito.example.com/")  // normalizes to same
        assertThat(Instance.startLocation(context))
            .isEqualTo("https://pito.example.com/conversations/42")
    }
}

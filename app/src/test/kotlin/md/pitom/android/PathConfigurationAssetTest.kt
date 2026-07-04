package md.pitom.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfiguration
import dev.hotwire.core.turbo.config.presentation
import dev.hotwire.core.turbo.config.pullToRefreshEnabled
import dev.hotwire.core.turbo.config.uri
import dev.hotwire.core.turbo.nav.Presentation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Validates the BUNDLED path configuration through the library's real parser
 * — the same code path that runs on device. If the JSON shape drifts from
 * what hotwire-native-android expects, this breaks here, not on phones.
 */
@RunWith(RobolectricTestRunner::class)
class PathConfigurationAssetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun loadBundledAssetOnly() {
        // Asset-only location: no remote fetch, no network in unit tests.
        Hotwire.loadPathConfiguration(
            context = context,
            location = PathConfiguration.Location(
                assetFilePath = "json/path-configuration.json",
            ),
        )
    }

    @Test
    fun `every route resolves to the web fragment`() {
        val properties = Hotwire.config.pathConfiguration
            .properties("https://pito.example.com/whatever/deep/path")
        assertThat(properties.uri.toString()).isEqualTo("hotwire://fragment/web")
    }

    @Test
    fun `pull to refresh is disabled (fights the chat ui)`() {
        val properties = Hotwire.config.pathConfiguration
            .properties("https://pito.example.com/anything")
        assertThat(properties.pullToRefreshEnabled).isFalse()
    }

    @Test
    fun `root gets clear_all presentation`() {
        val properties = Hotwire.config.pathConfiguration
            .properties("https://pito.example.com/")
        assertThat(properties.presentation).isEqualTo(Presentation.CLEAR_ALL)
    }
}

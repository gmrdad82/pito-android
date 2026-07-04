package md.pitom.android

import dev.hotwire.core.config.Hotwire
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric boots the real PitoApplication (from the merged manifest), so
 * these assert the app's actual startup configuration.
 */
@RunWith(RobolectricTestRunner::class)
class PitoApplicationTest {

    @Test
    fun `user agent carries the PITO prefix`() {
        assertThat(Hotwire.config.applicationUserAgentPrefix)
            .startsWith("PITO; v")
    }

    @Test
    fun `user agent identifies as Hotwire Native for Rails UA detection`() {
        // Rails detects the app via request.user_agent =~ /Hotwire Native/.
        assertThat(Hotwire.config.userAgent).contains("Hotwire Native")
        assertThat(Hotwire.config.userAgent).startsWith("PITO; v")
    }

    @Test
    fun `webview allows programmatic audio playback (chat sounds)`() {
        // The web app plays send/receive/notify mp3s with no user gesture —
        // the custom WebView factory must disable the autoplay restriction.
        val context = androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>()
        val webView = Hotwire.config.makeCustomWebView(context)
        org.junit.Assert.assertFalse(webView.settings.mediaPlaybackRequiresUserGesture)
    }

    @Test
    fun `google interception is registered ahead of the built-in handlers`() {
        // The library's Router keeps its list private, so the app's own
        // registration list (the routing table) is the testable invariant.
        val handlers = routeDecisionHandlers
        assertThat(handlers.first()).isInstanceOf(GoogleAuthRouteDecisionHandler::class.java)
        // Registration REPLACES the defaults — all three built-ins must be
        // re-listed, app navigation before browser tab before system.
        assertThat(handlers.map { it.javaClass.simpleName }).containsExactly(
            "GoogleAuthRouteDecisionHandler",
            "AppNavigationRouteDecisionHandler",
            "BrowserTabRouteDecisionHandler",
            "SystemNavigationRouteDecisionHandler",
        )
    }
}

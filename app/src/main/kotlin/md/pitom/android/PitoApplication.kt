package md.pitom.android

import android.app.Application
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.bridge.KotlinXJsonConverter
import dev.hotwire.core.turbo.webview.HotwireWebView
import dev.hotwire.navigation.config.defaultFragmentDestination
import dev.hotwire.navigation.config.registerFragmentDestinations
import dev.hotwire.navigation.config.registerRouteDecisionHandlers
import dev.hotwire.navigation.fragments.HotwireWebBottomSheetFragment
import dev.hotwire.navigation.fragments.HotwireWebFragment
import dev.hotwire.navigation.routing.AppNavigationRouteDecisionHandler
import dev.hotwire.navigation.routing.BrowserTabRouteDecisionHandler
import dev.hotwire.navigation.routing.SystemNavigationRouteDecisionHandler

// First match wins — this order IS the routing table: Google notice, then
// same-instance in-app, then other http(s) hosts in a Custom Tab, then
// everything else (mailto:, tel:, ...) to the system. Top-level so tests can
// assert the order (the library's Router keeps its list private).
internal val routeDecisionHandlers = listOf(
    GoogleAuthRouteDecisionHandler(),
    AppNavigationRouteDecisionHandler(),
    BrowserTabRouteDecisionHandler(),
    SystemNavigationRouteDecisionHandler(),
)

class PitoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Hotwire.config.debugLoggingEnabled = BuildConfig.DEBUG
        Hotwire.config.webViewDebuggingEnabled = BuildConfig.DEBUG
        Hotwire.config.jsonConverter = KotlinXJsonConverter()
        // The library appends "Hotwire Native Android; ..." — the Rails side
        // detects the app via request.user_agent =~ /Hotwire Native/.
        Hotwire.config.applicationUserAgentPrefix = "PITO; v${BuildConfig.VERSION_NAME};"

        // PITO plays short mp3s (send/receive/notify) from a Stimulus
        // controller with no user gesture; Chromium's autoplay policy would
        // mute them until the first tap. The library's sanctioned hook for
        // WebView settings is this factory.
        Hotwire.config.makeCustomWebView = { context ->
            HotwireWebView(context).apply {
                settings.mediaPlaybackRequiresUserGesture = false
                // WebView defaults textZoom to the SYSTEM font-size setting,
                // silently scaling every glyph — pito's braille charts are
                // sized in exact 14px cells, so any scale >100 overflows
                // their columns (the "APK charts overshoot" bug). Browsers
                // don't apply the OS scale this way; pin the WebView to
                // match them.
                settings.textZoom = 100
            }
        }

        // PitoWebFragment = stock web fragment with the native toolbar hidden
        // (the web app draws its own chrome).
        Hotwire.defaultFragmentDestination = PitoWebFragment::class
        Hotwire.registerFragmentDestinations(
            PitoWebFragment::class,
            HotwireWebBottomSheetFragment::class,
        )

        // registerRouteDecisionHandlers REPLACES the default router, so the
        // three built-ins are re-listed (see routeDecisionHandlers above).
        Hotwire.registerRouteDecisionHandlers(*routeDecisionHandlers.toTypedArray())

        Instance.configureHotwire(this)
    }
}

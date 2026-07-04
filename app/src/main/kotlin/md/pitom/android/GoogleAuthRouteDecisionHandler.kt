package md.pitom.android

import android.net.Uri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.routing.Router

/**
 * Google blocks OAuth inside embedded WebViews (disallowed_useragent), and
 * splitting the flow into a Custom Tab breaks the session-cookie PKCE state,
 * so the shell intercepts accounts.google.com entirely: show a native notice
 * and cancel. Reconnecting from any regular browser works — the connection is
 * server-side and the app picks it up immediately.
 */
class GoogleAuthRouteDecisionHandler : Router.RouteDecisionHandler {
    override val name = "google-auth-notice"

    override fun matches(location: String, configuration: NavigatorConfiguration): Boolean =
        Uri.parse(location).host == "accounts.google.com"

    override fun handle(
        location: String,
        configuration: NavigatorConfiguration,
        activity: HotwireActivity,
    ): Router.Decision {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.google_reconnect_title)
            .setMessage(R.string.google_reconnect_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
        return Router.Decision.CANCEL
    }
}

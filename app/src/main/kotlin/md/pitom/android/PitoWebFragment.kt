package md.pitom.android

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireWebFragment

/**
 * The default web destination, minus ALL native chrome: PITO's terminal UI is
 * full-screen and draws its own — a native app bar would just eat a strip of
 * screen. Nulling the toolbar isn't enough (the library layout's AppBarLayout
 * still reserves the status-bar inset via fitsSystemWindows — the "black band"),
 * so the whole app_bar container goes GONE and the web view takes the full
 * height, edge to edge.
 *
 * Pull-DOWN does NOTHING (owner-locked): the chat layout's one refresh gesture
 * is PITO's own bottom pull-up, which lives inside the page as plain JS. The
 * library's native swipe-to-refresh — a cold-boot reload that unloaded the
 * document into a dead black frame — is disabled outright: no listener, no
 * spinner, inert glass. The error screen's pull keeps the library reload (the
 * document there is already dead; a real reboot is the correct medicine), and
 * the neon logo stays what it was always meant to be: app-boot chrome.
 */
@HotwireDestinationDeepLink(uri = "hotwire://fragment/web")
class PitoWebFragment : HotwireWebFragment() {
    override fun toolbarForNavigation(): Toolbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(dev.hotwire.navigation.R.id.app_bar)?.visibility = View.GONE
        view.findViewById<SwipeRefreshLayout>(dev.hotwire.navigation.R.id.hotwire_webView_container)
            ?.isEnabled = false
    }
}

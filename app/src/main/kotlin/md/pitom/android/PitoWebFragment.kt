package md.pitom.android

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireWebFragment

/**
 * The default web destination, minus ALL native chrome: PITO's terminal UI is
 * full-screen and draws its own — a native app bar would just eat a strip of
 * screen. Nulling the toolbar isn't enough (the library layout's AppBarLayout
 * still reserves the status-bar inset via fitsSystemWindows — the "black band"),
 * so the whole app_bar container goes GONE and the web view takes the full
 * height, edge to edge.
 */
@HotwireDestinationDeepLink(uri = "hotwire://fragment/web")
class PitoWebFragment : HotwireWebFragment() {
    override fun toolbarForNavigation(): Toolbar? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(dev.hotwire.navigation.R.id.app_bar)?.visibility = View.GONE
    }
}

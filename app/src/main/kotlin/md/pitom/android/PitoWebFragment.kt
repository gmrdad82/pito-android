package md.pitom.android

import androidx.appcompat.widget.Toolbar
import dev.hotwire.navigation.destinations.HotwireDestinationDeepLink
import dev.hotwire.navigation.fragments.HotwireWebFragment

/**
 * The default web destination, minus the native toolbar: PITO's terminal UI
 * is full-screen and draws its own chrome — a native app bar showing the
 * page <title> would just eat a strip of screen.
 */
@HotwireDestinationDeepLink(uri = "hotwire://fragment/web")
class PitoWebFragment : HotwireWebFragment() {
    override fun toolbarForNavigation(): Toolbar? = null
}

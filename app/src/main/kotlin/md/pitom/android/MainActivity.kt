package md.pitom.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dev.hotwire.navigation.activities.HotwireActivity
import dev.hotwire.navigation.navigator.NavigatorConfiguration
import dev.hotwire.navigation.util.applyDefaultImeWindowInsets

class MainActivity : HotwireActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // OnboardingActivity gates on a stored URL, but guard anyway (e.g.
        // app data cleared while a task record survives).
        if (Instance.urlOrNull(this) == null) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // Web content stops below the status bar (black strip above): the
        // page's own top chrome (Esc, list actions) must stay tappable, and
        // touches inside the status-bar zone belong to the system. True
        // glass under-flow needs the WEB to inset its fixed chrome — logged
        // as a cross-repo contract with pito (shell would inject the inset,
        // pito renders a backdrop-filter header). statusBars ONLY — exactly
        // the clock/battery row; the cutout inset can report taller and left
        // a needlessly deep strip (owner 2026-07-05).
        val root = findViewById<View>(R.id.main_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            view.updatePadding(top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            insets
        }
        findViewById<View>(R.id.main_nav_host).applyDefaultImeWindowInsets()
    }

    override fun onPause() {
        super.onPause()
        // Resume point for the next cold start — otherwise every app open
        // lands on the start screen and spawns a fresh conversation.
        Instance.saveLastLocation(this, delegate.currentNavigator?.location)
    }

    override fun navigatorConfigurations() = listOf(
        NavigatorConfiguration(
            name = "main",
            // The fallback host is never rendered: onCreate finishes first.
            startLocation = Instance.startLocation(this) ?: "https://instance.invalid",
            navigatorHostId = R.id.main_nav_host,
        )
    )
}

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

        // Web content stops at the status bar (black strip above), because
        // the web app can't read Android's inset; bottom/IME stays with the
        // library's handler on the nav host.
        val root = findViewById<View>(R.id.main_root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            // statusBars ∪ displayCutout: the punch-hole/notch region can
            // exceed the status bar (esp. landscape) — pad past both.
            val top = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            ).top
            view.updatePadding(top = top)
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

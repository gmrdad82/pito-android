package md.pitom.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
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
        findViewById<View>(R.id.main_nav_host).applyDefaultImeWindowInsets()
    }

    override fun navigatorConfigurations() = listOf(
        NavigatorConfiguration(
            name = "main",
            // The fallback host is never rendered: onCreate finishes first.
            startLocation = Instance.urlOrNull(this) ?: "https://instance.invalid",
            navigatorHostId = R.id.main_nav_host,
        )
    )
}

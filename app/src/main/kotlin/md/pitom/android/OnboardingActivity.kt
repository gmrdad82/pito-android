package md.pitom.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * LAUNCHER activity and the app's only native screen: connect the shell to a
 * PITO instance. Shows the form on first run (no URL stored) or when opened
 * via the "Server" launcher shortcut; otherwise forwards straight to
 * MainActivity without rendering.
 */
class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishServerShortcut()

        val settingsMode = intent.action == ACTION_SETTINGS
        if (!settingsMode && Instance.urlOrNull(this) != null) {
            startMain()
            return
        }

        setContentView(R.layout.activity_onboarding)
        val urlField = findViewById<EditText>(R.id.instance_url)
        val errorView = findViewById<TextView>(R.id.error)
        urlField.setText(Instance.prefill(this))

        findViewById<Button>(R.id.connect).setOnClickListener {
            when (Instance.save(this, urlField.text.toString())) {
                Instance.SaveResult.OK -> {
                    Instance.configureHotwire(applicationContext)
                    startMain()
                }
                Instance.SaveResult.HTTPS_REQUIRED -> showError(errorView, R.string.error_https_required)
                Instance.SaveResult.INVALID -> showError(errorView, R.string.error_invalid_url)
            }
        }
    }

    private fun showError(view: TextView, message: Int) {
        view.setText(message)
        view.visibility = View.VISIBLE
    }

    private fun startMain() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    /** Long-press launcher icon -> "Server" reopens this form. Dynamic (not
     *  static XML) so it works under the .debug applicationId suffix too. */
    private fun publishServerShortcut() {
        val intent = Intent(this, OnboardingActivity::class.java).setAction(ACTION_SETTINGS)
        val shortcut = ShortcutInfoCompat.Builder(this, "server")
            .setShortLabel(getString(R.string.shortcut_server))
            .setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(intent)
            .build()
        ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
    }

    companion object {
        const val ACTION_SETTINGS = "md.pitom.android.SETTINGS"
    }
}

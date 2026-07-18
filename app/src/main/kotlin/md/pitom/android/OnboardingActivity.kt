package md.pitom.android

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
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
 *
 * Forwarding (see [startMain]) has two flavors: the fast-forward path
 * (already configured) resumes MainActivity's existing task warm — no
 * teardown, so the neon boot chrome doesn't replay on every launcher-icon
 * tap; the connect-button path tears the task down, because a newly saved
 * instance URL means the old WebView is now stale and must not survive.
 */
class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishServerShortcut()

        val settingsMode = intent.action == ACTION_SETTINGS
        if (!settingsMode && Instance.urlOrNull(this) != null) {
            // Fast-forward: resume the warm MainActivity task, don't tear it
            // down (see startMain kdoc).
            startMain(clearTask = false)
            return
        }

        setContentView(R.layout.activity_onboarding)
        val urlField = findViewById<EditText>(R.id.instance_url)
        val errorView = findViewById<TextView>(R.id.error)
        val connectButton = findViewById<Button>(R.id.connect)
        urlField.setText(Instance.prefill(this))

        connectButton.setOnClickListener {
            when (Instance.save(this, urlField.text.toString())) {
                Instance.SaveResult.OK -> {
                    Instance.configureHotwire(applicationContext)
                    // A URL was just saved (first run or "change server"):
                    // the old WebView, if any, points at the old server and
                    // must die, not resume.
                    startMain(clearTask = true)
                }
                Instance.SaveResult.HTTPS_REQUIRED -> showError(errorView, R.string.error_https_required)
                Instance.SaveResult.INVALID -> showError(errorView, R.string.error_invalid_url)
            }
        }

        // Enter / the keyboard's Go key submits like tapping Connect.
        urlField.setOnEditorActionListener { _, actionId, event ->
            val enterDown = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN
            if (actionId == EditorInfo.IME_ACTION_GO || enterDown) {
                connectButton.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun showError(view: TextView, message: Int) {
        view.setText(message)
        view.visibility = View.VISIBLE
    }

    /**
     * [clearTask] false (fast-forward): a plain intent — MainActivity's
     * singleTask launchMode brings the existing warm instance to the front
     * instead of recreating it. [clearTask] true (connect-button path):
     * the full NEW_TASK|CLEAR_TASK teardown, because switching instances
     * leaves the old WebView pointed at the old server.
     */
    private fun startMain(clearTask: Boolean) {
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(mainActivityFlags(clearTask))
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

        /**
         * Pure flag decision for [startMain], pulled out so it's
         * JVM-testable without an Activity or Robolectric. `clearTask =
         * false` (resume) carries no flags: MainActivity's singleTask
         * launchMode alone brings the existing instance forward. `clearTask
         * = true` (replace) carries NEW_TASK|CLEAR_TASK: the prior task,
         * WebView included, is destroyed rather than resumed.
         */
        fun mainActivityFlags(clearTask: Boolean): Int =
            if (clearTask) {
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            } else {
                0
            }
    }
}

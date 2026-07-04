package md.pitom.android

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        renderNeonLogo()
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

    /** Broken-neon treatment of the web start screen's block logo: crisp
     *  two-tone art over a blurred glow layer; tube-ignition flicker on
     *  entry, then a slow breathing glow. Animators respect the system
     *  animation scale (reduced motion → static glow). */
    private fun renderNeonLogo() {
        val blue = ContextCompat.getColor(this, R.color.pito_blue)
        val dim = ContextCompat.getColor(this, R.color.hint)

        val crisp = findViewById<TextView>(R.id.logo_text)
        crisp.text = AsciiLogo.spannable(blockColor = blue, frameColor = dim)

        val glow = findViewById<TextView>(R.id.logo_glow)
        glow.text = AsciiLogo.text
        glow.setShadowLayer(14f, 0f, 0f, blue)

        // Ignition: stuttering alpha like a neon tube catching.
        ObjectAnimator.ofFloat(crisp, View.ALPHA, 0f, 1f, 0.35f, 1f, 0.65f, 1f).apply {
            duration = 900
            start()
        }
        // Afterglow: gentle infinite breathe on the glow layer.
        ObjectAnimator.ofFloat(glow, View.ALPHA, 0.25f, 0.6f).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            start()
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

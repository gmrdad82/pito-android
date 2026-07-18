package md.pitom.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dev.hotwire.core.bridge.BridgeComponent
import dev.hotwire.core.bridge.BridgeDelegate
import dev.hotwire.core.bridge.Message
import dev.hotwire.navigation.destinations.HotwireDestination
import kotlinx.serialization.Serializable

/**
 * Drives FCM push registration for the web's Stimulus "push-registration"
 * controller. BRIDGE CONTRACT (fixed — the web side is built against this
 * verbatim, never change component name / message name / reply data key
 * without updating it in lockstep):
 *   - web sends "register" (no data) when its controller connects;
 *   - native ensures POST_NOTIFICATIONS (Android 13+), fetches the current
 *     FCM token, and replies to the SAME message with `{"token": "<token>"}`;
 *   - on ANY snag along the way — permission denied, no Firebase project
 *     configured (a pushless fork build), or the token fetch itself failing —
 *     native replies with NOTHING. The web never learns why; it just never
 *     sees a reply. Native never talks to the server or touches cookies — the
 *     web does the authenticated POST once (if ever) it has the token.
 *
 * The library's own `HotwireDestination.activityPermissionResultLauncher()`
 * hook exists for this, but it requires a launcher registered by the
 * destination Fragment BEFORE it reaches STARTED — "register" arrives long
 * after that (the web sends it once its Stimulus controller connects, well
 * into the page lifecycle). Registering directly against the Activity's raw
 * `ActivityResultRegistry` has no such timing restriction (the tradeoff is
 * manual `unregister()`, done in the callback below) and needs nothing from
 * PitoWebFragment/MainActivity.
 */
class PushRegistrationComponent(
    name: String,
    private val delegate: BridgeDelegate<HotwireDestination>,
) : BridgeComponent<HotwireDestination>(name, delegate) {

    override fun onReceive(message: Message) {
        if (message.event != EVENT_REGISTER) return

        val context = delegate.destination.fragment.context ?: return
        // Pushless fork build: no google-services.json means no FirebaseApp
        // was ever initialized. FirebaseMessaging.getInstance() throws on an
        // uninitialized app — bail before even asking for a permission that
        // could never be put to use.
        if (FirebaseApp.getApps(context).isEmpty()) return

        ensureNotificationPermission(message) { granted ->
            if (granted) fetchTokenAndReply(message)
        }
    }

    private fun ensureNotificationPermission(message: Message, onResult: (Boolean) -> Unit) {
        val activity = delegate.destination.fragment.activity ?: return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult(true)
            return
        }
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            onResult(true)
            return
        }

        // The registry's register() silently overwrites an existing entry
        // for the same key (no exception, no queueing) — a fixed key would
        // let a second concurrent "register" (a fast page-to-page bounce,
        // each with its own BridgeComponent instance sharing this Activity's
        // single registry) clobber the first flow's callback and strand its
        // launcher. message.id is unique per dispatch, so each in-flight
        // request gets its own key.
        var launcher: ActivityResultLauncher<String>? = null
        launcher = activity.activityResultRegistry.register(
            "$name-post-notifications-${message.id}",
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            launcher?.unregister()
            onResult(granted)
        }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun fetchTokenAndReply(message: Message) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = task.result?.takeIf { task.isSuccessful && it.isNotEmpty() }
            if (token != null) replyTo(message.event, TokenReplyData(token))
        }
    }

    @Serializable
    data class TokenReplyData(val token: String)

    companion object {
        const val NAME = "push-registration"
        private const val EVENT_REGISTER = "register"
    }
}

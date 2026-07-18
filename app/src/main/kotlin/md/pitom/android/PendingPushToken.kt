package md.pitom.android

import android.content.Context

/**
 * FCM calls `onNewToken()` whenever it likes — including while no PITO page
 * is loaded to carry it over the bridge (cold start, token rotation while
 * backgrounded). This is just a landing spot for that value: no network call
 * happens from here. The web's push-registration Stimulus controller resends
 * "register" on every page connect, and that flow always re-fetches the
 * CURRENT token from the FCM SDK (already updated by the time a new "register"
 * arrives) and does the authenticated POST itself — so nothing in this repo
 * needs to read this value back out today. It exists so the token is never
 * silently dropped, and so a future native surface (e.g. a debug screen) has
 * somewhere to look.
 */
object PendingPushToken {
    private const val PREFS = "pito"
    private const val KEY_TOKEN = "pending_push_token"

    fun save(context: Context, token: String) {
        prefs(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun peek(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

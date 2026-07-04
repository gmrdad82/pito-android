package md.pitom.android

import android.content.Context
import android.net.Uri
import dev.hotwire.core.config.Hotwire
import dev.hotwire.core.turbo.config.PathConfiguration

/**
 * The stored instance URL is the app's single point of configuration.
 * There is NO shipped default: release builds are instance-agnostic and the
 * app is unusable until the user connects it to their own PITO host.
 */
object Instance {
    private const val PREFS = "pito"
    private const val KEY_URL = "instance_url"
    private const val KEY_LAST_LOCATION = "last_location"
    private const val PATH_CONFIG_ASSET = "json/path-configuration.json"
    const val PATH_CONFIG_REMOTE_PATH = "/configurations/android_v1.json"

    enum class SaveResult { OK, INVALID, HTTPS_REQUIRED }

    fun urlOrNull(context: Context): String? =
        prefs(context).getString(KEY_URL, null)

    /** Onboarding field prefill: the saved URL, else the build-type default
     *  ("" in release — the field stays empty with a hint). */
    fun prefill(context: Context): String =
        urlOrNull(context) ?: BuildConfig.DEFAULT_INSTANCE_URL

    /** Pure validation, parameterized so unit tests cover both build types'
     *  rules from one variant. */
    fun validate(raw: String, allowHttp: Boolean = BuildConfig.ALLOW_HTTP): SaveResult {
        val normalized = normalize(raw) ?: return SaveResult.INVALID
        if (normalized.startsWith("http://") && !allowHttp) return SaveResult.HTTPS_REQUIRED
        return SaveResult.OK
    }

    fun save(context: Context, raw: String): SaveResult {
        val result = validate(raw)
        if (result == SaveResult.OK) {
            val normalized = normalize(raw)
            val editor = prefs(context).edit().putString(KEY_URL, normalized)
            // A different instance means the old resume point is meaningless.
            if (normalized != urlOrNull(context)) editor.remove(KEY_LAST_LOCATION)
            editor.apply()
        }
        return result
    }

    /** Remember where the user was, so a cold start resumes there instead of
     *  the start screen (which would spawn a fresh conversation every open).
     *  Only same-origin locations are kept — external URLs never become a
     *  start location. */
    fun saveLastLocation(context: Context, location: String?) {
        val instance = urlOrNull(context) ?: return
        if (location != null && sameOrigin(location, instance)) {
            prefs(context).edit().putString(KEY_LAST_LOCATION, location).apply()
        }
    }

    /** Where MainActivity starts: the last visited page on the CURRENT
     *  instance, else the instance root. Null until an instance is saved. */
    fun startLocation(context: Context): String? {
        val instance = urlOrNull(context) ?: return null
        val last = prefs(context).getString(KEY_LAST_LOCATION, null)
        return if (last != null && sameOrigin(last, instance)) last else instance
    }

    private fun sameOrigin(a: String, b: String): Boolean {
        val ua = Uri.parse(a)
        val ub = Uri.parse(b)
        return ua.scheme == ub.scheme && ua.host == ub.host && ua.port == ub.port
    }

    // Hostname sanity: labels of letters/digits/dots/hyphens (covers domains,
    // IPv4, punycode; not IPv6 literals — acceptable for v1).
    private val HOST_PATTERN = Regex("^[a-zA-Z0-9]([a-zA-Z0-9.-]*[a-zA-Z0-9])?$")

    /** Trim, assume https when no scheme is given, lowercase the scheme,
     *  strip trailing slashes. Returns null when the input can't be a usable
     *  http(s) base URL. */
    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
        val uri = Uri.parse(withScheme)
        // Lowercase, or "HTTP://x" would slip past both this check and the
        // startsWith("http://") cleartext gate in validate().
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return null
        val host = uri.host
        if (host.isNullOrBlank() || !HOST_PATTERN.matches(host)) return null
        return "$scheme://${withScheme.substringAfter("://")}".trimEnd('/')
    }

    /**
     * (Re)point Hotwire's path configuration at the current instance.
     * Load order is bundled asset -> cached remote -> fresh remote; a missing
     * or 404ing remote endpoint is ignored, so ANY instance works even before
     * it serves /configurations/android_v1.json.
     */
    fun configureHotwire(context: Context) {
        val url = urlOrNull(context)
        val location = if (url == null) {
            PathConfiguration.Location(assetFilePath = PATH_CONFIG_ASSET)
        } else {
            PathConfiguration.Location(
                assetFilePath = PATH_CONFIG_ASSET,
                remoteFileUrl = url + PATH_CONFIG_REMOTE_PATH,
            )
        }
        Hotwire.loadPathConfiguration(context, location)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

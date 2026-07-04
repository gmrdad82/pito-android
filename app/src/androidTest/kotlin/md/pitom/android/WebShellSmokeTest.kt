package md.pitom.android

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end shell smoke WITHOUT any real backend: an on-device MockWebServer
 * plays a minimal instance at http://localhost:<port> (the debug
 * network-security config permits localhost cleartext), and the real
 * MainActivity/WebView stack renders it.
 */
@RunWith(AndroidJUnit4::class)
class WebShellSmokeTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var server: MockWebServer

    private val page = """
        <!DOCTYPE html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>pito test instance</title>
          </head>
          <body><h1 id="hello">hello from the fake instance</h1></body>
        </html>
    """.trimIndent()

    @Before
    fun startFakeInstance() {
        server = MockWebServer()
        server.start()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/html; charset=utf-8")
                .setBody(page)
        )
        context.getSharedPreferences("pito", Context.MODE_PRIVATE)
            .edit()
            .putString("instance_url", server.url("/").toString().trimEnd('/'))
            .commit()
    }

    @After
    fun stopFakeInstance() {
        server.shutdown()
    }

    @Test
    fun webViewRendersTheInstancePage() {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent)

        onWebView()
            .withElement(findElement(Locator.ID, "hello"))
            .check(webMatches(getText(), containsString("hello from the fake instance")))
    }
}

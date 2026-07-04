package md.pitom.android

import dev.hotwire.navigation.navigator.NavigatorConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleAuthRouteDecisionHandlerTest {
    private val handler = GoogleAuthRouteDecisionHandler()
    private val configuration = NavigatorConfiguration(
        name = "test",
        startLocation = "https://pito.example.com",
        navigatorHostId = 0,
    )

    private fun matches(location: String) = handler.matches(location, configuration)

    @Test
    fun `matches google accounts host`() {
        assertThat(matches("https://accounts.google.com/o/oauth2/v2/auth?x=1")).isTrue()
        assertThat(matches("https://accounts.google.com")).isTrue()
    }

    @Test
    fun `does not match the instance itself`() {
        assertThat(matches("https://pito.example.com/connect")).isFalse()
    }

    @Test
    fun `does not match other google hosts`() {
        assertThat(matches("https://google.com")).isFalse()
        assertThat(matches("https://www.google.com/search?q=x")).isFalse()
        assertThat(matches("https://myaccount.google.com")).isFalse()
    }

    @Test
    fun `does not match lookalike hosts`() {
        // Host-suffix tricks must not trigger (nor bypass) the interception.
        assertThat(matches("https://accounts.google.com.evil.tld/phish")).isFalse()
        assertThat(matches("https://notaccounts.google.com")).isFalse()
    }

    @Test
    fun `does not match non-http schemes`() {
        assertThat(matches("mailto:someone@accounts.google.com")).isFalse()
    }
}

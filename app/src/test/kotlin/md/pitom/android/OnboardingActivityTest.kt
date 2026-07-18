package md.pitom.android

import android.content.Intent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OnboardingActivityTest {

    // --- mainActivityFlags ---------------------------------------------------

    @Test
    fun `resume mode carries no flags`() {
        assertThat(OnboardingActivity.mainActivityFlags(clearTask = false)).isEqualTo(0)
    }

    @Test
    fun `resume mode does not carry the clear-task bit`() {
        val flags = OnboardingActivity.mainActivityFlags(clearTask = false)
        assertThat(flags and Intent.FLAG_ACTIVITY_CLEAR_TASK).isEqualTo(0)
    }

    @Test
    fun `replace mode carries both new-task and clear-task bits`() {
        val flags = OnboardingActivity.mainActivityFlags(clearTask = true)
        assertThat(flags and Intent.FLAG_ACTIVITY_NEW_TASK).isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
        assertThat(flags and Intent.FLAG_ACTIVITY_CLEAR_TASK).isEqualTo(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }

    @Test
    fun `replace mode is exactly new-task or clear-task, nothing extra`() {
        assertThat(OnboardingActivity.mainActivityFlags(clearTask = true))
            .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}

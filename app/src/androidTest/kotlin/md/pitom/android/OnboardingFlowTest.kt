package md.pitom.android

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun clearStoredUrl() {
        context.getSharedPreferences("pito", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun firstLaunchShowsTheForm() {
        ActivityScenario.launch(OnboardingActivity::class.java)
        onView(withId(R.id.instance_url)).check(matches(isDisplayed()))
        onView(withId(R.id.connect)).check(matches(isDisplayed()))
    }

    @Test
    fun formIsPrefilledWithTheBuildDefault() {
        ActivityScenario.launch(OnboardingActivity::class.java)
        onView(withId(R.id.instance_url))
            .check(matches(withText(BuildConfig.DEFAULT_INSTANCE_URL)))
    }

    @Test
    fun invalidInputShowsAnInlineError() {
        ActivityScenario.launch(OnboardingActivity::class.java)
        onView(withId(R.id.instance_url))
            .perform(replaceText("ftp://nope"), closeSoftKeyboard())
        onView(withId(R.id.connect)).perform(click())
        onView(withId(R.id.error)).check(matches(isDisplayed()))
    }

    @Test
    fun settingsActionReopensTheFormEvenWhenConfigured() {
        Instance.save(context, "https://pito.example.com")
        val intent = Intent(context, OnboardingActivity::class.java)
            .setAction(OnboardingActivity.ACTION_SETTINGS)
        ActivityScenario.launch<OnboardingActivity>(intent)
        onView(withId(R.id.instance_url)).check(matches(isDisplayed()))
        // Existing URL is shown for editing, not wiped.
        onView(withId(R.id.instance_url))
            .check(matches(withText("https://pito.example.com")))
    }
}

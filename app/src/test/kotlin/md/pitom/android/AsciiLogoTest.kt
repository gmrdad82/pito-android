package md.pitom.android

import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AsciiLogoTest {

    @Test
    fun `art is a rectangular block matching the web component`() {
        assertThat(AsciiLogo.LINES).hasSize(6)
        // Fixed-width art: every row the same length, or the columns shear.
        assertThat(AsciiLogo.LINES.map { it.length }.distinct()).hasSize(1)
        assertThat(AsciiLogo.text).contains("█").contains("╚")
    }

    @Test
    fun `blocks get the accent color and the frame gets the dim color`() {
        val blue = Color.rgb(0x51, 0x70, 0xFF)
        val dim = Color.GRAY
        val spanned = AsciiLogo.spannable(blockColor = blue, frameColor = dim) as SpannableString

        val spans = spanned.getSpans(0, spanned.length, ForegroundColorSpan::class.java)
        assertThat(spans).isNotEmpty()
        spans.forEach { span ->
            val ch = spanned[spanned.getSpanStart(span)]
            val expected = if (ch == '█') blue else dim
            assertThat(span.foregroundColor).isEqualTo(expected)
        }
        // Every visible glyph is colored — nothing falls through to default.
        val visible = AsciiLogo.text.count { it != ' ' && it != '\n' }
        assertThat(spans.size).isEqualTo(visible)
    }
}

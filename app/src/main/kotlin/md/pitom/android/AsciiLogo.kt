package md.pitom.android

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan

/**
 * The PITO block-art, verbatim from the web app's start screen
 * (pito: app/components/pito/start_screen/component.rb LOGO_LINES).
 * `█` glyphs render pito-blue; the box-drawing frame renders dim —
 * same two-tone rule as the web component.
 */
object AsciiLogo {
    val LINES = listOf(
        "██████╗ ██╗████████╗ ██████╗ ",
        "██╔══██╗██║╚══██╔══╝██╔═══██╗",
        "██████╔╝██║   ██║   ██║   ██║",
        "██╔═══╝ ██║   ██║   ██║   ██║",
        "██║     ██║   ██║   ╚██████╔╝",
        "╚═╝     ╚═╝   ╚═╝    ╚═════╝ ",
    )

    val text: String = LINES.joinToString("\n")

    fun spannable(blockColor: Int, frameColor: Int): CharSequence {
        val spannable = SpannableString(text)
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch != ' ' && ch != '\n') {
                val color = if (ch == '█') blockColor else frameColor
                spannable.setSpan(
                    ForegroundColorSpan(color), i, i + 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            i++
        }
        return spannable
    }
}

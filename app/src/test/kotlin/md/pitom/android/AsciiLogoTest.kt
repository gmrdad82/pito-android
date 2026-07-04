package md.pitom.android

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AsciiLogoTest {

    @Test
    fun `art is a rectangular block matching the web component`() {
        assertThat(AsciiLogo.LINES).hasSize(6)
        // Fixed-width art: every row the same length, or the columns shear.
        assertThat(AsciiLogo.LINES.map { it.length }.distinct()).hasSize(1)
        assertThat(AsciiLogo.cols).isEqualTo(AsciiLogo.LINES[0].length)
        assertThat(AsciiLogo.rows).isEqualTo(6)
    }

    @Test
    fun `cells cover every visible glyph and classify blocks vs frame`() {
        val cells = AsciiLogo.cells()
        val visible = AsciiLogo.LINES.sumOf { line -> line.count { it != ' ' } }
        assertThat(cells).hasSize(visible)

        val blocks = cells.filter { it.isBlock }
        val frame = cells.filterNot { it.isBlock }
        assertThat(blocks).isNotEmpty()
        assertThat(frame).isNotEmpty()
        // The frame is exactly the box-drawing set the view knows how to draw.
        assertThat(frame.map { it.char }.distinct())
            .containsExactlyInAnyOrder('╗', '╔', '╝', '╚', '═', '║')
        // Cells carry valid coordinates.
        assertThat(cells).allSatisfy { cell ->
            assertThat(cell.row).isBetween(0, AsciiLogo.rows - 1)
            assertThat(cell.col).isBetween(0, AsciiLogo.cols - 1)
            assertThat(AsciiLogo.LINES[cell.row][cell.col]).isEqualTo(cell.char)
        }
    }
}

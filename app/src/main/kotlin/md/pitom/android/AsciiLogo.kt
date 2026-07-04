package md.pitom.android

/**
 * The PITO block-art, verbatim from the web app's start screen
 * (pito: app/components/pito/start_screen/component.rb LOGO_LINES).
 * `█` cells render pito-blue and solid; the box-drawing frame renders as
 * thin dim strokes — same two-tone rule as the web component. NeonLogoView
 * draws the cells; this object is the pure, testable data layer.
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

    val rows = LINES.size
    val cols = LINES[0].length

    data class Cell(val row: Int, val col: Int, val char: Char) {
        val isBlock: Boolean get() = char == '█'
    }

    /** Every visible glyph as a cell — blocks and frame alike flicker
     *  individually, exactly like the web's per-glyph spans. */
    fun cells(): List<Cell> = buildList {
        LINES.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch ->
                if (ch != ' ') add(Cell(row, col, ch))
            }
        }
    }
}

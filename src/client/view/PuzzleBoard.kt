package client.view

import client.Client
import java.awt.Color
import java.awt.GridLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * This class represent a puzzle board. This class paint puzzle and his tiles.
 *
 * @param rows, puzzle rows
 * @param columns, puzzle columns
 * @param client, represent Client
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
class PuzzleBoard(private val rows: Int, private val columns: Int, private val client: Client) : JFrame() {
    private val selectionManager: SelectionManager = SelectionManager()
    private val pointerPane = PointerPanel()
    private var status = Status.IDLE

    private enum class Status {
        IDLE,
        STARTED,
        COMPLETED
    }

    var tiles: List<Tile>? = null
        set(value) { field = value; if (status == Status.STARTED) paintPuzzle() }

    init {
        title = "Puzzle"
        layout = GridLayout (rows, columns, 0, 0)

        getRootPane().glassPane = pointerPane

        add(JButton("Start game").apply { addActionListener {
            this@PuzzleBoard.remove(this)

            status = Status.STARTED
            tiles?.let { paintPuzzle() }

            client.joinGame()
        } })

        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        isResizable = false
        isVisible = true
        pack()
    }

    /**
     * This method check if there are different between tile position then update position and repaint puzzle.
     */
    fun updateTiles(tilePositions: Map<String, Pair<Int,Int>>) = SwingUtilities.invokeAndWait {
        tiles ?: return@invokeAndWait

        val diffs = tilePositions.filter { tiles?.first { tile -> tile.tileID == it.key }?.currentPosition != it.value }

        if (diffs.isNotEmpty()) {
            tiles = tiles?.map { tile -> diffs[tile.tileID]?.let {
                Tile(
                    tile.image,
                    tile.tileID,
                    it
                )
            } ?: tile }
            paintPuzzle()
        }
    }

    fun updateMouse(x: Int, y: Int) = client.mouseMovement(x, y)

    /**
     * This method paint mouse in a specified position.
     *
     * @param mousePositions, represent a position.
     */
    fun updateMouses(mousePositions: Map<String, Pair<Int, Int>>) = SwingUtilities.invokeAndWait {
        pointerPane.drawPointers(mousePositions)
    }

    /**
     * This method, if the puzzle is complete create a JOptionPane.
     */
    fun complete() = SwingUtilities.invokeAndWait  {
        status = Status.COMPLETED
        paintPuzzle()
        JOptionPane("Puzzle Completed!").createDialog(this, "").apply {
            isModal = false
            defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
        }.isVisible = true
    }

    /**
     * This method paint puzzle on board.
     * Foreach tiles it create a new button and set an action listener.
     */
    private fun paintPuzzle() {
        contentPane.removeAll()

        val buttons = mutableListOf<TileButton>()
        tiles?.sorted()?.forEachIndexed { idx, tile ->
            val button = TileButton(tile)
            contentPane.add(button)
            buttons.add(button)
            button.border = BorderFactory.createLineBorder(Color.gray)
            if (status == Status.STARTED) button.addActionListener { selectionManager.selectTile(tile, client) }
            button.addMouseMotionListener(object : MouseAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    super.mouseMoved(e)
                    updateMouse((button.width * (idx % columns) + e.point.x) * 100/size.width,
                        (button.height * (idx / columns) + e.point.y) * 100/size.height)
                }
            })
        }
        pointerPane.buttons = buttons

        pack()
    }
}
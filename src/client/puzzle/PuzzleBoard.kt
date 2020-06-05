package client.puzzle

import client.Client
import java.awt.*
import javax.swing.*


class PuzzleBoard(rows: Int, columns: Int, private val client: Client) : JFrame() {
    private val selectionManager: SelectionManager = SelectionManager()
    private var started = false

    var tiles: List<Tile>? = null
        set(value) { field = value; if (started) paintPuzzle() }

    init {
        title = "Puzzle"

        layout = GridLayout (rows, columns, 0, 0)
        /*add(JTextField("Players Name:").apply { isEditable = false }, gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        val playerName = JTextField("")
        add(playerName, gbc)
        gbc.gridy = 1*/

        add(JButton("Start game").apply { addActionListener {
            this@PuzzleBoard.remove(this)

            started = true
            tiles?.let { paintPuzzle() }

            //namePlayer = if(playerName.text!=null) playerName.text else "Player"+ Random.nextInt(100)
            client.joinGame()
        } })

        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        isResizable = false
        isVisible = true
        pack()
    }

    fun updateTiles(tilePositions: Map<String, Pair<Int,Int>>) {
        tiles ?: return

        val diffs = tilePositions.filter { tiles?.first { tile -> tile.tileID == it.key }?.currentPosition != it.value }

        if (diffs.isNotEmpty()) {
            tiles = tiles?.map { tile -> diffs[tile.tileID]?.let { Tile(tile.image, tile.tileID, it) } ?: tile }
            paintPuzzle()
        }
    }

    private fun paintPuzzle() {
        contentPane.removeAll()

        tiles?.sorted()?.forEach { tile ->
            val button = TileButton(tile)
            contentPane.add(button)
            button.border = BorderFactory.createLineBorder(Color.gray)
            button.addActionListener { selectionManager.selectTile(tile, client) }
        }
        setLocationRelativeTo(null)
        pack()
    }

    private fun checkSolution() {
        // TODO request to server
        JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE)
    }
}
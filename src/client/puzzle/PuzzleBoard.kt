package client.puzzle

import client.Client
import io.vertx.core.json.JsonObject
import java.awt.*
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*


class PuzzleBoard(private val rows: Int, private val columns: Int, private val client: Client) : JFrame() {
    private val selectionManager: SelectionManager = SelectionManager()
    private val board = JPanel()

    private var tiles = emptyList<Tile>()

    init {
        title = "Puzzle"

        layout = GridBagLayout()
        val gbc = GridBagConstraints()

        gbc.fill = GridBagConstraints.BOTH
        /*add(JTextField("Players Name:").apply { isEditable = false }, gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        val playerName = JTextField("")
        add(playerName, gbc)
        gbc.gridy = 1*/

        add(JButton("Start game").apply { addActionListener {
            //namePlayer = if(playerName.text!=null) playerName.text else "Player"+ Random.nextInt(100)
            board.border = BorderFactory.createLineBorder(Color.gray)
            board.layout = GridLayout (rows, columns, 0, 0)
            contentPane.add(board, BorderLayout.CENTER)

            client.playGame()
            isEnabled = false
            isVisible = false
        } }, gbc)

        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        isResizable = false
        isVisible = true
        pack()
    }

    fun updateTiles(tileList: List<JsonObject>) {
        tiles = tileList.map {
            Tile(
                ImageIO.read(URL(it.getString("imageURL"))),
                it.getString("tileID"),
                Pair(it.getInteger("column"), it.getInteger("row"))
            )
        }
        paintPuzzle(board)
    }

    private fun paintPuzzle(board: JPanel) {
        board.removeAll()

        tiles.sorted().forEach { tile ->
            val button = TileButton(tile)
            board.add(button)
            button.border = BorderFactory.createLineBorder(Color.gray)
            button.addActionListener {
                selectionManager.selectTile(tile, object: SelectionManager.Listener {
                    override fun onSwapPerformed() {
                        // TODO request to server
                        checkSolution()
                    }
                }, client)
            }
        }
        pack()
        setLocationRelativeTo(null)
    }

    private fun checkSolution() {
        // TODO request to server
        JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE)
    }
}
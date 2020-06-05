package client.puzzle

import client.Client
import io.vertx.core.json.JsonObject
import java.awt.*
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*


class PuzzleBoard(
    private val rows: Int,
    private val columns: Int,
    private val client: Client,
    tiles: List<JsonObject>
) : JFrame() {
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

        tiles.forEach { tile ->
            client.vertx.createHttpClient().get(9000, "localhost", "/${tile.getString("imageURL")}").onComplete {
                it.result().body().onComplete {
                    this.tiles = this.tiles.toMutableList().apply { add( Tile(
                        ImageIcon(it.result().bytes).image,
                        tile.getString("tileID"),
                        Pair(tile.getInteger("column"), tile.getInteger("row"))
                    ) ) }
                    if (this.tiles.size == rows * columns) paintPuzzle()
                }
            }
        }

        add(JButton("Start game").apply { addActionListener {
            //namePlayer = if(playerName.text!=null) playerName.text else "Player"+ Random.nextInt(100)
            board.border = BorderFactory.createLineBorder(Color.gray)
            board.layout = GridLayout (rows, columns, 0, 0)
            this@PuzzleBoard.add(board, gbc)

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
        val tileData = tileList.map {
            object {
                val id = it.getString("tileID")
                val position = Pair(it.getInteger("column"), it.getInteger("row"))
            }
        }.map { tile -> Pair(tile, tiles.firstOrNull { it.tileID == tile.id }) }.filterNot { it.second == null }

        if (tileData.any { it.first.position != it.second?.currentPosition } && tileData.size == rows * columns) {
            tiles = tileData.map { pair -> Tile(pair.second!!.image, pair.second!!.tileID, pair.first.position ) }
            paintPuzzle()
        }
    }

    private fun paintPuzzle() {
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
        setLocationRelativeTo(null)
        pack()
    }

    private fun checkSolution() {
        // TODO request to server
        JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE)
    }
}
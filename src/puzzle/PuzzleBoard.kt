package src.puzzle

import client.Client
import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
import io.vertx.core.json.JsonObject
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.*
import java.util.stream.IntStream
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.random.Random

class PuzzleBoard(val rows:Int, val columns: Int, val imagePath:String, val client: Client) : JFrame() {

    private lateinit var tiles: List<Tile>
    private lateinit var selectionManager :SelectionManager
    private val board = JPanel()
    private val startButton = JButton("Start game")

    init {
        title = "Puzzle"
        isResizable = false
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

        layout = GridBagLayout()
        val gbc = GridBagConstraints()

        gbc.fill = GridBagConstraints.BOTH
        /*add(JTextField("Players Name:").apply { isEditable = false }, gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        val playerName = JTextField("")
        add(playerName, gbc)
        gbc.gridy = 1*/

        add(startButton.apply { addActionListener{
            //namePlayer = if(playerName.text!=null) playerName.text else "Player"+ Random.nextInt(100)
            board.border = BorderFactory.createLineBorder(Color.gray)
            board.layout = GridLayout (rows, columns, 0, 0)
            contentPane.add(board, BorderLayout.CENTER)
            client.playGame()
            startButton.isEnabled = false
            startButton.isVisible = false
        } }, gbc)

        isVisible = true
        pack()
    }

    fun createTiles(tileList: List<JsonObject>) {

        tileList.forEach{tiles.toMutableList().add(Tile(ImageIO.read(URL(it.getString("imageURL"))), it.getString("tileID"), Pair(it.getInteger("column"),
                it.getInteger("row"))))}

        paintPuzzle(board)
    }

    private fun paintPuzzle(board: JPanel){
        board.removeAll()

        Collections.sort(tiles)

        tiles.forEach { tile ->
            run {
                val btn = TileButton(tile)
                board.add(btn)
                btn.border = BorderFactory.createLineBorder(Color.gray)
                btn.addActionListener {
                        selectionManager.selectTile(tile, object:SelectionManager.Listener {
                            override fun onSwapPerformed() {
                                paintPuzzle(board)
                                checkSolution()
                            }
                        }, client)
                }
            }
        }

        pack()
        setLocationRelativeTo(null)
    }

    private fun checkSolution() {
        //TODO
            JOptionPane.showMessageDialog(this, "Puzzle Completed!", "", JOptionPane.INFORMATION_MESSAGE)
    }

}
package main.kotlin.client.view

import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JButton

/**
 * This class represent a tile button.
 * Creates a new button for the tile.
 * This one contains the image and has a red outline when clicked.
 *
 * @param tile, puzzle tile
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
class TileButton(private val tile: Tile) : JButton(ImageIcon(tile.image)) {
    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                border = BorderFactory.createLineBorder(Color.red)
            }
        })
    }

    /**
     * Clean the image
     */
    fun redraw() = graphics?.let { update(it) }
}

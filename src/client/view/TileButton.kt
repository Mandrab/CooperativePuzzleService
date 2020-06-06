package client.view

import java.awt.Color
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JButton


class TileButton(private val tile: Tile) : JButton(ImageIcon(tile.image)) {
    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                border = BorderFactory.createLineBorder(Color.red)
            }
        })
    }

    fun redraw() = graphics?.let { update(it) }
}

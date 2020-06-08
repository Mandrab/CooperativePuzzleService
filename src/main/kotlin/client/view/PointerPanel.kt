package main.kotlin.client.view

import java.awt.Color
import java.awt.Graphics2D
import javax.swing.JComponent

/**
 * This class is used to draw the pointers on the puzzle in a specified position.
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
class PointerPanel : JComponent() {
    var buttons: List<TileButton> = emptyList()

    init {
        isVisible = true
    }

    fun drawPointers(pointers: Map<String, Pair<Int, Int>>) {
        buttons.forEach { it.redraw() }
        pointers.forEach { drawPointer(it.key, it.value.first, it.value.second) }
    }

    private fun drawPointer(playerID: String, xPos: Int, yPos: Int) {
        val x = xPos/100.0 * size.width
        val y = yPos/100.0 * size.height

        val g2d = graphics as Graphics2D
        g2d.color = Color.RED
        g2d.fillOval((x - 3).toInt(), (y - 3).toInt(), 15, 15)
        g2d.drawString(playerID, x.toInt(), y.toInt() + 50)
    }
}
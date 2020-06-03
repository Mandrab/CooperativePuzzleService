package client.view

import client.Client
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.WindowConstants


class View(client: Client) : JFrame() {

    init {
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                super.mouseMoved(e)
                client.newMovement(Point(e.point.x * 100/size.width, e.point.y * 100/size.height))
            }
        })

        minimumSize = Dimension(200, 200)

        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        isVisible = true
    }

    fun drawPointer(playerID: String, xPos: Int, yPos: Int) {
        val x = xPos * size.width/100
        val y = yPos * size.height/100

        val g2d = graphics as Graphics2D
        g2d.clearRect(0, 0, size.width, size.height)
        g2d.drawString(playerID, x, y + 50)

        val icon = ImageIcon("res/tux_icon.png")
        icon.paintIcon(this, graphics, x, y)
    }
}
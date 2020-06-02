package client.view

import client.Client
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JFrame

class View(client: Client) : JFrame() {

    init {
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                super.mouseMoved(e)
                client.newMovement(e.point)
            }
        })

        isVisible = true
    }
}
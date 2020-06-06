import client.Client
import io.vertx.core.Vertx
import service.Gateway
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JFrame

fun main() {
    val vertx = Vertx.vertx()


    /*val frame = JFrame ("prova")
    frame.add(JButton(ImageIcon("trash\\Puzzle103796711\\tile0.png")))
    frame.isVisible = true*/
    vertx.deployVerticle(Gateway()) {
        Thread.sleep(200)
        vertx.deployVerticle(Client("marco", Gateway.PORT))
        vertx.deployVerticle(Client("giovanni", Gateway.PORT))
    }
}
import main.kotlin.client.Client
import io.vertx.core.Promise
import io.vertx.core.Vertx
import main.kotlin.service.Gateway

/**
 * This is main method.
 * It creates two clients after started the service.
 *
 * IMPORTANT: The client downloads tile images from the server, so one needs to be running.
 * In git repo there's a simple python script that starts a server for this use.
 * Also a folder named thrash as to be created in project directory.
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
fun main() {
    println("IMPORTANT: The client downloads tile images from the server, so one needs to be running.\n" +
            "In git repo there's a simple python script that starts a server for this use.\n" +
            "Also a folder named thrash as to be created in project directory.")

    val vertx = Vertx.vertx()

    val ready = Promise.promise<Void>()
    ready.future().onSuccess {
        vertx.deployVerticle(Client(Gateway.PORT)).onSuccess {
            vertx.deployVerticle(Client(Gateway.PORT))
        }
    }
    vertx.deployVerticle(Gateway(ready))
}
import client.Client
import io.vertx.core.Vertx
import service.Gateway

fun main() {
    val vertx = Vertx.vertx()

    vertx.deployVerticle(Gateway()) {
        Thread.sleep(200)
        vertx.deployVerticle(Client("marco", Gateway.PORT))
        vertx.deployVerticle(Client("giovanni", Gateway.PORT))
    }
}
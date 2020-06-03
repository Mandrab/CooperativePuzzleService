import client.Client
import io.vertx.core.Vertx
import service.Gateway

fun main() {
    val vertx = Vertx.vertx()

    vertx.deployVerticle(Gateway()) {
        vertx.deployVerticle(Client("marco", Gateway.PORT))
        vertx.deployVerticle(Client("giovanni", Gateway.PORT))
    }
}
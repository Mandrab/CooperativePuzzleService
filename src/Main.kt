import client.Client
import io.vertx.core.Promise
import io.vertx.core.Vertx
import service.Gateway

fun main() {
    val vertx = Vertx.vertx()

    val ready = Promise.promise<Void>()
    ready.future().onSuccess {
        vertx.deployVerticle(Client("marco", Gateway.PORT))
        vertx.deployVerticle(Client("giovanni", Gateway.PORT))
    }
    vertx.deployVerticle(Gateway(ready))
}
import client.Client
import io.vertx.core.Vertx
import service.Gateway

private const val PORT = 40426

fun main() {

    val vertx = Vertx.vertx()

    vertx.deployVerticle(Gateway(PORT)) { vertx.deployVerticle(Client(PORT)) }
}
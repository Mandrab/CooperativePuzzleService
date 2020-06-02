import io.vertx.core.Vertx

private const val PORT = 40426

fun main() {

    val vertx = Vertx.vertx()

    /*vertx.deployVerticle(Gateway(PORT)) {
        vertx.deployVerticle(Client(PORT))
        vertx.deployVerticle(Client(PORT))
    }*/
}
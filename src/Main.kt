import client.Client
import io.vertx.core.Vertx
import service.Gateway
import src.puzzle.PuzzleBoard

private const val PORT = 40426

fun main() {
    val vertx = Vertx.vertx()

    vertx.deployVerticle(Gateway(PORT)) {
        vertx.deployVerticle(Client("marco", PORT))
        vertx.deployVerticle(Client("giovanni", PORT))
    }
}
package service

import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture

class TestService {
    private val vertx = Vertx.vertx()
    private val client = WebClient.create(vertx)

    @Test fun test() {
        testGetPuzzleToJoin().thenAccept {
            testLoginToPuzzle(it)
            testGetTilesInfo(it)
        }

        Thread.sleep(2000)
    }

    private fun testGetPuzzleToJoin(): CompletableFuture<String> {
        val returns = CompletableFuture<String>()

        client.get(port, "localhost", "/puzzle").send {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                println("Response: $body")
                returns.complete(body.getString("puzzleID"))
            } else {
                println("Something went wrong ${it.cause().message}")
                returns.completeExceptionally(null)
            }
        }
        return returns
    }

    private fun testLoginToPuzzle(puzzleID: String) {
        val params = JsonObject().put("playerID", puzzleID)

        client.post(port, "localhost", "/puzzle/$puzzleID/user").sendJson(params) {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code")
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }

    private fun testGetTilesInfo(puzzleID: String): CompletableFuture<List<Tile>> {
        val result = CompletableFuture<List<Tile>>()
        client.get(port, "localhost", "/puzzle/$puzzleID/tile").send {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code ${response.body()}")
                result.complete(response.body().toJsonArray().map { it as JsonObject }.map { Tile.parse(it) })
            } else {
                println("Something went wrong ${it.cause().message}")
                result.completeExceptionally(null)
            }
        }
        return result
    }

    @Test fun testWebSocket() {
        client.get(port, "localhost", "/puzzle").send {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                println("Response: $body")

                val client: HttpClient = vertx.createHttpClient()
                val path = "/puzzle/${it.result().bodyAsJsonObject().getString("puzzleID")}/user"

                client.webSocket(port, "localhost", path) {
                    val msg = JsonObject().put("player", "marco").put("position", JsonObject().put("x", 5).put("y", 6))
                    it.result().writeBinaryMessage(msg.toBuffer())
                    it.result().writeTextMessage(msg.encode())

                    it.result().textMessageHandler {
                        println(it)
                    }.exceptionHandler {
                        TODO()
                    }.closeHandler {
                        TODO()
                    }
                }
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }

    private data class Tile(val ID: Int, val currentPosition: Pair<Int, Int>) {
        companion object {
            fun parse(jo: JsonObject) = Tile(jo.getString("ID").toInt(), Pair(jo.getString("currentX").toInt(),
                    jo.getString("currentY").toInt()))
        }
    }

    private fun testUpdatePosition(puzzleID: String, tileID: String, newPosition: Pair<Int, Int>, currentPosition: Pair<Int, Int>){
        val params = JsonObject().put("puzzleID", puzzleID).put("tileID", tileID).put("x", newPosition.first).put("y", newPosition.second)
        client.get(port, "localhost", "/puzzle/$puzzleID/$tileID").sendJson(params) {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code ${response.body()}")
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }

    private val port = 40426

    @Before fun startService() {
        val ready = Vertx.vertx().deployVerticle(Gateway(port))
        while (!ready.isComplete) Thread.sleep(100) // TODO
    }
}
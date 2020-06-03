package service

import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture
import kotlin.test.assertFalse

class TestService {
    private val playerID = "Marcantonio"

    private val vertx = Vertx.vertx()
    private val client = WebClient.create(vertx)

    @Test fun test() {
        var visited = false

        createPuzzle().thenAccept {
            getPuzzlesIDs()
            getPuzzleInfo(it)

            testGetPuzzleToJoin().thenAccept {
                testLoginToPuzzle(it)
                testGetTilesInfo(it)

                visited = true
            }
        }

        Thread.sleep(2000)
        assert(visited) { "All the callback should have been called" }
    }

    /**
     * Test creation of a new puzzle
     */
    private fun createPuzzle(): CompletableFuture<String> {
        val returns = CompletableFuture<String>()

        client.post(Gateway.PORT, "localhost", "/puzzle").send {
            assert(it.succeeded())

            val body = it.result().bodyAsJsonObject()

            assert(body.containsKey("puzzleID"))
            assert(body.containsKey("imageURL"))
            assert(body.containsKey("columns"))
            assert(body.containsKey("rows"))

            returns.complete(body.getString("puzzleID"))
        }
        return returns
    }

    /**
     * Test get of puzzles IDs
     */
    private fun getPuzzlesIDs() {
        client.get(Gateway.PORT, "localhost", "/puzzle").send {
            assert(it.succeeded())
            assertFalse(it.result().bodyAsJsonArray().isEmpty)
        }
    }

    /**
     * Test obtainment of puzzle info
     */
    private fun getPuzzleInfo(puzzleID: String) {
        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID").send {
            assert(it.succeeded())

            val body = it.result().bodyAsJsonObject()

            assert(body.containsKey("imageURL"))
            assert(body.containsKey("columns"))
            assert(body.containsKey("rows"))
            assert(body.containsKey("tiles"))

            val jArray = body.getJsonArray("tiles")

            assertFalse(jArray.isEmpty)
            assert(jArray.all { it is JsonObject })

            val first = jArray.first() as JsonObject
            assert(first.containsKey("tileID"))
            assert(first.containsKey("column"))
            assert(first.containsKey("row"))
        }
    }

    private fun testGetPuzzleToJoin(): CompletableFuture<String> {
        val returns = CompletableFuture<String>()

        client.get(Gateway.PORT, "localhost", "/puzzle").send {
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
        val params = JsonObject().put("playerID", playerID)

        client.post(Gateway.PORT, "localhost", "/puzzle/$puzzleID/user").sendJson(params) {
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
        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/tile").send {
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
        client.get(Gateway.PORT, "localhost", "/puzzle").send {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                println("Response: $body")

                val client: HttpClient = vertx.createHttpClient()
                val path = "/puzzle/${it.result().bodyAsJsonObject().getString("puzzleID")}/user"

                client.webSocket(Gateway.PORT, "localhost", path) {
                    val msg = JsonObject().put("player", "marco").put("position", JsonObject().put("x", 5).put("y", 6))
                    println(it.cause())
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
        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/$tileID").sendJson(params) {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code ${response.body()}")
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }

    @Before fun startService() {
        val ready = Vertx.vertx().deployVerticle(Gateway())
        while (!ready.isComplete) Thread.sleep(100) // TODO
    }
}
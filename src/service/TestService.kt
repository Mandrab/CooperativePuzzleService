package service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TestService {
    private val vertx = Vertx.vertx()
    private val client = WebClient.create(vertx)

    @Test fun test() {
        val result = ResultLock(0, 6)

        createPuzzle().onSuccess { puzzleID ->
            result.set { result.result +1 }
            getPuzzlesIDs()
            getPuzzleInfo(puzzleID)
            val tilesFuture = getPuzzleTiles(puzzleID).onSuccess { tileID ->
                result.set { result.result +1 }
                getPuzzleTile(puzzleID, tileID.first())
            }.onFailure { fail() }
            joinWithUser(puzzleID).onSuccess { player ->
                result.set { result.result +1 }
                tilesFuture.onSuccess { tiles ->
                    result.set { result.result +1 }
                    updatePuzzleTile(puzzleID, tiles.first(), player)
                }
                getMousePosition(puzzleID, 0).onSuccess {
                    result.set { result.result +1 }
                    putMousePosition(puzzleID, player).onSuccess {
                        result.set { result.result +1 }
                        getMousePosition(puzzleID, 1)
                    }.onFailure { fail() }
                }.onFailure { fail() }
            }.onFailure { fail() }
        }.onFailure { fail() }

        result.deadline(2000)
        assertEquals(6, result.result, "All the callback should have been called")
    }

    /**
     * Test creation of a new puzzle
     */
    private fun createPuzzle(): Future<String> {
        val returns = Promise.promise<String>()

        client.post(Gateway.PORT, "localhost", "/puzzle").send {
            assert(it.succeeded())

            val body = it.result().bodyAsJsonObject()

            assert(body.containsKey("puzzleID"))
            assert(body.containsKey("imageURL"))
            assert(body.containsKey("columns"))
            assert(body.containsKey("rows"))

            returns.complete(body.getString("puzzleID"))
        }
        return returns.future()
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
            assert(jArray.all { t -> t is JsonObject })
            assert(jArray.all { t -> (t as JsonObject).containsKey("tileID") })
            assert(jArray.all { t -> (t as JsonObject).containsKey("column") })
            assert(jArray.all { t -> (t as JsonObject).containsKey("row") })
        }
    }

    /**
     * Test get of puzzle tiles
     */
    private fun getPuzzleTiles(puzzleID: String): Future<List<String>> {
        val result = Promise.promise<List<String>>()

        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/tiles").send {
            assert(it.succeeded())

            val jArray = it.result().bodyAsJsonArray().map { t -> t as JsonObject }

            assert(jArray.all { t -> t.containsKey("tileID") })
            assert(jArray.all { t -> t.containsKey("column") })
            assert(jArray.all { t -> t.containsKey("row") })

            result.complete(jArray.map { it.getString("tileID") })
        }
        return result.future()
    }

    /**
     * Test get of a puzzle tile
     */
    private fun getPuzzleTile(puzzleID: String, tileID: String) {
        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/$tileID").send {
            assert(it.succeeded())

            val jObject = it.result().bodyAsJsonObject()

            assert(jObject.containsKey("tileID"))
            assert(jObject.containsKey("column"))
            assert(jObject.containsKey("row"))
        }
    }

    /**
     * Test update of a puzzle tile
     */
    private fun updatePuzzleTile(puzzleID: String, tileID: String, playerID: String) {
        client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/$tileID").sendJsonObject(
            JsonObject().put("playerToken", playerID).put("newColumn", 3).put("newRow", 2)
        ) {
            assert(it.succeeded())
            assertEquals(200, it.result().statusCode())
        }
        client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/$tileID").sendJsonObject(
            JsonObject().put("playerToken", "${playerID}a").put("newColumn", 3).put("newRow", 2)
        ) {
            assert(it.succeeded())
            assertEquals(401, it.result().statusCode())
        }
        client.put(Gateway.PORT, "localhost", "/puzzle/${puzzleID}a/${tileID}a").sendJsonObject(
            JsonObject().put("playerToken", playerID).put("newColumn", 3).put("newRow", 2)
        ) {
            assert(it.succeeded())
            assertEquals(404, it.result().statusCode())
        }
        client.put(Gateway.PORT, "localhost", "/puzzle/${puzzleID}/${tileID}").sendJsonObject(
            JsonObject().put("playerToken", playerID).put("newColumn", Int.MIN_VALUE).put("newRow", Int.MIN_VALUE)
        ) {
            assert(it.succeeded())
            assertEquals(409, it.result().statusCode())
        }
    }

    /**
     * Test join to a game
     */
    private fun joinWithUser(puzzleID: String): Future<String> {
        val playerID = "Marcantonio"
        val result = Promise.promise<String>()

        client.post(Gateway.PORT, "localhost", "/puzzle/${puzzleID}a/user").sendJson(
            JsonObject().put("playerID", playerID)
        ) {
            assertEquals(404, it.result().statusCode())
        }
        client.post(Gateway.PORT, "localhost", "/puzzle/$puzzleID/user").sendJson(
            JsonObject().put("playerID", playerID)
        ) {
            assertEquals(201, it.result().statusCode())
            assert(it.result().bodyAsJsonObject().containsKey("playerToken"))

            result.complete(it.result().bodyAsJsonObject().getString("playerToken"))
        }
        return result.future()
    }

    /**
     * Test mouse pointer post
     */
    private fun putMousePosition(puzzleID: String, playerID: String): Future<Boolean> {
        val result = Promise.promise<Boolean>()

        client.put(Gateway.PORT, "localhost", "/puzzle/${puzzleID}a/mouses").sendJsonObject(
            JsonObject().put("playerToken", playerID).put("column", 5).put("row", 5)
        ) {
            assertEquals(404, it.result().statusCode())
        }
        val notExpectedCode = AtomicInteger()
        for (i in 0..1) {
            client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/mouses").sendJsonObject(
                JsonObject().put("playerToken", playerID).put("column", 5).put("row", 5)
            ) {
                synchronized(notExpectedCode) {
                    val code = it.result().statusCode()

                    if (code == 201 && notExpectedCode.get() != 201) {
                        notExpectedCode.set(201)
                    } else if (code == 200 && notExpectedCode.get() != 200) {
                        notExpectedCode.set(200)
                        result.complete(true)
                    } else fail()
                }
            }
        }
        return result.future()
    }

    /**
     * Test mouse pointer get
     */
    private fun getMousePosition(puzzleID: String, expectedResult: Int): Future<Boolean> {
        val result = Promise.promise<Boolean>()

        client.get(Gateway.PORT, "localhost", "/puzzle/${puzzleID}a/mouses").send {
            assertEquals(404, it.result().statusCode())
        }
        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/mouses").send {
            assertEquals(200, it.result().statusCode())
            result.complete(true)
            assertEquals(expectedResult, it.result().bodyAsJsonArray().size())
        }
        return result.future()
    }

/*
    private fun testGetTilesInfo(puzzleID: String): Future<List<Tile>> {
        val result = Promise.promise<List<Tile>>()
        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/tile").send {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code ${response.body()}")
                result.complete(response.body().toJsonArray().map { it as JsonObject }.map { Tile.parse(it) })
            } else {
                println("Something went wrong ${it.cause().message}")
                result.fail("")
            }
        }
        return result.future()
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
    }*/

    @Before fun startService() {
        val ready = Vertx.vertx().deployVerticle(Gateway())
        while (!ready.isComplete) Thread.sleep(100) // TODO
    }
}

class ResultLock<T: Any>(alternativeResult: T, private val waitResult: T? = null): ReentrantLock() {
    private var completed: Boolean = false
    private val lock = newCondition()
    var result: T = alternativeResult

    fun deadline(maxMillis: Int) = withLock {
        if (!completed) lock.await(maxMillis.toLong(), TimeUnit.MILLISECONDS)
        completed = true
    }

    fun set(action: () -> T) = withLock {
        result = action()
        if (!completed && result == waitResult) {
            completed = true
            lock.signalAll()
        }
    }
}
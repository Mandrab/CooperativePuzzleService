package test.kotlin.service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.json.JsonObject
import org.junit.Assert.*
import org.junit.Test
import service.Gateway
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random


class RESTful : AbsServiceTest() {
    private lateinit var lastPuzzleID: String
    private lateinit var lastPlayerID: String
    private lateinit var lastTilesIDs: List<String>

    @Test fun testPlay() {
        val callbackCount = ResultLock(alternativeResult = false, waitResult = true)

        runSequentially(
            Pair(0) { getPuzzlesIDs(0) },
            Pair(0) { getPuzzleInfo(notExistingPuzzle(), 404) },
            Pair(0) { getPuzzleTiles(notExistingPuzzle(), 404) },
            Pair(0) { getPuzzleTile(notExistingPuzzle(), notExistingTile(),404) },
            Pair(0) { putPuzzleTile(notExistingPuzzle(), notExistingPlayer(), notExistingTile(), 0, 0, 404) },
            Pair(0) { postPlayer(notExistingPuzzle(), 404) },

            Pair(1) { createPuzzle() },

            Pair(2) { getPuzzlesIDs(1) },
            Pair(2) { getPuzzleInfo(lastPuzzleID, 200) },
            Pair(2) { getPuzzleInfo(notExistingPuzzle(), 404) },
            Pair(2) { getPuzzleTiles(lastPuzzleID, 200) },
            Pair(2) { getPuzzleTiles(notExistingPuzzle(), 404) },
            Pair(2) { postPlayer(lastPuzzleID, 201) },
            Pair(2) { postPlayer(notExistingPuzzle(), 404) },

            Pair(3) { getPuzzleTile(lastPuzzleID, lastTilesIDs.first(), 200) },
            Pair(3) { getPuzzleTile(notExistingPuzzle(), lastTilesIDs.first(), 404) },
            Pair(3) { getPuzzleTile(lastPuzzleID, notExistingTile(), 404) },
            Pair(3) { putPuzzleTile(lastPuzzleID, lastPlayerID, lastTilesIDs.first(), 0, 0, 200) },
            Pair(3) { putPuzzleTile(lastPuzzleID, notExistingPlayer(), lastTilesIDs.first(), 0, 0, 401) },
            Pair(3) { putPuzzleTile(notExistingPuzzle(), notExistingPlayer(), notExistingTile(), 0, 0, 404) },

            Pair(4) { createPuzzle() },    // N.B. it overwrites lastPuzzleID

            Pair(5) { getPuzzlesIDs(2) },
            Pair(5) { putPuzzleTile(lastPuzzleID, notExistingPlayer(), lastTilesIDs.first(), 0, 0, 401) },
            Pair(5) { putPuzzleTile(lastPuzzleID, lastPlayerID, lastTilesIDs.first(), 0, 0, 401) },    // old player new puzzle
            Pair(5) { putPuzzleTile(notExistingPuzzle(), notExistingPlayer(), notExistingTile(), 0, 0, 404) },

            Pair(6) { postPlayer(lastPuzzleID, 201) }, // N.B. it overwrites lastPlayerID variable

            Pair(7) { putPuzzleTile(lastPuzzleID, lastPlayerID, lastTilesIDs.first(), 0, 0, 200) },
            Pair(7) { putPuzzleTile(lastPuzzleID, notExistingPlayer(), lastTilesIDs.first(), 0, 0, 401) },
            Pair(7) { putPuzzleTile(notExistingPuzzle(), notExistingPlayer(), notExistingTile(), 0, 0, 404) },
            Pair(7) { putPuzzleTile(lastPuzzleID, lastPlayerID, lastTilesIDs.first(), Int.MIN_VALUE, Int.MIN_VALUE, 409) }
        ).onSuccess { callbackCount.set { true } }

        callbackCount.deadline(2000)
        assert(callbackCount.result) { "All the callback should have been called" }
    }

    @Test fun testMouse() {
        val callbackCount = ResultLock(alternativeResult = false, waitResult = true)

        runSequentially(
            Pair(0) { getMousePointers(notExistingPuzzle(), 404, 0) },
            Pair(0) { putMousePointers(notExistingPuzzle(), notExistingPlayer(), LocalDateTime.now(), 404) },

            Pair(1) { createPuzzle() },

            Pair(2) { getMousePointers(lastPuzzleID, 200, 0) },
            Pair(2) { getMousePointers(notExistingPuzzle(), 404, 0) },
            Pair(2) { putMousePointers(lastPuzzleID, notExistingPlayer(), LocalDateTime.now(), 404) },

            Pair(3) { postPlayer(lastPuzzleID, 201) },

            Pair(4) { getMousePointers(lastPuzzleID, 200, 0) },
            Pair(4) { getMousePointers(notExistingPuzzle(), 404, 0) },

            Pair(5) { putMousePointers(lastPuzzleID, lastPlayerID, LocalDateTime.now(), 201) },

            Pair(6) { putMousePointers(lastPuzzleID, lastPlayerID, LocalDateTime.now(), 200) },
            Pair(6) { putMousePointers(notExistingPuzzle(), lastPlayerID, LocalDateTime.now(), 404) },

            Pair(7) { postPlayer(lastPuzzleID, 201) },
            Pair(7) { getMousePointers(lastPuzzleID, 200, 1) },
            Pair(7) { getMousePointers(notExistingPuzzle(), 404, 0) },

            Pair(8) { putMousePointers(lastPuzzleID, lastPlayerID, LocalDateTime.now(), 201) },

            Pair(9) { getMousePointers(lastPuzzleID, 200, 2) },
            Pair(9) { getMousePointers(notExistingPuzzle(), 404, 0) },

            Pair(10) { putMousePointers(lastPuzzleID, lastPlayerID, LocalDateTime.now().minusYears(1), 409) },
            Pair(10) { putMousePointers(lastPuzzleID, lastPlayerID, LocalDateTime.now().minusDays(1), 409) }
        ).onSuccess { callbackCount.set { true } }

        callbackCount.deadline(2000)
        assert(callbackCount.result) { "All the callback should have been called" }
    }

    /*@Test fun test() {
        val callbackCount = ResultLock(0, 6)

        val gotPuzzleTiles = Promise.promise<List<String>>()
        val userJoined = Promise.promise<String>()
        val gotMousePositions = Promise.promise<Boolean>()

        gotPuzzleTiles.future().onSuccess { tilesIDs ->
            callbackCount.set { callbackCount.result +1 }
            this.tilesIDs = tilesIDs

            getPuzzleTile(puzzleID, tilesIDs.first())
        }

        userJoined.future().onSuccess { playerID ->
            callbackCount.set { callbackCount.result +1 }
            this.playerID = playerID

            getMousePosition(puzzleID, 0).onSuccess {
                gotMousePositions.complete(it)
            }
        }

        gotMousePositions.future().onSuccess {
            callbackCount.set { callbackCount.result +1 }

            putMousePosition(puzzleID, playerID).onSuccess {
                callbackCount.set { callbackCount.result +1 }

                getMousePosition(puzzleID, 1)
            }
        }

        CompositeFuture.all(listOf(gotPuzzleTiles.future(), userJoined.future())).onSuccess {
            callbackCount.set { callbackCount.result +1 }

            updatePuzzleTile(puzzleID, tilesIDs.first(), playerID)
        }

        createPuzzle().onSuccess { puzzleID ->
            callbackCount.set { callbackCount.result +1 }
            this.puzzleID = puzzleID

            getPuzzlesIDs()
            getPuzzleInfo(puzzleID)
            getPuzzleTiles(puzzleID).onComplete {
                gotPuzzleTiles.complete(it.result())
            }
            joinWithUser(puzzleID).onComplete {
                userJoined.complete(it.result())
            }
        }

        callbackCount.deadline(2000)
        assertEquals("All the callback should have been called", 6, callbackCount.result)
    }*/

    /**
     * Test creation of a new puzzle
     */
    private fun createPuzzle(): Future<Any> {
        val returns = Promise.promise<Any>()

        client.post(Gateway.PORT, "localhost", "/puzzle").send {
            assert(it.succeeded())

            val body = it.result().bodyAsJsonObject()

            assert(body.containsKey("puzzleID"))
            assert(body.containsKey("imageURL"))
            assert(body.containsKey("columns"))
            assert(body.containsKey("rows"))

            lastPuzzleID = body.getString("puzzleID")
            returns.complete()
        }
        return returns.future()
    }

    /**
     * Test get of puzzles IDs
     */
    private fun getPuzzlesIDs(expectedSize: Int): Future<Any> {
        val result = Promise.promise<Any>()
        client.get(Gateway.PORT, "localhost", "/puzzle").send {
            assert(it.succeeded())
            assertEquals(expectedSize, it.result().bodyAsJsonObject().getJsonArray("IDs").size())
            result.complete()
        }
        return result.future()
    }

    /**
     * Test obtainment of puzzle info
     */
    private fun getPuzzleInfo(puzzleID: String, expectedCode: Int): Future<Any> {
        val result = Promise.promise<Any>()
        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID").send {
            assert(it.succeeded())

            assertEquals(expectedCode, it.result().statusCode())
            if (expectedCode != 200) {
                result.complete()
                return@send
            }

            assert(it.result().bodyAsJsonObject().containsKey("timeStamp"))
            assert(it.result().bodyAsJsonObject().containsKey("info"))

            val body = it.result().bodyAsJsonObject().getJsonObject("info")

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

            result.complete()
        }
        return result.future()
    }

    /**
     * Test get of puzzle tiles
     */
    private fun getPuzzleTiles(puzzleID: String, expectedCode: Int): Future<Any> {
        val result = Promise.promise<Any>()

        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/tiles").send {
            assert(it.succeeded())

            assertEquals(expectedCode, it.result().statusCode())
            if (expectedCode != 200) {
                result.complete()
                return@send
            }

            assert(it.result().bodyAsJsonObject().containsKey("timeStamp"))
            assert(it.result().bodyAsJsonObject().containsKey("tiles"))

            val jArray = it.result().bodyAsJsonObject().getJsonArray("tiles").map { t -> t as JsonObject }

            assert(jArray.all { t -> t.containsKey("tileID") })
            assert(jArray.all { t -> t.containsKey("column") })
            assert(jArray.all { t -> t.containsKey("row") })

            lastTilesIDs = jArray.map { it.getString("tileID") }
            result.complete()
        }
        return result.future()
    }

    /**
     * Test get of a puzzle tile
     */
    private fun getPuzzleTile(puzzleID: String, tileID: String, expectedCode: Int): Future<Any> {
        val result = Promise.promise<Any>()

        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/$tileID").send {
            assert(it.succeeded())

            assertEquals(expectedCode, it.result().statusCode())
            if (expectedCode != 200) {
                result.complete()
                return@send
            }

            val jObject = it.result().bodyAsJsonObject()

            assert(jObject.containsKey("tileID"))
            assert(jObject.containsKey("column"))
            assert(jObject.containsKey("row"))
            result.complete()
        }
        return result.future()
    }

    /**
     * Test update of a puzzle tile
     */
    private fun putPuzzleTile(puzzleID: String, playerID: String, tileID: String, column: Int, row: Int,
            expectedCode: Int): Future<Any> {
        val result = Promise.promise<Any>()

        client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/$tileID").sendJsonObject(
            JsonObject().put("playerToken", playerID).put("newColumn", column).put("newRow", row)
        ) {
            assert(it.succeeded())
            assertEquals(expectedCode, it.result().statusCode())
            result.complete()
        }
        return result.future()
    }

    /**
     * Test join to a game
     */
    private fun postPlayer(puzzleID: String, expectedCode: Int): Future<Any> {
        val result = Promise.promise<Any>()

        client.post(Gateway.PORT, "localhost", "/puzzle/$puzzleID/user").send {
            assert(it.succeeded())
            assertEquals(expectedCode, it.result().statusCode())

            if (expectedCode != 201) {
                result.complete()
                return@send
            }

            assert(it.result().bodyAsJsonObject().containsKey("playerToken"))
            lastPlayerID = it.result().bodyAsJsonObject().getString("playerToken")
            result.complete()
        }
        return result.future()
    }

    /**
     * Test mouse pointer post
     */
    private fun putMousePointers(puzzleID: String, playerID: String, timestamp: LocalDateTime, expectedCode: Int): Future<Any> {
        val result = Promise.promise<Any>()

        client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/mouses").sendJsonObject(
            JsonObject().put("timeStamp", timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")))
                .put("playerToken", playerID).put("position", JsonObject().put("x", Random.nextInt()).put("y", Random.nextInt()))
        ) {
            assertEquals(expectedCode, it.result().statusCode())
            result.complete()
        }
        return result.future()
    }

    /**
     * Test mouse pointer get
     */
    private fun getMousePointers(puzzleID: String, expectedCode: Int, expectedPointers: Int): Future<Any> {
        val result = Promise.promise<Any>()

        client.get(Gateway.PORT, "localhost", "/puzzle/$puzzleID/mouses").send {
            assert(it.succeeded())
            assertEquals(expectedCode, it.result().statusCode())

            if (expectedCode != 200) {
                result.complete()
                return@send
            }

            assertEquals(expectedPointers, it.result().bodyAsJsonObject().getJsonArray("positions").size())
            result.complete()
        }
        return result.future()
    }
}
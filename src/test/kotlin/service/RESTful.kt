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


class RESTful : AbsServiceTest() {
    private lateinit var lastPuzzleID: String
    private lateinit var lastPlayerID: String
    private lateinit var lastTilesIDs: List<String>

    @Test fun test() {
        val callbackCount = ResultLock(0, 6)

        runSequentially(
            Pair(0, { getPuzzlesIDs(0) }),
            Pair(0, { getPuzzleInfo(notExistingPuzzle(), 404) }),
            Pair(0, { getPuzzleTiles(notExistingPuzzle(), 404) }),
            Pair(0, { getPuzzleTile(notExistingPuzzle(), notExistingTile(),404) }),
            Pair(0, { putPuzzleTile(notExistingPuzzle(), notExistingPlayer(), notExistingTile(), 0, 0, 404) }),
            Pair(0, { postPlayer(notExistingPuzzle(), 404) }),

            Pair(1, { createPuzzle() }),

            Pair(2, { getPuzzlesIDs(1) }),
            Pair(2, { getPuzzleInfo(lastPuzzleID, 200) }),
            Pair(2, { getPuzzleInfo(notExistingPuzzle(), 404) }),
            Pair(2, { getPuzzleTiles(lastPuzzleID, 200) }),
            Pair(2, { getPuzzleTiles(notExistingPuzzle(), 404) }),
            Pair(2, { postPlayer(lastPuzzleID, 201) }),
            Pair(2, { postPlayer(notExistingPuzzle(), 404) }),

            Pair(3, { getPuzzleTile(lastPuzzleID, lastTilesIDs.first(), 200) }),
            Pair(3, { getPuzzleTile(notExistingPuzzle(), lastTilesIDs.first(), 404) }),
            Pair(3, { getPuzzleTile(lastPuzzleID, notExistingTile(), 404) }),
            Pair(3, { putPuzzleTile(lastPuzzleID, lastPlayerID, lastTilesIDs.first(), 0, 0, 200) }),
            Pair(3, { putPuzzleTile(lastPuzzleID, notExistingPlayer(), lastTilesIDs.first(), 0, 0, 401) }),
            Pair(3, { putPuzzleTile(notExistingPuzzle(), notExistingPlayer(), notExistingTile(), 0, 0, 404) }),

            Pair(4, { createPuzzle() }),    // N.B. it overwrites lastPuzzleID

            Pair(5, { getPuzzlesIDs(2) }),
            Pair(5, { putPuzzleTile(lastPuzzleID, notExistingPlayer(), lastTilesIDs.first(), 0, 0, 401) }),
            Pair(5, { putPuzzleTile(lastPuzzleID, lastPlayerID, lastTilesIDs.first(), 0, 0, 401) }),    // old player new puzzle
            Pair(5, { putPuzzleTile(notExistingPuzzle(), notExistingPlayer(), notExistingTile(), 0, 0, 404) }),

            Pair(6, { postPlayer(lastPuzzleID, 201) }), // N.B. it overwrites lastPlayerID variable

            Pair(7, { putPuzzleTile(lastPuzzleID, lastPlayerID, lastTilesIDs.first(), 0, 0, 200) }),
            Pair(7, { putPuzzleTile(lastPuzzleID, notExistingPlayer(), lastTilesIDs.first(), 0, 0, 401) }),
            Pair(7, { putPuzzleTile(notExistingPuzzle(), notExistingPlayer(), notExistingTile(), 0, 0, 404) }),
            Pair(7, { putPuzzleTile(lastPuzzleID, lastPlayerID, lastTilesIDs.first(), Int.MIN_VALUE, Int.MIN_VALUE, 409) })
        ).onSuccess { callbackCount.set { 6 } }

        /*val gotPuzzleTiles = Promise.promise<List<String>>()
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
        }*/

        callbackCount.deadline(2000)
        assertEquals("All the callback should have been called", 6, callbackCount.result)
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
    private fun putMousePosition(puzzleID: String, playerID: String): Future<Boolean> {
        val result = Promise.promise<Boolean>()

        client.put(Gateway.PORT, "localhost", "/puzzle/${puzzleID}a/mouses").sendJsonObject(
            JsonObject().put("timeStamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")))
                .put("playerToken", playerID).put("position", JsonObject().put("x", 5).put("y", 5))
        ) {
            assertEquals(404, it.result().statusCode())
        }
        val notExpectedCode = AtomicInteger()
        for (i in 0..1) {
            client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/mouses").sendJsonObject(
                JsonObject().put("timeStamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")))
                    .put("playerToken", playerID).put("position", JsonObject().put("x", 5).put("y", 5))
            ) {
                synchronized(notExpectedCode) {
                    val code = it.result().statusCode()

                    if (code == 201 && notExpectedCode.get() != 201) {
                        notExpectedCode.set(201)
                    } else if (code == 200 && notExpectedCode.get() != 200) {
                        notExpectedCode.set(200)
                        result.complete(true)
                    } else fail("Got response code $code at mouse position submission")
                }
            }
        }
        result.future().onComplete {
            client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/mouses").sendJsonObject(
                JsonObject().put("timeStamp", LocalDateTime.now().minusDays(5).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")))
                    .put("playerToken", playerID).put("position", JsonObject().put("x", 5).put("y", 5))
            ) { assertEquals(409, it.result().statusCode()) }
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
            assertEquals(expectedResult, it.result().bodyAsJsonObject().getJsonArray("positions").size())
        }
        return result.future()
    }
}
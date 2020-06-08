package test.kotlin.service

import io.vertx.core.Vertx
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import main.kotlin.service.Gateway
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.random.Random

/**
 * Tests multiple players that make call to the service
 * It's aim to find service faults
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
class ConcurrentPlay : AbsServiceTest() {
    private lateinit var puzzleID: String
    private val callbackCount = ResultLock(0, 600)
    private var positionUpdates = 0

    @Test fun testPlay() {

        postPuzzle().thenAccept {
            for (i in 0..2) {
                Executors.newCachedThreadPool().execute {
                    val wc = WebClient.create(Vertx.vertx())
                    postPlayer(wc).thenAccept {
                        nonBlockingLongRunningPuts(wc, it, 100)
                    }
                }
            }

            for (i in 0..2) {
                Executors.newCachedThreadPool().execute {
                    val wc = WebClient.create(Vertx.vertx())
                    postPlayer(wc).thenAccept { player ->
                        openWebSocket().thenAccept {
                            nonBlockingLongRunningPuts(wc, it as WebSocket, player, 100)
                        }
                    }
                }
            }
        }

        callbackCount.deadline(10000)
        assertEquals("All the callback should have been called", 600, callbackCount.result)
        assert(positionUpdates > 0)
    }

    private fun postPuzzle(): CompletableFuture<Any> {
        val returns = CompletableFuture<Any>()

        client.post(Gateway.PORT, "localhost", "/puzzle").send {
            assert (it.succeeded())
            assertNotEquals(500, it.result().statusCode())

            puzzleID = it.result().bodyAsJsonObject().getString("puzzleID")
            returns.complete(Unit)
        }
        return returns
    }

    private fun postPlayer(client: WebClient): CompletableFuture<String> {
        val result = CompletableFuture<String>()

        client.post(Gateway.PORT, "localhost", "/puzzle/$puzzleID/user").send {
            assert (it.succeeded())
            assertNotEquals(500, it.result().statusCode())

            result.complete(it.result().bodyAsJsonObject().getString("playerToken"))
        }
        return result
    }

    private fun putPuzzleTile(client: WebClient, puzzleID: String, playerID: String, column: Int, row: Int): CompletableFuture<Any> {
        val result = CompletableFuture<Any>()

        client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/0").sendJsonObject(
            JsonObject().put("playerToken", playerID).put("newColumn", column).put("newRow", row)
        ) {
            assert (it.succeeded())
            assertNotEquals(500, it.result().statusCode())
            assertEquals(200, it.result().statusCode())
            result.complete(Unit)
        }
        return result
    }

    private fun openWebSocket(): CompletableFuture<Any> {
        val result = CompletableFuture<Any>()

        vertx.createHttpClient().webSocket(Gateway.PORT, "localhost", "/puzzle/$puzzleID/mouses") {
            println(it.result())
            it.result().binaryMessageHandler { positionUpdates++ }
            result.complete(it.result())
        }
        return result
    }

    private fun putPointerPosition(ws: WebSocket, playerID: String): Future<Any> {
        val message = JsonObject().put("timeStamp", timeStamp()).put("playerToken", playerID).put("position",
            JsonObject().put("x", Random.nextInt(50)).put("y", Random.nextInt(50)))

        ws.writeBinaryMessage(message.toBuffer())
        return CompletableFuture.completedFuture(Unit)
    }

    private fun nonBlockingLongRunningPuts(client: WebClient, playerID: String, dec: Int): CompletableFuture<Any> {
        if (dec == 0) return CompletableFuture.completedFuture(Unit)
        val result = CompletableFuture<Any>()
        client.put(Gateway.PORT, "localhost", "/puzzle/$puzzleID/0").sendJsonObject(
            JsonObject().put("playerToken", playerID).put("newColumn", Random.nextInt(4)).put("newRow", Random.nextInt(2))
        ) {
            assert (it.succeeded())
            assertNotEquals(500, it.result().statusCode())
            assertEquals(200, it.result().statusCode())
            nonBlockingLongRunningPuts(client, playerID, dec -1).thenAccept {
                callbackCount.set { callbackCount.result +1 }
                result.complete(Unit)
            }
        }
        return result
    }

    private fun nonBlockingLongRunningPuts(wc: WebClient, ws: WebSocket, playerID: String, dec: Int): CompletableFuture<Any> {
        if (dec == 0) return CompletableFuture.completedFuture(Unit)
        val result = CompletableFuture<Any>()
        putPuzzleTile(wc, puzzleID, playerID, Random.nextInt(4), Random.nextInt(2)).thenAccept {
            nonBlockingLongRunningPuts(wc, ws, playerID, dec -1).thenAccept {
                callbackCount.set { callbackCount.result +1 }
                result.complete(Unit)
            }
        }
        putPointerPosition(ws, playerID)
        return result
    }

    private fun timeStamp() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
}
package test.kotlin.service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import org.junit.Assert
import org.junit.Test
import main.kotlin.service.Gateway
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

/**
 * Tests system coherence after mouse's position submission through WebSocket
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
class WebSocket : AbsServiceTest() {
    private lateinit var puzzleID: String
    private lateinit var playerID: String
    private lateinit var webSocket: WebSocket
    private var expectedMessages: Int = 0

    @Test fun testMouse() {
        val callbackCount = ResultLock(0, 3)

        runSequentially(
            Pair(0) { postPuzzle() },

            Pair(1) { openWebSocket() },

            Pair(2) {
                webSocket.binaryMessageHandler {
                    Assert.assertEquals(expectedMessages, JsonObject(it).getJsonArray("positions").size())
                    callbackCount.set { callbackCount.result +1 }
                }
                Future.succeededFuture<Any>()
            },
            Pair(2) { postPlayer() },

            Pair(3) { putPointerPosition(true) },

            Pair(4) { putPointerPosition() },

            Pair(5) { postPlayer() },

            Pair(6) { putPointerPosition(true) }
        )

        callbackCount.deadline(2000)
        assertEquals(3, callbackCount.result, "All the callback should have been called")
    }

    private fun openWebSocket(): Future<Any> {
        val result = Promise.promise<Any>()

        vertx.createHttpClient().webSocket(Gateway.PORT, "localhost", "/puzzle/$puzzleID/mouses") {
            webSocket = it.result()
            result.complete()
        }
        return result.future()
    }

    private fun postPuzzle(): Future<Any> {
        val returns = Promise.promise<Any>()

        client.post(Gateway.PORT, "localhost", "/puzzle").send {
            puzzleID = it.result().bodyAsJsonObject().getString("puzzleID")
            returns.complete()
        }
        return returns.future()
    }

    private fun postPlayer(): Future<Any> {
        val result = Promise.promise<Any>()

        client.post(Gateway.PORT, "localhost", "/puzzle/$puzzleID/user").send() {
            playerID = it.result().bodyAsJsonObject().getString("playerToken")
            result.complete()
        }
        return result.future()
    }

    private fun putPointerPosition(incPointerCounter: Boolean = false): Future<Any> {
        val message = JsonObject().put("timeStamp", timeStamp()).put("playerToken", playerID).put("position",
            JsonObject().put("x", 50).put("y", 60))

        webSocket.writeBinaryMessage(message.toBuffer())
        if (incPointerCounter) expectedMessages++

        return Future.succeededFuture()
    }

    private fun timeStamp() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
}
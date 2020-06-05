package service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class TestWebSocket {
    private val vertx = Vertx.vertx()
    private val client = WebClient.create(vertx)

    @Test fun test() {
        val result = ResultLock(0, 3)

        createPuzzle().onSuccess { puzzleID ->
            result.set { result.result +1 }
            joinWithUser(puzzleID).onSuccess { player ->
                result.set { result.result +1 }
                sendAndReceiveData(puzzleID, player).onComplete { result.set { result.result +1 } }
            }.onFailure { Assert.fail() }
        }.onFailure { Assert.fail() }

        result.deadline(2000)
        assertEquals(3, result.result, "Other problems in service prevent from run this")
    }

    private fun sendAndReceiveData(puzzleID: String, playerID: String): Future<Boolean> {
        val result = Promise.promise<Boolean>()

        val client: HttpClient = vertx.createHttpClient()
        val path = "/client.puzzle/${puzzleID}/mouses"

        client.webSocket(Gateway.PORT, "localhost", path) {
            val msg = JsonObject().put("playerToken", playerID).put("position", JsonObject().put("x", 50).put("y", 60))

            it.result().writeBinaryMessage(msg.toBuffer())

            it.result().binaryMessageHandler { buffer ->
                assert(buffer.toJsonObject() == msg)
                result.complete(true)
            }
        }
        return result.future()
    }

    private fun createPuzzle(): Future<String> {
        val returns = Promise.promise<String>()

        client.post(Gateway.PORT, "localhost", "/client/puzzle").send {
            val body = it.result().bodyAsJsonObject()
            returns.complete(body.getString("puzzleID"))
        }
        return returns.future()
    }

    private fun joinWithUser(puzzleID: String): Future<String> {
        val playerID = "Marcantonio"
        val result = Promise.promise<String>()

        client.post(Gateway.PORT, "localhost", "/client.puzzle/$puzzleID/user").sendJson(
            JsonObject().put("playerID", playerID)
        ) { result.complete(it.result().bodyAsJsonObject().getString("playerToken")) }
        return result.future()
    }

    @Before fun startService() {
        val customLock = ResultLock(false)
        val complete = Promise.promise<Void>()
        Vertx.vertx().deployVerticle(Gateway(complete))
        complete.future().onComplete { customLock.set { true } }
        customLock.deadline(2000)
        assert(customLock.result)
    }
}
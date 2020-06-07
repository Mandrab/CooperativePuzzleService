package test.kotlin.service

import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonObject
import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Test
import service.Gateway
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class WebSocket : AbsServiceTest() {

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
        assertEquals("Other problems in service prevent from run this",3, result.result)
    }

    private fun sendAndReceiveData(puzzleID: String, playerID: String): Future<Boolean> {
        val result = Promise.promise<Boolean>()

        val client: HttpClient = vertx.createHttpClient()
        val path = "/puzzle/${puzzleID}/mouses"

        client.webSocket(Gateway.PORT, "localhost", path) {
            val msg = JsonObject().put("timeStamp", timeStamp()).put("playerToken", playerID).put("position", JsonObject().put("x", 50).put("y", 60))

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

        client.post(Gateway.PORT, "localhost", "/puzzle").send {
            val body = it.result().bodyAsJsonObject()
            returns.complete(body.getString("puzzleID"))
        }
        return returns.future()
    }

    private fun joinWithUser(puzzleID: String): Future<String> {
        val playerID = "Marcantonio"
        val result = Promise.promise<String>()

        client.post(Gateway.PORT, "localhost", "/puzzle/$puzzleID/user").sendJson(
            JsonObject().put("playerID", playerID)
        ) { result.complete(it.result().bodyAsJsonObject().getString("playerToken")) }
        return result.future()
    }

    private fun timeStamp() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
}
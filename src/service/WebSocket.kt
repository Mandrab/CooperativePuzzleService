package service

import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import service.db.DBConnector
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
/*
This object represent a webSocket.
This is an alternative to Rest API polling.

@author Baldini Paolo, Battistini Ylenia
 */
object WebSocket {

    /*
    This function verifies that the request matching with pattern. If so, call the handler.
     */
    internal fun webSocketHandler(ws: ServerWebSocket, vertx: Vertx) {
        when (ws.path()) {
            in Regex("/puzzle\\/([A-Z]|[a-z]|[0-9])*\\/mouses") -> mouseHandler(ws, vertx)
            else -> {
                println("Socket connection attempt rejected")
                ws.reject()
            }
        }
    }

    private operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

    /*
    This is the handler function.
    This function extracts the information from the jsonObject and,
    if the timestamp is more updated than the one I had updates the position
     */
    private fun mouseHandler(ws: ServerWebSocket, vertx: Vertx) {
        ws.binaryMessageHandler {
            val jObject = it.toJsonObject()
            if (listOf("playerToken", "position") !in jObject) return@binaryMessageHandler
            val position = jObject.getJsonObject("position")
            if (listOf("x", "y") !in position) return@binaryMessageHandler

            val puzzleID = ws.path().removeSurrounding("/puzzle/", "/mouses")
            if (puzzleID !in DBConnector.getPuzzlesIDs()) return@binaryMessageHandler

            val playerToken = jObject.getString("playerToken")
            val x = position.getInteger("x")
            val y = position.getInteger("y")
            val timestamp = jObject.getString("timeStamp")?.let { timestamp ->
                LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
            }!!
            DBConnector.newPosition(puzzleID, playerToken, x, y, timestamp)

            DBConnector.playerWS(puzzleID, jObject.getString("playerToken"), ws.binaryHandlerID())
            DBConnector.playersWS(puzzleID).forEach { id -> vertx.eventBus().send(id, it) }
        }
    }

    private operator fun JsonObject.contains(key: String): Boolean = this.containsKey(key)

    private operator fun JsonObject.contains(keys: Collection<String>): Boolean = keys.all { this.containsKey(it) }
}
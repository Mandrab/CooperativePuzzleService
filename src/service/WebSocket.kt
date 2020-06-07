package service

import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import service.db.DBConnector

object WebSocket {

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
            // TODO DBConnector.newPosition(puzzleID, playerToken, x, y)

            DBConnector.playerWS(puzzleID, jObject.getString("playerToken"), ws.binaryHandlerID())
            DBConnector.playersWS(puzzleID).forEach { id -> vertx.eventBus().send(id, it) }
        }
    }

    private operator fun JsonObject.contains(key: String): Boolean = this.containsKey(key)

    private operator fun JsonObject.contains(keys: Collection<String>): Boolean = keys.all { this.containsKey(it) }
}
package service

import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import service.db.DBConnector
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * This object represent a webSocket.
 * This is an alternative to Rest API polling.
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
object WebSocket {

    /**
     * This function verifies that the request matching with pattern. If so, call the handler.
     */
    internal fun webSocketHandler(ws: ServerWebSocket, vertx: Vertx) {
        when (ws.path()) {
            in Regex("/puzzle\\/([A-Z]|[a-z]|[0-9])*\\/mouses") -> {
                val puzzleID = ws.path().removeSurrounding("/puzzle/", "/mouses")

                if (DBConnector.getPuzzlePlayers(puzzleID).all { it.socketHandlerID == null }) {
                    vertx.setPeriodic(300) { sendPositions(puzzleID, vertx) }
                }
                mouseHandler(puzzleID, ws)
            }
            else -> {
                println("Socket connection attempt rejected")
                ws.reject()
            }
        }
    }

    private operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

    /**
     * This is the handler function.
     * This function extracts the information from the jsonObject and,
     * if the timestamp is more updated than the one I had updates the position
     */
    private fun mouseHandler(puzzleID: String, ws: ServerWebSocket) {
        ws.binaryMessageHandler {
            val jObject = it.toJsonObject()

            if (listOf("timeStamp", "playerToken", "position") !in jObject) return@binaryMessageHandler
            val position = jObject.getJsonObject("position")
            if (listOf("x", "y") !in position) return@binaryMessageHandler

            if (puzzleID !in DBConnector.getPuzzlesIDs()) return@binaryMessageHandler

            val timestamp = jObject.getString("timeStamp")?.let { timestamp ->
                LocalDateTime.parse(timestamp, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
            }!!
            val playerToken = jObject.getString("playerToken")
            val playerInfo = DBConnector.getPuzzlePlayers(puzzleID).firstOrNull { it.playerID == playerToken } ?: return@binaryMessageHandler

            if (playerInfo.timeStamp != null && timestamp.isBefore(playerInfo.timeStamp)) return@binaryMessageHandler

            val x = position.getInteger("x")
            val y = position.getInteger("y")
            DBConnector.newPosition(puzzleID, playerToken, x, y, timestamp)

            DBConnector.playerWS(puzzleID, jObject.getString("playerToken"), ws.binaryHandlerID())
        }
    }

    private fun sendPositions(puzzleID: String, vertx: Vertx) {
        val players = DBConnector.getPuzzlePlayers(puzzleID)

        val message = JsonObject()
            .put("timeStamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS")))
            .put("positions", JsonArray().apply {
                players.filter { it.lastPosition != null }.map { playerInfo ->
                    JsonObject().put("playerID", playerInfo.playerID).put(
                        "position",
                        JsonObject().put("x", playerInfo.lastPosition!!.first).put("y", playerInfo.lastPosition.second)
                    )
                }.forEach { add(it) }
            })
        players.filter { it.socketHandlerID != null }.forEach { player ->
            vertx.eventBus().send(player.socketHandlerID, message.toBuffer())
        }
    }

    private operator fun JsonObject.contains(key: String): Boolean = this.containsKey(key)

    private operator fun JsonObject.contains(keys: Collection<String>): Boolean = keys.all { this.containsKey(it) }
}
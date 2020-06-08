package service.db

import io.vertx.core.json.JsonObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
/*
This class represent a player information.

@param playerID, id of player
@param socketHandlerID, socket handler
@param last position, position of player (last update)
@param timeStamp, date-time

@author Baldini Paolo, Battistini Ylenia
 */
data class PlayerInfo(
    val playerID: String,
    val socketHandlerID: String? = null,
    val lastPosition: Pair<Int, Int>? = null,
    val timeStamp: LocalDateTime? = null
) {
    /*
    This method add all information in a JSonObject
     */
    fun toJson(): JsonObject = JsonObject().apply {
        put("id", playerID)
        socketHandlerID?.let { put("socket_handler_id", it) }
        lastPosition?.let { put("last_position", JsonObject().put("x", it.first).put("y", it.second)) }
        timeStamp?.let { put("timestamp", it.format(DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT))) }
    }

    companion object {
        const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS"
        /*
        This method parse all information from JSonObject
        */
        fun parse(json: JsonObject): PlayerInfo = PlayerInfo(
            json.getString("id"),
            json.getString("socket_handler_id"),
            json.getJsonObject("last_position")?.let {
                Pair(
                    it.getInteger("x"),
                    it.getInteger("y")
                )
            },
            json.getString("timestamp")?.let {
                LocalDateTime.parse(it, DateTimeFormatter.ofPattern(TIMESTAMP_FORMAT))
            }
        )
    }
}
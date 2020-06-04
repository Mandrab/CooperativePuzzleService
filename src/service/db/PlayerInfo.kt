package service.db

import io.vertx.core.json.JsonObject

data class PlayerInfo(
    val playerID: String,
    val socketHandlerID: String? = null,
    val lastPosition: Pair<Int, Int>? = null
) {
    fun toJson(): JsonObject = JsonObject().apply {
        put("id", playerID)
        socketHandlerID?.let { put("socket_handler_id", it) }
        lastPosition?.let { put("last_position", JsonObject().put("x", it.first).put("y", it.second)) }
    }

    companion object {
        fun parse(json: JsonObject): PlayerInfo = PlayerInfo(
            json.getString("id"),
            if (json.containsKey("socket_handler_id")) json.getString("socket_handler_id") else null,
            if (json.containsKey("last_position")) {
                Pair(
                    json.getJsonObject("last_position").getInteger("x"),
                    json.getJsonObject("last_position").getInteger("y")
                )
            }
            else null
        )
    }
}
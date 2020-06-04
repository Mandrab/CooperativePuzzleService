package service.db

import io.vertx.core.json.JsonObject

data class PlayerInfo(
    val playerID: String,
    val textSocketHandlerID: String? = null,
    val binarySocketHandlerID: String? = null,
    val lastPosition: Pair<Int, Int>? = null
) {
    fun toJson(): JsonObject = JsonObject().apply {
        put("id", playerID)
        textSocketHandlerID?.let { put("text_socket_handler_id", it) }
        binarySocketHandlerID?.let { put("binary_socket_handler_id", it) }
        lastPosition?.let { put("last_position", JsonObject().put("x", it.first).put("y", it.second)) }
    }

    companion object {
        fun parse(json: JsonObject): PlayerInfo = PlayerInfo(
            json.getString("id"),
            if (json.containsKey("text_socket_handler_id")) json.getString("text_socket_handler_id")
            else null,
            if (json.containsKey("binary_socket_handler_id")) json.getString("binary_socket_handler_id")
            else null,
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
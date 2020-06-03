package service.db

import io.vertx.core.json.JsonObject

data class PlayerInfo(
    val playerID: String,
    val textSocketHandlerID: String? = null,
    val binarySocketHandlerID: String? = null
) {
    fun toJson(): JsonObject = JsonObject().apply {
        put("id", playerID)
        textSocketHandlerID?.let { put("text_socket_handler_id", it) }
        binarySocketHandlerID?.let { put("binary_socket_handler_id", it) }
    }

    companion object {
        fun parse(json: JsonObject): PlayerInfo = PlayerInfo(
            json.getString("id"),
            json.getString("text_socket_handler_id"),
            json.getString("binary_socket_handler_id")
        )
    }
}
package service.db

import io.vertx.core.json.JsonObject

data class TileInfo(
    val tileID: String,
    val tileImageURL: String,
    val originalPosition: Pair<Int, Int>,
    val currentPosition: Pair<Int, Int>
) {
    fun toJson(): JsonObject = JsonObject().apply {
        put("id", tileID)
        put("imageURL", tileImageURL)
        put("original_position", JsonObject().apply {
            put("x", originalPosition.first)
            put("y", originalPosition.second)
        })
        put("current_position", JsonObject().apply {
            put("x", currentPosition.first)
            put("y", currentPosition.second)
        })
    }

    companion object {
        fun parse(json: JsonObject): TileInfo = TileInfo(
            json.getString("id"),
            json.getString("imageURL"),
            Pair(
                json.getJsonObject("original_position").getInteger("x"),
                json.getJsonObject("original_position").getInteger("y")
            ),
            Pair(
                json.getJsonObject("current_position").getInteger("x"),
                json.getJsonObject("current_position").getInteger("y")
            )
        )
    }
}
package service.db

import io.vertx.core.json.JsonObject

data class PuzzleInfo(
    val puzzleID: String,
    val imageURL: String,
    val gridX: Int,
    val gridY: Int
) {
    fun toJson(): JsonObject = JsonObject().apply {
        put("id", puzzleID)
        put("image_url", imageURL)
        put("size", JsonObject().apply {
            put("horizontal", gridX)
            put("vertical", gridY)
        })
    }

    companion object {
        fun parse(json: JsonObject): PuzzleInfo = PuzzleInfo(
            json.getString("id"),
            json.getString("image_url"),
            json.getJsonObject("size").getInteger("horizontal"),
            json.getJsonObject("size").getInteger("vertical")
        )
    }
}
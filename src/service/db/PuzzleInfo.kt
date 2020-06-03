package service.db

import io.vertx.core.json.JsonObject

data class PuzzleInfo(
    val puzzleID: String,
    val imageURL: String,
    val columnsCount: Int,
    val rowsCount: Int
) {
    fun toJson(): JsonObject = JsonObject().apply {
        put("id", puzzleID)
        put("image_url", imageURL)
        put("size", JsonObject().apply {
            put("horizontal", columnsCount)
            put("vertical", rowsCount)
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
package service.db

import io.vertx.core.json.JsonObject


data class PuzzleInfo(
    val puzzleID: String,
    val imageURL: String,
    val columnsCount: Int,
    val rowsCount: Int,
    val status: Status = Status.STARTED
) {
    enum class Status {
        STARTED,
        COMPLETED
    }

    fun toJson(): JsonObject = JsonObject().apply {
        put("id", puzzleID)
        put("image_url", imageURL)
        put("size", JsonObject().apply {
            put("horizontal", columnsCount)
            put("vertical", rowsCount)
        })
        put("status", status.name)
    }

    companion object {
        fun parse(json: JsonObject): PuzzleInfo = PuzzleInfo(
            json.getString("id"),
            json.getString("image_url"),
            json.getJsonObject("size").getInteger("horizontal"),
            json.getJsonObject("size").getInteger("vertical"),
            Status.valueOf(json.getString("status"))
        )
    }
}
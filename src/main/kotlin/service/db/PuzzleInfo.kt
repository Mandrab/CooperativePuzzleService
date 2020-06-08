package main.kotlin.service.db

import io.vertx.core.json.JsonObject

/**
 * This class represent a player information.
 *
 * @param puzzleID, id of puzzle
 * @param imageURL, url of the puzzle image
 * @param columnsCount, columns of puzzle image
 * @param rowsCount, rows of puzzle image
 * @param status, status of puzzle (STARTED or COMPLETED)
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
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

    /**
     * This method add all information in a JSonObject
     */
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

        /**
         * This method parse all information from JSonObject into a PuzzleInfo
         */
        fun parse(json: JsonObject): PuzzleInfo = PuzzleInfo(
            json.getString("id"),
            json.getString("image_url"),
            json.getJsonObject("size").getInteger("horizontal"),
            json.getJsonObject("size").getInteger("vertical"),
            Status.valueOf(json.getString("status"))
        )
    }
}
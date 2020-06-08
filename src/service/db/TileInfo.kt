package service.db

import io.vertx.core.json.JsonObject

/**
 * This class represent a tile information.
 *
 * @param tileID, id of tile
 * @param tileImageURL, url of the tile image
 * @param originalPosition, correct position of the tile in the puzzle
 * @param currentPosition, position of tile in puzzle (last update)
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
data class TileInfo(
    val tileID: String,
    val tileImageURL: String,
    val originalPosition: Pair<Int, Int>,
    val currentPosition: Pair<Int, Int>
) {
    /**
     * This method add all information in a JSonObject
     */
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
        /**
         * This method parse all information from JSonObject
         */
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
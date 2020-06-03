package service.db

import io.vertx.core.json.JsonObject
import java.io.File

/**
 * This class *simulate* a db-connector to store files on a DB.
 * Truly, it uses file storage.
 *
 * TODO use it as monitor ?
 *
 * @author Paolo Baldini
 */
object DBConnector {
    private const val PUZZLES_LIST_FILE = "puzzles_list"
    private const val PLAYERS_SUFFIX = "_players"
    private const val TILES_SUFFIX = "_tiles"

    fun addPuzzle(puzzleID: String, imageURL: String, gridX: Int, gridY: Int) {
        File(PUZZLES_LIST_FILE).appendText(PuzzleInfo(puzzleID, imageURL, gridX, gridY).toJson().toString() + "\n")
        File(puzzleID + TILES_SUFFIX).writeText((0 until gridY).joinToString("\n") { y ->
            (0 until gridX).joinToString("\n") { x ->
                TileInfo("${x + gridX * y}", Pair(x, y), Pair(x, y)).toJson().toString()
            }
        })
    }

    fun getPuzzlesID(): List<String> = File(PUZZLES_LIST_FILE).takeIf { it.exists() }?.readLines()
            ?.map { PuzzleInfo.parse(JsonObject(it)).puzzleID } ?: emptyList()

    fun getPuzzleTiles(puzzleID: String) = File(puzzleID + TILES_SUFFIX).readLines()
            .map { TileInfo.parse(JsonObject(it)) }

    fun addPlayer(puzzleID: String, playerID: String): Boolean {
        if (!getPuzzlesID().contains(puzzleID)) return false
        File(puzzleID + PLAYERS_SUFFIX).appendText(PlayerInfo(playerID).toJson().toString() + "\n")
        return true
    }

    fun updateTilePosition(puzzleID: String, tileID: String, newPosX: Int, newPosY: Int): Boolean {
        if (!getPuzzlesID().contains(puzzleID)) return false

        val tiles = File(puzzleID + TILES_SUFFIX).readLines().map { TileInfo.parse(JsonObject(it)) }
        val swapTile1 = tiles.firstOrNull { it.tileID == tileID }
        val swapTile2 = tiles.firstOrNull { it.currentPosition == Pair(newPosX, newPosY) }

        if (swapTile1 == null || swapTile2 == null) return false

        File(puzzleID + TILES_SUFFIX).writeText(
            tiles.joinToString("\n") {
                when (it.tileID) {
                    swapTile1.tileID -> TileInfo(
                        swapTile1.tileID,
                        swapTile1.originalPosition,
                        swapTile2.currentPosition
                    )
                    swapTile2.tileID -> TileInfo(
                        swapTile2.tileID,
                        swapTile2.originalPosition,
                        swapTile1.currentPosition
                    )
                    else -> it
                }.toJson().toString()
            }
        )
        return true
    }

    fun playerWS(puzzleID: String, playerID: String, textHandlerID: String?, binaryHandlerID: String? = null): Boolean {
        if (!getPuzzlesID().contains(puzzleID)) return false

        File(puzzleID + PLAYERS_SUFFIX).writeText(
            File(puzzleID + PLAYERS_SUFFIX).readLines().map { PlayerInfo.parse(JsonObject(it)) }
                .joinToString("\n") {
                    when (it.playerID) {
                        playerID -> PlayerInfo(
                            it.playerID,
                            textHandlerID ?: it.textSocketHandlerID,
                            binaryHandlerID ?: it.binarySocketHandlerID)
                        else -> it
                    }.toJson().toString()
            }
        )
        return true
    }

    fun playerTextWS(puzzleID: String, playerID: String): String? = File(puzzleID + PLAYERS_SUFFIX).readLines()
        .map { PlayerInfo.parse(JsonObject(it)) }.firstOrNull { it.playerID == playerID }?.textSocketHandlerID

    fun playerBinaryWS(puzzleID: String, playerID: String): String? = File(puzzleID + PLAYERS_SUFFIX).readLines()
        .map { PlayerInfo.parse(JsonObject(it)) }.firstOrNull { it.playerID == playerID }?.binarySocketHandlerID

    fun playersWS(puzzleID: String): List<String> = File(puzzleID + PLAYERS_SUFFIX).readLines()
            .map { PlayerInfo.parse(JsonObject(it)) }.mapNotNull { it.binarySocketHandlerID ?: it.textSocketHandlerID }
}
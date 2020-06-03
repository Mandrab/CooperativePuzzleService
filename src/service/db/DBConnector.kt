package service.db

import io.vertx.core.json.JsonObject
import java.io.File
import kotlin.random.Random

/**
 * This class *simulate* a db-connector to store files on a DB.
 * Truly, it uses file storage.
 *
 * TODO use it as monitor ?
 *
 * @author Paolo Baldini
 */
object DBConnector {
    private val PATH_PREFIX = ".trash${File.separator}"
    private val PUZZLES_LIST_FILE = "${PATH_PREFIX}puzzles_list"
    private const val PLAYERS_SUFFIX = "_players"
    private const val TILES_SUFFIX = "_tiles"

    fun addPuzzle(puzzleID: String, imageURL: String, colsCount: Int, rowsCount: Int): Boolean {
        val puzzles = File(PUZZLES_LIST_FILE).takeIf { it.exists() }?.readLines()?.map { PuzzleInfo.parse(JsonObject(it)) }
        if (puzzles != null && puzzles.any { it.puzzleID == puzzleID }) return false

        File(PUZZLES_LIST_FILE).appendText(PuzzleInfo(puzzleID, imageURL, colsCount, rowsCount).toJson().toString() + "\n")
        File(PATH_PREFIX + puzzleID + TILES_SUFFIX).writeText((0 until rowsCount).joinToString("\n") { y ->
            (0 until colsCount).joinToString("\n") { x ->
                TileInfo("${x + colsCount * y}", Pair(x, y), Pair(x, y)).toJson().toString()
            }
        })
        return true
    }

    fun getPuzzleInfo(puzzleID: String): PuzzleInfo? = File(PUZZLES_LIST_FILE).takeIf { it.exists() }?.readLines()
            ?.map { PuzzleInfo.parse(JsonObject(it)) }?.firstOrNull { it.puzzleID == puzzleID }

    fun getPuzzleTiles(puzzleID: String) = File(PATH_PREFIX + puzzleID + TILES_SUFFIX).readLines()
        .map { TileInfo.parse(JsonObject(it)) }

    fun getPuzzlePlayers(puzzleID: String): List<PlayerInfo> = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX)
        .takeIf { it.exists() }?.readLines()?.map { PlayerInfo.parse(JsonObject(it)) } ?: emptyList()

    fun getPuzzlesID(): List<String> = File(PUZZLES_LIST_FILE).takeIf { it.exists() }?.readLines()
            ?.map { PuzzleInfo.parse(JsonObject(it)).puzzleID } ?: emptyList()

    fun addPlayer(puzzleID: String): String? {
        if (!getPuzzlesID().contains(puzzleID)) return null
        val lines = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).takeIf { it.exists() }?.readLines() ?: emptyList()
        var playerID: String
        do playerID = Random.nextInt(0, Int.MAX_VALUE).toString()
        while(lines.contains(playerID))
        File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).appendText(PlayerInfo(playerID).toJson().toString() + "\n")
        return playerID
    }

    fun updateTilePosition(puzzleID: String, tileID: String, newColumn: Int, newRow: Int): Boolean {
        if (!getPuzzlesID().contains(puzzleID)) return false

        val tiles = File(PATH_PREFIX + puzzleID + TILES_SUFFIX).readLines().map { TileInfo.parse(JsonObject(it)) }
        val swapTile1 = tiles.firstOrNull { it.tileID == tileID }
        val swapTile2 = tiles.firstOrNull { it.currentPosition == Pair(newColumn, newRow) }

        if (swapTile1 == null || swapTile2 == null) return false

        File(PATH_PREFIX + puzzleID + TILES_SUFFIX).writeText(
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

        File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).writeText(
            File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).readLines().map { PlayerInfo.parse(JsonObject(it)) }
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

    fun playerTextWS(puzzleID: String, playerID: String): String? = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX)
        .readLines().map { PlayerInfo.parse(JsonObject(it)) }.firstOrNull { it.playerID == playerID }?.textSocketHandlerID

    fun playerBinaryWS(puzzleID: String, playerID: String): String? = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX)
        .readLines().map { PlayerInfo.parse(JsonObject(it)) }.firstOrNull { it.playerID == playerID }?.binarySocketHandlerID

    fun playersWS(puzzleID: String): List<String> = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).readLines()
            .map { PlayerInfo.parse(JsonObject(it)) }.mapNotNull { it.binarySocketHandlerID ?: it.textSocketHandlerID }
}
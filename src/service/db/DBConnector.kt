package service.db

import io.vertx.core.json.JsonObject
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import javax.imageio.ImageIO
import kotlin.random.Random


/**
 * This class *simulate* a db-connector to store files on a DB.
 * Truly, it uses file storage.
 *
 * @author Paolo Baldini
 */
object DBConnector {
    private val PATH_PREFIX = "trash${File.separator}"
    private val PUZZLES_LIST_FILE = "${PATH_PREFIX}puzzles_list"
    private const val PLAYERS_SUFFIX = "_players"
    private const val TILES_SUFFIX = "_tiles"

    @Synchronized fun addPuzzle(puzzleID: String, imageURL: String, colsCount: Int, rowsCount: Int): Boolean {
        val puzzles = File(PUZZLES_LIST_FILE).takeIf { it.exists() }?.readLines()?.map { PuzzleInfo.parse(JsonObject(it)) }
        if (puzzles != null && puzzles.any { it.puzzleID == puzzleID }) return false

        File(PUZZLES_LIST_FILE).appendText(PuzzleInfo(puzzleID, imageURL, colsCount, rowsCount).toJson().encode() + System.lineSeparator())
        val tilesPath = genTilesImage(imageURL, puzzleID, colsCount, rowsCount) ?: return false
        val positions = (0 until rowsCount).flatMap { y -> (0 until colsCount).map { x -> Pair(x, y) } }.shuffled().iterator()
        File(PATH_PREFIX + puzzleID + TILES_SUFFIX).writeText((0 until rowsCount).joinToString(System.lineSeparator()) { y ->
            (0 until colsCount).joinToString(System.lineSeparator()) { x -> (x + colsCount * y).let {
                TileInfo("$it", tilesPath[it], Pair(x, y), positions.next()).toJson().encode()
            } }
        })
        return true
    }

    @Synchronized fun getPuzzleInfo(puzzleID: String): PuzzleInfo? = File(PUZZLES_LIST_FILE).takeIf { it.exists() }?.readLines()
            ?.map { PuzzleInfo.parse(JsonObject(it)) }?.firstOrNull { it.puzzleID == puzzleID }

    @Synchronized fun getPuzzleTiles(puzzleID: String) = File(PATH_PREFIX + puzzleID + TILES_SUFFIX).readLines()
        .map { TileInfo.parse(JsonObject(it)) }

    @Synchronized fun getPuzzlePlayers(puzzleID: String): List<PlayerInfo> {
        val file = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).takeIf { it.exists() }
        return file?.readLines()?.map {
            PlayerInfo.parse(JsonObject(it)) } ?: emptyList()
    }

    fun isComplete(puzzleID: String): Boolean = File(PUZZLES_LIST_FILE).readLines()
        .map { PuzzleInfo.parse(JsonObject(it)) }.any { it.puzzleID == puzzleID && it.status == PuzzleInfo.Status.COMPLETED }

    fun complete(puzzleID: String) {
        File(PUZZLES_LIST_FILE).writeText(
            File(PUZZLES_LIST_FILE).readLines().map { PuzzleInfo.parse(JsonObject(it)) }
                .map {
                    if (it.puzzleID == puzzleID)
                        PuzzleInfo(it.puzzleID, it.imageURL, it.columnsCount, it.rowsCount, PuzzleInfo.Status.COMPLETED)
                    else it
                }.joinToString(System.lineSeparator()) { it.toJson().encode() }
        )
    }

    @Synchronized fun getPuzzlesIDs(): List<String> = File(PUZZLES_LIST_FILE).takeIf { it.exists() }?.readLines()
            ?.map { PuzzleInfo.parse(JsonObject(it)).puzzleID } ?: emptyList()

    @Synchronized fun updateTilePosition(puzzleID: String, tileID: String, newColumn: Int, newRow: Int): Boolean {
        if (!getPuzzlesIDs().contains(puzzleID) || isComplete(puzzleID)) return false

        val tiles = File(PATH_PREFIX + puzzleID + TILES_SUFFIX).readLines().map { TileInfo.parse(JsonObject(it)) }
        val swapTile1 = tiles.firstOrNull { it.tileID == tileID }
        val swapTile2 = tiles.firstOrNull { it.currentPosition == Pair(newColumn, newRow) }

        if (swapTile1 == null || swapTile2 == null) return false

        val newTilesList = tiles.map {
            when (it.tileID) {
                swapTile1.tileID -> TileInfo(
                    swapTile1.tileID,
                    swapTile1.tileImageURL,
                    swapTile1.originalPosition,
                    swapTile2.currentPosition
                )
                swapTile2.tileID -> TileInfo(
                    swapTile2.tileID,
                    swapTile2.tileImageURL,
                    swapTile2.originalPosition,
                    swapTile1.currentPosition
                )
                else -> it
            }
        }
        File(PATH_PREFIX + puzzleID + TILES_SUFFIX).writeText(newTilesList.joinToString(System.lineSeparator()) { it.toJson().encode() })
        if (newTilesList.all { it.currentPosition == it.originalPosition }) complete(puzzleID)
        return true
    }

    @Synchronized fun addPlayer(puzzleID: String): String? {
        if (!getPuzzlesIDs().contains(puzzleID)) return null
        val lines = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).takeIf { it.exists() }?.readLines() ?: emptyList()
        var playerID: String
        do playerID = Random.nextInt(0, Int.MAX_VALUE).toString()
        while(lines.contains(playerID))
        File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).appendText(PlayerInfo(playerID).toJson().encode() + System.lineSeparator())
        return playerID
    }

    @Synchronized fun newPosition(puzzleID: String, playerID: String, x: Int, y: Int, timestamp: LocalDateTime) {
        if (puzzleID !in getPuzzlesIDs()) return
        val lines = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).takeIf { it.exists() }?.readLines() ?: emptyList()
        File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).writeText(lines.map { PlayerInfo.parse(JsonObject(it)) }
            .joinToString(System.lineSeparator()) {
                when (it.playerID) {
                    playerID -> PlayerInfo(it.playerID, it.socketHandlerID, Pair(x, y), timestamp)
                    else -> it
                }.toJson().encode()
            })
    }

    @Synchronized fun playerWS(puzzleID: String, playerID: String, socketHandlerID: String? = null): Boolean {
        if (!getPuzzlesIDs().contains(puzzleID)) return false

        File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).writeText(
            File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX).readLines().map { PlayerInfo.parse(JsonObject(it)) }
                .joinToString(System.lineSeparator()) {
                    when (it.playerID) {
                        playerID -> PlayerInfo(it.playerID, socketHandlerID ?: it.socketHandlerID, it.lastPosition, it.timeStamp)
                        else -> it
                    }.toJson().encode()
            }
        )
        return true
    }

    @Synchronized fun playerWS(puzzleID: String, playerID: String): String? = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX)
        .readLines().map { PlayerInfo.parse(JsonObject(it)) }.firstOrNull { it.playerID == playerID }?.socketHandlerID

    @Synchronized fun playersWS(puzzleID: String): List<String> = File(PATH_PREFIX + puzzleID + PLAYERS_SUFFIX)
            .readLines().map { PlayerInfo.parse(JsonObject(it)) }.mapNotNull { it.socketHandlerID }

    private fun genTilesImage(path: String, puzzleID: String, columns: Int, rows: Int): List<String>? {
        val image = try { ImageIO.read(File(path)) }
        catch (ex: IOException) { return null }

        val imageWidth: Int = image.getWidth(null)
        val imageHeight: Int = image.getHeight(null)

        File(PATH_PREFIX + puzzleID).mkdir()
        val paths = mutableListOf<String>()

        for (y in 0 until rows) {
            for (x in 0 until columns) {
                val imagePortion = image.getSubimage(x * imageWidth / columns,
                    y * imageHeight / rows,
                    imageWidth / columns,
                    imageHeight / rows)

                val tilePath = PATH_PREFIX + puzzleID + File.separator + "tile${x + columns * y}.png"
                ImageIO.write(imagePortion, "png", File(tilePath))
                paths.add(tilePath.replace("\\", "/"))
            }
        }
        return paths
    }
}
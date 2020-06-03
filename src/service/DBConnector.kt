package service

import java.io.File

/**
 * This class *simulate* a db-connector to store files on a DB
 */
object DBConnector {
    private val PUZZLES_LIST_FILE = "puzzles_list"
    private val PLAYERS_SUFFIX = "_players"
    private val TILES_SUFFIX = "_tiles"

    fun addPuzzle(puzzleID: String, imageURL: String, gridx: Int, gridy: Int) {
        File(PUZZLES_LIST_FILE).appendText("$puzzleID; $imageURL; $gridx; $gridy")
        File(puzzleID + TILES_SUFFIX).writeText((0 until gridy).joinToString("\n") { y ->
            (0 until gridx).joinToString("\n") { x -> "${x + gridx * y};$x $y;$x $y" }
        })
    }

    fun getPuzzlesID(): List<String> {
        if (File("puzzles_list").exists())
            return File("puzzles_list").readLines().map { it.substringBefore(";") }
        return emptyList()
    }

    fun getPuzzleTiles(puzzleID: String) = File(puzzleID + TILES_SUFFIX).readLines()

    fun addPlayer(puzzleID: String, playerID: String) {
        check(getPuzzlesID().contains(puzzleID))
        File(puzzleID + PLAYERS_SUFFIX).appendText(playerID)
    }

    fun playerWS(puzzleID: String, playerID: String, wsHandlerID: String) {
        check(getPuzzlesID().contains(puzzleID))
        File(puzzleID + PLAYERS_SUFFIX).writeText(
            File(puzzleID + PLAYERS_SUFFIX).readLines().joinToString("\n") {
                if (it.substringBefore(";") == playerID) "${it.substringBefore(";")};$wsHandlerID"
                else it
            }
        )
    }

    fun playerWS(puzzleID: String, playerID: String): String? = File(puzzleID + PLAYERS_SUFFIX).readLines()
            .firstOrNull { it.substringBefore(";") == playerID }?.substringAfter(";")

    fun playersWS(puzzleID: String): List<String> = File(puzzleID + PLAYERS_SUFFIX).readLines()
            .map { it.substringAfter(";", "") }.filter { it.isNotBlank() }
}
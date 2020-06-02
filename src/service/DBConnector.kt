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
            (0 until gridx).joinToString("\n") { x -> "${x + gridx * y} $x $y" }
        })
    }

    fun getPuzzlesID(): List<String> {
        if (File("puzzles_list").exists())
            return File("puzzles_list").readLines().map { it.substringBefore(";") }
        return emptyList()
    }

    fun getPuzzleTiles(puzzleID: String) = File(puzzleID + TILES_SUFFIX).readLines()
            .map { it.substringBefore(";").substringBefore(";") }

    fun addPlayer(puzzleID: String, playerID: String) {
        check(getPuzzlesID().contains(puzzleID))
        File(puzzleID + PLAYERS_SUFFIX).appendText(playerID)
    }
}
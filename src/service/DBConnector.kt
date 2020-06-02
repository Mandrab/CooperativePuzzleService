package service

import java.io.File

/**
 * This class *simulate* a db-connector to store files on a DB
 */
object DBConnector {
    private val PUZZLES_LIST_FILE = "puzzles_list"
    private val PLAYERS_SUFFIX = "_players"
    private val TILES_SUFFIX = "_tiles"

    fun addPuzzle(puzzleID: String) {
        File(puzzleID + PUZZLES_LIST_FILE).appendText(puzzleID)
    }

    fun getPuzzlesID() = File("puzzles_list").readLines()

    fun addPlayer(puzzleID: String, playerID: String) {
        check(getPuzzlesID().contains(puzzleID))
        File(puzzleID + PLAYERS_SUFFIX).appendText(playerID)
    }
}
package main.kotlin.client.view

import java.awt.Image

/**
 * This class represent a Tile.
 * @param image, image of tile
 * @param tileID, id of tile
 * @param currentPosition, array of tile position(x,y)
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
data class Tile(val image: Image, val tileID: String, var currentPosition: Pair<Int, Int>) : Comparable<Tile> {

    /**
     * This method compare two tile.
     * If position are equal return 0, if x position is less than the other and y position is less or equal than the other return -1
     * if x position is less than the other but y position is greater than the other return 1.
     */
    override fun compareTo(other: Tile): Int {
        if (currentPosition == other.currentPosition) return 0
        if (currentPosition.first < other.currentPosition.first) {
            if (currentPosition.second <= other.currentPosition.second) return -1
            if (currentPosition.second > other.currentPosition.second) return 1
        }
        if (currentPosition.second < other.currentPosition.second) return -1
        return 1
     }
 }


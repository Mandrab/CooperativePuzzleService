package client.view

import java.awt.Image


data class Tile(val image: Image, val tileID: String, var currentPosition: Pair<Int, Int>) : Comparable<Tile> {

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


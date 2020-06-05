package src.puzzle

import java.awt.Image

class Tile(val image: Image, val tileID: String, var currentPosition: Pair<Int, Int>) : Comparable<Tile?> {

     override fun compareTo(other: Tile?): Int {
         return if (currentPosition.first < other!!.currentPosition.first && currentPosition.second < other!!.currentPosition.second ) -1 else if (currentPosition == other.currentPosition) 0 else 1
     }

 }


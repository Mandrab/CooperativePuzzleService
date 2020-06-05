package src.puzzle

import client.Client

class SelectionManager {
    private var selectionActive = false
    private var selectedTile: Tile? = null
    private lateinit var client:Client

    fun selectTile(tile: Tile, listener: SelectionManager.Listener, client: Client) {
        if (selectionActive) {
            selectionActive = false
            swap(selectedTile, tile)
            listener.onSwapPerformed()
        } else {
            selectionActive = true
            selectedTile = tile
        }
    }

    private fun swap(t1: Tile?, t2: Tile) {
        client.updateTilePosition(t1!!, t2.currentPosition.first, t2.currentPosition.second)
        val pos = t1!!.currentPosition
        t1!!.currentPosition = t2.currentPosition

        t2.currentPosition = pos
    }

    @FunctionalInterface
    interface Listener {
        fun onSwapPerformed()
    }
}

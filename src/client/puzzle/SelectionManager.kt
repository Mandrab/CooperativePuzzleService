package client.puzzle

import client.Client


class SelectionManager {
    private var selectedTile: Tile? = null

    fun selectTile(tile: Tile, listener: Listener, client: Client) {
        selectedTile = selectedTile?.let {
            client.updateTilePosition(tile, it.currentPosition.first, it.currentPosition.second)
            listener.onSwapPerformed()
            null
        } ?: tile
    }

    @FunctionalInterface
    interface Listener {
        fun onSwapPerformed()
    }
}

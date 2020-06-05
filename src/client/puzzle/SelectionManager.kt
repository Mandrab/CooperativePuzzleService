package client.puzzle

import client.Client


class SelectionManager {
    private var selectedTile: Tile? = null

    fun selectTile(tile: Tile, client: Client) {
        selectedTile = selectedTile?.let {
            client.updateTilePosition(tile, it.currentPosition.first, it.currentPosition.second)
            null
        } ?: tile
    }
}

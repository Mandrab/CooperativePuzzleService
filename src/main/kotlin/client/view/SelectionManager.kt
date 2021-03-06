package main.kotlin.client.view

import main.kotlin.client.Client

/**
 * This class manage swap of two tiles.
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
class SelectionManager {
    private var selectedTile: Tile? = null

    fun selectTile(tile: Tile, client: Client) {
        selectedTile?.let {
            client.updateTilePosition(tile, it.currentPosition.first, it.currentPosition.second)
            selectedTile = null
        } ?: also { selectedTile = tile }
    }
}

package client.view

import client.Client

/**
 * This class is a SelectionManager and it check a tiles position to swap.
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

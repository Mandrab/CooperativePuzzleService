package client

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import client.puzzle.PuzzleBoard
import client.puzzle.Tile
import io.vertx.core.Future
import io.vertx.core.Promise
import java.awt.Image
import javax.swing.ImageIcon


class Client(private val name: String, private val port: Int) : AbstractVerticle() {
    private val webClient: WebClient by lazy { WebClient.create(vertx) }
    private val httpClient: HttpClient by lazy { vertx.createHttpClient() }

    private lateinit var puzzleBoard: PuzzleBoard

    private lateinit var puzzleID: String
    private lateinit var playerToken: String

    override fun start() {
        webClient.post(port, SERVICE_HOST, "/puzzle").send().onSuccess { response ->
            val body = response.bodyAsJsonObject()
            puzzleID = body.getString("puzzleID")

            puzzleBoard = PuzzleBoard(
                body.getInteger("rows"),
                body.getInteger("columns"),
                this
            )

            val downloadedTiles = mutableMapOf<String,Image>()
            infoPuzzle().onSuccess { puzzle ->
                puzzle.tiles?.forEach { tile ->
                    httpClient.get(DATA_PORT, SERVICE_HOST, "/${tile.imageURL}").onSuccess { response ->
                        response.body().onSuccess { buffer ->
                            downloadedTiles[tile.tileID] = ImageIcon(buffer.bytes).image

                            if (downloadedTiles.size == puzzle.tiles.size) {
                                puzzleBoard.tiles = puzzle.tiles.map { tileInfo -> Tile(
                                    downloadedTiles[tileInfo.tileID]!!,
                                    tileInfo.tileID,
                                    tileInfo.position
                                ) }
                            }
                        }
                    }
                }
            }.onFailure { println("Something went wrong ${it.message}") }
        }.onFailure { println("Something went wrong ${it.message}") }
    }

    fun joinGame() {
        webClient.post(port, SERVICE_HOST, "/puzzle/$puzzleID/user").send().onSuccess {
            playerToken = it.bodyAsJsonObject().getString("playerToken")
        }.onFailure { println("Something went wrong ${it.message}") }

        schedulePeriodicUpdate()
    }

    fun updateTilePosition(tile : Tile, newPosX: Int, newPosY: Int) {
        val params = JsonObject().put("puzzleID", puzzleID).put("tileID", tile.tileID).put("palyerToken", playerToken).put("x", newPosX).put("y", newPosY)

        webClient.put(port, SERVICE_HOST, "/puzzle/$puzzleID/${tile.tileID}").sendJson(params).onSuccess {
            if (it.statusCode() != 200) println("Something went wrong. Status code: ${it.statusCode()}")
        }.onFailure { println("Something went wrong ${it.message}") }
    }

    private fun infoPuzzle(): Future<PuzzleInfo> {
        val result = Promise.promise<PuzzleInfo>()

        webClient.get(port, SERVICE_HOST, "/puzzle/$puzzleID").send().onSuccess { response ->
            val body = response.bodyAsJsonObject()

            result.complete(
                PuzzleInfo(
                    puzzleID,
                    body.getString("imageURL"),
                    Pair(body.getInteger("column"), body.getInteger("row")),
                    body.getString("status"),
                    body.getJsonArray("tiles").map { it as JsonObject }.map {
                        TileInfo(
                            it.getString("tileID"),
                            it.getString("imageURL"),
                            Pair(it.getInteger("column"), it.getInteger("row"))
                        )
                    }
                )
            )
        }.onFailure { println("Something went wrong ${it.message}") }
        return result.future()
    }

    private fun schedulePeriodicUpdate() {
        vertx.setPeriodic(5000) {
            infoPuzzle().onSuccess { puzzleInfo ->
                puzzleInfo.tiles?.let { tiles ->
                    puzzleBoard.updateTiles(tiles.map { Pair(it.tileID, it.position) }.toMap()) }
            }.onFailure { println("Something went wrong ${it.message}") }
        }
    }

    private data class PuzzleInfo(
        val puzzleID: String,
        val imageURL: String,
        val position: Pair<Int, Int>,
        val status: String,
        val tiles: List<TileInfo>?
    )

    private data class TileInfo(
        val tileID: String,
        val imageURL: String,
        val position: Pair<Int, Int>
    )

    companion object {
        const val SERVICE_HOST = "localhost"
        const val DATA_PORT = 9000
    }
}
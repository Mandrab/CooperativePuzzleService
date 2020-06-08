package client

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpClient
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import client.view.PuzzleBoard
import client.view.Tile
import io.vertx.core.Future
import io.vertx.core.Promise
import java.awt.Image
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.swing.ImageIcon

/*
This is a client class.
The client uses the rest api to request information from the service.

@param name, name of client
@param port, port to make request.

@author Baldini Paolo, Battistini Ylenia
 */
class Client(private val name: String, private val port: Int) : AbstractVerticle() {
    private val webClient: WebClient by lazy { WebClient.create(vertx) }
    private val httpClient: HttpClient by lazy { vertx.createHttpClient() }

    private lateinit var puzzleBoard: PuzzleBoard

    private lateinit var puzzleID: String
    private lateinit var playerToken: String

    private var lastMouseUpdate = System.currentTimeMillis()

    private var puzzleMousesTimestamp: Timestamp = Timestamp(Date().time)
    private var puzzleIDTimestamp: Timestamp = Timestamp(Date().time)
    private var puzzleMousesWSTimestamp: Timestamp = Timestamp(Date().time)

    /*
    When the client starts its execution in order to play it must first request a puzzle from the service.
    It use a get api (/puzzle) to check If there is an available puzzle then return it otherwise use a post api to create a new one.
    In both cases he gets the information from server and creates the puzzle board also creates the tiles.
     */
    override fun start() {
        webClient.get(port, SERVICE_HOST, "/puzzle").send().onSuccess { puzzles ->

                val puzzleIdentifier = Promise.promise<String>()
                if (puzzles.bodyAsJsonObject().getJsonArray("IDs").isEmpty) {
                    webClient.post(port, SERVICE_HOST, "/puzzle").send().onSuccess { puzzle ->
                        puzzleIdentifier.complete(puzzle.bodyAsJsonObject().getString("puzzleID"))
                    }
                } else puzzleIdentifier.complete(puzzles.bodyAsJsonObject().getJsonArray("IDs").getString(0))

                puzzleIdentifier.future().onSuccess { puzzleID ->
                    this.puzzleID = puzzleID

                    infoPuzzle().onSuccess { response ->
                        puzzleBoard = PuzzleBoard(response.position.second, response.position.first, this)

                        val downloadedTiles = mutableMapOf<String, Image>()
                        infoPuzzle().onSuccess { puzzle ->
                            if (puzzle.status == "completed") puzzleBoard.complete()
                            else puzzle.tiles?.forEach { tile ->
                                httpClient.get(DATA_PORT, SERVICE_HOST, "/${tile.imageURL}").onSuccess { response ->
                                    response.body().onSuccess { buffer ->
                                        downloadedTiles[tile.tileID] = ImageIcon(buffer.bytes).image

                                        if (downloadedTiles.size == puzzle.tiles.size) {
                                            puzzleBoard.tiles = puzzle.tiles.map { tileInfo ->
                                                Tile(
                                                    downloadedTiles[tileInfo.tileID]!!,
                                                    tileInfo.tileID,
                                                    tileInfo.position
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }.onFailure { println("Something went wrong ${it.message}") }
                    }
                }
        }.onFailure { println("Something went wrong ${it.message}") }
    }

    /*
    To add a new player to the puzzle the client must make a post request using the API  /puzzle/:{puzzleID}/user.
    After he call a schedulePeriodicUpdate method to begin the periodic updating of information.
     */
    fun joinGame() {
        webClient.post(port, SERVICE_HOST, "/puzzle/$puzzleID/user").send().onSuccess {
            playerToken = it.bodyAsJsonObject().getString("playerToken")
        }.onFailure { println("Something went wrong ${it.message}") }

        schedulePeriodicUpdate()
    }

    /*
    This method through a put request  the server to update the tile positions.
    Use the API /puzzle/:{puzzleID}/:{tileID} to be able to update specifies the puzzleID, the tileID, playerToken and the new positions.

    @param tile, represent one Tile of puzzle
    @param newColumn, new position for column
    @param newRow, new position for row.
     */
    fun updateTilePosition(tile : Tile, newColumn: Int, newRow: Int) {
        val params = JsonObject().put("puzzleID", puzzleID).put("tileID", tile.tileID).put("playerToken", playerToken)
            .put("newColumn", newColumn).put("newRow", newRow)

        webClient.put(port, SERVICE_HOST, "/puzzle/$puzzleID/${tile.tileID}").sendJson(params).onSuccess {
            if (it.statusCode() != 200) println("Something went wrong. Status code: ${it.statusCode()}")
        }.onFailure { println("Something went wrong ${it.message}") }
    }

    /*
    This method through a put request the server to update the mouse positions.
    Use the API /puzzle/:{puzzleID}/:mouses to be able to update specifies playerToken and the new positions x and y.
    This method use also a timeStamp to specify the time of sending the request
    It also shows how updating can be done with webSocket.

    @param x, x position
    @param y, y position.
     */
    fun mouseMovement(x: Int, y: Int) {
        if (this::playerToken.isInitialized && lastMouseUpdate + MOUSE_UPDATE_TIME < System.currentTimeMillis()) {
            webClient.put(port, SERVICE_HOST, "/puzzle/$puzzleID/mouses").sendJsonObject(
                    JsonObject().put("playerToken", playerToken).put("position", JsonObject().put("x", x).put("y", y)).put("timeStamp", timeStamp())
            )

            httpClient.webSocket(DATA_PORT, SERVICE_HOST, "/puzzle/$puzzleID/mouses") {
                JsonObject().put("timeStamp", timeStamp()).put("playerToken", playerToken).put("position", JsonObject().put("x", x).put("y", y))
            }
            lastMouseUpdate = System.currentTimeMillis()
        }
    }

    /*
    This method through a get request using the API /puzzle/:{puzzleID} allows to obtain from the service all the information of the puzzle.
    This method uses the timestamp to verify that the information I get is up to date with what I already had.
    */
    private fun infoPuzzle(): Future<PuzzleInfo> {
        val result = Promise.promise<PuzzleInfo>()

        webClient.get(port, SERVICE_HOST, "/puzzle/$puzzleID").send().onSuccess { response ->
            val body = response.bodyAsJsonObject().getJsonObject("info")
            if (puzzleIDTimestamp.before(Timestamp.valueOf(response.bodyAsJsonObject().getString("timeStamp")))) {
                puzzleIDTimestamp = Timestamp.valueOf(response.bodyAsJsonObject().getString("timeStamp"))
                result.complete(
                    PuzzleInfo(
                        puzzleID,
                        body.getString("imageURL"),
                        Pair(body.getInteger("columns"), body.getInteger("rows")),
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
            }
        }.onFailure { println("Something went wrong ${it.message}") }
        return result.future()
    }

    /*
    This method periodically schedules the request to server to obtain information
    in order to have the updated information.
    The information that is required is information about the puzzle and mouse positions.
    This method uses the timestamp to verify that the mouse's information I get is up to date with what I already had.
     */
    private fun schedulePeriodicUpdate() {
        vertx.setPeriodic(PUZZLE_UPDATE_TIME) { taskID ->
            infoPuzzle().onSuccess { puzzleInfo ->
                println(puzzleInfo.status)
                if (puzzleInfo.status == "completed") {
                    puzzleInfo.tiles?.let { tiles ->
                        puzzleBoard.updateTiles(tiles.map { Pair(it.tileID, it.position) }.toMap())
                    }
                    puzzleBoard.complete()
                    vertx.cancelTimer(taskID)
                } else puzzleInfo.tiles?.let { tiles ->
                    puzzleBoard.updateTiles(tiles.map { Pair(it.tileID, it.position) }.toMap()) }
            }.onFailure { println("Something went wrong ${it.message}") }
        }
        vertx.setPeriodic(MOUSE_UPDATE_TIME) {
            webClient.get(port, SERVICE_HOST, "/puzzle/$puzzleID/mouses").send().onSuccess {response ->
                val bodyParam = response.bodyAsJsonObject()
                if (puzzleMousesTimestamp.before(Timestamp.valueOf(bodyParam.getString("timeStamp")))) {
                    puzzleMousesTimestamp = Timestamp.valueOf(bodyParam.getString("timeStamp"))
                    puzzleBoard.updateMouses(response.bodyAsJsonObject().getJsonArray("positions").map { body ->
                        body as JsonObject
                        Pair(
                            body.getString("playerID"),
                            body.getJsonObject("position").let { p -> Pair(p.getInteger("x"), p.getInteger("y")) }
                        )
                    }.toMap())
                }

                httpClient.get(DATA_PORT, SERVICE_HOST, "/puzzle/$puzzleID/mouses").onSuccess { response ->
                    response.body().onSuccess { buffer ->
                        if (puzzleMousesWSTimestamp.before(Timestamp.valueOf(bodyParam.getString("timeStamp")))) {
                            puzzleMousesWSTimestamp = Timestamp.valueOf(bodyParam.getString("timeStamp"))
                            puzzleBoard.updateMouses(bodyParam.getJsonArray("positions").map { body ->
                                body as JsonObject
                                Pair(
                                        body.getString("playerID"),
                                        body.getJsonObject("position").let { p -> Pair(p.getInteger("x"), p.getInteger("y")) }
                                )
                            }.toMap())
                        }
                    }
                }

            }
        }
    }

    private fun timeStamp() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))

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
        const val MOUSE_UPDATE_TIME = 300L
        const val PUZZLE_UPDATE_TIME = 1500L
    }
}
package client

import client.view.View
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpClient
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import client.puzzle.PuzzleBoard
import client.puzzle.Tile
import java.awt.Point


class Client(private val name: String, private val port: Int) : AbstractVerticle() {
    private val view = View(this)

    private val webClient: WebClient by lazy { WebClient.create(vertx) }
    private val httpClient: HttpClient by lazy { vertx.createHttpClient() }
    private lateinit var webSocket: WebSocket
    private lateinit var puzzle: PuzzleBoard
    private var puzzleID = String()
    private var playerToken = String()

    override fun start() = joinPuzzle()

    private fun joinPuzzle() {
        webClient.post(port, "localhost", "/puzzle").send {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                println("Response: $body")
                puzzleID = body.getString("puzzleID")
                infoPuzzle(puzzleID)
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }

    private fun infoPuzzle(puzzleID : String) {
        val params = JsonObject().put("puzzleID", puzzleID)

        webClient.get(port, "localhost", "/puzzle/$puzzleID").sendJson(params) {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                println("Response: $body")
                puzzle = PuzzleBoard(
                    body.getInteger("rows"),
                    body.getInteger("columns"),
                    this,
                    body.getJsonArray("tiles").map { it as JsonObject }
                )
                puzzle.isVisible = true

                openWS(puzzleID)
            } else println("Something went wrong ${it.cause().message}")
        }
    }

    private fun openWS(puzzleID: String) {
        httpClient.webSocket(port, "localhost", "/puzzle/$puzzleID/user") {
            it.result()?.also { ws ->
                webSocket = ws

                ws.textMessageHandler {
                    println(this.toString() + it)
                    val player = JsonObject(it).getString("player")
                    JsonObject(it).getJsonObject("position").apply {
                        view.drawPointer(player, getInteger("x"), getInteger("y"))
                    }
                }.binaryMessageHandler {
                    it.toJsonObject().getString("player")
                    val player = it.toJsonObject().getString("player")
                    it.toJsonObject().getJsonObject("position").apply {
                        view.drawPointer(player, getInteger("x"), getInteger("y"))
                    }
                }.exceptionHandler {
                    TODO()
                }.closeHandler {
                    TODO()
                }
            }
        }
    }

    fun newMovement(point: Point) {
        val msg = JsonObject().put("player", name).put("position", JsonObject().put("x", point.x).put("y", point.y))
        //webSocket.writeBinaryMessage(msg.toBuffer())
    }

    fun playGame() {
        val params = JsonObject().put("puzzleID", puzzleID)

        webClient.post(port, "localhost", "/puzzle/$puzzleID/user").sendJson(params) {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                playerToken = body.getString("playerToken")
                getPuzzleTiles()
                println("Response: $body")
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }

    private fun getPuzzleTiles() {
        val params = JsonObject().put("puzzleID", puzzleID)

        vertx.setPeriodic(5000) {
            webClient.get(port, "localhost", "/puzzle/$puzzleID/tiles").sendJson(params) {
                if (it.succeeded()) {
                    val response = it.result()
                    val code = response.statusCode()
                    println("Status code: $code ${response.body()}")
                    puzzle.updateTiles(response.bodyAsJsonArray().map { it as JsonObject })
                } else println("Something went wrong ${it.cause().message}")
            }
        }
    }


    fun updateTilePosition(tile : Tile, newPosX: Int, newPosY: Int){
        val params = JsonObject().put("puzzleID", puzzleID).put("tileID", tile.tileID).put("palyerToken", playerToken).put("x", newPosX).put("y", newPosY)

        webClient.put(port, "localhost", "/puzzle/$puzzleID/${tile.tileID}").sendJson(params) {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code ${response.body()}")
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }
}
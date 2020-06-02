package client

import client.view.View
import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpClient
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import java.awt.Point


class Client(private val port: Int) : AbstractVerticle() {
    private val webClient: WebClient by lazy { WebClient.create(vertx) }
    private val httpClient: HttpClient by lazy { vertx.createHttpClient() }
    private lateinit var webSocket: WebSocket

    init {
        View(this)
    }

    override fun start() = joinPuzzle()

    private fun joinPuzzle() {
        webClient.get(port, "localhost", "/puzzle").send {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                println("Response: $body")
                openWS(body.getString("puzzleID"))
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }

    private fun openWS(puzzleID: String) {
        httpClient.webSocket(port, "localhost", "/puzzle/$puzzleID/user") {
            it.result()?.also { ws ->
                webSocket = ws
                ws.textMessageHandler {
                    println(it)
                }.exceptionHandler {
                    TODO()
                }.closeHandler {
                    TODO()
                }
            }
        }
    }

    fun newMovement(point: Point) {
        val msg = JsonObject().put("player", "marco").put("position", JsonObject().put("x", point.x).put("y", point.y))
        webSocket.writeBinaryMessage(msg.toBuffer())
    }
}
package client

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient


class Client(private val port: Int) : AbstractVerticle() {

    override fun start() {
        val client: HttpClient = vertx.createHttpClient()

        client.webSocket(port, "localhost", "/") {
            it.result()?.writeFinalTextFrame("pong")

            it.result().textMessageHandler {
                println(it)
            }.exceptionHandler {
                TODO()
            }.closeHandler {
                TODO()
            }
        }
    }
}
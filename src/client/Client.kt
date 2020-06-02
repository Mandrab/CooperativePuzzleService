package client

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpClient


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
package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServer


class Gateway(private val port: Int) : AbstractVerticle() {

    override fun start() {
        val server: HttpServer = vertx.createHttpServer()

        server.webSocketHandler {
            it.writeTextMessage("ping");

            it.textMessageHandler {
                println("Web-socket connected!")
            }.exceptionHandler {
                TODO()
            }.closeHandler {
                TODO()
            }
        }.listen(port, "localhost")
    }
}


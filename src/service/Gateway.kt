package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import service.db.DBConnector


class Gateway(val ready: Promise<Void> = Promise.promise()) : AbstractVerticle() {

    override fun start() {
        val router: Router = Router.router(vertx)

        router.apply {
            route().handler(BodyHandler.create())

            post("/puzzle").handler { RESTful.newPuzzle(it) }
            post("/puzzle/:puzzleID/user").handler { RESTful.newPlayer(it) }

            get("/puzzle").handler { RESTful.availablePuzzles(it) }
            get("/puzzle/:puzzleID").handler { RESTful.puzzleInfo(it) }
            get("/puzzle/:puzzleID/mouses").handler { RESTful.getPositions(it) }
            get("/puzzle/:puzzleID/tiles").handler { RESTful.getTiles(it) }
            get("/puzzle/:puzzleID/:tileID").handler { RESTful.getTile(it) }

            put("/puzzle/:puzzleID/mouses").handler { RESTful.positionSubmission(it) }
            put("/puzzle/:puzzleID/:tileID").handler{ RESTful.updateTilePosition(it) }
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .webSocketHandler { WebSocket.webSocketHandler(it, vertx) }
            .listen(PORT)

        ready.complete()
    }

    companion object {
        const val PORT = 40404
    }
}


package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler


class Gateway(private val ready: Promise<Void> = Promise.promise()) : AbstractVerticle() {

    override fun start() {
        val router: Router = Router.router(vertx)

        router.apply {
            route().handler(BodyHandler.create())

            post("/client/puzzle").handler { RESTful.newPuzzle(it) }
            post("/client/puzzle/:puzzleID/user").handler { RESTful.newPlayer(it) }

            get("/client/puzzle").handler { RESTful.availablePuzzles(it) }
            get("/client/puzzle/:puzzleID").handler { RESTful.puzzleInfo(it) }
            get("/client/puzzle/:puzzleID/mouses").handler { RESTful.getPositions(it) }
            get("/client/puzzle/:puzzleID/tiles").handler { RESTful.getTiles(it) }
            get("/client/puzzle/:puzzleID/:tileID").handler { RESTful.getTile(it) }

            put("/client/puzzle/:puzzleID/mouses").handler { RESTful.positionSubmission(it) }
            put("/client/puzzle/:puzzleID/:tileID").handler{ RESTful.updateTilePosition(it) }
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


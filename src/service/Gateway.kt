package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import kotlin.random.Random


class Gateway(private val localPort: Int) : AbstractVerticle() {
    override fun start() {
        setup()
        println("[SERVICE] Ready")
    }

    private fun setup() {
        val router: Router = Router.router(vertx)
        router.apply {
            route().handler(BodyHandler.create())
            get("/puzzle").handler { availablePuzzle(it) }
            get("/puzzle/:puzzleID/tile").handler { getTiles(it) }
            post("/puzzle/:puzzleID/user").handler { newPlayer(it) }
        }
        vertx.createHttpServer()
            .requestHandler(router)
            .webSocketHandler { webSocketHandler(it) }
            .listen(localPort)
    }

    private fun availablePuzzle(ctx: RoutingContext) {
        val puzzleID = DBConnector.getPuzzlesID().firstOrNull() ?: ("Puzzle" + Random.nextInt()).also {
            DBConnector.addPuzzle(it, imageURL, width, height)
        }
        val response: HttpServerResponse = ctx.response()
        response.putHeader("content-type", "application/json").end(JsonObject().put("puzzleID", puzzleID)
            .encodePrettily())
        response.statusCode = 200
        response.end()
    }

    private fun newPlayer(ctx: RoutingContext) {
        val response = ctx.response()
        try {
            val puzzleID = ctx.request().getParam("puzzleID")
            val playerID = ctx.bodyAsJson.getValue("playerID") as String

            DBConnector.addPlayer(puzzleID, playerID)

            response.statusCode = 201
        } catch(e: Exception) {
            response.statusCode = 409
        }
        response.end()
    }

    private fun getTiles(ctx: RoutingContext) {
        val response = ctx.response()
        try {
            val puzzleID = ctx.request().getParam("puzzleID")
            val tiles = DBConnector.getPuzzleTiles(puzzleID)
            response.statusCode = 200

            val returns = JsonArray()
            tiles.forEach { returns.add(JsonObject().apply {
                put("ID", it.substringBefore(";"))
                it.substringAfterLast(";").let {
                    put("currentX", it.substringBefore(" "))
                    put("currentY", it.substringAfter(" "))
                }
            }) }
            response.putHeader("content-type", "application/json").end(returns.encodePrettily())
        } catch (e: Exception) {
            response.statusCode = 404
        }
        response.end()
    }

    private fun webSocketHandler(ws: ServerWebSocket) {
        if (ws.path().matches("/puzzle\\/([A-Z]|[a-z]|[0-9])*\\/user".toRegex())) {
            ws.textMessageHandler {
                println(it)
                println(JsonObject(it).getString("player"))
            }.binaryMessageHandler {
                println(it)
                println(it.toJsonObject().getString("player"))
            }
        } else {
            println("Socket connection attempt rejected")
            ws.reject()
        }
    }

    companion object {
        private const val imageURL = "res/bletchley-park-mansion.jpg"
        private const val width = 5
        private const val height = 3
    }
}


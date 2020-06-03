package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import service.db.DBConnector
import kotlin.random.Random


class Gateway : AbstractVerticle() {

    override fun start() {
        val router: Router = Router.router(vertx)

        router.apply {
            route().handler(BodyHandler.create())

            post("/puzzle").handler { newPuzzle(it) }
            get("/puzzle").handler { availablePuzzles(it) }

            get("/puzzle/:puzzleID").handler { puzzleInfo(it) }

            get("/puzzle/:puzzleID/tile").handler { getTiles(it) }
            put("/puzzle/:puzzleID/:tileID").handler{ updateTilePosition(it) }
            post("/puzzle/:puzzleID/user").handler { newPlayer(it) }
        }

        vertx.createHttpServer()
            .requestHandler(router)
            .webSocketHandler { webSocketHandler(it) }
            .listen(PORT)
    }

    private fun newPuzzle(ctx: RoutingContext) {
        var puzzleID: String
        do {
            puzzleID = "Puzzle${Random.nextInt(0, Int.MAX_VALUE)}"
            val accomplished = DBConnector.addPuzzle(puzzleID, IMAGE_URL, GRID_COL_COUNT, GRID_ROW_COUNT)
        } while(!accomplished)

        val jResponse = JsonObject().put("puzzleID", puzzleID).put("imageURL", IMAGE_URL).put("columns", GRID_COL_COUNT)
            .put("rows", GRID_ROW_COUNT).encode()

        val response = ctx.response()
        response.statusCode = 200
        response.putHeader("content-type", "application/json").end(jResponse)
    }

    private fun availablePuzzles(ctx: RoutingContext) {
        val jArray = JsonArray()
        DBConnector.getPuzzlesID().forEach { jArray.add(it) }

        val response: HttpServerResponse = ctx.response()
        response.statusCode = 200
        response.putHeader("content-type", "application/json").end(jArray.encode())
    }

    private fun puzzleInfo(ctx: RoutingContext) {
        if (!ctx.request().params().contains("puzzleID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesID().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val puzzleInfo = DBConnector.getPuzzleInfo(puzzleID)!!
        val jObject = JsonObject().apply {
            put("imageURL", puzzleInfo.imageURL)
            put("columns", puzzleInfo.columnsCount)
            put("rows", puzzleInfo.rowsCount)

            val jArray = JsonArray()
            DBConnector.getPuzzleTiles(puzzleID).map {
                JsonObject().apply {
                    put("tileID", it.tileID)
                    put("column", it.currentPosition.first)
                    put("row", it.currentPosition.second)
                }
            }.forEach { jArray.add(it) }
            put("tiles", jArray)
        }
        val response = ctx.response()
        response.putHeader("content-type", "application/json").end(jObject.encode())
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

            val returns = JsonArray()
            tiles.forEach { returns.add(JsonObject().apply {
                put("ID", it.tileID)
                it.currentPosition.let {
                    put("currentX", it.first)
                    put("currentY", it.second)
                }
            }) }
            response.statusCode = 200
            response.putHeader("content-type", "application/json").end(returns.encodePrettily())
        } catch (e: Exception) {
            response.statusCode = 404
        }
        response.end()
    }

    private fun updateTilePosition(ctx: RoutingContext){
        val response = ctx.response()
        try {
            val puzzleID = ctx.request().getParam("puzzleID")
            val tiles = ctx.request().getParam("tileID")
            val newPosX = ctx.request().getParam("x").toInt()
            val newPosY = ctx.request().getParam("y").toInt()
            val update = DBConnector.updateTilePosition(puzzleID, tiles, newPosX, newPosY)
            response.statusCode = 200
        } catch (e: Exception) {
            response.statusCode = 404
        }
        response.end()
    }
// TODO rest ws join... atm do in recv
    private fun webSocketHandler(ws: ServerWebSocket) {
        if (ws.path().matches("/puzzle\\/([A-Z]|[a-z]|[0-9])*\\/user".toRegex())) {
            val puzzleID = ws.path().removeSurrounding("/puzzle/", "/user")

            ws.textMessageHandler {
                DBConnector.playerWS(
                    puzzleID,
                    JsonObject(it).getString("player"),
                    ws.textHandlerID()
                )
                DBConnector.playersWS(puzzleID).forEach { id -> vertx.eventBus().send(id, it) }
            }.binaryMessageHandler {
                DBConnector.playerWS(
                    puzzleID,
                    JsonObject(it).getString("player"),
                    null,
                    ws.binaryHandlerID()
                )
                DBConnector.playersWS(puzzleID).forEach { id -> println(id);vertx.eventBus().send(id, it) }
            }.exceptionHandler {
                TODO()
            }.closeHandler {
                TODO()
            }
        } else {
            println("Socket connection attempt rejected")
            ws.reject()
        }
    }

    companion object {
        private const val IMAGE_URL = "res/bletchley-park-mansion.jpg"
        private const val GRID_COL_COUNT = 5
        private const val GRID_ROW_COUNT = 3

        const val PORT = 40404
    }
}


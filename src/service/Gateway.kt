package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.http.HttpServerRequest
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
            post("/puzzle/:puzzleID/user").handler { newPlayer(it) }

            get("/puzzle").handler { availablePuzzles(it) }
            get("/puzzle/:puzzleID").handler { puzzleInfo(it) }
            get("/puzzle/:puzzleID/mouses").handler { getPositions(it) }
            get("/puzzle/:puzzleID/tiles").handler { getTiles(it) }
            get("/puzzle/:puzzleID/:tileID").handler { getTile(it) }

            put("/puzzle/:puzzleID/mouses").handler { positionSubmission(it) }
            put("/puzzle/:puzzleID/:tileID").handler{ updateTilePosition(it) }
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
        response.statusCode = 201
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

    private fun getTiles(ctx: RoutingContext) {
        if (!ctx.request().params().contains("puzzleID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesID().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val jArray = JsonArray()
        DBConnector.getPuzzleTiles(puzzleID).map {
            JsonObject().apply {
                put("tileID", it.tileID)
                put("column", it.currentPosition.first)
                put("row", it.currentPosition.second)
            }
        }.forEach { jArray.add(it) }

        val response = ctx.response()
        response.statusCode = 200
        response.putHeader("content-type", "application/json").end(jArray.encode())
    }

    private fun getTile(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), null, "puzzleID", "tileID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        val tileID = ctx.request().getParam("tileID")

        val tile = DBConnector.getPuzzleTiles(puzzleID).firstOrNull { it.tileID == tileID }
        tile ?: let { ctx.response().apply { statusCode = 404 }.end(); return }

        val response = ctx.response()
        response.statusCode = 200
        response.putHeader("content-type", "application/json").end(
            JsonObject().apply {
                put("tileID", tile.tileID)
                put("column", tile.currentPosition.first)
                put("row", tile.currentPosition.second)
            }.encode()
        )
    }

    private fun updateTilePosition(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), ctx.bodyAsJson, "puzzleID", "tileID", "playerToken","newColumn", "newRow")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        val tileID = ctx.request().getParam("tileID")

        if (!DBConnector.getPuzzlesID().contains(puzzleID) || DBConnector.getPuzzleTiles(puzzleID).all { it.tileID != tileID }) {
            ctx.response().apply { statusCode = 404 }.end()
            return
        }

        val playerID = ctx.bodyAsJson.getString("playerToken")
        if (DBConnector.getPuzzlePlayers(puzzleID).all { it.playerID != playerID }) {
            ctx.response().apply { statusCode = 401 }.end()
            return
        }

        val puzzleInfo = DBConnector.getPuzzleInfo(puzzleID)!!
        val newColumn = ctx.bodyAsJson.getInteger("newColumn")
        val newRow = ctx.bodyAsJson.getInteger("newRow")
        if (newColumn < 0 || newColumn > puzzleInfo.columnsCount || newRow < 0 || newRow > puzzleInfo.rowsCount) {
            ctx.response().apply { statusCode = 409 }.end()
            return
        }

        if (!DBConnector.updateTilePosition(puzzleID, tileID, newColumn, newRow)) {
            ctx.response().apply { statusCode = 500 }.end()
            return
        }
        ctx.response().apply { statusCode = 200 }.end()
    }

    private fun newPlayer(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), null, "puzzleID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesID().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val playerID = DBConnector.addPlayer(puzzleID)!!
        ctx.response().putHeader("content-type", "application/json").apply { statusCode = 201 }
                .end(JsonObject().put("playerToken", playerID).encode())
    }

    private fun positionSubmission(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), ctx.bodyAsJson, "puzzleID", "playerToken", "column", "row")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesID().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val playerToken = ctx.bodyAsJson.getString("playerToken")
        val playerInfo = DBConnector.getPuzzlePlayers(puzzleID).firstOrNull { it.playerID == playerToken }

        val column = ctx.bodyAsJson.getInteger("column")
        val row = ctx.bodyAsJson.getInteger("row")
        DBConnector.newPosition(puzzleID, playerToken, column, row)

        ctx.response().apply { statusCode = playerInfo?.lastPosition?.let { 200 } ?: 201 }.end()
    }

    private fun getPositions(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), null, "puzzleID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesID().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val jArray = JsonArray()
        DBConnector.getPuzzlePlayers(puzzleID).filter { it.lastPosition != null }.forEach { jArray.add(
            JsonObject().put("playerID", it.playerID).put("position",
                JsonObject().put("x", it.lastPosition!!.first).put("y", it.lastPosition.second)
            )) }

        ctx.response().apply { statusCode = 200 }.end(jArray.encode())
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

    private fun requestContains(request: HttpServerRequest, body: JsonObject?, vararg keys: String): Boolean =
            keys.all { request.params().contains(it) || body?.containsKey(it) ?: false }

    companion object {
        private const val IMAGE_URL = "res/bletchley-park-mansion.jpg"
        private const val GRID_COL_COUNT = 5
        private const val GRID_ROW_COUNT = 3

        const val PORT = 40404
    }
}


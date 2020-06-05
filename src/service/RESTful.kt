package service

import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import service.db.DBConnector
import kotlin.random.Random

object RESTful {
    private const val IMAGE_URL = "res/bletchley-park-mansion.jpg"
    private const val GRID_COL_COUNT = 5
    private const val GRID_ROW_COUNT = 3

    internal fun newPuzzle(ctx: RoutingContext) {
        var puzzleID: String
        do {
            puzzleID = "Puzzle${Random.nextInt(0, Int.MAX_VALUE)}"
            val accomplished = DBConnector.addPuzzle(puzzleID,
                IMAGE_URL,
                GRID_COL_COUNT,
                GRID_ROW_COUNT
            )
        } while(!accomplished)

        val jResponse = JsonObject().put("puzzleID", puzzleID).put("imageURL", IMAGE_URL).put("columns", GRID_COL_COUNT)
            .put("rows", GRID_ROW_COUNT).encode()

        val response = ctx.response()
        response.statusCode = 201
        response.putHeader("content-type", "application/json").end(jResponse)
    }

    internal fun availablePuzzles(ctx: RoutingContext) {
        val jArray = JsonArray()
        DBConnector.getPuzzlesIDs().forEach { jArray.add(it) }

        val response: HttpServerResponse = ctx.response()
        response.statusCode = 200
        response.putHeader("content-type", "application/json").end(jArray.encode())
    }

    internal fun puzzleInfo(ctx: RoutingContext) {
        if (!ctx.request().params().contains("puzzleID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesIDs().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val puzzleInfo = DBConnector.getPuzzleInfo(puzzleID)!!
        val jObject = JsonObject().apply {
            put("imageURL", puzzleInfo.imageURL)
            put("columns", puzzleInfo.columnsCount)
            put("rows", puzzleInfo.rowsCount)

            val jArray = JsonArray()
            DBConnector.getPuzzleTiles(puzzleID).map {
                JsonObject().apply {
                    put("tileID", it.tileID)
                    put("imageURL", it.tileImageURL)
                    put("column", it.currentPosition.first)
                    put("row", it.currentPosition.second)
                }
            }.forEach { jArray.add(it) }
            put("tiles", jArray)
        }
        val response = ctx.response()
        response.putHeader("content-type", "application/json").end(jObject.encode())
    }

    internal fun getTiles(ctx: RoutingContext) {
        if (!ctx.request().params().contains("puzzleID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesIDs().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val jArray = JsonArray()
        DBConnector.getPuzzleTiles(puzzleID).map {
            JsonObject().apply {
                put("tileID", it.tileID)
                put("imageURL", it.tileImageURL)
                put("column", it.currentPosition.first)
                put("row", it.currentPosition.second)
            }
        }.forEach { jArray.add(it) }

        val response = ctx.response()
        response.statusCode = 200
        response.putHeader("content-type", "application/json").end(jArray.encode())
    }

    internal fun getTile(ctx: RoutingContext) {
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

    internal fun updateTilePosition(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), ctx.bodyAsJson, "puzzleID", "tileID", "playerToken","newColumn", "newRow")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        val tileID = ctx.request().getParam("tileID")

        if (!DBConnector.getPuzzlesIDs().contains(puzzleID) || DBConnector.getPuzzleTiles(puzzleID).all { it.tileID != tileID }) {
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
        if (newColumn < 0 || newColumn > puzzleInfo.columnsCount -1 || newRow < 0 || newRow > puzzleInfo.rowsCount -1) {
            ctx.response().apply { statusCode = 409 }.end()
            return
        }

        if (!DBConnector.updateTilePosition(puzzleID, tileID, newColumn, newRow)) {
            ctx.response().apply { statusCode = 500 }.end()
            return
        }
        ctx.response().apply { statusCode = 200 }.end()
    }

    internal fun newPlayer(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), null, "puzzleID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesIDs().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val playerID = DBConnector.addPlayer(puzzleID)!!
        ctx.response().putHeader("content-type", "application/json").apply { statusCode = 201 }
            .end(JsonObject().put("playerToken", playerID).encode())
    }

    internal fun positionSubmission(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), ctx.bodyAsJson, "puzzleID", "playerToken", "column", "row")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (puzzleID !in DBConnector.getPuzzlesIDs()) { ctx.response().apply { statusCode = 404 }.end(); return }

        val playerToken = ctx.bodyAsJson.getString("playerToken")
        val playerInfo = DBConnector.getPuzzlePlayers(puzzleID).firstOrNull { it.playerID == playerToken }

        val column = ctx.bodyAsJson.getInteger("column")
        val row = ctx.bodyAsJson.getInteger("row")
        DBConnector.newPosition(puzzleID, playerToken, column, row)

        ctx.response().apply { statusCode = playerInfo?.lastPosition?.let { 200 } ?: 201 }.end()
    }

    internal fun getPositions(ctx: RoutingContext) {
        if (!requestContains(ctx.request(), null, "puzzleID")) return

        val puzzleID = ctx.request().getParam("puzzleID")
        if (!DBConnector.getPuzzlesIDs().contains(puzzleID)) { ctx.response().apply { statusCode = 404 }.end(); return }

        val jArray = JsonArray()
        DBConnector.getPuzzlePlayers(puzzleID).filter { it.lastPosition != null }.forEach { jArray.add(
            JsonObject().put("playerID", it.playerID).put("position",
                JsonObject().put("x", it.lastPosition!!.first).put("y", it.lastPosition.second)
            )) }

        ctx.response().apply { statusCode = 200 }.end(jArray.encode())
    }

    private fun requestContains(request: HttpServerRequest, body: JsonObject?, vararg keys: String): Boolean =
        keys.all { request.params().contains(it) || body?.containsKey(it) ?: false }
}
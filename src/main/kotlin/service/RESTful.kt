package main.kotlin.service

import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import main.kotlin.service.db.DBConnector
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * This class contains all the route handlers used by the gateway.
 *
 * @author Baldini Paolo, Battistini Ylenia
 */
object RESTful {
    private const val IMAGE_URL = "bletchley-park-mansion.jpg"
    private const val GRID_COL_COUNT = 5
    private const val GRID_ROW_COUNT = 3

    /**
     * This function create a new Puzzle with default image, columns and rows.
     * This is the handler of /puzzle (POST)
     */
    internal fun newPuzzle(ctx: RoutingContext) {
        var puzzleID: String
        do {
            puzzleID = "Puzzle${Random.nextInt(0, Int.MAX_VALUE)}"
            val accomplished = DBConnector.addPuzzle(puzzleID,
                "res${File.separator}$IMAGE_URL",
                GRID_COL_COUNT,
                GRID_ROW_COUNT
            )
        } while(!accomplished)

        ctx.response().apply {
            statusCode = 201
            putHeader("content-type", "application/json")
            end(
                JsonObject().put("puzzleID", puzzleID)
                    .put("imageURL", "res${File.separator}$IMAGE_URL")
                    .put("columns", GRID_COL_COUNT)
                    .put("rows", GRID_ROW_COUNT).encode()
            )
        }
    }

    /**
     * Send an array with existing puzzles IDs and status
     * This function is handler of /puzzle (GET).
     */
    internal fun availablePuzzles(ctx: RoutingContext) {
        val jArray = JsonArray()
        DBConnector.getPuzzlesIDs().mapNotNull { DBConnector.getPuzzleInfo(it) }.forEach {
            jArray.add(JsonObject().put("ID", it.puzzleID).put("status", it.status.name))
        }
        val response: HttpServerResponse = ctx.response()
        response.statusCode = 200
        response.putHeader("content-type", "application/json").end(
            JsonObject().apply {
                put("timeStamp", timeStamp())
                put("puzzles", jArray)
            }.encode()
        )
    }

    /**
     * This function get all puzzle information.
     * Get puzzleID, imageURL, column, row, status (STARTED or COMPLETED) and get tiles list.
     * This is the handler of /puzzle/:puzzleID (GET)
     */
    internal fun puzzleInfo(ctx: RoutingContext) {
        val puzzleID = ctx.request().getParam("puzzleID") ?: return
        val puzzleInfo = DBConnector.getPuzzleInfo(puzzleID) ?: let {
            ctx.response().apply { statusCode = 404 }.end()
            return
        }

        ctx.response().apply {
            statusCode = 200
            putHeader("content-type", "application/json")
        }.end(
            JsonObject().put("timeStamp", timeStamp()).put("info", JsonObject().apply {
                put("imageURL", puzzleInfo.imageURL)
                put("columns", puzzleInfo.columnsCount)
                put("rows", puzzleInfo.rowsCount)

                val tiles = DBConnector.getPuzzleTiles(puzzleID)
                put("status", if (tiles.all { it.currentPosition == it.originalPosition }) "completed" else "started")

                val jArray = JsonArray()
                tiles.map {
                    JsonObject().apply {
                        put("tileID", it.tileID)
                        put("imageURL", it.tileImageURL)
                        put("column", it.currentPosition.first)
                        put("row", it.currentPosition.second)
                    }
                }.forEach { jArray.add(it) }
                put("tiles", jArray)
            }).encode()
        )
    }

    /**
     * This function get all tiles of a puzzle
     * Get tileID, imageURL, column and row (actual position, not original one).
     * This is the handler of /puzzle/:puzzleID/tiles (GET)
     */
    internal fun getTiles(ctx: RoutingContext) {
        val puzzleID = ctx.request().getParam("puzzleID") ?: return
        if (puzzleID !in DBConnector.getPuzzlesIDs()) { ctx.response().apply { statusCode = 404 }.end(); return }

        val jArray = JsonArray()
        DBConnector.getPuzzleTiles(puzzleID).map {
            JsonObject().apply {
                put("tileID", it.tileID)
                put("imageURL", it.tileImageURL)
                put("column", it.currentPosition.first)
                put("row", it.currentPosition.second)
            }
        }.forEach { jArray.add(it) }

        ctx.response().apply {
            statusCode = 200
            putHeader("content-type", "application/json")
        }.end(JsonObject().put("timeStamp", timeStamp()).put("tiles", jArray).encode())
    }

    /**
     * This function get information of a specific tile.
     * Get timestamp, tileID, column and row (actual position, not original one).
     * This is the handler of /puzzle/:puzzleID/:tileID (GET)
     */
    internal fun getTile(ctx: RoutingContext) {
        val puzzleID = ctx.request().getParam("puzzleID") ?: return
        val tileID = ctx.request().getParam("tileID") ?: return

        if (puzzleID !in DBConnector.getPuzzlesIDs()) { ctx.response().apply { statusCode = 404 }.end(); return }
        val tile = DBConnector.getPuzzleTiles(puzzleID).firstOrNull { it.tileID == tileID }
        tile ?: let { ctx.response().apply { statusCode = 404 }.end(); return }

        val response = ctx.response()
        response.statusCode = 200
        response.putHeader("content-type", "application/json").end(
            JsonObject().apply {
                put("timeStamp", timeStamp())
                put("tileID", tile.tileID)
                put("column", tile.currentPosition.first)
                put("row", tile.currentPosition.second)
            }.encode()
        )
    }

    /**
     * This function update position of a specific tile.
     * The Json contains information about the new positions.
     * This is the handler of /puzzle/:puzzleID/:tileID (PUT)
     */
    internal fun updateTilePosition(ctx: RoutingContext) {
        val puzzleID = ctx.request().getParam("puzzleID") ?: return
        val tileID = ctx.request().getParam("tileID") ?: return

        if (!ctx.bodyAsJson.recursiveContainsKeys("playerToken", "newColumn", "newRow")) return

        if (puzzleID !in DBConnector.getPuzzlesIDs() || DBConnector.getPuzzleTiles(puzzleID).all { it.tileID != tileID }) {
            ctx.response().apply { statusCode = 404 }.end()
            return
        }

        val playerID = ctx.bodyAsJson.getString("playerToken") ?: return
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

    /**
     * This function add a new player in a specific puzzle.
     * This is the handler of /puzzle/:puzzleID/user (POST)
     */
    internal fun newPlayer(ctx: RoutingContext) {
        val puzzleID = ctx.request().getParam("puzzleID") ?: return
        if (puzzleID !in DBConnector.getPuzzlesIDs()) { ctx.response().apply { statusCode = 404 }.end(); return }

        val playerID = DBConnector.addPlayer(puzzleID)!!
        ctx.response().apply {
            statusCode = 201
            putHeader("content-type", "application/json")
        }.end(JsonObject().put("playerToken", playerID).encode())
    }

    /**
     * This function update mouse position.
     * The Json contains information about timestamp, playerToken and new mouse position.
     * This is the handler of /puzzle/:puzzleID/mouses (PUT)
     */
    internal fun positionSubmission(ctx: RoutingContext) {
        if (!ctx.bodyAsJson.recursiveContainsKeys("timeStamp", "playerToken", "position", "x", "y")) return

        val puzzleID = ctx.request().getParam("puzzleID") ?: return
        if (puzzleID !in DBConnector.getPuzzlesIDs()) { ctx.response().apply { statusCode = 404 }.end(); return }

        val playerToken = ctx.bodyAsJson.getString("playerToken")
        val playerInfo = DBConnector.getPuzzlePlayers(puzzleID).firstOrNull { it.playerID == playerToken } ?: let {
            ctx.response().apply { statusCode = 404 }.end(); return
        }
        val timestamp = ctx.bodyAsJson.getString("timeStamp")?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))
        }!!
        if (playerInfo.timeStamp != null && timestamp.isBefore(playerInfo.timeStamp)) {
            ctx.response().apply { statusCode = 409 }.end(); return
        }

        val position = ctx.bodyAsJson.getJsonObject("position")
        val x = position.getInteger("x")
        val y = position.getInteger("y")
        DBConnector.newPosition(puzzleID, playerToken, x, y, timestamp)

        ctx.response().apply { statusCode = playerInfo.lastPosition?.let { 200 } ?: 201 }.end()
    }

    /**
     * This function get mouse positions for a specific puzzle.
     * This is the handler of /puzzle/:puzzleID/mouses (GET)
     */
    internal fun getPositions(ctx: RoutingContext) {
        val puzzleID = ctx.request().getParam("puzzleID") ?: return
        if (puzzleID !in DBConnector.getPuzzlesIDs()) { ctx.response().apply { statusCode = 404 }.end(); return }

        val jArray = JsonArray()
        DBConnector.getPuzzlePlayers(puzzleID).filter { it.lastPosition != null }.forEach { jArray.add(
            JsonObject().put("playerID", it.playerID).put("position",
                JsonObject().put("x", it.lastPosition!!.first).put("y", it.lastPosition.second)
            )) }

        ctx.response().apply { statusCode = 200 }.end(
            JsonObject().put("timeStamp", timeStamp()).put("positions", jArray).encode()
        )
    }

    private fun timeStamp() = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS"))

    private fun JsonObject.recursiveContainsKeys(vararg keys: String): Boolean {
        return keys.all { key -> containsKey(key) || asSequence().any {
            when (it.value) {
                is JsonObject -> (it.value as JsonObject).recursiveContainsKeys(key)
                is JsonArray -> (it.value as JsonArray).recursiveContainsKeys(key)
                else -> false
            }
        } }
    }

    private fun JsonArray.recursiveContainsKeys(vararg keys: String): Boolean {
        return keys.all { key -> asSequence().any {
            when (it) {
                is JsonObject -> it.recursiveContainsKeys(key)
                is JsonArray -> it.recursiveContainsKeys(key)
                else -> false
            }
        } }
    }
}


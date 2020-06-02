package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import kotlin.random.Random


class Gateway(private val port: Int) : AbstractVerticle() {

    override fun start() {
        val server: HttpServer = vertx.createHttpServer()

        server.webSocketHandler {
            it.writeTextMessage("ping")

            it.textMessageHandler {
                println(it)
            }.exceptionHandler {
                TODO()
            }.closeHandler {
                TODO()
            }
        }.listen(port, "localhost")
    }
}

class PuzzleService(private val localPort: Int) : AbstractVerticle() {
    private var idEvents: Long = 0

    override fun start() {
        setup()
        println("[SERVICE] Ready")
    }

    private fun setup() {
        val router: Router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        router.get("/puzzle").handler { availablePuzzle(it) }
        router.post("/puzzle/:puzzleID/user").handler { newPlayer(it) }
        router.get("/puzzle/:puzzleID/tile").handler { getTiles(it) }
        getVertx().createHttpServer()
            .requestHandler(router) // .webSocketHandler(this::webSocketHandler)
            //.webSocketHandler { ws: ServerWebSocket -> webSocketHandler(ws) }
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


    private fun handleGetState(ctx: RoutingContext) {
        println("[SERVICE] New GetState from ${ctx.request().absoluteURI()}")
        val response: HttpServerResponse = ctx.response()
        val obj = JsonObject()
        obj.put("state", 10)
        response.putHeader("content-type", "application/json").end(obj.encodePrettily())
    }

    private fun handleUpdateResState(ctx: RoutingContext) {
        println("[SERVICE] NewUpdateState from ${ctx.request().absoluteURI()}")
        val response: HttpServerResponse = ctx.response()
        val resId: String = ctx.request().getParam("resID")
        println("[SERVICE] resId $resId")
        val newState: JsonObject = ctx.getBodyAsJson()
        println("[SERVICE] newState $newState")
        val obj = JsonObject()
        obj.put("newState", newState)
        obj.put("resId", resId)
        response.putHeader("content-type", "application/json").end(obj.encodePrettily())
    }

    private fun handleNewActionStart(ctx: RoutingContext) {
        val response: HttpServerResponse = ctx.response()
        // physicalAsset.doCmdStartHeating();
        val obj = JsonObject()
        val links = JsonObject()
        val newActionId = 13
        links.put("href", "/api/actions/start/$newActionId")
        obj.put("links", links)
        response.putHeader("content-type", "application/json").end(obj.encodePrettily())
        response.end()
    }

    private fun webSocketHandler(ws: ServerWebSocket) {
        if (ws.path() == "/api/events") {
            getVertx().setPeriodic(1000) { id: Long? ->
                val obj = JsonObject()
                obj.put("event", idEvents++)
                val buffer: Buffer = Buffer.buffer()
                obj.writeToBuffer(buffer)
                ws.write(buffer)
                println("[SERVICE] sent $obj")
            }
        } else {
            ws.reject()
        }
    }

    companion object {
        private const val imageURL = "res/bletchley-park-mansion.jpg"
        private const val width = 5
        private const val height = 3
    }
}


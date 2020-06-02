package service

import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.lang.Exception
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

class MyService(private val localPort: Int) : AbstractVerticle() {
    private var idEvents: Long = 0

    override fun start() {
        setup()
        println("[SERVICE] Ready")
    }

    private fun setup() {
        val router: Router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        //router.get("/api/state").handler { ctx: RoutingContext -> handleGetState(ctx) }
        //router.put("/api/resources/:resID").handler { ctx: RoutingContext -> handleUpdateResState(ctx) }
        router.get("/puzzle").handler { availablePuzzle(it) }
        router.post("/puzzle/:puzzleID/user").handler { newPlayer(it) }
        getVertx().createHttpServer()
            .requestHandler(router) // .webSocketHandler(this::webSocketHandler)
            //.webSocketHandler { ws: ServerWebSocket -> webSocketHandler(ws) }
            .listen(localPort)
    }

    private fun availablePuzzle(ctx: RoutingContext) {
        val puzzleID = DBConnector.getPuzzlesID().firstOrNull() ?: ("Puzzle" + Random.nextInt()).also {
            DBConnector.addPuzzle(it)
        }
        val response: HttpServerResponse = ctx.response()
        response.putHeader("content-type", "application/json").end(JsonObject().put("id", puzzleID).encodePrettily())
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
}


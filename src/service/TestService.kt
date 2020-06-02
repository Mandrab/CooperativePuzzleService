package service

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture

class TestService {
    private val vertx = Vertx.vertx()
    private val client = WebClient.create(vertx)

    @Test fun test() {
        testGetPuzzleToJoin().thenAccept {
            testLoginToPuzzle(it)
            testGetTilesInfo(it)
        }

        Thread.sleep(2000)
    }

    private fun testGetPuzzleToJoin(): CompletableFuture<String> {
        val returns = CompletableFuture<String>()

        client.get(port, "localhost", "/puzzle").send {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                println("Response: $body")
                returns.complete(body.getString("puzzleID"))
            } else {
                println("Something went wrong ${it.cause().message}")
                returns.completeExceptionally(null)
            }
        }
        return returns
    }

    private fun testLoginToPuzzle(puzzleID: String) {
        val params = JsonObject().put("playerID", puzzleID)

        client.post(port, "localhost", "/puzzle/$puzzleID/user").sendJson(params) {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code")
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
    }

    private fun testGetTilesInfo(puzzleID: String): CompletableFuture<List<Tile>> {
        val result = CompletableFuture<List<Tile>>()
        client.get(port, "localhost", "/puzzle/$puzzleID/tile").send {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code ${response.body()}")
                result.complete(response.body().toJsonArray().map { it as JsonObject }.map { Tile.parse(it) })
            } else {
                println("Something went wrong ${it.cause().message}")
                result.completeExceptionally(null)
            }
        }
        return result
    }

    private data class Tile(val ID: Int, val currentPosition: Pair<Int, Int>) {
        companion object {
            fun parse(jo: JsonObject) = Tile(jo.getString("ID").toInt(), Pair(jo.getString("currentX").toInt(),
                    jo.getString("currentY").toInt()))
        }
    }


        /*

        /* Doing a PUT with a JSON msg */client
            .put(port, "localhost", "/api/resources/res100") // .addQueryParam("param", "param_value")
            .sendJsonObject(
                newState
            ) { ar: AsyncResult<HttpResponse<Buffer?>> ->
                if (ar.succeeded()) {
                    val response = ar.result()
                    val body = response.bodyAsJsonObject()
                    println("Response: $body")
                } else {
                    println("Something went wrong " + ar.cause().message)
                }
            }

        /* Doing a PUT with a JSON msg */client
            .post(port, "localhost", "/api/actions/start")
            .sendJsonObject(
                newState
            ) { ar: AsyncResult<HttpResponse<Buffer?>> ->
                if (ar.succeeded()) {
                    val response = ar.result()
                    val body = response.bodyAsJsonObject()
                    println("Response: $body")
                } else {
                    println("Something went wrong " + ar.cause().message)
                }
            }

        /* WEB SOCKET */
        val options = HttpClientOptions()
            .setDefaultHost("localhost")
            .setDefaultPort(port)
        vertx.createHttpClient(options).webSocket(
            "/api/events"
        ) { res: AsyncResult<WebSocket> ->
            if (res.succeeded()) {
                println("Connected!")
                val ws = res.result()
                ws.frameHandler { frame -> println(">> $frame") }
            }
        }*/

        /*
        vertx.createHttpClient().websocket(port,"localhost","/api/events", res -> {
              // if (res.succeeded()) {
                  System.out.println("Connected!");
                  // WebSocket ws = res.result();
                  res.frameHandler(frame -> {
                      System.out.println(">> " + frame);
                  });
              // }
            });
    }*/

    private val port = 40426

    @Before fun startService() {
        val ready = Vertx.vertx().deployVerticle(PuzzleService(port))
        while (!ready.isComplete) Thread.sleep(100) // TODO
    }
}
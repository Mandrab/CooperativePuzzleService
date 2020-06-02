package service

import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.HttpResponse
import io.vertx.ext.web.client.WebClient
import org.junit.Before
import org.junit.Test

class TestService {

    @Test fun testGetPuzzleToJoin() {
        val vertx = Vertx.vertx()
        val client = WebClient.create(vertx)

        client.get(port, "localhost", "/puzzle").send {
            if (it.succeeded()) {
                val response = it.result()
                val body = response.bodyAsJsonObject()
                println("Response: $body")
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
        Thread.sleep(2000)
    }

    @Test fun testLoginToPuzzle() {
        val vertx = Vertx.vertx()
        val client = WebClient.create(vertx)

        val params = JsonObject().put("playerID", "player1")

        client.post(port, "localhost", "/puzzle/puzzle1/user").sendJson(params) {
            if (it.succeeded()) {
                val response = it.result()
                val code = response.statusCode()
                println("Status code: $code")
            } else {
                println("Something went wrong ${it.cause().message}")
            }
        }
        Thread.sleep(2000)

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
            });*/
    }

    private val port = 40426

    @Before fun startService() {
        val ready = Vertx.vertx().deployVerticle(MyService(port))
        while (!ready.isComplete) Thread.sleep(100) // TODO
    }
}
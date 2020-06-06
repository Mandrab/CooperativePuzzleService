package client.view

import javax.swing.JFrame


object Mouse {

    /* CLIENT PART
    private fun openWS(puzzleID: String) {
       httpClient.webSocket(port, SERVICE_HOST, "/puzzle/$puzzleID/user") {
           it.result()?.also { ws ->
               webSocket = ws

               ws.textMessageHandler {
                   println(this.toString() + it)
                   val player = JsonObject(it).getString("player")
                   JsonObject(it).getJsonObject("position").apply {
                       view.drawPointer(player, getInteger("x"), getInteger("y"))
                   }
               }.binaryMessageHandler {
                   it.toJsonObject().getString("player")
                   val player = it.toJsonObject().getString("player")
                   it.toJsonObject().getJsonObject("position").apply {
                       view.drawPointer(player, getInteger("x"), getInteger("y"))
                   }
               }.exceptionHandler {
                   TODO()
               }.closeHandler {
                   TODO()
               }
           }
       }
   }

   fun newMovement(point: Point) {
       val msg = JsonObject().put("player", name).put("position", JsonObject().put("x", point.x).put("y", point.y))
       //webSocket.writeBinaryMessage(msg.toBuffer())
   }*/
}
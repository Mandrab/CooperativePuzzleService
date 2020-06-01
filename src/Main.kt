import com.rabbitmq.client.*

private const val EXCHANGE_NAME = "logs"

fun main() {

    val factory = ConnectionFactory()
    factory.host = "localhost"
    val connection: Connection = factory.newConnection()
    val channel: Channel = connection.createChannel()

    channel.exchangeDeclare(EXCHANGE_NAME, "fanout")
    val queueName: String = channel.queueDeclare().queue
    channel.queueBind(queueName, EXCHANGE_NAME, "")

    println("[*] Waiting for messages. To exit press CTRL+C")

    val deliverCallback = DeliverCallback { _: String, delivery: Delivery ->
        val message = String(delivery.body, Charsets.UTF_8)
        println("[x] Received '$message'")
    }

    channel.basicConsume(queueName, deliverCallback, CancelCallback { })
}
import com.rabbitmq.client.ConnectionFactory

object Test2_Publisher {
    private const val EXCHANGE_NAME = "logs"
    private const val NO_QUEUE_NAME = ""

    @Throws(Exception::class)
    @JvmStatic
    fun main(argv: Array<String>) {
        val factory = ConnectionFactory()
        factory.host = "localhost"
        val connection = factory.newConnection()
        val channel = connection.createChannel()
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout")
        val message = getMessage(argv)
        channel.basicPublish(
            EXCHANGE_NAME,
            NO_QUEUE_NAME,
            null,
            message.toByteArray(charset("UTF-8"))
        )
        println("[x] Sent '$message'")
        channel.close()
        connection.close()
    }

    private fun getMessage(strings: Array<String>): String {
        return if (strings.size < 1) "info: Hello World!" else joinStrings(strings, " ")
    }

    private fun joinStrings(strings: Array<String>, delimiter: String): String {
        val length = strings.size
        if (length == 0) return ""
        val words = StringBuilder(strings[0])
        for (i in 1 until length) {
            words.append(delimiter).append(strings[i])
        }
        return words.toString()
    }
}
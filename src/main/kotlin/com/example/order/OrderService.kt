package com.example.order

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.math.BigDecimal

private const val ORDER_STATUS_PROCESSED = "PROCESSED"
private const val CANCELLATION_COMPLETED = "COMPLETED"

private const val NOTIFICATION_TYPE_ORDER_PLACED = "ORDER_PLACED"
private const val NOTIFICATION_TYPE_ORDER_CANCELLED = "ORDER_CANCELLED"

private const val SERVICE_NAME = "order-service"

@Service
class OrderService(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>
) {

    init {
        println("$SERVICE_NAME started running..")
    }

    companion object {
        private const val PLACE_ORDER_TOPIC = "place-order"
        private const val PROCESS_ORDER_TOPIC = "process-order"
        private const val CANCEL_ORDER_TOPIC = "cancel-order"
        private const val PROCESS_CANCELLATION_TOPIC = "process-cancellation"
        private const val NOTIFICATION_TOPIC = "notification"
    }


    @KafkaListener(topics = [PLACE_ORDER_TOPIC, CANCEL_ORDER_TOPIC])
    fun placeOrder(record: ConsumerRecord<String, ByteArray>, ack: Acknowledgment) {
        val topic = record.topic()

        when (topic) {
            PLACE_ORDER_TOPIC -> {
                val orderRequestJson = deserialize<OrderRequest>(record.value())

                println("[$SERVICE_NAME] Received message on topic $PLACE_ORDER_TOPIC - $orderRequestJson")

                processPlaceOrderMessage(orderRequestJson)
            }

            CANCEL_ORDER_TOPIC -> {
                val orderIdObject = deserialize<OrderId>(record.value())

                println("[$SERVICE_NAME] Received message on topic $CANCEL_ORDER_TOPIC - $orderIdObject")

                processCancellation(orderIdObject)
            }
        }

        ack.acknowledge()
    }


    private fun processPlaceOrderMessage(orderRequest: OrderRequest) {
        sendMessageOnProcessOrderTopic(orderRequest)
        val message = """{"message": "Order processed successfully", "type": "$NOTIFICATION_TYPE_ORDER_PLACED"}"""
        sendMessageOnNotificationTopic(message)
    }

    private fun processCancellation(orderIdObject: OrderId) {
        sendMessageOnProcessCancellationTopic(orderIdObject)
        val notificationMessage = """{"message": "Order cancelled successfully", "type": "$NOTIFICATION_TYPE_ORDER_CANCELLED"}"""
        sendMessageOnNotificationTopic(notificationMessage)
    }


    private fun sendMessageOnProcessCancellationTopic(orderIdObject: OrderId) {
        val message = serialize(CancellationReference(345, CANCELLATION_COMPLETED))
        println("[$SERVICE_NAME] Publishing a message on $PROCESS_CANCELLATION_TOPIC topic")
        kafkaTemplate.send(PROCESS_CANCELLATION_TOPIC, message)
    }

    private fun sendMessageOnProcessOrderTopic(orderRequest: OrderRequest) {
        val totalAmount = orderRequest.orderItems.sumOf { it.price * BigDecimal(it.quantity) }
        val message = serialize(mapOf("id" to 10, "totalAmount" to totalAmount, "status" to ORDER_STATUS_PROCESSED))

        println("[$SERVICE_NAME] Publishing a message on $PROCESS_ORDER_TOPIC topic")
        kafkaTemplate.send(PROCESS_ORDER_TOPIC, message)
    }

    private fun sendMessageOnNotificationTopic(message: String) {
        val messageBytes = serialize(mapOf("message" to message, "type" to NOTIFICATION_TYPE_ORDER_PLACED))
        println("[$SERVICE_NAME] Publishing a message on $NOTIFICATION_TOPIC topic")
        kafkaTemplate.send(NOTIFICATION_TOPIC, messageBytes)
    }

    private inline fun <reified T> deserialize(data: ByteArray): T {
        return ObjectMapper().registerKotlinModule().readValue(data, T::class.java)
    }

    private fun serialize(data: Any): ByteArray {
        return ObjectMapper().registerKotlinModule().writeValueAsBytes(data)
    }

}

data class OrderRequest(
    val id: Int,
    val orderItems: List<OrderItem>
)

data class OrderItem(
    val id: Int,
    val name: String,
    val quantity: Int,
    val price: BigDecimal
)

data class OrderId(
    val id: Int
)

data class CancellationReference(
    val reference: Int,
    val status: String
)
package com.example.order

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.header.Headers
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Service
import java.math.BigDecimal

private const val ORDER_STATUS_PROCESSED = "PROCESSED"
private const val CANCELLATION_COMPLETED = "COMPLETED"

private const val NOTIFICATION_TYPE_ORDER_PLACED = "ORDER_PLACED"
private const val NOTIFICATION_TYPE_ORDER_CANCELLED = "ORDER_CANCELLED"

private const val SERVICE_NAME = "order-service"

@Service
class OrderService(
    private val kafkaTemplate: KafkaTemplate<String, String>
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
    fun placeOrder(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val topic = record.topic()

        when(topic) {
            PLACE_ORDER_TOPIC -> {
                val orderRequest = record.value()
                val headers = record.headers()

                println("[$SERVICE_NAME] Received message on topic $PLACE_ORDER_TOPIC - $orderRequest")
                println("[$SERVICE_NAME] Headers: ${headers.joinToString { "${it.key()}: ${String(it.value())}" }}")

                val orderRequestJson = try {
                    jacksonObjectMapper().apply {
                        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                    }.readValue(orderRequest, OrderRequest::class.java)
                } catch(e: Exception) {
                    throw e
                }

                processPlaceOrderMessage(orderRequestJson, headers)
            }
            CANCEL_ORDER_TOPIC -> {
                val cancellationRequest = record.value()
                val headers = record.headers()

                println("[$SERVICE_NAME] Received message on topic $CANCEL_ORDER_TOPIC - $cancellationRequest")

                val orderIdObject = try {
                    jacksonObjectMapper().apply {
                        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                    }.readValue(cancellationRequest, OrderId::class.java)
                } catch(e: Exception) {
                    throw e
                }

                processCancellation(orderIdObject, headers)
            }
        }

        ack.acknowledge()
    }

    private fun processPlaceOrderMessage(orderRequest: OrderRequest, headers: Headers) {
        sendMessageOnProcessOrderTopic(orderRequest, headers)
        val message = """{"message": "Order processed successfully", "type": "$NOTIFICATION_TYPE_ORDER_PLACED"}"""
        sendMessageOnNotificationTopic(message, headers)
    }

    private fun processCancellation(orderIdObject: OrderId, headers: Headers) {
        sendMessageOnProcessCancellationTopic(orderIdObject, headers)
        val notificationMessage = """{"message": "Order cancelled successfully", "type": "$NOTIFICATION_TYPE_ORDER_CANCELLED"}"""
        sendMessageOnNotificationTopic(notificationMessage, headers)
    }

    private fun sendMessageOnProcessCancellationTopic(orderIdObject: OrderId, headers: Headers) {
        val reference = 345
        val cancellationMessage = """{"reference": $reference, "status": "$CANCELLATION_COMPLETED"}"""
        println("[$SERVICE_NAME] Publishing a message on $PROCESS_CANCELLATION_TOPIC topic: $cancellationMessage")
        val correlationId = headers.lastHeader("correlationId")?.value()?.let { String(it) }
        if (correlationId != null) {
            val invalidRecord = ProducerRecord<String, String>(PROCESS_CANCELLATION_TOPIC, cancellationMessage)
            invalidRecord.headers().add("correlationId", "\"ABC\"".toByteArray())
            println(invalidRecord.toString())
            kafkaTemplate.send(invalidRecord)

            Thread.sleep(1_000)
            val validRecord = ProducerRecord<String, String>(PROCESS_CANCELLATION_TOPIC, cancellationMessage)
            validRecord.headers().add("correlationId", correlationId.toByteArray())
            println(validRecord.toString())
            kafkaTemplate.send(validRecord)
        }
    }

    private fun sendMessageOnProcessOrderTopic(orderRequest: OrderRequest, headers: Headers) {
        val id = 10
        val totalAmount = orderRequest.orderItems.sumOf { it.price * BigDecimal(it.quantity) }
        val taskMessage = """{"id": $id, "totalAmount": $totalAmount, "status": "$ORDER_STATUS_PROCESSED"}"""
        println("[$SERVICE_NAME] Publishing a message on $PROCESS_ORDER_TOPIC topic: $taskMessage")
        val correlationId = headers.lastHeader("correlationId")?.value()?.let { String(it) }
        if (correlationId != null) {
            val invalidRecord = ProducerRecord<String, String>(PROCESS_ORDER_TOPIC, taskMessage)
            invalidRecord.headers().add("correlationId", "\"ABC\"".toByteArray())
            println(invalidRecord.toString())
            kafkaTemplate.send(invalidRecord)

            Thread.sleep(1_000)
            val validRecord = ProducerRecord<String, String>(PROCESS_ORDER_TOPIC, taskMessage)
            validRecord.headers().add("correlationId", correlationId.toByteArray())
            println(validRecord.toString())
            kafkaTemplate.send(validRecord)
        }
    }

    private fun sendMessageOnNotificationTopic(message: String, headers: Headers) {
        println("[$SERVICE_NAME] Publishing a message on $NOTIFICATION_TOPIC topic: $message")
        val correlationId = headers.lastHeader("correlationId")?.value()?.let { String(it) }
        if (correlationId != null) {
            val invalidRecord = ProducerRecord<String, String>(NOTIFICATION_TOPIC, message)
            invalidRecord.headers().add("correlationId", "\"ABC\"".toByteArray())
            println(invalidRecord.toString())
            kafkaTemplate.send(invalidRecord)

            Thread.sleep(1_000)
            val validRecord = ProducerRecord<String, String>(NOTIFICATION_TOPIC, message)
            validRecord.headers().add("correlationId", correlationId.toByteArray())
            println(validRecord.toString())
            kafkaTemplate.send(validRecord)
        }
    }
}

data class OrderRequest(
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
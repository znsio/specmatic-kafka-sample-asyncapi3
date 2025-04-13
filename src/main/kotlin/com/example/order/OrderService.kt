package com.example.order

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service
import java.math.BigDecimal

private const val ORDER_STATUS_PROCESSED = "PROCESSED"
private const val CANCELLATION_COMPLETED = "COMPLETED"
private const val SERVICE_NAME = "OrderService"
private const val PLACE_ORDER_TOPIC = "place-order"
private const val PROCESS_ORDER_TOPIC = "process-order"
private const val CANCEL_ORDER_TOPIC = "cancel-order"
private const val PROCESS_CANCELLATION_TOPIC = "process-cancellation"

@Service
class OrderService(private val kafkaTemplate: KafkaTemplate<String, String>) {
    init {
        println("$SERVICE_NAME started running..")
    }

    @KafkaListener(topics = [PLACE_ORDER_TOPIC, CANCEL_ORDER_TOPIC])
    fun orderUpdates(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
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

                processPlaceOrderMessage(orderRequestJson)
            }
            CANCEL_ORDER_TOPIC -> {
                val cancellationRequest = record.value()

                println("[$SERVICE_NAME] Received message on topic $CANCEL_ORDER_TOPIC - $cancellationRequest")

                val orderIdObject = try {
                    jacksonObjectMapper().apply {
                        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                    }.readValue(cancellationRequest, OrderId::class.java)
                } catch(e: Exception) {
                    throw e
                }

                processCancellation(orderIdObject)
            }
        }

        ack.acknowledge()
    }

    private fun processPlaceOrderMessage(orderRequest: OrderRequest) {
        sendMessageOnProcessOrderTopic(orderRequest)
    }

    private fun processCancellation(orderIdObject: OrderId) {
        sendMessageOnProcessCancellationTopic(orderIdObject)
    }

    private fun sendMessageOnProcessCancellationTopic(orderIdObject: OrderId) {
        val cancellationMessage = """{"reference": ${orderIdObject.id}, "status": "$CANCELLATION_COMPLETED"}"""

        println("[$SERVICE_NAME] Publishing a message on $PROCESS_CANCELLATION_TOPIC topic: $cancellationMessage")
        kafkaTemplate.send(PROCESS_CANCELLATION_TOPIC, cancellationMessage)
    }

    private fun sendMessageOnProcessOrderTopic(orderRequest: OrderRequest) {
        val totalAmount = orderRequest.orderItems.sumOf { it.price * BigDecimal(it.quantity) }
        val taskMessage = """{"id": ${orderRequest.id}, "totalAmount": $totalAmount, "status": "$ORDER_STATUS_PROCESSED"}"""

        println("[$SERVICE_NAME] Publishing a message on $PROCESS_ORDER_TOPIC topic: $taskMessage")
        kafkaTemplate.send(PROCESS_ORDER_TOPIC, taskMessage)
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
package com.example.order

import com.example.order.OrderStatus.CANCELLED
import com.example.order.OrderStatus.PROCESSED
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

private const val SERVICE_NAME = "OrderService"
private const val PLACE_ORDER_TOPIC = "place-order"
private const val PROCESS_ORDER_TOPIC = "process-order"
private const val CANCEL_ORDER_TOPIC = "cancel-order"
private const val PROCESS_CANCELLATION_TOPIC = "process-cancellation"

@Service
class OrderService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val orderRepository: OrderRepository
) {
    init {
        println("$SERVICE_NAME started running..")
    }

    @KafkaListener(topics = [PLACE_ORDER_TOPIC, CANCEL_ORDER_TOPIC])
    fun orderUpdates(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        val topic = record.topic()

        when (topic) {
            PLACE_ORDER_TOPIC -> {
                val orderRequest = record.value()
                val headers = record.headers()

                println("[$SERVICE_NAME] Received message on topic $PLACE_ORDER_TOPIC - $orderRequest")
                println("[$SERVICE_NAME] Headers: ${headers.joinToString { "${it.key()}: ${String(it.value())}" }}")

                val placeOrderRequestJson = try {
                    jacksonObjectMapper().apply {
                        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                    }.readValue(orderRequest, PlaceOrderRequest::class.java)
                } catch (e: Exception) {
                    throw e
                }

                processPlaceOrderMessage(placeOrderRequestJson)
            }

            CANCEL_ORDER_TOPIC -> {
                val cancellationRequest = record.value()

                println("[$SERVICE_NAME] Received message on topic $CANCEL_ORDER_TOPIC - $cancellationRequest")

                val orderIdObject = try {
                    jacksonObjectMapper().apply {
                        configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
                    }.readValue(cancellationRequest, OrderId::class.java)
                } catch (e: Exception) {
                    throw e
                }

                processCancellation(orderIdObject)
            }
        }

        ack.acknowledge()
    }

    private fun processPlaceOrderMessage(placeOrderRequest: PlaceOrderRequest) {
        val order = Order(
            id = placeOrderRequest.id,
            orderItems = placeOrderRequest.orderItems,
            status = PROCESSED
        )
        sendMessageOnProcessOrderTopic(order)
        orderRepository.save(order)
    }

    private fun processCancellation(id: OrderId) {
        val order = Order(
            id = id.id,
            status = CANCELLED
        )
        sendMessageOnProcessCancellationTopic(order)
        orderRepository.save(order)
    }

    private fun sendMessageOnProcessCancellationTopic(order: Order) {
        val cancellationMessage = """{"reference": ${order.id}, "status": "${order.status}"}"""

        println("[$SERVICE_NAME] Publishing a message on $PROCESS_CANCELLATION_TOPIC topic: $cancellationMessage")
        kafkaTemplate.send(PROCESS_CANCELLATION_TOPIC, cancellationMessage)
    }

    private fun sendMessageOnProcessOrderTopic(order: Order) {
        val taskMessage =
            """{"id": ${order.id}, "totalAmount": ${order.totalAmount()}, "status": "${order.status}"}"""

        println("[$SERVICE_NAME] Publishing a message on $PROCESS_ORDER_TOPIC topic: $taskMessage")
        kafkaTemplate.send(PROCESS_ORDER_TOPIC, taskMessage)
    }
}

data class PlaceOrderRequest(
    val id: Int,
    val orderItems: List<OrderItem>
)
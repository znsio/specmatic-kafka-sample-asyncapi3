package com.example.order

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Service

private const val SERVICE_NAME = "OrderDeliveryService"
private const val INITIATE_ORDER_DELIVERY_TOPIC = "initiate-order-delivery"

@Service
class OrderDeliveryService(
    private val orderRepository: OrderRepository
) {
    init {
        println("$SERVICE_NAME started running..")
    }

    @KafkaListener(topics = [INITIATE_ORDER_DELIVERY_TOPIC])
    fun orderDeliveryUpdates(record: ConsumerRecord<String, String>, ack: Acknowledgment) {
        when(record.topic()) {
            INITIATE_ORDER_DELIVERY_TOPIC -> initiateOrderDelivery(record)
        }
        ack.acknowledge()
    }

    private fun initiateOrderDelivery(record: ConsumerRecord<String, String>) {
        val initiateOrderDeliveryRequest = record.value()
        println("[$SERVICE_NAME] Received message on topic $INITIATE_ORDER_DELIVERY_TOPIC - $initiateOrderDeliveryRequest")

        val request = try {
            jacksonObjectMapper().apply {
                configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
            }.readValue(initiateOrderDeliveryRequest, OrderDeliveryRequest::class.java)
        } catch (e: Exception) {
            throw e
        }

        orderRepository.save(
            Order(
                id = request.orderId,
                lastUpdatedDate = request.deliveryDate,
                status = OrderStatus.SHIPPED
            )
        )
        println("[$SERVICE_NAME] Order with orderId '${request.orderId}' is ${OrderStatus.SHIPPED}")
    }

    fun findById(orderId: Int, status: OrderStatus): Order? {
        return orderRepository.findById(orderId, status)
    }
}

data class OrderDeliveryRequest(
    val orderId: Int,
    val deliveryAddress: String,
    val deliveryDate: String
)
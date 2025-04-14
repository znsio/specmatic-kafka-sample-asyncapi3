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
    private val orderDeliveryRepository: OrderDeliveryRepository
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

        orderDeliveryRepository.save(
            OrderDelivery(
                orderId = request.orderId,
                deliveryAddress = request.deliveryAddress,
                deliveryDate = request.deliveryDate,
                deliveryStatus = DeliveryStatus.INITIATED
            )
        )
        println("[$SERVICE_NAME] Order delivery with orderId '${request.orderId}' is initiated")
    }

    fun findById(orderId: Int): OrderDelivery? {
        return orderDeliveryRepository.findById(orderId)
    }
}

enum class DeliveryStatus {
    PENDING, INITIATED, SHIPPED, DELIVERED
}

data class OrderDeliveryRequest(
    val orderId: Int,
    val deliveryAddress: String,
    val deliveryDate: String
)

data class OrderDelivery(
    val orderId: Int,
    val deliveryAddress: String,
    val deliveryDate: String,
    val deliveryStatus: DeliveryStatus
)

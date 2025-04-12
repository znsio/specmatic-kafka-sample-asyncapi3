package com.example.order

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

private const val SERVICE_NAME = "order-notification-service"

@Service
class OrderNotificationService(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    fun notify(request: NotifyOrderRequest) {
        println("[$SERVICE_NAME] Publishing the notify message on topic 'notify-order'..")
        kafkaTemplate.send(
            "notify-order",
           ObjectMapper().writeValueAsString(request)
        ).get()
    }
}
package com.example.order

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

private const val SERVICE_NAME = "OrderNotificationService"
private const val TOPIC_NAME = "notify-order"

@Service
class OrderNotificationService(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    fun notify(request: NotifyOrderRequest) {
        println("[$SERVICE_NAME] Publishing the notify message on topic '$TOPIC_NAME'..")
        kafkaTemplate.send(
            TOPIC_NAME,
           ObjectMapper().writeValueAsString(request)
        ).get()
    }
}
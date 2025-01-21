package com.example.order

import io.specmatic.async.junit.SpecmaticKafkaContractTest
import io.specmatic.async.utils.CONSUMER_GROUP_ID
import io.specmatic.async.utils.EXAMPLES_DIR
import io.specmatic.kafka.mock.KafkaMock
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext

private const val IN_MEMORY_BROKER_HOST = "localhost"
private const val IN_MEMORY_BROKER_PORT = 9092

class ContractTest : SpecmaticKafkaContractTest {

    companion object {
        private lateinit var context: ConfigurableApplicationContext
        private lateinit var kafkaMock: KafkaMock

        @JvmStatic
        @BeforeAll
        fun setup() {
            System.setProperty(EXAMPLES_DIR, "src/test/resources")
            System.setProperty(CONSUMER_GROUP_ID, "order-consumer-group-id")
            kafkaMock = KafkaMock.startInMemoryBroker(IN_MEMORY_BROKER_HOST, IN_MEMORY_BROKER_PORT)
            startApplication()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            stopApplication()
            kafkaMock.stop()
        }

        private fun startApplication() {
            context = SpringApplication.run(OrderServiceApplication::class.java)
            Thread.sleep(5000)
        }

        private fun stopApplication() {
            context.stop()
        }
    }

}
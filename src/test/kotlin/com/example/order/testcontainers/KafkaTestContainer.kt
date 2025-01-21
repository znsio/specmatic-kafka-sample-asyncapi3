package com.example.order.testcontainers

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.containers.GenericContainer

class KafkaTestContainer {
    private val zookeeperPort = 2181
    private val kafkaPort = 9092
    private val host = "127.0.0.1"

    private val kafkaBroker = GenericContainer(
    "johnnypark/kafka-zookeeper"
    ).withExposedPorts(zookeeperPort, kafkaPort)
    .withCreateContainerCmdModifier {
        it.withHostConfig(
            HostConfig().withPortBindings(
                PortBinding(Ports.Binding.bindPort(zookeeperPort), ExposedPort(zookeeperPort)),
                PortBinding(Ports.Binding.bindPort(kafkaPort), ExposedPort(kafkaPort))
            )
        )
    }.withEnv("ADVERTISED_HOST", host)

    fun start() = kafkaBroker.start()
    fun stop() = kafkaBroker.stop()
}
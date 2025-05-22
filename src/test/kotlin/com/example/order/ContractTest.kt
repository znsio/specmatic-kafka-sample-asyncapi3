package com.example.order

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.startupcheck.StartupCheckStrategy
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ContractTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            zookeeper.start()
            kafkaBroker.start()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            zookeeper.stop()
            kafkaBroker.stop()
        }

        private val network = Network.newNetwork()

        private val zookeeper = GenericContainer(DockerImageName.parse("confluentinc/cp-zookeeper:7.2.1"))
            .withEnv("ZOOKEEPER_CLIENT_PORT", "2181")
            .withNetwork(network)
            .withNetworkAliases("zookeeper")
            .withExposedPorts(2181)
            .waitingFor(HostPortWaitStrategy().forPorts(2181))

        private val kafkaBroker = GenericContainer(DockerImageName.parse("confluentinc/cp-kafka:7.2.1"))
            .withNetwork(network)
            .withExposedPorts(9092)
            .withEnv("KAFKA_BROKER_ID", "1")
            .withEnv("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:2181")
            .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9092")
            .withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9092")
            .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
            .withNetworkAliases("kafka")
            .withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig?.withPortBindings(
                    com.github.dockerjava.api.model.PortBinding.parse("9092:9092")
                )
            }
            .waitingFor(
                LogMessageWaitStrategy()
                    .withRegEx(".*started.*kafka.server.KafkaServer.*")
                    .withStartupTimeout(Duration.ofSeconds(30))
            )
    }

    private val specmaticKafkaTestContainer: GenericContainer<*> = GenericContainer("znsio/specmatic-kafka")
        .withCommand(
            "test",
            "--host=localhost",
            "--port=9092",
            "--overlay=spec_overlay.yaml"
            // Set the external examples directory if there are no inline examples in the spec
            // "--examples=/path/to/examples_dir"
        )
        .withNetworkMode("host")
        .withFileSystemBind(
            "./src/test/resources/specmatic.yaml",
            "/usr/src/app/specmatic.yaml",
            BindMode.READ_ONLY
        )
        .withFileSystemBind(
            "./src/test/resources/spec_overlay.yaml",
            "/usr/src/app/spec_overlay.yaml",
            BindMode.READ_ONLY
        )
        .withFileSystemBind(
            "./build/reports/",
            "/usr/src/app/build/reports/",
            BindMode.READ_WRITE
        )
        .waitingFor(Wait.forLogMessage(".*Tests run:.*", 1))
        .withLogConsumer { print(it.utf8String) }

    @Test
    fun contractTests() {
        // wait for kafkaBroker to stabilize
        Thread.sleep(5000)
        specmaticKafkaTestContainer.start()
        val hasSucceeded = specmaticKafkaTestContainer.logs.contains("Result: FAILED").not()
        assertThat(hasSucceeded).isTrue()
    }
}
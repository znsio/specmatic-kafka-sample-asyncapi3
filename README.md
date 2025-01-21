# Specmatic Kafka Sample

* [Specmatic Website](https://specmatic.io)
* [Specmatic Documenation](https://specmatic.io/documentation.html)

This sample project demonstrates how we can run contract tests against a service which interacts with a kafka broker.

Here we are using [AsyncAPI 3.0.0 specification](https://www.asyncapi.com/docs/reference/specification/v3.0.0). For equivalent [AsyncAPI 2.6.0 API specification](https://www.asyncapi.com/blog/release-notes-2.6.0) based implementation, please refer to [this repository](https://github.com/znsio/specmatic-kafka-sample).

## Background

This project includes a consumer that listens to messages on receive topic for requests and then upon receiving a message, it processes the same and publishes a reply message to send topic.
Thereby it demonstrates the [request reply pattern](https://www.asyncapi.com/docs/tutorials/getting-started/request-reply) in AsyncAPI 3.0.0 specification also.

![Specmatic Kafka Sample Architecture](AsyncAPI-Request-Reply-Draft.gif)

## Pre-requisites
* Gradle
* JDK 17+
* Docker

## Run the tests
```shell
./gradlew clean test
```

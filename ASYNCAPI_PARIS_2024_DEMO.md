# AsyncAPI Paris 2024

[![Contract-Driven Development for Event-Driven Architecture](https://img.youtube.com/vi/zF88Bn1o-d0/maxresdefault.jpg)](https://www.youtube.com/watch?v=zF88Bn1o-d0)

Below are the instructions from the demo at AsyncAPI Paris 2024.

## Request Reply Pattern with AsyncAPI 2.6

### Start Kafka Broker
```shell
docker compose up
```

### Run the application
```shell
./gradlew bootRun 
```

### Run AsyncAPI 2.6 spec as a contract test for `request-reply` pattern
```shell
docker run --network host -v "$PWD/api-specifications/order-service-async-v2_6_0.yaml:/usr/src/app/order-service-async-v2_6_0.yaml" -v "$PWD/build/reports:/usr/src/app/build/reports" znsio/specmatic-kafka-trial test /usr/src/app/order-service-async-v2_6_0.yaml
```

You should now see the HTML Test Report in `build/reports/index.html` with the messages that were sent and received as part of the contract tests.

Note: Please stop the Kafka broker and the application after before moving to the next section.

## Convert AsyncAPI 2.6 to AsyncAPI 3.0

Install the `asyncapi` CLI tool [https://www.asyncapi.com/tools/cli](https://www.asyncapi.com/tools/cli) if you do not already have it.

Use the same to convert the `order-service-async-v2_6_0.yaml` to `order-service-async-v3_0_0.yaml` as shown below:

```shell
asyncapi convert ./api-specifications/order-service-async-v2_6_0.yaml > ./api-specifications/order-service-async-v3_0_0.yaml
```

Review the diff between the checked in version of `order-service-async-v3_0_0.yaml` and the converted version of `order-service-async-v3_0_0.yaml`. It should only be limited to the request reply syntax related changes.
Once you have read through these changes and understood the same, please use the checked in version of `order-service-async-v3_0_0.yaml` for the next section.

## Run 3.0 spec as a mock server
```shell
docker run --network host -p 9092:9092 -p 2181:2181 -p 29092:29092 -v "$PWD/api-specifications/order-service-async-v3_0_0.yaml:/usr/src/app/order-service-async-v3_0_0.yaml" znsio/specmatic-kafka-trial virtualize /usr/src/app/order-service-async-v3_0_0.yaml
```

## Run AsyncAPI 2.6 spec as a contract test for `request-reply` pattern
```shell
docker run --network host -v "$PWD/api-specifications/order-service-async-v2_6_0.yaml:/usr/src/app/order-service-async-v2_6_0.yaml" -v "$PWD/build/reports:/usr/src/app/build/reports" znsio/specmatic-kafka-trial test /usr/src/app/order-service-async-v2_6_0.yaml
```


# AsyncAPI Paris 2024

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
specmatic-kafka test ./api-specifications/order-service-async-v2_6_0.yaml
```

## Convert AsyncAPI 2.6 to AsyncAPI 3.0

```shell
asyncapi convert ./api-specifications/order-service-async-v2_6_0.yaml > ./api-specifications/order-service-async-v3_0_0.yaml
```

## Run 3.0 spec as a mock server
```shell
specmatic-kafka virtualize ./api-specifications/order-service-async-v3_0_0.yaml
```

## Run AsyncAPI 2.6 spec as a contract test for `request-reply` pattern
```shell
specmatic-kafka test ./api-specifications/order-service-async-v2_6_0.yaml
```


# Avro schema in AsyncAPI

### Start Kafka Broker
```shell
docker compose up
```

### Run the application
```shell
./gradlew bootRun 
```

### Run AsyncAPI spec (which refers to Avro for payload schema) as a contract test for `request-reply` pattern
```shell
specmatic-kafka test ./api-specifications/order-service-async-v3_0_0.yaml --examples=src/test/resources/specmatic/order_service_async_v1
```

## Loop Test - Running specification as stub and running the same spec as a test against the stub

Make sure you have shutdown all of the above running services before running these tests.

### Run AsyncAPI spec as a mock server
```shell
specmatic-kafka virtualize ./api-specifications/order-service-async-v3_0_0.yaml --examples=src/test/resources/specmatic/order_service_async_v1
```

### Running
```shell
specmatic-kafka test ./api-specifications/order-service-async-v3_0_0.yaml --examples=src/test/resources/specmatic/order_service_async_v1
```
# Getting Started

### Reference Documentation

For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.0/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.0/gradle-plugin/packaging-oci-image.html)
* [Spring for RabbitMQ](https://docs.spring.io/spring-boot/4.0.0/reference/messaging/amqp.html)
* [Cloud Bus](https://docs.spring.io/spring-cloud-bus/reference/)
* [OpenFeign](https://docs.spring.io/spring-cloud-openfeign/reference/)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/4.0.0/reference/using/devtools.html)
* [Docker Compose Support](https://docs.spring.io/spring-boot/4.0.0/reference/features/dev-services.html#features.dev-services.docker-compose)
* [MongoDB](https://docs.spring.io/spring-boot/4.0.0/reference/data/nosql.html#data.nosql.mongodb)
* [OpenTelemetry](https://docs.spring.io/spring-boot/4.0.0/reference/actuator/observability.html#actuator.observability.opentelemetry)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.0/reference/web/servlet.html)

### Guides

The following guides illustrate how to use some features concretely:

* [Messaging with RabbitMQ](https://spring.io/guides/gs/messaging-rabbitmq/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Additional Links

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
* [Declarative REST calls with Spring Cloud OpenFeign sample](https://github.com/spring-cloud-samples/feign-eureka)

### Docker Compose support

This project contains a Docker Compose file named `compose.yaml`.
In this file, the following services have been defined:

* grafana-lgtm: [`grafana/otel-lgtm:latest`](https://hub.docker.com/r/grafana/otel-lgtm)
* rabbitmq: [`rabbitmq:latest`](https://hub.docker.com/_/rabbitmq)

Please review the tags of the used images and set them to the same as you're running in production.

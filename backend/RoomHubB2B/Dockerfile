FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY . .

RUN chmod +x mvnw
RUN ./mvnw clean package \
    -Dtest=ListingAvailabilityServiceTest,ListingAvailabilityControllerTest

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN groupadd --system roomhub \
    && useradd --system --gid roomhub --home-dir /app --shell /usr/sbin/nologin roomhub

COPY --from=build --chown=roomhub:roomhub /app/target/app.jar app.jar

USER roomhub

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /workspace/target/customer-service-*.jar app.jar
USER 10001
ENV CUSTOMER_SERVICE_ADDRESS=0.0.0.0
EXPOSE 8282
ENTRYPOINT ["java", "-jar", "app.jar"]

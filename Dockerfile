# ---- build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# ---- run ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 planner && mkdir -p /app/logs && chown -R planner /app
COPY --from=build /app/target/*.jar app.jar
USER planner
ENV TZ=Asia/Tashkent
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
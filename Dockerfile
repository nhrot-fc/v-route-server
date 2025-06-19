########################################
# Stage 1 – BUILD
########################################
FROM gradle:8.5-jdk21-alpine AS builder  
# build context is /app
WORKDIR /app

# Copy build files first to leverage cache
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle

RUN ./gradlew --no-daemon build -x test || true

# Copy source code
COPY src ./src

RUN ./gradlew --no-daemon clean bootJar

########################################
# Stage 2 – RUNTIME
########################################
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=UTC

EXPOSE 8080
HEALTHCHECK CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar app.jar"]


########################################
# Stage 1 – BUILD
########################################
FROM gradle:8.5-jdk21-alpine AS builder
WORKDIR /app

# 1. Copiar archivos de build
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle

# 2. Hacer ejecutable el wrapper  ⬅️  NEW
RUN chmod +x gradlew

# 3. Descargar dependencias (aprovecha caché)
RUN ./gradlew --no-daemon build -x test || true

# 4. Copiar el resto del código
COPY src ./src

# 5. Compilar el JAR “fat”
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


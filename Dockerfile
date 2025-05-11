# Build stage

FROM bellsoft/liberica-openjdk-alpine:21 AS builder

WORKDIR /app

COPY . .

RUN ./gradlew clean build -x test

# Run stage

FROM bellsoft/liberica-openjdk-alpine:21

WORKDIR /app

RUN mkdir -p /app/tmp

ENV JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=/app/tmp"

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
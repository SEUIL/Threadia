FROM gradle:8.14.3-jdk17 AS build

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew --no-daemon bootJar && \
    jar_path="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" && \
    test -n "$jar_path" && \
    cp "$jar_path" /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system --gid 10001 threadia && \
    useradd --system --uid 10001 --gid threadia --home-dir /app --shell /usr/sbin/nologin threadia && \
    apt-get update && \
    apt-get install --no-install-recommends --yes curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build --chown=threadia:threadia /workspace/app.jar /app/app.jar

USER threadia

EXPOSE 8080

ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "/app/app.jar"]

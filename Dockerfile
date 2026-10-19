FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends python3 python3-venv ca-certificates \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 app
WORKDIR /app
COPY deploy/memory/requirements.txt /app/memory/requirements.txt
RUN python3 -m venv /opt/memory-venv \
    && /opt/memory-venv/bin/pip install --no-cache-dir -r /app/memory/requirements.txt
COPY deploy/memory/ /app/memory/
COPY --from=build /build/target/oncetold-0.0.1-SNAPSHOT.jar /app/app.jar
COPY deploy/start.sh /app/start.sh
RUN chmod 0555 /app/start.sh && chown -R app:app /app
USER app
ENV SPRING_PROFILES_ACTIVE=render
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
EXPOSE 8080
ENTRYPOINT ["/bin/bash", "/app/start.sh"]

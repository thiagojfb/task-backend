# ---- Estágio 1: build com Maven e JDK 21 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Baixa dependências primeiro para aproveitar o cache de camadas
COPY pom.xml lombok.config ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Estágio 2: runtime enxuto com JRE 21 ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuário não-root e diretório de dados do H2 (montado como volume)
RUN addgroup -S app && adduser -S -G app app \
    && mkdir -p /app/dados && chown -R app:app /app

COPY --from=build --chown=app:app /build/target/*.jar /app/app.jar

USER app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

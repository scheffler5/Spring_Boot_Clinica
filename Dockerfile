# syntax=docker/dockerfile:1

# ══════════════════════════════════════════════════════════════════════
# Estágio 1 — build
#   JDK 26 + Maven Wrapper: compila e empacota o .jar
#   Este estágio não vai para a imagem final.
# ══════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:26-jdk-noble AS build
WORKDIR /app

# Copia só o wrapper e o pom.xml primeiro.
# Assim, as dependências Maven só são rebaixadas quando o pom.xml muda.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Baixa dependências em camada separada do código-fonte.
# --mount=type=cache mantém ~/.m2 entre builds (BuildKit).
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw dependency:go-offline -B

# Agora copia o código e empacota (testes ficam no CI).
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests -B

# ══════════════════════════════════════════════════════════════════════
# Estágio 2 — runtime
#   Imagem mínima: só o JDK e o .jar.
#   Sem Maven, sem código-fonte, sem camadas desnecessárias.
# ══════════════════════════════════════════════════════════════════════
FROM eclipse-temurin:26-jdk-noble AS runtime

# Usuário não-root para segurança
RUN groupadd --system clinica && \
    useradd  --system --gid clinica --no-create-home clinica

WORKDIR /app

# Copia APENAS o jar do estágio de build
COPY --from=build /app/target/*.jar app.jar
RUN chown clinica:clinica app.jar

USER clinica
EXPOSE 8080

# UseContainerSupport  → JVM respeita os limites de memória do container
# MaxRAMPercentage=75  → heap usa até 75 % da RAM do container
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]

# Multi-stage build para otimização
FROM eclipse-temurin:17-jdk-alpine as builder

# Definir diretório de trabalho
WORKDIR /app

# Copiar arquivos de configuração do Maven
COPY pom.xml .

# Instalar Maven
RUN apk add --no-cache maven

# Baixar dependências (cache layer)
RUN mvn dependency:go-offline -B

# Copiar código fonte
COPY src src

# Compilar aplicação
RUN mvn clean package -DskipTests

# Imagem final otimizada
FROM eclipse-temurin:17-jre-alpine

# Instalar utilitários necessários
RUN apk add --no-cache curl

# Criar usuário não-root para segurança
RUN addgroup -g 1001 -S spring && adduser -u 1001 -S spring -G spring

# Definir diretório de trabalho
WORKDIR /app

# Copiar JAR da fase de build
COPY --from=builder /app/target/*.jar app.jar

# Alterar propriedade do arquivo para o usuário spring
RUN chown spring:spring app.jar

# Usar usuário não-root
USER spring:spring

# Expor porta da aplicação
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Configurações JVM otimizadas para containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Comando de inicialização
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
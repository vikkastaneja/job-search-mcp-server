# Use the official Eclipse Temurin Alpine image containing Java 25 JRE
FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Optional: Keep the container updated securely using Alpine's package manager
RUN apk update && apk upgrade --no-cache

COPY target/job-search-mcp-server.jar mcp-server.jar

ENV PORT=8081
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "mcp-server.jar"]

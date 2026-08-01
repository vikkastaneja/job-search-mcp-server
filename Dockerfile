FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/job-search-mcp-server.jar mcp-server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "mcp-server.jar"]
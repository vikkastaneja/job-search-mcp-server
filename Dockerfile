FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY target/job-search-mcp-server.jar mcp-server.jar
ENV PORT 8081
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "mcp-server.jar"]
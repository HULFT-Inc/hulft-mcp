FROM eclipse-temurin:17-jre

WORKDIR /app

COPY build/libs/hulft-mcp-1.0.0.jar /app/hulft-mcp.jar

EXPOSE 3333

ENV JAVA_OPTS="-Xmx512m -Xms256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/hulft-mcp.jar"]

FROM openjdk:17-alpine
VOLUME /tmp
COPY target/crypto-trading-bot-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]


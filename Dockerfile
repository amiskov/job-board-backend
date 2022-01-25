FROM openjdk:8-alpine

COPY target/uberjar/job-board-server.jar /job-board-server/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/job-board-server/app.jar"]

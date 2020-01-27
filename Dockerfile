FROM openjdk:8-jdk-alpine

COPY target/everyday-task-board-api-0.0.1-SNAPSHOT.jar everyday-task-board-api.jar

ENTRYPOINT ["java","-jar","/everyday-task-board-api.jar"]
FROM openjdk:17-oracle
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} onepiece-backend.jar
ENTRYPOINT ["java","-jar","/onepiece-backend.jar"]
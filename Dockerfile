#FROM openjdk:17-oracle
FROM container-registry.oracle.com/java/openjdk:17
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} onepiece-backend.jar
ENTRYPOINT ["java","-jar","/onepiece-backend.jar"]
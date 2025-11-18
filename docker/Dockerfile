# eclipse-temurin 17 버전 (alpine 기반) 사용
FROM eclipse-temurin:17-jdk-alpine
COPY ./build/libs/*SNAPSHOT.jar knowwhohow-main.jar
ENTRYPOINT ["java", "-jar", "knowwhohow-main.jar"]
LABEL authors="khj"
FROM maven:3.6.3-openjdk-14

COPY /performance-tests/target/performance-tests-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]



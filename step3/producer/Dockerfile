FROM maven:3.5-jdk-8 as mavenBuild
COPY pom.xml pom.xml
COPY src src
RUN ["mvn", "install"]

FROM java:8
COPY --from=mavenBuild ./target/*.jar ./
CMD [ "bash", "-c", "java -jar *.jar"]

FROM maven:3.6.1-jdk-8 as mavenBuild
COPY pom.xml pom.xml
COPY src src
RUN ["mvn", "install"]

FROM confluentinc/cp-base:5.3.1
COPY --from=mavenBuild ./target/*.jar ./
ENV BROKER_LIST "kafka-1:9092,kafka-2:9092,kafka-3:9092"
ENV JAVA_OPTS ""
CMD [ "bash", "-c", "cub kafka-ready -b ${BROKER_LIST} 3 30 && java ${JAVA_OPTS} -jar *.jar ${BROKER_LIST}" ]


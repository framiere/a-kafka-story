FROM maven:3.5-jdk-8 as mavenBuild
COPY pom.xml pom.xml
COPY src src
RUN ["mvn", "install"]

FROM confluentinc/cp-base
COPY --from=mavenBuild ./target/*.jar ./
ENV BROKER_LIST "kafka-1:9092,kafka-2:9092,kafka-3:9092"
ENV JAVA_OPTS ""
CMD [ "bash", "-c", "cub kafka-ready -b ${BROKER_LIST} 3 30 && java ${JAVA_OPTS} -jar *.jar ${BROKER_LIST}" ]


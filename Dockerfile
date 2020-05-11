FROM openjdk:11-jre-slim
COPY ./target/app.jar app.jar
ENTRYPOINT ["java","-Dfile.encoding=utf8","-jar","-noverify", "-XX:+AlwaysPreTouch","/app.jar"]

FROM openjdk:22-jdk-slim-bookworm

COPY ./target/cats-runner.jar /usr/local/bin/

CMD ["java", "-jar", "/usr/local/bin/cats-runner.jar"]

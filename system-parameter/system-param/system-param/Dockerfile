FROM maven:3.9.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src

CMD ["mvn", "-q", "test"]

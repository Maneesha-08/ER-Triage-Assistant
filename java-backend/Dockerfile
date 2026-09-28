FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src ./src

RUN mkdir -p bin && javac -d bin src/*.java

EXPOSE 8080

CMD ["java", "-cp", "bin", "Main"]

FROM mozilla/sbt as builder
WORKDIR /builder
COPY . .
RUN sbt compile assembly
RUN cp target/scala-2.13/backend-hnv-assembly-0.1.jar backend.jar

FROM openjdk:11
WORKDIR /app
RUN wget https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh && chmod +x wait-for-it.sh
COPY --from=builder /builder/backend.jar backend.jar
EXPOSE 8080
CMD java -jar backend.jar

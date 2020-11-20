FROM openjdk:11
WORKDIR /app
RUN wget https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh && chmod +x wait-for-it.sh
ADD target/scala-2.13/backend-hnv-assembly-0.1.jar backend.jar
EXPOSE 8080
CMD java -jar backend.jar

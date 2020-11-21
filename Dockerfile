FROM openjdk:11
WORKDIR /app
RUN wget https://raw.githubusercontent.com/vishnubob/wait-for-it/master/wait-for-it.sh && chmod +x wait-for-it.sh
ADD backend.jar backend.jar
EXPOSE 8080
CMD java -jar backend.jar

# Build in Clojure container
FROM clojure:alpine

WORKDIR /app

COPY ./project.clj .
RUN lein deps

ADD . .

RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

# Serve from a minimal JRE container
FROM openjdk:8-jre-alpine

EXPOSE 9999

WORKDIR /app
COPY --from=0 /app/app-standalone.jar /app
CMD [ "java", "-jar", "/app/app-standalone.jar" ]

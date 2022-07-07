FROM gcr.io/distroless/java11@sha256:7f178c59dd787ad1c9914a35943ae7d4a920428ff4f7fb8530264acd5dc2041e
EXPOSE 3004
COPY ./target/ms-fitnote-controller*.jar /ms-fitnote-controller.jar
COPY ./src/main/properties/tessdata /tessdata
COPY ./config.yml /config.yml
ENTRYPOINT [ "java", "-jar", "/ms-fitnote-controller.jar", "server", "/config.yml" ]

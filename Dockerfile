FROM gcr.io/distroless/java11@sha256:266203e60c9d67792f3a0e16d9e4b95caca61d7ea23d044171618c17e26ce2a9
USER nonroot
EXPOSE 3004
COPY ./target/ms-fitnote-controller*.jar /ms-fitnote-controller.jar
COPY ./src/main/properties/tessdata /tessdata
COPY ./config.yml /config.yml
ENTRYPOINT [ "java", "-jar", "/ms-fitnote-controller.jar", "server", "/config.yml" ]

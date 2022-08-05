FROM gcr.io/distroless/java11@sha256:5814a55f4ec3b2cebedab0e35f6073dbaa0554393026a333a905bf2578d5a481
EXPOSE 3004
COPY ./target/ms-fitnote-controller*.jar /ms-fitnote-controller.jar
COPY ./src/main/properties/tessdata /tessdata
COPY ./config.yml /config.yml
ENTRYPOINT [ "java", "-jar", "/ms-fitnote-controller.jar", "server", "/config.yml" ]

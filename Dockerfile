FROM maven:3.9.11-eclipse-temurin-17@sha256:2490f24bcc21b108f850f7039049a9d7f37d6d10b46b0b77779f5d76c12fad64 as builder
RUN wget --progress=dot:giga https://github.com/ImageMagick/ImageMagick/archive/refs/tags/7.1.1-10.tar.gz \
  && tar xzf 7.1.1-10.tar.gz

RUN groupadd --gid 1001 nonroot \
    && useradd --uid 1001 --gid 1001 -m nonroot
USER nonroot

FROM maven:3.9.11-eclipse-temurin-17@sha256:2490f24bcc21b108f850f7039049a9d7f37d6d10b46b0b77779f5d76c12fad64

RUN groupadd --gid 1001 nonroot \
    && useradd --uid 1001 --gid 1001 -m nonroot

WORKDIR /
COPY ./target/ms-fitnote-controller*.jar /ms-fitnote-controller.jar
COPY ./src/main/properties/tessdata /tessdata
COPY ./config.yml /config.yml
COPY --from=builder ./ImageMagick-7.1.1-10 ./ImageMagick-7.1.1-10
COPY --from=eyq18885.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

RUN apt-get update && apt-get install -y \
	autoconf=2.71-3 \
	pkgconf=1.8.1-2build1 \
	build-essential=12.10ubuntu1 \
	curl=8.5.0-2ubuntu10.6 \
	libcurl3-gnutls=8.5.0-2ubuntu10.6 \
	libpng-dev=1.6.43-5build1 \
	libde265-0=1.0.15-1build3 \
	libheif-dev=1.17.6-1ubuntu4.1 \
	libjpeg-dev=8c-2ubuntu11 \
	libtiff-dev=4.5.1+git230720-4ubuntu2.3 \
	libmagickcore-dev=8:6.9.12.98+dfsg1-5.2build2 \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

RUN sh ./ImageMagick-7.1.1-10/configure \
  --prefix=/usr/local \
  --with-bzlib=yes \
  --with-flif=yes \
  --with-fontconfig=yes \
  --with-magick-plus-plus=yes \
  --with-freetype=yes \
  --with-heic=yes \
  --with-gslib=yes \
  --with-gvc=yes \
  --with-jpeg=yes \
  --with-jp2=yes \
  --with-png=yes \
  --with-tiff=yes \
  --with-xml=yes \
  --with-gs-font-dir=yes \
  && make -j4 \
  && make install \
  && ldconfig /usr/local/lib/w

USER nonroot
EXPOSE 3004
ENTRYPOINT [ "java", "-jar", "/ms-fitnote-controller.jar", "server", "/config.yml" ]

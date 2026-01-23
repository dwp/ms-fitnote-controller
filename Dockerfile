ARG MAVEN_DEBIAN_IMAGE

FROM ${MAVEN_DEBIAN_IMAGE}

RUN groupadd --gid 1001 nonroot \
    && useradd --uid 1001 --gid 1001 -m nonroot

WORKDIR /
COPY ./target/ms-fitnote-controller*.jar /ms-fitnote-controller.jar
COPY ./src/main/properties/tessdata /tessdata
COPY ./config.yml /config.yml
COPY magick/ImageMagick ./ImageMagick
COPY --from=eyq18885.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

COPY ./properties/debian.sources /etc/apt/sources.list.d/debian.sources
RUN apt-get update && apt-get install -y \
	autoconf=2.71-3 \
	pkgconf=1.8.1-1 \
	build-essential=12.9 \
	curl=7.88.1-10+deb12u14 \
	libcurl3-gnutls=7.88.1-10+deb12u14 \
	libpng-dev=1.6.39-2+deb12u1 \
	libde265-0=1.0.11-1+deb12u2 \
	libheif-dev=1.15.1-1+deb12u1 \
	libjpeg-dev=1:2.1.5-2 \
	libtiff-dev=4.5.0-6+deb12u3 \
	libmagickcore-dev=8:6.9.11.60+dfsg-1.6+deb12u5 \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

RUN sh ./ImageMagick/configure \
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

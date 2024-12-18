FROM maven:3.9.6-eclipse-temurin-17 as builder
RUN wget --progress=dot:giga https://github.com/ImageMagick/ImageMagick/archive/refs/tags/7.1.1-10.tar.gz \
  && tar xzf 7.1.1-10.tar.gz

FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /
COPY ./target/ms-fitnote-controller*.jar /ms-fitnote-controller.jar
COPY ./src/main/properties/tessdata /tessdata
COPY ./config.yml /config.yml
COPY --from=builder ./ImageMagick-7.1.1-10 ./ImageMagick-7.1.1-10

RUN apt-get update && apt-get install -y \
	autoconf=2.71-2 \
	pkg-config=0.29.2-1ubuntu3 \
	build-essential=12.9ubuntu3 \
	curl=7.81.0-1ubuntu1.16 \
	libcurl3-gnutls=7.81.0-1ubuntu1.16 \
	locales=2.35-0ubuntu3.7 \
	libc-bin=2.35-0ubuntu3.7 \
	libpng-dev=1.6.37-3build5 \
	libde265-dev=1.0.8-1ubuntu0.3 \
	libheif-dev=1.12.0-2build1 \
	libjpeg-dev=8c-2ubuntu10 \
	libtiff-dev=4.3.0-6ubuntu0.10 \
	libmagickcore-dev=8:6.9.11.60+dfsg-1.3ubuntu0.22.04.5 \
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
  && make -j \
  && make install \
  && ldconfig /usr/local/lib/w

EXPOSE 3004
ENTRYPOINT [ "java", "-jar", "/ms-fitnote-controller.jar", "server", "/config.yml" ]

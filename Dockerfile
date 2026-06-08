ARG APP_IMAGE=nexus.nonprod.dwpcloud.uk/docker/dwp.gov.uk/jre:openjdk-21.0.10-dev@sha256:6ef345cbe9344f01a2fdcb327bc1a126fc9a1149caf7bda11ba3e16be34593b6
FROM ${APP_IMAGE} AS builder

USER root
ENV IMAGE_MAGICK_VERSION=7.1.1-10

# Install build dependencies
RUN apk update && apk add --no-cache \
    # Build tools
    build-base=1-r9 \
    libstdc++-dev=15.2.0-r11 \
    autoconf=2.73-r0 \
    automake=1.18.1-r5 \
    libtool=2.5.4-r3 \
    git=2.53.0-r2 \
    # Library development packages for ImageMagick features
    bzip2-dev=1.0.8-r23 \
    fontconfig-dev=2.17.1-r6 \
    freetype-dev=2.14.3-r2 \
    libheif-dev=1.21.2-r2 \
    ghostscript-dev=10.07.0-r1 \
    graphviz-dev=14.1.5-r0 \
    libjpeg-turbo-dev=3.1.4.1-r2 \
    openjpeg-dev=2.5.4-r2 \
    libpng-dev=1.6.58-r0 \
    tiff-dev=4.7.1-r4 \
    libxml2-dev=2.15.3-r0 \
    zlib-dev=1.3.2-r2 \
    lcms2-dev=2.18-r0 \
    libwebp-dev=1.6.0-r3 \
    fftw-dev=3.3.10-r7 \
    pango-dev=1.56.4-r4 \
    cairo-dev=1.18.4-r6 \
    && ln -sf libpng16.so.16 /usr/lib/libpng16.so

# Clone ImageMagick repository
WORKDIR /src
RUN git clone --depth 1 --branch ${IMAGE_MAGICK_VERSION} https://github.com/ImageMagick/ImageMagick.git

WORKDIR /src/ImageMagick

# Configure and build ImageMagick
RUN ./configure \
    --prefix=/usr/local \
    --enable-shared \
    --disable-static \
    --with-bzlib \
    --with-fontconfig \
    --with-magick-plus-plus \
    --with-freetype \
    --with-heic \
    --with-gslib \
    --with-gvc \
    --with-jpeg \
    --with-openjp2 \
    --with-png \
    --with-tiff \
    --with-xml \
    --with-gs-font-dir=/usr/share/fonts/ghostscript \
    --disable-docs \
    && make -j4 \
    && make install DESTDIR=/imagemagick-install

# Switch back to non root (required for docker lint)
USER nobody 
# Stage 2: Runtime image
FROM ${APP_IMAGE}

USER root

# Install only runtime dependencies (not -dev packages)
RUN apk update && apk add --no-cache \
    bzip2=1.0.8-r23 \
    fontconfig=2.17.1-r6 \
    freetype=2.14.3-r2 \
    libheif=1.21.2-r2 \
    ghostscript=10.07.0-r1 \
    graphviz=14.1.5-r0 \
    libjpeg-turbo=3.1.4.1-r2 \
    openjpeg=2.5.4-r2 \
    libpng=1.6.58-r0 \
    tiff=4.7.1-r5 \
    libxml2=2.15.3-r0 \
    zlib=1.3.2-r2 \
    lcms2=2.18-r0 \
    libwebp=1.6.0-r3 \
    fftw=3.3.10-r7 \
    pango=1.56.4-r4 \
    cairo=1.18.4-r6 \
    libgomp=15.2.0-r11 \
    libstdc++=15.2.0-r11 \
    openexr=3.4.11-r0 \
    ghostscript-fonts=8.11-r1

# Copy ImageMagick installation from builder
COPY --from=builder /imagemagick-install/usr/local /usr/local

# Update library cache
RUN ldconfig /usr/local/lib || true

# Set environment variables
ENV PATH="/usr/local/bin:$PATH"
ENV LD_LIBRARY_PATH="/usr/local/lib"
ENV MAGICK_HOME="/usr/local"

# Verify installation
RUN magick -version


COPY ./target/ms-fitnote-controller*.jar /app/ms-fitnote-controller.jar
COPY ./src/main/properties/tessdata /app/tessdata
COPY ./config.yml /app/config.yml
COPY --from=eyq18885.live.dynatrace.com/linux/oneagent-codemodules:java / /
ENV LD_PRELOAD /opt/dynatrace/oneagent/agent/lib64/liboneagentproc.so

USER nobody
EXPOSE 3004
ENTRYPOINT [ "java", "-jar", "/app/ms-fitnote-controller.jar", "server", "/app/config.yml" ]

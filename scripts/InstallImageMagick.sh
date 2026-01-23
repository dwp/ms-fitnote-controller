#!/bin/sh


export APT_STATE_LISTS="$APT_DIR"/lists && export APT_CACHE_ARCHIVES="$APT_DIR"/archives
printf "dir::state::lists    %s;\ndir::cache::archives    %s;\n" "${APT_STATE_LISTS}" "${APT_CACHE_ARCHIVES}" > /etc/apt/apt.conf
mkdir -p "${APT_STATE_LISTS}/partial" && mkdir -p "${APT_CACHE_ARCHIVES}/partial"


apt-get update -y && apt-get install -y \
  wget \
	autoconf \
	pkg-config \
	build-essential \
	libpng-dev \
	libde265-dev \
	libheif-dev \
	libjpeg-dev \
	libtiff-dev \
	libmagickcore-dev



if [ ! -d "magick" ]; then
  mkdir "magick"
fi

if [ ! -d magick/ImageMagick ]; then
  apt-get update && apt-get install -y git
  git clone --depth 1 --branch 7.1.1-10 https://github.com/ImageMagick/ImageMagick.git magick/ImageMagick
fi

# Allow override via env var; default to "magick/build"
MAGICK_BUILD_DIR="${MAGICK_BUILD_DIR:-magick/build}"

if [ ! -d "$MAGICK_BUILD_DIR" ]; then
  mkdir "$MAGICK_BUILD_DIR"
  (cd "$MAGICK_BUILD_DIR" && sh ../ImageMagick/configure \
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
    --with-gs-font-dir=yes)
fi

(cd "$MAGICK_BUILD_DIR" && make -j4 && make install && ldconfig /usr/local/lib/w)

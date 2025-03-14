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
  mkdir magick/ImageMagick
fi

if [ ! -f magick/7.1.1-10.tar.gz ]; then
  wget --progress=dot:giga https://github.com/ImageMagick/ImageMagick/archive/refs/tags/7.1.1-10.tar.gz -P magick
  tar xzf  magick/7.1.1-10.tar.gz -C magick/ImageMagick --strip-components=1
fi

if [ ! -d magick/build ]; then
  mkdir magick/build
  (cd magick/build && sh ../ImageMagick/configure \
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

(cd magick/build && make -j4 && make install && ldconfig /usr/local/lib/w)

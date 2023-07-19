#!/bin/sh

apt-get update

apt-get install -y \
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

rm -rf /var/lib/apt/lists/*
apt-get clean

wget --progress=dot:giga https://github.com/ImageMagick/ImageMagick/archive/refs/tags/7.1.1-10.tar.gz
tar xzf 7.1.1-10.tar.gz

sh ./ImageMagick-7.1.1-10/configure \
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
  --with-gs-font-dir=yes

make -j
make install
ldconfig /usr/local/lib/w

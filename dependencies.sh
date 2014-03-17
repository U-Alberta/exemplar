#!/bin/sh

set -e

mkdir -p lib
cd lib 
xargs -n 1 curl -O < ../dependencies.txt
unzip *.jar.zip
rm *.jar.zip

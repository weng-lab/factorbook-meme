#!/bin/bash

set -e

# cd to project root directory
cd "$(dirname "$(dirname "$0")")"

docker build --target base -t genomealmanac/motif-meme-base .

docker run --name motif-meme-base --rm -i -t -d \
    -v /tmp/motif-test:/tmp/motif-test \
    genomealmanac/motif-meme-base /bin/sh
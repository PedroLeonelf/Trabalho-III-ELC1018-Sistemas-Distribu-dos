#!/bin/bash
docker run \
    --rm -v "$PWD":/app \
    -w /app \
    -i -t \
    --network=bridge \
    openjdk:14 \
    ./compileAndRun.sh

#!/bin/bash

set -e
set -x

HERE=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )
cd "$HERE"

# Client

gcc -g client/client.c -o out/client -lSDL2

# Server

ANDROID_HOME=/usr/local/share/android-sdk/
MIN_SDK_VERSION=android-21
COMPILE_SDK_VERSION=30.0.3

javac server/lackhoa/akb/Server.java                               \
    -cp "out:$ANDROID_HOME/platforms/${MIN_SDK_VERSION}/android.jar" \
    -d out                                                           \
    -sourcepath server

pushd out
"$ANDROID_HOME/build-tools/${COMPILE_SDK_VERSION}/dx" --dex \
    --output classes.dex                                    \
    .
popd

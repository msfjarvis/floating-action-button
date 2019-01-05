#!/bin/bash

wget https://github.com/shyiko/ktlint/releases/download/0.29.0/ktlint
chmod +x ktlint
./ktlint "${TRAVIS_BUILD_DIR}"/library/src/main/**/*.kt "${TRAVIS_BUILD_DIR}"/sample/src/main/**/*.kt

#!/bin/bash
mkdir -p dist/web
cp resources/public/index.html dist/web
mkdir -p dist/web/css/
cp resources/public/css/* dist/web/css
lein cljsbuild once min

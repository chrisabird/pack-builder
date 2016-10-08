#!/bin/bash
mkdir -p app/js/compiled/
cp resources/public/index.html app
mkdir -p app/css/
cp resources/public/css/* app/css/
lein cljsbuild once app
cd app
npm install
npm install electron-packager -g
electron-packager . --platform=linux --arch=x64 --out ../dist
electron-packager . --platform=win32 --arch=x64 --out ../dist

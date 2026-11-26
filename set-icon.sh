#!/usr/bin/env bash
# Use your own app icon:   bash set-icon.sh my-icon.png     (square PNG, 512x512 or bigger)
# Needs ImageMagick (Ubuntu: apt install imagemagick   Termux: pkg install imagemagick)
set -e
[ -f "$1" ] || { echo "Usage: bash set-icon.sh icon.png"; exit 1; }
R="$(dirname "$0")/app/src/main/res"
for d in "mdpi 48" "hdpi 72" "xhdpi 96" "xxhdpi 144" "xxxhdpi 192"; do
  set -- "$1" $d
  convert "$1" -resize "${3}x${3}" "$R/mipmap-$2/ic_launcher.png"
done
echo "Icon replaced. Now run build-vps.sh / build-termux.sh again."

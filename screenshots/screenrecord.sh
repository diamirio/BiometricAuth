#!/bin/sh

echo "Doing some stuff for GIF recording"

adb shell screenrecord --time-limit 11 /sdcard/example.mp4 &

echo "Started screenrecord"

sleep 1

echo "Tap"
adb shell input tap 550 1020

sleep 2

echo "Touch wrong finger"
adb emu finger touch 2

sleep 4

echo "Touch right finger"
adb emu finger touch 1

echo "Waiting to pull file"
sleep 8

echo "Pulling file"
adb pull /sdcard/example.mp4 ./screenrecord.mp4

echo "File saved as ./screenrecord.mp4"
echo "Convert file to GIF using e.g. https://ezgif.com/video-to-gif"
echo "-> Upload"
echo "-> Convert to GIF (with defult values)"
echo "-> Resize to width=300 (original should have been 600px)"
echo "-> Right-Click on smaller GIF -> save image as"

#!/bin/bash
cd /home/pi/jodelBot/
rm log.txt nohup.txt
nohup java -jar JodelBot.jar &

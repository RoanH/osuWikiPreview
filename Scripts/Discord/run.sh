#!/bin/bash
while true; do
  java -jar osuWikiPreview.jar
  exitcode=$?
  
  update=0
  if [ $exitcode -eq 0 ];
  then 
    exit 0
  elif [ $exitcode -eq 10 ];
  then 
    update=1
  elif [ $exitcode -eq 30 ];
  then
    echo "Restarting..."
  else
    echo "A fatal error occurred! Restarting..."
  fi
  
  if [ $update -eq 1 ];
  then
    echo "Updating..."
    rm osuWikiPreview.jar
    unzip -o update.zip
    rm update.zip
    echo "Done updating!"
  fi
done

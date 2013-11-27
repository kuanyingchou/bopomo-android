#!/bin/bash

start_reading=false

while read line; do
  # echo this is $line
  if [ "$line" = "%chardef  begin" ]; then
    start_reading=true
  elif [ "$line" = "%chardef  end" ]; then
    start_reading=false
  else 
    if $start_reading; then
      word=`echo $line | cut -d' ' -f2`
      # echo "word = $word"
      count=`wget --user-agent Mozilla --output-document=- www.google.com/search?q=$word 2> /dev/null | sed -e 's/.*About \([0-9,]\+\) results/\1\n/g' | head -1 | sed -e 's/,//g'`
      echo "$word $count" | tee -a output 
    fi
  fi
  sleep 1
done < assets/phone.cin




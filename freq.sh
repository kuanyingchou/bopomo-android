#!/bin/bash

input=phone.cin.chars.cconv.uniq
output=freq.out

if [ -f $output ]; then
  rm $output
fi

function get_count() {
  term=$1
  count=`curl -A "Mozilla/4.0" 2> /dev/null "http://www.google.com/search?q=%22$term%22" | grep -o 'About [0-9,]* results' | cut -d' ' -f2 | tr -d ','`
  echo $count
}

while read word; do
  count=`get_count $word`
  echo $word $count | tee -a $output
  sleep 1
done < $input


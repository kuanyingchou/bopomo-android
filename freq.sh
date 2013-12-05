#!/bin/bash

# Google <term> and get frequency from "About ###,### results"

# How to convert from Simplified Chinese to Traditional Chinese?
#   Use cconv:
#   $ cconv -f UTF8-CN -t UTF8-HK jian.txt -o fan.txt

input=phone.cin.chars.cconv.uniq
output=freq.out

if [ -f $output ]; then
  rm $output
fi

function get_count() {
  term=$1
  count=`curl -A "Mozilla/4.0" 2> /dev/null "http://www.google.com/search?q=%22$term%22" | grep -o 'About [0-9,]* results' | cut -d' ' -f2 | tr -d ','`

  ## or use wget:
  # wget \
  # --user-agent Mozilla \  # so that Google thinks we are Mozilla
  # --output-document=-  \  # print to stdout
  # www.google.com/search?q=<term> 

  echo $count
}

function wait() {
  sec=$1
  while (( $sec > 0 )) ; do
    let sec=sec-1
    printf "\r$sec second(s) remaining..."
    sleep 1
  done
  echo ""
}

while read word; do
  echo "looking for '$word'..."
  count=`get_count $word`
  while [ -z $count ]; do
    echo "failed."
    wait 30
    count=`get_count $word`
  done
  echo $word $count | tee -a $output
  sleep 3
done < $input


# wen-input - a simple bopomo input service for Android.  

## Useful adb commands

    adb shell ime list -a

## Word frequency

- convert Simplified Chinese to Traditional Chinese:
	cconv -f UTF8-CN -t UTF8-HK jian.txt -o fan.txt

- get word/term frequency from Google:
  
  ```bash
  wget 
  --user-agent Mozilla   # so that Google thinks we are Mozilla
  --output-document=-    # print to stdout
  www.google.com/search?q=<term> | 
  ```  
  
  - find "About ###,### results" from the result
    
  sed -e 's/.*About \([0-9,]\+\) results/\1\n/g' |   
  head -1
    
  replace <term> with the actual word/term 

## Todo
- phrase suggestions
- better word suggestions when given incomplete keys
- word/phrase frequency from Google
- key component replacement:  我(ji3) + (p4) -> 問(jp4)
- enter Simplified Chinese 
- enter English 
- English word/phrase suggestions
- type multiple words at once
 
## Done
- find a better project name
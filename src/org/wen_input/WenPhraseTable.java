package org.wen_input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WenPhraseTable {
    
    private final Map<String, List<WenPhrase>> table = 
            new HashMap<String, List<WenPhrase>>();
    
    public WenPhraseTable(InputStream is) {
        parse(is);
    }
    
    private void parse(InputStream is) {
        final String data = WenUtil.readTextFile(is);
        parse(data);
    }
    
    private void parse(String data) {
        System.out.println("start parsing...");
        
        final BufferedReader reader = new BufferedReader(
                new StringReader(data));
        try { 
            String line = reader.readLine();
            int count=0;
            while(line != null && count < 1000) {
                final String[] values = line.split("\\s+");
                if(values.length >= 3) {
                    int freq = 0; 
                    try {
                        freq = Integer.valueOf(values[1]);
                    } catch(NumberFormatException e) {
                        e.printStackTrace();
                    }
                    final WenPhrase phrase = new WenPhrase(
                        values[0], 
                        freq, 
                        Arrays.copyOfRange(values, 2, values.length));
    
                    if(phrase.value.length() <= 2) {
System.out.println(phrase);
                        final String key = String.valueOf(values[0].charAt(0));
                        if(table.containsKey(key)) {
                            final List<WenPhrase> phrases = table.get(key);
                            if(! phrases.contains(phrase)) {
                                phrases.add(phrase);
                                Collections.sort(phrases);
                            }
                        } else {
                            final List<WenPhrase> list = new ArrayList<WenPhrase>();
                            list.add(phrase);
                            table.put(key, list);
                        }
                        count++;
                    }
                }
                line = reader.readLine();
            }
            
        } catch(IOException e) {
            e.printStackTrace();
        }
        System.out.println("done");
    }
    
    public List<WenPhrase> get(String key) {
        if(table.containsKey(key)) {
            return table.get(key);
        } else {
            return new ArrayList<WenPhrase>();
        }
    }
}

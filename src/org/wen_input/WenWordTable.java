package org.wen_input;

import java.io.*;
import java.util.*;


public class WenWordTable {
    private final Map<String, List<WenWord>> words =
            new HashMap<String, List<WenWord>>();
    private final Map<String, List<WenWord>> possibleWords =
            new HashMap<String, List<WenWord>>();
    private final Map<String, String> keys =
            new HashMap<String, String>();
    private Map<String, Long> freqTable;

    
    public WenWordTable(String path) {
        parse(path, null);
    }
    
    public WenWordTable(InputStream is) {
        this(is, null);
    }
    
    public WenWordTable(InputStream is, InputStream freq) {
        parse(is, freq);
    }

    private void parse(InputStream table, InputStream freq) {
        final String t = WenUtil.readTextFile(table);
        final String f = (freq == null)? null: WenUtil.readTextFile(freq);
        parse(t, f);
    }
    
    private enum ParsingState { START, KEYNAME, CHARDEF }

    private void buildFreqTable(String data) {
        freqTable = new HashMap<String, Long>();
        final BufferedReader reader = new BufferedReader(
                new StringReader(data));
        try { 
            String line = reader.readLine();
            ParsingState state = ParsingState.START;
            while(line != null) {
                final String[] com = line.split("\\s+");
                freqTable.put(com[0], Long.valueOf(com[1]));
                line = reader.readLine();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private long getFrequency(String word) {
        if(freqTable != null && freqTable.containsKey(word)) {
            return freqTable.get(word);
        } else {
            return 0;
        }
    }
    
    private void parse(String data, String freq) {
        System.out.println("start parsing...");

        if(freq != null) {
            buildFreqTable(freq);
        }
        
        final BufferedReader reader = new BufferedReader(
                new StringReader(data));
        try { 
            String line = reader.readLine();
            ParsingState state = ParsingState.START;
            while(line != null) {
                switch(state) {
                case START:
                    if(line.startsWith("%chardef  begin")) {
                        state = ParsingState.CHARDEF;
                    } else if(line.startsWith("%keyname  begin")) {
                        state = ParsingState.KEYNAME;
                    }
                    break;
                case KEYNAME:
                    if(line.startsWith("%keyname  end")) {
                        state = ParsingState.START;
                        break;
                    }
                    final String[] k = line.trim().split("\\s+");
                    keys.put(k[0], k[1]);
                    System.out.println(k[0] + " -> \"" + k[1]+"\"");
                    break;
                case CHARDEF:
                    if(line.startsWith("%chardef  end")) {
                        state = ParsingState.START;
                        break;
                    }
                    final String trimmedLine = line.trim();
                    final String[] kv = trimmedLine.split(" ");
                    if(words.containsKey(kv[0])) {
                        words.get(kv[0]).add(new WenWord(kv[1], 
                                getFrequency(kv[1])));
                    } else {
                        final List<WenWord> a = new ArrayList<WenWord>();
                        a.add(new WenWord(kv[1], 
                                getFrequency(kv[1])));
                        words.put(kv[0], a);
                    }

                    //[ possible words
                    if(kv[0].length() > 1) {
                        for(int i=1; i<kv[0].length(); i++) {
                            final String pk = kv[0].substring(0, i);
                            if(possibleWords.containsKey(pk)) {
                                final List<WenWord> list = possibleWords.get(pk);
                                list.add(new WenWord(kv[1], 
                                        getFrequency(kv[1])));
                                //Collections.sort(list, 
                                //        WenWord.frequencyComparator);
                            } else {
                                final List<WenWord> a = new ArrayList<WenWord>();
                                a.add(new WenWord(kv[1], 
                                        getFrequency(kv[1])));
                                possibleWords.put(pk, a);
                            }
                        }
                    }
                    //System.out.println("add "+kv[1] + " to " +kv[0]);                    
                    break;
                }
                line = reader.readLine();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        System.out.println("done");
    }

    public List<WenWord> get(String keyName) {

        final String key = getKeyCode(keyName);
        if(key == null) return new ArrayList<WenWord>();

        System.out.println("get words for "+keyName+" : "+key);        
        
        if(words.containsKey(key)) {
            return words.get(key);
        } else {
            return new ArrayList<WenWord>();
            //throw new RuntimeException("key not found:  "+ key);
        }
    }

    public List<WenWord> getPossible(String keyName, int limit) {
        final String key = getKeyCode(keyName);
        if(key == null) return new ArrayList<WenWord>();
        if(possibleWords.containsKey(key)) {
            final List<WenWord> res = possibleWords.get(key);
            if(limit > res.size()) return sort(res);
            else return sort(res.subList(0, limit));
        } else {
            return new ArrayList<WenWord>();
            //throw new RuntimeException("key not found:  "+ key);
        }
    }
    
    private List<WenWord> sort(List<WenWord> list) {
        Collections.sort(list, WenWord.frequencyComparator);
        return list;
    }

    public String getKeyName(String key) {
        if(keys.containsKey(key)) {
            return keys.get(key);
        } else {
            return ""; //>>>
        }
    }
    public String getKeyCode(String keyName) {
        final StringBuilder sb = new StringBuilder();
        for(int i=0; i<keyName.length(); i++) {
            for(String k : keys.keySet()) {
                if(keys.get(k).equals(String.valueOf(keyName.charAt(i)))) {
                    sb.append(k);
                }
            }
        }
        return sb.toString();
    }

    //===========================================
    public static void testBasic() {
        final WenWordTable t = new WenWordTable("./assets/phone.cin");

        System.out.println(t.get("ji3").get(0));  
        System.out.println(t.get("g4").get(0)); 
        System.out.println(t.get("5.").get(0));   
        System.out.println(t.get("ej04").get(0)); 
        System.out.println(t.get("u/3").get(0));  
    }
    public static void testArgs(String[] keys) {
        final WenWordTable t = new WenWordTable("./assets/phone.cin");
        for(int i=0; i<keys.length; i++) {
            //System.out.println(join(t.get(keys[i]), ", "));
            //System.out.print(t.get(keys[i]).get(0));
            System.out.println(WenUtil.join(WenUtil.subList(t.get(keys[i]), 10), ", "));
        }
        System.out.println();
    }
    public static void testInterpreter() throws IOException {
        final WenWordTable table = new WenWordTable("./assets/phone.cin");
        final BufferedReader br = new BufferedReader(
                new InputStreamReader(System.in));
        while(true) {
            final String input = br.readLine();
            System.out.println(table.get(input.trim()));
        }
    }
    public static void main(String[] args) throws IOException {
        //testBasic();
        //testArgs(args);
        testInterpreter();
    }
}



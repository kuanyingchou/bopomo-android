import java.io.*;
import java.util.*;

public class WordTable {
    private final Hashtable<String, List<Word>> words =
            new Hashtable<String, List<Word>>();

    public WordTable(String path) {
        final String data = readTextFile(path);
        final BufferedReader reader = new BufferedReader(
                new StringReader(data));
        try { 
            String line = reader.readLine();
            boolean startCharParsing = false;
            while(line != null) {
                if(line.startsWith("%chardef")) {
                    startCharParsing = true;
                } else {
                    if(startCharParsing) {
                        final String trimmedLine = line.trim();
                        final String[] kv = trimmedLine.split(" ");
                        if(words.containsKey(kv[0])) {
                            words.get(kv[0]).add(new Word(kv[1]));
                        } else {
                            final List<Word> a = new ArrayList<Word>();
                            a.add(new Word(kv[1]));
                            words.put(kv[0], a);
                        }
                        //System.out.println("add "+kv[1] + " to " +kv[0]);
                    }
                }
                line = reader.readLine();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        System.out.println("done parsing");
    }
    public List<Word> get(String key) {
        if(words.containsKey(key)) {
            return words.get(key);
        } else {
            return new ArrayList<Word>();
            //throw new RuntimeException("key not found:  "+ key);
        }
    }
    public static void fixedTest() {
        final WordTable t = new WordTable("./phone.cin");

        System.out.println(t.get("ji3").get(0));  
        System.out.println(t.get("g4").get(0)); 
        System.out.println(t.get("5.").get(0));   
        System.out.println(t.get("ej04").get(0)); 
        System.out.println(t.get("u/3").get(0));  
    }
    public static void dynamicTest(String[] keys) {
        final WordTable t = new WordTable("./phone.cin");
        for(int i=0; i<keys.length; i++) {
            //System.out.println(join(t.get(keys[i]), ", "));
            //System.out.print(t.get(keys[i]).get(0));
            System.out.println(join(subList(t.get(keys[i]), 10), ", "));
        }
        System.out.println();
    }
    public static void main(String[] args) {
        //fixedTest();
        dynamicTest(args);
    }

    public static <T> List<T> subList(List<T> target, int size) {
        if(target.size() <= size) {
            return new ArrayList<T>(target);
        } else {
            return target.subList(0, size);
        }
    }

    public static String join(List<?> objs, String del) {
        return join(objs.toArray(), del);
    }
    public static <T> String join(T[] objs, String del) {
        StringBuilder sb = new StringBuilder();
        if(objs.length > 1) {
            sb.append(objs[0].toString());
        }
        for(int i=1; i<objs.length; i++) {
            sb.append(del);
            sb.append(objs[i]);
        }
        return sb.toString();
    }

    public static String readTextFile(String path) {
        String everything="";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(path));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append('\n');
                line = br.readLine();
            }
            everything = sb.toString();
            br.close();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
        }
        return everything;
    }
}

class Word {
    private final char value;

    Word(char c) {
        value = c;
    }
    Word(String s) {
        value = s.charAt(0);
    }
    public String toString() {
        return String.valueOf(value);
    }
}

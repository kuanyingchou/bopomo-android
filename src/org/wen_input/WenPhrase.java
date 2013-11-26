package org.wen_input;

public class WenPhrase implements Comparable<WenPhrase> {
    final String value;
    final int frequency;
    final String[] key;
    
    public WenPhrase(String v, int f, String[] k) {
        value = v;
        frequency = f;
        key = k;
    }

    @Override
    public int compareTo(WenPhrase another) {
        return another.frequency - frequency;
    }
    
    @Override
    public String toString() {
        return value+" ("+frequency+") "+WenUtil.join(key, ", ", "( ", ")");
    }
}

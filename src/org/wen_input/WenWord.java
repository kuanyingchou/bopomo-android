package org.wen_input;

import java.util.Comparator;

public class WenWord {
    private final char value;
    private final long freq;
    public static final Comparator<WenWord> frequencyComparator =
            getFrequencyComparator();

    WenWord(char c) {
        value = c;
        freq = 0;
    }
    WenWord(String s, long f) {
        value = s.charAt(0);
        freq = f;
    }
    public String toString() {
        return String.valueOf(value);
    }

    public static Comparator<WenWord> getFrequencyComparator() {
        return new Comparator<WenWord>() {
            public int compare(WenWord a, WenWord b) {
                final long cmp = b.freq - a.freq;
                if(cmp > 0) return 1;
                else if(cmp < 0) return -1;
                else return 0;
            }
        };
    }
}

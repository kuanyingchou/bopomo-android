package org.wen_input;

public class WenWord {
    private final char value;

    WenWord(char c) {
        value = c;
    }
    WenWord(String s) {
        value = s.charAt(0);
    }
    public String toString() {
        return String.valueOf(value);
    }
}

package com.android.inputmethodcommon;

public class Word {
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
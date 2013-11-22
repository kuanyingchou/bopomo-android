package kuanyingchou.bopomo_android;

public class BoWord {
    private final char value;

    BoWord(char c) {
        value = c;
    }
    BoWord(String s) {
        value = s.charAt(0);
    }
    public String toString() {
        return String.valueOf(value);
    }
}

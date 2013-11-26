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
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        WenPhrase other = (WenPhrase) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        //return value+" ("+frequency+") "+WenUtil.join(key, ", ", "( ", ")");
        return value;
    }
}

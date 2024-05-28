package or.lotus.sensitive;

public class SensitiveWord implements Comparable<SensitiveWord> {
    public char c;
    public SensitiveWordList next = null;

    public SensitiveWord(char c) {
        this.c = c;
    }

    @Override
    public int compareTo(SensitiveWord sensitiveWord) {
        return c - sensitiveWord.c;
    }

    public String toString() {
        return c + "(" + (next == null ? null : next.size()) + ")";
    }
}

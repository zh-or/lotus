package or.lotus.sensitive;

import java.util.ArrayList;

public class WordFilterResult {
    public ArrayList<String> replaces;
    public String result;
    private StringBuilder current;

    public boolean hasSensitive() {
        return replaces.size() > 0;
    }

    public WordFilterResult() {
        this.replaces = new ArrayList<>();
        current = new StringBuilder();
    }

    public void append(char c) {
        current.append(c);
    }
    public WordFilterResult endCurrent() {
        if (current.length() > 0) {
            replaces.add(current.toString());
            current.setLength(0);
        }
        return this;
    }
    public WordFilterResult setResult(String result) {
        endCurrent();
        this.result = result;
        return this;
    }
    @Override
    public String toString() {
        return result;
    }
}

package or.lotus.core.sensitive;

import java.util.ArrayList;

public class SensitiveWordList extends ArrayList<SensitiveWord> {

    public SensitiveWord get(char c) {
        for (SensitiveWord w : this) {
            if (w.c == c) {
                return w;
            }
        }
        return null;
    }

    /**
     * 二分查找，必须先升序排序
     *
     * @param c 需要查找的字符
     * @return Word对象：如果找到   null:如果没找到
     */
    public SensitiveWord binaryGet(char c) {
        int left, right, key;
        SensitiveWord word;
        left = 0;
        right = this.size() - 1;
        while (left <= right) {
            key = (left + right) / 2;
            word = get(key);
            if (word.c == c) {
                return word;
            } else if (word.c > c) {
                right = key - 1;
            } else {
                left = key + 1;
            }
        }
        return null;
    }

    public SensitiveWord add(char c) {
        SensitiveWord word = new SensitiveWord(c);
        super.add(word);
        return word;
    }
}

package or.lotus.sensitive;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class SensitiveWordFilter {
    private SensitiveWordList wordList;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock rlock = rwLock.readLock();
    private final Lock wlock = rwLock.writeLock();
    private int wordCount = 0;
    private final static char replace = '*'; // 替代字符

    private final static char[] skip = new char[] { // 遇到这些字符就会跳过，例如,如果"AB"是敏感词，那么"A B","A=B"也会被屏蔽
            '!', '*', '-', '+', '_', '=', ',', '.', '@'
    };

    /**
     * 加载敏感词txt文件，每个敏感词独占一行，不可出现空格，空行，逗号等非文字内容,必须使用UTF-8编码
     * 有读写锁, 可运行时重新加载敏感词
     */
    public void load(File file) throws IOException {
        List<String> list = Files.readAllLines(file.toPath(), Charset.forName("utf-8"));
        Set<String> set = new HashSet<>();
        List<String> nList = new ArrayList<>(list.size());
        for(String item : list) {
            if(set.contains(item)) {
                System.out.println("敏感词重复：" + item);
            } else {
                set.add(item);
                nList.add(item);
            }
        }

        load(nList);
    }

    /**
     * 加载敏感词列表
     * 有读写锁, 可运行时重新加载敏感词
     */
    public void load(List<String> words) {
        if (words == null) {
            return;
        }
        wlock.lock();
        wordCount = words.size();
        try {
            char[] chars;
            SensitiveWordList now;
            SensitiveWord word;
            wordList = new SensitiveWordList();
            for (String line : words) {
                if (line == null || line.length() == 0) {
                    continue;
                }
                chars = line.toCharArray();
                now = wordList;
                word = null;
                for (char c : chars) {
                    if (word != null) {
                        if (word.next == null) {
                            word.next = new SensitiveWordList();
                        }
                        now = word.next;
                    }
                    word = now.get(c);
                    if (word == null) {
                        word = now.add(c);
                    }
                }
            }
            sort(wordList);
        } finally {
            wlock.unlock();
        }
    }

    public WordFilterResult filter(String text) {
        return this.filter(text, replace);
    }

    /**
     * 敏感词替换
     *
     * @param text 待替换文本
     * @return 替换后的文本
     */
    public WordFilterResult filter(String text, char rep) {
        WordFilterResult res = new WordFilterResult();
        rlock.lock();
        try {
            if (wordList == null || wordList.size() == 0) {
                return res.setResult(text);
            }
            char[] chars = text.toCharArray(); // 把String转化成char数组，便于遍历
            int i, j;
            SensitiveWord word;
            boolean flag; // 是否需要替换
            for (i = 0; i < chars.length; i++) { // 遍历所有字符
                char c = chars[i];
                word = wordList.binaryGet(c); // 使用二分查找来寻找字符，提高效率
                if (word != null) { // word != null说明找到了
                    flag = false;
                    j = i + 1;
                    while (j < chars.length) { // 开始逐个比较后面的字符
                        if (skip(chars[j])) { // 跳过空格之类的无关字符
                            j++;
                            continue;
                        }
                        if (word.next != null) { // 字符串尚未结束，不确定是否存在敏感词
                        /*
                        以下代码并没有使用二分查找，因为以同一个字符开头的敏感词较少
                        例如，wordList中记录了所有敏感词的开头第一个字，它的数量通常会有上千个
                        假如现在锁定了字符“T”开头的敏感词，而“T”开头的敏感词只有10个，这时使用二分查找的效率反而低于顺序查找
                         */
                            word = word.next.get(chars[j]);
                            if (word == null) {
                                break;
                            }
                            j++;
                        } else { // 字符串已结束，存在敏感词汇
                            flag = true;
                            break;
                        }
                    }
                    if (word != null && word.next == null) {
                        flag = true;
                    }
                    if (flag) { // 如果flag==true，说明检测出敏感粗，需要替换
                        while (i < j) {
                            if (skip(chars[i])) { // 跳过空格之类的无关字符，如果要把空格也替换成'*'，则删除这个if语句
                                i++;
                                continue;
                            }
                            res.append(chars[i]);
                            chars[i] = rep;
                            i++;
                        }
                        res.endCurrent();
                        i--;
                    }
                }
            }
            return res.setResult(new String(chars));
        } finally {
            rlock.unlock();
        }
    }

    public int getWordCount() {
        return wordCount;
    }

    /**
     * 对敏感词多叉树递增排序
     * @param list 待排序List
     */
    private void sort(SensitiveWordList list) {
        if (list == null) {
            return;
        }
        Collections.sort(list); // 递增排序
        for (SensitiveWord word : list) {
            sort(word.next);
        }
    }

    /**
     * 判断是否跳过当前字符
     * @param c 待检测字符
     * @return true:需要跳过   false:不需要跳过
     */
    private boolean skip(char c) {
        for (char c1 : skip) {
            if (c1 == c) {
                return true;
            }
        }
        return false;
    }
}

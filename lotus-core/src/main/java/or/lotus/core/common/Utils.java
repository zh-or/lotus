package or.lotus.core.common;

import or.lotus.core.files.FileSize;
import org.thymeleaf.util.StringUtils;

import java.io.Closeable;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class Utils {

    public static final String EN_TYPE_MD5 = "md5";
    public static final String EN_TYPE_SHA1 = "sha1";
    public static final int S_BOX_MAX_SIZE = 255;

    private Utils() {
    }

    public static void main(String[] args) throws Exception {

        int t = ceilDiv(187, 20);

        char[] key = rc4Init("123456");
        byte[] d1 = rc4Crypt("哈哈lawd1123".getBytes("utf-8"), key.clone());

        byte[] d2 = rc4Crypt(d1, key.clone());
        System.out.println(new String(d2, "utf-8"));

        System.out.println(EnCode("123456", EN_TYPE_MD5));
        System.out.println(EnCode("123456打完", EN_TYPE_MD5));
        System.out.println(EnCode("awjkdo1230awsdikoaw", EN_TYPE_MD5));

        System.out.println("-----------");
        System.out.println(substring("1天, 价格: 1, 购买天数: 1, 当前到期时间: 2025-05-30 15:09:33, deviceId:  4860, iccid: 8986062463009035420, userId: 1 , goodsId: 3", 120));
        System.out.println(substring("a空留一个开发了哇", 3));

    }


    public static Map<String, Object> systemInfo() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> map = new HashMap<>();
        map.put("已使用内存", new FileSize(runtime.totalMemory()).toString());
        map.put("空闲内存", new FileSize(runtime.freeMemory()).toString());
        map.put("最大内存", new FileSize(runtime.maxMemory()).toString());

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        map.put("线程总数", threadMXBean.getThreadCount());
        map.put("峰值线程数", threadMXBean.getPeakThreadCount());
        map.put("守护线程数", threadMXBean.getDaemonThreadCount());
        map.put("已启动线程总数", threadMXBean.getTotalStartedThreadCount());

        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {

            map.put("GC-" + gc.getName(), "次数: " + gc.getCollectionCount() + ",时间: " + gc.getCollectionTime() + " ms");
        }

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        map.put("系统负载", osBean.getSystemLoadAverage());

        return map;
    }

    public static String getSysTmpDir() {
        return System.getProperty("java.io.tmpdir") + File.separator;
    }

    /**
     * 100TB, 100GB, 100MB, 100KB, 如果没有带单位默认为kb
     */
    public static long formatSize(String sizeStr) {
        int[] n = Utils.getNumberFromStr(sizeStr, false);
        if (n.length <= 0) {
            return 0;
        }
        long size = n[0];
        if (sizeStr.endsWith("TB")) {
            size = size * 1024 * 1024 * 1024 * 1024;
        } else if (sizeStr.endsWith("GB")) {
            size = size * 1024 * 1024 * 1024;
        } else if (sizeStr.endsWith("MB")) {
            size = size * 1024 * 1024;
        } else {//kb
            size = size * 1024;
        }

        return size;
    }

    public static void assets(String v, String msg) {
        if (Utils.CheckNull(v)) {
            throw new IllegalArgumentException(msg);
        }
    }

    public static void assets(boolean v, String msg) {
        if (v) {
            throw new IllegalArgumentException(msg);
        }
    }


    public static BigDecimal calc(Object v1, String m, Object v2) {
        switch (m) {
            case "+":
                return new BigDecimal(v1.toString()).add(new BigDecimal(v2.toString()));
            case "-":
                return new BigDecimal(v1.toString()).subtract(new BigDecimal(v2.toString()));
            case "*":
                return new BigDecimal(v1.toString()).multiply(new BigDecimal(v2.toString()));
            case "/":
                return new BigDecimal(v1.toString()).divide(new BigDecimal(v2.toString()));
        }
        return new BigDecimal(0);
    }

    /**
     * 加密
     *
     * @param data
     * @param key
     * @return
     */
    public static byte[] rc4Encoded(byte[] data, String key) {
        char[] s_box = rc4Init(key);
        return rc4Crypt(data, s_box);
    }

    /**
     * rc4 加密解密
     *
     * @param data
     * @param key
     * @return
     */
    public static byte[] rc4Decode(byte[] data, String key) {
        char[] s_box = rc4Init(key);
        return rc4Crypt(data, s_box);
    }

    /**
     * 分开的步骤-1
     */
    public static char[] rc4Init(String key) {
        char[] rs = new char[S_BOX_MAX_SIZE];
        char[] key_ = key.toCharArray();
        int[] k = new int[S_BOX_MAX_SIZE];
        int i = 0, j = 0, len = key_.length;
        char temp;
        for (; i < S_BOX_MAX_SIZE; i++) {
            rs[i] = (char) i;
            k[i] = key_[i % len];
        }
        j = 0;
        for (i = 0; i < S_BOX_MAX_SIZE; i++) {
            j = (j + rs[i] + k[i]) % len;
            temp = rs[i];
            rs[i] = rs[j];
            rs[j] = temp;
        }
        return rs;
    }

    /**
     * 分开的步骤-2
     */
    public static byte[] rc4Crypt(byte[] data, char[] s_box) {
        int x = 0, y = 0, t = 0, i = 0, len = data.length;
        char tmp;
        int sBoxLen = s_box.length;
        char[] s_box_clone = new char[sBoxLen];

        System.arraycopy(s_box, 0, s_box_clone, 0, sBoxLen);

        for (i = 0; i < len; i++) {
            x = (x + 1) % S_BOX_MAX_SIZE;
            y = (y + s_box_clone[x]) % S_BOX_MAX_SIZE;
            tmp = s_box_clone[x];
            s_box_clone[x] = s_box_clone[y];
            s_box_clone[y] = tmp;
            t = (s_box_clone[x] + s_box_clone[y]) % S_BOX_MAX_SIZE;
            data[i] ^= s_box_clone[t];
        }
        return data;
    }


    /**
     * 按字节长度截取字符串, 默认字符串编码为utf-8
     */
    public static String substring(String s, int maxBytes) throws UnsupportedEncodingException {
        return substring(s, maxBytes, "utf-8");
    }

    /**
     * 按字节长度截取字符串
     */
    public static String substring(String s, int maxBytes, String charset) throws UnsupportedEncodingException {
        if (CheckNull(s)) {
            return s;
        }

        String s2 = s.length() > maxBytes ? s.substring(0, maxBytes) : s;
        int len = s2.getBytes(charset).length;

        while (len > maxBytes) {
            s2 = s2.substring(0, s2.length() - 1);
            len = s2.getBytes(charset).length;
        }
        return s2;
    }

    public static String RandomString(int charTotal) {
        return new String(RandomChars(charTotal));
    }

    /*
     * 获取随机字符
     */
    public static char[] RandomChars(int countNumber) {
        char[] c = new char[countNumber];
        int i = 0;
        while (i < countNumber) {
            char t = (char) (65 + Math.random() * (122 - 65 + 1));
            if (t < 91 || t > 96) {// 去掉大小写中间的几个符号
                c[i] = t;
                i++;
            }
        }
        return c;
    }

    /**
     * 获取随机数
     */
    public static int RandomNum(int start, int end) {
        return ThreadLocalRandom.current().nextInt(start, end + 1);
    }

    public static String RandomNum(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RandomNum(0, 9));
        }
        return sb.toString();
    }

    public static byte[] GetRepeatByte(int len, byte b) {
        byte[] res = new byte[len];
        for (int i = 0; i < len; i++) {
            res[i] = b;
        }
        return res;
    }

    public static int byte2short(byte[] b) {
        return byte2short(b, 0);
    }

    /**
     * 2byte to int
     */
    public static int byte2short(byte[] b, int offset) {
        if (b == null || b.length < 2)
            return 0;
        return ((b[1 + offset] & 0xff) << 8) | (b[0 + offset] & 0xff);
    }

    /**
     * int to 2byte
     */
    public static byte[] short2byte(int i) {
        byte[] result = new byte[2];
        result[0] = (byte) i;
        result[1] = (byte) (i >>> 8);
        return result;
    }

    public static byte[] int2byte(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) (i & 0xff);
        result[1] = (byte) ((i >>> 8) & 0xff);
        result[2] = (byte) ((i >>> 16) & 0xff);
        result[3] = (byte) ((i >>> 24) & 0xff);
        return result;
    }

    public static int byte2int(byte[] b) {
        return byte2int(b, 0);
    }

    public static int byte2int(byte[] b, int offset) {
        if (b == null || b.length < 4)
            return 0;
        int bound = offset;
        int i = 0;
        i = b[bound] & 0xff;
        bound++;
        i |= b[bound] << 8 & 0xff00;
        bound++;
        i |= b[bound] << 16 & 0xff0000;
        bound++;
        i |= b[bound] << 24 & 0xff000000;
        return i;
    }

    public static long byte2long(byte[] b) {
        if (b == null || b.length != 8)
            return 0;
        long i = 0l;
        i = b[0] & 0xff;
        i |= b[1] << 8 & 0xff00l;
        i |= b[2] << 16 & 0xff0000l;
        i |= b[3] << 24 & 0xff000000l;
        i |= b[4] << 32 & 0xff00000000l;
        i |= b[5] << 40 & 0xff0000000000l;
        i |= b[6] << 48 & 0xff000000000000l;
        i |= b[7] << 56 & 0xff00000000000000l;
        return i;
    }

    public static byte[] long2byte(long i) {
        byte[] result = new byte[8];
        result[0] = (byte) (i & 0xff);
        result[1] = (byte) ((i >>> 8) & 0xff);
        result[2] = (byte) ((i >>> 16) & 0xff);
        result[3] = (byte) ((i >>> 24) & 0xff);
        result[4] = (byte) ((i >>> 32) & 0xff);
        result[5] = (byte) ((i >>> 40) & 0xff);
        result[6] = (byte) ((i >>> 48) & 0xff);
        result[7] = (byte) ((i >>> 56) & 0xff);
        return result;
    }

    public static int byte2charA(byte b) {
        if (b < 0) {
            return b + 256;
        }
        return b;
    }

    public static String byte2str(byte[] b) {
        if (b == null)
            return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            sb.append(b[i] + ",");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static String byte2hex(byte[] b) {
        return byte2hex(b, b.length);
    }

    public static String byte2hex(byte[] b, int len) {
        if (b == null || b.length <= 0 || len <= 0)
            return "null";
        StringBuilder sb = new StringBuilder();
        String stmp = "";
        for (int n = 0; n < len; n++) {
            stmp = (Integer.toHexString(b[n] & 0xFF));
            if (stmp.length() == 1) {
                sb.append("0");
                sb.append(stmp);
            } else {
                sb.append(stmp);
            }
            sb.append(' ');
        }
        return sb.toString().toUpperCase();
    }

    public static void SLEEP(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public final static boolean CheckNull(String str) {
        return str == null || str.length() == 0;
    }

    /**
     * md5 或 sha1 加密
     *
     * @param s      欲加密字符串
     * @param entype 加密方式
     * @return
     * @throws Exception
     */
    public final static String EnCode(String s, String entype) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance(entype);
        md.update(s.getBytes("UTF-8"));
        byte[] digest = md.digest();
        StringBuffer md5 = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            md5.append(Character.forDigit((digest[i] & 0xF0) >> 4, 16));
            md5.append(Character.forDigit((digest[i] & 0xF), 16));
        }
        s = md5.toString();
        return s;
    }


    public final static byte[] SHA1(String s) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(EN_TYPE_SHA1);
        md.update(s.getBytes());
        return md.digest();
    }

    /**
     * 取中间文本
     *
     * @param str
     * @param l
     * @param r
     * @return
     */
    public static String getMid(String str, String l, String r) {
        int _l = str.indexOf(l);
        if (_l == -1)
            return "";
        _l += l.length();
        int _r = str.indexOf(r, _l);
        if (_r == -1)
            return "";
        return str.substring(_l, _r);
    }

    public static int tryInt(String str, int def) {
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static long tryLong(String str, long def) {
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static double tryDouble(String str, double def) {
        try {
            return Double.valueOf(str);
        } catch (Exception e) {
            return def;
        }
    }

    public static float tryFloat(String str, float def) {
        try {
            return Float.valueOf(str);
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean tryBoolean(String str) {
        if (!CheckNull(str) && "true".equals(str))
            return true;
        return false;
    }

    /**
     * 重组路径 删掉 ../ ../不会超过当前路径
     *
     * @param path
     * @return
     */
    public static String buildPath(String path) {
        int b = 0;
        path = path.replace("\\", "/");
        path = path.replace('/', File.separatorChar);
        String[] dirs = path.split(Pattern.quote(String.valueOf(File.separatorChar)));
        String[] build = new String[dirs.length];

        for (String dir : dirs) {
            if ("..".equals(dir)) {
                build[b] = null;
                if (b > 0) {
                    b--;
                }
            } else {
                build[b] = dir;
                b++;
            }
        }
        StringBuilder sb = new StringBuilder(path.length());
        for (String dir : build) {
            if (dir != null) {
                sb.append(dir);
                sb.append(File.separatorChar);
            }
        }
        if (sb.charAt(sb.length() - 1) == File.separatorChar) sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    public static String getUUID() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    /**
     * 这里返回用长整数代替以免出现负号
     *
     * @param ip
     * @return
     */
    public static long ip2int(String ip) {
        try {
            byte[] data = InetAddress.getByName(ip).getAddress();
            long addr = data[3] & 0xFF, t = 0l;
            addr |= ((data[2] << 8) & 0xFF00);
            addr |= ((data[1] << 16) & 0xFF0000);
            t = data[0] & 0xFF;// 出现负数??
            addr |= ((t << 24) & 0xFF000000);
            return addr;
        } catch (UnknownHostException e) {
        }
        return 0;
    }

    public static String int2ip(long ip) {
        StringBuilder sb = new StringBuilder();
        sb.append(((ip >> 24) & 0xff));
        sb.append('.');
        sb.append((ip >> 16) & 0xff);
        sb.append('.');
        sb.append((ip >> 8) & 0xff);
        sb.append('.');
        sb.append(ip & 0xff);
        return sb.toString();
    }

    public static String longTo35(long num) {
        if (num == 0) {
            return "0";
        }

        StringBuilder result = new StringBuilder();
        while (num > 0) {
            long remainder = num % 35;
            if (remainder < 10) {
                result.insert(0, remainder); // 0-9 对应 0-9
            } else {
                result.insert(0, (char) ('A' + (remainder - 10))); // 10-34 对应 A-Z
            }
            num /= 35;
        }
        return result.toString();
    }

    public static long str35ToLong(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("String cannot be null or empty");
        }

        long result = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int value;
            if (c >= '0' && c <= '9') {
                value = c - '0'; // 0-9 对应 0-9
            } else if (c >= 'A' && c <= 'Z') {
                value = c - 'A' + 10; // A-Z 对应 10-34
            } else {
                throw new IllegalArgumentException("Invalid character: " + c);
            }
            result = result * 35 + value;
        }
        return result;
    }

    public static long strHash(String str) {
        int len = str.length();
        long h = len;
        int step = (len >> 5) + 1;
        for (int i = len; i >= step; i -= step)
            h = h ^ ((h << 5) + (h >> 2) + (long) str.charAt(i - 1));
        return h;
    }

    /**
     * "a13dv222" 返回 [13, 222]
     *
     * @param str
     * @param deNegativeNum 是否解析负数
     * @return
     */
    public static int[] getNumberFromStr(String str, boolean deNegativeNum) {
        int[] ret = new int[0];
        int bound = 0, i = 0, len = str.length();
        StringBuilder sb = new StringBuilder(len);
        boolean nowIsAppend = false;

        while(i < len) {

            char n = str.charAt(i);

            if(n == 45 && deNegativeNum && sb.length() == 0) {
                sb.append('-');
            } else if (n >= 48 && n <= 57) {
                sb.append(n);
            } else if(sb.length() > 0) {
                try {
                    int num = Integer.valueOf(sb.toString());//解析成功后再申请内存
                    ret = Arrays.copyOf(ret, ret.length + 1);
                    ret[bound] = num;
                    bound++;
                    sb.setLength(0);
                } catch (Exception e) {
                    sb.setLength(0);
                    if(n == 45) {
                        sb.append('-');
                    }
                }
            }
            i++;
        }

        if(sb.length() > 0) {//处理最后一个数字, 如果存在的话
            try {
                int num = Integer.valueOf(sb.toString());//解析成功后再申请内存
                ret = Arrays.copyOf(ret, ret.length + 1);
                ret[bound] = num;
            } catch (Exception e) {
            }
        }

        return ret;
    }

    /**
     * 生成范围包含开始和结束
     *
     * @param start
     * @param end
     * @return
     */
    public static List<Integer> intRange(int start, int end) {
        List<Integer> arr = new ArrayList<>(end - start + 1);
        for (int i = start; i <= end; i++) {
            arr.add(Integer.valueOf(i));
        }
        return arr;
    }

    public static int ceilDiv(int a, int b) {
        if (a == 0 || b == 0)
            return 0;
        return (int) Math.ceil((double) a / b);
    }

    public static int floorDiv(int a, int b) {
        if (a == 0 || b == 0)
            return 0;
        return (int) Math.floor((double) a / b);
    }

    public static Calendar getCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("GTM+8"));
    }

    public static String getUploadFileName(String raw) {
        int p = raw.lastIndexOf(".");
        if (p != -1) {
            return UUID.randomUUID().toString() + raw.substring(p);
        }
        return UUID.randomUUID().toString();
    }

    public static String getFileSuffix(String path) {
        if(CheckNull(path)) {
            return "";
        }
        int p = path.lastIndexOf(".");
        if (p != -1) {
            return "." + path.substring(p + 1, path.length());
        }
        return "";
    }

    public static ArrayList<NetWorkAddress> getNetworkInfo(boolean isFilter) {
        Enumeration en;
        ArrayList<NetWorkAddress> ips = new ArrayList<>();
        try {
            en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) en.nextElement();
                NetWorkAddress addr = new NetWorkAddress(ni);
                if (isFilter) {
                    if (!CheckNull(addr.mac) && addr.ips.size() > 0) {
                        ips.add(addr);
                    }
                } else {
                    ips.add(addr);
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return ips;

    }

    public static String formatException(Throwable cause) {
        StackTraceElement[] stes = cause.getStackTrace();
        String message = cause.getMessage();
        StringBuffer sb = new StringBuffer();
        sb.append("\n  ");
        sb.append(cause.getClass().getName());
        if (message != null) {
            sb.append(message);
        }
        sb.append("\n");
        for (StackTraceElement ste : stes) {
            if (ste.getFileName() != null) {
                sb.append("    ");
                sb.append(ste.toString());
                sb.append("\n");
            }

        }

        Throwable ourCause = cause.getCause();

        if (ourCause != null) {
            sb.append("  Caused by ");
            sb.append(ourCause.getClass().getName());
            sb.append(":");
            sb.append(ourCause.getMessage());
            sb.append("\n");
            StackTraceElement[] ourStes = ourCause.getStackTrace();

            int n = stes.length - 1, m = ourStes.length - 1;
            while (m >= 0 && n >= 0 && ourStes[m].equals(stes[n])) {
                m--;
                n--;
            }
            for (int i = 0; i <= m; i++) {
                StackTraceElement ste = ourStes[i];
                if (ste.getFileName() != null) {
                    sb.append("    ");
                    sb.append(ste.toString());
                    sb.append("\n");
                }
            }

        }

        return sb.toString();
    }

    public static void closeable(AutoCloseable close) {
        if (close != null) {
            try {
                close.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public static void closeable(Closeable close) {
        if (close != null) {
            try {
                close.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** 如果分隔符在起始则会忽略第一个, 如果分隔符在结尾那么结尾也将忽略
     * 在开头的 =a=b=c  ["a", "b", "c"]
     * 在结尾的 a=b=c= ["a", "b", "c"]
     * */
    public static String[] splitManual(String target, String separator) {
        if (target == null) {
            return null;
        } else {
            StringTokenizer strTok = new StringTokenizer(target.toString(), separator);
            int size = strTok.countTokens();
            String[] array = new String[size];

            for(int i = 0; i < size; ++i) {
                array[i] = strTok.nextToken();
            }
            return array;
        }
    }
    /** 如果分隔符开始和结束那么将追加一个空字符串
     * 在开头的 =a=b=c  ["", "a", "b", "c"]
     * 在结尾的 a=b=c= ["a", "b", "c", ""]
     * */
    public static String[] splitManualEx(String target, char separator) {
        if (target == null) {
            return null;
        } else {
            List<String> res = new ArrayList<>(4);
            char[] arr = target.toCharArray();
            int start = 0, len = arr.length - 1;
            for(int i = 0; i <= len; i++) {
                if(arr[i] == separator) {
                    if(i == 0) {
                        res.add("");//起始位置
                    } else {
                        res.add(target.substring(start, i));
                        start = i + 1;
                        if(i == len) {
                            res.add("");//结束位置
                        }
                    }
                }
            }
            if(start < arr.length) {
                res.add(target.substring(start, arr.length));
            }

            return res.toArray(new String[0]);
        }
    }
}

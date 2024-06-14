package or.lotus.support;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Utils {

    public static final String EN_TYPE_MD5 = "md5";
    public static final String EN_TYPE_SHA1 = "sha1";
    public static final int S_BOX_MAX_SIZE = 255;

    private Utils() {}

    public static void main(String[] args) throws Exception {

        int t = ceilDiv(187, 20);

        char[] key = rc4_init("123456");
        byte[] d1 = rc4_crypt("哈哈lawd1123".getBytes("utf-8"), key.clone());

        byte[] d2 = rc4_crypt(d1, key.clone());
        System.out.println(new String(d2, "utf-8"));

        System.out.println(EnCode("123456", EN_TYPE_MD5));
        System.out.println(EnCode("123456打完", EN_TYPE_MD5));
        System.out.println(EnCode("awjkdo1230awsdikoaw", EN_TYPE_MD5));

        System.out.println("-----------");
        System.out.println(substring("1天, 价格: 1, 购买天数: 1, 当前到期时间: 2025-05-30 15:09:33, deviceId:  4860, iccid: 8986062463009035420, userId: 1 , goodsId: 3", 120));
        System.out.println(substring("a空留一个开发了哇", 3));

    }

    public static String getSysTmpDir() {
        return System.getProperty("java.io.tmpdir") + File.separator;
    }

    /**100TB, 100GB, 100MB, 100KB, 如果没有带单位默认为kb*/
    public static long formatSize(String sizeStr) {
        int[] n = Utils.getNumberFromStr(sizeStr);
        if(n.length <= 0) {
            return 0;
        }
        long size = n[0];
        if(sizeStr.endsWith("TB")) {
            size = size * 1024 * 1024 * 1024 * 1024;
        } else if(sizeStr.endsWith("GB")) {
            size = size * 1024 * 1024 * 1024;
        } else if(sizeStr.endsWith("MB")) {
            size = size * 1024 * 1024;
        } else {//kb
            size = size * 1024;
        }

        return size;
    }

    public static void assets(boolean v, String msg) {
        if(v) {
            throw new IllegalArgumentException(msg);
        }
    }


    public static BigDecimal calc(Object v1, String m, Object v2) {
        switch(m) {
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
    public static byte[] Encoded(byte[] data, String key) {
        char[] s_box = rc4_init(key);
        return rc4_crypt(data, s_box);
    }

    /**
     * rc4 加密解密
     *
     * @param data
     * @param key
     * @return
     */
    public static byte[] Decode(byte[] data, String key) {
        char[] s_box = rc4_init(key);
        return rc4_crypt(data, s_box);
    }

    public static char[] rc4_init(String key) {
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

    public static byte[] rc4_crypt(byte[] data, char[] s_box) {
        int x = 0, y = 0, t = 0, i = 0, len = data.length;
        char tmp;
        for (i = 0; i < len; i++) {
            x = (x + 1) % S_BOX_MAX_SIZE;
            y = (y + s_box[x]) % S_BOX_MAX_SIZE;
            tmp = s_box[x];
            s_box[x] = s_box[y];
            s_box[y] = tmp;
            t = (s_box[x] + s_box[y]) % S_BOX_MAX_SIZE;
            data[i] ^= s_box[t];
        }
        return data;
    }


    /**按字节长度截取字符串, 默认字符串编码为utf-8*/
    public static String substring(String s, int maxBytes) throws UnsupportedEncodingException {
        return substring(s, maxBytes, "utf-8");
    }

    /**按字节长度截取字符串*/
    public static String substring(String s, int maxBytes, String charset) throws UnsupportedEncodingException {
        if(CheckNull(s)) {
            return s;
        }

        String s2 =  s.length() > maxBytes ? s.substring(0, maxBytes) : s;
        int len = s2.getBytes(charset).length;

        while(len > maxBytes) {
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
        return (int) (start + Math.random() * (end - start + 1));
    }

    public static String RandomNum(int length) {
        StringBuilder sb = new StringBuilder(length);
        for(int i = 0; i < length; i++) {
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
        }
    }

    public final static boolean CheckNull(String str) {
        return str == null || "".equals(str);
    }

    /**
     * md5 或 sha1 加密
     *
     * @param s
     *            欲加密字符串
     * @param entype
     *            加密方式
     * @return
     * @throws Exception
     */
    public final static String EnCode(String s, String entype) throws Exception {
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


    public final static byte[] SHA1(String s) throws NoSuchAlgorithmException{
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

    public static boolean tryBoolean(String str) {
        if (!CheckNull(str) && "true".equals(str))
            return true;
        return false;
    }

    /**
     * 重组路径 删掉 ../ ../不会超过当前路径
     * @param path
     * @return
     */
    public static String BuildPath(String path) {
        int b = 0;
        String[] dirs = path.split("/");
        String[] build = new String[dirs.length];

        for(String dir : dirs){
            if("..".equals(dir)){
                build[b] = null;
                if(b > 0){
                    b--;
                }
            }else{
                build[b] = dir;
                b++;
            }
        }
        StringBuilder sb = new StringBuilder();
        for(String dir : build){
            if(dir != null){
                sb.append(dir);
                sb.append("/");
            }
        }
        if(sb.charAt(sb.length() - 1) == '/') sb.delete(sb.length() - 1, sb.length());
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
     * @param str
     * @return
     */
    public static int[] getNumberFromStr(String str) {
        String t = "";
        int[] ret = new int[1];
        int bound = 0;
        boolean ex = false;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) >= 48 && str.charAt(i) <= 57 || str.charAt(i) == 45) {
                if (ex)
                    ret = Arrays.copyOf(ret, ret.length + 1);
                t += str.charAt(i);
                ex = false;
            } else if (!ex && !"".equals(t)) {
                ex = true;
                ret[bound] = Integer.valueOf(t);
                t = "";
                bound++;
            }
        }
        if (!"".equals(t) && !ex) {
            ret[bound] = Integer.valueOf(t);
        }
        return ret;
    }

    public static int byteArrSearch(byte[] src, byte[] dest) {
        return byteArrSearch(src, dest, 0);
    }

    /**
     * 在src中查找dest的位置
     *
     * @param src
     * @param dest
     * @return 找到了返回dest在src的起始下标 未找到返回-1 此下标从0开始
     */
    public static int byteArrSearch(byte[] src, byte[] dest, int offset) {
        if(offset < 0) {
            offset = 0;
        }
        if (dest == null || src == null || src.length < dest.length)
            return -1;// fuck 了
        int p = -1, destLenEP = dest.length - 1, srcLen = src.length - destLenEP, k = 0, i = offset;
        if((destLenEP + offset) > src.length) {
            return -1;
        }
        boolean foundit = true;
        for (; i < srcLen; i++) {
            if (src[i] == dest[0] && src[i + destLenEP] == dest[destLenEP]) {// 第一个匹配 // 最后一个匹配
                for (k = 0; k < destLenEP; k++) {
                    if (src[i + k] != dest[k]) {
                        foundit = false;
                        break;
                    }
                }
                if (foundit) {
                    p = i;
                    break;
                }
            }
        }
        return p;
    }

    /**
     * 生成范围包含开始和结束
     * @param start
     * @param end
     * @return
     */
    public static List<Integer> intRange(int start, int end) {
        List<Integer> arr = new ArrayList<>(end - start + 1);
        for(int i = start; i <= end; i++) {
            arr.add(Integer.valueOf(i));
        }
        return arr;
    }

    public static int ceilDiv(int a, int b) {
        if(a == 0 || b == 0)
            return 0;
        return (int) Math.ceil((double) a / b);
    }

    public static int floorDiv(int a, int b) {
        if(a == 0 || b == 0)
            return 0;
        return (int) Math.floor((double) a / b);
    }

    public static Calendar getCalendar() {
        return Calendar.getInstance(TimeZone.getTimeZone("GTM+8"));
    }

    public static String getUploadFileName(String raw) {
        int p = raw.lastIndexOf(".");
        if(p != -1) {
            return UUID.randomUUID().toString() + raw.substring(p);
        }
        return UUID.randomUUID().toString();
    }

    public static String getFileSuffix(String path) {
        int p = path.lastIndexOf(".");
        if(p != -1) {
            return path.substring(p + 1, path.length());
        }
        return "";
    }

    public static ArrayList<Address> getNetworkInfo(boolean isFilter) {
        Enumeration en;
        ArrayList<Address> ips = new ArrayList<>();
        try {
            en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) en.nextElement();
                Address addr = new Address(ni);
                if(isFilter) {
                    if(!CheckNull(addr.mac) && addr.ips.size() > 0) {
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


}

package lotus.utils;

public class Base64 {            
    private static final byte[] encodingTable = {            
            (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E',            
            (byte) 'F', (byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J',            
            (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O',            
            (byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T',            
            (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X', (byte) 'Y',            
            (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd',            
            (byte) 'e', (byte) 'f', (byte) 'g', (byte) 'h', (byte) 'i',            
            (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n',            
            (byte) 'o', (byte) 'p', (byte) 'q', (byte) 'r', (byte) 's',            
            (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x',            
            (byte) 'y', (byte) 'z', (byte) '0', (byte) '1', (byte) '2',            
            (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',            
            (byte) '8', (byte) '9', (byte) '+', (byte) '/'            
        };            
    private static final byte[] decodingTable;            
    static {            
        decodingTable = new byte[128];            
        for (int i = 0; i < 128; i++) {            
            decodingTable[i] = (byte) -1;            
        }            
        for (int i = 'A'; i <= 'Z'; i++) {            
            decodingTable[i] = (byte) (i - 'A');            
        }            
        for (int i = 'a'; i <= 'z'; i++) {            
            decodingTable[i] = (byte) (i - 'a' + 26);            
        }            
        for (int i = '0'; i <= '9'; i++) {            
            decodingTable[i] = (byte) (i - '0' + 52);            
        }            
        decodingTable['+'] = 62;            
        decodingTable['/'] = 63;            
    }            
    public static byte[] encode(byte[] data,int offset) {            
        byte[] bytes;            
        int realCount=data.length-offset;            
        int modulus = realCount % 3;            
        if (modulus == 0) {            
            bytes = new byte[(4 * realCount) / 3];            
        } else {            
            bytes = new byte[4 * ((realCount / 3) + 1)];            
        }            
        int dataLength = (data.length - modulus);            
        int a1;            
        int a2;            
        int a3;            
        for (int i = offset, j = 0; i < dataLength; i += 3, j += 4) {            
            a1 = data[i] & 0xff;            
            a2 = data[i + 1] & 0xff;            
            a3 = data[i + 2] & 0xff;            
            bytes[j] = encodingTable[(a1 >>> 2) & 0x3f];            
            bytes[j + 1] = encodingTable[((a1 << 4) | (a2 >>> 4)) & 0x3f];            
            bytes[j + 2] = encodingTable[((a2 << 2) | (a3 >>> 6)) & 0x3f];            
            bytes[j + 3] = encodingTable[a3 & 0x3f];            
        }            
        int b1;            
        int b2;            
        int b3;            
        int d1;            
        int d2;            
        switch (modulus) {            
        case 0: /* nothing left to do */            
            break;            
        case 1:            
            d1 = data[data.length - 1] & 0xff;            
            b1 = (d1 >>> 2) & 0x3f;            
            b2 = (d1 << 4) & 0x3f;            
            bytes[bytes.length - 4] = encodingTable[b1];            
            bytes[bytes.length - 3] = encodingTable[b2];            
            bytes[bytes.length - 2] = (byte) '=';            
            bytes[bytes.length - 1] = (byte) '=';            
            break;            
        case 2:            
            d1 = data[data.length - 2] & 0xff;            
            d2 = data[data.length - 1] & 0xff;            
            b1 = (d1 >>> 2) & 0x3f;            
            b2 = ((d1 << 4) | (d2 >>> 4)) & 0x3f;            
            b3 = (d2 << 2) & 0x3f;            
            bytes[bytes.length - 4] = encodingTable[b1];            
            bytes[bytes.length - 3] = encodingTable[b2];            
            bytes[bytes.length - 2] = encodingTable[b3];            
            bytes[bytes.length - 1] = (byte) '=';            
            break;            
        }            
        return bytes;            
    }            
    public static byte[] decode(byte[] data) {            
        byte[] bytes;            
        byte b1;            
        byte b2;            
        byte b3;            
        byte b4;            
        data = discardNonBase64Bytes(data);            
        if (data[data.length - 2] == '=') {            
            bytes = new byte[(((data.length / 4) - 1) * 3) + 1];            
        } else if (data[data.length - 1] == '=') {            
            bytes = new byte[(((data.length / 4) - 1) * 3) + 2];            
        } else {            
            bytes = new byte[((data.length / 4) * 3)];            
        }            
        for (int i = 0, j = 0; i < (data.length - 4); i += 4, j += 3) {            
            b1 = decodingTable[data[i]];            
            b2 = decodingTable[data[i + 1]];            
            b3 = decodingTable[data[i + 2]];            
            b4 = decodingTable[data[i + 3]];            
            bytes[j] = (byte) ((b1 << 2) | (b2 >> 4));            
            bytes[j + 1] = (byte) ((b2 << 4) | (b3 >> 2));            
            bytes[j + 2] = (byte) ((b3 << 6) | b4);            
        }            
        if (data[data.length - 2] == '=') {            
            b1 = decodingTable[data[data.length - 4]];            
            b2 = decodingTable[data[data.length - 3]];            
            bytes[bytes.length - 1] = (byte) ((b1 << 2) | (b2 >> 4));            
        } else if (data[data.length - 1] == '=') {            
            b1 = decodingTable[data[data.length - 4]];            
            b2 = decodingTable[data[data.length - 3]];            
            b3 = decodingTable[data[data.length - 2]];            
            bytes[bytes.length - 2] = (byte) ((b1 << 2) | (b2 >> 4));            
            bytes[bytes.length - 1] = (byte) ((b2 << 4) | (b3 >> 2));            
        } else {            
            b1 = decodingTable[data[data.length - 4]];            
            b2 = decodingTable[data[data.length - 3]];            
            b3 = decodingTable[data[data.length - 2]];            
            b4 = decodingTable[data[data.length - 1]];            
            bytes[bytes.length - 3] = (byte) ((b1 << 2) | (b2 >> 4));            
            bytes[bytes.length - 2] = (byte) ((b2 << 4) | (b3 >> 2));            
            bytes[bytes.length - 1] = (byte) ((b3 << 6) | b4);            
        }            
        return bytes;            
    }            
    public static byte[] decode(String data) {            
        byte[] bytes;            
        byte b1;            
        byte b2;            
        byte b3;            
        byte b4;            
        data = discardNonBase64Chars(data);            
        if (data.charAt(data.length() - 2) == '=') {            
            bytes = new byte[(((data.length() / 4) - 1) * 3) + 1];            
        } else if (data.charAt(data.length() - 1) == '=') {            
            bytes = new byte[(((data.length() / 4) - 1) * 3) + 2];            
        } else {            
            bytes = new byte[((data.length() / 4) * 3)];            
        }            
        for (int i = 0, j = 0; i < (data.length() - 4); i += 4, j += 3) {            
            b1 = decodingTable[data.charAt(i)];            
            b2 = decodingTable[data.charAt(i + 1)];            
            b3 = decodingTable[data.charAt(i + 2)];            
            b4 = decodingTable[data.charAt(i + 3)];            
            bytes[j] = (byte) ((b1 << 2) | (b2 >> 4));            
            bytes[j + 1] = (byte) ((b2 << 4) | (b3 >> 2));            
            bytes[j + 2] = (byte) ((b3 << 6) | b4);            
        }            
        if (data.charAt(data.length() - 2) == '=') {            
            b1 = decodingTable[data.charAt(data.length() - 4)];            
            b2 = decodingTable[data.charAt(data.length() - 3)];            
            bytes[bytes.length - 1] = (byte) ((b1 << 2) | (b2 >> 4));            
        } else if (data.charAt(data.length() - 1) == '=') {            
            b1 = decodingTable[data.charAt(data.length() - 4)];            
            b2 = decodingTable[data.charAt(data.length() - 3)];            
            b3 = decodingTable[data.charAt(data.length() - 2)];            
            bytes[bytes.length - 2] = (byte) ((b1 << 2) | (b2 >> 4));            
            bytes[bytes.length - 1] = (byte) ((b2 << 4) | (b3 >> 2));            
        } else {            
            b1 = decodingTable[data.charAt(data.length() - 4)];            
            b2 = decodingTable[data.charAt(data.length() - 3)];            
            b3 = decodingTable[data.charAt(data.length() - 2)];            
            b4 = decodingTable[data.charAt(data.length() - 1)];            
            bytes[bytes.length - 3] = (byte) ((b1 << 2) | (b2 >> 4));            
            bytes[bytes.length - 2] = (byte) ((b2 << 4) | (b3 >> 2));            
            bytes[bytes.length - 1] = (byte) ((b3 << 6) | b4);            
        }            
        //for(int i=0;i<bytes.length;i++) System.out.println(","+bytes[i]);            
        return bytes;            
    }            
    private static byte[] discardNonBase64Bytes(byte[] data) {            
        byte[] temp = new byte[data.length];            
        int bytesCopied = 0;            
        for (int i = 0; i < data.length; i++) {            
            if (isValidBase64Byte(data[i])) {            
                temp[bytesCopied++] = data[i];            
            }            
        }            
        byte[] newData = new byte[bytesCopied];            
        System.arraycopy(temp, 0, newData, 0, bytesCopied);            
        return newData;            
    }            
    private static String discardNonBase64Chars(String data) {            
        StringBuffer sb = new StringBuffer();            
        int length = data.length();            
        for (int i = 0; i < length; i++) {            
            if (isValidBase64Byte((byte) (data.charAt(i)))) {            
                sb.append(data.charAt(i));            
            }            
        }            
        return sb.toString();            
    }            
    private static boolean isValidBase64Byte(byte b) {            
        if (b == '=') {            
            return true;            
        } else if ((b < 0) || (b >= 128)) {            
            return false;            
        } else if (decodingTable[b] == -1) {            
            return false;            
        }            
        return true;            
    }            
    public static String  encode(String data,String charset)throws Exception            
    {            
        // byte[] result =  (data.getBytes("Unicode"));            
         if(data==null || data.length()==0) return data;            
         int offset=0;            
         // getBytes("unicode")转完后会在前头加上两字节”FE“            
         byte[] result=encode (data.getBytes(charset),offset);            
         StringBuffer sb=new StringBuffer(result.length);            
         for (int i=0;i<result.length;i++)   sb.append((char)result[i]);            
         return sb.toString();            
    }            
    public static String  decode(String data,String charset)throws Exception            
    {            
        if(data==null || data.length()==0) return data;             
        return new String(Base64.decode(data),charset);            
    }            
    public static void main(String[] args) throws Exception {            
        
        String str = Utils.EnCode("w4v7O6xFTi36lq3RNcgctw==258EAFA5-E914-47DA-95CA-C5AB0DC85B11", Utils.EN_TYPE_SHA1);
        System.out.println(str);
        System.out.println(encode(str, "utf-8"));
        System.out.println(decode(encode(str, "utf-8"), "utf-8"));
    }            
}
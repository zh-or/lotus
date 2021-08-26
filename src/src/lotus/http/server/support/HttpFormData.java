package lotus.http.server.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.annotation.processing.FilerException;

import lotus.utils.Utils;

public class HttpFormData {
    private int         CACHE_BUFFER_SIZE   =   1024 * 4;
    private byte[]      HTTP_LINE_CHAR      =   "\r\n".getBytes();
    private byte[]      HTTP_DB_LINE_CHAR   =   "\r\n\r\n".getBytes();
    private HttpRequest request;
    private File        cacheFile;
    private FileChannel fileChannel;
    private boolean     isDecode = false;
    private File        cacheDir;
    private HashMap<String, Object> params;
    
    public HttpFormData(HttpRequest request) throws IOException {
        this.request = request;
        String cacheFileName = request.getContext().getUploadTempDir() + request.hashCode() + System.currentTimeMillis();
        this.cacheDir = new File(cacheFileName + "/");
        this.cacheDir.mkdirs();
        this.cacheFile = new File(cacheFileName + "_");
        if(this.cacheFile.exists()) {
            throw new FileAlreadyExistsException(this.cacheFile.getName());
        }
        if(!this.cacheFile.createNewFile()) {
            throw new FilerException("创建文件失败");
        }
        fileChannel = FileChannel.open(cacheFile.toPath(), StandardOpenOption.APPEND);
        params = new HashMap<String, Object>();
    }

    public void write(ByteBuffer buffer) throws IOException {
        fileChannel.write(buffer);
    }
    
    public long getCacheFileLength() throws IOException {
        return cacheFile.length();
    } 
    
    public String getParams(String key) throws Exception {
        decodeHandle();
        return (String) params.get(key);
    }
    
    public File getFile(String key) throws Exception {
        decodeHandle();
        return (File) params.get(key);
    }
    
    public File[] getFiles(String key) throws Exception {
        decodeHandle();
        Object tFs = params.get(key);
        if(tFs != null && tFs instanceof ArrayList) {
            @SuppressWarnings("unchecked")
            ArrayList<File> aFs = ((ArrayList<File>) tFs);
            File[] files = new File[aFs.size()];
            return aFs.toArray(files);
        }
        return null;
    }
    
    public File getCacheFile() {
        return cacheFile;
    }
    
    public HttpRequest getRequest() {
        return request;
    }
    
    /**
     * 解析FormData
     * @throws IOException 
     * @throws FileNotFoundException 
     */
    private void decodeHandle() throws Exception {
        if(isDecode) {
            return;
        }
        long dataFileLen = cacheFile.length();
        int readLen = -1, 
                tmp = 0,
                cachePos = 0,
                linePos = -1,
                fileNum = 1,
                state = 0;//0 
        
        String boundary = null,
               meta = null,
               contentType = null;
        File tmpFile = null;
        FileOutputStream tfileOut = null;
        
        byte[] endBoundary = null;//段结束
        byte[] ended = null;//完整结束
        
        byte[] metaTmp = new byte[CACHE_BUFFER_SIZE];
        int metaPos = 0;
        
        byte[] content = null;
        int contentPos = 0;
        
        byte[] cache = new byte[(int) (dataFileLen > CACHE_BUFFER_SIZE ? CACHE_BUFFER_SIZE : dataFileLen)];
        try (FileInputStream in = new FileInputStream(cacheFile)) {
            do {
                readLen = in.read(cache);
                cachePos = 0;
                
                try {
                    while(readLen > 0 && cachePos < readLen) {
                        
                        switch(state) {
                            case 0://read boundary
                                linePos = Utils.byteArrSearch(cache, HTTP_LINE_CHAR);
                                cachePos = cachePos + linePos + HTTP_LINE_CHAR.length;
                                boundary = new String(cache, 0, linePos, request.getCharacterEncoding());
                                endBoundary = ("\r\n" + boundary).getBytes();
                                ended = ("\r\n" + boundary + "--").getBytes();
                                state = 1;//goto meta
                                //System.out.println("boundary:" + boundary);
                                break;
                            case 1://read meta
                                
                                linePos = Utils.byteArrSearch(cache, HTTP_DB_LINE_CHAR, cachePos);
                                if(linePos == -1) {
                                    tmp = cache.length - cachePos;
                                    if(metaPos + tmp > metaTmp.length) {//扩容
                                        metaTmp = Arrays.copyOf(metaTmp, metaTmp.length + CACHE_BUFFER_SIZE);
                                    }
                                    
                                    System.arraycopy(cache, cachePos, metaTmp, metaPos, tmp);
                                    cachePos = cachePos + tmp;
                                    metaPos = metaPos + tmp;
                                } else {
                                    tmp = linePos - cachePos;
                                    System.arraycopy(cache, cachePos, metaTmp, metaPos, tmp);
                                    cachePos = cachePos + tmp + HTTP_DB_LINE_CHAR.length;
                                    metaPos = metaPos + tmp;
                                    meta = new String(metaTmp, 0, metaPos);
                                    if(meta.indexOf("Content-Type: ") != -1) {
                                        contentType = Utils.getMid(meta, "Content-Type: ", "\r\n");
                                        
                                        if(meta.indexOf("filename=\"") != -1) {
                                            //强迫症修改此处 增加包装类 文件名前增加标识防止重复文件名用
                                            tmpFile = new File(cacheDir, fileNum + "_" + Utils.getMid(meta, "filename=\"", "\""));
                                            fileNum ++;
                                            tmpFile.createNewFile();
                                            tfileOut = new FileOutputStream(tmpFile); 
                                        }
                                        
                                    } else {
                                        contentType = null;
                                    }
                                    //System.out.println("meta data:" + meta);
                                    metaPos = 0;
                                    state = 2;//goto data
                                }
                                break;
                            case 2://read data
                                int endPos = Utils.byteArrSearch(cache, endBoundary, cachePos);
                                int len;
                                if(endPos != -1) {
                                    len = endPos - cachePos;
                                    state = 1;//goto meta
                                   
                                } else {
                                    len = readLen - cachePos;
                                }
                                if(tfileOut != null) {
                                    tfileOut.write(cache, cachePos, len);
                                } else {
                                    if(content == null) {
                                        content = new byte[CACHE_BUFFER_SIZE];
                                        contentPos = 0;
                                    }
                                    if(content.length - contentPos < len) {
                                        content = Arrays.copyOf(content, content.length + CACHE_BUFFER_SIZE);
                                    }
                                    System.arraycopy(cache, cachePos, content, contentPos, len);
                                    contentPos = contentPos + len;
                                }
                                cachePos = cachePos + len;
                                if(endPos != -1) {
                                    String name = Utils.getMid(meta, "name=\"", "\"");
                                    if(tfileOut != null) {
                                        tfileOut.close();
                                        tfileOut = null;
                                        Object oldFile = params.get(name);
                                        if(oldFile != null) {
                                            if(oldFile instanceof File) {
                                                ArrayList<File> fs = new ArrayList<File>(2);
                                                fs.add((File) oldFile);
                                                fs.add(tmpFile);
                                                params.put(name, fs);
                                            } else {
                                                @SuppressWarnings("unchecked")
                                                ArrayList<File> fs = (ArrayList<File>) oldFile;
                                                fs.add(tmpFile);
                                            }
                                        } else {
                                            params.put(name, tmpFile);
                                        }
                                        
                                    } else {
                                        params.put(name, new String(content, 0, contentPos));
                                        contentPos = 0;
                                    }
                                   //检查是否全部读取结束 bin:ended
                                    int e = Utils.byteArrSearch(cache, ended, cachePos);
                                    if(e == endPos) {
                                        //System.out.println("全部读取完毕");
                                        break;
                                    }
                                    cachePos = cachePos + endBoundary.length + HTTP_LINE_CHAR.length;
                                }
                                break;
                                
                        }
                        
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
                
                
            } while(readLen > 0);
            if(tfileOut != null) {
                tfileOut.close();
                tfileOut = null;
            }
        }
        isDecode = true;
    }
   
    
    public void close() {
        if(fileChannel != null) {
            try {
                fileChannel.close();
                fileChannel = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void removeCache() {
        cacheFile.delete();
        File[] fs = cacheDir.listFiles();
        for(File f : fs) {
            f.delete();
        }
        cacheDir.delete();
    }
}

package or.lotus.core.nio.http;

import or.lotus.core.http.restful.RestfulFormData;
import or.lotus.core.http.restful.RestfulFormDataItem;
import or.lotus.core.nio.LotusByteBuf;
import or.lotus.core.nio.LotusByteBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class HttpBodyData extends RestfulFormData {

    File bodyFile = null;
    FileChannel fileChannel = null;
    LotusByteBuffer bodyBuffer = null;

    boolean isUseFile;

    long bodyLength;

    HttpRequest request;

    public HttpBodyData(HttpRequest request, long bodyLength, boolean isUseFile) throws IOException {
        this.request = request;
        this.bodyLength = bodyLength;
        this.isUseFile = isUseFile;
        if(isUseFile) {
            String setTmpPath = request.getContext().getUploadTmpDir();
            if(setTmpPath != null) {
                bodyFile = Files.createTempFile(Paths.get(setTmpPath), "req-", ".tmp").toFile();
            } else {
                bodyFile = Files.createTempFile("req-", ".tmp").toFile();
                fileChannel = FileChannel.open(bodyFile.toPath());
            }
        }
    }

    /** body写入文件缓存的超时时间, 有可能磁盘满了,  */
    public static final int BUFFER_WRITE_TO_FILE_TIME_OUT = 1000 * 30;

    /** 从网络读取数据并写入, 如果已经达到contentLength 则返回true, 否则返回false */
    public boolean appendData(LotusByteBuffer buff) throws IOException {
        if(isUseFile) {
            long start = System.currentTimeMillis();
            do {
                fileChannel.write(buff.getAllDataBuffer());
                if(System.currentTimeMillis() - start > BUFFER_WRITE_TO_FILE_TIME_OUT) {
                    throw new IOException("请检查所设置的临时目录是否已满, 请求body写入磁盘缓存时超时 > " + BUFFER_WRITE_TO_FILE_TIME_OUT + "ms");
                }
            } while(buff.getDataLength() > 0);
            return fileChannel.size() >= bodyLength;
        } else {
            //写入数据
            bodyBuffer = LotusByteBuffer.mergeBuffer(bodyBuffer, buff, bodyLength);
            return bodyBuffer != null && bodyBuffer.getDataLength() >= bodyLength;
        }
    }

    @Override
    public RestfulFormDataItem getFormDataItem(String name) {
        return null;
    }

    @Override
    public List<RestfulFormDataItem> getFormDataItems(String name) {
        return null;
    }


    @Override
    public String toString() {
        return super.toString();
    }


    public void close() throws IOException {
        if(fileChannel != null) {
            fileChannel.close();
        }
        if(bodyBuffer != null) {
            while(!bodyBuffer.release());
        }
    }
}

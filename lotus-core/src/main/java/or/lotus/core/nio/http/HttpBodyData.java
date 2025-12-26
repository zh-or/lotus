package or.lotus.core.nio.http;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.RestfulFormData;
import or.lotus.core.http.restful.RestfulFormDataItem;
import or.lotus.core.nio.LotusByteBuf;
import or.lotus.core.nio.LotusByteBuffer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HttpBodyData extends RestfulFormData {
    /*
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="username"

alice
------WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="file"; filename="avatar.jpg"
Content-Type: image/jpeg

[这里是 avatar.jpg 的二进制数据]
------WebKitFormBoundary7MA4YWxkTrZu0gW--


每个部分以 --boundary 开始（注意开头有两个连字符）。
最后一部分以 --boundary-- 结束（末尾多两个连字符表示结束）。
文本字段只有 Content-Disposition，无 Content-Type（默认为 text/plain）。
文件字段包含：
name="..."：表单字段名（如 <input name="file"> 中的 name）
filename="..."：原始文件名（可选，但通常提供）
Content-Type：MIME 类型（如 image/jpeg, application/pdf 等）
     */
    File bodyFile = null;
    FileChannel fileChannel = null;
    LotusByteBuffer bodyBuffer = null;

    boolean isUseFile;

    long bodyLength;

    HttpRequest request;

    public HttpBodyData(HttpRequest request, boolean isUseFile) throws IOException {
        this.request = request;
        this.bodyLength = request.contentLength;
        this.isUseFile = isUseFile;
        if(isUseFile) {
            String setTmpPath = request.getContext().getUploadTmpDir();
            if(setTmpPath != null) {
                bodyFile = Files.createTempFile(Paths.get(setTmpPath), "req-", ".tmp").toFile();
            } else {
                bodyFile = Files.createTempFile("req-", ".tmp").toFile();
                fileChannel = FileChannel.open(bodyFile.toPath(), StandardOpenOption.APPEND);
            }
        }
    }

    /** body写入文件缓存的超时时间, 有可能磁盘满了,  */
    public static final int BUFFER_WRITE_TO_FILE_TIME_OUT = 1000 * 60;

    /** 从网络读取数据并写入, 如果已经达到contentLength 则返回true, 否则返回false */
    public boolean appendData(LotusByteBuffer buff) throws IOException {
        if(isUseFile) {
            buff.writeToFile(fileChannel, bodyLength, BUFFER_WRITE_TO_FILE_TIME_OUT);
            return fileChannel.size() >= bodyLength;
        } else {
            //写入数据
            bodyBuffer = LotusByteBuffer.mergeBuffer(bodyBuffer, buff, bodyLength);
            return bodyBuffer != null && bodyBuffer.getDataLength() >= bodyLength;
        }
    }

    HashMap<String, List<RestfulFormDataItem>> formDataItemsMap = null;

    @Override
    public RestfulFormDataItem getFormDataItem(String name) {
        decodeBodyData();
        if(formDataItemsMap != null) {
            List<RestfulFormDataItem> items = formDataItemsMap.get(name);
            if(items != null && items.size() > 0) {
                return items.get(0);
            }
        }
        return null;
    }

    @Override
    public List<RestfulFormDataItem> getFormDataItems(String name) {
        decodeBodyData();
        if(formDataItemsMap != null) {
            List<RestfulFormDataItem> items = formDataItemsMap.get(name);
            if(items != null && items.size() > 0) {
                return items;
            }
        }
        return null;
    }

    protected boolean isDecoded = false;

    protected void decodeBodyData() {
        if(isDecoded) {
            return;
        }
        if(!request.isMultipart()) {
            return;
        }
        if(bodyLength <= 0) {
            return;
        }
        try {

            formDataItemsMap = new HashMap<>();
            byte[] boundary = null;
            byte[] line = "\r\n".getBytes(request.getContext().getCharset());
            byte[] metaEnd = "\r\n\r\n".getBytes(request.getContext().getCharset());

            if(isUseFile) {
                fileChannel.force(false);
                try (FileChannel channel = FileChannel.open(bodyFile.toPath(), StandardOpenOption.READ);) {
                    bodyBuffer = (LotusByteBuffer) request.getContext().server.pulledByteBuffer();
                    long len = bodyFile.length();
                    do {
                        long step = Math.min(Integer.MAX_VALUE, len);
                        MappedByteBuffer mapBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, step);
                        bodyBuffer.append(mapBuffer);
                        len -= step;
                    } while(len > 0);
                }
            }
            if(bodyBuffer != null) {
                int state = 0;
                HttpFormDataItem item = null;
                do {
                    switch (state) {
                        case 0://read boundary
                            int lineEnd = bodyBuffer.search(line);
                            if(lineEnd == -1) {
                                throw new RuntimeException("无boundary");
                            }

                            boundary = new byte[lineEnd + 2];
                            boundary[0] = '\r';
                            boundary[1] = '\n';
                            bodyBuffer.get(boundary, 2, lineEnd);
                            state = 1;
                            break;
                        case 1://read meta
                            int metaEndPos = bodyBuffer.search(metaEnd);
                            if(metaEndPos == -1) {
                                throw new RuntimeException("无meta");
                            }
                            byte[] metaData = new byte[metaEndPos + 4];
                            bodyBuffer.get(metaData);
                            String meta = new String(metaData, request.getContext().getCharset());
                            item = new HttpFormDataItem(meta, request);
                            state = 2;
                            break;
                        case 2://read data
                            int dataEndPos = bodyBuffer.search(boundary);
                            if(dataEndPos == -1) {
                                throw new RuntimeException("无data");
                            }
                            if(item.isStringContent) {
                                byte[] stringData = new byte[dataEndPos];
                                bodyBuffer.get(stringData);
                                item.value = new String(stringData, request.getContext().getCharset());
                            } else {
                                item.writeFileData(bodyBuffer, dataEndPos);
                            }
                            List<RestfulFormDataItem> items = formDataItemsMap.get(item.name);
                            if(items == null) {
                                items = new ArrayList<>();
                                formDataItemsMap.put(item.name, items);
                            }
                            items.add(item);
                            item = null;

                            if(bodyBuffer.getDataLength() <= boundary.length + 2 + 2) {
                                //结束了
                                bodyBuffer.rewind();
                                state = 3;
                                break;
                            }

                            state = 1;
                            break;
                    }
                } while (state < 3);
                isDecoded = true;
            }
        } catch (Exception e) {
            throw new RuntimeException("解码失败:" + bodyFile, e);
        }
    }

    protected String bodyString = null;

    /** 这个方法会读取所有body并转换为string, 需要考虑body大小问题 */
    public String getBodyString() {
        if(bodyString != null) {
            return bodyString;
        }
        if(bodyLength >= Integer.MAX_VALUE) {
            throw new RuntimeException("body 过长不能直接转换为String");
        }
        if(bodyFile != null) {
            try {
                return new String(Files.readAllBytes(bodyFile.toPath()), request.getContext().getCharset());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        StringBuilder sb = new StringBuilder((int) bodyLength);
        if(bodyBuffer != null) {
            ByteBuffer[] buffers = bodyBuffer.getAllDataBuffer();
            for (ByteBuffer buff : buffers) {
                sb.append(StandardCharsets.UTF_8.decode(buff.duplicate()));
            }
        }
        bodyString = sb.toString();
        return bodyString;
    }

    protected void close() {
        try {
            if(fileChannel != null) {
                fileChannel.close();
                fileChannel = null;
            }
        } catch (Exception e) {
            request.getContext().log.warn("清理临时文件失败 a:", e);
        }
        if(bodyBuffer != null) {
            while(!bodyBuffer.release());
            bodyBuffer = null;
        }
        try {
            if(bodyFile != null && bodyFile.exists()) {
                bodyFile.delete();
                bodyFile = null;
            }
        } catch (Exception e) {
            request.getContext().log.warn("清理临时文件失败 b:", e);
        }

        if(formDataItemsMap != null && !formDataItemsMap.isEmpty()) {
            for(List<RestfulFormDataItem> items : formDataItemsMap.values()) {
                for(RestfulFormDataItem item : items) {
                    HttpFormDataItem httpItem = (HttpFormDataItem) item;
                    try {
                        if(httpItem.file != null && httpItem.file.exists()) {
                            httpItem.file.delete();
                        }
                    } catch (Exception e) {
                        request.getContext().log.warn("清理临时文件失败 c:", e);
                    }
                }
            }
            formDataItemsMap.clear();
        }

    }
}

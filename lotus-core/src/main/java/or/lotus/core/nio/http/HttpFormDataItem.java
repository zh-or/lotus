package or.lotus.core.nio.http;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.RestfulFormDataItem;
import or.lotus.core.nio.LotusByteBuffer;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static java.nio.file.StandardOpenOption.APPEND;

public class HttpFormDataItem implements RestfulFormDataItem {
    String metaRaw;
    File file = null;
    String name = null;
    String fileName = null;
    String metaContentType = null;
    String value = null;
    boolean isStringContent = true;
    HttpRequest request;

    public HttpFormDataItem(String metaRaw, HttpRequest request) {
        this.request = request;
        this.name = Utils.getMid(metaRaw, "name=\"", "\"");
        this.fileName = Utils.getMid(metaRaw, "filename=\"", "\"");
        this.metaContentType = Utils.getMid(metaRaw, "Content-Type: ", "\r\n");
        if(Utils.CheckNull(this.metaContentType)) {
            this.metaContentType = "text/plain";
        }
        isStringContent = "text/plain".equals(metaContentType);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public InputStream getInputStream() {
        if(file != null) {
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public File getFile() {
        return file;
    }

    protected void writeFileData(LotusByteBuffer buff, long len) {
        FileChannel out = null;
        try {
            if(file == null) {
                String setTmpPath = request.getContext().getUploadTmpDir();
                if(file != null) {
                    file = Files.createTempFile(Paths.get(setTmpPath), "req-b-", ".tmp").toFile();
                } else {
                    file = Files.createTempFile("req-b-", ".tmp").toFile();
                }
            }
            out = FileChannel.open(file.toPath(), APPEND);
            buff.writeToFile(out, len, HttpBodyData.BUFFER_WRITE_TO_FILE_TIME_OUT);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(out != null) {
                try {
                    out.close();
                } catch (IOException e) {}
            }
        }
    }
}

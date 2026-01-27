package or.lotus.core.http.restful;

import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.support.ModelAndView;
import or.lotus.core.http.restful.support.RestfulResponseStatus;
import or.lotus.core.nio.http.HttpHeaderNames;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;


public abstract class RestfulResponse extends Writer {
    protected HashMap<String, String> headers;

    public Charset charset;
    public RestfulResponseStatus status;


    public RestfulResponse(RestfulContext context) {
        this.headers = new HashMap<>();
        this.charset = context.getCharset();
        setHeader("server", RestfulContext.TAG);

        Date time = new Date();
        setHeader("expires", time.toString());
        setHeader("date", time.toString());
        status = RestfulResponseStatus.SUCCESS_OK;
    }

    /** 支持 File, String, Object -> toString */
    public RestfulResponse writeObject(Object object) {
        try {
            if(object instanceof File) {
                file = (File) object;
                return this;
            }


            write(object.toString());
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public abstract void write(String str, int off, int len) throws IOException;
    public abstract void write(int c) throws IOException;
    public abstract RestfulResponse write(byte[] data);

    /** 清除content buffer中的数据 */
    public abstract RestfulResponse clearWrite();

    protected File file;
    /**  写入文件后, 不再支持其他写入 */
    public RestfulResponse write(File file) {
        this.file = file;
        return this;
    }

    public boolean isFileResponse() {
        return file != null;
    }

    public File getFile() {
        return file;
    }

    protected long fileLength = -1;

    public long getFileLength() {
        if(file == null) {
            return 0;
        }
        if(fileLength == -1) {
            fileLength = file.length();
        }
        return fileLength;
    }

    public RestfulResponse redirect(String path) {
        status = RestfulResponseStatus.REDIRECTION_FOUND;
        setHeader("Location", path);
        return this;
    }

    public RestfulResponse setStatus(RestfulResponseStatus status) {
        this.status = status;
        return this;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public String getContentType() {
        String tmp = headers.get(HttpHeaderNames.CONTENT_TYPE);
        if(!Utils.CheckNull(tmp)) {
            int p = tmp.indexOf(";");
            if(p != -1) {
                tmp = tmp.substring(0, p);
            }
            return tmp;
        }
        return null;
    }

    public RestfulResponse setHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public RestfulResponse removeHeader(String key) {
        headers.remove(key);
        return this;
    }
}

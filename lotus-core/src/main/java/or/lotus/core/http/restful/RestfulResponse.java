package or.lotus.core.http.restful;

import or.lotus.core.http.ApiRes;
import or.lotus.core.http.restful.support.ModelAndView;
import or.lotus.core.http.restful.support.RestfulResponseStatus;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;


public abstract class RestfulResponse extends Writer {
    protected RestfulRequest request;
    protected HashMap<String, String> headers;

    public Charset charset = Charset.forName("UTF-8");
    public RestfulResponseStatus status;


    public RestfulResponse(RestfulRequest request) {
        this.request = request;
        this.headers = new HashMap<>();
        this.charset = request.context.getCharset();
        setHeader("Server", RestfulContext.TAG);

        Date time = new Date();
        setHeader("Expires", time.toString());
        setHeader("Date", time.toString());
        status = RestfulResponseStatus.SUCCESS_OK;
    }

    /** 支持 String, Object -> toString, ModelAndView */
    public RestfulResponse writeObject(Object object) throws IOException {
        if(object instanceof ModelAndView) {
            request.context.handleModelAndView(request, this, (ModelAndView) object);
            return this;
        }
        if(object instanceof ApiRes) {

        }
        write(object.toString());
        return this;
    }

    public abstract void write(String str, int off, int len) throws IOException;
    public abstract void write(int c) throws IOException;
    public abstract RestfulResponse write(byte[] data);
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

    public RestfulResponse setHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public RestfulResponse removeHeader(String key) {
        headers.remove(key);
        return this;
    }
}

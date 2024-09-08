package or.lotus.core.http.restful;

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
            ModelAndView mv = (ModelAndView) object;
            if(mv.isRedirect) {
                redirect(mv.getViewName());
                return this;
            }
            setHeader("Content-type", "text/html; charset=" + charset.displayName());
            request.context.templateEngine.process(mv.getViewName(), mv.values, this);
            if(request.context.outModelAndViewTime) {
                try {
                    write("<!-- handle time: " + ((System.nanoTime() - mv.createTime) / 1_000_000) + "ms -->");
                } catch (IOException e) {}
            }
            return this;
        }
        write(object.toString());
        return this;
    }

    /** 调用此方法后其他write就没有意义了 */
    /*public RestfulResponse writeFile(File file) {
        this.file = file;

        String ifModifiedSince = request.getHeader("if-modified-since");
        if (!Utils.CheckNull(ifModifiedSince)) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = null;
            try {
                ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
                long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
                long fileLastModifiedSeconds = file.lastModified() / 1000;
                if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                    status = RestfulResponseStatus.REDIRECTION_NOT_MODIFIED;
                    return this;
                }
            } catch (ParseException e) {
                //格式化时间出错
            }
        }
        setHeader("Content-type", RestfulUtils.getMimeType(charset.displayName(), file));

        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.getDefault());
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        setHeader("Date", dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        setHeader("Expires", dateFormatter.format(time.getTime()));
        setHeader("Cache-Control", "private, max-age=" + HTTP_CACHE_SECONDS);
        setHeader("Last-Modified", dateFormatter.format(new Date(file.lastModified())));

        return this;
    }*/

    public abstract void write(String str, int off, int len) throws IOException;
    public abstract void write(int c) throws IOException;
    public abstract RestfulResponse write(byte[] data);
    public abstract RestfulResponse clearWrite();

    protected File file;
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

package or.lotus.core.http.restful;

import java.io.File;
import java.io.InputStream;

/** 文本使用getValue, 二进制使用getFile */
public interface RestfulFormDataItem {
    /** 不支持带引号的name */
    public String getName();

    /** 只有文本字段可使用该方法获取, 其他方法应调用getFile 或 getInputStream 读取 */
    public String getValue();

    public String getFileName();

    /** 用完InputStream需要调用close */
    public InputStream getInputStream();

    public File getFile();
}

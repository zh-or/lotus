package or.lotus.core.http.restful;

import java.io.File;
import java.io.InputStream;

/** 文本使用getValue, 二进制使用getFile */
public interface RestfulFormDataItem {
    public String getName();

    public String getValue();

    public String getFileName();
    public InputStream getInputStream();

    public File getFile();
}

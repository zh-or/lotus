package or.lotus.core.nio.http;

import or.lotus.core.http.restful.RestfulFormDataItem;

import java.io.File;
import java.io.InputStream;

public class HttpFormDataItem implements RestfulFormDataItem {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public String getFileName() {
        return null;
    }

    @Override
    public InputStream getInputStream() {
        return null;
    }

    @Override
    public File getFile() {
        return null;
    }
}

package or.lotus.core.http.restful;


import java.io.File;
import java.io.InputStream;

public abstract class RestfulFormData {

    public abstract RestfulFormDataItem getFormDataItem(String name);

    public String getFormDataItemValue(String name) {
        RestfulFormDataItem item = getFormDataItem(name);
        if(item == null) {
            return null;
        }
        return item.getValue();
    }

    public InputStream getFormDataItemInputStream(String name) {
        RestfulFormDataItem item = getFormDataItem(name);
        if(item == null) {
            return null;
        }
        return item.getInputStream();
    }

    public File getFormDataItemFile(String name) {
        RestfulFormDataItem item = getFormDataItem(name);
        if(item == null) {
            return null;
        }
        return item.getFile();
    }
}

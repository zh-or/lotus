package or.lotus.core.http.restful;


import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class RestfulFormData {

    public abstract RestfulFormDataItem getFormDataItem(String name);
    public abstract List<RestfulFormDataItem> getFormDataItems(String name);

    public List<String> getFormDataItemValues(String name) {
        List<RestfulFormDataItem> items = getFormDataItems(name);
        List<String> values = new ArrayList(items.size());
        for(RestfulFormDataItem item : items) {
            values.add(item.getValue());
        }
        return values;
    }

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

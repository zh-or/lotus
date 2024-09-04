package or.lotus.core.http.restful;


import java.io.InputStream;

public abstract class RestfulFormData {

    public abstract RestfulFormDataItem getFormDataItem(String name);

    public String getFormDataItemValue(String name) {
        return getFormDataItem(name).getValue();
    }

    public InputStream getFormDataItemInputStream(String name) {
        return getFormDataItem(name).getInputStream();
    }

}

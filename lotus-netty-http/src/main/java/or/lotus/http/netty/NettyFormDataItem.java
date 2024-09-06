package or.lotus.http.netty;

import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import or.lotus.core.http.restful.RestfulFormDataItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class NettyFormDataItem implements RestfulFormDataItem {
    InterfaceHttpData item;

    public NettyFormDataItem(InterfaceHttpData item) {
        this.item = item;
    }

    @Override
    public String getName() {
        return item.getName();
    }

    @Override
    public String getValue() {
        if(item.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
            Attribute attribute = (Attribute) item;
            try {
                return attribute.getValue();
            } catch (IOException e) {
            }
        }
        return null;
    }

    @Override
    public String getFileName() {
        if(item.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) item;
            return fileUpload.getFilename();
        }
        return null;
    }

    @Override
    public InputStream getInputStream() {

        if(item.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) item;
            try {
                return new FileInputStream(fileUpload.getFile());
            } catch (IOException e) {
            }
        }

        return null;
    }

    public File getFile() {
        if(item.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) item;
            try {
                return fileUpload.getFile();
            } catch (IOException e) {
            }
        }

        return null;
    }
}

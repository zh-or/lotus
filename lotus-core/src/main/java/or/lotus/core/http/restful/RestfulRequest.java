package or.lotus.core.http.restful;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import or.lotus.core.http.restful.support.PostBodyType;
import or.lotus.core.http.restful.support.RestfulCookie;
import or.lotus.core.http.restful.support.RestfulHttpMethod;

import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RestfulRequest {
    protected RestfulContext context;
    protected HashMap<String, RestfulCookie> cookies = null;
    protected HashMap<String, Object> attributes;

    public RestfulRequest(RestfulContext context) {
        this.context = context;
        this.attributes = new HashMap<>();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public abstract String getUrl();

    public String getDispatchUrl() {
        return getUrl() + getMethod().name();
    }

    public abstract String getQueryString();

    /** 获取url参数 index 从1开始, 顺序为从右到左 */
    public String getPathParamByIndex(int index) {
        String path = getUrl();
        if(index < 1 || Utils.CheckNull(path)) {
            return null;
        }

        String[] pars = path.split("/");
        int len = pars.length;
        if(len > 0 && len > index) {
            return pars[len - index];
        }
        return null;
    }

    public int getParameterInt(String name) {
        return getParameterInt(name, 0);
    }

    public int getParameterInt(String name, int def) {
        String p = getParameter(name, null);
        if(p != null) {
            return Integer.parseInt(p);
        }
        return def;
    }

    public String getParameter(String name) {
        return getParameter(name, null);
    }

    /** 从 GET -> queryString |  POST -> URLENCODED BODY 获取数据*/
    public String getParameter(String name, String def) {
        RestfulHttpMethod method = getMethod();
        String queryString = null;
        if(method == RestfulHttpMethod.GET || method == RestfulHttpMethod.DELETE || method == RestfulHttpMethod.OPTIONS) {
            queryString = getQueryString();
        } else if(method == RestfulHttpMethod.POST && getPostBodyType() == PostBodyType.URLENCODED) {
            queryString = getBodyString();
        }
        if(queryString != null) {
            Pattern pattern = Pattern.compile("[&]" + name + "=([^&]*)");
            Matcher m = pattern.matcher("&" +queryString);
            if(m.find()){
                try {
                    String par = m.group(1);
                    return URLDecoder.decode(par, context.charset.displayName());
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        return def;
    }

    PostBodyType bodyType = null;
    public PostBodyType getPostBodyType() {
        if(bodyType == null) {
            bodyType = PostBodyType.getByType(getHeader("Content-type"));
        }
        return bodyType;
    }

    protected ObjectNode bodyJson = null;
    public ObjectNode getJSON() throws JsonProcessingException {
        if(bodyJson == null) {
            bodyJson = BeanUtils.parseObject(getBodyString());
        }
        return bodyJson;
    }

    public JsonNode getJsonNodeForPath(String path) throws JsonProcessingException {
        return getJSON().path(path);
    }

    public abstract String getBodyString();

    public abstract RestfulHttpMethod getMethod();

    /** 需要在此方法内处理大小写问题 */
    public abstract String getHeader(String name);

    public synchronized RestfulCookie getCookie(String key) {
        RestfulCookie cookie = null;
        if(cookies == null) {
            cookies = new HashMap<String, RestfulCookie>();
            String cookies_str = getHeader("cookie");
            if(!Utils.CheckNull(cookies_str)) {
                String [] cookies_str_arr = cookies_str.split(";");
                for(String item : cookies_str_arr) {
                    item = item.trim();
                    RestfulCookie tmp = RestfulCookie.parseFormString(item);
                    cookies.put(tmp.key, tmp);
                }
            }
        }
        cookie = cookies.get(key);
        return cookie;
    }

    /**当前是否Multipart请求*/
    public boolean isMultipart() {
        return false;
    }

    public abstract SocketAddress getRemoteAddress();

    public abstract RestfulFormData getBodyFormData();

    public RestfulContext getContext() {
        return context;
    }
}
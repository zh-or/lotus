package or.lotus.core.http.restful.support;

import java.lang.reflect.Method;

public class BeanWrapTmp {
    public Object obj;
    public int sort;
    public String name;
    public Method method;

    public BeanWrapTmp(Object obj, String name, int sort, Method method) {
        this.obj = obj;
        this.name = name;
        this.sort = sort;
        this.method = method;
    }

    public int getSort() {
        return sort;
    }
}

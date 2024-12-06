package or.lotus.core.http.restful.support;

import java.lang.reflect.Method;

public class BeanSortWrap {
    public Object obj;
    public String name;
    public int sort;
    public Method method;
    public boolean useReturn = false;

    public BeanSortWrap(Object obj, String name, int sort, Method method, boolean useReturn) {
        this.obj = obj;
        this.sort = sort;
        this.name = name;
        this.method = method;
        this.useReturn = useReturn;
    }

    public BeanSortWrap(Object obj, String name, int sort, Method method) {
        this.obj = obj;
        this.name = name;
        this.sort = sort;
        this.method = method;
    }

    public int getSort() {
        return sort;
    }
}

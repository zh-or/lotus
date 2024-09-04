package or.lotus.core.http.restful.support;

import or.lotus.core.http.restful.RestfulResponse;
import org.thymeleaf.context.Context;

import java.util.Map;

public class ModelAndView {
    public String viewName;
    public Context values;
    public boolean isRedirect = false;
    public long createTime;

    public static ModelAndView redirect(String viewName) {
        return new ModelAndView(viewName).redirect();
    }

    public ModelAndView(String viewName) {
        this.viewName = viewName;
        this.values = new Context();
        this.createTime = System.nanoTime();
    }

    public ModelAndView redirect() {
        isRedirect = true;
        return this;
    }

    public ModelAndView set(Map<String, Object> map) {
        this.values.setVariables(map);
        return this;
    }

    public ModelAndView set(String key, Object value) {
        this.values.setVariable(key, value);
        return this;
    }

    public Object get(String key) {
        return this.values.getVariable(key);
    }

    public String getViewName() {
        return this.viewName;
    }

    public ModelAndView setViewName(String viewName) {
        this.viewName = viewName;
        return this;
    }
}

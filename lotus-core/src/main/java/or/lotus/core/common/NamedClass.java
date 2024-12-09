package or.lotus.core.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class NamedClass {

    @Target({ ElementType.TYPE, ElementType.METHOD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NamedAnnotation {
        String name();
    }

    private class NamedObj {
        public Object rawObj;
        public Method m;

        public NamedObj(Object rawObj, Method m) {
            this.rawObj = rawObj;
            this.m = m;
        }
    }

    private ConcurrentHashMap<String, NamedObj> namedMap;


    public NamedClass() {
        namedMap = new ConcurrentHashMap<>();
    }

    /**
     * 注册对象
     * @param obj
     * @throws Exception 当有重复名字时触发
     */
    public void registerClass(Object obj) {
        if(obj == null) {
            throw new RuntimeException("所注册对象不能为空");
        }
        Class<? extends Object> clazz = obj.getClass();
        NamedAnnotation na = clazz.getAnnotation(NamedAnnotation.class);

        Method[] ms = clazz.getDeclaredMethods();

        for(Method m : ms) {
            NamedAnnotation ma = m.getAnnotation(NamedAnnotation.class);

            if(ma != null) {
                NamedObj mWrap = new NamedObj(obj, m);
                String name;
                if(na != null) {
                    name = na.name() + ma.name();
                } else {
                    name = ma.name();
                }
                if(namedMap.containsKey(name)) {
                    throw new RuntimeException("出现重复名称: " + name);
                }
                namedMap.put(name, mWrap);
            }
        }
    }

    /**
     * 需要注意被调用方法的参数个数, 以及参数类型
     * @param name
     * @param args
     * @return
     * @throws Exception
     */
    public Object callMethodByName(String name, Object... args) throws Exception {
        NamedObj wrap = namedMap.get(name);
        if(wrap == null) {
            throw new NoSuchMethodException("name:" + name + " 未找到");
        }

        return wrap.m.invoke(wrap.rawObj, args);
    }
}

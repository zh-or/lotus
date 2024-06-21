package or.lotus.swl;

public interface SlideWindowData {
    public <T> T getObj(Class<T> clazz, String key);
    public void putObj(String key, Object obj, int timeoutSec);
}

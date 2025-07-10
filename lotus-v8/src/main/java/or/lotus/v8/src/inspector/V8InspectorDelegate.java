package or.lotus.v8.src.inspector;

public interface V8InspectorDelegate {
    public void onResponse(String message);

    public void waitFrontendMessageOnPause();
}

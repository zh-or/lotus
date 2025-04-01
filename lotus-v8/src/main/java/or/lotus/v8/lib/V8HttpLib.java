package or.lotus.v8.lib;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.HashMap;


import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Function;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import or.lotus.http.HttpClient5;
import or.lotus.v8.Message;
import or.lotus.v8.V8Context;
import or.lotus.v8.support.JavaLibBase;

public class V8HttpLib extends JavaLibBase {
    private V8Context base = null;
    private V8 runtime = null;

    protected Proxy proxy = null;
    protected String cookie = null;
    protected boolean debug_http = false;
    protected FileOutputStream http_out = null;
    protected final int HTTP_RES = V8Context.getMessageId();

    protected int TIME_OUT = 1000 * 20;

    @Override
    public void onInit(V8Context v8b) {
        this.base = v8b;
        runtime = v8b.getRuntimer();

        V8Object http = new V8Object(runtime);

        http.registerJavaMethod(this, "setProxy", "setProxy", new Class<?>[] {String.class, int.class, String.class});
        http.registerJavaMethod(this, "setCookie", "setCookie", new Class<?>[] {String.class});
        http.registerJavaMethod(this, "get", "get", new Class<?>[] {String.class, V8Function.class});

        http.registerJavaMethod(this, "post", "post", new Class<?>[] {String.class, V8Object.class, V8Function.class});

        http.registerJavaMethod(new JavaVoidCallback() {

            @Override
            public void invoke(V8Object receiver, V8Array parameters) {
                debug_http = parameters.getBoolean(0);
                try {
                    if(http_out != null) {
                        http_out.flush();
                        http_out.close();
                    }
                } catch (Exception e) { }
                if(debug_http) {
                    try {
                        http_out = new FileOutputStream(new File("./log/http_debug.log"), true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "debug");

        runtime.add("http", http);
        http.close();

    }

    @Override
    public boolean MessageLoop(Message msg) {
        int type = msg.getType();
        if(type == HTTP_RES){
            V8Function callback = (V8Function) msg.getAttr1();
            base.removeSyncObj(callback);
            String res = (String) msg.getResult();
            V8Array par = new V8Array(runtime);
            try{
                par.push(res);
                callback.call(runtime, par);
            }catch(Exception e){
                e.printStackTrace();
            }finally{
                par.close();
                callback.close();
            }
            return true;
        }

        return false;
    }


    @Override
    public void onQuit() {
        try {
            if(http_out != null) {
                http_out.flush();
            }
        }catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {

    }

    private void appendToHttpDebug(String req, String res) {
        appendToHttpDebug(req, null, res);
    }

    private void appendToHttpDebug(String req, HashMap<String, String> arg,  String res) {
        if(debug_http && http_out != null) {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append("\n-----------------------------------------------------------------------------------------------\n");
                sb.append("request:");
                sb.append(req);
                if(arg != null) {
                    sb.append("parmars:");
                    sb.append(arg.toString());
                }
                sb.append("\n\ncookie:");
                sb.append(cookie);
                sb.append("\n\n");
                sb.append(res);
                http_out.write(sb.toString().getBytes());
            } catch (Exception e) {
                try {
                    http_out.close();
                }catch (Exception e1) {}
                http_out = null;
                e.printStackTrace();
            }
        }
    }

    public void setProxy(String ip, int port, String type){
        if(Utils.CheckNull(ip)){
            proxy = null;
            return;
        }
        if(Utils.CheckNull(type)){
            type = "HTTP";
        }
        proxy = new Proxy(Type.valueOf(type), new InetSocketAddress(ip, port));

    }

    public void setCookie(String cookie){
        this.cookie = cookie;
    }

    public String get(String url) {
        String res = HttpClient5.get(url, TIME_OUT);

        appendToHttpDebug(url, res);

        return res;
    }

    public void get(final String url, V8Function callback) {
        //callback.
        final V8Function newCallback = callback.twin();
        base.addSyncObj(newCallback);
        base.runSyncTask(new Runnable() {

            @Override
            public void run() {
                String res = get(url);
                Message msg = new Message(HTTP_RES);
                msg.setResult(res);
                msg.setAttr1(newCallback);
                base.pushMessage(msg);

            }
        });
    }

    public String post(String url, V8Object par) {
        try {
            HashMap<String, String> arg = new HashMap<String, String>();
            for(String key : par.getKeys()){
                arg.put(key, par.getString(key));
            }

            String res = HttpClient5.postJson(url, BeanUtils.ObjToJson(arg), TIME_OUT);
            appendToHttpDebug(url, arg, res);
            return res;
        } catch (Exception e) {
            base.e("post 失败:", e);
        }
        return null;
    }

    public void post(final String url, final V8Object par, final V8Function callback){
        final V8Function newCallback = callback.twin();
        base.addSyncObj(newCallback);
        base.runSyncTask(new Runnable() {

            @Override
            public void run() {
                String res = post(url, par);
                Message msg = new Message(HTTP_RES);
                msg.setResult(res);
                msg.setAttr1(newCallback);
                base.pushMessage(msg);
            }
        });
    }


}

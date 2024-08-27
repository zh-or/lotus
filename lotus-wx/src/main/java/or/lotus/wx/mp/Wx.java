package or.lotus.wx.mp;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import or.lotus.core.http.HttpClient5;
import or.lotus.core.common.BeanUtils;
import or.lotus.core.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Wx implements Closeable {
    public static String WX_EXCEPTION = "微信服务繁忙, 请稍后再试~";

    protected static Logger log = LoggerFactory.getLogger(Wx.class);
    public static final int REQ_TIMEOUT = 10 * 1000;
    protected String token = null;
    protected int timeout = 7200;
    protected Timer timer = null;
    protected Object initLock = new Object();
    protected String appid;
    protected String secret;

    public Wx(String appid, String secret) {
        this.appid = appid;
        this.secret = secret;
    }

    @Override
    public void close() {
        try{
            timer.cancel();
        } catch(Exception e) {
            log.error("关闭微信服务失败:", e);
        }
    }

    public class WxSessionObj {
        public String openid;
        public String session_key;
        public String unionid;

        public WxSessionObj(String openid, String session_key, String unionid) {
            this.openid = openid;
            this.session_key = session_key;
            this.unionid = unionid;
        }
    }

    /** 微信小程序  wx.login 登录的code换取openid, unionid*/
    public WxSessionObj code2Session(String code) throws Exception {
        waitInit();
        HashMap<String, String> params = new HashMap<>();
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");
        params.put("appid", appid);
        params.put("secret", secret);

        String res = HttpClient5.getWithParams("https://api.weixin.qq.com/sns/jscode2session", params, REQ_TIMEOUT);
        JsonNode json = BeanUtils.parseNode(res);
        throwWxException("code2Session", params.toString() , json);
        //token = json.path("access_token").asText();
        return new WxSessionObj(
                json.path("openid").asText(),
                json.path("session_key").asText(),
                json.path("unionid").asText()
        );
    }


    /**公众号等网页应用的code换取openid, unionid*/
    public HashMap<String, String> oauth2(String code) throws Exception {
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appid
                + "&secret=" + secret
                + "&code=" + code
                + "&grant_type=authorization_code";
        String res = HttpClient5.get(url, REQ_TIMEOUT);
        JsonNode json = BeanUtils.parseNode(res);
        throwWxException("oauth2", url, json);

        HashMap<String, String> data = new HashMap<>();
        //https://developers.weixin.qq.com/doc/offiaccount/OA_Web_Apps/Wechat_webpage_authorization.html
        data.put("access_token", json.path("access_token").asText());
        data.put("expires_in", json.path("expires_in").asText());
        data.put("refresh_token", json.path("refresh_token").asText());
        data.put("openid", json.path("openid").asText());
        data.put("scope", json.path("scope").asText());
        data.put("is_snapshotuser", json.path("is_snapshotuser").asText());
        data.put("unionid", json.path("unionid").asText());

        return data;
    }


    /**
     * 获取小程序码
     * @param scene 最大32个可见字符, 不能为空?
     * @param page 默认是主页，页面 page，例如 pages/index/index，根路径前不要填加 /
     * @param width 默认430，二维码的宽度，单位 px，最小 280px，最大 1280px
     * @param line_color 默认是{"r":0,"g":0,"b":0} 。auto_color 为 false 时生效，使用 rgb 设置颜色 例如 {"r":"xxx","g":"xxx","b":"xxx"} 十进制表示
     * @param is_hyaline 默认是false，是否需要透明底色，为 true 时，生成透明底色的小程序
     * @return 成功直接返回图片二进制数据, 失败抛出异常
     */
    public byte[] getUnlimitedQRCode(String scene, String page, int width, String line_color, boolean is_hyaline, String version, boolean check_path) throws Exception {
        ObjectNode json = BeanUtils.createNode();
        json.put("scene", scene);
        json.put("env_version", version);
        json.put("check_path", check_path);//检查path是否合法
        json.put("page", page);
        if(width > 0) {
            json.put("width", width);
        }
        if(!Utils.CheckNull(line_color)) {
            json.put("line_color", BeanUtils.parseNode(line_color));
        }

        json.put("is_hyaline", is_hyaline);
        byte[] res = HttpClient5.postJsonWithBytes("https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + waitInit(), json.toString(), REQ_TIMEOUT);
        if(res != null && res.length > 0) {
            if(res[0] == 123) {//返回的是json出错了
                throwWxException("getUnlimitedQRCode", json.toString(), new String(res, "utf-8"));
            } else {
                //Files.write(Paths.get("./testQr.png"), res);
                return res;
            }
        }
        throw new Exception(WX_EXCEPTION);
    }

    /**网页应用调取jsapi前的签名计算*/
    public HashMap<String, String> getJSApiSignature(String url) throws Exception {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", waitInit());
        params.put("type", "jsapi");
        String res = HttpClient5.getWithParams("https://api.weixin.qq.com/cgi-bin/ticket/getticket", params, REQ_TIMEOUT);
        JsonNode json = BeanUtils.parseNode(res);
        throwWxException("getJSApiTicket", params.toString() , json);

        String ticket = json.path("ticket").asText();
        String noncestr = Utils.RandomString(16);
        Long timestamp = System.currentTimeMillis() / 1000;
        StringBuilder sb = new StringBuilder();
        sb.append("jsapi_ticket=").append(ticket).append("&");
        sb.append("noncestr=").append(noncestr).append("&");
        sb.append("timestamp=").append(timestamp + "").append("&");
        sb.append("url=").append(url);

        HashMap<String, String> map = new HashMap<>();
        map.put("signature", Utils.EnCode(sb.toString(), Utils.EN_TYPE_SHA1));
        map.put("appId", appid);
        map.put("timestamp", timestamp + "");
        map.put("nonceStr", noncestr);

        return map;
    }

    /**获取token需使用此方法*/
    private String waitInit() {
        String copyToken = null;
        synchronized (initLock) {
            copyToken = new String(token.toCharArray());
        }
        return copyToken;
    }

    /**创建对象后需要手动调用此方法初始化access_token*/
    public boolean init() {
        synchronized (initLock) {
            try {
                ObjectNode params = BeanUtils.createNode();
                params.put("grant_type", "client_credential");
                params.put("appid", appid);
                params.put("secret", secret);

                String res = HttpClient5.postJson("https://api.weixin.qq.com/cgi-bin/stable_token", params.toString(), REQ_TIMEOUT);
                JsonNode json = BeanUtils.parseNode(res);
                throwWxException("init", params.toString() , json);
                token = json.path("access_token").asText();
                timeout = json.path("expires_in").asInt();
                log.info("微信access_token刷新成功: {}", res);

                if(!Utils.CheckNull(token)) {
                    timeout = timeout - 60;//提前一分钟获取, 在普通模式调用下，平台会提前5分钟更新access_token，即在有效期倒计时5分钟内发起调用会获取新的access_token。在新旧access_token交接之际，平台会保证在5分钟内，新旧access_token都可用，这保证了用户业务的平滑过渡；
                    if(timer != null) {
                        timer.cancel();
                    }
                    timer = new Timer();

                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            log.info("开始刷新微信access_token...");
                            init();
                        }
                    }, timeout * 1000, timeout * 1000);
                    return true;
                }
                log.info("刷新微信token错误返回:", res);
            } catch(Exception e) {
                log.error("初始化微信API服务失败:", e);
            }
        }
        return false;
    }

    /**检查微信接口返回值是否有错误, 如有错误则直接抛异常*/
    public void throwWxException(String m, String req, String res) throws Exception {
        throwWxException(m, req, BeanUtils.parseNode(res));
    }

    /**检查微信接口返回值是否有错误, 如有错误则直接抛异常*/
    public void throwWxException(String m, String req, JsonNode res) throws Exception {
        int code = res.path("errcode").asInt();
        if(code != 0) {
            log.error("微信API返回错误, \n调用方法: {}, \n参数: {}, \n返回: {}", m, req, res);
            String errmsg = res.path("errmsg").asText();
            switch (code) {
                case 40001:
                    log.error("因为 40001 错误, 刷新token...");
                    init();
                case -1: throw new Exception(WX_EXCEPTION);
                default: throw new Exception(errmsg);
            }
        }
    }
}

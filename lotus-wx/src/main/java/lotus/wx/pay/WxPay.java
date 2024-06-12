package lotus.wx.pay;

import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import or.lotus.support.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class WxPay implements Closeable {

    protected static Logger log = LoggerFactory.getLogger(WxPay.class);

    public String appid = "";

    /** 商户号 */
    public String merchantId = "";//"190000****"

    /** 商户API私钥路径 */
    public String privateKeyPath = "";//"/Users/yourname/your/path/apiclient_key.pem"

    /** 商户证书序列号 */
    public String merchantSerialNumber = "";//5157F09EFDC096DE15EBE81A47057A72********"

    /** 商户APIV3密钥 */
    public String apiV3Key = "";

    public Config config = null;
    public JsapiServiceExtension jsapiService = null;
    public RefundService refundService = null;

    public WxPay(String appid, String merchantId, String privateKeyPath, String merchantSerialNumber, String apiV3Key) {
        this.appid = appid;

        this.merchantId = merchantId;
        this.privateKeyPath = privateKeyPath;
        this.merchantSerialNumber = merchantSerialNumber;
        this.apiV3Key = apiV3Key;

        // 使用自动更新平台证书的RSA配置, 会自动更新证书???
        // 一个商户号只能初始化一个配置，否则会因为重复的下载任务报错
        config = new RSAAutoCertificateConfig.Builder()
                .merchantId(merchantId)
                .privateKeyFromPath(privateKeyPath)
                .merchantSerialNumber(merchantSerialNumber)
                .apiV3Key(apiV3Key)
                .build();

        //jsapiService = new JsapiService.Builder().config(config).build();
        jsapiService = new JsapiServiceExtension.Builder().config(config).build();
        refundService = new RefundService.Builder().config(config).build();
    }

    /**获取回调解析器*/
    public NotificationParser getNotificationParser() {
        return new NotificationParser((NotificationConfig) config);
    }

    /**根据订单号发起退款申请*/
    public Refund requestRefund(
            String tradeNo,
            String notifyUrl,
            String refundTradeNo,
            String reason,//备注 可空
            long refund,//退款金额
            long total//原订单总金额
            ) {
        CreateRequest refundRequest = new CreateRequest();
        refundRequest.setOutTradeNo(tradeNo);
        refundRequest.setNotifyUrl(notifyUrl /*"https://notify_url"*/);
        refundRequest.setOutRefundNo(refundTradeNo);
        refundRequest.setReason(reason);

        AmountReq amount = new AmountReq();
        amount.setRefund(refund);
        amount.setTotal(total);
        amount.setCurrency("CNY");
        refundRequest.setAmount(amount);

        //log.info("开始调用微信退款接口: {}", refundRequest);

        return refundService.create(refundRequest);
    }

    /**在微信下单并获取支付参数 goodsDesc 有最大字符限制 140
     * 回调解签示例
     *
     * WxPay wxPay = context.getWxPayJsApi();
     * String body = request.getBodyString();
     * RequestParam requestParam = new RequestParam.Builder()
     *      .serialNumber(request.getHeader("Wechatpay-Serial"))
     *      .nonce(request.getHeader("Wechatpay-Nonce"))
     *     .signature(request.getHeader("Wechatpay-Signature"))
     *     .timestamp(request.getHeader("Wechatpay-Timestamp"))
     *     .body(body)
     *     .build();
     * // 初始化 NotificationParser
     * NotificationParser parser = wxPay.getNotificationParser();
     * Transaction transaction = parser.parse(requestParam, Transaction.class);
     *
     *
     * */
    public PrepayWithRequestPaymentResponse jsapiPayMakeOrder(
            String openId,
            String notifyUrl,
            int amountNum,
            String goodsDesc,
            String tradeNo) {

        if(!Utils.CheckNull(goodsDesc) && goodsDesc.length() > 140) {
            goodsDesc = goodsDesc.substring(0, 140);
        }

        // request.setXxx(val)设置所需参数，具体参数可见Request定义
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(amountNum);
        amount.setCurrency("CNY");
        request.setAmount(amount);
        request.setAppid(appid /*"wxa9d9651ae******"*/);
        request.setMchid(merchantId /*"190000****"*/);
        request.setDescription(goodsDesc/*"测试商品标题"*/);
        request.setNotifyUrl(notifyUrl /*"https://notify_url"*/);
        request.setOutTradeNo(tradeNo/*"out_trade_no_001"*/);
        Payer payer = new Payer();
        payer.setOpenid(openId /*"oLTPCuN5a-nBD4rAL_fa********"*/);
        request.setPayer(payer);
        //log.info("开始调用微信支付接口: {}", request);
        return jsapiService.prepayWithRequestPayment(request);
    }

    /**关闭订单, 关闭后应该就不能再支付了*/
    public void closeOrder(String orderNo) {
        CloseOrderRequest request = new CloseOrderRequest();
        request.setOutTradeNo(orderNo);
        request.setMchid(merchantId);
        jsapiService.closeOrder(request);
    }

    /**根据订单号查询订单*/
    public Transaction queryOrderByOutTradeNo(String orderNo) {
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setOutTradeNo(orderNo);
        request.setMchid(merchantId);
        return jsapiService.queryOrderByOutTradeNo(request);
    }


    @Override
    public void close() {

    }
}

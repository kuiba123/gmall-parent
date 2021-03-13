package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    //  http://api.gmall.com/api/payment/alipay/submit/140
    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String aliPaySubmit(@PathVariable Long orderId){
        //获取到完成的表单
        String form = alipayService.createAliPay(orderId);
        //将这个form直接输出到页面
        return form;
    }

    //  设置一个同步回调地址映射：
    //  return_payment_url=http://api.gmall.com/api/payment/alipay/callback/return
    //  给用户展示一个支付成功或者是支付失败的页面。
    //  所有的页面都在web-all 项目中
    @RequestMapping("callback/return")
    public String callback(){
        //  使用重定向到一个 web-all 的控制器中
        //  http://payment.gmall.com/pay/success.html
        return "redirect:" + AlipayConfig.return_order_url;
    }

    //  异步回调地址
    //  notify_payment_url=http://frynej.natappfree.cc/api/payment/alipay/callback/notify
    //  参考一下异步回调的业务逻辑 参考api文档。 如果支付成功了，会返回success 这七个字符。
    //  https: //商家网站通知地址?voucher_detail_list=[{"amount":"0.20","merchantContribute":"0.00","name":"5折券","otherContribute":"0.20","type":"ALIPAY_DISCOUNT_VOUCHER","voucherId":"2016101200073002586200003BQ4"}]&fund_bill_list=[{"amount":"0.80","fundChannel":"ALIPAYACCOUNT"},{"amount":"0.20","fundChannel":"MDISCOUNT"}]&subject=PC网站支付交易&trade_no=2016101221001004580200203978&gmt_create=2016-10-12 21:36:12&notify_type=trade_status_sync&total_amount=1.00&out_trade_no=mobile_rdm862016-10-12213600&invoice_amount=0.80&seller_id=2088201909970555&notify_time=2016-10-12 21:41:23&trade_status=TRADE_SUCCESS&gmt_payment=2016-10-12 21:37:19&receipt_amount=0.80&passback_params=passback_params123&buyer_id=2088102114562585&app_id=2016092101248425&notify_id=7676a2e1e4e737cff30015c4b7b55e3kh6& sign_type=RSA2&buyer_pay_amount=0.80&sign=***&point_amount=0.00
    @RequestMapping("callback/notify")
    @ResponseBody
    public String callBackNotify(@RequestParam Map<String ,String> paramMap){
        System.out.println("来了，您内! 里面请!");
        //  调用验签的方法
        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        //  获取到对应out_trade_no,trade_status
        String outTradeNo = paramMap.get("out_trade_no");
        String tradeStatus = paramMap.get("trade_status");

        //  通过out_trade_no 能够找到对应的paymentInfo 记录。那么就说明这两个out_trade_no 一致！
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        //  如果没有找到对象则说明参数不一致，则返回failure
        if (paymentInfo==null){
            return "failure";
        }

        //  金额，卖家id 等，在后面继续写
        if(signVerified){
            //  TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            //  验证参数，验证返回值的交易状态
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                //  细节的判断，防止误发错误信息！ 如果有人工客户操作的时候，可能会手动更新交易记录。
                //  这个判断通常不会走！ 防止意外！
                if ("PAID".equals(paymentInfo.getPaymentStatus()) || "ClOSED".equals(paymentInfo.getPaymentStatus())){
                    //  应该是失败！
                    return "failure";
                }
                //  正常流程，支付成了，改变交易记录的状态 PaymentStatus= PAID
                paymentService.paySuccess(outTradeNo,PaymentType.ALIPAY.name(),paramMap);
                //  表示支付成功
                return "success";
            }
        }else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    //  关闭支付宝交易记录
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){
        //  返回关闭交易记录的结果
        return alipayService.closeAliPay(orderId);
    }

    //  查看支付宝中的交易记录
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){
        //返回结果
        return alipayService.checkPayment(orderId);
    }

    //  通过outTradeNo 查询paymentInfo 数据
    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (paymentInfo!=null){
            return paymentInfo;
        }
        return null;
    }


}

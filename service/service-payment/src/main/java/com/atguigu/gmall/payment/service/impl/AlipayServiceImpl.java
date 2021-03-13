package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderFeignClient orderFeignClient;

    @Override
    public String createAliPay(Long orderId) {
        //  根据orderId 获取到orderInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        //1.保存交易记录
        paymentService.savePaymentInfo(orderInfo, PaymentType.ALIPAY.name());

        //2.生产二维码
        //	Api AlipayClient 支付的类。
        //	AlipayClient alipayClient =  new  DefaultAlipayClient(URL,APP_ID,APP_PRIVATE_KEY,FORMAT,CHARSET,ALIPAY_PUBLIC_KEY,SIGN_TYPE);
        //  AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        //	创建支付请求
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        //	设置同步回调参数地址
        alipayRequest.setReturnUrl( "http://domain.com/CallBack/return_url.jsp" );
        //	设置异步回调参数地址
        alipayRequest.setNotifyUrl( "http://domain.com/CallBack/notify_url.jsp" ); //在公共参数中设置回跳和通知地址
        //	设置的生成二维码需要的参数
        //        alipayRequest.setBizContent( "{"  +
        //                "    \"out_trade_no\":\"20150320010101001\","  +
        //                "    \"product_code\":\"FAST_INSTANT_TRADE_PAY\","  +
        //                "    \"total_amount\":88.88,"  +
        //                "    \"subject\":\"Iphone6 16G\","  +
        //                "    \"body\":\"Iphone6 16G\","  +
        //                "    \"passback_params\":\"merchantBizType%3d3C%26merchantBizNo%3d2016010101111\","  +
        //                "    \"extend_params\":{"  +
        //                "    \"sys_service_provider_id\":\"2088511833207846\""  +
        //                "    }" +
        //               ; //填充业务参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount","0.01");
        map.put("subject",orderInfo.getTradeBody());

        //  将map 转换为Json 字符串。
        alipayRequest.setBizContent(JSON.toJSONString(map));
        String form= "" ;
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        //返回表单的字符串
        return form;
    }

    @Override
    public Boolean refund(Long orderId) {
        //根据订单Id 查询到orderInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //  AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();  //  创建退款的请求对象

        //  传入参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        //  map.put("orderInfo",paymentInfo.getTradeNo());
        map.put("refund_amount","0.01");
        map.put("refund_reason","颜色不好看");
        //  map.put("out_request_no","HZ01RF001"); 表示部分退款
        request.setBizContent(JSON.toJSONString(map));
        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("退款成功");
            //  如果退款成功，则需要将支付宝内部的交易状态改为 "TRADE_CLOSED" 支付宝内部会自动改变。
            //  那么我们的交易记录也需要改成CLOSED
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
            paymentService.updatePaymentInfo(orderInfo.getOutTradeNo(),PaymentType.ALIPAY.name(),paymentInfo);

            return true;
        } else {
            System.out.println("退款失败");
            return false;
        }
    }

    @Override
    public Boolean closeAliPay(Long orderId) {
        //根据orderId获取orderInfo对象
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);

        //关闭支付宝交易记录
        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();

        //传入对应的业务参数:
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        map.put("operator_id","YX01");
        request.setBizContent(JSON.toJSONString(map));

        //执行方法
        AlipayTradeCloseResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            System.out.println("调用成功");
            return true;
        }else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public Boolean checkPayment(Long orderId) {
        //获取orderInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId);
        //  封装业务参数
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        //  声明一个map
        HashMap<String, Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
        request.setBizContent(JSON.toJSONString(map));
        //  执行请求
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }
}

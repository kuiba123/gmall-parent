package com.atguigu.gmall.payment.client;

import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.client.impl.PaymentFeignClientImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient(value = "service-payment",fallback = PaymentFeignClientImpl.class)
public interface PaymentFeignClient {

    //  根据outTradeNo 查询paymentInfo
    @GetMapping("/api/payment/alipay/getPaymentInfo/{outTradeNo}")
    PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);

    //  查询支付宝交易记录
    @RequestMapping("/api/payment/alipay/checkPayment/{orderId}")
    Boolean checkPayment(@PathVariable Long orderId);

    //  关闭支付宝交易记录
    @GetMapping("/api/payment/alipay/closePay/{orderId}")
    Boolean closePay(@PathVariable Long orderId);
}

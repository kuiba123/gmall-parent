package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    /**
     * 保存交易记录
     * @param orderInfo
     * @param paymentType 支付类型（1：微信 2：支付宝）
     */
    void savePaymentInfo(OrderInfo orderInfo,String paymentType);

    /**
     * 付款成功：调用更新交易状态
     * @param outTradeNo
     * @param name
     * @param paramMap
     */
    void paySuccess(String outTradeNo, String name, Map<String, String> paramMap);

    /**
     * 更新交易记录状态
     * @param outTradeNo
     * @param name
     * @param paymentInfo
     */
    void updatePaymentInfo(String outTradeNo, String name, PaymentInfo paymentInfo);

    /**
     * 根据outTradeNo ,支付方式查询交易记录
     * @param outTradeNo
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String outTradeNo, String name);

    /*
    * 根据订单Id关闭交易记录
    * @param orderId:
    * @return: void
    */
    void closePayment(Long orderId);
}

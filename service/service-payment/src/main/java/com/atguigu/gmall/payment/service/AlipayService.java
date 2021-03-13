package com.atguigu.gmall.payment.service;

public interface AlipayService {

    //  支付生产二维码 参数orderId，返回值String {接收完整的表单，将表单输出到页面的时候，我们在控制器完成}
    String createAliPay(Long orderId);

    //  根据订单Id 退款
    Boolean refund(Long orderId);

    //关闭支付宝交易记录
    Boolean closeAliPay(Long orderId);

    //根据订单Id查询支付宝交易记录
    Boolean checkPayment(Long orderId);
}

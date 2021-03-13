package com.atguigu.gmall.payment.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public void savePaymentInfo(OrderInfo orderInfo, String paymentType) {
        //细节问题:考虑是否允许相同的订单Id,同样的支付方式,在交易记录表中有多条数据
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderInfo.getId());
        paymentInfoQueryWrapper.eq("payment_type",paymentType);
        Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        //  说明交易记录表中已经有当前的数据，不需要再次插入了！
        if (count > 0) return;

        PaymentInfo paymentInfo = new PaymentInfo();
        //  out_trade_no，order_id，payment_type,total_amount,subject,payment_status,create_time
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setCreateTime(new Date());
        paymentInfoMapper.insert(paymentInfo);
    }

    @Override
    public void paySuccess(String outTradeNo, String name, Map<String, String> paramMap) {
        //  根据outTradeNo ,name 查询
        PaymentInfo paymentInfoQuery = this.getPaymentInfo(outTradeNo,name);
        //  判断一下：
        if ("ClOSED".equals(paymentInfoQuery.getPaymentStatus())
                || "PAID".equals(paymentInfoQuery.getPaymentStatus())){
            return;
        }

        //  设置更新的数据
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setTradeNo(paramMap.get("trade_no"));
        paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
        paymentInfo.setCallbackTime(new Date());
        paymentInfo.setCallbackContent(paramMap.toString());

        //  设置一个更新条件
        //        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        //        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        //        paymentInfoQueryWrapper.eq("payment_type",name);
        //        //  执行了更新方法
        //        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);

        //  在更新代码的基础上，做个方法的提取！
        this.updatePaymentInfo(outTradeNo,name,paymentInfo);

        //  还需要修改订单的状态.....
        //  发送的消息内容是什么? 订单Id  paymentInfo.getOrderId() 是空的！
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfoQuery.getOrderId());
    }

    //  更新方法： 提取这个方法是为了后面做方法重用！
    @Override
    public void updatePaymentInfo(String outTradeNo, String name, PaymentInfo paymentInfo) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",name);
        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
    }

    @Override
    public PaymentInfo getPaymentInfo(String outTradeNo, String name) {
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("out_trade_no",outTradeNo);
        paymentInfoQueryWrapper.eq("payment_type",name);
        //  out_trade_no ， payment_type 在交易记录表中有且只有一条记录。
        return paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
    }

    @Override
    public void closePayment(Long orderId) {
        //  执行一个更新方法payment_status=CLOSED

        //  设置更新的条件
        QueryWrapper<PaymentInfo> paymentInfoQueryWrapper = new QueryWrapper<>();
        paymentInfoQueryWrapper.eq("order_id",orderId);

        //  细节处理？ 当前这个paymentInfo 记录表中一定有orderId 么?
        //  当用户只有点击支付宝支付的时候，才会在paymentInfo 中产生记录
        PaymentInfo paymentInfoQuery = paymentInfoMapper.selectOne(paymentInfoQueryWrapper);
        //  Integer count = paymentInfoMapper.selectCount(paymentInfoQueryWrapper);
        if (paymentInfoQuery==null) return;

        //  设置更新的内容
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED.name());
        paymentInfoMapper.update(paymentInfo,paymentInfoQueryWrapper);
    }
}

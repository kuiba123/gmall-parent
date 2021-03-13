package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    @Autowired
    private RabbitService rabbitService;

    //监听消息
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void cancelOrder(Long orderId , Message message, Channel channel){
        //判断当前订单Id是否为空
            if (orderId != null) {
                //  发过来的是订单Id，那么你就需要判断一下当前的订单是否已经支付了。
                //  未支付的情况下：关闭订单
                //  根据订单Id 查询orderInfo select * from order_info where id = orderId
                //  利用这个接口IService  实现类ServiceImpl 完成根据订单Id 查询订单信息 ServiceImpl 类底层还是使用的mapper
                OrderInfo orderInfo = orderService.getById(orderId);
                //判断支付状态,进度状态
                if (orderInfo != null && "UNPAID".equals(orderInfo.getOrderStatus())&& "UNPAID".equals(orderInfo.getProcessStatus())) {
                    //  在电商本地一定会有交易记录存在么？ paymentInfo;
                    //  如果只点击了提交订单，没有点击扫码支付，则paymentInfo 不会产生记录！，只需要关闭orderInfo 即可！
                    //  根据条件查询一下paymentInfo 中是否有记录！如果有，可能需要关闭！如果没有，肯定不需要关闭paymentInfo
                    //  调用paymentService.getPaymentInfo(outTradeNo,name);
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    //  开始判断
                    if (paymentInfo!=null && "UNPAID".equals(paymentInfo.getPaymentStatus())){
                        //  当前在电商本地存在paymentInfo 记录
                        //  电商中的paymentInfo 有记录，那么在支付宝中就一定会有交易记录的存在么？
                        //  查看一下在支付宝内部是否存在交易记录：
                        //  支付宝内部如果有交易记录，可能需要关闭，如果没有交易记录。则不关闭支付宝交易记录！
                        Boolean flag = paymentFeignClient.checkPayment(orderId);
                        //  flag==true，说明有交易记录，表明用户扫码了二维码！
                        if (flag) {
                            //  扫码了，就一定会支付么？ 不一定！ 如何判断用户是否支付？ 通过关闭支付宝交易方法判断
                            Boolean result = paymentFeignClient.closePay(orderId);
                            //  result == true;
                            if (result){
                                //  说明用户没有付款；
                                //  需要关闭orderInfo,paymentInfo
                                orderService.execExpiredOrder(orderId,"2");
                            }else{
                                //  说明一种情况，用户在这一瞬间进行了付款！
                                //  业务逻辑，应该是走支付成功的业务！
                                //  如果支付成功了，正常会走一个异步回调，会发送这个消息！
                                //  rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
                            }
                        }else {
                            //  在支付宝中一定没有交易记录，orderInfo,paymentInfo 在电商本地应该是有数据的！关闭
                            orderService.execExpiredOrder(orderId,"2");
                        }
                    }else {
                        //  在电商本地不存在paymentInfo 记录。只能关闭orderInfo
                        //  关闭订单
                        orderService.execExpiredOrder(orderId,"1"); // 这个方法有可能两个表都关闭！
                    }
                }
            }
            //  手动确认消息 如果不确认，有可能会到消息残留。
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //  监听支付成功的消息
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updateOrderStatus(Long orderId , Message message,Channel channel) throws IOException {

        try {
            //  判断
            if(orderId!=null){
                //  开始的时候支付状态是应该是UNPAID , 将UNPAID 改为PAID
                OrderInfo orderInfo = orderService.getById(orderId);
                //  支付状态，进度状态都是未支付的情况下，那么我更新订单状态为PAID
                if (orderInfo!=null && "UNPAID".equals(orderInfo.getOrderStatus()) && "UNPAID".equals(orderInfo.getProcessStatus())){
                    //  更新订单状态
                    orderService.updateOrderStatus(orderId, ProcessStatus.PAID);

                    //  通知库存系统减库存 消息：这个消息是什么样的? 减库存的消息队列消费端接口需要什么数据，我们就发送什么数据！
                    orderService.sendOrderStatus(orderId);
                }
            }
        } catch (Exception e) {
            //  消息重入队列
            channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            e.printStackTrace();
            //  消息只重回一次
            return;
        }
        //  消息手动确认确认：
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}

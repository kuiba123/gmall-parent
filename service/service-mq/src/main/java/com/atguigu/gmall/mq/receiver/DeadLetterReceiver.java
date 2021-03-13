package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DeadLetterReceiver {

    //  不需要这么繁琐的绑定规则了，因为在配置类中已经设置好了。
    //    @RabbitListener(bindings = @QueueBinding(
    //            value = @Queue(value = "queue.confirm",durable = "true",autoDelete = "false"),
    //            exchange = @Exchange(value = "exchange.confirm"),
    //            key = {"routing.confirm"}
    //    ))
    //  监听队列2
    @SneakyThrows
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void getMsg(String msg, Message message, Channel channel){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("接收到的消息：\t时间:"+simpleDateFormat.format(new Date()) + "内容是：\t"+msg);

        //  手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}

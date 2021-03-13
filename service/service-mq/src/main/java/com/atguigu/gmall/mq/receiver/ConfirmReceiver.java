package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Configuration
public class ConfirmReceiver {

    //  监听消息：使用注解 设置一个绑定关系！
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",durable = "true",autoDelete = "false"),
            exchange=@Exchange(value = "exchange.confirm"),
            key = {"routing.confirm"}
    ))
    public void getMsg(String msg, Message message, Channel channel) throws IOException {
        //获取到消息
        System.out.println(msg);
        byte[] body = message.getBody();
        System.out.println("接收的消息：\t"+ new String(body));
        //  消息的确认 ，第二个参数表示是否是批量确认消息！
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}

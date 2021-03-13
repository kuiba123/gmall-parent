package com.atguigu.gmall.mq.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/mq")
@Slf4j
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    //  编写一个发送消息的控制器
    @GetMapping("sendConfirm")
    public Result sendConfirm(){
        //构建发送数据的参数
        rabbitService.sendMessage("exchange.confirm","routing.confirm","来人了,开始接客了1");
        return Result.ok();
    }

    //  发送延迟消息： 15:08:40 ---> 15:08:50
    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        //设置发送的消息
        rabbitService.sendMessage(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"来人了,开始干活吧。");
        //记录一下发送的时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("发送消息的时间：\t"+simpleDateFormat.format(new Date()));
        return Result.ok();
    }

    //  发送延迟消息： 15:08:40 ---> 15:08:50
    @GetMapping("sendDelay")
    public Result sendDelay() {
        //  设置一下发送的内容，然后设置一下发送需要延迟的时间：10秒。
        //  记录一下发送的时间
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay, "基于插件...", new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                //  在次设置消息的延迟时间10秒
                message.getMessageProperties().setDelay(100000);
                System.out.println("发送消息的时间：\t"+simpleDateFormat.format(new Date()));
                return message;
            }
        });
        return Result.ok();
    }
}

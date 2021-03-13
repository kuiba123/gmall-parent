package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class DelayedMqConfig {

    //  声明一些变量 交换机，队列，路由键
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    //创建交换机
    @Bean
    public CustomExchange delayExchange(){
        //  使用插件做的延迟队列
        //  设置的key，value 都是固定值，不能随意更改！
        HashMap<String, Object> map = new HashMap<>();
        map.put("x-delayed-type","direct");
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,map);
    }

    //创建队列
    @Bean
    public Queue delayQueue(){
        return new Queue(queue_delay_1,true,false,false,null);
    }

    //创建绑定关系
    @Bean
    public Binding delayBinding(){
        //  通过死信交换机的形式返回的！
        //  return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
        //  返回的数据类型是Binding
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay).noargs();
    }

}

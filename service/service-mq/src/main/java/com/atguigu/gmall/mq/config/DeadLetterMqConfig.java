package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class DeadLetterMqConfig {

    //声明一些变量
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    //定义交换机
    @Bean
    public DirectExchange exchange(){

        return new DirectExchange(exchange_dead,true,false,null);
    }

    //创建队列
    @Bean
    public Queue queue1(){
        //  在队列中可以设置消息的ttl
        //  在Arguments 中添加x-message-ttl 为5000 （单位是毫秒）
        HashMap<String, Object> map = new HashMap<>();
        //设置交换机绑定其他队列
        map.put("x-dead-letter-exchange",exchange_dead);
        map.put("x-dead-letter-routing-key",routing_dead_2);
        //  10秒钟的过期时间 24*60*60*1000
        map.put("x-message-ttl",10000);
        //  第三个参数表示是否排外： true 只可以在本次链接中访问 ,第五个参数 给队列设置其他属性值时，可以封装到一个map中
        return new Queue(queue_dead_1,true,false,false,map);
    }

    //进行绑定
    @Bean
    public  Binding binding1(){
        //返回绑定对象
        return BindingBuilder.bind(queue1()).to(exchange()).with(routing_dead_1);
    }

    //声明一个队列2
    @Bean
    public Queue queue2(){
        return new Queue(queue_dead_2,true,false,false,null);
    }

    //在设定一个绑定关系
    @Bean
    public Binding binding2(){
        //  返回绑定对象
        return BindingBuilder.bind(queue2()).to(exchange()).with(routing_dead_2);
    }
}

package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Component
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importToRedis(String msg, Message message, Channel channel) throws IOException {

        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        seckillGoodsQueryWrapper.eq("status","1").gt("stock_count",0);
        //当前系统时间，只判断年月日，将时间做个处理并比较
        seckillGoodsQueryWrapper.eq("date_format(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);

        //将数据库商品放入缓存
        if (!CollectionUtils.isEmpty(seckillGoodsList)) {
            //循环遍历
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                String seckillKey = RedisConst.SECKILL_GOODS;
                //判断一下在缓存中是否有了当前的秒杀商品
                Boolean flag = redisTemplate.boundHashOps(seckillKey).hasKey(seckillGoods.getSkuId().toString());
                if (flag) {
                    //flag为true,说明缓存中已经有这个商品了，不需要重复添加，跳过当前循环操作
                    continue;
                }
                //缓存中没有数据，向缓存添加数据
                redisTemplate.boundHashOps(seckillKey).put(seckillGoods.getSkuId().toString(),seckillGoods);

                //如何控制商品超卖,利用缓存存储数据的机制来实现
                for (int i = 0; i < seckillGoods.getStockCount(); i++) {
                    //定义key = seckill:stock:skuId
                    String seckillNumKey = RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId().toString();
                    redisTemplate.boundListOps(seckillNumKey).leftPush(seckillGoods.getSkuId().toString());
                }
                //  初始化状态为 1 可以秒杀了！
                redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
                //  后续业务：如果商品没有库存了，这个状态位应该0  redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":0");
                //  String stat = CacheHelper.get("skuId"); stat.equals(0) 那么你是没有办法秒杀商品的！

            }
            // 手动确认接收消息成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    //  监听队列中的用户UserRecode;
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillUserRecode(UserRecode userRecode, Message message, Channel channel){
        if (userRecode!=null){
            //  编写要给方法即可！
            seckillGoodsService.seckillOrder(userRecode.getSkuId(),userRecode.getUserId());
            //  手动确认：
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    //  秒杀结束清空缓存数据：
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18,durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearData(Message message,Channel channel){
        //  清空：秒杀结束的商品数据 18 点 status=1 表示审核通过，end_time<new Date();
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        seckillGoodsQueryWrapper.eq("status",1);
        seckillGoodsQueryWrapper.le("end_time",new Date());
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);

        //  准备清空数据
        //  库存剩余数：seckill:stock:40
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId());
        }

        //  seckill:goods
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        //  seckill:orders:users
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
        //  seckill:orders 当下单成功之后，删除过这个数据
        //  当秒杀结束之后：删除所有的预订单记录！
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);

        //  数据库： 相关的数据：更新掉！
        //  设置更新的数据： status=1 表示审核通过 status = 2 表示秒杀结束
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setStatus("2");
        seckillGoodsMapper.update(seckillGoods,seckillGoodsQueryWrapper);

        //  手动确认！
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}

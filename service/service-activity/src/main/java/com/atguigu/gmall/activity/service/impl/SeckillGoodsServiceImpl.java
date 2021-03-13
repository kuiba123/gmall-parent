package com.atguigu.gmall.activity.service.impl;

import com.alibaba.nacos.client.naming.utils.StringUtils;
import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public List<SeckillGoods> findAll() {
        //从缓存中查询秒杀商品列表
        String seckillKey = RedisConst.SECKILL_GOODS;
        List<SeckillGoods>  seckillGoodsList = redisTemplate.boundHashOps(seckillKey).values();
        return seckillGoodsList;
    }

    @Override
    public SeckillGoods getSeckillGoodsById(Long skuId) {
        //hget(key,field)
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId.toString());
        return seckillGoods;
    }

    @Override
    public void seckillOrder(Long skuId, String userId) {
        //1.判断商品状态位的状态位
        String status = (String) CacheHelper.get(skuId.toString());
        // 只有状态位 是1 的时候才能正常往下走！
        if ("0".equals(status) || StringUtils.isEmpty(status)){
            return;
        }

        //  判断用户是否已经下过预订单 使用setnx(key,value); key = seckill:user:
        String userRecodeKey = RedisConst.SECKILL_USER+userId;
        //  setnx seckill:user:userId 40
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(userRecodeKey, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        //  flag == true 说明执行成功：当前用户是第一次下单，
        //  flag==false 表示执行失败，说明当前key 已经存在了，不是第一次下单了！
        if (!flag) {
            return;
        }

        //  让库存数量 list key = seckill:stock:40 让你的这个list 吐出一个数据！
        String seckillNumKey = RedisConst.SECKILL_STOCK_PREFIX+skuId.toString();
        String redisSkuId = (String) redisTemplate.boundListOps(seckillNumKey).rightPop();
        if (StringUtils.isEmpty(redisSkuId)){
            //  说明商品已经售罄 通知其他节点 publish seckillpush 40:0;
            redisTemplate.convertAndSend("seckillpush",skuId+":0");
            //  程序停止
            return;
        }

        //  UserRecode 类中记录的是 userId，skuId
        //  OrderRecode 类记录的是 当前用户秒杀商品是谁，已经多少件等信息
        //  将用户的下单记录保存到缓存中！
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setNum(1);
        //  订单码
        orderRecode.setOrderStr(MD5.encrypt(userId+skuId));
        orderRecode.setSeckillGoods(this.getSeckillGoodsById(skuId));
        orderRecode.setUserId(userId);

        //  将这个实体类记录到缓存中 数据类型：Hash hset(key,field,value)
        //  key = seckill:orders field = userId ,value = orderRecode
        String orderUserKey = RedisConst.SECKILL_ORDERS;
        redisTemplate.boundHashOps(orderUserKey).put(userId,orderRecode);

        //  更新库存。
        this.updateStock(skuId);
    }

    @Override
    public Result checkOrder(Long skuId, String userId) {
        /*
        1.  判断用户是否在缓存中存在
        2.  判断用户是否抢单成功
        3.  判断用户是否下过订单
        4.  判断状态位
         */
        //  用户如果在缓存中有：则一定有这个key = RedisConst.SECKILL_USER+userId
        String userRecodeKey = RedisConst.SECKILL_USER + userId;
        Boolean flag = redisTemplate.hasKey(userRecodeKey);
        //  flag == true 说明用户在缓存中存在了！
        //  flag == false 说明用户在缓存中不存在！
        if (flag){
            //  判断用户是否下单 userId = 2 判断这个2号用户是否下过订单!
            //  key = seckill:orders field = userId ,value = orderRecode
            String orderUserKey = RedisConst.SECKILL_ORDERS;
            Boolean result = redisTemplate.boundHashOps(orderUserKey).hasKey(userId);
            //  判断 result == true 说明这个2号用户下过订单！
            //  orderRecode 记录当前哪个用户，秒杀的哪个SeckillGoods ，包括买了1个 都记录在这里了。
            if (result){
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(orderUserKey).get(userId);
                //  表示用户已经抢单成功或者说秒杀成功！
                //  this.show = 3
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }

        //  判断用户是否下过订单 | 预下单记录： RedisConst.SECKILL_USER+userId
        //  用户用户秒杀成功，之后下单填写下单地址等信息，然后到支付。
        //  如何存储这样的数据：
        //  准备下单成功之后我们这样存储！
        //  hset(key,field,value) key = seckill:orders:users field = userId value=orderId
        String orderKey = RedisConst.SECKILL_ORDERS_USERS;
        Boolean isExist = redisTemplate.boundHashOps(orderKey).hasKey(userId);
        //  isExist == true 表示用户已经秒杀成功了，并且已经下过订单了。this.show = 4
        if (isExist){
            //  下单成功查看我的订单
            String orderId = (String) redisTemplate.boundHashOps(orderKey).get(userId);
            return Result.build(orderId,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        //  判断状态位 状态为0 是不能秒杀商品， 1 才可以秒杀商品
        //  验证状态位：通过map 存储的。
        String stat = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(stat) || "0".equals(stat)){
            return Result.build(null,ResultCodeEnum.SECKILL_FAIL);
        }
        //  给默认值
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

    //根据秒杀的商品进行更新库存
    private void updateStock(Long skuId) {
        //  库存的数据量在 list key = seckill:stock:40
        String seckillNumKey = RedisConst.SECKILL_STOCK_PREFIX+skuId.toString();
        //  获取剩余的库存数据
        Long count = redisTemplate.boundListOps(seckillNumKey).size();
        //  更新的时候：需要更新一下缓存 ，同时还需要更新一下数据库。
        //  redis key=seckill:goods filed = 40  mysql stockCount--;
        //  为了不频繁更新数据库
        if (count%2==0){
            //  直接获取到缓存的秒杀商品
            SeckillGoods seckillGoods = this.getSeckillGoodsById(skuId);
            seckillGoods.setStockCount(count.intValue());
            seckillGoodsMapper.updateById(seckillGoods);

            //  更新缓存
            String seckillKey = RedisConst.SECKILL_GOODS;
            redisTemplate.boundHashOps(seckillKey).put(skuId.toString(),seckillGoods);
        }
    }
}




























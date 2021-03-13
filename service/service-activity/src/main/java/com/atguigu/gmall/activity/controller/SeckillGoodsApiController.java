package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.activity.util.CacheHelper;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsApiController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private OrderFeignClient orderFeignClient;

    //  获取到所有的秒杀商品列表
    @GetMapping("findAll")
    public Result findAll() {
        List<SeckillGoods> seckillGoodsList = seckillGoodsService.findAll();
        return Result.ok(seckillGoodsList);
    }

    //  根据商品skuId 获取到对应的商品详情对象
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoodsById(@PathVariable Long skuId) {
        return Result.ok(seckillGoodsService.getSeckillGoodsById(skuId));
    }

    //  获取下单码并返回
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request) {
        //  下单码的构成！可以用userId ，对它进行一个MD5加密，加密之后的这个字符串，就是下单码！
        String userId = AuthContextHolder.getUserId(request);
        //  下单生成的条件是：必须在当前秒杀时间范围内！
        //  通过skuId 获取秒杀商品
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoodsById(skuId);
        if (seckillGoods != null) {
            //  判断是否在活动范围内
            Date date = new Date();
            //  当前系统时间，要在活动开始之后，结束之前才能够生产下单码
            if (DateUtil.dateCompare(seckillGoods.getStartTime(), date) &&
                    DateUtil.dateCompare(date, seckillGoods.getEndTime())) {
                //  生产下单码
                String skuIdStr = MD5.encrypt(userId);
                return Result.ok(skuIdStr);
            }
        }
        //  返回错误信息
        return Result.fail().message("获取下单码失败!");
    }

    //  '/auth/seckillOrder/' + skuId + '?skuIdStr=' + skuIdStr
    @PostMapping("/auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId, HttpServletRequest request) {
        //  获取下单码 : 是从url 传递过来的
        String skuIdStr = request.getParameter("skuIdStr");
        //  验证下单码 ： 对用户Id 进行md5加密的结果
        String userId = AuthContextHolder.getUserId(request);
        if (!skuIdStr.equals(MD5.encrypt(userId))) {
            //  校验下单码失败！ 给提示信息
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        //  验证状态位：通过map 存储的。
        String stat = (String) CacheHelper.get(skuId.toString());
        if (StringUtils.isEmpty(stat)) {
            //  状态位是null
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        } else if ("0".equals(stat)) {
            //  表示商品已经被秒杀完成
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        } else {
            //  状态位：1 表示用户可以秒杀
            UserRecode userRecode = new UserRecode();
            userRecode.setSkuId(skuId);
            userRecode.setUserId(userId);
            //  将数据发送到mq 进行排队！
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER, MqConst.ROUTING_SECKILL_USER, userRecode);
            return Result.ok();
        }
    }

    //  检查订单：
    @GetMapping("/auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId,HttpServletRequest request) {
        String userId = AuthContextHolder.getUserId(request);
        //  调用服务层方法
        return seckillGoodsService.checkOrder(skuId,userId);
    }

    //  下订单需要的数据
    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request){
        //  声明一个map
        HashMap<String, Object> map = new HashMap<>();

        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userFeignClient.findUserAddreddListByUserId(userId);

        //  订单明细：detailArrayList
        //  key = seckill:orders field = userId ,value = orderRecode
        OrderRecode orderRecode = (OrderRecode) this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        //  判断
        if (orderRecode==null){
            return Result.fail().message("没有对应的数据!");
        }
        //  获取到秒杀商品
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();
        //  声明一个订单明细集合
        List<OrderDetail> orderDetails = new ArrayList<>();

        //  秒杀的时候一个一个的秒！
        //  声明一个订单明细
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setCreateTime(new Date());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());
        orderDetail.setSkuName(seckillGoods.getSkuName());
        orderDetail.setSkuNum(orderRecode.getNum()); // 1
        orderDetails.add(orderDetail);

        //  将用户的收货地址列表 存在map 中
        map.put("userAddressList",userAddressList);
        map.put("detailArrayList",orderDetails);
        //  如果你秒杀多个商品，则需要这样操作。
        //  map.put("totalAmount",orderInfo.getTotalAmount());
        //  如果我们一次只能秒杀一个商品！
        map.put("totalAmount",seckillGoods.getCostPrice());
        map.put("totalNum",1);
        return Result.ok(map);
    }

    //  这个控制器是谁？ /auth/submitOrder
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        //  获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //  远程调用 在这是将数据保存到数据库中。
        Long orderId = orderFeignClient.submitOrder(orderInfo);
        if (orderId==null){
            return Result.fail().message("下订单失败!");
        }

        //  原来缓存中对应有关订单的数据就不要了！
        //  抢单成功数据就不需要了。
        this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        //  需要做的是：将这个数据重新保存到缓存，
        //  hset(key,field,value) key = seckill:orders:users field = userId value=orderId
        this.redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId,orderId.toString());
        //  返回数据！
        return Result.ok(orderId);
    }
}


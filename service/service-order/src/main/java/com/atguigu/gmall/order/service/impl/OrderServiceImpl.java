package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Value("${ware.url}")
    private String wareUrl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrderInfo(OrderInfo orderInfo) {
        /*
        1.  看用户前端传递的数据都有哪些
        2.  将没有传递的数据，进行赋值。
        3.  数据保存 order_info,order_detail
         */
        //  total_amount,order_status,user_id,out_trade_no,trade_body,
        //  create_time,expire_time,process_status
        //  计算总金额 ,因为在传递参数数据的时候 orderDetailList 是有数据的，会自动封装。
        orderInfo.sumTotalAmount();
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //  user_id 在控制器中获取即可！*****
        //  第三方交易编号:在支付的时候使用 不能重复
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //  给一个固定的字符串,当前订单描述
        orderInfo.setTradeBody("过年了，买点衣服");
        //  另一种写法：你可以给每个商品的skuName ! 注意的是：长度是200， 不能超过整个数。
        orderInfo.setCreateTime(new Date());
        //  过期时间：给24小时
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //  进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        orderInfoMapper.insert(orderInfo);
        //赋值订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetail.setCreateTime(new Date());
                orderDetailMapper.insert(orderDetail);
            }
        }
        //返回的是订单Id根据订单Id进行支付
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        //制作一个UUID
        String tradeNo = UUID.randomUUID().toString();
        //  存储到缓存中
        //  采用String 数据类型
        String tradeNoKey = "user:" + userId + ":tradeNo";
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        //返回流水号
        return tradeNo;
    }

    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {

        //缓存流水号与页面的流水号比较
        String tradeNoKey = "user:"+userId+":tradeNo";
        //缓存的流水号
        String redisTradeNo = (String) redisTemplate.opsForValue().get(tradeNoKey);
        //直接返回比较结果
        return tradeNo.equals(redisTradeNo);
    }

    @Override
    public void delTradeNo(String userId) {
        //获取缓存的key
        String tradeNoKey = "user:"+userId+":tradeNo";
        //直接删除
        redisTemplate.delete(tradeNoKey);
    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        //  远程调用：能否使用feign 远程调用? 不行！ httpClient
        //  http://localhost:9001/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet(wareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //  0：无库存   1：有库存
        return "1".equals(result);
    }

    @Override
    public void execExpiredOrder(Long orderId) {
        //  要操作更新语句  update order_info set order_status=? , process_status = ? where id = orderId;
        //        OrderInfo orderInfo = new OrderInfo();
        //        orderInfo.setId(orderId);
        //        orderInfo.setOrderStatus(OrderStatus.CLOSED.name());
        //        orderInfo.setProcessStatus(ProcessStatus.CLOSED.name());
        //        orderInfoMapper.updateById(orderInfo);

        //  后续我们会有很多类似的操作 ： 根据订单Id 修改订单的状态，还有修改进度的状态
        //  例如：以后我们会讲到拆单： updateOrderStatus(orderId,ProcessStatus.SPLIT);
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //  后续我们会有很多类似的操作 ： 根据订单Id 修改订单的状态，还有修改进度的状态
        //  例如：以后我们会讲到拆单： updateOrderStatus(orderId,ProcessStatus.SPLIT);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
    }

    //  根据订单Id ，已经进度状态更新订单表
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        //订单状态
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        //查询订单信息 + 订单明细
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        if (orderInfo != null) {
            //获取订单明细数据
            List<OrderDetail> orderDetailList = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", orderId));
            //赋值
            orderInfo.setOrderDetailList(orderDetailList);
        }
        //返回orderInfo对象
        return orderInfo;
    }

    @Override
    public void sendOrderStatus(Long orderId) {
        //  更改一个订单的状态：
        this.updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        //  接收当前发送的消息
        String wareJson = this.initWareOrder(orderId);
        //  发送消息：
        this.rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK, MqConst.ROUTING_WARE_STOCK,wareJson);
    }

    //  获取发送的Json 字符串
    private String initWareOrder(Long orderId) {
        //  这Json 字符串是由OrderInfo 构成的。 先获取到orderInfo
        OrderInfo orderInfo = this.getOrderInfo(orderId);
        //  将orderInfo 中对应的字段 转成一个 Map 集合,这个map 中只有我们需要的这几个字段值。
        Map map = this.initWareOrder(orderInfo);
        return JSON.toJSONString(map);
    }

    //  这个方法就是将OrderInfo 中的部分字段转换为map 集合。 后面会使用到这个方法。
    @Override
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        //  封装需要的使用的字段即可
        map.put("orderId",orderInfo.getId());
        map.put("consignee",orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody",orderInfo.getTradeBody());
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2"); // 在线付款
        map.put("wareId",orderInfo.getWareId());    // 设置仓库Id
        //  赋值订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        List<Map> maps = new ArrayList<>();
        /*
        details:[{skuId:101,skuNum:1,skuName:’小米手64G’},
                 {skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        if (!CollectionUtils.isEmpty(orderDetailList)){
            //  循环获取数据
            for (OrderDetail orderDetail : orderDetailList) {
                //  声明一个Map 集合
                HashMap<Object, Object> hashMap = new HashMap<>();
                hashMap.put("skuId",orderDetail.getSkuId());
                hashMap.put("skuNum",orderDetail.getSkuNum());
                hashMap.put("skuName",orderDetail.getSkuName());

                //  将hashMap 添加到集合中
                maps.add(hashMap);
            }
        }
        map.put("details",maps);
        return map;
    }

    //拆单实现类
    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        /*
        1.  获取到原始订单
        2.  wareSkuMap 这参数变为我们可以操作的对象
        3.  将创建一个新的子订单，并给子订单进行赋值
        4.  需要将每个子订单添加到集合中
        5.  保存子订单
        6.  改变原始订单的状态
         */
        List<OrderInfo> subOrderInfoList = new ArrayList<>();

        //1.  获取到原始订单
        OrderInfo orderInfoOrigin = this.getOrderInfo(Long.parseLong(orderId));
        //2.  wareSkuMap 这参数变为我们可以操作的对象
        //  [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        if (!CollectionUtils.isEmpty(mapList)) {
            //循环遍历
            for (Map map : mapList) {
                //获取到仓库Id
                String wareId = (String) map.get("skuIds");
                //获取到仓库Id对应的商品集合
                List<String> skuIdsList = (List<String>) map.get("skuIds");
                //3.创建一个新的子订单,并给子订单及进行赋值
                OrderInfo subOrderInfo = new OrderInfo();
                //属性拷贝
                BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
                //设置Id防止主键冲突
                subOrderInfo.setId(null);
                //赋值子订单的父Id
                subOrderInfo.setParentOrderId(Long.parseLong(orderId));
                //赋值当前子订单的仓库Id
                subOrderInfo.setWareId(wareId);

                //  每个orderInfo 都会对应一个订单明细！ 给子订单赋值订单明细
                //  先获取到原始的订单明细
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();

                //声明一个子订单明细集合
                List<OrderDetail> orderDetails = new ArrayList<>();
                if (!CollectionUtils.isEmpty(orderDetailList)) {
                    //循环遍历
                    for (OrderDetail orderDetail : orderDetailList) {
                        //  原始的订单明细中应该有skuId 与 仓库Id 对应的商品Id 进行比较。
                        for (String skuId : skuIdsList) {
                            if (Long.parseLong(skuId) == orderDetail.getSkuId().longValue()) {
                                //  skuId 如果相等，则这个商品的skuId 就是子订单明细的。
                                orderDetails.add(orderDetail);
                            }
                        }
                    }
                }
                //  给子订单赋值订单明细集合
                subOrderInfo.setOrderDetailList(orderDetails);
                //  计算子订单的价格
                subOrderInfo.sumTotalAmount();
                //  4.  需要将每个子订单添加到集合中
                subOrderInfoList.add(subOrderInfo);
                //  5.  保存子订单
                saveOrderInfo(subOrderInfo);
            }
        }
        //  6.  改变原始订单的状态
        this.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.SPLIT);
        //  返回子订单集合
        return subOrderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        //  这个会关闭orderInfo;
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        //  判断flag
        if ("2".equals(flag)){
            //  需要关闭两个表 orderInfo,paymentInfo
            //  如果有交易记录，那么我们需要关闭交易记录，发送消息即可！
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }


}

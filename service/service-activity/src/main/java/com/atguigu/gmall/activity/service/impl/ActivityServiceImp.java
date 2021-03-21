package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.service.ActivityInfoService;
import com.atguigu.gmall.activity.service.ActivityService;
import com.atguigu.gmall.activity.service.CouponInfoService;
import com.atguigu.gmall.model.activity.ActivityRule;
import com.atguigu.gmall.model.activity.CouponInfo;
import com.atguigu.gmall.model.cart.CarInfoVo;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderDetailVo;
import com.atguigu.gmall.model.order.OrderTradeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.w3c.dom.Node;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ActivityServiceImp implements ActivityService {

    @Autowired
    private ActivityInfoService activityInfoService;

    @Autowired
    private CouponInfoService couponInfoService;


    @Override
    public Map<String, Object> findActivityAndCoupon(Long skuId, long userId) {

        Map<String, Object> map = new HashMap<>();
        /*
         1.  获取促销活动的： findActivityRule(Long skuId)
         2.  获取优惠券： findCouponInfo(Long skuId, Long activityId, Long userId)
                activityId 如何获取? findActivityRule(Long skuId) 的返回值获取到！
         */

        List<ActivityRule> activityRuleList = activityInfoService.findActivityRule(skuId);

        //获取到activityId
        long activityId = 0;
        //判断不为空
        if (!CollectionUtils.isEmpty(activityRuleList)) {
            activityId = activityRuleList.get(0).getActivityId();
        }

        List<CouponInfo> couponInfoList = couponInfoService.findCouponInfo(skuId, activityId, userId);

        //存储促销活动规则
        map.put("activityRuleList",activityRuleList);
        map.put("couponInfoList",couponInfoList);
        return map;
    }

    @Override
    public List<CarInfoVo> findCartActivityAndCoupon(List<CartInfo> cartInfoList, Long userId) {
        //  需要调用活动+优惠券
        //  声明一个参数 skuIdToActivityIdMap
        Map<Long, Long> skuIdToActivityIdMap = new HashMap<>();
        //  经过findCartActivityRuleMap 方法调用之后 skuIdToActivityIdMap 有值了！

        //  有活动的促销列表规则！
        List<CarInfoVo> carInfoVoList = activityInfoService.findCartActivityRuleMap(cartInfoList, skuIdToActivityIdMap);
        //  后面我们在使用skuIdToActivityIdMap 就有数据了！

        //  获取skuId 对应的优惠券列表集合
        Map<Long, List<CouponInfo>> skuIdToCouponInfoListMap = couponInfoService.findCartCouponInfo(cartInfoList, skuIdToActivityIdMap, userId);

        //  处理没有活动的 促销列表规则！
        //  声明一个集合来记录当前没有活动的cartInfo;
        List<CartInfo> noJoinCartInfoList = new ArrayList<>();
        //  skuIdToActivityIdMap 存储的是skuId , activityId  记录了哪些skuId 是参与活动的！
        for (CartInfo cartInfo : cartInfoList) {
            //  声明一个变量
            /*boolean flag = false;
            //  循环这个skuIdToActivityIdMap 集合
            Iterator<Map.Entry<Long, Long>> iterator = skuIdToActivityIdMap.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<Long, Long> entry = iterator.next();
                Long skuId = entry.getKey();
                Long activityId = entry.getValue();
                //  判断
                if(cartInfo.getSkuId().intValue()==skuId.intValue()){
                    //  说有这个skuId 是参与活动的
                    flag=true;
                    break;
                }
            }
            //  没有参与活动的！没有参与活动的没有促销活动规则列表
            if (!flag){
                noJoinCartInfoList.add(cartInfo);
            }*/

            if (!skuIdToActivityIdMap.containsKey(cartInfo.getSkuId())) {
                noJoinCartInfoList.add(cartInfo);
            }
        }

        //  noJoinCartInfoList 表示没有参与活动的数据
        if (!CollectionUtils.isEmpty(noJoinCartInfoList)) {
            //  赋值：
            //  声明一个CarInfoVo
            CarInfoVo carInfoVo = new CarInfoVo();
            carInfoVo.setCartInfoList(noJoinCartInfoList);
            //  没有活动就没有规则
            carInfoVo.setActivityRuleList(null);
            //  添加进去
            carInfoVoList.add(carInfoVo);
        }

        //处理优惠券规则
        for (CarInfoVo carInfoVo : carInfoVoList) {
            //  获取到对应的 List<CartInfo>
            List<CartInfo> cartInfoList1 = carInfoVo.getCartInfoList();
            //  循环遍历
            for (CartInfo cartInfo : cartInfoList1) {
                //  skuIdToCouponInfoListMap  获取skuId 对应的优惠券列表集合
                cartInfo.setCouponInfoList(skuIdToCouponInfoListMap.get(cartInfo.getSkuId()));
            }
        }
        //  返回数据
        return carInfoVoList;
    }

    @Override
    public OrderTradeVo findTradeActivityAndCoupon(List<OrderDetail> orderDetailList, Long userId) {

        //  声明一个对象
        OrderTradeVo orderTradeVo = new OrderTradeVo();
        //  private List<OrderDetailVo> orderDetailVoList;
        //  private BigDecimal activityReduceAmount;
        //  private List<CouponInfo> couponInfoList;

        Map<Long, OrderDetail> skuIdToOrderDetailMap = new HashMap<>();
        for (OrderDetail orderDetail : orderDetailList) {
            // 赋值
            skuIdToOrderDetailMap.put(orderDetail.getSkuId(),orderDetail);
        }

        //  获取订单的活动最优促销列表
        //  key = activityId value = ActivityRule
        Map<Long, ActivityRule> activityIdToActivityRuleMap = activityInfoService.findTradeActivityRuleMap(orderDetailList);
        //  存储活动Id
        List<Long> activitySkuId = new ArrayList<>();
        //  声明一个送货清单的集合
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        //  获取减少的金额
        BigDecimal activityReduceAmount = new BigDecimal("0");

        //  这个集合存储很多数据
        Iterator<Map.Entry<Long, ActivityRule>> iterator = activityIdToActivityRuleMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ActivityRule> entry = iterator.next();
            //  获取里面的数据
            ActivityRule activityRule = entry.getValue();
            //  获取活动规则下的skuId集合
            List<Long> skuIdList = activityRule.getSkuIdList();
            //  声明一个orderDetail集合
            List<OrderDetail> detailList = new ArrayList<>();

            for (Long skuId : skuIdList) {
                //  有skuId 有orderDetail.
                OrderDetail orderDetail = skuIdToOrderDetailMap.get(skuId);
                //  赋值orderDetail 集合
                detailList.add(orderDetail);
            }
            //  赋值减少的金额  activityRule.getReduceAmount();
            activityReduceAmount = activityReduceAmount.add(activityRule.getReduceAmount());
            //  activityReduceAmount+= activityRule.getReduceAmount();

            //  声明一个对象 ：
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            //  赋值orderDetailList
            orderDetailVo.setOrderDetailList(detailList);
            orderDetailVo.setActivityRule(activityRule);
            //  赋值 OrderDetailVo
            orderDetailVoList.add(orderDetailVo);
            //  应该都是有活动的！
            activitySkuId.addAll(skuIdList);
        }

        // 无活动的购物项
        //  定义一个集合记录没有活动的数据！
        List<OrderDetail> detailList = new ArrayList<>();
        //  循环遍历这个集合
        //  这集合中包含有活动，没有活动的skuId
        for (OrderDetail orderDetail : orderDetailList) {
            if (!activitySkuId.contains(orderDetail.getSkuId())){
                //  没有活动的数据
                detailList.add(skuIdToOrderDetailMap.get(orderDetail.getSkuId()));
            }
        }
        //  没有活动的对象
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        //  赋值 orderDetailList 没有活动的！
        orderDetailVo.setOrderDetailList(detailList);
        orderDetailVo.setActivityRule(null);
        // 添加没有活动的数据
        orderDetailVoList.add(orderDetailVo);

        // 赋值订单的送货清单数据！包含有活动，没有活动的数据！
        orderTradeVo.setOrderDetailVoList(orderDetailVoList);
        //  获取到订单活动的优惠券列表
        List<CouponInfo> couponInfoList = couponInfoService.findTradeCouponInfo(orderDetailList, activityIdToActivityRuleMap, userId);
        //赋值优惠劵
        orderTradeVo.setCouponInfoList(couponInfoList);
        //赋值优惠金额
        orderTradeVo.setActivityReduceAmount(activityReduceAmount);
        return orderTradeVo;
    }
}

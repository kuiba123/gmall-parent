package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.model.cart.CarInfoVo;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderTradeVo;

import java.util.List;
import java.util.Map;

public interface ActivityService{

    //根据skuId,userId获取促销活动+优惠劵列表数据
    Map<String, Object> findActivityAndCoupon(Long skuId, long userId);

    //获取购物车满足条件的促销与优惠劵信息
    List<CarInfoVo> findCartActivityAndCoupon(List<CartInfo> cartInfoList, Long userId);

    //获取交易满足条件的促销与优惠劵信息
    OrderTradeVo findTradeActivityAndCoupon(List<OrderDetail> orderDetailList, Long userId);

}

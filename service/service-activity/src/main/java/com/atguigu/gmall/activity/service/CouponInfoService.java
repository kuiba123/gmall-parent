package com.atguigu.gmall.activity.service;


import com.atguigu.gmall.model.activity.ActivityRule;
import com.atguigu.gmall.model.activity.CouponInfo;
import com.atguigu.gmall.model.activity.CouponRuleVo;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface CouponInfoService extends IService<CouponInfo> {

    //分页查询
    IPage<CouponInfo> selectPage(Page<CouponInfo> pageParam);

    //新增优惠卷规则
    void saveCouponRule(CouponRuleVo couponRuleVo);

    Map<String, Object> findActivityRuleList(Long id);

    List<CouponInfo> findCouponByKeyword(String keyword);

    //获取优惠劵信息
    List<CouponInfo> findCouponInfo(Long skuId, Long activityId, Long userId);

    //领取优惠劵
    void getCouponInfo(Long couponId, Long userId);

    //我的优惠劵
    IPage<CouponInfo> selectPageByUserId(Page<CouponInfo> couponInfoPage, Long userId);

    /**
     * 获取购物项对应的优惠劵列表
     * @param cartInfoList
     * @param skuIdToActivityIdMap 这个skuId是否存在对应的活动
     * @param userId 标识当前用户是否领取优惠劵
     * @return
     */
    Map<Long, List<CouponInfo>> findCartCouponInfo(List<CartInfo> cartInfoList, Map<Long, Long> skuIdToActivityIdMap, Long userId);

    /**
     * 获取优惠券列表
     * @param orderDetailList
     * @param activityIdToActivityRuleMap
     * @param userId
     * @return
     */
    List<CouponInfo> findTradeCouponInfo(List<OrderDetail> orderDetailList, Map<Long, ActivityRule> activityIdToActivityRuleMap, Long userId);

}

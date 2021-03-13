package com.atguigu.gmall.activity.service;


import com.atguigu.gmall.model.activity.CouponInfo;
import com.atguigu.gmall.model.activity.CouponRuleVo;
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
}

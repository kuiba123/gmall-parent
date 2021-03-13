package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.CouponInfoMapper;
import com.atguigu.gmall.activity.mapper.CouponRangeMapper;
import com.atguigu.gmall.activity.service.CouponInfoService;
import com.atguigu.gmall.model.activity.CouponInfo;
import com.atguigu.gmall.model.activity.CouponRange;
import com.atguigu.gmall.model.activity.CouponRuleVo;
import com.atguigu.gmall.model.enums.CouponRangeType;
import com.atguigu.gmall.model.enums.CouponType;
import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.misc.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.HashMapChangeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {

    @Autowired
    private CouponInfoMapper couponInfoMapper;

    @Autowired
    private CouponRangeMapper couponRangeMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Override
    public IPage<CouponInfo> selectPage(Page<CouponInfo> pageParam) {

        QueryWrapper<CouponInfo> couponInfoQueryWrapper = new QueryWrapper<>();
        couponInfoQueryWrapper.orderByDesc("id");
        IPage<CouponInfo> couponInfoIPage = couponInfoMapper.selectPage(pageParam, couponInfoQueryWrapper);
        //设置优惠卷类型
        couponInfoIPage.getRecords().stream().forEach((couponInfo -> {
            couponInfo.setCouponTypeString(CouponType.getNameByType(couponInfo.getCouponType()));
            if (couponInfo.getRangeType()!=null) {
                couponInfo.setRangeTypeString(CouponRangeType.getNameByType(couponInfo.getRangeType()));
            }
        }));

        return couponInfoIPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCouponRule(CouponRuleVo couponRuleVo) {
        //先删除,在增加
        couponRangeMapper.delete(new QueryWrapper<CouponRange>().eq("coupon_id",couponRuleVo.getCouponId()));

        //获取到优惠券对象
        CouponInfo couponInfo = this.getById(couponRuleVo.getCouponId());
        couponInfo.setRangeType(couponRuleVo.getRangeType().name());
        couponInfo.setConditionAmount(couponRuleVo.getConditionAmount());
        couponInfo.setConditionNum(couponRuleVo.getConditionNum());
        couponInfo.setBenefitAmount(couponRuleVo.getBenefitAmount());
        couponInfo.setBenefitDiscount(couponRuleVo.getBenefitDiscount());
        couponInfo.setRangeDesc(couponRuleVo.getRangeDesc());

        this.updateById(couponInfo);

        List<CouponRange> couponRangeList = couponRuleVo.getCouponRangeList();
        for (CouponRange couponRange : couponRangeList) {
            couponRange.setCouponId(couponRuleVo.getCouponId());
            couponRangeMapper.insert(couponRange);
        }
    }

    @Override
    public Map<String, Object> findActivityRuleList(Long id) {
        //封装数据
        HashMap<String, Object> map = new HashMap<>();
        //获取使用规则
        CouponInfo couponInfo = this.getById(id);
        //  获取当前优惠券使用范围
        QueryWrapper<CouponRange> couponRangeQueryWrapper = new QueryWrapper<>();
        couponRangeQueryWrapper.eq("coupon_id",id);
        List<CouponRange> couponRangeList = couponRangeMapper.selectList(couponRangeQueryWrapper);

        //获取到rangId集合
        List<Long> rangeIdList = couponRangeList.stream().map(CouponRange::getRangeId).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(rangeIdList)){
            //  判断
            if (couponInfo.getRangeType().equals("SPU")){
                List<SpuInfo> spuInfoList = productFeignClient.findSpuInfoBySpuIdList(rangeIdList);
                map.put("spuInfoList",spuInfoList);
            }else if ("TRADEMARK".equals(couponInfo.getRangeType())){
                List<BaseTrademark> baseTrademarkList = productFeignClient.findBaseTrademarkByTrademarkIdList(rangeIdList);
                map.put("trademarkList",baseTrademarkList);
            }else {
                List<BaseCategory3> category3List = productFeignClient.findBaseCategory3ByCategory3IdList(rangeIdList);
                map.put("category3List", category3List);
            }
        }
        return map;
    }

    @Override
    public List<CouponInfo> findCouponByKeyword(String keyword) {
        QueryWrapper<CouponInfo> couponInfoQueryWrapper = new QueryWrapper<>();
        couponInfoQueryWrapper.like("coupon_name",keyword);
        return couponInfoMapper.selectList(couponInfoQueryWrapper);
    }
}

package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.ActivityInfoMapper;
import com.atguigu.gmall.activity.mapper.ActivityRuleMapper;
import com.atguigu.gmall.activity.mapper.ActivitySkuMapper;
import com.atguigu.gmall.activity.mapper.CouponInfoMapper;
import com.atguigu.gmall.activity.service.ActivityInfoService;
import com.atguigu.gmall.model.activity.*;
import com.atguigu.gmall.model.enums.ActivityType;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.assist.ISqlRunner;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.bouncycastle.jcajce.provider.util.SecretKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActivityInfoServiceImpl extends ServiceImpl<ActivityInfoMapper, ActivityInfo> implements ActivityInfoService {

    @Autowired
    private ActivityInfoMapper activityInfoMapper;

    @Autowired
    private ActivityRuleMapper activityRuleMapper;

    @Autowired
    private ActivitySkuMapper activitySkuMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CouponInfoMapper couponInfoMapper;

    @Override
    public IPage<ActivityInfo> getPage(Page<ActivityInfo> infoPage) {
        //构造查询条件
        QueryWrapper<ActivityInfo> activityInfoQueryWrapper = new QueryWrapper<>();
        activityInfoQueryWrapper.orderByDesc("id");
        IPage<ActivityInfo> activityInfoIPage = activityInfoMapper.selectPage(infoPage, activityInfoQueryWrapper);
        //  细节： 活动的数据类型： 在表中不存在！activityTypeString
        //  Consumer  void accept(T t);
        activityInfoIPage.getRecords().stream().forEach(activityInfo -> {
            //  如何获取到对应的数据ActivityType.getNameByType(type) ;
            activityInfo.setActivityTypeString(ActivityType.getNameByType(activityInfo.getActivityType()));
        });
        //返回数据
        return activityInfoIPage;
    }

    //  ActivityRuleVo ： 既包含保存，同时也可以包含修改内容！
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveActivityRule(ActivityRuleVo activityRuleVo) {
        //  在写平台属性保存的时候： 先删除对应的数据，然后再新增数据！
        //  原来对应的商品活动范围列表删除：
        activitySkuMapper.delete(new QueryWrapper<ActivitySku>().eq("activity_id",activityRuleVo.getActivityId()));
        //  删除原来的活动规则列表
        activityRuleMapper.delete(new QueryWrapper<ActivityRule>().eq("activity_id",activityRuleVo.getActivityId()));
        //优惠卷列表暂时不写

        //  保存数据：
        List<ActivitySku> activitySkuList = activityRuleVo.getActivitySkuList();
        List<ActivityRule> activityRuleList = activityRuleVo.getActivityRuleList();

        //删除活动与优惠卷绑定关系
        CouponInfo couponInfo = new CouponInfo();
        couponInfo.setActivityId(0L);
        couponInfoMapper.update(couponInfo,new QueryWrapper<CouponInfo>().eq("activity_id",activityRuleVo.getActivityId()));


        for (ActivitySku activitySku : activitySkuList) {
            //  需要将活动Id 赋值给当前对象
            activitySku.setActivityId(activityRuleVo.getActivityId());
            activitySkuMapper.insert(activitySku);
        }

        for (ActivityRule activityRule : activityRuleList) {
            //  需要将活动Id 赋值给当前对象
            activityRule.setActivityId(activityRuleVo.getActivityId());
            activityRuleMapper.insert(activityRule);
        }

        //开始绑定关系
        List<Long> couponIdList = activityRuleVo.getCouponIdList();
        if (!CollectionUtils.isEmpty(couponIdList)) {
            for (Long couponId : couponIdList) {
                CouponInfo couponInfoUPD = couponInfoMapper.selectById(couponId);
                couponInfoUPD.setActivityId(activityRuleVo.getActivityId());
                couponInfoMapper.updateById(couponInfoUPD);
            }
        }
    }

    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        //获取到所有的skuInfo集合列表
        List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoByKeyword(keyword);
        //将集合转换为skuId集合列表
        List<Long> skuIdList = skuInfoList.stream().map(SkuInfo::getId).collect(Collectors.toList());
        //找出参加活动的skuId
        List<Long> exitSkuIdList = activityInfoMapper.selectExistSkuIdList(skuIdList);
        //通过这个存在的skuId找到对应的skuInfo
        List<SkuInfo> skuInfos = exitSkuIdList.stream().map(skuId -> {
            return productFeignClient.getSkuInfo(skuId);
        }).collect(Collectors.toList());

        skuInfoList.removeAll(skuInfos);
        return skuInfoList;
    }

    @Override
    public Map<String, Object> findActivityRuleList(Long id) {
        Map<String,Object> map = new HashMap<>();
        //回显数据: activity_rule ，activity_sku
        QueryWrapper<ActivityRule> activityRuleQueryWrapper = new QueryWrapper<>();
        activityRuleQueryWrapper.eq("activity_id",id);
        List<ActivityRule> activityRuleList = activityRuleMapper.selectList(activityRuleQueryWrapper);

        map.put("activityRuleList",activityRuleList);

        QueryWrapper<ActivitySku> activitySkuQueryWrapper = new QueryWrapper<>();
        activitySkuQueryWrapper.eq("activity_id",id);
        List<ActivitySku> activitySkuList = activitySkuMapper.selectList(activitySkuQueryWrapper);
        //  获取到Id 集合
        List<Long> skuIdList = activitySkuList.stream().map(ActivitySku::getSkuId).collect(Collectors.toList());

        List<SkuInfo> skuInfoList =  productFeignClient.findSkuInfoBySkuIdList(skuIdList);

        map.put("skuInfoList",skuInfoList);

        //活动规则回显
        QueryWrapper<CouponInfo> couponInfoQueryWrapper = new QueryWrapper<>();
        couponInfoQueryWrapper.eq("activity_id",id);

        List<CouponInfo> couponInfoList = couponInfoMapper.selectList(couponInfoQueryWrapper);
        map.put("couponInfoList",couponInfoList);

        return map;
    }
}

package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.ActivityInfoMapper;
import com.atguigu.gmall.activity.mapper.ActivityRuleMapper;
import com.atguigu.gmall.activity.mapper.ActivitySkuMapper;
import com.atguigu.gmall.activity.mapper.CouponInfoMapper;
import com.atguigu.gmall.activity.service.ActivityInfoService;
import com.atguigu.gmall.model.activity.*;
import com.atguigu.gmall.model.cart.CarInfoVo;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.ActivityType;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.assist.ISqlRunner;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.swagger.annotations.ApiModelProperty;
import org.bouncycastle.jcajce.provider.util.SecretKeyUtil;
import org.redisson.misc.Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
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
        activitySkuMapper.delete(new QueryWrapper<ActivitySku>().eq("activity_id", activityRuleVo.getActivityId()));
        //  删除原来的活动规则列表
        activityRuleMapper.delete(new QueryWrapper<ActivityRule>().eq("activity_id", activityRuleVo.getActivityId()));
        //优惠卷列表暂时不写

        //  保存数据：
        List<ActivitySku> activitySkuList = activityRuleVo.getActivitySkuList();
        List<ActivityRule> activityRuleList = activityRuleVo.getActivityRuleList();

        //删除活动与优惠卷绑定关系
        CouponInfo couponInfo = new CouponInfo();
        couponInfo.setActivityId(0L);
        couponInfoMapper.update(couponInfo, new QueryWrapper<CouponInfo>().eq("activity_id", activityRuleVo.getActivityId()));


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
        Map<String, Object> map = new HashMap<>();
        //回显数据: activity_rule ，activity_sku
        QueryWrapper<ActivityRule> activityRuleQueryWrapper = new QueryWrapper<>();
        activityRuleQueryWrapper.eq("activity_id", id);
        List<ActivityRule> activityRuleList = activityRuleMapper.selectList(activityRuleQueryWrapper);

        map.put("activityRuleList", activityRuleList);

        QueryWrapper<ActivitySku> activitySkuQueryWrapper = new QueryWrapper<>();
        activitySkuQueryWrapper.eq("activity_id", id);
        List<ActivitySku> activitySkuList = activitySkuMapper.selectList(activitySkuQueryWrapper);
        //  获取到Id 集合
        List<Long> skuIdList = activitySkuList.stream().map(ActivitySku::getSkuId).collect(Collectors.toList());

        List<SkuInfo> skuInfoList = productFeignClient.findSkuInfoBySkuIdList(skuIdList);

        map.put("skuInfoList", skuInfoList);

        //活动规则回显
        QueryWrapper<CouponInfo> couponInfoQueryWrapper = new QueryWrapper<>();
        couponInfoQueryWrapper.eq("activity_id", id);

        List<CouponInfo> couponInfoList = couponInfoMapper.selectList(couponInfoQueryWrapper);
        map.put("couponInfoList", couponInfoList);

        return map;
    }

    @Override
    public List<ActivityRule> findActivityRule(Long skuId) {
        return activityInfoMapper.selectActivityRuleList(skuId);
    }

    //获取促销活动列表
    @Override
    public List<CarInfoVo> findCartActivityRuleMap(List<CartInfo> cartInfoList, Map<Long, Long> skuIdToActivityIdMap) {

        //已知 cartInfoList； 活动Id 下有哪些skuId ，通过这个skuId 能够找到CartInfo
        //cartInfoList 下面有多少个skuId
        List<CarInfoVo> carInfoVoList = new ArrayList<>();

        //定义map集合，key = skuI= cartInfo
        Map<Long, CartInfo> skuIdToCartInfoMap = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            skuIdToCartInfoMap.put(cartInfo.getSkuId(), cartInfo);
        }
        //获取到skuId集合列表
        List<Long> skuIdList = cartInfoList.stream().map(CartInfo::getSkuId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(skuIdList)) return new ArrayList<>();

        //找到对应的活动规则:这个方法给skuId进行赋值
        List<ActivityRule> activityRuleList = activityInfoMapper.selectCartActivityRuleList(skuIdList);

        //以skuId 进行分组 key = skuId value = List<ActivityRule>
        Map<Long, List<ActivityRule>> skuIdToActivityRuleListMap = activityRuleList.stream().collect(Collectors.groupingBy(activityRule -> activityRule.getSkuId()));

        //  ActivityRule 这个实体类中封装过了一个 skuId
        //  以activityId 进行分组
        //  key = activityId value = List<ActivityRule>
        Map<Long, List<ActivityRule>> activityIdToActivityRuleListAllMap = activityRuleList.stream().collect(Collectors.groupingBy(activityRule -> activityRule.getActivityId()));

        //循环遍历获取数据
        Iterator<Map.Entry<Long, List<ActivityRule>>> iterator = activityIdToActivityRuleListAllMap.entrySet().iterator();
        while (iterator.hasNext()) {
            //获取数据
            Map.Entry<Long, List<ActivityRule>> entry = iterator.next();
            //获取key
            Long activityId = entry.getKey();
            //获取value
            List<ActivityRule> currentActivityRuleList = entry.getValue();

            //  活动规则下有哪些skuId? ActivityRule 这个对象中已经有skuId , 获取skuId ,活动规则对应的skuId
            //  分组的话：不能重复！ 但是返回的是map 集合，我们需要的是skuId 集合列表！
            //  40 ,41 ,42, 43
            Set<Long> activitySkuIdSet = currentActivityRuleList.stream().map(activityRule -> activityRule.getSkuId()).collect(Collectors.toSet());

            //  声明一个对象：CarInfoVo
            CarInfoVo carInfoVo = new CarInfoVo();

            List<CartInfo> cartInfos = new ArrayList<>();
            //有了skuId 循环遍历
            for (Long skuId : activitySkuIdSet) {
                //  记录一下skuId 对应的哪个活动Id  key = skuId value = activityId
                skuIdToActivityIdMap.put(skuId, activityId);
                //获取cartInfo
                CartInfo cartInfo = skuIdToCartInfoMap.get(skuId);
                cartInfos.add(cartInfo);
            }
            carInfoVo.setCartInfoList(cartInfos);

            //  根据skuId 获取对应的活动规则
            //  所有的skuId集合
            List<ActivityRule> ruleList = skuIdToActivityRuleListMap.get(activitySkuIdSet.iterator().next());
            carInfoVo.setActivityRuleList(ruleList);
            //添加数据到集合
            carInfoVoList.add(carInfoVo);
        }
        return carInfoVoList;
    }

    @Override
    public Map<Long, ActivityRule> findTradeActivityRuleMap(List<OrderDetail> orderDetailList) {

        //  声明一个集合来存储活动Id 对应的活动规则！
        Map<Long, ActivityRule> activityIdToActivityRuleMap = new HashMap<>();
        //  声明一个map 来存储skuId 对应的订单明细
        Map<Long, OrderDetail> skuIdToOrderDetailMap = new HashMap<>();
        for (OrderDetail orderDetail : orderDetailList) {
            skuIdToOrderDetailMap.put(orderDetail.getSkuId(), orderDetail);
        }

        //获取skuIdList
        List<Long> skuIdList = orderDetailList.stream().map(OrderDetail::getSkuId).collect(Collectors.toList());

        //获取对应的活动规则集合数据
        List<ActivityRule> activityRuleList = activityInfoMapper.selectCartActivityRuleList(skuIdList);

        //  以skuId分组获取数据
        //  key = skuId , value = List<ActivityRule>
        Map<Long, List<ActivityRule>> skuIdToActivityRuleListMap = activityRuleList.stream().collect(Collectors.groupingBy(activityRule -> activityRule.getSkuId()));

        //  以活动Id 进行分组
        Map<Long, List<ActivityRule>> activityIdToActivityRuleListMap = activityRuleList.stream().collect(Collectors.groupingBy(activityRule -> activityRule.getActivityId()));
        //  循环这个集合activityIdToActivityRuleListMap
        Iterator<Map.Entry<Long, List<ActivityRule>>> iterator = activityIdToActivityRuleListMap.entrySet().iterator();
        while (iterator.hasNext()) {
            //获取里面的数据
            Map.Entry<Long, List<ActivityRule>> entry = iterator.next();
            Long activityId = entry.getKey();
            //获取到活动下对应的规则集合
            List<ActivityRule> currentActivityRuleList = entry.getValue();
            //  获取多动规则下有哪些skuId ,去重复 40，41，42，43
            Set<Long> activitySkuIdSet = currentActivityRuleList.stream().map(activityRule -> activityRule.getSkuId()).collect(Collectors.toSet());
            //记录当前的总金额，还有记录件数
            BigDecimal activityTotalAmount = new BigDecimal("0");
            //  声明这两个参数的目的：区分这个活动是满减，满件，才能计算！
            Integer activityTotalNum = 0;

            //循环遍历
            for (Long skuId : activitySkuIdSet) {
                //  根据这个skuId 能获取到对应的orderDetail;
                OrderDetail orderDetail = skuIdToOrderDetailMap.get(skuId);
                //订单明细的总金额
                BigDecimal skuTotalAmount = orderDetail.getOrderPrice().multiply(new BigDecimal(orderDetail.getSkuNum()));
                //计算这个活动的总金额
                activityTotalAmount = activityTotalAmount.add(skuTotalAmount);
                activityTotalNum += orderDetail.getSkuNum();
            }

            //根据skuId获取到活动规则列表
            List<ActivityRule> skuActivityRuleList = skuIdToActivityRuleListMap.get(activitySkuIdSet.iterator().next());
            //循环遍历
            for (ActivityRule activityRule : skuActivityRuleList) {
                //  "活动类型（1：满减，2：折扣）"
                if (activityRule.getActivityType().equals(ActivityType.FULL_REDUCTION.name())) {
                    //  5000 800 2000 300 1000 100
                    //  订单中总价格>满减规则的价格
                    if (activityTotalAmount.compareTo(activityRule.getConditionAmount()) > -1) {
                        //设置最优规则
                        activityRule.setReduceAmount(activityRule.getBenefitAmount());
                        //设置skuIdList
                        activityRule.setSkuIdList(new ArrayList<>(activitySkuIdSet));
                        //将活动Id对应的最优规则进行保存
                        activityIdToActivityRuleMap.put(activityRule.getActivityId(), activityRule);
                        break;
                    }
                } else {
                    //  折扣 判断：购买的商品总件数 >= 折扣件数
                    if (activityTotalNum >= activityRule.getConditionNum()) {
                        //  8折 1000*0.8 = 1000*8/10  skuDiscountTotalAmount = 800
                        BigDecimal skuDiscountTotalAmount = activityTotalAmount.multiply(activityRule.getBenefitDiscount()).divide(new BigDecimal("10"));
                        //设置优惠价格
                        activityRule.setReduceAmount(activityTotalAmount.subtract(skuDiscountTotalAmount));
                        //设置skuIdList
                        activityRule.setSkuIdList(new ArrayList<>(activitySkuIdSet));
                        //将活动Id对应的最优规则进行保存
                        activityIdToActivityRuleMap.put(activityRule.getActivityId(), activityRule);
                        break;
                    }
                }
            }

        }
        //返回数据
        return activityIdToActivityRuleMap;
    }
}






















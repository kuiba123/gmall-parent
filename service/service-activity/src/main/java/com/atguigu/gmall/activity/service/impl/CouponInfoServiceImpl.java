package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.CouponInfoMapper;
import com.atguigu.gmall.activity.mapper.CouponRangeMapper;
import com.atguigu.gmall.activity.mapper.CouponUseMapper;
import com.atguigu.gmall.activity.service.CouponInfoService;
import com.atguigu.gmall.common.execption.GmallException;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.model.activity.*;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.CouponRangeType;
import com.atguigu.gmall.model.enums.CouponStatus;
import com.atguigu.gmall.model.enums.CouponType;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.product.BaseCategory3;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {

    @Autowired
    private CouponInfoMapper couponInfoMapper;

    @Autowired
    private CouponRangeMapper couponRangeMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CouponUseMapper couponUseMapper;

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

    @Override
    public List<CouponInfo> findCouponInfo(Long skuId, Long activityId, Long userId) {

        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if (skuInfo == null) {
            return new ArrayList<>();
        }

        //获取普通优惠券
        List<CouponInfo> couponInfoList = couponInfoMapper.selectCouponInfoList(skuInfo.getSpuId(), skuInfo.getCategory3Id(), skuInfo.getTmId(), userId);
        //获取活动优惠券
        if (activityId != null) {
            List<CouponInfo> activityCouponInfoList = couponInfoMapper.selectActivityCouponInfoList(skuInfo.getSpuId(), skuInfo.getCategory3Id(), skuInfo.getTmId(), activityId, userId);
            couponInfoList.addAll(activityCouponInfoList);
        }
        return couponInfoList;
    }

    @Override
    public void getCouponInfo(Long couponId, Long userId) {
        //判断优惠劵是否领完
        CouponInfo couponInfo  = this.getById(couponId);
        if (couponInfo.getTakenCount() > couponInfo.getLimitNum()) {
            throw new GmallException(ResultCodeEnum.COUPON_LIMIT_GET);
        }

        //判断该用户是否已领,一个用户只能领用一张
        QueryWrapper<CouponUse> couponUseQueryWrapper = new QueryWrapper<>();
        couponUseQueryWrapper.eq("coupon_id",couponId);
        couponUseQueryWrapper.eq("user_id",userId);

        Integer count = couponUseMapper.selectCount(couponUseQueryWrapper);
        if (count > 0) {
            throw new GmallException(ResultCodeEnum.COUPON_GET);
        }

        //更新领取个数
        int takeCount = couponInfo.getTakenCount().intValue() + 1;
        couponInfo.setTakenCount(takeCount);
        this.updateById(couponInfo);

        CouponUse couponUse = new CouponUse();
        couponUse.setCouponId(couponId);
        couponUse.setUserId(userId);
        couponUse.setCouponStatus(CouponStatus.NOT_USED.name());
        couponUse.setGetTime(new Date());
        couponUse.setExpireTime(couponInfo.getExpireTime());
        couponUseMapper.insert(couponUse);
    }

    @Override
    public IPage<CouponInfo> selectPageByUserId(Page<CouponInfo> couponInfoPage, Long userId) {
        return couponInfoMapper.selectPageByUserId(couponInfoPage,userId);
    }

    @Override
    public Map<Long, List<CouponInfo>> findCartCouponInfo(List<CartInfo> cartInfoList, Map<Long, Long> skuIdToActivityIdMap, Long userId) {

        //  记录优惠券使用范围 spuId,tmId,category3Id 暂时存在到 map 中！
        //  map key=  value= "range:1:" + skuInfo.getSpuId() tmId category3Id  value = skuIdList
        Map<String, List<Long>> rangeToSkuIdMap = new HashMap<>();
        for (CartInfo cartInfo : cartInfoList) {
            //获取对应的skuInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(cartInfo.getSkuId());
            //  skuInfo 获取范围类型用的。
            //  skuInfo 包含spuId,tmId,category3Id;
            this.setRuleData(skuInfo,rangeToSkuIdMap);
        }

        /**
         * rangeType(范围类型)  1:商品(spuId) 2:品类(category3Id) 3:品牌tmId
         * rangeId(范围id) spuId, categoryId , tmId,
         * 同一张优惠券不能包含多个范围类型，同一张优惠券可以对应同一范围类型的多个范围id（即：同一张优惠券可以包含多个spuId）
         * 示例数据：
         * couponId   rangeType   rangeId
         * 1             1             20
         * 1             1             30
         * 2             2             20
         */
        // 通过skuId 获取到skuInfo ,skuInfo 中  spuId, categoryId , tmId 都存在！
        //  声明一个集合来存储skuInfo
        List<SkuInfo> skuInfoList = new ArrayList<>();
        //循环赋值
        for (CartInfo cartInfo : cartInfoList) {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(cartInfo.getSkuId());
            skuInfoList.add(skuInfo);
        }
        if (CollectionUtils.isEmpty(skuInfoList)) return new HashMap<>();

        //  查询优惠券列表我们需要优惠券使用范围类型rangeType  rangeId  userId
        //  这个优惠券中有rangeId 通过这个方法，可以给这个字段进行赋值coupon_range.range_id
        List<CouponInfo> allCouponInfoList = couponInfoMapper.selectCartCouponInfoList(skuInfoList,userId);

        //循环遍历所有的优惠劵集合列表
        for (CouponInfo couponInfo : allCouponInfoList) {
            //获取到对应的优惠劵类型
            String rangeType = couponInfo.getRangeType();
            //获取到对应的rang_id
            Long rangeId = couponInfo.getRangeId();
            //  目的：key = skuId value = List<CouponInfo>
            //  如何知道这个skuId 是否参与了活动！ skuIdToActivityIdMap key = skuId value = activityId

            //优惠劵:活动优惠券{activityId 不为空} debug  + 普通优惠券
            if (couponInfo.getActivityId()!=null) {
                //声明一个skuIdList集合
                List<Long> skuIdList = new ArrayList<>();
                //  skuIdToActivityIdMap 中存储的数据 参加活动的skuId
                Iterator<Map.Entry<Long, Long>> iterator = skuIdToActivityIdMap.entrySet().iterator();
                //循环遍历当前的集合
                while (iterator.hasNext()){
                    Map.Entry<Long, Long> entry = iterator.next();
                    Long skuId = entry.getKey();
                    Long activityId = entry.getValue();

                    //判断你的活动Id 下 有哪些skuId 对应的优惠券 说明是同一个活动！
                    if (couponInfo.getActivityId().intValue() == activityId.intValue()){
                        //  找到skuId了,找到优惠券！
                        //  优惠券：coupon_range.range_type coupon_range.range_id
                        //  判断优惠券的类型： spuId,category3Id,tmId
                        //  通过skuId 获取到skuInfo
                        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
                        //判断优惠劵的范围
                        if (couponInfo.getRangeType().equals(CouponRangeType.SPU.name())) {
                            //  判断rageId
                            if (couponInfo.getRangeId().intValue()==skuInfo.getSpuId().intValue()){
                                //  记录这个skuId 属于哪个范围的优惠券！
                                skuIdList.add(skuId);
                            }
                        }else if (couponInfo.getRangeType().equals(CouponRangeType.TRADEMARK.name())){
                            if (couponInfo.getRangeId().intValue()==skuInfo.getTmId().intValue()){
                                //  记录这个skuId 属于哪个范围的优惠券！
                                skuIdList.add(skuId);
                            }
                        }else {
                            if (couponInfo.getRangeId().intValue()==skuInfo.getCategory3Id().intValue()){
                                //  记录这个skuId 属于哪个范围的优惠券！
                                skuIdList.add(skuId);
                            }
                        }
                    }
                }
                //  属于活动优惠券的！ 将这个skuIdList 集合 赋值给优惠券
                couponInfo.setSkuIdList(skuIdList);
            }else {
                //普通优惠劵
                //  判断使用范围 spuId
                //  一个skuId 对应的优惠券使用范围：
                if (rangeType.equals(CouponRangeType.SPU.name())){
                    //  setRuleData 初始化优惠券使用范围的规则  rangeToSkuIdMap key=spuId,tmId,category3Id ,value=skuIdList
                    //  "range:1:" + skuInfo.getSpuId()  因为 rangeId 这个字段对应的存储 ： spuId,tmId,category3Id
                    couponInfo.setSkuIdList(rangeToSkuIdMap.get("range:1:" + rangeId));
                }else if (rangeType.equals(CouponRangeType.CATAGORY.name())){
                    //  category3Id
                    couponInfo.setSkuIdList(rangeToSkuIdMap.get("range:2:" + rangeId));
                }else {
                    // 判断使用范围 tmId
                    couponInfo.setSkuIdList(rangeToSkuIdMap.get("range:3:" + rangeId));
                }
            }
        }

        //给优惠劵赋值:优惠劵对应skuId列表
        //  目的： map key = skuId  value = List<CouponInfo>
        Map<Long, List<CouponInfo>> skuIdToCouponInfoListMap = new HashMap<>();

        //  循环遍历当前的所有优惠券集合
        for (CouponInfo couponInfo : allCouponInfoList) {
            //  获取到优惠券下对应的skuId集合
            List<Long> skuIdList = couponInfo.getSkuIdList();
            //  遍历当前skuIdList
            for (Long skuId : skuIdList) {
                //  使用skuIdToCouponInfoListMap 这个集合判断
                //  这个集合有对应的skuId 时， key = skuId 这个skuId 对应了多个优惠券
                if (skuIdToCouponInfoListMap.containsKey(skuId)){
                    //  从原有集合中获取到数据 并放入map
                    List<CouponInfo> couponInfoList = skuIdToCouponInfoListMap.get(skuId);
                    couponInfoList.add(couponInfo);
                }else {
                    //  第一次进来就走这！
                    List<CouponInfo> couponInfoList = new ArrayList<>();
                    couponInfoList.add(couponInfo);
                    //  没有skuId 时
                    skuIdToCouponInfoListMap.put(skuId,couponInfoList);
                }
            }
        }
        return skuIdToCouponInfoListMap;
    }

    @Override
    public List<CouponInfo> findTradeCouponInfo(List<OrderDetail> orderDetailList, Map<Long, ActivityRule> activityIdToActivityRuleMap, Long userId) {

        // 优惠券范围规则数据
        Map<String, List<Long>> rangeToSkuIdMap = new HashMap<>();

        //  声明一个map集合来存储skuId,skuInfo
        Map<Long,SkuInfo> skuIdToSkuInfoMap = new HashMap<>();

        // 初始化数据，后续使用
        Map<Long, OrderDetail> skuIdToOrderDetailMap = new HashMap<>();

        //  声明集合对象
        List<SkuInfo> skuInfoList = new ArrayList<>();
        for (OrderDetail orderDetail : orderDetailList) {
            //  远程调用
            SkuInfo skuInfo = productFeignClient.getSkuInfo(orderDetail.getSkuId());
            //  直接赋值！
            skuInfoList.add(skuInfo);
            //  给map 赋值！
            skuIdToSkuInfoMap.put(orderDetail.getSkuId(),skuInfo);
            //  赋值
            skuIdToOrderDetailMap.put(orderDetail.getSkuId(),orderDetail);
            //  初始化优惠券的使用范围规则
            this.setRuleData(skuInfo,rangeToSkuIdMap);
        }

        /**
         * rangeType(范围类型)  1:商品(spuid) 2:品类(三级分类id) 3:品牌
         * rangeId(范围id)
         * 同一张优惠券不能包含多个范围类型，同一张优惠券可以对应同一范围类型的多个范围id（即：同一张优惠券可以包含多个spuId）
         * 示例数据：
         * couponId   rangeType   rangeId
         * 1             1             20 小米 skuId 12,13,14
         * 1             1             30 苹果 skuId 18
         * 2             1             20
         */

        if(CollectionUtils.isEmpty(skuInfoList)) return new ArrayList<>();

        //  查询所有优惠券列表 要获取到skuInfo中有tmId,category3Id,spuId
        //  这些优惠券一定的未使用过的！
        List<CouponInfo> allCouponInfoList = couponInfoMapper.selectTradeCouponInfoList(skuInfoList, userId);
        //  优惠券列表： 普通优惠券 + 活动优惠券！ 记录到优惠券使用范围规则中：
        for (CouponInfo couponInfo : allCouponInfoList) {
            //  获取rangeId
            Long rangeId = couponInfo.getRangeId();
            //  获取使用类型
            String rangeType = couponInfo.getRangeType();
            if (couponInfo.getActivityId()!=null) {
                //   //  ActivityRule activityRule = activityIdToActivityRuleMap.get(couponInfo.getActivityId());
                // 根据活动Id 获取到活动规则集合
                Iterator<Map.Entry<Long, ActivityRule>> iterator = activityIdToActivityRuleMap.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<Long, ActivityRule> entry = iterator.next();
                    Long activityId = entry.getKey();
                    ActivityRule activityRule = entry.getValue();
                    //  判断是否属于同一个活动
                    if (couponInfo.getActivityId().intValue() == activityId.intValue()) {
                        //  这个活动Id下 有哪些skuId
                        List<Long> activitySkuIdList = activityRule.getSkuIdList();
                        //  声明一个集合来存储skuId
                        List<Long> skuIdList = new ArrayList<>();
                        //  循环遍历
                        for (Long skuId : activitySkuIdList) {
                            //  获取skuInfo 中 有 tmId,spuId,category3Id;
                            //  存储一个集合map  key = skuId value skuInfo;
                            SkuInfo skuInfo = skuIdToSkuInfoMap.get(skuId);
                            //  判断使用范围
                            if (rangeType.equals(CouponRangeType.SPU.name())) {
                                if (skuInfo.getSpuId().longValue() == rangeId.longValue()) {
                                    //  将对应的skuId 记录起来！
                                    skuIdList.add(skuId);
                                }
                            } else if (rangeType.equals(CouponRangeType.CATAGORY.name())) {
                                if (skuInfo.getCategory3Id().longValue() == rangeId.longValue()) {
                                    skuIdList.add(skuId);
                                }
                            } else {
                                if (skuInfo.getTmId().longValue() == rangeId.longValue()) {
                                    skuIdList.add(skuId);
                                }
                            }
                        }
                        //  赋值
                        couponInfo.setSkuIdList(skuIdList);
                    }
                }
            }else {
                //  没有活动的优惠券！
                if(rangeType.equals(CouponRangeType.SPU.name())) {
                    //  获取对应的skuId 集合
                    couponInfo.setSkuIdList(rangeToSkuIdMap.get("range:1:" + rangeId));
                } else if (rangeType.equals(CouponRangeType.CATAGORY.name())) {
                    couponInfo.setSkuIdList(rangeToSkuIdMap.get("range:2:" + rangeId));
                } else {
                    couponInfo.setSkuIdList(rangeToSkuIdMap.get("range:3:" + rangeId));
                }
            }
        }

        //  找到优惠券对应的skuId 集合！
        //  同一张优惠劵可能对应着所有sku列表 !  12,13,14,18
        //  外层声明一个集合来存储最新的优惠券列表！
        List<CouponInfo> resultCouponInfoList = new ArrayList<>();

        //  以优惠券Id 进行分组  key = couponInfo.id value = List<CouponInfo>
        Map<Long, List<CouponInfo>> couponIdToListMap = allCouponInfoList.stream().collect(Collectors.groupingBy(couponInfo -> couponInfo.getId()));
        //  循环遍历这个集合
        Iterator<Map.Entry<Long, List<CouponInfo>>> iterator = couponIdToListMap.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Long, List<CouponInfo>> entry = iterator.next();
            //  获取数据
            Long couponInfoId = entry.getKey();
            //  获取当前优惠券Id 对应的优惠券列表
            //  优惠券 1             1             20
            List<CouponInfo> couponInfoList = entry.getValue();

            //  声明一个skuIdList 集合
            //  12,13,14,18
            List<Long> skuIdList = new ArrayList<>();
            //  循环couponInfoList
            for (CouponInfo couponInfo : couponInfoList) {
                //  第一次：1             1             20 小米 skuId 12,13,14
                //  第二次：1             1             30 苹果 skuId 18
                skuIdList.addAll(couponInfo.getSkuIdList());
            }
            //  获取到当前的优惠券赋值skuIdList 集合
            CouponInfo couponInfo = couponInfoList.get(0);
            couponInfo.setSkuIdList(skuIdList); // 12,13,14,18
            //  这个优惠券 做了变更！
            resultCouponInfoList.add(couponInfo);
        }
        //  计算：优惠券的最优规则！
        //  购物券类型 1 现金券 2 折扣券 3 满减券 4 满件打折券

        // 记录最优选项金额
        BigDecimal checkeAmount = new BigDecimal("0");
        //记录最优优惠券
        CouponInfo checkeCouponInfo = null;
        //  循环遍历所有优惠券列表  1. 记录总金额，2. 计算有多少个！
        for (CouponInfo couponInfo : resultCouponInfoList) {
            //  获取对应的skuId
            List<Long> skuIdList = couponInfo.getSkuIdList();
            //  记录总金额
            BigDecimal totalAmount = new BigDecimal("0");
            //该优惠券对应的购物项总个数
            int totalNum = 0;
            //  循环判断
            for (Long skuId : skuIdList) {
                //  通过这个skuId 获取到当前的orderDetail
                OrderDetail orderDetail = skuIdToOrderDetailMap.get(skuId);
                //  计算每个orderDetail 的金额
                BigDecimal skuAmount = orderDetail.getOrderPrice().multiply(new BigDecimal(orderDetail.getSkuNum()));

                //  需要将每次遍历的数据都统计在一起！
                totalAmount = totalAmount.add(skuAmount);
                //  总数量
                totalNum += orderDetail.getSkuNum();
            }

            /**
             * reduceAmount: 优惠后减少金额
             * isChecked:    是否最优选项（1：最优）
             * isSelect:     是否可选（1：满足优惠券使用条件，可选）（如：满减 100减10 200减30 500减70，当前可选：满减 100减10、200减30）
             */
            // 优惠后减少金额
            BigDecimal reduceAmount = new BigDecimal("0");
            // 购物券类型 1 现金券 2 折扣券 3 满减券 4 满件打折券
            if(couponInfo.getCouponType().equals(CouponType.CASH.name())) {
                reduceAmount = couponInfo.getBenefitAmount();
                // 标记可选
                couponInfo.setIsSelect(1);
            } else if (couponInfo.getCouponType().equals(CouponType.DISCOUNT.name())) {
                BigDecimal skuDiscountTotalAmount = totalAmount.multiply(couponInfo.getBenefitDiscount().divide(new BigDecimal("10")));
                reduceAmount = totalAmount.subtract(skuDiscountTotalAmount);
                //标记可选
                couponInfo.setIsSelect(1);
            }  else if (couponInfo.getCouponType().equals(CouponType.FULL_REDUCTION.name())) {
                if (totalAmount.compareTo(couponInfo.getConditionAmount()) > -1) {
                    reduceAmount = couponInfo.getBenefitAmount();
                    //标记可选
                    couponInfo.setIsSelect(1);
                }
            } else {
                if(totalNum >= couponInfo.getConditionNum().intValue()) {
                    BigDecimal skuDiscountTotalAmount1 = totalAmount.multiply(couponInfo.getBenefitDiscount().divide(new BigDecimal("10")));
                    reduceAmount = totalAmount.subtract(skuDiscountTotalAmount1);
                    //标记可选
                    couponInfo.setIsSelect(1);
                }
            }
            //  reduceAmount 计算最优的价格，checkeAmount 选中的最优金额
            if (reduceAmount.compareTo(checkeAmount) > 0) {
                checkeAmount = reduceAmount;
                checkeCouponInfo = couponInfo;
            }
            // 优惠后减少金额
            couponInfo.setReduceAmount(reduceAmount);
        }
        //如果最优优惠劵存在，则设置为默认选中
        if(null != checkeCouponInfo) {
            for(CouponInfo couponInfo : resultCouponInfoList) {
                //  根据优惠券Id 进行比较，找出我们计算最优的哪个优惠券！
                if(couponInfo.getId().longValue() == checkeCouponInfo.getId().longValue()) {
                    //  要将最优的哪个优惠券选中！
                    couponInfo.setIsChecked(1);
                }
            }
        }
        return resultCouponInfoList;
    }

    //设置优惠券对应的存储规则！ 做个初始化操作。
    private void setRuleData(SkuInfo skuInfo, Map<String, List<Long>> rangeToSkuIdMap) {
        String key1 = "range:1:" + skuInfo.getSpuId(); // 1,2,3,4,5  add(6);
        if (rangeToSkuIdMap.containsKey(key1)) {
            //获取对应的数据
            List<Long> skuIdList = rangeToSkuIdMap.get(key1);
            skuIdList.add(skuInfo.getId());
        }else {
            //  说明没有这个key，声明一个集合将skuId 添加进去，并保存到map 集合中！  skuId=40
            List<Long> skuIdList = new ArrayList<>();
            skuIdList.add(skuInfo.getId());
            rangeToSkuIdMap.put(key1,skuIdList);
        }
        //  范围 category3Id
        String key2 = "range:2:" + skuInfo.getCategory3Id();  // skuId = 40
        if (rangeToSkuIdMap.containsKey(key2)){
            //  获取对应的数据
            List<Long> skuIdList = rangeToSkuIdMap.get(key2);
            //  将对应的skuId skuInfo.getId();
            skuIdList.add(skuInfo.getId());
        }else {
            //  说明没有这个key，声明一个集合将skuId 添加进去，并保存到map 集合中！
            List<Long> skuIdList = new ArrayList<>();
            skuIdList.add(skuInfo.getId());
            rangeToSkuIdMap.put(key2,skuIdList);
        }
        //  范围 tmId
        String key3 = "range:3:" + skuInfo.getTmId();   // skuId = 40;
        if(rangeToSkuIdMap.containsKey(key3)) {
            List<Long> skuIdList = rangeToSkuIdMap.get(key3);
            skuIdList.add(skuInfo.getId());
        } else {
            List<Long> skuIdList = new ArrayList<>();
            skuIdList.add(skuInfo.getId());
            rangeToSkuIdMap.put(key3, skuIdList);
        }
    }
}

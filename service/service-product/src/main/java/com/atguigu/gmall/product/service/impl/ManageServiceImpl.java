package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;

    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitService rabbitService;

    @Override
    public List<BaseCategory1> getCategory1() {
        return baseCategory1Mapper.selectList(null);
    }

    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        return baseCategory2Mapper.selectList(new QueryWrapper<BaseCategory2>().eq("category1_id",category1Id));
    }

    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        return baseCategory3Mapper.selectList(new QueryWrapper<BaseCategory3>().eq("category2_id",category2Id));

    }

    @Override
    public List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id) {
        return baseAttrInfoMapper.selectBaseAttrInfoList(category1Id,category2Id,category3Id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        if (baseAttrInfo.getId() != null ) {
            //修改数据
            baseAttrInfoMapper.updateById(baseAttrInfo);
        }else{
            baseAttrInfoMapper.insert(baseAttrInfo);
        }
        baseAttrValueMapper.delete(new QueryWrapper<BaseAttrValue>().eq("attr_id", baseAttrInfo.getId()));

        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (!CollectionUtils.isEmpty(attrValueList)) {
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }

    }

    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {

        return baseAttrValueMapper.selectList(new QueryWrapper<BaseAttrValue>().eq("attr_id",attrId));

    }

    @Override
    public BaseAttrInfo getAttrInfo(Long attrId) {

        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        if (baseAttrInfo != null) {
            baseAttrInfo.setAttrValueList(getAttrValueList(attrId));
        }
        return baseAttrInfo;
    }

    @Override
    public IPage getSpuInfoPage(Page<SpuInfo> pageParam, SpuInfo spuInfo) {

        QueryWrapper<SpuInfo> queryWrapper  = new QueryWrapper<SpuInfo>()
                .eq("category3_id", spuInfo.getCategory3Id())
                .orderByDesc("id");


        return spuInfoMapper.selectPage(pageParam,queryWrapper);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {

        return baseSaleAttrMapper.selectList(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSpuInfo(SpuInfo spuInfo) {

        spuInfoMapper.insert(spuInfo);

        //获取前台传递过来的spuImage列表
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }

        //获取销售属性:spuSaleAttr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insert(spuSaleAttr);

                //获取销售属性值:spuSaleAttrValue
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());
                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);
                    }
                }
            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(Long spuId) {

        return spuImageMapper.selectList(new QueryWrapper<SpuImage>().eq("spu_id",spuId));

    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttr(Long spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {

        /*
        skuInfo;
        skuImage;
        skuSaleAttrValue; 记录sku 与 销售属性值的关系
        skuAttrValue; 记录sku 与 平台属性值的关系
         */

        skuInfoMapper.insert(skuInfo);

        //获取图片集合
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (!CollectionUtils.isEmpty(skuImageList)) {
            for (SkuImage skuImage : skuImageList) {
                skuImage.setSkuId(skuInfo.getId());
                skuImageMapper.insert(skuImage);
            }
        }

        //获取数据:skuSaleAttrValue
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());
                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            }
        }

        //先获取数据:skuAttrValue
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (!CollectionUtils.isEmpty(skuAttrValueList)){
            // 循环遍历
            for (SkuAttrValue skuAttrValue : skuAttrValueList) {
                //  赋值skuId sku_attr_value.sku_id = sku_info.id
                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);
            }
        }

    }


    @Override
    public IPage<SkuInfo> getSkuInfoList(Page<SkuInfo> skuInfoPage) {
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.orderByDesc("id");
        return skuInfoMapper.selectPage(skuInfoPage,skuInfoQueryWrapper);
    }

    @Override
    public void onSale(Long skuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(1);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);

        //  发送消息 给service-list 调用商品的上架方法，
        //  发送的消息：
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_UPPER,skuId);
    }

    @Override
    public void cancelSale(Long skuId) {
        // update sku_info set is_sale = 0 where id = skuId 其他字段不会有变化！
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setIsSale(0);
        skuInfo.setId(skuId);
        skuInfoMapper.updateById(skuInfo);

        //发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_GOODS,MqConst.ROUTING_GOODS_LOWER,skuId);

    }

    @Override
    @GmallCache(prefix = "skuInfo:")
    public SkuInfo getSkuInfo(Long skuId) {

        /**
        * if(true){
         *     //获取缓存
         * }else{
         *     //获取数据库 getSkuInfoDB(skuId)
         *     //放入缓存
         * }
         * //如果redis宕机了,redisTemplate就不存在了,使用数据库兜底!
        */
        return getSkuInfoDB(skuId);
    }

    //使用redis做分布式锁
    private SkuInfo getSkuInfoRedis(Long skuId){

        SkuInfo skuInfo = null;

        try {
            //获取缓存key
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //获取缓存数据:skuInfo
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //判断获取数据是否为空
            if (skuInfo == null) {
                //为空，则直接从数据库获取，但是直接获取可能造成缓存击穿,先加锁
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                //定义锁的值
                String uuid = UUID.randomUUID().toString().replace("-", "");
                //上锁
                Boolean isExist = redisTemplate.opsForValue().setIfAbsent(lockKey,uuid,RedisConst.SKULOCK_EXPIRE_PX2,TimeUnit.SECONDS);
                if (isExist) {
                    //执行成功,上锁
                    System.out.println("获取到分布式锁");
                    //防止数据穿透(没有这个数据)
                    skuInfo = getSkuInfoDB(skuId);
                    if (skuInfo == null) {
                        //此时数据为空,加一条空数据防止报错
                        SkuInfo skuInfo1 = new SkuInfo();
                        redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        return skuInfo1;
                    }else {
                        //数据库中获取到值,存缓存中
                        redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                        //解锁:使用lua脚本解锁
                        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                        //设置lua脚本的数据类型
                        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                        //设置lua脚本返回类型为Long
                        redisScript.setResultType(Long.class);
                        redisScript.setScriptText(script);
                        //删除key对应的value
                        redisTemplate.execute(redisScript, Arrays.asList(uuid));
                        return skuInfo;
                    }
                }else{
                    //其他线程等待
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else{
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //防止缓存宕机,直接从数据库获取数据
        return getSkuInfoDB(skuId);
    }

    //使用Redisson做为分布式锁
    private SkuInfo getSKuInfoByRedisson(Long skuId){

        SkuInfo skuInfo = null;
        //获取缓存key  skuKey sku:skuId:info
        try {
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //通过key获取缓存数据
            skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //如果没有数据，先加锁
            if (skuInfo == null) {
                //使用redisson来解决
                //定义一个分布锁的key = sku:skuId:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                //得到锁
                RLock lock = redissonClient.getLock(lockKey);
                //尝试加锁,最多等待1秒,上锁后1秒自动解锁
                boolean flag = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                //查看锁是否存在
                if (flag) {
                    try {
                        //加锁成功，就从数据库获取数据
                        skuInfo = getSkuInfoDB(skuId);
                        //防止缓存穿透,查看是否存在数据
                        if (skuInfo == null) {
                            SkuInfo skuInfo1 = new SkuInfo();//这个对象有地址,对象的属性值是空的
                            //这个数据是不存在的
                            redisTemplate.opsForValue().set(skuKey,skuInfo1,RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                            //返回
                            return skuInfo1;
                        }else{
                            //数据不为空，将数据放入缓存
                            redisTemplate.opsForValue().set(skuKey,skuInfo,RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                            return skuInfo;
                        }
                    } finally {
                        //解锁
                        lock.unlock();
                    }
                }else{
                    //如果没锁就重新执行获取缓存key方法
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else{
                //缓存有数据直接返回
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //redis宕机，兜底直接从数据库获取
        return getSkuInfoDB(skuId);
    }


    //直接从数据库获取skuInfo数据
    private SkuInfo getSkuInfoDB(Long skuId) {
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

        //根据skuId获取图片列表集合
        QueryWrapper<SkuImage> queryWrapper = new QueryWrapper<SkuImage>().eq("sku_id", skuId);
        List<SkuImage> skuImageList = skuImageMapper.selectList(queryWrapper);
        skuInfo.setSkuImageList(skuImageList);
        return skuInfo;
    }

    @Override
    @GmallCache(prefix = "categoryView:")
    public BaseCategoryView getCategoryViewByCategory3Id(Long category3Id) {

        return baseCategoryViewMapper.selectById(category3Id);
    }

    @Override
    @GmallCache(prefix = "skuPrice:")
    public BigDecimal getSkuPrice(Long skuId) {

        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        if (skuInfo != null) {
            return skuInfo.getPrice();
        }
        return new BigDecimal("0");
    }

    @Override
    @GmallCache(prefix = "SpuSaleAttrList:")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    @Override
    @GmallCache(prefix = "SkuIdsMap:")
    public Map<Object, Object> getSkuValueIdsMap (Long spuId) {
        Map<Object, Object> map = new HashMap<>();
        //map要存储数据,应该是从数据库中获取执行sql语句
        //key = 125|123,value = 37
        List<Map> mapList = skuSaleAttrValueMapper.selectSaleAttrValuesBySpu(spuId);
        if (!CollectionUtils.isEmpty(mapList)) {
            //循环遍历
            for (Map skuMap : mapList) {
                //  {"106|110":40,"107|110":41}
                map.put(skuMap.get("value_ids"),
                        skuMap.get("sku_id"));
            }
        }
        return map;
    }

    @Override
    @GmallCache(prefix = "index:")
    public List<JSONObject> getBaseCategoryList() {
        //声明一个集合
        List<JSONObject> jsonObjectList = new ArrayList<>();
        /**
        * 1.获取到所有的分类名称+分类Id
         * 2.构建Json格式的数据
        */
        List<BaseCategoryView> baseCategoryViewList = baseCategoryViewMapper.selectList(null);
        //  以一级分类Id 进行分组 Collectors.groupingBy(BaseCategoryView::getCategory1Id)
        //  key = category1Id value = List<BaseCategoryView>
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViewList.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //声明一个index
        int index = 1;
        //循环遍历当前的集合category1Map
        for (Map.Entry<Long, List<BaseCategoryView>> entry : category1Map.entrySet()){
            //获取一级分类id
            Long category1Id = entry.getKey();
            //获取到一级分类Id下集合数据
            List<BaseCategoryView> categoryViewList1 = entry.getValue();
            //创建一个对象
            JSONObject category1 = new JSONObject();
            category1.put("index",index);
            category1.put("categoryId",category1Id);
            //当以分类Id进行分组后,后面的所有分类名称都一样
            category1.put("categoryName",categoryViewList1.get(0).getCategory1Name());
            index ++;

            //  获取二级分类数据：以二级分类Id 进行分组获取数据
            Map<Long, List<BaseCategoryView>> category2Map = categoryViewList1.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //  声明一个集合来存储二级分类的数据
            List<JSONObject> category2Child = new ArrayList<>();
            //  循环变量
            for (Map.Entry<Long, List<BaseCategoryView>> entry1 : category2Map.entrySet()) {
                //  获取二级分类Id
                Long category2Id = entry1.getKey();

                // 获取二级分类Id 下的所有数据
                List<BaseCategoryView> categoryViewList2 = entry1.getValue();

                //  声明对象
                JSONObject category2 = new JSONObject();
                category2.put("categoryId",category2Id);
                category2.put("categoryName",categoryViewList2.get(0).getCategory2Name());

                //  将二级分类对象添加到集合中
                category2Child.add(category2);

                //  声明一个集合来存储三级分类的数据
                List<JSONObject> category3Child = new ArrayList<>();
                //  获取三级分类数据
                // Consumer void accept(T t)
                categoryViewList2.stream().forEach((category3View)->{
                    //  声明三级分类对象
                    JSONObject category3 = new JSONObject();
                    category3.put("categoryId",category3View.getCategory3Id());
                    category3.put("categoryName",category3View.getCategory3Name());
                    //  需要将三级分类对象添加到集合
                    category3Child.add(category3);
                });
                //  将三级分类集合放入二级
                category2.put("categoryChild",category3Child);
            }
            //  将二级分类集合放入一级
            category1.put("categoryChild",category2Child);

            //  需要将所有的一级分类数据添加到集合
            jsonObjectList.add(category1);
        }
        return jsonObjectList;
    }


    @Override
    public BaseTrademark getTrademarkByTmId(Long tmId) {
        return baseTrademarkMapper.selectById(tmId);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return baseAttrInfoMapper.selectBaseAttrInfoListBySkuId(skuId);
    }

    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        QueryWrapper<SkuInfo> skuInfoQueryWrapper = new QueryWrapper<>();
        skuInfoQueryWrapper.like("sku_name",keyword);
        return skuInfoMapper.selectList(skuInfoQueryWrapper);
    }

    @Override
    public List<SkuInfo> findSkuInfoBySkuIdList(List<Long> skuIdList) {
        return skuInfoMapper.selectBatchIds(skuIdList);
    }

    @Override
    public List<SpuInfo> findSpuInfoByKeyword(String keyword) {
        QueryWrapper<SpuInfo> spuInfoQueryWrapper = new QueryWrapper<>();
        spuInfoQueryWrapper.like("spu_name",keyword);
        return spuInfoMapper.selectList(spuInfoQueryWrapper);
    }

    @Override
    public List<SpuInfo> findSpuInfoBySpuIdList(List<Long> spuIdList) {
        return spuInfoMapper.selectBatchIds(spuIdList);
    }

    @Override
    public List<BaseCategory3> findBaseCategory3ByCategory3IdList(List<Long> category3IdList) {
        return baseCategory3Mapper.selectBatchIds(category3IdList);
    }


}




































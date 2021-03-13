package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ListFeignClient listFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {

        Map<String, Object> result  = new HashMap<>();

        //通过skuId查询skuInfo
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(()->{
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            //保存skuInfo
            result.put("skuInfo",skuInfo);
            return skuInfo;
        },threadPoolExecutor);


        //获取商品分类
        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            //保存商品分类数据
            result.put("categoryView",categoryView);
        },threadPoolExecutor);

        //获取商品最新价格
        //获取商品最新价格
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            result.put("price", skuPrice);
        },threadPoolExecutor);

        //销售属性-销售属性值回显并锁定
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
            //保存数据
            result.put("spuSaleAttrList",spuSaleAttrList);
        },threadPoolExecutor);

        //根据spuId查询map集合属性
        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo ->{
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            //保存json字符串
            String mapJson = JSON.toJSONString(skuValueIdsMap);
            System.out.println("mapJson = " + mapJson);
            //保存valuesSkuJson
            result.put("valuesSkuJson",mapJson);
        },threadPoolExecutor);

        //更新商品incrHotScore
        CompletableFuture<Void> incrHotScoreCompletableFuture  = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        },threadPoolExecutor);


        //任务组合
        CompletableFuture.allOf(
                skuInfoCompletableFuture,
                spuSaleAttrCompletableFuture,
                skuValueIdsMapCompletableFuture,
                skuPriceCompletableFuture,
                categoryViewCompletableFuture,
                incrHotScoreCompletableFuture).join();
        return result;
    }
}

package com.atguigu.gmall.product.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class ProductDegradeFeignClient implements ProductFeignClient {

    //如果远程调用微服务的话，会走这个实现类
    @Override
    public SkuInfo getSkuInfo(Long skuId) {
        return null;
    }

    @Override
    public BaseCategoryView getCategoryView(Long category3Id) {
        return null;
    }

    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        return null;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {
        return null;
    }

    @Override
    public Map getSkuValueIdsMap(Long spuId) {
        return null;
    }

    @Override
    public Result getBaseCategoryList() {
        return null;
    }

    @Override
    public BaseTrademark getTrademark(Long tmId) {
        return null;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        return null;
    }

    @Override
    public List<SkuInfo> findSkuInfoByKeyword(String keyword) {
        return null;
    }

    @Override
    public List<SkuInfo> findSkuInfoBySkuIdList(List<Long> skuIdList) {
        return null;
    }

    @Override
    public List<SpuInfo> findSpuInfoBySpuIdList(List<Long> spuIdList) {
        return null;
    }

    @Override
    public List<BaseCategory3> findBaseCategory3ByCategory3IdList(List<Long> category3IdList) {
        return null;
    }

    @Override
    public List<BaseTrademark> findBaseTrademarkByTrademarkIdList(List<Long> trademarkIdList) {
        return null;
    }
}

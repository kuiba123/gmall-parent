package com.atguigu.gmall.product.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.impl.ProductDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(value = "service-product",fallback = ProductDegradeFeignClient.class)
public interface ProductFeignClient {

    /*
    * 根据skuId获取sku信息
    * 发布数据接口
    * @param skuId:
    * @return: com.atguigu.gmall.model.product.SkuInfo
    */
    @GetMapping("api/product/inner/getSkuInfo/{skuId}")
    SkuInfo getSkuInfo(@PathVariable Long skuId);

    /**
     * 通过三级分类id查询分类信息
     * @param category3Id
     * @return
     */
    @GetMapping("api/product/inner/getCategoryView/{category3Id}")
    BaseCategoryView getCategoryView(@PathVariable Long category3Id);

    /**
     * 获取sku最新价格
     *
     * @param skuId
     * @return
     */
    @GetMapping("api/product/inner/getSkuPrice/{skuId}")
    BigDecimal getSkuPrice(@PathVariable Long skuId);

    /**
     * 根据spuId，skuId 查询销售属性集合
     *
     * @param skuId
     * @param spuId
     * @return
     */
    @GetMapping("api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId, @PathVariable Long spuId);

    /**
     * 根据spuId 查询map 集合属性
     * @param spuId
     * @return
     */
    @GetMapping("api/product/inner/getSkuValueIdsMap/{spuId}")
    Map getSkuValueIdsMap(@PathVariable Long spuId);


    /*
    * 获取全部分类信息
    * @return: com.atguigu.gmall.common.result.Result
    */
    @GetMapping("/api/product/getBaseCategoryList")
    Result getBaseCategoryList();

    /**
     * 通过品牌Id 集合来查询数据
     * @param tmId
     * @return
     */
    @GetMapping("/api/product/inner/getTrademark/{tmId}")
    BaseTrademark getTrademark(@PathVariable("tmId")Long tmId);

    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */
    @GetMapping("/api/product/inner/getAttrList/{skuId}")
    List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId);

    /*
    * 根据关键字获取sku列表，活动使用
    */
    @GetMapping("/api/product/inner/findSkuInfoByKeyword/{keyword}")
    List<SkuInfo> findSkuInfoByKeyword(@PathVariable("keyword") String keyword);

    @PostMapping("/api/product/inner/findSkuInfoBySkuIdList")
    List<SkuInfo> findSkuInfoBySkuIdList(List<Long> skuIdList);

    @PostMapping("/admin/product/inner/findSpuInfoBySpuIdList")
    List<SpuInfo> findSpuInfoBySpuIdList(@RequestBody List<Long> spuIdList);

    @PostMapping("/admin/product/inner/findBaseCategory3ByCategory3IdList")
    List<BaseCategory3> findBaseCategory3ByCategory3IdList(@RequestBody List<Long> category3IdList);

    @PostMapping("/admin/product/baseTrademark/inner/findBaseTrademarkByTrademarkIdList")
    List<BaseTrademark> findBaseTrademarkByTrademarkIdList(@RequestBody List<Long> trademarkIdList);



}

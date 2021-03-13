package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public interface ManageService {


   /*
   * 获取全部分类信息
   * @return: java.util.List<com.alibaba.fastjson.JSONObject>
   */
   List<JSONObject> getBaseCategoryList();


   /*
   * 查询所有的一级分类信息
   * @param null:
   * @return: null
   */
   List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类Id 查询二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类Id 查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类Id 获取平台属性数据
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> getAttrValueList(Long attrId);

    BaseAttrInfo getAttrInfo(Long attrId);

    /*
    * spu分页查询
    * @param pageParam:
    * @param spuInfo:
    * @return: null
    */
    IPage<SpuInfo> getSpuInfoPage(Page<SpuInfo> pageParam, SpuInfo spuInfo);

    /*
    * 查询所有的销售属性数据
    * @param null:
    * @return: null
    */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /*
    * 保存spuInfo
    * @param spuInfo:
    * @return: void
    */
    void saveSpuInfo(SpuInfo spuInfo);

    /*
    * 根据spuId查询图片列表
    * @param spuId:
    * @return: java.util.List<com.atguigu.gmall.model.product.SpuImage>
    */
    List<SpuImage> getSpuImageList(Long spuId);

    /*
    * 根据spuId获取销售属性
    * @param spuId:
    * @return: java.util.List<com.atguigu.gmall.model.product.SpuSaleAttr>
    */
    List<SpuSaleAttr> getSpuSaleAttr(Long spuId);

    /*
    * 查询skuInfo列表数据
    * @param skuInfoPage:
    * @return: com.baomidou.mybatisplus.core.metadata.IPage<com.atguigu.gmall.model.product.SkuInfo>
    */
    IPage<SkuInfo> getSkuInfoList(Page<SkuInfo> skuInfoPage);

    void saveSkuInfo(SkuInfo skuInfo);

    void onSale(Long skuId);

    void cancelSale(Long skuId);

     /*
     * 根据skuId查询skuInfo
     * @param skuId:
     * @return: com.atguigu.gmall.model.product.SkuInfo
     */
     SkuInfo getSkuInfo(Long skuId);

     /*
     * 通过三级分类id查询分类信息
     * @param category3Id:
     * @return: com.atguigu.gmall.model.product.BaseCategoryView
     */
     BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

     /*
     * 根据skuId获取sku价格
     * @param skuId:
     * @return: java.math.BigDecimal
     */
     BigDecimal getSkuPrice(Long skuId);

     /*
     *  根据spuId,skuId查询销售属性集合
     * @param skuId:
      * @param spuId:
     * @return: java.util.List<com.atguigu.gmall.model.product.SpuSaleAttr>
     */
     List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId,Long spuId);

     /*
     * 根据spuId获取到销售属性值id与skuId组成的集合
     * @param spuId:
     * @return: java.util.Map
     */
     Map<Object,Object> getSkuValueIdsMap(Long spuId);

     /*
     * 通过品牌Id,查询数据
     * @param tmId:
     * @return: com.atguigu.gmall.model.product.BaseTrademark
     */
     BaseTrademark getTrademarkByTmId(Long tmId);

     /*
     * 通过skuId集合查询数据
     * @param skuId:
     * @return: java.util.List<com.atguigu.gmall.model.product.BaseAttrInfo>
     */
     List<BaseAttrInfo> getAttrList(Long skuId);

     List<SkuInfo> findSkuInfoByKeyword(String keyword);

     List<SkuInfo> findSkuInfoBySkuIdList(List<Long> skuIdList);

     List<SpuInfo> findSpuInfoByKeyword(String keyword);

     List<SpuInfo> findSpuInfoBySpuIdList(List<Long> spuIdList);

     List<BaseCategory3> findBaseCategory3ByCategory3IdList(List<Long> category3IdList);
}


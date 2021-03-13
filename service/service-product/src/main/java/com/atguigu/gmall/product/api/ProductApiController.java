package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Api(tags = "api接口文档")
@RestController
@RequestMapping("api/product")
public class ProductApiController {

    @Autowired
    private ManageService manageService;

    /*
    * 根据skuId获取Sku信息
    * @param skuId:
    * @return: com.atguigu.gmall.model.product.SkuInfo
    */
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getAttrValueList(@PathVariable Long skuId){

        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        return skuInfo;
    }

    /*
    * 通过三级分类id查询分类信息
    * @param category3Id:
    * @return: com.atguigu.gmall.model.product.BaseCategoryView
    */
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return manageService.getCategoryViewByCategory3Id(category3Id);
    }

    /*
    * 获取sku最新价格
    * @param skuId:
    * @return: java.math.BigDecimal
    */
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return manageService.getSkuPrice(skuId);
    }

    /*
    * 根据spuId,skuId查询销售属性集合
    * @param skuId:
     * @param spuId:
    * @return: java.util.List<com.atguigu.gmall.model.product.SpuSaleAttr>
    */
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId){
        return manageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    /*
    * 根据spuId获取销售属性值与skuId组合成的map数据
    * @param spuId:
    * @return: java.util.Map
    */
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        return manageService.getSkuValueIdsMap(spuId);
    }

    //获取首页数据
    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList(){
        List<JSONObject> baseCategoryList = manageService.getBaseCategoryList();
        return Result.ok(baseCategoryList);
    }

    /**
     * 通过品牌Id 集合来查询数据
     * @param tmId
     * @return
     */
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable("tmId") Long tmId){
        return manageService.getTrademarkByTmId(tmId);
    }

    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable("skuId") Long skuId){
        return manageService.getAttrList(skuId);
    }

    @GetMapping("inner/findSkuInfoByKeyword/{keyword}")
    public List<SkuInfo> findSkuInfoByKeyword(@PathVariable("keyword") String keyword){
        return manageService.findSkuInfoByKeyword(keyword);
    }

    @PostMapping("inner/findSkuInfoBySkuIdList")
    public List<SkuInfo> findSkuInfoBySkuIdList(@RequestBody List<Long> skuIdList){
        return manageService.findSkuInfoBySkuIdList(skuIdList);
    }
}

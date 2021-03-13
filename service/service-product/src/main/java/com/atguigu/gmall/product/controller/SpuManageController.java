package com.atguigu.gmall.product.controller;


import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "SPU后台接口管理")
@RestController
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    @GetMapping("{page}/{size}")
    public Result getSpuInfoPage(@PathVariable Long page,
                                 @PathVariable Long size,
                                 SpuInfo spuInfo){

        Page<SpuInfo> spuInfoPage  = new Page<>(page,size);

        IPage<SpuInfo> spuInfoPageList = manageService.getSpuInfoPage(spuInfoPage, spuInfo);

        return Result.ok(spuInfoPageList);


    }

    //http://api.gmall.com/admin/product/baseSaleAttrList
    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){

        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    //http://api.gmall.com/admin/product/saveSpuInfo
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    //http://api.gmall.com/admin/product/spuImageList/{spuId}
    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable Long spuId){

        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }

    //http://api.gmall.com/admin/product/spuSaleAttrList/{spuId}
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable Long spuId){

        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttr(spuId);
        return Result.ok(spuSaleAttrList);
    }

    /*
    * 根据关键字获取spu列表，活动使用
    */
    @GetMapping("findSpuInfoByKeyword/{keyword}")
    public Result findSpuInfoByKeyword(@PathVariable("keyword") String keyword){
        List<SpuInfo> spuInfoList = manageService.findSpuInfoByKeyword(keyword);
        return Result.ok(spuInfoList);
    }

    //根据spuId列表获取spu列表，活动使用
    @PostMapping("inner/findSpuInfoBySpuIdList")
    public List<SpuInfo> findSpuInfoBySpuIdList(@RequestBody List<Long> spuIdList) {
        return manageService.findSpuInfoBySpuIdList(spuIdList);
    }

    //根据category3Id列表获取category3列表，活动使用
    @PostMapping("inner/findBaseCategory3ByCategory3IdList")
    public List<BaseCategory3> findBaseCategory3ByCategory3IdList(@RequestBody List<Long> category3IdList) {
        return manageService.findBaseCategory3ByCategory3IdList(category3IdList);
    }


}

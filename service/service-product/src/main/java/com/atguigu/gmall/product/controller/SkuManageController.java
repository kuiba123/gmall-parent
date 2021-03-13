package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "sku的接口")
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    //http://api.gmall.com/admin/product/saveSkuInfo
    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    //http://api.gmall.com/admin/product/list/{page}/{limit}
    @GetMapping("list/{page}/{limit}")
    public Result getSkuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit){
        Page<SkuInfo> skuInfoPage = new Page<>(page,limit);
        IPage<SkuInfo> infoIPage = manageService.getSkuInfoList(skuInfoPage);
        return Result.ok(infoIPage);
    }

    // http://api.gmall.com/admin/product/onSale/{skuId}
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        manageService.onSale(skuId);
        return Result.ok();
    }

    // http://api.gmall.com/admin/product/cancelSale/{skuId}
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        //  调用方法
        manageService.cancelSale(skuId);
        return Result.ok();
    }


}

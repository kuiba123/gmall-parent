package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "品牌的数据接口")
@RestController
@RequestMapping("admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    //http://api.gmall.com/admin/product/baseTrademark/{page}/{limit}
    @ApiOperation(value = "分页列表")
    @GetMapping("{page}/{limit}")
    public Result selectPage(@PathVariable Long page,
                             @PathVariable Long limit){
        Page<BaseTrademark> pageParam = new Page<>(page,limit);
        IPage<BaseTrademark> pageModel = baseTrademarkService.selectPage(pageParam);
        return Result.ok(pageModel);
    }

    //http://api.gmall.com/admin/product/baseTrademark/save
    @ApiOperation(value = "新增BaseTrademark")
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark banner){
        baseTrademarkService.save(banner);
        return Result.ok();
    }

    //http://api.gmall.com/admin/product/baseTrademark/update
    @ApiOperation(value = "修改BaseTrademark")
    @PutMapping("update")
    public Result updateById(@RequestBody BaseTrademark banner){
        baseTrademarkService.updateById(banner);
        return Result.ok();
    }

    //http://api.gmall.com/admin/product/baseTrademark/remove/{id}
    @ApiOperation(value = "删除BaseTrademark")
    @DeleteMapping("remove/{id}")
    public Result removeById(@PathVariable Long id){
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    //http://api.gmall.com/admin/product/baseTrademark/get/{id}
    @ApiOperation(value = "获取BaseTrademark")
    @GetMapping("get/{id}")
    public Result get(@PathVariable String id){
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    //http://api.gmall.com/admin/product/baseTrademark/getTrademarkList
    @GetMapping("getTrademarkList")
    public Result getTrademarkList(){
        return Result.ok(baseTrademarkService.list(null));
    }

    @GetMapping("findBaseTrademarkByKeyword/{keyword}")
    public Result findBaseTrademarkByKeyword(@PathVariable String keyword){
        List<BaseTrademark> trademarkList = baseTrademarkService.findBaseTrademarkByKeyword(keyword);
        return Result.ok(trademarkList);
    }

    //根据trademarkId列表获取trademark列表，活动使用
    @PostMapping("inner/findBaseTrademarkByTrademarkIdList")
    public List<BaseTrademark> findBaseTrademarkByTrademarkIdList(@RequestBody List<Long> trademarkIdList) {
        return baseTrademarkService.findBaseTrademarkByTrademarkIdList(trademarkIdList);
    }
}

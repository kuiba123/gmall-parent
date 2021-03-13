package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    @GetMapping("inner/createIndex")
    public Result createIndex() {
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    /*
    *  上架商品
    * @param skuId:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable("skuId") Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }

    /*
    * 下架商品
    * @param skuId:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable("skuId") Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    /*
    * 更新商品incrHotScore
    * @param skuId:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable("skuId") Long skuId){
        //调用服务层
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    /*
    * 检索控制器
    * @param searchParam:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @PostMapping
    public Result list(@RequestBody SearchParam searchParam) throws IOException {

        SearchResponseVo searchResponseVo = searchService.search(searchParam);
        return  Result.ok(searchResponseVo);
    }


}

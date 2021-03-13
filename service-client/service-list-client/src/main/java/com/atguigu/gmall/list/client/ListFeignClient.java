package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.impl.ListDegradeFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseAttrVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-list", fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {

    /*
    * 商品热度排名数据接口
    * @param skuId:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @GetMapping("/api/list/inner/incrHotScore/{skuId}")
    Result incrHotScore(@PathVariable("skuId") Long skuId);

    /*
    * 搜索商品
    * @param searchParam:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @PostMapping("/api/list")
    Result list(@RequestBody SearchParam searchParam);

    /*
    * 上架商品
    * @param skuId:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @GetMapping("/api/list/inner/upperGoods/{skuId}")
    Result upperGoods(@PathVariable("skuId") Long skuId);

    /*
    * 下架商品
    * @param skuId:
    * @return: com.atguigu.gmall.common.result.Result
    */
    @GetMapping("/api/list/inner/lowerGoods/{skuId}")
    Result lowerGoods(@PathVariable("skuId") Long skuId);
}

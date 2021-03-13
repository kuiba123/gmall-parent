package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

public interface SearchService {

    /*
    * 上架商品列表
    * @param skuId:
    * @return: void
    */
    void upperGoods(Long skuId);

    /*
    * 下架商品列表
    * @param skuId:
    * @return: void
    */
    void lowerGoods(Long skuId);

    /*
    * 更新热点
    * @param skuId:
    * @return: void
    */
    void incrHotScore(Long skuId);

    //检索数据接口
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}

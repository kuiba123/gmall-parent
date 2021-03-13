package com.atguigu.gmall.item.service;

import java.util.Map;

public interface ItemService {

    /*
    *  获取sku详情信息
    * @param skuId:
    * @return: java.util.Map<java.lang.String,java.lang.Object>
    */
    Map<String,Object> getBySkuId(Long skuId);
}

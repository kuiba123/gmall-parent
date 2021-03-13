package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

public interface SeckillGoodsService {

    //查询所有的秒杀商品列表
    List<SeckillGoods> findAll();

    //根据当前商品的Id查询到秒杀商品详情
    SeckillGoods getSeckillGoodsById(Long skuId);

    //秒杀下单数据
    void seckillOrder(Long skuId,String userId);

    //检查订单接口
    Result checkOrder(Long skuId, String userId);
}

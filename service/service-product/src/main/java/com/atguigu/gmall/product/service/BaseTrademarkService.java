package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseTrademarkService extends IService<BaseTrademark> {

    //banner分页列表
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam);

    //获取品牌数据
    List<BaseTrademark> findBaseTrademarkByKeyword(String keyword);

    List<BaseTrademark> findBaseTrademarkByTrademarkIdList(List<Long> trademarkIdList);
}

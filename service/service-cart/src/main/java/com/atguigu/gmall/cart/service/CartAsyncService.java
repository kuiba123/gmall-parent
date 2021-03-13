package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

public interface CartAsyncService {

    //异步更新
    void updateCartInfo(CartInfo cartInfo);

    //异步添加
    void saveCartInfo(CartInfo cartInfo);

    //删除购物车
    void deleteCartInfo(String userTempId);

    //更新数据库商品的状态
    void checkCart(String userId,Integer isChecked,Long skuId);

    //删除购物车
    void deleteCart(String userId,Long skuId);
}

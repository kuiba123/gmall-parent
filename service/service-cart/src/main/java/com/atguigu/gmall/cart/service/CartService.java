package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {

    //添加购物车用户Id,商品Id,商品数量
    void addToCart(Long skuId,String userId,Integer skuNum);

    //查询购物车列表
    List<CartInfo> getCartInfoList(String userId,String userTempId);

    //  根据用户Id查询数据 包含登录的，也包含未登录
    List<CartInfo> getCartInfoList(String userId);

    //选中状态变更
    void checkCart(String userId,Integer isChecked,Long skuId);

    //  删除购物车
    void deleteCart(String userId,Long skuId);

    //  根据用户Id 查询购物车选中的商品列表
    List<CartInfo> getCartCheckedList(String userId);

    //根据用户Id查询最新的商品价格购物车列表
    List<CartInfo> loadCartCache(String userId);


}

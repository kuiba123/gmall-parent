package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncServiceImpl implements CartAsyncService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Override
    @Async
    public void updateCartInfo(CartInfo cartInfo) {
        //更新数据方法
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",cartInfo.getUserId());
        cartInfoQueryWrapper.eq("sku_id",cartInfo.getSkuId());
        cartInfoMapper.update(cartInfo,cartInfoQueryWrapper);
    }

    @Override
    @Async
    public void saveCartInfo(CartInfo cartInfo) {
        //保存方法
        cartInfoMapper.insert(cartInfo);
    }

    @Override
    @Async
    public void deleteCartInfo(String userTempId) {
        //delete from cart_info where user_id = userTempId;
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userTempId));
    }

    @Override
    @Async
    public void checkCart(String userId, Integer isChecked, Long skuId) {

        //  mybatis-plus ： 执行的时候，更新的数据只有is_checked=isChecked 其他的字段数据保持不变
        //  update cart_info set is_checked=isChecked where user_id = userId and sku_id=skuId;
        //设置按照怎样的条件进行更新
        QueryWrapper<CartInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        queryWrapper.eq("sku_id",skuId);
        //属于更新的数据
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);

        //  时间不用更新吗?  如果从数据的角度来讲当前商品确实更新，updateTime 就更新一次。
        //  如果更新了一次updateTime ， 查询的时候，按照updateTime 进行排序的。
        //  如果你想让展示的时候，用户点击状态的情况下也去更新商品的排名，那么我们就可以做更新时间的改变。否则就不变。
        //  cartInfo.setUpdateTime(new Timestamp(new Date().getTime()));
        cartInfoMapper.update(cartInfo,queryWrapper);
    }

    @Override
    @Async
    public void deleteCart(String userId, Long skuId) {
        //  delete from cart_info where user_id = userId and sku_id=skuId;
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId);
        cartInfoQueryWrapper.eq("sku_id",skuId);
        cartInfoMapper.delete(cartInfoQueryWrapper);
    }
}

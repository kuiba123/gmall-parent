package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.cart.client.impl.CartDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(value = "service-cart",fallback = CartDegradeFeignClient.class)
public interface CartFeignClient {

    //  处理添加购物车方法,查看购物车列表之外，其他的操作都是异步请求！
    //  将service-cart 数据接口发送过来！添加购物车的方法！ 直接访问web-all
    //  在此不需要 HttpServletRequest request; request 到底如何处理后续讲。通过feign 远程调用的时候，不传递头文件信息。
    @PostMapping("api/cart/addToCart/{skuId}/{skuNum}")
    Result addToCart(@PathVariable Long skuId,
                     @PathVariable Integer skuNum);

    //根据用户Id查询购物车选中的商品列表
    @GetMapping("api/cart/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable String userId);

    //根据用户Id查询购物车列表
    @GetMapping("api/cart/loadCartCache/{userId}")
    Result loadCartCache(@PathVariable String userId);
}

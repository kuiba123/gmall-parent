package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.client.naming.utils.StringUtils;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    //最终看页面需要怎样的数据:UserAddressList detailArrayList totalNum totalAmount
    @GetMapping("auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        Map<String, Object> map = new HashMap<>();

        //获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userFeignClient.findUserAddreddListByUserId(userId);

        //获取购物车中的送货清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);

        //声明一个集合来存储订单明细
        ArrayList<OrderDetail> orderDetails = new ArrayList<>();
        //需要将cartCheckedList集合中的数据赋值给orderDetail数据
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            //添加到集合
            orderDetails.add(orderDetail);
        }

        //计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetails);
        orderInfo.sumTotalAmount();

        //保存流水号
        String tradeNo = orderService.getTradeNo(userId);

        map.put("userAddressList",userAddressList);
        map.put("detailArrayList",orderDetails);
        map.put("totalNum",orderDetails.size());
        map.put("totalAmount",orderInfo.getTotalAmount());
        map.put("tradeNo",tradeNo);

        return  Result.ok(map);
    }

    //  保存订单的控制器 获取到前端传递的数据：Json ---> JavaObject; 使用@RequestBody
    //  http://api.gmall.com/api/order/auth/submitOrder?tradeNo=null
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request){
        //  user_id 在控制器中获取即可！*****
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        //获取页面传递过来的流水号
        String tradeNo = request.getParameter("tradeNo");
        //调用此比较方法
        boolean flag = orderService.checkTradeNo(tradeNo, userId);
        if (!flag) {
            //比较失败
            return Result.fail().message("不能重复提交订单");
        }

        //  用户提示集中到一起，放入一个字符串集合中
        List<String> errorList = new ArrayList<>();
        //  创建一个集合来存储异步编排对象
        List<CompletableFuture> futureList = new ArrayList<>();
        //验证库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        if (!CollectionUtils.isEmpty(orderDetailList)) {
            for (OrderDetail orderDetail : orderDetailList) {

                //在这个位置获取一个异步编排对象,用户提示集中到一起，放入一个字符串集合中
                CompletableFuture<Void> checkStockCompletableFuture = CompletableFuture.runAsync(() -> {
                    //验证库存
                    boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                    if (!result) {
                        errorList.add(orderDetail.getSkuName() + "库存不足!");
                    }
                }, threadPoolExecutor);
                //将这个异步编排对象放入集合
                futureList.add(checkStockCompletableFuture);

                CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
                    //验证价格
                    BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                    if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                        //重新查询价格!
                        cartFeignClient.loadCartCache(userId);
                        errorList.add(orderDetail.getSkuName() + "价格有变动!");
                    }
                }, threadPoolExecutor);
                //  将这个异步编排对象放入集合
                futureList.add(skuPriceCompletableFuture);
            }
        }

        //  使用allOf 将任务组合在一起
        //  CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();

        //  利用errorList 这个集合来判断是否有不合法的数据
        if (errorList.size() > 0) {
            //  将errorList 中的数据全部拿出来显示 xxx 价格有变动！，xxx 库存不足！
            return Result.fail().message(StringUtils.join(errorList,","));
        }
        //删除缓存中的流水号
        orderService.delTradeNo(userId);

        Long orderId = orderService.saveOrderInfo(orderInfo);
        //  返回订单Id
        return Result.ok(orderId);
    }

    /*
     * 内部调用获取订单
    * @param orderId:
    * @return: com.atguigu.gmall.model.order.OrderInfo
    */
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable(value = "orderId") Long orderId){
        return orderService.getOrderInfo(orderId);
    }

    //  拆单控制器：
    //  http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=xxxx
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        //  商品与仓库的关系
        //  [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        String wareSkuMap = request.getParameter("wareSkuMap");
        //  返回子订单的json 集合字符串！ 子订单通过拆单得到！
        List<OrderInfo> subOrderInfoList = orderService.orderSplit(orderId, wareSkuMap);

        //声明一个集合来存储map
        List<Map> maps = new ArrayList<>();

        //  判断子订单集合不为空
        if (!CollectionUtils.isEmpty(subOrderInfoList)) {
            //  循环遍历
            for (OrderInfo orderInfo : subOrderInfoList) {
                //  orderInfo 变为Map
                Map map = orderService.initWareOrder(orderInfo);
                maps.add(map);
            }
        }
        // maps 中的数据是子订单集合想要的数据！
        return JSON.toJSONString(maps);
    }

    //提交订单
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){
        //  返回订单Id
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }


}

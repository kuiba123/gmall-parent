package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {

    //  第二种方案：
    @Autowired
    private OrderFeignClient orderFeignClient;

    //  http://payment.gmall.com/pay.html?orderId=139
    @GetMapping("pay.html") // Long orderId,
    public String pay( HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        //  通过页面可以得出结论： 存储一个orderInfo
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        request.setAttribute("orderInfo",orderInfo);
        return "payment/pay";
    }
}

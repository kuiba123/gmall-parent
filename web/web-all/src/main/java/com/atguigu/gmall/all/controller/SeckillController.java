package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Controller
public class SeckillController {

    @Autowired
    private ActivityFeignClient activityFeignClient;

    //  http://activity.gmall.com/seckill.html
    @GetMapping("seckill.html")
    public String seckillIndex(Model model){
        //  由页面得知：存储一个${list}
        Result result = activityFeignClient.findAll();
        model.addAttribute("list",result.getData());
        //  商品秒杀数据：从service-activity 微服务中来！
        return "seckill/index";
    }

    //  编写秒杀的详情页
    //  http://activity.gmall.com/seckill/40.html
    @GetMapping("seckill/{skuId}.html")
    public String seckillItem(@PathVariable Long skuId, Model model){
        Result result = activityFeignClient.getSeckillGoodsById(skuId);
        model.addAttribute("item",result.getData());
        //  页面需要什么数据 item
        return "seckill/item";
    }

    //  编写排队页面
    //  '/seckill/queue.html?skuId='+this.skuId+'&skuIdStr='+skuIdStr
    @GetMapping("seckill/queue.html")
    public String queue(HttpServletRequest request){
        //  需要获取到skuId skuIdStr
        String skuId = request.getParameter("skuId");
        String skuIdStr = request.getParameter("skuIdStr");
        //  存储页面需要的数据
        request.setAttribute("skuId",skuId);
        request.setAttribute("skuIdStr",skuIdStr);
        //  返回对应的视图名称
        return "seckill/queue";
    }

    //  /seckill/trade.html
    @GetMapping("/seckill/trade.html")
    public String seckillTrade(Model model) {
        //  页面需要 ： userAddressList detailArrayList totalAmount
        Result<Map> result = activityFeignClient.trade();
        if (result.isOk()){
            model.addAllAttributes(result.getData());
            //  返回视图名称
            return "seckill/trade";
        }else {
            model.addAttribute("message","下单失败!");
            //  返回视图名称
            return "seckill/fail";
        }
    }
}

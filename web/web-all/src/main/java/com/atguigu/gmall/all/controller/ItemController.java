package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;
    /*
    * Sku详情页面
    * @param skuId:
     * @param model:
    * @return: java.lang.String
    */
    @GetMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId, Model model){
       //通过skuId查询skuInfo
        Result<Map> result = itemFeignClient.getItem(skuId);
        //获取到result中的data它才是我们想要的map封装好的数据！
        //用来存储所有数据Map集合map的key就是页面要渲染的key！
        model.addAllAttributes(result.getData());
        //返回页面视图名称
        return "item/index";
    }
}

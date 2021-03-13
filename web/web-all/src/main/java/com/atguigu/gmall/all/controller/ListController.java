package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    /*
    * 列表搜索
    * @param searchParam:
     * @param model:
    * @return: java.lang.String
    */
    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model){

        Result<Map> result = listFeignClient.list(searchParam);

        //排序： ${orderMap.type}  ${orderMap.sort}  type sort 可以认为是类的属性，或者是map 的key
        Map<String,Object> orderMap = this.orderByMap(searchParam.getOrder());
        //处理品牌条件回显:面包屑处理 ${propsParamList}  ${trademarkParam}
        String trademarkParam = this.makeTrademark(searchParam.getTrademark());
        //处理平台属性条件回显
        List<Map<String,String>> propsParamList = this.makeProps(searchParam.getProps());

        //记录拼接url
        //  ${searchParam.keyword} 前台页面需要的数据
        String urlParam = this.makeUrlParam(searchParam);

        model.addAttribute("orderMap",orderMap);
        model.addAttribute("trademarkParam",trademarkParam);
        model.addAttribute("propsParamList",propsParamList);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("searchParam",searchParam);
        //  result.getData() == SearchResponseVo
        model.addAllAttributes(result.getData());
        return "list/index";
    }

    //设置排序
    private Map<String, Object> orderByMap(String order) {
        Map<String, Object> map = new HashMap<>();
        //  判断 order=2:desc  order=2:asc
        if (!StringUtils.isEmpty(order)) {
            String[] split = order.split(":");
            //  ${orderMap.type} ${orderMap.sort}
            if (split != null && split.length == 2) {
                map.put("type",split[0]);
                map.put("sort",split[1]);
            }
        }else {
            //默认值
            map.put("type","1");//综合hotScore
            map.put("sort","asc");//默认排序规则
        }
        return map;
    }

    //制作平台属性
    private List<Map<String, String>> makeProps(String[] props) {
        System.out.println("props = " + props);
        List<Map<String, String>> list = new ArrayList<>();
        //判断是否根据平台属性进行过滤
        if (props != null && props.length > 0) {
            //循环遍历 &props = 23:8G:运行内存33
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split != null && split.length == 3) {
                    Map<String, String> map = new HashMap<>();
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);
                    list.add(map);
                }
            }
        }
        return list;
    }

    /*
    * 处理品牌条件回显
    * @param trademark:
    * @return: java.lang.String
    */
    private String makeTrademark(String trademark) {

        if (!StringUtils.isEmpty(trademark)) {
            //分割数据 4:小米
            String[] split = trademark.split(":");
            if (split != null && split.length == 2) {
                return "品牌:" + split[1];
            }
        }
        return null;

    }

    private String makeUrlParam(SearchParam searchParam) {

        StringBuffer urlParam = new StringBuffer();
        //  判断用户是否先通过分类Id检索
        //  http://list.atguigu.cn/list.html?category1Id=2
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())){
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        //  http://list.atguigu.cn/list.html?category2Id=13
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())){
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }

        //  http://list.atguigu.cn/list.html?category3Id=61
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())){
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }

        //  判断是否根据关键字检索
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            //http://list.atguigu.cn/list.html?keyword=小米手机
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }

        //  判断是否又根据品牌进行了检索
        if (!StringUtils.isEmpty(searchParam.getTrademark())){
            // http://list.atguigu.cn/list.html?keyword=小米手机&trademark=2:苹果
            if (urlParam.length()>0){
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //  判断是否又根据平台属性值进行了检索
        // http://list.atguigu.cn/list.html?keyword=小米手机&trademark=2:苹果&props=107:苹果:二级手机&props=23:4G:运行内存33
        String[] props = searchParam.getProps();
        if (props!=null && props.length>0){
            // 循环遍历
            for (String prop : props) {
                if (urlParam.length()>0){
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        return "list.html?"+urlParam.toString();
    }
}

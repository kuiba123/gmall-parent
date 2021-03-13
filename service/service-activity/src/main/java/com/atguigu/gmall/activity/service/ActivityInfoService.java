package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.model.activity.ActivityInfo;
import com.atguigu.gmall.model.activity.ActivityRuleVo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

//后面还会对这个接口进行一系列的CRUD操作
public interface ActivityInfoService extends IService<ActivityInfo> {

    //带分页的数据查询
    IPage<ActivityInfo> getPage(Page<ActivityInfo> infoPage);

    //保存数据
    void saveActivityRule(ActivityRuleVo activityRuleVo);

    List<SkuInfo> findSkuInfoByKeyword(String keyword);

    Map<String, Object> findActivityRuleList(Long id);
}

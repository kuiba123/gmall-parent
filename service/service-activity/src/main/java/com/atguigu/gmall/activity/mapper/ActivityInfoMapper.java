package com.atguigu.gmall.activity.mapper;

import com.atguigu.gmall.model.activity.ActivityInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityInfoMapper extends BaseMapper<ActivityInfo> {
    List<Long> selectExistSkuIdList(@Param("skuIdList") List<Long> skuIdList);

}

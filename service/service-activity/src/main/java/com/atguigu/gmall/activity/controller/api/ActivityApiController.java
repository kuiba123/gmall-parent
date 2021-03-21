package com.atguigu.gmall.activity.controller.api;

import com.atguigu.gmall.activity.service.ActivityService;
import com.atguigu.gmall.activity.service.CouponInfoService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.activity.CouponInfo;
import com.atguigu.gmall.model.cart.CarInfoVo;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderTradeVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityApiController {

    //ActivityService主要起的作用是汇总数据 优惠劵+促销活动
    @Autowired
    private ActivityService activityService;

    @Autowired
    private CouponInfoService couponInfoService;

    @GetMapping("findActivityAndCoupon/{skuId}")
    public Result findActivityAndCoupond (@PathVariable Long skuId, HttpServletRequest request){

        //获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        //判断是否为空
        if (StringUtils.isEmpty(userId)) {
            userId = "0";
        }

        /**
         1.  获取促销活动的： findActivityRule(Long skuId)
         2.  获取优惠券： findCouponInfo(Long skuId, Long activityId, Long userId)
         activityId 如何获取? findActivityRule(Long skuId) 的返回值获取到！
        */
        Map<String,Object> map = activityService.findActivityAndCoupon(skuId,Long.parseLong(userId));

        return Result.ok(map);
    }

    @ApiOperation(value = "领取优惠券")
    @GetMapping(value = "auth/getCouponInfo/{couponId}")
    public Result getCouponInfo(@PathVariable("couponId") Long couponId, HttpServletRequest request){
        //当前登录用户
        String userId = AuthContextHolder.getUserId(request);
        couponInfoService.getCouponInfo(couponId, Long.parseLong(userId));
        return Result.ok();
    }

    @ApiOperation(value = "我的优惠券")
    @GetMapping("auth/{page}/{limit}")
    public Result index(@PathVariable Long page,
                        @PathVariable Long limit,
                        HttpServletRequest request){
        //获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        Page<CouponInfo> couponInfoPage = new Page<>(page,limit);
        IPage<CouponInfo> pageModel= couponInfoService.selectPageByUserId(couponInfoPage,Long.parseLong(userId));
        return Result.ok(pageModel);
    }

    @ApiOperation(value = "获取购物车满足条件的促销与优惠券信息")
    @PostMapping("inner/findCartActivityAndCoupon/{userId}")
    public List<CarInfoVo> findCartActivityAndCoupon(@RequestBody List<CartInfo> cartInfoList, @PathVariable("userId") Long userId) {
        return activityService.findCartActivityAndCoupon(cartInfoList,userId);
    }

    @ApiOperation(value = "获取交易满足条件的促销与优惠券信息")
    @PostMapping("inner/findTradeActivityAndCoupon/{userId}")
    public OrderTradeVo findTradeActivityAndCoupon(@RequestBody List<OrderDetail> orderDetailList, @PathVariable("userId") Long userId, HttpServletRequest request){
        return activityService.findTradeActivityAndCoupon(orderDetailList,userId);
    }

}

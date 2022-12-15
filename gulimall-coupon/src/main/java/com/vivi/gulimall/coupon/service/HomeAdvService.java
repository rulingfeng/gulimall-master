package com.vivi.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vivi.common.utils.PageUtils;
import com.vivi.gulimall.coupon.entity.HomeAdvEntity;

import java.util.Map;

/**
 * 首页轮播广告
 *
 * @author
 * @email i@ baidu.com
 * @date 2020-09-13 10:52:21
 */
public interface HomeAdvService extends IService<HomeAdvEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


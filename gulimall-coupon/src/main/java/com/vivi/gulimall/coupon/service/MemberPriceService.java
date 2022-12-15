package com.vivi.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vivi.common.utils.PageUtils;
import com.vivi.gulimall.coupon.entity.MemberPriceEntity;

import java.util.Map;

/**
 * 商品会员价格
 *
 * @author
 * @email i@ baidu.com
 * @date 2020-09-13 10:52:21
 */
public interface MemberPriceService extends IService<MemberPriceEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


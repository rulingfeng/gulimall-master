package com.vivi.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vivi.common.utils.PageUtils;
import com.vivi.gulimall.order.entity.PaymentInfoEntity;

import java.util.Map;

/**
 * 支付信息表
 *
 * @author
 * @email i@ baidu.com
 * @date 2020-09-13 10:49:58
 */
public interface PaymentInfoService extends IService<PaymentInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


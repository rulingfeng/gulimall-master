package com.vivi.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vivi.common.utils.PageUtils;
import com.vivi.gulimall.member.entity.MemberLoginLogEntity;

import java.util.Map;

/**
 * 会员登录记录
 *
 * @author
 * @email i@ baidu.com
 * @date 2020-09-13 10:51:13
 */
public interface MemberLoginLogService extends IService<MemberLoginLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


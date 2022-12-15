package com.vivi.gulimall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vivi.common.utils.PageUtils;
import com.vivi.gulimall.member.entity.MemberCollectSubjectEntity;

import java.util.Map;

/**
 * 会员收藏的专题活动
 *
 * @author
 * @email i@ baidu.com
 * @date 2020-09-13 10:51:13
 */
public interface MemberCollectSubjectService extends IService<MemberCollectSubjectEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


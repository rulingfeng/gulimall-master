package com.vivi.gulimall.member.dao;

import com.vivi.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 会员
 * 
 * @author  
 * @email i@ baidu.com
 * @date 2020-09-13 10:51:13
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {

    MemberEntity getByAccount(@Param("account") String account);
}

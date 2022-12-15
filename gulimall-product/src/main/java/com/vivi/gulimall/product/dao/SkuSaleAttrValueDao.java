package com.vivi.gulimall.product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vivi.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.vivi.gulimall.product.vo.ItemSaleAttrVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * sku销售属性&值
 * 
 * @author  
 * @email i@ baidu.com
 * @date 2020-09-13 10:48:46
 */
@Mapper
public interface SkuSaleAttrValueDao extends BaseMapper<SkuSaleAttrValueEntity> {

    List<ItemSaleAttrVO> allAttrValueWithSkuBySpuId(@Param("spuId") Long spuId);

    List<String> stringListBySkuId(@Param("skuId") Long skuId);
}

package com.vivi.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vivi.common.utils.PageUtils;
import com.vivi.gulimall.product.entity.SkuSaleAttrValueEntity;
import com.vivi.gulimall.product.vo.ItemSaleAttrVO;

import java.util.List;
import java.util.Map;

/**
 * sku销售属性&值
 *
 * @author  
 * @email i@ baidu.com
 * @date 2020-09-13 10:48:46
 */
public interface SkuSaleAttrValueService extends IService<SkuSaleAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SkuSaleAttrValueEntity> listBySkuId(Long skuId);

    /**
     * 指定spu的所有销售属性及其值(值为多个值，逗号分隔，总体为一个string)
     * @param spuId
     * @return
     */
    List<ItemSaleAttrVO> allAttrValueWithSkuBySpuId(Long spuId);


    /**
     * 指定sku的所有销售属性及其值(属性名:值)
     * @param skuId
     * @return
     */
    List<String> stringListBySkuId(Long skuId);
}


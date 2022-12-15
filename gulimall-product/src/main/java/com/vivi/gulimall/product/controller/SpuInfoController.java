package com.vivi.gulimall.product.controller;

import com.vivi.common.to.SpuInfoTO;
import com.vivi.common.utils.PageUtils;
import com.vivi.common.utils.R;
import com.vivi.gulimall.product.entity.SpuInfoEntity;
import com.vivi.gulimall.product.service.SpuInfoService;
import com.vivi.gulimall.product.vo.SpuVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


/**
 * spu信息
 *
 * @author  
 * @email i@ baidu.com
 * @date 2020-09-13 10:48:45
 */
@RestController
@RequestMapping("product/spuinfo")
public class SpuInfoController {
    @Autowired
    private SpuInfoService spuInfoService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    // @RequiresPermissions("product:spuinfo:list")
    public R list(@RequestParam Map<String, Object> params){
        // PageUtils page = spuInfoService.queryPage(params);
        PageUtils page = spuInfoService.queryPageCondition(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    // @RequiresPermissions("product:spuinfo:info")
    public R info(@PathVariable("id") Long id){
		SpuInfoEntity spuInfo = spuInfoService.getById(id);

        return R.ok().put("spuInfo", spuInfo);
    }

    /**
     * 上架
     */
    @RequestMapping("/{id}/up")
    public R up(@PathVariable("id") Long id){
        spuInfoService.statusUp(id);
        return R.ok();
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    // @RequiresPermissions("product:spuinfo:save")
    public R save(@RequestBody /*SpuInfoEntity spuInfo*/SpuVO spuVO){
		// spuInfoService.save(spuInfo);
		spuInfoService.save(spuVO);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    // @RequiresPermissions("product:spuinfo:update")
    public R update(@RequestBody SpuInfoEntity spuInfo){
		spuInfoService.updateById(spuInfo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    // @RequiresPermissions("product:spuinfo:delete")
    public R delete(@RequestBody Long[] ids){
		spuInfoService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 远程调用，根据skuId查找
     */
    @GetMapping("/skuId/{skuId}")
    public R getBySkuId(@PathVariable("skuId") Long skuId) {
        SpuInfoTO spuInfoTO = spuInfoService.getBySkuId(skuId);
        return R.ok().setData(spuInfoTO);
    }

}

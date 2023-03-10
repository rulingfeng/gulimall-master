package com.vivi.gulimall.ware.controller;

import com.vivi.common.to.OrderLockStockTO;
import com.vivi.common.to.SkuStockTO;
import com.vivi.common.utils.PageUtils;
import com.vivi.common.utils.R;
import com.vivi.gulimall.ware.entity.WareSkuEntity;
import com.vivi.gulimall.ware.service.WareSkuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * 商品库存
 *
 * @author  
 * @email i@ baidu.com
 * @date 2020-09-13 10:47:27
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {


    @Autowired
    private WareSkuService wareSkuService;


    /**
     * 列表
     */
    @RequestMapping("/list")
    // @RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);
        return R.ok().put("page", page);
    }

    @RequestMapping("/stock/batch")
    public R getSkuStockBatch(@RequestBody List<Long> skuIds) {
        List<SkuStockTO> skuStockTOS = wareSkuService.getSkusStock(skuIds);
        R ok = R.ok();
        ok.setData(skuStockTOS);
        return ok;
    }

    @RequestMapping("/stock/{skuId}")
    public R getSkuStock(@PathVariable("skuId") Long skuId) {
        Long stock  = wareSkuService.getSkuStock(skuId);
        return R.ok().setData(stock);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    // @RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    // @RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    // @RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    // @RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }
//    http://localhost:11000/ware/waresku/lockStock
    /**
     * 为订单锁定库存
     */
    @PostMapping("/lockStock")
    public R lockStock(@RequestBody OrderLockStockTO lockStockTO) {
        wareSkuService.lockOrderStock(lockStockTO);
        return R.ok();
    }

}

package com.vivi.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.vivi.common.to.OrderLockStockTO;
import com.vivi.common.to.OrderTO;
import com.vivi.common.to.SkuStockTO;
import com.vivi.common.to.mq.StockLockTO;
import com.vivi.common.utils.PageUtils;
import com.vivi.gulimall.ware.entity.WareSkuEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author wanwgei
 * @email i@weiwang.com
 * @date 2020-09-13 10:47:27
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 给指定仓库中的指定商品增加库存
     * @param wareId
     * @param skuId
     * @param num
     * @return
     */
    boolean addStock(Long wareId, Long skuId, Integer num);

    /**
     * 查出指定的skus的库存信息，封装成map
     * @param skuIds
     * @return
     */
    List<SkuStockTO> getSkusStock(List<Long> skuIds);

    /**
     * 查出某个sku的库存信息
     * @param skuId
     * @return
     */
    Long getSkuStock(Long skuId);

    /**
     * 为订单锁定库存
     * @param lockStockTO
     * @return
     */
    boolean lockOrderStock(OrderLockStockTO lockStockTO);

    /**
     * 收到库存锁定过期消息，释放库存
     * @param stockLockTO
     * @return
     */
    void unlockStock(StockLockTO stockLockTO);

    /**
     * 收到订单关闭消息，释放库存
     */
    void unlockStock(OrderTO orderTO);

}


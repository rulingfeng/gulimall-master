package com.vivi.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vivi.common.constant.ProductConstant;
import com.vivi.common.exception.BizCodeEnum;
import com.vivi.common.exception.BizException;
import com.vivi.common.to.*;
import com.vivi.common.utils.PageUtils;
import com.vivi.common.utils.Query;
import com.vivi.common.utils.R;
import com.vivi.gulimall.product.dao.SpuInfoDao;
import com.vivi.gulimall.product.entity.*;
import com.vivi.gulimall.product.feign.CouponFeignService;
import com.vivi.gulimall.product.feign.SearchFeignService;
import com.vivi.gulimall.product.feign.WareFeignService;
import com.vivi.gulimall.product.service.*;
import com.vivi.gulimall.product.vo.SpuVO;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private SearchFeignService searchFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );
        return new PageUtils(page);
    }

    // TODO ??????????????????,seata???????????????????????????
    @GlobalTransactional
    // @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean save(SpuVO spuVO) {
        // System.out.println(spuVO);
        // 1. ??????spu???????????? pms->pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuVO, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.save(spuInfoEntity);

        Long spuId = spuInfoEntity.getId();

        // 2. spu????????????????????? pms->pms_spu_info_desc
        List<String> descript = spuVO.getDescript();
        if (!CollectionUtils.isEmpty(descript)) {
            String desUrlPath = String.join(",", descript);
            SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
            spuInfoDescEntity.setDescript(desUrlPath);
            spuInfoDescEntity.setSpuId(spuId);
            spuInfoDescService.save(spuInfoDescEntity);
        }

        // 3. spu????????????????????? pms->pms_spu_images
        List<String> images = spuVO.getImages();
        if (!CollectionUtils.isEmpty(images)) {
            List<SpuImagesEntity> imagesEntities = images.stream().map(image -> {
                SpuImagesEntity spuImagesEntity = new SpuImagesEntity();
                spuImagesEntity.setSpuId(spuId);
                spuImagesEntity.setImgUrl(image);
                return spuImagesEntity;
            }).filter(image -> !StringUtils.isEmpty(image.getImgUrl())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(imagesEntities)) {
                spuImagesService.saveBatch(imagesEntities);
            }
        }

        // 4. spu????????????????????? pms->pms_product_attr_value
        List<SpuVO.BaseAttrs> baseAttrs = spuVO.getBaseAttrs();
        if (!CollectionUtils.isEmpty(baseAttrs)) {
            List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().map(baseAttr -> {
                ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
                AttrEntity attrEntity = attrService.getById(baseAttr.getAttrId());
                productAttrValueEntity.setAttrId(baseAttr.getAttrId());
                productAttrValueEntity.setAttrName(attrEntity.getAttrName());
                productAttrValueEntity.setAttrValue(baseAttr.getAttrValues());
                productAttrValueEntity.setQuickShow(baseAttr.getShowDesc());
                productAttrValueEntity.setSpuId(spuId);
                return productAttrValueEntity;
            }).collect(Collectors.toList());
            productAttrValueService.saveBatch(productAttrValueEntities);
        }

        // 5. spu????????????????????? sms->sms_spu_bounds
        SpuVO.Bounds spuVOBounds = spuVO.getBounds();
        SpuBoundsTO spuBoundsTO = new SpuBoundsTO();
        BeanUtils.copyProperties(spuVOBounds, spuBoundsTO);
        spuBoundsTO.setSpuId(spuId);
        R r = couponFeignService.saveSpuBounds(spuBoundsTO);
        if(r.getCode() != 0){
            log.error("????????????spu??????????????????");
        }

        // 6. spu????????????sku??????
        List<SpuVO.Sku> skus = spuVO.getSkus();
        if (!CollectionUtils.isEmpty(skus)) {
            for (SpuVO.Sku sku : skus) {
                // 6.1 sku???????????? pms->pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(sku, skuInfoEntity);
                skuInfoEntity.setSpuId(spuId);
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setBrandId(spuVO.getBrandId());
                skuInfoEntity.setCatelogId(spuVO.getCatelogId());
                skuInfoEntity.setSkuDesc(String.join(",", sku.getDescar()));
                String defaultImg = "";
                for (SpuVO.Images image : sku.getImages()) {
                    if(image.getDefaultImg() == 1){
                        defaultImg = image.getImgUrl();
                    }
                }
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                skuInfoService.save(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();
                // 6.2 sku?????? pms->pms_sku_images
                List<SpuVO.Images> imagesList = sku.getImages();
                if (!CollectionUtils.isEmpty(imagesList)) {
                    List<SkuImagesEntity> imagesEntities = imagesList.stream().map(image -> {
                        SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                        BeanUtils.copyProperties(image, skuImagesEntity);
                        skuImagesEntity.setSkuId(skuId);
                        return skuImagesEntity;
                    }).filter(entity->{
                        //??????true???????????????false????????????
                        return !StringUtils.isEmpty(entity.getImgUrl());
                    }).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(imagesEntities)) {
                        skuImagesService.saveBatch(imagesEntities);
                    }
                }
                // 6.3 sku??????????????? pms->pms_sku_sale_attr_value
                List<SpuVO.Attr> attrList = sku.getAttr();
                if (!CollectionUtils.isEmpty(attrList)) {
                    List<SkuSaleAttrValueEntity> skuSaleAttrValueEntityList = attrList.stream().map(attr -> {
                        SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                        BeanUtils.copyProperties(attr, skuSaleAttrValueEntity);
                        skuSaleAttrValueEntity.setSkuId(skuId);
                        return skuSaleAttrValueEntity;
                    }).collect(Collectors.toList());
                    skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntityList);
                }
                // 6.4 sku???????????? sms_sku_ladder
                // 6.5 sku???????????? sms->sms_sku_full_reduction
                // 6.6 sku??????????????? sms->sms_member_price
                SkuDiscountTO skuDiscountTO = new SkuDiscountTO();
                BeanUtils.copyProperties(sku, skuDiscountTO);
                skuDiscountTO.setSkuId(skuId);
                R r1 = couponFeignService.saveDiscount(skuDiscountTO);
                if(r1.getCode() != 0){
                    log.error("????????????sku??????????????????");
                }
            }
        }
        return true;
    }

    @Override
    public PageUtils queryPageCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();
        // ???????????????
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and(wrapper -> {
                wrapper.eq("id", key).or().like("spu_name", key);
            });
        }
        // ????????????
        String catelogId = (String) params.get("catelogId");
        if (isValidId(catelogId)) {
            queryWrapper.eq("catelog_id", catelogId);
        }
        // ????????????
        String brandId = (String) params.get("brandId");
        if (isValidId(brandId)) {
            queryWrapper.eq("brand_id", brandId);
        }
        // ??????????????????
        String status = (String) params.get("status");
        if (!StringUtils.isEmpty(status)) {
            queryWrapper.eq("publish_status", status);
        }
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                queryWrapper
        );
        return new PageUtils(page);
    }

    @Override
    public boolean statusUp(Long id) {
        // 2.5 ????????????????????????????????????sku????????????spu????????????????????????attrs???????????????????????????????????????
        // ?????????spu??????????????????????????????????????????????????????
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.listForSpu(id);
        List<SkuESModel.Attrs> attrs = null;
        if (!CollectionUtils.isEmpty(productAttrValueEntities)) {
            // ?????????id??????
            List<Long> ids = productAttrValueEntities.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
            // ?????????id???????????????attr???????????????
            List<AttrEntity> searchAttrs = attrService.listSearchAttrByIds(ids);
            // ???????????????????????????????????????attrId
            List<Long> searchIds = searchAttrs.stream().map(attr -> attr.getAttrId()).collect(Collectors.toList());
            Set<Long> searchAttrIdSet = new HashSet<>(searchIds);
            // ???????????????id????????????????????????????????????spu????????????attr???????????????????????????
            attrs = productAttrValueEntities.stream().filter(item -> searchAttrIdSet.contains(item.getAttrId())).map(item -> {
                SkuESModel.Attrs attr = new SkuESModel.Attrs();
                attr.setAttrId(item.getAttrId());
                attr.setAttrName(item.getAttrName());
                attr.setAttrValue(item.getAttrValue());
                // ??????SkuESmodel??????????????????
                return attr;
            }).collect(Collectors.toList());
        }
        // 1. ?????????spu?????????sku
        List<SkuInfoEntity> skuList = skuInfoService.listBySpuId(id);
        // 2. ???sku -> skuESModel
        if (!CollectionUtils.isEmpty(skuList)) {
            // 2.6 ??????gulimall-ware???????????????sku???????????????
            // ????????????sku?????????????????????????????????????????????????????????????????????????????????sku???????????????
            Map<Long, Long> stockMap = new HashMap<>();
            List<Long> ids = skuList.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());
            try {
                R r = wareFeignService.getSkuStockBatch(ids);
                if (r.getCode() == 0) {
                    List<SkuStockTO> stockTOList = r.getData(new TypeReference<List<SkuStockTO>>() {
                    });
                    stockMap = stockTOList.stream().collect(Collectors.toMap(SkuStockTO::getSkuId, SkuStockTO::getStock));
                }
            } catch (Exception e) {
                log.error("??????gulimall-ware???????????????????????????{}", e);
            }
            List<SkuESModel.Attrs> finalAttrs = attrs;
            Map<Long, Long> finalStockMap = stockMap;
            List<SkuESModel> collect = skuList.stream().map(sku -> {
                SkuESModel skuESModel = new SkuESModel();
                // 2.1 ??????????????????
                BeanUtils.copyProperties(sku, skuESModel);

                // 2.2 ?????????????????????
                // skuImg skuPrice hotScore hasStock brandName brandImg catelogName attrs
                skuESModel.setSkuPrice(sku.getPrice());
                skuESModel.setSkuImg(sku.getSkuDefaultImg());
                skuESModel.setHotScore(0L);
                // 2.3 ??????brand????????????
                BrandEntity brandEntity = brandService.getById(sku.getBrandId());
                if (brandEntity != null) {
                    skuESModel.setBrandName(brandEntity.getName());
                    skuESModel.setBrandImg(brandEntity.getLogo());
                }
                // 2.4 ??????category????????????
                CategoryEntity categoryEntity = categoryService.getById(sku.getCatelogId());
                if (categoryEntity != null) {
                    skuESModel.setCatelogName(categoryEntity.getName());
                }
                // 2.5 ?????????????????????
                skuESModel.setAttrs(finalAttrs);
                // 2.6 ??????gulimall-ware???????????????sku???????????????
                Long stock = finalStockMap.getOrDefault(sku.getSkuId(), 0L);
                skuESModel.setHasStock(stock > 0 ? true : false);
                return skuESModel;
            }).collect(Collectors.toList());
            // 3. ??????gulimall-search?????????List<SkuESModel>??????
            searchFeignService.batchSaveSku(collect);
        }
        // 4. ?????????spu?????????????????????
        updateStatus(id, ProductConstant.SpuPublishStatus.UP.getValue());
        return true;
    }

    @Override
    public boolean updateStatus(Long spuId, Integer publishStatus) {
        this.baseMapper.updateStatus(spuId, publishStatus);
        return false;
    }

    @Override
    public SpuInfoTO getBySkuId(Long skuId) {

        SpuInfoTO spuInfoTO = new SpuInfoTO();

        SkuInfoEntity sku = skuInfoService.getById(skuId);
        Long spuId = sku.getSpuId();

        SpuInfoEntity spu = this.getById(spuId);
        // ??????????????????
        BeanUtils.copyProperties(spu, spuInfoTO);

        CategoryEntity categoryEntity = categoryService.getById(spu.getCatelogId());
        BrandEntity brandEntity = brandService.getById(spu.getBrandId());
        // ??????????????????
        spuInfoTO.setCatelogName(categoryEntity.getName());
        spuInfoTO.setBrandName(brandEntity.getName());

        R r = couponFeignService.getBySpuId(spuId);
        if (r.getCode() != 0) {
            log.error("gulimall-product??????gulimall-coupon??????");
            throw new BizException(BizCodeEnum.CALL_FEIGN_SERVICE_FAILED, "??????????????????");
        }
        SpuBoundsTO boundsTO = r.getData(SpuBoundsTO.class);

        spuInfoTO.setGrowBounds(boundsTO.getGrowBounds());
        spuInfoTO.setIntegration(boundsTO.getBuyBounds());

        return spuInfoTO;
    }


    private boolean isValidId(String key) {
        return !StringUtils.isEmpty(key) && !"0".equalsIgnoreCase(key);
    }

}
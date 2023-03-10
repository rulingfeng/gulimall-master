package com.vivi.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vivi.common.constant.ProductConstant;
import com.vivi.common.utils.PageUtils;
import com.vivi.common.utils.Query;
import com.vivi.gulimall.product.dao.CategoryBrandRelationDao;
import com.vivi.gulimall.product.dao.CategoryDao;
import com.vivi.gulimall.product.entity.CategoryEntity;
import com.vivi.gulimall.product.service.CategoryService;
import com.vivi.gulimall.product.vo.Catelog2VO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationDao categoryBrandRelationDao;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }


    @Cacheable(cacheNames = {ProductConstant.CacheName.PRODUCT_CATEGORY},
            key = "'categoryList'")
    @Override
    public List<CategoryEntity> listWithTree() {
        // ?????????????????????
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        return categoryEntities.stream()
                // ?????????????????????
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                // ?????????????????????????????????????????????
                .map(entity -> getChildren(entity, categoryEntities))
                // ???????????????????????????
                .sorted(Comparator.comparingInt(CategoryEntity::getSort))
                // ???????????????????????????
                .collect(Collectors.toList());
    }

    /**
     * ????????????????????????????????????????????????????????????
     * @param list
     * @return
     */
    @CacheEvict(cacheNames = {ProductConstant.CacheName.PRODUCT_CATEGORY},
            allEntries = true)
    @Override
    public boolean removeBatchByIds(List<Long> list) {
        // TODO ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        baseMapper.deleteBatchIds(list);
        return true;
    }

    @Override
    public List<Long> findCategoryPath(Long catId) {
        LinkedList<Long> path = new LinkedList<>();
        LinkedList<Long> fullPath = getFullPath(catId, path);
        // ??????
        Collections.reverse(fullPath);
        return fullPath;
    }

    /**
     * ???????????????updateById()??????????????????category????????????
     * ?????????????????????????????????????????????????????????????????????category_id????????????????????????category???
     * ??????????????????category_id?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????category_name
     * ???????????????brand????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ?????????????????????????????????
     *
     *
     * ?????????????????????
     * @param categoryEntity
     */
    @CacheEvict(cacheNames = {ProductConstant.CacheName.PRODUCT_CATEGORY},
                allEntries = true)
    @Override
    public boolean updateCascadeById(CategoryEntity categoryEntity) {
        // ?????????category?????????
        this.updateById(categoryEntity);

        // ??????????????????????????????????????????????????????????????????????????????

        // brand_category_relation????????????category_id?????????category????????????????????????category_name
        if (!StringUtils.isEmpty(categoryEntity.getName())) {
            // ??????brand_category_relation?????????brand_name????????????
            categoryBrandRelationDao.updateCategoryName(categoryEntity.getCatId(), categoryEntity.getName());
        }

        // TODO ?????????????????????????????????????????????????????????
        return true;
    }

    /**
     * ??????????????????????????????
     * @return
     */
    @Cacheable(cacheNames = {ProductConstant.CacheName.PRODUCT_CATEGORY},
            key = "'level1Categories'")
    @Override
    public List<CategoryEntity> getLevel1Categories() {
        return this.list(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    /**
     * ???????????????????????????????????????????????????     ----??????
     *
     * springCache??????
     * @return
     */
    @Cacheable(cacheNames = {ProductConstant.CacheName.PRODUCT_CATEGORY},
                key = "'catelogJson'")
    @Override
    public Map<String, List<Catelog2VO>> getCatelogJson() {
        return getCatelogJsonFromDB();
    }

    /**
     * ???????????????????????????????????????????????????  ---- ??????
     *
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????redis
     * @return
     */
    private Map<String, List<Catelog2VO>> getCatelogJsonOld() {
        String str = stringRedisTemplate.opsForValue().get(ProductConstant.RedisKey.CATELOG_JSON_VALUE);
        // ??????redis????????????????????????
        if (!StringUtils.isEmpty(str)) {
            System.out.println("?????????????????????????????????");
            return JSON.parseObject(str, new TypeReference<Map<String, List<Catelog2VO>>>(){});
        //  redis?????????
        } else {
            System.out.println("????????????????????????????????????...");
            /**
             * ????????????????????????????????????????????????????????????
             * ??????????????????????????????????????????????????????????????????????????????
             * ???????????????????????????????????????????????????????????????????????????????????????????????????
             * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
             * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
             * ????????????????????????????????????????????????????????????
             *
             * ???????????????????????? ???????????????+???????????? ??????????????????????????????????????????????????????????????????
             * ?????????????????????????????????????????????????????????????????????????????????????????????????????????
             */
            // ???????????????
            // Map<String, List<Catelog2VO>> catelogJsonFromDB = getCatelogJsonWithLocalLock();
            // ??????redis???????????????????????????
            // Map<String, List<Catelog2VO>> catelogJsonFromDB = getCatelogJsonWithRedisLock();
            // ??????redisson????????????
            Map<String, List<Catelog2VO>> catelogJsonFromDB = getCatelogJsonWithRedisson();
            // ??????
            return catelogJsonFromDB;
        }
    }


    /**
     * ???????????????
     * synchronized??????????????????????????????????????????????????????????????????????????????
     * springboot??????????????????????????????????????????????????????this???????????????
     *
     * ???????????????????????????????????????????????????????????????????????????????????????????????????
     *      ?????????8??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *      ???????????????????????????????????????????????????????????????????????????
     *
     * ?????????????????????????????????????????????????????????
     * ???????????????redis??????????????????????????????????????????????????????redis
     * ?????????????????????????????????????????????????????????????????????redis??????????????????????????????????????????
     * @return
     */
    private Map<String, List<Catelog2VO>> getCatelogJsonWithLocalLock() {
        synchronized (this) {
            // ?????????????????????????????????????????????????????????????????????(??????????????????????????????????????????????????????)
            // ??????????????????????????????
            return getCatelogJsonData();
        }
    }

    /**
     * ??????redis???set???del????????????lua???????????????????????????????????????
     * https://redis.io/commands/set
     *
     * setnx?????? : ????????????????????????????????????????????????????????????????????????????????????
     * ???????????????set<key,value>?????????????????????????????????????????????????????????????????????????????????????????????????????????
     *  1. ???redis????????????????????????
     *  2. ???????????????????????????
     *  3. ?????????
     *
     *  ????????????
     *      ??????????????????????????????????????????????????????????????????????????????????????????
     *  ?????????
     *      ?????????????????????????????????????????????????????????????????????????????????????????????????????????redis???????????????
     *  ?????????
     *      Boolean result = stringRedisTemplate.opsForValue().setIfAbsent("lock","v");
     *      stringRedisTemplate.expire("lock", 30s)
     *      ????????????
     *      del("lock")
     *  ????????????
     *      ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *  ?????????
     *      ???????????????+?????????????????? ?????????????????????
     *      setnx????????????????????????????????????
     *      ??????????????????????????????????????? redisTemplate.setIfAbsent("lock","v",30s)
     *
     *  ????????????
     *      ??????+????????????????????????????????????
     *      ??????????????????????????????????????????????????????????????????redis???????????????
     *      ????????????????????????????????????(??????????????????)???
     *      ??????????????????????????????????????????????????????????????????????????????????????????
     *  ?????????
     *      ??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *      ???????????????????????????????????????redis???????????????????????????????????????????????????????????????????????????????????????????????????
     *      ???????????????????????????????????????????????????redis?????????????????????????????????????????????????????????????????????
     *  ?????????
     *      ??????set("lock",uuid)?????????????????????
     *      ????????????
     *      ???if(uuid.equals(redis.get("lock")))  ???redis.del("key")
     *  ????????????
     *      ?????????????????????????????????????????????????????????
     *      ?????????????????????????????????????????????????????????redis?????????????????????????????????
     *      ????????????????????????????????????????????????????????????????????????????????????redis?????????????????????
     *      ???????????????del("lock")????????????????????????(????????????????????????)
     *  ?????????
     *      ?????? ??????????????????????????????????????????  ????????????????????????
     *      ???get()??????del()???????????????redis?????????lua??????????????????????????????????????????
     *      https://redis.io/commands/set
     *
     *  ?????????
     *      ???????????????????????????????????????????????????????????????????????????????????????????????????
     *      ???????????????????????????????????????????????????????????????redis??????????????????
     * @return
     */
    private Map<String, List<Catelog2VO>> getCatelogJsonWithRedisLock() {
        // ?????????????????????
        String uuid = UUID.randomUUID().toString();
        // 1. ???redis????????????????????????????????????????????????30s
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(ProductConstant.RedisKey.CATELOG_JSON_LOCK, uuid, 30, TimeUnit.SECONDS);
        // ????????????
        Map<String, List<Catelog2VO>> data = null;
        if (result) {
            // 2. ????????????
            try {
                // ?????????????????????????????????????????????????????????????????????(??????????????????????????????????????????????????????)
                // ??????????????????????????????
                data =  getCatelogJsonData();
            } finally {
            //     3. ?????????????????????, lua??????
            //     String res = stringRedisTemplate.opsForValue().get(ProductConstant.RedisKey.CATELOG_JSON_LOCK);
            //     // ?????????????????????
            //     if (res.equals(uuid)) {
            //         stringRedisTemplate.delete(ProductConstant.RedisKey.CATELOG_JSON_LOCK);
            //     }
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                stringRedisTemplate.execute(new DefaultRedisScript<Integer>(script) {}, Arrays.asList(ProductConstant.RedisKey.CATELOG_JSON_LOCK), uuid);
                return data;
            }
        } else {
            //    ??????100ms?????????????????????????????????
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("heheheheh");
            }
            return getCatelogJsonWithRedisLock();
        }
    }


    /**
     * ?????????????????? redisson
     * https://redis.io/topics/distlock
     * https://github.com/redisson/redisson
     *
     * Redisson????????????Redis?????????????????????Java????????????????????????In-Memory Data Grid????????????????????????????????????????????????Java???????????????????????????????????????????????????
     * ????????????(BitSet, Set, Multimap, SortedSet, Map, List, Queue, BlockingQueue, Deque, BlockingDeque, Semaphore, Lock, AtomicLong, CountDownLatch, Publish / Subscribe, Bloom filter, Remote service, Spring cache, Executor service, Live Object service, Scheduler service)
     * Redisson???????????????Redis????????????????????????????????????Redisson??????????????????????????????Redis??????????????????Separation of Concern?????????????????????????????????????????????????????????????????????????????????
     *
     * 1. ??????erdisson???????????????maven??????
     * 2. ???????????????????????? RedissonClient??????
     * 3. @autowired??????RedissonClient??????
     * 4. ????????? ????????????????????????
     *          // ?????????????????????????????????????????????
     *         RLock lock = redissonClient.getLock("??????");
     *         // ???????????????
     *         redissonClient.getReadWriteLock("anyRWLock");
     *         // ?????????
     *         redissonClient.getSemaphore("semaphore");
     *
     *    Rlock?????????juc??????lock????????????????????????????????????????????????
     *
     * 5. ?????????????????????
     *   ?????????????????? lock.lock();
     *      Redisson???????????????????????????????????????????????????????????????Redisson?????????????????????(????????????)?????????????????????????????????
     *      ?????????????????????????????????????????????????????????30??????????????????????????????Config.lockWatchdogTimeout???????????????
     *      ????????????????????????????????????????????????????????????????????????30s???????????????????????????????????????????????????????????????????????????30s???????????????????????????
     *      ?????????????????????????????????????????????????????????
     *      ????????????????????????????????? ??????????????? / 3  = 10s
     *
     *   ??????????????? lock.lock(10, TimeUnit.SECONDS);??????????????????
     *      ???????????????????????????????????????????????????????????????????????????????????????
     *      ???????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *      ????????????unlock????????????????????????????????????
     *      ????????????redis???????????????????????????????????????????????????????????????lua????????????
     *      ???????????????????????????????????????????????????????????????????????????lua????????????????????????????????????
     *      ??????????????????????????????????????????????????????????????????????????????
     *
     *
     * @return
     */
    private Map<String, List<Catelog2VO>> getCatelogJsonWithRedisson() {
        // ???????????????????????????
        RLock lock = redissonClient.getLock("anyLock");
        // ??????
        lock.lock();
        Map<String, List<Catelog2VO>> catelogJsonData = null;
        try {
            // ????????????
            // ????????????????????????????????????redisson???????????????????????????
            Thread.sleep(30000);
            catelogJsonData = getCatelogJsonData();
        } catch (InterruptedException e) {
            System.out.println("hahahahha");
        } finally {
        //     ?????????
            lock.unlock();
        }
        return catelogJsonData;
    }

    /**
     * ???????????????????????????????????????????????????????????????
     * @return
     */
    private Map<String, List<Catelog2VO>> getCatelogJsonData() {
        // ?????????????????????????????????????????????????????????????????????(??????????????????????????????????????????????????????)
        String str = stringRedisTemplate.opsForValue().get(ProductConstant.RedisKey.CATELOG_JSON_VALUE);
        // ??????redis????????????????????????
        if (!StringUtils.isEmpty(str)) {
            return JSON.parseObject(str, new TypeReference<Map<String, List<Catelog2VO>>>() {
            });
        }
        // ????????????????????????????????????DB
        Map<String, List<Catelog2VO>> catelogMap = getCatelogJsonFromDB();
        // ???????????????
        stringRedisTemplate.opsForValue().set(ProductConstant.RedisKey.CATELOG_JSON_VALUE, JSON.toJSONString(catelogMap));
        // ?????????????????????map
        return catelogMap;
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     * ?????????????????? Map<id, List<Catelog2VO>>???map???????????????catelog1Id???????????????????????????id
     * @return
     */
    private Map<String, List<Catelog2VO>> getCatelogJsonFromDB() {
        System.out.println("????????????MySQL?????????...");
        // ?????????????????????
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        // ??????????????????
        List<CategoryEntity> level1Categories = categoryEntities.stream().filter(item -> item.getParentCid() == 0).collect(Collectors.toList());

        Map<String, List<Catelog2VO>> catelogMap = level1Categories.stream()
                // collectors.toMap ??????map
                .collect(Collectors.toMap(
                        // ???????????????catId??????map??????
                        level1Category -> level1Category.getCatId().toString(),
                        // ???????????????????????????????????????map??????
                        leve1Category -> {
                            // ??????????????????????????????????????????????????????
                            List<Catelog2VO> catelog2VOList = categoryEntities.stream().filter(category -> category.getParentCid().equals(leve1Category.getCatId()))
                                    // ??????????????????????????? -> Catelog2VO
                                    .map(level2Category -> {
                                        // ????????????
                                        Catelog2VO catelog2VO = new Catelog2VO();
                                        catelog2VO.setCatelog1Id(leve1Category.getCatId().toString());
                                        catelog2VO.setId(level2Category.getCatId().toString());
                                        catelog2VO.setName(level2Category.getName());
                                        // Catelog2VO??????????????????Catelog3VO
                                        // ????????????????????????????????????????????????
                                        List<Catelog2VO.Catelog3VO> catelog3VOList = categoryEntities.stream().filter(category -> category.getParentCid().equals(level2Category.getCatId()))
                                                // ?????????????????????????????? -> catelog3VO
                                                .map(level3Category -> {
                                                    // ????????????
                                                    Catelog2VO.Catelog3VO catelog3VO = new Catelog2VO.Catelog3VO();
                                                    catelog3VO.setCatelog2Id(level2Category.getCatId().toString());
                                                    catelog3VO.setId(level3Category.getCatId().toString());
                                                    catelog3VO.setName(level3Category.getName());
                                                    return catelog3VO;
                                                }).collect(Collectors.toList());
                                        // ?????????catelog2VO???????????????
                                        catelog2VO.setCatelog3List(catelog3VOList);
                                        return catelog2VO;
                                    }).collect(Collectors.toList());
                            // ??????catelog2VOList?????????map<k,v>??????
                            return catelog2VOList;
                        }));
        return catelogMap;
    }

    // ?????? [225, 25, 2]
    private LinkedList<Long> getFullPath(Long catId, LinkedList<Long> path) {
        // ?????????????????????
        path.add(catId);
        // ??????????????????????????????
        CategoryEntity categoryEntity = this.getById(catId);
        if (categoryEntity.getParentCid() != 0) {
            // ???????????????????????????
            getFullPath(categoryEntity.getParentCid(), path);
        }
        // ??????????????????
        return path;
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * @param entity
     * @param entityList
     * @return
     */
    private CategoryEntity getChildren(CategoryEntity entity, List<CategoryEntity> entityList) {
        // ????????????children
        entity.setChildren(entityList.stream()
                // ????????????????????????(??????)
                .filter(categoryEntity -> categoryEntity.getParentCid().equals(entity.getCatId()))
                // ?????????????????????????????????????????????(?????????)????????????????????????
                .map(item -> getChildren(item, entityList))
                // ???????????????????????????
                .sorted(Comparator.comparingInt(CategoryEntity::getSort))
                // ???????????????????????????????????????????????????
                .collect(Collectors.toList()));
        // ??????????????????????????????????????????
        return entity;
    }

}
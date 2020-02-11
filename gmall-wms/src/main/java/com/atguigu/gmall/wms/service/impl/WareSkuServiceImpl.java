package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;


    private static final String KEY_PREFIX="wms:stock:";
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    //正常情况下不需要返回数据，
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> skuLockVos) {

        if(CollectionUtils.isEmpty(skuLockVos)){
            return  null;
        }

        //遍历清单集合，验库存并锁库存
        skuLockVos.forEach(skuLockVo -> {
                checkLock(skuLockVo);
        });

        //判断锁定的结果集中是否包含锁定失败的商品,如果包含就需要回滚锁定成功的数据
        if(skuLockVos.stream().anyMatch(skuLockVo -> skuLockVo.getLock()==false)){
            skuLockVos.stream().filter(skuLockVo -> skuLockVo.getLock()).forEach(skuLockVo -> {
                wareSkuDao.unLock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
            });

            return skuLockVos;
        }
        String orderToken = skuLockVos.get(0).getOrderToken();
        redisTemplate.opsForValue().set(KEY_PREFIX+orderToken, JSON.toJSONString(skuLockVos));


       //定时释放库存
        amqpTemplate.convertAndSend("ORDER-EXCHANGE","wms.ttl",orderToken);

        return null;
    }

    /**
     * 验库存并锁库存
     * 分布式锁
     */
    private void checkLock(SkuLockVo skuLockVo){

        RLock fairLock = redissonClient.getFairLock("lock" + skuLockVo.getSkuId());
        fairLock.lock();
        //验库存
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.check(skuLockVo.getSkuId(), skuLockVo.getCount());
        if(!CollectionUtils.isEmpty(wareSkuEntities)){
            //锁库存
            WareSkuEntity wareSkuEntity = wareSkuEntities.get(0);
            int lock = wareSkuDao.lock(wareSkuEntity.getId(), skuLockVo.getCount());
            if(lock!=0){
                skuLockVo.setLock(true);
                skuLockVo.setWareSkuId(wareSkuEntity.getId());
            }
        }
        fairLock.unlock();
    }




}
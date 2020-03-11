package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
public class StockListener {

    @Autowired
    private StringRedisTemplate redisTemplater;

    @Autowired
    private WareSkuDao wareSkuDao;

    private static final String KEY_PREFIX = "wms:stock:";

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "STOCK-UNLOCK-QUEUE", durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"stock.unlock", "wms.dead"}
    ))
    public void unlock(String orderToken) {

        String json = redisTemplater.opsForValue().get(KEY_PREFIX + orderToken);
        System.out.println(KEY_PREFIX + orderToken + json);
        if (StringUtils.isEmpty(json)) {
            return;
        }

        //反序列化
        List<SkuLockVo> skuLockVos = JSON.parseArray(json, SkuLockVo.class);
        skuLockVos.forEach(skuLockVo -> {
            wareSkuDao.unLock(skuLockVo.getWareSkuId(), skuLockVo.getCount());
        });

        //防止重复解锁库存
        redisTemplater.delete(KEY_PREFIX + orderToken);
    }
}

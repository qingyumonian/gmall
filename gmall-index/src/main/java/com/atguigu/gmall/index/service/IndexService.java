package com.atguigu.gmall.index.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.vo.CategoryVo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexService {


    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final String cate="index:cates";

    @GmallCache(value = "index:cates",timeout = 7200,bound = 100,lockName = "lock")
    public List<CategoryEntity> queryLvl1Catageory() {

        Resp<List<CategoryEntity>> listResp = pmsClient.queryLevelOrCid(1, null);
        List<CategoryEntity> data = listResp.getData();
        return  data;

    }

    @GmallCache(value = "index:cates",timeout = 7200,bound = 100,lockName = "lock")
    public List<CategoryVo> queryCategoryWithSub(Long pid) {
//        //查询缓存中是否有数据
//        String cateJSON = redisTemplate.opsForValue().get(cate+pid);
//        if(!StringUtils.isEmpty(cateJSON)){
//            List<CategoryVo> categoryVoList = JSON.parseArray(cateJSON, CategoryVo.class);
//
//            return categoryVoList;
//        }
//
//        //加入分布式锁
//        RLock lock = redissonClient.getLock("lock" + pid);
//        lock.lock(10L, TimeUnit.SECONDS);
//
//        //再次判断缓存中是否有数据
//        String cateJSON1 = redisTemplate.opsForValue().get(cate+pid);
//        if(!StringUtils.isEmpty(cateJSON1)){
//            List<CategoryVo> categoryVoList = JSON.parseArray(cateJSON1, CategoryVo.class);
//            lock.unlock();
//            return categoryVoList;
//        }

        //没有则去查询数据库，
        Resp<List<CategoryVo>> listResp = pmsClient.queryCategoryWithSub(pid);
        //将数据放入缓存中去
//        redisTemplate.opsForValue().set(cate+pid,JSON.toJSONString(listResp.getData()));
//
//        lock.unlock();

        return listResp.getData();
    }

    public  void testNum() {

        //解决分布式锁的问题，setIfAbsent 设置了只有一个能获取该锁，其他只能等待，并重新发送请求
//        String uuid = UUID.randomUUID().toString();
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,3L, TimeUnit.SECONDS);
//        if(lock){
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        String num = redisTemplate.opsForValue().get("num");
            if(num==null){
                return;
            }
            else {
                Integer valueOf = Integer.valueOf(num);
                redisTemplate.opsForValue().set("num",(++valueOf).toString());
            }
            lock.unlock();
            //            if(StringUtils.equals(uuid,redisTemplate.opsForValue().get("lock"))){
//                redisTemplate.delete("lock");
//            }
//        }else{
//            try {
//                Thread.sleep(1000);
//                testNum();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

    }

    public void read() {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("RWlock");
        RLock rLock = readWriteLock.readLock();
        rLock.lock(10,TimeUnit.SECONDS);
        String name = redisTemplate.opsForValue().get("name");
        System.out.println("获取数据："+name);
    }

    public void write() {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("RWlock");
        RLock wLock =readWriteLock.writeLock();
        wLock.lock(10,TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("name","hhhh");
        System.out.println("设置数据："+"hhh");
    }
}

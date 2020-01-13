package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    //环绕通知，并且该通知只对加了GmallCache注解的方法有效
    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{

        MethodSignature singnature=(MethodSignature)joinPoint.getSignature();
        Method method = singnature.getMethod();
        Class returnType = singnature.getReturnType();//获取切点方法的返回值类型
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);//获取注解对象

        //1.获取缓存数据
        String prefix = gmallCache.value();
        List<Object> ages = Arrays.asList(joinPoint.getArgs());
        String key=prefix+ages;

        //判断缓存是否为空
        Object cache = this.getCache(key, returnType);
        if(cache!=null){
            return  cache;
        }
        //缓存为空，加分布式锁
        String lockName = gmallCache.lockName();
        RLock fairLock = redissonClient.getFairLock(lockName + ages);
        fairLock.lock();

        //再判断缓存是否为空
        Object cache1 = this.getCache(key, returnType);
        if(cache1!=null){
            fairLock.unlock();
            return  cache1;
        }

        //，从数据库获取数据
        Object result = joinPoint.proceed(joinPoint.getArgs());

        //把查询出来的数据放入缓存
        redisTemplate.opsForValue().set(key,JSON.toJSONString(result),gmallCache.timeout()+new Random().nextInt(gmallCache.bound()), TimeUnit.MINUTES);


        fairLock.unlock();
        return result;
    }

    private Object getCache(String key,Class returnType){
        String cateJSON = redisTemplate.opsForValue().get(key);
        //判断缓存是否为空
        if(!StringUtils.isEmpty(cateJSON)){
            return JSON.parseObject(cateJSON,returnType);

        }

        return null;
    }

}

package com.atguigu.gmall.index.config;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GmallCache {


    //自定义缓存的key值
    String value() default "";

    //自定义缓存的有效时间
    //单位是分钟
    int timeout() default 30;

    //防止雪崩，而设置随机值范围
    int bound() default  5;

    //自定义分布式锁的名称
    String lockName() default "lock";
}

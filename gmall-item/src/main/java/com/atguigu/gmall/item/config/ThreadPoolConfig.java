package com.atguigu.gmall.item.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sun.awt.windows.ThemeReader;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor getThreadPoolExecutor(){

        return  new ThreadPoolExecutor(50,100,30L, TimeUnit.MINUTES,new ArrayBlockingQueue<>(30));
    }









}

package com.atguigu.gmall.wms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitConfig {

    /**
     * 声明一个延时队列
     * 延时时间：1min
     * 死信路由：order-exchange
     * 死信routingkey：wms.dead
     * @return
     */
    @Bean("ttl-queue")
    public Queue ttlQueue(){
        Map<String, Object> map = new HashMap<>();
        map.put("x-dead-letter-exchange", "ORDER-EXCHANGE");
        map.put("x-dead-letter-routing-key", "wms.dead");
        map.put("x-message-ttl", 90000);
        Queue queue = new Queue("WMS-TTL-QUEUE",true,false,false,map);

        return queue;
    }

    /**
     * 延时队列绑定到交换机
     * @return
     */
    @Bean("ttl-binding")
    public Binding ttlBinding(){
        return  new Binding("WMS-TTL-QUEUE",Binding.DestinationType.QUEUE,"ORDER-EXCHANGE","wms.ttl",null);
    }


//    @Bean("dead-queue")
//    public Queue deadQueue(){
//
//        Queue queue = new Queue("WMS-DEAD-QUEUE", true, false, false, null);
//        return queue;
//    }
//    /**
//     * 延时队列绑定到交换机
//     * @return
//     */
//    @Bean("dead-binding")
//    public Binding deadBinding(){
//        return  new Binding("WMS-DEAD-QUEUE",Binding.DestinationType.QUEUE,"ORDER-EXCHANGE","wms.dead",null);
//    }
}

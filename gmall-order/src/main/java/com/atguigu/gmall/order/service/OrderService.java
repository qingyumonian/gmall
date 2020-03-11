package com.atguigu.gmall.order.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {


    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private GmallWmsClient wmsClient;


    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;


//    private ThreadPoolExecutor threadPoolExecutor= new ThreadPoolExecutor(500, 800, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100000));
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    private static final String TOKEN_PREFIX="order:token:";



    //确认订单
    public OrderConfirmVo confirm() {

        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //获取用户的地址信息，远程接口
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            Resp<List<MemberReceiveAddressEntity>> memberResp = umsClient.queryAddressesByUserId(userInfo.getUserId());
            List<MemberReceiveAddressEntity> memberReceiveAddressEntities = memberResp.getData();
            orderConfirmVo.setAddresses(memberReceiveAddressEntities);

        }, threadPoolExecutor);

        //获取订单的详情列表，远程接口
        //注意获取订单的信息，不能所有的数据都从redis中获取，要从数据库获取，数据以数据库为准
        //从redis中获取购物车中选中的商品的skuId和商品数量,然后从数据库中查询其他数据
        CompletableFuture<Void> orderFuture = CompletableFuture.supplyAsync(() -> {
            Resp<List<Cart>> cartResp = cartClient.queryCheckedCarts(userInfo.getUserId());
            return cartResp.getData();
        }, threadPoolExecutor).thenAcceptAsync(carts -> {
            List<OrderItemVo> orderItemVos = carts.stream().map(cart -> {
                Long skuId = cart.getSkuId();
                Integer count = cart.getCount();
                OrderItemVo orderItemVo = new OrderItemVo();
                orderItemVo.setCount(count);
                orderItemVo.setSkuId(skuId);

                //查询sku相关信息
                CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {
                    Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if (skuInfoEntity != null) {
                        orderItemVo.setPrice(skuInfoEntity.getPrice());
                        orderItemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
                        orderItemVo.setWeight(skuInfoEntity.getWeight());
                        orderItemVo.setImage(skuInfoEntity.getSkuDefaultImg());
                    }

                }, threadPoolExecutor);

                //查询商品的库存信息
                CompletableFuture<Void> storeFuture = CompletableFuture.runAsync(() -> {

                    Resp<List<WareSkuEntity>> wareSkuResp = wmsClient.querySkuBySpuId(skuId);
                    List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                    if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                        orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                    }
                }, threadPoolExecutor);

                //查询销售属性
                CompletableFuture<Void> saleAttrFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = pmsClient.querySaleAttrValueBySkuId(skuId);
                    List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrResp.getData();

                    orderItemVo.setSaleAttrs(skuSaleAttrValueEntities);

                }, threadPoolExecutor);

                //查询营销信息
                CompletableFuture<Void> saleFuture = CompletableFuture.runAsync(() -> {
                    Resp<List<ItemSaleVO>> resp = smsClient.querySaleVoBySkuId(skuId);
                    List<ItemSaleVO> itemSaleVOS = resp.getData();
                    orderItemVo.setSales(itemSaleVOS);

                }, threadPoolExecutor);
                CompletableFuture.allOf(skuFuture, storeFuture, saleAttrFuture, saleFuture).join();
                return orderItemVo;
            }).collect(Collectors.toList());

            orderConfirmVo.setOrderItemVos(orderItemVos);
        }, threadPoolExecutor);


        //获取用户的积分信息，远程接口ums
        CompletableFuture<Void> boundFuture = CompletableFuture.runAsync(() -> {
            Resp<MemberEntity> memberEntityResp = umsClient.queryMemberById(userInfo.getUserId());
            MemberEntity memberEntity = memberEntityResp.getData();
            if (memberEntity != null) {
                orderConfirmVo.setBounds(memberEntity.getIntegration());
            }

        }, threadPoolExecutor);

        //防止重复提交的唯一标志
        //分布式ID生成器（mybites-plus中集成了一个）
        CompletableFuture<Void> tokenFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            orderConfirmVo.setOrderToken(orderToken);//浏览器一份

            //保存redis一份
            redisTemplate.opsForValue().set(TOKEN_PREFIX + orderToken, orderToken, 3, TimeUnit.HOURS);

        }, threadPoolExecutor);

        CompletableFuture.allOf(addressFuture,orderFuture,boundFuture,tokenFuture).join();

        return  orderConfirmVo;
    }

    public OrderEntity submit(OrderSubmitVo orderSubmitVo) {

            //1.校验是否重复提交
           //判断redis中有没有，有没有提交，没有已经提交
        String orderToken = orderSubmitVo.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(TOKEN_PREFIX + orderSubmitVo.getOrderToken()), orderToken);
        if(flag==0){
            throw new OrderException("请不要重复提交");
        }

        //2.验价
        BigDecimal totalPrice = orderSubmitVo.getTotalPrice();//页面提交的总价
        //获取数据库的实时价格
        List<OrderItemVo> items = orderSubmitVo.getItems();
        if(CollectionUtils.isEmpty(items)){
            throw new OrderException("请选中商品");
        }
        BigDecimal reduce = items.stream().map(orderItemVo -> {
            Long skuId = orderItemVo.getSkuId();
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity != null) {
                return skuInfoEntity.getPrice().multiply(new BigDecimal(orderItemVo.getCount()));
            }
            return new BigDecimal(0);
        }).reduce((a, b) -> a.add(b)).get();

        //比较价格是否一致
        if(totalPrice.compareTo(reduce)!=0){
            throw new OrderException("订单价格异常，请重新刷新页面");
        }

        //3.验证库存并锁定库存
         List<SkuLockVo> skuLockVos=items.stream().map(orderItemVo -> {
             SkuLockVo skuLockVo = new SkuLockVo();
             skuLockVo.setSkuId(orderItemVo.getSkuId());
             skuLockVo.setCount(orderItemVo.getCount());
             skuLockVo.setOrderToken(orderSubmitVo.getOrderToken());
         return skuLockVo;
         }).collect(Collectors.toList());
        Resp<List<SkuLockVo>> skuLockResp = wmsClient.checkAndLock(skuLockVos);
        List<SkuLockVo> skuLockVoList = skuLockResp.getData();
        if (!CollectionUtils.isEmpty(skuLockVoList)) {
           throw new OrderException("库存不足，请重新查看对应商品库存是否满足您的需求"+JSON.toJSONString(skuLockVoList));
        }
        //异常：后续订单无法创建，定时释放库存  PS:当验库锁库成功后，若发生问题，那么在锁库的为服务中会有超时机制，超过指定时间就会解锁库存。

        //4.新增订单，订单状态为未支付
        OrderEntity orderEntity=null;
        try {
            UserInfo userInfo = LoginInterceptor.getUserInfo();
            Resp<OrderEntity> orderEntityResp = omsClient.saveOrder(orderSubmitVo,userInfo.getUserId());
            orderEntity = orderEntityResp.getData();
        } catch (Exception e) {
            //释放库存，消息队列（异步）
            amqpTemplate.convertAndSend("ORDER-EXCHANGE","stock.unlock",orderSubmitVo.getOrderToken());
            e.printStackTrace();
            throw new OrderException("订单保存失败，服务错误") ;
        }

        //5.删除购物车中的相应的记录
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("userId",LoginInterceptor.getUserInfo().getUserId());
            List<Long> skuIds = items.stream().map(orderItemVo -> orderItemVo.getSkuId()).collect(Collectors.toList());
            map.put("skuIds",JSON.toJSONString(skuIds));
            amqpTemplate.convertAndSend("ORDER-EXCHANGE","cart.delete",map);
        } catch (AmqpException e) {
            e.printStackTrace();
        }

        return orderEntity;

    }
}

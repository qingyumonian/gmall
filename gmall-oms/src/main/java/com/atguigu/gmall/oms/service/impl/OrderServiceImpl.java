package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.core.exception.OrderException;
import com.atguigu.gmall.oms.dao.OrderItemDao;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private OrderItemDao orderItemDao;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo orderSubmitVo, Long userId) {
        //新增订单表
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderSubmitVo.getOrderToken());
        orderEntity.setTotalAmount(orderSubmitVo.getTotalPrice());
        orderEntity.setPayType(orderSubmitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setDeliveryCompany(orderSubmitVo.getDeliveryCompany());
        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(orderEntity.getCreateTime());
        orderEntity.setConfirmStatus(0);
        orderEntity.setStatus(0);
        orderEntity.setDeleteStatus(0);
        orderEntity.setMemberId(userId);
        //收货地址
        MemberReceiveAddressEntity address = orderSubmitVo.getAddress();
        if(address==null){
            throw new OrderException("订单地址异常，请重新提交");
        }
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverRegion(address.getRegion());
        //查询用户名
        boolean flag = save(orderEntity);
        //新增订单详情表
        if(flag){
            List<OrderItemVo> items = orderSubmitVo.getItems();
            if(!CollectionUtils.isEmpty(items)){
                items.forEach(orderItemVo -> {
                    OrderItemEntity orderItemEntity = new OrderItemEntity();
                    orderItemEntity.setOrderId(orderEntity.getId());
                    orderItemEntity.setOrderSn(orderSubmitVo.getOrderToken());
                    //先查询sku信息
                    Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(orderItemVo.getSkuId());
                    SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
                    if(skuInfoEntity!=null){
                        orderItemEntity.setSkuId(orderItemVo.getSkuId());
                        orderItemEntity.setSkuQuantity(orderItemVo.getCount());
                        orderItemEntity.setSkuPic(orderItemVo.getImage());
                        orderItemEntity.setSkuName(orderItemVo.getSkuTitle());
                        orderItemEntity.setSkuAttrsVals(JSON.toJSONString(orderItemVo.getSaleAttrs()));
                        orderItemEntity.setSkuPrice(skuInfoEntity.getPrice());
                        //设置spu相关信息
                        Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(skuInfoEntity.getSpuId());
                        SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
                        if(spuInfoEntity!=null){
                            orderItemEntity.setSpuId(spuInfoEntity.getId());
                            orderItemEntity.setSpuName(spuInfoEntity.getSpuName());
                            orderItemEntity.setSpuBrand(spuInfoEntity.getBrandId().toString());
                            orderItemEntity.setCategoryId(spuInfoEntity.getCatalogId());
                        }
                    }
                    orderItemDao.insert(orderItemEntity);
                });
            }
        }

        amqpTemplate.convertAndSend("ORDER-EXCHANGE", "order.ttl", orderSubmitVo.getOrderToken());
        return orderEntity;
    }

}
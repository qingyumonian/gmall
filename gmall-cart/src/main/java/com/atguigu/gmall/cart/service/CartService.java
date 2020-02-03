package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String key_prefix="cart:item:";

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    public void addCart(Cart cart) {

        //获取用户的登录信息
        String key=key_prefix;
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        if(userInfo.getUserId()!=null){
            key+=userInfo.getUserId();
        }else{
            key+=userInfo.getUserKey();
        }

        //1.获取购物车信息
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        //2.判断购物车中是否有该商品
        String skuId = cart.getSkuId().toString();
        Integer count = cart.getCount();
        if(hashOps.hasKey(skuId)){
            //有 更新数量
            String cartJson = hashOps.get(skuId).toString();
            cart=JSON.parseObject(cartJson,Cart.class);
            cart.setCount(cart.getCount()+count);
            hashOps.put(skuId,JSON.toJSONString(cart));
        }else{

            cart.setCheck(true);
            //无 新增

              //获取sku信息
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(cart.getSkuId());
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if(skuInfoEntity==null){
                return;
            }
            cart.setPrice(skuInfoEntity.getPrice());
            cart.setImage(skuInfoEntity.getSkuDefaultImg());
            cart.setSkuTitle(skuInfoEntity.getSkuTitle());

               //查询库存信息
            Resp<List<WareSkuEntity>> listResp = wmsClient.querySkuBySpuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = listResp.getData();
            if(!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()>0));
            }

               //查询销售属性
            Resp<List<SkuSaleAttrValueEntity>> saleAttrResp = pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = saleAttrResp.getData();
            cart.setSaleAttrs(skuSaleAttrValueEntities);

                //查询营销信息
            Resp<List<ItemSaleVO>>saleResp  = smsClient.querySaleVoBySkuId(cart.getSkuId());
            List<ItemSaleVO> itemSaleVOS = saleResp.getData();
            cart.setSales(itemSaleVOS);


            hashOps.put(skuId,JSON.toJSONString(cart));
        }

    }

    public List<Cart> queryCatrs() {

        //获取用户的登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        String userKey = userInfo.getUserKey();
        Long userId = userInfo.getUserId();
        //1.先查询未登录的购物车
         userKey=key_prefix+userKey;
        BoundHashOperations<String, Object, Object> userKeyHashOps = redisTemplate.boundHashOps(userKey);
        List<Object> values = userKeyHashOps.values();
        List<Cart>userKeyCarts=new ArrayList<>();
        if(!CollectionUtils.isEmpty(values)){
            userKeyCarts=values.stream().map(cartJson->JSON.parseObject(cartJson.toString(),Cart.class)).collect(Collectors.toList());
        }

        //2.判断是否登录
        if(userId==null){
            return  userKeyCarts;
        }

        //3.登录了合并登录的购物车到一登录的购物车
        String userIdKey=key_prefix+userId;
        BoundHashOperations<String, Object, Object> userIdHashOps = redisTemplate.boundHashOps(userIdKey);
        if(!CollectionUtils.isEmpty(userKeyCarts)){
            userKeyCarts.forEach(cart -> {//如果登良路状态下有数据就修改数量
                if(userIdHashOps.hasKey(cart.getSkuId().toString())){
                    String cartJson = userIdHashOps.get(cart.getSkuId().toString()).toString();
                    Integer count = cart.getCount();
                    cart = JSON.parseObject(cartJson, Cart.class);
                    cart.setCount(cart.getCount()+count);
                }

                //如果登录状态下没有记录直接新增
                userIdHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            });

        }

        //4.删除未登录的购物车
          redisTemplate.delete(userKey);
        //5展示查询
        List<Object> userIdCArtJson = userIdHashOps.values();
        if(!CollectionUtils.isEmpty(userIdCArtJson)){
            return userIdCArtJson.stream().map(cartJson->JSON.parseObject(cartJson.toString(),Cart.class)).collect(Collectors.toList());
        }
           return  null;
    }
}

package com.atguigu.gmall.search.listener;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PmsListener {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private GmallWmsClient wmsClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "GMALL-SEARCH-QUEUE",durable = "true"),
            exchange = @Exchange(value = "GMALL-PMS-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.insert"}
    ))
   public void  listener(Long spuId){

        //获取每个spu中的所有sku的商品的相关信息
        Resp<List<SkuInfoEntity>> skuResp = this.pmsClient.querySkuBySpuId(spuId);
        List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
        if (!CollectionUtils.isEmpty(skuInfoEntities)) {
            //将获取的sku的信息封装成Goods对象，并写入elasticsearch中去
            List<Goods> goodsList = skuInfoEntities.stream().map(skuInfoEntity -> {
                Goods goods = new Goods();

                // 查询库存信息
                Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.querySkuBySpuId(skuInfoEntity.getSkuId());
                List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                    goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                }
                goods.setSkuId(skuInfoEntity.getSkuId());
                goods.setSale(10L);
                goods.setPrice(skuInfoEntity.getPrice().doubleValue());
                goods.setCreateTime(pmsClient.querySpuById(spuId).getData().getCreateTime());
                Resp<CategoryEntity> categoryEntityResp = this.pmsClient.queryCategoryById1(skuInfoEntity.getCatalogId());
                CategoryEntity categoryEntity = categoryEntityResp.getData();
                if (categoryEntity != null) {
                    goods.setCategoryId(skuInfoEntity.getCatalogId());
                    goods.setCategoryName(categoryEntity.getName());
                }
                Resp<BrandEntity> brandEntityResp = this.pmsClient.queryBrandById(skuInfoEntity.getBrandId());
                BrandEntity brandEntity = brandEntityResp.getData();
                if (brandEntity != null) {
                    goods.setBrandId(skuInfoEntity.getBrandId());
                    goods.setBrandName(brandEntity.getName());
                }
                Resp<List<ProductAttrValueEntity>> attrValueResp = this.pmsClient.querySearchAttrValue(spuId);
                List<ProductAttrValueEntity> attrValueEntities = attrValueResp.getData();
                List<SearchAttrValue> searchAttrValues = attrValueEntities.stream().map(attrValueEntity -> {
                    SearchAttrValue searchAttrValue = new SearchAttrValue();
                    searchAttrValue.setAttrId(attrValueEntity.getAttrId());
                    searchAttrValue.setAttrName(attrValueEntity.getAttrName());
                    searchAttrValue.setAttrValue(attrValueEntity.getAttrValue());
                    return searchAttrValue;
                }).collect(Collectors.toList());
                goods.setAttrs(searchAttrValues);
                goods.setDefaultImage(skuInfoEntity.getSkuDefaultImg());
                goods.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
                goods.setSkuTitle(skuInfoEntity.getSkuTitle());
                return goods;
            }).collect(Collectors.toList());

            //存储数据到es中
            this.repository.saveAll(goodsList);
        }

   }
}

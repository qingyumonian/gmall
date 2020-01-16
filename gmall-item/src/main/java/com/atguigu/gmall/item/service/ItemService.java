package com.atguigu.gmall.item.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.config.ThreadPoolConfig;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVO;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class ItemService {

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    public ItemVO queryItemVO(Long skuId) {

        ItemVO itemVO = new ItemVO();
        //根据sku的id查询sku
        CompletableFuture<SkuInfoEntity> completableFuture = CompletableFuture.supplyAsync(() -> {
            itemVO.setSkuId(skuId);
            Resp<SkuInfoEntity> skuInfoEntityResp = pmsClient.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return null;
            }
            itemVO.setWeight(skuInfoEntity.getWeight());
            itemVO.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVO.setSkuSubtitle(skuInfoEntity.getSkuSubtitle());
            itemVO.setPrice(skuInfoEntity.getPrice());

            return skuInfoEntity;
        },threadPoolExecutor);

        //根据sku中的品牌的id查询分类
        CompletableFuture<Void> cateCompletablbe = completableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<CategoryEntity> categoryEntityResp = pmsClient.queryCategoryById1(skuInfoEntity.getCatalogId());
            CategoryEntity categoryEntity = categoryEntityResp.getData();
            if (categoryEntity != null) {

                itemVO.setCategoryId(categoryEntity.getCatId());
                itemVO.setCategoryName(categoryEntity.getName());
            }

        },threadPoolExecutor);

        //根据sku中的brandID查询品牌
        CompletableFuture<Void> brandCompletablbe = completableFuture.thenAcceptAsync(skuInfoEntity -> {

            Resp<BrandEntity> brandEntityResp = pmsClient.queryBrandById(skuInfoEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResp.getData();
            if(brandEntity!=null){
                itemVO.setBrandId(brandEntity.getBrandId());
                itemVO.setBrandName(brandEntity.getName());

            }
        },threadPoolExecutor);

        //根据sku中的spu查询spu
        CompletableFuture<Void> spuCompletable = completableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoEntity> spuInfoEntityResp = pmsClient.querySpuById(skuInfoEntity.getSpuId());
            SpuInfoEntity spuInfoEntity = spuInfoEntityResp.getData();
            if (skuInfoEntity != null) {
                itemVO.setSpuId(spuInfoEntity.getId());
                itemVO.setSpuName(spuInfoEntity.getSpuName());
            }
        },threadPoolExecutor);
        //根据sku中的信息查询图片
        CompletableFuture<Void> imagesCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<SkuImagesEntity>> queryImagesBySkuId = pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> imagesEntities = queryImagesBySkuId.getData();
            if (!CollectionUtils.isEmpty(imagesEntities)) {
                itemVO.setImeges(imagesEntities);
            }
        },threadPoolExecutor);


        //根据skuid查询库存
        CompletableFuture<Void> skuCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<WareSkuEntity>> listResp = wmsClient.querySkuBySpuId(skuId);
            List<WareSkuEntity> wareSkuEntities = listResp.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVO.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }
        },threadPoolExecutor);


        //根据skuid查询营销信息3个
        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            Resp<List<ItemSaleVO>> listResp1 = this.smsClient.querySaleVoBySkuId(skuId);
            List<ItemSaleVO> itemSaleVOList = listResp1.getData();
            itemVO.setSales(itemSaleVOList);
        },threadPoolExecutor);


        //根据sku中的spuID查询描述信息
        CompletableFuture<Void> descCompletableFuture = completableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<SpuInfoDescEntity> spuInfoDescEntityResp = pmsClient.querySpuDescBySpuId(skuInfoEntity.getSpuId());
            SpuInfoDescEntity spuInfoDescEntity = spuInfoDescEntityResp.getData();
            if (spuInfoDescEntity != null && StringUtils.isNotBlank(spuInfoDescEntity.getDecript())) {

                itemVO.setDesc(Arrays.asList(StringUtils.split(spuInfoDescEntity.getDecript(), ",")));
            }
        },threadPoolExecutor);

        CompletableFuture<Void> groupCompletableFuture = completableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<ItemGroupVO>> listResp2 = this.pmsClient.queryItemGroupVOsByCidAndSpuId(skuInfoEntity.getCatalogId(), skuInfoEntity.getSpuId());
            List<ItemGroupVO> itemGroupVOS = listResp2.getData();
            itemVO.setGroupVOS(itemGroupVOS);

        },threadPoolExecutor);


        //根据sku中的spuId查询skus
        //根据skuIds查询销售属性
        CompletableFuture<Void> attrValueCompletableFuture = completableFuture.thenAcceptAsync(skuInfoEntity -> {
            Resp<List<SkuSaleAttrValueEntity>> listResp3 = pmsClient.querySaleAttrValueBySpuId(skuInfoEntity.getSpuId());
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = listResp3.getData();
            itemVO.setSaleAttrs(skuSaleAttrValueEntities);

        },threadPoolExecutor);

        CompletableFuture.allOf(completableFuture,cateCompletablbe,brandCompletablbe,spuCompletable,imagesCompletableFuture
                               ,skuCompletableFuture,saleCompletableFuture,descCompletableFuture,
                groupCompletableFuture,attrValueCompletableFuture).join();

        return itemVO;
    }
}

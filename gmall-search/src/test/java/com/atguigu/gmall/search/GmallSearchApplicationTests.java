package com.atguigu.gmall.search;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValue;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {


    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    private GmallWmsClient wmsClient;

    @Test
    void contextLoads() {

      restTemplate.createIndex(Goods.class);
      restTemplate.putMapping(Goods.class);
    }
    @Test
    void tes1(){
        Resp<CategoryEntity> resp = pmsClient.queryCategoryById1((long) 1);
        System.out.println(resp.getData());
    }

    @Test
    void importData(){

        Long pageNum = 1l;
        Long pageSize = 100l;

        do{
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setPage(pageNum);
            queryCondition.setLimit(pageSize);
            // 分页查询spu
            Resp<List<SpuInfoEntity>> listResp = this.pmsClient.querySpuByPage(queryCondition);
            List<SpuInfoEntity> spuInfoEntities = listResp.getData();
            // 判断spu是否为空
            if (CollectionUtils.isEmpty(spuInfoEntities)){
                return;
            }

            // 遍历spu，获取sku导入es
            spuInfoEntities.forEach(spuInfoEntity -> {
                Resp<List<SkuInfoEntity>> skuResp = this.pmsClient.querySkuBySpuId(spuInfoEntity.getId());
                List<SkuInfoEntity> skuInfoEntities = skuResp.getData();
                if (!CollectionUtils.isEmpty(skuInfoEntities)) {
                    List<Goods> goodsList = skuInfoEntities.stream().map(skuInfoEntity -> {
                        Goods goods = new Goods();

                        // 查询库存信息
                        Resp<List<WareSkuEntity>> wareSkuResp = this.wmsClient.querySkuBySpuId(skuInfoEntity.getSkuId());
                        List<WareSkuEntity> wareSkuEntities = wareSkuResp.getData();
                        if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                            goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                        }
                        goods.setSkuId(skuInfoEntity.getSkuId());
                        goods.setSale(10l);
                        goods.setPrice(skuInfoEntity.getPrice().doubleValue());
                        goods.setCreateTime(spuInfoEntity.getCreateTime());
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
                        Resp<List<ProductAttrValueEntity>> attrValueResp = this.pmsClient.querySearchAttrValue(spuInfoEntity.getId());
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
                    this.repository.saveAll(goodsList);
                }
            });

            pageSize = (long) spuInfoEntities.size();
            pageNum++;
        } while (pageSize == 100);
    }
}

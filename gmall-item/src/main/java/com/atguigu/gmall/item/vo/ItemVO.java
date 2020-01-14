package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情页
 */
@Data
public class ItemVO {

    //1、当前sku的基本信息
    private Long skuId;
    private Long spuId;
    private String spuName;
    private Long categoryId;
    private String categoryName;
    private String brandName;
    private Long brandId;
    private String skuTitle;
    private String skuSubtitle;
    private BigDecimal price;
    private BigDecimal weight;
    private Boolean store;


    //2、sku的所有图片
    private List<SkuImagesEntity> imeges;


    //3、sku的所有促销信息
    private List<ItemSaleVO> sales;

    //4、sku的所有销售属性组合
    private List<SkuSaleAttrValueEntity> saleAttrs;

    //5、spu的所有基本属性
    private List<ItemGroupVO> groupVOS;

    //6、详情介绍
    private List<String> desc;
}
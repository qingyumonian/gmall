package com.atguigu.gmall.oms.vo;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    private Long skuId;
    private String skuTitle;
    private String image;
    private List<SkuSaleAttrValueEntity> saleAttrs;//销售属性
    private BigDecimal price;       //加入购物车时的价格
    private Integer count;
    private Boolean store=false;//库存
    private List<ItemSaleVO> sales;//促销信息
    private BigDecimal weight;//重量
}

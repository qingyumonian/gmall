package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVo {

    private Long skuId;
    private Integer count;
    private Boolean lock=false;//锁定状态，true-验库并锁定成功，false-库存不足

    private Long wareSkuId;//wms_ware_sku表的主键
    private  String orderToken;
}

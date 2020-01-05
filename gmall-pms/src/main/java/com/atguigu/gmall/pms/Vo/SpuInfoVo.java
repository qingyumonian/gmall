package com.atguigu.gmall.pms.Vo;

import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuInfoVo extends SpuInfoEntity {


    private List<String> spuImages;

    private List<BaseAttrValueVo>baseAttrs;

    private List<SkuInfoVo>skus;


}

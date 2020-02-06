package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.dao.SkuFullReductionDao;
import com.atguigu.gmall.sms.dao.SkuLadderDao;
import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVO;
import com.atguigu.gmall.sms.vo.SaleVo;
import io.swagger.models.auth.In;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.sms.dao.SkuBoundsDao;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsDao, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    private SkuLadderDao skuLadderDao;

    @Autowired
    private SkuFullReductionDao skuFullReductionDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SkuBoundsEntity> page = this.page(
                new Query<SkuBoundsEntity>().getPage(params),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public void saveSale(SaleVo saleVo) {
        //3.1 skuBounds积分
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(saleVo,skuBoundsEntity);
        List<String> works = saleVo.getWork();
        skuBoundsEntity.setWork(new Integer(works.get(0))+new Integer(works.get(1))*2+new Integer(works.get(2))*4+new Integer(works.get(3))*8);
        this.save(skuBoundsEntity);
        //3.2skuLadder打折
        SkuLadderEntity ladderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(saleVo,ladderEntity);
        ladderEntity.setAddOther(saleVo.getLadderAddOther());
        skuLadderDao.insert(ladderEntity);
        //3.3fullReduction满减
        SkuFullReductionEntity fullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(saleVo,fullReductionEntity);
        fullReductionEntity.setAddOther(saleVo.getFullAddOther());
        skuFullReductionDao.insert(fullReductionEntity);
    }

    @Override
    public List<ItemSaleVO> querySaleVoBySkuId(Long skuId) {
        List<ItemSaleVO> itemSaleVOS = new ArrayList<>();
        //根据skuId查询积分信息
        SkuBoundsEntity skuBoundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if(skuBoundsEntity!=null){
            ItemSaleVO itemSaleVO = new ItemSaleVO();
            itemSaleVOS.add(itemSaleVO);
            itemSaleVO.setType("积分");
            itemSaleVO.setDesc("赠送成长"+skuBoundsEntity.getGrowBounds()+"积分,"+skuBoundsEntity.getBuyBounds()+"购物积分");
        }

        //根据sku信息查询打折信息
        SkuLadderEntity skuLadderEntity = this.skuLadderDao.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if(skuBoundsEntity!=null){
            ItemSaleVO itemSaleVO = new ItemSaleVO();
            itemSaleVOS.add(itemSaleVO);
            itemSaleVO.setType("打折");
            itemSaleVO.setDesc("满"+skuLadderEntity.getFullCount()+"打"+skuLadderEntity.getDiscount().divide(new BigDecimal(10))+"折");

        }


        //根据skuId查询满减信息
        SkuFullReductionEntity fullReductionEntity = this.skuFullReductionDao.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if(skuBoundsEntity!=null){
            ItemSaleVO itemSaleVO = new ItemSaleVO();
            itemSaleVOS.add(itemSaleVO);
            itemSaleVO.setType("满减");
            itemSaleVO.setDesc("满"+fullReductionEntity.getFullPrice()+"钱，减"+fullReductionEntity.getReducePrice()+"钱");

        }
        return itemSaleVOS;
    }

}
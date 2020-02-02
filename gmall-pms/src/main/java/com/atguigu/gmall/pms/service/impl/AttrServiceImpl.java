package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.Vo.AttrVo;
import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.service.AttrService;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {



    @Autowired
    private AttrAttrgroupRelationDao relationDao;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo queryAttrByCidOrTypePage(QueryCondition queryCondition, Long cid, Integer type) {
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<>();

        if (type != null) {
            wrapper.eq("attr_type",type);
        }
        wrapper.eq("catelog_id",cid);
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(queryCondition),
                wrapper
        );



        return new PageVo(page);
    }

    @Override
    public void saveAttrVo(AttrVo attrVo) {

        //新增Attr
        this.save(attrVo);

        //新增ralation
        AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
        relationEntity.setAttrId(attrVo.getAttrId());
        relationEntity.setAttrGroupId(attrVo.getAttrGroupId());
        relationEntity.setAttrSort(0);
        int insert = relationDao.insert(relationEntity);
    }

}
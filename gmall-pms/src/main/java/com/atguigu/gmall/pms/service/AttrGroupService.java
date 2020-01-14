package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.Vo.GroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 属性分组
 *
 * @author saber
 * @email lxf@theking.com
 * @date 2019-12-31 16:41:32
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo queryGroupByCid(QueryCondition queryCondition, Long catId);

    GroupVo queryGroupVoByGid(Long gid);

    List<GroupVo> queryGroupVoById(Long catId);

    List<ItemGroupVO> queryItemGroupVOsByCidAndSpuId(Long cid, Long spuId);
}


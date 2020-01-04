package com.atguigu.gmall.sms.dao;

import com.atguigu.gmall.sms.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author saber
 * @email lxf@theking.com
 * @date 2020-01-04 11:47:23
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
